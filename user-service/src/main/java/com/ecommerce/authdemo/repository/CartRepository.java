package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUser_Id(Long userId);

    List<Cart> findAllByUser_Id(Long userId);

    Optional<Cart> findByUser_IdAndProductIdAndVariantId(Long userId, Long productId, Long variantId);

    void deleteByUser_Id(Long userId);

    Integer countByUser_Id(Long userId);
}