package com.x5.food.controller;

import com.x5.food.dto.ProductResponse;
import com.x5.food.exception.BadRequestException;
import com.x5.food.exception.ResourceNotFoundException;
import com.x5.food.service.BarcodeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(path = "api/barcode")
@RequiredArgsConstructor
public class BarcodeController {

    private final BarcodeService barcodeService;

    @GetMapping("/{barcode}")
    public ResponseEntity<ProductResponse> getProductByBarcode(@PathVariable String barcode) {
        try {
            if (barcode == null || barcode.isEmpty()) {
                throw new BadRequestException("Штрих-код не может быть пустым");
            }

            BarcodeService.ResponseWithStatus responseWithStatus = barcodeService.getProductByBarcode(barcode);

            if (responseWithStatus.response() == null) {
                throw new ResourceNotFoundException("Продукт с таким штрих-кодом не найден");
            }

            return ResponseEntity.status(responseWithStatus.status()).body(responseWithStatus.response());
        } catch (Exception e) {
            throw new RuntimeException(e); // будет обработано глобальным обработчиком
        }
    }

    @DeleteMapping("/{barcode}")
    public ResponseEntity<String> deleteBarcodeById(@PathVariable String barcode, HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();

        // Проверяем, является ли запрос локальным
        if (!isLocalIp(clientIp)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Доступ запрещен");
        }
        barcodeService.deleteBarcodeById(barcode);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping
    public ResponseEntity<?> getBarcodesStat() {
        log.info("getBarcodesStat");
        return barcodeService.getBarcodeAndSkuCounts()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private boolean isLocalIp(String ip) {
        return "127.0.0.1".equals(ip) || "localhost".equals(ip) || "::1".equals(ip);
    }
}