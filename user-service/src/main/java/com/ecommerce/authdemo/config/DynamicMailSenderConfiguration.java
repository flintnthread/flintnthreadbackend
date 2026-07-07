package com.ecommerce.authdemo.config;

import com.ecommerce.authdemo.service.PlatformIntegrationSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
@EnableConfigurationProperties(MailProperties.class)
@RequiredArgsConstructor
public class DynamicMailSenderConfiguration {

    private final PlatformIntegrationSettings integrationSettings;

    @Value("${spring.mail.host:smtp.sendgrid.net}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:apikey}")
    private String username;

    @Value("${spring.mail.protocol:smtp}")
    private String protocol;

    @Value("${spring.mail.default-encoding:UTF-8}")
    private String defaultEncoding;

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl impl = new JavaMailSenderImpl();
        impl.setHost(host);
        impl.setPort(port);
        impl.setUsername(username);
        impl.setPassword(integrationSettings.getSendGridApiKey());
        impl.setDefaultEncoding(defaultEncoding);
        impl.setProtocol(protocol);
        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust", host);
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        impl.setJavaMailProperties(props);
        return new RefreshingJavaMailSender(impl, integrationSettings);
    }
}
