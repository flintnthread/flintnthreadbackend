package com.ecommerce.adminbackend.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central logger factory for admin-backend. Use instead of {@code @Slf4j} or direct
 * {@link LoggerFactory} so all application logs share one entry point.
 */
public final class LogFactory {

    private LogFactory() {
    }

    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }
}
