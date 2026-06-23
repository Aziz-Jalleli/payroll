package com.same.payroll.repository;

import com.same.payroll.entity.Overtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OvertimeRepository extends JpaRepository<Overtime, Long> {
    List<Overtime> findByEmployeeIdAndYearAndMonth(Long employeeId, Integer year, Integer month);

    @Query("SELECT COALESCE(SUM(o.hours * o.rateMultiplier), 0) FROM Overtime o " +
            "WHERE o.employee.id = :employeeId AND o.year = :year AND o.month = :month")
    BigDecimal sumWeightedOvertimeHours(Long employeeId, Integer year, Integer month);
}
