package com.same.payroll.repository;

import com.same.payroll.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmployeeId(String employeeId);
    Optional<Employee> findByNationalId(String nationalId);
    Optional<Employee> findByEmail(String email);
    List<Employee> findByStatus(Employee.EmployeeStatus status);
    List<Employee> findByDepartmentId(Long departmentId);
}

