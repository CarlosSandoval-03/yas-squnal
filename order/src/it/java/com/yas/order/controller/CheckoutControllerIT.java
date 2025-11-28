package com.yas.order.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.yas.order.viewmodel.checkout.CheckoutStatusPutVm;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@DisplayName("CheckoutController Integration Tests - Backend ↔ Database")
class CheckoutControllerIT {

    private static final String CUSTOMER_ID = "customer-int-tests";

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
    @DisplayName("ORDER-BE-INT-01: Crear checkout persiste items y estado PENDING")
    void testCreateCheckout_PersistsData() throws Exception {
        Map<Long, ProductCheckoutListVm> productInfo = Map.of(1L, ProductCheckoutListVm.builder()
                .id(1L)
                .name("Product One")
                .price(50.0)
                .taxClassId(1L)
                .build());
        Mockito.when(productService.getProductInfomation(Mockito.anySet(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(productInfo);

        CheckoutPostVm requestVm = new CheckoutPostVm(
                "customer@email.com",
                "Deliver ASAP",
                "PROMO10",
                "SHIP_STANDARD",
                "PAYPAL",
                "ADDR-1",
                List.of(new CheckoutItemPostVm(1L, "Great product", 2))
        );

        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);

            mockMvc.perform(post("/storefront/checkouts")
                            .with(authenticatedUser())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestVm)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.checkoutState").value(CheckoutState.PENDING.name()))
                    .andExpect(jsonPath("$.checkoutItemVms.length()").value(1))
                    .andExpect(jsonPath("$.totalAmount").value(100.0));
        }

        List<Checkout> checkouts = checkoutRepository.findAll();
        assertThat(checkouts).hasSize(1);
        Checkout savedCheckout = checkouts.get(0);
        assertThat(savedCheckout.getCheckoutState()).isEqualTo(CheckoutState.PENDING);
        assertThat(savedCheckout.getPaymentMethodId()).isEqualTo("PAYPAL");
        assertThat(savedCheckout.getCheckoutItems()).hasSize(1);
        assertThat(savedCheckout.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(100.0));
    }

    @Test
    @DisplayName("ORDER-BE-INT-02: Actualizar estado de checkout refleja cambio en DB")
    void testUpdateCheckoutStatus_PersistsNewState() throws Exception {
        Checkout checkout = Checkout.builder()
                .email("customer@email.com")
                .paymentMethodId("PAYPAL")
                .checkoutState(CheckoutState.PENDING)
                .build();
        checkout.setCreatedBy(CUSTOMER_ID);
        checkout = checkoutRepository.save(checkout);

        CheckoutStatusPutVm statusPutVm = new CheckoutStatusPutVm(checkout.getId(), CheckoutState.COMPLETED.name());

        Mockito.when(orderService.findOrderByCheckoutId(checkout.getId())).thenAnswer(invocation -> {
            com.yas.order.model.Order order = new com.yas.order.model.Order();
            order.setId(321L);
            return order;
        });

        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);

            mockMvc.perform(put("/storefront/checkouts/status")
                            .with(authenticatedUser())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusPutVm)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(321));
        }

        Checkout updatedCheckout = checkoutRepository.findById(checkout.getId()).orElseThrow();
        assertThat(updatedCheckout.getCheckoutState()).isEqualTo(CheckoutState.COMPLETED);
    }

    private RequestPostProcessor authenticatedUser() {
        return jwt().jwt(jwt -> jwt.claim("realm_access", Map.of("roles", List.of("USER"))));
    }
}

