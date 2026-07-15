package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.AddressRequest;
import com.ecommerce.authdemo.dto.ApiResponse;
import com.ecommerce.authdemo.entity.Address;
import com.ecommerce.authdemo.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    public ResponseEntity<ApiResponse<Address>> add(@Valid @RequestBody AddressRequest request) {
        Address address = addressService.addAddress(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Address saved successfully", address));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Address>>> getAll() {
        List<Address> list = addressService.getUserAddresses();
        return ResponseEntity.ok(new ApiResponse<>(true, "Address list fetched", list));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Address>> update(@PathVariable Integer id,
                                                       @Valid @RequestBody AddressRequest request) {
        Address updated = addressService.updateAddress(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Address updated successfully", updated));
    }

    @DeleteMapping("/all")
    public ResponseEntity<ApiResponse<String>> deleteAllForCurrentUser() {
        addressService.deleteAllForCurrentUser();
        return ResponseEntity.ok(new ApiResponse<>(true, "All addresses deleted successfully", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Integer id) {
        addressService.deleteAddress(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Address deleted successfully", null));
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<ApiResponse<Address>> setDefault(@PathVariable Integer id) {
        Address address = addressService.setDefaultAddress(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Default address updated", address));
    }

    @GetMapping("/default")
    public ResponseEntity<ApiResponse<Address>> getDefault() {
        Address address = addressService.getDefaultAddress();
        return ResponseEntity.ok(new ApiResponse<>(true, "Default address fetched", address));
    }
}