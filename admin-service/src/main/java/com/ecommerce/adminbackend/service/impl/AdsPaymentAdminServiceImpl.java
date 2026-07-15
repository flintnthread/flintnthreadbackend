package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.entity.ads.AdsPayment;
import com.ecommerce.adminbackend.repository.AdsPaymentRepository;
import com.ecommerce.adminbackend.service.AdsPaymentAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdsPaymentAdminServiceImpl extends BaseAdminService implements AdsPaymentAdminService {

    private final AdsPaymentRepository repository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> list(String search, String status, int page, int size) {
        var result = repository.search(blankToNull(search), blankToNull(status), PageRequest.of(page, size));
        return PageResponse.from(result.map(this::toMap));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> get(Integer id) {
        return toMap(requireFound(repository.findById(id), "Ads payment not found."));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalPayments", repository.count());
        stats.put("totalSuccessfulAmount", repository.sumSuccessfulAmount());
        return stats;
    }

    private Map<String, Object> toMap(AdsPayment payment) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", payment.getId());
        row.put("orderId", payment.getOrderId());
        row.put("razorpayPaymentId", payment.getRazorpayPaymentId());
        row.put("razorpayOrderId", payment.getRazorpayOrderId());
        row.put("razorpaySignature", payment.getRazorpaySignature());
        row.put("amount", payment.getAmount());
        row.put("currency", payment.getCurrency());
        row.put("status", payment.getStatus());
        row.put("method", payment.getMethod());
        row.put("bank", payment.getBank());
        row.put("wallet", payment.getWallet());
        row.put("vpa", payment.getVpa());
        row.put("email", payment.getEmail());
        row.put("contact", payment.getContact());
        row.put("fee", payment.getFee());
        row.put("tax", payment.getTax());
        row.put("createdAt", payment.getCreatedAt());
        return row;
    }
}
