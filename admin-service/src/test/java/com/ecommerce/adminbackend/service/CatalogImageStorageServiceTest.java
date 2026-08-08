package com.ecommerce.adminbackend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogImageStorageServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private CatalogImageStorageService service;

    @BeforeEach
    void setUp() {
        service = new CatalogImageStorageService(cloudinary);
        ReflectionTestUtils.setField(service, "cloudinaryFolderPrefix", "flintnthread");
        ReflectionTestUtils.setField(service, "cmsDirectory", "uploads/cms");
    }

    @Test
    void storeCategoryImage_uploadsToCloudinaryCategoriesFolder() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "secure_url",
                "https://res.cloudinary.com/dnce88bry/image/upload/v1/flintnthread/categories/abc.jpg"
        ));

        MockMultipartFile file = new MockMultipartFile(
                "mobileImage",
                "chair.jpeg",
                "image/jpeg",
                new byte[]{1, 2, 3, 4}
        );

        String url = service.storeCategoryImage(file);

        assertTrue(url.startsWith("https://res.cloudinary.com/"));
        assertEquals(
                "https://res.cloudinary.com/dnce88bry/image/upload/v1/flintnthread/categories/abc.jpg",
                url
        );
    }

    @Test
    void storeSubcategoryImage_uploadsToCloudinarySubcategoriesFolder() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "secure_url",
                "https://res.cloudinary.com/dnce88bry/image/upload/v1/flintnthread/subcategories/xyz.jpg"
        ));

        MockMultipartFile file = new MockMultipartFile(
                "image",
                "sub.jpg",
                "image/jpeg",
                new byte[]{9, 8, 7}
        );

        String url = service.storeSubcategoryImage(file);

        assertEquals(
                "https://res.cloudinary.com/dnce88bry/image/upload/v1/flintnthread/subcategories/xyz.jpg",
                url
        );
    }

    @Test
    void normalizeCategoryImageValue_keepsCloudinaryUrl() {
        String cloudinary = "https://res.cloudinary.com/dnce88bry/image/upload/v1/flintnthread/categories/x.jpg";
        assertEquals(cloudinary, service.normalizeCategoryImageValue(cloudinary));
    }
}
