package com.yas.cart.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yas.cart.model.CartItem;
import com.yas.cart.repository.CartItemRepository;
import com.yas.cart.service.ProductService;
import com.yas.cart.viewmodel.CartItemPostVm;
import com.yas.commonlibrary.utils.AuthenticationUtils;
import java.util.List;
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
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration Tests for CartItemController - Level 1: Backend ↔ Database
 * 
 * These tests validate the integration between the REST API and the database.
 * They use a real PostgreSQL database via Testcontainers and verify data persistence.
 * 
 * Test Cases:
 * - CART-BE-INT-01: Add item to cart and verify it can be read
 * - CART-BE-INT-02: Read cart items and verify JSON structure
 * - CART-BE-INT-03: Update cart item quantity
 * - CART-BE-INT-04: Remove item from cart
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@DisplayName("CartItemController Integration Tests - Backend ↔ Database")
class CartItemControllerIT {

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
    private CartItemRepository cartItemRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private static final String CUSTOMER_ID = "customer-123";
    private static final Long PRODUCT_ID = 1L;
    private static final int QUANTITY = 2;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        cartItemRepository.deleteAll();
        Mockito.reset(productService);
        Mockito.when(productService.existsById(Mockito.anyLong())).thenReturn(true);
    }

    @Test
    @DisplayName("CART-BE-INT-01: Add item to cart and verify it can be read from database")
    void testAddCartItem_ThenReadFromDatabase() throws Exception {
        // Given: A cart item to add
        CartItemPostVm cartItemPostVm = new CartItemPostVm(PRODUCT_ID, QUANTITY);

        // When: POST request to add item
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);

            mockMvc.perform(post("/storefront/cart/items")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cartItemPostVm)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productId").value(PRODUCT_ID))
                    .andExpect(jsonPath("$.quantity").value(QUANTITY));
        }

        // Then: Verify item is persisted in database
        var items = cartItemRepository.findByCustomerIdOrderByCreatedOnDesc(CUSTOMER_ID);
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(items.get(0).getQuantity()).isEqualTo(QUANTITY);
        assertThat(items.get(0).getCustomerId()).isEqualTo(CUSTOMER_ID);
    }

    @Test
    @DisplayName("CART-BE-INT-02: Read cart items and verify JSON structure")
    void testGetCartItems_VerifyJsonStructure() throws Exception {
        // Given: Items already in database
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);

            // Add items directly to database
            CartItem item1 = CartItem.builder()
                    .customerId(CUSTOMER_ID)
                    .productId(1L)
                    .quantity(2)
                    .build();
            CartItem item2 = CartItem.builder()
                    .customerId(CUSTOMER_ID)
                    .productId(2L)
                    .quantity(1)
                    .build();
            cartItemRepository.saveAll(List.of(item1, item2));

            // When: GET request to retrieve items
            mockMvc.perform(get("/storefront/cart/items"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].productId").exists())
                    .andExpect(jsonPath("$[0].quantity").exists())
                    .andExpect(jsonPath("$[1].productId").exists())
                    .andExpect(jsonPath("$[1].quantity").exists());
        }

        // Then: Verify items are still in database
        var items = cartItemRepository.findByCustomerIdOrderByCreatedOnDesc(CUSTOMER_ID);
        assertThat(items).hasSize(2);
    }

    @Test
    @DisplayName("CART-BE-INT-03: Update cart item quantity and verify persistence")
    void testUpdateCartItem_VerifyQuantityUpdatedInDatabase() throws Exception {
        // Given: Item already in cart
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);

            CartItem existingItem = CartItem.builder()
                    .customerId(CUSTOMER_ID)
                    .productId(PRODUCT_ID)
                    .quantity(2)
                    .build();
            cartItemRepository.save(existingItem);

            // When: Update quantity via PUT request
            int newQuantity = 5;
            String updateJson = String.format("{\"quantity\": %d}", newQuantity);

            mockMvc.perform(put("/storefront/cart/items/{productId}", PRODUCT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.quantity").value(newQuantity));
        }

        // Then: Verify quantity is updated in database
        CartItem updatedItem = cartItemRepository.findByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID)
                .orElseThrow();
        assertThat(updatedItem.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("CART-BE-INT-04: Remove item from cart and verify deletion from database")
    void testRemoveCartItem_VerifyDeletedFromDatabase() throws Exception {
        // Given: Items in cart
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);

            CartItem item1 = CartItem.builder()
                    .customerId(CUSTOMER_ID)
                    .productId(1L)
                    .quantity(2)
                    .build();
            CartItem item2 = CartItem.builder()
                    .customerId(CUSTOMER_ID)
                    .productId(2L)
                    .quantity(1)
                    .build();
            cartItemRepository.saveAll(List.of(item1, item2));

            // When: Remove item via POST /remove
            String removeJson = "[{\"productId\": 1, \"quantity\": 2}]";

            mockMvc.perform(post("/storefront/cart/items/remove")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(removeJson))
                    .andExpect(status().isOk());
        }

        // Then: Verify item is removed from database
        var remainingItems = cartItemRepository.findByCustomerIdOrderByCreatedOnDesc(CUSTOMER_ID);
        assertThat(remainingItems).hasSize(1);
        assertThat(remainingItems.get(0).getProductId()).isEqualTo(2L);
    }
}

