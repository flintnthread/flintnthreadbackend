package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.CreateShopperDTO;
import com.ecommerce.authdemo.dto.ShopperDTO;

import java.util.List;

    public interface ShopperService {

        List<ShopperDTO> getAll();

        ShopperDTO create(CreateShopperDTO dto);

        void activate(Integer id);

        void delete(Integer id);
    }

