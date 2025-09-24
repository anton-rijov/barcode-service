package com.x5.food.service;

import com.x5.food.dto.ProductResponse;
import com.x5.food.dto.projection.BarcodeStatisticProjection;
import com.x5.food.entity.Product;
import com.x5.food.exception.ResourceNotFoundException;
import com.x5.food.external.ExternalProductService;
import com.x5.food.repository.BarcodeRepository;
import com.x5.food.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
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
    private ExternalProductService externalProductService;

    @InjectMocks
    private BarcodeService barcodeService;

    @Test
    void getProductByBarcode_WhenProductExistsLocally_ReturnsOkStatus() {
        // Arrange
        Product productEntity = createProductEntity(testSku, "Test Product");
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
        verifyNoInteractions(externalProductService, barcodeRepository);
    }

    @Test
    void getProductByBarcode_WhenProductNotExistsLocallyButExistsExternally_ReturnsCreatedStatus() {
        // Arrange
        ProductResponse mockProduct = new ProductResponse(testSku, "External Product", List.of(testBarcode));

        when(productRepository.findByBarcode(testBarcode))
                .thenReturn(Optional.empty());
        when(externalProductService.getProductByBarcode(testBarcode))
                .thenReturn(Mono.just(Optional.of(mockProduct)));

        // Act
        BarcodeService.ResponseWithStatus result = barcodeService.getProductByBarcode(testBarcode);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.status());
        assertNotNull(result.response());
        assertEquals(testSku, result.response().sku());

        verify(productRepository).findByBarcode(testBarcode);
        verify(externalProductService).getProductByBarcode(testBarcode);
        verify(productRepository).upsertProduct(testSku, "External Product");
        verify(barcodeRepository).insertBarcodeIfNotExists(testBarcode, testSku);
    }

    @Test
    void getProductByBarcode_WhenProductNotExistsAnywhere_ReturnsNotFoundStatus() {
        // Arrange
        when(productRepository.findByBarcode(testBarcode))
                .thenReturn(Optional.empty());
        when(externalProductService.getProductByBarcode(testBarcode))
                .thenReturn(Mono.just(Optional.empty()));

        // Act
        BarcodeService.ResponseWithStatus result = barcodeService.getProductByBarcode(testBarcode);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.NOT_FOUND, result.status());
        assertNull(result.response());

        verify(productRepository).findByBarcode(testBarcode);
        verify(externalProductService).getProductByBarcode(testBarcode);
        verifyNoMoreInteractions(productRepository, barcodeRepository);
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

    private Product createProductEntity(String sku, String name) {
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        return product;
    }
}