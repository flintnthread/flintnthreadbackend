package com.ecommerce.authdemo.service;

import java.util.Map;

public interface ShiprocketWebhookService {
    void handleWebhook(Map<String, Object> payload);
}
