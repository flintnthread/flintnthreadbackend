package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.EmailLogResponse;
import com.ecommerce.authdemo.entity.EmailLogStatus;

import java.util.List;

public interface EmailLogService {
    void createLog(Integer userId,
                   String emailType,
                   String recipient,
                   String subject,
                   EmailLogStatus status,
                   String errorMessage);

    List<EmailLogResponse> getLogs(Integer userId,
                                   String emailType,
                                   String recipient,
                                   EmailLogStatus status);
}
