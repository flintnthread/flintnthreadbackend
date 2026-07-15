package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.entity.EmailLog;
import com.ecommerce.adminbackend.entity.MarketplaceUser;
import com.ecommerce.adminbackend.entity.Seller;
import com.ecommerce.adminbackend.repository.EmailLogRepository;
import com.ecommerce.adminbackend.repository.MarketplaceUserRepository;
import com.ecommerce.adminbackend.repository.SellerRepository;
import com.ecommerce.adminbackend.service.AdminEmailBroadcastService;
import com.ecommerce.adminbackend.service.MailService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminEmailBroadcastServiceImpl extends BaseAdminService implements AdminEmailBroadcastService {

    private final MarketplaceUserRepository marketplaceUserRepository;
    private final SellerRepository sellerRepository;
    private final EmailLogRepository emailLogRepository;
    private final MailService mailService;

    @Override
    @Transactional
    public Map<String, Object> sendToCustomers(Map<String, Object> body) {
        String subject = requireNonBlank(stringAt(body, "subject"), "subject");
        String message = requireNonBlank(stringAt(body, "message"), "message");
        boolean sendAll = Boolean.TRUE.equals(asBoolean(body.get("sendAll")));

        List<Recipient> recipients = new ArrayList<>();
        if (sendAll) {
            for (MarketplaceUser user : marketplaceUserRepository.findAll()) {
                if (user.getEmail() != null && !user.getEmail().isBlank()) {
                    recipients.add(new Recipient(user.getId(), user.getEmail().trim()));
                }
            }
        } else {
            recipients.addAll(resolveCustomerRecipients(body));
        }

        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("No customer recipients resolved. Provide recipients, emails, or sendAll=true.");
        }

        int sent = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        String html = wrapHtml(message);

        for (Recipient recipient : recipients) {
            try {
                mailService.sendHtmlEmail(recipient.email(), subject, html);
                if (recipient.userId() != null) {
                    emailLogRepository.save(buildLog(recipient.userId(), recipient.email(), subject, "sent", null));
                }
                sent++;
            } catch (Exception ex) {
                failed++;
                errors.add(recipient.email() + ": " + ex.getMessage());
                if (recipient.userId() != null) {
                    emailLogRepository.save(buildLog(
                            recipient.userId(),
                            recipient.email(),
                            subject,
                            "failed",
                            ex.getMessage()));
                }
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("audience", "customers");
        response.put("requested", recipients.size());
        response.put("sent", sent);
        response.put("failed", failed);
        response.put("errors", errors);
        response.put("message", "Customer email broadcast completed.");
        return response;
    }

    @Override
    @Transactional
    public Map<String, Object> sendToSellers(Map<String, Object> body) {
        String subject = requireNonBlank(stringAt(body, "subject"), "subject");
        String message = requireNonBlank(stringAt(body, "message"), "message");
        boolean sendAll = Boolean.TRUE.equals(asBoolean(body.get("sendAll")));

        List<String> emails = new ArrayList<>();
        if (sendAll) {
            for (Seller seller : sellerRepository.findAll()) {
                if (seller.getEmail() != null && !seller.getEmail().isBlank()) {
                    emails.add(seller.getEmail().trim());
                }
            }
        } else {
            emails.addAll(resolveSellerEmails(body));
        }

        Set<String> unique = new LinkedHashSet<>();
        for (String email : emails) {
            if (email != null && !email.isBlank()) {
                unique.add(email.trim().toLowerCase(Locale.ROOT));
            }
        }
        if (unique.isEmpty()) {
            throw new IllegalArgumentException("No seller recipients resolved. Provide recipients, emails, or sendAll=true.");
        }

        int sent = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        String html = wrapHtml(message);

        // Marketing broadcast must NOT write seller_email_logs (email_type enum is restricted).
        for (String email : unique) {
            try {
                mailService.sendHtmlEmail(email, subject, html);
                sent++;
            } catch (Exception ex) {
                failed++;
                errors.add(email + ": " + ex.getMessage());
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("audience", "sellers");
        response.put("requested", unique.size());
        response.put("sent", sent);
        response.put("failed", failed);
        response.put("errors", errors);
        response.put("message", "Seller email broadcast completed (no seller_email_logs written for marketing).");
        return response;
    }

    private List<Recipient> resolveCustomerRecipients(Map<String, Object> body) {
        List<Recipient> recipients = new ArrayList<>();
        Object recipientIds = body.get("recipients");
        if (recipientIds instanceof Collection<?> collection) {
            for (Object rawId : collection) {
                if (rawId == null) {
                    continue;
                }
                Integer id;
                try {
                    id = Integer.valueOf(String.valueOf(rawId).trim());
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("recipients must contain numeric user ids.");
                }
                MarketplaceUser user = marketplaceUserRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
                if (user.getEmail() == null || user.getEmail().isBlank()) {
                    throw new IllegalArgumentException("Customer " + id + " has no email.");
                }
                recipients.add(new Recipient(user.getId(), user.getEmail().trim()));
            }
        }

        Object emails = body.get("emails");
        if (emails instanceof Collection<?> collection) {
            for (Object rawEmail : collection) {
                if (rawEmail == null || String.valueOf(rawEmail).isBlank()) {
                    continue;
                }
                String email = String.valueOf(rawEmail).trim();
                MarketplaceUser user = marketplaceUserRepository.findFirstByEmailIgnoreCase(email).orElse(null);
                recipients.add(new Recipient(user != null ? user.getId() : null, email));
            }
        }
        return recipients;
    }

    private List<String> resolveSellerEmails(Map<String, Object> body) {
        List<String> emails = new ArrayList<>();
        Object recipientIds = body.get("recipients");
        if (recipientIds instanceof Collection<?> collection) {
            for (Object rawId : collection) {
                if (rawId == null) {
                    continue;
                }
                Long id;
                try {
                    id = Long.valueOf(String.valueOf(rawId).trim());
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("recipients must contain numeric seller ids.");
                }
                Seller seller = sellerRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Seller not found: " + id));
                if (seller.getEmail() == null || seller.getEmail().isBlank()) {
                    throw new IllegalArgumentException("Seller " + id + " has no email.");
                }
                emails.add(seller.getEmail().trim());
            }
        }
        Object rawEmails = body.get("emails");
        if (rawEmails instanceof Collection<?> collection) {
            for (Object rawEmail : collection) {
                if (rawEmail != null && !String.valueOf(rawEmail).isBlank()) {
                    emails.add(String.valueOf(rawEmail).trim());
                }
            }
        }
        return emails;
    }

    private EmailLog buildLog(Integer userId, String recipient, String subject, String status, String error) {
        EmailLog logEntry = new EmailLog();
        logEntry.setUserId(userId);
        logEntry.setEmailType("notification");
        logEntry.setRecipient(recipient);
        logEntry.setSubject(subject);
        logEntry.setStatus(status);
        logEntry.setErrorMessage(error);
        logEntry.setSentAt(LocalDateTime.now());
        return logEntry;
    }

    private String wrapHtml(String message) {
        String escaped = message
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        String withBreaks = escaped.replace("\n", "<br/>");
        return """
                <div style="font-family:Arial,sans-serif;line-height:1.6;color:#111827;">
                  %s
                </div>
                """.formatted(withBreaks);
    }

    private Boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return false;
        }
        String text = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        return "true".equals(text) || "1".equals(text) || "yes".equals(text);
    }

    private record Recipient(Integer userId, String email) {
        Recipient {
            Objects.requireNonNull(email);
        }
    }
}
