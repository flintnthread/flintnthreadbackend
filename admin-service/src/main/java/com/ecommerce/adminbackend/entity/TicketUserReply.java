package com.ecommerce.adminbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_user_replies")
@Getter
@Setter
public class TicketUserReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ticket_id", nullable = false)
    private Integer ticketId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
