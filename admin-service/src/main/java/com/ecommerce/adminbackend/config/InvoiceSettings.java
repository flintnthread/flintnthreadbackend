package com.ecommerce.adminbackend.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Getter
public class InvoiceSettings {

    @Value("${app.invoice.company-name:Flint & Thread (India) Pvt. Ltd.}")
    private String companyName;

    @Value("${app.invoice.company-country:India}")
    private String companyCountry;

    @Value("${app.invoice.company-phone:+91 9063499092}")
    private String companyPhone;

    @Value("${app.invoice.company-email:support@flintnthread.in}")
    private String companyEmail;

    @Value("${app.invoice.company-gstin:36AAGCF5402J1ZP}")
    private String companyGstin;

    @Value("${app.admin.frontend-url:http://localhost:8081}")
    private String adminFrontendUrl;

    public Map<String, Object> toCompanyMap() {
        Map<String, Object> company = new LinkedHashMap<>();
        company.put("name", companyName);
        company.put("country", companyCountry);
        company.put("phone", companyPhone);
        company.put("email", companyEmail);
        company.put("gstin", companyGstin);
        return company;
    }
}
