package com.same.payroll.controller;

import com.same.payroll.annotation.RequirePermission;
import com.same.payroll.dto.AllowanceDto;
import com.same.payroll.dto.UpsertAllowanceRequest;
import com.same.payroll.service.AllowanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for the Allowances module.
 *
 * Auth strategy (mirrors AttendanceController):
 *   - All operations require ALLOWANCE:* — enforced by PermissionAspect via
 *     @RequirePermission. Only ADMIN and HR_MANAGER hold these permissions.
 *   - EMPLOYEE_VIEWER receives 403 before reaching the service layer.
 *   - CORS handled centrally in SecurityConfig — no @CrossOrigin here.
 */
@RestController
@RequestMapping("/api/allowances")
@RequiredArgsConstructor
public class AllowanceController {

    private final AllowanceService allowanceService;

    /** List all allowances across all employees. Requires ALLOWANCE:READ. */
    @GetMapping
    @RequirePermission(resource = "ALLOWANCE", action = "READ")
    public List<AllowanceDto> listAll() {
        return allowanceService.listAll();
    }

    /** List allowances for a specific employee. Requires ALLOWANCE:READ. */
    @GetMapping("/employee/{employeeId}")
    @RequirePermission(resource = "ALLOWANCE", action = "READ")
    public List<AllowanceDto> listByEmployee(@PathVariable Long employeeId) {
        return allowanceService.listByEmployee(employeeId);
    }

    /** Get a single allowance by ID. Requires ALLOWANCE:READ. */
    @GetMapping("/{id}")
    @RequirePermission(resource = "ALLOWANCE", action = "READ")
    public AllowanceDto getById(@PathVariable Long id) {
        return allowanceService.getById(id);
    }

    /** Create a new allowance. Requires ALLOWANCE:CREATE. */
    @PostMapping
    @RequirePermission(resource = "ALLOWANCE", action = "CREATE")
    public ResponseEntity<AllowanceDto> create(@Valid @RequestBody UpsertAllowanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(allowanceService.create(request));
    }

    /** Update an existing allowance. Requires ALLOWANCE:UPDATE. */
    @PutMapping("/{id}")
    @RequirePermission(resource = "ALLOWANCE", action = "UPDATE")
    public AllowanceDto update(@PathVariable Long id,
                               @Valid @RequestBody UpsertAllowanceRequest request) {
        return allowanceService.update(id, request);
    }

    /** Delete an allowance. Requires ALLOWANCE:DELETE. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(resource = "ALLOWANCE", action = "DELETE")
    public void delete(@PathVariable Long id) {
        allowanceService.delete(id);
    }
}