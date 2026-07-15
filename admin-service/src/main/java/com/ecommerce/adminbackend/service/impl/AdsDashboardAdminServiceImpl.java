package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.repository.AdsOrderRepository;
import com.ecommerce.adminbackend.repository.AdsPaymentRepository;
import com.ecommerce.adminbackend.repository.AdsUserRepository;
import com.ecommerce.adminbackend.service.AdsDashboardAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdsDashboardAdminServiceImpl extends BaseAdminService implements AdsDashboardAdminService {

    private final AdsOrderRepository orderRepository;
    private final AdsPaymentRepository paymentRepository;
    private final AdsUserRepository adsUserRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> dashboard(String period) {
        String normalized = period == null || period.isBlank() ? "monthly" : period.trim().toLowerCase(Locale.ROOT);
        LocalDateTime[] range = resolveRange(normalized);
        LocalDateTime from = range[0];
        LocalDateTime to = range[1];

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("period", normalized);
        dashboard.put("from", from);
        dashboard.put("to", to);

        Map<String, Object> orders = new LinkedHashMap<>();
        orders.put("total", orderRepository.count());
        orders.put("inPeriod", orderRepository.countCreatedBetween(from, to));
        orders.put("paidInPeriod", orderRepository.countByStatusBetween("paid", from, to));
        orders.put("pending", orderRepository.countByStatusIgnoreCase("pending"));
        orders.put("paid", orderRepository.countByStatusIgnoreCase("paid"));
        orders.put("paidAmountTotal", orderRepository.sumPaidAmount());
        orders.put("paidAmountInPeriod", orderRepository.sumPaidAmountBetween(from, to));
        dashboard.put("orders", orders);

        Map<String, Object> payments = new LinkedHashMap<>();
        payments.put("total", paymentRepository.count());
        payments.put("inPeriod", paymentRepository.countCreatedBetween(from, to));
        payments.put("successfulAmountTotal", paymentRepository.sumSuccessfulAmount());
        payments.put("successfulAmountInPeriod", paymentRepository.sumSuccessfulAmountBetween(from, to));
        dashboard.put("payments", payments);

        Map<String, Object> customers = new LinkedHashMap<>();
        customers.put("total", adsUserRepository.count());
        customers.put("newInPeriod", adsUserRepository.countCreatedBetween(from, to));
        dashboard.put("customers", customers);

        return dashboard;
    }

    private LocalDateTime[] resolveRange(String period) {
        LocalDate today = LocalDate.now();
        return switch (period) {
            case "daily" -> new LocalDateTime[]{
                    today.atStartOfDay(),
                    today.plusDays(1).atStartOfDay()
            };
            case "weekly" -> new LocalDateTime[]{
                    today.minusDays(6).atStartOfDay(),
                    today.plusDays(1).atStartOfDay()
            };
            case "yearly" -> new LocalDateTime[]{
                    today.withDayOfYear(1).atStartOfDay(),
                    today.plusDays(1).atStartOfDay()
            };
            case "monthly" -> new LocalDateTime[]{
                    today.withDayOfMonth(1).atStartOfDay(),
                    today.plusDays(1).atStartOfDay()
            };
            default -> throw new IllegalArgumentException(
                    "period must be daily, weekly, monthly, or yearly.");
        };
    }
}
