package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.ContactInquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ContactInquiryRepository extends JpaRepository<ContactInquiry, Integer> {

    Page<ContactInquiry> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(Boolean status);

    @Query("SELECT COUNT(c) FROM ContactInquiry c WHERE c.status IS NULL OR c.status = false")
    long countUnread();
}
