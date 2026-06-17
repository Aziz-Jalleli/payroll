package com.same.payroll.service;


import com.opencsv.CSVReader;
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

    // Step 1: Upload CSV and get mapping suggestions from Python service
    public MappingResponse analyzeHeaders(MultipartFile file) throws Exception {
        List<String> headers = extractHeaders(file);

        MappingRequest request = new MappingRequest(headers);
        MappingResponse response = restTemplate.postForObject(
                pythonServiceUrl + "/map-columns",
                request,
                MappingResponse.class
        );

        // Check which required fields are missing
        if (response != null) {
            List<String> mappedFields = response.getMappings().values()
                    .stream()
                    .filter(m -> !"UNMAPPED".equals(m.getStatus()))
                    .map(ColumnMapping::getMappedTo)
                    .toList();

            List<String> missing = REQUIRED_FIELDS.stream()
                    .filter(f -> !mappedFields.contains(f))
                    .toList();

            response.setMissingRequiredFields(missing);
        }

        return response;
    }

    // Step 2: User confirms mappings, then import
    public ImportResult importWithMapping(MultipartFile file, Map<String, String> confirmedMappings) throws Exception {
        List<String[]> rows = extractAllRows(file);
        if (rows.isEmpty()) return new ImportResult(0, 0, 0, List.of("Empty file"));

        String[] headers = rows.get(0);
        List<String[]> dataRows = rows.subList(1, rows.size());

        int success = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < dataRows.size(); i++) {
            try {
                Map<String, String> rowData = mapRowToSchema(headers, dataRows.get(i), confirmedMappings);
                saveEmployee(rowData);
                success++;
            } catch (Exception e) {
                errors.add("Row " + (i + 2) + ": " + e.getMessage());
                log.error("Error importing row {}: {}", i + 2, e.getMessage());
            }
        }

        return new ImportResult(dataRows.size(), success, errors.size(), errors);
    }

    // Map a CSV row using confirmed mappings
    private Map<String, String> mapRowToSchema(String[] headers, String[] values, Map<String, String> mappings) {
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < headers.length && i < values.length; i++) {
            String schemaField = mappings.get(headers[i]);
            if (schemaField != null && !values[i].isBlank()) {
                result.put(schemaField, values[i].trim());
            }
        }
        return result;
    }

    // Save employee from mapped row data
    private void saveEmployee(Map<String, String> data) {
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

        // Build and save employee
        Employee employee = Employee.builder()
                .employeeId(data.get("employee_id"))
                .fullName(data.get("full_name"))
                .nationalId(data.get("national_id"))
                .email(data.get("email"))
                .phone(data.get("phone"))
                .hireDate(parseDate(data.get("hire_date")))
                .department(department)
                .position(position)
                .build();

        employee = employeeRepository.save(employee);

        // Save salary structure if gross salary provided
        if (data.containsKey("gross_salary")) {
            SalaryStructure salary = SalaryStructure.builder()
                    .employee(employee)
                    .baseSalary(new BigDecimal(data.get("gross_salary").replaceAll("[^\\d.]", "")))
                    .effectiveDate(LocalDate.now())
                    .isCurrent(true)
                    .build();
            salaryStructureRepository.save(salary);
        }
    }

    private List<String> extractHeaders(MultipartFile file) throws Exception {
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] headers = reader.readNext();
            return headers != null ? Arrays.asList(headers) : List.of();
        }
    }

    private List<String[]> extractAllRows(MultipartFile file) throws Exception {
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            return reader.readAll();
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        List<DateTimeFormatter> formats = List.of(
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
        );
        for (DateTimeFormatter fmt : formats) {
            try { return LocalDate.parse(value, fmt); } catch (Exception ignored) {}
        }
        return null;
    }
}