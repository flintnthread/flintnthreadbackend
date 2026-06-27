package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.ApiResponse;
import com.ecommerce.authdemo.dto.NotificationInboxDTO;
import com.ecommerce.authdemo.dto.PushNotificationDetailDTO;
import com.ecommerce.authdemo.dto.PushNotificationRequest;
import com.ecommerce.authdemo.dto.PushNotificationResponse;
import com.ecommerce.authdemo.dto.PushTokenRequestDTO;
import com.ecommerce.authdemo.entity.User;
import com.ecommerce.authdemo.repository.UserRepository;
import com.ecommerce.authdemo.service.PushNotificationService;
import com.ecommerce.authdemo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/push-notifications")
@RequiredArgsConstructor
public class PushNotificationController {

    private final PushNotificationService pushNotificationService;
    private final UserService userService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<PushNotificationResponse>> create(
            @Valid @RequestBody PushNotificationRequest request) {
        PushNotificationResponse data = pushNotificationService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Push notification created successfully", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PushNotificationResponse>>> getNotifications(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isRead) {
        List<PushNotificationResponse> data = pushNotificationService.getNotifications(userId, type, isRead);
        return ResponseEntity.ok(new ApiResponse<>(true, "Push notifications fetched successfully", data));
    }

    @GetMapping("/inbox")
    public ResponseEntity<ApiResponse<NotificationInboxDTO>> getInbox(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "all") String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "7") int size) {
        NotificationInboxDTO data = pushNotificationService.getInbox(userId, category, page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, "Notification inbox fetched successfully", data));
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<ApiResponse<PushNotificationDetailDTO>> getDetail(
            @PathVariable Integer id,
            @RequestParam Long userId) {
        PushNotificationDetailDTO data = pushNotificationService.getDetail(id, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Notification detail fetched successfully", data));
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<Page<PushNotificationResponse>>> getNotificationsPaged(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isRead,
            Pageable pageable) {
        Page<PushNotificationResponse> data = pushNotificationService.getNotificationsPaged(userId, type, isRead, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Push notifications fetched successfully", data));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<PushNotificationResponse>> markAsRead(@PathVariable Integer id) {
        PushNotificationResponse data = pushNotificationService.markAsRead(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Push notification marked as read", data));
    }

    @PatchMapping("/{id}/unread")
    public ResponseEntity<ApiResponse<PushNotificationResponse>> markAsUnread(@PathVariable Integer id) {
        PushNotificationResponse data = pushNotificationService.markAsUnread(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Push notification marked as unread", data));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(@RequestParam Long userId) {
        int data = pushNotificationService.markAllAsRead(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "All notifications marked as read", data));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@RequestParam Long userId) {
        long data = pushNotificationService.getUnreadCount(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Unread notification count fetched successfully", data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Integer id) {
        pushNotificationService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Push notification deleted successfully", "OK"));
    }

    @PostMapping("/push-token")
    public ResponseEntity<?> savePushToken(
            @RequestBody
            PushTokenRequestDTO dto
    ) {

        User user =
                userService.getCurrentUser();

        user.setExpoPushToken(
                dto.getExpoPushToken()
        );

        userRepository.save(user);

        return ResponseEntity.ok(
                "Push token saved"
        );
    }
}
