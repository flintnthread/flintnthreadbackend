package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.SearchResponseDTO;
import com.ecommerce.authdemo.payload.ApiResponse;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SearchService {

    /*
    -----------------------------------------
    🔎 MAIN SEARCH
    -----------------------------------------
    */
    ApiResponse<SearchResponseDTO> search(String keyword, Long userId, String sessionId);

    /*
    -----------------------------------------
    🔍 SEARCH SUGGESTIONS
    (Auto complete search bar)
    -----------------------------------------
    */
    ApiResponse<List<String>> getSuggestions(String keyword);

    /*
    -----------------------------------------
    🔥 TRENDING SEARCHES
    -----------------------------------------
    */
    ApiResponse<List<String>> getTrendingSearches();

    /*
    -----------------------------------------
    📄 PAGINATED SEARCH
    -----------------------------------------
    */
    ApiResponse<Page<SearchResponseDTO>> searchWithPagination(
            String keyword,
            Long userId,
            String sessionId,
            int page,
            int size
    );

    /*
    -----------------------------------------
    🎤 VOICE SEARCH
    -----------------------------------------
    */
    ApiResponse<SearchResponseDTO> voiceSearch(String keyword, Long userId, String sessionId);

    ApiResponse<SearchResponseDTO> imageSearch(MultipartFile image, Long userId, String sessionId);

    ApiResponse<List<String>> getSearchHistory(Long userId, String sessionId);

    ApiResponse<String> clearSearchHistory(Long userId, String sessionId);

    ApiResponse<String> recordSearchHistory(String keyword, Long userId, String sessionId);

}