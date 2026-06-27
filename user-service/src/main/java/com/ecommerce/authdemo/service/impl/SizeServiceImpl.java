package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.SizeDTO;
import com.ecommerce.authdemo.entity.Size;
import com.ecommerce.authdemo.repository.SizeRepository;
import com.ecommerce.authdemo.service.SizeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SizeServiceImpl implements SizeService {

    @Autowired
    private SizeRepository sizeRepository;

    @Override
    public List<SizeDTO> getAllSizes() {
        List<Size> sizes = sizeRepository.findAll();
        return sizes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private SizeDTO convertToDTO(Size size) {
        SizeDTO dto = new SizeDTO();
        dto.setId(size.getId());
        dto.setName(size.getName());
        dto.setCode(size.getCode());
        return dto;
    }
}
