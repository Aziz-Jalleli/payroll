package com.same.payroll.controller;


import com.same.payroll.entity.LegalConfig;
import com.same.payroll.repository.LegalConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/legal-config")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LegalConfigController {

    private final LegalConfigRepository legalConfigRepository;

    // GET all configs
    @GetMapping
    public List<LegalConfig> getAll() {
        return legalConfigRepository.findAllByOrderByKeyAsc();
    }

    // GET by key (e.g. /api/legal-config/key/SMIG)
    @GetMapping("/key/{key}")
    public ResponseEntity<LegalConfig> getByKey(@PathVariable String key) {
        return legalConfigRepository.findByKey(key)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET by id
    @GetMapping("/{id}")
    public ResponseEntity<LegalConfig> getById(@PathVariable Long id) {
        return legalConfigRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST create
    @PostMapping
    public ResponseEntity<?> create(@RequestBody LegalConfig config) {
        if (legalConfigRepository.findByKey(config.getKey()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body("Key '" + config.getKey() + "' already exists. Use PUT to update.");
        }
        return ResponseEntity.ok(legalConfigRepository.save(config));
    }

    // PUT update by key
    @PutMapping("/key/{key}")
    public ResponseEntity<LegalConfig> updateByKey(@PathVariable String key, @RequestBody LegalConfig updated) {
        return legalConfigRepository.findByKey(key).map(config -> {
            config.setValue(updated.getValue());
            config.setUnit(updated.getUnit());
            config.setDescription(updated.getDescription());
            config.setEffectiveDate(updated.getEffectiveDate());
            return ResponseEntity.ok(legalConfigRepository.save(config));
        }).orElse(ResponseEntity.notFound().build());
    }

    // PUT update by id
    @PutMapping("/{id}")
    public ResponseEntity<LegalConfig> updateById(@PathVariable Long id, @RequestBody LegalConfig updated) {
        return legalConfigRepository.findById(id).map(config -> {
            config.setKey(updated.getKey());
            config.setValue(updated.getValue());
            config.setUnit(updated.getUnit());
            config.setDescription(updated.getDescription());
            config.setEffectiveDate(updated.getEffectiveDate());
            return ResponseEntity.ok(legalConfigRepository.save(config));
        }).orElse(ResponseEntity.notFound().build());
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!legalConfigRepository.existsById(id)) return ResponseEntity.notFound().build();
        legalConfigRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}