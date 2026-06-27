package com.ecommerce.adminbackend.entity.converter;

import com.ecommerce.adminbackend.entity.SellerAccountStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SellerAccountStatusConverter implements AttributeConverter<SellerAccountStatus, String> {

    @Override
    public String convertToDatabaseColumn(SellerAccountStatus attribute) {
        return attribute == null ? SellerAccountStatus.pending.name() : attribute.name();
    }

    @Override
    public SellerAccountStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return SellerAccountStatus.pending;
        }
        try {
            return SellerAccountStatus.valueOf(dbData.trim());
        } catch (IllegalArgumentException ex) {
            return SellerAccountStatus.pending;
        }
    }
}
