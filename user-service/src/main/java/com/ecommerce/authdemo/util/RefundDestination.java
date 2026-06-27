package com.ecommerce.authdemo.util;

public final class RefundDestination {
    public static final String SOURCE = "source";
    public static final String FNT_WALLET = "fnt_wallet";

    private RefundDestination() {}

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return SOURCE;
        String v = raw.trim().toLowerCase();
        if (FNT_WALLET.equals(v) || "wallet".equals(v)) return FNT_WALLET;
        return SOURCE;
    }
}
