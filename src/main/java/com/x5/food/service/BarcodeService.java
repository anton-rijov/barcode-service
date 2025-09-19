package com.x5.food.service;

import com.x5.food.dto.OpenFoodFactsResponse;
import com.x5.food.dto.ProductResponse;
import com.x5.food.dto.projection.BarcodeStatisticProjection;
import com.x5.food.exception.ApiResponseFormatException;
import com.x5.food.exception.ResourceNotFoundException;
import com.x5.food.repository.BarcodeRepository;
import com.x5.food.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BarcodeService {

    private final ProductRepository productRepository;
    private final BarcodeRepository barcodeRepository;
    private final RestTemplate restTemplate;

    @Value("${external.api.url}")
    private String externalApiUrl;

    @Value("${external.retry.delay:1000}")
    private long retryDelay;

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
        Optional<ProductResponse> externalProduct = getProductFromExternalService(barcode);

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

    @Retryable(
            retryFor = {
                    HttpServerErrorException.class,
                    HttpClientErrorException.class,
                    ResourceAccessException.class
            },
            exclude = {
                    ApiResponseFormatException.class // Не retry для бизнес-ошибок
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public Optional<ProductResponse> getProductFromExternalService(String barcode) {
        try {
            String url = externalApiUrl + barcode;
            OpenFoodFactsResponse response = restTemplate.getForObject(url, OpenFoodFactsResponse.class);

            if (response != null && response.product() != null) {
                var productName = response.product().productName();
                if (productName == null || productName.isBlank()) {
                    throw new ApiResponseFormatException("Invalid Api response - empty product name");
                }
                return Optional.of(ProductResponse.fromExternal(response, barcode));
            }
        } catch (Exception e) {
            log.error("Error when fetching from external service for barcode {}.", barcode, e);
            throw e; // Важно: пробрасываем исключение для работы retry
        }
        return Optional.empty();
    }

    // Метод для обработки после исчерпания попыток
    @Recover
    public Optional<ProductResponse> recoverGetProductFromExternalService(Exception e, String barcode) {
        log.warn("All retry attempts failed for barcode: {}", barcode, e);
        return Optional.empty();
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
