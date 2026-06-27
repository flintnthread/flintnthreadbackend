package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.ApiResponse;
import com.ecommerce.authdemo.dto.TermsConditionsRequest;
import com.ecommerce.authdemo.dto.TermsConditionsResponse;
import com.ecommerce.authdemo.service.TermsConditionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/terms-conditions")
@RequiredArgsConstructor
public class TermsConditionsController {

    private final TermsConditionsService termsConditionsService;

    @GetMapping
    public ResponseEntity<ApiResponse<TermsConditionsResponse>> getTermsConditions() {
        TermsConditionsResponse response = termsConditionsService.getLatest();
        return ResponseEntity.ok(new ApiResponse<>(true, "Terms and conditions fetched successfully", response));
    }

    @PutMapping("/admin")
    public ResponseEntity<ApiResponse<TermsConditionsResponse>> upsertTermsConditions(
            @RequestBody TermsConditionsRequest request) {
        TermsConditionsResponse response = termsConditionsService.upsert(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Terms and conditions updated successfully", response));
    }
}
