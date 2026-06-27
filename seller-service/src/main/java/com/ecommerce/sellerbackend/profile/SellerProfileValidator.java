package com.ecommerce.sellerbackend.profile;

import com.ecommerce.sellerbackend.entity.Seller;
import com.ecommerce.sellerbackend.entity.SellerCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class SellerProfileValidator {

    private static final Pattern GST_REGEX =
            Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");
    private static final Pattern PAN_REGEX = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]{1}$");
    private static final Pattern COMPANY_PAN_REGEX = Pattern.compile("^[A-Z]{3}C[A-Z][0-9]{4}[A-Z]$");
    private static final Pattern AADHAAR_REGEX = Pattern.compile("^\\d{12}$");
    private static final Pattern IFSC_REGEX = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");
    private static final Pattern PINCODE_REGEX = Pattern.compile("^\\d{6}$");

    private SellerProfileValidator() {
    }

    public static String normalizeAadhaar(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\D", "");
    }

    public static String validatePan(String pan) {
        if (pan == null || pan.isBlank()) {
            return "PAN number is required";
        }
        if (!PAN_REGEX.matcher(pan.trim().toUpperCase()).matches()) {
            return "Invalid PAN format (e.g. ABCDE1234F)";
        }
        return null;
    }

    public static String validateCompanyPan(String pan) {
        if (pan == null || pan.isBlank()) {
            return "Company PAN number is required";
        }
        String upper = pan.trim().toUpperCase();
        if (!COMPANY_PAN_REGEX.matcher(upper).matches()) {
            return "Invalid company PAN format (4th character must be C)";
        }
        return null;
    }

    public static String validateAadhaar(String aadhaar) {
        String digits = normalizeAadhaar(aadhaar);
        if (digits.isEmpty()) {
            return "Aadhaar number is required";
        }
        if (!AADHAAR_REGEX.matcher(digits).matches()) {
            return "Aadhaar must be exactly 12 digits";
        }
        return null;
    }

    public static String validateGst(String gst) {
        if (gst == null || gst.isBlank()) {
            return "GST number is required";
        }
        String upper = gst.trim().toUpperCase();
        if (upper.length() != 15) {
            return "GST number must be 15 characters";
        }
        if (!GST_REGEX.matcher(upper).matches()) {
            return "Invalid GST format (e.g. 29AAAPL1234C1Z1)";
        }
        return null;
    }

    public static String validateIfsc(String ifsc) {
        if (ifsc == null || ifsc.isBlank()) {
            return "IFSC code is required";
        }
        if (!IFSC_REGEX.matcher(ifsc.trim().toUpperCase()).matches()) {
            return "Invalid IFSC code format (e.g. SBIN0000345)";
        }
        return null;
    }

    public static String validateAccountNumber(String account) {
        if (account == null || account.isBlank()) {
            return "Account number is required";
        }
        if (!account.matches("^\\d+$")) {
            return "Account number must contain only digits";
        }
        if (account.length() < 9) {
            return "Account number must be at least 9 digits";
        }
        return null;
    }

    public static String validateAccountHolder(String name) {
        if (name == null || name.isBlank()) {
            return "Account holder name is required";
        }
        if (name.trim().length() < 3) {
            return "Account holder name must be at least 3 characters";
        }
        return null;
    }

    public static String validatePincode(String pincode) {
        if (pincode == null || pincode.isBlank()) {
            return "Pincode is required";
        }
        if (!PINCODE_REGEX.matcher(pincode.trim()).matches()) {
            return "Pincode must be 6 digits";
        }
        return null;
    }

    public static List<String> validateForSubmit(Seller seller) {
        List<String> errors = new ArrayList<>();

        if (seller.getBusinessName() == null || seller.getBusinessName().isBlank()) {
            errors.add("Business name is required");
        }
        if (seller.getBusinessType() == null || seller.getBusinessType().isBlank()) {
            errors.add("Business type is required");
        }
        if (seller.getSellerCategory() == null) {
            errors.add("Business category is required");
        }

        boolean gstRequired = seller.getSellerCategory() == SellerCategory.b2b
                || Boolean.TRUE.equals(seller.getHasGst());
        if (gstRequired) {
            String gstErr = validateGst(seller.getGstNumber());
            if (gstErr != null) {
                errors.add(gstErr);
            }
        }

        String panErr = validatePan(seller.getPanNumber());
        if (panErr != null) {
            errors.add(panErr);
        }

        String aadhaarErr = validateAadhaar(seller.getAadhaarNumber());
        if (aadhaarErr != null) {
            errors.add(aadhaarErr);
        }

        if (seller.getAddress() == null || seller.getAddress().isBlank()) {
            errors.add("Street address is required");
        }
        if (seller.getLandmark() == null || seller.getLandmark().isBlank()) {
            errors.add("Landmark is required");
        }
        if (seller.getCity() == null || seller.getCity().isBlank()) {
            errors.add("City is required");
        }
        if (seller.getState() == null || seller.getState().isBlank()) {
            errors.add("State is required");
        }
        if (seller.getCountry() == null || seller.getCountry().isBlank()) {
            errors.add("Country is required");
        }
        String pinErr = validatePincode(seller.getPincode());
        if (pinErr != null) {
            errors.add(pinErr);
        }

        String ifscErr = validateIfsc(seller.getIfscCode());
        if (ifscErr != null) {
            errors.add(ifscErr);
        }
        String accErr = validateAccountNumber(seller.getAccountNumber());
        if (accErr != null) {
            errors.add(accErr);
        }
        String holderErr = validateAccountHolder(seller.getAccountHolder());
        if (holderErr != null) {
            errors.add(holderErr);
        }
        if (seller.getBankName() == null || seller.getBankName().isBlank()) {
            errors.add("Bank name is required");
        }

        if (isBlank(seller.getAadharFront())) {
            errors.add("Aadhaar front document is required");
        }
        if (isBlank(seller.getAadharBack())) {
            errors.add("Aadhaar back document is required");
        }
        if (isBlank(seller.getPanCard())) {
            errors.add("PAN card document is required");
        }
        if (isBlank(seller.getBusinessProof())) {
            errors.add("Business proof document is required");
        }
        if (isBlank(seller.getBankProof())) {
            errors.add("Bank account proof is required");
        }
        if (isBlank(seller.getCancelledCheque())) {
            errors.add("Cancelled cheque is required");
        }
        if (isBlank(seller.getLiveSelfie())) {
            errors.add("Live selfie is required");
        }

        if (seller.getSellerCategory() == SellerCategory.b2b) {
            String companyPanErr = validateCompanyPan(seller.getCompanyPan());
            if (companyPanErr != null) {
                errors.add(companyPanErr);
            }
            if (isBlank(seller.getCompanyPanDoc())) {
                errors.add("Company PAN document is required");
            }
            if (isBlank(seller.getIncorporationCertificate())) {
                errors.add("Certificate of incorporation is required");
            }
            if (isBlank(seller.getPartnershipDeed())) {
                errors.add("Partnership deed is required");
            }
            if (isBlank(seller.getMsmeCertificate())) {
                errors.add("MSME / Udyam certificate is required");
            }
            if (isBlank(seller.getIecCertificate())) {
                errors.add("IEC certificate is required");
            }
        }

        return errors;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
