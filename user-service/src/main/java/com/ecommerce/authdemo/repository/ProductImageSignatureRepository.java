package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.ProductImageSignature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductImageSignatureRepository extends JpaRepository<ProductImageSignature, Long> {
    Optional<ProductImageSignature> findByImagePath(String imagePath);
    List<ProductImageSignature> findByImagePathIn(Collection<String> imagePaths);
}
