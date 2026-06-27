package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.SupportTicketRequest;
import com.ecommerce.authdemo.dto.SupportTicketResponse;
import com.ecommerce.authdemo.dto.SupportTicketEditRequest;
import com.ecommerce.authdemo.entity.SupportTicket;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.repository.SupportTicketRepository;
import com.ecommerce.authdemo.service.SupportTicketService;
import com.ecommerce.authdemo.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SupportTicketServiceImpl implements SupportTicketService {

    private final SupportTicketRepository supportTicketRepository;
    private final SecurityUtil securityUtil;

    @Override
    public SupportTicketResponse create(SupportTicketRequest request) {
        Integer customerId = Math.toIntExact(securityUtil.getCurrentUserId());

        SupportTicket entity = SupportTicket.builder()
                .customerId(customerId)
                .subject(request.getSubject().trim())
                .type(request.getType().trim())
                .message(request.getMessage().trim())
                .attachmentPath(normalize(request.getAttachmentPath()))
                .status("open")
                .build();
        return toResponse(supportTicketRepository.save(entity));
    }

    @Override
    public List<SupportTicketResponse> getTickets(Integer customerId, String status, String type) {
        return supportTicketRepository.findWithFilters(customerId, normalize(status), normalize(type))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public SupportTicketResponse updateStatus(Integer id, String status) {
        SupportTicket entity = supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found"));
        entity.setStatus(status.trim().toLowerCase(Locale.ROOT));
        return toResponse(supportTicketRepository.save(entity));
    }

    @Override
    public SupportTicketResponse editByCustomer(Integer id, SupportTicketEditRequest request) {
        Integer customerId = Math.toIntExact(securityUtil.getCurrentUserId());

        SupportTicket entity = supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found"));

        if (!customerId.equals(entity.getCustomerId())) {
            throw new ResourceNotFoundException("Support ticket not found");
        }

        boolean updated = false;

        if (request.getType() != null && !request.getType().trim().isEmpty()) {
            entity.setType(request.getType().trim());
            updated = true;
        }

        if (request.getMessage() != null && !request.getMessage().trim().isEmpty()) {
            entity.setMessage(request.getMessage().trim());
            updated = true;
        }

        if (!updated) {
            throw new IllegalArgumentException("At least one field (type/message) is required");
        }

        return toResponse(supportTicketRepository.save(entity));
    }

    @Override
    public SupportTicketResponse deleteByCustomer(Integer id) {
        Integer customerId = Math.toIntExact(securityUtil.getCurrentUserId());

        SupportTicket entity = supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found"));

        if (!customerId.equals(entity.getCustomerId())) {
            throw new ResourceNotFoundException("Support ticket not found");
        }

        SupportTicketResponse deleted = toResponse(entity);
        supportTicketRepository.delete(entity);
        return deleted;
    }

    private SupportTicketResponse toResponse(SupportTicket entity) {
        return SupportTicketResponse.builder()
                .id(entity.getId())
                .customerId(entity.getCustomerId())
                .subject(entity.getSubject())
                .type(entity.getType())
                .message(entity.getMessage())
                .attachmentPath(entity.getAttachmentPath())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
