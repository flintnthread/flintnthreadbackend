package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.TicketUserReplyRequest;
import com.ecommerce.authdemo.dto.TicketUserReplyResponse;

import java.util.List;

public interface TicketUserReplyService {
    TicketUserReplyResponse create(Integer ticketId, TicketUserReplyRequest request);

    List<TicketUserReplyResponse> getByTicketId(Integer ticketId);
}
