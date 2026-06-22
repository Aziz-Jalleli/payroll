package com.same.payroll.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
@Entity
@Table(name = "legal_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String key;

    @Column(nullable = false)
    private String value;
    private String unit;

    @Column(length = 500)
    private String description;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;
}