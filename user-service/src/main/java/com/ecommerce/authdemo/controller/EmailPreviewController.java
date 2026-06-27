package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.exception.OrderException;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.service.EmailService;
import com.ecommerce.authdemo.service.InvoiceService;
import com.ecommerce.authdemo.util.EmailHtmlTemplates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/email/preview", "/api/email/preview"})
public class EmailPreviewController {

    public static final String EMAIL_TEMPLATE_VERSION = "html-otp-v4-single-span";

    @Autowired
    private EmailService emailService;

    @Autowired
    private InvoiceService invoiceService;

    @GetMapping("/version")
    public Map<String, String> templateVersion() {
        return Map.of(
                "emailTemplateVersion", EMAIL_TEMPLATE_VERSION,
                "description", "Styled HTML OTP and welcome emails"
        );
    }

    @GetMapping(value = "/otp", produces = MediaType.TEXT_HTML_VALUE)
    public String previewOtpEmail(@RequestParam(defaultValue = "123456") String otp) {
        return EmailHtmlTemplates.buildOtpEmailHtml(otp);
    }

    @GetMapping(value = "/welcome", produces = MediaType.TEXT_HTML_VALUE)
    public String previewWelcomeEmail(
            @RequestParam(defaultValue = "Sandhya Gudisa") String name,
            @RequestParam(defaultValue = "Sandhya") String username,
            @RequestParam(defaultValue = "REFSAND001426") String referralCode
    ) {
        return EmailHtmlTemplates.buildWelcomeEmailHtml(name, username, referralCode);
    }

    /** Public invoice page for QR scans — uses already-public `/api/email/preview/**` path. */
    @GetMapping(value = "/invoice-html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> previewInvoiceHtml(
            @RequestParam(name = "order_id", required = false) Integer orderIdSnake,
            @RequestParam(name = "orderId", required = false) Integer orderIdCamel,
            @RequestParam(name = "seller_id", required = false) Long sellerIdSnake,
            @RequestParam(name = "sellerId", required = false) Long sellerIdCamel,
            @RequestParam(name = "product_id", required = false) Long productIdSnake,
            @RequestParam(name = "productId", required = false) Long productIdCamel,
            @RequestParam(name = "line_index", required = false) Integer lineIndexSnake,
            @RequestParam(name = "lineIndex", required = false) Integer lineIndexCamel
    ) {
        Integer orderId = orderIdSnake != null ? orderIdSnake : orderIdCamel;
        Long sellerId = sellerIdSnake != null ? sellerIdSnake : sellerIdCamel;
        Long productId = productIdSnake != null ? productIdSnake : productIdCamel;
        Integer lineIndex = lineIndexSnake != null ? lineIndexSnake : lineIndexCamel;

        if (orderId == null || orderId <= 0) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_HTML)
                    .body("<html><body><p>Invalid order.</p></body></html>");
        }
        try {
            String html = invoiceService.getPublicViewHtml(orderId, sellerId, productId, lineIndex);
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404)
                    .contentType(MediaType.TEXT_HTML)
                    .body("<html><body><p>Invoice not found.</p></body></html>");
        } catch (OrderException e) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_HTML)
                    .body("<html><body><p>" + e.getMessage() + "</p></body></html>");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_HTML)
                    .body("<html><body><p>Could not load invoice.</p></body></html>");
        }
    }

    /** Sends a real HTML OTP email for testing — restart backend after code changes. */
    @PostMapping("/send-otp-test")
    public ResponseEntity<Map<String, String>> sendOtpTestEmail(
            @RequestParam String to,
            @RequestParam(defaultValue = "123456") String otp
    ) {
        emailService.sendOtpEmail(to, otp);
        return ResponseEntity.ok(Map.of(
                "status", "sent",
                "to", to,
                "message", "HTML OTP email sent. Check inbox (and spam). Restart backend if you still see plain text."
        ));
    }
}
