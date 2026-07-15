package com.ecommerce.sellerbackend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live Cloudinary credential/upload smoke (uses app default keys).
 */
class LiveCloudinaryUploadIT {

    @Test
    void uploadTinyPng_returnsSecureUrl() throws Exception {
        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", System.getenv().getOrDefault("CLOUDINARY_CLOUD_NAME", "dnce88bry"),
                "api_key", System.getenv().getOrDefault("CLOUDINARY_API_KEY", "692843357754938"),
                "api_secret", System.getenv().getOrDefault("CLOUDINARY_API_SECRET", "hCqzms4k0L6LwAY4kxzXpyjLBR4"),
                "secure", true
        ));

        byte[] png = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53,
                (byte) 0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41,
                0x54, 0x08, (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xCF,
                (byte) 0xC0, 0x00, 0x00, 0x00, 0x03, 0x00, 0x01,
                0x00, 0x05, (byte) 0xFE, (byte) 0xD4, (byte) 0xEF,
                0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
                (byte) 0xAE, 0x42, 0x60, (byte) 0x82
        };

        @SuppressWarnings("rawtypes")
        Map result = cloudinary.uploader().upload(png, ObjectUtils.asMap(
                "folder", "flintnthread/products",
                "resource_type", "image",
                "overwrite", false
        ));

        Object secureUrl = result.get("secure_url");
        assertNotNull(secureUrl, "Cloudinary did not return secure_url");
        String url = String.valueOf(secureUrl);
        System.out.println("LIVE_CLOUDINARY_URL=" + url);
        assertTrue(url.startsWith("https://res.cloudinary.com/"), "Expected Cloudinary HTTPS URL, got: " + url);
    }
}
