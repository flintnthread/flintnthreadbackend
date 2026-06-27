package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.SellerRegisterDTO;
import com.ecommerce.authdemo.entity.Seller;

public interface SellerService {

    String registerSeller(SellerRegisterDTO registerDTO);

    String loginSeller(String email, String password);
}

