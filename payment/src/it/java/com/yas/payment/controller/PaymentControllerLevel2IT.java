package com.yas.payment.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yas.payment.model.CapturedPayment;
import com.yas.payment.model.InitiatedPayment;
import com.yas.payment.model.enumeration.PaymentMethod;
import com.yas.payment.model.enumeration.PaymentStatus;
import com.yas.payment.service.OrderService;
import com.yas.payment.service.provider.handler.PaypalHandler;
import com.yas.payment.viewmodel.CapturePaymentRequestVm;
import com.yas.payment.viewmodel.InitPaymentRequestVm;
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

/**
 * Integration Tests for PaymentController - Level 2: Frontend ↔ Backend
 * 
 * These tests validate the API contract and HTTP response structure
 * that the frontend expects when interacting with payment endpoints.
 * 
 * Test Cases:
 * - PAYMENT-FE-INT-01: Frontend inicializa pago - valida estructura de respuesta
 * - PAYMENT-FE-INT-02: Frontend captura pago - valida estructura de respuesta
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@DisplayName("PaymentController Integration Tests - Frontend ↔ Backend")
class PaymentControllerLevel2IT {

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

    @MockBean
    private PaypalHandler paypalHandler;

    @MockBean
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        Mockito.reset(paypalHandler, orderService);
        Mockito.when(paypalHandler.getProviderId()).thenReturn(PAYMENT_METHOD);
    }

    @Test
    @DisplayName("PAYMENT-FE-INT-01: Frontend inicializa pago - valida estructura de respuesta")
    void testInitPayment_ValidatesResponseStructureForFrontend() throws Exception {
        // Given: Init payment request
        InitPaymentRequestVm requestVm = InitPaymentRequestVm.builder()
                .checkoutId("chk-init-123")
                .paymentMethod(PAYMENT_METHOD)
                .totalPrice(BigDecimal.valueOf(100.00))
                .build();

        InitiatedPayment initiatedPayment = InitiatedPayment.builder()
                .paymentId("pay-init-123")
                .redirectUrl("https://paypal.com/checkout")
                .status("SUCCESS")
                .build();

        Mockito.when(paypalHandler.initPayment(Mockito.any())).thenReturn(initiatedPayment);

        // When: Frontend calls POST /init
        mockMvc.perform(post("/init")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVm)))
                // Then: Validate HTTP response structure for frontend
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Content-Type", "application/json"))
                // Validate response JSON structure
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.redirectUrl").exists());
    }

    @Test
    @DisplayName("PAYMENT-FE-INT-02: Frontend captura pago - valida estructura de respuesta")
    void testCapturePayment_ValidatesResponseStructureForFrontend() throws Exception {
        // Given: Successful payment capture
        CapturedPayment capturedPayment = CapturedPayment.builder()
                .checkoutId("chk-success")
                .amount(BigDecimal.valueOf(150.00))
                .paymentFee(BigDecimal.valueOf(2.50))
                .gatewayTransactionId("gw-tx-001")
                .paymentMethod(PaymentMethod.PAYPAL)
                .paymentStatus(PaymentStatus.COMPLETED)
                .failureMessage(null)
                .build();

        Mockito.when(paypalHandler.capturePayment(Mockito.any())).thenReturn(capturedPayment);
        Mockito.when(orderService.updateCheckoutStatus(Mockito.any())).thenReturn(500L);
        Mockito.doNothing().when(orderService).updateOrderStatus(Mockito.any());

        CapturePaymentRequestVm requestVm = CapturePaymentRequestVm.builder()
                .paymentMethod(PAYMENT_METHOD)
                .token("token-valid")
                .build();

        // When: Frontend calls POST /capture
        mockMvc.perform(post("/capture")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVm)))
                // Then: Validate HTTP response structure for frontend
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Content-Type", "application/json"))
                // Validate response JSON structure
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.orderId").value(500))
                .andExpect(jsonPath("$.checkoutId").exists())
                .andExpect(jsonPath("$.checkoutId").value("chk-success"))
                .andExpect(jsonPath("$.amount").exists())
                .andExpect(jsonPath("$.amount").isNumber())
                .andExpect(jsonPath("$.paymentFee").exists())
                .andExpect(jsonPath("$.paymentFee").isNumber())
                .andExpect(jsonPath("$.gatewayTransactionId").exists())
                .andExpect(jsonPath("$.gatewayTransactionId").value("gw-tx-001"))
                .andExpect(jsonPath("$.paymentMethod").exists())
                .andExpect(jsonPath("$.paymentMethod").value(PAYMENT_METHOD))
                .andExpect(jsonPath("$.paymentStatus").exists())
                .andExpect(jsonPath("$.paymentStatus").value(PaymentStatus.COMPLETED.name()))
                .andExpect(jsonPath("$.failureMessage").doesNotExist());
    }

    @Test
    @DisplayName("PAYMENT-FE-INT-03: Frontend cancela pago - valida respuesta")
    void testCancelPayment_ValidatesResponseForFrontend() throws Exception {
        // When: Frontend calls GET /cancel
        mockMvc.perform(get("/cancel")
                        .with(authenticatedUser()))
                // Then: Validate HTTP response
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value("Payment cancelled"));
    }

    private RequestPostProcessor authenticatedUser() {
        return jwt().jwt(jwt -> jwt.claim("realm_access", Map.of("roles", List.of("USER"))));
    }
}
