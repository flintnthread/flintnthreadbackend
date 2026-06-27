package com.ecommerce.authdemo.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CategoryRequest {

        private String name;

        private MultipartFile bannerImage;

        private MultipartFile mobileImage;

}

