package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.common.PageResponse;

import java.util.Map;

public interface SupportAdminService {

    PageResponse<Map<String, Object>> listTickets(String status, String priority, String search, int page, int size);

    Map<String, Object> ticketStats();

    Map<String, Object> getTicket(Long id);

    Map<String, Object> addMessage(Long ticketId, String message);

    Map<String, Object> updateStatus(Long ticketId, String status);
}
