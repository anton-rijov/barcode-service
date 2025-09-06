package com.x5.food.controller;

import com.x5.food.dto.projection.ProductProjection;
import com.x5.food.service.BarcodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(path = "api/barcode")
public class BarcodeController {

    private final BarcodeService barcodeService;

    public BarcodeController(BarcodeService barcodeService) {
        this.barcodeService = barcodeService;
    }

    @GetMapping(name = "/get", path = "{barcode}")
    public ResponseEntity<Map<String, Object>> getProductByBarcode(@PathVariable(name = "barcode") String barcode) {
        Optional<ProductProjection> product = barcodeService.getProduct(barcode);
        if (product.isPresent()) {
            return ResponseEntity.ok(Map.of("product", product));
        }
        return ResponseEntity.notFound().build();
    }

}
