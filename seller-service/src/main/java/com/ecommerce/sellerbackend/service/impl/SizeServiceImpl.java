package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.SizeRequest;
import com.ecommerce.sellerbackend.dto.SizeResponse;
import com.ecommerce.sellerbackend.entity.Size;
import com.ecommerce.sellerbackend.exception.DuplicateResourceException;
import com.ecommerce.sellerbackend.exception.ResourceNotFoundException;
import com.ecommerce.sellerbackend.repository.SizeRepository;
import com.ecommerce.sellerbackend.service.SizeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SizeServiceImpl implements SizeService {

    private final SizeRepository sizeRepository;

    @Override
    public List<SizeResponse> listForSeller(Long sellerId) {
        return sizeRepository.findVisibleForSeller(sellerId).stream()
                .map(size -> SizeResponse.from(size, sellerId))
                .toList();
    }

    @Override
    @Transactional
    public SizeResponse create(Long sellerId, SizeRequest request) {
        String code = normalizeCode(request.getCode());
        if (sizeRepository.existsVisibleCodeForSeller(sellerId, code)) {
            throw new DuplicateResourceException("A size with code \"" + code + "\" already exists.");
        }

        Size size = new Size();
        size.setSizeName(request.getName().trim());
        size.setSizeCode(code);
        size.setStatus(request.isActive());
        size.setSellerId(sellerId);

        return SizeResponse.from(sizeRepository.save(size), sellerId);
    }

    @Override
    @Transactional
    public SizeResponse update(Long sellerId, Long id, SizeRequest request) {
        Size size = getOwnedSize(sellerId, id);
        String code = normalizeCode(request.getCode());

        if (sizeRepository.existsVisibleCodeForSellerExcludingId(sellerId, code, id)) {
            throw new DuplicateResourceException("A size with code \"" + code + "\" already exists.");
        }

        size.setSizeName(request.getName().trim());
        size.setSizeCode(code);
        size.setStatus(request.isActive());

        return SizeResponse.from(sizeRepository.save(size), sellerId);
    }

    @Override
    @Transactional
    public void delete(Long sellerId, Long id) {
        Size size = sizeRepository.findByIdAndSellerId(id, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Size not found or you cannot delete shared catalog sizes."));
        sizeRepository.delete(size);
    }

    private Size getOwnedSize(Long sellerId, Long id) {
        return sizeRepository.findByIdAndSellerId(id, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Size not found or you can only edit sizes you added."));
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Size code is required.");
        }
        return code.trim().toUpperCase();
    }
}

