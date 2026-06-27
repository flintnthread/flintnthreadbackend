package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    List<SearchHistory> findTop20ByUserIdOrderBySearchedAtDesc(Long userId);

    List<SearchHistory> findTop20BySessionIdOrderBySearchedAtDesc(String sessionId);

    void deleteByUserId(Long userId);

    void deleteBySessionId(String sessionId);
}
