package com.ecommerce.sellerbackend.dto.order;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SellerOrderDetailDto {
    private String id;
    private Long orderId;
    private String orderNumber;
    private String date;
    private String status;
    private String dbStatus;
    private SellerCustomerDto customer;
    private SellerCustomerDto billing;
    private List<SellerOrderLineDto> items;
    private SellerPricingDto pricing;
    private SellerPaymentDto payment;
    private List<SellerOrderStepDto> steps;
    private List<OrderStatusHistoryEntryDto> statusHistory;
    private List<OrderEmailLogDto> emailLogs;
    private List<OrderGstDto> gstRecords;
    private List<OrderReturnDto> returns;
    private List<OrderExchangeDto> exchanges;
    private List<OrderReplacementDto> replacements;
    private List<OrderItemCancellationDto> cancellations;
    private OrderReviewSummaryDto reviewSummary;
    /** Customer-facing note from checkout (order_notes). */
    private String customerNote;
    /** Internal / seller note (same as order_notes when no separate column). */
    private String sellerNote;
    private String cancelReason;
    private String gstNumber;
    private String gstInfo;
    private String primaryActionLabel;
    private String secondaryActionLabel;
    private String extraNote;
    private String shiprocketOrderId;
    private String shiprocketShipmentId;
    private String shiprocketAwbCode;
    private String shiprocketCourierName;
    private String shiprocketStatus;
    private String shiprocketTrackingUrl;
    private String shiprocketSyncedAt;
}
