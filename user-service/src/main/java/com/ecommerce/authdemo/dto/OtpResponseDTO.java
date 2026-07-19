package com.ecommerce.authdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Amazon-style OTP send result.
 * <ul>
 *   <li>{@code nextStep=VERIFY_OTP} — OTP was sent; client should collect the code</li>
 *   <li>{@code nextStep=ADD_PHONE} — email needs a linked mobile before OTP can be sent</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpResponseDTO {

    public static final String NEXT_VERIFY_OTP = "VERIFY_OTP";
    public static final String NEXT_ADD_PHONE = "ADD_PHONE";
    public static final String CODE_PHONE_REQUIRED = "PHONE_REQUIRED";

    private boolean success;
    private String message;
    /** SMS or EMAIL when an OTP was delivered. */
    private String deliveryChannel;
    /** VERIFY_OTP or ADD_PHONE */
    private String nextStep;
    /** PHONE_REQUIRED when the client must collect a mobile number */
    private String code;
    private String email;
    /** Masked linked/target phone, e.g. ******3210 */
    private String maskedPhone;
}
