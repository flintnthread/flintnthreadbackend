package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.SearchResponseDTO;
import com.ecommerce.authdemo.payload.ApiResponse;
import com.ecommerce.authdemo.service.SearchService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /*
    -----------------------------------------
    🔎 MAIN PRODUCT SEARCH
    -----------------------------------------
    */
    @GetMapping
    public ResponseEntity<ApiResponse<SearchResponseDTO>> searchProducts(
            @RequestParam
            @NotBlank(message = "Keyword is required")
            String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String sessionId) {

        ApiResponse<SearchResponseDTO> response = searchService.search(keyword, userId, sessionId);
        return ResponseEntity.ok(response);
    }

    /*
    -----------------------------------------
    🔍 SEARCH SUGGESTIONS
    (Auto suggestions in search bar)
    -----------------------------------------
    */
    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<?>> getSuggestions(
            @RequestParam
            @NotBlank(message = "Keyword is required")
            String keyword) {

        ApiResponse<?> response = searchService.getSuggestions(keyword);
        return ResponseEntity.ok(response);
    }

    /*
    -----------------------------------------
    🔥 TRENDING SEARCHES
    -----------------------------------------
    */
    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<?>> getTrendingSearches() {

        ApiResponse<?> response = searchService.getTrendingSearches();
        return ResponseEntity.ok(response);
    }

    /*
    -----------------------------------------
    📄 PAGINATED SEARCH
    -----------------------------------------
    */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<Page<SearchResponseDTO>>> searchWithPagination(
            @RequestParam String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String sessionId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {

        ApiResponse<Page<SearchResponseDTO>> response =
                searchService.searchWithPagination(keyword, userId, sessionId, page, size);

        return ResponseEntity.ok(response);
    }

    /*
    -----------------------------------------
    🎤 VOICE SEARCH
    -----------------------------------------
    */
    @GetMapping("/voice")
    public ResponseEntity<ApiResponse<SearchResponseDTO>> voiceSearch(
            @RequestParam String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String sessionId) {

        ApiResponse<SearchResponseDTO> response =
                searchService.voiceSearch(keyword, userId, sessionId);

        return ResponseEntity.ok(response);

    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SearchResponseDTO>> imageSearch(
            @RequestParam("image") MultipartFile image,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String sessionId) {
        ApiResponse<SearchResponseDTO> response = searchService.imageSearch(image, userId, sessionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<String>>> getSearchHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String sessionId) {
        ApiResponse<List<String>> response = searchService.getSearchHistory(userId, sessionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/history")
    public ResponseEntity<ApiResponse<String>> recordSearchHistory(
            @RequestParam
            @NotBlank(message = "Keyword is required")
            String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String sessionId) {
        ApiResponse<String> response = searchService.recordSearchHistory(keyword, userId, sessionId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/history")
    public ResponseEntity<ApiResponse<String>> clearSearchHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String sessionId) {
        ApiResponse<String> response = searchService.clearSearchHistory(userId, sessionId);
        return ResponseEntity.ok(response);
    }
}