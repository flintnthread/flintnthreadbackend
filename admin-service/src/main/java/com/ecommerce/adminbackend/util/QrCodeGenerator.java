package com.ecommerce.adminbackend.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
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

    /** Code-128 barcode PNG data URL for AWB / tracking numbers. */
    public static String toBase64BarcodePngDataUrl(String content, int width, int height) {
        if (content == null || content.isBlank()) {
            return "";
        }
        try {
            BitMatrix matrix = new Code128Writer().encode(
                    content.trim(),
                    BarcodeFormat.CODE_128,
                    Math.max(120, width),
                    Math.max(40, height));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate barcode.", ex);
        }
    }
}
