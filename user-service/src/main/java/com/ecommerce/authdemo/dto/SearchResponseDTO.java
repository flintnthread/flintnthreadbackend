package com.ecommerce.authdemo.dto;

import com.ecommerce.authdemo.entity.Category;
import com.ecommerce.authdemo.entity.Product;
import com.ecommerce.authdemo.entity.SubCategory;

import java.util.List;

public class SearchResponseDTO {

        private List<Category> categories;
        private List<SubCategory> subCategories;
        private List<Product> products;

        public SearchResponseDTO(List<Category> categories,
                                 List<SubCategory> subCategories,
                                 List<Product> products) {
            this.categories = categories;
            this.subCategories = subCategories;
            this.products = products;
        }

        public List<Category> getCategories() {
            return categories;
        }

        public List<SubCategory> getSubCategories() {
            return subCategories;
        }

        public List<Product> getProducts() {
            return products;
        }
    }

