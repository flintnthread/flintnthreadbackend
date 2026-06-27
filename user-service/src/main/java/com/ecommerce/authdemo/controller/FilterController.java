package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.FilterOptionsDTO;
import com.ecommerce.authdemo.service.FilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/filters")
@RequiredArgsConstructor
public class FilterController {

    private final FilterService filterService;

    @GetMapping
    public FilterOptionsDTO getFilterOptions(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) List<Long> categoryIds
    ) {
        return filterService.getFilterOptions(categoryId, categoryIds);
    }

    @GetMapping("/categories")
    public List<FilterOptionsDTO.CategoryFilterDTO> getCategories(
            @RequestParam(required = false) Long parentId
    ) {
        return filterService.getCategories(parentId);
    }

    @GetMapping("/colors")
    public List<FilterOptionsDTO.ColorFilterDTO> getColors(
            @RequestParam(required = false) Long categoryId
    ) {
        return filterService.getColors(categoryId);
    }

    @GetMapping("/sizes")
    public List<FilterOptionsDTO.SizeFilterDTO> getSizes(
            @RequestParam(required = false) Long categoryId
    ) {
        return filterService.getSizes(categoryId);
    }

    @GetMapping("/genders")
    public List<FilterOptionsDTO.GenderFilterDTO> getGenders(
            @RequestParam(required = false) Long categoryId
    ) {
        return filterService.getGenders(categoryId);
    }

    @GetMapping("/price-ranges")
    public List<FilterOptionsDTO.PriceRangeDTO> getPriceRanges(
            @RequestParam(required = false) Long categoryId
    ) {
        return filterService.getPriceRanges(categoryId);
    }

    @GetMapping("/ratings")
    public List<FilterOptionsDTO.RatingFilterDTO> getRatings(
            @RequestParam(required = false) Long categoryId
    ) {
        return filterService.getRatings(categoryId);
    }
}
