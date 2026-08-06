package com.ecommerce.sellerbackend.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * First annual registration fee is deferred for {@code graceMonths} from seller join ({@code createdAt}).
 */
public final class RegistrationGraceHelper {

    private RegistrationGraceHelper() {
    }

    public record GraceSnapshot(
            LocalDateTime joinedAt,
            LocalDateTime graceEndsAt,
            boolean graceActive,
            boolean firstPaymentDue,
            long daysRemainingInGrace
    ) {
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
        return new GraceSnapshot(joined, graceEndsAt, graceActive, firstPaymentDue, daysRemaining);
    }

    /**
     * Subscription treated as active for access control when paid-active or still in unpaid grace.
     */
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

    /**
     * Payment is forced when first fee is due after grace, or when a prior subscription expired.
     */
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
