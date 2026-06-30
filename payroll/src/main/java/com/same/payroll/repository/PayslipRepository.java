package com.same.payroll.repository;

import com.same.payroll.entity.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayslipRepository extends JpaRepository<Payslip, Long> {

    Optional<Payslip> findByEmployeeIdAndYearAndMonth(Long employeeId, Integer year, Integer month);

    List<Payslip> findByYearAndMonth(Integer year, Integer month);

    @Query("SELECT p FROM Payslip p WHERE p.year = :year AND p.month = :month")
    List<Payslip> findAllForPeriod(@Param("year") Integer year, @Param("month") Integer month);
}