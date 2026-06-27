package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.entity.CustomerSupportTicket;
import com.ecommerce.adminbackend.entity.MarketplaceUser;
import com.ecommerce.adminbackend.entity.Order;
import com.ecommerce.adminbackend.entity.TicketAdminResponse;
import com.ecommerce.adminbackend.entity.TicketUserReply;
import com.ecommerce.adminbackend.repository.CustomerSupportTicketRepository;
import com.ecommerce.adminbackend.repository.MarketplaceUserRepository;
import com.ecommerce.adminbackend.repository.OrderRepository;
import com.ecommerce.adminbackend.repository.TicketAdminResponseRepository;
import com.ecommerce.adminbackend.repository.TicketUserReplyRepository;
import com.ecommerce.adminbackend.security.AdminSecurityUtils;
import com.ecommerce.adminbackend.service.CustomerSupportAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerSupportAdminServiceImpl extends BaseAdminService implements CustomerSupportAdminService {

    private final CustomerSupportTicketRepository ticketRepository;
    private final TicketAdminResponseRepository responseRepository;
    private final TicketUserReplyRepository userReplyRepository;
    private final MarketplaceUserRepository userRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listTickets(
            String status, String type, String search, int page, int size) {
        Page<CustomerSupportTicket> result = ticketRepository.searchTickets(
                normalizeStatusFilter(status),
                normalizeTypeFilter(type),
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
        stats.put("closed", ticketRepository.countByStatusIgnoreCase("closed"));
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getTicket(Integer id) {
        CustomerSupportTicket ticket = requireTicket(id);
        Map<String, Object> detail = toTicketSummary(ticket);
        detail.put("messages", buildConversation(ticket));
        return detail;
    }

    @Override
    @Transactional
    public Map<String, Object> addResponse(Integer ticketId, String message) {
        CustomerSupportTicket ticket = requireTicket(ticketId);
        TicketAdminResponse response = new TicketAdminResponse();
        response.setTicketId(ticketId);
        response.setAdminId(AdminSecurityUtils.currentAdminId().intValue());
        response.setResponse(requireNonBlank(message, "Response message"));
        responseRepository.save(response);

        if ("closed".equalsIgnoreCase(ticket.getStatus())) {
            ticket.setStatus("open");
        }
        ticketRepository.save(ticket);

        return Map.of(
                "ticketId", ticketId,
                "responseId", response.getId(),
                "message", "Response sent.");
    }

    @Override
    @Transactional
    public Map<String, Object> updateStatus(Integer ticketId, String status) {
        CustomerSupportTicket ticket = requireTicket(ticketId);
        String normalized = normalizeStatusFilter(status);
        if (normalized == null) {
            throw new IllegalArgumentException("Invalid ticket status.");
        }
        ticket.setStatus(normalized);
        ticketRepository.save(ticket);
        return Map.of("ticketId", ticketId, "status", ticket.getStatus(), "message", "Ticket status updated.");
    }

    private CustomerSupportTicket requireTicket(Integer id) {
        return requireFound(ticketRepository.findById(id), "Support ticket not found.");
    }

    private Map<String, Object> toTicketSummary(CustomerSupportTicket ticket) {
        MarketplaceUser user = userRepository.findById(ticket.getCustomerId()).orElse(null);
        Order order = ticket.getOrderId() != null
                ? orderRepository.findById(ticket.getOrderId().longValue()).orElse(null)
                : null;

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", ticket.getId());
        row.put("ticketNumber", formatTicketNumber(ticket.getId()));
        row.put("customerId", ticket.getCustomerId());
        row.put("customerName", user != null ? user.getName() : null);
        row.put("customerEmail", user != null ? user.getEmail() : null);
        row.put("customerPhone", user != null ? user.getContactNumber() : null);
        row.put("subject", ticket.getSubject());
        row.put("type", ticket.getType());
        row.put("typeLabel", formatTypeLabel(ticket.getType()));
        row.put("message", ticket.getMessage());
        row.put("orderId", ticket.getOrderId());
        row.put("orderNumber", order != null ? order.getOrderNumber() : extractOrderNumber(ticket.getSubject()));
        row.put("attachmentPath", ticket.getAttachmentPath());
        row.put("status", ticket.getStatus());
        row.put("statusLabel", formatStatusLabel(ticket.getStatus()));
        row.put("createdAt", ticket.getCreatedAt());
        row.put("updatedAt", ticket.getUpdatedAt());
        return row;
    }

    private List<Map<String, Object>> buildConversation(CustomerSupportTicket ticket) {
        List<Map<String, Object>> messages = new ArrayList<>();

        Map<String, Object> initial = new LinkedHashMap<>();
        initial.put("id", "initial-" + ticket.getId());
        initial.put("senderType", "customer");
        initial.put("senderName", resolveCustomerName(ticket.getCustomerId()));
        initial.put("message", ticket.getMessage());
        initial.put("createdAt", ticket.getCreatedAt());
        messages.add(initial);

        for (TicketUserReply reply : userReplyRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId())) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", reply.getId());
            row.put("senderType", "customer");
            row.put("senderName", resolveCustomerName(reply.getUserId()));
            row.put("message", reply.getMessage());
            row.put("createdAt", reply.getCreatedAt());
            messages.add(row);
        }

        for (TicketAdminResponse response : responseRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId())) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", response.getId());
            row.put("senderType", "admin");
            row.put("senderName", "Admin");
            row.put("message", response.getResponse());
            row.put("createdAt", response.getCreatedAt());
            messages.add(row);
        }

        messages.sort(Comparator.comparing(
                m -> Optional.ofNullable((java.time.LocalDateTime) m.get("createdAt"))
                        .orElse(java.time.LocalDateTime.MIN)));
        return messages;
    }

    private String resolveCustomerName(Integer userId) {
        return userRepository.findById(userId)
                .map(MarketplaceUser::getName)
                .filter(name -> name != null && !name.isBlank())
                .orElse("Customer");
    }

    private String formatTicketNumber(Integer id) {
        return "TKT-" + String.format("%05d", id);
    }

    private String formatTypeLabel(String type) {
        if (type == null || type.isBlank()) {
            return "Other";
        }
        return switch (type.trim().toLowerCase()) {
            case "delivery_issue" -> "Delivery Issue";
            case "payment_issue" -> "Payment Issue";
            case "product_issue" -> "Product Issue";
            case "other" -> "Other";
            default -> type.replace('_', ' ');
        };
    }

    private String formatStatusLabel(String status) {
        if (status == null || status.isBlank()) {
            return "Open";
        }
        return switch (status.trim().toLowerCase()) {
            case "open" -> "Open";
            case "in_progress" -> "In Progress";
            case "closed" -> "Closed";
            default -> status;
        };
    }

    private String normalizeTypeFilter(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        return switch (type.trim().toLowerCase()) {
            case "delivery issue", "delivery_issue" -> "delivery_issue";
            case "payment issue", "payment_issue" -> "payment_issue";
            case "product issue", "product_issue" -> "product_issue";
            case "other" -> "other";
            default -> type.trim().toLowerCase().replace(' ', '_');
        };
    }

    private String normalizeStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return switch (status.trim().toLowerCase()) {
            case "open" -> "open";
            case "in progress", "in_progress" -> "in_progress";
            case "closed" -> "closed";
            default -> status.trim().toLowerCase();
        };
    }

    private String extractOrderNumber(String subject) {
        if (subject == null) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("FNT\\d+")
                .matcher(subject);
        return matcher.find() ? matcher.group() : null;
    }
}
