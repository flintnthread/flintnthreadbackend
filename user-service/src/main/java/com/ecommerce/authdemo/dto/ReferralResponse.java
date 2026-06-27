package com.ecommerce.authdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ReferralResponse {

    private boolean success;
    private String message;
}