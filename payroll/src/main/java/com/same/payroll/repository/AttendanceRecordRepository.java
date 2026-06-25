package com.same.payroll.repository;


import com.same.payroll.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    Optional<AttendanceRecord> findByEmployeeIdAndYearAndMonth(
            Long employeeId, Integer year, Integer month);

    List<AttendanceRecord> findByEmployeeId(Long employeeId);

    List<AttendanceRecord> findByYearAndMonth(Integer year, Integer month);

    /** Count distinct employees that have a record for this period. */
    @Query("SELECT COUNT(a) FROM AttendanceRecord a WHERE a.year = :year AND a.month = :month")
    long countByYearAndMonth(@Param("year") Integer year, @Param("month") Integer month);

    void deleteByEmployeeIdAndYearAndMonth(Long employeeId, Integer year, Integer month);

}