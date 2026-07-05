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
        stats.put("waiting", ticketRepository.countWaitingStatuses());
        stats.put("closed", ticketRepository.countByStatusIgnoreCase("closed"));
        stats.put("resolved", ticketRepository.countByStatusIgnoreCase("resolved"));
        stats.put("urgent", ticketRepository.countUrgentPriorities());
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
        if (isTerminalStatus(ticket.getStatus())) {
            ticket.setStatus("open");
            ticket.setClosedAt(null);
        } else {
            ticket.setStatus("waiting_seller");
        }
        ticketRepository.save(ticket);

        return Map.of("ticketId", ticketId, "messageId", reply.getId(), "message", "Reply sent.");
    }

    @Override
    @Transactional
    public Map<String, Object> updateStatus(Long ticketId, String status) {
        String normalized = normalizeSellerTicketStatus(requireNonBlank(status, "Status"));
        SellerSupportTicket ticket = requireTicket(ticketId);
        ticket.setStatus(normalized);
        if ("closed".equals(normalized) || "resolved".equals(normalized)) {
            if (ticket.getClosedAt() == null) {
                ticket.setClosedAt(LocalDateTime.now());
            }
        } else {
            ticket.setClosedAt(null);
        }
        ticketRepository.save(ticket);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ticketId", ticketId);
        response.put("status", ticket.getStatus());
        response.put("statusLabel", formatStatusLabel(ticket.getStatus()));
        response.put("statusClosed", isTerminalStatus(ticket.getStatus()));
        response.put("canResolve", canResolve(ticket.getStatus()));
        response.put("canClose", canClose(ticket.getStatus()));
        response.put("canReopen", canReopen(ticket.getStatus()));
        response.put("message", "Ticket status updated.");
        return response;
    }

    private SellerSupportTicket requireTicket(Long id) {
        return requireFound(ticketRepository.findById(id), "Support ticket not found.");
    }

    private Map<String, Object> toTicketSummary(SellerSupportTicket ticket) {
        Seller seller = sellerRepository.findById(ticket.getSellerId()).orElse(null);
        String status = ticket.getStatus() != null ? ticket.getStatus().toLowerCase() : "open";
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", ticket.getId());
        summary.put("ticketNumber", ticket.getTicketNumber());
        summary.put("sellerId", ticket.getSellerId());
        summary.put("sellerName", seller != null ? seller.getFullName() : null);
        summary.put("sellerEmail", seller != null ? seller.getEmail() : null);
        summary.put("sellerPhone", seller != null ? seller.getMobile() : null);
        summary.put("subject", ticket.getSubject());
        summary.put("category", ticket.getCategory());
        summary.put("priority", ticket.getPriority());
        summary.put("status", status);
        summary.put("statusLabel", formatStatusLabel(status));
        summary.put("statusClosed", isTerminalStatus(status));
        summary.put("canResolve", canResolve(status));
        summary.put("canClose", canClose(status));
        summary.put("canReopen", canReopen(status));
        summary.put("lastResponseBy", ticket.getLastResponseBy());
        summary.put("lastResponseAt", ticket.getLastResponseAt());
        summary.put("closedAt", ticket.getClosedAt());
        summary.put("createdAt", ticket.getCreatedAt());
        summary.put("updatedAt", ticket.getUpdatedAt());
        return summary;
    }

    private String normalizeSellerTicketStatus(String status) {
        String normalized = status.trim().toLowerCase().replace(' ', '_');
        return switch (normalized) {
            case "open" -> "open";
            case "in_progress", "inprogress" -> "in_progress";
            case "waiting_admin", "waitingadmin" -> "waiting_admin";
            case "waiting_seller", "waitingseller" -> "waiting_seller";
            case "waiting" -> "waiting_admin";
            case "resolved" -> "resolved";
            case "closed" -> "closed";
            default -> throw new IllegalArgumentException("Unsupported ticket status: " + status);
        };
    }

    private String formatStatusLabel(String status) {
        if (status == null || status.isBlank()) {
            return "Open";
        }
        return switch (status.toLowerCase()) {
            case "open" -> "Open";
            case "in_progress" -> "In Progress";
            case "waiting_admin" -> "Waiting Admin";
            case "waiting_seller" -> "Waiting Seller";
            case "resolved" -> "Resolved";
            case "closed" -> "Closed";
            default -> status.substring(0, 1).toUpperCase() + status.substring(1).replace('_', ' ');
        };
    }

    private boolean isTerminalStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.toLowerCase();
        return "closed".equals(normalized) || "resolved".equals(normalized);
    }

    private boolean canResolve(String status) {
        return !isTerminalStatus(status);
    }

    private boolean canClose(String status) {
        return !isTerminalStatus(status);
    }

    private boolean canReopen(String status) {
        return isTerminalStatus(status);
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
