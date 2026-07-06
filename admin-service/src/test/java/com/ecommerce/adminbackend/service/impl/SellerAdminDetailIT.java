package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.service.SellerAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class SellerAdminDetailIT {

    @Autowired
    private SellerAdminService sellerAdminService;

    @Test
    void listAdminApprovedSellers_returnsRows() {
        PageResponse<Map<String, Object>> page = sellerAdminService.listAdminApprovedSellers(null, 0, 5);
        assertNotNull(page);
        assertNotNull(page.items());
        assertFalse(page.items().isEmpty(), "Expected at least one approved seller in database");
    }

    @Test
    void getSeller_returnsKycAndProfileFields() {
        PageResponse<Map<String, Object>> page = sellerAdminService.listAdminApprovedSellers(null, 0, 1);
        Long sellerId = ((Number) page.items().get(0).get("id")).longValue();

        Map<String, Object> detail = sellerAdminService.getSeller(sellerId);

        assertNotNull(detail.get("firstName"));
        assertNotNull(detail.get("email"));
        assertNotNull(detail.get("kycStatusLabel"));
        assertNotNull(detail.get("kycVerificationStatus"));
        assertNotNull(detail.get("kycImageCount"));
        assertNotNull(detail.get("documents"));
        assertNotNull(detail.get("liveSelfieImages"));
        assertTrue(detail.containsKey("kycSubmittedAt"));
        assertTrue(detail.containsKey("kycVerifiedAt"));
        assertTrue(detail.containsKey("kycRemarks"));
        assertTrue(detail.containsKey("sellerUniqueId"));
        assertTrue(detail.containsKey("warehouseAddress"));
    }

    @Test
    void getSeller_withKnownId_matchesDatabaseUniqueId() {
        Map<String, Object> detail = sellerAdminService.getSeller(55L);
        assertEquals("FNT-SELLER-001", detail.get("sellerUniqueId"));
        assertEquals("Saranya", detail.get("firstName"));
        assertEquals(true, detail.get("kycVerified"));
        assertNotNull(detail.get("kycVerifiedAt"));
    }

    @Test
    void updateSellerStatus_persistsKycRemarks() {
        Map<String, Object> before = sellerAdminService.getSeller(55L);
        String originalRemarks = before.get("kycRemarks") != null ? before.get("kycRemarks").toString() : "";
        String testRemark = originalRemarks + " [integration-test]";

        Map<String, Object> updated = sellerAdminService.updateSellerStatus(
                55L, "active", "verified", testRemark);

        assertEquals("Verified", updated.get("kycVerificationStatus"));
        assertEquals(testRemark, updated.get("kycRemarks"));

        sellerAdminService.updateSellerStatus(55L, "active", "verified", originalRemarks);
    }
}
