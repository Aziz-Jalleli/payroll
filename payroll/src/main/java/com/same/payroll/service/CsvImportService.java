
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

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvImportService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final FamilySituationRepository familySituationRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final RestTemplate restTemplate;

    @Value("${python.service.url:http://localhost:8000}")
    private String pythonServiceUrl;

    private static final List<String> REQUIRED_FIELDS = List.of("full_name", "national_id");

    // ─────────────────────────────────────────────
    // Step 1: Analyze CSV headers → get mapping suggestions
    // ─────────────────────────────────────────────
    public MappingResponse analyzeHeaders(MultipartFile file) throws Exception {
        List<String> headers = extractHeaders(file);

        MappingRequest request = new MappingRequest(headers);
        MappingResponse response = restTemplate.postForObject(
                pythonServiceUrl + "/map-columns",
                request,
                MappingResponse.class
        );

        if (response != null) {
            Set<String> mappedFields = new HashSet<>();
            response.getMappings().values().forEach(m -> {
                if (m.getMappedTo() != null) mappedFields.add(m.getMappedTo());
            });

            // NOM + PRENOM counts as full_name
            boolean hasNom    = headers.stream().anyMatch(h -> clean(h).equalsIgnoreCase("NOM"));
            boolean hasPrenom = headers.stream().anyMatch(h -> clean(h).equalsIgnoreCase("PRENOM"));
            if (hasNom && hasPrenom) mappedFields.add("full_name");

            List<String> missing = REQUIRED_FIELDS.stream()
                    .filter(f -> !mappedFields.contains(f))
                    .toList();
            response.setMissingRequiredFields(missing);
        }

        return response;
    }

    // ─────────────────────────────────────────────
    // Step 2: Import with confirmed mappings
    // ─────────────────────────────────────────────
    public ImportResult importWithMapping(MultipartFile file, Map<String, String> confirmedMappings) throws Exception {
        List<String[]> rows = extractAllRows(file);
        if (rows.isEmpty()) return new ImportResult(0, 0, 0, List.of("Empty file"));

        String[] rawHeaders = rows.get(0);
        String[] headers = Arrays.stream(rawHeaders).map(this::clean).toArray(String[]::new);
        List<String[]> dataRows = rows.subList(1, rows.size());

        int success = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < dataRows.size(); i++) {
            String[] rawRow = dataRows.get(i);

            // Skip totals/summary rows — detected when first column is not numeric
            // or when all numeric columns look like sums
            if (isTotalsRow(rawRow, headers)) {
                log.info("Skipping totals row at line {}", i + 2);
                continue;
            }

            // Skip fully empty rows
            if (isEmptyRow(rawRow)) continue;

            try {
                Map<String, String> rowData = mapRowToSchema(headers, rawRow, confirmedMappings);
                saveEmployee(rowData);
                success++;
            } catch (Exception e) {
                errors.add("Row " + (i + 2) + ": " + e.getMessage());
                log.error("Error importing row {}: {}", i + 2, e.getMessage());
            }
        }

        return new ImportResult(dataRows.size(), success, errors.size(), errors);
    }

    // ─────────────────────────────────────────────
    // Totals row detection
    // A row is a totals row if:
    // - First cell is a number that matches count of other rows, OR
    // - First cell equals the number of data rows (summary count)
    // - Most numeric cells are larger than any individual employee value
    // ─────────────────────────────────────────────
    private boolean isTotalsRow(String[] row, String[] headers) {
        if (row.length == 0) return false;
        String firstCell = clean(row[0]);

        // If first cell is a number > 100 it's likely a totals count row
        try {
            double val = Double.parseDouble(firstCell);
            if (val > 100 && val == Math.floor(val)) return true;
        } catch (NumberFormatException ignored) {}

        // If first cell contains text like "TOTAL", "Total", "Somme"
        if (firstCell.toUpperCase().contains("TOTAL")
                || firstCell.toUpperCase().contains("SOMME")
                || firstCell.toUpperCase().contains("SUM")) {
            return true;
        }

        // If employee_id maps to first header and the value is suspiciously large
        // (e.g. "45" employees = total count appearing as employee ID)
        // Check: if ALL numeric columns have the same value (copy-paste totals pattern)
        long numericCount = Arrays.stream(row)
                .filter(cell -> {
                    try { Double.parseDouble(clean(cell)); return true; }
                    catch (NumberFormatException e) { return false; }
                }).count();

        if (numericCount > 3) {
            Set<String> numericValues = new HashSet<>();
            for (String cell : row) {
                try {
                    double v = Double.parseDouble(clean(cell));
                    if (v > 0) numericValues.add(String.valueOf(v));
                } catch (NumberFormatException ignored) {}
            }
            // If all numeric cells are the same value → totals pattern
            if (numericValues.size() == 1) return true;
        }

        return false;
    }

    private boolean isEmptyRow(String[] row) {
        return Arrays.stream(row).allMatch(cell -> cell == null || clean(cell).isEmpty());
    }

    // ─────────────────────────────────────────────
    // Map a CSV row to schema fields using confirmed mappings
    // Handles NOM + PRENOM → full_name merge
    // ─────────────────────────────────────────────
    private Map<String, String> mapRowToSchema(String[] headers, String[] values,
                                               Map<String, String> mappings) {
        Map<String, String> result = new HashMap<>();
        String nom = null, prenom = null;

        for (int i = 0; i < headers.length && i < values.length; i++) {
            String header = headers[i];
            String value  = clean(values[i]);
            if (value.isEmpty()) continue;

            // Capture NOM and PRENOM for later merge
            if (header.equalsIgnoreCase("NOM"))    { nom    = value; continue; }
            if (header.equalsIgnoreCase("PRENOM")) { prenom = value; continue; }

            String schemaField = mappings.get(header);
            if (schemaField != null) {
                result.put(schemaField, value);
            }
        }

        // Merge NOM + PRENOM into full_name
        if (nom != null || prenom != null) {
            String fullName = ((prenom != null ? prenom : "") + " " + (nom != null ? nom : "")).trim();
            result.put("full_name", fullName);
        }

        return result;
    }

    // ─────────────────────────────────────────────
    // Save employee from mapped data
    // ─────────────────────────────────────────────
    private void saveEmployee(Map<String, String> data) {
        Department department = null;
        if (data.containsKey("department")) {
            department = departmentRepository.findByName(data.get("department"))
                    .orElseGet(() -> departmentRepository.save(
                            Department.builder().name(data.get("department")).build()
                    ));
        }

        Position position = null;
        if (data.containsKey("position")) {
            position = positionRepository.findByTitle(data.get("position"))
                    .orElseGet(() -> positionRepository.save(
                            Position.builder().title(data.get("position")).build()
                    ));
        }

        // Check if employee already exists (upsert by employeeId)
        String empId = data.get("employee_id");
        Employee employee = (empId != null)
                ? employeeRepository.findByEmployeeId(empId).orElse(new Employee())
                : new Employee();

        employee.setEmployeeId(empId);
        employee.setFullName(data.get("full_name"));
        employee.setNationalId(data.get("national_id"));
        employee.setEmail(data.get("email"));
        employee.setPhone(data.get("phone"));
        employee.setHireDate(parseDate(data.get("hire_date")));
        employee.setDepartment(department);
        employee.setPosition(position);
        if (employee.getStatus() == null) employee.setStatus(Employee.EmployeeStatus.ACTIVE);

        employee = employeeRepository.save(employee);

        // Save salary structure
        if (data.containsKey("gross_salary") || data.containsKey("base_salary")) {
            String salaryStr = data.getOrDefault("gross_salary", data.get("base_salary"));
            if (salaryStr != null) {
                final Employee savedEmployee = employee;
                salaryStructureRepository.findByEmployeeIdAndIsCurrentTrue(savedEmployee.getId())
                        .ifPresent(s -> { s.setIsCurrent(false); salaryStructureRepository.save(s); });

                SalaryStructure salary = SalaryStructure.builder()
                        .employee(savedEmployee)
                        .baseSalary(parseBigDecimal(salaryStr))
                        .effectiveDate(LocalDate.now())
                        .isCurrent(true)
                        .build();
                salaryStructureRepository.save(salary);
            }
        }

        // Save family situation
        if (data.containsKey("marital_status") || data.containsKey("number_of_children")) {
            final Employee savedEmployee = employee;
            FamilySituation family = familySituationRepository
                    .findByEmployeeId(savedEmployee.getId())
                    .orElse(FamilySituation.builder().employee(savedEmployee).build());

            if (data.containsKey("marital_status")) {
                family.setMaritalStatus(parseMaritalStatus(data.get("marital_status")));
            }
            if (data.containsKey("number_of_children")) {
                try { family.setNumberOfChildren(Integer.parseInt(data.get("number_of_children"))); }
                catch (NumberFormatException ignored) {}
            }
            if (family.getMaritalStatus() == null)
                family.setMaritalStatus(FamilySituation.MaritalStatus.SINGLE);
            familySituationRepository.save(family);
        }
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private String clean(String value) {
        if (value == null) return "";
        return value.trim().replace("\uFEFF", ""); // also strip BOM
    }

    private List<String> extractHeaders(MultipartFile file) throws Exception {
        try (CSVReader reader = buildReader(file)) {
            String[] headers = reader.readNext();
            return headers != null
                    ? Arrays.stream(headers).map(this::clean).toList()
                    : List.of();
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

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        for (DateTimeFormatter fmt : List.of(
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
        )) {
            try { return LocalDate.parse(value, fmt); } catch (Exception ignored) {}
        }
        return null;
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null) return BigDecimal.ZERO;
        try { return new BigDecimal(value.replaceAll("[^\\d.]", "")); }
        catch (Exception e) { return BigDecimal.ZERO; }
    }

    private FamilySituation.MaritalStatus parseMaritalStatus(String value) {
        if (value == null) return FamilySituation.MaritalStatus.SINGLE;
        return switch (value.trim().toUpperCase()
                .replace("É", "E").replace("È", "E").replace("Ê", "E")) {
            case "MARIE", "MARIEE", "MARRIED"   -> FamilySituation.MaritalStatus.MARRIED;
            case "DIVORCE", "DIVORCEE"           -> FamilySituation.MaritalStatus.DIVORCED;
            case "VEUF", "VEUVE", "WIDOWED"      -> FamilySituation.MaritalStatus.WIDOWED;
            default                              -> FamilySituation.MaritalStatus.SINGLE;
        };
    }
}
