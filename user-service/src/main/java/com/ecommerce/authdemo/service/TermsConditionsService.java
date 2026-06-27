package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.TermsConditionsRequest;
import com.ecommerce.authdemo.dto.TermsConditionsResponse;

public interface TermsConditionsService {
    TermsConditionsResponse getLatest();

    TermsConditionsResponse upsert(TermsConditionsRequest request);
}
