package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BankAccountResponse {
    private String bankName;
    private String accountNumberMasked;
    private String ifscCode;
    private String accountHolder;
    private boolean verified;
}
