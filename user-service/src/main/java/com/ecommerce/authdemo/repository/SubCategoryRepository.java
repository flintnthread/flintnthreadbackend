package com.ecommerce.authdemo.repository;

    import com.ecommerce.authdemo.dto.SubCategoryResponseDTO;
    import com.ecommerce.authdemo.entity.SubCategory;
    import org.springframework.data.jpa.repository.JpaRepository;
    import java.util.List;

public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {

    List<SubCategory> findByCategoryId(Long categoryId);

    List<SubCategory> findBySubcategoryNameContainingIgnoreCase(String keyword);

    List<SubCategory> findByCategoryIdAndStatus(Long categoryId, Integer status);

    List<SubCategory> findTop10BySubcategoryNameContainingIgnoreCase(String keyword);




}
