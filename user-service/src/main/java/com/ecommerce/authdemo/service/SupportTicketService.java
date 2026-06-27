package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.SupportTicketRequest;
import com.ecommerce.authdemo.dto.SupportTicketResponse;
import com.ecommerce.authdemo.dto.SupportTicketEditRequest;

import java.util.List;

public interface SupportTicketService {
    SupportTicketResponse create(SupportTicketRequest request);

    List<SupportTicketResponse> getTickets(Integer customerId, String status, String type);

    SupportTicketResponse updateStatus(Integer id, String status);

    SupportTicketResponse editByCustomer(Integer id, SupportTicketEditRequest request);

    SupportTicketResponse deleteByCustomer(Integer id);
}
