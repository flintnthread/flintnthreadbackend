package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByOrderIdOrderByCreatedAtDesc(Integer orderId);
}
