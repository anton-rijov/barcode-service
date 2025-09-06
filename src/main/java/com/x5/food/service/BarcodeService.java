package com.x5.food.service;

import com.x5.food.dto.projection.ProductProjection;
import com.x5.food.repository.BarcodeRepository;
import com.x5.food.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BarcodeService {

    private final ProductRepository productRepository;
    private final BarcodeRepository barcodeRepository;

    public Optional<ProductProjection> getProduct(String barcode) {
        return barcodeRepository.findProductByBarcode(barcode);
    }

}