package com.ecommerce.adminbackend.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface SiteLogoAdminService {

    Map<String, Object> get();

    Map<String, Object> update(Map<String, Object> body);

    Map<String, Object> upload(String slot, MultipartFile file);
}
