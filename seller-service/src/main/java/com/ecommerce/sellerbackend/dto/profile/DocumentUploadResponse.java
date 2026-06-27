package com.ecommerce.sellerbackend.dto.profile;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DocumentUploadResponse {
    private final String documentType;
    private final String fileName;
    private final String url;
}
