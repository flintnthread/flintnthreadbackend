package com.ecommerce.authdemo.dto;

    public class OtpResponseDTO {

        private String message;
        private String otp;

        public OtpResponseDTO(String message, String otp) {
            this.message = message;
            this.otp = otp;
        }

        public String getMessage() {
            return message;
        }

        public String getOtp() {
            return otp;
        }
    }

