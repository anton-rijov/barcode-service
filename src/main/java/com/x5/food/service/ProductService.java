package com.x5.food.service;

import com.x5.food.repository.BarcodeRepository;
import com.x5.food.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final BarcodeRepository barcodeRepository;

    public String getData() {
        if (productRepository.count() > 0 || barcodeRepository.count() > 0) {
            return "1111";
        }
        return "0000";
    }

}