package com.x5.food.external;

import com.x5.food.dto.OpenFoodFactsResponse;
import com.x5.food.dto.ProductResponse;
import com.x5.food.exception.ApiResponseFormatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalProductService {

    private final WebClient webClient;

    @Value("${external.api.url}")
    private String externalApiUrl;

    @Value("${external.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${external.retry.delay:1000}")
    private long retryDelay;

    public Mono<Optional<ProductResponse>> getProductByBarcode(String barcode) {
        String url = externalApiUrl + barcode;

        return webClient.get()
                .uri(url)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        Mono.error(createClientException(barcode, clientResponse))
                )
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                        Mono.error(createServerException(barcode, clientResponse))
                )
                .bodyToMono(OpenFoodFactsResponse.class)
                .retryWhen(Retry.backoff(maxAttempts, Duration.ofMillis(retryDelay))
                        .filter(this::isRetryableException)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            log.warn("All retry attempts failed for barcode: {}", barcode, retrySignal.failure());
                            return createRetryExhaustedException(barcode, retrySignal.failure());
                        }))
                .flatMap(response -> processResponse(response, barcode))
                .onErrorResume(throwable -> recoverGetProductByBarcode(throwable, barcode))
                .defaultIfEmpty(Optional.empty()); // Гарантируем, что никогда не вернется null
    }

    private Mono<Optional<ProductResponse>> processResponse(OpenFoodFactsResponse response, String barcode) {
        try {
            // Простая ручная валидация
            if (response == null) {
                return Mono.just(Optional.empty());
            }

            // Валидация штрихкода
            if (response.barcode() == null || response.barcode().isBlank()) {
                return Mono.error(new ApiResponseFormatException("Invalid API response - empty barcode"));
            }

            if (response.product() == null) {
                return Mono.just(Optional.empty());
            }

            // Валидация имени продукта
            var productName = response.product().productName();
            if (productName == null || productName.isBlank()) {
                return Mono.error(new ApiResponseFormatException("Invalid Api response - empty product name"));
            }

            return Mono.just(Optional.of(ProductResponse.fromExternal(response, barcode)));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
    private boolean isRetryableException(Throwable throwable) {
        return throwable instanceof WebClientResponseException;
    }

    private RuntimeException createRetryExhaustedException(String barcode, Throwable cause) {
        return new RuntimeException("All retry attempts exhausted for barcode: " + barcode, cause);
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

    private Mono<Optional<ProductResponse>> recoverGetProductByBarcode(Throwable e, String barcode) {
        log.warn("Recovering from error after all retry attempts for barcode: {}", barcode, e);

        // Если это наше бизнес-исключение - пробрасываем его дальше
        if (e instanceof ApiResponseFormatException) {
            return Mono.error(e);
        }

        // Для других ошибок возвращаем empty
        return Mono.just(Optional.empty());
    }


}