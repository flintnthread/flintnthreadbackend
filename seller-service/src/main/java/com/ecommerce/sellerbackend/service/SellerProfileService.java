package com.ecommerce.sellerbackend.service;

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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface SellerProfileService {

    SellerProfileResponse getProfile(Long sellerId);

    SellerProfileResponse uploadProfilePhoto(Long sellerId, MultipartFile file) throws IOException;

    SellerProfileResponse updateBusiness(Long sellerId, BusinessProfileRequest request);

    SellerProfileResponse updateAddress(Long sellerId, AddressProfileRequest request);

    SellerProfileResponse updateBanking(Long sellerId, BankingProfileRequest request);

    DocumentUploadResponse uploadDocument(Long sellerId, SellerDocumentType type, MultipartFile file)
            throws IOException;

    SellerProfileResponse updateCompanyPan(Long sellerId, CompanyPanRequest request);

    GstVerifyResponse verifyGst(Long sellerId, GstVerifyRequest request);

    IfscLookupResponse lookupIfsc(String ifscCode);

    RegistrationPaymentOrderResponse createRegistrationPaymentOrder(Long sellerId);

    RegistrationPaymentStatusResponse verifyRegistrationPayment(Long sellerId, RegistrationPaymentVerifyRequest request);

    RegistrationPaymentStatusResponse getRegistrationPaymentStatus(Long sellerId);

    ProfileSubmitResponse submitProfile(Long sellerId);
}
