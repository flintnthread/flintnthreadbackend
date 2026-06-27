package com.ecommerce.authdemo.converter;

import com.ecommerce.authdemo.dto.Enum.AdminStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

    @Converter(autoApply = false)
    public class AdminStatusConverter implements AttributeConverter<AdminStatus, String> {

        @Override
        public String convertToDatabaseColumn(AdminStatus status) {
            if (status == null) {
                return null;
            }
            return status.name().toLowerCase();
        }

        @Override
        public AdminStatus convertToEntityAttribute(String dbData) {
            if (dbData == null) {
                return null;
            }
            return AdminStatus.valueOf(dbData.toUpperCase());
        }
    }

