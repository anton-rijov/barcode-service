package com.x5.food.service;

import com.x5.food.dto.OpenFoodFactsResponse;
import com.x5.food.dto.projection.BarcodeStatisticProjection;
import com.x5.food.entity.Barcode;
import com.x5.food.entity.Product;
import com.x5.food.repository.BarcodeRepository;
import com.x5.food.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BarcodeService {

    private final ProductRepository productRepository;
    private final BarcodeRepository barcodeRepository;
    private final RestTemplate restTemplate;

    @Value("${external.api.url:https://world.openfoodfacts.net/api/v2/product/}")
    private String externalApiUrl;

    public Optional<ProductResponse> getProductByBarcode(String barcode) {

        // Сначала ищем в локальной базе
        Optional<ProductResponse> localProduct = productRepository.findByBarcode(barcode)
                .map(ProductResponse::fromEntity);

        if (localProduct.isPresent()) {
            return localProduct;
        }

        // Если не найдено локально, запрашиваем внешний сервис
        return getProductFromExternalService(barcode);
    }

    private Optional<ProductResponse> getProductFromExternalService(String barcode) {
        try {
            String url = externalApiUrl + barcode;
            OpenFoodFactsResponse response = restTemplate.getForObject(url, OpenFoodFactsResponse.class);

            if (response != null && response.product() != null) {
                return Optional.of(convertExternalResponseToProductResponse(response, barcode));
            }
        } catch (Exception e) {
            System.out.println("Error fetching from external service: " + e.getMessage());
        }
        return Optional.empty();
    }

    private ProductResponse convertExternalResponseToProductResponse(OpenFoodFactsResponse externalResponse, String barcode) {
        var product = externalResponse.product();

        // Формируем name: конкатенация имени, количества и бренда
        String name = buildProductName(product);

        // Формируем sku: 'SKU_' + последние 6 цифр штрих-кода
        String sku = "SKU_" + getLastSixDigits(barcode);

        return new ProductResponse(
                sku,
                name,
                List.of(barcode) // только один штрих-код из запроса
        );
    }

    private String buildProductName(OpenFoodFactsResponse.Product product) {
        StringBuilder nameBuilder = new StringBuilder();

        if (product.productName() != null && !product.productName().isEmpty()) {
            nameBuilder.append(product.productName());
        } else if (product.brands() != null && !product.brands().isEmpty()) {
            nameBuilder.append(product.brands());
        } else {
            nameBuilder.append("Unknown Product");
        }

        // Добавляем количество, если есть
        if (product.quantity() != null && !product.quantity().isEmpty()) {
            nameBuilder.append(" ").append(product.quantity());
        }

        return nameBuilder.toString();
    }

    private String getLastSixDigits(String barcode) {
        if (barcode == null || barcode.length() < 6) {
            return barcode != null ? barcode : "000000";
        }
        return barcode.substring(barcode.length() - 6);
    }


    public Optional<BarcodeStatisticProjection> getBarcodeAndSkuCounts() {
        return barcodeRepository.getBarcodeStatistics();
    }

    //DTO
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