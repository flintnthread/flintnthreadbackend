package com.ecommerce.authdemo.mail;

import com.ecommerce.authdemo.dto.OrderItemDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

/**
 * HTML order confirmation email — matches Flint & Thread branded layout
 * (gradient hero, order details panel with orange accent).
 */
public final class OrderConfirmationEmailBuilder {

    private static final String BRAND_ORANGE = "#E97A1F";
    private static final String BRAND_ORANGE_DARK = "#C45F12";
    private static final String SUPPORT_EMAIL = "support@flintnthread.in";
    private static final String SUPPORT_PHONE = "+91 9063499092";

    private OrderConfirmationEmailBuilder() {
    }

    public static String buildSubject(String orderNumber) {
        String num = safe(orderNumber);
        return "Order Confirmation - #" + num + " | Flint & Thread";
    }

    public static String buildHtml(OrderConfirmationEmailModel model) {
        String customerName = escape(safe(model.customerName()));
        String orderNumber = escape(safe(model.orderNumber()));
        String orderDate = escape(safe(model.orderDate()));
        String paymentLabel = escape(safe(model.paymentMethodLabel()));
        String paymentStatus = escape(safe(model.paymentStatusLabel()));
        String shippingAddress = escape(safe(model.shippingAddress())).replace("\n", "<br/>");
        String orderViewUrl = escape(safe(model.orderViewUrl()));
        String itemsHtml = buildItemsRows(model.items());
        String breakdownHtml = buildBreakdownRows(model);

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>Order Confirmation</title>
            </head>
            <body style="margin:0;padding:0;background:#f3f4f6;font-family:Segoe UI,Roboto,Helvetica,Arial,sans-serif;color:#111827;">
              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f3f4f6;padding:24px 12px;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:600px;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb;">
                      <tr>
                        <td style="padding:20px 24px 8px;text-align:center;">
                          <div style="font-size:28px;font-weight:800;letter-spacing:-0.5px;">
                            <span style="color:#1f2937;">F</span><span style="color:%1$s;">&amp;T</span>
                          </div>
                          <div style="font-size:12px;color:#6b7280;margin-top:4px;">Flint &amp; Thread</div>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:0 24px 20px;">
                          <div style="background:linear-gradient(135deg,%1$s 0%%,%2$s 100%%);border-radius:10px;padding:28px 20px;text-align:center;color:#ffffff;">
                            <div style="font-size:32px;line-height:1;margin-bottom:8px;">&#127881;</div>
                            <div style="font-size:22px;font-weight:800;margin-bottom:6px;">Order Confirmed!</div>
                            <div style="font-size:14px;opacity:0.95;">Thank you for your purchase</div>
                          </div>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:0 24px 16px;font-size:15px;line-height:1.6;color:#374151;">
                          <p style="margin:0 0 12px;">Hello <strong>%3$s</strong>,</p>
                          <p style="margin:0;">Your order has been successfully placed and is being processed. We&apos;re excited to get your items to you!</p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:0 24px 24px;">
                          <div style="background:#f9fafb;border-radius:10px;border-left:4px solid %1$s;padding:16px 18px;">
                            <div style="font-size:16px;font-weight:800;color:#111827;margin-bottom:12px;">Order Details</div>
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="font-size:14px;color:#374151;">
                              <tr><td style="padding:4px 0;color:#6b7280;width:140px;">Order Number</td><td style="padding:4px 0;font-weight:700;color:#16a34a;">#%4$s</td></tr>
                              <tr><td style="padding:4px 0;color:#6b7280;">Order Date</td><td style="padding:4px 0;">%5$s</td></tr>
                              <tr><td style="padding:4px 0;color:#6b7280;">Payment</td><td style="padding:4px 0;">%6$s (%7$s)</td></tr>
                            </table>
                            %8$s
                            <div style="margin-top:14px;font-size:13px;font-weight:700;color:#111827;">Items</div>
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="margin-top:8px;font-size:13px;border-collapse:collapse;">
                              <tr style="background:#f3f4f6;">
                                <th align="left" style="padding:8px;border-bottom:1px solid #e5e7eb;">Product</th>
                                <th align="center" style="padding:8px;border-bottom:1px solid #e5e7eb;">Qty</th>
                                <th align="right" style="padding:8px;border-bottom:1px solid #e5e7eb;">Amount</th>
                              </tr>
                              %9$s
                            </table>
                            %10$s
                            <div style="margin-top:16px;font-size:13px;color:#6b7280;">Delivery Address</div>
                            <div style="font-size:14px;line-height:1.5;margin-top:4px;">%11$s</div>
                          </div>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:0 24px 24px;text-align:center;">
                          <a href="%12$s" style="display:inline-block;background:%1$s;color:#ffffff;text-decoration:none;font-weight:700;font-size:14px;padding:12px 24px;border-radius:999px;">View Order Details</a>
                        </td>
                      </tr>
                      %13$s
                      <tr>
                        <td style="padding:16px 24px 24px;border-top:1px solid #f3f4f6;font-size:12px;color:#9ca3af;text-align:center;line-height:1.5;">
                          Questions? Contact us at <a href="mailto:%14$s" style="color:%1$s;">%14$s</a><br/>
                          &copy; Flint &amp; Thread &mdash; <a href="https://flintnthread.in" style="color:%1$s;">flintnthread.in</a>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(
                BRAND_ORANGE,
                BRAND_ORANGE_DARK,
                customerName,
                orderNumber,
                orderDate,
                paymentLabel,
                paymentStatus,
                breakdownHtml,
                itemsHtml,
                buildPayableHighlight(model),
                shippingAddress.isBlank() ? "—" : shippingAddress,
                orderViewUrl.isBlank() ? "https://flintnthread.in" : orderViewUrl,
                buildNeedHelpSection(),
                SUPPORT_EMAIL
        );
    }

    private static String buildItemsRows(List<OrderItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return """
                <tr>
                  <td colspan="3" style="padding:10px 8px;border-bottom:1px solid #e5e7eb;">Your order items</td>
                </tr>
                """;
        }
        StringBuilder sb = new StringBuilder();
        for (OrderItemDTO item : items) {
            String name = escape(safe(item.getProductName()));
            String meta = buildItemMeta(item);
            if (!meta.isBlank()) {
                name += "<br/><span style=\"color:#9ca3af;font-size:11px;\">" + escape(meta) + "</span>";
            }
            int qty = item.getQuantity() != null ? item.getQuantity() : 1;
            double lineTotal = item.getTotal() != null
                    ? item.getTotal()
                    : (item.getPrice() != null ? item.getPrice() : 0) * qty;
            sb.append("""
                <tr>
                  <td style="padding:10px 8px;border-bottom:1px solid #f3f4f6;vertical-align:top;">%s</td>
                  <td align="center" style="padding:10px 8px;border-bottom:1px solid #f3f4f6;">%d</td>
                  <td align="right" style="padding:10px 8px;border-bottom:1px solid #f3f4f6;font-weight:600;">%s</td>
                </tr>
                """.formatted(name, qty, formatInr(lineTotal)));
        }
        return sb.toString();
    }

    private static String buildItemMeta(OrderItemDTO item) {
        StringBuilder meta = new StringBuilder();
        if (item.getSize() != null && !item.getSize().isBlank()) {
            meta.append("Size: ").append(item.getSize());
        }
        if (item.getColor() != null && !item.getColor().isBlank()) {
            if (!meta.isEmpty()) meta.append(" · ");
            meta.append("Color: ").append(item.getColor());
        }
        return meta.toString();
    }

    private static String buildBreakdownRows(OrderConfirmationEmailModel model) {
        StringBuilder rows = new StringBuilder();
        rows.append("<div style=\"margin-top:14px;font-size:13px;font-weight:700;color:#111827;\">Amount Summary</div>");
        rows.append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"margin-top:8px;font-size:13px;\">");

        appendRow(rows, "Offer price (subtotal)", formatInr(model.subtotal()), false, false);
        if (model.discount() > 0.009) {
            appendRow(rows, "Discount", "-" + formatInr(model.discount()), true, false);
        }
        if (model.shipping() > 0.009) {
            appendRow(rows, "Delivery", formatInr(model.shipping()), false, false);
        } else {
            appendRow(rows, "Delivery", "FREE", true, false);
        }
        appendRow(rows, "Order total", formatInr(model.orderGrandTotal()), false, true);
        if (model.walletUsed() > 0.009) {
            appendRow(rows, "FNT Wallet", "-" + formatInr(model.walletUsed()), true, false);
        }
        appendRow(rows, "Payable amount", formatInr(model.payable()), false, true);

        rows.append("</table>");
        return rows.toString();
    }

    private static void appendRow(
            StringBuilder rows,
            String label,
            String value,
            boolean positive,
            boolean bold
    ) {
        String color = positive ? "color:#16a34a;" : "";
        String weight = bold ? "font-weight:800;" : "";
        rows.append("""
            <tr>
              <td style="padding:5px 0;color:#6b7280;%s">%s</td>
              <td align="right" style="padding:5px 0;%s%s">%s</td>
            </tr>
            """.formatted(weight, escape(label), color, weight, escape(value)));
    }

    private static String buildPayableHighlight(OrderConfirmationEmailModel model) {
        return """
            <div style="margin-top:14px;padding:12px;background:#fff7ed;border-radius:8px;text-align:center;">
              <div style="font-size:12px;color:#9a3412;">Amount Paid / Due</div>
              <div style="font-size:22px;font-weight:800;color:%s;margin-top:4px;">%s</div>
            </div>
            """.formatted(BRAND_ORANGE, formatInr(model.payable()));
    }

    private static String buildNeedHelpSection() {
        return """
            <tr>
              <td style="padding:0 24px 24px;">
                <div style="background:#fffbeb;border-radius:10px;padding:16px 18px;border:1px solid #fde68a;">
                  <table role="presentation" cellspacing="0" cellpadding="0" style="margin-bottom:8px;">
                    <tr>
                      <td style="vertical-align:middle;padding-right:8px;font-size:16px;line-height:1;color:#6b7280;">&#128222;</td>
                      <td style="font-size:15px;font-weight:800;color:#111827;vertical-align:middle;">Need Help?</td>
                    </tr>
                  </table>
                  <div style="font-size:13px;color:#374151;line-height:1.6;margin-bottom:10px;">
                    Our support team is here to assist you:
                  </div>
                  <div style="font-size:13px;color:#374151;line-height:1.8;">
                    <strong>Email:</strong>
                    <a href="mailto:%s" style="color:#2563eb;text-decoration:underline;">%s</a><br/>
                    <strong>Phone:</strong>
                    <a href="tel:+919063499092" style="color:#374151;text-decoration:none;">%s</a>
                  </div>
                </div>
              </td>
            </tr>
            """.formatted(SUPPORT_EMAIL, SUPPORT_EMAIL, SUPPORT_PHONE);
    }

    private static String formatInr(double amount) {
        BigDecimal value = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
        return "₹" + String.format(Locale.ENGLISH, "%,.2f", value);
    }

    private static String safe(String value) {
        return value != null ? value.trim() : "";
    }

    private static String escape(String value) {
        return safe(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
