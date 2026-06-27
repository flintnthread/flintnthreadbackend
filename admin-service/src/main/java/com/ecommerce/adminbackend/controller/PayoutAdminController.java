package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.logging.LogFactory;
import org.slf4j.Logger;
import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.dto.common.NoteRequest;
import com.ecommerce.adminbackend.service.PayoutAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/payouts")
@RequiredArgsConstructor
public class PayoutAdminController {

    private static final Logger log = LogFactory.getLogger(PayoutAdminController.class);

    private final PayoutAdminService payoutAdminService;

    @GetMapping("/stats")
    public Map<String, Object> payoutStats() {
        return payoutAdminService.payoutStats();
    }

    @GetMapping
    public PageResponse<Map<String, Object>> listPayouts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return payoutAdminService.listPayouts(status, page, size);
    }

    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    public String exportPayouts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer minReminderDays) {
        return payoutAdminService.exportPayoutsCsv(status, minReminderDays);
    }

    @GetMapping("/{id}")
    public Map<String, Object> getPayout(@PathVariable Long id) {
        return payoutAdminService.getPayoutDetail(id);
    }

    @GetMapping("/{id}/invoice")
    public Map<String, Object> invoice(@PathVariable Long id) {
        return payoutAdminService.generateInvoice(id);
    }

    @PostMapping("/{id}/pay")
    public Map<String, Object> pay(@PathVariable Long id, @RequestBody(required = false) NoteRequest request) {
        String ref = request != null ? request.getTransactionRef() : null;
        String note = request != null ? request.getNote() : null;
        String status = request != null ? request.getStatus() : null;
        return payoutAdminService.markPaid(id, ref, note, status);
    }
}
