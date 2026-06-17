package com.same.payroll.repository;
import com.same.payroll.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface FamilySituationRepository extends JpaRepository<FamilySituation, Long> {
    Optional<FamilySituation> findByEmployeeId(Long employeeId);
}