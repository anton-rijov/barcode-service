package com.x5.food.external;

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
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
@ActiveProfiles("test")
class ExternalProductServiceIntegrationTest {

    @Value("${external.api.url}")
    private String testApiUrl;

    @Autowired
    private ExternalProductService externalProductService;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        mockServer.reset();
    }

    @Test
    void shouldRetryThreeTimesOnExternalApiFailure() {
        // Arrange
        String barcode = "123456789";
        String expectedUrl = testApiUrl + barcode;

        // Настраиваем 3 неудачных запроса
        for (int i = 0; i < 3; i++) {
            mockServer.expect(requestTo(expectedUrl))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        }

        // 1 успешный запрос
        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
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
                        """, MediaType.APPLICATION_JSON));

        // Act - первые 3 вызова должны упасть и вернуть empty
        Optional<ProductResponse> result = externalProductService.getProductByBarcode(barcode);
        assertFalse(result.isPresent());

        // Act - четвертый вызов должен быть успешным
        result = externalProductService.getProductByBarcode(barcode);
        assertTrue(result.isPresent());
        assertEquals("Test Product 500g", result.get().name());

        // Проверяем, что было выполнено 4 запроса (3 retry + 1 успешный)
        mockServer.verify();
    }
}