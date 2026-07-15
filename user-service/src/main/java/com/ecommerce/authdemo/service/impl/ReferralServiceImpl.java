package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.ReferralDashboardDto;
import com.ecommerce.authdemo.dto.ReferralResponse;
import com.ecommerce.authdemo.dto.ReferralRewardStatusDto;
import com.ecommerce.authdemo.dto.ShareDto;
import com.ecommerce.authdemo.entity.ReferralOrderDiscountRedemption;
import com.ecommerce.authdemo.entity.User;
import com.ecommerce.authdemo.repository.OrderRepository;
import com.ecommerce.authdemo.repository.ReferralDiscountRepository;
import com.ecommerce.authdemo.repository.ReferralTransactionRepository;
import com.ecommerce.authdemo.repository.UserRepository;
import com.ecommerce.authdemo.service.ReferralService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReferralServiceImpl implements ReferralService {

    private final UserRepository userRepository;
    private final ReferralTransactionRepository transactionRepository;
    private final ReferralDiscountRepository discountRepository;
    private final OrderRepository orderRepository;

    private static final int TARGET_REFERRALS = 10;
    private static final BigDecimal DISCOUNT_PERCENT_POINTS = new BigDecimal("10");
    private static final SecureRandom REF_CODE_RANDOM = new SecureRandom();

    /**
     * Preferred format: {@code FNTSRAV004716} — {@code FNT} + 4-char username slug + 6 digits.
     */
    private static final String REF_CODE_TAG = "FNT";
    /** Legacy brand prefix previously used before {@link #REF_CODE_TAG}. */
    private static final String LEGACY_REF_CODE_TAG = "REF";

    @Override
    public int getRequiredReferralsForReward() {
        return TARGET_REFERRALS;
    }

    @Override
    public void generateCodes(Long userId, String username) {

        User user = userRepository.findById(userId).orElseThrow();

        user.setCompanyReferenceId(
                "f&t" + String.format("%06d", userId)
        );

        user.setReferralCode(buildReferralCode(userId, username));
        userRepository.save(user);
    }

    /** First 4 alphanumeric chars of username, uppercased and padded if short. */
    private String usernameSlug(String username) {
        String cleaned = username == null
                ? ""
                : username.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (cleaned.isEmpty()) {
            cleaned = "USER";
        }
        if (cleaned.length() >= 4) {
            return cleaned.substring(0, 4);
        }
        StringBuilder padded = new StringBuilder(cleaned);
        while (padded.length() < 4) {
            padded.append('X');
        }
        return padded.toString();
    }

    private String buildReferralCode(Long userId, String username) {
        String prefix = REF_CODE_TAG + usernameSlug(username);
        for (int attempt = 0; attempt < 100; attempt++) {
            int suffixNum = REF_CODE_RANDOM.nextInt(1_000_000);
            String code = prefix + String.format("%06d", suffixNum);
            if (userRepository.findByReferralCode(code).isEmpty()) {
                return code;
            }
        }
        long fallback = (userId + System.currentTimeMillis()) % 1_000_000L;
        return prefix + String.format("%06d", fallback);
    }

    private boolean hasObsoleteRefBrandPrefix(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String upper = code.trim().toUpperCase();
        return upper.startsWith(LEGACY_REF_CODE_TAG) && !upper.startsWith(REF_CODE_TAG);
    }

    /** {@code FNTSRAV004716} / {@code REFSRAV004716} style (brand + 4-char slug + 6 digits). */
    private boolean hasUsernameSlugInCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String upper = code.trim().toUpperCase();
        if (upper.startsWith(LEGACY_REF_CODE_TAG) && !upper.startsWith(REF_CODE_TAG)) {
            upper = REF_CODE_TAG + upper.substring(LEGACY_REF_CODE_TAG.length());
        }
        return upper.matches("^FNT[A-Z0-9]{4}\\d{6}$");
    }

    /** Brand + 6 digits only, e.g. {@code FNT004716}. */
    private boolean isDigitsOnlyFntCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return code.trim().toUpperCase().matches("^FNT\\d{6}$");
    }

    /** {@code FNTSRAV004716} / {@code REFSRAV004716} → {@code FNT004716} (lookup helper only). */
    private String stripUsernameSlugFromCode(String code) {
        String upper = code.trim().toUpperCase();
        if (upper.startsWith(LEGACY_REF_CODE_TAG) && !upper.startsWith(REF_CODE_TAG)) {
            upper = REF_CODE_TAG + upper.substring(LEGACY_REF_CODE_TAG.length());
        }
        if (upper.matches("^FNT[A-Z0-9]{4}\\d{6}$")) {
            return REF_CODE_TAG + upper.substring(7);
        }
        return upper;
    }

    /** {@code REFSRAV004716} → {@code FNTSRAV004716} */
    private String toFntBrandPrefix(String code) {
        String upper = code.trim().toUpperCase();
        if (upper.startsWith(LEGACY_REF_CODE_TAG) && upper.length() > LEGACY_REF_CODE_TAG.length()) {
            return REF_CODE_TAG + upper.substring(LEGACY_REF_CODE_TAG.length());
        }
        return upper;
    }

    /**
     * Keep / restore {@code FNT + username slug + digits}. Convert legacy {@code REF…}
     * to {@code FNT…}. Do not strip the username slug.
     */
    private User ensureModernReferralCode(User user) {
        Long userId = user.getId();
        String code = user.getReferralCode();
        if (code == null || code.isBlank()) {
            generateCodes(userId, user.getUsername());
            return userRepository.findById(userId).orElseThrow();
        }

        String upper = code.trim().toUpperCase();

        // Legacy REF… → FNT… (preserve slug + digits).
        if (hasObsoleteRefBrandPrefix(upper)) {
            String branded = toFntBrandPrefix(upper);
            if (!branded.equalsIgnoreCase(upper)
                    && userRepository.findByReferralCode(branded).isEmpty()) {
                user.setReferralCode(branded);
                userRepository.save(user);
                upper = branded;
            } else if (!branded.equalsIgnoreCase(upper)) {
                // Collision on branded value — leave REF code as-is for lookup compatibility.
                return user;
            } else {
                upper = branded;
            }
        }

        // Already has username slug — keep it.
        if (hasUsernameSlugInCode(upper)) {
            if (!upper.equals(code.trim())) {
                user.setReferralCode(upper);
                userRepository.save(user);
            }
            return user;
        }

        // FNT + 6 digits only → restore username slug, keep the same 6 digits.
        if (isDigitsOnlyFntCode(upper)) {
            String digits = upper.substring(REF_CODE_TAG.length());
            String restored = REF_CODE_TAG + usernameSlug(user.getUsername()) + digits;
            if (!restored.equals(upper) && userRepository.findByReferralCode(restored).isEmpty()) {
                user.setReferralCode(restored);
                userRepository.save(user);
                return user;
            }
            // Collision: keep existing short code rather than breaking shared links.
            return user;
        }

        return user;
    }

    @Override
    public Optional<User> findReferrerByReferralCode(String referralCode) {
        String code = referralCode == null ? "" : referralCode.trim().toUpperCase();
        if (code.isEmpty()) {
            return Optional.empty();
        }
        Optional<User> direct = userRepository.findByReferralCode(code);
        if (direct.isPresent()) {
            return direct;
        }
        // Accept FNT… when DB still has legacy REF… (and the reverse during migration).
        if (code.startsWith(REF_CODE_TAG) && code.length() > REF_CODE_TAG.length()) {
            Optional<User> legacy = userRepository.findByReferralCode(
                    LEGACY_REF_CODE_TAG + code.substring(REF_CODE_TAG.length())
            );
            if (legacy.isPresent()) {
                return legacy;
            }
        }
        if (code.startsWith(LEGACY_REF_CODE_TAG) && code.length() > LEGACY_REF_CODE_TAG.length()) {
            Optional<User> branded = userRepository.findByReferralCode(
                    REF_CODE_TAG + code.substring(LEGACY_REF_CODE_TAG.length())
            );
            if (branded.isPresent()) {
                return branded;
            }
        }
        // Accept slug codes (FNTSRAV004716) against short FNT004716 in DB.
        String stripped = stripUsernameSlugFromCode(code);
        if (!stripped.equals(code)) {
            Optional<User> byStripped = userRepository.findByReferralCode(stripped);
            if (byStripped.isPresent()) {
                return byStripped;
            }
            if (stripped.startsWith(REF_CODE_TAG)) {
                Optional<User> legacyStripped = userRepository.findByReferralCode(
                        LEGACY_REF_CODE_TAG + stripped.substring(REF_CODE_TAG.length())
                );
                if (legacyStripped.isPresent()) {
                    return legacyStripped;
                }
            }
        }
        // Accept short FNT004716 against slug form FNT????004716 in DB.
        if (isDigitsOnlyFntCode(code)) {
            String sixDigits = code.substring(REF_CODE_TAG.length());
            List<User> slugMatches = userRepository.findByReferralCodeSlugEndingWith(sixDigits);
            if (slugMatches.size() == 1) {
                return Optional.of(slugMatches.get(0));
            }
        }
        return Optional.empty();
    }

    @Override
    public ReferralResponse applyReferral(Long userId, String referralCode) {

        User user = userRepository.findById(userId).orElseThrow();

        if (user.getReferredBy() != null) {
            return new ReferralResponse(false, "Referral already used");
        }

        String code = referralCode == null ? "" : referralCode.trim().toUpperCase();
        if (code.isEmpty()) {
            return new ReferralResponse(false, "Enter a referral code");
        }

        User referrer = findReferrerByReferralCode(code)
                .orElseThrow(() -> new RuntimeException("Invalid referral code"));

        if (referrer.getId().equals(userId)) {
            return new ReferralResponse(false, "Own code not allowed");
        }

        user.setReferredBy(referrer.getId());
        userRepository.save(user);

        return new ReferralResponse(true, "Referral code saved");
    }

    @Override
    public BigDecimal calculateFirstOrderDiscount(
            Long userId,
            BigDecimal subtotal) {

        User user = userRepository.findById(userId).orElseThrow();

        if (!inviterDiscountEligible(user, userId)) {
            return BigDecimal.ZERO;
        }

        return subtotal.multiply(new BigDecimal("0.10"));
    }

    @Override
    public BigDecimal getAvailableReferralDiscountPercentForUser(Long userId) {

        User user = userRepository.findById(userId).orElseThrow();

        if (!inviterDiscountEligible(user, userId)) {
            return BigDecimal.ZERO;
        }

        return DISCOUNT_PERCENT_POINTS;
    }

    private boolean inviterDiscountEligible(User user, Long userId) {
        if (orderRepository.existsByUserIdAndReferralInviterDiscountAppliedTrue(userId)) {
            return false;
        }
        return Boolean.TRUE.equals(user.getRewardUnlocked())
                && Boolean.TRUE.equals(user.getDiscountAvailable())
                && !Boolean.TRUE.equals(user.getFirstOrderCompleted());
    }

    @Override
    public void markReferralDiscountUsed(Long userId, Long orderId) {

        ReferralOrderDiscountRedemption redemption =
                new ReferralOrderDiscountRedemption();

        redemption.setUserId(userId);
        redemption.setOrderId(orderId);
        redemption.setDiscountAmount(BigDecimal.ZERO);
        redemption.setCartSubtotal(BigDecimal.ZERO);

        discountRepository.save(redemption);
    }

    @Override
    public void confirmReferralOnFirstPaidOrder(Long userId) {
        // use order lifecycle in OrderServiceImpl
    }

    @Override
    public ReferralDashboardDto getDashboard(Long userId) {

        if (userId == null) {
            throw new RuntimeException("UserId is null");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        user = ensureModernReferralCode(user);

        int invites = user.getReferralCount() != null ? user.getReferralCount() : 0;

        int remaining = TARGET_REFERRALS - invites;
        if (remaining < 0) {
            remaining = 0;
        }

        boolean alreadyUsedReferral = user.getReferredBy() != null;
        boolean rewardUnlocked = Boolean.TRUE.equals(user.getRewardUnlocked());
        boolean discountAvailable = Boolean.TRUE.equals(user.getDiscountAvailable())
                && rewardUnlocked
                && !Boolean.TRUE.equals(user.getFirstOrderCompleted());

        return new ReferralDashboardDto(
                user.getId(),
                user.getReferralCode(),
                invites,
                TARGET_REFERRALS,
                remaining,
                rewardUnlocked,
                alreadyUsedReferral,
                discountAvailable,
                "10% discount on your first order after 10 successful referrals"
        );
    }

    @Override
    public ReferralRewardStatusDto getRewardStatus(Long userId) {

        User user = userRepository.findById(userId).orElseThrow();

        long paidOrders = orderRepository.countByUserIdAndPaymentStatus(userId, "paid");

        boolean rewardUnlocked = Boolean.TRUE.equals(user.getRewardUnlocked());
        boolean discountAvailable = Boolean.TRUE.equals(user.getDiscountAvailable());
        boolean used = Boolean.TRUE.equals(user.getFirstOrderCompleted());

        boolean anotherOrderHasReferralDiscount =
                orderRepository.existsByUserIdAndReferralInviterDiscountAppliedTrue(userId);

        boolean eligible = rewardUnlocked && discountAvailable && !used
                && !anotherOrderHasReferralDiscount;

        String message;
        if (used || !discountAvailable) {
            message = "Referral reward already used on a previous order.";
        } else if (!rewardUnlocked) {
            message = "Invite 10 friends who complete a purchase to unlock 10% off your next order.";
        } else if (anotherOrderHasReferralDiscount && paidOrders == 0) {
            message = "You already have an order with referral pricing. Complete or cancel it before placing another.";
        } else if (paidOrders > 0) {
            message = "Referral discount applies only on your first paid order.";
        } else {
            message = "10% referral discount will apply to this order subtotal.";
        }

        return new ReferralRewardStatusDto(
                rewardUnlocked,
                discountAvailable && !used,
                eligible,
                eligible ? 10 : 0,
                message
        );
    }

    @Override
    public String refreshReferralCode(Long userId) {

        User user = userRepository.findById(userId).orElseThrow();
        user = ensureModernReferralCode(user);
        return user.getReferralCode();
    }

    @Override
    public ShareDto getShareData(Long userId) {

        User user = userRepository.findById(userId).orElseThrow();
        user = ensureModernReferralCode(user);
        String code = user.getReferralCode();

        return new ShareDto(
                "Join Flint & Thread using my referral code "
                        + code
                        + " and get amazing offers!",
                "https://flintnthread.com/register?ref=" + code
        );
    }
}
