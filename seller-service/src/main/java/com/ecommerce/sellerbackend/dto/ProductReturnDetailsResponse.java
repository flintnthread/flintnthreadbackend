package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProductReturnDetailsResponse {
    private String window;
    private List<String> conditions;
    private String process;
    private String refundMode;
}
