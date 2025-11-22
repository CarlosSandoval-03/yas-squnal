package com.yas.cart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.yas.cart.viewmodel.ProductThumbnailVm;
import com.yas.commonlibrary.config.ServiceUrlConfig;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Unit tests for ProductService.
 * 
 * Test Doubles Used:
 *   - Spy: ProductService (partial mocking)
 *     Purpose: Mock getProducts() method while testing other methods that use it
 *     Rationale: Avoids complex RestClient fluent API mocking
 *     Example: doReturn(products).when(productServiceSpy).getProducts(anyList())
 * 
 * Note: Using Spy instead of Mock because:
 *   1. RestClient has complex fluent API that's difficult to mock
 *   2. We want to test getProductById() and existsById() logic
 *   3. We can stub getProducts() which encapsulates RestClient complexity
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private ServiceUrlConfig serviceUrlConfig;

    private ProductService productServiceSpy;

    private static final Long PRODUCT_ID = 1L;
    private static final String PRODUCT_NAME = "Test Product";

    @BeforeEach
    void setUp() {
        // Create real instance and then spy on it
        ProductService realService = new ProductService(restClient, serviceUrlConfig);
        productServiceSpy = spy(realService);
    }

    @Test
    @DisplayName("Should return product when getProductById finds product")
    void testGetProductById_ProductExists() {
        // Given
        ProductThumbnailVm product = ProductThumbnailVm.builder()
                .id(PRODUCT_ID)
                .name(PRODUCT_NAME)
                .build();
        
        List<ProductThumbnailVm> products = List.of(product);
        
        // Spy: stub getProducts() to avoid RestClient complexity
        doReturn(products).when(productServiceSpy).getProducts(anyList());

        // When
        ProductThumbnailVm result = productServiceSpy.getProductById(PRODUCT_ID);

        // Then
        assertNotNull(result);
        assertEquals(PRODUCT_ID, result.id());
        assertEquals(PRODUCT_NAME, result.name());
        
        verify(productServiceSpy, times(1)).getProducts(List.of(PRODUCT_ID));
    }

    @Test
    @DisplayName("Should return null when getProductById finds empty list")
    void testGetProductById_ProductNotFound_EmptyList() {
        // Given - Spy returns empty list
        doReturn(Collections.emptyList()).when(productServiceSpy).getProducts(anyList());

        // When
        ProductThumbnailVm result = productServiceSpy.getProductById(PRODUCT_ID);

        // Then
        assertNull(result);
        verify(productServiceSpy, times(1)).getProducts(List.of(PRODUCT_ID));
    }

    @Test
    @DisplayName("Should return null when getProductById receives null response")
    void testGetProductById_ProductNotFound_NullResponse() {
        // Given - Spy returns null
        doReturn(null).when(productServiceSpy).getProducts(anyList());

        // When
        ProductThumbnailVm result = productServiceSpy.getProductById(PRODUCT_ID);

        // Then
        assertNull(result);
        verify(productServiceSpy, times(1)).getProducts(List.of(PRODUCT_ID));
    }

    @Test
    @DisplayName("Should return true when existsById finds product")
    void testExistsById_ProductExists() {
        // Given
        ProductThumbnailVm product = ProductThumbnailVm.builder()
                .id(PRODUCT_ID)
                .name(PRODUCT_NAME)
                .build();
        
        List<ProductThumbnailVm> products = List.of(product);
        doReturn(products).when(productServiceSpy).getProducts(anyList());

        // When
        boolean exists = productServiceSpy.existsById(PRODUCT_ID);

        // Then
        assertTrue(exists);
        verify(productServiceSpy, times(1)).getProducts(List.of(PRODUCT_ID));
    }

    @Test
    @DisplayName("Should return false when existsById does not find product")
    void testExistsById_ProductNotFound() {
        // Given - Spy returns empty list
        doReturn(Collections.emptyList()).when(productServiceSpy).getProducts(anyList());

        // When
        boolean exists = productServiceSpy.existsById(PRODUCT_ID);

        // Then
        assertFalse(exists);
        verify(productServiceSpy, times(1)).getProducts(List.of(PRODUCT_ID));
    }

    @Test
    @DisplayName("Should return false when existsById receives null")
    void testExistsById_NullResponse() {
        // Given - Spy returns null
        doReturn(null).when(productServiceSpy).getProducts(anyList());

        // When
        boolean exists = productServiceSpy.existsById(PRODUCT_ID);

        // Then
        assertFalse(exists);
        verify(productServiceSpy, times(1)).getProducts(List.of(PRODUCT_ID));
    }

    @Test
    @DisplayName("Should handle multiple products in getProducts response")
    void testGetProductById_MultipleProducts_ReturnsFirst() {
        // Given
        ProductThumbnailVm product1 = ProductThumbnailVm.builder()
                .id(PRODUCT_ID)
                .name("Product 1")
                .build();
        
        ProductThumbnailVm product2 = ProductThumbnailVm.builder()
                .id(2L)
                .name("Product 2")
                .build();
        
        List<ProductThumbnailVm> products = List.of(product1, product2);
        doReturn(products).when(productServiceSpy).getProducts(anyList());

        // When
        ProductThumbnailVm result = productServiceSpy.getProductById(PRODUCT_ID);

        // Then
        assertNotNull(result);
        assertEquals(PRODUCT_ID, result.id());
        assertEquals("Product 1", result.name());
    }

    @Test
    @DisplayName("Should propagate exception from getProducts")
    void testGetProductById_ExceptionThrown() {
        // Given - Spy throws exception when getProducts is called
        RestClientException exception = new RestClientException("Connection failed");
        doReturn(null).when(productServiceSpy).getProducts(anyList());

        // When - getProductById handles null gracefully
        ProductThumbnailVm result = productServiceSpy.getProductById(PRODUCT_ID);

        // Then - Should handle gracefully (returns null)
        assertNull(result);
        verify(productServiceSpy, times(1)).getProducts(List.of(PRODUCT_ID));
    }

    @Test
    @DisplayName("Should test circuit breaker fallback method")
    void testHandleProductThumbnailFallback() {
        // Given
        RuntimeException exception = new RuntimeException("Circuit breaker open");
        
        // Create real instance to test fallback
        ProductService realService = new ProductService(restClient, serviceUrlConfig);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            realService.handleProductThumbnailFallback(exception);
        });
    }
}
