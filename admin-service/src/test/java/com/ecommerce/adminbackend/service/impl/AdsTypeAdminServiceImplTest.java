package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.entity.ads.AdsType;
import com.ecommerce.adminbackend.repository.AdsTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdsTypeAdminServiceImplTest {

    @Mock
    private AdsTypeRepository repository;

    @InjectMocks
    private AdsTypeAdminServiceImpl service;

    @Test
    void createPersistsNormalizedStatus() {
        when(repository.save(any(AdsType.class))).thenAnswer(invocation -> {
            AdsType saved = invocation.getArgument(0);
            saved.setId(11);
            return saved;
        });

        Map<String, Object> created = service.create(Map.of(
                "name", "Banner Ads",
                "category", "Banner Ads",
                "description", "Homepage banner inventory",
                "status", "Active"));

        ArgumentCaptor<AdsType> captor = ArgumentCaptor.forClass(AdsType.class);
        verify(repository).save(captor.capture());
        assertEquals("active", captor.getValue().getStatus());
        assertEquals(11, created.get("id"));
        assertEquals("Banner Ads", created.get("name"));
    }

    @Test
    void getThrowsWhenMissing() {
        when(repository.findById(99)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.get(99));
    }
}
