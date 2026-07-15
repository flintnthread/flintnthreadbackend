package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailLogRepository extends JpaRepository<EmailLog, Integer> {
}
