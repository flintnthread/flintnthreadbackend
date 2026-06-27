package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.*;
import com.ecommerce.authdemo.entity.Cart;
import com.ecommerce.authdemo.entity.Product;
import com.ecommerce.authdemo.entity.ProductVariant;
import com.ecommerce.authdemo.entity.User;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.exception.CartException;
import com.ecommerce.authdemo.repository.CartRepository;
import com.ecommerce.authdemo.repository.ProductImageRepository;
import com.ecommerce.authdemo.repository.ProductRepository;
import com.ecommerce.authdemo.repository.ProductVariantRepository;
import com.ecommerce.authdemo.repository.UserRepository;
import com.ecommerce.authdemo.service.CartService;
import com.ecommerce.authdemo.util.SecurityUtil;
import com.ecommerce.authdemo.util.SizeColorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final SecurityUtil securityUtil;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;
    private final SizeColorMapper sizeColorMapper;
    
    @Value("${app.media.public-base-url}")
    private String publicBaseUrl;

    @Override
    @Transactional
    public CartResponseDTO addToCart(AddToCartDTO dto) {
        log.info("Adding item to cart: productId={}, variantId={}, quantity={}", 
                dto.getProductId(), dto.getVariantId(), dto.getQuantity());
        
        validateAddToCartRequest(dto);
        
        Long userId = securityUtil.getCurrentUserId();
        BigDecimal productPrice = resolveUnitPriceStrict(dto.getProductId(), dto.getVariantId());
        
        // Check if item already exists in cart
        Optional<Cart> existingCart = cartRepository.findByUser_IdAndProductIdAndVariantId(
                userId, dto.getProductId(), dto.getVariantId());

        if (existingCart.isPresent()) {
            Cart cart = existingCart.get();
            int newQuantity = cart.getQuantity() + dto.getQuantity();
            validateQuantity(newQuantity);
            cart.setQuantity(newQuantity);
            cart.setPrice(productPrice);
            cart.setTotalAmount(productPrice.multiply(BigDecimal.valueOf(newQuantity)));
            BigDecimal discount = cart.getDiscountAmount() != null ? cart.getDiscountAmount() : BigDecimal.ZERO;
            BigDecimal shipping = cart.getShippingAmount() != null ? cart.getShippingAmount() : BigDecimal.ZERO;
            cart.setFinalAmount(cart.getTotalAmount().subtract(discount).add(shipping));
            cartRepository.save(cart);
            log.info("Updated existing cart item: cartId={}, newQuantity={}", cart.getId(), newQuantity);
        } else {
            Cart newCart = createCart(
                    userId,
                    dto.getProductId(),
                    dto.getVariantId(),
                    dto.getQuantity(),
                    productPrice
            );
            log.info("Created new cart item: cartId={}", newCart.getId());
        }

        // Get updated cart items
        List<Cart> cartItems = cartRepository.findAllByUser_Id(userId);
        return buildCartResponse(cartItems);
    }

    @Override
    @Transactional
    public CartResponseDTO getCart() {
        Long userId = securityUtil.getCurrentUserId();
        List<Cart> cartItems = cartRepository.findAllByUser_Id(userId);
        return buildCartResponse(cartItems);
    }

    @Override
@Transactional
public CartResponseDTO updateQuantity(Long itemId, Integer change) {

    log.info("Updating cart item quantity: itemId={}, change={}", itemId, change);

    Cart cart = cartRepository.findById(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

    validateCartOwnership(cart);

    int currentQuantity = cart.getQuantity();
    int newQuantity = currentQuantity + change;

    // ✅ FETCH VARIANT (CORRECT WAY)
    ProductVariant variant = productVariantRepository.findById(cart.getVariantId())
            .orElseThrow(() -> new CartException("Variant not found"));

    Integer stock = variant.getStock();

    // ✅ STRICT STOCK CHECK
    if (stock == null) {
        throw new CartException("Stock not defined for this variant");
    }

    if (stock <= 0) {
        throw new CartException("Stock unavailable");
    }

    if (newQuantity > stock) {
        throw new CartException("Only " + stock + " items available in stock");
    }

    // ✅ HANDLE REMOVE OR UPDATE
    if (newQuantity <= 0) {
        cartRepository.delete(cart);
        log.info("Item removed due to zero quantity: itemId={}", itemId);
    } else {
        // ✅ UPDATE QUANTITY
        BigDecimal productPrice = resolveUnitPriceStrict(cart.getProductId(), cart.getVariantId());

        cart.setQuantity(newQuantity);
        cart.setPrice(productPrice);
        cart.setTotalAmount(productPrice.multiply(BigDecimal.valueOf(newQuantity)));

        BigDecimal discount = cart.getDiscountAmount() != null ? cart.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal shipping = cart.getShippingAmount() != null ? cart.getShippingAmount() : BigDecimal.ZERO;

        cart.setFinalAmount(cart.getTotalAmount().subtract(discount).add(shipping));

        cartRepository.save(cart);
        log.info("Updated cart item quantity: itemId={}, newQuantity={}", itemId, newQuantity);
    }

    List<Cart> cartItems = cartRepository.findAllByUser_Id(securityUtil.getCurrentUserId());
    return buildCartResponse(cartItems);
}

    @Override
    @Transactional
    public CartResponseDTO removeItem(Long itemId) {
        log.info("Removing cart item: itemId={}", itemId);
        
        Cart cart = cartRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        
        validateCartOwnership(cart);
        
        cartRepository.delete(cart);
        
        List<Cart> cartItems = cartRepository.findAllByUser_Id(securityUtil.getCurrentUserId());
        return buildCartResponse(cartItems);
    }

    @Override
    @Transactional
    public void clearCart() {
        log.info("Clearing cart for user");
        
        Long userId = securityUtil.getCurrentUserId();
        List<Cart> cartItems = cartRepository.findAllByUser_Id(userId);
        
        cartRepository.deleteAll(cartItems);
    }

    @Override
    public Cart getCartItemById(Long itemId) {
        return cartRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
    }

    @Override
    public Integer getStockByVariantId(Long variantId) {
        if (variantId == null) return 0;
        return productVariantRepository.findById(variantId)
                .map(ProductVariant::getStock)
                .orElse(0);
    }

    @Override
    public Integer getCartCount() {
        Long userId = securityUtil.getCurrentUserId();
        List<Cart> cartItems = cartRepository.findAllByUser_Id(userId);

        // Return distinct product count (unique productIds), not total quantity
        long distinctProductCount = cartItems.stream()
                .map(Cart::getProductId)
                .distinct()
                .count();

        log.info("Cart count for user {}: {} distinct products", userId, distinctProductCount);
        return (int) distinctProductCount;
    }

    @Override
    @Transactional
    public CartResponseDTO applyCoupon(String code) {
        if (!code.equalsIgnoreCase("SAVE500")) {
            throw new CartException("Invalid coupon code");
        }

        Long userId = securityUtil.getCurrentUserId();
        List<Cart> cartItems = cartRepository.findAllByUser_Id(userId);
        
        for (Cart cart : cartItems) {
            cart.setCouponCode(code);
            cart.setDiscountAmount(BigDecimal.valueOf(500));
            cart.setFinalAmount(cart.getTotalAmount().subtract(cart.getDiscountAmount()).add(cart.getShippingAmount()));
            cartRepository.save(cart);
        }

        return buildCartResponse(cartItems);
    }

    private CartResponseDTO buildCartResponse(List<Cart> cartItems) {
        List<CartItemResponseDTO> itemDTOs = new ArrayList<>();
        List<Cart> cartsToPersist = new ArrayList<>();

        for (Cart cart : cartItems) {
            CartItemResponseDTO dto = new CartItemResponseDTO();
            dto.setItemId(cart.getId());
            dto.setProductId(cart.getProductId());
            dto.setVariantId(cart.getVariantId());

            // ✅ DEBUG: Log cart variantId before lookup
            log.info("[STOCK DEBUG] Cart itemId={}, variantId={}", cart.getId(), cart.getVariantId());

            // ✅ Fetch variant with product to ensure proper loading
            ProductVariant variant = null;
            if (cart.getVariantId() != null) {
                variant = productVariantRepository.findByIdWithProduct(cart.getVariantId()).orElse(null);
            }

            if (variant != null) {
                // ✅ Get stock directly from variant
                Integer stock = variant.getStock();

                // ✅ DEBUG: Log raw stock value from entity
                log.info("[STOCK DEBUG] variantId={}, rawStockFromDB={}", cart.getVariantId(), stock);

                // Handle null stock as 0
                int availableStock = (stock != null) ? stock : 0;

                dto.setAvailableStock(availableStock);
                dto.setOutOfStock(availableStock <= 0);

                log.info("[STOCK SET] itemId={}, variantId={}, availableStock={}, outOfStock={}",
                        cart.getId(), cart.getVariantId(), availableStock, availableStock <= 0);

            } else {
                log.warn("[STOCK DEBUG] Variant NOT FOUND for variantId={}", cart.getVariantId());
                dto.setAvailableStock(0);
                dto.setOutOfStock(true);
            }

            BigDecimal unitSell = resolveSellingForCartLine(variant, cart);
            BigDecimal rawMrp = variant != null ? variant.resolveMrpUnitPrice() : null;
            BigDecimal unitMrp = (rawMrp != null && rawMrp.compareTo(unitSell) > 0) ? rawMrp : unitSell;

            // ✅ PRICE CORRECTION BLOCK (if variant exists)
            if (variant != null) {
                // 🔥 FIX PRICE if wrong
                BigDecimal stored = cart.getPrice() != null ? cart.getPrice() : BigDecimal.ZERO;
                if (stored.compareTo(unitSell) != 0) {
                    cart.setPrice(unitSell);
                    cart.setTotalAmount(unitSell.multiply(BigDecimal.valueOf(cart.getQuantity())));
                    cart.setFinalAmount(cart.getTotalAmount()
                            .subtract(cart.getDiscountAmount())
                            .add(cart.getShippingAmount()));
                    if (!cartsToPersist.contains(cart)) {
                        cartsToPersist.add(cart);
                    }
                }
            }

            dto.setSellingPrice(unitSell);
            dto.setMrpPrice(unitMrp);
            dto.setPrice(unitSell);
            dto.setOriginalPrice(unitMrp);

            String productTitle = productRepository.findById(cart.getProductId())
                    .map(Product::getName)
                    .filter(n -> n != null && !n.isBlank())
                    .map(String::trim)
                    .orElse("Product " + cart.getProductId());
            dto.setName(productTitle);
            dto.setProductName(productTitle);

            dto.setImageUrl(getProductImageUrl(cart.getProductId()));
            dto.setQuantity(cart.getQuantity());
            dto.setTotal(unitSell.multiply(BigDecimal.valueOf(cart.getQuantity())));

            // ✅ Set size/color from variant (stock already set above)
            if (variant != null) {
                String sizeLabel = sizeColorMapper.getSizeName(variant.getSize());
                String colorLabel = sizeColorMapper.getColorName(variant.getColor());

                dto.setSize(sizeLabel);
                dto.setColor(colorLabel);
                dto.setColorName(colorLabel);
            }

            itemDTOs.add(dto);
        }

        if (!cartsToPersist.isEmpty()) {
            cartRepository.saveAll(cartsToPersist);
        }

        BigDecimal subtotal = itemDTOs.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal discount = cartItems.stream()
                .map(Cart::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal delivery = cartItems.stream()
                .map(Cart::getShippingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal finalTotal = subtotal.add(delivery).subtract(discount);

        PriceSummaryDTO summary = new PriceSummaryDTO();
        summary.setSubtotal(subtotal);
        summary.setDiscount(discount);
        summary.setDeliveryCharge(delivery);
        summary.setFinalTotal(finalTotal);

        // Calculate distinct product count (unique productIds)
        long distinctProductCount = cartItems.stream()
                .map(Cart::getProductId)
                .distinct()
                .count();

        CartResponseDTO response = new CartResponseDTO();
        response.setItems(itemDTOs);
        response.setPriceSummary(summary);
        response.setCouponApplied(cartItems.isEmpty() ? null : cartItems.get(0).getCouponCode());
        response.setProductCount((int) distinctProductCount);

        return response;
    }

   private Cart createCart(
           Long userId,
           Long productId,
           Long variantId,
           Integer quantity,
           BigDecimal price
   ) {

    if (variantId == null) {
        throw new CartException("Variant ID is required");
    }

    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    Cart cart = Cart.builder()
            .user(user)
            .sessionId("USER-" + userId)
            .productId(productId)
            .variantId(variantId) // ✅ FIXED (removed default 1L)
            .quantity(quantity != null ? quantity : 1)
            .price(price != null ? price : BigDecimal.ZERO)
            .deliveryType(com.ecommerce.authdemo.dto.Enum.DeliveryType.metro_metro)
            .totalAmount(price.multiply(BigDecimal.valueOf(quantity)))
            .discountAmount(BigDecimal.ZERO)
            .shippingAmount(BigDecimal.ZERO)
            .finalAmount(price.multiply(BigDecimal.valueOf(quantity)))
            .currency("USD")
            .build();

    return cartRepository.save(cart);
}
    private Cart getCartEntity() {
        Long userId = securityUtil.getCurrentUserId();
        return getOrCreateCart(userId);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUser_Id(userId)
                .orElseGet(() -> createCart(userId, 0L, 1L, 1, BigDecimal.ZERO));
    }

    private void validateAddToCartRequest(AddToCartDTO dto) {
        if (dto == null) {
            throw new CartException("Add to cart request cannot be null");
        }
        if (dto.getProductId() == null || dto.getProductId() <= 0) {
            throw new CartException("Valid product ID is required");
        }
        if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
            throw new CartException("Quantity must be greater than 0");
        }
        if (dto.getVariantId() == null || dto.getVariantId() <= 0) {
            throw new CartException("Valid variant ID is required");
        }
        validateQuantity(dto.getQuantity());
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new CartException("Quantity must be greater than 0");
        }
        if (quantity > 100) {
            throw new CartException("Maximum quantity allowed is 100");
        }
    }

    private void validateVariantStockLimit(Long variantId, Integer newQuantity) {

    if (variantId == null) {
        throw new CartException("Variant ID cannot be null");
    }

    ProductVariant variant = productVariantRepository.findByIdWithProduct(variantId)
            .orElseThrow(() -> new CartException("Variant not found"));

    Integer stock = variant.getStock();

    if (stock == null) {
        throw new CartException("Stock not defined");
    }

    if (stock <= 0) {
        throw new CartException("Out of stock");
    }

    if (newQuantity > stock) {
        throw new CartException("Only " + stock + " items available in stock");
    }
}

    private void validateCartOwnership(Cart cart) {
        Long currentUserId = securityUtil.getCurrentUserId();
        if (!cart.getUserId().equals(currentUserId)) {
            throw new CartException("Access denied: You can only modify your own cart");
        }
    }

    /**
     * Unit price when writing cart rows: validated variant + {@link ProductVariant#resolveSellingUnitPrice()},
     * else legacy stub (only if variant prices are missing in DB).
     */
    private BigDecimal resolveUnitPriceStrict(Long productId, Long variantId) {
        ProductVariant variant = productVariantRepository.findByIdWithProduct(variantId)
                .orElseThrow(() -> new CartException("Product variant not found"));
        if (variant.getProduct() == null || !variant.getProduct().getId().equals(productId)) {
            throw new CartException("Variant does not belong to this product");
        }
        BigDecimal fromVariant = variant.resolveSellingUnitPrice();
        if (fromVariant != null && fromVariant.compareTo(BigDecimal.ZERO) > 0) {
            return fromVariant;
        }
        return getProductPriceFallback(productId);
    }

    private BigDecimal getProductPriceFallback(Long productId) {
        return BigDecimal.valueOf(500 + (productId % 100) * 10);
    }

    /**
     * Selling unit for a cart row: variant {@link ProductVariant#resolveSellingUnitPrice()}, else stored cart price, else stub.
     */
    private BigDecimal resolveSellingForCartLine(ProductVariant variant, Cart cart) {
        if (variant != null) {
            BigDecimal v = variant.resolveSellingUnitPrice();
            if (v != null && v.compareTo(BigDecimal.ZERO) > 0) {
                return v;
            }
        }
        BigDecimal stored = cart.getPrice();
        if (stored != null && stored.compareTo(BigDecimal.ZERO) > 0) {
            return stored;
        }
        return getProductPriceFallback(cart.getProductId());
    }

    private BigDecimal calculateShipping(BigDecimal subtotal) {
        // Free shipping for orders above $1000
        if (subtotal.compareTo(BigDecimal.valueOf(1000)) >= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(99);
    }

    private String getProductImageUrl(Long productId) {
        try {
            String imagePath = null;
            
            // Try to get primary image first
            var primaryImage = productImageRepository.findTopByProductIdAndIsPrimaryTrue(productId);
            if (primaryImage != null && primaryImage.getImagePath() != null) {
                imagePath = primaryImage.getImagePath();
            } else {
                // If no primary image, get first image
                var firstImage = productImageRepository.findFirstByProductId(productId);
                if (firstImage.isPresent() && firstImage.get().getImagePath() != null) {
                    imagePath = firstImage.get().getImagePath();
                }
            }
            
            if (imagePath != null && !imagePath.isEmpty()) {
                // Construct full URL using base URL
                if (imagePath.startsWith("http")) {
                    return imagePath; // Already a full URL
                } else {
                    return publicBaseUrl + (imagePath.startsWith("/") ? imagePath : "/" + imagePath);
                }
            }
            
            // Fallback to placeholder if no images found
            return "https://via.placeholder.com/150";
        } catch (Exception e) {
            log.warn("Error fetching image for product {}: {}", productId, e.getMessage());
            return "https://via.placeholder.com/150";
        }
    }
}
