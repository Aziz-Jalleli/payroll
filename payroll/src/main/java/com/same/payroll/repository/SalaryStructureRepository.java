package com.same.payroll.repository;
import com.same.payroll.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, Long> {
    Optional<SalaryStructure> findByEmployeeIdAndIsCurrentTrue(Long employeeId);
    List<SalaryStructure> findByEmployeeIdOrderByEffectiveDateDesc(Long employeeId);
}