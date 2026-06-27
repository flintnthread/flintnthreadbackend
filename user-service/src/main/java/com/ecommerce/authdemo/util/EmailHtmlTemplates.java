package com.ecommerce.authdemo.util;

public final class EmailHtmlTemplates {

    private static final String WEBSITE = "https://flintnthread.in";
    private static final String SUPPORT_EMAIL = "support@flintnthread.in";
    private static final int CURRENT_YEAR = 2026;

    private EmailHtmlTemplates() {
    }

    public static String buildOtpEmailHtml(String otp) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>FlintnThread Login OTP</title>
                  <style type="text/css">
                    .otp-digit {
                      font-size: 24px !important;
                      font-weight: 700 !important;
                      color: #111827 !important;
                      font-family: Arial, Helvetica, sans-serif !important;
                      line-height: 28px !important;
                      text-align: center !important;
                    }
                    @media only screen and (max-width: 480px) {
                      .otp-wrap { padding-left: 2px !important; padding-right: 2px !important; }
                      .otp-digit { font-size: 22px !important; line-height: 26px !important; }
                      .otp-cell { width: 34px !important; min-width: 34px !important; max-width: 34px !important; }
                      .otp-gap { width: 4px !important; min-width: 4px !important; max-width: 4px !important; }
                    }
                  </style>
                </head>
                <body style="margin:0;padding:0;background:#eef1f6;font-family:Arial,Helvetica,sans-serif;color:#333;">
                  <div style="display:none;max-height:0;overflow:hidden;mso-hide:all;font-size:1px;line-height:1px;color:#eef1f6;">Your FlintnThread login code is %s. Valid for 5 minutes.</div>
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#eef1f6;padding:24px 12px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%%;background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 8px 24px rgba(0,0,0,0.08);">

                          <!-- Header -->
                          <tr>
                            <td style="background:#1a2f4d;padding:22px 28px;">
                              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                                <tr>
                                  <td style="vertical-align:middle;">
                                    <table role="presentation" cellpadding="0" cellspacing="0">
                                      <tr>
                                        <td style="background:linear-gradient(135deg,#d4a843,#f0d78c);color:#1a2f4d;font-weight:700;font-size:18px;width:42px;height:42px;border-radius:8px;text-align:center;line-height:42px;">FT</td>
                                        <td style="padding-left:12px;">
                                          <div style="color:#ffffff;font-size:22px;font-weight:700;line-height:1.2;">FlintnThread</div>
                                          <div style="color:#c8d4e3;font-size:12px;margin-top:2px;">— Wear Your Style —</div>
                                        </td>
                                      </tr>
                                    </table>
                                  </td>
                                  <td align="right" style="font-size:28px;line-height:1;">🛍️</td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- OTP Body -->
                          <tr>
                            <td style="padding:32px 12px 20px;background:#f7f8fb;">
                              <p style="margin:0 0 16px;font-size:18px;font-weight:700;color:#111827;">Hello,</p>
                              <p style="margin:0 0 24px;font-size:15px;line-height:1.7;color:#4b5563;">
                                We received a request to sign in to your FlintnThread account. Use the OTP below to complete your login.
                              </p>

                              <p style="margin:0 0 10px;font-size:12px;font-weight:700;letter-spacing:1px;color:#6b7280;text-align:center;">YOUR OTP</p>
                              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                                <tr>
                                  <td align="center" class="otp-wrap" style="background:#fff8e6;border:2px dashed #f5b942;border-radius:12px;padding:16px 4px;">
                                    %s
                                  </td>
                                </tr>
                              </table>

                              <p style="margin:18px 0 24px;font-size:14px;color:#6b7280;text-align:center;">
                                ⏱ This OTP is valid for <strong style="color:#d97706;">5 minutes.</strong>
                              </p>

                              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#e8f0fe;border-radius:10px;">
                                <tr>
                                  <td style="padding:16px 18px;">
                                    <table role="presentation" cellpadding="0" cellspacing="0">
                                      <tr>
                                        <td style="font-size:24px;padding-right:12px;vertical-align:top;">🛡️</td>
                                        <td style="font-size:14px;line-height:1.6;color:#374151;">
                                          For security reasons, do not share this OTP with anyone. FlintnThread will never ask for your OTP via email, phone, or message.
                                        </td>
                                      </tr>
                                    </table>
                                  </td>
                                </tr>
                              </table>

                              <p style="margin:20px 0 0;font-size:14px;line-height:1.6;color:#6b7280;">
                                If you did not request this login, please ignore this email.
                              </p>
                            </td>
                          </tr>

                          <!-- Welcome hero (images 1-4 reference) -->
                          <tr>
                            <td style="padding:0 28px 24px;background:#f7f8fb;">
                              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="border-radius:16px 16px 0 0;overflow:hidden;">
                                <tr>
                                  <td align="center" style="background:linear-gradient(180deg,#f58220 0%%,#ffb366 100%%);padding:32px 20px;">
                                    <div style="font-size:42px;line-height:1;margin-bottom:12px;">🎉</div>
                                    <div style="color:#ffffff;font-size:28px;font-weight:700;margin-bottom:8px;">Welcome to F&amp;T!</div>
                                    <div style="color:#fff4e8;font-size:15px;">Your amazing journey starts here</div>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Quick start + links -->
                          <tr>
                            <td style="padding:0 28px 28px;background:#ffffff;">
                              <div style="height:4px;background:#f58220;border-radius:2px;margin-bottom:22px;"></div>
                              <p style="margin:0 0 14px;font-size:16px;line-height:1.7;color:#4b5563;">
                                We're <strong>thrilled</strong> to have you with us. Explore fashion, discover deals, and enjoy a seamless shopping experience.
                              </p>

                              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#fff8f0;border-left:4px solid #f58220;border-radius:8px;margin:20px 0;">
                                <tr>
                                  <td style="padding:18px 20px;">
                                    <div style="font-size:17px;font-weight:700;color:#1a2f4d;margin-bottom:14px;">🚀 Quick Start Guide</div>
                                    %s
                                  </td>
                                </tr>
                              </table>

                              %s

                              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="border:2px dashed #f58220;border-radius:12px;background:#fffaf5;margin:22px 0;">
                                <tr>
                                  <td style="padding:20px;">
                                    <div style="text-align:center;font-size:16px;font-weight:700;color:#1a2f4d;margin-bottom:14px;">📌 Important Links</div>
                                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                                      <tr>
                                        <td width="50%%" style="padding:8px 6px;font-size:14px;"><a href="%s/forgot-password" style="color:#f58220;text-decoration:none;">🔑 Forgot Password?</a></td>
                                        <td width="50%%" style="padding:8px 6px;font-size:14px;"><a href="%s/account" style="color:#f58220;text-decoration:none;">⚙️ Account Settings</a></td>
                                      </tr>
                                      <tr>
                                        <td width="50%%" style="padding:8px 6px;font-size:14px;"><a href="mailto:%s" style="color:#f58220;text-decoration:none;">💬 Contact Support</a></td>
                                        <td width="50%%" style="padding:8px 6px;font-size:14px;"><a href="%s/faq" style="color:#f58220;text-decoration:none;">❓ FAQs</a></td>
                                      </tr>
                                    </table>
                                  </td>
                                </tr>
                              </table>

                              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f3f4f6;border-radius:10px;border-left:5px solid #6b7280;">
                                <tr>
                                  <td style="padding:16px 18px;">
                                    <table role="presentation" cellpadding="0" cellspacing="0">
                                      <tr>
                                        <td style="font-size:24px;padding-right:12px;">🛡️</td>
                                        <td>
                                          <div style="font-size:15px;font-weight:700;color:#1a2f4d;margin-bottom:4px;">Security Reminder</div>
                                          <div style="font-size:13px;line-height:1.6;color:#6b7280;">Never share your password with anyone. We'll never ask for your password via email or phone.</div>
                                        </td>
                                      </tr>
                                    </table>
                                  </td>
                                </tr>
                              </table>

                              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin:24px 0 8px;">
                                <tr>
                                  <td style="vertical-align:top;">
                                    <p style="margin:0 0 8px;font-size:15px;color:#111827;">Thanks, <strong>FlintnThread Team</strong></p>
                                    <p style="margin:0;font-size:14px;">
                                      <a href="%s" style="color:#2563eb;text-decoration:none;">🌐 %s</a>
                                    </p>
                                  </td>
                                  <td align="right" style="vertical-align:top;">
                                    %s
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Footer -->
                          <tr>
                            <td style="background:#1a2f4d;padding:24px 28px;text-align:center;">
                              <div style="color:#ffffff;font-size:16px;font-weight:700;margin-bottom:8px;">Thank you for choosing Flint &amp; Thread!</div>
                              <div style="color:#c8d4e3;font-size:13px;line-height:1.6;">© %d Flint &amp; Thread. All rights reserved.<br>Your trusted platform for amazing experiences.</div>
                              <div style="margin-top:14px;font-size:13px;">
                                <a href="%s/terms" style="color:#f58220;text-decoration:none;">Terms</a>
                                <span style="color:#6b8aad;">&nbsp;|&nbsp;</span>
                                <a href="%s/privacy" style="color:#f58220;text-decoration:none;">Privacy Policy</a>
                                <span style="color:#6b8aad;">&nbsp;|&nbsp;</span>
                                <a href="mailto:%s" style="color:#f58220;text-decoration:none;">Contact Us</a>
                              </div>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                escapeHtml(otp.replaceAll("\\D", "")),
                buildOtpDigitsRowHtml(otp),
                buildQuickStartSteps(true),
                buildCtaButtonsHtml(),
                WEBSITE, WEBSITE, SUPPORT_EMAIL, WEBSITE,
                WEBSITE, WEBSITE,
                buildSocialIconsHtml(),
                CURRENT_YEAR,
                WEBSITE, WEBSITE, SUPPORT_EMAIL
        );
    }

    public static String buildWelcomeEmailHtml(String displayName, String username, String referralCode) {
        String safeName = escapeHtml(displayName);
        String safeUsername = escapeHtml(username);
        String safeReferral = escapeHtml(referralCode != null ? referralCode : "—");

        String referralSection = referralCode != null && !referralCode.isBlank() ? """
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin:24px 0;border-radius:14px;overflow:hidden;">
                  <tr>
                    <td align="center" style="background:linear-gradient(90deg,#10b981 0%%,#14b8a6 100%%);padding:28px 20px;">
                      <div style="color:#ffffff;font-size:16px;font-weight:700;margin-bottom:14px;">🎁 Your Referral Code</div>
                      <table role="presentation" cellpadding="0" cellspacing="0">
                        <tr>
                          <td style="background:rgba(255,255,255,0.22);border-radius:10px;padding:14px 28px;">
                            <div style="font-size:24px;font-weight:700;color:#ffffff;letter-spacing:2px;font-family:'Courier New',Courier,monospace;">%s</div>
                          </td>
                        </tr>
                      </table>
                      <div style="color:#eafff8;font-size:13px;margin-top:14px;line-height:1.5;">Share this code with friends and earn rewards when they sign up!</div>
                    </td>
                  </tr>
                </table>
                """.formatted(safeReferral) : "";

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Welcome to Flint &amp; Thread</title>
                </head>
                <body style="margin:0;padding:0;background:#f0f4f8;font-family:Arial,Helvetica,sans-serif;color:#333;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f0f4f8;padding:24px 12px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%%;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 10px 30px rgba(0,0,0,0.08);">

                          <!-- Hero -->
                          <tr>
                            <td align="center" style="background:linear-gradient(180deg,#f58220 0%%,#ffb366 100%%);padding:40px 24px;">
                              <div style="font-size:48px;line-height:1;margin-bottom:14px;">🎉</div>
                              <div style="color:#ffffff;font-size:30px;font-weight:700;margin-bottom:8px;">Welcome to F&amp;T!</div>
                              <div style="color:#fff4e8;font-size:16px;">Your amazing journey starts here</div>
                            </td>
                          </tr>

                          <!-- Greeting -->
                          <tr>
                            <td style="padding:28px 28px 8px;">
                              <div style="height:4px;background:#f58220;border-radius:2px;margin-bottom:20px;"></div>
                              <p style="margin:0 0 16px;font-size:22px;font-weight:700;color:#1a2f4d;">Hi %s! 👋</p>
                              <p style="margin:0 0 14px;font-size:15px;line-height:1.7;color:#4b5563;">
                                We're <strong>thrilled</strong> to have you join our community! Your account has been successfully created, and you're now part of something special.
                              </p>
                              <p style="margin:0 0 8px;font-size:15px;color:#4b5563;">
                                Your username is: <strong style="color:#f58220;">%s</strong>
                              </p>
                            </td>
                          </tr>

                          <!-- Quick start -->
                          <tr>
                            <td style="padding:8px 28px 8px;">
                              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#fff8f0;border-left:4px solid #f58220;border-radius:8px;">
                                <tr>
                                  <td style="padding:20px 22px;">
                                    <div style="font-size:18px;font-weight:700;color:#1a2f4d;margin-bottom:16px;">🚀 Quick Start Guide</div>
                                    %s
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Referral + CTA -->
                          <tr>
                            <td style="padding:8px 28px 24px;">
                              %s
                              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                                <tr>
                                  <td align="center" style="padding:8px 0 12px;">
                                    <a href="%s" style="display:inline-block;background:#1a2f4d;color:#ffffff;text-decoration:none;font-size:16px;font-weight:700;padding:14px 36px;border-radius:50px;">🚀 Get Started Now</a>
                                  </td>
                                </tr>
                                <tr>
                                  <td align="center" style="padding-top:4px;">
                                    <a href="%s" style="color:#f58220;font-size:15px;font-weight:600;text-decoration:none;">Visit Our Homepage →</a>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Important links -->
                          <tr>
                            <td style="padding:0 28px 24px;">
                              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="border:2px dashed #f58220;border-radius:12px;background:#fffaf5;">
                                <tr>
                                  <td style="padding:20px;">
                                    <div style="text-align:center;font-size:16px;font-weight:700;color:#1a2f4d;margin-bottom:14px;">📌 Important Links</div>
                                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                                      <tr>
                                        <td width="50%%" style="padding:8px 6px;font-size:14px;"><a href="%s/forgot-password" style="color:#f58220;text-decoration:none;">🔑 Forgot Password?</a></td>
                                        <td width="50%%" style="padding:8px 6px;font-size:14px;"><a href="%s/account" style="color:#f58220;text-decoration:none;">⚙️ Account Settings</a></td>
                                      </tr>
                                      <tr>
                                        <td width="50%%" style="padding:8px 6px;font-size:14px;"><a href="mailto:%s" style="color:#f58220;text-decoration:none;">💬 Contact Support</a></td>
                                        <td width="50%%" style="padding:8px 6px;font-size:14px;"><a href="%s/faq" style="color:#f58220;text-decoration:none;">❓ FAQs</a></td>
                                      </tr>
                                    </table>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Security -->
                          <tr>
                            <td style="padding:0 28px 28px;">
                              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f3f4f6;border-radius:10px;border-left:5px solid #6b7280;">
                                <tr>
                                  <td style="padding:16px 18px;">
                                    <table role="presentation" cellpadding="0" cellspacing="0">
                                      <tr>
                                        <td style="font-size:24px;padding-right:12px;">🛡️</td>
                                        <td>
                                          <div style="font-size:15px;font-weight:700;color:#1a2f4d;margin-bottom:4px;">Security Reminder</div>
                                          <div style="font-size:13px;line-height:1.6;color:#6b7280;">Never share your password with anyone. We'll never ask for your password via email or phone.</div>
                                        </td>
                                      </tr>
                                    </table>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Footer -->
                          <tr>
                            <td style="background:#1a2f4d;padding:26px 28px;text-align:center;">
                              <div style="color:#ffffff;font-size:17px;font-weight:700;margin-bottom:8px;">Thank you for choosing Flint &amp; Thread!</div>
                              <div style="color:#c8d4e3;font-size:13px;line-height:1.6;">© %d Flint &amp; Thread. All rights reserved.<br>Your trusted platform for amazing experiences.</div>
                              <div style="margin-top:14px;font-size:13px;">
                                <a href="%s/terms" style="color:#f58220;text-decoration:none;">Terms</a>
                                <span style="color:#6b8aad;">&nbsp;|&nbsp;</span>
                                <a href="%s/privacy" style="color:#f58220;text-decoration:none;">Privacy Policy</a>
                                <span style="color:#6b8aad;">&nbsp;|&nbsp;</span>
                                <a href="mailto:%s" style="color:#f58220;text-decoration:none;">Contact Us</a>
                              </div>
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
                safeUsername,
                buildQuickStartSteps(true),
                referralSection,
                WEBSITE,
                WEBSITE,
                WEBSITE, WEBSITE, SUPPORT_EMAIL, WEBSITE,
                CURRENT_YEAR,
                WEBSITE, WEBSITE, SUPPORT_EMAIL
        );
    }

    public static String buildOtpPlainText(String otp) {
        String digits = otp.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            digits = otp;
        }
        return """
                Hello,

                Your FlintnThread login code: %s

                This code expires in 5 minutes. Do not share it with anyone.

                Website: %s

                Thanks,
                FlintnThread Team
                """.formatted(digits, WEBSITE);
    }

    public static String buildWelcomePlainText(String displayName, String username, String referralCode) {
        String referralLine = referralCode != null && !referralCode.isBlank()
                ? "\nYour referral code: " + referralCode + "\n"
                : "\n";
        return """
                Hi %s!

                Welcome to Flint & Thread! Your account has been successfully created.

                Your username is: %s
                %s
                Get started: %s

                Thanks,
                FlintnThread Team
                """.formatted(displayName, username, referralLine, WEBSITE);
    }

    private static String buildOtpDigitsRowHtml(String otp) {
        String digits = otp.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            digits = escapeHtml(otp);
        } else {
            digits = escapeHtml(digits);
        }

        // Single text node — Gmail mobile notification shows digits on one line (not one per table cell).
        return """
                <table role="presentation" cellpadding="0" cellspacing="0" border="0" align="center" style="margin:0 auto;">
                  <tr>
                    <td align="center" valign="middle" style="padding:4px 12px;white-space:nowrap;word-break:keep-all;mso-line-height-rule:exactly;">
                      <span style="display:inline-block;font-size:34px;font-weight:700;letter-spacing:14px;color:#111827;font-family:'Courier New',Courier,monospace;line-height:1.2;white-space:nowrap;word-break:keep-all;">%s</span>
                    </td>
                  </tr>
                </table>
                """.formatted(digits);
    }

    private static String buildCtaButtonsHtml() {
        return """
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin:8px 0 22px;">
                  <tr>
                    <td align="center" style="padding:8px 0 12px;">
                      <a href="%s" style="display:inline-block;background:#1a2f4d;color:#ffffff;text-decoration:none;font-size:16px;font-weight:700;padding:14px 36px;border-radius:50px;">🚀 Get Started Now</a>
                    </td>
                  </tr>
                  <tr>
                    <td align="center" style="padding-top:4px;">
                      <a href="%s" style="color:#f58220;font-size:15px;font-weight:600;text-decoration:none;">Visit Our Homepage →</a>
                    </td>
                  </tr>
                </table>
                """.formatted(WEBSITE, WEBSITE);
    }

    private static String buildSocialIconsHtml() {
        return """
                <table role="presentation" cellpadding="0" cellspacing="0">
                  <tr>
                    <td style="padding-left:6px;">
                      <a href="https://instagram.com/flintnthread" style="display:inline-block;width:34px;height:34px;background:#1a2f4d;border-radius:50%%;text-align:center;line-height:34px;color:#d4a843;font-size:14px;font-weight:700;text-decoration:none;">in</a>
                    </td>
                    <td style="padding-left:6px;">
                      <a href="https://facebook.com/flintnthread" style="display:inline-block;width:34px;height:34px;background:#1a2f4d;border-radius:50%%;text-align:center;line-height:34px;color:#d4a843;font-size:14px;font-weight:700;text-decoration:none;">f</a>
                    </td>
                    <td style="padding-left:6px;">
                      <a href="https://x.com/flintnthread" style="display:inline-block;width:34px;height:34px;background:#1a2f4d;border-radius:50%%;text-align:center;line-height:34px;color:#d4a843;font-size:14px;font-weight:700;text-decoration:none;">x</a>
                    </td>
                  </tr>
                </table>
                """;
    }

    private static String buildQuickStartSteps(boolean includeStep3) {
        String step3 = includeStep3 ? """
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin-top:14px;">
                  <tr>
                    <td width="36" valign="top">
                      <div style="width:28px;height:28px;background:#f58220;color:#ffffff;border-radius:50%%;text-align:center;line-height:28px;font-size:13px;font-weight:700;">3</div>
                    </td>
                    <td style="padding-left:10px;">
                      <div style="font-size:15px;font-weight:700;color:#1a2f4d;">Start Your Journey</div>
                      <div style="font-size:13px;color:#6b7280;margin-top:4px;">Begin using our platform to achieve your goals</div>
                    </td>
                  </tr>
                </table>
                """ : "";

        return """
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                  <tr>
                    <td width="36" valign="top">
                      <div style="width:28px;height:28px;background:#f58220;color:#ffffff;border-radius:50%%;text-align:center;line-height:28px;font-size:13px;font-weight:700;">1</div>
                    </td>
                    <td style="padding-left:10px;">
                      <div style="font-size:15px;font-weight:700;color:#1a2f4d;">Complete Your Profile</div>
                      <div style="font-size:13px;color:#6b7280;margin-top:4px;">Add your details and preferences to personalize your experience</div>
                    </td>
                  </tr>
                </table>
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin-top:14px;">
                  <tr>
                    <td width="36" valign="top">
                      <div style="width:28px;height:28px;background:#f58220;color:#ffffff;border-radius:50%%;text-align:center;line-height:28px;font-size:13px;font-weight:700;">2</div>
                    </td>
                    <td style="padding-left:10px;">
                      <div style="font-size:15px;font-weight:700;color:#1a2f4d;">Explore Our Features</div>
                      <div style="font-size:13px;color:#6b7280;margin-top:4px;">Discover all the amazing tools and services we offer</div>
                    </td>
                  </tr>
                </table>
                %s
                """.formatted(step3);
    }

    private static String escapeHtml(String value) {
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
