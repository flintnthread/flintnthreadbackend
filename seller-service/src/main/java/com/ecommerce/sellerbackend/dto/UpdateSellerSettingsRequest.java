package com.ecommerce.sellerbackend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSellerSettingsRequest {
    private Boolean pushNotifications;
    private Boolean orderUpdates;
    private Boolean payoutAlerts;
    private Boolean vacationMode;
    private Boolean darkMode;
    private String language;
    private Boolean biometricLogin;
}
