package com.x5.food.external;

import com.x5.food.dto.OpenFoodFactsResponse;
import com.x5.food.dto.ProductResponse;
import com.x5.food.exception.ApiResponseFormatException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalProductServiceTest {

    private final String testBarcode = "1234567890";

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ExternalProductService externalProductService;

    @Test
    void getProductByBarcode_WithValidResponse_ReturnsProduct() {
        // Arrange
        OpenFoodFactsResponse externalResponse = new OpenFoodFactsResponse(
                testBarcode,
                new OpenFoodFactsResponse.Product(
                        "Test Product",
                        "500g",
                        "Test Brand",
                        new OpenFoodFactsResponse.Nutriments(250.0)
                )
        );
        when(restTemplate.getForObject(anyString(), eq(OpenFoodFactsResponse.class)))
                .thenReturn(externalResponse);

        // Act
        Optional<ProductResponse> result = externalProductService.getProductByBarcode(testBarcode);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Test Product 500g", result.get().name());
        assertEquals(List.of(testBarcode), result.get().barcodes());
        verify(restTemplate).getForObject(anyString(), eq(OpenFoodFactsResponse.class));
    }

    @Test
    void getProductByBarcode_WithEmptyProductName_ThrowsException() {
        // Arrange
        OpenFoodFactsResponse externalResponse = new OpenFoodFactsResponse(
                testBarcode,
                new OpenFoodFactsResponse.Product(
                        "", // empty product name
                        "1kg",
                        "Test Brand",
                        new OpenFoodFactsResponse.Nutriments(100.0)
                )
        );
        when(restTemplate.getForObject(anyString(), eq(OpenFoodFactsResponse.class)))
                .thenReturn(externalResponse);

        // Act & Assert
        assertThrows(ApiResponseFormatException.class, () -> {
            externalProductService.getProductByBarcode(testBarcode);
        });
    }
}