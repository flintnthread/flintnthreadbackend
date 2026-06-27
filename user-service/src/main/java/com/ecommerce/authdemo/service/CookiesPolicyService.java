package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.CookiesPolicyRequest;
import com.ecommerce.authdemo.dto.CookiesPolicyResponse;

public interface CookiesPolicyService {
    CookiesPolicyResponse getLatest();

    CookiesPolicyResponse upsert(CookiesPolicyRequest request);
}
