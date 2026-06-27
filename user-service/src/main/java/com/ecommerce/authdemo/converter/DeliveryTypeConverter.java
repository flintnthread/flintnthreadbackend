package com.ecommerce.authdemo.converter;

import com.ecommerce.authdemo.dto.Enum.DeliveryType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DeliveryTypeConverter implements AttributeConverter<DeliveryType, String> {

    @Override
    public String convertToDatabaseColumn(DeliveryType deliveryType) {
        if (deliveryType == null) {
            return null;
        }
        switch (deliveryType) {
            case intra_city:
                return "intra_city";
            case metro_metro:
                return "metro_metro";
            default:
                throw new IllegalArgumentException("Unknown delivery type: " + deliveryType);
        }
    }

    @Override
    public DeliveryType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        switch (dbData) {
            case "intra_city":
                return DeliveryType.intra_city;
            case "metro_metro":
                return DeliveryType.metro_metro;
            default:
                // Handle legacy database values
                if ("INTRA_CITY".equals(dbData)) {
                    return DeliveryType.intra_city;
                } else if ("METRO_METRO".equals(dbData)) {
                    return DeliveryType.metro_metro;
                }
                throw new IllegalArgumentException("Unknown delivery type: " + dbData);
        }
    }
}
