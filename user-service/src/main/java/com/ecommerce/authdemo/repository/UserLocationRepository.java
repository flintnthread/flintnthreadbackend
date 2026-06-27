package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.UserLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface UserLocationRepository extends JpaRepository<UserLocation, Long> {

}