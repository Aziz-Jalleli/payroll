package com.same.payroll.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.same.payroll.dto.CsvMappingDtos.*;
import com.same.payroll.service.CsvImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@lombok.extern.slf4j.Slf4j
public class CsvImportController {

    private final CsvImportService csvImportService;
    private final ObjectMapper objectMapper;

    // Step 1: Upload CSV → get mapping suggestions (including ambiguous ones)
    @PostMapping("/analyze")
    public ResponseEntity<MappingResponse> analyzeHeaders(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(csvImportService.analyzeHeaders(file));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Step 2: Confirm mappings → import
    // Params:
    //   file     - the CSV file
    //   mappings - JSON string: {"NOM":"last_name","PRENOM":"first_name",...}
    //   year     - payroll year  (e.g. 2024) - optional, needed for attendance
    //   month    - payroll month (e.g. 6)    - optional, needed for attendance
    @PostMapping("/confirm")
    public ResponseEntity<ImportResult> confirmAndImport(
            @RequestParam("file")              MultipartFile file,
            @RequestParam("mappings")          String mappingsJson,
            @RequestParam(value = "year",  required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month
    ) {
        try {
            Map<String, String> confirmedMappings = objectMapper.readValue(
                    mappingsJson, new TypeReference<Map<String, String>>() {}
            );
            ImportResult result = csvImportService.importWithMapping(file, confirmedMappings, year, month);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // log.error("Import error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}