package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.*;
import com.ecommerce.authdemo.entity.Product;
import com.ecommerce.authdemo.entity.ProductView;
import com.ecommerce.authdemo.entity.Seller;
import com.ecommerce.authdemo.entity.User;
import com.ecommerce.authdemo.mapper.ProductMapper;
import com.ecommerce.authdemo.util.GenderBrowseHelper;
import com.ecommerce.authdemo.util.ProductCatalogVisibility;
import com.ecommerce.authdemo.util.SizeColorMapper;
import com.ecommerce.authdemo.entity.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import com.ecommerce.authdemo.repository.*;
import com.ecommerce.authdemo.service.ProductService;
import com.ecommerce.authdemo.specification.ProductSpecification;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepo;
    private final ProductVariantRepository variantRepo;
    private final ProductViewRepository viewRepo;
    private final CategoryRepository categoryRepository;
    private final SellerRepository sellerRepository;
    private final SizeColorMapper sizeColorMapper;
    private final ProductMapper mapper;

    @Override
    @Transactional(readOnly = false)
    public ProductDTO createProduct(ProductDTO dto) {
        Product product = mapper.toEntity(dto);
        return mapper.toDTO(productRepo.save(product));
    }

    @Override
    public ProductDTO getProduct(Long id) {
        Product product = productRepo.findAllWithImagesAndVariantsByIdIn(List.of(id))
                .stream()
                .findFirst()
                .orElseGet(() -> productRepo.findById(id)
                        .orElseThrow(() -> new RuntimeException("Product not found")));
        if (!ProductCatalogVisibility.isVisibleToUsers(product)) {
            throw new RuntimeException("Product not found");
        }
        ProductDTO dto = mapper.toDTO(product);
        applySellerBusinessName(dto);
        return dto;
    }

    private void applySellerBusinessName(ProductDTO dto) {
        Long sellerId = dto.getSellerId();
        if (sellerId == null || sellerId <= 0) {
            return;
        }
        sellerRepository.findById(sellerId).ifPresent(seller -> {
            String label = resolveSellerBusinessLabel(seller);
            if (label != null && !label.isBlank()) {
                dto.setSellerBusinessName(label);
            }
        });
    }

    private String resolveSellerBusinessLabel(Seller seller) {
        if (seller == null) {
            return null;
        }
        String business = seller.getBusinessName() == null ? "" : seller.getBusinessName().trim();
        if (!business.isEmpty()) {
            return business;
        }
        String first = seller.getFirstName() == null ? "" : seller.getFirstName().trim();
        String last = seller.getLastName() == null ? "" : seller.getLastName().trim();
        String fullName = (first + " " + last).trim();
        return fullName.isEmpty() ? null : fullName;
    }

    @Override
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        Page<Product> page = productRepo.findAll(ProductCatalogVisibility.visibleToUsers(), pageable);
        return mapPageWithImages(page, pageable);
    }

    @Override
    public Page<ProductDTO> getByCategory(Long categoryId, Pageable pageable) {
        Page<Product> page = productRepo.findByCategoryIdAndStatus(
                categoryId, ProductCatalogVisibility.USER_VISIBLE_STATUS, pageable);
        return mapPageWithImages(page, pageable);
    }

    /** Re-load images/variants in one query so list cards always get image URLs. */
    private Page<ProductDTO> mapPageWithImages(Page<Product> page, Pageable pageable) {
        List<Long> ids = page.getContent().stream().map(Product::getId).toList();
        if (ids.isEmpty()) {
            return Page.empty(pageable);
        }
        Map<Long, ProductDTO> byId = new LinkedHashMap<>();
        productRepo.findAllWithImagesAndVariantsByIdIn(ids).stream()
                .filter(ProductCatalogVisibility::isVisibleToUsers)
                .forEach(p -> byId.put(p.getId(), mapper.toDTO(p)));
        List<ProductDTO> mapped = ids.stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        return new PageImpl<>(mapped, pageable, page.getTotalElements());
    }

    @Override
    public List<ProductDTO> getRecentProducts() {
        return productRepo.findTop10ByStatusOrderByCreatedAtDesc(ProductCatalogVisibility.USER_VISIBLE_STATUS)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<ProductDTO> getRecentProductsByMainCategory(Long mainCategoryId) {
        return productRepo.findRecentProductsByMainCategory(mainCategoryId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<ProductDTO> getLatestProductsByMainCategory(Long mainCategoryId) {
        return productRepo.findLatestProductsByMainCategory(mainCategoryId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<ProductDTO> getPopularProducts() {
        return productRepo.findPopularProducts()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<ProductDTO> getPopularProductsByMainCategory(Long mainCategoryId) {
        return productRepo.findPopularProductsByMainCategory(mainCategoryId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<ProductDTO> getTrendingProducts() {
        LocalDateTime last7Days = LocalDateTime.now().minusDays(7);
        return productRepo.findTrendingProducts(last7Days)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<ProductDTO> getTrendingProductsByMainCategory(Long mainCategoryId) {
        LocalDateTime last7Days = LocalDateTime.now().minusDays(7);
        return productRepo.findTrendingProductsByMainCategory(last7Days, mainCategoryId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<ProductDTO> getRelatedProducts(Long productId) {
        Product product = productRepo.findById(productId).orElseThrow();
        return productRepo.findTop10ByCategoryIdAndStatusAndIdNot(
                Long.valueOf(product.getCategoryId()),
                ProductCatalogVisibility.USER_VISIBLE_STATUS,
                productId
        ).stream().map(mapper::toDTO).toList();
    }

    @Override
    public List<ProductDTO> getRelatedProductsByMainCategory(Long mainCategoryId) {
        return productRepo.findRelatedProductsByMainCategory(mainCategoryId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<ProductDTO> searchProducts(String keyword) {
        return productRepo.search(keyword)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<ProductDTO> getSellerProducts(Long sellerId) {
        return productRepo.findBySellerId(sellerId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = false)
    public void trackView(ProductViewDTO dto) {
        ProductView view = new ProductView();
        Product product = new Product();
        product.setId(dto.getProductId());
        view.setProduct(product);
        view.setSessionId(dto.getSessionId());

        if (dto.getUserId() != null) {
            User user = new User();
            user.setId(dto.getUserId());
            view.setUser(user);
        }

        view.setSessionId(dto.getSessionId());
        viewRepo.save(view);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getBySubCategory(Long subCategoryId) {
        List<Product> products = productRepo.findBySubCategoryFull(subCategoryId);
        return products.stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<ProductDTO> getTopSellingPriceProducts() {
        return productRepo.findTopSellingProducts()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<ProductDTO> getTopProductsByCategory(Long categoryId) {
        return productRepo.findTopProductsByCategory(categoryId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<ProductDTO> getTopDiscountProducts() {
        return productRepo.findTopDiscountProducts()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<ProductDTO> getDiscountProductsByMainCategoryAsc(Long mainCategoryId) {
        return productRepo.findDiscountProductsByMainCategoryAsc(mainCategoryId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<ProductDTO> getProductsByMainCategoryAndExactDiscountPercentage(Long mainCategoryId, Double discountPercentage) {
        return productRepo.findProductsByMainCategoryAndExactDiscountPercentage(mainCategoryId, discountPercentage)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<ProductDTO> getProductsByMainCategoryWithDiscountLessThanEqual(Long mainCategoryId, Double maxDiscountPercentage) {
        return productRepo.findProductsByMainCategoryWithDiscountLessThanEqual(mainCategoryId, maxDiscountPercentage)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<ProductDTO> getSpotlightProductsByMainCategory(Long mainCategoryId) {
        return productRepo.findSpotlightProductsByMainCategory(mainCategoryId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<ProductDTO> getUniqueProductsByMainCategory(Long mainCategoryId) {
        return productRepo.findUniqueProductsByMainCategory(mainCategoryId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<ProductDTO> getTopCollectionsByMainCategory(Long mainCategoryId) {
        return productRepo.findTopCollectionsByMainCategory(mainCategoryId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<ProductDTO> getRecommendedProductsByMainCategory(Long mainCategoryId, Long userId, String sessionId) {
        return productRepo.findRecommendedProductsByMainCategory(mainCategoryId, userId, sessionId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<ProductDTO> getRecentlyViewedProductsByMainCategory(Long mainCategoryId, Long userId, String sessionId) {
        List<Long> productIds = productRepo.findRecentlyViewedProductIdsByMainCategory(mainCategoryId, userId, sessionId);
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return mapProductsByIdOrder(productIds);
    }

    @Override
    public List<ProductDTO> getRecentlyViewedProducts(Long userId, String sessionId) {
        List<Long> productIds = productRepo.findRecentlyViewedProductIds(userId, sessionId);
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return mapProductsByIdOrder(productIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getByMainCategory(Long mainCategoryId) {
        return productRepo.findByMainCategoryFull(mainCategoryId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public Page<ProductDTO> getFilteredProducts(ProductFilterRequestDTO filterRequest) {
        if (filterRequest.getMainCategoryId() != null
                && filterRequest.getGenders() != null
                && !filterRequest.getGenders().isEmpty()) {
            return getProductsByGenderBrowse(
                    filterRequest.getGenders().get(0),
                    filterRequest.getMainCategoryId(),
                    filterRequest.getPage(),
                    filterRequest.getSize(),
                    filterRequest.getSortBy(),
                    filterRequest.getSortDirection()
            );
        }

        Specification<Product> spec = ProductSpecification.filterProductsLegacy(
                resolveLegacyColorSizeTokens(filterRequest)
        );
        
        Pageable pageable = PageRequest.of(
            filterRequest.getPage(),
            filterRequest.getSize(),
            Sort.by(
                Sort.Direction.fromString(filterRequest.getSortDirection()),
                filterRequest.getSortBy()
            )
        );
        
        return productRepo.findAll(spec, pageable)
                .map(mapper::toDTO);
    }

    @Override
    public Page<ProductDTO> getSortedProducts(String sort, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<Product> products;

        if ("highToLow".equalsIgnoreCase(sort)) {

            products = productRepo.findHighToLow(pageable);

        } else if ("discount".equalsIgnoreCase(sort)) {

            products = productRepo.findDiscountProducts(pageable);

        } else {

            products = productRepo.findLowToHigh(pageable);
        }

        return products.map(mapper::toDTO);
    }
    
    @Override
    public FilterResponseDTO getFilteredProductsEnhanced(EnhancedProductFilterRequestDTO filterRequest) {
        Specification<Product> spec = ProductSpecification.filterProducts(
                resolveEnhancedColorSizeTokens(filterRequest)
        );
        
        Pageable pageable = PageRequest.of(
            filterRequest.getPage(),
            filterRequest.getSize(),
            Sort.by(
                Sort.Direction.fromString(filterRequest.getSortDirection()),
                filterRequest.getSortBy()
            )
        );
        
        Page<Product> productPage = productRepo.findAll(spec, pageable);
        
        FilterResponseDTO response = new FilterResponseDTO();
        response.setProducts(productPage.getContent().stream().map(mapper::toDTO).collect(java.util.stream.Collectors.toList()));
        response.setTotalProducts(productPage.getTotalElements());
        response.setCurrentPage(productPage.getNumber());
        response.setTotalPages(productPage.getTotalPages());
        response.setPageSize(productPage.getSize());
        response.setHasNext(productPage.hasNext());
        response.setHasPrevious(productPage.hasPrevious());
        
        // Create applied filters summary
        FilterResponseDTO.AppliedFiltersSummary summary = new FilterResponseDTO.AppliedFiltersSummary();
        summary.setTotalActiveFilters(calculateActiveFilters(filterRequest));
        response.setAppliedFilters(summary);
        
        return response;
    }
    
    private int calculateActiveFilters(EnhancedProductFilterRequestDTO request) {
        int count = 0;
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) count++;
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) count += request.getCategoryIds().size();
        if (request.getSubcategoryIds() != null && !request.getSubcategoryIds().isEmpty()) count += request.getSubcategoryIds().size();
        if (request.getGenders() != null && !request.getGenders().isEmpty()) count += request.getGenders().size();
        if (request.getColorIds() != null && !request.getColorIds().isEmpty()) count += request.getColorIds().size();
        if (request.getColorNames() != null && !request.getColorNames().isEmpty()) count += request.getColorNames().size();
        if (request.getSizeIds() != null && !request.getSizeIds().isEmpty()) count += request.getSizeIds().size();
        if (request.getSizeNames() != null && !request.getSizeNames().isEmpty()) count += request.getSizeNames().size();
        if (request.getMinPrice() != null || request.getMaxPrice() != null) count++;
        if (request.getMinRating() != null) count++;
        if (request.getSellerId() != null) count++;
        if (request.getInStock() != null && !request.getInStock()) count++;
        return count;
    }

    /**
     * Variants store color/size as catalog IDs (e.g. {@code "1"} for Red).
     * Expand request names/ids into match tokens before the JPA specification runs.
     */
    private EnhancedProductFilterRequestDTO resolveEnhancedColorSizeTokens(
            EnhancedProductFilterRequestDTO request
    ) {
        if (request == null) {
            return null;
        }
        List<String> colorTokens = sizeColorMapper.resolveColorVariantTokens(
                request.getColorIds(),
                request.getColorNames()
        );
        if (!colorTokens.isEmpty()) {
            request.setColorNames(colorTokens);
            request.setColorIds(null);
        }

        List<String> sizeTokens = sizeColorMapper.resolveSizeVariantTokens(
                request.getSizeIds(),
                request.getSizeNames()
        );
        if (!sizeTokens.isEmpty()) {
            request.setSizeNames(sizeTokens);
            request.setSizeIds(null);
        }
        return request;
    }

    private ProductFilterRequestDTO resolveLegacyColorSizeTokens(ProductFilterRequestDTO request) {
        if (request == null) {
            return null;
        }
        if (request.getColors() != null && !request.getColors().isEmpty()) {
            request.setColors(sizeColorMapper.resolveColorVariantTokens(null, request.getColors()));
        }
        if (request.getSizes() != null && !request.getSizes().isEmpty()) {
            request.setSizes(sizeColorMapper.resolveSizeVariantTokens(null, request.getSizes()));
        }
        return request;
    }

    @Override
    public Page<ProductDTO> getProductsByGenderBrowse(
            String gender,
            Long mainCategoryId,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        String normalized = GenderBrowseHelper.normalizeGenderLabel(gender);
        if (normalized.isBlank()) {
            return Page.empty();
        }

        Long resolvedMainCategoryId = mainCategoryId;
        if (resolvedMainCategoryId == null) {
            List<Category> mainCategories = categoryRepository.findByParentIdIsNull();
            resolvedMainCategoryId = GenderBrowseHelper.resolveMainCategoryId(normalized, mainCategories);
        }

        EnhancedProductFilterRequestDTO filterRequest = new EnhancedProductFilterRequestDTO();
        filterRequest.setPage(page);
        filterRequest.setSize(size);
        filterRequest.setSortBy(sortBy != null ? sortBy : "createdAt");
        filterRequest.setSortDirection(sortDirection != null ? sortDirection : "desc");
        filterRequest.setInStock(true);

        if (resolvedMainCategoryId != null) {
            filterRequest.setMainCategoryIds(List.of(resolvedMainCategoryId));
            if (GenderBrowseHelper.shouldFilterByProductGenderField(normalized)) {
                filterRequest.setGenders(List.of(normalized));
            }
        } else {
            filterRequest.setGenders(GenderBrowseHelper.genderFieldValues(normalized));
        }

        Specification<Product> spec = ProductSpecification.filterProducts(filterRequest);
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.fromString(filterRequest.getSortDirection()),
                        filterRequest.getSortBy()
                )
        );

        return productRepo.findAll(spec, pageable).map(mapper::toDTO);
    }

    @Override
    public List<String> getAllSizes() {
        List<String> sizeIdsOrNames = variantRepo.findAllDistinctSizes();
        return sizeIdsOrNames.stream()
                .map(sizeColorMapper::getSizeName)
                .distinct()
                .toList();
    }

    @Override
    public List<String> getAllColors() {
        List<String> colorIdsOrNames = variantRepo.findAllDistinctColors();
        return colorIdsOrNames.stream()
                .map(sizeColorMapper::getColorName)
                .distinct()
                .toList();
    }

    @Override
    public List<String> getSizesByProductId(Long productId) {
        List<String> sizeIdsOrNames = variantRepo.findDistinctSizesByProductId(productId);
        return sizeIdsOrNames.stream()
                .map(sizeColorMapper::getSizeName)
                .distinct()
                .toList();
    }

    @Override
    public List<String> getColorsByProductId(Long productId) {
        List<String> colorIdsOrNames = variantRepo.findDistinctColorsByProductId(productId);
        return colorIdsOrNames.stream()
                .map(sizeColorMapper::getColorName)
                .distinct()
                .toList();
    }

    private List<ProductDTO> mapProductsByIdOrder(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        List<Product> products = productRepo.findAllWithImagesAndVariantsByIdIn(productIds);
        Map<Long, ProductDTO> dtoById = new LinkedHashMap<>();
        products.stream()
                .filter(ProductCatalogVisibility::isVisibleToUsers)
                .forEach(product -> dtoById.put(product.getId(), mapper.toDTO(product)));
        return productIds.stream()
                .map(dtoById::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

//    private List<ProductDTO> mapProductsByIdOrder

    @Override
    public Page<ProductDTO> advancedSearch(String keyword, Pageable pageable) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return Page.empty(pageable);
        }

        String trimmed = keyword.trim();
        List<String> tokens = tokenizeSearchKeyword(trimmed);
        if (tokens.isEmpty()) {
            return productRepo.advancedSearch(trimmed, pageable).map(mapper::toDTO);
        }

        // Single simple non-color token → existing phrase LIKE is enough.
        if (tokens.size() == 1 && !isColorToken(tokens.get(0)) && aliasesForToken(tokens.get(0)).size() <= 1) {
            return productRepo.advancedSearch(tokens.get(0), pageable).map(mapper::toDTO);
        }

        // Multi-word queries ("mens t-shirt"): phrase LIKE fails because catalog names
        // are like "Men's … T-Shirt". Fetch candidates by the most selective token,
        // expand aliases (t-shirt/tshirt), then AND-filter all tokens in memory.
        String primary = selectPrimarySearchToken(tokens);
        int fetchSize = Math.min(500, Math.max(120, pageable.getPageSize() * 12));
        Pageable fetchPage = PageRequest.of(0, fetchSize);

        Map<Long, Product> byId = new LinkedHashMap<>();
        for (String probe : aliasesForToken(primary)) {
            for (Product product : productRepo.advancedSearch(probe, fetchPage).getContent()) {
                if (product.getId() != null) {
                    byId.putIfAbsent(product.getId(), product);
                }
            }
        }
        // Also try the raw phrase in case it uniquely matches.
        for (Product product : productRepo.advancedSearch(trimmed, fetchPage).getContent()) {
            if (product.getId() != null) {
                byId.putIfAbsent(product.getId(), product);
            }
        }

        List<Product> matched = byId.values().stream()
                .filter(product -> productMatchesSearchTokens(product, tokens))
                .toList();

        int start = (int) pageable.getOffset();
        if (start >= matched.size()) {
            return new PageImpl<>(List.of(), pageable, matched.size());
        }
        int end = Math.min(start + pageable.getPageSize(), matched.size());
        List<ProductDTO> slice = matched.subList(start, end).stream()
                .map(mapper::toDTO)
                .toList();
        return new PageImpl<>(slice, pageable, matched.size());
    }

    /** Split / normalize shopper queries into searchable tokens. */
    static List<String> tokenizeSearchKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String normalized = keyword.toLowerCase(Locale.ROOT)
                .replace('’', '\'')
                .replaceAll("\\b(men's|mens|man)\\b", "men")
                .replaceAll("\\b(women's|womens|woman|ladies|lady)\\b", "women")
                .replaceAll("\\b(kid's|kids|kid|boys?|girls?|children)\\b", "kids")
                .replaceAll("\\bt\\s*-?\\s*shirts?\\b", "tshirt")
                .replaceAll("\\btees?\\b", "tshirt")
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        Set<String> stop = Set.of(
                "a", "an", "the", "for", "of", "and", "or", "in", "on", "to", "with", "from"
        );
        List<String> tokens = new ArrayList<>();
        for (String part : normalized.split(" ")) {
            if (part.length() < 2 || stop.contains(part)) {
                continue;
            }
            tokens.add(part);
        }
        return tokens;
    }

    static String selectPrimarySearchToken(List<String> tokens) {
        // Prefer product-type tokens over gender/color so we don't pull the whole Men catalog.
        for (String token : tokens) {
            if (!isGenderToken(token) && !isColorToken(token) && token.length() >= 4) {
                return token;
            }
        }
        for (String token : tokens) {
            if (!isGenderToken(token) && !isColorToken(token)) {
                return token;
            }
        }
        return tokens.get(tokens.size() - 1);
    }

    private static final Set<String> COLOR_TOKENS = Set.of(
            "red", "blue", "green", "black", "white", "yellow", "pink", "purple",
            "orange", "brown", "grey", "gray", "navy", "maroon", "beige", "cream",
            "cyan", "gold", "silver", "multicolor", "multi", "olive", "khaki",
            "burgundy", "crimson", "mustard", "teal", "coral", "ivory", "charcoal"
    );

    static boolean isColorToken(String token) {
        return token != null && COLOR_TOKENS.contains(token.toLowerCase(Locale.ROOT));
    }

    static List<String> aliasesForToken(String token) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (token == null || token.isBlank()) {
            return List.of();
        }
        out.add(token);
        if ("tshirt".equals(token)) {
            out.add("t-shirt");
            out.add("tshirts");
            out.add("tee");
            out.add("tees");
        } else if ("men".equals(token)) {
            out.add("mens");
            out.add("man's");
            out.add("men's");
        } else if ("women".equals(token)) {
            out.add("womens");
            out.add("woman");
            out.add("women's");
            out.add("ladies");
        }
        return new ArrayList<>(out);
    }

    static boolean isGenderToken(String token) {
        return "men".equals(token) || "women".equals(token) || "kids".equals(token)
                || "unisex".equals(token);
    }

    private boolean productMatchesSearchTokens(Product product, List<String> tokens) {
        String gender = normalizeSearchBlob(product.getGender());
        String blob = normalizeSearchBlob(String.join(" ",
                nullToEmpty(product.getName()),
                nullToEmpty(product.getShortDescription()),
                nullToEmpty(product.getDescription()),
                nullToEmpty(product.getFeatures()),
                nullToEmpty(product.getGender())
        ));
        for (String token : tokens) {
            if (isColorToken(token)) {
                if (!productHasColorToken(product, token, blob)) {
                    return false;
                }
                continue;
            }
            if (!tokenMatchesProduct(token, blob, gender)) {
                return false;
            }
        }
        return true;
    }

    private boolean productHasColorToken(Product product, String colorToken, String textBlob) {
        List<String> needles = colorAliases(colorToken);
        String paddedBlob = " " + textBlob + " ";
        for (String needle : needles) {
            String n = normalizeSearchBlob(needle);
            if (n.isEmpty()) {
                continue;
            }
            if (paddedBlob.contains(" " + n + " ") || paddedBlob.contains(n)) {
                // Prefer whole-word-ish hits for short color names inside blob.
                if (n.length() >= 3 && paddedBlob.contains(" " + n + " ")) {
                    return true;
                }
                if (n.length() >= 4 && textBlob.contains(n)) {
                    return true;
                }
            }
        }

        if (product.getId() == null) {
            return false;
        }
        List<String> colorIdsOrNames = variantRepo.findDistinctColorsByProductId(product.getId());
        for (String raw : colorIdsOrNames) {
            String resolved = normalizeSearchBlob(sizeColorMapper.getColorName(raw));
            String rawNorm = normalizeSearchBlob(raw);
            for (String needle : needles) {
                String n = normalizeSearchBlob(needle);
                if (n.isEmpty()) {
                    continue;
                }
                if (resolved.equals(n) || resolved.contains(n) || rawNorm.equals(n) || rawNorm.contains(n)) {
                    return true;
                }
            }
        }
        return false;
    }

    static List<String> colorAliases(String token) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (token == null || token.isBlank()) {
            return List.of();
        }
        String t = token.toLowerCase(Locale.ROOT).trim();
        out.add(t);
        switch (t) {
            case "red" -> {
                out.add("maroon");
                out.add("burgundy");
                out.add("crimson");
            }
            case "blue" -> {
                out.add("navy");
                out.add("indigo");
                out.add("teal");
            }
            case "green" -> {
                out.add("olive");
                out.add("emerald");
            }
            case "grey", "gray" -> {
                out.add("grey");
                out.add("gray");
                out.add("charcoal");
                out.add("silver");
            }
            case "yellow" -> {
                out.add("mustard");
                out.add("gold");
            }
            case "white" -> {
                out.add("ivory");
                out.add("cream");
            }
            case "pink" -> {
                out.add("rose");
                out.add("magenta");
            }
            case "brown" -> {
                out.add("tan");
                out.add("khaki");
                out.add("beige");
            }
            case "beige", "cream" -> {
                out.add("beige");
                out.add("cream");
                out.add("tan");
            }
            case "multicolor", "multi" -> {
                out.add("multicolor");
                out.add("multi");
                out.add("printed");
                out.add("floral");
            }
            default -> {
            }
        }
        return new ArrayList<>(out);
    }

    private static boolean tokenMatchesProduct(String token, String blob, String gender) {
        if ("men".equals(token)) {
            if (gender.contains("women")) {
                return false;
            }
            if (gender.contains("men") || gender.contains("male") || gender.contains("unisex")) {
                return true;
            }
            // Avoid matching "women" / "womens" via substring "men".
            if (blob.contains("women")) {
                return blob.contains(" men ") || blob.startsWith("men ") || blob.contains("mens");
            }
            return blob.contains("men");
        }
        if ("women".equals(token)) {
            return gender.contains("women") || gender.contains("female") || gender.contains("lady")
                    || gender.contains("unisex")
                    || blob.contains("women") || blob.contains("lady") || blob.contains("ladies");
        }
        if ("kids".equals(token)) {
            return gender.contains("kid") || gender.contains("boy") || gender.contains("girl")
                    || gender.contains("child") || gender.contains("unisex")
                    || blob.contains("kid") || blob.contains("boy") || blob.contains("girl")
                    || blob.contains("child");
        }
        for (String alias : aliasesForToken(token)) {
            String needle = normalizeSearchBlob(alias);
            if (!needle.isEmpty() && blob.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeSearchBlob(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.toLowerCase(Locale.ROOT)
                .replace('’', '\'')
                .replace("'", "")
                .replace("-", "")
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

}
