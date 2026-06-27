package com.ecommerce.sellerbackend.profile;

import java.util.Arrays;
import java.util.Optional;

public enum SellerDocumentType {
    PROFILE_PIC("profile_pic", "profilePic", false),
    AADHAR_FRONT("aadhar_front", "aadharFront", false),
    AADHAR_BACK("aadhar_back", "aadharBack", false),
    PAN_CARD("pan_card", "panCard", false),
    BUSINESS_PROOF("business_proof", "businessProof", false),
    BANK_PROOF("bank_proof", "bankProof", false),
    CANCELLED_CHEQUE("cancelled_cheque", "cancelledCheque", false),
    LIVE_SELFIE("live_selfie", "liveSelfie", true),
    COMPANY_PAN_DOC("company_pan_doc", "companyPanDoc", false),
    INCORPORATION_CERTIFICATE("incorporation_certificate", "incorporationCertificate", false),
    PARTNERSHIP_DEED("partnership_deed", "partnershipDeed", false),
    MSME_CERTIFICATE("msme_certificate", "msmeCertificate", false),
    IEC_CERTIFICATE("iec_certificate", "iecCertificate", false);

    private final String fileToken;
    private final String sellerField;
    private final boolean allowMultiple;

    SellerDocumentType(String fileToken, String sellerField, boolean allowMultiple) {
        this.fileToken = fileToken;
        this.sellerField = sellerField;
        this.allowMultiple = allowMultiple;
    }

    public String getFileToken() {
        return fileToken;
    }

    public String getSellerField() {
        return sellerField;
    }

    public boolean isAllowMultiple() {
        return allowMultiple;
    }

    public static Optional<SellerDocumentType> fromParam(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase().replace('-', '_');
        return Arrays.stream(values())
                .filter(t -> t.name().equalsIgnoreCase(normalized)
                        || t.fileToken.equalsIgnoreCase(normalized)
                        || t.sellerField.equalsIgnoreCase(normalized))
                .findFirst();
    }
}
