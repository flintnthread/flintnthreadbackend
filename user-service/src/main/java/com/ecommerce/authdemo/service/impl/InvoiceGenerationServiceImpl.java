package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.InvoiceRequest;
import com.ecommerce.authdemo.service.InvoiceGenerationService;
import com.ecommerce.authdemo.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceGenerationServiceImpl implements InvoiceGenerationService {

    private final InvoiceService invoiceService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createForOrder(Integer orderId) {
        if (orderId == null || orderId <= 0) {
            return;
        }
        InvoiceRequest request = new InvoiceRequest();
        request.setOrderId(orderId);
        try {
            invoiceService.create(request);
        } catch (Exception error) {
            log.warn("[INVOICE] createForOrder failed for orderId={}: {}", orderId, error.getMessage());
        }
    }
}
