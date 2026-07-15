package com.ecommerce.adminbackend.util;

public final class ProductApprovedEmailBuilder {

    private ProductApprovedEmailBuilder() {
    }

    public static String buildSubject(String productName) {
        String name = productName != null && !productName.isBlank() ? productName.trim() : "Your product";
        return "Flint & Thread — Product Approved: " + name;
    }

    public static String buildHtml(
            String sellerName,
            String productName,
            String productSku,
            Long productId,
            String supportEmail,
            String supportPhone) {
        String safeName = escape(sellerName != null && !sellerName.isBlank() ? sellerName.trim() : "Seller");
        String safeProduct = escape(productName != null && !productName.isBlank() ? productName.trim() : "your product");
        String safeSku = productSku != null && !productSku.isBlank()
                ? "<p style=\"margin:12px 0 0;color:#374151;font-size:14px;\"><strong>SKU:</strong> "
                + escape(productSku.trim()) + "</p>"
                : "";
        String idLine = productId != null
                ? "<p style=\"margin:8px 0 0;color:#6b7280;font-size:13px;\">Product ID: "
                + productId + "</p>"
                : "";
        String safeSupportEmail = escape(supportEmail != null ? supportEmail : "support@flintnthread.in");
        String safeSupportPhone = escape(supportPhone != null ? supportPhone : "");

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
                              <h1 style="margin:0;color:#ffffff;font-size:22px;">Product Approved</h1>
                              <p style="margin:8px 0 0;color:#cbd5e1;font-size:14px;">Flint &amp; Thread Seller Notification</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <p style="margin:0 0 12px;color:#111827;font-size:16px;">Hello %s,</p>
                              <p style="margin:0;color:#374151;font-size:15px;line-height:1.6;">
                                Great news! Your product <strong>%s</strong> has been approved by our team
                                and is now live in the Flint &amp; Thread store.
                              </p>
                              %s
                              %s
                              <p style="margin:24px 0 0;color:#374151;font-size:15px;line-height:1.6;">
                                Customers can now discover and purchase this listing. Keep stock and pricing up to date.
                              </p>
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
                """.formatted(
                safeName,
                safeProduct,
                safeSku,
                idLine,
                safeSupportEmail,
                safeSupportEmail,
                safeSupportPhone);
    }

    private static String escape(String value) {
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
