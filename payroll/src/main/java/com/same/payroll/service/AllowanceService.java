package com.same.payroll.service;

import com.same.payroll.dto.AllowanceDto;
import com.same.payroll.dto.UpsertAllowanceRequest;
import com.same.payroll.entity.Allowance;
import com.same.payroll.entity.Employee;
import com.same.payroll.repository.AllowanceRepository;
import com.same.payroll.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for the Allowances module.
 *
 * Auth notes (mirrors AttendanceService convention):
 *   - Write operations are pre-checked by @RequirePermission on the controller
 *     via PermissionAspect — no second check here.
 *   - Only ADMIN and HR_MANAGER hold ALLOWANCE:* permissions.
 *     EMPLOYEE_VIEWER has none and will receive 403 before reaching this layer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AllowanceService {

    private final AllowanceRepository allowanceRepository;
    private final EmployeeRepository  employeeRepository;
    private final PermissionService   permissionService;

    // ─── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AllowanceDto> listAll() {
        return allowanceRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AllowanceDto> listByEmployee(Long employeeId) {
        return allowanceRepository.findByEmployeeId(employeeId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AllowanceDto getById(Long id) {
        Allowance a = findOrThrow(id);
        return toDto(a);
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    @Transactional
    public AllowanceDto create(UpsertAllowanceRequest req) {
        Employee employee = employeeRepository.findById(req.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Employé introuvable : " + req.getEmployeeId()));

        Allowance allowance = Allowance.builder()
                .employee(employee)
                .type(req.getType())
                .amount(req.getAmount())
                .isRecurring(req.getIsRecurring() != null ? req.getIsRecurring() : true)
                .isTaxable(req.getIsTaxable()    != null ? req.getIsTaxable()   : true)
                .build();

        allowance = allowanceRepository.save(allowance);
        log.info("Created allowance id={} type={} for employee={} by user {}",
                allowance.getId(), allowance.getType(), employee.getId(),
                permissionService.getCurrentPrincipal().getEmail());
        return toDto(allowance);
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    @Transactional
    public AllowanceDto update(Long id, UpsertAllowanceRequest req) {
        Allowance allowance = findOrThrow(id);

        // Employee can be re-assigned on update
        if (!allowance.getEmployee().getId().equals(req.getEmployeeId())) {
            Employee newEmployee = employeeRepository.findById(req.getEmployeeId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Employé introuvable : " + req.getEmployeeId()));
            allowance.setEmployee(newEmployee);
        }

        allowance.setType(req.getType());
        allowance.setAmount(req.getAmount());
        if (req.getIsRecurring() != null) allowance.setIsRecurring(req.getIsRecurring());
        if (req.getIsTaxable()   != null) allowance.setIsTaxable(req.getIsTaxable());

        allowance = allowanceRepository.save(allowance);
        log.info("Updated allowance id={} by user {}",
                id, permissionService.getCurrentPrincipal().getEmail());
        return toDto(allowance);
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long id) {
        Allowance allowance = findOrThrow(id);
        allowanceRepository.delete(allowance);
        log.info("Deleted allowance id={} by user {}",
                id, permissionService.getCurrentPrincipal().getEmail());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Allowance findOrThrow(Long id) {
        return allowanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Allocation introuvable : " + id));
    }

    private AllowanceDto toDto(Allowance a) {
        Employee emp = a.getEmployee();
        return AllowanceDto.builder()
                .id(a.getId())
                .employeeId(emp.getId())
                .employeeFullName(emp.getFullName())
                .employeeCode(emp.getEmployeeId())
                .type(a.getType())
                .amount(a.getAmount())
                .isRecurring(a.getIsRecurring())
                .isTaxable(a.getIsTaxable())
                .build();
    }
}