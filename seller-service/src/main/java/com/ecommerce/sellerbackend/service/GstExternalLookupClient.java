package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.profile.GstVerifyResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Live GSTIN lookup via AppyFlow (or compatible providers).
 * Configure {@code app.gst.lookup.api-key} with your AppyFlow key_secret.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GstExternalLookupClient {

    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    @Value("${app.gst.lookup.url:https://appyflow.in/api/verifyGST}")
    private String lookupUrl;

    @Value("${app.gst.lookup.api-key:}")
    private String apiKey;

    /**
     * @return verified/details response, provider error response, or empty if lookup is not configured
     */
    public Optional<GstVerifyResponse> lookup(String gst) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("GST AppyFlow lookup skipped — app.gst.lookup.api-key is not configured");
            return Optional.empty();
        }

        try {
            String body = restClient.post()
                    .uri(lookupUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "gstNo", gst,
                            "key_secret", apiKey.trim()
                    ))
                    .retrieve()
                    .body(String.class);

            if (body == null || body.isBlank()) {
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(body);
            if (root.path("error").asBoolean(false)) {
                String errMsg = firstText(root, "message", "errorMessage", "msg");
                log.info("AppyFlow GST lookup rejected {}: {}", gst, errMsg);
                return Optional.of(GstVerifyResponse.builder()
                        .verified(false)
                        .alreadyExists(false)
                        .gstNumber(gst)
                        .message(errMsg.isBlank()
                                ? "GST portal could not verify this GSTIN."
                                : errMsg)
                        .build());
            }

            JsonNode taxpayer = firstNode(root, "taxpayerInfo", "data", "result", "taxpayer");
            if (taxpayer == null || taxpayer.isMissingNode() || taxpayer.isNull()) {
                log.warn("AppyFlow GST lookup returned no taxpayerInfo for {}", gst);
                return Optional.empty();
            }

            return Optional.of(mapTaxpayer(gst, taxpayer));
        } catch (Exception e) {
            log.warn("GST external lookup failed for {}: {}", gst, e.getMessage());
            return Optional.empty();
        }
    }

    private GstVerifyResponse mapTaxpayer(String gst, JsonNode taxpayer) {
        String legalName = firstText(taxpayer, "lgnm", "legal_name", "legalName", "businessName");
        String tradeName = firstText(taxpayer, "tradeNam", "trade_name", "tradeName");
        if (tradeName.isBlank()) {
            tradeName = legalName;
        }

        String status = firstText(taxpayer, "sts", "status");
        String taxpayerType = firstText(taxpayer, "dty", "taxpayer_type", "taxpayerType");
        String registrationDate = firstText(taxpayer, "rgdt", "registration_date", "registrationDate");
        String cancellationDate = firstText(taxpayer, "cxdt", "cancellation_date", "cancellationDate");
        String stateJurisdiction = firstText(taxpayer, "stj", "state_jurisdiction");
        String centreJurisdiction = firstText(taxpayer, "ctj", "centre_jurisdiction");

        JsonNode pradr = firstNode(taxpayer, "pradr");
        String principalPlaceType = pradr != null ? firstText(pradr, "ntr", "nature") : "";

        JsonNode addressNode = pradr != null ? pradr : firstNode(taxpayer, "registered_address", "address");
        JsonNode addr = addressNode != null ? firstNode(addressNode, "addr", "address") : null;
        if (addr == null || addr.isMissingNode()) {
            addr = addressNode;
        }

        String street = joinNonBlank(
                text(addr, "bno"),
                text(addr, "bnm"),
                text(addr, "st"),
                text(addr, "flno"),
                text(addr, "loc")
        );
        if (street.isBlank()) {
            street = firstText(addressNode, "address", "street");
        }

        String city = firstText(addr, "loc", "city", "locality", "dst");
        String state = firstText(addr, "stcd", "state");
        String pincode = firstText(addr, "pncd", "pincode", "pin");

        String businessType = firstText(taxpayer, "ctb", "constitution", "business_type", "businessType");
        String pan = firstText(taxpayer, "panNo", "pan", "panNumber");
        if (pan.isBlank() && gst.length() == 15) {
            pan = gst.substring(2, 12);
        }

        if (legalName.isBlank() && tradeName.isBlank()) {
            throw new IllegalStateException("AppyFlow response missing business name");
        }

        boolean active = status.isBlank() || "active".equalsIgnoreCase(status);
        String message = active
                ? "GST verified successfully via AppyFlow."
                : "GSTIN found but status is " + status + ".";

        return GstVerifyResponse.builder()
                .verified(active)
                .alreadyExists(false)
                .gstNumber(gst)
                .message(message)
                .businessName(legalName.isBlank() ? tradeName : legalName)
                .tradeName(tradeName.isBlank() ? legalName : tradeName)
                .businessType(businessNameToType(businessType))
                .panNumber(pan.isBlank() ? null : pan.toUpperCase(Locale.ROOT))
                .address(street.isBlank() ? null : street)
                .city(city.isBlank() ? null : city)
                .state(state.isBlank() ? null : state)
                .pincode(pincode.isBlank() ? null : pincode)
                .status(status.isBlank() ? null : status)
                .taxpayerType(taxpayerType.isBlank() ? null : taxpayerType)
                .registrationDate(registrationDate.isBlank() ? null : registrationDate)
                .cancellationDate(cancellationDate.isBlank() ? null : cancellationDate)
                .stateJurisdiction(stateJurisdiction.isBlank() ? null : stateJurisdiction)
                .centreJurisdiction(centreJurisdiction.isBlank() ? null : centreJurisdiction)
                .principalPlaceType(principalPlaceType.isBlank() ? null : principalPlaceType)
                .build();
    }

    private static String businessNameToType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("private limited") || lower.contains("pvt")) {
            return "Private Limited";
        }
        if (lower.contains("public limited")) {
            return "Public Limited";
        }
        if (lower.contains("llp")) {
            return "LLP";
        }
        if (lower.contains("partnership") || lower.contains("firm")) {
            return "Partnership";
        }
        if (lower.contains("proprietorship") || lower.contains("individual")) {
            return "Sole Proprietorship";
        }
        return raw.trim();
    }

    private static JsonNode firstNode(JsonNode node, String... fields) {
        if (node == null) {
            return null;
        }
        for (String field : fields) {
            JsonNode child = node.get(field);
            if (child != null && !child.isMissingNode() && !child.isNull()) {
                return child;
            }
        }
        return null;
    }

    private static String firstText(JsonNode node, String... fields) {
        if (node == null) {
            return "";
        }
        for (String field : fields) {
            String value = text(node, field);
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String text(JsonNode node, String field) {
        if (node == null || field == null) {
            return "";
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("").trim();
    }

    private static String joinNonBlank(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(part.trim());
        }
        return sb.toString();
    }
}
