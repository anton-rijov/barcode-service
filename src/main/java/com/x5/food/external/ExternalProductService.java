package com.x5.food.external;

import com.x5.food.dto.OpenFoodFactsResponse;
import com.x5.food.dto.ProductResponse;
import com.x5.food.exception.ApiResponseFormatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalProductService {

    private final WebClient webClient;

    @Value("${external.api.url}")
    private String externalApiUrl;

    @Retryable(
            retryFor = {
                    WebClientResponseException.class,
                    RuntimeException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public Optional<ProductResponse> getProductByBarcode(String barcode) {
        try {
            String url = externalApiUrl + barcode;

            OpenFoodFactsResponse response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                            Mono.error(createClientException(barcode, clientResponse))
                    )
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            Mono.error(createServerException(barcode, clientResponse))
                    )
                    .bodyToMono(OpenFoodFactsResponse.class)
                    .block();

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

    private WebClientResponseException createClientException(String barcode,
                                                             org.springframework.web.reactive.function.client.ClientResponse clientResponse) {
        log.error("Client error when fetching product for barcode: {}. Status: {}",
                barcode, clientResponse.statusCode());
        return WebClientResponseException.create(
                clientResponse.statusCode().value(),
                "Client error for barcode: " + barcode,
                clientResponse.headers().asHttpHeaders(),
                null,
                null
        );
    }

    private WebClientResponseException createServerException(String barcode,
                                                             org.springframework.web.reactive.function.client.ClientResponse clientResponse) {
        log.error("Server error when fetching product for barcode: {}. Status: {}",
                barcode, clientResponse.statusCode());
        return WebClientResponseException.create(
                clientResponse.statusCode().value(),
                "Server error for barcode: " + barcode,
                clientResponse.headers().asHttpHeaders(),
                null,
                null
        );
    }

    @Recover
    public Optional<ProductResponse> recoverGetProductByBarcode(Exception e, String barcode) {
        log.warn("All retry attempts failed for barcode: {}", barcode, e);
        return Optional.empty();
    }
}