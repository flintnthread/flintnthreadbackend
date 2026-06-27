package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.TicketResponseRequest;
import com.ecommerce.authdemo.dto.TicketResponseResponse;
import com.ecommerce.authdemo.entity.TicketResponse;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.repository.SupportTicketRepository;
import com.ecommerce.authdemo.repository.TicketResponseRepository;
import com.ecommerce.authdemo.service.TicketResponseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketResponseServiceImpl implements TicketResponseService {

    private final TicketResponseRepository ticketResponseRepository;
    private final SupportTicketRepository supportTicketRepository;

    @Override
    public TicketResponseResponse create(Integer ticketId, TicketResponseRequest request) {
        if (!supportTicketRepository.existsById(ticketId)) {
            throw new ResourceNotFoundException("Support ticket not found");
        }

        TicketResponse entity = TicketResponse.builder()
                .ticketId(ticketId)
                .adminId(request.getAdminId())
                .response(request.getResponse().trim())
                .build();

        return toResponse(ticketResponseRepository.save(entity));
    }

    @Override
    public List<TicketResponseResponse> getByTicketId(Integer ticketId) {
        return ticketResponseRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TicketResponseResponse toResponse(TicketResponse entity) {
        return TicketResponseResponse.builder()
                .id(entity.getId())
                .ticketId(entity.getTicketId())
                .adminId(entity.getAdminId())
                .response(entity.getResponse())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
