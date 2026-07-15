package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.entity.cms.Banner;
import com.ecommerce.adminbackend.repository.BannerRepository;
import com.ecommerce.adminbackend.service.GeneralBannerAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GeneralBannerAdminServiceImpl extends BaseAdminService implements GeneralBannerAdminService {

    private static final Set<String> TEXT_POSITIONS = Set.of("left", "center", "right");
    private static final Set<String> SIZES = Set.of("small", "medium", "large", "full");

    private final BannerRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(String search, Integer status) {
        if (blankToNull(search) == null && status == null) {
            return repository.findAllByOrderByIdDesc().stream().map(this::toMap).toList();
        }
        return repository.search(blankToNull(search), status).stream().map(this::toMap).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> get(Integer id) {
        return toMap(requireFound(repository.findById(id), "Banner not found."));
    }

    @Override
    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        Banner banner = new Banner();
        apply(banner, body, true);
        return toMap(repository.save(banner));
    }

    @Override
    @Transactional
    public Map<String, Object> update(Integer id, Map<String, Object> body) {
        Banner banner = requireFound(repository.findById(id), "Banner not found.");
        apply(banner, body, false);
        return toMap(repository.save(banner));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        requireFound(repository.findById(id), "Banner not found.");
        repository.deleteById(id);
    }

    private void apply(Banner banner, Map<String, Object> body, boolean creating) {
        if (creating || body.containsKey("title")) {
            banner.setTitle(requireNonBlank(stringAt(body, "title"), "title"));
        }
        if (creating || body.containsKey("image")) {
            banner.setImage(requireNonBlank(stringAt(body, "image"), "image"));
        }
        if (body.containsKey("mobileImage") || body.containsKey("mobile_image")) {
            banner.setMobileImage(body.containsKey("mobileImage")
                    ? stringAt(body, "mobileImage")
                    : stringAt(body, "mobile_image"));
        }
        if (body.containsKey("textContent") || body.containsKey("text_content")) {
            banner.setTextContent(body.containsKey("textContent")
                    ? stringAt(body, "textContent")
                    : stringAt(body, "text_content"));
        }
        if (body.containsKey("buttonUrl") || body.containsKey("button_url")) {
            banner.setButtonUrl(body.containsKey("buttonUrl")
                    ? stringAt(body, "buttonUrl")
                    : stringAt(body, "button_url"));
        }
        if (body.containsKey("buttonText") || body.containsKey("button_text")) {
            String text = body.containsKey("buttonText")
                    ? stringAt(body, "buttonText")
                    : stringAt(body, "button_text");
            banner.setButtonText(text == null || text.isBlank() ? "Shop Now" : text);
        } else if (creating) {
            banner.setButtonText("Shop Now");
        }
        if (body.containsKey("textPosition") || body.containsKey("text_position")) {
            String pos = body.containsKey("textPosition")
                    ? stringAt(body, "textPosition")
                    : stringAt(body, "text_position");
            banner.setTextPosition(normalizeEnum(pos, TEXT_POSITIONS, "left", "textPosition"));
        } else if (creating) {
            banner.setTextPosition("left");
        }
        if (body.containsKey("size")) {
            banner.setSize(normalizeEnum(stringAt(body, "size"), SIZES, "full", "size"));
        } else if (creating) {
            banner.setSize("full");
        }
        if (body.containsKey("status")) {
            banner.setStatus(normalizeStatus(body.get("status")));
        } else if (creating) {
            banner.setStatus(1);
        }
    }

    private String normalizeEnum(String value, Set<String> allowed, String defaultValue, String label) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException(label + " must be one of " + allowed);
        }
        return normalized;
    }

    private Integer normalizeStatus(Object value) {
        if (value == null) {
            return 1;
        }
        if (value instanceof Boolean bool) {
            return bool ? 1 : 0;
        }
        String text = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        if ("active".equals(text) || "true".equals(text) || "1".equals(text)) {
            return 1;
        }
        if ("inactive".equals(text) || "false".equals(text) || "0".equals(text)) {
            return 0;
        }
        try {
            int parsed = Integer.parseInt(text);
            return parsed != 0 ? 1 : 0;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("status must be 1/0 or active/inactive.");
        }
    }

    private Map<String, Object> toMap(Banner banner) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", banner.getId());
        row.put("title", banner.getTitle());
        row.put("image", banner.getImage());
        row.put("mobileImage", banner.getMobileImage());
        row.put("textContent", banner.getTextContent());
        row.put("buttonUrl", banner.getButtonUrl());
        row.put("buttonText", banner.getButtonText());
        row.put("textPosition", banner.getTextPosition());
        row.put("size", banner.getSize());
        row.put("status", banner.getStatus());
        row.put("createdAt", banner.getCreatedAt());
        row.put("updatedAt", banner.getUpdatedAt());
        return row;
    }
}
