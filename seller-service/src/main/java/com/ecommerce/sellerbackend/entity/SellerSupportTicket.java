package com.ecommerce.sellerbackend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "seller_support_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerSupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, unique = true)
    private String ticketNumber;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String priority;

    @Column(nullable = false)
    private String status;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "last_response_by")
    private String lastResponseBy;

    @Column(name = "last_response_at")
    private LocalDateTime lastResponseAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = "open";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
