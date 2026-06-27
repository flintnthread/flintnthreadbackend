package com.ecommerce.sellerbackend.controller;

import com.ecommerce.sellerbackend.dto.BulkImportResponse;
import com.ecommerce.sellerbackend.dto.CreateProductRequest;
import com.ecommerce.sellerbackend.dto.CreateProductResponse;
import com.ecommerce.sellerbackend.dto.ProductDeliverySettingsResponse;
import com.ecommerce.sellerbackend.dto.ProductDetailResponse;
import com.ecommerce.sellerbackend.dto.ProductListItemResponse;
import com.ecommerce.sellerbackend.dto.UpdateProductDeliveryRequest;
import com.ecommerce.sellerbackend.dto.UpdateProductRequest;
import com.ecommerce.sellerbackend.dto.VariantMutationRequest;
import com.ecommerce.sellerbackend.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    public static final String SELLER_ID_HEADER = "X-Seller-Id";

    private final ProductService productService;

    @GetMapping
    public List<ProductListItemResponse> list(@RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        return productService.listForSeller(requireSellerId(sellerId));
    }

    @GetMapping("/{id}")
    public ProductDetailResponse getById(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @PathVariable Long id) {
        return productService.getDetailForSeller(requireSellerId(sellerId), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateProductResponse create(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @Valid @RequestBody CreateProductRequest request) {
        return productService.createForSeller(requireSellerId(sellerId), request);
    }

    @PutMapping("/{id}")
    public CreateProductResponse update(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        return productService.updateForSeller(requireSellerId(sellerId), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @PathVariable Long id) {
        productService.deleteForSeller(requireSellerId(sellerId), id);
    }

    @PostMapping("/{productId}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateProductResponse.CreatedVariantRef createVariant(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @PathVariable Long productId,
            @Valid @RequestBody VariantMutationRequest request) {
        return productService.createVariant(requireSellerId(sellerId), productId, request);
    }

    @PutMapping("/{productId}/variants/{variantId}")
    public CreateProductResponse.CreatedVariantRef updateVariant(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @Valid @RequestBody VariantMutationRequest request) {
        return productService.updateVariant(requireSellerId(sellerId), productId, variantId, request);
    }

    @DeleteMapping("/{productId}/variants/{variantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVariant(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @PathVariable Long productId,
            @PathVariable Long variantId) {
        productService.deleteVariant(requireSellerId(sellerId), productId, variantId);
    }

    @PostMapping(value = "/bulk-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BulkImportResponse bulkImport(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @RequestParam("file") MultipartFile file) {
        return productService.bulkImport(requireSellerId(sellerId), file);
    }

    @GetMapping("/{id}/delivery-settings")
    public ProductDeliverySettingsResponse getDeliverySettings(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @PathVariable Long id,
            @RequestParam(required = false) String search) {
        return productService.getDeliverySettings(requireSellerId(sellerId), id, search);
    }

    @PutMapping("/{id}/delivery-settings")
    public ProductDeliverySettingsResponse updateDeliverySettings(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductDeliveryRequest request) {
        return productService.updateDeliverySettings(requireSellerId(sellerId), id, request);
    }

    private Long requireSellerId(Long sellerId) {
        if (sellerId == null || sellerId <= 0) {
            throw new IllegalArgumentException("Valid seller id is required.");
        }
        return sellerId;
    }
}
