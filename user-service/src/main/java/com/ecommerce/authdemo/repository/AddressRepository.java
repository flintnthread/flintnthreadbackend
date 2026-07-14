package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Integer> {

    List<Address> findByUserId(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Address a WHERE a.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    Optional<Address> findByUserIdAndIsDefaultTrue(Long userId);

    Optional<Address> findByIdAndUserId(Integer id, Long userId);

    List<Address> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);
}