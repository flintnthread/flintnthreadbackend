package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.entity.CategoryRequest;
import com.ecommerce.adminbackend.entity.CategoryRequestStatus;
import com.ecommerce.adminbackend.entity.Seller;
import com.ecommerce.adminbackend.repository.CategoryRequestRepository;
import com.ecommerce.adminbackend.repository.SellerRepository;
import com.ecommerce.adminbackend.security.AdminSecurityUtils;
import com.ecommerce.adminbackend.service.CategoryRequestAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import com.ecommerce.adminbackend.util.TextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryRequestAdminServiceImpl extends BaseAdminService implements CategoryRequestAdminService {

    private final CategoryRequestRepository categoryRequestRepository;
    private final SellerRepository sellerRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listRequests(String status, int page, int size) {
        var pageable = PageRequest.of(page, size);
        CategoryRequestStatus statusEnum = TextUtils.parseEnum(status, CategoryRequestStatus.class, "category request status");
        var result = statusEnum != null
                ? categoryRequestRepository.findByStatusOrderByCreatedAtDesc(statusEnum, pageable)
                : categoryRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PageResponse.from(result.map(this::toSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        long pending = categoryRequestRepository.countByStatus(CategoryRequestStatus.pending);
        long approved = categoryRequestRepository.countByStatus(CategoryRequestStatus.approved);
        long rejected = categoryRequestRepository.countByStatus(CategoryRequestStatus.rejected);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", pending + approved + rejected);
        stats.put("pending", pending);
        stats.put("approved", approved);
        stats.put("rejected", rejected);
        return stats;
    }

    @Override
    @Transactional
    public Map<String, Object> approve(Long id, String note) {
        CategoryRequest request = requirePending(id);
        request.setStatus(CategoryRequestStatus.approved);
        request.setAdminNotes(note);
        request.setProcessedBy(AdminSecurityUtils.currentAdminId());
        request.setProcessedAt(LocalDateTime.now());
        categoryRequestRepository.save(request);
        return Map.of("requestId", id, "status", "approved", "message", "Category request approved.");
    }

    @Override
    @Transactional
    public Map<String, Object> reject(Long id, String note) {
        CategoryRequest request = requirePending(id);
        request.setStatus(CategoryRequestStatus.rejected);
        request.setAdminNotes(note != null && !note.isBlank() ? note.trim() : "Rejected.");
        request.setProcessedBy(AdminSecurityUtils.currentAdminId());
        request.setProcessedAt(LocalDateTime.now());
        categoryRequestRepository.save(request);
        return Map.of("requestId", id, "status", "rejected", "message", "Category request rejected.");
    }

    private CategoryRequest requirePending(Long id) {
        CategoryRequest request = requireFound(
                categoryRequestRepository.findById(id),
                "Category request not found.");
        if (request.getStatus() != CategoryRequestStatus.pending) {
            throw new IllegalArgumentException("Category request is not pending.");
        }
        return request;
    }

    private Map<String, Object> toSummary(CategoryRequest request) {
        Seller seller = sellerRepository.findById(request.getSellerId()).orElse(null);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", request.getId());
        summary.put("sellerId", request.getSellerId());
        summary.put("sellerName", seller != null ? seller.getFullName() : null);
        summary.put("sellerEmail", seller != null ? seller.getEmail() : null);
        summary.put("categoryName", request.getCategoryName());
        summary.put("description", request.getDescription());
        summary.put("reason", request.getReason());
        summary.put("status", request.getStatus().name());
        summary.put("adminNotes", request.getAdminNotes());
        summary.put("createdAt", request.getCreatedAt());
        return summary;
    }
}
