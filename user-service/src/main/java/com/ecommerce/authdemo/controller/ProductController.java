package com.ecommerce.authdemo.controller;



import com.ecommerce.authdemo.dto.*;

import com.ecommerce.authdemo.service.ProductService;



import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;



import java.math.BigDecimal;

import java.util.List;



@RestController

@RequestMapping("/api/products")

@RequiredArgsConstructor

public class ProductController {



    private final ProductService productService;



    @PostMapping

    public ProductDTO create(@RequestBody ProductDTO dto) {

        return productService.createProduct(dto);

    }



    @GetMapping

    public Page<ProductDTO> all(Pageable pageable) {

        return productService.getAllProducts(pageable);

    }
    @GetMapping("/sort")
    public Page<ProductDTO> getSortedProducts(
            @RequestParam(defaultValue = "lowToHigh") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return productService.getSortedProducts(sort, page, size);
    }



    @PostMapping("/search/filter")

    public Page<ProductDTO> filterProducts(@RequestBody ProductFilterRequestDTO filterRequest) {

        return productService.getFilteredProducts(filterRequest);

    }

    @PostMapping("/search/filter/enhanced")

    public FilterResponseDTO filterProductsEnhanced(
            @RequestBody EnhancedProductFilterRequestDTO filterRequest) {

        return productService.getFilteredProductsEnhanced(filterRequest);

    }



    @GetMapping("/search/filter")

    public Page<ProductDTO> filterProductsGet(

            @RequestParam(required = false) String keyword,

            @RequestParam(required = false) Long categoryId,

            @RequestParam(required = false) Long subcategoryId,

            @RequestParam(required = false) Long sellerId,

            @RequestParam(required = false) String sortBy,

            @RequestParam(required = false) String sortDirection,

            @RequestParam(required = false) Integer page,

            @RequestParam(required = false) Integer size,

            @RequestParam(required = false) String gender

    ) {

        ProductFilterRequestDTO filterRequest = new ProductFilterRequestDTO();



        // Set search criteria

        filterRequest.setKeyword(keyword);

        filterRequest.setCategoryId(categoryId);

        filterRequest.setSubcategoryId(subcategoryId);

        filterRequest.setSellerId(sellerId);



        // Set pagination

        filterRequest.setPage(page != null ? page : 0);

        filterRequest.setSize(size != null ? size : 10);



        // Set sorting

        filterRequest.setSortBy(sortBy != null ? sortBy : "createdAt");

        filterRequest.setSortDirection(sortDirection != null ? sortDirection : "desc");



        // Set gender filter

        if (gender != null) {

            filterRequest.setGenders(List.of(gender));

        }



        return productService.getFilteredProducts(filterRequest);

    }



    @GetMapping("/gender/{gender}")

    public Page<ProductDTO> getByGender(

            @PathVariable String gender,

            @RequestParam(required = false) Long mainCategoryId,

            @RequestParam(required = false) Long categoryId,

            @RequestParam(required = false) Integer page,

            @RequestParam(required = false) Integer size,

            @RequestParam(required = false) String sortBy,

            @RequestParam(required = false) String sortDirection

    ) {

        Long scopedMainCategoryId = mainCategoryId != null ? mainCategoryId : categoryId;

        return productService.getProductsByGenderBrowse(
                gender,
                scopedMainCategoryId,
                page != null ? page : 0,
                size != null ? size : 100,
                sortBy != null ? sortBy : "createdAt",
                sortDirection != null ? sortDirection : "desc"
        );

    }



    @GetMapping("/filter-products")

    public Page<ProductDTO> filterProductsSimple(

            @RequestParam(required = false) Long categoryId,

            @RequestParam(required = false) String gender,

            @RequestParam(required = false) Integer page,

            @RequestParam(required = false) Integer size

    ) {

        ProductFilterRequestDTO filterRequest = new ProductFilterRequestDTO();



        if (categoryId != null) {

            filterRequest.setCategoryId(categoryId);

        }



        if (gender != null) {

            filterRequest.setGenders(List.of(gender));

        }



        filterRequest.setPage(page != null ? page : 0);

        filterRequest.setSize(size != null ? size : 20);

        filterRequest.setSortBy("createdAt");

        filterRequest.setSortDirection("desc");



        return productService.getFilteredProducts(filterRequest);

    }



    @GetMapping("/category/{id}")

    public Page<ProductDTO> byCategory(@PathVariable Long id, Pageable pageable) {

        return productService.getByCategory(id, pageable);

    }



    @GetMapping("/recent")

    public List<ProductDTO> recent() {

        return productService.getRecentProducts();

    }



    @GetMapping("/main-category/{mainCategoryId}/recent")

    public List<ProductDTO> recentByMainCategory(@PathVariable Long mainCategoryId) {

        return productService.getRecentProductsByMainCategory(mainCategoryId);

    }



    @GetMapping("/main-category/{mainCategoryId}/latest")

    public List<ProductDTO> latestByMainCategory(@PathVariable Long mainCategoryId) {

        return productService.getLatestProductsByMainCategory(mainCategoryId);

    }



    @GetMapping("/popular")

    public List<ProductDTO> popular() {

        return productService.getPopularProducts();

    }



    @GetMapping("/main-category/{mainCategoryId}/popular")

    public List<ProductDTO> popularByMainCategory(@PathVariable Long mainCategoryId) {

        return productService.getPopularProductsByMainCategory(mainCategoryId);

    }



    @GetMapping("/trending")

    public List<ProductDTO> trending() {

        return productService.getTrendingProducts();

    }



    @GetMapping("/main-category/{mainCategoryId}/trending")

    public List<ProductDTO> trendingByMainCategory(@PathVariable Long mainCategoryId) {

        return productService.getTrendingProductsByMainCategory(mainCategoryId);

    }



    @GetMapping("/related/{id}")

    public List<ProductDTO> related(@PathVariable Long id) {

        return productService.getRelatedProducts(id);

    }



    @GetMapping("/main-category/{mainCategoryId}/related")

    public List<ProductDTO> relatedByMainCategory(@PathVariable Long mainCategoryId) {

        return productService.getRelatedProductsByMainCategory(mainCategoryId);

    }



    @GetMapping("/search")
    public Page<ProductDTO> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return productService.advancedSearch(q, pageable);
    }


    @PostMapping("/view")

    public void trackView(@RequestBody ProductViewDTO dto) {

        productService.trackView(dto);

    }





    @GetMapping("/subcategory/{id}")

    public List<ProductDTO> bySubCategory(@PathVariable Long id) {

        return productService.getBySubCategory(id);

    }





    @GetMapping("/top-selling-price")

    public List<ProductDTO> getTopSellingPriceProducts() {

        return productService.getTopSellingPriceProducts();

    }



    @GetMapping("/category/{categoryId}/top-products")

    public List<ProductDTO> getTopProductsByCategory(

            @PathVariable Long categoryId) {



        return productService.getTopProductsByCategory(categoryId);

    }





    @GetMapping("/discount/top")

    public List<ProductDTO> getTopDiscountProducts() {

        return productService.getTopDiscountProducts();

    }



    @GetMapping("/main-category/{mainCategoryId}/discount/asc")

    public List<ProductDTO> getDiscountProductsByMainCategoryAsc(@PathVariable Long mainCategoryId) {

        return productService.getDiscountProductsByMainCategoryAsc(mainCategoryId);

    }



    @GetMapping("/main-category/{mainCategoryId}/discount/percentage/{discountPercentage}")

    public List<ProductDTO> getProductsByMainCategoryAndExactDiscountPercentage(

            @PathVariable Long mainCategoryId,

            @PathVariable Double discountPercentage

    ) {

        return productService.getProductsByMainCategoryAndExactDiscountPercentage(mainCategoryId, discountPercentage);

    }



    @GetMapping("/main-category/{mainCategoryId}/discount/upto/{maxDiscountPercentage}")

    public List<ProductDTO> getProductsByMainCategoryWithDiscountLessThanEqual(

            @PathVariable Long mainCategoryId,

            @PathVariable Double maxDiscountPercentage

    ) {

        return productService.getProductsByMainCategoryWithDiscountLessThanEqual(mainCategoryId, maxDiscountPercentage);

    }



    @GetMapping("/main-category/{mainCategoryId}/spotlight")

    public List<ProductDTO> getSpotlightProductsByMainCategory(@PathVariable Long mainCategoryId) {

        return productService.getSpotlightProductsByMainCategory(mainCategoryId);

    }



    @GetMapping("/main-category/{mainCategoryId}/unique")

    public List<ProductDTO> getUniqueProductsByMainCategory(@PathVariable Long mainCategoryId) {

        return productService.getUniqueProductsByMainCategory(mainCategoryId);

    }



    @GetMapping("/main-category/{mainCategoryId}/top-collections")

    public List<ProductDTO> getTopCollectionsByMainCategory(@PathVariable Long mainCategoryId) {

        return productService.getTopCollectionsByMainCategory(mainCategoryId);

    }



    @GetMapping("/main-category/{mainCategoryId}/recommended")

    public List<ProductDTO> getRecommendedProductsByMainCategory(

            @PathVariable Long mainCategoryId,

            @RequestParam(required = false) Long userId,

            @RequestParam(required = false) String sessionId

    ) {

        return productService.getRecommendedProductsByMainCategory(mainCategoryId, userId, sessionId);

    }



    @GetMapping("/main-category/{mainCategoryId}/recently-viewed")

    public List<ProductDTO> getRecentlyViewedProductsByMainCategory(

            @PathVariable Long mainCategoryId,

            @RequestParam(required = false) Long userId,

            @RequestParam(required = false) String sessionId

    ) {

        return productService.getRecentlyViewedProductsByMainCategory(mainCategoryId, userId, sessionId);

    }



    @GetMapping("/recently-viewed")

    public List<ProductDTO> getRecentlyViewedProducts(

            @RequestParam(required = false) Long userId,

            @RequestParam(required = false) String sessionId

    ) {

        return productService.getRecentlyViewedProducts(userId, sessionId);

    }



    @GetMapping("/main-category/{mainCategoryId}")

    public List<ProductDTO> getByMainCategory(@PathVariable Long mainCategoryId) {

        return productService.getByMainCategory(mainCategoryId);

    }



    @GetMapping("/{id}")

    public ProductDTO get(@PathVariable Long id) {

        return productService.getProduct(id);

    }



    @GetMapping("/sizes")

    public List<String> getAllSizes() {

        return productService.getAllSizes();

    }



    @GetMapping("/colors")

    public List<String> getAllColors() {

        return productService.getAllColors();

    }



    @GetMapping("/{productId}/sizes")

    public List<String> getSizesByProductId(@PathVariable Long productId) {

        return productService.getSizesByProductId(productId);

    }



    @GetMapping("/{productId}/colors")

    public List<String> getColorsByProductId(@PathVariable Long productId) {

        return productService.getColorsByProductId(productId);

    }



    @GetMapping("/filter/price-range")

    public Page<ProductDTO> filterByPriceRange(

            @RequestParam(required = false) BigDecimal minPrice,

            @RequestParam(required = false) BigDecimal maxPrice,

            @RequestParam(required = false) Integer page,

            @RequestParam(required = false) Integer size,

            @RequestParam(required = false) String sortBy,

            @RequestParam(required = false) String sortDirection

    ) {

        ProductFilterRequestDTO filterRequest = new ProductFilterRequestDTO();

        filterRequest.setMinPrice(minPrice);

        filterRequest.setMaxPrice(maxPrice);

        filterRequest.setPage(page != null ? page : 0);

        filterRequest.setSize(size != null ? size : 20);

        filterRequest.setSortBy(sortBy != null ? sortBy : "createdAt");

        filterRequest.setSortDirection(sortDirection != null ? sortDirection : "asc");



        return productService.getFilteredProducts(filterRequest);

    }



    @GetMapping("/filter/rating")

    public Page<ProductDTO> filterByRating(

            @RequestParam Double minRating,

            @RequestParam(required = false) Integer page,

            @RequestParam(required = false) Integer size,

            @RequestParam(required = false) String sortBy,

            @RequestParam(required = false) String sortDirection

    ) {

        ProductFilterRequestDTO filterRequest = new ProductFilterRequestDTO();

        filterRequest.setMinRating(minRating);

        filterRequest.setPage(page != null ? page : 0);

        filterRequest.setSize(size != null ? size : 20);

        filterRequest.setSortBy(sortBy != null ? sortBy : "rating");

        filterRequest.setSortDirection(sortDirection != null ? sortDirection : "desc");



        return productService.getFilteredProducts(filterRequest);

    }



    @GetMapping("/filter/price-rating")

    public Page<ProductDTO> filterByPriceAndRating(

            @RequestParam(required = false) BigDecimal minPrice,

            @RequestParam(required = false) BigDecimal maxPrice,

            @RequestParam(required = false) Double minRating,

            @RequestParam(required = false) Integer page,

            @RequestParam(required = false) Integer size,

            @RequestParam(required = false) String sortBy,

            @RequestParam(required = false) String sortDirection

    ) {

        ProductFilterRequestDTO filterRequest = new ProductFilterRequestDTO();

        filterRequest.setMinPrice(minPrice);

        filterRequest.setMaxPrice(maxPrice);

        filterRequest.setMinRating(minRating);

        filterRequest.setPage(page != null ? page : 0);

        filterRequest.setSize(size != null ? size : 20);

        filterRequest.setSortBy(sortBy != null ? sortBy : "createdAt");

        filterRequest.setSortDirection(sortDirection != null ? sortDirection : "asc");



        return productService.getFilteredProducts(filterRequest);

    }

}