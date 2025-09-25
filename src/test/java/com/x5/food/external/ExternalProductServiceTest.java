package com.x5.food.external;

import com.x5.food.dto.OpenFoodFactsResponse;
import com.x5.food.dto.ProductResponse;
import com.x5.food.exception.ApiResponseFormatException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalProductServiceTest {

    private final String testBarcode = "1234567890";

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private ExternalProductService externalProductService;

    @Test
    void getProductByBarcode_WithEmptyProductName_ThrowsApiResponseFormatException() {
        // Arrange
        OpenFoodFactsResponse mockResponse = new OpenFoodFactsResponse(
                testBarcode,
                new OpenFoodFactsResponse.Product(
                        "", // empty product name
                        "500g",
                        "Test Brand",
                        new OpenFoodFactsResponse.Nutriments(250.0)
                )
        );

        mockWebClientCalls();
        when(responseSpec.bodyToMono(OpenFoodFactsResponse.class))
                .thenReturn(Mono.just(mockResponse));

        // Act & Assert
        Mono<Optional<ProductResponse>> resultMono = externalProductService.getProductByBarcode(testBarcode);

        // Используем block() и ловим исключение из Mono
        ApiResponseFormatException exception = assertThrows(ApiResponseFormatException.class,
                () -> resultMono.block());

        assertEquals("Invalid Api response - empty product name", exception.getMessage());
    }

    @Test
    void getProductByBarcode_WithBlankProductName_ThrowsApiResponseFormatException() {
        // Arrange
        OpenFoodFactsResponse mockResponse = new OpenFoodFactsResponse(
                testBarcode,
                new OpenFoodFactsResponse.Product(
                        "   ", // blank product name
                        "500g",
                        "Test Brand",
                        new OpenFoodFactsResponse.Nutriments(250.0)
                )
        );

        mockWebClientCalls();
        when(responseSpec.bodyToMono(OpenFoodFactsResponse.class))
                .thenReturn(Mono.just(mockResponse));

        // Act & Assert
        Mono<Optional<ProductResponse>> resultMono = externalProductService.getProductByBarcode(testBarcode);

        ApiResponseFormatException exception = assertThrows(ApiResponseFormatException.class,
                () -> resultMono.block());

        assertEquals("Invalid Api response - empty product name", exception.getMessage());
    }

    @Test
    void getProductByBarcode_WithNullProductName_ThrowsApiResponseFormatException() {
        // Arrange
        OpenFoodFactsResponse mockResponse = new OpenFoodFactsResponse(
                testBarcode,
                new OpenFoodFactsResponse.Product(
                        null, // null product name
                        "500g",
                        "Test Brand",
                        new OpenFoodFactsResponse.Nutriments(250.0)
                )
        );

        mockWebClientCalls();
        when(responseSpec.bodyToMono(OpenFoodFactsResponse.class))
                .thenReturn(Mono.just(mockResponse));

        // Act & Assert
        Mono<Optional<ProductResponse>> resultMono = externalProductService.getProductByBarcode(testBarcode);

        ApiResponseFormatException exception = assertThrows(ApiResponseFormatException.class,
                () -> resultMono.block());

        assertEquals("Invalid Api response - empty product name", exception.getMessage());
    }

    @Test
    void getProductByBarcode_WithValidResponse_ReturnsProduct() {
        // Arrange
        OpenFoodFactsResponse mockResponse = new OpenFoodFactsResponse(
                testBarcode,
                new OpenFoodFactsResponse.Product(
                        "Test Product",
                        "500g",
                        "Test Brand",
                        new OpenFoodFactsResponse.Nutriments(250.0)
                )
        );

        mockWebClientCalls();
        when(responseSpec.bodyToMono(OpenFoodFactsResponse.class))
                .thenReturn(Mono.just(mockResponse));

        // Act
        Optional<ProductResponse> result = externalProductService.getProductByBarcode(testBarcode).block();

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Test Product 500g", result.get().name());
        assertEquals(testBarcode, result.get().barcodes().get(0));
    }

    @Test
    void getProductByBarcode_WithNullProduct_ReturnsEmpty() {
        // Arrange
        OpenFoodFactsResponse mockResponse = new OpenFoodFactsResponse(testBarcode, null);

        mockWebClientCalls();
        when(responseSpec.bodyToMono(OpenFoodFactsResponse.class))
                .thenReturn(Mono.just(mockResponse));

        // Act
        Optional<ProductResponse> result = externalProductService.getProductByBarcode(testBarcode).block();

        // Assert
        assertFalse(result.isPresent());
    }

    private void mockWebClientCalls() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    }
}