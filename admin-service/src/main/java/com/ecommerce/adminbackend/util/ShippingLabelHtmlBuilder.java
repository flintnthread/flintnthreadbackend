package com.ecommerce.adminbackend.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shipping label HTML for PDF — matches Flint &amp; Thread label layout:
 * brand header → title → courier + GST pill → barcode + QR → SHIP TO → meta →
 * PRODUCT DETAILS → RETURN ADDRESS (+ GST) → footer.
 */
public final class ShippingLabelHtmlBuilder {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

    private ShippingLabelHtmlBuilder() {
    }

    @SuppressWarnings("unchecked")
    public static String build(Map<String, Object> label) {
        Map<String, Object> company = map(label.get("company"));
        Map<String, Object> shipping = map(label.get("shipping"));
        Map<String, Object> totals = map(label.get("totals"));
        Map<String, Object> payment = map(label.get("payment"));
        Map<String, Object> qrCode = map(label.get("qrCode"));
        Map<String, Object> barcode = map(label.get("barcode"));
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
            awb = str(barcode.get("value"));
        }
        if (awb.isBlank()) {
            awb = str(label.get("orderNumber")).replaceAll("\\D", "");
        }

        String sellerGst = firstNonBlank(
                str(seller.get("gstin")),
                str(sellerAddress.get("gstin")),
                str(label.get("gstNumber")),
                str(company.get("gstin")));
        if (sellerGst.isBlank()) {
            sellerGst = "—";
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
                        .append("<td class=\"c\">").append(esc(firstNonBlank(str(item.get("hsnCode")), "—"))).append("</td>")
                        .append("<td class=\"c\">").append(str(item.get("qty"))).append("</td>")
                        .append("<td class=\"r\">").append(money(item.get("unitPrice"))).append("</td>")
                        .append("<td class=\"r\">").append(cgst.compareTo(BigDecimal.ZERO) > 0 ? money(cgst) : "₹0.00").append("</td>")
                        .append("<td class=\"r\">").append(sgst.compareTo(BigDecimal.ZERO) > 0 ? money(sgst) : "₹0.00").append("</td>")
                        .append("<td class=\"r\">").append(igst.compareTo(BigDecimal.ZERO) > 0 ? money(igst) : "₹0.00").append("</td>")
                        .append("<td class=\"r\"><strong>").append(money(lineTotal)).append("</strong></td>")
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
        String paymentLabel = paymentMethod.toLowerCase(Locale.ROOT).contains("cod") ? "COD" : "PREPAID";
        String qrImg = str(qrCode.get("imageDataUrl"));
        String barcodeImg = str(barcode.get("imageDataUrl"));
        String courier = firstNonBlank(str(label.get("courierName")), "Courier");
        String companyName = firstNonBlank(str(company.get("name")), "Flint & Thread");
        String returnPhone = firstNonBlank(str(sellerAddress.get("phone")), str(seller.get("phone")));
        String returnAddr = formatSellerAddress(sellerAddress);
        if (!returnPhone.isBlank()) {
            returnAddr = returnAddr.isBlank()
                    ? "Ph: " + returnPhone
                    : returnAddr + " | Ph: " + returnPhone;
        }

        String qrHtml = qrImg.isBlank()
                ? ""
                : "<img class=\"qr\" src=\"" + qrImg + "\" alt=\"QR\" />";
        String barcodeHtml = barcodeImg.isBlank()
                ? "<div class=\"barcode-fallback\">" + esc(awb) + "</div>"
                : "<img class=\"barcode\" src=\"" + barcodeImg + "\" alt=\"Barcode\" />";

        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8" />
                  <style>
                    @page { size: A4; margin: 10mm; }
                    * { box-sizing: border-box; }
                    body {
                      font-family: Arial, Helvetica, sans-serif;
                      color: #111827;
                      margin: 0;
                      padding: 0;
                      background: #fff;
                    }
                    .sheet {
                      position: relative;
                      max-width: 520px;
                      margin: 0 auto;
                      border: 1px solid #111;
                      overflow: hidden;
                    }
                    .watermark {
                      position: absolute;
                      left: 50%%;
                      top: 42%%;
                      transform: translate(-50%%, -50%%) rotate(-28deg);
                      opacity: 0.06;
                      font-size: 28px;
                      font-weight: 900;
                      letter-spacing: 2px;
                      white-space: nowrap;
                      pointer-events: none;
                      z-index: 0;
                      color: #1E2B6B;
                    }
                    .content { position: relative; z-index: 1; }
                    .brand-row {
                      display: flex;
                      align-items: center;
                      gap: 10px;
                      padding: 12px 14px 8px;
                      border-bottom: 1px solid #E5E7EB;
                    }
                    .brand-mark {
                      width: 42px;
                      height: 42px;
                      border-radius: 8px;
                      background: linear-gradient(135deg, #1E2B6B 0%%, #F97316 100%%);
                      color: #fff;
                      font-weight: 900;
                      font-size: 16px;
                      display: flex;
                      align-items: center;
                      justify-content: center;
                    }
                    .brand-name { font-size: 15px; font-weight: 900; letter-spacing: 0.5px; color: #1E2B6B; }
                    .brand-tag { font-size: 9px; color: #6B7280; margin-top: 2px; }
                    .title {
                      text-align: center;
                      font-size: 13px;
                      font-weight: 900;
                      letter-spacing: 1px;
                      padding: 10px 12px 8px;
                      text-transform: uppercase;
                    }
                    .courier-row {
                      display: flex;
                      justify-content: space-between;
                      align-items: center;
                      gap: 10px;
                      padding: 0 14px 10px;
                      flex-wrap: wrap;
                    }
                    .courier-name { font-size: 13px; font-weight: 700; }
                    .gst-pill {
                      background: #F97316;
                      color: #fff;
                      font-size: 10px;
                      font-weight: 800;
                      padding: 4px 10px;
                      border-radius: 999px;
                      white-space: nowrap;
                    }
                    .codes {
                      display: flex;
                      align-items: center;
                      justify-content: space-between;
                      gap: 12px;
                      padding: 8px 14px 12px;
                      border-bottom: 1px solid #E5E7EB;
                    }
                    .codes-left { flex: 1; text-align: center; min-width: 0; }
                    .barcode { max-width: 100%%; height: 52px; object-fit: contain; }
                    .barcode-fallback {
                      font-size: 18px;
                      font-weight: 900;
                      letter-spacing: 2px;
                      padding: 12px;
                      border: 1px dashed #CBD5E1;
                    }
                    .awb-under { font-size: 12px; font-weight: 700; letter-spacing: 1px; margin-top: 4px; }
                    .qr { width: 78px; height: 78px; border: 1px solid #E5E7EB; }
                    .pad { padding: 12px 14px; }
                    .section-h {
                      font-size: 11px;
                      font-weight: 900;
                      letter-spacing: 0.6px;
                      margin-bottom: 6px;
                      text-transform: uppercase;
                    }
                    .ship-name { font-size: 14px; font-weight: 800; margin-bottom: 4px; }
                    .muted { color: #374151; font-size: 12px; line-height: 1.45; }
                    .meta { margin-top: 10px; font-size: 12px; }
                    .meta-row {
                      display: flex;
                      gap: 8px;
                      padding: 3px 0;
                    }
                    .meta-k { width: 92px; color: #6B7280; font-weight: 600; flex-shrink: 0; }
                    .meta-v { font-weight: 700; word-break: break-word; }
                    .prod-h {
                      background: #1E2B6B;
                      color: #fff;
                      font-size: 11px;
                      font-weight: 900;
                      letter-spacing: 0.8px;
                      padding: 8px 14px;
                      text-transform: uppercase;
                    }
                    table { width: 100%%; border-collapse: collapse; font-size: 10px; }
                    th, td { padding: 6px 5px; border-bottom: 1px solid #E5E7EB; vertical-align: top; }
                    th { background: #F3F4F6; text-align: left; font-weight: 800; }
                    .c { text-align: center; }
                    .r { text-align: right; }
                    .total-row td { font-weight: 800; background: #F9FAFB; border-top: 1px solid #111; }
                    .return {
                      padding: 12px 14px;
                      border-top: 1px solid #E5E7EB;
                      background: #FFF9F5;
                    }
                    .return-top {
                      display: flex;
                      align-items: center;
                      gap: 8px;
                      flex-wrap: wrap;
                      margin-bottom: 4px;
                    }
                    .return-name { font-size: 13px; font-weight: 900; }
                    .footer {
                      text-align: center;
                      padding: 10px 12px 12px;
                      font-size: 10px;
                      color: #6B7280;
                      border-top: 1px solid #E5E7EB;
                    }
                    .footer strong { color: #1E2B6B; }
                  </style>
                </head>
                <body>
                  <div class="sheet">
                    <div class="watermark">FLINT &amp; THREAD</div>
                    <div class="content">
                      <div class="brand-row">
                        <div class="brand-mark">FT</div>
                        <div>
                          <div class="brand-name">FLINT &amp; THREAD</div>
                          <div class="brand-tag">The infinity and Vanguard.</div>
                        </div>
                      </div>
                      <div class="title">SHIPPING LABEL FOR FLINT &amp; THREAD</div>
                      <div class="courier-row">
                        <div class="courier-name">%s</div>
                        <div class="gst-pill">GST: %s</div>
                      </div>
                      <div class="codes">
                        <div class="codes-left">
                          %s
                          <div class="awb-under">%s</div>
                        </div>
                        %s
                      </div>
                      <div class="pad">
                        <div class="section-h">SHIP TO</div>
                        <div class="ship-name">%s</div>
                        <div class="muted">%s</div>
                        <div class="muted">%s</div>
                        <div class="muted">PIN: %s%s</div>
                        <div class="meta">
                          <div class="meta-row"><span class="meta-k">Order ID:</span><span class="meta-v">%s</span></div>
                          <div class="meta-row"><span class="meta-k">AWB #:</span><span class="meta-v">%s</span></div>
                          <div class="meta-row"><span class="meta-k">Invoice:</span><span class="meta-v">%s</span></div>
                          <div class="meta-row"><span class="meta-k">Date:</span><span class="meta-v">%s</span></div>
                          <div class="meta-row"><span class="meta-k">Payment:</span><span class="meta-v">%s %s</span></div>
                          <div class="meta-row"><span class="meta-k">Weight:</span><span class="meta-v">%s</span></div>
                          <div class="meta-row"><span class="meta-k">Dimensions:</span><span class="meta-v">%s</span></div>
                        </div>
                      </div>
                      <div class="prod-h">PRODUCT DETAILS</div>
                      <table>
                        <thead>
                          <tr>
                            <th>Item</th><th class="c">HSN</th><th class="c">Q</th><th class="r">Price</th>
                            <th class="r">CGST</th><th class="r">SGST</th><th class="r">IGST</th><th class="r">Total</th>
                          </tr>
                        </thead>
                        <tbody>
                          %s
                          <tr class="total-row">
                            <td colspan="4"><strong>TOTAL</strong></td>
                            <td class="r">%s</td>
                            <td class="r">%s</td>
                            <td class="r">%s</td>
                            <td class="r">%s</td>
                          </tr>
                        </tbody>
                      </table>
                      <div class="return">
                        <div class="section-h">RETURN ADDRESS</div>
                        <div class="return-top">
                          <div class="return-name">%s</div>
                          <div class="gst-pill">GST: %s</div>
                        </div>
                        <div class="muted">%s</div>
                      </div>
                      <div class="footer">
                        <div>AUTO GENERATED LABEL - NO SIGNATURE REQUIRED.</div>
                        <div style="margin-top:4px;">Powered By <strong>%s</strong></div>
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                esc(courier),
                esc(sellerGst),
                barcodeHtml,
                esc(awb),
                qrHtml,
                esc(str(shipping.get("name"))),
                esc(joinNonBlank(str(shipping.get("line1")), str(shipping.get("line2")))),
                esc(joinNonBlank(str(shipping.get("city")), str(shipping.get("state")), str(shipping.get("country")))),
                esc(str(shipping.get("pincode"))),
                str(shipping.get("phone")).isBlank() ? "" : esc(" | Ph: " + str(shipping.get("phone"))),
                esc(str(label.get("orderNumber"))),
                esc(awb),
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
                esc(sellerGst),
                esc(returnAddr.isBlank() ? "Registered seller address on file" : returnAddr),
                esc(companyName)
        );
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Object value) {
        if (value instanceof List<?> raw) {
            return (List<Map<String, Object>>) raw;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
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
                str(address.get("pincode")),
                str(address.get("country")));
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
        return l.setScale(2, RoundingMode.HALF_UP)
                + "cm x "
                + w.setScale(2, RoundingMode.HALF_UP)
                + "cm x "
                + h.setScale(2, RoundingMode.HALF_UP)
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

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
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
