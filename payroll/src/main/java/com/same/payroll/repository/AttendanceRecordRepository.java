package com.same.payroll.repository;


import com.same.payroll.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    Optional<AttendanceRecord> findByEmployeeIdAndYearAndMonth(Long employeeId, Integer year, Integer month);
    List<AttendanceRecord> findByEmployeeId(Long employeeId);
    List<AttendanceRecord> findByYearAndMonth(Integer year, Integer month);
}