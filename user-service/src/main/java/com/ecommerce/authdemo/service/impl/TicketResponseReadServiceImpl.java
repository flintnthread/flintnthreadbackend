package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.TicketResponseReadRequest;
import com.ecommerce.authdemo.dto.TicketResponseReadResponse;
import com.ecommerce.authdemo.entity.TicketResponseRead;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.repository.TicketResponseReadRepository;
import com.ecommerce.authdemo.repository.TicketResponseRepository;
import com.ecommerce.authdemo.service.TicketResponseReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketResponseReadServiceImpl implements TicketResponseReadService {

    private final TicketResponseReadRepository ticketResponseReadRepository;
    private final TicketResponseRepository ticketResponseRepository;

    @Override
    public TicketResponseReadResponse markAsRead(TicketResponseReadRequest request) {
        if (!ticketResponseRepository.existsById(request.getResponseId())) {
            throw new ResourceNotFoundException("Ticket response not found");
        }

        TicketResponseRead entity = ticketResponseReadRepository
                .findByUserIdAndResponseId(request.getUserId(), request.getResponseId())
                .orElseGet(() -> ticketResponseReadRepository.save(
                        TicketResponseRead.builder()
                                .userId(request.getUserId())
                                .responseId(request.getResponseId())
                                .build()
                ));

        return toResponse(entity);
    }

    @Override
    public List<TicketResponseReadResponse> getByUserId(Integer userId) {
        return ticketResponseReadRepository.findByUserIdOrderByReadAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<TicketResponseReadResponse> getByResponseId(Integer responseId) {
        return ticketResponseReadRepository.findByResponseIdOrderByReadAtDesc(responseId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TicketResponseReadResponse toResponse(TicketResponseRead entity) {
        return TicketResponseReadResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .responseId(entity.getResponseId())
                .readAt(entity.getReadAt())
                .build();
    }
}
