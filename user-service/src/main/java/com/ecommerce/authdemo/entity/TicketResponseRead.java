package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_responses_read")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponseRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "response_id", nullable = false)
    private Integer responseId;

    @Column(name = "read_at", insertable = false, updatable = false)
    private LocalDateTime readAt;
}
