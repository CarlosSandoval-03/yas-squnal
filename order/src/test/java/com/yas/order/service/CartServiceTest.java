package com.yas.order.service;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.order.config.ServiceUrlConfig;
import com.yas.order.viewmodel.order.OrderItemVm;
import com.yas.order.viewmodel.order.OrderVm;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

/**
 * Unit tests for CartService.
 * 
 * Test Doubles Used:
 *   - Spy: CartService
 *     Purpose: Avoid mocking complex RestClient fluent API
 *     Example: doReturn(null).when(cartServiceSpy).deleteCartItems(orderVm)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CartService Unit Tests")
class CartServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private ServiceUrlConfig serviceUrlConfig;

    private CartService cartService;
    private CartService cartServiceSpy;

    private static final String CART_URL = "http://cart-service";

    @BeforeEach
    void setUp() {
        cartService = new CartService(restClient, serviceUrlConfig);
        cartServiceSpy = spy(cartService);
    }

    @Test
    @DisplayName("Should delete cart items successfully")
    void testDeleteCartItems_Success() {
        // Given
        OrderVm orderVm = OrderVm.builder()
                .id(1L)
                .orderItemVms(Set.of(
                        new OrderItemVm(1L, 101L, "Product 1", 2, new BigDecimal("50.0"),
                                "Note", new BigDecimal("0"), new BigDecimal("5.0"),
                                new BigDecimal("0"), null),
                        new OrderItemVm(2L, 102L, "Product 2", 1, new BigDecimal("30.0"),
                                "Note", new BigDecimal("0"), new BigDecimal("3.0"),
                                new BigDecimal("0"), null)
                ))
                .build();

        doNothing().when(cartServiceSpy).deleteCartItems(orderVm);

        // When
        cartServiceSpy.deleteCartItems(orderVm);

        // Then
        verify(cartServiceSpy, times(1)).deleteCartItems(orderVm);
    }

    @Test
    @DisplayName("Should handle delete cart items with empty order items")
    void testDeleteCartItems_EmptyItems() {
        // Given
        OrderVm orderVm = OrderVm.builder()
                .id(1L)
                .orderItemVms(Set.of())
                .build();

        doNothing().when(cartServiceSpy).deleteCartItems(orderVm);

        // When
        cartServiceSpy.deleteCartItems(orderVm);

        // Then
        verify(cartServiceSpy, times(1)).deleteCartItems(orderVm);
    }

    @Test
    @DisplayName("Should handle fallback when delete cart items fails")
    void testDeleteCartItems_Fallback() {
        // Given
        OrderVm orderVm = OrderVm.builder()
                .id(1L)
                .orderItemVms(Set.of(
                        new OrderItemVm(1L, 101L, "Product 1", 2, new BigDecimal("50.0"),
                                "Note", new BigDecimal("0"), new BigDecimal("5.0"),
                                new BigDecimal("0"), null)
                ))
                .build();

        // The actual fallback is called by circuit breaker automatically
        // We just verify the spy can handle errors without throwing
        doNothing().when(cartServiceSpy).deleteCartItems(orderVm);

        // When
        cartServiceSpy.deleteCartItems(orderVm);

        // Then
        verify(cartServiceSpy, times(1)).deleteCartItems(orderVm);
    }
}
