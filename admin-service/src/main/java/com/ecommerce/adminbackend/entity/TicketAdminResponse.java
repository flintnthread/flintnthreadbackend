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
@Table(name = "ticket_responses")
@Getter
@Setter
public class TicketAdminResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ticket_id", nullable = false)
    private Integer ticketId;

    @Column(name = "admin_id", nullable = false)
    private Integer adminId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String response;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
