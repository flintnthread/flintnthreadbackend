package com.ecommerce.authdemo.mapper;

import com.ecommerce.authdemo.dto.ProductDTO;
import com.ecommerce.authdemo.dto.ProductImageDTO;
import com.ecommerce.authdemo.dto.ProductVariantDTO;
import com.ecommerce.authdemo.entity.Product;
import com.ecommerce.authdemo.entity.ProductImage;
import com.ecommerce.authdemo.entity.ProductVariant;
import com.ecommerce.authdemo.entity.SizeChart;
import com.ecommerce.authdemo.repository.SizeChartRepository;
import com.ecommerce.authdemo.util.SizeChartUtil;
import com.ecommerce.authdemo.util.SizeColorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductMapper {

    @Value("${app.media.public-base-url:}")
    private String mediaPublicBaseUrl;
    
    private final SizeColorMapper sizeColorMapper;
    private final SizeChartRepository sizeChartRepository;

    public String resolveImageUrl(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }
        if (storedPath.startsWith("http://") || storedPath.startsWith("https://")) {
            return storedPath;
        }
        String path = storedPath.startsWith("/") ? storedPath : "/" + storedPath;
        if (mediaPublicBaseUrl.isEmpty()) {
            return path;
        }
        String base = mediaPublicBaseUrl.endsWith("/")
                ? mediaPublicBaseUrl.substring(0, mediaPublicBaseUrl.length() - 1)
                : mediaPublicBaseUrl;
        return base + path;
    }

    public ProductDTO toDTO(Product p) {

        ProductDTO dto = new ProductDTO();

        // ------------------------
        // BASIC
        // ------------------------
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setDescription(p.getDescription());

        if (p.getCategoryId() != null) {
            dto.setCategoryId(Long.valueOf(p.getCategoryId()));
        }

        if (p.getSubcategoryId() != null) {
            dto.setSubcategoryId(Long.valueOf(p.getSubcategoryId()));
        }

        dto.setSellerId(p.getSellerId());
        dto.setStatus(p.getStatus());
        dto.setCreatedAt(p.getCreatedAt());

        dto.setShortDescription(p.getShortDescription());
        dto.setFeatures(p.getFeatures());
        dto.setSpecifications(p.getSpecifications());
        dto.setReturnPolicy(p.getReturnPolicy());

        dto.setSizeChartId(p.getSizeChartId());
        applySizeChartFromDatabase(dto, p);

        dto.setGstPercentage(p.getGstPercentage());
        dto.setProductWeight(p.getProductWeight());

        dto.setDeliveryTimeMin(p.getDeliveryTimeMin());
        dto.setDeliveryTimeMax(p.getDeliveryTimeMax());
        boolean customizedProduct = Boolean.TRUE.equals(p.getIsCustomizedProduct());
        dto.setIsCustomizedProduct(customizedProduct);
        dto.setIsCustomized(customizedProduct);
        dto.setCustomRequiredFields(p.getCustomRequiredFields());

        // ------------------------
        // VARIANTS (always non-null for list/detail APIs)
        // ------------------------
        if (p.getVariants() != null && !p.getVariants().isEmpty()) {
            List<ProductVariantDTO> variantDTOs = p.getVariants()
                    .stream()
                    .map(v -> {
                        ProductVariantDTO vd = new ProductVariantDTO();

                        vd.setId(v.getId());

                        if (v.getProduct() != null) {
                            vd.setProductId(v.getProduct().getId());
                        }

                        vd.setColor(sizeColorMapper.getColorName(v.getColor()));
                        vd.setSize(sizeColorMapper.getSizeName(v.getSize()));
                        vd.setSku(v.getSku());

                        Integer stock = v.getStock();
                        boolean inStock = stock != null && stock > 0;

                        // When out of stock, return null price fields so UI can show "Out of stock"
                        // instead of displaying a price.
                        vd.setMrpPrice(inStock ? v.getMrpPrice() : null);
                        vd.setSellingPrice(inStock ? v.getSellingPrice() : null);
                        vd.setFinalPrice(inStock ? v.getFinalPrice() : null);

                        vd.setDiscountPercentage(inStock ? v.getDiscountPercentage() : null);
                        vd.setDiscountAmount(inStock ? v.getDiscountAmount() : null);

                        vd.setTaxPercentage(v.getTaxPercentage());
                        vd.setTaxAmount(v.getTaxAmount());

                        vd.setStock(stock);
                        vd.setInStock(inStock);

                        vd.setVideoPath(v.getVideoPath());
                        vd.setWeight(v.getWeight());

                        return vd;
                    })
                    .collect(Collectors.toList());

            dto.setVariants(variantDTOs);
        } else {
            dto.setVariants(Collections.emptyList());
        }

        // ------------------------
        // IMAGES (always non-null; imageUrl from app.media.public-base-url)
        // ------------------------
        if (p.getImages() != null && !p.getImages().isEmpty()) {
            List<ProductImageDTO> imageDTOs = p.getImages()
                    .stream()
                    .map(this::toImageDTO)
                    .collect(Collectors.toList());

            dto.setImages(imageDTOs);
        } else {
            dto.setImages(Collections.emptyList());
        }

        return dto;
    }

    public ProductImageDTO toImageDTO(ProductImage img) {
        ProductImageDTO im = new ProductImageDTO();
        im.setId(img.getId());
        if (img.getProduct() != null) {
            im.setProductId(img.getProduct().getId());
        }
        im.setImagePath(img.getImagePath());
        im.setImageUrl(resolveImageUrl(img.getImagePath()));
        im.setIsPrimary(img.getIsPrimary());
        im.setSortOrder(img.getSortOrder());
        im.setVariantId(img.getVariantId());
        return im;
    }

    // ------------------------
    // DTO → ENTITY
    // ------------------------
    public Product toEntity(ProductDTO dto) {

        Product p = new Product();

        p.setName(dto.getName());
        p.setDescription(dto.getDescription());

        if (dto.getCategoryId() != null) {
            p.setCategoryId(Math.toIntExact(dto.getCategoryId()));
        }

        if (dto.getSubcategoryId() != null) {
            p.setSubcategoryId(Math.toIntExact(dto.getSubcategoryId()));
        }

        p.setSellerId(dto.getSellerId());

        p.setShortDescription(dto.getShortDescription());
        p.setFeatures(dto.getFeatures());
        p.setSpecifications(dto.getSpecifications());
        p.setReturnPolicy(dto.getReturnPolicy());
        p.setSizeChartId(dto.getSizeChartId());
        Boolean customizedFlag = dto.getIsCustomizedProduct() != null
                ? dto.getIsCustomizedProduct()
                : dto.getIsCustomized();
        p.setIsCustomizedProduct(Boolean.TRUE.equals(customizedFlag));

        return p;
    }

    private void applySizeChartFromDatabase(ProductDTO dto, Product product) {
        if (product.getSizeChartId() != null) {
            sizeChartRepository.findById(product.getSizeChartId()).ifPresent(chart -> {
                if (Boolean.FALSE.equals(chart.getIsActive())) {
                    return;
                }
                String normalized = SizeChartUtil.normalizeDbChartDataToApiJson(chart.getChartData());
                if (normalized != null && !normalized.isBlank()) {
                    dto.setSizeChart(normalized);
                    dto.setSizeChartUnit(SizeChartUtil.resolveSizeChartUnit(normalized));
                    dto.setSizeChartName(chart.getChartName());
                    dto.setSizeChartImage(resolveImageUrl(chart.getChartImage()));
                }
            });
        }

        if (dto.getSizeChart() == null || dto.getSizeChart().isBlank()) {
            String fromSpecs = SizeChartUtil.extractFromSpecifications(product.getSpecifications());
            if (fromSpecs != null && !fromSpecs.isBlank()) {
                dto.setSizeChart(fromSpecs);
                dto.setSizeChartUnit(SizeChartUtil.resolveSizeChartUnit(fromSpecs));
            }
        }
    }
}