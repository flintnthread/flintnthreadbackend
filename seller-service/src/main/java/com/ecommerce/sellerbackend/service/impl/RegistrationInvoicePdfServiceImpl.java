package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.entity.Seller;
import com.ecommerce.sellerbackend.service.RegistrationInvoicePdfService;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Locale;

@Service
public class RegistrationInvoicePdfServiceImpl implements RegistrationInvoicePdfService {

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

    @Value("${app.invoice.logo-url:}")
    private String invoiceLogoUrl;

    @Override
    public byte[] generateRegistrationInvoice(
            Seller seller,
            String invoiceNumber,
            String paymentId,
            String orderId,
            int amountInPaise,
            String paidAtText) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24);
        Font headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

     addLogoIfConfigured(document);

Paragraph companyInfo =
        new Paragraph(
                companyAddress + "\n"
                        + companyEmail
                        + " | "
                        + companyPhone
                        + "\nGSTIN : "
                        + companyGstin,
                bodyFont);

companyInfo.setAlignment(
        Paragraph.ALIGN_CENTER);

document.add(companyInfo);

document.add(new Paragraph(" "));

        Paragraph title =
        new Paragraph(
                "SELLER REGISTRATION INVOICE",
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        18));

title.setAlignment(Paragraph.ALIGN_CENTER);

document.add(title);

document.add(new Paragraph(
        "Seller GSTIN : "
                + seller.getGstNumber(),
        bodyFont));
document.add(new Paragraph(
        "Seller State : "
                + seller.getState(),
        bodyFont));
document.add(new Paragraph(
        "Company GSTIN : "
                + companyGstin,
        bodyFont));

document.add(new Paragraph(" "));
        Paragraph invoiceHeading =
        new Paragraph(
                "INVOICE",
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        22,
                        Color.ORANGE));

invoiceHeading.setAlignment(
        Paragraph.ALIGN_RIGHT);

document.add(invoiceHeading);

Paragraph invoiceNo =
        new Paragraph(
                invoiceNumber,
                FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD,
                        16));

invoiceNo.setAlignment(
        Paragraph.ALIGN_RIGHT);

document.add(invoiceNo);

document.add(new Paragraph(" "));

Paragraph orderLine =
        new Paragraph(
                "Order : #"
                        + orderId,
                bodyFont);

orderLine.setAlignment(
        Paragraph.ALIGN_RIGHT);

document.add(orderLine);

Paragraph dateLine =
        new Paragraph(
                "Date : "
                        + paidAtText,
                bodyFont);

dateLine.setAlignment(
        Paragraph.ALIGN_RIGHT);

document.add(dateLine);

document.add(new Paragraph(" "));

        document.add(new Paragraph("Bill To", headingFont));
        document.add(new Paragraph(
        seller.getBusinessName() != null
                ? seller.getBusinessName()
                : seller.getFullName(),
        bodyFont));

document.add(new Paragraph(
        seller.getEmail() != null
                ? seller.getEmail()
                : "-",
        bodyFont));

document.add(new Paragraph(
        seller.getMobile() != null
                ? seller.getMobile()
                : "-",
        bodyFont));

document.add(new Paragraph(
        seller.getAddress() != null
                ? seller.getAddress()
                : "-",
        bodyFont));

document.add(new Paragraph(
        (seller.getCity() != null ? seller.getCity() : "")
                + ", "
                + (seller.getState() != null ? seller.getState() : "")
                + " - "
                + (seller.getPincode() != null ? seller.getPincode() : ""),
        bodyFont));

document.add(new Paragraph(
        "GSTIN : "
                + (seller.getGstNumber() != null
                ? seller.getGstNumber()
                : "N/A"),
        bodyFont));document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{6f, 2f, 2f});
        table.addCell(headerCell("Description"));
        table.addCell(headerCell("Qty"));
        table.addCell(headerCell("Amount"));
        double totalAmount = amountInPaise / 100.0;

double registrationFee =
        totalAmount / 1.18;



double gstAmount = totalAmount - registrationFee;
 String sellerState =
        seller.getState() != null
                ? seller.getState().trim()
                : "";

boolean sameState =
        sellerState.equalsIgnoreCase(companyState);

double cgst = 0;
double sgst = 0;
double igst = 0;

if (sameState) {

    cgst = gstAmount / 2;
    sgst = gstAmount / 2;

} else {

    igst = gstAmount;
}

table.addCell(bodyCell("Seller Registration Fee"));
table.addCell(bodyCell("1"));
table.addCell(bodyCell(
        "Rs " +
        String.format("%.2f",
                registrationFee)));

if (sameState) {

    table.addCell(bodyCell("CGST @ 9%"));
    table.addCell(bodyCell("1"));
    table.addCell(bodyCell(
            "Rs " +
            String.format("%.2f",
                    cgst)));

    table.addCell(bodyCell("SGST @ 9%"));
    table.addCell(bodyCell("1"));
    table.addCell(bodyCell(
            "Rs " +
            String.format("%.2f",
                    sgst)));

} else {

    table.addCell(bodyCell("IGST @ 18%"));
    table.addCell(bodyCell("1"));
    table.addCell(bodyCell(
            "Rs " +
            String.format("%.2f",
                    igst)));
}
document.add(table);
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

document.add(new Paragraph(
        "Registration Fee : Rs "
                + String.format("%.2f", registrationFee),
        headingFont));

if (sameState) {

    document.add(new Paragraph(
            "CGST (9%) : Rs "
                    + String.format("%.2f", cgst),
            headingFont));

    document.add(new Paragraph(
            "SGST (9%) : Rs "
                    + String.format("%.2f", sgst),
            headingFont));

} else {

    document.add(new Paragraph(
            "IGST (18%) : Rs "
                    + String.format("%.2f", igst),
            headingFont));
}

Font totalFont =
        FontFactory.getFont(
                FontFactory.HELVETICA_BOLD,
                22,
                Color.ORANGE);

document.add(new Paragraph(
        "TOTAL PAID : Rs "
                + formatMoney(amountInPaise),
        totalFont));
        document.add(new Paragraph("Status: PAID", headingFont));
        document.add(new Paragraph(" "));
     document.add(new Paragraph(
        "Thank you for registering as a seller with Flint & Thread.",
        bodyFont));

document.add(new Paragraph(
        "This invoice serves as proof of registration payment.",
        bodyFont));
        document.close();
        return out.toByteArray();
       
    }

   private void addLogoIfConfigured(Document document) {

    try {

        ClassPathResource resource =
                new ClassPathResource(
                        "static/images/flint-logo.jpg");

        Image logo =
                Image.getInstance(
                        resource.getURL());

        logo.scaleToFit(500f, 140f);

        logo.setAlignment(Image.ALIGN_CENTER);

        document.add(logo);

        document.add(new Paragraph(" "));

    } catch (Exception e) {
        e.printStackTrace();
    }
}
private String getStateCodeFromGstin(String gstin) {

    if (gstin == null || gstin.length() < 2) {
        return "";
    }

    return gstin.substring(0, 2);
}
    private PdfPCell headerCell(String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setBackgroundColor(new Color(240, 240, 240));
        cell.setPadding(8f);
        return cell;
    }

    private PdfPCell bodyCell(String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 11);
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setPadding(8f);
        cell.setBorder(Rectangle.BOX);
        return cell;
    }

    private String formatMoney(int amountInPaise) {
        return String.format(Locale.ENGLISH, "%.2f", amountInPaise / 100.0);
    }
}
