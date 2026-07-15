package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.ProductDeliveryCheckResponse;
import com.ecommerce.authdemo.entity.Product;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.repository.DeliveryPincodeRepository;
import com.ecommerce.authdemo.repository.ProductPincodeRepository;
import com.ecommerce.authdemo.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductDeliveryCheckService {

    private final ProductRepository productRepository;
    private final ProductPincodeRepository productPincodeRepository;
    private final DeliveryPincodeRepository deliveryPincodeRepository;

    @Transactional(readOnly = true)
    public ProductDeliveryCheckResponse check(Long productId, String rawPincode) {
        if (productId == null || productId <= 0) {
            throw new ResourceNotFoundException("Product not found");
        }
        String pincode = normalizePincode(rawPincode);
        if (pincode == null) {
            return ProductDeliveryCheckResponse.builder()
                    .productId(productId)
                    .pincode(rawPincode != null ? rawPincode.trim() : "")
                    .deliverable(false)
                    .deliverAllLocations(false)
                    .message("Enter a valid 6-digit pincode to check delivery.")
                    .build();
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        boolean deliverAll = product.getDeliverAllLocations() == null
                || Boolean.TRUE.equals(product.getDeliverAllLocations());

        if (deliverAll) {
            boolean platformSupports = deliveryPincodeRepository.existsServiceablePincode(pincode);
            return ProductDeliveryCheckResponse.builder()
                    .productId(productId)
                    .pincode(pincode)
                    .deliverable(platformSupports)
                    .deliverAllLocations(true)
                    .message(
                            platformSupports
                                    ? "Delivery is available to this pincode."
                                    : "This pincode is not serviceable for delivery.")
                    .build();
        }

        boolean sellerAllows = productPincodeRepository.existsActiveDeliveryForProductAndPincode(
                productId, pincode);
        return ProductDeliveryCheckResponse.builder()
                .productId(productId)
                .pincode(pincode)
                .deliverable(sellerAllows)
                .deliverAllLocations(false)
                .message(
                        sellerAllows
                                ? "Seller delivers to this pincode."
                                : "Seller does not deliver to this pincode. Choose another address.")
                .build();
    }

    private static String normalizePincode(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() < 6) {
            return null;
        }
        return digits.substring(0, 6);
    }
}
