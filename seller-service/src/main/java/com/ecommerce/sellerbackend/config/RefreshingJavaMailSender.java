package com.ecommerce.sellerbackend.config;

import com.ecommerce.sellerbackend.service.PlatformIntegrationSettings;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessagePreparator;

@RequiredArgsConstructor
public class RefreshingJavaMailSender implements JavaMailSender {

    private final JavaMailSenderImpl delegate;
    private final PlatformIntegrationSettings integrationSettings;

    @Override
    public MimeMessage createMimeMessage() {
        refreshCredentials();
        return delegate.createMimeMessage();
    }

    @Override
    public MimeMessage createMimeMessage(java.io.InputStream contentStream) throws MailException {
        refreshCredentials();
        return delegate.createMimeMessage(contentStream);
    }

    @Override
    public void send(MimeMessage mimeMessage) throws MailException {
        refreshCredentials();
        delegate.send(mimeMessage);
    }

    @Override
    public void send(MimeMessage... mimeMessages) throws MailException {
        refreshCredentials();
        delegate.send(mimeMessages);
    }

    @Override
    public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
        refreshCredentials();
        delegate.send(mimeMessagePreparator);
    }

    @Override
    public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
        refreshCredentials();
        delegate.send(mimeMessagePreparators);
    }

    @Override
    public void send(SimpleMailMessage simpleMessage) throws MailException {
        refreshCredentials();
        delegate.send(simpleMessage);
    }

    @Override
    public void send(SimpleMailMessage... simpleMessages) throws MailException {
        refreshCredentials();
        delegate.send(simpleMessages);
    }

    private void refreshCredentials() {
        String apiKey = integrationSettings.getSendGridApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            delegate.setPassword(apiKey);
        }
    }
}
