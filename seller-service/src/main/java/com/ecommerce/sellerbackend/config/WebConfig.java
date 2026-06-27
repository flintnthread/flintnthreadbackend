package com.ecommerce.sellerbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:8081,http://localhost:19006,http://127.0.0.1:8081}")
    private String allowedOrigins;

    @Value("${app.upload.directory:uploads/sellers}")
    private String uploadDirectory;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(false);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDirectory).toAbsolutePath().normalize();
        String location = uploadPath.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/sellers/**")
                .addResourceLocations(location);

        registry.addResourceHandler("/uploads/seller_documents/**")
                .addResourceLocations(location);


        Path kycPath = Paths.get(uploadDir, "kyc_images").toAbsolutePath().normalize();
        String kycLocation = kycPath.toUri().toString();
        if (!kycLocation.endsWith("/")) {
            kycLocation = kycLocation + "/";
        }
        registry.addResourceHandler("/uploads/kyc_images/**")
                .addResourceLocations(kycLocation);
        Path productsPath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("products");
        String productsLocation = productsPath.toUri().toString();
        if (!productsLocation.endsWith("/")) {
            productsLocation = productsLocation + "/";
        }
        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations(productsLocation);


        Path sizeChartsPath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("size_charts");
        String sizeChartsLocation = sizeChartsPath.toUri().toString();
        if (!sizeChartsLocation.endsWith("/")) {
            sizeChartsLocation = sizeChartsLocation + "/";
        }
        registry.addResourceHandler("/uploads/size_charts/**")
                .addResourceLocations(sizeChartsLocation);

    }
}
