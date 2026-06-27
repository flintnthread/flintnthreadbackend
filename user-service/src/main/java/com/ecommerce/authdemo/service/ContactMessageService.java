package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.ContactMessageRequest;
import com.ecommerce.authdemo.dto.ContactMessageResponse;

import java.util.List;

public interface ContactMessageService {
    ContactMessageResponse create(ContactMessageRequest request);

    List<ContactMessageResponse> getAll(Boolean status);

    ContactMessageResponse updateStatus(Integer id, Boolean status);
}
