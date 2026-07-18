package com.ecommerce.adminbackend.service.support;

import com.ecommerce.adminbackend.entity.Category;
import com.ecommerce.adminbackend.entity.Subcategory;
import com.ecommerce.adminbackend.repository.CategoryRepository;
import com.ecommerce.adminbackend.repository.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Resolves main category + leaf subcategory ids for product create/update.
 * <p>
 * Database layout:
 * <ul>
 *   <li>{@code categories} — main rows ({@code parent_id IS NULL}) and middle rows ({@code parent_id = main.id})</li>
 *   <li>{@code subcategories} — leaf rows linked via {@code category_id} to a middle category, or directly to main when flat</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ProductCatalogResolver {

    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;

    public record CategorySubcategoryIds(Integer categoryId, Integer subcategoryId, Subcategory subcategory) {}

    public CategorySubcategoryIds resolve(
            Integer categoryId,
            String categoryName,
            Integer subcategoryId,
            String subcategoryName,
            Integer childCategoryId,
            String middleCategoryName) {
        Integer mainId = resolveMainCategoryId(categoryId, categoryName);
        if (mainId == null) {
            throw new IllegalArgumentException("Category is required.");
        }

        if (subcategoryId != null) {
            Optional<Subcategory> direct = subcategoryRepository.findById(subcategoryId);
            if (direct.isPresent()) {
                Subcategory sub = direct.get();
                if (subcategoryBelongsToMain(mainId, sub)) {
                    return new CategorySubcategoryIds(mainId, sub.getId(), sub);
                }
            }
            if (isChildCategoryId(subcategoryId, mainId)) {
                Integer resolvedSubId = resolveSubcategoryIdForChildCategory(subcategoryId, subcategoryName)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Subcategory not found for the selected category."));
                Subcategory sub = subcategoryRepository.findById(resolvedSubId)
                        .orElseThrow(() -> new IllegalArgumentException("Subcategory not found."));
                return new CategorySubcategoryIds(mainId, resolvedSubId, sub);
            }
            if (direct.isPresent()) {
                throw new IllegalArgumentException("Subcategory does not belong to the selected category.");
            }
            throw new IllegalArgumentException("Subcategory not found: " + subcategoryId);
        }

        Integer childCatId = childCategoryId;
        String middleName = trimToNull(middleCategoryName);
        String subName = trimToNull(subcategoryName);

        if (childCatId == null && middleName != null) {
            childCatId = findChildCategoryId(mainId, middleName).orElse(null);
        }

        if (childCatId != null) {
            if (isChildCategoryId(childCatId, mainId)) {
                String lookupName = subName != null ? subName : middleName;
                Integer resolvedSubId = resolveSubcategoryIdForChildCategory(childCatId, lookupName)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Subcategory not found"
                                        + (lookupName != null ? ": " + lookupName : ".")));
                Subcategory sub = subcategoryRepository.findById(resolvedSubId)
                        .orElseThrow(() -> new IllegalArgumentException("Subcategory not found."));
                return new CategorySubcategoryIds(mainId, resolvedSubId, sub);
            }
            Optional<Subcategory> flatMiddle = subcategoryRepository.findById(childCatId);
            if (flatMiddle.isPresent() && subcategoryBelongsToMain(mainId, flatMiddle.get())) {
                Subcategory sub = flatMiddle.get();
                return new CategorySubcategoryIds(mainId, sub.getId(), sub);
            }
        }

        if (subName != null) {
            Optional<Subcategory> legacy = findSubcategoryByNameUnderCategory(mainId, subName);
            if (legacy.isPresent()) {
                Subcategory sub = legacy.get();
                return new CategorySubcategoryIds(mainId, sub.getId(), sub);
            }
            Optional<Integer> middleCatId = findChildCategoryId(mainId, subName);
            if (middleCatId.isPresent()) {
                Integer resolvedSubId = resolveSubcategoryIdForChildCategory(middleCatId.get(), subName)
                        .orElseThrow(() -> new IllegalArgumentException("Subcategory not found: " + subName));
                Subcategory sub = subcategoryRepository.findById(resolvedSubId)
                        .orElseThrow(() -> new IllegalArgumentException("Subcategory not found."));
                return new CategorySubcategoryIds(mainId, resolvedSubId, sub);
            }
        }

        throw new IllegalArgumentException("Category and subcategory are required.");
    }

    private Integer resolveMainCategoryId(Integer categoryId, String categoryName) {
        if (categoryId != null) {
            return categoryRepository.findById(categoryId)
                    .map(cat -> cat.getParentId() == null ? cat.getId() : cat.getParentId())
                    .orElse(categoryId);
        }
        if (categoryName == null || categoryName.isBlank()) {
            return null;
        }
        return categoryRepository.findByParentIdIsNullOrderByCategoryNameAsc().stream()
                .filter(cat -> categoryName.trim().equalsIgnoreCase(cat.getCategoryName()))
                .map(Category::getId)
                .findFirst()
                .orElse(null);
    }

    private boolean subcategoryBelongsToMain(Integer mainCategoryId, Subcategory sub) {
        if (mainCategoryId.equals(sub.getCategoryId())) {
            return true;
        }
        return categoryRepository.findById(sub.getCategoryId())
                .map(middle -> mainCategoryId.equals(middle.getParentId()))
                .orElse(false);
    }

    private boolean isChildCategoryId(Integer categoryId, Integer mainCategoryId) {
        if (categoryId == null || mainCategoryId == null) {
            return false;
        }
        return categoryRepository.findById(categoryId)
                .map(child -> mainCategoryId.equals(child.getParentId()))
                .orElse(false);
    }

    private Optional<Integer> findChildCategoryId(Integer mainCategoryId, String childCategoryName) {
        if (mainCategoryId == null || childCategoryName == null || childCategoryName.isBlank()) {
            return Optional.empty();
        }
        return categoryRepository.findByParentIdOrderByCategoryNameAsc(mainCategoryId).stream()
                .filter(row -> childCategoryName.trim().equalsIgnoreCase(row.getCategoryName()))
                .map(Category::getId)
                .findFirst();
    }

    private Optional<Integer> resolveSubcategoryIdForChildCategory(
            Integer childCategoryId,
            String subcategoryName) {
        if (childCategoryId == null) {
            return Optional.empty();
        }
        List<Subcategory> subs = subcategoryRepository
                .findByCategoryIdOrderBySubcategoryNameAsc(childCategoryId);
        if (subs.isEmpty()) {
            return Optional.empty();
        }
        if (subcategoryName != null && !subcategoryName.isBlank()) {
            return subs.stream()
                    .filter(sub -> subcategoryName.trim().equalsIgnoreCase(sub.getSubcategoryName()))
                    .map(Subcategory::getId)
                    .findFirst();
        }
        return Optional.of(subs.get(0).getId());
    }

    private Optional<Subcategory> findSubcategoryByNameUnderCategory(Integer categoryId, String name) {
        if (categoryId == null || name == null || name.isBlank()) {
            return Optional.empty();
        }
        return subcategoryRepository.findByCategoryIdOrderBySubcategoryNameAsc(categoryId).stream()
                .filter(sub -> name.trim().equalsIgnoreCase(sub.getSubcategoryName()))
                .findFirst();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
