package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BulkImportResponse {
    private int productsCreated;
    private int variantsCreated;
    private List<Long> productIds;
    private List<String> errors;
}
