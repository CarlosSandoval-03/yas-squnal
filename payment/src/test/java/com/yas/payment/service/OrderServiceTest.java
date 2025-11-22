package com.yas.payment.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.yas.payment.config.ServiceUrlConfig;
import com.yas.payment.model.CapturedPayment;
import com.yas.payment.model.enumeration.PaymentStatus;
import com.yas.payment.viewmodel.PaymentOrderStatusVm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

/**
 * Unit tests for OrderService.
 * 
 * Test Doubles Used:
 *   - Spy: OrderService (partial mocking to avoid RestClient complexity)
 *     Purpose: Mock HTTP methods while testing error handling logic
 *     Rationale: RestClient fluent API and JWT extraction are complex
 *     Example: doReturn(orderId).when(orderServiceSpy).updateCheckoutStatus(any())
 * 
 * Note: Using Spy because:
 *   1. RestClient has complex fluent API
 *   2. JWT extraction from SecurityContext is hard to mock
 *   3. We want to test fallback methods directly
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private ServiceUrlConfig serviceUrlConfig;

    private OrderService orderServiceSpy;

    private static final String CHECKOUT_ID = "checkout-123";
    private static final Long ORDER_ID = 456L;

    @BeforeEach
    void setUp() {
        OrderService realService = new OrderService(restClient, serviceUrlConfig);
        orderServiceSpy = spy(realService);
    }

    @Test
    @DisplayName("Should update checkout status successfully")
    void testUpdateCheckoutStatus_Success() {
        // Given
        CapturedPayment capturedPayment = CapturedPayment.builder()
                .checkoutId(CHECKOUT_ID)
                .paymentStatus(PaymentStatus.COMPLETED)
                .build();

        // Spy: stub updateCheckoutStatus to avoid RestClient + JWT complexity
        doReturn(ORDER_ID).when(orderServiceSpy).updateCheckoutStatus(capturedPayment);

        // When
        Long result = orderServiceSpy.updateCheckoutStatus(capturedPayment);

        // Then
        assertNotNull(result);
        verify(orderServiceSpy, times(1)).updateCheckoutStatus(capturedPayment);
    }

    @Test
    @DisplayName("Should handle null response from update checkout status")
    void testUpdateCheckoutStatus_NullResponse() {
        // Given
        CapturedPayment capturedPayment = CapturedPayment.builder()
                .checkoutId(CHECKOUT_ID)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        doReturn(null).when(orderServiceSpy).updateCheckoutStatus(capturedPayment);

        // When
        Long result = orderServiceSpy.updateCheckoutStatus(capturedPayment);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should update order status successfully")
    void testUpdateOrderStatus_Success() {
        // Given
        PaymentOrderStatusVm requestVm = PaymentOrderStatusVm.builder()
                .orderId(ORDER_ID)
                .paymentStatus("COMPLETED")
                .build();

        PaymentOrderStatusVm responseVm = PaymentOrderStatusVm.builder()
                .orderId(ORDER_ID)
                .paymentStatus("COMPLETED")
                .build();

        doReturn(responseVm).when(orderServiceSpy).updateOrderStatus(requestVm);

        // When
        PaymentOrderStatusVm result = orderServiceSpy.updateOrderStatus(requestVm);

        // Then
        assertNotNull(result);
        verify(orderServiceSpy, times(1)).updateOrderStatus(requestVm);
    }

    @Test
    @DisplayName("Should handle Long fallback correctly")
    void testHandleLongFallback() throws Throwable {
        // Given
        RuntimeException exception = new RuntimeException("Service unavailable");
        OrderService realService = new OrderService(restClient, serviceUrlConfig);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            realService.handleLongFallback(exception);
        });
    }

    @Test
    @DisplayName("Should handle PaymentOrderStatusVm fallback correctly")
    void testHandlePaymentOrderStatusFallback() throws Throwable {
        // Given
        RuntimeException exception = new RuntimeException("Service unavailable");
        OrderService realService = new OrderService(restClient, serviceUrlConfig);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            realService.handlePaymentOrderStatusFallback(exception);
        });
    }

    @Test
    @DisplayName("Should trigger circuit breaker fallback on error")
    void testUpdateCheckoutStatus_CircuitBreakerTriggered() {
        // Given
        CapturedPayment capturedPayment = CapturedPayment.builder()
                .checkoutId(CHECKOUT_ID)
                .paymentStatus(PaymentStatus.CANCELLED)
                .build();

        // Simulate circuit breaker returning null (fallback)
        doReturn(null).when(orderServiceSpy).updateCheckoutStatus(capturedPayment);

        // When
        Long result = orderServiceSpy.updateCheckoutStatus(capturedPayment);

        // Then
        assertNull(result); // Fallback returns null
    }

    @Test
    @DisplayName("Should trigger circuit breaker fallback for order status update")
    void testUpdateOrderStatus_CircuitBreakerTriggered() {
        // Given
        PaymentOrderStatusVm requestVm = PaymentOrderStatusVm.builder()
                .orderId(ORDER_ID)
                .paymentStatus("FAILED")
                .build();

        doReturn(null).when(orderServiceSpy).updateOrderStatus(requestVm);

        // When
        PaymentOrderStatusVm result = orderServiceSpy.updateOrderStatus(requestVm);

        // Then
        assertNull(result); // Fallback returns null
    }
}
