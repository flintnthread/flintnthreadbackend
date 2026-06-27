package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

    Optional<Otp> findTopByIdentifierOrderByExpiryTimeDesc(String identifier);

    void deleteByIdentifier(String identifier);
}

