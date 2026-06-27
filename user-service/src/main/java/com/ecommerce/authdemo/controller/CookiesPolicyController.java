package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.ApiResponse;
import com.ecommerce.authdemo.dto.CookiesPolicyRequest;
import com.ecommerce.authdemo.dto.CookiesPolicyResponse;
import com.ecommerce.authdemo.service.CookiesPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cookies-policy")
@RequiredArgsConstructor
public class CookiesPolicyController {

    private final CookiesPolicyService cookiesPolicyService;

    @GetMapping
    public ResponseEntity<ApiResponse<CookiesPolicyResponse>> getCookiesPolicy() {
        CookiesPolicyResponse response = cookiesPolicyService.getLatest();
        return ResponseEntity.ok(new ApiResponse<>(true, "Cookies policy fetched successfully", response));
    }

    @PutMapping("/admin")
    public ResponseEntity<ApiResponse<CookiesPolicyResponse>> upsertCookiesPolicy(
            @Valid @RequestBody CookiesPolicyRequest request) {
        CookiesPolicyResponse response = cookiesPolicyService.upsert(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cookies policy updated successfully", response));
    }
}
