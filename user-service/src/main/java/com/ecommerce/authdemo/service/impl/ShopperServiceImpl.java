package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.CreateShopperDTO;
import com.ecommerce.authdemo.dto.ShopperDTO;
import com.ecommerce.authdemo.entity.Shopper;
import com.ecommerce.authdemo.entity.User;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.repository.ShopperRepository;
import com.ecommerce.authdemo.service.ShopperService;
import com.ecommerce.authdemo.util.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

    @Transactional
    @Service
    @RequiredArgsConstructor
    public class ShopperServiceImpl implements ShopperService {

        private final ShopperRepository shopperRepository;
        private final SecurityUtil securityUtil;

        @Override
        public List<ShopperDTO> getAll() {

            User user = securityUtil.getCurrentUser();

            return shopperRepository.findByUserId(user.getId())
                    .stream()
                    .map(s -> new ShopperDTO(
                            s.getId(),
                            s.getName(),
                            s.getIsActive()
                    ))
                    .toList();
        }

        @Override
        public ShopperDTO create(CreateShopperDTO dto) {

            User user = securityUtil.getCurrentUser();

            Shopper shopper = new Shopper();
            shopper.setName(dto.getName());
            shopper.setUser(user);
            shopper.setIsActive(false);

            shopperRepository.save(shopper);

            return new ShopperDTO(shopper.getId(), shopper.getName(), false);
        }

        @Override
        public void activate(Integer id) {

            User user = securityUtil.getCurrentUser();

            List<Shopper> shoppers = shopperRepository.findByUserId(user.getId());

            boolean found = false;

            for (Shopper s : shoppers) {
                if (s.getId().equals(id)) {
                    s.setIsActive(true);
                    found = true;
                } else {
                    s.setIsActive(false);
                }
            }

            if (!found) {
                throw new ResourceNotFoundException("Shopper not found");
            }

            shopperRepository.saveAll(shoppers);
        }

        @Override
        public void delete(Integer id) {
            User user = securityUtil.getCurrentUser();

            Shopper shopper = shopperRepository.findByIdAndUser_Id(id, user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Shopper not found"));

            shopperRepository.delete(shopper);
        }
    }

