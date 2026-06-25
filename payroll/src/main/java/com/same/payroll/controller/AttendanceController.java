package com.same.payroll.controller;

import com.same.payroll.annotation.RequirePermission;
import com.same.payroll.dto.AttendanceRecordDto;
import com.same.payroll.dto.MonthSummaryDto;
import com.same.payroll.dto.UpsertAttendanceRequest;
import com.same.payroll.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for the Pointage (attendance) module.
 *
 * Auth strategy mirrors the rest of the app:
 *   - The @RequirePermission aspect (PermissionAspect) handles ADMIN/HR_MANAGER
 *     gating on write operations — it reads the current principal from
 *     SecurityContextHolder, exactly like PermissionService.getCurrentPrincipal().
 *   - Read endpoints are open to any authenticated user; the service layer
 *     narrows EMPLOYEE_VIEWER to their own row via PermissionService.
 *   - No @CrossOrigin here — CORS is centrally handled in SecurityConfig
 *     (CorsConfigurationSource bean), so repeating it per-controller is
 *     redundant and can cause header duplication.
 */
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    /**
     * Returns 12 MonthSummaryDtos for the calendar overview grid.
     * Any authenticated user may call this (EMPLOYEE_VIEWER included).
     */
    @GetMapping("/summary/{year}")
    public List<MonthSummaryDto> getYearSummary(@PathVariable Integer year) {
        return attendanceService.getYearSummary(year);
    }

    /**
     * Returns one row per active employee for the given month.
     * The service narrows EMPLOYEE_VIEWER to only their own row.
     * Filters (name / email / department) are ignored for EMPLOYEE_VIEWER.
     */
    @GetMapping("/{year}/{month}")
    public List<AttendanceRecordDto> getMonthDetail(
            @PathVariable Integer year,
            @PathVariable Integer month,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String department) {
        return attendanceService.getMonthDetail(year, month, name, email, department);
    }

    /**
     * Create or update a single employee attendance record.
     * Requires ATTENDANCE:WRITE permission — enforced by PermissionAspect.
     */
    @PostMapping
    @RequirePermission(resource = "ATTENDANCE", action = "WRITE")
    public ResponseEntity<AttendanceRecordDto> upsert(
            @Valid @RequestBody UpsertAttendanceRequest request) {
        return ResponseEntity.ok(attendanceService.upsert(request));
    }

    /**
     * Delete a single employee's record for a given month.
     * Requires ATTENDANCE:DELETE permission — enforced by PermissionAspect.
     */
    @DeleteMapping("/{employeeId}/{year}/{month}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(resource = "ATTENDANCE", action = "DELETE")
    public void delete(
            @PathVariable Long employeeId,
            @PathVariable Integer year,
            @PathVariable Integer month) {
        attendanceService.delete(employeeId, year, month);
    }
}