package com.ecommerce.authdemo.dto;

import lombok.Data;

import java.util.List;

@Data
public class SaveOrderItemCustomDetailsRequestDTO {

    private List<CustomDetailFieldValueDTO> fields;
}
