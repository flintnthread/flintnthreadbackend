package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.entity.ads.AdsOrder;
import com.ecommerce.adminbackend.entity.ads.AdsUser;
import com.ecommerce.adminbackend.repository.AdsOrderRepository;
import com.ecommerce.adminbackend.repository.AdsUserRepository;
import com.ecommerce.adminbackend.service.AdsOrderAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdsOrderAdminServiceImpl extends BaseAdminService implements AdsOrderAdminService {

    private final AdsOrderRepository orderRepository;
    private final AdsUserRepository adsUserRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> list(String search, String status, String billingType, int page, int size) {
        var result = orderRepository.search(
                blankToNull(search),
                blankToNull(status),
                blankToNull(billingType),
                PageRequest.of(page, size));
        return PageResponse.from(result.map(this::toSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> get(Integer id) {
        AdsOrder order = requireFound(orderRepository.findById(id), "Ads order not found.");
        Map<String, Object> detail = toSummary(order);
        AdsUser user = adsUserRepository.findById(order.getUserId()).orElse(null);
        if (user != null) {
            Map<String, Object> userInfo = new LinkedHashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("name", user.getName());
            userInfo.put("email", user.getEmail());
            userInfo.put("phone", user.getPhone());
            userInfo.put("company", user.getCompany());
            userInfo.put("address", user.getAddress());
            userInfo.put("city", user.getCity());
            userInfo.put("state", user.getState());
            userInfo.put("pincode", user.getPincode());
            detail.put("user", userInfo);
            detail.put("customerName", user.getName());
            detail.put("customerEmail", user.getEmail());
            detail.put("customerPhone", user.getPhone());
        } else {
            detail.put("user", null);
        }
        return detail;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalOrders", orderRepository.count());
        stats.put("pendingOrders", orderRepository.countByStatusIgnoreCase("pending"));
        stats.put("paidOrders", orderRepository.countByStatusIgnoreCase("paid"));
        stats.put("failedOrders", orderRepository.countByStatusIgnoreCase("failed"));
        stats.put("cancelledOrders", orderRepository.countByStatusIgnoreCase("cancelled"));
        stats.put("refundedOrders", orderRepository.countByStatusIgnoreCase("refunded"));
        stats.put("totalPaidAmount", orderRepository.sumPaidAmount());
        stats.put("totalCustomers", adsUserRepository.count());
        return stats;
    }

    private Map<String, Object> toSummary(AdsOrder order) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", order.getId());
        row.put("orderId", order.getOrderId());
        row.put("userId", order.getUserId());
        row.put("adType", order.getAdType());
        row.put("adId", order.getAdId());
        row.put("adName", order.getAdName());
        row.put("adDescription", order.getAdDescription());
        row.put("amount", order.getAmount());
        row.put("billingType", order.getBillingType());
        row.put("dailyRate", order.getDailyRate());
        row.put("monthlyRate", order.getMonthlyRate());
        row.put("selectedPlan", order.getSelectedPlan());
        row.put("currency", order.getCurrency());
        row.put("status", order.getStatus());
        row.put("paymentId", order.getPaymentId());
        row.put("paymentStatus", order.getPaymentStatus());
        row.put("razorpayOrderId", order.getRazorpayOrderId());
        row.put("razorpayPaymentId", order.getRazorpayPaymentId());
        row.put("createdAt", order.getCreatedAt());
        row.put("updatedAt", order.getUpdatedAt());
        AdsUser user = order.getUserId() != null
                ? adsUserRepository.findById(order.getUserId()).orElse(null)
                : null;
        if (user != null) {
            row.put("customerName", user.getName());
            row.put("customerEmail", user.getEmail());
            row.put("customerPhone", user.getPhone());
        }
        return row;
    }
}
