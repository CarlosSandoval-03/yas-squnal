package com.yas.cart.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.cart.service.CartItemService;
import com.yas.cart.viewmodel.CartItemDeleteVm;
import com.yas.cart.viewmodel.CartItemGetVm;
import com.yas.cart.viewmodel.CartItemPostVm;
import com.yas.cart.viewmodel.CartItemPutVm;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for CartItemController.
 * 
 * Test Doubles Used:
 *   - Mock: CartItemService
 *     Purpose: Verify that controller delegates correctly to service layer
 *     Example: verify(cartItemService, times(1)).addCartItem(cartItemPostVm)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CartItemController Unit Tests")
class CartItemControllerTest {

    @Mock
    private CartItemService cartItemService;

    @InjectMocks
    private CartItemController cartItemController;

    private static final String CUSTOMER_ID = "customer-123";
    private static final Long PRODUCT_ID = 1L;
    private static final int QUANTITY = 2;

    private CartItemPostVm cartItemPostVm;
    private CartItemPutVm cartItemPutVm;
    private CartItemGetVm cartItemGetVm;

    @BeforeEach
    void setUp() {
        cartItemPostVm = new CartItemPostVm(PRODUCT_ID, QUANTITY);
        cartItemPutVm = new CartItemPutVm(5);
        cartItemGetVm = new CartItemGetVm(CUSTOMER_ID, PRODUCT_ID, QUANTITY);
    }

    @Test
    @DisplayName("Should successfully add cart item via POST endpoint")
    void testAddCartItem_Success() {
        // Given
        when(cartItemService.addCartItem(cartItemPostVm)).thenReturn(cartItemGetVm);

        // When
        ResponseEntity<CartItemGetVm> response = cartItemController.addCartItem(cartItemPostVm);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(PRODUCT_ID, response.getBody().productId());
        assertEquals(QUANTITY, response.getBody().quantity());
        
        verify(cartItemService, times(1)).addCartItem(cartItemPostVm);
    }

    @Test
    @DisplayName("Should successfully update cart item via PUT endpoint")
    void testUpdateCartItem_Success() {
        // Given
        CartItemGetVm updatedCartItem = new CartItemGetVm(CUSTOMER_ID, PRODUCT_ID, 5);
        when(cartItemService.updateCartItem(PRODUCT_ID, cartItemPutVm)).thenReturn(updatedCartItem);

        // When
        ResponseEntity<CartItemGetVm> response = cartItemController.updateCartItem(PRODUCT_ID, cartItemPutVm);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5, response.getBody().quantity());
        
        verify(cartItemService, times(1)).updateCartItem(PRODUCT_ID, cartItemPutVm);
    }

    @Test
    @DisplayName("Should retrieve all cart items via GET endpoint")
    void testGetCartItems_Success() {
        // Given
        List<CartItemGetVm> cartItems = Arrays.asList(
                new CartItemGetVm(CUSTOMER_ID, 1L, 2),
                new CartItemGetVm(CUSTOMER_ID, 2L, 3)
        );
        when(cartItemService.getCartItems()).thenReturn(cartItems);

        // When
        ResponseEntity<List<CartItemGetVm>> response = cartItemController.getCartItems();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        
        verify(cartItemService, times(1)).getCartItems();
    }

    @Test
    @DisplayName("Should return empty list when no cart items exist")
    void testGetCartItems_EmptyCart() {
        // Given
        when(cartItemService.getCartItems()).thenReturn(Collections.emptyList());

        // When
        ResponseEntity<List<CartItemGetVm>> response = cartItemController.getCartItems();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
        
        verify(cartItemService, times(1)).getCartItems();
    }

    @Test
    @DisplayName("Should remove/adjust cart items via POST remove endpoint")
    void testRemoveCartItems_Success() {
        // Given
        List<CartItemDeleteVm> deleteVms = Arrays.asList(
                new CartItemDeleteVm(1L, 2),
                new CartItemDeleteVm(2L, 1)
        );
        
        List<CartItemGetVm> remainingItems = Collections.singletonList(
                new CartItemGetVm(CUSTOMER_ID, 2L, 2) // Item 2 had quantity 3, deleted 1, remaining 2
        );
        
        when(cartItemService.deleteOrAdjustCartItem(deleteVms)).thenReturn(remainingItems);

        // When
        ResponseEntity<List<CartItemGetVm>> response = cartItemController.removeCartItems(deleteVms);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        
        verify(cartItemService, times(1)).deleteOrAdjustCartItem(deleteVms);
    }

    @Test
    @DisplayName("Should delete cart item via DELETE endpoint")
    void testDeleteCartItem_Success() {
        // Given
        doNothing().when(cartItemService).deleteCartItem(PRODUCT_ID);

        // When
        ResponseEntity<Void> response = cartItemController.deleteCartItem(PRODUCT_ID);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        
        verify(cartItemService, times(1)).deleteCartItem(PRODUCT_ID);
    }

    @Test
    @DisplayName("Should delegate service exceptions to exception handler")
    void testAddCartItem_ServiceThrowsException() {
        // Given
        when(cartItemService.addCartItem(any(CartItemPostVm.class)))
                .thenThrow(new RuntimeException("Service error"));

        // When & Then
        try {
            cartItemController.addCartItem(cartItemPostVm);
        } catch (RuntimeException e) {
            assertEquals("Service error", e.getMessage());
        }
        
        verify(cartItemService, times(1)).addCartItem(cartItemPostVm);
    }
}
