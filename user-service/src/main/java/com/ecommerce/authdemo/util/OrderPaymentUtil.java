package com.ecommerce.authdemo.util;

import com.ecommerce.authdemo.entity.Order;

public final class OrderPaymentUtil {
    private OrderPaymentUtil() {}

    public static boolean isPaid(Order order) {
        if (order == null || order.getPaymentStatus() == null) return false;
        String ps = order.getPaymentStatus().trim().toLowerCase();
        return "paid".equals(ps) || "success".equals(ps) || "completed".equals(ps);
    }

    public static boolean isPrepaidOnline(Order order) {
        if (order == null || order.getPaymentMethod() == null) return false;
        String pm = order.getPaymentMethod().trim().toLowerCase();
        return "razorpay".equals(pm) || "online".equals(pm) || "prepaid".equals(pm)
                || "upi".equals(pm) || pm.contains("card") || "netbanking".equals(pm);
    }

    public static boolean isCod(Order order) {
        if (order == null || order.getPaymentMethod() == null) return false;
        String pm = order.getPaymentMethod().trim().toLowerCase();
        return pm.contains("cod") || pm.contains("cash");
    }
}
