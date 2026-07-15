package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.entity.cms.HomepageBanner;
import com.ecommerce.adminbackend.repository.HomepageBannerRepository;
import com.ecommerce.adminbackend.service.HomepageBannerAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HomepageBannerAdminServiceImpl extends BaseAdminService implements HomepageBannerAdminService {

    private final HomepageBannerRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(String section) {
        List<HomepageBanner> banners = blankToNull(section) == null
                ? repository.findAllByOrderBySectionAscSortOrderAscPositionAscIdAsc()
                : repository.findBySectionIgnoreCaseOrderBySortOrderAscPositionAscIdAsc(section.trim());
        return banners.stream().map(this::toMap).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> get(Integer id) {
        return toMap(requireFound(repository.findById(id), "Homepage banner not found."));
    }

    @Override
    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        HomepageBanner banner = new HomepageBanner();
        apply(banner, body, true);
        return toMap(repository.save(banner));
    }

    @Override
    @Transactional
    public Map<String, Object> update(Integer id, Map<String, Object> body) {
        HomepageBanner banner = requireFound(repository.findById(id), "Homepage banner not found.");
        apply(banner, body, false);
        return toMap(repository.save(banner));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        requireFound(repository.findById(id), "Homepage banner not found.");
        repository.deleteById(id);
    }

    private void apply(HomepageBanner banner, Map<String, Object> body, boolean creating) {
        if (creating || body.containsKey("section")) {
            banner.setSection(requireNonBlank(stringAt(body, "section"), "section"));
        }
        if (creating || body.containsKey("position")) {
            banner.setPosition(toInt(body.get("position"), creating ? 1 : banner.getPosition()));
        }
        if (creating || body.containsKey("title")) {
            banner.setTitle(requireNonBlank(stringAt(body, "title"), "title"));
        }
        if (creating || body.containsKey("altText") || body.containsKey("alt_text")) {
            String alt = body.containsKey("altText") ? stringAt(body, "altText") : stringAt(body, "alt_text");
            banner.setAltText(requireNonBlank(alt, "altText"));
        }
        if (creating || body.containsKey("imagePath") || body.containsKey("image_path")) {
            String path = body.containsKey("imagePath") ? stringAt(body, "imagePath") : stringAt(body, "image_path");
            banner.setImagePath(requireNonBlank(path, "imagePath"));
        }
        if (body.containsKey("linkUrl") || body.containsKey("link_url")) {
            String link = body.containsKey("linkUrl") ? stringAt(body, "linkUrl") : stringAt(body, "link_url");
            banner.setLinkUrl(link == null || link.isBlank() ? "shop-grid.php" : link);
        } else if (creating) {
            banner.setLinkUrl("shop-grid.php");
        }
        if (body.containsKey("buttonText") || body.containsKey("button_text")) {
            banner.setButtonText(body.containsKey("buttonText")
                    ? stringAt(body, "buttonText")
                    : stringAt(body, "button_text"));
        }
        if (body.containsKey("isActive") || body.containsKey("is_active")) {
            Object value = body.containsKey("isActive") ? body.get("isActive") : body.get("is_active");
            banner.setIsActive(toBool(value, true));
        } else if (creating) {
            banner.setIsActive(true);
        }
        if (body.containsKey("sortOrder") || body.containsKey("sort_order")) {
            Object value = body.containsKey("sortOrder") ? body.get("sortOrder") : body.get("sort_order");
            banner.setSortOrder(toInt(value, 0));
        } else if (creating) {
            banner.setSortOrder(0);
        }
    }

    private int toInt(Object value, int defaultValue) {
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Numeric field must be an integer.");
        }
    }

    private boolean toBool(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = String.valueOf(value).trim().toLowerCase();
        if ("1".equals(text) || "true".equals(text) || "yes".equals(text)) {
            return true;
        }
        if ("0".equals(text) || "false".equals(text) || "no".equals(text)) {
            return false;
        }
        return defaultValue;
    }

    private Map<String, Object> toMap(HomepageBanner banner) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", banner.getId());
        row.put("section", banner.getSection());
        row.put("position", banner.getPosition());
        row.put("title", banner.getTitle());
        row.put("altText", banner.getAltText());
        row.put("imagePath", banner.getImagePath());
        row.put("linkUrl", banner.getLinkUrl());
        row.put("buttonText", banner.getButtonText());
        row.put("isActive", banner.getIsActive());
        row.put("sortOrder", banner.getSortOrder());
        row.put("createdAt", banner.getCreatedAt());
        row.put("updatedAt", banner.getUpdatedAt());
        return row;
    }
}
