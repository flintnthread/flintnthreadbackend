package com.ecommerce.authdemo.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Schema bootstrap placeholder. Does not ALTER {@code orders} — columns are managed
 * in the live database and JPA entities must match existing columns only.
 */
@Component
@Slf4j
public class OrderSchemaBootstrap {

    @PostConstruct
    public void ensureOrderColumns() {
        log.info("Order schema bootstrap skipped (no column changes); using existing orders table");
    }
}
