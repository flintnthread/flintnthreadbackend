package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_user_replies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
