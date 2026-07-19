package com.ecommerce.authdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {

    private String token;
    private String role;
    private Long userId;
    /** Real email when present (synthetic mobile emails omitted). */
    private String email;
    private String contactNumber;
    private String displayName;
}
