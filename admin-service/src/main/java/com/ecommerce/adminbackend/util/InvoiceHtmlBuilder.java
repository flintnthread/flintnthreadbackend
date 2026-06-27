package com.ecommerce.adminbackend.util;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class InvoiceHtmlBuilder {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private InvoiceHtmlBuilder() {
    }

    @SuppressWarnings("unchecked")
    public static String build(Map<String, Object> invoice) {
        Map<String, Object> company = map(invoice.get("company"));
        Map<String, Object> billing = map(invoice.get("billing"));
        Map<String, Object> shipping = map(invoice.get("shipping"));
        Map<String, Object> totals = map(invoice.get("totals"));
        Map<String, Object> gstBreakdown = map(invoice.get("gstBreakdown"));
        Map<String, Object> payment = map(invoice.get("payment"));
        Map<String, Object> qrCode = map(invoice.get("qrCode"));
        List<Map<String, Object>> sellerGroups = list(invoice.get("sellerGroups"));
        Map<String, Object> firstGroup = sellerGroups.isEmpty() ? Map.of() : sellerGroups.get(0);
        Map<String, Object> seller = map(firstGroup.get("seller"));
        Map<String, Object> sellerAddress = map(seller.get("address"));

        boolean isIntraState = Boolean.TRUE.equals(invoice.get("isIntraState"));
        String qrImg = str(qrCode.get("imageDataUrl"));

        StringBuilder itemRows = new StringBuilder();
        for (Map<String, Object> group : sellerGroups) {
            for (Map<String, Object> item : list(group.get("products"))) {
                itemRows.append("<tr>")
                        .append("<td>").append(esc(str(item.get("name")))).append("</td>")
                        .append("<td style=\"text-align:center\">").append(esc(str(item.get("hsnCode")))).append("</td>")
                        .append("<td style=\"text-align:right\">").append(str(item.get("qty"))).append("</td>")
                        .append("<td style=\"text-align:right\">").append(money(item.get("unitPrice"))).append("</td>")
                        .append("<td style=\"text-align:right\">").append(money(item.get("taxPercent"))).append("%</td>")
                        .append("<td style=\"text-align:right\">").append(money(item.get("taxAmount"))).append("</td>")
                        .append("<td style=\"text-align:right\">").append(money(item.get("lineTotal"))).append("</td>")
                        .append("</tr>");
            }
        }

        BigDecimal shippingAmount = decimal(totals.get("shipping"));
        String shippingLabel = shippingAmount.compareTo(BigDecimal.ZERO) == 0 ? "FREE" : money(shippingAmount);

        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8" />
                  <style>
                    body { font-family: Arial, sans-serif; color: #111827; padding: 24px; }
                    h1 { color: #1E2B6B; margin: 0 0 4px; }
                    .meta { color: #6B7280; font-size: 13px; margin-bottom: 6px; }
                    .grid { display: flex; gap: 24px; margin-bottom: 18px; }
                    .col { flex: 1; }
                    .label { font-size: 11px; font-weight: 700; color: #F97316; }
                    .name { font-weight: 700; margin: 4px 0; }
                    .muted { color: #6B7280; font-size: 13px; line-height: 1.5; }
                    table { width: 100%%; border-collapse: collapse; margin-top: 12px; font-size: 12px; }
                    th, td { border-bottom: 1px solid #E5E7EB; padding: 8px 6px; vertical-align: top; }
                    th { background: #FFF7ED; text-align: left; }
                    .totals { margin-top: 18px; width: 320px; margin-left: auto; }
                    .totals div { display: flex; justify-content: space-between; padding: 6px 0; }
                    .grand { font-size: 16px; font-weight: 800; border-top: 2px solid #1E2B6B; margin-top: 8px; padding-top: 8px; }
                    .qr { width: 88px; height: 88px; }
                  </style>
                </head>
                <body>
                  <div style="display:flex; justify-content:space-between; gap:20px;">
                    <div>
                      <h1>INVOICE</h1>
                      <div class="meta">%s</div>
                      <div class="meta">Order: %s</div>
                      <div class="meta">Date: %s</div>
                      <div class="muted" style="margin-top:12px;">
                        <strong>%s</strong><br/>
                        %s<br/>
                        Phone: %s<br/>
                        Email: %s<br/>
                        GSTIN: %s
                      </div>
                    </div>
                    %s
                  </div>
                  <div class="grid">
                    <div class="col">
                      <div class="label">SOLD BY</div>
                      <div class="name">%s</div>
                      <div class="muted">
                        %s<br/>
                        %s<br/>
                        %s
                      </div>
                    </div>
                  </div>
                  <div class="grid">
                    <div class="col">
                      <div class="label">BILL TO</div>
                      <div class="name">%s</div>
                      <div class="muted">%s</div>
                    </div>
                    <div class="col">
                      <div class="label">SHIP TO</div>
                      <div class="name">%s</div>
                      <div class="muted">%s</div>
                    </div>
                  </div>
                  <table>
                    <thead>
                      <tr>
                        <th>Item</th>
                        <th>HSN</th>
                        <th style="text-align:right">Qty</th>
                        <th style="text-align:right">Unit Price</th>
                        <th style="text-align:right">Tax %%</th>
                        <th style="text-align:right">Tax Amt</th>
                        <th style="text-align:right">Total</th>
                      </tr>
                    </thead>
                    <tbody>%s</tbody>
                  </table>
                  <div class="totals">
                    <div><span>Subtotal</span><span>%s</span></div>
                    <div><span>%s</span><span>%s</span></div>
                    <div><span>Shipping</span><span>%s</span></div>
                    <div class="grand"><span>Grand Total</span><span>%s</span></div>
                  </div>
                  <div class="muted" style="margin-top:18px;">
                    CGST: %s | SGST: %s | IGST: %s<br/>
                    Payment: %s | Status: %s
                  </div>
                </body>
                </html>
                """.formatted(
                esc(str(invoice.get("invoiceNumber"))),
                esc(str(invoice.get("orderNumber"))),
                esc(formatDate(invoice.get("invoiceDate"), invoice.get("orderDate"))),
                esc(str(company.get("name"))),
                esc(str(company.get("country"))),
                esc(str(company.get("phone"))),
                esc(str(company.get("email"))),
                esc(str(company.get("gstin"))),
                qrImg.isBlank() ? "" : "<img class=\"qr\" src=\"" + qrImg + "\" alt=\"Order QR\" />",
                esc(str(seller.get("name"))),
                esc(joinAddress(sellerAddress)),
                sellerPhoneEmailGst(seller),
                esc(formatCustomer(billing)),
                esc(str(billing.get("name"))),
                esc(formatCustomer(shipping)),
                esc(str(shipping.get("name"))),
                itemRows,
                money(totals.get("subtotal")),
                isIntraState ? "CGST + SGST" : "IGST",
                money(totals.get("tax")),
                shippingLabel,
                money(totals.get("grandTotal")),
                money(gstBreakdown.get("cgst")),
                money(gstBreakdown.get("sgst")),
                money(gstBreakdown.get("igst")),
                esc(str(payment.get("method"))),
                esc(str(payment.get("status")))
        );
    }

    private static String sellerPhoneEmailGst(Map<String, Object> seller) {
        StringBuilder sb = new StringBuilder();
        String phone = str(seller.get("phone"));
        String email = str(seller.get("email"));
        String gstin = str(seller.get("gstin"));
        if (!phone.isBlank()) {
            sb.append("Phone: ").append(esc(phone)).append("<br/>");
        }
        if (!email.isBlank()) {
            sb.append("Email: ").append(esc(email)).append("<br/>");
        }
        if (!gstin.isBlank()) {
            sb.append("GSTIN: ").append(esc(gstin));
        }
        return sb.toString();
    }

    private static String formatCustomer(Map<String, Object> address) {
        String line = str(address.get("address"));
        if (line.isBlank()) {
            line = (str(address.get("line1")) + " " + str(address.get("line2"))).trim();
        }
        String cityState = String.join(", ",
                str(address.get("city")),
                str(address.get("state"))).replaceAll("(^, |, $)", "").trim();
        String pincode = str(address.get("pincode"));
        String phone = str(address.get("phone"));
        String email = str(address.get("email"));
        StringBuilder sb = new StringBuilder();
        if (!line.isBlank()) {
            sb.append(esc(line)).append("<br/>");
        }
        if (!cityState.isBlank()) {
            sb.append(esc(cityState));
            if (!pincode.isBlank()) {
                sb.append(" - ").append(esc(pincode));
            }
            sb.append("<br/>");
        }
        if (!phone.isBlank()) {
            sb.append("Phone: ").append(esc(phone)).append("<br/>");
        }
        if (!email.isBlank()) {
            sb.append("Email: ").append(esc(email));
        }
        return sb.toString();
    }

    private static String joinAddress(Map<String, Object> address) {
        String line1 = str(address.get("line1"));
        String cityState = String.join(", ",
                str(address.get("city")),
                str(address.get("state"))).replaceAll("(^, |, $)", "").trim();
        String pincode = str(address.get("pincode"));
        StringBuilder sb = new StringBuilder();
        if (!line1.isBlank()) {
            sb.append(esc(line1)).append("<br/>");
        }
        if (!cityState.isBlank()) {
            sb.append(esc(cityState));
            if (!pincode.isBlank()) {
                sb.append(" - ").append(esc(pincode));
            }
        }
        return sb.toString();
    }

    private static String formatDate(Object primary, Object fallback) {
        LocalDateTime value = parseDate(primary);
        if (value == null) {
            value = parseDate(fallback);
        }
        return value != null ? value.format(DATE_FMT) : "—";
    }

    private static LocalDateTime parseDate(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .toList();
        }
        return List.of();
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private static String money(Object value) {
        return "₹" + decimal(value).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static String esc(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
