package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.TicketResponseRequest;
import com.ecommerce.authdemo.dto.TicketResponseResponse;

import java.util.List;

public interface TicketResponseService {
    TicketResponseResponse create(Integer ticketId, TicketResponseRequest request);

    List<TicketResponseResponse> getByTicketId(Integer ticketId);
}
