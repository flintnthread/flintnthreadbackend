package com.ecommerce.adminbackend.entity;

/** Matches admin_users.role column enum in the shared database. */
public enum AdminRole {
    super_admin,
    admin,
    product_management,
    order_management,
    sellers_management,
    category_management,
    finance_management
}
