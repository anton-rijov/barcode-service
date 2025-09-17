package com.x5.food.service;

import com.x5.food.dto.OpenFoodFactsResponse;
import com.x5.food.dto.ProductResponse;
import com.x5.food.dto.projection.BarcodeStatisticProjection;
import com.x5.food.entity.Product;
import com.x5.food.exception.ResourceNotFoundException;
import com.x5.food.repository.BarcodeRepository;
import com.x5.food.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BarcodeServiceTest {

    private final String testBarcode = "1234567890";
    private final String testSku = "SKU_123";
    @Mock
    private ProductRepository productRepository;
    @Mock
    private BarcodeRepository barcodeRepository;
    @Mock
    private RestTemplate restTemplate;
    @InjectMocks
    private BarcodeService barcodeService;

    @Test
    void getProductByBarcode_WhenProductExistsLocally_ReturnsOkStatus() {
        // Arrange
        Product productEntity = TestData.createProductEntity(testSku, testBarcode);
        when(productRepository.findByBarcode(testBarcode))
                .thenReturn(Optional.of(productEntity));

        // Act
        BarcodeService.ResponseWithStatus result = barcodeService.getProductByBarcode(testBarcode);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.status());
        assertNotNull(result.response());
        assertEquals(testSku, result.response().sku());
        verify(productRepository).findByBarcode(testBarcode);
        verifyNoInteractions(restTemplate, barcodeRepository);
    }

    @Test
    void getProductByBarcode_WhenProductNotExistsLocallyButExistsExternally_ReturnsCreatedStatus() {
        // Arrange
        when(productRepository.findByBarcode(testBarcode)).thenReturn(Optional.empty());

        OpenFoodFactsResponse externalResponse = new OpenFoodFactsResponse(
                testBarcode,
                new OpenFoodFactsResponse.Product(
                        "External Product",
                        "1kg",
                        "Test Brand",
                        new OpenFoodFactsResponse.Nutriments(100.0)
                )
        );
        when(restTemplate.getForObject(anyString(), eq(OpenFoodFactsResponse.class)))
                .thenReturn(externalResponse);

        // Act
        BarcodeService.ResponseWithStatus result = barcodeService.getProductByBarcode(testBarcode);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.status());
        assertNotNull(result.response());
        assertEquals("External Product 1kg", result.response().name());
        verify(productRepository).findByBarcode(testBarcode);
        verify(restTemplate).getForObject(anyString(), eq(OpenFoodFactsResponse.class));
        verify(productRepository).upsertProduct(anyString(), anyString());
        verify(barcodeRepository).insertBarcodeIfNotExists(anyString(), anyString());
    }

    @Test
    void getProductByBarcode_WhenExternalServiceReturnsNullProduct_ReturnsNotFound() {
        // Arrange
        when(productRepository.findByBarcode(testBarcode)).thenReturn(Optional.empty());

        OpenFoodFactsResponse externalResponse = new OpenFoodFactsResponse(
                testBarcode,
                null // product is null
        );
        when(restTemplate.getForObject(anyString(), eq(OpenFoodFactsResponse.class)))
                .thenReturn(externalResponse);

        // Act
        BarcodeService.ResponseWithStatus result = barcodeService.getProductByBarcode(testBarcode);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.NOT_FOUND, result.status());
        assertNull(result.response());
    }

    @Test
    void getProductByBarcode_WhenProductNotExistsAnywhere_ReturnsNotFoundStatus() {
        // Arrange
        when(productRepository.findByBarcode(testBarcode)).thenReturn(Optional.empty());
        when(restTemplate.getForObject(anyString(), eq(OpenFoodFactsResponse.class)))
                .thenReturn(null); // External service returns null

        // Act
        BarcodeService.ResponseWithStatus result = barcodeService.getProductByBarcode(testBarcode);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.NOT_FOUND, result.status());
        assertNull(result.response());
        verify(productRepository).findByBarcode(testBarcode);
        verify(restTemplate).getForObject(anyString(), eq(OpenFoodFactsResponse.class));
        verifyNoMoreInteractions(productRepository, barcodeRepository);
    }

    @Test
    void getProductByBarcode_WhenExternalServiceFails_ReturnsNotFoundStatus() {
        // Arrange
        when(productRepository.findByBarcode(testBarcode)).thenReturn(Optional.empty());
        when(restTemplate.getForObject(anyString(), eq(OpenFoodFactsResponse.class)))
                .thenThrow(new RuntimeException("Connection failed"));

        // Act
        BarcodeService.ResponseWithStatus result = barcodeService.getProductByBarcode(testBarcode);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.NOT_FOUND, result.status());
        assertNull(result.response());
        verify(productRepository).findByBarcode(testBarcode);
        verify(restTemplate).getForObject(anyString(), eq(OpenFoodFactsResponse.class));
    }

    @Test
    void getBarcodeAndSkuCounts_WhenDataExists_ReturnsStatistics() {
        // Arrange
        BarcodeStatisticProjection stats = mock(BarcodeStatisticProjection.class);
        when(barcodeRepository.getBarcodeStatistics()).thenReturn(Optional.of(stats));

        // Act
        Optional<BarcodeStatisticProjection> result = barcodeService.getBarcodeAndSkuCounts();

        // Assert
        assertTrue(result.isPresent());
        assertEquals(stats, result.get());
        verify(barcodeRepository).getBarcodeStatistics();
    }

    @Test
    void getBarcodeAndSkuCounts_WhenNoData_ReturnsEmpty() {
        // Arrange
        when(barcodeRepository.getBarcodeStatistics()).thenReturn(Optional.empty());

        // Act
        Optional<BarcodeStatisticProjection> result = barcodeService.getBarcodeAndSkuCounts();

        // Assert
        assertFalse(result.isPresent());
        verify(barcodeRepository).getBarcodeStatistics();
    }

    @Test
    void deleteBarcodeById_WhenBarcodeExists_DeletesSuccessfully() {
        // Arrange
        when(barcodeRepository.existsByBarcode(testBarcode)).thenReturn(true);
        doNothing().when(barcodeRepository).deleteById(testBarcode);

        // Act
        assertDoesNotThrow(() -> barcodeService.deleteBarcodeById(testBarcode));

        // Assert
        verify(barcodeRepository).existsByBarcode(testBarcode);
        verify(barcodeRepository).deleteById(testBarcode);
    }

    @Test
    void deleteBarcodeById_WhenBarcodeNotExists_ThrowsException() {
        // Arrange
        when(barcodeRepository.existsByBarcode(testBarcode)).thenReturn(false);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> barcodeService.deleteBarcodeById(testBarcode));

        assertEquals("Штрих-код не найден", exception.getMessage());
        verify(barcodeRepository).existsByBarcode(testBarcode);
        verify(barcodeRepository, never()).deleteById(anyString());
    }

    @Test
    void getProductFromExternalService_WithValidResponse_ReturnsProduct() {
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

        // Act - используем рефлексию для вызова приватного метода
        Optional<ProductResponse> result = barcodeService.getProductFromExternalService(testBarcode);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Test Product 500g", result.get().name());
        verify(restTemplate).getForObject(anyString(), eq(OpenFoodFactsResponse.class));
    }

    @Test
    void getProductFromExternalService_WithNullResponse_ReturnsEmpty() {
        // Arrange
        when(restTemplate.getForObject(anyString(), eq(OpenFoodFactsResponse.class)))
                .thenReturn(null);

        // Act - используем рефлексию для вызова приватного метода
        Optional<ProductResponse> result = barcodeService.getProductFromExternalService(testBarcode);

        // Assert
        assertFalse(result.isPresent());
        verify(restTemplate).getForObject(anyString(), eq(OpenFoodFactsResponse.class));
    }

    @Test
    void getProductFromExternalService_WithException_ReturnsEmpty() {
        // Arrange
        when(restTemplate.getForObject(anyString(), eq(OpenFoodFactsResponse.class)))
                .thenThrow(new RuntimeException("API error"));

        // Act - используем рефлексию для вызова приватного метода
        Optional<ProductResponse> result = barcodeService.getProductFromExternalService(testBarcode);

        // Assert
        assertFalse(result.isPresent());
        verify(restTemplate).getForObject(anyString(), eq(OpenFoodFactsResponse.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {""})
    @ValueSource(strings = {" "})
    void getProductFromExternalService_WithNullProductName_ReturnsNotFound(String testProductName) {
        when(productRepository.findByBarcode(testBarcode)).thenReturn(Optional.empty());

        OpenFoodFactsResponse externalResponse = new OpenFoodFactsResponse(
                testBarcode,
                new OpenFoodFactsResponse.Product(
                        testProductName,
                        "1kg",
                        "Test Brand",
                        new OpenFoodFactsResponse.Nutriments(100.0)
                )
        );
        when(restTemplate.getForObject(anyString(), eq(OpenFoodFactsResponse.class)))
                .thenReturn(externalResponse);

        // Act
        BarcodeService.ResponseWithStatus result = barcodeService.getProductByBarcode(testBarcode);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.NOT_FOUND, result.status());
        assertNull(result.response());
    }
}

// Вспомогательный класс для тестовых данных
class TestData {
    static Product createProductEntity(String sku, String barcode) {
        Product product = new Product();
        product.setSku(sku);
        product.setName("Test Product");

        // Если нужно протестировать преобразование в ProductResponse,
        // то нужно создать связанные сущности Barcode
        return product;
    }

}