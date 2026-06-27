package com.ecommerce.adminbackend.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayOutputStream;

public final class InvoicePdfRenderer {

    private InvoicePdfRenderer() {
    }

    public static byte[] renderPdf(String html) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate invoice PDF.", ex);
        }
    }
}
