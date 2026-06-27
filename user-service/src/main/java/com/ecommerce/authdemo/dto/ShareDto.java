package com.ecommerce.authdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public class ShareDto {

        private String message;
        private String shareLink;
    }

