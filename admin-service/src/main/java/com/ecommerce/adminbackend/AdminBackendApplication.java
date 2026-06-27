package com.ecommerce.adminbackend;

import com.ecommerce.adminbackend.logging.LogFactory;
import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class AdminBackendApplication {

    private static final Logger log = LogFactory.getLogger(AdminBackendApplication.class);

    public static void main(String[] args) {
        log.info("Starting admin-backend...");
        SpringApplication.run(AdminBackendApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("admin-backend is ready and accepting requests on port 8082");
    }
}
