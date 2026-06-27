package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.SizeDTO;
import com.ecommerce.authdemo.service.SizeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sizes")
public class SizeController {

    @Autowired
    private SizeService sizeService;

    @GetMapping
    public ResponseEntity<List<SizeDTO>> getAllSizes() {
        List<SizeDTO> sizes = sizeService.getAllSizes();
        return ResponseEntity.ok(sizes);
    }
}
