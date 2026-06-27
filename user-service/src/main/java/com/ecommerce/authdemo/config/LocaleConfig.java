package com.ecommerce.authdemo.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

@Configuration
public class LocaleConfig {

    @Bean
    public MessageSource messageSource(){

        ResourceBundleMessageSource source =
                new ResourceBundleMessageSource();

        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");   // VERY IMPORTANT
        source.setUseCodeAsDefaultMessage(true);

        return source;
    }
}


