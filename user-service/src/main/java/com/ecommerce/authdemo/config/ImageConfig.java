package com.ecommerce.authdemo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Serves local invoice/product upload files. Prefer CDN ({@code app.media.public-base-url})
 * for clients; this handler covers local/dev and same-host fallbacks.
 */
@Configuration
public class ImageConfig implements WebMvcConfigurer {

    @Value("${app.upload.products-directory:../seller-service/uploads/products}")
    private String productsUploadDirectory;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path invoiceDir = Path.of(System.getProperty("user.dir"), "invoices").toAbsolutePath().normalize();
        String invoiceLocation = "file:" + invoiceDir.toString().replace("\\", "/") + "/";

        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:C:/ecommerce-images/");

        registry.addResourceHandler("/invoices/**")
                .addResourceLocations(invoiceLocation);

        Path productsPath = Paths.get(productsUploadDirectory).toAbsolutePath().normalize();
        String productsLocation = productsPath.toUri().toString();
        if (!productsLocation.endsWith("/")) {
            productsLocation = productsLocation + "/";
        }
        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations(productsLocation);

        Path uploadsRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        String uploadsLocation = uploadsRoot.toUri().toString();
        if (!uploadsLocation.endsWith("/")) {
            uploadsLocation = uploadsLocation + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsLocation);
    }
}
