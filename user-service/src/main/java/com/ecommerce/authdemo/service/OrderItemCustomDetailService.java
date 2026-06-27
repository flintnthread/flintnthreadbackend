package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.CustomDetailFieldValueDTO;
import com.ecommerce.authdemo.dto.CustomRequiredFieldDTO;
import com.ecommerce.authdemo.dto.OrderCustomDetailsResponseDTO;
import com.ecommerce.authdemo.dto.OrderItemCustomDetailDTO;
import com.ecommerce.authdemo.dto.OrderItemDTO;
import com.ecommerce.authdemo.entity.OrderItem;
import com.ecommerce.authdemo.entity.Product;

import java.util.List;

public interface OrderItemCustomDetailService {

    List<CustomRequiredFieldDTO> resolveRequiredFields(Product product);

    List<OrderItemCustomDetailDTO> loadSavedDetails(Long orderItemId);

    boolean isDetailsComplete(List<CustomRequiredFieldDTO> requiredFields, List<OrderItemCustomDetailDTO> saved);

    void enrichOrderItemDto(OrderItem item, OrderItemDTO.OrderItemDTOBuilder builder);

    OrderCustomDetailsResponseDTO getOrderCustomDetails(Long orderId, Long userId);

    void saveOrderItemCustomDetails(
            Long orderItemId,
            Long userId,
            List<CustomDetailFieldValueDTO> fields
    );
}
