package com.ecommerce.sellerbackend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.ecommerce.sellerbackend.profile.SellerDocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaStorageServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private ProductMediaStorageService productMediaStorageService;
    private MediaStorageService mediaStorageService;

    @BeforeEach
    void setUp() throws Exception {
        productMediaStorageService = new ProductMediaStorageService(cloudinary);
        ReflectionTestUtils.setField(productMediaStorageService, "cloudinaryFolderPrefix", "flintnthread");

        mediaStorageService = new MediaStorageService(
                Files.createTempDirectory("seller-docs-test").toString(),
                "https://flintnthread.com",
                productMediaStorageService
        );
    }

    @Test
    void storeSellerDocument_uploadsImageToCloudinaryAndReturnsSecureUrl() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "secure_url",
                "https://res.cloudinary.com/dnce88bry/image/upload/v1/flintnthread/sellers/documents/aadhar_front/abc.jpg"
        ));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "aadhar.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01}
        );

        MediaStorageService.StoredFile stored =
                mediaStorageService.storeSellerDocument(12L, SellerDocumentType.AADHAR_FRONT, file);

        assertTrue(stored.fileName().startsWith("https://res.cloudinary.com/"));
        assertEquals(stored.fileName(), stored.publicUrl());
        assertTrue(stored.publicUrl().contains("/sellers/documents/aadhar_front/"));
        verify(uploader).upload(any(byte[].class), anyMap());
    }

    @Test
    void storeSellerDocument_uploadsPdfAsRawResource() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "secure_url",
                "https://res.cloudinary.com/dnce88bry/raw/upload/v1/flintnthread/sellers/documents/pan_card/doc.pdf"
        ));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pan.pdf",
                "application/pdf",
                new byte[]{0x25, 0x50, 0x44, 0x46}
        );

        MediaStorageService.StoredFile stored =
                mediaStorageService.storeSellerDocument(12L, SellerDocumentType.PAN_CARD, file);

        assertTrue(stored.publicUrl().contains("/raw/upload/"));
        assertEquals(
                "https://res.cloudinary.com/dnce88bry/raw/upload/v1/flintnthread/sellers/documents/pan_card/doc.pdf",
                stored.publicUrl()
        );
    }
}
