package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class ProductDetailResponse {

    private Long id;
    private Integer categoryId;
    private Integer subcategoryId;
    private Integer sizeChartId;
    private String name;
    private String sku;
    private BigDecimal price;
    private BigDecimal mrp;
    private BigDecimal mrpExclGst;
    private BigDecimal mrpInclGst;
    private BigDecimal sellingPriceExGst;
    private Integer discount;
    private List<String> images;
    private String status;
    private String rawStatus;
    private Integer stock;
    /** Minimum order quantity from the lowest-price variant (when set). */
    private Integer minQuantity;
    private String updated;
    private String category;
    private String categorySub;
    private String subcategory;
    private String color;
    private String size;
    private String hsnCode;
    private String gst;
    private String createdAt;
    private String approvedAt;
    private String shortDescription;
    private String description;
    private String material;
    private String weight;
    private String dimensions;
    private String returnPolicy;
    private String warranty;
    private String careInstructions;
    private String adminNotes;
    private Integer deliveryTimeMin;
    private Integer deliveryTimeMax;
    private BigDecimal intraCityCharge;
    private BigDecimal metroMetroCharge;
    private Boolean acceptCod;
    private Boolean fragile;
    private Boolean customized;
    private String customTitle;
    private String customInstructions;
    private String customLeadDays;
    private String customCharge;
    private Boolean customAllowPhoto;
    private String customImageLabel;
    private Boolean customAllowText;
    private String customTextLabel;
    private List<ProductSpecResponse> specifications;
    private List<String> features;
    private ProductDeliveryInfoResponse delivery;
    private ProductPackagingResponse packaging;
    private List<ProductDeliveryChargeResponse> deliveryCharges;
    private ProductReturnDetailsResponse returnDetails;
    private List<ProductVariantDetailResponse> variants;
    private List<ProductSizeChartRowResponse> sizeChart;
    private String sizeChartName;
    private String sizeChartImage;
}
