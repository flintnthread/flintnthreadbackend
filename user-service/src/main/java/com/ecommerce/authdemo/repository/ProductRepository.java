package com.ecommerce.authdemo.repository;



import com.ecommerce.authdemo.entity.Product;

import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.*;

import org.springframework.data.repository.query.Param;



import java.time.LocalDateTime;

import java.util.Collection;
import java.util.List;



public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {



    // ---------------- BASIC ----------------

    Page<Product> findByCategoryIdAndStatus(Long categoryId, String status, Pageable pageable);

    Page<Product> findBySellerIdAndStatus(Long sellerId, String status, Pageable pageable);

    @EntityGraph(attributePaths = {"images", "variants"})
    List<Product> findTop10ByStatusOrderByCreatedAtDesc(String status);

    @EntityGraph(attributePaths = {"images", "variants"})
    List<Product> findTop10ByStatusOrderByIdDesc(String status);

    List<Product> findTop5ByNameContainingIgnoreCaseAndStatus(String name, String status);

    List<Product> findTop10ByCategoryIdAndStatusAndIdNot(Long categoryId, String status, Long id);

    List<Product> findTop20BySellerIdAndStatusOrderByCreatedAtDesc(Long sellerId, String status);

    long countBySellerIdAndStatus(Long sellerId, String status);

    long countByCategoryIdAndStatus(Long categoryId, String status);

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    Long countByCategoryId(Long categoryId);

    @Query("SELECT COUNT(p) FROM Product p JOIN p.variants v WHERE p.status = 'active' AND v.sellingPrice BETWEEN :min AND :max")
    Long countByPriceRange(@Param("min") Double min, @Param("max") Double max);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = 'active' AND p.rating >= :rating")
    Long countByMinRating(@Param("rating") Integer rating);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = 'active' AND p.gender = :gender")
    Long countByGender(@Param("gender") String gender);

    List<Product> findTop10ByOrderByCreatedAtDesc();



    List<Product> findBySellerId(Long sellerId);

    Page<Product> findBySellerId(Long sellerId, Pageable pageable);

    List<Product> findTop20BySellerIdOrderByCreatedAtDesc(Long sellerId);

    long countBySellerId(Long sellerId);



    List<Product> findTop10ByCategoryIdAndIdNot(Long categoryId, Long id);



    List<Product> findByNameContainingIgnoreCase(String name);



    List<Product> findTop20ByNameContainingIgnoreCaseAndStatus(String name, String status);



    List<Product> findTop5ByNameContainingIgnoreCase(String name);



    List<Product> findTop10ByOrderByIdDesc();



    List<Product> findTop60ByStatusOrderByCreatedAtDesc(String status);

    List<Product> findTop300ByStatusOrderByCreatedAtDesc(String status);



    Page<Product> findByNameContainingIgnoreCaseAndStatus(String name, String status, Pageable pageable);



    // ---------------- SEARCH ----------------

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND p.status = 'active'")

    List<Product> search(@Param("keyword") String keyword);

    @Query(value = """
        SELECT DISTINCT p.*
        FROM products p
        LEFT JOIN product_variants v ON v.product_id = p.id
        LEFT JOIN categories c ON c.id = p.category_id
        LEFT JOIN subcategories sc ON sc.id = p.subcategory_id
        WHERE p.status = 'active'
          AND (
            LOWER(p.name) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(p.short_description) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(p.description) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(p.features) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(v.color) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(v.size) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(c.category_name) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(sc.subcategory_name) LIKE LOWER(CONCAT('%', :kw, '%'))
          )
        ORDER BY
          CASE WHEN LOWER(p.name) LIKE LOWER(CONCAT('%', :kw, '%')) THEN 0 ELSE 1 END,
          CASE WHEN LOWER(v.color) LIKE LOWER(CONCAT('%', :kw, '%')) THEN 0 ELSE 1 END,
          CASE WHEN LOWER(v.size) LIKE LOWER(CONCAT('%', :kw, '%')) THEN 0 ELSE 1 END,
          p.created_at DESC
        LIMIT 40
    """, nativeQuery = true)
    List<Product> fullTextSearch(@Param("kw") String keyword);

    @Query(value = """
        SELECT DISTINCT p.*
        FROM products p
        LEFT JOIN product_variants v ON v.product_id = p.id
        LEFT JOIN categories c ON c.id = p.category_id
        LEFT JOIN subcategories sc ON sc.id = p.subcategory_id
        WHERE p.status = 'active'
          AND (
            LOWER(p.name) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(p.short_description) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(p.description) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(p.features) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(v.color) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(v.size) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(c.category_name) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(sc.subcategory_name) LIKE LOWER(CONCAT('%', :kw, '%'))
          )
        ORDER BY
          CASE WHEN LOWER(p.name) LIKE LOWER(CONCAT('%', :kw, '%')) THEN 0 ELSE 1 END,
          p.created_at DESC
    """,
    countQuery = """
        SELECT COUNT(DISTINCT p.id)
        FROM products p
        LEFT JOIN product_variants v ON v.product_id = p.id
        LEFT JOIN categories c ON c.id = p.category_id
        LEFT JOIN subcategories sc ON sc.id = p.subcategory_id
        WHERE p.status = 'active'
          AND (
            LOWER(p.name) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(p.short_description) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(p.description) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(p.features) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(v.color) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(v.size) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(c.category_name) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(sc.subcategory_name) LIKE LOWER(CONCAT('%', :kw, '%'))
          )
    """,
    nativeQuery = true)
    Page<Product> fullTextSearchPaged(@Param("kw") String keyword, Pageable pageable);

    @Query(value = """
        SELECT DISTINCT val FROM (
          SELECT DISTINCT LOWER(TRIM(v.color)) AS val
          FROM product_variants v
          JOIN products p ON p.id = v.product_id
          WHERE p.status = 'active'
            AND v.color IS NOT NULL
            AND LOWER(v.color) LIKE LOWER(CONCAT('%', :kw, '%'))
          UNION
          SELECT DISTINCT LOWER(TRIM(v.size)) AS val
          FROM product_variants v
          JOIN products p ON p.id = v.product_id
          WHERE p.status = 'active'
            AND v.size IS NOT NULL
            AND LOWER(v.size) LIKE LOWER(CONCAT('%', :kw, '%'))
          UNION
          SELECT DISTINCT LOWER(TRIM(p.name)) AS val
          FROM products p
          WHERE p.status = 'active'
            AND LOWER(p.name) LIKE LOWER(CONCAT('%', :kw, '%'))
          UNION
          SELECT DISTINCT LOWER(TRIM(c.category_name)) AS val
          FROM categories c
          WHERE LOWER(c.category_name) LIKE LOWER(CONCAT('%', :kw, '%'))
        ) combined
        ORDER BY val
        LIMIT 10
    """, nativeQuery = true)
    List<String> findSuggestionsByKeyword(@Param("kw") String keyword);



    // ---------------- POPULAR ----------------

    @Query("""

        SELECT p FROM Product p

        JOIN p.views v

        WHERE p.status = 'active'

        GROUP BY p

        ORDER BY COUNT(v.id) DESC

    """)

    List<Product> findPopularProducts();



    @Query(value = """

        SELECT p.*

        FROM products p

        WHERE p.status = 'active'

          AND (p.category_id = :mainCategoryId

           OR p.category_id IN (

               SELECT c.id FROM categories c WHERE c.parent_id = :mainCategoryId

           ))

        ORDER BY p.created_at DESC

        LIMIT 10

    """, nativeQuery = true)

    List<Product> findRecentProductsByMainCategory(@Param("mainCategoryId") Long mainCategoryId);



    @Query(value = """

        SELECT p.*

        FROM products p

        WHERE p.status = 'active'

          AND (p.category_id = :mainCategoryId

           OR p.category_id IN (

               SELECT c.id FROM categories c WHERE c.parent_id = :mainCategoryId

           ))

        ORDER BY p.created_at DESC

        LIMIT 10

    """, nativeQuery = true)

    List<Product> findLatestProductsByMainCategory(@Param("mainCategoryId") Long mainCategoryId);



    @Query(value = """

        SELECT p.*

        FROM products p

        JOIN product_views v ON v.product_id = p.id

        WHERE p.status = 'active'

          AND (p.category_id = :mainCategoryId

           OR p.category_id IN (

               SELECT c.id FROM categories c WHERE c.parent_id = :mainCategoryId

           ))

        GROUP BY p.id

        ORDER BY COUNT(v.id) DESC

        LIMIT 10

    """, nativeQuery = true)

    List<Product> findPopularProductsByMainCategory(@Param("mainCategoryId") Long mainCategoryId);



    // ---------------- TRENDING ----------------

    @Query("""

        SELECT p FROM Product p

        JOIN p.views v

        WHERE p.status = 'active'

          AND v.viewedAt >= :date

        GROUP BY p

        ORDER BY COUNT(v.id) DESC

    """)

    List<Product> findTrendingProducts(@Param("date") LocalDateTime date);



    @Query(value = """

        SELECT p.*

        FROM products p

        JOIN product_views v ON v.product_id = p.id

        WHERE v.viewed_at >= :date

          AND p.status = 'active'

          AND (

               p.category_id = :mainCategoryId

               OR p.category_id IN (

                   SELECT c.id FROM categories c WHERE c.parent_id = :mainCategoryId

               )

          )

        GROUP BY p.id

        ORDER BY COUNT(v.id) DESC

        LIMIT 10

    """, nativeQuery = true)

    List<Product> findTrendingProductsByMainCategory(@Param("date") LocalDateTime date, @Param("mainCategoryId") Long mainCategoryId);



    @Query(value = """

        SELECT p.*

        FROM products p

        WHERE p.status = 'active'

          AND (p.category_id = :mainCategoryId

           OR p.category_id IN (

               SELECT c.id FROM categories c WHERE c.parent_id = :mainCategoryId

           ))

        ORDER BY p.created_at DESC

        LIMIT 10

    """, nativeQuery = true)

    List<Product> findRelatedProductsByMainCategory(@Param("mainCategoryId") Long mainCategoryId);



    @Query(value = """

        SELECT p.*

        FROM products p

        LEFT JOIN product_variants v ON v.product_id = p.id

        LEFT JOIN product_views pv ON pv.product_id = p.id

        WHERE p.status = 'active'

          AND (p.category_id = :mainCategoryId

           OR p.category_id IN (

               SELECT c.id FROM categories c WHERE c.parent_id = :mainCategoryId

           ))

        GROUP BY p.id

        ORDER BY MAX(COALESCE(v.discount_percentage, 0)) DESC,

                 COUNT(pv.id) DESC,

                 p.created_at DESC

        LIMIT 10

    """, nativeQuery = true)

    List<Product> findSpotlightProductsByMainCategory(@Param("mainCategoryId") Long mainCategoryId);



    @Query(value = """

        SELECT p.*

        FROM products p

        JOIN (

            SELECT MIN(p2.id) AS id

            FROM products p2

            WHERE p2.status = 'active'

              AND (p2.category_id = :mainCategoryId

               OR p2.category_id IN (

                   SELECT c.id FROM categories c WHERE c.parent_id = :mainCategoryId

               ))

            GROUP BY LOWER(TRIM(p2.name))

        ) uniq ON uniq.id = p.id

        WHERE p.status = 'active'

        ORDER BY p.created_at DESC

        LIMIT 10

    """, nativeQuery = true)

    List<Product> findUniqueProductsByMainCategory(@Param("mainCategoryId") Long mainCategoryId);



    @Query(value = """

        SELECT p.*

        FROM products p

        JOIN order_items oi ON oi.product_id = p.id

        WHERE p.status = 'active'

          AND (p.category_id = :mainCategoryId

           OR p.category_id IN (

               SELECT c.id FROM categories c WHERE c.parent_id = :mainCategoryId

           ))

        GROUP BY p.id

        ORDER BY SUM(oi.quantity) DESC, MAX(oi.created_at) DESC

        LIMIT 10

    """, nativeQuery = true)

    List<Product> findTopCollectionsByMainCategory(@Param("mainCategoryId") Long mainCategoryId);



    @Query(value = """

        SELECT p.*

        FROM products p

        LEFT JOIN product_views popularity ON popularity.product_id = p.id

        LEFT JOIN product_variants v ON v.product_id = p.id

        WHERE p.status = 'active'

          AND (

               p.category_id = :mainCategoryId

               OR p.category_id IN (

                   SELECT c.id FROM categories c WHERE c.parent_id = :mainCategoryId

               )

        )

          AND (

               (:userId IS NULL AND (:sessionId IS NULL OR :sessionId = ''))

               OR NOT EXISTS (

                   SELECT 1

                   FROM product_views seen

                   WHERE seen.product_id = p.id

                     AND (

                         (:userId IS NOT NULL AND seen.user_id = :userId)

                         OR (:sessionId IS NOT NULL AND :sessionId <> '' AND seen.session_id = :sessionId)

                     )

               )

          )

        GROUP BY p.id

        ORDER BY COUNT(popularity.id) DESC,

                 MAX(COALESCE(v.discount_percentage, 0)) DESC,

                 p.created_at DESC

        LIMIT 10

    """, nativeQuery = true)

    List<Product> findRecommendedProductsByMainCategory(

            @Param("mainCategoryId") Long mainCategoryId,

            @Param("userId") Long userId,

            @Param("sessionId") String sessionId

    );



    @Query(value = """

        SELECT pv.product_id

        FROM product_views pv

        JOIN products p ON p.id = pv.product_id

        WHERE p.status = 'active'

          AND (

               p.category_id = :mainCategoryId

               OR p.category_id IN (

                   SELECT c.id FROM categories c WHERE c.parent_id = :mainCategoryId

               )

        )

          AND (

               (:userId IS NOT NULL AND pv.user_id = :userId)

               OR (:sessionId IS NOT NULL AND :sessionId <> '' AND pv.session_id = :sessionId)

          )

        GROUP BY pv.product_id

        ORDER BY MAX(pv.viewed_at) DESC

        LIMIT 10

    """, nativeQuery = true)

    List<Long> findRecentlyViewedProductIdsByMainCategory(

            @Param("mainCategoryId") Long mainCategoryId,

            @Param("userId") Long userId,

            @Param("sessionId") String sessionId

    );



    @Query(value = """

        SELECT pv.product_id

        FROM product_views pv

        JOIN products p ON p.id = pv.product_id

        WHERE p.status = 'active'

          AND (

               (:userId IS NOT NULL AND pv.user_id = :userId)

               OR (:sessionId IS NOT NULL AND :sessionId <> '' AND pv.session_id = :sessionId)

        )

        GROUP BY pv.product_id

        ORDER BY MAX(pv.viewed_at) DESC

        LIMIT 10

    """, nativeQuery = true)

    List<Long> findRecentlyViewedProductIds(

            @Param("userId") Long userId,

            @Param("sessionId") String sessionId

    );



    // ---------------- FULL FETCH ----------------

    @Query("""
        SELECT p FROM Product p
        WHERE p.categoryId = :categoryId
          AND p.status = 'active'
        ORDER BY p.createdAt DESC
    """)
    List<Product> findByCategoryFull(@Param("categoryId") Long categoryId);



    /**
     * List products for a subcategory without multiple JOIN FETCH bags (avoids huge
     * cartesian results and multi-minute responses). Variants/images load via {@code @BatchSize}.
     */
    @Query("""
        SELECT p FROM Product p
        WHERE p.subcategoryId = :subId
          AND p.status = 'active'
        ORDER BY p.createdAt DESC
    """)
    List<Product> findBySubCategoryFull(@Param("subId") Long subId);



    // ---------------- TOP BY SELLING PRICE (ALL CATEGORIES) ----------------

    @Query(value = """

        SELECT p.*

        FROM products p

        JOIN product_variants v ON p.id = v.product_id

        WHERE p.status = 'active'

        GROUP BY p.id

        ORDER BY MAX(v.selling_price) DESC

        LIMIT 10

    """, nativeQuery = true)

    List<Product> findTopSellingProducts();



    // ---------------- TOP PRODUCTS BY CATEGORY ----------------

    @Query(value = """

        SELECT p.* 

        FROM products p

        JOIN product_variants v ON p.id = v.product_id

        WHERE p.status = 'active'

          AND p.category_id = :categoryId

        GROUP BY p.id

        ORDER BY MAX(v.selling_price) DESC

        LIMIT 10

    """, nativeQuery = true)

    List<Product> findTopProductsByCategory(@Param("categoryId") Long categoryId);





    @Query(value = """

    SELECT p.* 

    FROM products p

    JOIN product_variants v ON p.id = v.product_id

    WHERE p.status = 'active'

    GROUP BY p.id

    ORDER BY MAX(v.discount_percentage) DESC

    LIMIT 10

""", nativeQuery = true)

    List<Product> findTopDiscountProducts();



    @Query(value = """

        SELECT p.*

        FROM products p

        JOIN product_variants v ON p.id = v.product_id

        WHERE p.status = 'active'

          AND (p.category_id = :mainCategoryId

           OR p.category_id IN (

               SELECT c.id FROM categories c WHERE c.parent_id = :mainCategoryId

           ))

        GROUP BY p.id

        ORDER BY MIN(v.discount_percentage) ASC

    """, nativeQuery = true)

    List<Product> findDiscountProductsByMainCategoryAsc(@Param("mainCategoryId") Long mainCategoryId);



    @Query(value = """

        SELECT p.*

        FROM products p

        JOIN product_variants v ON p.id = v.product_id

        WHERE p.status = 'active'

          AND (

               p.category_id = :mainCategoryId

               OR p.category_id IN (

                   SELECT c.id FROM categories c WHERE c.parent_id = :mainCategoryId

               )

        )

          AND ROUND(COALESCE(v.discount_percentage, 0), 2) = ROUND(:discountPercentage, 2)

        GROUP BY p.id

        ORDER BY p.created_at DESC

    """, nativeQuery = true)

    List<Product> findProductsByMainCategoryAndExactDiscountPercentage(

            @Param("mainCategoryId") Long mainCategoryId,

            @Param("discountPercentage") Double discountPercentage

    );



    @Query(value = """

        SELECT p.*

        FROM products p

        JOIN product_variants v ON p.id = v.product_id

        WHERE p.status = 'active'

          AND (

               p.category_id = :mainCategoryId

               OR p.category_id IN (

                   SELECT c.id FROM categories c WHERE c.parent_id = :mainCategoryId

               )

        )

          AND COALESCE(v.discount_percentage, 0) <= :maxDiscountPercentage

        GROUP BY p.id

        ORDER BY MAX(COALESCE(v.discount_percentage, 0)) DESC, p.created_at DESC

    """, nativeQuery = true)

    List<Product> findProductsByMainCategoryWithDiscountLessThanEqual(

            @Param("mainCategoryId") Long mainCategoryId,

            @Param("maxDiscountPercentage") Double maxDiscountPercentage

    );
// ---------------- SORT PRODUCTS ----------------



    @Query(value = """
    SELECT p.*
    FROM products p
    JOIN product_variants v ON v.product_id = p.id
    WHERE p.status = 'active'
      AND v.selling_price > 0
    GROUP BY p.id
    ORDER BY MIN(v.selling_price) ASC
""", nativeQuery = true)
    Page<Product> findLowToHigh(Pageable pageable);

    @Query(value = """
    SELECT p.*
    FROM products p
    JOIN product_variants v ON v.product_id = p.id
    WHERE p.status = 'active'
      AND v.selling_price > 0
    GROUP BY p.id
    ORDER BY MAX(v.selling_price) DESC
""", nativeQuery = true)
    Page<Product> findHighToLow(Pageable pageable);

    @Query(value = """
    SELECT p.*
    FROM products p
    JOIN product_variants v ON v.product_id = p.id
    WHERE p.status = 'active'
      AND v.discount_percentage > 0
      AND v.selling_price > 0
    GROUP BY p.id
    ORDER BY MAX(v.discount_percentage) DESC
""", nativeQuery = true)
    Page<Product> findDiscountProducts(Pageable pageable);
    // ---------------- PRODUCTS BY MAIN CATEGORY (INCLUDING SUBCATEGORIES) ----------------

    @Query(value = """

        SELECT DISTINCT p.* 

        FROM products p

        WHERE p.status = 'active'

          AND (p.category_id = :mainCategoryId

        OR p.category_id IN (

            SELECT c.id FROM categories c WHERE c.parent_id = :mainCategoryId

        ))

    """, nativeQuery = true)

    List<Product> findByMainCategory(@Param("mainCategoryId") Long mainCategoryId);



    @Query(value = """

        SELECT DISTINCT p.* 

        FROM products p

        LEFT JOIN product_variants v ON p.id = v.product_id

        WHERE p.status = 'active'

          AND (p.category_id = :mainCategoryId

        OR p.category_id IN (

            SELECT c.id FROM categories c WHERE c.parent_id = :mainCategoryId

        ))

    """, nativeQuery = true)

    List<Product> findByMainCategoryFull(@Param("mainCategoryId") Long mainCategoryId);





    @Query("""
    SELECT DISTINCT p
    FROM Product p
    LEFT JOIN p.variants v
    WHERE p.status = 'active'
      AND (
        LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(COALESCE(p.shortDescription, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(COALESCE(p.features, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(COALESCE(p.specifications, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(COALESCE(p.gender, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(COALESCE(v.color, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(COALESCE(v.size, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(COALESCE(v.sku, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
""")
    Page<Product> advancedSearch(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"images", "variants"})
    @Query("SELECT p FROM Product p WHERE p.id IN :ids")
    List<Product> findAllWithImagesAndVariantsByIdIn(@Param("ids") Collection<Long> ids);
}