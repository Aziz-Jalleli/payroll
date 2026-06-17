package com.same.payroll.repository;
import com.same.payroll.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
interface AllowanceRepository extends JpaRepository<Allowance, Long> {
    List<Allowance> findByEmployeeId(Long employeeId);
    List<Allowance> findByEmployeeIdAndIsRecurringTrue(Long employeeId);
}
