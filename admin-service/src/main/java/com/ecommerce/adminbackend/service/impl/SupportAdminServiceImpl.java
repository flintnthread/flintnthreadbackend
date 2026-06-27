package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.entity.Seller;
import com.ecommerce.adminbackend.entity.SellerSupportMessage;
import com.ecommerce.adminbackend.entity.SellerSupportTicket;
import com.ecommerce.adminbackend.repository.SellerRepository;
import com.ecommerce.adminbackend.repository.SellerSupportMessageRepository;
import com.ecommerce.adminbackend.repository.SellerSupportTicketRepository;
import com.ecommerce.adminbackend.security.AdminSecurityUtils;
import com.ecommerce.adminbackend.service.SupportAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SupportAdminServiceImpl extends BaseAdminService implements SupportAdminService {

    private final SellerSupportTicketRepository ticketRepository;
    private final SellerSupportMessageRepository messageRepository;
    private final SellerRepository sellerRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listTickets(
            String status, String priority, String search, int page, int size) {
        var result = ticketRepository.searchTickets(
                blankToNull(status),
                blankToNull(priority),
                blankToNull(search),
                PageRequest.of(page, size));
        return PageResponse.from(result.map(this::toTicketSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> ticketStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", ticketRepository.count());
        stats.put("open", ticketRepository.countByStatusIgnoreCase("open"));
        stats.put("inProgress", ticketRepository.countByStatusIgnoreCase("in_progress"));
        stats.put("waiting", ticketRepository.countByStatusIgnoreCase("waiting"));
        stats.put("resolved", ticketRepository.countByStatusIgnoreCase("closed"));
        stats.put("urgent", ticketRepository.countByPriorityIgnoreCase("critical"));
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getTicket(Long id) {
        SellerSupportTicket ticket = requireTicket(id);
        List<SellerSupportMessage> messages = messageRepository.findByTicketIdOrderByCreatedAtAsc(id);
        Map<String, Object> detail = toTicketSummary(ticket);
        detail.put("messages", messages.stream().map(this::toMessage).toList());
        return detail;
    }

    @Override
    @Transactional
    public Map<String, Object> addMessage(Long ticketId, String message) {
        String body = requireNonBlank(message, "Message");
        SellerSupportTicket ticket = requireTicket(ticketId);

        SellerSupportMessage reply = new SellerSupportMessage();
        reply.setTicketId(ticketId);
        reply.setSenderType("admin");
        reply.setSenderId(AdminSecurityUtils.currentAdminId());
        reply.setMessage(body);
        messageRepository.save(reply);

        ticket.setLastResponseBy("admin");
        ticket.setLastResponseAt(LocalDateTime.now());
        if ("closed".equalsIgnoreCase(ticket.getStatus())) {
            ticket.setStatus("open");
            ticket.setClosedAt(null);
        }
        ticketRepository.save(ticket);

        return Map.of("ticketId", ticketId, "messageId", reply.getId(), "message", "Reply sent.");
    }

    @Override
    @Transactional
    public Map<String, Object> updateStatus(Long ticketId, String status) {
        String normalized = requireNonBlank(status, "Status").toLowerCase();
        SellerSupportTicket ticket = requireTicket(ticketId);
        ticket.setStatus(normalized);
        if ("closed".equals(normalized)) {
            ticket.setClosedAt(LocalDateTime.now());
        } else {
            ticket.setClosedAt(null);
        }
        ticketRepository.save(ticket);
        return Map.of("ticketId", ticketId, "status", ticket.getStatus(), "message", "Ticket status updated.");
    }

    private SellerSupportTicket requireTicket(Long id) {
        return requireFound(ticketRepository.findById(id), "Support ticket not found.");
    }

    private Map<String, Object> toTicketSummary(SellerSupportTicket ticket) {
        Seller seller = sellerRepository.findById(ticket.getSellerId()).orElse(null);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", ticket.getId());
        summary.put("ticketNumber", ticket.getTicketNumber());
        summary.put("sellerId", ticket.getSellerId());
        summary.put("sellerName", seller != null ? seller.getFullName() : null);
        summary.put("subject", ticket.getSubject());
        summary.put("category", ticket.getCategory());
        summary.put("priority", ticket.getPriority());
        summary.put("status", ticket.getStatus());
        summary.put("lastResponseBy", ticket.getLastResponseBy());
        summary.put("lastResponseAt", ticket.getLastResponseAt());
        summary.put("createdAt", ticket.getCreatedAt());
        summary.put("updatedAt", ticket.getUpdatedAt());
        return summary;
    }

    private Map<String, Object> toMessage(SellerSupportMessage message) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", message.getId());
        row.put("senderType", message.getSenderType());
        row.put("senderId", message.getSenderId());
        row.put("message", message.getMessage());
        row.put("attachment", message.getAttachment());
        row.put("createdAt", message.getCreatedAt());
        return row;
    }
}
