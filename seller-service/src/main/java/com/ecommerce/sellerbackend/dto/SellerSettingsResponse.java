package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SellerSettingsResponse {
    private boolean pushNotifications;
    private boolean orderUpdates;
    private boolean payoutAlerts;
    private boolean vacationMode;
    private boolean darkMode;
    private String language;
    private boolean biometricLogin;
}
