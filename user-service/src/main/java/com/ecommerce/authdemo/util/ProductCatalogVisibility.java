package com.ecommerce.authdemo.util;

import com.ecommerce.authdemo.entity.Product;
import org.springframework.data.jpa.domain.Specification;

/**
 * User catalog shows only admin-approved products ({@code status = active}).
 */
public final class ProductCatalogVisibility {

    public static final String USER_VISIBLE_STATUS = "active";

    private ProductCatalogVisibility() {}

    public static boolean isVisibleToUsers(Product product) {
        return product != null
                && product.getStatus() != null
                && USER_VISIBLE_STATUS.equalsIgnoreCase(product.getStatus().trim());
    }

    public static Specification<Product> visibleToUsers() {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("status")), USER_VISIBLE_STATUS);
    }
}
