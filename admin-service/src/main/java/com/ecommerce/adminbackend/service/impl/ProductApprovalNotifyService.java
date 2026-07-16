package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.entity.Product;
import com.ecommerce.adminbackend.entity.Seller;
import com.ecommerce.adminbackend.repository.SellerRepository;
import com.ecommerce.adminbackend.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * After admin approves a product:
 * 1) email the seller
 * 2) write seller in-app notification
 * 3) fan-out customer inbox + Expo push ("new product arrived")
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductApprovalNotifyService {

    private final MailService mailService;
    private final SellerRepository sellerRepository;
    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Schedule notifications after the product row is committed as active.
     *
     * @param announceToCustomers when true, notify marketplace users of a new listing
     */
    public void afterProductGoLive(Product product, boolean announceToCustomers) {
        if (product == null || product.getId() == null) {
            return;
        }
        Long productId = product.getId();
        String productName = product.getName();
        String productSku = product.getSku();
        Long sellerId = product.getSellerId();

        Runnable work = () -> CompletableFuture.runAsync(() -> {
            try {
                notifySeller(sellerId, productId, productName, productSku);
                if (announceToCustomers) {
                    broadcastNewProductToCustomers(productId, productName);
                }
            } catch (Exception ex) {
                log.error("Post-approval notify failed productId={}", productId, ex);
            }
        });

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    work.run();
                }
            });
        } else {
            work.run();
        }
    }

    private void notifySeller(Long sellerId, Long productId, String productName, String productSku) {
        if (sellerId == null) {
            return;
        }
        Seller seller = sellerRepository.findById(sellerId).orElse(null);
        if (seller == null) {
            log.warn("Product approved but seller not found: sellerId={} productId={}", sellerId, productId);
            return;
        }

        String title = "Product approved";
        String message = "Your product \"" + safe(productName) + "\" is now live in the store.";
        try {
            jdbcTemplate.update(
                    "INSERT INTO seller_notifications (seller_id, title, message, is_read, created_at) "
                            + "VALUES (?, ?, ?, false, CURRENT_TIMESTAMP)",
                    sellerId,
                    title,
                    message);
        } catch (Exception ex) {
            log.error("Failed seller_notifications insert sellerId={} productId={}: {}",
                    sellerId, productId, ex.getMessage());
        }

        try {
            mailService.sendProductApprovedEmail(
                    seller.getEmail(),
                    seller.getFullName(),
                    productName,
                    productSku,
                    productId);
        } catch (RuntimeException ex) {
            log.error("Product approved but seller email failed sellerId={} productId={}",
                    sellerId, productId, ex);
        }
    }

    private void broadcastNewProductToCustomers(Long productId, String productName) {
        String title = "New product arrived";
        String message = "Check out \"" + safe(productName) + "\" — now available on Flint & Thread.";
        String link = "/productdetail?id=" + productId;
        String type = "promotion";

        try {
            // All active registered marketplace customers (every row in `users` with role USER).
            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                    "SELECT id, expo_push_token FROM users "
                            + "WHERE id IS NOT NULL "
                            + "AND (status IS NULL OR LOWER(TRIM(status)) = 'active') "
                            + "AND (role IS NULL OR UPPER(TRIM(role)) = 'USER')");
            int inbox = 0;
            int pushed = 0;
            for (Map<String, Object> row : users) {
                Object idObj = row.get("id");
                if (idObj == null) {
                    continue;
                }
                long userId = ((Number) idObj).longValue();
                try {
                    jdbcTemplate.update(
                            "INSERT INTO push_notifications (user_id, title, message, type, link, is_read, created_at) "
                                    + "VALUES (?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP)",
                            userId,
                            title,
                            message,
                            type,
                            link);
                    inbox++;
                } catch (Exception ex) {
                    log.warn("Failed push_notifications insert userId={} productId={}: {}",
                            userId, productId, ex.getMessage());
                    continue;
                }

                Object tokenObj = row.get("expo_push_token");
                String token = tokenObj != null ? String.valueOf(tokenObj).trim() : "";
                if (!token.isBlank() && !"null".equalsIgnoreCase(token)) {
                    if (sendExpoPush(token, title, message)) {
                        pushed++;
                    }
                }
            }
            log.info(
                    "New-product notify done productId={} inboxRows={} expoPushes={}",
                    productId,
                    inbox,
                    pushed);
        } catch (Exception ex) {
            log.error("New-product customer broadcast failed productId={}", productId, ex);
        }
    }

    private boolean sendExpoPush(String expoPushToken, String title, String message) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("to", expoPushToken);
            body.put("title", title);
            body.put("body", message);
            body.put("sound", "default");
            restTemplate.postForEntity(
                    "https://exp.host/--/api/v2/push/send",
                    body,
                    String.class);
            return true;
        } catch (Exception ex) {
            log.warn("Expo push failed: {}", ex.getMessage());
            return false;
        }
    }

    private static String safe(String value) {
        return value != null && !value.isBlank() ? value.trim() : "New product";
    }
}
