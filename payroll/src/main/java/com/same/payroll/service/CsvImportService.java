package com.same.payroll.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.RFC4180Parser;
import com.opencsv.RFC4180ParserBuilder;
import com.same.payroll.dto.CsvMappingDtos.*;
import com.same.payroll.entity.*;
import com.same.payroll.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvImportService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EmployeeBonusRepository employeeBonusRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final RestTemplate restTemplate;

    @Value("${python.service.url:http://localhost:8000}")
    private String pythonServiceUrl;

    private static final List<String> REQUIRED_FIELDS = List.of("full_name", "national_id");

    // ─── Step 1: Analyze headers ────────────────────────────────
    public MappingResponse analyzeHeaders(MultipartFile file) throws Exception {
        List<String> headers = extractHeaders(file);

        MappingRequest request = new MappingRequest(headers);
        MappingResponse response = restTemplate.postForObject(
                pythonServiceUrl + "/map-columns",
                request,
                MappingResponse.class
        );

        if (response != null) {
            List<String> mappedFields = response.getMappings().values()
                    .stream()
                    .filter(m -> !"UNMAPPED".equals(m.getStatus()))
                    .map(ColumnMapping::getMappedTo)
                    .filter(Objects::nonNull)
                    .toList();

            List<String> missing = REQUIRED_FIELDS.stream()
                    .filter(f -> !mappedFields.contains(f))
                    .toList();
            response.setMissingRequiredFields(missing);
        }

        return response;
    }

    // ─── Step 2: Confirm mappings and import ────────────────────
    public ImportResult importWithMapping(MultipartFile file, Map<String, String> confirmedMappings,
                                          Integer year, Integer month) throws Exception {
        List<String[]> rows = extractAllRows(file);
        if (rows.isEmpty()) return new ImportResult(0, 0, 0, List.of("Empty file"));

        // Clean headers: strip surrounding quotes
        String[] rawHeaders = rows.get(0);
        String[] headers = Arrays.stream(rawHeaders)
                .map(h -> h.trim().replace("\"", ""))
                .toArray(String[]::new);

        List<String[]> dataRows = rows.subList(1, rows.size());

        int success = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < dataRows.size(); i++) {
            String[] rawRow = dataRows.get(i);

            // Skip totals row: first cell is not a valid employee ID (all digits check)
            String firstCell = cleanValue(rawRow.length > 0 ? rawRow[0] : "");
            if (isTotalsRow(firstCell, rawRow)) {
                log.info("Skipping totals row at index {}", i + 2);
                continue;
            }

            // Skip completely empty rows
            if (Arrays.stream(rawRow).allMatch(v -> cleanValue(v).isEmpty())) continue;

            try {
                Map<String, String> rowData = mapRowToSchema(headers, rawRow, confirmedMappings);

                // Merge NOM + PRENOM into full_name if needed
                if (!rowData.containsKey("full_name")) {
                    String nom    = rowData.getOrDefault("last_name", "");
                    String prenom = rowData.getOrDefault("first_name", "");
                    if (!nom.isEmpty() || !prenom.isEmpty()) {
                        rowData.put("full_name", (prenom + " " + nom).trim());
                    }
                }

                saveEmployee(rowData, year, month);
                success++;
            } catch (Exception e) {
                errors.add("Row " + (i + 2) + ": " + e.getMessage());
                log.error("Error importing row {}: {}", i + 2, e.getMessage());
            }
        }

        return new ImportResult(dataRows.size(), success, errors.size(), errors);
    }

    // ─── Detect totals row ──────────────────────────────────────
    // Only skip if first cell is empty OR all non-empty cells are identical
    // (the real totals row in the original CSV had "45,45,45,45,45,0,0,0...")
    private boolean isTotalsRow(String firstCell, String[] row) {
        // Empty first cell = skip
        if (firstCell.isEmpty()) return true;

        // First cell must look like an employee ID (alphanumeric, not a pure large sum)
        // Employee IDs like "EMP001", "1", "12" are fine
        // A totals row first cell would be something non-ID like empty or same as count
        // Only skip if ALL non-empty values are the exact same string (pure totals row)
        List<String> nonEmpty = Arrays.stream(row)
                .map(this::cleanValue)
                .filter(v -> !v.isEmpty())
                .collect(java.util.stream.Collectors.toList());

        if (nonEmpty.isEmpty()) return true;

        long distinct = nonEmpty.stream().distinct().count();
        // True totals row: all values identical (e.g. "45,45,45,45,45...")
        return nonEmpty.size() > 4 && distinct == 1;
    }

    // ─── Map CSV row to schema fields ───────────────────────────
    private Map<String, String> mapRowToSchema(String[] headers, String[] values,
                                               Map<String, String> mappings) {
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < headers.length && i < values.length; i++) {
            String schemaField = mappings.get(headers[i]);
            String value = cleanValue(values[i]);
            if (schemaField != null && !value.isEmpty()) {
                result.put(schemaField, value);
            }
        }
        return result;
    }

    // ─── Save employee + attendance + bonus ─────────────────────
    private void saveEmployee(Map<String, String> data, Integer year, Integer month) {
        // Resolve or create department
        Department department = null;
        if (data.containsKey("department")) {
            department = departmentRepository.findByName(data.get("department"))
                    .orElseGet(() -> departmentRepository.save(
                            Department.builder().name(data.get("department")).build()
                    ));
        }

        // Resolve or create position
        Position position = null;
        if (data.containsKey("position")) {
            position = positionRepository.findByTitle(data.get("position"))
                    .orElseGet(() -> positionRepository.save(
                            Position.builder().title(data.get("position")).build()
                    ));
        }

        // Find existing employee by ID or create new
        String employeeIdStr = data.get("employee_id");
        Employee employee = null;

        if (employeeIdStr != null) {
            employee = employeeRepository.findByEmployeeId(employeeIdStr).orElse(null);
        }

        if (employee == null) {
            employee = Employee.builder()
                    .employeeId(employeeIdStr)
                    .fullName(data.get("full_name"))
                    .nationalId(data.get("national_id"))
                    .email(data.get("email"))
                    .phone(data.get("phone"))
                    .hireDate(parseDate(data.get("hire_date")))
                    .department(department)
                    .position(position)
                    .build();
            employee = employeeRepository.save(employee);
        }

        // Save salary if provided
        if (data.containsKey("gross_salary") || data.containsKey("base_salary")) {
            String salaryStr = data.getOrDefault("base_salary", data.get("gross_salary"));
            final Employee emp = employee;
            salaryStructureRepository.findByEmployeeIdAndIsCurrentTrue(emp.getId())
                    .ifPresentOrElse(
                            s -> { /* already has salary, skip */ },
                            () -> salaryStructureRepository.save(
                                    SalaryStructure.builder()
                                            .employee(emp)
                                            .baseSalary(parseBigDecimal(salaryStr))
                                            .effectiveDate(LocalDate.now())
                                            .isCurrent(true)
                                            .build()
                            )
                    );
        }

        // Save attendance record if month/year provided
        if (year != null && month != null) {
            final Employee emp = employee;
            AttendanceRecord record = AttendanceRecord.builder()
                    .employee(emp)
                    .year(year)
                    .month(month)
                    .hoursWorked(parseBigDecimal(data.get("hours_worked")))
                    .daysWorked(parseBigDecimal(data.get("days_worked")))
                    .publicHolidays(parseBigDecimal(data.get("public_holidays")))
                    .leaveDaysBase(parseBigDecimal(data.get("leave_days_base")))
                    .leavePay(parseBigDecimal(data.get("leave_pay")))
                    .advanceDeduction(parseBigDecimalOrZero(data.get("advance_deduction")))
                    .regime(data.get("regime"))
                    .affectation(data.get("affectation"))
                    .service(data.get("service"))
                    .section(data.get("section"))
                    .build();
            attendanceRecordRepository.save(record);
        }

        // Save bonus if provided
        boolean hasBonus = data.containsKey("bonus_gross") || data.containsKey("bonus_rappel")
                || data.containsKey("gratification_note");
        if (hasBonus && year != null && month != null) {
            final Employee emp = employee;
            EmployeeBonus bonus = EmployeeBonus.builder()
                    .employee(emp)
                    .year(year)
                    .month(month)
                    .bonusGross(parseBigDecimal(data.get("bonus_gross")))
                    .bonusRappel(parseBigDecimal(data.get("bonus_rappel")))
                    .gratificationNote(data.get("gratification_note"))
                    .build();
            employeeBonusRepository.save(bonus);
        }
    }

    // ─── Helpers ────────────────────────────────────────────────
    private String cleanValue(String value) {
        if (value == null) return "";
        return value.trim().replace("\"", "");
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || cleanValue(value).isEmpty()) return null;
        try {
            return new BigDecimal(cleanValue(value).replaceAll("[^\\d.]", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal parseBigDecimalOrZero(String value) {
        BigDecimal result = parseBigDecimal(value);
        return result != null ? result : BigDecimal.ZERO;
    }

    private LocalDate parseDate(String value) {
        if (value == null || cleanValue(value).isEmpty()) return null;
        List<DateTimeFormatter> formats = List.of(
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
        );
        for (DateTimeFormatter fmt : formats) {
            try { return LocalDate.parse(cleanValue(value), fmt); } catch (Exception ignored) {}
        }
        return null;
    }

    private List<String> extractHeaders(MultipartFile file) throws Exception {
        try (CSVReader reader = buildReader(file)) {
            String[] headers = reader.readNext();
            return headers != null ? Arrays.asList(headers) : List.of();
        }
    }

    private List<String[]> extractAllRows(MultipartFile file) throws Exception {
        try (CSVReader reader = buildReader(file)) {
            return reader.readAll();
        }
    }

    private CSVReader buildReader(MultipartFile file) throws Exception {
        RFC4180Parser parser = new RFC4180ParserBuilder().build();
        return new CSVReaderBuilder(new InputStreamReader(file.getInputStream()))
                .withCSVParser(parser)
                .build();
    }
}