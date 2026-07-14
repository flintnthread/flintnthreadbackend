package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.profile.GstVerifyResponse;
import com.ecommerce.sellerbackend.entity.SellerGstDetails;
import com.ecommerce.sellerbackend.repository.SellerGstDetailsRepository;
import com.ecommerce.sellerbackend.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GstVerificationService {

    private static final String DUPLICATE_GST_MESSAGE =
            "This GSTIN is already registered with another seller account. "
                    + "Each GSTIN can only be used once. Please contact support if you need help.";

    private final GstExternalLookupClient externalLookupClient;
    private final SellerRepository sellerRepository;
    private final SellerGstDetailsRepository sellerGstDetailsRepository;

    /** Known GSTINs for local/dev verification with full business details. */
    private static final Map<String, GstVerifyResponse> KNOWN_GST = Map.of(
            "27AWMPS1214Q1ZX",
            GstVerifyResponse.builder()
                    .verified(true)
                    .gstNumber("27AWMPS1214Q1ZX")
                    .message("GST verified successfully.")
                    .businessName("PICKCELL")
                    .tradeName("PICKCELL ONLINE")
                    .businessType("Sole Proprietorship")
                    .panNumber("AWMPS1214Q")
                    .address("B-506, Radha Vallabh, Near D mart, 150 Feet Road")
                    .city("Mumbai")
                    .state("Maharashtra")
                    .pincode("401101")
                    .status("Active")
                    .taxpayerType("Regular")
                    .registrationDate("01/07/2017")
                    .stateJurisdiction("Maharashtra")
                    .centreJurisdiction("RANGE-I")
                    .principalPlaceType("Office / Sale Office")
                    .build(),
            "29AAAPL1234C1Z1",
            GstVerifyResponse.builder()
                    .verified(true)
                    .gstNumber("29AAAPL1234C1Z1")
                    .message("GST verified successfully.")
                    .businessName("AAAPL RETAIL PRIVATE LIMITED")
                    .tradeName("AAAPL RETAIL")
                    .businessType("Private Limited")
                    .panNumber("AAAPL1234C")
                    .address("MG Road, Bengaluru")
                    .city("Bengaluru")
                    .state("Karnataka")
                    .pincode("560001")
                    .status("Active")
                    .taxpayerType("Regular")
                    .registrationDate("15/08/2017")
                    .stateJurisdiction("Karnataka")
                    .centreJurisdiction("RANGE-II")
                    .principalPlaceType("Office / Sale Office")
                    .build(),
            "36AAGCF5402J1ZP",
            GstVerifyResponse.builder()
                    .verified(true)
                    .gstNumber("36AAGCF5402J1ZP")
                    .message("GST verified successfully.")
                    .businessName("FLINT & THREAD (INDIA) PRIVATE LIMITED")
                    .tradeName("FLINT & THREAD (INDIA) PRIVATE LIMITED")
                    .businessType("Private Limited")
                    .panNumber("AAGCF5402J")
                    .address("H NO 1-179/9/620/KDC, KRUSHI DEFENCE COLONY, Hyderabad")
                    .city("Hyderabad")
                    .state("Telangana")
                    .pincode("502319")
                    .status("Active")
                    .taxpayerType("Regular")
                    .registrationDate("31/07/2025")
                    .stateJurisdiction("Patancheruvu")
                    .centreJurisdiction("PATANCHERU")
                    .principalPlaceType("Others")
                    .build(),
            "36AGQCF5402J1ZP",
            GstVerifyResponse.builder()
                    .verified(true)
                    .gstNumber("36AGQCF5402J1ZP")
                    .message("GST verified successfully.")
                    .businessName("FLINT & THREAD (INDIA) PVT LTD")
                    .tradeName("FLINT & THREAD")
                    .businessType("Private Limited")
                    .panNumber("AGQCF5402J")
                    .address("Hyderabad, Telangana")
                    .city("Hyderabad")
                    .state("Telangana")
                    .pincode("500001")
                    .status("Active")
                    .taxpayerType("Regular")
                    .registrationDate("01/07/2017")
                    .stateJurisdiction("Telangana")
                    .centreJurisdiction("RANGE-III")
                    .principalPlaceType("Office / Sale Office")
                    .build()
    );

    /**
     * Verifies a GSTIN for a new/onboarding seller.
     * Uses GST portal lookup (or dev fixtures) only — never reuses another seller's saved profile.
     */
    public GstVerifyResponse verify(String rawGst, Long currentSellerId) {
        String gst = rawGst.trim().toUpperCase(Locale.ROOT);

        if (!isValidGstFormat(gst)) {
            return GstVerifyResponse.builder()
                    .verified(false)
                    .alreadyExists(false)
                    .gstNumber(gst)
                    .message("Invalid GST number format.")
                    .build();
        }

        if (isGstRegisteredToAnotherSeller(gst, currentSellerId)) {
            return duplicateGstResponse(gst);
        }

        GstVerifyResponse lookup = lookupGstDetails(gst);
        if (lookup.isVerified()) {
            return lookup;
        }
        return withAlreadyExists(lookup, false);
    }

    /** Fetches GST portal / fixture details without duplicate checks. */
    private GstVerifyResponse lookupGstDetails(String gst) {
        GstVerifyResponse known = KNOWN_GST.get(gst);
        if (known != null) {
            return known;
        }

        Optional<GstVerifyResponse> external = externalLookupClient.lookup(gst);
        if (external.isPresent()) {
            GstVerifyResponse response = external.get();
            if (response.isVerified() || hasRegistryDetails(response)) {
                return response;
            }
            if (!isBlank(response.getMessage())) {
                return response;
            }
        }

        return gstNotFoundResponse(gst);
    }

    private GstVerifyResponse duplicateGstResponse(String gst) {
        Optional<SellerGstDetails> saved = sellerGstDetailsRepository.findByGstin(gst);
        if (saved.isPresent()) {
            return fromSavedDetails(saved.get());
        }

        GstVerifyResponse lookup = lookupGstDetails(gst);
        if (hasRegistryDetails(lookup)) {
            return withAlreadyExists(lookup, true);
        }

        return GstVerifyResponse.builder()
                .verified(false)
                .alreadyExists(true)
                .gstNumber(gst)
                .panNumber(extractPanFromGst(gst))
                .message(DUPLICATE_GST_MESSAGE)
                .build();
    }

    private GstVerifyResponse fromSavedDetails(SellerGstDetails gst) {
        return GstVerifyResponse.builder()
                .verified(false)
                .alreadyExists(true)
                .gstNumber(gst.getGstin())
                .message(DUPLICATE_GST_MESSAGE)
                .businessName(gst.getLegalName())
                .tradeName(gst.getTradeName())
                .businessType(gst.getConstitution())
                .panNumber(gst.getPan())
                .address(gst.getAddress())
                .city(gst.getCity())
                .state(gst.getState())
                .pincode(gst.getPincode())
                .status(gst.getGstStatus())
                .taxpayerType(gst.getTaxpayerType())
                .registrationDate(gst.getRegistrationDate())
                .cancellationDate(gst.getCancellationDate())
                .stateJurisdiction(gst.getStateJurisdiction())
                .centreJurisdiction(gst.getCentreJurisdiction())
                .principalPlaceType(gst.getPrincipalPlace())
                .build();
    }

    private GstVerifyResponse withAlreadyExists(GstVerifyResponse source, boolean alreadyExists) {
        return GstVerifyResponse.builder()
                .verified(source.isVerified())
                .alreadyExists(alreadyExists)
                .gstNumber(source.getGstNumber())
                .message(alreadyExists ? DUPLICATE_GST_MESSAGE : source.getMessage())
                .businessName(source.getBusinessName())
                .tradeName(source.getTradeName())
                .businessType(source.getBusinessType())
                .panNumber(source.getPanNumber())
                .address(source.getAddress())
                .city(source.getCity())
                .state(source.getState())
                .pincode(source.getPincode())
                .status(source.getStatus())
                .taxpayerType(source.getTaxpayerType())
                .registrationDate(source.getRegistrationDate())
                .cancellationDate(source.getCancellationDate())
                .stateJurisdiction(source.getStateJurisdiction())
                .centreJurisdiction(source.getCentreJurisdiction())
                .principalPlaceType(source.getPrincipalPlaceType())
                .build();
    }

    private boolean hasRegistryDetails(GstVerifyResponse response) {
        return !isBlank(response.getBusinessName())
                || !isBlank(response.getTradeName())
                || !isBlank(response.getAddress())
                || !isBlank(response.getStatus());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public boolean isGstRegisteredToAnotherSeller(String gst, Long currentSellerId) {
        if (currentSellerId == null || currentSellerId <= 0) {
            return sellerRepository.findFirstByGstNumberIgnoreCase(gst).isPresent();
        }
        return sellerRepository.existsByGstNumberIgnoreCaseAndIdNot(gst, currentSellerId);
    }

    private GstVerifyResponse gstNotFoundResponse(String gst) {
        return GstVerifyResponse.builder()
                .verified(false)
                .alreadyExists(false)
                .gstNumber(gst)
                .panNumber(extractPanFromGst(gst))
                .message(
                        "Could not fetch registered business from the GST portal. "
                                + "PAN has been derived from your GSTIN. "
                                + "Configure app.gst.lookup.api-key (AppyFlow key_secret) for live verification, "
                                + "or double-check the GSTIN on the GST portal."
                )
                .build();
    }

    private static boolean isValidGstFormat(String gst) {
        if (gst == null || gst.length() != 15) {
            return false;
        }
        return gst.matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");
    }

    public Optional<String> extractPan(String gst) {
        if (gst == null || gst.length() != 15) {
            return Optional.empty();
        }
        return Optional.of(extractPanFromGst(gst.toUpperCase(Locale.ROOT)));
    }

    private String extractPanFromGst(String gst) {
        return gst.substring(2, 12);
    }
}
