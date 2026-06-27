package com.ecommerce.authdemo.repository;


import com.ecommerce.authdemo.entity.Order;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

    public interface OrderAnalyticsRepository {

        @Query("SELECT o FROM Order o WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate")
        List<Order> findOrdersBetweenDates(
                @Param("startDate") String startDate,
                @Param("endDate") String endDate
        );
    }

