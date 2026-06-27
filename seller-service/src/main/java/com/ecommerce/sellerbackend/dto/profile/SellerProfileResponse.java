package com.ecommerce.sellerbackend.dto.profile;

import com.ecommerce.sellerbackend.entity.Seller;
import com.ecommerce.sellerbackend.entity.SellerAccountStatus;
import com.ecommerce.sellerbackend.service.MediaStorageService;
import com.ecommerce.sellerbackend.util.SellerAccountStatusHelper;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Builder
public class SellerProfileResponse {

    private final Long sellerId;
    private final String sellerUniqueId;
    private final String email;
    private final String mobile;
    private final String firstName;
    private final String lastName;
    private final String fullName;
    private final boolean profileCompleted;
    private final boolean kycCompleted;
    private final LocalDateTime kycSubmittedAt;
    private final SellerAccountStatusResponse accountStatus;

    private final PersonalSection personal;
    private final BusinessSection business;
    private final AddressSection address;
    private final BankingSection banking;
    private final DocumentsSection documents;
    private final ProfileSteps steps;

    @Getter
    @Builder
    public static class PersonalSection {
        private final String profilePicUrl;
    }

    @Getter
    @Builder
    public static class BusinessSection {
        private final String businessCategory;
        private final String businessName;
        private final String businessType;
        private final String address;
        private final boolean hasGst;
        private final String gstType;
        private final String gstNumber;
        private final String panNumber;
        private final String aadhaarNumber;
        private final boolean aadhaarOnFile;
        private final String companyPan;
    }

    @Getter
    @Builder
    public static class AddressSection {
        private final String streetAddress;
        private final String landmark;
        private final String city;
        private final String state;
        private final String area;
        private final String country;
        private final String pincode;
        private final boolean warehouseDifferent;
        private final String warehouseAddress;
        private final String warehouseLandmark;
        private final String warehouseCity;
        private final String warehouseState;
        private final String warehouseArea;
        private final String warehouseCountry;
        private final String warehousePincode;
    }

    @Getter
    @Builder
    public static class BankingSection {
        private final String ifscCode;
        private final String bankName;
        private final String branchName;
        private final String accountHolderName;
        private final boolean accountNumberPresent;
    }

    @Getter
    @Builder
    public static class DocumentsSection {
        private final Map<String, String> files;
        private final String liveSelfieUrl;
    }

    @Getter
    @Builder
    public static class ProfileSteps {
        private final boolean personal;
        private final boolean business;
        private final boolean address;
        private final boolean banking;
        private final boolean documents;
    }

    public static SellerProfileResponse from(Seller seller, MediaStorageService mediaStorage) {
        String fullName = joinName(seller.getFirstName(), seller.getLastName());
        boolean warehouseDifferent = seller.getWarehouseAddress() != null && !seller.getWarehouseAddress().isBlank();

        Map<String, String> files = new LinkedHashMap<>();
        putFile(files, "profilePic", seller.getProfilePic(), mediaStorage);
        putFile(files, "aadharFront", seller.getAadharFront(), mediaStorage);
        putFile(files, "aadharBack", seller.getAadharBack(), mediaStorage);
        putFile(files, "panCard", seller.getPanCard(), mediaStorage);
        putFile(files, "businessProof", seller.getBusinessProof(), mediaStorage);
        putFile(files, "bankProof", seller.getBankProof(), mediaStorage);
        putFile(files, "cancelledCheque", seller.getCancelledCheque(), mediaStorage);
        putFile(files, "companyPanDoc", seller.getCompanyPanDoc(), mediaStorage);
        putFile(files, "incorporationCertificate", seller.getIncorporationCertificate(), mediaStorage);
        putFile(files, "partnershipDeed", seller.getPartnershipDeed(), mediaStorage);
        putFile(files, "msmeCertificate", seller.getMsmeCertificate(), mediaStorage);
        putFile(files, "iecCertificate", seller.getIecCertificate(), mediaStorage);

        boolean personalDone = seller.getProfilePic() != null && !seller.getProfilePic().isBlank();
        boolean businessDone = seller.getBusinessName() != null && !seller.getBusinessName().isBlank()
                && seller.getAddress() != null && !seller.getAddress().isBlank()
                && seller.getPanNumber() != null && !seller.getPanNumber().isBlank();
        boolean addressDone = seller.getAddress() != null && !seller.getAddress().isBlank()
                && seller.getPincode() != null && !seller.getPincode().isBlank();
        boolean bankingDone = seller.getIfscCode() != null && !seller.getIfscCode().isBlank()
                && seller.getAccountNumber() != null && !seller.getAccountNumber().isBlank();
        boolean documentsDone = seller.getAadharFront() != null && seller.getLiveSelfie() != null;

        return SellerProfileResponse.builder()
                .sellerId(seller.getId())
                .sellerUniqueId(seller.getSellerUniqueId())
                .email(seller.getEmail())
                .mobile(seller.getMobile())
                .firstName(seller.getFirstName())
                .lastName(seller.getLastName())
                .fullName(fullName)
                .profileCompleted(Boolean.TRUE.equals(seller.getProfileCompleted()))
                .kycCompleted(Boolean.TRUE.equals(seller.getKycCompleted()))
                .kycSubmittedAt(seller.getKycSubmittedAt())
                .accountStatus(SellerAccountStatusHelper.build(seller))
                .personal(PersonalSection.builder()
                        .profilePicUrl(mediaStorage.toPublicUrl(seller.getProfilePic()))
                        .build())
                .business(BusinessSection.builder()
                        .businessCategory(seller.getSellerCategory() != null
                                ? seller.getSellerCategory().name().toUpperCase()
                                : null)
                        .businessName(seller.getBusinessName())
                        .businessType(seller.getBusinessType())
                        .address(seller.getAddress())
                        .hasGst(Boolean.TRUE.equals(seller.getHasGst()))
                        .gstType(seller.getGstType())
                        .gstNumber(seller.getGstNumber())
                        .panNumber(seller.getPanNumber())
                        .aadhaarNumber(maskAadhaar(seller.getAadhaarNumber()))
                        .aadhaarOnFile(seller.getAadhaarNumber() != null
                                && seller.getAadhaarNumber().replaceAll("\\D", "").length() == 12)
                        .companyPan(seller.getCompanyPan())
                        .build())
                .address(AddressSection.builder()
                        .streetAddress(seller.getAddress())
                        .landmark(seller.getLandmark())
                        .city(seller.getCity())
                        .state(seller.getState())
                        .area(seller.getArea())
                        .country(seller.getCountry())
                        .pincode(seller.getPincode())
                        .warehouseDifferent(warehouseDifferent)
                        .warehouseAddress(extractWarehouseStreet(seller.getWarehouseAddress()))
                        .warehouseLandmark(extractWarehouseLandmark(seller.getWarehouseAddress()))
                        .warehouseCity(seller.getWarehouseCity())
                        .warehouseState(seller.getWarehouseState())
                        .warehouseArea(seller.getWarehouseArea())
                        .warehouseCountry(seller.getWarehouseCountry())
                        .warehousePincode(extractWarehousePincode(seller.getWarehouseAddress()))
                        .build())
                .banking(BankingSection.builder()
                        .ifscCode(seller.getIfscCode())
                        .bankName(seller.getBankName())
                        .branchName(seller.getBranchName())
                        .accountHolderName(seller.getAccountHolder())
                        .accountNumberPresent(seller.getAccountNumber() != null && !seller.getAccountNumber().isBlank())
                        .build())
                .documents(DocumentsSection.builder()
                        .files(files)
                        .liveSelfieUrl(mediaStorage.toPublicUrl(seller.getLiveSelfie()))
                        .build())
                .steps(ProfileSteps.builder()
                        .personal(personalDone)
                        .business(businessDone)
                        .address(addressDone)
                        .banking(bankingDone)
                        .documents(documentsDone)
                        .build())
                .build();
    }

    private static void putFile(Map<String, String> files, String key, String fileName, MediaStorageService media) {
        if (fileName != null && !fileName.isBlank()) {
            files.put(key, media.toPublicUrl(fileName));
        }
    }

    private static String joinName(String first, String last) {
        String f = first == null ? "" : first.trim();
        String l = last == null ? "" : last.trim();
        if (f.isEmpty()) {
            return l;
        }
        if (l.isEmpty()) {
            return f;
        }
        return f + " " + l;
    }

    private static String maskAadhaar(String aadhaar) {
        if (aadhaar == null || aadhaar.length() < 4) {
            return aadhaar;
        }
        return "XXXX XXXX " + aadhaar.substring(aadhaar.length() - 4);
    }

    private static String extractWarehousePincode(String warehouseAddress) {
        if (warehouseAddress == null) {
            return null;
        }
        int idx = warehouseAddress.lastIndexOf("PIN:");
        if (idx < 0) {
            return null;
        }
        return warehouseAddress.substring(idx + 4).trim();
    }

    private static String extractWarehouseLandmark(String warehouseAddress) {
        if (warehouseAddress == null) {
            return null;
        }
        for (String line : warehouseAddress.split("\n")) {
            if (line.startsWith("Landmark: ")) {
                return line.substring("Landmark: ".length()).trim();
            }
        }
        return null;
    }

    private static String extractWarehouseStreet(String warehouseAddress) {
        if (warehouseAddress == null || warehouseAddress.isBlank()) {
            return null;
        }
        int landmarkIdx = warehouseAddress.indexOf("\nLandmark:");
        int pinIdx = warehouseAddress.indexOf("\nPIN:");
        int end = warehouseAddress.length();
        if (landmarkIdx >= 0) {
            end = Math.min(end, landmarkIdx);
        }
        if (pinIdx >= 0) {
            end = Math.min(end, pinIdx);
        }
        return warehouseAddress.substring(0, end).trim();
    }
}
