package com.ecommerce.authdemo.dto;

import lombok.Data;

@Data
public class SellerStatsDTO {
    private Double sellerRating;
    private String sellerRatingLabel;
    private Integer onTimeDeliveryPercent;
    private String onTimeDeliveryLabel;
    private Long ordersCompleted;
    private String ordersCompletedLabel;
    private Integer positiveFeedbackPercent;
    private String positiveFeedbackLabel;
    private Integer avgDispatchDays;
    private String avgDispatchDaysLabel;
}
