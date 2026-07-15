package com.ecommerce.sellerbackend.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(
            @Value("${cloudinary.cloud-name:${CLOUDINARY_CLOUD_NAME:dnce88bry}}") String cloudName,
            @Value("${cloudinary.api-key:${CLOUDINARY_API_KEY:692843357754938}}") String apiKey,
            @Value("${cloudinary.api-secret:${CLOUDINARY_API_SECRET:hCqzms4k0L6LwAY4kxzXpyjLBR4}}") String apiSecret) {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }
}
