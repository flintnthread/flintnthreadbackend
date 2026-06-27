package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Integer> {

    @Query("""
            SELECT f FROM Faq f
            WHERE f.status = true
            ORDER BY f.categoryId ASC, f.sortOrder ASC, f.id ASC
            """)
    List<Faq> findAllActiveFaqs();

    @Query(value = """
            SELECT * FROM faqs f
            WHERE f.status = 1
            AND f.is_seller = 1
            ORDER BY f.category_id ASC, f.sort_order ASC, f.id ASC
            """, nativeQuery = true)
    List<Faq> findActiveSellerFaqs();

    @Query("""
            SELECT f FROM Faq f
            WHERE f.status = true
            AND (LOWER(f.question) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(f.answer) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY f.categoryId ASC, f.sortOrder ASC, f.id ASC
            """)
    List<Faq> searchAllActiveFaqs(@Param("q") String q);

    @Query(value = """
            SELECT * FROM faqs f
            WHERE f.status = 1
            AND f.is_seller = 1
            AND (LOWER(f.question) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(f.answer) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY f.category_id ASC, f.sort_order ASC, f.id ASC
            """, nativeQuery = true)
    List<Faq> searchActiveSellerFaqs(@Param("q") String q);
}
