package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.entity.Address;
import com.ecommerce.authdemo.repository.AddressRepository;
import com.ecommerce.authdemo.repository.DeliveryPincodeRepository;
import com.ecommerce.authdemo.service.impl.AddressServiceImpl;
import com.ecommerce.authdemo.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceDeleteTest {

    @Mock
    private AddressRepository addressRepository;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private DeliveryPincodeRepository deliveryPincodeRepository;

    @InjectMocks
    private AddressServiceImpl addressService;

    private static final Long USER_ID = 42L;

    @BeforeEach
    void setUp() {
        when(securityUtil.getCurrentUserId()).thenReturn(USER_ID);
    }

    @Test
    void deleteAddress_deletesAddress() {
        Address address = Address.builder()
                .id(55)
                .userId(USER_ID)
                .name("Current Location")
                .isDefault(false)
                .build();

        when(addressRepository.findByIdAndUserId(55, USER_ID)).thenReturn(Optional.of(address));

        addressService.deleteAddress(55);

        verify(addressRepository).delete(address);
        verify(addressRepository).flush();
    }

    @Test
    void deleteAddress_whenDefault_promotesRemainingAddress() {
        Address toDelete = Address.builder()
                .id(55)
                .userId(USER_ID)
                .name("Current Location")
                .isDefault(true)
                .build();

        Address remaining = Address.builder()
                .id(56)
                .userId(USER_ID)
                .name("Home")
                .isDefault(false)
                .build();

        when(addressRepository.findByIdAndUserId(55, USER_ID)).thenReturn(Optional.of(toDelete));
        when(addressRepository.findByUserId(USER_ID)).thenReturn(List.of(remaining));

        addressService.deleteAddress(55);

        verify(addressRepository).delete(toDelete);
        verify(addressRepository).flush();

        ArgumentCaptor<Address> saved = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(saved.capture());
        assertEquals(56, saved.getValue().getId());
        assertTrue(Boolean.TRUE.equals(saved.getValue().getIsDefault()));
    }

    @Test
    void deleteAddress_notFound_throws() {
        when(addressRepository.findByIdAndUserId(eq(99), eq(USER_ID))).thenReturn(Optional.empty());

        try {
            addressService.deleteAddress(99);
            throw new AssertionError("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("Address not found", ex.getMessage());
        }
    }
}
