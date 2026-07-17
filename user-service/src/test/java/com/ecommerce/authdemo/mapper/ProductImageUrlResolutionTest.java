package com.ecommerce.authdemo.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Lightweight mapper resolution checks (Cloudinary vs legacy uploads path).
 */
class ProductImageUrlResolutionTest {

    private ProductMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ProductMapper(null, null, null);
        ReflectionTestUtils.setField(mapper, "mediaPublicBaseUrl", "http://localhost:8083");
    }

    @Test
    void resolveImageUrl_cloudinaryKeptAsIs() {
        String url = "https://res.cloudinary.com/dnce88bry/image/upload/v1/flintnthread/products/x.jpg";
        assertEquals(url, mapper.resolveImageUrl(url));
    }

    @Test
    void resolveImageUrl_legacyRelativePathPrefixedWithMediaHost() {
        assertEquals(
                "https://flintnthread.com/uploads/products/old.jpg",
                mapper.resolveImageUrl("uploads/products/old.jpg")
        );
    }

    @Test
    void resolveImageUrl_rewritesWrongHostToProductCdn() {
        assertEquals(
                "https://flintnthread.com/uploads/products/x.jpeg",
                mapper.resolveImageUrl("https://flintnthread.in/uploads/products/x.jpeg")
        );
    }

    @Test
    void resolveImageUrl_blankReturnsNull() {
        assertNull(mapper.resolveImageUrl(null));
        assertNull(mapper.resolveImageUrl("  "));
    }
}
