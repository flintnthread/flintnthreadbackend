package com.ecommerce.adminbackend.util;

public final class OrderStatusUpdateEmailBuilder {

    private OrderStatusUpdateEmailBuilder() {
    }

    public static String buildSubject(String orderNumber) {
        String label = orderNumber == null || orderNumber.isBlank() ? "your order" : orderNumber;
        return "Order update — " + label;
    }

    public static String buildHtml(
            String customerName,
            String orderNumber,
            String statusLabel,
            String comment,
            String supportEmail,
            String supportPhone) {
        String name = customerName == null || customerName.isBlank() ? "Customer" : customerName;
        String order = orderNumber == null || orderNumber.isBlank() ? "—" : orderNumber;
        String status = statusLabel == null || statusLabel.isBlank() ? "Updated" : statusLabel;
        String note = comment == null || comment.isBlank()
                ? "No additional comments were provided."
                : comment.trim();
        int year = java.time.Year.now().getValue();

        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family:Arial,sans-serif;background:#f3f4f6;padding:24px;margin:0;">
                  <div style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb;">
                    <div style="background:linear-gradient(135deg,#F97316 0%%,#1E3A6E 100%%);padding:28px 32px;text-align:center;">
                      <h1 style="color:#ffffff;margin:0;font-size:22px;">Order Status Updated</h1>
                    </div>
                    <div style="padding:32px;">
                      <p style="color:#111827;font-size:18px;font-weight:bold;margin:0 0 16px;">Hello %s,</p>
                      <p style="color:#374151;line-height:1.7;margin:0 0 16px;font-size:15px;">
                        Your order <strong>%s</strong> has been updated.
                      </p>
                      <div style="background:#fff7ed;border:1px solid #fed7aa;border-radius:10px;padding:16px;margin:0 0 16px;">
                        <p style="margin:0 0 8px;color:#9a3412;font-size:12px;font-weight:700;letter-spacing:0.4px;">NEW STATUS</p>
                        <p style="margin:0;color:#1f2937;font-size:18px;font-weight:800;">%s</p>
                      </div>
                      <p style="margin:0 0 8px;color:#111827;font-size:14px;font-weight:700;">Update note</p>
                      <p style="margin:0 0 20px;color:#374151;font-size:14px;line-height:1.6;">%s</p>
                      <p style="color:#6b7280;font-size:13px;line-height:1.6;margin:0;">
                        If you have questions, contact us at
                        <a href="mailto:%s" style="color:#2563eb;text-decoration:none;">%s</a>
                        or %s.
                      </p>
                      <p style="color:#9ca3af;font-size:12px;margin-top:24px;margin-bottom:0;text-align:center;">
                        &copy; %d Flint &amp; Thread. All rights reserved.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(name, order, status, esc(note), supportEmail, supportEmail, supportPhone, year);
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
