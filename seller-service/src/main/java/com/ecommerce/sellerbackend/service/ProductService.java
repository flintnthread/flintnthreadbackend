package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.BulkImportResponse;
import com.ecommerce.sellerbackend.dto.CreateProductRequest;
import com.ecommerce.sellerbackend.dto.CreateProductResponse;
import com.ecommerce.sellerbackend.dto.ProductDeliverySettingsResponse;
import com.ecommerce.sellerbackend.dto.ProductDetailResponse;
import com.ecommerce.sellerbackend.dto.ProductListItemResponse;
import com.ecommerce.sellerbackend.dto.UpdateProductDeliveryRequest;
import com.ecommerce.sellerbackend.dto.UpdateProductRequest;
import com.ecommerce.sellerbackend.dto.VariantMutationRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {

    List<ProductListItemResponse> listForSeller(Long sellerId);

    ProductDetailResponse getDetailForSeller(Long sellerId, Long productId);

    CreateProductResponse createForSeller(Long sellerId, CreateProductRequest request);

    CreateProductResponse updateForSeller(Long sellerId, Long productId, UpdateProductRequest request);

    void deleteForSeller(Long sellerId, Long productId);

    CreateProductResponse.CreatedVariantRef createVariant(
            Long sellerId, Long productId, VariantMutationRequest request);

    CreateProductResponse.CreatedVariantRef updateVariant(
            Long sellerId, Long productId, Long variantId, VariantMutationRequest request);

    void deleteVariant(Long sellerId, Long productId, Long variantId);

    BulkImportResponse bulkImport(Long sellerId, MultipartFile file);

    ProductDeliverySettingsResponse getDeliverySettings(Long sellerId, Long productId, String search);

    ProductDeliverySettingsResponse updateDeliverySettings(
            Long sellerId, Long productId, UpdateProductDeliveryRequest request);
}
