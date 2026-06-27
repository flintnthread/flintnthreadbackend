package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.entity.Banner;
import com.ecommerce.authdemo.entity.BannerSetting;
import com.ecommerce.authdemo.repository.BannerRepository;
import com.ecommerce.authdemo.repository.BannerSettingRepository;
import com.ecommerce.authdemo.service.BannerService;
import com.ecommerce.authdemo.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;
    private final BannerSettingRepository bannerSettingRepository;
    private final ImageUploadService imageUploadService;

    @Override
    public List<Banner> getAllBanners() {
        return bannerRepository.findAll()
                .stream()
                .sorted(getSortOrder())
                .toList();
    }

    @Override
    public List<Banner> getActiveBanners() {
        return bannerRepository.findByStatus(1)
                .stream()
                .sorted(getSortOrder())
                .toList();
    }

    @Override
    public Banner getBannerById(Long id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found with id: " + id));
    }

    @Override
    public Banner createBanner(Banner banner, MultipartFile desktopImageFile, MultipartFile mobileImageFile) {
        if (desktopImageFile != null && !desktopImageFile.isEmpty()) {
            banner.setDesktopImage(imageUploadService.uploadImage(desktopImageFile));
        }
        if (mobileImageFile != null && !mobileImageFile.isEmpty()) {
            banner.setMobileImage(imageUploadService.uploadImage(mobileImageFile));
        }
        validateBanner(banner);
        return bannerRepository.save(banner);
    }

    @Override
    public Banner updateBanner(Long id, Banner request, MultipartFile desktopImageFile, MultipartFile mobileImageFile) {
        Banner banner = getBannerById(id);

        if (hasText(request.getName())) {
            banner.setName(request.getName().trim());
        }
        if (hasText(request.getDesktopImage())) {
            banner.setDesktopImage(request.getDesktopImage().trim());
        }
        if (hasText(request.getMobileImage())) {
            banner.setMobileImage(request.getMobileImage().trim());
        }
        if (request.getDescription() != null) {
            banner.setDescription(request.getDescription());
        }
        if (request.getTargetUrl() != null) {
            banner.setTargetUrl(request.getTargetUrl());
        }
        if (request.getButtonText() != null) {
            banner.setButtonText(request.getButtonText());
        }
        if (hasText(request.getTextAlign())) {
            banner.setTextAlign(request.getTextAlign().trim());
        }
        if (hasText(request.getBannerType())) {
            banner.setBannerType(request.getBannerType().trim());
        }
        if (request.getStatus() != null) {
            banner.setStatus(request.getStatus());
        }
        if (desktopImageFile != null && !desktopImageFile.isEmpty()) {
            banner.setDesktopImage(imageUploadService.uploadImage(desktopImageFile));
        }
        if (mobileImageFile != null && !mobileImageFile.isEmpty()) {
            banner.setMobileImage(imageUploadService.uploadImage(mobileImageFile));
        }

        validateBanner(banner);
        return bannerRepository.save(banner);
    }

    @Override
    public Banner updateBannerStatus(Long id, Integer status) {
        Banner banner = getBannerById(id);
        banner.setStatus(status);
        return bannerRepository.save(banner);
    }

    @Override
    public void deleteBanner(Long id) {
        Banner banner = getBannerById(id);
        bannerRepository.delete(banner);
    }

    @Override
    public List<BannerSetting> getAllSettings() {
        return bannerSettingRepository.findAll();
    }

    @Override
    public BannerSetting upsertSetting(String settingKey, String settingValue) {
        if (settingKey == null || settingKey.isBlank()) {
            throw new IllegalArgumentException("Setting key is required");
        }
        if (settingValue == null || settingValue.isBlank()) {
            throw new IllegalArgumentException("Setting value is required");
        }

        BannerSetting setting = bannerSettingRepository.findBySettingKey(settingKey)
                .orElseGet(BannerSetting::new);
        setting.setSettingKey(settingKey.trim());
        setting.setSettingValue(settingValue.trim());
        return bannerSettingRepository.save(setting);
    }

    private void validateBanner(Banner banner) {
        if (banner.getName() == null || banner.getName().isBlank()) {
            throw new IllegalArgumentException("Banner name is required");
        }
        if (banner.getDesktopImage() == null || banner.getDesktopImage().isBlank()) {
            throw new IllegalArgumentException("Desktop image is required");
        }
    }

    private Comparator<Banner> getSortOrder() {
        return Comparator
                .comparing(Banner::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Banner::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
