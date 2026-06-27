package com.ecommerce.authdemo.service;



import com.ecommerce.authdemo.dto.ProductDTO;

import com.ecommerce.authdemo.dto.ProductFilterRequestDTO;
import com.ecommerce.authdemo.dto.EnhancedProductFilterRequestDTO;
import com.ecommerce.authdemo.dto.FilterResponseDTO;
import com.ecommerce.authdemo.dto.ProductViewDTO;

import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;



import java.util.List;



public interface ProductService {



    ProductDTO createProduct(ProductDTO dto);



    ProductDTO getProduct(Long id);



    Page<ProductDTO> getAllProducts(Pageable pageable);



    Page<ProductDTO> getByCategory(Long categoryId, Pageable pageable);



    List<ProductDTO> getRecentProducts();

    List<ProductDTO> getRecentProductsByMainCategory(Long mainCategoryId);

    List<ProductDTO> getLatestProductsByMainCategory(Long mainCategoryId);



    List<ProductDTO> getPopularProducts();

    List<ProductDTO> getPopularProductsByMainCategory(Long mainCategoryId);



    List<ProductDTO> getTrendingProducts();

    List<ProductDTO> getTrendingProductsByMainCategory(Long mainCategoryId);



    List<ProductDTO> getRelatedProducts(Long productId);

    List<ProductDTO> getRelatedProductsByMainCategory(Long mainCategoryId);



    List<ProductDTO> searchProducts(String keyword);



    List<ProductDTO> getSellerProducts(Long sellerId);



    void trackView(ProductViewDTO dto);



    List<ProductDTO> getBySubCategory(Long subCategoryId);









    List<ProductDTO> getTopSellingPriceProducts();



    List<ProductDTO> getTopProductsByCategory(Long categoryId);



    List<ProductDTO> getTopDiscountProducts();

    List<ProductDTO> getDiscountProductsByMainCategoryAsc(Long mainCategoryId);

    List<ProductDTO> getProductsByMainCategoryAndExactDiscountPercentage(Long mainCategoryId, Double discountPercentage);

    List<ProductDTO> getProductsByMainCategoryWithDiscountLessThanEqual(Long mainCategoryId, Double maxDiscountPercentage);

    List<ProductDTO> getSpotlightProductsByMainCategory(Long mainCategoryId);

    List<ProductDTO> getUniqueProductsByMainCategory(Long mainCategoryId);

    List<ProductDTO> getTopCollectionsByMainCategory(Long mainCategoryId);

    List<ProductDTO> getRecommendedProductsByMainCategory(Long mainCategoryId, Long userId, String sessionId);

    List<ProductDTO> getRecentlyViewedProductsByMainCategory(Long mainCategoryId, Long userId, String sessionId);

    List<ProductDTO> getRecentlyViewedProducts(Long userId, String sessionId);



    List<ProductDTO> getByMainCategory(Long mainCategoryId);



    Page<ProductDTO> getFilteredProducts(ProductFilterRequestDTO filterRequest);
    Page<ProductDTO> getSortedProducts(String sort, int page, int size);
    FilterResponseDTO getFilteredProductsEnhanced(EnhancedProductFilterRequestDTO filterRequest);

    /** Women/Men → all products in department; Boys/Girls → kids department + gender. */
    Page<ProductDTO> getProductsByGenderBrowse(
            String gender,
            Long mainCategoryId,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );



    List<String> getAllSizes();



    List<String> getAllColors();



    List<String> getSizesByProductId(Long productId);



    List<String> getColorsByProductId(Long productId);



    Page<ProductDTO> advancedSearch(String keyword, Pageable pageable);

}