package com.ecommerce.authdemo.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

    @Configuration
    public class CloudinaryConfig {

        @Bean
        public Cloudinary cloudinary() {
            return new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", "dnce88bry",
                    "api_key", "692843357754938",
                    "api_secret", "hCqzms4k0L6LwAY4kxzXpyjLBR4",
                    "secure", true
            ));
        }
    }

