package com.same.payroll.controller;

import com.same.payroll.entity.CnssRate;
import com.same.payroll.repository.CnssRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RestController
@RequestMapping("/api/cnss-rates")
@RequiredArgsConstructor
public class CnssRateController {

    private final CnssRateRepository cnssRateRepository;

    @GetMapping
    public List<CnssRate> getAll() {
        return cnssRateRepository.findAllByOrderByEffectiveDateDesc();
    }

    // GET current active rate
    @GetMapping("/current")
    public ResponseEntity<CnssRate> getCurrent() {
        return cnssRateRepository.findByIsCurrentTrue()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET by id
    @GetMapping("/{id}")
    public ResponseEntity<CnssRate> getById(@PathVariable Long id) {
        return cnssRateRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST create new rate (marks previous as not current)
    @PostMapping
    public ResponseEntity<CnssRate> create(@RequestBody CnssRate rate) {
        // Deactivate previous current rate
        cnssRateRepository.findByIsCurrentTrue().ifPresent(current -> {
            current.setIsCurrent(false);
            current.setEndDate(rate.getEffectiveDate().minusDays(1));
            cnssRateRepository.save(current);
        });
        rate.setIsCurrent(true);
        return ResponseEntity.ok(cnssRateRepository.save(rate));
    }

    // PUT update
    @PutMapping("/{id}")
    public ResponseEntity<CnssRate> update(@PathVariable Long id, @RequestBody CnssRate updated) {
        return cnssRateRepository.findById(id).map(rate -> {
            rate.setEmployeeRate(updated.getEmployeeRate());
            rate.setEmployerRate(updated.getEmployerRate());
            rate.setEffectiveDate(updated.getEffectiveDate());
            rate.setEndDate(updated.getEndDate());
            rate.setDescription(updated.getDescription());
            return ResponseEntity.ok(cnssRateRepository.save(rate));
        }).orElse(ResponseEntity.notFound().build());
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!cnssRateRepository.existsById(id)) return ResponseEntity.notFound().build();
        cnssRateRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}