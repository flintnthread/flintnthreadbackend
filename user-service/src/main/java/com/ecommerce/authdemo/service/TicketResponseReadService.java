package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.TicketResponseReadRequest;
import com.ecommerce.authdemo.dto.TicketResponseReadResponse;

import java.util.List;

public interface TicketResponseReadService {
    TicketResponseReadResponse markAsRead(TicketResponseReadRequest request);

    List<TicketResponseReadResponse> getByUserId(Integer userId);

    List<TicketResponseReadResponse> getByResponseId(Integer responseId);
}
