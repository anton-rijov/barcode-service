package com.x5.food.service;

import com.x5.food.dto.ProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@SpringBootTest
@ActiveProfiles("test")
class BarcodeServiceIntegrationTest {

    @Value("${external.api.url}")
    private String testApiUrl;

    @Autowired
    private BarcodeService barcodeService;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        // Сбрасываем и создаем новый mock server перед каждым тестом
        mockServer = MockRestServiceServer.createServer(restTemplate);
        mockServer.reset(); // Важно: сбрасываем предыдущие ожидания
    }

    @Test
    void shouldRetryThreeTimesOnExternalApiFailure() {
        // Arrange
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
        String barcode = "123456789";
        String expectedUrl = testApiUrl + barcode;

        // Настраиваем 3 неудачных запроса и затем успешный
        for (int i = 0; i < 3; i++) {
            mockServer.expect(requestTo(expectedUrl))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        }

        // 1 успешный запрос
        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                    "product": {
                                        "product_name": "Test Product",
                                        "code": "123456789"
                                    }
                                }
                                """));

        // Act 3 х 500
        Optional<ProductResponse> result = barcodeService.getProductFromExternalService(barcode);

        // Assert
        assertFalse(result.isPresent());

        // Act again 1 x 200
        result = barcodeService.getProductFromExternalService(barcode);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Test Product", result.get().name());

        // Проверяем, что было выполнено 4 запроса (3 retry + 1 успешный)
        mockServer.verify();
    }
}