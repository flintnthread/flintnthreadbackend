package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.logging.LogFactory;
import org.slf4j.Logger;
import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.dto.common.NoteRequest;
import com.ecommerce.adminbackend.service.SellerAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/sellers")
@RequiredArgsConstructor
public class SellerAdminController {

    private static final Logger log = LogFactory.getLogger(SellerAdminController.class);

    private final SellerAdminService sellerAdminService;

    @GetMapping
    public PageResponse<Map<String, Object>> listSellers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return sellerAdminService.listSellers(status, search, page, size);
    }

    @GetMapping("/approved")
    public PageResponse<Map<String, Object>> listAdminApprovedSellers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "500") int size) {
        return sellerAdminService.listAdminApprovedSellers(search, page, size);
    }

    @GetMapping("/approved/location-stats")
    public Map<String, Object> adminApprovedLocationStats() {
        return sellerAdminService.adminApprovedLocationStats();
    }

    @GetMapping("/graph")
    public PageResponse<Map<String, Object>> listSellersForGraph(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return sellerAdminService.listSellersForGraph(search, sellerId, page, size);
    }

    @GetMapping("/graph/names")
    public List<Map<String, Object>> listSellerGraphNames() {
        return sellerAdminService.listSellerGraphNames();
    }

    @GetMapping("/analytics/summary")
    public Map<String, Object> analyticsSummary(
            @RequestParam(required = false) String filterType,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Long sellerId) {
        return sellerAdminService.analyticsSummary(filterType, year, fromDate, toDate, sellerId);
    }

    @GetMapping("/analytics/chart")
    public Map<String, Object> analyticsChart(
            @RequestParam(required = false) String filterType,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Long sellerId) {
        return sellerAdminService.analyticsChart(filterType, year, fromDate, toDate, sellerId);
    }

    @GetMapping("/analytics/insights")
    public List<Map<String, Object>> analyticsInsights(
            @RequestParam(required = false) String filterType,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Long sellerId) {
        return sellerAdminService.analyticsInsights(filterType, year, fromDate, toDate, sellerId);
    }

    @GetMapping("/analytics/years")
    public List<String> analyticsYearOptions() {
        return sellerAdminService.analyticsYearOptions();
    }

    @PostMapping("/{id}/block")
    public Map<String, Object> block(@PathVariable Long id) {
        return sellerAdminService.blockSeller(id);
    }

    @PostMapping("/{id}/unblock")
    public Map<String, Object> unblock(@PathVariable Long id) {
        return sellerAdminService.unblockSeller(id);
    }

    @GetMapping("/bank/pending")
    public PageResponse<Map<String, Object>> pendingBank(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return sellerAdminService.listPendingBank(page, size);
    }

    @GetMapping("/bank/stats")
    public Map<String, Object> bankStats() {
        return sellerAdminService.bankStats();
    }

    @GetMapping("/{id}")
    public Map<String, Object> getSeller(@PathVariable Long id) {
        return sellerAdminService.getSeller(id);
    }

    @GetMapping("/shiprocket")
    public PageResponse<Map<String, Object>> listShiprocketSellers(
            @RequestParam(defaultValue = "pending") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return sellerAdminService.listShiprocketSellers(status, page, size);
    }

    @GetMapping("/bank/verifications")
    public PageResponse<Map<String, Object>> listBankVerifications(
            @RequestParam(defaultValue = "pending") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return sellerAdminService.listBankVerifications(status, page, size);
    }

    @GetMapping("/{id}/bank")
    public Map<String, Object> bankDetails(@PathVariable Long id) {
        return sellerAdminService.getBankDetails(id);
    }

    @PostMapping("/{id}/bank/approve")
    public Map<String, Object> approveBank(@PathVariable Long id, @RequestBody(required = false) NoteRequest request) {
        String note = request != null ? request.getNote() : null;
        return sellerAdminService.approveBank(id, note);
    }

    @PostMapping("/{id}/bank/reject")
    public Map<String, Object> rejectBank(@PathVariable Long id, @RequestBody(required = false) NoteRequest request) {
        String note = request != null ? (request.getNote() != null ? request.getNote() : request.getReason()) : null;
        return sellerAdminService.rejectBank(id, note);
    }
}
