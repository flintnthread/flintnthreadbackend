package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.AddressRequest;
import com.ecommerce.authdemo.entity.Address;

import java.util.List;

public interface AddressService {

    Address addAddress(AddressRequest request);

    List<Address> getUserAddresses();

    Address updateAddress(Integer id, AddressRequest request);

    void deleteAddress(Integer id);

    Address setDefaultAddress(Integer addressId);

    Address getDefaultAddress();

    void deleteAllForCurrentUser();
}