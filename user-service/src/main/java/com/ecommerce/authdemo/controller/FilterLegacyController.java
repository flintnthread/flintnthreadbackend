package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.FilterOptionsDTO;
import com.ecommerce.authdemo.service.FilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Legacy mobile paths expected by the app ({@code /api/prices}, {@code /api/rating}, {@code /api/gender}).
 * Delegates to {@link FilterService} — same data as {@code /api/filters/*}.
 */
@RestController
@RequiredArgsConstructor
public class FilterLegacyController {

    private final FilterService filterService;

    @GetMapping("/api/prices")
    public List<FilterOptionsDTO.PriceRangeDTO> getPrices(
            @RequestParam(required = false) Long categoryId
    ) {
        return filterService.getPriceRanges(categoryId);
    }

    @GetMapping("/api/rating")
    public List<FilterOptionsDTO.RatingFilterDTO> getRating(
            @RequestParam(required = false) Long categoryId
    ) {
        return filterService.getRatings(categoryId);
    }

    @GetMapping("/api/gender")
    public List<FilterOptionsDTO.GenderFilterDTO> getGender(
            @RequestParam(required = false) Long categoryId
    ) {
        return filterService.getGenders(categoryId);
    }
}
