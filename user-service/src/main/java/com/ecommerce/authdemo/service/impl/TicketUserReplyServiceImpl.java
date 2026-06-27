package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.TicketUserReplyRequest;
import com.ecommerce.authdemo.dto.TicketUserReplyResponse;
import com.ecommerce.authdemo.entity.TicketUserReply;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.repository.SupportTicketRepository;
import com.ecommerce.authdemo.repository.TicketUserReplyRepository;
import com.ecommerce.authdemo.service.TicketUserReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketUserReplyServiceImpl implements TicketUserReplyService {

    private final TicketUserReplyRepository ticketUserReplyRepository;
    private final SupportTicketRepository supportTicketRepository;

    @Override
    public TicketUserReplyResponse create(Integer ticketId, TicketUserReplyRequest request) {
        if (!supportTicketRepository.existsById(ticketId)) {
            throw new ResourceNotFoundException("Support ticket not found");
        }

        TicketUserReply entity = TicketUserReply.builder()
                .ticketId(ticketId)
                .userId(request.getUserId())
                .message(request.getMessage().trim())
                .build();

        return toResponse(ticketUserReplyRepository.save(entity));
    }

    @Override
    public List<TicketUserReplyResponse> getByTicketId(Integer ticketId) {
        return ticketUserReplyRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TicketUserReplyResponse toResponse(TicketUserReply entity) {
        return TicketUserReplyResponse.builder()
                .id(entity.getId())
                .ticketId(entity.getTicketId())
                .userId(entity.getUserId())
                .message(entity.getMessage())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
