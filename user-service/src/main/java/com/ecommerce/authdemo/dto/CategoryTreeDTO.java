package com.ecommerce.authdemo.dto;

import lombok.Data;

import java.util.List;

@Data
public class CategoryTreeDTO {

    private Long id;
    private String name;
    private String image;

    private List<CategoryTreeDTO> children;

}