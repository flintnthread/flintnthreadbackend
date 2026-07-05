package com.ecommerce.authdemo.event;

import com.ecommerce.authdemo.service.InvoiceGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceOrderEventListener {

    private final InvoiceGenerationService invoiceGenerationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPlaced(OrderPlacedEvent event) {
        if (event == null || event.orderId() == null || event.orderId() <= 0) {
            return;
        }
        try {
            invoiceGenerationService.createForOrder(event.orderId());
        } catch (Exception error) {
            log.warn(
                    "[ORDER] post-commit invoice generation failed for orderId={}: {}",
                    event.orderId(),
                    error.getMessage()
            );
        }
    }
}
