package com.x5.food.external;

import com.x5.food.dto.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ExternalProductServiceIntegrationTest {

    @Autowired
    private ExternalProductService externalProductService;

    @Autowired
    private TestConfig testConfig;

    @Test
    void shouldRetryThreeTimesAndReturnProductOnFourthAttempt() {
        // Arrange
        final String testBarcode = "123456789";

        // Act - один вызов сервиса делает 4 попытки API (1 исходная + 3 ретрая)
        Optional<ProductResponse> result = externalProductService.getProductByBarcode(testBarcode).block();

        // Assert - на 4-й попытке получаем успешный результат
        assertTrue(result.isPresent(), "Should return product after 4 attempts (1 initial + 3 retries)");
        assertEquals("Test Product 500g", result.get().name());
        assertEquals(testBarcode, result.get().barcodes().get(0));

        // Проверяем, что было ровно 4 вызова API
        assertEquals(4, testConfig.getCallCount(), "Should be 4 API calls total");
    }

    @TestConfiguration
    static class TestConfig {

        private final AtomicInteger callCount = new AtomicInteger(0);

        @Bean
        @Primary
        public WebClient webClient() {
            return WebClient.builder()
                    .exchangeFunction(clientRequest -> {
                        int count = callCount.incrementAndGet();
                        System.out.println("API Call #" + count + " for barcode: " +
                                extractBarcodeFromUrl(clientRequest.url().toString()));

                        if (count <= 3) {
                            // Первые 3 вызова API возвращают 500 ошибку
                            return Mono.just(org.springframework.web.reactive.function.client.ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .header("Content-Type", "application/json")
                                    .body("{\"error\": \"Server Error\"}")
                                    .build());
                        } else {
                            // 4-й и последующие вызовы API возвращают успешный ответ
                            String successResponse = """
                                    {
                                        "product": {
                                            "product_name": "Test Product",
                                            "quantity": "500g",
                                            "brands": "Test Brand",
                                            "nutriments": {
                                                "energy-kcal_100g": 250.0
                                            }
                                        }
                                    }
                                    """;

                            return Mono.just(org.springframework.web.reactive.function.client.ClientResponse.create(HttpStatus.OK)
                                    .header("Content-Type", "application/json")
                                    .body(successResponse)
                                    .build());
                        }
                    })
                    .build();
        }

        private String extractBarcodeFromUrl(String url) {
            String[] parts = url.split("/");
            return parts[parts.length - 1];
        }

        public int getCallCount() {
            return callCount.get();
        }

        public void resetCallCount() {
            callCount.set(0);
        }
    }

}