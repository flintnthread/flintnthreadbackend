package com.ecommerce.authdemo.service;

import org.json.JSONObject;

public interface RazorpayService {

    JSONObject createOrder(Double amount);

    boolean verifyPayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature);

    /** Public key id for Razorpay Checkout (same source as createOrder). */
    String getPublicKeyId();

    /** Display name on Razorpay Checkout (e.g. F&T). */
    String getCompanyName();

    /** Currency code (default INR). */
    String getCurrency();

    JSONObject createRefund(
            String paymentId,
            Double amount,
            String remarks
    );
}