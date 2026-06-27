package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.ApplyReferralRequest;
import com.ecommerce.authdemo.dto.ReferralDashboardDto;
import com.ecommerce.authdemo.dto.ReferralResponse;
import com.ecommerce.authdemo.dto.ReferralRewardStatusDto;
import com.ecommerce.authdemo.dto.ShareDto;
import com.ecommerce.authdemo.service.ReferralService;
import com.ecommerce.authdemo.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/referral")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;
    private final SecurityUtil securityUtil;

    @PostMapping("/apply")
    public ReferralResponse apply(
            @RequestBody ApplyReferralRequest request) {

        Long userId = securityUtil.getCurrentUserId();
        return referralService.applyReferral(
                userId,
                request.getReferralCode()
        );
    }

    @GetMapping("/discount/{subtotal}")
    public BigDecimal discount(
            @PathVariable BigDecimal subtotal) {

        Long userId = securityUtil.getCurrentUserId();
        return referralService.calculateFirstOrderDiscount(
                userId,
                subtotal
        );
    }

    @GetMapping("/dashboard")
    public ReferralDashboardDto dashboard() {
        Long userId = securityUtil.getCurrentUserId();
        return referralService.getDashboard(userId);
    }

    /** Alias for dashboard (spec: GET /api/referral/me). */
    @GetMapping("/me")
    public ReferralDashboardDto me() {
        return dashboard();
    }

    /** Checkout / profile: inviter reward eligibility. */
    @GetMapping("/reward-status")
    public ReferralRewardStatusDto rewardStatus() {
        Long userId = securityUtil.getCurrentUserId();
        return referralService.getRewardStatus(userId);
    }

    @PostMapping("/refresh")
    public String refreshCode() {
        Long userId = securityUtil.getCurrentUserId();
        return referralService.refreshReferralCode(userId);
    }

    @GetMapping("/share")
    public ShareDto share() {
        Long userId = securityUtil.getCurrentUserId();
        return referralService.getShareData(userId);
    }

    @PostMapping("/generate/{username}")
    public String generate(@PathVariable String username) {
        Long userId = securityUtil.getCurrentUserId();
        referralService.generateCodes(userId, username);
        return "Referral code generated successfully";
    }
}