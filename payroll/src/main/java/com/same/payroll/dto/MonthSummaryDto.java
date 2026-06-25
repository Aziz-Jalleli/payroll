package com.same.payroll.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Summary info for a single year/month card on the Pointage overview page.
 */
@Data
@Builder
public class MonthSummaryDto {
    private Integer year;
    private Integer month;
    /** Total number of active employees for this period. */
    private int totalEmployees;
    /** How many employees have at least one field filled. */
    private int filledCount;
    /**
     * VIDE   – no records at all
     * EN_COURS – some employees filled, not all
     * COMPLET  – every employee has a record
     * CLOTURE  – month has been administratively closed (future feature placeholder)
     */
    private String status;
    private boolean currentMonth;
}