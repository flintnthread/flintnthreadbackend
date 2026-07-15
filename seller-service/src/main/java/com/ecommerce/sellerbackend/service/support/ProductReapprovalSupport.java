package com.ecommerce.sellerbackend.service.support;

import com.ecommerce.sellerbackend.entity.Product;

/**
 * After a seller edits a live (or previously rejected) product, return it to
 * {@code pending} so it is hidden from customers until admin re-approves —
 * same flow as a newly submitted listing.
 */
public final class ProductReapprovalSupport {

    private ProductReapprovalSupport() {
    }

    /**
     * @return true if status was changed to pending
     */
    public static boolean markPendingIfNeedsReapproval(Product product) {
        if (product == null) {
            return false;
        }
        String current = product.getStatus() != null ? product.getStatus().trim().toLowerCase() : "";
        if ("active".equals(current) || "approved".equals(current) || "rejected".equals(current)) {
            product.setStatus("pending");
            product.setReviewedAt(null);
            return true;
        }
        return false;
    }
}
