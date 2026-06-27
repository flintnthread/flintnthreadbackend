package com.ecommerce.authdemo.dto;

import lombok.Data;

import java.util.List;

@Data
public class SellerStoreResponseDTO {
    private SellerProfileDTO profile;
    private SellerStatsDTO stats;
    private String aboutText;
    private List<SellerHighlightDTO> highlights;
    private List<SellerPolicyDTO> policies;
    private List<ProductDTO> topProducts;
    private long productCount;
    private long reviewCount;
    private long qaCount;
}
