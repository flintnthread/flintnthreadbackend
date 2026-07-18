package com.ecommerce.authdemo.mapper;

import com.ecommerce.authdemo.dto.ProductDTO;
import com.ecommerce.authdemo.dto.ProductImageDTO;
import com.ecommerce.authdemo.dto.ProductVariantDTO;
import com.ecommerce.authdemo.entity.Product;
import com.ecommerce.authdemo.entity.ProductImage;
import com.ecommerce.authdemo.entity.ProductVariant;
import com.ecommerce.authdemo.entity.SizeChart;
import com.ecommerce.authdemo.repository.SizeChartRepository;
import com.ecommerce.authdemo.service.CustomerPriceResolver;
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

    @Value("${app.media.public-base-url:https://flintnthread.com}")
    private String mediaPublicBaseUrl;

    /** Hard-coded product image CDN — only host for /uploads/products/. */
    private static final String PRODUCT_IMAGE_CDN = "https://flintnthread.com";
    
    private final SizeColorMapper sizeColorMapper;
    private final SizeChartRepository sizeChartRepository;
    private final CustomerPriceResolver customerPriceResolver;

    public String resolveImageUrl(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }
        if (storedPath.startsWith("http://") || storedPath.startsWith("https://")) {
            String lower = storedPath.toLowerCase();
            // Cloudinary absolute URLs — never rewrite onto media host
            if (lower.contains("res.cloudinary.com/")) {
                return storedPath;
            }
            int productsIdx = storedPath.indexOf("/uploads/products/");
            if (productsIdx >= 0) {
                // Product images ONLY → https://flintnthread.com/uploads/products/...
                return PRODUCT_IMAGE_CDN + storedPath.substring(productsIdx);
            }
            // Seller docs / KYC / other uploads — leave unchanged
            return storedPath;
        }

        String trimmed = storedPath.trim().replace("\\", "/");
        // uploads/products/... or /uploads/products/...
        if (trimmed.toLowerCase().contains("uploads/products/")) {
            int idx = trimmed.toLowerCase().indexOf("uploads/products/");
            String path = "/" + trimmed.substring(idx).replaceAll("^/+", "");
            return PRODUCT_IMAGE_CDN + path;
        }
        // Bare filename from product_images → products folder on .com
        if (!trimmed.contains("/")) {
            return PRODUCT_IMAGE_CDN + "/uploads/products/" + trimmed;
        }

        String path = trimmed.startsWith("/") ? trimmed : "/" + trimmed;
        if (mediaPublicBaseUrl == null || mediaPublicBaseUrl.isBlank()) {
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

                        // Always expose prices for UI (home/web cards). Out-of-stock
                        // is signaled via inStock/stock — do not null price fields.
                        CustomerPriceResolver.ResolvedPrice pricing =
                                    customerPriceResolver.resolve(p, v);
                            vd.setMrpExclGst(v.getMrpExclGst());
                            vd.setMrpInclGst(v.getMrpInclGst());
                            if (pricing != null) {
                                vd.setSellingPriceExclGst(pricing.sellingExclGst());
                                vd.setSellingPrice(pricing.customerPrice());
                                vd.setFinalPrice(pricing.priceAfterGst());
                                vd.setCustomerPrice(pricing.customerPrice());
                                vd.setMrpPrice(
                                        customerPriceResolver.resolveCustomerStrikeMrp(p, v));
                                vd.setTaxPercentage(pricing.gstPercent());
                                vd.setTaxAmount(pricing.taxAmount());
                                vd.setCommissionPercentage(pricing.commissionPercent());
                                vd.setCommissionAmount(pricing.commissionAmount());
                                vd.setIntraCityDeliveryCharge(pricing.intraCityDeliveryCharge());
                                vd.setMetroMetroDeliveryCharge(pricing.metroMetroDeliveryCharge());
                                vd.setDeliveryCharge(pricing.deliveryCharge());
                                vd.setTotalPriceIntraCity(pricing.totalPriceIntraCity());
                                vd.setTotalPriceMetroMetro(pricing.totalPriceMetroMetro());
                            } else {
                                vd.setSellingPrice(v.getSellingPrice());
                                vd.setSellingPriceExclGst(v.getSellingPrice());
                                vd.setFinalPrice(v.getFinalPrice());
                                vd.setCustomerPrice(
                                        v.getFinalPrice() != null ? v.getFinalPrice() : v.getSellingPrice());
                                vd.setMrpPrice(v.resolveMrpUnitPrice());
                                vd.setTaxPercentage(v.getTaxPercentage());
                                vd.setTaxAmount(v.getTaxAmount());
                            }

                        vd.setDiscountPercentage(v.getDiscountPercentage());
                        vd.setDiscountAmount(v.getDiscountAmount());

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
                    .sorted((a, b) -> {
                        int ap = Boolean.TRUE.equals(a.getIsPrimary()) ? 0 : 1;
                        int bp = Boolean.TRUE.equals(b.getIsPrimary()) ? 0 : 1;
                        if (ap != bp) return ap - bp;
                        int ao = a.getSortOrder() == null ? Integer.MAX_VALUE : a.getSortOrder();
                        int bo = b.getSortOrder() == null ? Integer.MAX_VALUE : b.getSortOrder();
                        return Integer.compare(ao, bo);
                    })
                    .map(this::toImageDTO)
                    .collect(Collectors.toList());

            dto.setImages(imageDTOs);
            // List/card convenience field used by home / admin / recent rows
            if (!imageDTOs.isEmpty() && imageDTOs.get(0).getImageUrl() != null) {
                dto.setImageUrl(imageDTOs.get(0).getImageUrl());
            }
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