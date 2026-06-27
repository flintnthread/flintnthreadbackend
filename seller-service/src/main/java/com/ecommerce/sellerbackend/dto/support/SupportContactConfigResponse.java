package com.ecommerce.sellerbackend.dto.support;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportContactConfigResponse {
    private ChatContact chat;
    private EmailContact email;
    private CallContact call;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatContact {
        private boolean enabled;
        private String subtitle;
        private String whatsappNumber;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmailContact {
        private String address;
        private String subtitle;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CallContact {
        private String phone;
        private String subtitle;
        private String hours;
    }
}
