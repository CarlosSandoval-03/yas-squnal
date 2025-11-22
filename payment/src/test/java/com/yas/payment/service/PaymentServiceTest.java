package com.yas.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.payment.model.CapturedPayment;
import com.yas.payment.model.InitiatedPayment;
import com.yas.payment.model.Payment;
import com.yas.payment.model.enumeration.PaymentMethod;
import com.yas.payment.model.enumeration.PaymentStatus;
import com.yas.payment.repository.PaymentRepository;
import com.yas.payment.service.provider.handler.PaymentHandler;
import com.yas.payment.viewmodel.CapturePaymentRequestVm;
import com.yas.payment.viewmodel.CapturePaymentResponseVm;
import com.yas.payment.viewmodel.InitPaymentRequestVm;
import com.yas.payment.viewmodel.InitPaymentResponseVm;
import com.yas.payment.viewmodel.PaymentOrderStatusVm;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for PaymentService.
 * 
 * Test Doubles Used:
 *   - Mock: PaymentRepository, OrderService, PaymentHandler
 *     Purpose: Verify interactions - check how many times methods were called
 *     Example: verify(paymentHandler, times(1)).initPayment(request)
 *     Records behavior for assertions
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    private static final String CHECKOUT_ID = "checkout-123";
    private static final String PAYMENT_ID = "payment-123";
    private static final String PAYMENT_PROVIDER_ID = "PAYPAL";
    private static final BigDecimal AMOUNT = new BigDecimal("100.00");

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private PaymentHandler paymentHandler;

    @InjectMocks
    private PaymentService paymentService;

    private InitPaymentRequestVm initPaymentRequestVm;
    private CapturePaymentRequestVm capturePaymentRequestVm;
    private InitiatedPayment initiatedPayment;
    private CapturedPayment capturedPayment;
    private Payment payment;

    @BeforeEach
    void setUp() {
        // Initialize payment handlers map
        List<PaymentHandler> handlers = Arrays.asList(paymentHandler);
        paymentService = new PaymentService(paymentRepository, orderService, handlers);
        
        when(paymentHandler.getProviderId()).thenReturn("PAYPAL");
        paymentService.initializeProviders();

        // Setup test data
        initPaymentRequestVm = InitPaymentRequestVm.builder()
                .checkoutId(CHECKOUT_ID)
                .paymentMethod(PAYMENT_PROVIDER_ID)
                .totalPrice(AMOUNT)
                .build();

        capturePaymentRequestVm = CapturePaymentRequestVm.builder()
                .paymentMethod(PAYMENT_PROVIDER_ID)
                .token("test-token")
                .build();

        initiatedPayment = InitiatedPayment.builder()
                .paymentId("payment-init-123")
                .status("CREATED")
                .redirectUrl("https://payment.gateway.com/redirect")
                .build();

        capturedPayment = CapturedPayment.builder()
                .checkoutId("checkout-123")
                .amount(new BigDecimal("100.00"))
                .paymentFee(new BigDecimal("2.50"))
                .paymentMethod(PaymentMethod.PAYPAL)
                .paymentStatus(PaymentStatus.COMPLETED)
                .gatewayTransactionId("txn-456")
                .build();

        payment = Payment.builder()
                .id(1L)
                .checkoutId("checkout-123")
                .orderId(100L)
                .amount(new BigDecimal("100.00"))
                .paymentFee(new BigDecimal("2.50"))
                .paymentMethod(PaymentMethod.PAYPAL)
                .paymentStatus(PaymentStatus.COMPLETED)
                .gatewayTransactionId("txn-456")
                .build();
    }

    @Test
    @DisplayName("Should initialize providers on post construct")
    void testInitializeProviders_Success() {
        // Given
        PaymentHandler handler1 = paymentHandler;
        when(handler1.getProviderId()).thenReturn("PAYPAL");

        // When
        PaymentService service = new PaymentService(paymentRepository, orderService, Arrays.asList(handler1));
        service.initializeProviders();

        // Then
        // Verification is implicit - if no exception thrown, initialization succeeded
        assertNotNull(service);
    }

    @Test
    @DisplayName("Should successfully initialize payment with valid request")
    void testInitPayment_Success() {
        // Given
        when(paymentHandler.initPayment(initPaymentRequestVm)).thenReturn(initiatedPayment);

        // When
        InitPaymentResponseVm response = paymentService.initPayment(initPaymentRequestVm);

        // Then
        assertNotNull(response);
        assertEquals("payment-init-123", response.paymentId());
        assertEquals("CREATED", response.status());
        assertEquals("https://payment.gateway.com/redirect", response.redirectUrl());
        
        verify(paymentHandler, times(1)).initPayment(initPaymentRequestVm);
    }

    @Test
    @DisplayName("Should throw exception when payment handler not found for provider")
    void testInitPayment_HandlerNotFound() {
        // Given
        InitPaymentRequestVm invalidRequest = InitPaymentRequestVm.builder()
                .checkoutId("invalid-checkout")
                .paymentMethod("INVALID_PROVIDER")
                .totalPrice(new BigDecimal("50.00"))
                .build();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> paymentService.initPayment(invalidRequest));
        
        assertEquals("No payment handler found for provider: INVALID_PROVIDER", exception.getMessage());
        verify(paymentHandler, never()).initPayment(any());
    }

    @Test
    @DisplayName("Should successfully capture payment with valid request")
    void testCapturePayment_Success() {
        // Given
        when(paymentHandler.capturePayment(capturePaymentRequestVm)).thenReturn(capturedPayment);
        when(orderService.updateCheckoutStatus(capturedPayment)).thenReturn(100L);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // When
        CapturePaymentResponseVm response = paymentService.capturePayment(capturePaymentRequestVm);

        // Then
        assertNotNull(response);
        assertEquals(100L, response.orderId());
        assertEquals("checkout-123", response.checkoutId());
        assertEquals(new BigDecimal("100.00"), response.amount());
        assertEquals(new BigDecimal("2.50"), response.paymentFee());
        assertEquals("txn-456", response.gatewayTransactionId());
        assertEquals(PaymentMethod.PAYPAL, response.paymentMethod());
        assertEquals(PaymentStatus.COMPLETED, response.paymentStatus());

        verify(paymentHandler, times(1)).capturePayment(capturePaymentRequestVm);
        verify(orderService, times(1)).updateCheckoutStatus(capturedPayment);
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(orderService, times(1)).updateOrderStatus(any(PaymentOrderStatusVm.class));
    }

    @Test
    @DisplayName("Should handle failed payment capture correctly")
    void testCapturePayment_Failed() {
        // Given
        CapturedPayment failedPayment = CapturedPayment.builder()
                .checkoutId("checkout-123")
                .amount(new BigDecimal("100.00"))
                .paymentFee(BigDecimal.ZERO)
                .paymentMethod(PaymentMethod.PAYPAL)
                .paymentStatus(PaymentStatus.CANCELLED)
                .failureMessage("Insufficient funds")
                .build();

        Payment failedPaymentEntity = Payment.builder()
                .id(1L)
                .checkoutId("checkout-123")
                .orderId(100L)
                .amount(new BigDecimal("100.00"))
                .paymentFee(BigDecimal.ZERO)
                .paymentMethod(PaymentMethod.PAYPAL)
                .paymentStatus(PaymentStatus.CANCELLED)
                .failureMessage("Insufficient funds")
                .build();

        when(paymentHandler.capturePayment(capturePaymentRequestVm)).thenReturn(failedPayment);
        when(orderService.updateCheckoutStatus(failedPayment)).thenReturn(100L);
        when(paymentRepository.save(any(Payment.class))).thenReturn(failedPaymentEntity);

        // When
        CapturePaymentResponseVm response = paymentService.capturePayment(capturePaymentRequestVm);

        // Then
        assertNotNull(response);
        assertEquals(PaymentStatus.CANCELLED, response.paymentStatus());
        assertEquals("Insufficient funds", response.failureMessage());

        verify(paymentHandler, times(1)).capturePayment(capturePaymentRequestVm);
        verify(orderService, times(1)).updateCheckoutStatus(failedPayment);
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(orderService, times(1)).updateOrderStatus(any(PaymentOrderStatusVm.class));
    }

    @Test
    @DisplayName("Should throw exception when capturing payment with invalid provider")
    void testCapturePayment_InvalidProvider() {
        // Given
        CapturePaymentRequestVm invalidRequest = CapturePaymentRequestVm.builder()
                .paymentMethod("INVALID_PROVIDER")
                .token("token")
                .build();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> paymentService.capturePayment(invalidRequest));
        
        assertEquals("No payment handler found for provider: INVALID_PROVIDER", exception.getMessage());
        verify(paymentHandler, never()).capturePayment(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create payment entity with all captured payment details")
    void testCreatePayment_AllFieldsMapped() {
        // Given
        CapturedPayment capturedPaymentWithAllFields = CapturedPayment.builder()
                .checkoutId("checkout-456")
                .orderId(200L)
                .amount(new BigDecimal("250.00"))
                .paymentFee(new BigDecimal("5.00"))
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.PENDING)
                .gatewayTransactionId("txn-789")
                .failureMessage(null)
                .build();

        Payment expectedPayment = Payment.builder()
                .id(2L)
                .checkoutId("checkout-456")
                .orderId(200L)
                .amount(new BigDecimal("250.00"))
                .paymentFee(new BigDecimal("5.00"))
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.PENDING)
                .gatewayTransactionId("txn-789")
                .build();

        when(paymentHandler.capturePayment(any(CapturePaymentRequestVm.class))).thenReturn(capturedPaymentWithAllFields);
        when(orderService.updateCheckoutStatus(any())).thenReturn(200L);
        when(paymentRepository.save(any(Payment.class))).thenReturn(expectedPayment);

        // When
        CapturePaymentResponseVm response = paymentService.capturePayment(capturePaymentRequestVm);

        // Then
        assertNotNull(response);
        assertEquals(200L, response.orderId());
        assertEquals("checkout-456", response.checkoutId());
        assertEquals(new BigDecimal("250.00"), response.amount());
        assertEquals(PaymentMethod.COD, response.paymentMethod());
        
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }
}
