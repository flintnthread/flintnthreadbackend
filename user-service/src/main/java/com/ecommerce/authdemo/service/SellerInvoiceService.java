

    package com.ecommerce.authdemo.service;

    public interface SellerInvoiceService {

        void createInvoice(Integer orderId, Integer sellerId, Double amount);
    }

