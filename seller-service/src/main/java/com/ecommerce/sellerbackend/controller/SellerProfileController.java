package com.ecommerce.sellerbackend.controller;

import com.ecommerce.sellerbackend.dto.profile.AddressProfileRequest;
import com.ecommerce.sellerbackend.dto.profile.BankingProfileRequest;
import com.ecommerce.sellerbackend.dto.profile.BusinessProfileRequest;
import com.ecommerce.sellerbackend.dto.profile.CompanyPanRequest;
import com.ecommerce.sellerbackend.dto.profile.DocumentUploadResponse;
import com.ecommerce.sellerbackend.dto.profile.GstVerifyRequest;
import com.ecommerce.sellerbackend.dto.profile.GstVerifyResponse;
import com.ecommerce.sellerbackend.dto.profile.IfscLookupResponse;
import com.ecommerce.sellerbackend.dto.profile.ProfileSubmitResponse;
import com.ecommerce.sellerbackend.dto.profile.RegistrationPaymentOrderResponse;
import com.ecommerce.sellerbackend.dto.profile.RegistrationPaymentStatusResponse;
import com.ecommerce.sellerbackend.dto.profile.RegistrationPaymentVerifyRequest;
import com.ecommerce.sellerbackend.dto.profile.SellerProfileResponse;
import com.ecommerce.sellerbackend.profile.SellerDocumentType;
import com.ecommerce.sellerbackend.security.AuthenticatedSellerResolver;
import com.ecommerce.sellerbackend.service.SellerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/seller/profile")
@RequiredArgsConstructor
public class SellerProfileController {

    public static final String SELLER_ID_HEADER = "X-Seller-Id";

    private final SellerProfileService sellerProfileService;
    private final AuthenticatedSellerResolver authenticatedSellerResolver;

    @GetMapping
    public SellerProfileResponse getProfile(@RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        return sellerProfileService.getProfile(authenticatedSellerResolver.requireCurrentSellerId(sellerId));
    }

    @PostMapping(value = "/personal/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SellerProfileResponse uploadProfilePhoto(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @RequestParam("file") MultipartFile file) throws IOException {
        return sellerProfileService.uploadProfilePhoto(authenticatedSellerResolver.requireCurrentSellerId(sellerId), file);
    }

    @PutMapping("/business")
    public SellerProfileResponse updateBusiness(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @Valid @RequestBody BusinessProfileRequest request) {
        return sellerProfileService.updateBusiness(authenticatedSellerResolver.requireCurrentSellerId(sellerId), request);
    }

    @PutMapping("/address")
    public SellerProfileResponse updateAddress(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @Valid @RequestBody AddressProfileRequest request) {
        return sellerProfileService.updateAddress(authenticatedSellerResolver.requireCurrentSellerId(sellerId), request);
    }

    @PutMapping("/banking")
    public SellerProfileResponse updateBanking(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @Valid @RequestBody BankingProfileRequest request) {
        return sellerProfileService.updateBanking(authenticatedSellerResolver.requireCurrentSellerId(sellerId), request);
    }

    @PutMapping("/company-pan")
    public SellerProfileResponse updateCompanyPan(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @Valid @RequestBody CompanyPanRequest request) {
        return sellerProfileService.updateCompanyPan(authenticatedSellerResolver.requireCurrentSellerId(sellerId), request);
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentUploadResponse uploadDocument(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @RequestParam("type") String type,
            @RequestParam("file") MultipartFile file) throws IOException {
        SellerDocumentType documentType = SellerDocumentType.fromParam(type)
                .orElseThrow(() -> new IllegalArgumentException("Unknown document type: " + type));
        return sellerProfileService.uploadDocument(authenticatedSellerResolver.requireCurrentSellerId(sellerId), documentType, file);
    }

    @PostMapping("/gst/verify")
    public GstVerifyResponse verifyGst(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @Valid @RequestBody GstVerifyRequest request) {
        return sellerProfileService.verifyGst(authenticatedSellerResolver.requireCurrentSellerId(sellerId), request);
    }

    @GetMapping("/ifsc/{code}")
    public IfscLookupResponse lookupIfsc(@PathVariable("code") String code) {
        return sellerProfileService.lookupIfsc(code);
    }

    @PostMapping("/submit")
    public ResponseEntity<ProfileSubmitResponse> submitProfile(@RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        ProfileSubmitResponse response = sellerProfileService.submitProfile(
                authenticatedSellerResolver.requireCurrentSellerId(sellerId));
        if (!response.isSubmitted()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/registration-payment/order")
    public RegistrationPaymentOrderResponse createRegistrationPaymentOrder(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        return sellerProfileService.createRegistrationPaymentOrder(
                authenticatedSellerResolver.requireCurrentSellerId(sellerId));
    }

    @PostMapping("/registration-payment/verify")
    public RegistrationPaymentStatusResponse verifyRegistrationPayment(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @Valid @RequestBody RegistrationPaymentVerifyRequest request) {
        return sellerProfileService.verifyRegistrationPayment(
                authenticatedSellerResolver.requireCurrentSellerId(sellerId),
                request);
    }

    @PostMapping("/registration-payment/resend-invoice")
    public RegistrationPaymentStatusResponse resendRegistrationInvoiceEmail(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        return sellerProfileService.resendRegistrationInvoiceEmail(
                authenticatedSellerResolver.requireCurrentSellerId(sellerId));
    }

    @GetMapping("/registration-payment/status")
    public RegistrationPaymentStatusResponse getRegistrationPaymentStatus(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId) {
        return sellerProfileService.getRegistrationPaymentStatus(
                authenticatedSellerResolver.requireCurrentSellerId(sellerId));
    }
}
