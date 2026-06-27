package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.SellerRegisterDTO;
import com.ecommerce.authdemo.entity.Seller;
import com.ecommerce.authdemo.repository.SellerRepository;
import com.ecommerce.authdemo.service.SellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SellerServiceImpl implements SellerService {

    private final SellerRepository sellerRepository;

    @Override
    public String registerSeller(SellerRegisterDTO registerDTO) {

        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            return "Passwords do not match!";
        }

        if (sellerRepository.findByEmail(registerDTO.getEmail()).isPresent()) {
            return "Email already registered!";
        }

        if (sellerRepository.findByMobileNumber(registerDTO.getMobile()).isPresent()) {
            return "Mobile number already registered!";
        }

        Seller seller = new Seller();
        seller.setFirstName(registerDTO.getFirstName());
        seller.setLastName(registerDTO.getLastName());
        seller.setMobileNumber(registerDTO.getMobile());
        seller.setEmail(registerDTO.getEmail());
        seller.setPassword(registerDTO.getPassword()); // Later we encrypt

        sellerRepository.save(seller);

        return "Seller registered successfully!";
    }

    @Override
    public String loginSeller(String email, String password) {

        Optional<Seller> sellerOptional = sellerRepository.findByEmail(email);

        if (sellerOptional.isEmpty()) {
            return "Seller not found!";
        }

        Seller seller = sellerOptional.get();

        if (!seller.getPassword().equals(password)) {
            return "Invalid password!";
        }

        return "Login successful!";
    }
}
