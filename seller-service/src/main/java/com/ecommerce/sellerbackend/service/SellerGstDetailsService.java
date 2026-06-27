package com.ecommerce.sellerbackend.service;


import com.ecommerce.sellerbackend.dto.profile.GstVerifyResponse;
import com.ecommerce.sellerbackend.dto.SellerGstDetailsDto;
import com.ecommerce.sellerbackend.entity.SellerGstDetails;

public interface SellerGstDetailsService {

        SellerGstDetails saveOrUpdate(
                Integer sellerId,
                GstVerifyResponse response);

        SellerGstDetailsDto getBySellerId(Integer sellerId);
    }

