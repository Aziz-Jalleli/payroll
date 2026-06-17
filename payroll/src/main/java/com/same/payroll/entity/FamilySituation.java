package com.same.payroll.entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "family_situation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilySituation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status", nullable = false)
    private MaritalStatus maritalStatus;

    @Column(name = "number_of_children")
    @Builder.Default
    private Integer numberOfChildren = 0;

    @Column(name = "number_of_dependents")
    @Builder.Default
    private Integer numberOfDependents = 0;

    public enum MaritalStatus {
        SINGLE, MARRIED, DIVORCED, WIDOWED
    }

    // Tunisian tax deduction per dependent (chef de famille + children)
    public int getTotalChargesToDeduct() {
        int charges = 0;
        if (maritalStatus == MaritalStatus.MARRIED) charges += 1;
        charges += numberOfChildren;
        charges += numberOfDependents;
        return charges;
    }
}