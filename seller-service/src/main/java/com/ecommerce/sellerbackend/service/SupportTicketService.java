package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.support.*;
import com.ecommerce.sellerbackend.entity.*;
import com.ecommerce.sellerbackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private final SellerSupportTicketRepository ticketRepository;
    private final SellerSupportMessageRepository messageRepository;

    // ── Ticket Operations ───────────────────────────────────────────────────

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        String ticketNumber = generateTicketNumber();

        SellerSupportTicket ticket = SellerSupportTicket.builder()
                .ticketNumber(ticketNumber)
                .sellerId(request.getSellerId())
                .subject(request.getSubject())
                .category(request.getCategory().toLowerCase())
                .priority(request.getPriority().toLowerCase())
                .status("open")
                .lastResponseBy("seller")
                .lastResponseAt(LocalDateTime.now())
                .build();

        ticket = ticketRepository.save(ticket);

        String attachment = request.getAttachment() != null && !request.getAttachment().isBlank()
                ? request.getAttachment().trim()
                : null;
        String description = request.getDescription() != null ? request.getDescription().trim() : "";

        if (!description.isBlank() || attachment != null) {
            String text = !description.isBlank() ? description : "Attachment";
            saveSellerMessage(ticket.getId(), request.getSellerId(), text, attachment);
        }

        return toTicketResponse(ticket, true);
    }

    @Transactional
    public TicketResponse createTicketWithAttachment(CreateTicketRequest request, String attachmentUrl) {
        String ticketNumber = generateTicketNumber();

        SellerSupportTicket ticket = SellerSupportTicket.builder()
                .ticketNumber(ticketNumber)
                .sellerId(request.getSellerId())
                .subject(request.getSubject())
                .category(request.getCategory().toLowerCase())
                .priority(request.getPriority().toLowerCase())
                .status("open")
                .lastResponseBy("seller")
                .lastResponseAt(LocalDateTime.now())
                .build();

        ticket = ticketRepository.save(ticket);

        String text = request.getDescription() != null && !request.getDescription().isBlank()
                ? request.getDescription().trim()
                : "Attachment";
        saveSellerMessage(ticket.getId(), request.getSellerId(), text, attachmentUrl);

        return toTicketResponse(ticket, true);
    }

    public List<TicketResponse> getTicketsBySeller(Long sellerId) {
        List<SellerSupportTicket> tickets = ticketRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
        return tickets.stream()
                .map(t -> toTicketResponse(t, false))
                .collect(Collectors.toList());
    }

    public List<TicketResponse> getTicketsBySellerAndStatus(Long sellerId, String status) {
        List<SellerSupportTicket> tickets = ticketRepository.findBySellerIdAndStatusOrderByCreatedAtDesc(sellerId, status);
        return tickets.stream()
                .map(t -> toTicketResponse(t, false))
                .collect(Collectors.toList());
    }

    public TicketResponse getTicketById(Long ticketId, Long sellerId) {
        SellerSupportTicket ticket = ticketRepository.findByIdAndSellerId(ticketId, sellerId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        return toTicketResponse(ticket, true);
    }

    public TicketResponse getTicketByNumber(String ticketNumber) {
        SellerSupportTicket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        return toTicketResponse(ticket, true);
    }

    @Transactional
    public TicketResponse closeTicket(Long ticketId, Long sellerId) {
        SellerSupportTicket ticket = ticketRepository.findByIdAndSellerId(ticketId, sellerId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        ticket.setStatus("closed");
        ticket.setClosedAt(LocalDateTime.now());
        ticket = ticketRepository.save(ticket);

        return toTicketResponse(ticket, false);
    }

    // ── Message Operations ──────────────────────────────────────────────────

    @Transactional
    public MessageResponse addMessage(CreateMessageRequest request) {
        SellerSupportTicket ticket = ticketRepository.findById(request.getTicketId())
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        String text = request.getMessage() != null ? request.getMessage().trim() : "";
        String attachment = request.getAttachment() != null ? request.getAttachment().trim() : null;
        if (text.isBlank() && (attachment == null || attachment.isBlank())) {
            throw new IllegalArgumentException("Message or attachment is required");
        }
        if (text.isBlank()) {
            text = "Attachment";
        }

        SellerSupportMessage message = SellerSupportMessage.builder()
                .ticketId(request.getTicketId())
                .senderType(request.getSenderType())
                .senderId(request.getSenderId())
                .message(text)
                .attachment(attachment)
                .build();
        message = messageRepository.save(message);

        ticket.setLastResponseBy(request.getSenderType());
        ticket.setLastResponseAt(LocalDateTime.now());

        if ("seller".equals(request.getSenderType())) {
            ticket.setStatus("waiting_admin");
        } else if ("admin".equals(request.getSenderType())) {
            ticket.setStatus("waiting_seller");
        }

        ticketRepository.save(ticket);

        return toMessageResponse(message);
    }

    public List<MessageResponse> getMessagesByTicket(Long ticketId) {
        return messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    // ── Helper Methods ──────────────────────────────────────────────────────

    private String generateTicketNumber() {
        return "TKT-" + Long.toHexString(System.currentTimeMillis()).toUpperCase()
                + Integer.toHexString((int) (Math.random() * 0xFFFF)).toUpperCase();
    }

    private TicketResponse toTicketResponse(SellerSupportTicket ticket, boolean includeMessages) {
        TicketResponse response = TicketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .sellerId(ticket.getSellerId())
                .subject(ticket.getSubject())
                .category(ticket.getCategory())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .assignedTo(ticket.getAssignedTo())
                .lastResponseBy(ticket.getLastResponseBy())
                .lastResponseAt(ticket.getLastResponseAt())
                .closedAt(ticket.getClosedAt())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();

        if (includeMessages) {
            List<MessageResponse> messages = messageRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId())
                    .stream()
                    .map(this::toMessageResponse)
                    .collect(Collectors.toList());
            response.setMessages(messages);
        }

        return response;
    }

    private void saveSellerMessage(Long ticketId, Long sellerId, String text, String attachmentUrl) {
        SellerSupportMessage message = SellerSupportMessage.builder()
                .ticketId(ticketId)
                .senderType("seller")
                .senderId(sellerId)
                .message(text)
                .attachment(attachmentUrl)
                .build();
        messageRepository.save(message);
    }

    private MessageResponse toMessageResponse(SellerSupportMessage message) {
        return MessageResponse.builder()
                .id(message.getId())
                .ticketId(message.getTicketId())
                .senderType(message.getSenderType())
                .senderId(message.getSenderId())
                .message(message.getMessage())
                .attachment(message.getAttachment())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
