package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.PincodeOptionResponse;
import com.ecommerce.sellerbackend.dto.ProductDeliverySettingsResponse;
import com.ecommerce.sellerbackend.dto.UpdateProductDeliveryRequest;
import com.ecommerce.sellerbackend.entity.Product;
import com.ecommerce.sellerbackend.entity.ProductPincode;
import com.ecommerce.sellerbackend.exception.ResourceNotFoundException;
import com.ecommerce.sellerbackend.repository.PincodeRepository;
import com.ecommerce.sellerbackend.repository.ProductPincodeRepository;
import com.ecommerce.sellerbackend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductDeliveryService {

    private final ProductRepository productRepository;
    private final ProductPincodeRepository productPincodeRepository;
    private final PincodeRepository pincodeRepository;

    @Transactional(readOnly = true)
    public ProductDeliverySettingsResponse getSettings(Long sellerId, Long productId, String search) {
        Product product = productRepository.findByIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));

        Set<Integer> selectedIds = productPincodeRepository.findByProductId(productId).stream()
                .map(ProductPincode::getPincodeId)
                .collect(Collectors.toCollection(HashSet::new));

        List<PincodeOptionResponse> options = pincodeRepository.searchWithLocation(search).stream()
                .map(row -> PincodeOptionResponse.builder()
                        .pincodeId(((Number) row[0]).intValue())
                        .pincode(row[1] != null ? row[1].toString() : "")
                        .area(row[2] != null ? row[2].toString() : "")
                        .city(row[3] != null ? row[3].toString() : "")
                        .state(row[4] != null ? row[4].toString() : "")
                        .country(row[5] != null ? row[5].toString() : "")
                        .selected(selectedIds.contains(((Number) row[0]).intValue()))
                        .build())
                .toList();

        return ProductDeliverySettingsResponse.builder()
                .productId(productId)
                .deliverAllLocations(Boolean.TRUE.equals(product.getDeliverAllLocations()))
                .pincodes(options)
                .build();
    }

    @Transactional
    public ProductDeliverySettingsResponse updateSettings(
            Long sellerId,
            Long productId,
            UpdateProductDeliveryRequest request) {
        Product product = productRepository.findByIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));

        product.setDeliverAllLocations(request.isDeliverAllLocations());
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);

        productPincodeRepository.deleteByProductId(productId);
        if (!request.isDeliverAllLocations() && request.getPincodeIds() != null) {
            LocalDateTime now = LocalDateTime.now();
            for (Integer pincodeId : request.getPincodeIds()) {
                if (pincodeId == null) {
                    continue;
                }
                ProductPincode link = new ProductPincode();
                link.setProductId(productId);
                link.setPincodeId(pincodeId);
                link.setStatus(1);
                link.setCreatedAt(now);
                productPincodeRepository.save(link);
            }
        }

        return getSettings(sellerId, productId, null);
    }
}
