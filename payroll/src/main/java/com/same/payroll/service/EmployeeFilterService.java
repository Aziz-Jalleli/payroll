package com.same.payroll.service;

import com.same.payroll.dto.EmployeeFilterDto;
import com.same.payroll.dto.EmployeeResponseDto;
import com.same.payroll.entity.Employee;
import com.same.payroll.entity.SalaryStructure;
import com.same.payroll.repository.SalaryStructureRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class EmployeeFilterService {

    private final EntityManager em;
    private final SalaryStructureRepository salaryStructureRepository;

    public Page<EmployeeResponseDto> filter(EmployeeFilterDto f) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        // ── Count query ──────────────────────────────────────────
        CriteriaQuery<Long> countQ = cb.createQuery(Long.class);
        Root<Employee> countRoot = countQ.from(Employee.class);
        countQ.select(cb.count(countRoot))
                .where(buildPredicates(f, cb, countRoot).toArray(new Predicate[0]));
        long total = em.createQuery(countQ).getSingleResult();

        // ── Data query ───────────────────────────────────────────
        CriteriaQuery<Employee> dataQ = cb.createQuery(Employee.class);
        Root<Employee> root = dataQ.from(Employee.class);

        // Fetch joins to avoid N+1
        root.fetch("department", JoinType.LEFT);
        root.fetch("position",   JoinType.LEFT);

        dataQ.where(buildPredicates(f, cb, root).toArray(new Predicate[0]));
        dataQ.orderBy(buildOrder(f, cb, root));

        TypedQuery<Employee> query = em.createQuery(dataQ);
        query.setFirstResult(f.getPage() * f.getSize());
        query.setMaxResults(f.getSize());

        List<Employee> employees = query.getResultList();

        // Map to response DTOs with current salary
        List<EmployeeResponseDto> dtos = employees.stream().map(e -> {
            var salary = salaryStructureRepository
                    .findByEmployeeIdAndIsCurrentTrue(e.getId())
                    .map(SalaryStructure::getBaseSalary)
                    .orElse(null);
            return EmployeeResponseDto.from(e, salary);
        }).toList();

        return new PageImpl<>(dtos, PageRequest.of(f.getPage(), f.getSize()), total);
    }

    private List<Predicate> buildPredicates(EmployeeFilterDto f, CriteriaBuilder cb, Root<Employee> root) {
        List<Predicate> predicates = new ArrayList<>();

        // Name search (case-insensitive, partial match)
        if (f.getName() != null && !f.getName().isBlank()) {
            predicates.add(cb.like(
                    cb.lower(root.get("fullName")),
                    "%" + f.getName().toLowerCase() + "%"
            ));
        }

        // National ID search
        if (f.getNationalId() != null && !f.getNationalId().isBlank()) {
            predicates.add(cb.like(root.get("nationalId"), "%" + f.getNationalId() + "%"));
        }

        // Email search
        if (f.getEmail() != null && !f.getEmail().isBlank()) {
            predicates.add(cb.like(
                    cb.lower(root.get("email")),
                    "%" + f.getEmail().toLowerCase() + "%"
            ));
        }

        // Department filter
        if (f.getDepartment() != null && !f.getDepartment().isBlank()) {
            Join<Object, Object> dept = root.join("department", JoinType.LEFT);
            predicates.add(cb.like(
                    cb.lower(dept.get("name")),
                    "%" + f.getDepartment().toLowerCase() + "%"
            ));
        }

        // Position filter
        if (f.getPosition() != null && !f.getPosition().isBlank()) {
            Join<Object, Object> pos = root.join("position", JoinType.LEFT);
            predicates.add(cb.like(
                    cb.lower(pos.get("title")),
                    "%" + f.getPosition().toLowerCase() + "%"
            ));
        }

        // Status filter
        if (f.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), f.getStatus()));
        }

        // Hire date range
        if (f.getHireDateFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("hireDate"), f.getHireDateFrom()));
        }
        if (f.getHireDateTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("hireDate"), f.getHireDateTo()));
        }

        return predicates;
    }

    private Order buildOrder(EmployeeFilterDto f, CriteriaBuilder cb, Root<Employee> root) {
        // Allowed sort fields mapped to entity paths
        Map<String, String> sortableFields = Map.of(
                "fullName",   "fullName",
                "employeeId", "employeeId",
                "hireDate",   "hireDate",
                "status",     "status",
                "email",      "email"
        );

        String field = sortableFields.getOrDefault(f.getSortBy(), "fullName");
        boolean desc  = "desc".equalsIgnoreCase(f.getOrder());

        return desc ? cb.desc(root.get(field)) : cb.asc(root.get(field));
    }
}