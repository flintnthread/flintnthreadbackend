package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.ColorRequest;
import com.ecommerce.sellerbackend.dto.ColorResponse;
import com.ecommerce.sellerbackend.entity.Color;
import com.ecommerce.sellerbackend.exception.DuplicateResourceException;
import com.ecommerce.sellerbackend.exception.ResourceNotFoundException;
import com.ecommerce.sellerbackend.repository.ColorRepository;
import com.ecommerce.sellerbackend.service.ColorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ColorServiceImpl implements ColorService {

    private final ColorRepository colorRepository;

    @Override
    public List<ColorResponse> listForSeller(Long sellerId) {
        return colorRepository.findVisibleForSeller(sellerId).stream()
                .map(color -> ColorResponse.from(color, sellerId))
                .toList();
    }

    @Override
    @Transactional
    public ColorResponse create(Long sellerId, ColorRequest request) {
        String hex = normalizeHex(request.getHex());
        if (colorRepository.existsVisibleCodeForSeller(sellerId, hex)) {
            throw new DuplicateResourceException("A color with hex code \"" + hex + "\" already exists.");
        }

        Color color = new Color();
        color.setColorName(request.getName().trim());
        color.setColorCode(hex);
        color.setStatus(request.isActive());
        color.setSellerId(sellerId);

        return ColorResponse.from(colorRepository.save(color), sellerId);
    }

    @Override
    @Transactional
    public ColorResponse update(Long sellerId, Long id, ColorRequest request) {
        Color color = getOwnedColor(sellerId, id);
        String hex = normalizeHex(request.getHex());

        if (colorRepository.existsVisibleCodeForSellerExcludingId(sellerId, hex, id)) {
            throw new DuplicateResourceException("A color with hex code \"" + hex + "\" already exists.");
        }

        color.setColorName(request.getName().trim());
        color.setColorCode(hex);
        color.setStatus(request.isActive());

        return ColorResponse.from(colorRepository.save(color), sellerId);
    }

    @Override
    @Transactional
    public void delete(Long sellerId, Long id) {
        Color color = colorRepository.findByIdAndSellerId(id, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Color not found or you can only delete colors you added."));
        colorRepository.delete(color);
    }

    private Color getOwnedColor(Long sellerId, Long id) {
        return colorRepository.findByIdAndSellerId(id, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Color not found or you can only edit colors you added."));
    }

    private String normalizeHex(String hex) {
        if (hex == null || hex.isBlank()) {
            throw new IllegalArgumentException("Color hex code is required.");
        }
        String v = hex.trim();
        if (!v.startsWith("#")) {
            v = "#" + v;
        }
        if (!v.matches("#[0-9A-Fa-f]{6}")) {
            throw new IllegalArgumentException("Invalid hex color code.");
        }
        return v.toUpperCase();
    }
}

