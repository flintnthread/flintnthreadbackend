package com.ecommerce.sellerbackend.service;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductMediaStorageServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private ProductMediaStorageService service;

    @BeforeEach
    void setUp() {
        service = new ProductMediaStorageService(cloudinary);
        ReflectionTestUtils.setField(service, "cloudinaryFolderPrefix", "flintnthread");
    }

    @Test
    void storeProductImage_keepsLegacyUploadsPath() {
        assertEquals(
                "uploads/products/old-abc.jpg",
                service.storeProductImage("uploads/products/old-abc.jpg")
        );
        assertEquals(
                "uploads/products/old-abc.jpg",
                service.storeProductImage("/uploads/products/old-abc.jpg")
        );
    }

    @Test
    void storeProductImage_keepsCloudinaryUrlAsIs() {
        String url = "https://res.cloudinary.com/dnce88bry/image/upload/v1/flintnthread/products/new.jpg";
        assertEquals(url, service.storeProductImage(url));
    }

    @Test
    void storeProductImage_extractsRelativePathFromUploadsHttpsUrl() {
        String source = "http://localhost:8083/uploads/products/legacy.jpg";
        assertEquals("uploads/products/legacy.jpg", service.storeProductImage(source));
    }

    @Test
    void uploadMultipart_returnsCloudinarySecureUrl() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "secure_url",
                "https://res.cloudinary.com/dnce88bry/image/upload/v1/flintnthread/products/abc.jpg"
        ));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "product.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02}
        );

        String url = service.uploadMultipart(file);

        assertTrue(url.startsWith("https://res.cloudinary.com/"));
        assertEquals(
                "https://res.cloudinary.com/dnce88bry/image/upload/v1/flintnthread/products/abc.jpg",
                url
        );
        verify(uploader).upload(any(byte[].class), anyMap());
    }
}
