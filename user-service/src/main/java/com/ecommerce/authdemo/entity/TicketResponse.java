package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_responses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {

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
