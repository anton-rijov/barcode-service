package com.x5.food.service;

import com.x5.food.dto.ProductResponse;
import com.x5.food.dto.projection.BarcodeStatisticProjection;
import com.x5.food.exception.ResourceNotFoundException;
import com.x5.food.external.ExternalProductService;
import com.x5.food.repository.BarcodeRepository;
import com.x5.food.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BarcodeService {

    private final ProductRepository productRepository;
    private final BarcodeRepository barcodeRepository;
    private final ExternalProductService externalProductService;

    @Transactional
    public ResponseWithStatus getProductByBarcode(String barcode) {
        // Сначала ищем в локальной базе
        Optional<ProductResponse> localProduct = productRepository.findByBarcode(barcode)
                .map(ProductResponse::fromEntity);

        if (localProduct.isPresent()) {
            // Возвращаем 200 OK при получении из БД
            return new ResponseWithStatus(HttpStatus.OK, localProduct.get());
        }

        // Если не найдено локально, запрашиваем внешний сервис
        Optional<ProductResponse> externalProduct = externalProductService.getProductByBarcode(barcode);

        // Сохраняем в базу, если данные получены из внешнего сервиса
        externalProduct.ifPresent(productResponse -> saveToDatabase(productResponse, barcode));

        // Возвращаем 201 Created при получении из внешнего API
        return externalProduct.map(product ->
                        new ResponseWithStatus(HttpStatus.CREATED, product))
                .orElse(new ResponseWithStatus(HttpStatus.NOT_FOUND, null));
    }

    private void saveToDatabase(ProductResponse productResponse, String barcode) {
        // UPSERT продукта
        productRepository.upsertProduct(productResponse.sku(), productResponse.name());

        // INSERT штрих-кода если не существует
        barcodeRepository.insertBarcodeIfNotExists(barcode, productResponse.sku());
    }

    public Optional<BarcodeStatisticProjection> getBarcodeAndSkuCounts() {
        return barcodeRepository.getBarcodeStatistics();
    }

    public void deleteBarcodeById(String barcode) {
        if (!barcodeRepository.existsByBarcode(barcode)) {
            throw new ResourceNotFoundException("Штрих-код не найден");
        }
        barcodeRepository.deleteById(barcode);
    }

    // Вспомогательный класс для возврата статуса и данных
    public record ResponseWithStatus(
            HttpStatus status,
            ProductResponse response
    ) {
    }
}