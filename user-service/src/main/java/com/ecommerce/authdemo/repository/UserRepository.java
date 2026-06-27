package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByContactNumber(String contactNumber);

    Optional<User> findByUsername(String username);

    Optional<User> findByReferralCode(String referralCode);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

}