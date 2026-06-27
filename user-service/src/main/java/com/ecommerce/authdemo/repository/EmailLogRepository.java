package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.EmailLog;
import com.ecommerce.authdemo.entity.EmailLogStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmailLogRepository extends JpaRepository<EmailLog, Integer> {

    @Query("""
            SELECT e
            FROM EmailLog e
            WHERE (:userId IS NULL OR e.userId = :userId)
              AND (:emailType IS NULL OR LOWER(e.emailType) = LOWER(:emailType))
              AND (:recipient IS NULL OR LOWER(e.recipient) = LOWER(:recipient))
              AND (:status IS NULL OR e.status = :status)
            ORDER BY e.createdAt DESC
            """)
    List<EmailLog> findWithFilters(@Param("userId") Integer userId,
                                   @Param("emailType") String emailType,
                                   @Param("recipient") String recipient,
                                   @Param("status") EmailLogStatus status);
}
