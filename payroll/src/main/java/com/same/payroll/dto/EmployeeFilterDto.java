package com.same.payroll.dto;


import com.same.payroll.entity.Employee;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EmployeeFilterDto {

    // Search
    private String name;
    private String nationalId;
    private String email;

    // Filters
    private String department;
    private String position;
    private Employee.EmployeeStatus status;

    // Hire date range
    private LocalDate hireDateFrom;
    private LocalDate hireDateTo;

    // Salary range
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;

    // Sorting
    private String sortBy = "fullName"; // default sort field
    private String order  = "asc";      // asc or desc

    // Pagination
    private int page = 0;
    private int size = 10;
}