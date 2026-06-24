package com.same.payroll.controller;


import com.same.payroll.dto.PayrollCalculationDto;
import com.same.payroll.service.PayrollCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")

public class PayrollController {

    private final PayrollCalculationService payrollCalculationService;

    // Calculate salary for one employee
    // GET /api/payroll/calculate/1?year=2024&month=6
    @GetMapping("/calculate/{employeeId}")
    public ResponseEntity<PayrollCalculationDto> calculateOne(
            @PathVariable Long employeeId,
            @RequestParam Integer year,
            @RequestParam Integer month
    ) {
        try {
            return ResponseEntity.ok(payrollCalculationService.calculate(employeeId, year, month));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Calculate salary for ALL employees in a month
    // GET /api/payroll/calculate?year=2024&month=6
    @GetMapping("/calculate")
    public ResponseEntity<List<PayrollCalculationDto>> calculateAll(
            @RequestParam Integer year,
            @RequestParam Integer month
    ) {
        return ResponseEntity.ok(payrollCalculationService.calculateAll(year, month));
    }
}