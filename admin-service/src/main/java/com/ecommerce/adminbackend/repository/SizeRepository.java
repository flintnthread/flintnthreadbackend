package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SizeRepository extends JpaRepository<Size, Long> {

    List<Size> findAllByOrderBySizeNameAsc();

    /**
     * Maps each size to main category names used by products that reference the size
     * via product_variants.size (size id stored as string, with name/code fallback).
     * Returns rows: [sizeId, mainCategoryName]
     */
    @Query(value = """
            SELECT DISTINCT
                s.id AS size_id,
                COALESCE(parent.category_name, c.category_name) AS main_category_name
            FROM sizes s
            INNER JOIN product_variants pv
                ON (
                    pv.size = CAST(s.id AS CHAR)
                    OR LOWER(TRIM(pv.size)) = LOWER(TRIM(s.size_name))
                    OR LOWER(TRIM(pv.size)) = LOWER(TRIM(s.size_code))
                )
            INNER JOIN products p ON p.id = pv.product_id
            INNER JOIN categories c ON c.id = p.category_id
            LEFT JOIN categories parent ON parent.id = c.parent_id
            WHERE COALESCE(parent.category_name, c.category_name) IS NOT NULL
              AND TRIM(COALESCE(parent.category_name, c.category_name)) <> ''
            ORDER BY s.id, main_category_name
            """, nativeQuery = true)
    List<Object[]> findSizeMainCategoryPairs();
}
