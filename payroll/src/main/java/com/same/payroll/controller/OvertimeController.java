package com.same.payroll.controller;

import com.same.payroll.annotation.RequirePermission;
import com.same.payroll.dto.CreateOvertimeRequest;
import com.same.payroll.dto.OvertimeDto;
import com.same.payroll.service.OvertimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Read endpoints are open to any authenticated user (the sum feeds the
 * H.SUPP column a viewer might see on their own row - though in practice
 * the main table is driven by AttendanceService.getMonthDetail, which
 * already row-scopes viewers; this controller has no such row-scoping of
 * its own, so don't expose it directly to a viewer-facing UI without
 * checking employeeId against their own row first if that changes).
 * Write operations mirror AttendanceController's permission model.
 */
@RestController
@RequestMapping("/api/overtime")
@RequiredArgsConstructor
public class OvertimeController {

    private final OvertimeService overtimeService;

    /** Individual entries for the "manage overtime" modal. */
    @GetMapping("/{employeeId}/{year}/{month}")
    public List<OvertimeDto> list(
            @PathVariable Long employeeId,
            @PathVariable Integer year,
            @PathVariable Integer month) {
        return overtimeService.listForEmployeePeriod(employeeId, year, month);
    }

    /** Weighted sum (hours * rateMultiplier) for the read-only H.SUPP table column. */
    @GetMapping("/{employeeId}/{year}/{month}/sum")
    public Map<String, BigDecimal> sum(
            @PathVariable Long employeeId,
            @PathVariable Integer year,
            @PathVariable Integer month) {
        return Map.of("weightedHours", overtimeService.getWeightedSum(employeeId, year, month));
    }

    @PostMapping
    @RequirePermission(resource = "ATTENDANCE", action = "WRITE")
    public OvertimeDto create(@Valid @RequestBody CreateOvertimeRequest request) {
        return overtimeService.create(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(resource = "ATTENDANCE", action = "DELETE")
    public void delete(@PathVariable Long id) {
        overtimeService.delete(id);
    }
}