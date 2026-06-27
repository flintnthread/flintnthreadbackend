package com.ecommerce.sellerbackend.service.support;

import com.ecommerce.sellerbackend.dto.CreateProductRequest;
import com.ecommerce.sellerbackend.dto.CreateProductVariantRequest;
import com.ecommerce.sellerbackend.entity.Color;
import com.ecommerce.sellerbackend.entity.Size;
import com.ecommerce.sellerbackend.entity.Subcategory;
import com.ecommerce.sellerbackend.repository.ColorRepository;
import com.ecommerce.sellerbackend.repository.SizeRepository;
import com.ecommerce.sellerbackend.repository.SubcategoryRepository;
import com.ecommerce.sellerbackend.service.CatalogHierarchyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductCatalogResolver {

    private final CatalogHierarchyService catalogHierarchyService;
    private final SubcategoryRepository subcategoryRepository;
    private final ColorRepository colorRepository;
    private final SizeRepository sizeRepository;

    public record CategorySubcategoryIds(Integer categoryId, Integer subcategoryId) {}

    public CategorySubcategoryIds resolveCategoryIds(CreateProductRequest request) {
        Integer mainId = resolveMainCategoryId(request);
        if (mainId == null) {
            throw new IllegalArgumentException("Category is required.");
        }

        if (request.getSubcategoryId() != null) {
            Optional<Subcategory> direct = subcategoryRepository.findById(request.getSubcategoryId());
            if (direct.isPresent()) {
                return new CategorySubcategoryIds(mainId, direct.get().getId());
            }
            if (catalogHierarchyService.isChildCategoryId(request.getSubcategoryId(), mainId)) {
                Integer subId = catalogHierarchyService
                        .resolveSubcategoryIdForChildCategory(
                                request.getSubcategoryId(),
                                request.getSubcategoryName())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Subcategory not found for the selected category."));
                return new CategorySubcategoryIds(mainId, subId);
            }
            throw new IllegalArgumentException("Subcategory not found: " + request.getSubcategoryId());
        }

        Integer childCatId = request.getChildCategoryId();
        String middleName = trimToNull(request.getMiddleCategoryName());
        String subName = trimToNull(request.getSubcategoryName());

        if (childCatId == null && middleName != null) {
            childCatId = catalogHierarchyService.findChildCategoryId(mainId, middleName).orElse(null);
        }

        if (childCatId != null) {
            String lookupName = subName != null ? subName : middleName;
            Integer subId = catalogHierarchyService
                    .resolveSubcategoryIdForChildCategory(childCatId, lookupName)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Subcategory not found"
                                    + (lookupName != null ? ": " + lookupName : ".")));
            return new CategorySubcategoryIds(mainId, subId);
        }

        if (subName != null) {
            Optional<Subcategory> legacy = subcategoryRepository
                    .findBySubcategoryNameIgnoreCaseAndCategoryId(subName, mainId);
            if (legacy.isPresent()) {
                return new CategorySubcategoryIds(mainId, legacy.get().getId());
            }
            var mainCategory = catalogHierarchyService.findMainCategoryById(mainId)
                    .orElseThrow(() -> new IllegalArgumentException("Main category not found."));
            Subcategory middle = catalogHierarchyService
                    .findMiddleSubcategory(mainCategory.id(), subName)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + subName));
            if (subcategoryRepository.findById(middle.getId()).isPresent()) {
                return new CategorySubcategoryIds(mainId, middle.getId());
            }
            if (catalogHierarchyService.isChildCategoryId(middle.getId(), mainId)) {
                Integer subId = catalogHierarchyService
                        .resolveSubcategoryIdForChildCategory(middle.getId(), subName)
                        .orElseThrow(() -> new IllegalArgumentException("Subcategory not found: " + subName));
                return new CategorySubcategoryIds(mainId, subId);
            }
        }

        throw new IllegalArgumentException("Category and subcategory are required.");
    }

    public String resolveColorId(Long sellerId, CreateProductVariantRequest variantReq) {
        if (variantReq.getColorId() != null) {
            return String.valueOf(variantReq.getColorId());
        }
        Color color = colorRepository.findVisibleByNameForSeller(sellerId, variantReq.getColor())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Color not found: " + variantReq.getColor()));
        return String.valueOf(color.getId());
    }

    public String resolveSizeId(Long sellerId, CreateProductVariantRequest variantReq) {
        if (variantReq.getSizeId() != null) {
            return String.valueOf(variantReq.getSizeId());
        }
        Size size = sizeRepository.findVisibleByNameOrCodeForSeller(sellerId, variantReq.getSize())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Size not found: " + variantReq.getSize()));
        return String.valueOf(size.getId());
    }

    private Integer resolveMainCategoryId(CreateProductRequest request) {
        if (request.getCategoryId() != null) {
            return request.getCategoryId();
        }
        if (request.getCategoryName() == null || request.getCategoryName().isBlank()) {
            return null;
        }
        return catalogHierarchyService.findMainCategoryByName(request.getCategoryName().trim())
                .map(CatalogHierarchyService.MainCategoryRow::id)
                .orElse(null);
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
