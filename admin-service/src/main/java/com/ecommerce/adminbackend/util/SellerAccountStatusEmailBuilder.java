package com.ecommerce.adminbackend.util;

public final class SellerAccountStatusEmailBuilder {

    private SellerAccountStatusEmailBuilder() {
    }

    public static String buildSubject(String statusLabel) {
        return "Flint & Thread Seller Account Update — " + statusLabel;
    }

    public static String buildHtml(
            String sellerName,
            String statusLabel,
            String reason,
            String supportEmail,
            String supportPhone) {
        String safeName = escape(sellerName != null && !sellerName.isBlank() ? sellerName.trim() : "Seller");
        String safeStatus = escape(statusLabel);
        String safeReason = reason != null && !reason.isBlank()
                ? "<p style=\"margin:16px 0 0;color:#374151;font-size:15px;line-height:1.6;\"><strong>Reason:</strong> "
                + escape(reason.trim()) + "</p>"
                : "";
        String safeSupportEmail = escape(supportEmail);
        String safeSupportPhone = escape(supportPhone);

        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f8fafc;font-family:Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f8fafc;padding:24px 0;">
                    <tr>
                      <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb;">
                          <tr>
                            <td style="background:linear-gradient(135deg,#1D324E 0%%,#0f172a 100%%);padding:28px 32px;">
                              <h1 style="margin:0;color:#ffffff;font-size:22px;">Seller Account Update</h1>
                              <p style="margin:8px 0 0;color:#cbd5e1;font-size:14px;">Flint &amp; Thread Admin Notification</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <p style="margin:0 0 12px;color:#111827;font-size:16px;">Hello %s,</p>
                              <p style="margin:0;color:#374151;font-size:15px;line-height:1.6;">
                                Your seller account status has been updated to <strong>%s</strong>.
                              </p>
                              %s
                              <p style="margin:24px 0 0;color:#6b7280;font-size:14px;line-height:1.6;">
                                If you have questions, contact us at
                                <a href="mailto:%s" style="color:#EA580C;">%s</a>
                                or call %s.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(safeName, safeStatus, safeReason, safeSupportEmail, safeSupportEmail, safeSupportPhone);
    }

    private static String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
