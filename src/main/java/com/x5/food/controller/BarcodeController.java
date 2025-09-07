package com.x5.food.controller;

import com.x5.food.dto.projection.BarcodeStatisticProjection;
import com.x5.food.dto.projection.ProductProjection;
import com.x5.food.service.BarcodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping(path = "api/barcode")
public class BarcodeController {

    private final BarcodeService barcodeService;

    public BarcodeController(BarcodeService barcodeService) {
        this.barcodeService = barcodeService;
    }

    @GetMapping(path = "{barcode}")
    public ResponseEntity<Map<String, Object>> getProductByBarcode(@PathVariable(name = "barcode") String barcode) {
        log.info("getProductByBarcode {}", barcode);

        Optional<ProductProjection> product = barcodeService.getProduct(barcode);
        if (product.isPresent()) {
            return ResponseEntity.ok(Map.of("product", product));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity getBarcodesStat() {
        log.info("getBarcodesStat");

        Optional<BarcodeStatisticProjection> stat = barcodeService.getBarcodeAndSkuCounts();
        if (stat.isPresent()) {
            return ResponseEntity.ok(stat.get());
        }
        return ResponseEntity.notFound().build();
    }

}
