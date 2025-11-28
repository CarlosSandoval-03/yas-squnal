package com.yas.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yas.payment.model.CapturedPayment;
import com.yas.payment.model.Payment;
import com.yas.payment.model.enumeration.PaymentMethod;
import com.yas.payment.model.enumeration.PaymentStatus;
import com.yas.payment.repository.PaymentRepository;
import com.yas.payment.service.OrderService;
import com.yas.payment.service.provider.handler.PaypalHandler;
import com.yas.payment.viewmodel.CapturePaymentRequestVm;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@DisplayName("PaymentController Integration Tests - Backend ↔ Database")
class PaymentControllerIT {

    private static final String PAYMENT_METHOD = PaymentMethod.PAYPAL.name();

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("payment_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockBean
    private PaypalHandler paypalHandler;

    @MockBean
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        Mockito.reset(paypalHandler, orderService);
        Mockito.when(paypalHandler.getProviderId()).thenReturn(PAYMENT_METHOD);
    }

    @Test
    @DisplayName("PAYMENT-BE-INT-01: Capturar pago exitoso persiste registro en DB")
    void testCapturePayment_PersistsSuccessfulRecord() throws Exception {
        CapturedPayment capturedPayment = CapturedPayment.builder()
                .checkoutId("chk-success")
                .amount(BigDecimal.valueOf(120.45))
                .paymentFee(BigDecimal.valueOf(2.10))
                .gatewayTransactionId("gw-001")
                .paymentMethod(PaymentMethod.PAYPAL)
                .paymentStatus(PaymentStatus.COMPLETED)
                .failureMessage(null)
                .build();

        Mockito.when(paypalHandler.capturePayment(Mockito.any())).thenReturn(capturedPayment);
        Mockito.when(orderService.updateCheckoutStatus(Mockito.any())).thenReturn(987L);
        Mockito.doNothing().when(orderService).updateOrderStatus(Mockito.any());

        CapturePaymentRequestVm requestVm = CapturePaymentRequestVm.builder()
                .paymentMethod(PAYMENT_METHOD)
                .token("token-success")
                .build();

        mockMvc.perform(post("/capture")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(987))
                .andExpect(jsonPath("$.paymentStatus").value(PaymentStatus.COMPLETED.name()))
                .andExpect(jsonPath("$.paymentMethod").value(PAYMENT_METHOD));

        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments).hasSize(1);
        Payment savedPayment = payments.get(0);
        assertThat(savedPayment.getOrderId()).isEqualTo(987L);
        assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(savedPayment.getGatewayTransactionId()).isEqualTo("gw-001");
        assertThat(savedPayment.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(120.45));
    }

    @Test
    @DisplayName("PAYMENT-BE-INT-02: Capturar pago con error almacena failureMessage en DB")
    void testCapturePayment_PersistsFailureDetails() throws Exception {
        CapturedPayment capturedPayment = CapturedPayment.builder()
                .checkoutId("chk-failed")
                .amount(BigDecimal.valueOf(85.00))
                .paymentFee(BigDecimal.valueOf(1.50))
                .gatewayTransactionId("gw-err")
                .paymentMethod(PaymentMethod.PAYPAL)
                .paymentStatus(PaymentStatus.CANCELLED)
                .failureMessage("Card declined")
                .build();

        Mockito.when(paypalHandler.capturePayment(Mockito.any())).thenReturn(capturedPayment);
        Mockito.when(orderService.updateCheckoutStatus(Mockito.any())).thenReturn(654L);
        Mockito.doNothing().when(orderService).updateOrderStatus(Mockito.any());

        CapturePaymentRequestVm requestVm = CapturePaymentRequestVm.builder()
                .paymentMethod(PAYMENT_METHOD)
                .token("token-failure")
                .build();

        mockMvc.perform(post("/capture")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(654))
                .andExpect(jsonPath("$.paymentStatus").value(PaymentStatus.CANCELLED.name()))
                .andExpect(jsonPath("$.failureMessage").value("Card declined"));

        Payment savedPayment = paymentRepository.findAll().get(0);
        assertThat(savedPayment.getOrderId()).isEqualTo(654L);
        assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(savedPayment.getFailureMessage()).isEqualTo("Card declined");
    }

    private RequestPostProcessor authenticatedUser() {
        return jwt().jwt(jwt -> jwt.claim("realm_access", Map.of("roles", List.of("USER"))));
    }
}

