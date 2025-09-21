package com.x5.food.external;

import com.x5.food.dto.OpenFoodFactsResponse;
import com.x5.food.dto.ProductResponse;
import com.x5.food.exception.ApiResponseFormatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalProductService {

    private final RestTemplate restTemplate;

    @Value("${external.api.url}")
    private String externalApiUrl;

    @Retryable(
            retryFor = {
                    HttpServerErrorException.class,
                    HttpClientErrorException.class,
                    ResourceAccessException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public Optional<ProductResponse> getProductByBarcode(String barcode) {
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
            throw e;
        }
        return Optional.empty();
    }

    @Recover
    public Optional<ProductResponse> recoverGetProductByBarcode(Exception e, String barcode) {
        log.warn("All retry attempts failed for barcode: {}", barcode, e);
        return Optional.empty();
    }
}