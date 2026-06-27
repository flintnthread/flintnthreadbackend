package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.entity.Banner;
import com.ecommerce.authdemo.entity.BannerSetting;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BannerService {

    List<Banner> getAllBanners();

    List<Banner> getActiveBanners();

    Banner getBannerById(Long id);

    Banner createBanner(Banner banner, MultipartFile desktopImageFile, MultipartFile mobileImageFile);

    Banner updateBanner(Long id, Banner banner, MultipartFile desktopImageFile, MultipartFile mobileImageFile);

    Banner updateBannerStatus(Long id, Integer status);

    void deleteBanner(Long id);

    List<BannerSetting> getAllSettings();

    BannerSetting upsertSetting(String settingKey, String settingValue);
}
