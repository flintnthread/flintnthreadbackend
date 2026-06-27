package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.ColorDTO;
import com.ecommerce.authdemo.entity.Color;
import com.ecommerce.authdemo.repository.ColorRepository;
import com.ecommerce.authdemo.service.ColorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ColorServiceImpl implements ColorService {

    @Autowired
    private ColorRepository colorRepository;

    @Override
    public List<ColorDTO> getAllColors() {
        List<Color> colors = colorRepository.findAll();
        return colors.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ColorDTO convertToDTO(Color color) {
        ColorDTO dto = new ColorDTO();
        dto.setId(color.getId());
        dto.setName(color.getName());
        dto.setCode(color.getCode());
        dto.setHex(color.getHex());
        return dto;
    }
}
