package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.Enum.DeliveryType;
import com.ecommerce.authdemo.entity.Pincode;
import com.ecommerce.authdemo.repository.PincodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Resolves intra-city vs metro-metro from seller and buyer location data in the database.
 */
@Service
@RequiredArgsConstructor
public class DeliveryZoneResolver {

    private final PincodeRepository pincodeRepository;

    public DeliveryType resolveDeliveryType(String sellerPincode, String buyerPincode, String buyerCity) {
        String sellerCode = normalizePincode(sellerPincode);
        String buyerCode = normalizePincode(buyerPincode);
        if (sellerCode != null && buyerCode != null && sellerCode.equals(buyerCode)) {
            return DeliveryType.intra_city;
        }

        Integer sellerCityId = resolveCityId(sellerCode);
        Integer buyerCityId = resolveCityId(buyerCode);
        if (sellerCityId != null && buyerCityId != null && sellerCityId.equals(buyerCityId)) {
            return DeliveryType.intra_city;
        }

        if (sellerCode != null && buyerCode != null
                && sellerCode.length() >= 3
                && sellerCode.substring(0, 3).equals(buyerCode.substring(0, 3))) {
            return DeliveryType.intra_city;
        }

        String normalizedBuyerCity = normalizeCity(buyerCity);
        if (normalizedBuyerCity != null && sellerCode != null) {
            List<Pincode> sellerRows = pincodeRepository.findByPincode(sellerCode);
            for (Pincode row : sellerRows) {
                if (row.getCityId() != null && buyerCityId != null && row.getCityId().equals(buyerCityId)) {
                    return DeliveryType.intra_city;
                }
            }
        }

        return DeliveryType.metro_metro;
    }

    private Integer resolveCityId(String pincode) {
        if (pincode == null) {
            return null;
        }
        List<Pincode> rows = pincodeRepository.findByPincode(pincode);
        if (rows.isEmpty()) {
            return null;
        }
        return rows.get(0).getCityId();
    }

    private static String normalizePincode(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        return digits.length() >= 6 ? digits.substring(0, 6) : (digits.isEmpty() ? null : digits);
    }

    private static String normalizeCity(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim().toLowerCase(Locale.ENGLISH);
        return trimmed.isEmpty() ? null : trimmed;
    }
}
