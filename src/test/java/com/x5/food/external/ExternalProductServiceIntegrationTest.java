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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ExternalProductServiceIntegrationTest {

    @Autowired
    private ExternalProductService externalProductService;

    @Test
    void shouldRetryThreeTimesAndReturnProductOnFourthAttempt() {
        // Arrange
        final String testBarcode = "123456789";

        // Act - один вызов сервиса, который должен привести к 3 вызовам API
        Optional<ProductResponse> result = externalProductService.getProductByBarcode(testBarcode);
        assertFalse(result.isPresent(), "Should return empty product after 3 retry attempts");

        // Assert - после 3 неудачных попыток, 4-я должна быть успешной
        result = externalProductService.getProductByBarcode(testBarcode);
        assertTrue(result.isPresent(), "Should return product");
        assertEquals("Test Product 500g", result.get().name());
        assertEquals(testBarcode, result.get().barcodes().get(0));
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

                        if (count <= 3) {
                            // Первые 3 вызова API возвращают 500 ошибку
                            return Mono.just(org.springframework.web.reactive.function.client.ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .header("Content-Type", "application/json")
                                    .body("Server Error")
                                    .build());
                        } else {
                            // 4-й вызов API возвращает успешный ответ
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
    }
    
}