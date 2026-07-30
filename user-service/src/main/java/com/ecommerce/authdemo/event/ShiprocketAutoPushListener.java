package com.ecommerce.authdemo.event;

import com.ecommerce.authdemo.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * After order/payment commit, push to Shiprocket off the request thread.
 * Uses a dedicated non-daemon pool so the push is not cancelled when the HTTP request ends.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShiprocketAutoPushListener {

    private static final ExecutorService PUSH_EXECUTOR = Executors.newFixedThreadPool(
            4,
            new ThreadFactory() {
                private final AtomicInteger seq = new AtomicInteger(1);

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "shiprocket-auto-push-" + seq.getAndIncrement());
                    t.setDaemon(false);
                    return t;
                }
            }
    );

    private final OrderService orderService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleShiprocketAutoPush(ShiprocketAutoPushEvent event) {
        if (event == null || event.orderId() == null || event.orderId() <= 0) {
            return;
        }
        final Long orderId = event.orderId();
        final String orderNumber = event.orderNumber();

        CompletableFuture.runAsync(() -> pushWithRetries(orderId, orderNumber), PUSH_EXECUTOR);
    }

    private void pushWithRetries(Long orderId, String orderNumber) {
        // Let order items / payment commit become fully visible before the first attempt.
        sleepQuietly(800);

        Exception lastError = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                orderService.pushOrderToShiprocket(orderId);
                log.info(
                        "Shiprocket auto-push OK orderNumber={} orderId={} attempt={}",
                        orderNumber,
                        orderId,
                        attempt
                );
                return;
            } catch (Exception e) {
                lastError = e;
                String msg = e.getMessage() != null ? e.getMessage() : "";
                boolean retryable = attempt < 3 && (
                        msg.toLowerCase().contains("no order items")
                                || msg.toLowerCase().contains("timeout")
                                || msg.toLowerCase().contains("timed out")
                                || msg.toLowerCase().contains("connection")
                                || msg.toLowerCase().contains("temporarily")
                );
                log.warn(
                        "Shiprocket auto-push attempt {} failed orderNumber={} orderId={}: {}",
                        attempt,
                        orderNumber,
                        orderId,
                        msg
                );
                if (!retryable) {
                    break;
                }
                sleepQuietly(2000L * attempt);
            }
        }

        log.error(
                "Shiprocket auto-push exhausted retries orderNumber={} orderId={}: {}",
                orderNumber,
                orderId,
                lastError != null ? lastError.getMessage() : "unknown",
                lastError
        );
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
