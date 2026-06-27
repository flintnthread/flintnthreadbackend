package com.ecommerce.authdemo.repository;



import com.ecommerce.authdemo.entity.SellerPaymentInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

    public interface SellerPaymentInvoiceRepository extends JpaRepository<SellerPaymentInvoice, Integer> {

        List<SellerPaymentInvoice> findBySellerId(Integer sellerId);

        List<SellerPaymentInvoice> findByOrderId(Integer orderId);

        SellerPaymentInvoice findByInvoiceNumber(String invoiceNumber);
    }

