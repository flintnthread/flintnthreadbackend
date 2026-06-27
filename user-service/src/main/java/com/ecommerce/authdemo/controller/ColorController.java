package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.ColorDTO;
import com.ecommerce.authdemo.service.ColorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/colors")
public class ColorController {

    @Autowired
    private ColorService colorService;

    @GetMapping
    public ResponseEntity<List<ColorDTO>> getAllColors() {
        List<ColorDTO> colors = colorService.getAllColors();
        return ResponseEntity.ok(colors);
    }
}
