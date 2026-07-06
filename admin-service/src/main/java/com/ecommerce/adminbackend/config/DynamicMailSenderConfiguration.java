package com.ecommerce.adminbackend.config;

import com.ecommerce.adminbackend.service.PlatformIntegrationSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties(MailProperties.class)
@RequiredArgsConstructor
public class DynamicMailSenderConfiguration {

    private final MailProperties mailProperties;
    private final PlatformIntegrationSettings integrationSettings;

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl impl = new JavaMailSenderImpl();
        impl.setHost(mailProperties.getHost());
        if (mailProperties.getPort() != null) {
            impl.setPort(mailProperties.getPort());
        }
        impl.setUsername(mailProperties.getUsername());
        impl.setPassword(integrationSettings.getSendGridApiKey());
        impl.setDefaultEncoding(
                mailProperties.getDefaultEncoding() != null
                        ? mailProperties.getDefaultEncoding().name()
                        : Charset.defaultCharset().name());
        if (mailProperties.getProtocol() != null) {
            impl.setProtocol(mailProperties.getProtocol());
        }
        Properties props = new Properties();
        Map<String, String> configured = mailProperties.getProperties();
        if (configured != null) {
            props.putAll(configured);
        }
        impl.setJavaMailProperties(props);
        return new RefreshingJavaMailSender(impl, integrationSettings);
    }
}
