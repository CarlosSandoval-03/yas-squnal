package com.yas.cart.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yas.cart.model.CartItem;
import com.yas.cart.repository.CartItemRepository;
import com.yas.cart.service.ProductService;
import com.yas.cart.viewmodel.CartItemPostVm;
import com.yas.commonlibrary.utils.AuthenticationUtils;
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
 * Integration Tests for CartItemController - Level 2: Frontend ↔ Backend
 * 
 * These tests validate the API contract and HTTP response structure
 * that the frontend expects. They focus on JSON structure, HTTP headers,
 * and status codes rather than database persistence.
 * 
 * Test Cases:
 * - CART-FE-INT-01: Frontend obtains cart items - validates JSON structure
 * - CART-FE-INT-02: Frontend adds item to cart - validates response structure
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@DisplayName("CartItemController Integration Tests - Frontend ↔ Backend")
class CartItemControllerLevel2IT {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cart_test")
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
    private CartItemRepository cartItemRepository;

    @MockBean
    private ProductService productService;

    private static final String CUSTOMER_ID = "customer-level2-tests";
    private static final Long PRODUCT_ID = 100L;
    private static final int QUANTITY = 3;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();
        Mockito.reset(productService);
        Mockito.when(productService.existsById(Mockito.anyLong())).thenReturn(true);
    }

    @Test
    @DisplayName("CART-FE-INT-01: Frontend obtiene items del carrito - valida estructura JSON")
    void testGetCartItems_ValidatesJsonStructureForFrontend() throws Exception {
        // Given: Items in database
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);

            CartItem item1 = CartItem.builder()
                    .customerId(CUSTOMER_ID)
                    .productId(100L)
                    .quantity(2)
                    .build();
            CartItem item2 = CartItem.builder()
                    .customerId(CUSTOMER_ID)
                    .productId(200L)
                    .quantity(1)
                    .build();
            cartItemRepository.saveAll(List.of(item1, item2));

            // When: Frontend calls GET /storefront/cart/items
            mockMvc.perform(get("/storefront/cart/items")
                            .with(authenticatedUser()))
                    // Then: Validate HTTP response structure for frontend
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    // Validate first item structure
                    .andExpect(jsonPath("$[0].customerId").exists())
                    .andExpect(jsonPath("$[0].customerId").value(CUSTOMER_ID))
                    .andExpect(jsonPath("$[0].productId").exists())
                    .andExpect(jsonPath("$[0].productId").isNumber())
                    .andExpect(jsonPath("$[0].quantity").exists())
                    .andExpect(jsonPath("$[0].quantity").isNumber())
                    // Validate second item structure
                    .andExpect(jsonPath("$[1].customerId").exists())
                    .andExpect(jsonPath("$[1].productId").exists())
                    .andExpect(jsonPath("$[1].quantity").exists());
        }
    }

    @Test
    @DisplayName("CART-FE-INT-02: Frontend agrega item al carrito - valida estructura de respuesta")
    void testAddCartItem_ValidatesResponseStructureForFrontend() throws Exception {
        // Given: Cart item to add
        CartItemPostVm cartItemPostVm = new CartItemPostVm(PRODUCT_ID, QUANTITY);

        // When: Frontend sends POST /storefront/cart/items
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);

            mockMvc.perform(post("/storefront/cart/items")
                            .with(authenticatedUser())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cartItemPostVm)))
                    // Then: Validate HTTP response structure for frontend
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(header().string("Content-Type", "application/json"))
                    // Validate response JSON structure
                    .andExpect(jsonPath("$.customerId").exists())
                    .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID))
                    .andExpect(jsonPath("$.productId").exists())
                    .andExpect(jsonPath("$.productId").value(PRODUCT_ID))
                    .andExpect(jsonPath("$.quantity").exists())
                    .andExpect(jsonPath("$.quantity").value(QUANTITY))
                    // Ensure no unexpected fields
                    .andExpect(jsonPath("$.id").doesNotExist());
        }
    }

    private RequestPostProcessor authenticatedUser() {
        return jwt().jwt(jwt -> jwt.claim("realm_access", Map.of("roles", List.of("USER"))));
    }
}
