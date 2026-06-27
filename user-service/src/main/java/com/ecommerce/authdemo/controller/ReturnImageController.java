package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.ReturnImageRequest;
import com.ecommerce.authdemo.dto.ReturnImageResponse;
import com.ecommerce.authdemo.service.ReturnImageService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/return-images")
@RequiredArgsConstructor
public class ReturnImageController {

    private final ReturnImageService
            returnImageService;


    @PostMapping
    public ReturnImageResponse create(
            @RequestBody
            ReturnImageRequest request
    ) {

        return returnImageService.create(
                request
        );
    }


    @GetMapping("/{returnId}")
    public List<ReturnImageResponse> getByReturnId(
            @PathVariable Long returnId
    ) {

        return returnImageService
                .getByReturnId(returnId);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id
    ) {

        returnImageService.delete(id);
    }

    @PostMapping(
            value = "/upload/{returnId}",
            consumes = "multipart/form-data"
    )
    public void uploadReturnImages(

            @PathVariable Long returnId,

            @RequestParam("files")
            List<MultipartFile> files
    ) {

        returnImageService.uploadReturnImages(
                returnId,
                files
        );
    }


    @GetMapping("/gallery/{returnId}")
    public List<ReturnImageResponse>
    getReturnImages(

            @PathVariable Long returnId
    ) {

        return returnImageService
                .getReturnImages(returnId);
    }
}