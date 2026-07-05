package com.ecommerce.sellerbackend.service.impl;

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
import com.ecommerce.sellerbackend.entity.Seller;
import com.ecommerce.sellerbackend.entity.SellerAccountStatus;
import com.ecommerce.sellerbackend.entity.SellerCategory;
import com.ecommerce.sellerbackend.entity.SellerKycImage;
import com.ecommerce.sellerbackend.exception.ResourceNotFoundException;
import com.ecommerce.sellerbackend.profile.SellerDocumentType;
import com.ecommerce.sellerbackend.profile.SellerProfileValidator;
import com.ecommerce.sellerbackend.repository.SellerKycImageRepository;
import com.ecommerce.sellerbackend.repository.SellerRegistrationInvoiceRepository;
import com.ecommerce.sellerbackend.repository.SellerRegistrationPaymentRepository;
import com.ecommerce.sellerbackend.repository.SellerRepository;
import com.ecommerce.sellerbackend.service.GstVerificationService;
import com.ecommerce.sellerbackend.service.IfscLookupService;
import com.ecommerce.sellerbackend.service.MailService;
import com.ecommerce.sellerbackend.service.MediaStorageService;
import com.ecommerce.sellerbackend.service.RegistrationInvoicePdfService;
import com.ecommerce.sellerbackend.service.SellerGstDetailsService;
import com.ecommerce.sellerbackend.service.SellerProfileService;
import com.ecommerce.sellerbackend.service.SellerUniqueIdService;
import com.ecommerce.sellerbackend.util.RegistrationReferenceNumberHelper;
import com.ecommerce.sellerbackend.util.SellerAccountStatusHelper;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerProfileServiceImpl implements SellerProfileService {

    private final SellerRepository sellerRepository;
    private final SellerKycImageRepository sellerKycImageRepository;
    private final MediaStorageService mediaStorageService;
    private final GstVerificationService gstVerificationService;
    private final IfscLookupService ifscLookupService;
    private final SellerGstDetailsService sellerGstDetailsService;
    private final SellerRegistrationPaymentRepository registrationPaymentRepository;
    private final SellerRegistrationInvoiceRepository registrationInvoiceRepository;
    private final MailService mailService;
    private final RegistrationInvoicePdfService registrationInvoicePdfService;
    private final SellerUniqueIdService sellerUniqueIdService;

    @Value("${razorpay.key_id}")
    private String razorpayKeyId;

    @Value("${razorpay.key_secret}")
    private String razorpayKeySecret;

    @Value("${app.registration.fee.inr:899}")
    private int registrationFeeInr;

    @Value("${app.registration.gst.percent:18}")
    private int registrationGstPercent;

    @Value("${app.registration.fee.currency:INR}")
    private String registrationFeeCurrency;

    @Override
    @Transactional
    public SellerProfileResponse getProfile(Long sellerId) {
        Seller seller = requireSeller(sellerId);
        sellerUniqueIdService.ensureSellerUniqueId(seller);
        return SellerProfileResponse.from(seller, mediaStorageService);
    }

    @Override
    @Transactional
    public SellerProfileResponse uploadProfilePhoto(Long sellerId, MultipartFile file) throws IOException {
        Seller seller = requireSeller(sellerId);
        MediaStorageService.StoredFile stored =
                mediaStorageService.storeSellerDocument(sellerId, SellerDocumentType.PROFILE_PIC, file);
        seller.setProfilePic(stored.fileName());
        touchProfile(seller);
        sellerRepository.save(seller);
        return SellerProfileResponse.from(seller, mediaStorageService);
    }

    @Override
    @Transactional
    public SellerProfileResponse updateBusiness(Long sellerId, BusinessProfileRequest request) {
        Seller seller = requireSeller(sellerId);

        SellerCategory category = parseCategory(request.getBusinessCategory());
        seller.setSellerCategory(category);
        seller.setBusinessName(request.getBusinessName().trim());
        seller.setBusinessType(request.getBusinessType().trim());
        seller.setAddress(request.getAddress().trim());

        boolean hasGst = category == SellerCategory.b2b || Boolean.TRUE.equals(request.getHasGst());
        seller.setHasGst(hasGst);

        if (hasGst) {
            String gst = request.getGstNumber() != null ? request.getGstNumber().trim().toUpperCase(Locale.ROOT) : "";
            String gstError = SellerProfileValidator.validateGst(gst);
            if (gstError != null) {
                throw new IllegalArgumentException(gstError);
            }
            if (!Boolean.TRUE.equals(request.getGstVerified())) {
                throw new IllegalArgumentException("Please verify your GST number before continuing.");
            }
            if (gstVerificationService.isGstRegisteredToAnotherSeller(gst, sellerId)) {
                throw new IllegalArgumentException(
                        "This GSTIN is already registered with another seller account.");
            }
            seller.setGstNumber(gst);
            seller.setGstType(trimOrNull(request.getGstType()));
        } else {
            seller.setGstNumber(null);
            seller.setGstType(null);
        }

        String pan = request.getPanNumber().trim().toUpperCase(Locale.ROOT);
        String panError = SellerProfileValidator.validatePan(pan);
        if (panError != null) {
            throw new IllegalArgumentException(panError);
        }
        seller.setPanNumber(pan);

        String aadhaar = SellerProfileValidator.normalizeAadhaar(request.getAadhaarNumber());
        if (aadhaar.isEmpty() || isMaskedAadhaarInput(request.getAadhaarNumber())) {
            String existingError = SellerProfileValidator.validateAadhaar(seller.getAadhaarNumber());
            if (existingError != null) {
                throw new IllegalArgumentException("Aadhaar number is required");
            }
        } else {
            String aadhaarError = SellerProfileValidator.validateAadhaar(aadhaar);
            if (aadhaarError != null) {
                throw new IllegalArgumentException(aadhaarError);
            }
            seller.setAadhaarNumber(aadhaar);
        }

        touchProfile(seller);
        sellerRepository.save(seller);
        return SellerProfileResponse.from(seller, mediaStorageService);
    }

    @Override
    @Transactional
    public SellerProfileResponse updateAddress(Long sellerId, AddressProfileRequest request) {
        Seller seller = requireSeller(sellerId);

        seller.setAddress(request.getStreetAddress().trim());
        seller.setLandmark(request.getLandmark().trim());
        seller.setCity(request.getCity().trim());
        seller.setState(request.getState().trim());
        seller.setArea(request.getArea().trim());
        seller.setCountry(request.getCountry().trim());

        String pinError = SellerProfileValidator.validatePincode(request.getPincode());
        if (pinError != null) {
            throw new IllegalArgumentException(pinError);
        }
        seller.setPincode(request.getPincode().trim());

        if (Boolean.TRUE.equals(request.getWarehouseDifferent())) {
            validateWarehouse(request);
            seller.setWarehouseAddress(buildWarehouseAddress(request));
            seller.setWarehouseArea(trimOrNull(request.getWarehouseArea()));
            seller.setWarehouseCity(trimOrNull(request.getWarehouseCity()));
            seller.setWarehouseState(trimOrNull(request.getWarehouseState()));
            seller.setWarehouseCountry(trimOrNull(request.getWarehouseCountry()));
        } else {
            seller.setWarehouseAddress(null);
            seller.setWarehouseArea(null);
            seller.setWarehouseCity(null);
            seller.setWarehouseState(null);
            seller.setWarehouseCountry(null);
        }

        touchProfile(seller);
        sellerRepository.save(seller);
        return SellerProfileResponse.from(seller, mediaStorageService);
    }

    @Override
    @Transactional
    public SellerProfileResponse updateBanking(Long sellerId, BankingProfileRequest request) {
        Seller seller = requireSeller(sellerId);

        String ifsc = request.getIfscCode().trim().toUpperCase(Locale.ROOT);
        String ifscError = SellerProfileValidator.validateIfsc(ifsc);
        if (ifscError != null) {
            throw new IllegalArgumentException(ifscError);
        }
        seller.setIfscCode(ifsc);

        IfscLookupResponse lookup = lookupIfsc(ifsc);
        if (!lookup.isFound() && isBlank(request.getBankName())) {
            throw new IllegalArgumentException("IFSC code not found. Please enter a valid IFSC code.");
        }
        String bankName = trimOrNull(request.getBankName());
        String branchName = trimOrNull(request.getBranchName());
        seller.setBankName(bankName != null ? bankName : lookup.getBankName());
        seller.setBranchName(branchName != null ? branchName : lookup.getBranchName());

        String holderError = SellerProfileValidator.validateAccountHolder(request.getAccountHolderName());
        if (holderError != null) {
            throw new IllegalArgumentException(holderError);
        }
        seller.setAccountHolder(request.getAccountHolderName().trim());

        String accError = SellerProfileValidator.validateAccountNumber(request.getAccountNumber());
        if (accError != null) {
            throw new IllegalArgumentException(accError);
        }
        seller.setAccountNumber(request.getAccountNumber().trim());

        touchProfile(seller);
        sellerRepository.save(seller);
        return SellerProfileResponse.from(seller, mediaStorageService);
    }

    @Override
    @Transactional
    public DocumentUploadResponse uploadDocument(Long sellerId, SellerDocumentType type, MultipartFile file)
            throws IOException {
        Seller seller = requireSeller(sellerId);
        MediaStorageService.StoredFile stored = mediaStorageService.storeSellerDocument(sellerId, type, file);
        applyDocument(seller, type, stored.fileName());

        if (type == SellerDocumentType.LIVE_SELFIE) {
            saveKycSelfie(sellerId, stored.fileName());
        }

        touchProfile(seller);
        sellerRepository.save(seller);

        return DocumentUploadResponse.builder()
                .documentType(type.name())
                .fileName(stored.fileName())
                .url(stored.publicUrl())
                .build();
    }

    @Override
    @Transactional
    public SellerProfileResponse updateCompanyPan(Long sellerId, CompanyPanRequest request) {
        Seller seller = requireSeller(sellerId);
        if (seller.getSellerCategory() != SellerCategory.b2b) {
            throw new IllegalArgumentException("Company PAN is only required for B2B sellers.");
        }
        String pan = request.getCompanyPanNumber().trim().toUpperCase(Locale.ROOT);
        String error = SellerProfileValidator.validateCompanyPan(pan);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        seller.setCompanyPan(pan);
        touchProfile(seller);
        sellerRepository.save(seller);
        return SellerProfileResponse.from(seller, mediaStorageService);
    }

    @Override
    @Transactional
    public GstVerifyResponse verifyGst(Long sellerId, GstVerifyRequest request) {
        Seller seller = requireSeller(sellerId);
        String gst = request.getGstNumber().trim().toUpperCase(Locale.ROOT);
        String error = SellerProfileValidator.validateGst(gst);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }

        GstVerifyResponse response = gstVerificationService.verify(gst, sellerId);
        if (!response.isVerified()) {
            return response;
        }

        seller.setGstNumber(gst);
        seller.setHasGst(true);
        if (isBlank(seller.getBusinessName()) && !isBlank(response.getBusinessName())) {
            seller.setBusinessName(response.getBusinessName());
        }
        if (isBlank(seller.getBusinessType()) && !isBlank(response.getBusinessType())) {
            seller.setBusinessType(response.getBusinessType());
        }
        if (isBlank(seller.getPanNumber()) && !isBlank(response.getPanNumber())) {
            seller.setPanNumber(response.getPanNumber());
        }
        if (isBlank(seller.getAddress()) && !isBlank(response.getAddress())) {
            seller.setAddress(response.getAddress());
        }
        if (isBlank(seller.getCity()) && !isBlank(response.getCity())) {
            seller.setCity(response.getCity());
        }
        if (isBlank(seller.getState()) && !isBlank(response.getState())) {
            seller.setState(response.getState());
        }
        if (isBlank(seller.getPincode()) && !isBlank(response.getPincode())) {
            seller.setPincode(response.getPincode());
        }
        touchProfile(seller);
        sellerRepository.save(seller);

        sellerGstDetailsService.saveOrUpdate(sellerId.intValue(), response);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public IfscLookupResponse lookupIfsc(String ifscCode) {
        return ifscLookupService.lookup(ifscCode);
    }

    @Override
    @Transactional
    public ProfileSubmitResponse submitProfile(Long sellerId) {
        Seller seller = requireSeller(sellerId);

        if (!registrationPaymentRepository.isPaid(sellerId)) {
            return ProfileSubmitResponse.builder()
                    .submitted(false)
                    .profileCompleted(false)
                    .message("Please complete the registration payment to submit your profile.")
                    .errors(List.of("Registration payment of Rs " + registrationFeeInr + " is pending."))
                    .accountStatus(SellerAccountStatusHelper.build(seller))
                    .build();
        }
        List<String> errors = SellerProfileValidator.validateForSubmit(seller);
        if (!errors.isEmpty()) {
            return ProfileSubmitResponse.builder()
                    .submitted(false)
                    .profileCompleted(false)
                    .message("Profile is incomplete. Please complete all required steps.")
                    .errors(errors)
                    .accountStatus(SellerAccountStatusHelper.build(seller))
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();
        seller.setProfileCompleted(true);
        seller.setProfileNeedsVerification(true);
        seller.setStatus(SellerAccountStatus.pending);
        seller.setKycCompleted(false);
        seller.setAdminRemarks(null);
        seller.setKycRemarks(null);
        seller.setKycSubmittedAt(now);
        seller.setProfileUpdatedAt(now);
        seller.setUpdatedAt(now);
        sellerRepository.save(seller);

        return ProfileSubmitResponse.builder()
                .submitted(true)
                .profileCompleted(true)
                .message("Your seller profile has been submitted for verification. "
                        + "Our team will review your documents within "
                        + SellerAccountStatusHelper.REVIEW_ESTIMATE_HOURS + " hours.")
                .errors(List.of())
                .accountStatus(SellerAccountStatusHelper.build(seller))
                .build();
    }

    @Override
    @Transactional
    public RegistrationPaymentOrderResponse createRegistrationPaymentOrder(Long sellerId) {
        Seller seller = requireSeller(sellerId);
        registrationPaymentRepository.ensureTable();
        SellerRegistrationPaymentRepository.PaymentRecord existing =
                registrationPaymentRepository.findBySellerId(sellerId);
        double gstAmount = registrationFeeInr * registrationGstPercent / 100.0;
        double totalAmount = registrationFeeInr + gstAmount;
        int expectedAmountPaise = (int) Math.round(totalAmount * 100);
        if (existing != null && registrationPaymentRepository.isSubscriptionActive(sellerId)) {
            return RegistrationPaymentOrderResponse.builder()
                    .keyId(razorpayKeyId)
                    .orderId(existing.getOrderId())
                    .amount(expectedAmountPaise)
                    .registrationFee((double) registrationFeeInr)
                    .gstAmount(gstAmount)
                    .totalAmount(totalAmount)
                    .currency(existing.getCurrency())
                    .receipt(existing.getReceipt())
                    .paid(true)
                    .build();
        }

        if (existing != null
                && "pending".equalsIgnoreCase(existing.getStatus())
                && existing.getOrderId() != null
                && existing.getAmount() == expectedAmountPaise) {
            return RegistrationPaymentOrderResponse.builder()
                    .keyId(razorpayKeyId)
                    .orderId(existing.getOrderId())
                    .amount(expectedAmountPaise)
                    .registrationFee((double) registrationFeeInr)
                    .gstAmount(gstAmount)
                    .totalAmount(totalAmount)
                    .currency(existing.getCurrency() != null ? existing.getCurrency() : registrationFeeCurrency)
                    .receipt(existing.getReceipt())
                    .paid(false)
                    .build();
        }

        String displayOrderNumber = RegistrationReferenceNumberHelper.buildDisplayOrderNumber(
                seller.getId(),
                LocalDate.now());
        String receipt = "seller-reg-" + sellerId + "-" + System.currentTimeMillis();
        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject req = new JSONObject();
            req.put("amount", expectedAmountPaise);
            req.put("currency", registrationFeeCurrency);
            req.put("receipt", receipt);
            req.put("payment_capture", 1);
            Order order = client.orders.create(req);
            String razorpayOrderId = order.get("id");
            registrationPaymentRepository.saveOrUpdateOrder(
                    sellerId, expectedAmountPaise, registrationFeeCurrency, razorpayOrderId, receipt, displayOrderNumber);
            return RegistrationPaymentOrderResponse.builder()
                    .keyId(razorpayKeyId)
                    .orderId(razorpayOrderId)
                    .amount(expectedAmountPaise)
                    .registrationFee((double) registrationFeeInr)
                    .gstAmount(gstAmount)
                    .totalAmount(totalAmount)
                    .currency(registrationFeeCurrency)
                    .receipt(receipt)
                    .paid(false)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not create payment order. Error: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public RegistrationPaymentStatusResponse verifyRegistrationPayment(Long sellerId, RegistrationPaymentVerifyRequest request) {
        Seller seller = requireSeller(sellerId);
        registrationPaymentRepository.ensureTable();
        SellerRegistrationPaymentRepository.PaymentRecord record = registrationPaymentRepository.findBySellerId(sellerId);
        if (record == null || record.getOrderId() == null) {
            throw new IllegalArgumentException("Payment order not found. Please start payment again.");
        }
        if (!record.getOrderId().equals(request.getRazorpayOrderId())) {
            throw new IllegalArgumentException("Payment order mismatch. Please start payment again.");
        }
        if (!isValidRazorpaySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature())) {
            throw new IllegalArgumentException("Payment signature verification failed.");
        }

        String displayOrderNumber = resolveDisplayOrderNumber(seller, record);
        String invoiceNumber = RegistrationReferenceNumberHelper.buildInvoiceNumber(
                seller.getId(),
                java.time.Year.now().getValue());

        registrationPaymentRepository.markPaid(
                sellerId,
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature(),
                invoiceNumber);
        SellerRegistrationPaymentRepository.PaymentRecord paidRecord = registrationPaymentRepository.findBySellerId(sellerId);

        double gstAmount = registrationFeeInr * registrationGstPercent / 100.0;
        double totalAmount = registrationFeeInr + gstAmount;
        int expectedAmountPaise = (int) Math.round(totalAmount * 100);

        String paidAtText = RegistrationReferenceNumberHelper.formatInvoiceDate(paidRecord.getPaidAt());
        byte[] invoicePdf = registrationInvoicePdfService.generateRegistrationInvoice(
                seller,
                invoiceNumber,
                request.getRazorpayPaymentId(),
                displayOrderNumber,
                expectedAmountPaise,
                paidAtText
        );
        registrationInvoiceRepository.saveInvoice(
                sellerId,
                invoiceNumber,
                displayOrderNumber,
                request.getRazorpayPaymentId(),
                expectedAmountPaise,
                paidRecord.getCurrency(),
                paidRecord.getPaidAt() != null ? paidRecord.getPaidAt() : LocalDateTime.now(),
                invoicePdf
        );
        boolean invoiceEmailSent = sendRegistrationInvoiceEmail(
                seller,
                invoiceNumber,
                displayOrderNumber,
                request.getRazorpayPaymentId(),
                expectedAmountPaise,
                invoicePdf
        );
        return RegistrationPaymentStatusResponse.builder()
                .paid(true)
                .subscriptionActive(true)
                .paymentPending(false)
                .orderId(displayOrderNumber)
                .paymentId(request.getRazorpayPaymentId())
                .paidAt(paidRecord.getPaidAt() != null ? paidRecord.getPaidAt().toString() : null)
                .subscriptionExpiresAt(paidRecord.getSubscriptionExpiresAt() != null
                        ? paidRecord.getSubscriptionExpiresAt().toString()
                        : null)
                .amount(expectedAmountPaise)
                .registrationFee(registrationFeeInr)
                .gstAmount(gstAmount)
                .totalAmount(totalAmount)
                .currency(paidRecord.getCurrency())
                .invoiceEmailSent(invoiceEmailSent)
                .build();
    }

    @Override
    @Transactional
    public RegistrationPaymentStatusResponse resendRegistrationInvoiceEmail(Long sellerId) {
        Seller seller = requireSeller(sellerId);
        if (seller.getEmail() == null || seller.getEmail().isBlank()) {
            throw new IllegalArgumentException("Your profile email is missing. Please update personal info before requesting the invoice.");
        }

        registrationPaymentRepository.ensureTable();
        registrationInvoiceRepository.ensureTable();
        if (!registrationPaymentRepository.hasEverPaid(sellerId)) {
            throw new IllegalArgumentException("No paid registration found.");
        }

        SellerRegistrationPaymentRepository.PaymentRecord paidRecord = registrationPaymentRepository.findBySellerId(sellerId);
        if (paidRecord == null) {
            throw new IllegalStateException("Payment record not found.");
        }

        double gstAmount = registrationFeeInr * registrationGstPercent / 100.0;
        double totalAmount = registrationFeeInr + gstAmount;
        int expectedAmountPaise = (int) Math.round(totalAmount * 100);

        String orderId = firstNonBlank(
                paidRecord.getDisplayOrderNumber(),
                paidRecord.getOrderId(),
                "-");
        String paymentId = firstNonBlank(paidRecord.getPaymentId(), "-");
        String invoiceNumber = firstNonBlank(
                paidRecord.getInvoiceNumber(),
                RegistrationReferenceNumberHelper.buildInvoiceNumber(sellerId, java.time.Year.now().getValue()));
        LocalDateTime paidAt = paidRecord.getPaidAt() != null ? paidRecord.getPaidAt() : LocalDateTime.now();
        String currency = paidRecord.getCurrency() != null ? paidRecord.getCurrency() : registrationFeeCurrency;
        boolean invoiceEmailSent = false;

        try {
            SellerRegistrationInvoiceRepository.InvoiceRecord latest =
                    resolveOrCreateLatestInvoice(sellerId, seller, paidRecord, expectedAmountPaise);

            paidAt = latest.getPaidAt() != null ? latest.getPaidAt() : paidAt;
            orderId = firstNonBlank(
                    latest.getDisplayOrderNumber(),
                    paidRecord.getDisplayOrderNumber(),
                    paidRecord.getOrderId(),
                    latest.getInvoiceNumber(),
                    orderId);
            paymentId = firstNonBlank(latest.getPaymentId(), paidRecord.getPaymentId(), paymentId);
            invoiceNumber = firstNonBlank(
                    latest.getInvoiceNumber(),
                    paidRecord.getInvoiceNumber(),
                    invoiceNumber);
            if (latest.getCurrency() != null && !latest.getCurrency().isBlank()) {
                currency = latest.getCurrency();
            }

            String paidAtText = RegistrationReferenceNumberHelper.formatInvoiceDate(paidAt);
            byte[] invoicePdf = registrationInvoicePdfService.generateRegistrationInvoice(
                    seller,
                    invoiceNumber,
                    paymentId,
                    orderId,
                    expectedAmountPaise,
                    paidAtText
            );
            if (latest.getId() != null) {
                try {
                    registrationInvoiceRepository.updateInvoicePdf(
                            sellerId, latest.getId(), expectedAmountPaise, invoicePdf);
                } catch (Exception ex) {
                    log.warn("Could not update stored invoice PDF for seller {}: {}", sellerId, ex.getMessage());
                }
            }
            invoiceEmailSent = sendRegistrationInvoiceEmail(
                    seller,
                    invoiceNumber,
                    orderId,
                    paymentId,
                    expectedAmountPaise,
                    invoicePdf
            );
        } catch (Exception ex) {
            log.error("Registration invoice resend failed for seller {}", sellerId, ex);
        }

        return RegistrationPaymentStatusResponse.builder()
                .paid(true)
                .subscriptionActive(registrationPaymentRepository.isSubscriptionActive(sellerId))
                .paymentPending(false)
                .orderId(orderId)
                .paymentId(paymentId)
                .paidAt(paidAt.toString())
                .subscriptionExpiresAt(paidRecord.getSubscriptionExpiresAt() != null
                        ? paidRecord.getSubscriptionExpiresAt().toString()
                        : null)
                .amount(expectedAmountPaise)
                .registrationFee(registrationFeeInr)
                .gstAmount(gstAmount)
                .totalAmount(totalAmount)
                .currency(currency)
                .invoiceEmailSent(invoiceEmailSent)
                .build();
    }

    private SellerRegistrationInvoiceRepository.InvoiceRecord resolveOrCreateLatestInvoice(
            Long sellerId,
            Seller seller,
            SellerRegistrationPaymentRepository.PaymentRecord paidRecord,
            int expectedAmountPaise) {
        var invoices = registrationInvoiceRepository.findBySellerId(sellerId);
        if (!invoices.isEmpty()) {
            return invoices.get(0);
        }

        String invoiceNumber = firstNonBlank(
                paidRecord.getInvoiceNumber(),
                RegistrationReferenceNumberHelper.buildInvoiceNumber(sellerId, java.time.Year.now().getValue()));
        String orderId = firstNonBlank(
                paidRecord.getDisplayOrderNumber(),
                paidRecord.getOrderId(),
                invoiceNumber);
        String paymentId = firstNonBlank(paidRecord.getPaymentId(), "-");
        LocalDateTime paidAt = paidRecord.getPaidAt() != null ? paidRecord.getPaidAt() : LocalDateTime.now();
        String paidAtText = RegistrationReferenceNumberHelper.formatInvoiceDate(paidAt);

        byte[] invoicePdf = registrationInvoicePdfService.generateRegistrationInvoice(
                seller,
                invoiceNumber,
                paymentId,
                orderId,
                expectedAmountPaise,
                paidAtText
        );
        Long invoiceId = registrationInvoiceRepository.saveInvoice(
                sellerId,
                invoiceNumber,
                orderId,
                paymentId,
                expectedAmountPaise,
                paidRecord.getCurrency() != null ? paidRecord.getCurrency() : registrationFeeCurrency,
                paidAt,
                invoicePdf
        );

        SellerRegistrationInvoiceRepository.InvoiceRecord created = new SellerRegistrationInvoiceRepository.InvoiceRecord();
        created.setId(invoiceId);
        created.setInvoiceNumber(invoiceNumber);
        created.setDisplayOrderNumber(orderId);
        created.setPaymentId(paymentId);
        created.setAmount(expectedAmountPaise);
        created.setCurrency(paidRecord.getCurrency() != null ? paidRecord.getCurrency() : registrationFeeCurrency);
        created.setPaidAt(paidAt);
        return created;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean sendRegistrationInvoiceEmail(
            Seller seller,
            String invoiceNumber,
            String displayOrderNumber,
            String paymentId,
            int amountInPaise,
            byte[] invoicePdf) {
        String toEmail = seller.getEmail() != null ? seller.getEmail().trim() : "";
        if (toEmail.isBlank()) {
            log.warn("Registration invoice email skipped — seller {} has no email", seller.getId());
            return false;
        }
        try {
            return mailService.sendRegistrationPaymentSuccessEmail(
                    toEmail,
                    seller.getFullName(),
                    invoiceNumber,
                    displayOrderNumber,
                    paymentId,
                    amountInPaise,
                    invoicePdf
            );
        } catch (Exception ex) {
            log.error("Registration invoice email failed for seller {} ({})", seller.getId(), toEmail, ex);
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RegistrationPaymentStatusResponse getRegistrationPaymentStatus(Long sellerId) {
        requireSeller(sellerId);
        registrationPaymentRepository.ensureTable();
        double gstAmount = registrationFeeInr * registrationGstPercent / 100.0;
        double totalAmount = registrationFeeInr + gstAmount;
        int expectedAmountPaise = (int) Math.round(totalAmount * 100);
        SellerRegistrationPaymentRepository.PaymentRecord record = registrationPaymentRepository.findBySellerId(sellerId);
        if (record == null) {
            return RegistrationPaymentStatusResponse.builder()
                    .paid(false)
                    .subscriptionActive(false)
                    .paymentPending(false)
                    .orderId(null)
                    .paymentId(null)
                    .paidAt(null)
                    .subscriptionExpiresAt(null)
                    .amount(expectedAmountPaise)
                    .registrationFee(registrationFeeInr)
                    .gstAmount(gstAmount)
                    .totalAmount(totalAmount)
                    .currency(registrationFeeCurrency)
                    .invoiceEmailSent(false)
                    .build();
        }
        boolean subscriptionActive = registrationPaymentRepository.isSubscriptionActive(sellerId);
        boolean paymentPending = registrationPaymentRepository.hasEverPaid(sellerId) && !subscriptionActive;
        String responseOrderId = record.getDisplayOrderNumber() != null && !record.getDisplayOrderNumber().isBlank()
                ? record.getDisplayOrderNumber()
                : record.getOrderId();
        int displayAmount = subscriptionActive && record.getAmount() > 0
                ? record.getAmount()
                : expectedAmountPaise;
        return RegistrationPaymentStatusResponse.builder()
                .paid(subscriptionActive)
                .subscriptionActive(subscriptionActive)
                .paymentPending(paymentPending)
                .orderId(responseOrderId)
                .paymentId(record.getPaymentId())
                .paidAt(record.getPaidAt() != null ? record.getPaidAt().toString() : null)
                .subscriptionExpiresAt(record.getSubscriptionExpiresAt() != null
                        ? record.getSubscriptionExpiresAt().toString()
                        : null)
                .amount(displayAmount)
                .registrationFee(registrationFeeInr)
                .gstAmount(gstAmount)
                .totalAmount(totalAmount)
                .currency(record.getCurrency() != null ? record.getCurrency() : registrationFeeCurrency)
                .invoiceEmailSent(subscriptionActive)
                .build();
    }

    private void saveKycSelfie(Long sellerId, String fileName) {
        SellerKycImage image = new SellerKycImage();
        image.setSellerId(sellerId);
        image.setImagePath(fileName);
        image.setImageType("face");
        image.setDocType("live_selfie");
        image.setFileName(fileName);
        image.setCapturedAt(LocalDateTime.now());
        sellerKycImageRepository.save(image);
    }

    private void applyDocument(Seller seller, SellerDocumentType type, String fileName) {
        switch (type) {
            case PROFILE_PIC -> seller.setProfilePic(fileName);
            case AADHAR_FRONT -> seller.setAadharFront(fileName);
            case AADHAR_BACK -> seller.setAadharBack(fileName);
            case PAN_CARD -> seller.setPanCard(fileName);
            case BUSINESS_PROOF -> seller.setBusinessProof(fileName);
            case BANK_PROOF -> seller.setBankProof(fileName);
            case CANCELLED_CHEQUE -> seller.setCancelledCheque(fileName);
            case LIVE_SELFIE -> seller.setLiveSelfie(fileName);
            case COMPANY_PAN_DOC -> seller.setCompanyPanDoc(fileName);
            case INCORPORATION_CERTIFICATE -> seller.setIncorporationCertificate(fileName);
            case PARTNERSHIP_DEED -> seller.setPartnershipDeed(fileName);
            case MSME_CERTIFICATE -> seller.setMsmeCertificate(fileName);
            case IEC_CERTIFICATE -> seller.setIecCertificate(fileName);
        }
    }

    private void validateWarehouse(AddressProfileRequest request) {
        if (isBlank(request.getWarehouseAddress())) {
            throw new IllegalArgumentException("Warehouse street address is required.");
        }
        if (isBlank(request.getWarehouseLandmark())) {
            throw new IllegalArgumentException("Warehouse landmark is required.");
        }
        if (isBlank(request.getWarehouseCity())) {
            throw new IllegalArgumentException("Warehouse city is required.");
        }
        if (isBlank(request.getWarehouseState())) {
            throw new IllegalArgumentException("Warehouse state is required.");
        }
        if (isBlank(request.getWarehouseArea())) {
            throw new IllegalArgumentException("Warehouse area is required.");
        }
        if (isBlank(request.getWarehouseCountry())) {
            throw new IllegalArgumentException("Warehouse country is required.");
        }
        String pinError = SellerProfileValidator.validatePincode(request.getWarehousePincode());
        if (pinError != null) {
            throw new IllegalArgumentException(pinError);
        }
    }

    private String buildWarehouseAddress(AddressProfileRequest request) {
        StringBuilder sb = new StringBuilder(request.getWarehouseAddress().trim());
        if (!isBlank(request.getWarehouseLandmark())) {
            sb.append("\nLandmark: ").append(request.getWarehouseLandmark().trim());
        }
        if (!isBlank(request.getWarehousePincode())) {
            sb.append("\nPIN: ").append(request.getWarehousePincode().trim());
        }
        return sb.toString();
    }

    private Seller requireSeller(Long sellerId) {
        return sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found."));
    }

    private SellerCategory parseCategory(String value) {
        return SellerCategory.valueOf(value.trim().toLowerCase(Locale.ROOT));
    }

    private void touchProfile(Seller seller) {
        LocalDateTime now = LocalDateTime.now();
        seller.setProfileUpdatedAt(now);
        seller.setUpdatedAt(now);
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean isMaskedAadhaarInput(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String trimmed = value.trim().toUpperCase(Locale.ROOT);
        return trimmed.contains("XXXX") || SellerProfileValidator.normalizeAadhaar(value).length() < 12;
    }

    private String resolveDisplayOrderNumber(
            Seller seller,
            SellerRegistrationPaymentRepository.PaymentRecord record) {
        if (record.getDisplayOrderNumber() != null && !record.getDisplayOrderNumber().isBlank()) {
            return record.getDisplayOrderNumber();
        }
        LocalDate orderDate = record.getCreatedAt() != null
                ? record.getCreatedAt().toLocalDate()
                : LocalDate.now();
        return RegistrationReferenceNumberHelper.buildDisplayOrderNumber(seller.getId(), orderDate);
    }

    private boolean isValidRazorpaySignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    razorpayKeySecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder expected = new StringBuilder();
            for (byte b : hash) {
                expected.append(String.format("%02x", b));
            }
            return expected.toString().equals(signature);
        } catch (Exception e) {
            return false;
        }
    }
}
