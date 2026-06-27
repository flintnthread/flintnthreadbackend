package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.BannerSettingRequest;
import com.ecommerce.authdemo.entity.Banner;
import com.ecommerce.authdemo.entity.BannerSetting;
import com.ecommerce.authdemo.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @GetMapping
    public ResponseEntity<List<Banner>> getAllBanners() {
        return ResponseEntity.ok(bannerService.getAllBanners());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Banner>> getActiveBanners() {
        return ResponseEntity.ok(bannerService.getActiveBanners());
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<Banner> getBannerById(@PathVariable Long id) {
        return ResponseEntity.ok(bannerService.getBannerById(id));
    }

    @PostMapping
    public ResponseEntity<Banner> createBanner(
            @RequestParam String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "targetUrl", required = false) String targetUrl,
            @RequestParam(value = "buttonText", required = false) String buttonText,
            @RequestParam(value = "textAlign", required = false) String textAlign,
            @RequestParam(value = "bannerType", required = false) String bannerType,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "desktopImage", required = false) String desktopImage,
            @RequestParam(value = "mobileImage", required = false) String mobileImage,
            @RequestParam(value = "desktopImageFile", required = false) MultipartFile desktopImageFile,
            @RequestParam(value = "mobileImageFile", required = false) MultipartFile mobileImageFile
    ) {
        Banner banner = new Banner();
        banner.setName(name);
        banner.setDescription(description);
        banner.setTargetUrl(targetUrl);
        banner.setButtonText(buttonText);
        banner.setTextAlign(textAlign);
        banner.setBannerType(bannerType);
        banner.setStatus(status);
        banner.setDesktopImage(desktopImage);
        banner.setMobileImage(mobileImage);
        return ResponseEntity.ok(bannerService.createBanner(banner, desktopImageFile, mobileImageFile));
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<Banner> updateBanner(
            @PathVariable Long id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "targetUrl", required = false) String targetUrl,
            @RequestParam(value = "buttonText", required = false) String buttonText,
            @RequestParam(value = "textAlign", required = false) String textAlign,
            @RequestParam(value = "bannerType", required = false) String bannerType,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "desktopImage", required = false) String desktopImage,
            @RequestParam(value = "mobileImage", required = false) String mobileImage,
            @RequestParam(value = "desktopImageFile", required = false) MultipartFile desktopImageFile,
            @RequestParam(value = "mobileImageFile", required = false) MultipartFile mobileImageFile
    ) {
        Banner banner = new Banner();
        banner.setName(name);
        banner.setDescription(description);
        banner.setTargetUrl(targetUrl);
        banner.setButtonText(buttonText);
        banner.setTextAlign(textAlign);
        banner.setBannerType(bannerType);
        banner.setStatus(status);
        banner.setDesktopImage(desktopImage);
        banner.setMobileImage(mobileImage);
        return ResponseEntity.ok(bannerService.updateBanner(id, banner, desktopImageFile, mobileImageFile));
    }

    @PutMapping("/{id:\\d+}/images")
    public ResponseEntity<Banner> updateBannerImages(
            @PathVariable Long id,
            @RequestParam(value = "desktopImageFile", required = false) MultipartFile desktopImageFile,
            @RequestParam(value = "mobileImageFile", required = false) MultipartFile mobileImageFile
    ) {
        return ResponseEntity.ok(
                bannerService.updateBanner(id, new Banner(), desktopImageFile, mobileImageFile)
        );
    }

    @PutMapping("/{id:\\d+}/status")
    public ResponseEntity<Banner> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return ResponseEntity.ok(bannerService.updateBannerStatus(id, status));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteBanner(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/settings")
    public ResponseEntity<List<BannerSetting>> getSettings() {
        return ResponseEntity.ok(bannerService.getAllSettings());
    }

    @PutMapping("/settings/{settingKey}")
    public ResponseEntity<BannerSetting> upsertSetting(@PathVariable String settingKey,
                                                       @RequestBody BannerSettingRequest request) {
        return ResponseEntity.ok(
                bannerService.upsertSetting(settingKey, request.getValue())
        );
    }
}
