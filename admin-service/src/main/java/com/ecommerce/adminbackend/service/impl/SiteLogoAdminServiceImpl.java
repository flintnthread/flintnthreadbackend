package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.entity.cms.SiteLogo;
import com.ecommerce.adminbackend.repository.SiteLogoRepository;
import com.ecommerce.adminbackend.service.CatalogImageStorageService;
import com.ecommerce.adminbackend.service.SiteLogoAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SiteLogoAdminServiceImpl extends BaseAdminService implements SiteLogoAdminService {

    private final SiteLogoRepository repository;
    private final CatalogImageStorageService catalogImageStorageService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> get() {
        return toMap(requireOrCreate());
    }

    @Override
    @Transactional
    public Map<String, Object> update(Map<String, Object> body) {
        SiteLogo logo = requireOrCreate();
        if (body.containsKey("logoDark") || body.containsKey("logo_dark")) {
            logo.setLogoDark(body.containsKey("logoDark")
                    ? stringAt(body, "logoDark")
                    : stringAt(body, "logo_dark"));
        }
        if (body.containsKey("logoLight") || body.containsKey("logo_light")) {
            logo.setLogoLight(body.containsKey("logoLight")
                    ? stringAt(body, "logoLight")
                    : stringAt(body, "logo_light"));
        }
        if (body.containsKey("favicon")) {
            logo.setFavicon(stringAt(body, "favicon"));
        }
        return toMap(repository.save(logo));
    }

    @Override
    @Transactional
    public Map<String, Object> upload(String slot, MultipartFile file) {
        if (slot == null || slot.isBlank()) {
            throw new IllegalArgumentException("slot is required (logoDark, logoLight, or favicon).");
        }
        String normalized = slot.trim().toLowerCase(Locale.ROOT)
                .replace("-", "")
                .replace("_", "");
        String path = catalogImageStorageService.storeCmsMedia(file, "logos");
        SiteLogo logo = requireOrCreate();
        switch (normalized) {
            case "logodark" -> logo.setLogoDark(path);
            case "logolight" -> logo.setLogoLight(path);
            case "favicon" -> logo.setFavicon(path);
            default -> throw new IllegalArgumentException("slot must be logoDark, logoLight, or favicon.");
        }
        Map<String, Object> response = toMap(repository.save(logo));
        response.put("uploadedPath", path);
        response.put("slot", slot);
        return response;
    }

    private SiteLogo requireOrCreate() {
        return repository.findById(1)
                .or(() -> repository.findFirstByOrderByIdAsc())
                .orElseGet(() -> {
                    SiteLogo created = new SiteLogo();
                    return repository.save(created);
                });
    }

    private Map<String, Object> toMap(SiteLogo logo) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", logo.getId());
        row.put("logoDark", logo.getLogoDark());
        row.put("logoLight", logo.getLogoLight());
        row.put("favicon", logo.getFavicon());
        row.put("updatedAt", logo.getUpdatedAt());
        return row;
    }
}
