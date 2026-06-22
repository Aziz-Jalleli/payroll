package com.same.payroll.dto;

import com.same.payroll.entity.Employee;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponseDto {

    private Long id;
    private String employeeId;
    private String fullName;
    private String nationalId;
    private String email;
    private String phone;
    private LocalDate hireDate;
    private String department;
    private String position;
    private Employee.EmployeeStatus status;
    private BigDecimal baseSalary;

    public static EmployeeResponseDto from(Employee e, BigDecimal salary) {
        return EmployeeResponseDto.builder()
                .id(e.getId())
                .employeeId(e.getEmployeeId())
                .fullName(e.getFullName())
                .nationalId(e.getNationalId())
                .email(e.getEmail())
                .phone(e.getPhone())
                .hireDate(e.getHireDate())
                .department(e.getDepartment() != null ? e.getDepartment().getName() : null)
                .position(e.getPosition() != null ? e.getPosition().getTitle() : null)
                .status(e.getStatus())
                .baseSalary(salary)
                .build();
    }
}