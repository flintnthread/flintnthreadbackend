package com.ecommerce.authdemo.service.impl;



import com.ecommerce.authdemo.entity.SellerPaymentInvoice;
import com.ecommerce.authdemo.repository.SellerPaymentInvoiceRepository;
import com.ecommerce.authdemo.service.SellerInvoiceService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

    @Service
    @RequiredArgsConstructor
    public class SellerInvoiceServiceImpl implements SellerInvoiceService {

        private final SellerPaymentInvoiceRepository invoiceRepo;

        @Override
        public void createInvoice(Integer orderId, Integer sellerId, Double amount) {

            String invoiceNumber = "INV-" + System.currentTimeMillis();

            SellerPaymentInvoice invoice = SellerPaymentInvoice.builder()
                    .orderId(orderId)
                    .sellerId(sellerId)
                    .invoiceNumber(invoiceNumber)
                    .filename(invoiceNumber + ".pdf")
                    .filepath("/invoices/" + invoiceNumber + ".pdf")
                    .amount(BigDecimal.valueOf(amount))
                    .build();

            invoiceRepo.save(invoice);
        }
    }

