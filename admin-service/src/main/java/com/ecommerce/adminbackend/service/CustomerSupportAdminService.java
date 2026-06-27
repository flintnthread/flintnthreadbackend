package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.common.PageResponse;

import java.util.Map;

public interface CustomerSupportAdminService {

    PageResponse<Map<String, Object>> listTickets(String status, String type, String search, int page, int size);

    Map<String, Object> ticketStats();

    Map<String, Object> getTicket(Integer id);

    Map<String, Object> addResponse(Integer ticketId, String message);

    Map<String, Object> updateStatus(Integer ticketId, String status);
}
