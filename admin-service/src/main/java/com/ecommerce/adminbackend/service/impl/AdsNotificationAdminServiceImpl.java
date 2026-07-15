package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.entity.ads.AdsAdminNotification;
import com.ecommerce.adminbackend.repository.AdsAdminNotificationRepository;
import com.ecommerce.adminbackend.service.AdsNotificationAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdsNotificationAdminServiceImpl extends BaseAdminService implements AdsNotificationAdminService {

    private final AdsAdminNotificationRepository repository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> list(String search, String status, Boolean unreadOnly, int page, int size) {
        var result = repository.search(
                blankToNull(search),
                blankToNull(status),
                unreadOnly,
                PageRequest.of(page, size));
        return PageResponse.from(result.map(this::toMap));
    }

    @Override
    @Transactional
    public Map<String, Object> patch(Integer id, Map<String, Object> body) {
        AdsAdminNotification notification =
                requireFound(repository.findById(id), "Ads notification not found.");
        if (body.containsKey("isRead") || body.containsKey("is_read")) {
            Object value = body.containsKey("isRead") ? body.get("isRead") : body.get("is_read");
            notification.setIsRead(toBoolean(value));
        }
        if (body.containsKey("status")) {
            String status = stringAt(body, "status");
            if (status == null || status.isBlank()) {
                throw new IllegalArgumentException("status cannot be blank.");
            }
            notification.setStatus(status.trim());
        }
        return toMap(repository.save(notification));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", repository.count());
        stats.put("unread", repository.countByIsReadFalseOrIsReadIsNull());
        stats.put("read", repository.countByIsReadTrue());
        return stats;
    }

    private Boolean toBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = String.valueOf(value).trim().toLowerCase();
        return "1".equals(text) || "true".equals(text) || "yes".equals(text);
    }

    private Map<String, Object> toMap(AdsAdminNotification notification) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", notification.getId());
        row.put("orderId", notification.getOrderId());
        row.put("userName", notification.getUserName());
        row.put("userEmail", notification.getUserEmail());
        row.put("adName", notification.getAdName());
        row.put("amount", notification.getAmount());
        row.put("billingType", notification.getBillingType());
        row.put("dailyRate", notification.getDailyRate());
        row.put("monthlyRate", notification.getMonthlyRate());
        row.put("status", notification.getStatus());
        row.put("isRead", Boolean.TRUE.equals(notification.getIsRead()));
        row.put("createdAt", notification.getCreatedAt());
        return row;
    }
}
