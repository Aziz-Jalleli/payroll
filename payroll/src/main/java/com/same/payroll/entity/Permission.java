package com.same.payroll.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "permissions", uniqueConstraints = @UniqueConstraint(columnNames = {"resource", "action"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** e.g. "EMPLOYEE", "PAYROLL", "DEPARTMENT" */
    @Column(nullable = false)
    private String resource;

    /** e.g. "READ", "CREATE", "UPDATE", "DELETE", "APPROVE" */
    @Column(nullable = false)
    private String action;

    private String description;

    @ManyToMany(mappedBy = "permissions")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Role> roles = new HashSet<>();

    /** Authority string used by Spring Security, e.g. "EMPLOYEE:READ" */
    @Transient
    public String getAuthority() {
        return resource.toUpperCase() + ":" + action.toUpperCase();
    }
}