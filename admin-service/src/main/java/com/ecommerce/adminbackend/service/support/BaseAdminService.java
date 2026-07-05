package com.ecommerce.adminbackend.service.support;

import com.ecommerce.adminbackend.exception.ResourceNotFoundException;
import com.ecommerce.adminbackend.logging.LogFactory;
import com.ecommerce.adminbackend.util.TextUtils;
import org.slf4j.Logger;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public abstract class BaseAdminService {

    protected final Logger log = LogFactory.getLogger(getClass());

    protected String blankToNull(String value) {
        return TextUtils.blankToNull(value);
    }

    protected String requireNonBlank(String value, String fieldName) {
        return TextUtils.requireNonBlank(value, fieldName);
    }

    protected <T> T requireFound(java.util.Optional<T> optional, String message) {
        return optional.orElseThrow(() -> new ResourceNotFoundException(message));
    }

    protected Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    protected LocalDateTime toDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return null;
    }

    protected String csvEscape(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text + "\"";
        }
        return text;
    }

    protected String csvHeader(String... columns) {
        return String.join(",", columns);
    }

    protected String stringAt(java.util.Map<String, Object> map, String key) {
        if (map == null || key == null) {
            return null;
        }
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }
}
