package com.ecommerce.sellerbackend.controller;

import com.ecommerce.sellerbackend.dto.GstVerifyResponse;
import com.ecommerce.sellerbackend.dto.IfscLookupResponse;
import com.ecommerce.sellerbackend.service.IfscLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/seller/util")
@RequiredArgsConstructor
public class UtilController {

    private static final Pattern GST_PATTERN =
            Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");

    private final IfscLookupService ifscLookupService;

    @GetMapping("/ifsc/{code}")
    public IfscLookupResponse lookupIfsc(@PathVariable String code) {
        com.ecommerce.sellerbackend.dto.profile.IfscLookupResponse lookup = ifscLookupService.lookup(code);
        return IfscLookupResponse.builder()
                .ifsc(lookup.getIfscCode())
                .bank(lookup.getBankName())
                .branch(lookup.getBranchName())
                .found(lookup.isFound())
                .city("")
                .state("")
                .build();
    }

    @GetMapping("/verify-gst")
    public GstVerifyResponse verifyGst(@RequestParam String gstNumber) {
        String gst = gstNumber != null ? gstNumber.trim().toUpperCase() : "";
        if (gst.isEmpty()) {
            return GstVerifyResponse.builder()
                    .valid(false)
                    .verified(false)
                    .message("GST number is required")
                    .build();
        }
        if (gst.length() != 15) {
            return GstVerifyResponse.builder()
                    .valid(false)
                    .verified(false)
                    .message("GST must be 15 characters")
                    .build();
        }
        boolean valid = GST_PATTERN.matcher(gst).matches();
        return GstVerifyResponse.builder()
                .valid(valid)
                .verified(valid)
                .message(valid ? "GST format verified" : "Invalid GST format")
                .build();
    }
}
