package com.ecommerce.authdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCustomDetailsResponseDTO {

    private Long orderId;
    private boolean hasCustomizableItems;
    private boolean allDetailsComplete;
    private List<OrderCustomDetailsItemDTO> items;
}
