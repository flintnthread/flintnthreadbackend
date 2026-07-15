package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.*;
import com.ecommerce.authdemo.entity.Address;
import com.ecommerce.authdemo.entity.Cart;
import com.ecommerce.authdemo.entity.Product;
import com.ecommerce.authdemo.entity.ProductVariant;
import com.ecommerce.authdemo.entity.Seller;
import com.ecommerce.authdemo.entity.User;
import com.ecommerce.authdemo.dto.Enum.DeliveryType;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.exception.CartException;
import com.ecommerce.authdemo.repository.AddressRepository;
import com.ecommerce.authdemo.repository.CartRepository;
import com.ecommerce.authdemo.repository.ProductImageRepository;
import com.ecommerce.authdemo.repository.ProductRepository;
import com.ecommerce.authdemo.repository.ProductVariantRepository;
import com.ecommerce.authdemo.repository.SellerRepository;
import com.ecommerce.authdemo.repository.UserRepository;
import com.ecommerce.authdemo.service.CartService;
import com.ecommerce.authdemo.service.CustomerPriceResolver;
import com.ecommerce.authdemo.service.DeliveryZoneResolver;
import com.ecommerce.authdemo.util.ProductCatalogVisibility;
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
    private final CustomerPriceResolver customerPriceResolver;
    private final DeliveryZoneResolver deliveryZoneResolver;
    private final AddressRepository addressRepository;
    private final SellerRepository sellerRepository;
    
    @Value("${app.media.public-base-url}")
    private String publicBaseUrl;

    @Override
    @Transactional
    public CartResponseDTO addToCart(AddToCartDTO dto) {
        log.info("Adding item to cart: productId={}, variantId={}, quantity={}", 
                dto.getProductId(), dto.getVariantId(), dto.getQuantity());
        
        validateAddToCartRequest(dto);
        
        Long userId = securityUtil.getCurrentUserId();
        LinePricing linePricing = resolveLinePricing(dto.getProductId(), dto.getVariantId(), null);
        
        // Check if item already exists in cart
        Optional<Cart> existingCart = cartRepository.findByUser_IdAndProductIdAndVariantId(
                userId, dto.getProductId(), dto.getVariantId());

        if (existingCart.isPresent()) {
            Cart cart = existingCart.get();
            int newQuantity = cart.getQuantity() + dto.getQuantity();
            validateQuantity(newQuantity);
            cart.setQuantity(newQuantity);
            applyLinePricing(cart, linePricing);
            cartRepository.save(cart);
            log.info("Updated existing cart item: cartId={}, newQuantity={}", cart.getId(), newQuantity);
        } else {
            Cart newCart = createCart(
                    userId,
                    dto.getProductId(),
                    dto.getVariantId(),
                    dto.getQuantity(),
                    linePricing
            );
            log.info("Created new cart item: cartId={}", newCart.getId());
        }

        // Get updated cart items
        List<Cart> cartItems = cartRepository.findAllByUser_Id(userId);
        return buildCartResponse(cartItems, null);
    }

    @Override
    @Transactional
    public CartResponseDTO getCart() {
        Long userId = securityUtil.getCurrentUserId();
        List<Cart> cartItems = cartRepository.findAllByUser_Id(userId);
        return buildCartResponse(cartItems, null);
    }

    @Override
    @Transactional
    public CartResponseDTO getCart(Integer addressId) {
        Long userId = securityUtil.getCurrentUserId();
        List<Cart> cartItems = cartRepository.findAllByUser_Id(userId);
        Address address = loadUserAddress(userId, addressId);
        refreshCartDeliveryForAddress(cartItems, address);
        return buildCartResponse(cartItems, address);
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponseDTO previewCartForAddress(Integer addressId) {
        return getCart(addressId);
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
        cart.setQuantity(newQuantity);
        LinePricing linePricing = resolveLinePricing(cart.getProductId(), cart.getVariantId(), null);
        applyLinePricing(cart, linePricing);

        cartRepository.save(cart);
        log.info("Updated cart item quantity: itemId={}, newQuantity={}", itemId, newQuantity);
    }

    List<Cart> cartItems = cartRepository.findAllByUser_Id(securityUtil.getCurrentUserId());
    return buildCartResponse(cartItems, null);
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
        return buildCartResponse(cartItems, null);
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

        return buildCartResponse(cartItems, null);
    }

    private record LinePricing(
            DeliveryType deliveryType,
            BigDecimal baseUnitPrice,
            BigDecimal deliveryUnitCharge,
            BigDecimal customerUnitPrice) {}

    private CartResponseDTO buildCartResponse(List<Cart> cartItems, Address address) {
        if (address != null) {
            refreshCartDeliveryForAddress(cartItems, address);
        } else {
            refreshCartDeliveryForAddress(cartItems, null);
        }

        List<CartItemResponseDTO> itemDTOs = new ArrayList<>();

        for (Cart cart : cartItems) {
            CartItemResponseDTO dto = new CartItemResponseDTO();
            dto.setItemId(cart.getId());
            dto.setProductId(cart.getProductId());
            dto.setVariantId(cart.getVariantId());

            ProductVariant variant = null;
            if (cart.getVariantId() != null) {
                variant = productVariantRepository.findByIdWithProduct(cart.getVariantId()).orElse(null);
            }

            if (variant != null) {
                Integer stock = variant.getStock();
                int availableStock = stock != null ? stock : 0;
                dto.setAvailableStock(availableStock);
                dto.setOutOfStock(availableStock <= 0);
            } else {
                dto.setAvailableStock(0);
                dto.setOutOfStock(true);
            }

            LinePricing linePricing = resolveLinePricing(cart.getProductId(), cart.getVariantId(), address);
            BigDecimal unitCustomer = linePricing.customerUnitPrice();
            Product productForStrike = variant != null ? variant.getProduct() : null;
            BigDecimal unitMrp = customerPriceResolver.resolveCustomerStrikeMrp(productForStrike, variant);
            if (unitMrp == null) {
                unitMrp = unitCustomer;
            }

            dto.setSellingPrice(unitCustomer);
            dto.setMrpPrice(unitMrp);
            dto.setPrice(unitCustomer);
            dto.setOriginalPrice(unitMrp);
            dto.setDeliveryCharge(linePricing.deliveryUnitCharge());

            String productTitle = productRepository.findById(cart.getProductId())
                    .map(Product::getName)
                    .filter(n -> n != null && !n.isBlank())
                    .map(String::trim)
                    .orElse("Product " + cart.getProductId());
            dto.setName(productTitle);
            dto.setProductName(productTitle);
            dto.setImageUrl(getProductImageUrl(cart.getProductId()));
            dto.setQuantity(cart.getQuantity());
            dto.setTotal(unitCustomer.multiply(BigDecimal.valueOf(cart.getQuantity())));

            if (variant != null) {
                dto.setSize(sizeColorMapper.getSizeName(variant.getSize()));
                dto.setColor(sizeColorMapper.getColorName(variant.getColor()));
                dto.setColorName(sizeColorMapper.getColorName(variant.getColor()));
            }

            itemDTOs.add(dto);
        }

        BigDecimal subtotal = cartItems.stream()
                .map(cart -> cart.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = cartItems.stream()
                .map(Cart::getDiscountAmount)
                .map(amount -> amount != null ? amount : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal delivery = cartItems.stream()
                .map(Cart::getShippingAmount)
                .map(amount -> amount != null ? amount : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal finalTotal = subtotal.add(delivery).subtract(discount);

        PriceSummaryDTO summary = new PriceSummaryDTO();
        summary.setSubtotal(subtotal);
        summary.setDiscount(discount);
        summary.setDeliveryCharge(delivery);
        summary.setFinalTotal(finalTotal);

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

    private void refreshCartDeliveryForAddress(List<Cart> cartItems, Address address) {
        List<Cart> changed = new ArrayList<>();
        for (Cart cart : cartItems) {
            LinePricing before = linePricingFromCart(cart);
            LinePricing after = resolveLinePricing(cart.getProductId(), cart.getVariantId(), address);
            if (before.deliveryType() != after.deliveryType()
                    || before.baseUnitPrice().compareTo(after.baseUnitPrice()) != 0
                    || before.deliveryUnitCharge().compareTo(after.deliveryUnitCharge()) != 0) {
                applyLinePricing(cart, after);
                changed.add(cart);
            }
        }
        if (!changed.isEmpty()) {
            cartRepository.saveAll(changed);
        }
    }

    private LinePricing linePricingFromCart(Cart cart) {
        BigDecimal base = cart.getPrice() != null ? cart.getPrice() : BigDecimal.ZERO;
        int qty = cart.getQuantity() != null && cart.getQuantity() > 0 ? cart.getQuantity() : 1;
        BigDecimal lineShipping = cart.getShippingAmount() != null ? cart.getShippingAmount() : BigDecimal.ZERO;
        BigDecimal deliveryUnit = lineShipping.divide(BigDecimal.valueOf(qty), 2, java.math.RoundingMode.HALF_UP);
        DeliveryType type = cart.getDeliveryType() != null ? cart.getDeliveryType() : DeliveryType.metro_metro;
        return new LinePricing(type, base, deliveryUnit, base.add(deliveryUnit));
    }

    private void applyLinePricing(Cart cart, LinePricing pricing) {
        int qty = cart.getQuantity() != null && cart.getQuantity() > 0 ? cart.getQuantity() : 1;
        cart.setPrice(pricing.baseUnitPrice());
        cart.setDeliveryType(pricing.deliveryType());
        BigDecimal lineShipping = pricing.deliveryUnitCharge().multiply(BigDecimal.valueOf(qty));
        cart.setShippingAmount(lineShipping);
        cart.setTotalAmount(pricing.baseUnitPrice().multiply(BigDecimal.valueOf(qty)));
        BigDecimal discount = cart.getDiscountAmount() != null ? cart.getDiscountAmount() : BigDecimal.ZERO;
        cart.setFinalAmount(cart.getTotalAmount().subtract(discount).add(lineShipping));
    }

    private LinePricing resolveLinePricing(Long productId, Long variantId, Address address) {
        ProductVariant variant = productVariantRepository.findByIdWithProduct(variantId)
                .orElseThrow(() -> new CartException("Product variant not found"));
        Product product = variant.getProduct();
        if (product == null || !product.getId().equals(productId)) {
            throw new CartException("Variant does not belong to this product");
        }
        if (!ProductCatalogVisibility.isVisibleToUsers(product)) {
            throw new CartException("Product is not available for purchase");
        }

        DeliveryType deliveryType = null;
        if (address != null && product.getSellerId() != null) {
            Seller seller = sellerRepository.findById(product.getSellerId()).orElse(null);
            String sellerPincode = seller != null ? seller.getPincode() : null;
            deliveryType = deliveryZoneResolver.resolveDeliveryType(
                    sellerPincode, address.getPincode(), address.getCity());
        }

        CustomerPriceResolver.ResolvedPrice resolved =
                customerPriceResolver.resolve(product, variant, deliveryType);
        if (resolved == null) {
            throw new CartException("Price not available for this product variant");
        }

        return new LinePricing(
                deliveryType,
                resolved.priceBeforeDelivery(),
                resolved.deliveryCharge(),
                resolved.customerPrice());
    }

    private Address loadUserAddress(Long userId, Integer addressId) {
        if (addressId == null) {
            return null;
        }
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found for current user"));
    }

    private Cart createCart(
            Long userId,
            Long productId,
            Long variantId,
            Integer quantity,
            LinePricing pricing) {

        if (variantId == null) {
            throw new CartException("Variant ID is required");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Cart cart = Cart.builder()
                .user(user)
                .sessionId("USER-" + userId)
                .productId(productId)
                .variantId(variantId)
                .quantity(quantity != null ? quantity : 1)
                .price(pricing.baseUnitPrice())
                .deliveryType(pricing.deliveryType())
                .discountAmount(BigDecimal.ZERO)
                .currency("INR")
                .build();
        applyLinePricing(cart, pricing);
        return cartRepository.save(cart);
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
