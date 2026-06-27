package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.DeliveryWeightSlabResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliverySlabLookupService {

    private static final BigDecimal DEFAULT_INTRA = new BigDecimal("175.00");
    private static final BigDecimal DEFAULT_METRO = new BigDecimal("205.00");

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<DeliveryWeightSlabResponse> DELIVERY_CHARGES_MAPPER = (rs, rowNum) ->
            DeliveryWeightSlabResponse.builder()
                    .id(rs.getLong("id"))
                    .label(rs.getString("label"))
                    .minWeightKg(rs.getBigDecimal("min_weight"))
                    .maxWeightKg(rs.getBigDecimal("max_weight"))
                    .intraCityCharge(rs.getBigDecimal("intra_city_charge"))
                    .metroMetroCharge(rs.getBigDecimal("metro_metro_charge"))
                    .custom(rs.getInt("is_custom") == 1)
                    .build();

    private static final RowMapper<DeliveryWeightSlabResponse> WEIGHT_SLABS_MAPPER = (rs, rowNum) ->
            DeliveryWeightSlabResponse.builder()
                    .id(rs.getLong("id"))
                    .label(rs.getString("label"))
                    .minWeightKg(rs.getBigDecimal("min_weight_kg"))
                    .maxWeightKg(rs.getBigDecimal("max_weight_kg"))
                    .intraCityCharge(rs.getBigDecimal("intra_city_charge"))
                    .metroMetroCharge(rs.getBigDecimal("metro_metro_charge"))
                    .custom(false)
                    .build();

    public List<DeliveryWeightSlabResponse> listActiveSlabs() {
        List<DeliveryWeightSlabResponse> fromCharges = loadFromDeliveryCharges();
        if (!fromCharges.isEmpty()) {
            log.info("Loaded {} delivery slab(s) from delivery_charges", fromCharges.size());
            return fromCharges;
        }
        List<DeliveryWeightSlabResponse> fromLegacy = loadFromLegacySlabs();
        if (!fromLegacy.isEmpty()) {
            log.info("Loaded {} delivery slab(s) from delivery_weight_slabs", fromLegacy.size());
            return fromLegacy;
        }
        log.warn("No delivery slabs found in database; using built-in defaults");
        return defaultSlabs();
    }

    public DeliveryWeightSlabResponse resolveForWeight(BigDecimal weightKg) {
        if (weightKg == null || weightKg.compareTo(BigDecimal.ZERO) <= 0) {
            return fallbackSlab("—");
        }
        List<DeliveryWeightSlabResponse> slabs = listActiveSlabs();
        for (DeliveryWeightSlabResponse slab : slabs) {
            if (weightKg.compareTo(slab.getMinWeightKg()) >= 0
                    && weightKg.compareTo(slab.getMaxWeightKg()) <= 0) {
                return slab;
            }
        }
        DeliveryWeightSlabResponse nextHigher = null;
        for (DeliveryWeightSlabResponse slab : slabs) {
            if (weightKg.compareTo(slab.getMinWeightKg()) < 0
                    && (nextHigher == null
                            || slab.getMinWeightKg().compareTo(nextHigher.getMinWeightKg()) < 0)) {
                nextHigher = slab;
            }
        }
        if (nextHigher != null) {
            return nextHigher;
        }
        if (!slabs.isEmpty()) {
            return slabs.get(slabs.size() - 1);
        }
        return fallbackSlab(weightLabelFor(weightKg));
    }

    private List<DeliveryWeightSlabResponse> loadFromDeliveryCharges() {
        List<String> queries = List.of(
                """
                SELECT id,
                       weight_slab AS label,
                       weight_min AS min_weight,
                       weight_max AS max_weight,
                       intra_city_charge,
                       metro_metro_charge,
                       COALESCE(is_custom, 0) AS is_custom
                FROM delivery_charges
                WHERE COALESCE(status, 1) = 1
                ORDER BY weight_min ASC, id ASC
                """,
                """
                SELECT id,
                       weight_slab AS label,
                       weight_min AS min_weight,
                       weight_max AS max_weight,
                       intra_city_charge,
                       metro_metro_charge,
                       COALESCE(is_custom, 0) AS is_custom
                FROM delivery_charges
                ORDER BY weight_min ASC, id ASC
                """,
                """
                SELECT id,
                       label,
                       min_weight_kg AS min_weight,
                       max_weight_kg AS max_weight,
                       intra_city_charge,
                       metro_metro_charge,
                       0 AS is_custom
                FROM delivery_charges
                ORDER BY min_weight_kg ASC, id ASC
                """
        );

        for (String sql : queries) {
            try {
                List<DeliveryWeightSlabResponse> rows = jdbcTemplate.query(sql, DELIVERY_CHARGES_MAPPER);
                if (!rows.isEmpty()) {
                    return rows;
                }
            } catch (Exception ex) {
                log.debug("delivery_charges query skipped: {}", ex.getMessage());
            }
        }
        return List.of();
    }

    private List<DeliveryWeightSlabResponse> loadFromLegacySlabs() {
        try {
            return jdbcTemplate.query(
                    """
                    SELECT id, label, min_weight_kg, max_weight_kg, intra_city_charge, metro_metro_charge
                    FROM delivery_weight_slabs
                    WHERE active = 1
                    ORDER BY sort_order ASC, id ASC
                    """,
                    WEIGHT_SLABS_MAPPER);
        } catch (Exception ex) {
            log.debug("delivery_weight_slabs query failed: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<DeliveryWeightSlabResponse> defaultSlabs() {
        List<DeliveryWeightSlabResponse> slabs = new ArrayList<>();
        slabs.add(slab("500gms-1kg", "0.5", "1", "20", "25", false));
        slabs.add(slab("1-2 kg", "1", "2", "80", "95", false));
        slabs.add(slab("2-5 kg", "2", "5", "175", "205", false));
        slabs.add(slab("Above 5 kg", "5", "999.999", "0", "0", true));
        return slabs;
    }

    private DeliveryWeightSlabResponse slab(
            String label, String min, String max, String intra, String metro, boolean custom) {
        return DeliveryWeightSlabResponse.builder()
                .label(label)
                .minWeightKg(new BigDecimal(min))
                .maxWeightKg(new BigDecimal(max))
                .intraCityCharge(new BigDecimal(intra))
                .metroMetroCharge(new BigDecimal(metro))
                .custom(custom)
                .build();
    }

    private DeliveryWeightSlabResponse fallbackSlab(String label) {
        return DeliveryWeightSlabResponse.builder()
                .label(label)
                .minWeightKg(BigDecimal.ZERO)
                .maxWeightKg(BigDecimal.ZERO)
                .intraCityCharge(DEFAULT_INTRA)
                .metroMetroCharge(DEFAULT_METRO)
                .custom(false)
                .build();
    }

    private String weightLabelFor(BigDecimal weightKg) {
        double w = weightKg.doubleValue();
        if (w <= 1) return "500gms-1kg";
        if (w <= 2) return "1-2 kg";
        if (w <= 5) return "2-5 kg";
        return "Above 5 kg";
    }
}
