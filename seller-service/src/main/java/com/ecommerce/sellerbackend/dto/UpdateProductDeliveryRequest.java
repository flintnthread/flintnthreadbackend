package com.ecommerce.sellerbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class UpdateProductDeliveryRequest {
    private boolean deliverAllLocations;
    private List<Integer> pincodeIds = new ArrayList<>();
}
