package com.ecommerce.authdemo.scheduler;

import com.ecommerce.authdemo.entity.Order;
import com.ecommerce.authdemo.repository.OrderRepository;
import com.ecommerce.authdemo.service.ShiprocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShiprocketStatusSyncJob {

    private final OrderRepository orderRepository;
    private final ShiprocketService shiprocketService;

    @Value("${shiprocket.status-sync.enabled:true}")
    private boolean enabled;

    @Value("${shiprocket.status-sync.lookback-hours:72}")
    private int lookbackHours;

    @Value("${shiprocket.status-sync.batch-size:100}")
    private int batchSize;

    @Value("${shiprocket.status-sync.min-interval-minutes:30}")
    private int minIntervalMinutes;

    @Scheduled(
            fixedDelayString = "${shiprocket.status-sync.fixed-delay-ms:300000}",
            initialDelayString = "${shiprocket.status-sync.initial-delay-ms:120000}"
    )
    public void syncStatuses() {
        if (!enabled) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAfter = now.minusHours(Math.max(lookbackHours, 1));
        LocalDateTime syncedBefore = now.minusMinutes(Math.max(minIntervalMinutes, 1));
        int size = Math.max(1, Math.min(batchSize, 500));

        List<Order> candidates = orderRepository.findShiprocketStatusSyncCandidates(
                createdAfter,
                syncedBefore,
                PageRequest.of(0, size)
        );

        if (candidates.isEmpty()) {
            return;
        }

        int success = 0;
        int failed = 0;
        for (Order order : candidates) {
            try {
                shiprocketService.syncShipmentDetails(order);
                success++;
            } catch (Exception ex) {
                failed++;
                log.warn("Shiprocket periodic sync failed for orderId={} orderNumber={} msg={}",
                        order.getId(), order.getOrderNumber(), ex.getMessage());
            }
        }

        log.info("Shiprocket periodic status sync complete candidates={} success={} failed={}",
                candidates.size(), success, failed);
    }
}

