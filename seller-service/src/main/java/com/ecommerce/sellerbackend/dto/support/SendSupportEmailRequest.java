package com.ecommerce.sellerbackend.dto.support;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendSupportEmailRequest {

    @NotNull
    private Integer sellerId;

    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;

    private String phone;

    @NotBlank
    private String subject;

    @NotBlank
    private String message;
}
