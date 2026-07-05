package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.entity.Seller;
import com.ecommerce.adminbackend.entity.SellerAccountStatus;
import com.ecommerce.adminbackend.entity.SellerKycImage;
import com.ecommerce.adminbackend.repository.OrderItemRepository;
import com.ecommerce.adminbackend.repository.ProductRepository;
import com.ecommerce.adminbackend.repository.SellerKycImageRepository;
import com.ecommerce.adminbackend.repository.SellerRepository;
import com.ecommerce.adminbackend.service.SellerAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import com.ecommerce.adminbackend.util.MediaUrlHelper;
import com.ecommerce.adminbackend.util.SellerGraphPeriodUtil;
import com.ecommerce.adminbackend.util.TextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SellerAdminServiceImpl extends BaseAdminService implements SellerAdminService {

    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("dd MMM, yyyy hh:mm a");

    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final SellerKycImageRepository sellerKycImageRepository;
    private final MediaUrlHelper mediaUrlHelper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listSellers(String status, String search, int page, int size) {
        SellerAccountStatus statusEnum = TextUtils.parseEnum(status, SellerAccountStatus.class, "seller status");
        Pageable pageable = PageRequest.of(page, size);
        String query = blankToNull(search);
        Page<Seller> result = statusEnum == null
                ? sellerRepository.searchSellersAll(query, pageable)
                : sellerRepository.searchSellers(statusEnum, query, pageable);
        List<Long> sellerIds = result.getContent().stream().map(Seller::getId).toList();
        Map<Long, Long> productCounts = loadProductCounts(sellerIds);
        Map<Long, java.math.BigDecimal> revenueBySeller = loadRevenueForSellers(sellerIds);
        Map<Long, Long> orderCounts = loadOrderCounts(sellerIds);
        Map<Long, String> kycImageFallbacks = loadKycImageFallbacks(sellerIds);
        return PageResponse.from(result.map(seller ->
                toSellerSummary(seller, revenueBySeller, productCounts, orderCounts, kycImageFallbacks)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listAdminApprovedSellers(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Seller> result = sellerRepository.findAdminApproved(blankToNull(search), pageable);
        List<Long> sellerIds = result.getContent().stream().map(Seller::getId).toList();
        Map<Long, Long> productCounts = loadProductCounts(sellerIds);
        Map<Long, java.math.BigDecimal> revenueBySeller = loadRevenueForSellers(sellerIds);
        Map<Long, Long> orderCounts = loadOrderCounts(sellerIds);
        Map<Long, String> kycImageFallbacks = loadKycImageFallbacks(sellerIds);
        return PageResponse.from(result.map(seller ->
                toSellerSummary(seller, revenueBySeller, productCounts, orderCounts, kycImageFallbacks)));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> adminApprovedLocationStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("stateCounts", toLocationCountRows(sellerRepository.countAdminApprovedByState()));
        stats.put("cityCounts", toLocationCountRows(sellerRepository.countAdminApprovedByCity()));
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listSellersForGraph(String search, Long sellerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Seller> result = sellerRepository.searchSellersForGraph(blankToNull(search), sellerId, pageable);
        return PageResponse.from(result.map(this::toSellerGraphRow));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listSellerGraphNames() {
        return sellerRepository.findAllOrderedByName().stream()
                .map(seller -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", seller.getId());
                    row.put("fullName", seller.getFullName());
                    row.put("businessName", seller.getBusinessName());
                    row.put("sellerUniqueId", resolveSellerUniqueId(seller));
                    return row;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> analyticsSummary(String filterType, Integer year, String fromDate, String toDate, Long sellerId) {
        Map<String, Object> summary = new LinkedHashMap<>();
        long registered = sellerId == null ? sellerRepository.count() : 1L;
        if (sellerId != null) {
            registered = sellerRepository.findById(sellerId).isPresent() ? 1L : 0L;
        }
        long profileCompleted = sellerRepository.countProfileCompleted(sellerId);
        long approved = sellerRepository.countApproved(sellerId);
        long productsAdded = sellerId == null
                ? productRepository.count()
                : productRepository.countBySellerId(sellerId);
        long shiprocketUploaded = sellerRepository.countShiprocketReady(sellerId);

        summary.put("registered", registered);
        summary.put("profileCompleted", profileCompleted);
        summary.put("approved", approved);
        summary.put("productsAdded", productsAdded);
        summary.put("shiprocketUploaded", shiprocketUploaded);
        summary.put("shiprocketPending", sellerId == null
                ? sellerRepository.countShiprocketPending()
                : Math.max(0L, profileCompleted - shiprocketUploaded));
        summary.put("total", registered);
        summary.put("active", approved);
        summary.put("pending", sellerId == null
                ? sellerRepository.countPendingActivation()
                : 0L);
        summary.put("emailPending", sellerId == null
                ? sellerRepository.countByStatus(SellerAccountStatus.email_pending)
                : 0L);
        summary.put("suspended", sellerId == null
                ? sellerRepository.countByStatus(SellerAccountStatus.suspended)
                : 0L);
        summary.put("rejected", sellerId == null
                ? sellerRepository.countByStatus(SellerAccountStatus.rejected)
                : 0L);
        summary.put("pendingBank", sellerId == null
                ? sellerRepository.countPendingBankVerification()
                : 0L);
        summary.put("bankVerified", sellerId == null
                ? sellerRepository.countBankVerified()
                : 0L);
        summary.put("filterType", filterType);
        summary.put("year", year);
        summary.put("fromDate", fromDate);
        summary.put("toDate", toDate);
        summary.put("sellerId", sellerId);
        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> analyticsChart(String filterType, Integer year, String fromDate, String toDate, Long sellerId) {
        List<SellerGraphPeriodUtil.PeriodBucket> buckets = SellerGraphPeriodUtil.buildBuckets(filterType, year, fromDate, toDate);
        List<String> labels = new ArrayList<>();
        List<Long> registered = new ArrayList<>();
        List<Long> profileCompleted = new ArrayList<>();
        List<Long> approved = new ArrayList<>();
        List<Long> productsAdded = new ArrayList<>();
        List<Long> shiprocketUploaded = new ArrayList<>();

        long maxValue = 0L;
        for (SellerGraphPeriodUtil.PeriodBucket bucket : buckets) {
            labels.add(bucket.label());
            LocalDateTime periodStart = bucket.periodStart();
            LocalDateTime periodEnd = bucket.periodEnd();

            long registeredCount = sellerRepository.countRegisteredInPeriod(sellerId, periodStart, periodEnd);
            long profileCount = sellerRepository.countProfileCompletedInPeriod(sellerId, periodStart, periodEnd);
            long approvedCount = sellerRepository.countApprovedInPeriod(sellerId, periodStart, periodEnd);
            long productCount = productRepository.countCreatedInPeriod(sellerId, periodStart, periodEnd);
            long shiprocketCount = sellerRepository.countShiprocketReadyInPeriod(sellerId, periodStart, periodEnd);

            registered.add(registeredCount);
            profileCompleted.add(profileCount);
            approved.add(approvedCount);
            productsAdded.add(productCount);
            shiprocketUploaded.add(shiprocketCount);

            maxValue = Math.max(maxValue, Math.max(registeredCount,
                    Math.max(profileCount, Math.max(approvedCount, Math.max(productCount, shiprocketCount)))));
        }

        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("labels", labels);
        chart.put("registered", registered);
        chart.put("profileCompleted", profileCompleted);
        chart.put("approved", approved);
        chart.put("productsAdded", productsAdded);
        chart.put("shiprocketUploaded", shiprocketUploaded);
        chart.put("maxY", computeChartMaxY(maxValue));
        chart.put("year", year);
        chart.put("filterType", filterType);
        return chart;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> analyticsInsights(String filterType, Integer year, String fromDate, String toDate, Long sellerId) {
        List<Map<String, Object>> insights = new ArrayList<>();
        int selectedYear = year != null ? year : LocalDate.now().getYear();

        long currentRegistered = sellerRepository.countRegisteredOnOrBefore(sellerId, LocalDateTime.now());
        LocalDateTime previousPeriodEnd = LocalDate.now().minusMonths(1).atTime(23, 59, 59);
        long previousRegistered = sellerRepository.countRegisteredOnOrBefore(sellerId, previousPeriodEnd);
        long registrationDelta = currentRegistered - previousRegistered;
        long registrationPct = previousRegistered > 0
                ? Math.round((registrationDelta * 100.0) / previousRegistered)
                : (registrationDelta > 0 ? 100L : 0L);

        insights.add(insight(
                "trending-up",
                "Registered sellers " + (registrationDelta >= 0 ? "increased" : "decreased")
                        + " by " + Math.abs(registrationPct) + "% vs previous period.",
                "#2563EB",
                "#EFF6FF"
        ));

        long approvedCount = sellerRepository.countApproved(sellerId);
        insights.add(insight(
                "checkmark-circle",
                approvedCount + " seller" + (approvedCount == 1 ? "" : "s")
                        + " are fully approved and active.",
                "#16A34A",
                "#F0FDF4"
        ));

        LocalDateTime yearStart = LocalDate.of(selectedYear, 1, 1).atStartOfDay();
        LocalDateTime yearEnd = LocalDate.of(selectedYear, 12, 31).atTime(23, 59, 59);
        long productsThisYear = productRepository.countCreatedOnOrBefore(sellerId, yearEnd)
                - productRepository.countCreatedOnOrBefore(sellerId, yearStart.minusSeconds(1));
        insights.add(insight(
                "cube",
                productsThisYear + " product" + (productsThisYear == 1 ? "" : "s")
                        + " added in " + selectedYear + ".",
                "#7C3AED",
                "#F5F3FF"
        ));

        long shiprocketReady = sellerRepository.countShiprocketReady(sellerId);
        long profileCompletedCount = sellerRepository.countProfileCompleted(sellerId);
        long pendingShiprocket = Math.max(0L, profileCompletedCount - shiprocketReady);
        String shiprocketMessage = pendingShiprocket == 0
                ? "Shiprocket pickup locations are up to date for profile-completed sellers."
                : pendingShiprocket + " profile-completed seller"
                + (pendingShiprocket == 1 ? " still needs" : "s still need")
                + " Shiprocket warehouse upload.";
        insights.add(insight("sync", shiprocketMessage, "#F97316", "#FFF7ED"));

        return insights;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> analyticsYearOptions() {
        Integer earliest = sellerRepository.findEarliestRegistrationYear();
        int startYear = earliest != null ? earliest : LocalDate.now().getYear();
        int endYear = LocalDate.now().getYear() + 1;
        List<String> years = new ArrayList<>();
        for (int y = endYear; y >= startYear; y--) {
            years.add(String.valueOf(y));
        }
        return years;
    }

    @Override
    @Transactional
    public Map<String, Object> blockSeller(Long id) {
        Seller seller = requireSeller(id);
        seller.setStatus(SellerAccountStatus.suspended);
        sellerRepository.save(seller);
        return Map.of("sellerId", id, "status", "suspended", "message", "Seller blocked.");
    }

    @Override
    @Transactional
    public Map<String, Object> unblockSeller(Long id) {
        Seller seller = requireSeller(id);
        seller.setStatus(SellerAccountStatus.active);
        sellerRepository.save(seller);
        return Map.of("sellerId", id, "status", "active", "message", "Seller unblocked.");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listPendingBank(int page, int size) {
        Page<Seller> result = sellerRepository.findPendingBankVerification(PageRequest.of(page, size));
        return PageResponse.from(result.map(this::toBankSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> bankStats() {
        long pending = sellerRepository.countPendingBankVerification();
        long verified = sellerRepository.countBankVerified();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("pending", pending);
        stats.put("verified", verified);
        stats.put("total", pending + verified);
        stats.put("processing", 0L);
        stats.put("failed", 0L);
        stats.put("expired", 0L);
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getBankDetails(Long id) {
        return toBankDetail(requireSeller(id));
    }

    @Override
    @Transactional
    public Map<String, Object> approveBank(Long id, String note) {
        Seller seller = requireSeller(id);
        seller.setBankVerified(true);
        if (note != null && !note.isBlank()) {
            seller.setAdminRemarks(note.trim());
        }
        sellerRepository.save(seller);
        return Map.of("sellerId", id, "bankVerified", true, "message", "Bank details approved.");
    }

    @Override
    @Transactional
    public Map<String, Object> rejectBank(Long id, String note) {
        Seller seller = requireSeller(id);
        seller.setBankVerified(false);
        seller.setAdminRemarks(note != null && !note.isBlank() ? note.trim() : "Bank details rejected.");
        sellerRepository.save(seller);
        return Map.of("sellerId", id, "bankVerified", false, "message", "Bank details rejected.");
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSeller(Long id) {
        return toSellerDetail(requireSeller(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listShiprocketSellers(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String normalized = status == null ? "pending" : status.trim().toLowerCase();
        Page<Seller> result = "uploaded".equals(normalized)
                ? sellerRepository.findShiprocketUploaded(pageable)
                : sellerRepository.findShiprocketPending(pageable);
        return PageResponse.from(result.map(this::toShiprocketRow));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listBankVerifications(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String normalized = status == null ? "pending" : status.trim().toLowerCase();
        Page<Seller> result = "verified".equals(normalized)
                ? sellerRepository.findBankVerified(pageable)
                : sellerRepository.findPendingBankVerification(pageable);
        return PageResponse.from(result.map(this::toBankSummary));
    }

    private Seller requireSeller(Long id) {
        return requireFound(sellerRepository.findById(id), "Seller not found.");
    }

    private Map<String, Object> insight(String iconName, String text, String color, String bg) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("iconName", iconName);
        row.put("text", text);
        row.put("color", color);
        row.put("bg", bg);
        return row;
    }

    private long computeChartMaxY(long maxValue) {
        if (maxValue <= 0) {
            return 100L;
        }
        long step = 150L;
        return ((maxValue / step) + 1) * step;
    }

    private Map<String, Object> toSellerGraphRow(Seller seller) {
        Map<String, Object> row = toSellerSummary(seller);
        row.put("products", productRepository.countBySellerId(seller.getId()));
        row.put("profile", Boolean.TRUE.equals(seller.getProfileCompleted()) ? "Complete" : "Incomplete");
        row.put("kyc", resolveKycLabel(seller));
        row.put("supplement", hasSupplementDocs(seller) ? "Provided" : "Not Provided");
        row.put("shiprocket", hasShiprocketWarehouse(seller) ? "Uploaded" : "Not Uploaded");
        row.put("shipDate", formatShipDate(seller));
        return row;
    }

    private String resolveKycLabel(Seller seller) {
        if (Boolean.TRUE.equals(seller.getKycVerified())) {
            return "Complete";
        }
        if (Boolean.TRUE.equals(seller.getKycCompleted())) {
            return "Pending";
        }
        return "Not done";
    }

    private boolean hasSupplementDocs(Seller seller) {
        return isPresent(seller.getMsmeCertificate())
                || isPresent(seller.getPartnershipDeed())
                || isPresent(seller.getIncorporationCertificate())
                || isPresent(seller.getIecCertificate());
    }

    private boolean hasShiprocketWarehouse(Seller seller) {
        return isPresent(seller.getWarehouseAddress());
    }

    private String formatShipDate(Seller seller) {
        if (!hasShiprocketWarehouse(seller)) {
            return null;
        }
        LocalDateTime stamp = seller.getUpdatedAt() != null ? seller.getUpdatedAt() : seller.getProfileUpdatedAt();
        return stamp != null ? DISPLAY_DATE_TIME.format(stamp) : null;
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private String resolveSellerUniqueId(Seller seller) {
        if (seller == null || seller.getId() == null) {
            return "";
        }
        String unique = trim(seller.getSellerUniqueId());
        if (!unique.isEmpty()) {
            return unique;
        }
        return formatSellerUniqueId(seller.getId());
    }

    private String formatSellerUniqueId(Long sellerId) {
        return "FNT-SELLER-" + String.format("%06d", sellerId);
    }

    private String resolveSellerState(Seller seller) {
        if (isPresent(seller.getState())) {
            return seller.getState().trim();
        }
        if (isPresent(seller.getWarehouseState())) {
            return seller.getWarehouseState().trim();
        }
        return null;
    }

    private String resolveSellerCity(Seller seller) {
        if (isPresent(seller.getCity())) {
            return seller.getCity().trim();
        }
        if (isPresent(seller.getWarehouseCity())) {
            return seller.getWarehouseCity().trim();
        }
        return null;
    }

    private List<Map<String, Object>> toLocationCountRows(List<Object[]> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", row[0].toString());
            entry.put("count", row[1] != null ? ((Number) row[1]).longValue() : 0L);
            result.add(entry);
        }
        return result;
    }

    private Map<String, Object> toSellerSummary(Seller seller) {
        Long sellerId = seller.getId();
        return toSellerSummary(
                seller,
                loadRevenueForSellers(List.of(sellerId)),
                loadProductCounts(List.of(sellerId)),
                loadOrderCounts(List.of(sellerId)),
                loadKycImageFallbacks(List.of(sellerId)));
    }

    private Map<String, Object> toSellerSummary(
            Seller seller,
            Map<Long, java.math.BigDecimal> revenueBySeller,
            Map<Long, Long> productCounts,
            Map<Long, Long> orderCounts,
            Map<Long, String> kycImageFallbacks) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", seller.getId());
        summary.put("fullName", seller.getFullName());
        summary.put("email", seller.getEmail());
        summary.put("mobile", seller.getMobile());
        summary.put("businessName", seller.getBusinessName());
        summary.put("status", seller.getStatus() != null ? seller.getStatus().name() : null);
        summary.put("sellerCategory", seller.getSellerCategory() != null ? seller.getSellerCategory().name() : null);
        summary.put("kycVerified", seller.getKycVerified());
        summary.put("kycCompleted", seller.getKycCompleted());
        summary.put("profileCompleted", seller.getProfileCompleted());
        summary.put("bankVerified", seller.getBankVerified());
        summary.put("walletBalance", seller.getWalletBalance());
        summary.put("createdAt", seller.getCreatedAt());
        summary.put("updatedAt", seller.getUpdatedAt());
        summary.put("profilePicPath", blankToNull(seller.getProfilePic()));
        summary.put("liveSelfiePath", blankToNull(seller.getLiveSelfie()));
        summary.put("profilePicUrl", resolveSellerImageUrl(seller, kycImageFallbacks));
        summary.put("sellerUniqueId", resolveSellerUniqueId(seller));
        summary.put("referralCode", seller.getReferralCode());
        summary.put("productCount", productCounts.getOrDefault(seller.getId(), 0L));
        summary.put("totalOrders", orderCounts.getOrDefault(seller.getId(), 0L));
        summary.put("city", resolveSellerCity(seller));
        summary.put("state", resolveSellerState(seller));
        summary.put("businessType", seller.getBusinessType());
        summary.put("country", seller.getCountry());
        java.math.BigDecimal revenue = revenueBySeller.getOrDefault(seller.getId(), java.math.BigDecimal.ZERO);
        summary.put("totalRevenue", revenue);
        return summary;
    }

    private Map<Long, Long> loadOrderCounts(List<Long> sellerIds) {
        if (sellerIds == null || sellerIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : orderItemRepository.countOrdersBySeller()) {
            if (row[0] == null) {
                continue;
            }
            Long sellerId = ((Number) row[0]).longValue();
            if (!sellerIds.contains(sellerId)) {
                continue;
            }
            Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            counts.put(sellerId, count);
        }
        for (Long sellerId : sellerIds) {
            counts.putIfAbsent(sellerId, 0L);
        }
        return counts;
    }

    private Map<Long, String> loadKycImageFallbacks(List<Long> sellerIds) {
        if (sellerIds == null || sellerIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> fallbackBySeller = new LinkedHashMap<>();
        for (SellerKycImage image : sellerKycImageRepository.findLiveSelfiesBySellerIds(sellerIds)) {
            String path = firstNonBlank(image.getFileName(), image.getImagePath());
            if (isPresent(path)) {
                fallbackBySeller.putIfAbsent(image.getSellerId(), path);
            }
        }
        for (SellerKycImage image : sellerKycImageRepository.findFaceImagesBySellerIds(sellerIds)) {
            if (isPresent(image.getImagePath())) {
                fallbackBySeller.putIfAbsent(image.getSellerId(), image.getImagePath());
            }
        }
        return fallbackBySeller;
    }

    private String firstNonBlank(String first, String second) {
        if (isPresent(first)) {
            return first.trim();
        }
        if (isPresent(second)) {
            return second.trim();
        }
        return null;
    }

    private String resolveSellerImageUrl(Seller seller, Map<Long, String> kycImageFallbacks) {
        if (isPresent(seller.getProfilePic())) {
            return mediaUrlHelper.toPublicUrl(seller.getProfilePic());
        }
        if (isPresent(seller.getLiveSelfie())) {
            return mediaUrlHelper.toPublicUrl(seller.getLiveSelfie());
        }
        String kycPath = kycImageFallbacks.get(seller.getId());
        if (isPresent(kycPath)) {
            return mediaUrlHelper.toPublicUrl(kycPath);
        }
        return null;
    }

    private Map<Long, Long> loadProductCounts(List<Long> sellerIds) {
        if (sellerIds == null || sellerIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : productRepository.countProductsBySellerIds(sellerIds)) {
            if (row[0] == null) {
                continue;
            }
            Long sellerId = ((Number) row[0]).longValue();
            Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            counts.put(sellerId, count);
        }
        return counts;
    }

    private Map<Long, java.math.BigDecimal> loadRevenueForSellers(List<Long> sellerIds) {
        if (sellerIds == null || sellerIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, java.math.BigDecimal> allRevenue = loadRevenueBySeller();
        Map<Long, java.math.BigDecimal> scoped = new HashMap<>();
        for (Long sellerId : sellerIds) {
            scoped.put(sellerId, allRevenue.getOrDefault(sellerId, java.math.BigDecimal.ZERO));
        }
        return scoped;
    }

    private Map<Long, java.math.BigDecimal> loadRevenueBySeller() {
        Map<Long, java.math.BigDecimal> revenueBySeller = new HashMap<>();
        for (Object[] row : orderItemRepository.sumRevenueBySeller()) {
            if (row[0] == null) {
                continue;
            }
            Long sellerId = ((Number) row[0]).longValue();
            java.math.BigDecimal amount = row[1] != null
                    ? new java.math.BigDecimal(row[1].toString())
                    : java.math.BigDecimal.ZERO;
            revenueBySeller.put(sellerId, amount);
        }
        return revenueBySeller;
    }

    private Map<String, Object> toSellerDetail(Seller seller) {
        Map<String, Object> detail = toSellerSummary(seller);
        detail.put("fullName", seller.getFullName());
        detail.put("firstName", seller.getFirstName());
        detail.put("lastName", seller.getLastName());
        detail.put("businessType", seller.getBusinessType());
        detail.put("emailVerified", seller.getEmailVerified());
        detail.put("mobileVerified", seller.getMobileVerified());
        detail.put("lastLoginAt", seller.getLastLoginAt());
        detail.put("profileUpdatedAt", seller.getProfileUpdatedAt());
        detail.put("address", seller.getAddress());
        detail.put("city", seller.getCity());
        detail.put("state", seller.getState());
        detail.put("pincode", seller.getPincode());
        detail.put("country", seller.getCountry());
        detail.put("gstNumber", seller.getGstNumber());
        detail.put("panNumber", seller.getPanNumber());
        detail.put("hasGst", seller.getHasGst());
        detail.put("bankName", seller.getBankName());
        detail.put("accountHolder", seller.getAccountHolder());
        detail.put("accountNumber", seller.getAccountNumber());
        detail.put("ifscCode", seller.getIfscCode());
        detail.put("branchName", seller.getBranchName());
        detail.put("warehouseAddress", seller.getWarehouseAddress());
        detail.put("warehouseCity", seller.getWarehouseCity());
        detail.put("warehouseState", seller.getWarehouseState());
        detail.put("warehouseCountry", seller.getWarehouseCountry());
        detail.put("warehouseArea", seller.getWarehouseArea());
        detail.put("adminRemarks", seller.getAdminRemarks());
        detail.put("kycRemarks", seller.getKycRemarks());
        detail.put("profileNeedsVerification", seller.getProfileNeedsVerification());
        detail.put("bankProofPath", blankToNull(seller.getBankProof()));
        detail.put("cancelledChequePath", blankToNull(seller.getCancelledCheque()));
        detail.put("bankProofUrl", mediaUrlHelper.toPublicUrl(seller.getBankProof()));
        detail.put("cancelledChequeUrl", mediaUrlHelper.toPublicUrl(seller.getCancelledCheque()));
        detail.put("documents", buildDocumentList(seller));
        detail.put("productStatusDistribution", buildProductStatusDistribution(seller.getId()));
        detail.put("orderStatusDistribution", buildOrderStatusDistribution(seller.getId()));
        return detail;
    }

    private Map<String, Object> buildProductStatusDistribution(Long sellerId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("active", 0L);
        counts.put("inactive", 0L);
        counts.put("pending", 0L);
        for (Object[] row : productRepository.countProductsByStatusForSeller(sellerId)) {
            if (row[0] == null) {
                continue;
            }
            String status = row[0].toString().toLowerCase();
            long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            if (status.contains("pending") || status.contains("review")) {
                counts.merge("pending", count, Long::sum);
            } else if (status.contains("inactive") || status.contains("reject") || status.contains("draft")) {
                counts.merge("inactive", count, Long::sum);
            } else {
                counts.merge("active", count, Long::sum);
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("active", counts.get("active"));
        out.put("inactive", counts.get("inactive"));
        out.put("pending", counts.get("pending"));
        return out;
    }

    private Map<String, Object> buildOrderStatusDistribution(Long sellerId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("pending", 0L);
        counts.put("processing", 0L);
        counts.put("shipped", 0L);
        counts.put("delivered", 0L);
        counts.put("cancelled", 0L);
        for (Object[] row : orderItemRepository.countOrdersByStatusForSeller(sellerId)) {
            if (row[0] == null) {
                continue;
            }
            String status = row[0].toString().toLowerCase();
            long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            if (status.contains("cancel")) {
                counts.merge("cancelled", count, Long::sum);
            } else if (status.contains("deliver") || status.contains("complete")) {
                counts.merge("delivered", count, Long::sum);
            } else if (status.contains("ship") || status.contains("transit") || status.contains("pick")
                    || status.contains("awb") || status.contains("out_for")) {
                counts.merge("shipped", count, Long::sum);
            } else if (status.contains("process") || status.contains("pack") || status.contains("confirm")) {
                counts.merge("processing", count, Long::sum);
            } else {
                counts.merge("pending", count, Long::sum);
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("pending", counts.get("pending"));
        out.put("processing", counts.get("processing"));
        out.put("shipped", counts.get("shipped"));
        out.put("delivered", counts.get("delivered"));
        out.put("cancelled", counts.get("cancelled"));
        return out;
    }

    private List<Map<String, Object>> buildDocumentList(Seller seller) {
        List<Map<String, Object>> docs = new ArrayList<>();
        addDoc(docs, "Profile Picture", seller.getProfilePic());
        addDoc(docs, "Aadhaar Front", seller.getAadharFront());
        addDoc(docs, "Aadhaar Back", seller.getAadharBack());
        addDoc(docs, "PAN Card", seller.getPanCard());
        addDoc(docs, "Cancelled Cheque", seller.getCancelledCheque());
        addDoc(docs, "Business Proof", seller.getBusinessProof());
        addDoc(docs, "Bank Proof", seller.getBankProof());
        addDoc(docs, "Company PAN Doc", seller.getCompanyPanDoc());
        addDoc(docs, "MSME Certificate", seller.getMsmeCertificate());
        addDoc(docs, "Incorporation Certificate", seller.getIncorporationCertificate());
        addDoc(docs, "Partnership Deed", seller.getPartnershipDeed());
        addDoc(docs, "IEC Certificate", seller.getIecCertificate());
        addDoc(docs, "Live Selfie", seller.getLiveSelfie());

        for (SellerKycImage image : sellerKycImageRepository.findBySellerIdOrderByCapturedAtDesc(seller.getId())) {
            String path = resolveKycImagePath(seller.getId(), image);
            if (!isPresent(path)) {
                continue;
            }
            addDoc(docs, formatKycDocumentName(image), path);
        }
        return docs;
    }

    private String resolveKycImagePath(Long sellerId, SellerKycImage image) {
        if (isPresent(image.getImagePath())) {
            return image.getImagePath().trim();
        }
        if (isPresent(image.getFileName())) {
            return image.getFileName().trim();
        }
        return null;
    }

    private String formatKycDocumentName(SellerKycImage image) {
        String docType = image.getDocType() != null ? image.getDocType().trim() : "";
        String imageType = image.getImageType() != null ? image.getImageType().trim() : "";
        if ("live_selfie".equalsIgnoreCase(docType)) {
            return "Live Selfie (KYC)";
        }
        if ("business_proof".equalsIgnoreCase(docType)) {
            return "Business Proof (KYC)";
        }
        if ("aadhaar_front".equalsIgnoreCase(imageType)) {
            return "Aadhaar Front (KYC)";
        }
        if ("aadhaar_back".equalsIgnoreCase(imageType)) {
            return "Aadhaar Back (KYC)";
        }
        if ("pan".equalsIgnoreCase(imageType)) {
            return "PAN Card (KYC)";
        }
        if ("face".equalsIgnoreCase(imageType)) {
            return "Face Verification (KYC)";
        }
        if (!docType.isBlank() && !"regular".equalsIgnoreCase(docType)) {
            return docType.replace('_', ' ') + " (KYC)";
        }
        return "KYC Document";
    }

    private void addDoc(List<Map<String, Object>> docs, String name, String path) {
        if (!isPresent(path)) {
            return;
        }
        String normalizedName = name.trim();
        boolean exists = docs.stream().anyMatch(doc -> normalizedName.equalsIgnoreCase(String.valueOf(doc.get("name"))));
        if (exists) {
            return;
        }
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("name", normalizedName);
        doc.put("path", path.trim());
        doc.put("url", mediaUrlHelper.toPublicUrl(path));
        doc.put("available", true);
        docs.add(doc);
    }

    private Map<String, Object> toShiprocketRow(Seller seller) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", seller.getId());
        row.put("businessName", seller.getBusinessName());
        row.put("contactPerson", seller.getFullName());
        row.put("phone", seller.getMobile());
        row.put("city", seller.getWarehouseCity() != null ? seller.getWarehouseCity() : seller.getCity());
        row.put("state", seller.getWarehouseState() != null ? seller.getWarehouseState() : seller.getState());
        row.put("email", seller.getEmail());
        row.put("warehouseAddress", seller.getWarehouseAddress());
        boolean uploaded = hasShiprocketWarehouse(seller);
        row.put("status", uploaded ? "Uploaded" : "Not Uploaded");
        row.put("uploadedAt", uploaded ? formatShipDate(seller) : null);
        return row;
    }

    private Map<String, Object> toBankSummary(Seller seller) {
        Map<String, Object> summary = toSellerSummary(seller);
        summary.put("bankName", seller.getBankName());
        summary.put("accountHolder", seller.getAccountHolder());
        summary.put("accountNumber", seller.getAccountNumber());
        summary.put("ifscCode", seller.getIfscCode());
        summary.put("branchName", seller.getBranchName());
        summary.put("updatedAt", seller.getUpdatedAt());
        return summary;
    }

    private Map<String, Object> toBankDetail(Seller seller) {
        Map<String, Object> detail = toBankSummary(seller);
        detail.put("accountNumber", seller.getAccountNumber());
        detail.put("ifscCode", seller.getIfscCode());
        detail.put("branchName", seller.getBranchName());
        detail.put("bankProofUrl", mediaUrlHelper.toPublicUrl(seller.getBankProof()));
        detail.put("cancelledChequeUrl", mediaUrlHelper.toPublicUrl(seller.getCancelledCheque()));
        detail.put("adminRemarks", seller.getAdminRemarks());
        return detail;
    }
}
