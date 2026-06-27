package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.SellerSettingsResponse;
import com.ecommerce.sellerbackend.dto.UpdateSellerSettingsRequest;
import com.ecommerce.sellerbackend.entity.SellerPreferences;
import com.ecommerce.sellerbackend.repository.SellerPreferencesRepository;
import com.ecommerce.sellerbackend.repository.SellerRepository;
import com.ecommerce.sellerbackend.service.SellerSettingsService;
import com.ecommerce.sellerbackend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SellerSettingsServiceImpl implements SellerSettingsService {

    private final SellerRepository sellerRepository;
    private final SellerPreferencesRepository preferencesRepository;

    @Override
    @Transactional(readOnly = true)
    public SellerSettingsResponse get(Long sellerId) {
        sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found."));
        return toResponse(loadOrDefault(sellerId));
    }

    @Override
    @Transactional
    public SellerSettingsResponse update(Long sellerId, UpdateSellerSettingsRequest request) {
        sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found."));
        SellerPreferences prefs = loadOrDefault(sellerId);
        if (request.getPushNotifications() != null) {
            prefs.setPushNotifications(request.getPushNotifications());
        }
        if (request.getOrderUpdates() != null) {
            prefs.setOrderUpdates(request.getOrderUpdates());
        }
        if (request.getPayoutAlerts() != null) {
            prefs.setPayoutAlerts(request.getPayoutAlerts());
        }
        if (request.getVacationMode() != null) {
            prefs.setVacationMode(request.getVacationMode());
        }
        if (request.getDarkMode() != null) {
            prefs.setDarkMode(request.getDarkMode());
        }
        if (request.getLanguage() != null && !request.getLanguage().isBlank()) {
            prefs.setLanguage(request.getLanguage().trim());
        }
        if (request.getBiometricLogin() != null) {
            prefs.setBiometricLogin(request.getBiometricLogin());
        }
        prefs.setUpdatedAt(LocalDateTime.now());
        preferencesRepository.save(prefs);
        return toResponse(prefs);
    }

    private SellerPreferences loadOrDefault(Long sellerId) {
        return preferencesRepository.findById(sellerId).orElseGet(() -> {
            SellerPreferences created = new SellerPreferences();
            created.setSellerId(sellerId);
            created.setPushNotifications(true);
            created.setOrderUpdates(true);
            created.setPayoutAlerts(true);
            created.setVacationMode(false);
            created.setDarkMode(false);
            created.setLanguage("en-IN");
            created.setBiometricLogin(false);
            created.setUpdatedAt(LocalDateTime.now());
            return preferencesRepository.save(created);
        });
    }

    private SellerSettingsResponse toResponse(SellerPreferences prefs) {
        return SellerSettingsResponse.builder()
                .pushNotifications(Boolean.TRUE.equals(prefs.getPushNotifications()))
                .orderUpdates(Boolean.TRUE.equals(prefs.getOrderUpdates()))
                .payoutAlerts(Boolean.TRUE.equals(prefs.getPayoutAlerts()))
                .vacationMode(Boolean.TRUE.equals(prefs.getVacationMode()))
                .darkMode(Boolean.TRUE.equals(prefs.getDarkMode()))
                .language(prefs.getLanguage() != null ? prefs.getLanguage() : "en-IN")
                .biometricLogin(Boolean.TRUE.equals(prefs.getBiometricLogin()))
                .build();
    }

    @Override
    @Transactional
    public void deactivateAccount(Long sellerId) {
        var seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found."));
        seller.setStatus(com.ecommerce.sellerbackend.entity.SellerAccountStatus.inactive);
        sellerRepository.save(seller);
    }
}
