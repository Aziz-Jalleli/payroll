package com.same.payroll.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

public class CsvMappingDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MappingRequest {
        private List<String> headers;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnMapping {
        private String mappedTo;
        private double confidence;
        private String status; // AUTO_MAPPED, NEEDS_CONFIRMATION, UNMAPPED
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MappingResponse {
        private Map<String, ColumnMapping> mappings;
        private List<String> unmappedColumns;
        private List<String> missingRequiredFields;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfirmedMapping {
        // key = CSV column name, value = schema field name
        private Map<String, String> confirmedMappings;
        private String csvContent; // base64 encoded original CSV
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportResult {
        private int totalRows;
        private int successCount;
        private int errorCount;
        private List<String> errors;
    }
}