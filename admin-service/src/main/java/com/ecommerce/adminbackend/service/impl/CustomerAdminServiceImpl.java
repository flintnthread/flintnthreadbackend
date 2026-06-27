package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.repository.CustomerQueryRepository;
import com.ecommerce.adminbackend.service.CustomerAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import com.ecommerce.adminbackend.service.support.CustomerAnalyticsAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomerAdminServiceImpl extends BaseAdminService implements CustomerAdminService {

    private final CustomerQueryRepository customerQueryRepository;
    private final CustomerAnalyticsAssembler customerAnalyticsAssembler;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listCustomers(String search, int page, int size) {
        String query = blankToNull(search);
        long total = customerQueryRepository.countCustomers(query);
        List<Object[]> rows = customerQueryRepository.listCustomers(query, page * size, size);
        List<Map<String, Object>> items = rows.stream().map(this::toCustomerSummary).toList();
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PageResponse<>(items, total, totalPages, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", customerQueryRepository.countDistinctCustomers());
        stats.put("totalCustomers", customerQueryRepository.countDistinctCustomers());
        stats.put("totalRevenue", customerQueryRepository.sumCustomerRevenue());
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getCustomer(Long id) {
        Object[] row = requireFound(
                customerQueryRepository.findCustomerById(id),
                "Customer not found.");
        Map<String, Object> detail = toCustomerDetail(row);
        detail.put("orders", buildOrderHistory(id));
        detail.put("monthlySpending", buildMonthlySpending(id));
        return detail;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getCustomerAnalytics(Long id) {
        Object[] row = requireFound(
                customerQueryRepository.findCustomerById(id),
                "Customer not found.");
        return customerAnalyticsAssembler.build(id, row);
    }

    private List<Map<String, Object>> buildOrderHistory(Long customerId) {
        List<Map<String, Object>> orders = new ArrayList<>();
        for (Object[] row : customerQueryRepository.listOrdersByCustomerId(customerId)) {
            Map<String, Object> order = new LinkedHashMap<>();
            order.put("id", row[0]);
            order.put("orderNumber", row[1]);
            order.put("createdAt", toDateTime(row[2]));
            order.put("totalAmount", row[3]);
            order.put("paymentMethod", row[4]);
            order.put("paymentStatus", row[5]);
            order.put("orderStatus", row[6]);
            order.put("itemCount", toLong(row[7]));
            orders.add(order);
        }
        return orders;
    }

    private List<Map<String, Object>> buildMonthlySpending(Long customerId) {
        String[] monthLabels = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        Map<Integer, Number> amounts = new LinkedHashMap<>();
        for (Object[] row : customerQueryRepository.monthlySpendingByCustomerId(customerId)) {
            amounts.put(((Number) row[0]).intValue(), (Number) row[1]);
        }
        List<Map<String, Object>> spending = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("month", monthLabels[month - 1]);
            entry.put("amount", amounts.getOrDefault(month, 0));
            spending.add(entry);
        }
        return spending;
    }

    private Map<String, Object> toCustomerSummary(Object[] row) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", toLong(row[0]));
        summary.put("email", row[1]);
        summary.put("name", row[2]);
        summary.put("phone", row[3]);
        summary.put("city", row[4]);
        summary.put("state", row[5]);
        summary.put("country", row[6]);
        summary.put("orderCount", toLong(row[7]));
        summary.put("totalSpent", row[8]);
        summary.put("lastOrderAt", toDateTime(row[9]));
        return summary;
    }

    private Map<String, Object> toCustomerDetail(Object[] row) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", toLong(row[0]));
        detail.put("email", row[1]);
        detail.put("name", row[2]);
        detail.put("phone", row[3]);
        detail.put("address1", row[4]);
        detail.put("address2", row[5]);
        detail.put("city", row[6]);
        detail.put("state", row[7]);
        detail.put("country", row[8]);
        detail.put("pincode", row[9]);
        detail.put("orderCount", toLong(row[10]));
        detail.put("totalSpent", row[11]);
        detail.put("firstOrderAt", toDateTime(row[12]));
        detail.put("lastOrderAt", toDateTime(row[13]));
        return detail;
    }
}
