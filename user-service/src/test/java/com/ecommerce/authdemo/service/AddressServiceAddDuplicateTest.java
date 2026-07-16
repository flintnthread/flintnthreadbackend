package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.AddressRequest;
import com.ecommerce.authdemo.entity.Address;
import com.ecommerce.authdemo.entity.User;
import com.ecommerce.authdemo.repository.AddressRepository;
import com.ecommerce.authdemo.repository.DeliveryPincodeRepository;
import com.ecommerce.authdemo.repository.OrderRepository;
import com.ecommerce.authdemo.service.impl.AddressServiceImpl;
import com.ecommerce.authdemo.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceAddDuplicateTest {

    @Mock
    private AddressRepository addressRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private DeliveryPincodeRepository deliveryPincodeRepository;

    @InjectMocks
    private AddressServiceImpl addressService;

    private static final Long USER_ID = 228L;

    @BeforeEach
    void setUp() {
        when(securityUtil.getCurrentUserId()).thenReturn(USER_ID);
        when(deliveryPincodeRepository.existsServiceablePincode("502319")).thenReturn(true);
    }

    @Test
    void addAddress_sameLineAndPincode_returnsExistingWithoutNewSave() {
        Address existing = Address.builder()
                .id(87)
                .userId(USER_ID)
                .name("Adari Kusuma")
                .addressLine1("Krushi defence colony")
                .city("hyderabad")
                .state("Telangana")
                .pincode("502319")
                .isDefault(true)
                .build();

        when(addressRepository.findByUserId(USER_ID)).thenReturn(List.of(existing));

        AddressRequest request = new AddressRequest();
        request.setName("Adari Kusuma");
        request.setAddressLine1("Krushi defence colony");
        request.setCity("hyderabad");
        request.setState("Telangana");
        request.setPincode("502319");
        request.setAddressType("home");
        request.setIsDefault(true);

        Address result = addressService.addAddress(request);

        assertSame(existing, result);
        assertEquals(87, result.getId());
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    void addAddress_sameGps_returnsExisting() {
        Address existing = Address.builder()
                .id(55)
                .userId(USER_ID)
                .name("Current Location")
                .addressLine1("Patancheru")
                .city("hyderabad")
                .pincode("502319")
                .latitude(17.5310)
                .longitude(78.2650)
                .isDefault(false)
                .build();

        when(addressRepository.findByUserId(USER_ID)).thenReturn(List.of(existing));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        AddressRequest request = new AddressRequest();
        request.setName("Current Location");
        request.setAddressLine1("Patancheru nearby");
        request.setCity("hyderabad");
        request.setPincode("502319");
        request.setAddressType("home");
        request.setLatitude(17.5311);
        request.setLongitude(78.2651);
        request.setIsDefault(true);

        Address result = addressService.addAddress(request);

        assertEquals(55, result.getId());
        verify(addressRepository).save(existing); // default + coords refresh
    }

    @Test
    void addAddress_newLocation_createsRow() {
        when(addressRepository.findByUserId(USER_ID)).thenReturn(List.of());
        User user = new User();
        user.setEmail("a@b.com");
        user.setContactNumber("9999999999");
        when(securityUtil.getCurrentUser()).thenReturn(user);
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> {
            Address a = inv.getArgument(0);
            a.setId(100);
            return a;
        });

        AddressRequest request = new AddressRequest();
        request.setName("Home");
        request.setAddressLine1("New street 12");
        request.setCity("hyderabad");
        request.setState("Telangana");
        request.setPincode("502319");
        request.setAddressType("home");

        Address result = addressService.addAddress(request);

        assertEquals(100, result.getId());
        verify(addressRepository).save(any(Address.class));
    }
}
