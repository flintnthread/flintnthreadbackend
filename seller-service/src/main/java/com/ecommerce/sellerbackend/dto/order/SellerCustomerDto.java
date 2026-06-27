package com.ecommerce.sellerbackend.dto.order;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SellerCustomerDto {
    private String name;
    private String phone;
    private String email;
    private String address;
}
