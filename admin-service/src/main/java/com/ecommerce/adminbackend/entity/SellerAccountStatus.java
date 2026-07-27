package com.ecommerce.adminbackend.entity;

public enum SellerAccountStatus {
    active,
    inactive,
    pending,
    email_pending,
    suspended,
    rejected,
    /** Deactivation request pending admin approval (fits VARCHAR(20)). */
    deact_req,
    /** Activation request pending admin approval while account is deactivated. */
    act_req
}
