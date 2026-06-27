package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.*;
import com.ecommerce.authdemo.entity.Order;
import com.ecommerce.authdemo.entity.PushNotification;
import com.ecommerce.authdemo.entity.User;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.repository.OrderRepository;
import com.ecommerce.authdemo.repository.PushNotificationRepository;
import com.ecommerce.authdemo.repository.UserRepository;
import com.ecommerce.authdemo.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import org.json.JSONObject;

import org.springframework.http.HttpEntity;

import org.springframework.http.HttpHeaders;

import org.springframework.http.MediaType;

import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationServiceImpl implements PushNotificationService {

    private static final ZoneId ORDER_DISPLAY_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter ORDER_CREATED_DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy, h:mm a", Locale.ENGLISH);

    private static final Pattern ORDER_ID_IN_LINK =
            Pattern.compile("orderId=(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ORDER_NUM_IN_TEXT =
            Pattern.compile("#(\\d+)");

    private final PushNotificationRepository pushNotificationRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public PushNotificationResponse create(PushNotificationRequest request) {
        PushNotification entity = PushNotification.builder()
                .userId(request.getUserId())
                .title(request.getTitle().trim())
                .message(request.getMessage().trim())
                .type(normalize(request.getType()))
                .link(normalize(request.getLink()))
                .build();
        return toResponse(pushNotificationRepository.save(entity));
    }

    @Override
    public List<PushNotificationResponse> getNotifications(Long userId, String type, Boolean isRead) {
        return pushNotificationRepository.findWithFilters(userId, normalize(type), isRead)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public Page<PushNotificationResponse> getNotificationsPaged(Long userId, String type, Boolean isRead, Pageable pageable) {
        return pushNotificationRepository.findPageWithFilters(userId, normalize(type), isRead, pageable)
                .map(this::toResponse);
    }

    @Override
    public PushNotificationResponse markAsRead(Integer id) {
        PushNotification entity = pushNotificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Push notification not found"));
        entity.setIsRead(Boolean.TRUE);
        entity.setReadAt(LocalDateTime.now());
        return toResponse(pushNotificationRepository.save(entity));
    }

    @Override
    public PushNotificationResponse markAsUnread(Integer id) {
        PushNotification entity = pushNotificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Push notification not found"));
        entity.setIsRead(Boolean.FALSE);
        entity.setReadAt(null);
        return toResponse(pushNotificationRepository.save(entity));
    }

    @Override
    @Transactional
    public int markAllAsRead(Long userId) {
        return pushNotificationRepository.markAllAsReadByUserId(userId, LocalDateTime.now());
    }

    @Override
    public long getUnreadCount(Long userId) {
        return pushNotificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    public NotificationInboxDTO getInbox(Long userId, String category, int page, int size) {
        String normalizedCategory = normalizeCategory(category);
        Boolean isRead = "unread".equalsIgnoreCase(normalizedCategory) ? Boolean.FALSE : null;
        if ("unread".equalsIgnoreCase(normalizedCategory)) {
            normalizedCategory = "all";
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PushNotificationResponse> notifications = pushNotificationRepository
                .findInbox(userId, normalizedCategory, isRead, pageable)
                .map(this::toResponse);

        NotificationInboxSummaryDTO summary = new NotificationInboxSummaryDTO();
        summary.setTotal(pushNotificationRepository.countByUserId(userId));
        summary.setUnread(pushNotificationRepository.countByUserIdAndIsReadFalse(userId));
        summary.setOrders(pushNotificationRepository.countOrdersByUserId(userId));
        summary.setPromotions(pushNotificationRepository.countPromotionsByUserId(userId));
        summary.setSystem(pushNotificationRepository.countSystemByUserId(userId));

        NotificationInboxDTO inbox = new NotificationInboxDTO();
        inbox.setSummary(summary);
        inbox.setNotifications(notifications);
        return inbox;
    }

    @Override
    public PushNotificationDetailDTO getDetail(Integer id, Long userId) {
        PushNotification entity = pushNotificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Push notification not found"));
        if (!entity.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Push notification not found");
        }

        PushNotificationDetailDTO detail = new PushNotificationDetailDTO();
        detail.setNotification(toResponse(entity));

        Long orderId = resolveOrderId(entity);
        if (orderId != null) {
            orderRepository.findById(orderId).ifPresent(order -> {
                if (!order.getUserId().equals(userId)) {
                    return;
                }
                detail.setOrderDetail(toOrderDetail(order));
            });
        }
        return detail;
    }

    @Override
    @Transactional
    public void notifyUser(Long userId, String title, String message, String type, String link) {
        if (userId == null || userId <= 0) {
            return;
        }
        PushNotificationRequest request = new PushNotificationRequest();
        request.setUserId(userId);
        request.setTitle(title);
        request.setMessage(message);
        request.setType(type);
        request.setLink(link);
        create(request);

        User user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getExpoPushToken() != null && !user.getExpoPushToken().isBlank()) {
            sendNotification(user.getExpoPushToken(), title, message);
        }
    }

    @Override
    public void delete(Integer id) {
        PushNotification entity = pushNotificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Push notification not found"));
        pushNotificationRepository.delete(entity);
    }

    private PushNotificationResponse toResponse(PushNotification entity) {
        return PushNotificationResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .type(entity.getType())
                .link(entity.getLink())
                .isRead(entity.getIsRead())
                .createdAt(entity.getCreatedAt())
                .readAt(entity.getReadAt())
                .build();
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "all";
        }
        return category.trim().toLowerCase(Locale.ENGLISH);
    }

    private Long resolveOrderId(PushNotification entity) {
        String link = entity.getLink();
        if (link != null && !link.isBlank()) {
            Matcher linkMatcher = ORDER_ID_IN_LINK.matcher(link);
            if (linkMatcher.find()) {
                return Long.parseLong(linkMatcher.group(1));
            }
        }
        for (String text : List.of(entity.getTitle(), entity.getMessage())) {
            if (text == null || text.isBlank()) continue;
            Matcher textMatcher = ORDER_NUM_IN_TEXT.matcher(text);
            if (textMatcher.find()) {
                return Long.parseLong(textMatcher.group(1));
            }
        }
        return null;
    }

    private NotificationOrderDetailDTO toOrderDetail(Order order) {
        NotificationOrderDetailDTO dto = new NotificationOrderDetailDTO();
        dto.setOrderId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setOrderDate(formatOrderCreatedDate(order.getCreatedAt()));
        dto.setAmount(
                order.getTotalAmount() != null
                        ? BigDecimal.valueOf(order.getTotalAmount())
                        : null
        );
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setDeliveryAddress(formatAddress(order));
        dto.setOrderStatus(order.getOrderStatus());
        return dto;
    }

    private String formatOrderCreatedDate(LocalDateTime createdAt) {
        if (createdAt == null) {
            return null;
        }
        return createdAt
                .atZone(ZoneOffset.UTC)
                .withZoneSameInstant(ORDER_DISPLAY_ZONE)
                .format(ORDER_CREATED_DISPLAY_FORMAT);
    }

    private String formatAddress(Order order) {
        StringBuilder sb = new StringBuilder();
        if (order.getShippingAddress1() != null && !order.getShippingAddress1().isBlank()) {
            sb.append(order.getShippingAddress1().trim());
        }
        if (order.getShippingCity() != null && !order.getShippingCity().isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(order.getShippingCity().trim());
        }
        if (order.getShippingState() != null && !order.getShippingState().isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(order.getShippingState().trim());
        }
        if (order.getShippingPincode() != null && !order.getShippingPincode().isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(order.getShippingPincode().trim());
        }
        return sb.toString();
    }

    @Override
    public void sendNotification(
            String expoPushToken,
            String title,
            String message
    ) {

        try {

            if (
                    expoPushToken == null
                            || expoPushToken.isBlank()
            ) {

                log.warn(
                        "Expo push token missing"
                );

                return;
            }

            JSONObject body =
                    new JSONObject();

            body.put(
                    "to",
                    expoPushToken
            );

            body.put(
                    "title",
                    title
            );

            body.put(
                    "body",
                    message
            );

            HttpHeaders headers =
                    new HttpHeaders();

            headers.setContentType(
                    MediaType.APPLICATION_JSON
            );

            HttpEntity<String> entity =
                    new HttpEntity<>(
                            body.toString(),
                            headers
                    );

            restTemplate.postForEntity(
                    "https://exp.host/--/api/v2/push/send",
                    entity,
                    String.class
            );

            log.info(
                    "Push notification sent successfully"
            );

        } catch (Exception e) {

            log.error(
                    "Push notification failed",
                    e
            );
        }
    }
}
