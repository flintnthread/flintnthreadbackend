package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.entity.ads.AdsUser;
import com.ecommerce.adminbackend.repository.AdsOrderRepository;
import com.ecommerce.adminbackend.repository.AdsUserRepository;
import com.ecommerce.adminbackend.service.AdsCustomerAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdsCustomerAdminServiceImpl extends BaseAdminService implements AdsCustomerAdminService {

    private final AdsUserRepository adsUserRepository;
    private final AdsOrderRepository adsOrderRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> list(String search, int page, int size) {
        var result = adsUserRepository.search(blankToNull(search), PageRequest.of(page, size));
        return PageResponse.from(result.map(this::toMap));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> get(Integer id) {
        AdsUser user = requireFound(adsUserRepository.findById(id), "Ads customer not found.");
        Map<String, Object> detail = toMap(user);
        detail.put("orderCount", adsOrderRepository.countByUserId(id));
        return detail;
    }

    @Override
    @Transactional
    public Map<String, Object> delete(Integer id) {
        requireFound(adsUserRepository.findById(id), "Ads customer not found.");
        long orders = adsOrderRepository.countByUserId(id);
        adsUserRepository.deleteById(id);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", orders > 0
                ? "Ads customer deleted. Related orders were cascade-deleted by the database (" + orders + ")."
                : "Ads customer deleted.");
        response.put("cascadedOrders", orders);
        return response;
    }

    private Map<String, Object> toMap(AdsUser user) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", user.getId());
        row.put("name", user.getName());
        row.put("email", user.getEmail());
        row.put("phone", user.getPhone());
        row.put("company", user.getCompany());
        row.put("address", user.getAddress());
        row.put("city", user.getCity());
        row.put("state", user.getState());
        row.put("pincode", user.getPincode());
        row.put("createdAt", user.getCreatedAt());
        row.put("updatedAt", user.getUpdatedAt());
        row.put("orderCount", adsOrderRepository.countByUserId(user.getId()));
        return row;
    }
}
