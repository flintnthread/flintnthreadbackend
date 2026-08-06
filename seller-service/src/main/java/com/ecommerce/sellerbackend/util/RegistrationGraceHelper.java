package com.ecommerce.sellerbackend.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * First annual registration fee is deferred from seller join ({@code createdAt}):
 * <ul>
 *   <li>New sellers (joined on/after cutoff): {@code newSellerGraceMonths} (default 3)</li>
 *   <li>Existing sellers (joined before cutoff): {@code existingSellerGraceMonths} (default 12)</li>
 * </ul>
 */
public final class RegistrationGraceHelper {

    private RegistrationGraceHelper() {
    }

    public record GraceSnapshot(
            LocalDateTime joinedAt,
            LocalDateTime graceEndsAt,
            boolean graceActive,
            boolean firstPaymentDue,
            long daysRemainingInGrace,
            int gracePeriodMonths
    ) {
    }

    public static int resolveGraceMonths(
            LocalDateTime joinedAt,
            LocalDateTime newSellerAfter,
            int newSellerGraceMonths,
            int existingSellerGraceMonths
    ) {
        int neu = Math.max(newSellerGraceMonths, 0);
        int existing = Math.max(existingSellerGraceMonths, 0);
        if (joinedAt == null || newSellerAfter == null || !joinedAt.isBefore(newSellerAfter)) {
            return neu;
        }
        return existing;
    }

    public static GraceSnapshot compute(
            LocalDateTime joinedAt,
            int graceMonths,
            boolean hasEverPaid,
            LocalDateTime now
    ) {
        LocalDateTime joined = joinedAt != null ? joinedAt : now;
        int months = Math.max(graceMonths, 0);
        LocalDateTime graceEndsAt = joined.plusMonths(months);
        boolean graceActive = !hasEverPaid && now.isBefore(graceEndsAt);
        boolean firstPaymentDue = !hasEverPaid && !now.isBefore(graceEndsAt);
        long daysRemaining = 0;
        if (graceActive) {
            daysRemaining = ChronoUnit.DAYS.between(now.toLocalDate(), graceEndsAt.toLocalDate());
            if (daysRemaining <= 0) {
                daysRemaining = 1;
            }
        }
        return new GraceSnapshot(joined, graceEndsAt, graceActive, firstPaymentDue, daysRemaining, months);
    }

    public static boolean resolveSubscriptionActive(
            boolean profileCompleted,
            boolean subscriptionRowActive,
            boolean graceActive
    ) {
        if (!profileCompleted) {
            return true;
        }
        return subscriptionRowActive || graceActive;
    }

    public static boolean resolvePaymentPending(
            boolean profileCompleted,
            boolean subscriptionRowActive,
            boolean hasEverPaid,
            boolean firstPaymentDue
    ) {
        if (!profileCompleted) {
            return false;
        }
        if (subscriptionRowActive) {
            return false;
        }
        return firstPaymentDue || hasEverPaid;
    }
}
