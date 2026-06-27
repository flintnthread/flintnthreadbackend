package com.ecommerce.sellerbackend.service.impl;


import com.ecommerce.sellerbackend.dto.profile.GstVerifyResponse;
import com.ecommerce.sellerbackend.dto.SellerGstDetailsDto;
import com.ecommerce.sellerbackend.entity.SellerGstDetails;
import com.ecommerce.sellerbackend.repository.SellerGstDetailsRepository;
import com.ecommerce.sellerbackend.service.SellerGstDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
    @RequiredArgsConstructor
    public class SellerGstDetailsServiceImpl
            implements SellerGstDetailsService {

        private final SellerGstDetailsRepository repository;

        @Override
        public SellerGstDetails saveOrUpdate(
                Integer sellerId,
                GstVerifyResponse response) {

            SellerGstDetails gst =
                    repository.findBySellerId(sellerId)
                            .orElse(new SellerGstDetails());

            gst.setSellerId(sellerId);

            gst.setGstin(response.getGstNumber());
            gst.setLegalName(response.getBusinessName());
            gst.setTradeName(response.getTradeName());

            gst.setGstStatus(response.getStatus());
            gst.setTaxpayerType(response.getTaxpayerType());
            gst.setConstitution(response.getBusinessType());

            gst.setRegistrationDate(
                    response.getRegistrationDate());

            gst.setCancellationDate(
                    response.getCancellationDate());

            gst.setStateJurisdiction(
                    response.getStateJurisdiction());

            gst.setCentreJurisdiction(
                    response.getCentreJurisdiction());

            gst.setPrincipalPlace(
                    response.getPrincipalPlaceType());

            gst.setPan(response.getPanNumber());

            gst.setAddress(response.getAddress());

            gst.setCity(response.getCity());

            gst.setState(response.getState());

            gst.setPincode(response.getPincode());

            gst.setVerified(true);

            return repository.save(gst);
        }

        @Override
        public SellerGstDetailsDto getBySellerId(
                Integer sellerId) {

            SellerGstDetails gst =
                    repository.findBySellerId(sellerId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "GST details not found"));

            SellerGstDetailsDto dto =
                    new SellerGstDetailsDto();

            BeanUtils.copyProperties(gst, dto);

            return dto;
        }
    }

