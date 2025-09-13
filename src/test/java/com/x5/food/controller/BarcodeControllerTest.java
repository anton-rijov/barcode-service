package com.x5.food.controller;

import com.x5.food.dto.ProductResponse;
import com.x5.food.exception.BadRequestException;
import com.x5.food.exception.ResourceNotFoundException;
import com.x5.food.service.BarcodeService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BarcodeControllerTest {

    @Mock
    private BarcodeService barcodeService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private BarcodeController barcodeController;

    private String validBarcode;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        validBarcode = "1234567890";
        productResponse = new ProductResponse("SKU_3757", "Бананы 1кг",
                List.of(validBarcode, "ANOTHER_TEST_BARCODE"));
    }

    // Вспомогательный метод для тестирования приватного метода
    private Boolean invokeIsLocalIp(String ip) {
        try {
            Method method = ReflectionUtils.findMethod(BarcodeController.class, "isLocalIp", String.class);
            assertNotNull(method, "Метод isLocalIp не найден");
            method.setAccessible(true);
            return (Boolean) method.invoke(barcodeController, ip);
        } catch (Exception e) {
            fail("Ошибка при вызове приватного метода: " + e.getMessage());
            return false;
        }
    }

    @Test
    void isLocalIp_WithLocalIpv4_ReturnsTrue() {
        assertTrue(invokeIsLocalIp("127.0.0.1"));
    }

    @Test
    void isLocalIp_WithLocalhost_ReturnsTrue() {
        assertTrue(invokeIsLocalIp("localhost"));
    }

    @Test
    void isLocalIp_WithIpv6Localhost_ReturnsTrue() {
        assertTrue(invokeIsLocalIp("::1"));
    }

    @Test
    void isLocalIp_WithRemoteIp_ReturnsFalse() {
        assertFalse(invokeIsLocalIp("192.168.1.100"));
        assertFalse(invokeIsLocalIp("10.0.0.1"));
        assertFalse(invokeIsLocalIp("8.8.8.8"));
    }

    @Test
    void isLocalIp_WithEmptyString_ReturnsFalse() {
        assertFalse(invokeIsLocalIp(""));
    }

    @Test
    void isLocalIp_WithNull_ReturnsFalse() {
        assertFalse(invokeIsLocalIp(null));
    }

    @Test
    void deleteBarcodeById_WithLocalIp_ReturnsOkResponse() {
        // Arrange
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        doNothing().when(barcodeService).deleteBarcodeById(validBarcode);

        // Act
        ResponseEntity<String> response = barcodeController.deleteBarcodeById(validBarcode, request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(barcodeService).deleteBarcodeById(validBarcode);
        verify(request).getRemoteAddr();
    }

    @Test
    void deleteBarcodeById_WithRemoteIp_ReturnsForbiddenResponse() {
        // Arrange
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        // Act
        ResponseEntity<String> response = barcodeController.deleteBarcodeById(validBarcode, request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Доступ запрещен", response.getBody());
        verifyNoInteractions(barcodeService);
        verify(request).getRemoteAddr();
    }

    @Test
    void getProductByBarcode_WithValidBarcode_ReturnsProductResponse() {
        // Arrange
        BarcodeService.ResponseWithStatus responseWithStatus =
                new BarcodeService.ResponseWithStatus(HttpStatus.OK, productResponse);

        when(barcodeService.getProductByBarcode(validBarcode)).thenReturn(responseWithStatus);

        // Act
        ResponseEntity<ProductResponse> response = barcodeController.getProductByBarcode(validBarcode);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(productResponse, response.getBody()); // Сравниваем с ProductResponse, а не ResponseWithStatus
        verify(barcodeService).getProductByBarcode(validBarcode);
    }

    @Test
    void getProductByBarcode_WithEmptyBarcode_ThrowsBadRequestException() {
        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> barcodeController.getProductByBarcode(""));

        assertEquals("Штрих-код не может быть пустым", exception.getMessage());
        verifyNoInteractions(barcodeService);
    }

    @Test
    void getProductByBarcode_WithNullBarcode_ThrowsBadRequestException() {
        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> barcodeController.getProductByBarcode(null));

        assertEquals("Штрих-код не может быть пустым", exception.getMessage());
        verifyNoInteractions(barcodeService);
    }

    @Test
    void getProductByBarcode_WithNonExistentBarcode_ThrowsResourceNotFoundException() {
        // Arrange
        BarcodeService.ResponseWithStatus responseWithStatus =
                new BarcodeService.ResponseWithStatus(HttpStatus.NOT_FOUND, null);

        when(barcodeService.getProductByBarcode(validBarcode)).thenReturn(responseWithStatus);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> barcodeController.getProductByBarcode(validBarcode));

        assertEquals("Продукт с таким штрих-кодом не найден", exception.getMessage());
        verify(barcodeService).getProductByBarcode(validBarcode);
    }

}