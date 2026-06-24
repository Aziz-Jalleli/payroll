package com.same.payroll.controller;
import com.same.payroll.entity.IrppBracket;
import com.same.payroll.repository.IrppBracketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/irpp-brackets")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")

public class IrppBracketController {

    private final IrppBracketRepository irppBracketRepository;
    @GetMapping
    public List<IrppBracket> getAll() {
        return irppBracketRepository.findAllByOrderByFiscalYearDescBracketOrderAsc();
    }
    @GetMapping("/year/{year}")
    public List<IrppBracket> getByYear(@PathVariable Integer year) {
        return irppBracketRepository.findByFiscalYearOrderByBracketOrderAsc(year);
    }
    @GetMapping("/{id}")
    public ResponseEntity<IrppBracket> getById(@PathVariable Long id) {
        return irppBracketRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @PostMapping
    public ResponseEntity<IrppBracket> create(@RequestBody IrppBracket bracket) {
        return ResponseEntity.ok(irppBracketRepository.save(bracket));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<IrppBracket>> createBulk(@RequestBody List<IrppBracket> brackets) {
        return ResponseEntity.ok(irppBracketRepository.saveAll(brackets));
    }
    @PutMapping("/{id}")
    public ResponseEntity<IrppBracket> update(@PathVariable Long id, @RequestBody IrppBracket updated) {
        return irppBracketRepository.findById(id).map(bracket -> {
            bracket.setMinIncome(updated.getMinIncome());
            bracket.setMaxIncome(updated.getMaxIncome());
            bracket.setRate(updated.getRate());
            bracket.setFiscalYear(updated.getFiscalYear());
            bracket.setEffectiveDate(updated.getEffectiveDate());
            bracket.setBracketOrder(updated.getBracketOrder());
            return ResponseEntity.ok(irppBracketRepository.save(bracket));
        }).orElse(ResponseEntity.notFound().build());
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!irppBracketRepository.existsById(id)) return ResponseEntity.notFound().build();
        irppBracketRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/year/{year}")
    public ResponseEntity<Void> deleteByYear(@PathVariable Integer year) {
        List<IrppBracket> brackets = irppBracketRepository.findByFiscalYearOrderByBracketOrderAsc(year);
        irppBracketRepository.deleteAll(brackets);
        return ResponseEntity.noContent().build();
    }
}