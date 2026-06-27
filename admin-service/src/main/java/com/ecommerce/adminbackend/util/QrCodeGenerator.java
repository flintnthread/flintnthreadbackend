package com.ecommerce.adminbackend.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

public final class QrCodeGenerator {

    private QrCodeGenerator() {
    }

    public static String toBase64PngDataUrl(String content, int size) {
        if (content == null || content.isBlank()) {
            return "";
        }
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate QR code.", ex);
        }
    }
}
