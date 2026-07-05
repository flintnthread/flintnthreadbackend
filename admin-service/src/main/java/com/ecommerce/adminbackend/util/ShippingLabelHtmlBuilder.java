package com.ecommerce.adminbackend.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ShippingLabelHtmlBuilder {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private ShippingLabelHtmlBuilder() {
    }

    @SuppressWarnings("unchecked")
    public static String build(Map<String, Object> label) {
        Map<String, Object> company = map(label.get("company"));
        Map<String, Object> shipping = map(label.get("shipping"));
        Map<String, Object> totals = map(label.get("totals"));
        Map<String, Object> gstBreakdown = map(label.get("gstBreakdown"));
        Map<String, Object> payment = map(label.get("payment"));
        Map<String, Object> qrCode = map(label.get("qrCode"));
        List<Map<String, Object>> sellerGroups = list(label.get("sellerGroups"));
        Map<String, Object> firstGroup = sellerGroups.isEmpty() ? Map.of() : sellerGroups.get(0);
        Map<String, Object> seller = map(firstGroup.get("seller"));
        Map<String, Object> sellerAddress = map(seller.get("address"));
        boolean isIntraState = Boolean.TRUE.equals(label.get("isIntraState"));

        String awb = str(label.get("awbCode"));
        if (awb.isBlank()) {
            awb = str(label.get("trackingId"));
        }
        if (awb.isBlank()) {
            awb = str(label.get("orderNumber")).replaceAll("\\D", "");
        }

        StringBuilder itemRows = new StringBuilder();
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Map<String, Object> group : sellerGroups) {
            for (Map<String, Object> item : list(group.get("products"))) {
                BigDecimal lineSubtotal = decimal(item.get("lineSubtotal"));
                if (lineSubtotal.compareTo(BigDecimal.ZERO) == 0) {
                    lineSubtotal = decimal(item.get("unitPrice")).multiply(decimal(item.get("qty")));
                }
                BigDecimal taxAmount = decimal(item.get("taxAmount"));
                if (taxAmount.compareTo(BigDecimal.ZERO) == 0) {
                    taxAmount = lineSubtotal
                            .multiply(decimal(item.get("taxPercent")))
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                }
                BigDecimal cgst = isIntraState
                        ? taxAmount.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                BigDecimal sgst = cgst;
                BigDecimal igst = isIntraState ? BigDecimal.ZERO : taxAmount;
                BigDecimal lineTotal = decimal(item.get("lineTotal"));
                if (lineTotal.compareTo(BigDecimal.ZERO) == 0) {
                    lineTotal = lineSubtotal.add(taxAmount);
                }

                totalCgst = totalCgst.add(cgst);
                totalSgst = totalSgst.add(sgst);
                totalIgst = totalIgst.add(igst);
                grandTotal = grandTotal.add(lineTotal);

                itemRows.append("<tr>")
                        .append("<td>").append(esc(str(item.get("name")))).append("</td>")
                        .append("<td style=\"text-align:center\">").append(esc(str(item.get("hsnCode")))).append("</td>")
                        .append("<td style=\"text-align:right\">").append(str(item.get("qty"))).append("</td>")
                        .append("<td style=\"text-align:right\">").append(money(item.get("unitPrice"))).append("</td>")
                        .append("<td style=\"text-align:right\">").append(cgst.compareTo(BigDecimal.ZERO) > 0 ? money(cgst) : "-").append("</td>")
                        .append("<td style=\"text-align:right\">").append(sgst.compareTo(BigDecimal.ZERO) > 0 ? money(sgst) : "-").append("</td>")
                        .append("<td style=\"text-align:right\">").append(igst.compareTo(BigDecimal.ZERO) > 0 ? money(igst) : "-").append("</td>")
                        .append("<td style=\"text-align:right\">").append(money(lineTotal)).append("</td>")
                        .append("</tr>");
            }
        }

        if (grandTotal.compareTo(BigDecimal.ZERO) == 0) {
            grandTotal = decimal(totals.get("grandTotal"));
        }

        Map<String, Object> dimensions = map(label.get("dimensionsCm"));
        String dimensionsText = formatDimensions(dimensions);
        String weightText = formatWeight(label.get("weightKg"));
        String paymentMethod = str(payment.get("method"));
        String paymentLabel = paymentMethod.toLowerCase(Locale.ROOT).contains("cod") ? "COD" : "Prepaid";
        String qrImg = str(qrCode.get("imageDataUrl"));
        String gstFooter = isIntraState
                ? "CGST+SGST: " + money(totalCgst.add(totalSgst))
                : "IGST: " + money(totalIgst);

        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8" />
                  <style>
                    body { font-family: Arial, sans-serif; color: #111827; padding: 20px; }
                    .brand { text-align: center; margin-bottom: 8px; }
                    .title { text-align: center; font-size: 18px; font-weight: 800; color: #1E2B6B; margin: 8px 0 14px; }
                    .courier { background: #1E2B6B; color: #fff; text-align: center; padding: 8px; font-weight: 700; border-radius: 6px; margin-bottom: 14px; }
                    .awb-box { display: flex; justify-content: space-between; gap: 16px; margin-bottom: 16px; }
                    .awb { flex: 1; border: 1px solid #E5E7EB; border-radius: 8px; padding: 12px; }
                    .awb-label { font-size: 10px; font-weight: 700; color: #6B7280; letter-spacing: 0.4px; }
                    .awb-value { font-size: 16px; font-weight: 800; margin-top: 6px; }
                    .qr { width: 88px; height: 88px; border: 1px solid #E5E7EB; border-radius: 8px; }
                    .section-label { font-size: 10px; font-weight: 800; color: #F97316; letter-spacing: 0.5px; margin-bottom: 4px; }
                    .ship-name { font-size: 15px; font-weight: 800; margin-bottom: 4px; }
                    .muted { color: #4B5563; font-size: 12px; line-height: 1.5; }
                    .meta { margin: 14px 0; font-size: 12px; }
                    .meta div { display: flex; justify-content: space-between; padding: 4px 0; border-bottom: 1px solid #F3F4F6; }
                    table { width: 100%%; border-collapse: collapse; margin-top: 10px; font-size: 11px; }
                    th, td { border: 1px solid #E5E7EB; padding: 7px 5px; }
                    th { background: #FFF7ED; text-align: left; }
                    .footer { margin-top: 16px; text-align: center; font-size: 10px; color: #6B7280; }
                  </style>
                </head>
                <body>
                  <div class="brand"><strong>%s</strong></div>
                  <div class="title">SHIPPING LABEL</div>
                  <div class="courier">%s</div>
                  <div class="awb-box">
                    <div class="awb">
                      <div class="awb-label">AWB NUMBER</div>
                      <div class="awb-value">%s</div>
                    </div>
                    %s
                  </div>
                  <div class="section-label">SHIP TO</div>
                  <div class="ship-name">%s</div>
                  <div class="muted">%s</div>
                  <div class="muted">%s</div>
                  <div class="muted">PIN: %s | Ph: %s</div>
                  <div class="meta">
                    <div><span>Order #</span><strong>%s</strong></div>
                    <div><span>Invoice</span><strong>%s</strong></div>
                    <div><span>Date</span><strong>%s</strong></div>
                    <div><span>Payment</span><strong>%s %s</strong></div>
                    <div><span>Weight</span><strong>%s</strong></div>
                    <div><span>Dimensions</span><strong>%s</strong></div>
                  </div>
                  <div class="section-label">PRODUCT DETAILS</div>
                  <table>
                    <thead>
                      <tr>
                        <th>Item</th><th>HSN</th><th>Q</th><th>Price</th><th>CGST</th><th>SGST</th><th>IGST</th><th>Total</th>
                      </tr>
                    </thead>
                    <tbody>
                      %s
                      <tr>
                        <td colspan="4"><strong>TOTAL</strong></td>
                        <td style="text-align:right"><strong>%s</strong></td>
                        <td style="text-align:right"><strong>%s</strong></td>
                        <td style="text-align:right"><strong>%s</strong></td>
                        <td style="text-align:right"><strong>%s</strong></td>
                      </tr>
                    </tbody>
                  </table>
                  <div style="margin-top:14px;">
                    <div class="section-label">RETURN ADDRESS</div>
                    <div class="ship-name">%s</div>
                    <div class="muted">%s</div>
                  </div>
                  <div class="footer">
                    <div>GST: %s</div>
                    <div>AUTO-GENERATED LABEL · NO SIGNATURE REQUIRED</div>
                    <div>Powered By Flint &amp; Thread</div>
                  </div>
                </body>
                </html>
                """.formatted(
                esc(str(company.get("name"))),
                esc(str(label.get("courierName"))),
                esc(awb),
                qrImg.isBlank() ? "" : "<img class=\"qr\" src=\"" + qrImg + "\" alt=\"QR\" />",
                esc(str(shipping.get("name"))),
                esc(joinNonBlank(str(shipping.get("line1")), str(shipping.get("line2")))),
                esc(joinNonBlank(str(shipping.get("city")), str(shipping.get("state")))),
                esc(str(shipping.get("pincode"))),
                esc(str(shipping.get("phone"))),
                esc(str(label.get("orderNumber"))),
                esc(str(label.get("invoiceNumber"))),
                esc(formatDate(label.get("orderDate"))),
                paymentLabel,
                money(grandTotal),
                esc(weightText),
                esc(dimensionsText),
                itemRows,
                money(totalCgst),
                money(totalSgst),
                money(totalIgst),
                money(grandTotal),
                esc(str(seller.get("name"))),
                esc(formatSellerAddress(sellerAddress)),
                esc(gstFooter)
        );
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Object value) {
        if (value instanceof List<?> raw) {
            return (List<Map<String, Object>>) raw;
        }
        return List.of();
    }

    private static Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> raw) {
            return (Map<String, Object>) raw;
        }
        return Map.of();
    }

    private static String formatSellerAddress(Map<String, Object> address) {
        return joinNonBlank(
                str(address.get("line1")),
                joinNonBlank(str(address.get("city")), str(address.get("state"))),
                str(address.get("pincode")));
    }

    private static String formatDimensions(Map<String, Object> dimensions) {
        if (dimensions.isEmpty()) {
            return "—";
        }
        BigDecimal l = decimal(dimensions.get("l"));
        BigDecimal w = decimal(dimensions.get("w"));
        BigDecimal h = decimal(dimensions.get("h"));
        if (l.compareTo(BigDecimal.ZERO) == 0
                && w.compareTo(BigDecimal.ZERO) == 0
                && h.compareTo(BigDecimal.ZERO) == 0) {
            return "—";
        }
        return l.setScale(1, RoundingMode.HALF_UP)
                + "cm × "
                + w.setScale(1, RoundingMode.HALF_UP)
                + "cm × "
                + h.setScale(1, RoundingMode.HALF_UP)
                + "cm";
    }

    private static String formatWeight(Object value) {
        BigDecimal weight = decimal(value);
        if (weight.compareTo(BigDecimal.ZERO) == 0) {
            return "—";
        }
        return weight.setScale(2, RoundingMode.HALF_UP) + " kg";
    }

    private static String formatDate(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return DATE_FMT.format(dateTime);
        }
        String text = str(value);
        return text.isBlank() ? "—" : text;
    }

    private static String joinNonBlank(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(part.trim());
        }
        return builder.toString();
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private static String money(Object value) {
        return "₹" + decimal(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static String esc(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
