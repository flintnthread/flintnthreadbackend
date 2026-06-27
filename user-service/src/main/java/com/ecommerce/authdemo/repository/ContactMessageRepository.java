package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Integer> {

    @Query("""
            SELECT c
            FROM ContactMessage c
            WHERE (:status IS NULL OR c.status = :status)
            ORDER BY c.createdAt DESC
            """)
    List<ContactMessage> findAllByStatusFilter(@Param("status") Boolean status);
}
