package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.entity.Seller;
import com.ecommerce.sellerbackend.service.RegistrationInvoicePdfService;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.Locale;

@Service
public class RegistrationInvoicePdfServiceImpl implements RegistrationInvoicePdfService {

    private static final String FOOTER_NOTE =
            "This is a computer-generated invoice and does not require a physical signature or stamp. "
                    + "It serves as proof of payment for your annual seller registration subscription on Flint & Thread.";

    @Value("${invoice.seller.name:Flint & Thread (India) Pvt. Ltd.}")
    private String companyName;

    @Value("${invoice.seller.address:India}")
    private String companyAddress;

    @Value("${invoice.seller.email:support@flintnthread.in}")
    private String companyEmail;

    @Value("${invoice.seller.gstin:}")
    private String companyGstin;

    @Value("${invoice.seller.phone:+91 9063499092}")
    private String companyPhone;

    @Value("${invoice.company.state:Telangana}")
    private String companyState;

    @Value("${app.registration.fee.inr:899}")
    private int registrationFeeInr;

    @Value("${app.registration.gst.percent:18}")
    private int registrationGstPercent;

    @Override
    public byte[] generateRegistrationInvoice(
            Seller seller,
            String invoiceNumber,
            String paymentId,
            String orderId,
            int amountInPaise,
            String paidAtText) {
        try {
            return buildRegistrationInvoicePdf(seller, invoiceNumber, paymentId, orderId, amountInPaise, paidAtText);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not generate registration invoice PDF.", ex);
        }
    }

    private byte[] buildRegistrationInvoicePdf(
            Seller seller,
            String invoiceNumber,
            String paymentId,
            String orderId,
            int amountInPaise,
            String paidAtText) throws com.lowagie.text.DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(249, 115, 22));

        addHeader(document, bodyFont, headingFont, invoiceNumber, orderId, paidAtText, paymentId);

        document.add(new Paragraph("SELLER REGISTRATION INVOICE", titleFont));
        document.add(spacer(8f));

        document.add(new Paragraph("Bill To", headingFont));
        document.add(new Paragraph(resolveBillToName(seller), bodyFont));
        document.add(new Paragraph(nullSafe(seller.getEmail()), bodyFont));
        document.add(new Paragraph(nullSafe(seller.getMobile()), bodyFont));
        document.add(new Paragraph(nullSafe(seller.getAddress()), bodyFont));
        document.add(new Paragraph(formatCityLine(seller), bodyFont));
        if (hasSellerGst(seller)) {
            document.add(new Paragraph("GSTIN: " + seller.getGstNumber().trim().toUpperCase(Locale.ROOT), bodyFont));
        }
        document.add(spacer(10f));

        double registrationFee = registrationFeeInr;
        double gstAmount = registrationFeeInr * registrationGstPercent / 100.0;
        double totalAmount = registrationFee + gstAmount;

        String sellerState = seller.getState() != null ? seller.getState().trim() : "";
        boolean sameState = sellerState.equalsIgnoreCase(companyState);
        double cgst = sameState ? gstAmount / 2 : 0;
        double sgst = sameState ? gstAmount / 2 : 0;
        double igst = sameState ? 0 : gstAmount;

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 6f, 1.2f, 2.2f });
        table.addCell(headerCell("Description"));
        table.addCell(headerCell("Qty"));
        table.addCell(headerCell("Amount"));
        table.addCell(bodyCell("Annual Seller Registration Fee (per annum)"));
        table.addCell(bodyCell("1"));
        table.addCell(bodyCell("Rs " + formatMoney(registrationFee)));
        if (sameState) {
            table.addCell(bodyCell("CGST @ 9%"));
            table.addCell(bodyCell("1"));
            table.addCell(bodyCell("Rs " + formatMoney(cgst)));
            table.addCell(bodyCell("SGST @ 9%"));
            table.addCell(bodyCell("1"));
            table.addCell(bodyCell("Rs " + formatMoney(sgst)));
        } else {
            table.addCell(bodyCell("IGST @ 18%"));
            table.addCell(bodyCell("1"));
            table.addCell(bodyCell("Rs " + formatMoney(igst)));
        }
        document.add(table);
        document.add(spacer(12f));

        document.add(new Paragraph("Registration Fee (per annum): Rs " + formatMoney(registrationFee), headingFont));
        if (sameState) {
            document.add(new Paragraph("CGST (9%): Rs " + formatMoney(cgst), bodyFont));
            document.add(new Paragraph("SGST (9%): Rs " + formatMoney(sgst), bodyFont));
        } else {
            document.add(new Paragraph("IGST (18%): Rs " + formatMoney(igst), bodyFont));
        }
        document.add(new Paragraph("TOTAL PAID: Rs " + formatMoney(totalAmount), totalFont));
        document.add(new Paragraph("Status: PAID", headingFont));
        document.add(spacer(14f));

        Paragraph note = new Paragraph(FOOTER_NOTE, smallFont);
        note.setAlignment(Element.ALIGN_JUSTIFIED);
        document.add(note);

        document.close();
        return out.toByteArray();
    }

    private void addHeader(
            Document document,
            Font bodyFont,
            Font headingFont,
            String invoiceNumber,
            String orderId,
            String paidAtText,
            String paymentId) throws com.lowagie.text.DocumentException {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[] { 1.4f, 2f });

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_TOP);
        logoCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        logoCell.setPadding(0f);
        Image logo = loadLogo();
        if (logo != null) {
            logo.scaleToFit(130f, 44f);
            logoCell.addElement(logo);
        } else {
            logoCell.addElement(new Paragraph(companyName, headingFont));
        }
        header.addCell(logoCell);

        PdfPCell metaCell = new PdfPCell();
        metaCell.setBorder(Rectangle.NO_BORDER);
        metaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        metaCell.setVerticalAlignment(Element.ALIGN_TOP);
        metaCell.setPadding(0f);
        metaCell.addElement(rightLine(companyName, headingFont));
        metaCell.addElement(rightLine(companyAddress, bodyFont));
        metaCell.addElement(rightLine(companyEmail, bodyFont));
        metaCell.addElement(rightLine(companyPhone, bodyFont));
        if (hasText(companyGstin)) {
            metaCell.addElement(rightLine("GSTIN: " + companyGstin.trim(), bodyFont));
        }
        metaCell.addElement(spacer(6f));
        metaCell.addElement(rightLine("INVOICE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(249, 115, 22))));
        metaCell.addElement(rightLine(invoiceNumber, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        metaCell.addElement(rightLine("Order: #" + orderId, bodyFont));
        metaCell.addElement(rightLine("Date: " + paidAtText, bodyFont));
        if (hasText(paymentId)) {
            metaCell.addElement(rightLine("Payment ID: " + paymentId, bodyFont));
        }
        header.addCell(metaCell);

        document.add(header);
        document.add(spacer(12f));
    }

    private Paragraph rightLine(String text, Font font) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_RIGHT);
        paragraph.setSpacingAfter(2f);
        return paragraph;
    }

    private Paragraph spacer(float height) {
        Paragraph paragraph = new Paragraph(" ");
        paragraph.setSpacingAfter(height);
        return paragraph;
    }

    private Image loadLogo() {
        String[] candidates = {
                "static/images/flint-thread-logo.png",
                "static/images/flint-logo.jpg"
        };
        for (String path : candidates) {
            try {
                ClassPathResource resource = new ClassPathResource(path);
                if (resource.exists()) {
                    return Image.getInstance(resource.getURL());
                }
            } catch (Exception ignored) {
                // try next resource
            }
        }
        return null;
    }

    private boolean hasSellerGst(Seller seller) {
        if (seller == null || seller.getGstNumber() == null) {
            return false;
        }
        String gst = seller.getGstNumber().trim();
        return !gst.isEmpty() && !"N/A".equalsIgnoreCase(gst) && !"-".equals(gst);
    }

    private String resolveBillToName(Seller seller) {
        if (seller.getBusinessName() != null && !seller.getBusinessName().isBlank()) {
            return seller.getBusinessName().trim();
        }
        return seller.getFullName() != null ? seller.getFullName() : "-";
    }

    private String formatCityLine(Seller seller) {
        return (seller.getCity() != null ? seller.getCity() : "")
                + ", "
                + (seller.getState() != null ? seller.getState() : "")
                + " - "
                + (seller.getPincode() != null ? seller.getPincode() : "");
    }

    private String nullSafe(String value) {
        return value != null && !value.isBlank() ? value : "-";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank() && !"N/A".equalsIgnoreCase(value.trim());
    }

    private PdfPCell headerCell(String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setBackgroundColor(new Color(240, 240, 240));
        cell.setPadding(7f);
        return cell;
    }

    private PdfPCell bodyCell(String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 10);
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setPadding(7f);
        cell.setBorder(Rectangle.BOX);
        return cell;
    }

    private String formatMoney(double amount) {
        return String.format(Locale.ENGLISH, "%.2f", amount);
    }
}
