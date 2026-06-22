package com.same.payroll.controller;
import com.same.payroll.dto.EmployeeFilterDto;
import com.same.payroll.dto.EmployeeResponseDto;
import com.same.payroll.service.EmployeeFilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmployeeController {

    private final EmployeeFilterService employeeFilterService;

    @GetMapping
    public ResponseEntity<Page<EmployeeResponseDto>> getEmployees(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String nationalId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hireDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hireDateTo,
            @RequestParam(required = false) BigDecimal salaryMin,
            @RequestParam(required = false) BigDecimal salaryMax,
            @RequestParam(defaultValue = "fullName") String sortBy,
            @RequestParam(defaultValue = "asc")      String order,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        EmployeeFilterDto filter = new EmployeeFilterDto();
        filter.setName(name);
        filter.setNationalId(nationalId);
        filter.setEmail(email);
        filter.setDepartment(department);
        filter.setPosition(position);
        filter.setHireDateFrom(hireDateFrom);
        filter.setHireDateTo(hireDateTo);
        filter.setSalaryMin(salaryMin);
        filter.setSalaryMax(salaryMax);
        filter.setSortBy(sortBy);
        filter.setOrder(order);
        filter.setPage(page);
        filter.setSize(size);

        if (status != null) {
            try {
                filter.setStatus(com.same.payroll.entity.Employee.EmployeeStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }

        return ResponseEntity.ok(employeeFilterService.filter(filter));
    }
}