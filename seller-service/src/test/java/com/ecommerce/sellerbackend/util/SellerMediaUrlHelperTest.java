package com.ecommerce.sellerbackend.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SellerMediaUrlHelperTest {

    @Test
    void bareFilename_mapsToSellerDocumentsPath() {
        assertEquals(
                "/uploads/seller_documents/12_aadhar_front_1759064059.png",
                SellerMediaUrlHelper.toPublicPath("12_aadhar_front_1759064059.png")
        );
    }

    @Test
    void legacySellersPath_rewritesToSellerDocuments() {
        assertEquals(
                "/uploads/seller_documents/12_profile_pic_1759064059.jpg",
                SellerMediaUrlHelper.toPublicPath("/uploads/sellers/12_profile_pic_1759064059.jpg")
        );
    }

    @Test
    void cloudinaryUrl_unchanged() {
        String url = "https://res.cloudinary.com/dnce88bry/image/upload/v1/flintnthread/sellers/profile/x.jpg";
        assertEquals(url, SellerMediaUrlHelper.toPublicPath(url));
    }

    @Test
    void absoluteUrl_forcesComCdn_evenWhenBaseIsIn() {
        String out = SellerMediaUrlHelper.toAbsoluteUrl(
                "12_aadhar_front_1759064059.png",
                "https://flintnthread.in"
        );
        assertEquals(
                "https://flintnthread.com/uploads/seller_documents/12_aadhar_front_1759064059.png",
                out
        );
    }

    @Test
    void isSellerDocumentFileName_matchesProfilePic() {
        assertTrue(SellerMediaUrlHelper.isSellerDocumentFileName("12_profile_pic_1759064059.jpg"));
    }
}
