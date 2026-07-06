package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.FilterOptionsDTO;
import com.ecommerce.authdemo.entity.*;
import com.ecommerce.authdemo.repository.*;
import com.ecommerce.authdemo.util.ProductCatalogVisibility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FilterService {

    private final CategoryRepository categoryRepository;
    private final ColorRepository colorRepository;
    private final SizeRepository sizeRepository;
    private final ProductRepository productRepository;
    private final ProductColorRepository productColorRepository;
    private final ProductSizeRepository productSizeRepository;

    public FilterOptionsDTO getFilterOptions(Long categoryId, List<Long> categoryIds) {
        FilterOptionsDTO filterOptions = new FilterOptionsDTO();
        
        filterOptions.setCategories(getCategories(categoryId != null ? categoryId : 0));
        filterOptions.setColors(getColors(categoryId));
        filterOptions.setSizes(getSizes(categoryId));
        filterOptions.setGenders(getGenders(categoryId));
        filterOptions.setPriceRanges(getPriceRanges(categoryId));
        filterOptions.setRatings(getRatings(categoryId));
        
        return filterOptions;
    }

    public List<FilterOptionsDTO.CategoryFilterDTO> getCategories(Long parentId) {
        List<Category> categories;
        
        if (parentId == null || parentId == 0) {
            categories = categoryRepository.findByParentIdIsNull();
        } else {
            categories = categoryRepository.findByParentId(parentId);
        }

        return categories.stream()
                .map(this::convertToCategoryFilterDTO)
                .collect(Collectors.toList());
    }

    public List<FilterOptionsDTO.ColorFilterDTO> getColors(Long categoryId) {
        List<Color> colors = colorRepository.findByStatus(1);
        
        return colors.stream()
                .map(this::convertToColorFilterDTO)
                .collect(Collectors.toList());
    }

    public List<FilterOptionsDTO.SizeFilterDTO> getSizes(Long categoryId) {
        List<Size> sizes = sizeRepository.findByStatus(1);
        
        return sizes.stream()
                .map(this::convertToSizeFilterDTO)
                .collect(Collectors.toList());
    }

    public List<FilterOptionsDTO.GenderFilterDTO> getGenders(Long categoryId) {
        return List.of(
                createGenderFilter("Men", "Men"),
                createGenderFilter("Women", "Women"),
                createGenderFilter("Unisex", "Unisex"),
                createGenderFilter("Kids", "Kids")
        );
    }

    public List<FilterOptionsDTO.PriceRangeDTO> getPriceRanges(Long categoryId) {
        return List.of(
                createPriceRangeDTO("0-500", "₹0 - ₹500", 0.0, 500.0),
                createPriceRangeDTO("500-1000", "₹500 - ₹1,000", 500.0, 1000.0),
                createPriceRangeDTO("1000-2000", "₹1,000 - ₹2,000", 1000.0, 2000.0),
                createPriceRangeDTO("2000-5000", "₹2,000 - ₹5,000", 2000.0, 5000.0),
                createPriceRangeDTO("5000+", "₹5,000+", 5000.0, 999999.0)
        );
    }

    public List<FilterOptionsDTO.RatingFilterDTO> getRatings(Long categoryId) {
        return List.of(
                createRatingFilterDTO(4, "4★ & above"),
                createRatingFilterDTO(3, "3★ & above"),
                createRatingFilterDTO(2, "2★ & above"),
                createRatingFilterDTO(1, "1★ & above")
        );
    }

    private FilterOptionsDTO.CategoryFilterDTO convertToCategoryFilterDTO(Category category) {
        FilterOptionsDTO.CategoryFilterDTO dto = new FilterOptionsDTO.CategoryFilterDTO();
        dto.setId(category.getId());
        dto.setName(category.getCategoryName());
        dto.setImage(category.getImage());
        dto.setParentId(category.getParentId());
        dto.setProductCount(countProductsByCategory(category.getId()));
        return dto;
    }

    private FilterOptionsDTO.ColorFilterDTO convertToColorFilterDTO(Color color) {
        FilterOptionsDTO.ColorFilterDTO dto = new FilterOptionsDTO.ColorFilterDTO();
        dto.setId(color.getId());
        dto.setName(color.getName());
        dto.setCode(color.getCode());
        dto.setHex(color.getHex());
        dto.setProductCount(countProductsByColor(color.getId()));
        return dto;
    }

    private FilterOptionsDTO.SizeFilterDTO convertToSizeFilterDTO(Size size) {
        FilterOptionsDTO.SizeFilterDTO dto = new FilterOptionsDTO.SizeFilterDTO();
        dto.setId(size.getId());
        dto.setName(size.getName());
        dto.setCode(size.getCode());
        dto.setProductCount(countProductsBySize(size.getId()));
        return dto;
    }

    private FilterOptionsDTO.GenderFilterDTO createGenderFilter(String value, String label) {
        FilterOptionsDTO.GenderFilterDTO dto = new FilterOptionsDTO.GenderFilterDTO();
        dto.setValue(value);
        dto.setLabel(label);
        dto.setProductCount(countProductsByGender(value));
        return dto;
    }

    private FilterOptionsDTO.PriceRangeDTO createPriceRangeDTO(String id, String label, Double min, Double max) {
        FilterOptionsDTO.PriceRangeDTO dto = new FilterOptionsDTO.PriceRangeDTO();
        dto.setId(id);
        dto.setLabel(label);
        dto.setMin(min);
        dto.setMax(max);
        dto.setProductCount(countProductsByPriceRange(min, max));
        return dto;
    }

    private FilterOptionsDTO.RatingFilterDTO createRatingFilterDTO(Integer value, String label) {
        FilterOptionsDTO.RatingFilterDTO dto = new FilterOptionsDTO.RatingFilterDTO();
        dto.setValue(value);
        dto.setLabel(label);
        dto.setProductCount(countProductsByRating(value));
        return dto;
    }

    // Helper methods to count products (simplified implementations)
    private Integer countProductsByCategory(Long categoryId) {
        Long count = productRepository.countByCategoryIdAndStatus(
                categoryId, ProductCatalogVisibility.USER_VISIBLE_STATUS);
        return count != null ? count.intValue() : 0;
    }

    private Integer countProductsByColor(Long colorId) {
        Long count = productColorRepository.countByColorId(colorId);
        return count != null ? count.intValue() : 0;
    }

    private Integer countProductsBySize(Long sizeId) {
        Long count = productSizeRepository.countBySizeId(sizeId);
        return count != null ? count.intValue() : 0;
    }

    private Integer countProductsByGender(String gender) {
        Long count = productRepository.countByGender(gender);
        return count != null ? count.intValue() : 0;
    }

    private Integer countProductsByPriceRange(Double min, Double max) {
        Long count = productRepository.countByPriceRange(min, max);
        return count != null ? count.intValue() : 0;
    }

    private Integer countProductsByRating(Integer rating) {
        Long count = productRepository.countByMinRating(rating);
        return count != null ? count.intValue() : 0;
    }
}
