package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.cms.HomepageBanner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HomepageBannerRepository extends JpaRepository<HomepageBanner, Integer> {

    List<HomepageBanner> findBySectionIgnoreCaseOrderBySortOrderAscPositionAscIdAsc(String section);

    List<HomepageBanner> findAllByOrderBySectionAscSortOrderAscPositionAscIdAsc();
}
