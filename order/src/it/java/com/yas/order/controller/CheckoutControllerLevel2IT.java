package com.yas.order.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yas.commonlibrary.utils.AuthenticationUtils;
import com.yas.order.model.Checkout;
import com.yas.order.model.enumeration.CheckoutState;
import com.yas.order.repository.CheckoutRepository;
import com.yas.order.service.OrderService;
import com.yas.order.service.ProductService;
import com.yas.order.viewmodel.checkout.CheckoutItemPostVm;
import com.yas.order.viewmodel.checkout.CheckoutPostVm;
import com.yas.order.viewmodel.product.ProductCheckoutListVm;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
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
 * Integration Tests for CheckoutController - Level 2: Frontend ↔ Backend
 * 
 * These tests validate the API contract and HTTP response structure
 * that the frontend expects when interacting with checkout endpoints.
 * 
 * Test Cases:
 * - ORDER-FE-INT-01: Frontend crea checkout - valida estructura de respuesta
 * - ORDER-FE-INT-02: Frontend obtiene checkout - valida estructura de respuesta
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@DisplayName("CheckoutController Integration Tests - Frontend ↔ Backend")
class CheckoutControllerLevel2IT {

    private static final String CUSTOMER_ID = "customer-level2-tests";

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("order_test")
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
    private CheckoutRepository checkoutRepository;

    @MockBean
    private ProductService productService;

    @MockBean
    private OrderService orderService;

    @BeforeEach
    void cleanDatabase() {
        checkoutRepository.deleteAll();
    }

    @Test
    @DisplayName("ORDER-FE-INT-01: Frontend crea checkout - valida estructura de respuesta")
    void testCreateCheckout_ValidatesResponseStructureForFrontend() throws Exception {
        // Given: Product information mocked
        Map<Long, ProductCheckoutListVm> productInfo = Map.of(1L, ProductCheckoutListVm.builder()
                .id(1L)
                .name("Test Product")
                .price(75.50)
                .taxClassId(1L)
                .build());
        Mockito.when(productService.getProductInfomation(Mockito.anySet(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(productInfo);

        CheckoutPostVm requestVm = new CheckoutPostVm(
                "customer@test.com",
                "Test note",
                "PROMO10",
                "SHIP_STANDARD",
                "PAYPAL",
                "ADDR-1",
                List.of(new CheckoutItemPostVm(1L, "Test product description", 2))
        );

        // When: Frontend calls POST /storefront/checkouts
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);

            mockMvc.perform(post("/storefront/checkouts")
                            .with(authenticatedUser())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestVm)))
                    // Then: Validate HTTP response structure for frontend
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(header().string("Content-Type", "application/json"))
                    // Validate response JSON structure
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.id").isString())
                    .andExpect(jsonPath("$.email").exists())
                    .andExpect(jsonPath("$.email").value("customer@test.com"))
                    .andExpect(jsonPath("$.checkoutState").exists())
                    .andExpect(jsonPath("$.checkoutState").value(CheckoutState.PENDING.name()))
                    .andExpect(jsonPath("$.totalAmount").exists())
                    .andExpect(jsonPath("$.totalAmount").isNumber())
                    .andExpect(jsonPath("$.paymentMethodId").exists())
                    .andExpect(jsonPath("$.paymentMethodId").value("PAYPAL"));
        }
    }

    @Test
    @DisplayName("ORDER-FE-INT-02: Frontend obtiene checkout - valida estructura de respuesta")
    void testGetCheckout_ValidatesResponseStructureForFrontend() throws Exception {
        // Given: Checkout in database
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);

            Checkout checkout = Checkout.builder()
                    .email("customer@test.com")
                    .paymentMethodId("PAYPAL")
                    .checkoutState(CheckoutState.PENDING)
                    .totalAmount(BigDecimal.valueOf(150.00))
                    .build();
            checkout.setCreatedBy(CUSTOMER_ID);
            checkout = checkoutRepository.save(checkout);

            // When: Frontend calls GET /storefront/checkouts/{id}
            mockMvc.perform(get("/storefront/checkouts/{id}", checkout.getId())
                            .with(authenticatedUser()))
                    // Then: Validate HTTP response structure for frontend
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(header().string("Content-Type", "application/json"))
                    // Validate response JSON structure
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.id").value(checkout.getId()))
                    .andExpect(jsonPath("$.email").exists())
                    .andExpect(jsonPath("$.email").value("customer@test.com"))
                    .andExpect(jsonPath("$.checkoutState").exists())
                    .andExpect(jsonPath("$.checkoutState").value(CheckoutState.PENDING.name()))
                    .andExpect(jsonPath("$.totalAmount").exists())
                    .andExpect(jsonPath("$.totalAmount").isNumber())
                    .andExpect(jsonPath("$.paymentMethodId").exists())
                    .andExpect(jsonPath("$.paymentMethodId").value("PAYPAL"));
        }
    }

    private RequestPostProcessor authenticatedUser() {
        return jwt().jwt(jwt -> jwt.claim("realm_access", Map.of("roles", List.of("USER"))));
    }
}
