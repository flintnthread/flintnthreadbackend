package com.ecommerce.authdemo.repository;


import com.ecommerce.authdemo.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartId(Long cartId);

    Optional<CartItem> findByCartIdAndProductIdAndVariantId(
            Long cartId, Long productId, Long variantId
    );
    
}
