package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.ProductDTO;
import com.ecommerce.authdemo.dto.ProductImageDTO;
import com.ecommerce.authdemo.dto.WishlistResponse;
import com.ecommerce.authdemo.entity.Product;
import com.ecommerce.authdemo.entity.ProductImage;
import com.ecommerce.authdemo.entity.ProductVariant;
import com.ecommerce.authdemo.entity.User;
import com.ecommerce.authdemo.entity.Wishlist;
import com.ecommerce.authdemo.mapper.ProductMapper;
import com.ecommerce.authdemo.util.SizeColorMapper;
import com.ecommerce.authdemo.repository.WishlistRepository;
import com.ecommerce.authdemo.repository.UserRepository;
import com.ecommerce.authdemo.service.ProductService;
import com.ecommerce.authdemo.service.WishlistService;
import com.ecommerce.authdemo.util.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductService productService;
    private final ProductMapper productMapper;
    private final SecurityUtil securityUtil;
    private final UserRepository userRepository;
    private final SizeColorMapper sizeColorMapper;


    public WishlistServiceImpl(WishlistRepository wishlistRepository,
                               ProductService productService,
                               ProductMapper productMapper,
                               SecurityUtil securityUtil,
                               UserRepository userRepository,
                               SizeColorMapper sizeColorMapper) {
        this.wishlistRepository = wishlistRepository;
        this.productService = productService;
        this.productMapper = productMapper;
        this.securityUtil = securityUtil;
        this.userRepository = userRepository;
        this.sizeColorMapper = sizeColorMapper;
    }

    @Override
    public WishlistResponse addToWishlist(Long userId, Long productId, Long variantId) {

        // Get authenticated user
        User authenticatedUser = securityUtil.getCurrentUser();
        
        // Check if product exists before adding to wishlist
        ProductDTO productDTO = productService.getProduct(productId);
        if (productDTO == null) {
            throw new RuntimeException("Product not found with ID: " + productId);
        }

        // Convert ProductDTO to Product entity for wishlist
        Product product = new Product();
        product.setId(productDTO.getId());
        product.setName(productDTO.getName());
        // Set other necessary fields if needed
        product.setCategoryId(productDTO.getCategoryId() != null ? productDTO.getCategoryId().intValue() : null);
        product.setSubcategoryId(productDTO.getSubcategoryId() != null ? productDTO.getSubcategoryId().intValue() : null);
        product.setStatus(productDTO.getStatus());
        product.setSellerId(productDTO.getSellerId());

        // Check if this product variant is already in the user's wishlist
        Optional<Wishlist> existingWishlist = wishlistRepository
                .findByUserIdAndProductIdAndVariantId(authenticatedUser.getId(), productId, variantId);
        
        if (existingWishlist.isPresent()) {
            // Product variant already exists in wishlist, return the existing one
            log.info("Product variant {} already exists in wishlist for user {}", productId, authenticatedUser.getId());
            return buildResponse(existingWishlist.get());
        }
        
        // Create new wishlist entry
        Wishlist newWishlist = new Wishlist();
        newWishlist.setUser(authenticatedUser);
        newWishlist.setProduct(product);
        newWishlist.setVariantId(variantId);
        
        Wishlist savedWishlist = wishlistRepository.save(newWishlist);
        log.info("Added new product variant {} to wishlist for user {}", productId, authenticatedUser.getId());
        
        return buildResponse(savedWishlist);
    }
    @Override
    public List<WishlistResponse> getUserWishlist(Long userId) {

        // Get authenticated user
        User authenticatedUser = securityUtil.getCurrentUser();

        List<Wishlist> wishlistItems =
                wishlistRepository.findWishlistWithProduct(authenticatedUser.getId());

        return wishlistItems.stream()
                .map(this::buildResponse)
                .toList();
    }

    @Override
    @Transactional
    public void removeFromWishlist(Long userId, Long productId, Long variantId) {

        // Get authenticated user
        User authenticatedUser = securityUtil.getCurrentUser();

        Wishlist wishlist = wishlistRepository
                .findByUserIdAndProductIdAndVariantId(authenticatedUser.getId(), productId, variantId)
                .orElseThrow(() -> new RuntimeException("Wishlist item not found"));

        wishlistRepository.delete(wishlist);
    }

    @Override
    public boolean isProductInWishlist(Long userId, Long productId) {
        // Get authenticated user
        User authenticatedUser = securityUtil.getCurrentUser();
        return wishlistRepository
                .findByUserIdAndProductId(authenticatedUser.getId(), productId)
                .isPresent();
    }

    @Override
    public boolean isProductVariantInWishlist(Long userId, Long productId, Long variantId) {
        // Get authenticated user
        User authenticatedUser = securityUtil.getCurrentUser();
        return wishlistRepository
                .findByUserIdAndProductIdAndVariantId(authenticatedUser.getId(), productId, variantId)
                .isPresent();
    }


    private WishlistResponse buildResponse(Wishlist wishlist) {

        Product product = wishlist.getProduct();

        WishlistResponse response = new WishlistResponse();

        response.setWishlistId(wishlist.getId());
        response.setProductId(product.getId());
        response.setVariantId(wishlist.getVariantId());
        response.setProductName(product.getName());
        response.setAddedAt(wishlist.getCreatedAt());

        if (product.getImages() != null && !product.getImages().isEmpty()) {
            // Convert ProductImage entities to ProductImageDTOs (same as ProductDTO)
            List<ProductImageDTO> imageDTOs = product.getImages().stream()
                    .map(productMapper::toImageDTO)
                    .collect(Collectors.toList());
            
            response.setImages(imageDTOs);
            
            // Get the primary image or first image for backward compatibility
            ProductImage primaryImage = product.getImages().stream()
                    .filter(img -> img.getIsPrimary() != null && img.getIsPrimary())
                    .findFirst()
                    .orElse(product.getImages().iterator().next());
            
            // Use imageUrl (absolute URL) like products do
            String primaryImageUrl = null;
            if (primaryImage.getImagePath() != null) {
                // ProductImage entity has imagePath, let ProductMapper convert to URL
                primaryImageUrl = productMapper.resolveImageUrl(primaryImage.getImagePath());
            }
            
            // Set both fields for backward compatibility
            response.setImage(primaryImageUrl);
            response.setImageUrl(primaryImageUrl);
        }

        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            // Find the specific variant that matches the variantId in the wishlist
            ProductVariant variant = product.getVariants().stream()
                    .filter(v -> v.getId().equals(wishlist.getVariantId()))
                    .findFirst()
                    .orElse(product.getVariants().iterator().next()); // Fallback to first variant

            response.setMrpPrice(variant.getSellingPrice());
            response.setSellingPrice(variant.getSellingPrice());
            response.setSize(sizeColorMapper.getSizeName(variant.getSize()));
            response.setColor(sizeColorMapper.getColorName(variant.getColor()));
            response.setInStock(variant.getStock() > 0);
        }

        return response;
    }
}