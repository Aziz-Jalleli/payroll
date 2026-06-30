package com.same.payroll.controller;

import com.same.payroll.dto.PayslipDto;
import com.same.payroll.service.PayslipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Employee-facing payslip surface. Every method delegates its access
 * check to PayslipService (PAYROLL:GENERATE / PAYROLL:READ_ALL / own-row
 * scoping) rather than using @RequirePermission here, since the rule for
 * viewing is row-scoped ("your own employee row") and can't be expressed
 * by the generic resource:action aspect alone.
 *
 * No @CrossOrigin here - CORS is centrally handled in SecurityConfig.
 */
@RestController
@RequestMapping("/api/payslips")
@RequiredArgsConstructor
public class PayslipController {

    private final PayslipService payslipService;

    /** Admin/HR/payroll-officer only - generates or regenerates one employee's payslip. */
    @PostMapping("/{employeeId}/generate")
    public PayslipDto generate(
            @PathVariable Long employeeId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return payslipService.generate(employeeId, year, month);
    }

    /** Admin/HR/payroll-officer only - generates payslips for every employee with attendance data that period. */
    @PostMapping("/generate-all")
    public List<PayslipDto> generateAll(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestBody List<Long> employeeIds) {
        return employeeIds.stream()
                .map(id -> payslipService.generate(id, year, month))
                .toList();
    }

    @PatchMapping("/{employeeId}/lock")
    public PayslipDto lock(
            @PathVariable Long employeeId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return payslipService.setLocked(employeeId, year, month, true);
    }

    @PatchMapping("/{employeeId}/unlock")
    public PayslipDto unlock(
            @PathVariable Long employeeId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return payslipService.setLocked(employeeId, year, month, false);
    }

    /** Admin/HR/payroll-officer only (PAYROLL:READ_ALL) - status for every employee in a period. */
    @GetMapping
    public List<PayslipDto> listForPeriod(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return payslipService.listForPeriod(year, month);
    }

    /**
     * Status for one employee/period. A viewer may call this for their
     * OWN employeeId only - PayslipService enforces that and throws
     * AccessDeniedException (-> 403, handled globally) otherwise.
     */

    @GetMapping("/{employeeId}/status")
    public ResponseEntity<PayslipDto> getStatus(
            @PathVariable Long employeeId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return payslipService.getStatus(employeeId, year, month)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Downloads the PDF for one employee/period. Same own-row-or-READ_ALL
     * rule as getStatus. Returns 404 if no payslip has been generated yet -
     * a viewer cannot trigger generation by calling this.
     */
    @GetMapping("/{employeeId}/pdf")
    public ResponseEntity<byte[]> getPdf(
            @PathVariable Long employeeId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return payslipService.getPdf(employeeId, year, month)
                .map(pdf -> {
                    String filename = "fiche-paie-" + employeeId + "-" + year + "-"
                            + String.format("%02d", month) + ".pdf";
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_PDF);
                    headers.setContentDisposition(ContentDisposition.inline().filename(filename).build());
                    return ResponseEntity.ok().headers(headers).body(pdf);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    @GetMapping("/me")
    public ResponseEntity<PayslipDto> getMyPayslip(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return payslipService.getMyPayslip(year, month)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}