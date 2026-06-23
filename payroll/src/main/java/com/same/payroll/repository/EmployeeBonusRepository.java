package com.same.payroll.repository;


import com.same.payroll.entity.EmployeeBonus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EmployeeBonusRepository extends JpaRepository<EmployeeBonus, Long> {
    List<EmployeeBonus> findByEmployeeId(Long employeeId);
    List<EmployeeBonus> findByYearAndMonth(Integer year, Integer month);
}