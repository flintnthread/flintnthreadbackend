package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.EmailLogResponse;
import com.ecommerce.authdemo.entity.EmailLog;
import com.ecommerce.authdemo.entity.EmailLogStatus;
import com.ecommerce.authdemo.repository.EmailLogRepository;
import com.ecommerce.authdemo.service.EmailLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailLogServiceImpl implements EmailLogService {

    private final EmailLogRepository emailLogRepository;

    @Override
    public void createLog(Integer userId,
                          String emailType,
                          String recipient,
                          String subject,
                          EmailLogStatus status,
                          String errorMessage) {
        try {
            EmailLog entity = EmailLog.builder()
                    .userId(userId)
                    .emailType(emailType == null ? "general" : emailType.trim())
                    .recipient(recipient == null ? "" : recipient.trim())
                    .subject(subject == null ? "" : subject.trim())
                    .status(status == null ? EmailLogStatus.pending : status)
                    .errorMessage(errorMessage)
                    .sentAt(LocalDateTime.now())
                    .build();
            emailLogRepository.save(entity);
        } catch (Exception ex) {
            log.error("Failed to write email log for recipient={}", recipient, ex);
        }
    }

    @Override
    public List<EmailLogResponse> getLogs(Integer userId,
                                          String emailType,
                                          String recipient,
                                          EmailLogStatus status) {
        return emailLogRepository
                .findWithFilters(userId, normalize(emailType), normalize(recipient), status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private EmailLogResponse toResponse(EmailLog entity) {
        return EmailLogResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .emailType(entity.getEmailType())
                .recipient(entity.getRecipient())
                .subject(entity.getSubject())
                .status(entity.getStatus())
                .errorMessage(entity.getErrorMessage())
                .sentAt(entity.getSentAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
