package com.ecommerce.sellerbackend.controller;

import com.ecommerce.sellerbackend.dto.BankEditApprovalRequest;
import com.ecommerce.sellerbackend.dto.BankEditRequest;
import com.ecommerce.sellerbackend.dto.BankEditResponse;
import com.ecommerce.sellerbackend.entity.SellerPayoutRequest;
import com.ecommerce.sellerbackend.service.SellerFinancialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payout")
@RequiredArgsConstructor
public class PayoutController {

    public static final String SELLER_ID_HEADER = "X-Seller-Id";

    private final SellerFinancialService sellerFinancialService;

    @GetMapping("/summary")
    public Map<String, Object> summary(@RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        return sellerFinancialService.getPayoutSummary(requireSellerId(sellerId));
    }

    @GetMapping("/bank-details")
    public Map<String, Object> bankDetails(@RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        return sellerFinancialService.getBankDetails(requireSellerId(sellerId));
    }

    @PutMapping("/bank-details")
    public Map<String, Object> updateBankDetails(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @RequestBody Map<String, String> body) {
        return sellerFinancialService.updateBankDetails(requireSellerId(sellerId), body);
    }

    @PostMapping("/bank-details/confirm")
    public Map<String, String> confirmBankDetails(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.get("note") : null;
        return Map.of("message", sellerFinancialService.confirmBankDetails(requireSellerId(sellerId), note));
    }

    @PostMapping("/requests")
    public Map<String, Object> submitRequest(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @RequestBody Map<String, String> body) {
        SellerPayoutRequest row = sellerFinancialService.submitPayoutRequest(
                requireSellerId(sellerId),
                body.get("orderId"),
                body.get("sellerNote"));
        return toPayoutRow(row);
    }

    @GetMapping("/requests")
    public List<Map<String, Object>> listRequests(@RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        return sellerFinancialService.listPayoutRequests(requireSellerId(sellerId)).stream()
                .map(this::toPayoutRow)
                .toList();
    }

    @GetMapping(value = "/requests/export", produces = "text/csv")
    public String exportRequests(@RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        return sellerFinancialService.exportPayoutRequestsCsv(requireSellerId(sellerId));
    }

    @PostMapping("/bank-edit-requests")
    public BankEditResponse submitBankEditRequest(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @Valid @RequestBody BankEditRequest request) {
        return sellerFinancialService.submitBankEditRequest(requireSellerId(sellerId), request);
    }

    @GetMapping("/bank-edit-requests")
    public List<BankEditResponse> listBankEditRequests(@RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        return sellerFinancialService.listBankEditRequests(requireSellerId(sellerId));
    }

    @PostMapping("/bank-edit-requests/{id}/approve")
    public BankEditResponse approveBankEditRequest(
            @PathVariable Long id,
            @Valid @RequestBody BankEditApprovalRequest approvalRequest,
            @RequestHeader(SELLER_ID_HEADER) Long adminId) {
        return sellerFinancialService.approveBankEditRequest(id, approvalRequest, adminId);
    }

    private Map<String, Object> toPayoutRow(SellerPayoutRequest row) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", row.getId());
        out.put("sellerId", row.getSellerId());
        out.put("orderId", row.getOrderId());
        out.put("requestedAmount", row.getRequestedAmount());
        out.put("status", row.getStatus());
        out.put("sellerNote", row.getSellerNote());
        out.put("adminNote", row.getAdminNote());
        out.put("transactionRef", row.getTransactionRef());
        out.put("requestedAt", row.getRequestedAt());
        out.put("reviewedAt", row.getReviewedAt());
        out.put("paidAt", row.getPaidAt());
        out.put("reviewedByAdminId", row.getReviewedByAdminId());
        out.put("updatedAt", row.getUpdatedAt());
        return out;
    }

    private Long requireSellerId(Long sellerId) {
        if (sellerId == null) {
            throw new IllegalArgumentException("Missing X-Seller-Id");
        }
        return sellerId;
    }
}
