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
        
        String gstBreakdownNote = isIntraState 
            ? "*intra-state transaction - CGST and SGST applicable"
            : "*inter-state transaction - IGST applicable";

        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1" />
                  <title>Invoice %s</title>
                  <style>
                    body { font-family: Arial, sans-serif; margin: 0; background: #fff; color: #1f2937; font-size: 12px; }
                    .page { max-width: 794px; margin: 0 auto; background: #fff; border: none; border-radius: 0; overflow: hidden; }
                    .section { padding: 8px 12px; }
                    .header { display: flex; justify-content: space-between; gap: 10px; border-bottom: 1px solid #e5e7eb; padding-bottom: 6px; }
                    .company h1 { margin: 0 0 4px; font-size: 20px; }
                    .company p { margin: 2px 0; font-size: 12px; }
                    .invoice-meta { min-width: 220px; border: 1px solid #e5e7eb; border-radius: 6px; padding: 8px; }
                    .invoice-meta h2 { margin: 0 0 4px; font-size: 16px; color: #b91c1c; text-transform: uppercase; letter-spacing: 0.5px; }
                    .invoice-meta p { margin: 2px 0; font-size: 11px; }
                    .heading { margin: 0 0 6px; font-size: 18px; }
                    .subheading { margin: 0 0 4px; font-size: 11px; text-transform: uppercase; color: #6b7280; }
                    .party-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 8px; }
                    .party-box { border: 1px solid #e5e7eb; border-radius: 6px; padding: 8px; }
                    .party-box h3 { margin: 0 0 4px; font-size: 14px; }
                    .party-box p { margin: 2px 0; font-size: 11px; }
                    .items-table { width: 100%%; border-collapse: collapse; margin-top: 6px; table-layout: fixed; }
                    .items-table th { background: #123763; color: #fff; font-size: 9px; text-align: left; padding: 5px 4px; text-transform: uppercase; vertical-align: middle; }
                    .items-table th:nth-child(2) { text-align: center; }
                    .items-table th:nth-child(n+3) { text-align: right; }
                    .items-table td { font-size: 10px; padding: 5px 4px; border-bottom: 1px solid #e5e7eb; vertical-align: top; word-wrap: break-word; }
                    .items-table td:nth-child(2) { text-align: center; }
                    .items-table td.right { text-align: right; white-space: nowrap; }
                    .item-meta { color: #6b7280; font-size: 9px; margin-top: 1px; }
                    .total { text-align: right; border-top: 1px solid #e5e7eb; margin-top: 8px; padding-top: 6px; }
                    .total p { margin: 3px 0; font-size: 11px; }
                    .grand { font-size: 20px; color: #92400e; font-weight: 700; }
                    .muted { color: #6b7280; font-size: 10px; }
                    @page { size: A4 portrait; margin: 7mm; }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <div class="section header">
                      <div class="company">
                        <h1>%s</h1>
                        <p>%s</p>
                        <p><strong>Phone:</strong> %s</p>
                        <p><strong>Email:</strong> %s</p>
                        <p><strong>GSTIN:</strong> %s</p>
                      </div>
                      <div class="invoice-meta">
                        <h2>Invoice</h2>
                        <p><strong>Invoice:</strong> %s</p>
                        <p><strong>Order ID:</strong> %s</p>
                        <p><strong>Date:</strong> %s</p>
                        <p><strong>Order Date:</strong> %s</p>
                      </div>
                    </div>

                    <div class="section">
                      <p class="subheading">Sold By</p>
                      <p><strong>%s</strong></p>
                      <p>%s</p>
                      <p><strong>Phone:</strong> %s</p>
                      <p><strong>Email:</strong> %s</p>
                    </div>

                    <div class="section">
                      <table class="items-table">
                        <thead>
                          <tr>
                            <th>Item Description</th>
                            <th>HSN Code</th>
                            <th>Qty</th>
                            <th>Unit Price</th>
                            <th>Tax %%</th>
                            <th>Tax Amount</th>
                            <th>Total</th>
                          </tr>
                        </thead>
                        <tbody>
                          %s
                        </tbody>
                      </table>

                      <div class="party-grid">
                        <div class="party-box">
                          <h3>Bill To:</h3>
                          <p><strong>%s</strong></p>
                          <p>%s</p>
                          <p><strong>Phone:</strong> %s</p>
                          <p><strong>Email:</strong> %s</p>
                        </div>
                        <div class="party-box">
                          <h3>Ship To:</h3>
                          <p><strong>%s</strong></p>
                          <p>%s</p>
                          <p><strong>Phone:</strong> %s</p>
                          <p><strong>Email:</strong> %s</p>
                        </div>
                      </div>
                      <div class="total">
                        <p><strong>Subtotal (Before Tax):</strong> Rs %s</p>
                        <p class="subheading">GST Breakdown Summary</p>
                        <p><strong>Total GST:</strong> Rs %s</p>
                        <p><strong>Total CGST:</strong> Rs %s</p>
                        <p><strong>Total SGST:</strong> Rs %s</p>
                        <p><strong>Total IGST:</strong> Rs %s</p>
                        <p class="muted">%s</p>
                        <p><strong>Shipping Charges:</strong> Rs %s</p>
                        <p><strong>Grand Total:</strong></p>
                        <p class="grand">Rs %s</p>
                      </div>
                      <p class="muted">Generated by FlintNThread backend.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                esc(str(invoice.get("invoiceNumber"))),
                esc(str(company.get("name"))),
                esc(str(company.get("country"))),
                esc(str(company.get("phone"))),
                esc(str(company.get("email"))),
                esc(str(company.get("gstin"))),
                esc(str(invoice.get("invoiceNumber"))),
                esc(str(invoice.get("orderNumber"))),
                esc(formatDate(invoice.get("invoiceDate"), invoice.get("orderDate"))),
                esc(formatDate(invoice.get("orderDate"), invoice.get("orderDate"))),
                esc(str(seller.get("name"))),
                esc(joinAddress(sellerAddress)),
                esc(str(seller.get("phone"))),
                esc(str(seller.get("email"))),
                itemRows,
                esc(str(billing.get("name"))),
                esc(formatCustomer(billing)),
                esc(str(billing.get("phone"))),
                esc(str(billing.get("email"))),
                esc(str(shipping.get("name"))),
                esc(formatCustomer(shipping)),
                esc(str(shipping.get("phone"))),
                esc(str(shipping.get("email"))),
                money(totals.get("subtotal")),
                money(totals.get("tax")),
                money(gstBreakdown.get("cgst")),
                money(gstBreakdown.get("sgst")),
                money(gstBreakdown.get("igst")),
                gstBreakdownNote,
                shippingLabel,
                money(totals.get("grandTotal"))
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
