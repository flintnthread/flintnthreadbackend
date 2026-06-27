package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.*;
import com.ecommerce.authdemo.entity.Order;
import com.ecommerce.authdemo.entity.OrderItem;
import com.ecommerce.authdemo.entity.OrderItemCustomDetail;
import com.ecommerce.authdemo.entity.Product;
import com.ecommerce.authdemo.exception.OrderException;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.repository.OrderItemCustomDetailRepository;
import com.ecommerce.authdemo.repository.OrderItemRepository;
import com.ecommerce.authdemo.repository.OrderRepository;
import com.ecommerce.authdemo.repository.ProductRepository;
import com.ecommerce.authdemo.service.OrderItemCustomDetailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderItemCustomDetailServiceImpl implements OrderItemCustomDetailService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final OrderItemCustomDetailRepository customDetailRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Override
    public List<CustomRequiredFieldDTO> resolveRequiredFields(Product product) {
        if (product == null || !Boolean.TRUE.equals(product.getIsCustomizedProduct())) {
            return List.of();
        }
        String raw = product.getCustomRequiredFields();
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(raw.trim());
            if (!root.isArray() || root.isEmpty()) {
                return List.of();
            }
            List<CustomRequiredFieldDTO> fields = new ArrayList<>();
            for (int i = 0; i < root.size(); i++) {
                JsonNode node = root.get(i);
                if (node == null || !node.isObject()) {
                    continue;
                }
                String label = readText(node, "label");
                if (label.isBlank()) {
                    continue;
                }
                String type = normalizeFieldType(readText(node, "type"));
                boolean required = node.path("required").asBoolean(true);
                String key = readText(node, "key");
                fields.add(CustomRequiredFieldDTO.builder()
                        .key(key.isBlank() ? "f" + i : key)
                        .label(label)
                        .type(type)
                        .required(required)
                        .build());
            }
            return fields;
        } catch (Exception e) {
            log.warn("Could not parse custom_required_fields for product {}: {}", product.getId(), e.getMessage());
            return List.of();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderItemCustomDetailDTO> loadSavedDetails(Long orderItemId) {
        if (orderItemId == null || orderItemId <= 0) {
            return List.of();
        }
        return customDetailRepository.findByOrderItemIdOrderByIdAsc(orderItemId).stream()
                .map(this::toDetailDto)
                .toList();
    }

    @Override
    public boolean isDetailsComplete(
            List<CustomRequiredFieldDTO> requiredFields,
            List<OrderItemCustomDetailDTO> saved
    ) {
        if (requiredFields == null || requiredFields.isEmpty()) {
            return true;
        }
        Map<String, OrderItemCustomDetailDTO> savedByKey = saved == null
                ? Map.of()
                : saved.stream()
                .filter(d -> d.getFieldKey() != null && !d.getFieldKey().isBlank())
                .collect(Collectors.toMap(
                        d -> d.getFieldKey().trim(),
                        d -> d,
                        (a, b) -> b
                ));

        for (CustomRequiredFieldDTO field : requiredFields) {
            if (!field.isRequired()) {
                continue;
            }
            OrderItemCustomDetailDTO value = savedByKey.get(field.getKey());
            if (value == null) {
                return false;
            }
            if (isImageField(field.getType())) {
                if (isBlank(value.getValueFile())) {
                    return false;
                }
            } else if (isBlank(value.getValueText())) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public void enrichOrderItemDto(OrderItem item, OrderItemDTO.OrderItemDTOBuilder builder) {
        if (item == null || builder == null) {
            return;
        }

        builder.orderItemId(item.getId());
        builder.variantId(item.getVariantId());

        Product product = productRepository.findById(item.getProductId()).orElse(null);
        boolean customizable = product != null && Boolean.TRUE.equals(product.getIsCustomizedProduct());
        builder.isCustomizable(customizable);

        if (!customizable) {
            builder.customRequiredFields(List.of());
            builder.customDetails(List.of());
            builder.customDetailsComplete(true);
            return;
        }

        List<CustomRequiredFieldDTO> requiredFields = resolveRequiredFields(product);
        List<OrderItemCustomDetailDTO> savedDetails = loadSavedDetails(item.getId());
        builder.customRequiredFields(requiredFields);
        builder.customDetails(savedDetails);
        builder.customDetailsComplete(isDetailsComplete(requiredFields, savedDetails));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderCustomDetailsResponseDTO getOrderCustomDetails(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        assertOrderOwner(order, userId);

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        List<OrderCustomDetailsItemDTO> customizableItems = new ArrayList<>();

        for (OrderItem item : orderItems) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product == null || !Boolean.TRUE.equals(product.getIsCustomizedProduct())) {
                continue;
            }
            List<CustomRequiredFieldDTO> requiredFields = resolveRequiredFields(product);
            List<OrderItemCustomDetailDTO> savedDetails = loadSavedDetails(item.getId());
            customizableItems.add(OrderCustomDetailsItemDTO.builder()
                    .orderItemId(item.getId())
                    .productId(item.getProductId())
                    .variantId(item.getVariantId())
                    .productName(item.getProductName())
                    .productImage(item.getProductImagePath())
                    .quantity(item.getQuantity())
                    .customizable(true)
                    .requiredFields(requiredFields)
                    .savedDetails(savedDetails)
                    .detailsComplete(isDetailsComplete(requiredFields, savedDetails))
                    .build());
        }

        boolean allComplete = customizableItems.isEmpty()
                || customizableItems.stream().allMatch(OrderCustomDetailsItemDTO::isDetailsComplete);

        return OrderCustomDetailsResponseDTO.builder()
                .orderId(orderId)
                .hasCustomizableItems(!customizableItems.isEmpty())
                .allDetailsComplete(allComplete)
                .items(customizableItems)
                .build();
    }

    @Override
    @Transactional
    public void saveOrderItemCustomDetails(
            Long orderItemId,
            Long userId,
            List<CustomDetailFieldValueDTO> fields
    ) {
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found"));

        Order order = orderRepository.findById(item.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        assertOrderOwner(order, userId);

        Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (!Boolean.TRUE.equals(product.getIsCustomizedProduct())) {
            throw new OrderException("This product does not require customization details");
        }

        List<CustomRequiredFieldDTO> requiredFields = resolveRequiredFields(product);
        Map<String, CustomRequiredFieldDTO> requiredByKey = requiredFields.stream()
                .collect(Collectors.toMap(CustomRequiredFieldDTO::getKey, f -> f, (a, b) -> b));

        if (fields == null) {
            fields = List.of();
        }

        Map<String, CustomDetailFieldValueDTO> submitted = new LinkedHashMap<>();
        for (CustomDetailFieldValueDTO field : fields) {
            if (field == null || isBlank(field.getFieldKey())) {
                continue;
            }
            submitted.put(field.getFieldKey().trim(), field);
        }

        for (CustomRequiredFieldDTO required : requiredFields) {
            if (!required.isRequired()) {
                continue;
            }
            CustomDetailFieldValueDTO value = submitted.get(required.getKey());
            if (value == null) {
                throw new OrderException("Missing required field: " + required.getLabel());
            }
            if (isImageField(required.getType())) {
                if (isBlank(value.getValueFile())) {
                    throw new OrderException("Image required for: " + required.getLabel());
                }
            } else if (isBlank(value.getValueText())) {
                throw new OrderException("Value required for: " + required.getLabel());
            }
        }

        customDetailRepository.deleteByOrderItemId(orderItemId);

        for (Map.Entry<String, CustomDetailFieldValueDTO> entry : submitted.entrySet()) {
            CustomRequiredFieldDTO definition = requiredByKey.get(entry.getKey());
            if (definition == null) {
                continue;
            }
            CustomDetailFieldValueDTO value = entry.getValue();
            String text = trimToNull(value.getValueText());
            String file = trimToNull(value.getValueFile());
            if (text == null && file == null) {
                continue;
            }
            if (isImageField(definition.getType()) && file == null) {
                continue;
            }
            if (!isImageField(definition.getType()) && text == null) {
                continue;
            }

            OrderItemCustomDetail row = OrderItemCustomDetail.builder()
                    .orderItemId(orderItemId)
                    .fieldKey(definition.getKey())
                    .fieldLabel(definition.getLabel())
                    .valueText(text)
                    .valueFile(file)
                    .build();
            customDetailRepository.save(row);
        }
    }

    private OrderItemCustomDetailDTO toDetailDto(OrderItemCustomDetail entity) {
        return OrderItemCustomDetailDTO.builder()
                .fieldKey(entity.getFieldKey())
                .fieldLabel(entity.getFieldLabel())
                .valueText(entity.getValueText())
                .valueFile(entity.getValueFile())
                .build();
    }

    private void assertOrderOwner(Order order, Long userId) {
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new OrderException("Access denied");
        }
    }

    private static String readText(JsonNode node, String key) {
        JsonNode value = node.get(key);
        return value != null && !value.isNull() ? value.asText("").trim() : "";
    }

    private static String normalizeFieldType(String raw) {
        String type = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if ("image".equals(type) || "file".equals(type) || "photo".equals(type)) {
            return "image";
        }
        return "text";
    }

    private static boolean isImageField(String type) {
        return "image".equalsIgnoreCase(String.valueOf(type));
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
