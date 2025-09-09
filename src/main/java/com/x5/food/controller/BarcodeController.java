package com.x5.food.controller;

import com.x5.food.service.BarcodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(path = "api/barcode")
@RequiredArgsConstructor
public class BarcodeController {

    private final BarcodeService barcodeService;

    @GetMapping("/{barcode}")
    public ResponseEntity<?> getProductByBarcode(@PathVariable String barcode) {
        return barcodeService.getProductByBarcode(barcode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<?> getBarcodesStat() {
        log.info("getBarcodesStat");
        return barcodeService.getBarcodeAndSkuCounts()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}