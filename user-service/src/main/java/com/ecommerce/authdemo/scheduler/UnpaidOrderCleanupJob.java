package com.ecommerce.authdemo.scheduler;

import com.ecommerce.authdemo.entity.Order;
import com.ecommerce.authdemo.repository.OrderRepository;
import com.ecommerce.authdemo.service.OrderService;
import com.ecommerce.authdemo.service.RazorpayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Cancels ONLINE orders that were created but never paid within the payment
 * window. Runs on a fixed delay (default every 60s). For each expired order it
 * first re-checks Razorpay in case the payment landed late (then marks it paid);
 * otherwise it cancels the order, restoring stock and refunding any FNT Wallet
 * amount used.
 *
 * Kept in a separate bean from OrderServiceImpl so the OrderService calls go
 * through the transactional proxy (self-invocation would bypass @Transactional).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UnpaidOrderCleanupJob {

    /** Grace period before an unpaid online order is auto-cancelled. */
    private static final long PAYMENT_WINDOW_MINUTES = 5;

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final RazorpayService razorpayService;

    @Scheduled(
            fixedDelayString = "${orders.unpaid-cleanup.interval-ms:60000}",
            initialDelayString = "${orders.unpaid-cleanup.initial-delay-ms:60000}"
    )
    public void cancelExpiredUnpaidOrders() {
        LocalDateTime cutoff = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(PAYMENT_WINDOW_MINUTES);

        List<Order> expired;
        try {
            expired = orderRepository.findExpiredUnpaidOnlineOrders(cutoff);
        } catch (Exception e) {
            log.error("Unpaid-order cleanup query failed: {}", e.getMessage(), e);
            return;
        }
        if (expired == null || expired.isEmpty()) {
            return;
        }

        int cancelled = 0;
        int reconciledPaid = 0;

        for (Order order : expired) {
            try {
                // Late-payment guard: if Razorpay actually captured the payment
                // in the meantime, confirm the order instead of cancelling it.
                String razorpayOrderId = order.getRazorpayOrderId();
                if (razorpayOrderId != null && !razorpayOrderId.isBlank()) {
                    String paymentId = null;
                    try {
                        paymentId = razorpayService.findCapturedPaymentId(razorpayOrderId);
                    } catch (Exception e) {
                        log.warn(
                                "Razorpay status check failed for order {} (rzp={}): {}",
                                order.getOrderNumber(), razorpayOrderId, e.getMessage()
                        );
                    }
                    if (paymentId != null && !paymentId.isBlank()) {
                        orderService.markOrderAsPaid(razorpayOrderId, paymentId);
                        reconciledPaid++;
                        continue;
                    }
                }

                Order result = orderService.systemCancelUnpaidOrder(
                        order.getId(),
                        "Payment not completed within " + PAYMENT_WINDOW_MINUTES + " minutes"
                );
                if (result != null && "cancelled".equalsIgnoreCase(result.getOrderStatus())) {
                    cancelled++;
                }
            } catch (Exception e) {
                log.error(
                        "Auto-cancel failed for order {} (id={}): {}",
                        order.getOrderNumber(), order.getId(), e.getMessage(), e
                );
            }
        }

        if (cancelled > 0 || reconciledPaid > 0) {
            log.info(
                    "Unpaid-order cleanup: scanned={} cancelled={} reconciledPaid={}",
                    expired.size(), cancelled, reconciledPaid
            );
        }
    }
}
