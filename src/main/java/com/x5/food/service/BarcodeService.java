package com.x5.food.service;

import com.x5.food.dto.projection.BarcodeStatisticProjection;
import com.x5.food.entity.Barcode;
import com.x5.food.entity.Product;
import com.x5.food.repository.BarcodeRepository;
import com.x5.food.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BarcodeService {

    private final ProductRepository productRepository;
    private final BarcodeRepository barcodeRepository;

    public Optional<ProductResponse> getProductByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode)
                .map(ProductResponse::fromEntity);
    }

    public Optional<BarcodeStatisticProjection> getBarcodeAndSkuCounts() {
        return barcodeRepository.getBarcodeStatistics();
    }

    //dto
    public record ProductResponse(
            String sku,
            String name,
            List<String> barcodes
    ) {
        public static ProductResponse fromEntity(Product product) {
            List<String> barcodeList = product.getBarcodes().stream()
                    .map(Barcode::getBarcode)
                    .collect(Collectors.toList());

            return new ProductResponse(
                    product.getSku(),
                    product.getName(),
                    barcodeList
            );
        }
    }
}