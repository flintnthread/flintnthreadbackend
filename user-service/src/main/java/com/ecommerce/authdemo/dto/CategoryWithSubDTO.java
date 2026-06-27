package com.ecommerce.authdemo.dto;

import lombok.Data;
import java.util.List;

@Data
public class CategoryWithSubDTO {

    private String categoryName;
    private String mobileImage; // ✅ ADD

    private List<SubCategoryResponseDTO> subcategories;

}