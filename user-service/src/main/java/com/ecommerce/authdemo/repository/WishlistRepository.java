package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.dto.WishlistResponse;
import com.ecommerce.authdemo.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist,Integer> {

    List<Wishlist> findByUserId(Long userId);

    Optional<Wishlist> findByUserIdAndProductId(Long userId, Long productId);

    Optional<Wishlist> findByUserIdAndProductIdAndVariantId(Long userId, Long productId, Long variantId);

    void deleteByUserIdAndProductId(Long userId,Long productId);

    void deleteByUserIdAndProductIdAndVariantId(Long userId, Long productId, Long variantId);

    @Query("SELECT w FROM Wishlist w JOIN FETCH w.product WHERE w.user.id = :userId")
    List<Wishlist> findByUserIdWithProduct(@Param("userId") Long userId);

    @Query("SELECT w FROM Wishlist w JOIN FETCH w.product WHERE w.user.id = :userId")
    List<Wishlist> findWishlistWithProduct(@Param("userId") Long userId);

    long countByUserId(Long userId);
}