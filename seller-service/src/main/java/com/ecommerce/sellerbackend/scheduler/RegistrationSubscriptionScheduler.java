package com.ecommerce.sellerbackend.scheduler;

import com.ecommerce.sellerbackend.entity.Seller;
import com.ecommerce.sellerbackend.repository.SellerRegistrationPaymentRepository;
import com.ecommerce.sellerbackend.repository.SellerRepository;
import com.ecommerce.sellerbackend.service.MailService;
import com.ecommerce.sellerbackend.util.RegistrationReferenceNumberHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationSubscriptionScheduler {

    private final SellerRegistrationPaymentRepository registrationPaymentRepository;
    private final SellerRepository sellerRepository;
    private final MailService mailService;

    @Value("${app.registration.fee.inr:899}")
    private int registrationFeeInr;

    @Scheduled(cron = "${app.registration.renewal-reminder.cron:0 0 8 * * *}")
    @Transactional
    public void sendRenewalReminders() {
        registrationPaymentRepository.ensureTable();
        List<Long> sellerIds = registrationPaymentRepository.findSellerIdsDueForRenewalReminder();
        if (sellerIds.isEmpty()) {
            return;
        }
        log.info("Sending subscription renewal reminders to {} seller(s)", sellerIds.size());
        for (Long sellerId : sellerIds) {
            try {
                Seller seller = sellerRepository.findById(sellerId).orElse(null);
                if (seller == null || seller.getEmail() == null || seller.getEmail().isBlank()) {
                    registrationPaymentRepository.markRenewalReminderSent(sellerId);
                    continue;
                }
                SellerRegistrationPaymentRepository.PaymentRecord record =
                        registrationPaymentRepository.findBySellerId(sellerId);
                String expiryDate = record != null && record.getSubscriptionExpiresAt() != null
                        ? RegistrationReferenceNumberHelper.formatInvoiceDate(record.getSubscriptionExpiresAt())
                        : "today";
                String displayName = seller.getFullName();
                if (displayName == null || displayName.isBlank()) {
                    displayName = seller.getBusinessName();
                }
                mailService.sendSubscriptionRenewalReminderEmail(
                        seller.getEmail(),
                        displayName,
                        expiryDate,
                        registrationFeeInr
                );
                registrationPaymentRepository.markRenewalReminderSent(sellerId);
            } catch (Exception ex) {
                log.error("Failed renewal reminder for seller {}", sellerId, ex);
            }
        }
    }
}
