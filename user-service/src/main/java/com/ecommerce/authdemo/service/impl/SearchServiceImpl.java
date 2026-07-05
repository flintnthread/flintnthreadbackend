package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.SearchResponseDTO;
import com.ecommerce.authdemo.entity.Category;
import com.ecommerce.authdemo.entity.Product;
import com.ecommerce.authdemo.entity.SearchHistory;
import com.ecommerce.authdemo.entity.SubCategory;
import com.ecommerce.authdemo.entity.User;
import com.ecommerce.authdemo.payload.ApiResponse;
import com.ecommerce.authdemo.repository.CategoryRepository;
import com.ecommerce.authdemo.repository.ProductImageSignatureRepository;
import com.ecommerce.authdemo.repository.ProductRepository;
import com.ecommerce.authdemo.repository.SearchHistoryRepository;
import com.ecommerce.authdemo.repository.SubCategoryRepository;
import com.ecommerce.authdemo.service.AIImageService;
import com.ecommerce.authdemo.service.SearchService;
import com.ecommerce.authdemo.service.impl.AIImageSearchDecorator;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private static final Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);
    private static final long MAX_IMAGE_BYTES = 8L * 1024L * 1024L;
    private static final String ACTIVE_STATUS = "active";
    private static final Set<String> STOP_WORDS = Set.of(
            "img", "image", "photo", "camera", "snap", "capture", "upload", "search", "product"
    );
    private static final Set<String> COLOR_KEYWORDS = Set.of(
            "black", "white", "grey", "gray", "brown", "beige", "red", "green", "blue",
            "purple", "yellow", "pink", "multicolor", "orange"
    );
    private static final int MAX_ONLINE_HASH_COMPUTES_PER_REQUEST = 20;
    private volatile boolean signatureTableAvailable = true;

    @Value("${app.media.public-base-url:}")
    private String mediaPublicBaseUrl;

    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final ProductRepository productRepository;
    private final ProductImageSignatureRepository productImageSignatureRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final JdbcTemplate jdbcTemplate;
    private final AIImageService aiImageService;
    private final AIImageSearchDecorator aiImageSearchDecorator;

    /*
    -----------------------------------------
    🔎 MAIN SEARCH
    -----------------------------------------
    */
    @Override
    public ApiResponse<SearchResponseDTO> search(String keyword, Long userId, String sessionId) {

        if (!StringUtils.hasText(keyword)) {
            return new ApiResponse<>(false, "Search keyword cannot be empty", null);
        }

        keyword = keyword.trim();

        try {
            saveSearchHistory(keyword, userId, sessionId);
        } catch (Exception e) {
            log.warn("Search history save failed (non-blocking): {}", e.getMessage());
        }

        List<Category> categories = new ArrayList<>(
                categoryRepository.findTop10ByCategoryNameContainingIgnoreCase(keyword));

        List<SubCategory> subCategories = new ArrayList<>(
                subCategoryRepository.findTop10BySubcategoryNameContainingIgnoreCase(keyword));

        String normalizedKeyword = keyword.replaceAll("\\s+", "");
        if (categories.isEmpty() && keyword.contains(" ")) {
            List<Category> joinedMatch =
                    categoryRepository.findTop10ByCategoryNameContainingIgnoreCase(normalizedKeyword);
            categories.addAll(joinedMatch);

            String[] words = keyword.split("\\s+");
            for (String word : words) {
                if (word.length() >= 3) {
                    List<Category> wordCats =
                            categoryRepository.findTop10ByCategoryNameContainingIgnoreCase(word);
                    for (Category c : wordCats) {
                        if (categories.stream().noneMatch(existing -> existing.getId().equals(c.getId()))) {
                            categories.add(c);
                        }
                    }
                    List<SubCategory> wordSubs =
                            subCategoryRepository.findTop10BySubcategoryNameContainingIgnoreCase(word);
                    for (SubCategory s : wordSubs) {
                        if (subCategories.stream().noneMatch(existing -> existing.getId().equals(s.getId()))) {
                            subCategories.add(s);
                        }
                    }
                }
            }
        }

        List<Product> products;
        try {
            products = productRepository.fullTextSearch(keyword);
        } catch (Exception e) {
            log.warn("fullTextSearch failed, falling back to name search: {}", e.getMessage());
            products = productRepository.findTop20ByNameContainingIgnoreCaseAndStatus(keyword, "active");
        }

        SearchResponseDTO response =
                new SearchResponseDTO(categories, subCategories, products);

        return new ApiResponse<>(true, "Search results fetched successfully", response);
    }

    /*
    -----------------------------------------
    🔍 SEARCH SUGGESTIONS
    -----------------------------------------
    */
    @Override
    public ApiResponse<List<String>> getSuggestions(String keyword) {

        if (!StringUtils.hasText(keyword)) {
            return new ApiResponse<>(false, "Keyword required", null);
        }

        keyword = keyword.trim();

        List<String> suggestions;
        try {
            suggestions = productRepository.findSuggestionsByKeyword(keyword);
        } catch (Exception e) {
            log.warn("findSuggestionsByKeyword failed, falling back: {}", e.getMessage());
            suggestions = productRepository.findTop5ByNameContainingIgnoreCase(keyword)
                    .stream()
                    .map(Product::getName)
                    .distinct()
                    .collect(Collectors.toList());
        }

        return new ApiResponse<>(true, "Suggestions fetched successfully", suggestions);
    }

    /*
    -----------------------------------------
    🔥 TRENDING SEARCHES
    -----------------------------------------
    */
    @Override
    public ApiResponse<List<String>> getTrendingSearches() {

        List<String> trending =
                productRepository.findTop10ByOrderByIdDesc()
                        .stream()
                        .map(Product::getName)
                        .collect(Collectors.toList());

        return new ApiResponse<>(true, "Trending searches fetched", trending);
    }

    /*
    -----------------------------------------
    📄 SEARCH WITH PAGINATION
    -----------------------------------------
    */
    @Override
    public ApiResponse<Page<SearchResponseDTO>> searchWithPagination(
            String keyword,
            Long userId,
            String sessionId,
            int page,
            int size) {

        if (!StringUtils.hasText(keyword)) {
            return new ApiResponse<>(false, "Search keyword cannot be empty", null);
        }

        keyword = keyword.trim();

        try {
            saveSearchHistory(keyword, userId, sessionId);
        } catch (Exception e) {
            log.warn("Search history save failed (non-blocking): {}", e.getMessage());
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<Product> productPage;
        try {
            productPage = productRepository.fullTextSearchPaged(keyword, pageable);
        } catch (Exception e) {
            log.warn("fullTextSearchPaged failed, falling back: {}", e.getMessage());
            productPage = productRepository.findByNameContainingIgnoreCaseAndStatus(
                    keyword, "active",
                    PageRequest.of(page, size, Sort.by("createdAt").descending())
            );
        }

        Page<SearchResponseDTO> responsePage =
                productPage.map(product ->
                        new SearchResponseDTO(
                                Collections.emptyList(),
                                Collections.emptyList(),
                                List.of(product)
                        )
                );

        return new ApiResponse<>(true, "Search results fetched successfully", responsePage);
    }

    /*
    -----------------------------------------
    🎤 VOICE SEARCH
    -----------------------------------------
    */
    @Override
    public ApiResponse<SearchResponseDTO> voiceSearch(String keyword, Long userId, String sessionId) {

        if (!StringUtils.hasText(keyword)) {
            return new ApiResponse<>(false, "Voice keyword required", null);
        }

        keyword = keyword.trim();

        try {
            saveSearchHistory(keyword, userId, sessionId);
        } catch (Exception e) {
            log.warn("Voice search history save failed (non-blocking): {}", e.getMessage());
        }

        List<Product> products;
        try {
            products = productRepository.fullTextSearch(keyword);
        } catch (Exception e) {
            log.warn("fullTextSearch for voice failed, falling back: {}", e.getMessage());
            products = productRepository.findTop20ByNameContainingIgnoreCaseAndStatus(keyword, "active");
        }

        if (products.isEmpty()) {
            return new ApiResponse<>(false, "No product found", null);
        }

        SearchResponseDTO response =
                new SearchResponseDTO(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        products
                );

        return new ApiResponse<>(true, "Voice search result", response);
    }

    @Override
    public ApiResponse<SearchResponseDTO> imageSearch(MultipartFile image, Long userId, String sessionId) {
        log.info("Starting camera search");

        try {
            if (image == null || image.isEmpty()) {
                return new ApiResponse<>(false, "Image file is required", null);
            }

            if (!StringUtils.hasText(image.getContentType()) || !image.getContentType().toLowerCase(Locale.ROOT).startsWith("image/")) {
                return new ApiResponse<>(false, "Only image files are supported", null);
            }

            if (image.getSize() > MAX_IMAGE_BYTES) {
                return new ApiResponse<>(false, "Image size should be less than 8MB", null);
            }

            ApiResponse<SearchResponseDTO> aiResponse =
                    aiImageSearchDecorator.performImageSearch(image, userId, sessionId);
            List<Product> aiProducts = extractSearchProducts(aiResponse);
            if (aiResponse.isSuccess() && !aiProducts.isEmpty()) {
                return aiResponse;
            }

            log.info("AI camera search unavailable or had no matches — running local visual similarity search");
            List<Product> localMatches = findSimilarProductsByVisualSignature(image);
            if (!localMatches.isEmpty()) {
                SearchResponseDTO response = new SearchResponseDTO(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        localMatches
                );
                return new ApiResponse<>(true, "Camera search completed successfully", response);
            }

            if (aiResponse.isSuccess()) {
                return aiResponse;
            }

            return new ApiResponse<>(
                    true,
                    "No similar products found for this image.",
                    new SearchResponseDTO(
                            Collections.emptyList(),
                            Collections.emptyList(),
                            Collections.emptyList()
                    )
            );

        } catch (Exception e) {
            log.error("Error during camera search", e);
            return new ApiResponse<>(false, "Camera search failed: " + e.getMessage(), null);
        }
    }

    private List<Product> extractSearchProducts(ApiResponse<SearchResponseDTO> response) {
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return Collections.emptyList();
        }
        List<Product> products = response.getData().getProducts();
        return products == null ? Collections.emptyList() : products;
    }

    private List<Product> findSimilarProductsByVisualSignature(MultipartFile image) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(image.getInputStream());
        if (bufferedImage == null) {
            return Collections.emptyList();
        }

        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        keywords.addAll(extractKeywordsFromFilename(image.getOriginalFilename()));
        keywords.addAll(extractDominantColorKeywords(bufferedImage));
        keywords = expandKeywords(keywords);
        if (keywords.isEmpty()) {
            return Collections.emptyList();
        }

        Long uploadedHash = computeDHash(bufferedImage);
        String uploadedStem = toFileStem(image.getOriginalFilename());
        List<Product> candidates = fetchCandidateProducts(keywords);
        return rankProductsByImageKeywords(candidates, keywords, uploadedStem, uploadedHash);
    }

    @Override
    public ApiResponse<List<String>> getSearchHistory(Long userId, String sessionId) {
        if (userId == null && !StringUtils.hasText(sessionId)) {
            return new ApiResponse<>(false, "userId or sessionId is required", List.of());
        }

        List<SearchHistory> entries = userId != null
                ? searchHistoryRepository.findTop20ByUserIdOrderBySearchedAtDesc(userId)
                : searchHistoryRepository.findTop20BySessionIdOrderBySearchedAtDesc(sessionId.trim());

        LinkedHashSet<String> uniqueKeywords = new LinkedHashSet<>();
        for (SearchHistory entry : entries) {
            if (StringUtils.hasText(entry.getKeyword())) {
                uniqueKeywords.add(entry.getKeyword().trim());
            }
        }

        return new ApiResponse<>(
                true,
                "Search history fetched successfully",
                new ArrayList<>(uniqueKeywords)
        );
    }

    @Override
    @Transactional
    public ApiResponse<String> clearSearchHistory(Long userId, String sessionId) {
        if (userId == null && !StringUtils.hasText(sessionId)) {
            return new ApiResponse<>(false, "userId or sessionId is required", null);
        }

        if (userId != null) {
            searchHistoryRepository.deleteByUserId(userId);
        } else {
            searchHistoryRepository.deleteBySessionId(sessionId.trim());
        }

        return new ApiResponse<>(true, "Search history cleared successfully", "CLEARED");
    }

    @Override
    public ApiResponse<String> recordSearchHistory(String keyword, Long userId, String sessionId) {
        if (!StringUtils.hasText(keyword)) {
            return new ApiResponse<>(false, "Keyword is required", null);
        }
        if (userId == null && !StringUtils.hasText(sessionId)) {
            return new ApiResponse<>(false, "userId or sessionId is required", null);
        }
        try {
            saveSearchHistory(keyword.trim(), userId, sessionId);
            return new ApiResponse<>(true, "Search recorded", "OK");
        } catch (Exception e) {
            log.warn("Search history record failed: {}", e.getMessage());
            return new ApiResponse<>(false, "Failed to record search", null);
        }
    }

    private void saveSearchHistory(String keyword, Long userId, String sessionId) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }

        SearchHistory history = new SearchHistory();
        history.setKeyword(keyword.trim());

        if (userId != null) {
            User user = new User();
            user.setId(userId);
            history.setUser(user);
        } else if (StringUtils.hasText(sessionId)) {
            history.setSessionId(sessionId.trim());
        } else {
            return;
        }

        try {
            searchHistoryRepository.save(history);
        } catch (Exception ex) {
            log.warn("Skipping search history persistence: {}",
                    ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
        }
    }

    private Set<String> extractKeywordsFromFilename(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return Set.of();
        }

        String normalized = originalFilename.toLowerCase(Locale.ROOT)
                .replaceAll("\\.[a-z0-9]{2,6}$", "")
                .replaceAll("[^a-z0-9]+", " ")
                .trim();

        if (!StringUtils.hasText(normalized)) {
            return Set.of();
        }

        return Arrays.stream(normalized.split("\\s+"))
                .filter(token -> token.length() >= 3)
                .filter(token -> !STOP_WORDS.contains(token))
                .limit(8)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> extractDominantColorKeywords(MultipartFile imageFile) throws IOException {
        BufferedImage image = ImageIO.read(imageFile.getInputStream());
        if (image == null) {
            return Set.of();
        }
        return extractDominantColorKeywords(image);
    }

    private Set<String> extractDominantColorKeywords(BufferedImage image) {
        Map<String, Integer> colorHits = new HashMap<>();
        int width = image.getWidth();
        int height = image.getHeight();
        int stepX = Math.max(width / 30, 1);
        int stepY = Math.max(height / 30, 1);

        for (int y = 0; y < height; y += stepY) {
            for (int x = 0; x < width; x += stepX) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                String color = toColorKeyword(r, g, b);
                colorHits.merge(color, 1, Integer::sum);
            }
        }

        return colorHits.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String toColorKeyword(int r, int g, int b) {
        if (r < 45 && g < 45 && b < 45) return "black";
        if (r > 220 && g > 220 && b > 220) return "white";
        if (Math.abs(r - g) < 15 && Math.abs(g - b) < 15) return "grey";
        if (r > 160 && g > 120 && b < 90) return "brown";
        if (r > 200 && g > 180 && b < 120) return "beige";
        if (r > g + 40 && r > b + 40) return "red";
        if (g > r + 40 && g > b + 30) return "green";
        if (b > r + 30 && b > g + 30) return "blue";
        if (r > 180 && b > 160 && g < 120) return "purple";
        if (r > 200 && g > 160 && b < 110) return "yellow";
        if (r > 200 && g > 120 && b > 120) return "pink";
        return "multicolor";
    }

    private Set<String> expandKeywords(Set<String> keywords) {
        LinkedHashSet<String> expanded = new LinkedHashSet<>(keywords);
        for (String keyword : keywords) {
            String k = keyword.toLowerCase(Locale.ROOT).trim();
            if ("grey".equals(k)) expanded.add("gray");
            if ("gray".equals(k)) expanded.add("grey");
            if ("multicolor".equals(k)) {
                expanded.add("printed");
                expanded.add("floral");
                expanded.add("striped");
                expanded.add("checks");
            }
            if ("beige".equals(k) || "brown".equals(k)) {
                expanded.add("tan");
                expanded.add("khaki");
            }
            if ("red".equals(k)) expanded.add("maroon");
            if ("blue".equals(k)) expanded.add("navy");
            if ("green".equals(k)) expanded.add("olive");
            if ("pink".equals(k)) expanded.add("rose");
        }
        return expanded;
    }

    private List<Product> fetchCandidateProducts(Set<String> keywords) {
        Set<Long> seenIds = new HashSet<>();
        List<Product> candidates = new ArrayList<>();

        for (String keyword : keywords) {
            List<Product> matches = productRepository.findTop20ByNameContainingIgnoreCaseAndStatus(keyword, ACTIVE_STATUS);
            for (Product product : matches) {
                if (product.getId() != null && seenIds.add(product.getId())) {
                    candidates.add(product);
                }
            }
        }

        // Always blend with recent catalog so color/spec/model matches can still be found
        for (Product product : productRepository.findTop300ByStatusOrderByCreatedAtDesc(ACTIVE_STATUS)) {
            if (product.getId() != null && seenIds.add(product.getId())) {
                candidates.add(product);
            }
        }

        return candidates;
    }

    private List<Product> rankProductsByImageKeywords(
            List<Product> candidates,
            Set<String> keywords,
            String uploadedNameStem,
            Long uploadedImageHash
    ) {
        if (candidates.isEmpty() || keywords.isEmpty()) {
            return candidates.stream().limit(20).toList();
        }

        Map<Long, Integer> scoreMap = new HashMap<>();
        Map<String, Long> pathHashCache = new HashMap<>();
        Map<String, Long> persistedHashMap = loadPersistedHashes(candidates);
        Map<Long, Integer> bestImageDistanceByProduct = new HashMap<>();
        int[] remainingHashBudget = new int[]{MAX_ONLINE_HASH_COMPUTES_PER_REQUEST};

        for (Product product : candidates) {
            if (product.getId() == null) {
                continue;
            }
            String searchableText = buildSearchText(product);
            Set<String> productTokens = splitTokens(searchableText);
            int score = 0;
            int exactTokenHits = 0;
            int fuzzyHits = 0;
            int colorHits = 0;
            int modelHits = 0;

            for (String keyword : keywords) {
                if (!StringUtils.hasText(keyword)) continue;
                String normalizedKeyword = keyword.toLowerCase(Locale.ROOT).trim();

                if (containsWord(product.getName(), normalizedKeyword)) score += 16;
                if (containsToken(product.getName(), normalizedKeyword)) score += 10;
                if (containsToken(product.getShortDescription(), normalizedKeyword)) score += 7;
                if (containsToken(product.getDescription(), normalizedKeyword)) score += 5;
                if (containsToken(product.getFeatures(), normalizedKeyword)) score += 6;
                if (containsToken(product.getSpecifications(), normalizedKeyword)) score += 8;
                if (searchableText.contains(" " + normalizedKeyword + " ")) score += 3;

                if (productTokens.contains(normalizedKeyword)) {
                    exactTokenHits++;
                    score += 12;
                } else if (hasFuzzyTokenMatch(normalizedKeyword, productTokens)) {
                    fuzzyHits++;
                    score += 6;
                }

                if (COLOR_KEYWORDS.contains(normalizedKeyword) && searchableText.contains(normalizedKeyword)) {
                    colorHits++;
                    score += 10;
                }

                if (isModelToken(normalizedKeyword) && searchableText.contains(normalizedKeyword)) {
                    modelHits++;
                    score += 14;
                }
            }

            if (exactTokenHits > 0) score += 8;
            if (fuzzyHits > 0) score += 4;
            if (colorHits > 0) score += 10;
            if (modelHits > 0) score += 12;

            if (exactTokenHits == 0 && fuzzyHits == 0 && colorHits == 0 && modelHits == 0) {
                // allow very soft match using overlap with product attributes
                int overlap = tokenOverlapCount(productTokens, keywords);
                score += overlap * 2;
            }

            // If uploaded image name resembles product image filename, strongly boost.
            if (StringUtils.hasText(uploadedNameStem)) {
                String productImageStem = getFirstProductImageStem(product);
                if (StringUtils.hasText(productImageStem) && productImageStem.equalsIgnoreCase(uploadedNameStem)) {
                    score += 140;
                }
            }

            // Perceptual hash comparison for "same image / near-identical image" ranking boost.
            if (uploadedImageHash != null) {
                int bestDistance = computeBestImageDistance(
                        product,
                        uploadedImageHash,
                        pathHashCache,
                        persistedHashMap,
                        remainingHashBudget
                );
                if (bestDistance >= 0) {
                    bestImageDistanceByProduct.put(product.getId(), bestDistance);
                    if (bestDistance <= 6) {
                        score += 320;
                    } else if (bestDistance <= 10) {
                        score += 220;
                    } else if (bestDistance <= 14) {
                        score += 140;
                    } else if (bestDistance <= 18) {
                        score += 80;
                    }
                }
            }

            if (score > 0) {
                scoreMap.put(product.getId(), score);
            }
        }

        // If we have strong visual matches, force them to the top (prevents color-only false positives like sarees).
        List<Product> strongVisualMatches = candidates.stream()
                .filter(product -> product.getId() != null)
                .filter(product -> {
                    Integer distance = bestImageDistanceByProduct.get(product.getId());
                    return distance != null && distance <= 12;
                })
                .sorted(Comparator
                        .comparingInt((Product product) -> bestImageDistanceByProduct.get(product.getId()))
                        .thenComparing(Product::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        if (!strongVisualMatches.isEmpty()) {
            return diversifyByCategory(strongVisualMatches, 20, 4);
        }

        List<Product> sortedByScore = candidates.stream()
                .filter(product -> product.getId() != null && scoreMap.containsKey(product.getId()))
                .sorted(Comparator
                        .comparingInt((Product product) -> scoreMap.get(product.getId())).reversed()
                        .thenComparing(Product::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return diversifyByCategory(sortedByScore, 20, 3);
    }

    private List<Product> diversifyByCategory(List<Product> products, int limit, int maxPerCategory) {
        if (products.isEmpty()) return List.of();

        List<Product> diversified = new ArrayList<>();
        Map<Integer, Integer> categoryCount = new HashMap<>();
        List<Product> overflow = new ArrayList<>();

        for (Product product : products) {
            Integer categoryId = product.getCategoryId();
            int count = categoryCount.getOrDefault(categoryId, 0);
            if (count < maxPerCategory) {
                diversified.add(product);
                categoryCount.put(categoryId, count + 1);
            } else {
                overflow.add(product);
            }
            if (diversified.size() >= limit) return diversified;
        }

        for (Product product : overflow) {
            diversified.add(product);
            if (diversified.size() >= limit) break;
        }

        return diversified;
    }

    private String getFirstProductImagePath(Product product) {
        if (product.getImages() == null || product.getImages().isEmpty()) return null;
        return product.getImages().stream()
                .sorted(Comparator
                        .comparing((com.ecommerce.authdemo.entity.ProductImage img) -> img.getSortOrder() == null ? Integer.MAX_VALUE : img.getSortOrder())
                        .thenComparing(img -> img.getId() == null ? Long.MAX_VALUE : img.getId()))
                .map(com.ecommerce.authdemo.entity.ProductImage::getImagePath)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private int computeBestImageDistance(
            Product product,
            long uploadedImageHash,
            Map<String, Long> pathHashCache,
            Map<String, Long> persistedHashMap,
            int[] remainingHashBudget
    ) {
        if (product.getImages() == null || product.getImages().isEmpty()) return -1;
        int best = Integer.MAX_VALUE;
        for (com.ecommerce.authdemo.entity.ProductImage image : product.getImages()) {
            String path = image != null ? image.getImagePath() : null;
            if (!StringUtils.hasText(path)) continue;
            Long productHash = persistedHashMap.get(path);
            if (productHash == null) {
                productHash = pathHashCache.get(path);
            }
            if (productHash == null) {
                if (remainingHashBudget[0] <= 0) {
                    continue;
                }
                remainingHashBudget[0]--;
                productHash = computeProductImageHash(path);
                if (productHash != null) {
                    pathHashCache.put(path, productHash);
                    persistedHashMap.put(path, productHash);
                    persistImageHash(product.getId(), path, productHash);
                }
            }
            if (productHash == null) continue;
            int hamming = Long.bitCount(uploadedImageHash ^ productHash);
            if (hamming < best) best = hamming;
            if (best == 0) break;
        }
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    private String getFirstProductImageStem(Product product) {
        return toFileStem(getFirstProductImagePath(product));
    }

    private String toFileStem(String path) {
        if (!StringUtils.hasText(path)) return "";
        String clean = path;
        int slash = Math.max(clean.lastIndexOf('/'), clean.lastIndexOf('\\'));
        if (slash >= 0 && slash < clean.length() - 1) {
            clean = clean.substring(slash + 1);
        }
        int q = clean.indexOf('?');
        if (q > -1) clean = clean.substring(0, q);
        int dot = clean.lastIndexOf('.');
        if (dot > 0) clean = clean.substring(0, dot);
        return clean.trim().toLowerCase(Locale.ROOT);
    }

    private Long computeProductImageHash(String imagePath) {
        if (!StringUtils.hasText(imagePath)) return null;
        String path = resolveImageUrl(imagePath.trim());
        if (!StringUtils.hasText(path)) return null;
        try {
            URLConnection conn = new URL(path).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            try (var in = conn.getInputStream()) {
                BufferedImage img = ImageIO.read(in);
                if (img == null) return null;
                return computeDHash(img);
            }
        } catch (Exception ex) {
            return null;
        }
    }

    private String resolveImageUrl(String rawPath) {
        if (!StringUtils.hasText(rawPath)) return null;
        String path = rawPath.trim();
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }

        String base = StringUtils.hasText(mediaPublicBaseUrl) ? mediaPublicBaseUrl.trim() : "";
        if (!StringUtils.hasText(base)) {
            return null;
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (path.startsWith("/")) {
            return base + path;
        }
        return base + "/" + path;
    }

    private Map<String, Long> loadPersistedHashes(List<Product> candidates) {
        if (!signatureTableAvailable) {
            return new HashMap<>();
        }

        Set<String> paths = new HashSet<>();
        for (Product product : candidates) {
            if (product.getImages() == null) continue;
            for (com.ecommerce.authdemo.entity.ProductImage image : product.getImages()) {
                if (image != null && StringUtils.hasText(image.getImagePath())) {
                    paths.add(image.getImagePath().trim());
                }
            }
        }
        if (paths.isEmpty()) return new HashMap<>();

        Map<String, Long> hashMap = new HashMap<>();
        try {
            productImageSignatureRepository.findByImagePathIn(paths).forEach(signature -> {
                Long hash = parseHexHash(signature.getDHashHex());
                if (hash != null && StringUtils.hasText(signature.getImagePath())) {
                    hashMap.put(signature.getImagePath().trim(), hash);
                }
            });
        } catch (DataAccessException ex) {
            if (tryEnsureSignatureTable(ex)) {
                try {
                    productImageSignatureRepository.findByImagePathIn(paths).forEach(signature -> {
                        Long hash = parseHexHash(signature.getDHashHex());
                        if (hash != null && StringUtils.hasText(signature.getImagePath())) {
                            hashMap.put(signature.getImagePath().trim(), hash);
                        }
                    });
                    return hashMap;
                } catch (DataAccessException retryEx) {
                    signatureTableAvailable = false;
                    log.warn("Image signature table unavailable after auto-create attempt: {}", retryEx.getMostSpecificCause() != null
                            ? retryEx.getMostSpecificCause().getMessage()
                            : retryEx.getMessage());
                    return new HashMap<>();
                }
            }
            signatureTableAvailable = false;
            log.warn("Image signature table unavailable, continuing without persisted hashes: {}", ex.getMostSpecificCause() != null
                    ? ex.getMostSpecificCause().getMessage()
                    : ex.getMessage());
            return new HashMap<>();
        }
        return hashMap;
    }

    private void persistImageHash(Long productId, String imagePath, long hash) {
        if (!signatureTableAvailable) return;
        if (productId == null || !StringUtils.hasText(imagePath)) return;
        String normalizedPath = imagePath.trim();
        String hashHex = String.format("%016x", hash);
        try {
            jdbcTemplate.update("""
                INSERT INTO product_image_signatures (product_id, image_path, dhash_hex, created_at, updated_at)
                VALUES (?, ?, ?, NOW(), NOW())
                ON DUPLICATE KEY UPDATE
                    product_id = VALUES(product_id),
                    dhash_hex = VALUES(dhash_hex),
                    updated_at = NOW()
            """, productId, normalizedPath, hashHex);
        } catch (DataAccessException ex) {
            signatureTableAvailable = false;
            log.debug("Skipping image hash persistence for {}: {}", normalizedPath, ex.getMessage());
        } catch (Exception ex) {
            log.debug("Skipping image hash persistence for {}: {}", normalizedPath, ex.getMessage());
        }
    }

    private Long parseHexHash(String hashHex) {
        if (!StringUtils.hasText(hashHex)) return null;
        try {
            return Long.parseUnsignedLong(hashHex.trim(), 16);
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean tryEnsureSignatureTable(DataAccessException ex) {
        String msg = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        if (msg == null) return false;
        String lower = msg.toLowerCase(Locale.ROOT);
        if (!lower.contains("product_image_signatures") || !lower.contains("doesn't exist")) {
            return false;
        }
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS product_image_signatures (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    product_id BIGINT NOT NULL,
                    image_path VARCHAR(1024) NOT NULL,
                    dhash_hex CHAR(16) NOT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id)
                )
            """);
            try {
                jdbcTemplate.execute("ALTER TABLE product_image_signatures ADD UNIQUE INDEX uk_image_path_signature (image_path)");
            } catch (Exception ignored) {
                // Index already exists.
            }
            try {
                jdbcTemplate.execute("ALTER TABLE product_image_signatures ADD INDEX idx_product_image_signatures_product_id (product_id)");
            } catch (Exception ignored) {
                // Index already exists.
            }
            signatureTableAvailable = true;
            log.info("Auto-created missing table: product_image_signatures");
            return true;
        } catch (Exception createEx) {
            log.warn("Failed to auto-create product_image_signatures table: {}", createEx.getMessage());
            return false;
        }
    }

    private long computeDHash(BufferedImage source) {
        BufferedImage resized = new BufferedImage(9, 8, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = resized.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source, 0, 0, 9, 8, null);
        } finally {
            g.dispose();
        }

        long hash = 0L;
        int bit = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int left = resized.getRGB(x, y) & 0xFF;
                int right = resized.getRGB(x + 1, y) & 0xFF;
                if (left > right) {
                    hash |= (1L << bit);
                }
                bit++;
            }
        }
        return hash;
    }

    private String buildSearchText(Product product) {
        return (" " + nullSafeLower(product.getName())
                + " " + nullSafeLower(product.getShortDescription())
                + " " + nullSafeLower(product.getDescription())
                + " " + nullSafeLower(product.getFeatures())
                + " " + nullSafeLower(product.getSpecifications())
                + " ").replaceAll("\\s+", " ");
    }

    private boolean containsToken(String source, String token) {
        return nullSafeLower(source).contains(token.toLowerCase(Locale.ROOT));
    }

    private boolean containsWord(String source, String token) {
        String s = " " + nullSafeLower(source).replaceAll("[^a-z0-9]+", " ").trim() + " ";
        return s.contains(" " + token + " ");
    }

    private Set<String> splitTokens(String value) {
        if (!StringUtils.hasText(value)) return Set.of();
        return Arrays.stream(value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim().split("\\s+"))
                .filter(token -> token.length() >= 2)
                .collect(Collectors.toSet());
    }

    private boolean hasFuzzyTokenMatch(String keyword, Set<String> productTokens) {
        if (!StringUtils.hasText(keyword)) return false;
        for (String token : productTokens) {
            if (token.length() < 2) continue;
            if (Math.abs(token.length() - keyword.length()) > 2) continue;
            int distance = levenshteinDistance(keyword, token);
            if (distance <= 1 || (keyword.length() >= 6 && distance <= 2)) {
                return true;
            }
        }
        return false;
    }

    private int tokenOverlapCount(Set<String> productTokens, Set<String> keywords) {
        int overlap = 0;
        for (String keyword : keywords) {
            String k = keyword.toLowerCase(Locale.ROOT).trim();
            if (productTokens.contains(k)) overlap++;
        }
        return overlap;
    }

    private boolean isModelToken(String token) {
        return token != null && token.matches("(?=.*[a-z])(?=.*\\d)[a-z\\d-]{3,}");
    }

    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[a.length()][b.length()];
    }

    private String nullSafeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}