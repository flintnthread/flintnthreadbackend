package com.ecommerce.authdemo.service.impl;



import com.ecommerce.authdemo.dto.SubCategoryResponseDTO;
import com.ecommerce.authdemo.entity.SubCategory;
import com.ecommerce.authdemo.repository.SubCategoryRepository;
import com.ecommerce.authdemo.service.SubCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

    @Service
    public class SubCategoryServiceImpl implements SubCategoryService {

        private final SubCategoryRepository subCategoryRepository;

        public SubCategoryServiceImpl(SubCategoryRepository subCategoryRepository) {
            this.subCategoryRepository = subCategoryRepository;
        }

        @Override
        public List<SubCategoryResponseDTO> getSubCategories(Long categoryId) {

            List<SubCategory> subCategories =
                    subCategoryRepository.findByCategoryId(categoryId);

            return subCategories.stream()
                    .map(sc -> new SubCategoryResponseDTO(
                            sc.getId(),
                            sc.getSubcategoryName(),
                            sc.getSubcategoryImage(),
                            sc.getMobileImage()        // ✅ mobile image
                    ))
                    .collect(Collectors.toList());
        }
    }

