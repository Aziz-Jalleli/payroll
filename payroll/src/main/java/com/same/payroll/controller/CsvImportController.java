package com.same.payroll.controller;

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
@CrossOrigin(origins = "*")
public class CsvImportController {

    private final CsvImportService csvImportService;

    @PostMapping("/analyze")
    public ResponseEntity<MappingResponse> analyzeHeaders(@RequestParam("file") MultipartFile file) {
        try {
            MappingResponse response = csvImportService.analyzeHeaders(file);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<ImportResult> confirmAndImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam Map<String, String> confirmedMappings
    ) {
        try {
            confirmedMappings.remove("file");
            ImportResult result = csvImportService.importWithMapping(file, confirmedMappings);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}