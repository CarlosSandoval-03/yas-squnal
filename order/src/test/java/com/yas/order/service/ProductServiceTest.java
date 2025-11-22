package com.yas.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.order.config.ServiceUrlConfig;
import com.yas.order.viewmodel.order.OrderItemVm;
import com.yas.order.viewmodel.order.OrderVm;
import com.yas.order.viewmodel.product.ProductCheckoutListVm;
import com.yas.order.viewmodel.product.ProductVariationVm;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

/**
 * Unit tests for ProductService.
 * 
 * Test Doubles Used:
 *   - Spy: ProductService (RestClient calls too complex to mock completely)
 *     Purpose: Avoid mocking RestClient fluent API
 *     Example: doReturn(productList).when(productServiceSpy).getProductVariations(productId)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private ServiceUrlConfig serviceUrlConfig;

    private ProductService productService;
    private ProductService productServiceSpy;

    private static final Long PRODUCT_ID = 1L;
    private static final String PRODUCT_URL = "http://product-service";

    @BeforeEach
    void setUp() {
        productService = new ProductService(restClient, serviceUrlConfig);
        productServiceSpy = spy(productService);
    }

    @Test
    @DisplayName("Should get product variations successfully")
    void testGetProductVariations_Success() {
        // Given
        List<ProductVariationVm> expectedVariations = List.of(
                new ProductVariationVm(1L, "Color-Red", "SKU-001")
        );
        
        doReturn(expectedVariations).when(productServiceSpy).getProductVariations(PRODUCT_ID);

        // When
        List<ProductVariationVm> result = productServiceSpy.getProductVariations(PRODUCT_ID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Color-Red", result.get(0).name());
        verify(productServiceSpy, times(1)).getProductVariations(PRODUCT_ID);
    }

    @Test
    @DisplayName("Should subtract product stock quantity successfully")
    void testSubtractProductStockQuantity_Success() {
        // Given
        OrderVm orderVm = OrderVm.builder()
                .id(1L)
                .orderItemVms(Set.of(
                        new OrderItemVm(1L, PRODUCT_ID, "Product 1", 2, new BigDecimal("50.0"),
                                "Note", new BigDecimal("0"), new BigDecimal("5.0"),
                                new BigDecimal("0"), null)
                ))
                .build();

        doNothing().when(productServiceSpy).subtractProductStockQuantity(orderVm);

        // When
        productServiceSpy.subtractProductStockQuantity(orderVm);

        // Then
        verify(productServiceSpy, times(1)).subtractProductStockQuantity(orderVm);
    }

    @Test
    @DisplayName("Should get product information successfully")
    void testGetProductInformation_Success() {
        // Given
        Set<Long> productIds = Set.of(1L, 2L);
        Map<Long, ProductCheckoutListVm> expectedProducts = new HashMap<>();
        expectedProducts.put(1L, ProductCheckoutListVm.builder()
                .id(1L)
                .name("Product 1")
                .price(50.0)
                .build());
        expectedProducts.put(2L, ProductCheckoutListVm.builder()
                .id(2L)
                .name("Product 2")
                .price(30.0)
                .build());

        doReturn(expectedProducts).when(productServiceSpy)
                .getProductInfomation(productIds, 0, 100);

        // When
        Map<Long, ProductCheckoutListVm> result = productServiceSpy.getProductInfomation(productIds, 0, 100);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Product 1", result.get(1L).getName());
        assertEquals("Product 2", result.get(2L).getName());
        verify(productServiceSpy, times(1)).getProductInfomation(productIds, 0, 100);
    }
}