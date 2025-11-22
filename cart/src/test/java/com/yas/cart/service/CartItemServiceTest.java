package com.yas.cart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.cart.mapper.CartItemMapper;
import com.yas.cart.model.CartItem;
import com.yas.cart.repository.CartItemRepository;
import com.yas.cart.viewmodel.CartItemDeleteVm;
import com.yas.cart.viewmodel.CartItemGetVm;
import com.yas.cart.viewmodel.CartItemPostVm;
import com.yas.cart.viewmodel.CartItemPutVm;
import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.InternalServerErrorException;
import com.yas.commonlibrary.exception.NotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.PessimisticLockingFailureException;

import com.yas.commonlibrary.utils.AuthenticationUtils;

/**
 * Unit tests for CartItemService.
 * 
 * Test Doubles Used:
 *   - Mock: CartItemRepository, ProductService, CartItemMapper
 *     Purpose: Record and verify interactions - check that methods were called
 *     Example: verify(repository, times(1)).save(cartItem)
 *   - Stub: AuthenticationUtils via MockedStatic
 *     Purpose: Return fixed responses with no additional logic
 *     Example: Always returns "customer-123" when extractUserId() is called
 *     Note: Similar to "Responder" pattern - injects valid values
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CartItemService Unit Tests")
class CartItemServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductService productService;

    @Mock
    private CartItemMapper cartItemMapper;

    @InjectMocks
    private CartItemService cartItemService;

    private static final String CUSTOMER_ID = "customer-123";
    private static final Long PRODUCT_ID = 1L;
    private static final int QUANTITY = 2;

    private CartItemPostVm cartItemPostVm;
    private CartItem cartItem;
    private CartItemGetVm cartItemGetVm;

    @BeforeEach
    void setUp() {
        cartItemPostVm = new CartItemPostVm(PRODUCT_ID, QUANTITY);

        cartItem = CartItem.builder()
                .customerId(CUSTOMER_ID)
                .productId(PRODUCT_ID)
                .quantity(QUANTITY)
                .build();

        cartItemGetVm = new CartItemGetVm(CUSTOMER_ID, PRODUCT_ID, QUANTITY);
    }

    @Test
    @DisplayName("Should successfully add new cart item when product exists and item not in cart")
    void testAddCartItem_NewItem_Success() {
        // Given
        when(productService.existsById(PRODUCT_ID)).thenReturn(true);
        when(cartItemMapper.toCartItem(cartItemPostVm, CUSTOMER_ID)).thenReturn(cartItem);
        when(cartItemRepository.findByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID))
                .thenReturn(Optional.empty());
        when(cartItemRepository.save(cartItem)).thenReturn(cartItem);
        when(cartItemMapper.toGetVm(cartItem)).thenReturn(cartItemGetVm);

        // When
        CartItemGetVm result;
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);
            result = cartItemService.addCartItem(cartItemPostVm);
        }

        // Then
        assertNotNull(result);
        assertEquals(PRODUCT_ID, result.productId());
        assertEquals(QUANTITY, result.quantity());

        verify(productService, times(1)).existsById(PRODUCT_ID);
        verify(cartItemRepository, times(1)).findByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID);
        verify(cartItemRepository, times(1)).save(cartItem);
    }

    @Test
    @DisplayName("Should update quantity when adding existing cart item")
    void testAddCartItem_ExistingItem_UpdateQuantity() {
        // Given
        CartItem existingCartItem = CartItem.builder()
                .customerId(CUSTOMER_ID)
                .productId(PRODUCT_ID)
                .quantity(3)
                .build();

        CartItem updatedCartItem = CartItem.builder()
                .customerId(CUSTOMER_ID)
                .productId(PRODUCT_ID)
                .quantity(5) // 3 existing + 2 new
                .build();

        CartItemGetVm updatedGetVm = new CartItemGetVm(CUSTOMER_ID, PRODUCT_ID, 5);

        when(productService.existsById(PRODUCT_ID)).thenReturn(true);
        when(cartItemRepository.findByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID))
                .thenReturn(Optional.of(existingCartItem));
        when(cartItemRepository.save(existingCartItem)).thenReturn(updatedCartItem);
        when(cartItemMapper.toGetVm(updatedCartItem)).thenReturn(updatedGetVm);

        // When
        CartItemGetVm result;
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);
            result = cartItemService.addCartItem(cartItemPostVm);
        }

        // Then
        assertNotNull(result);
        assertEquals(5, result.quantity());
        verify(cartItemRepository, times(1)).save(existingCartItem);
        assertEquals(5, existingCartItem.getQuantity());
    }

    @Test
    @DisplayName("Should throw NotFoundException when adding cart item with non-existent product")
    void testAddCartItem_ProductNotFound() {
        // Given
        when(productService.existsById(PRODUCT_ID)).thenReturn(false);

        // When & Then
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);
            
            assertThrows(NotFoundException.class, () -> cartItemService.addCartItem(cartItemPostVm));
        }

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InternalServerErrorException when pessimistic lock fails")
    void testAddCartItem_PessimisticLockFailure() {
        // Given
        when(productService.existsById(PRODUCT_ID)).thenReturn(true);
        when(cartItemRepository.findByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID))
                .thenThrow(new PessimisticLockingFailureException("Lock acquisition failed"));

        // When & Then
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);
            
            assertThrows(InternalServerErrorException.class, 
                    () -> cartItemService.addCartItem(cartItemPostVm));
        }
    }

    @Test
    @DisplayName("Should successfully update cart item quantity")
    void testUpdateCartItem_Success() {
        // Given
        CartItem updatedCartItem = CartItem.builder()
                .customerId(CUSTOMER_ID)
                .productId(PRODUCT_ID)
                .quantity(5)
                .build();

        CartItemGetVm updatedGetVm = new CartItemGetVm(CUSTOMER_ID, PRODUCT_ID, 5);

        when(productService.existsById(PRODUCT_ID)).thenReturn(true);
        when(cartItemMapper.toCartItem(CUSTOMER_ID, PRODUCT_ID, 5)).thenReturn(updatedCartItem);
        when(cartItemRepository.save(updatedCartItem)).thenReturn(updatedCartItem);
        when(cartItemMapper.toGetVm(updatedCartItem)).thenReturn(updatedGetVm);

        // When
        CartItemGetVm result;
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);
            result = cartItemService.updateCartItem(PRODUCT_ID, new CartItemPutVm(5));
        }

        // Then
        assertNotNull(result);
        assertEquals(5, result.quantity());
        verify(cartItemRepository, times(1)).save(updatedCartItem);
    }

    @Test
    @DisplayName("Should retrieve all cart items for current user")
    void testGetCartItems_Success() {
        // Given
        List<CartItem> cartItems = Arrays.asList(
                CartItem.builder().customerId(CUSTOMER_ID).productId(1L).quantity(2).build(),
                CartItem.builder().customerId(CUSTOMER_ID).productId(2L).quantity(3).build()
        );

        List<CartItemGetVm> cartItemGetVms = Arrays.asList(
                new CartItemGetVm(CUSTOMER_ID, 1L, 2),
                new CartItemGetVm(CUSTOMER_ID, 2L, 3)
        );

        when(cartItemRepository.findByCustomerIdOrderByCreatedOnDesc(CUSTOMER_ID)).thenReturn(cartItems);
        when(cartItemMapper.toGetVms(cartItems)).thenReturn(cartItemGetVms);

        // When
        List<CartItemGetVm> result;
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);
            result = cartItemService.getCartItems();
        }

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(cartItemRepository, times(1)).findByCustomerIdOrderByCreatedOnDesc(CUSTOMER_ID);
    }

    @Test
    @DisplayName("Should delete cart item when quantity to delete equals cart item quantity")
    void testDeleteOrAdjustCartItem_DeleteItem() {
        // Given
        CartItem existingItem = CartItem.builder()
                .customerId(CUSTOMER_ID)
                .productId(PRODUCT_ID)
                .quantity(2)
                .build();

        CartItemDeleteVm deleteVm = new CartItemDeleteVm(PRODUCT_ID, 2);
        List<CartItemDeleteVm> deleteVms = Collections.singletonList(deleteVm);

        when(cartItemRepository.findByCustomerIdAndProductIdIn(eq(CUSTOMER_ID), anyList()))
                .thenReturn(Collections.singletonList(existingItem));
        when(cartItemRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(cartItemMapper.toGetVms(anyList())).thenReturn(Collections.emptyList());

        // When
        List<CartItemGetVm> result;
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);
            result = cartItemService.deleteOrAdjustCartItem(deleteVms);
        }

        // Then
        verify(cartItemRepository, times(1)).deleteAll(anyList());
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should adjust cart item quantity when quantity to delete is less than cart item quantity")
    void testDeleteOrAdjustCartItem_AdjustQuantity() {
        // Given
        CartItem existingItem = CartItem.builder()
                .customerId(CUSTOMER_ID)
                .productId(PRODUCT_ID)
                .quantity(5)
                .build();

        CartItemDeleteVm deleteVm = new CartItemDeleteVm(PRODUCT_ID, 2);
        List<CartItemDeleteVm> deleteVms = Collections.singletonList(deleteVm);

        when(cartItemRepository.findByCustomerIdAndProductIdIn(eq(CUSTOMER_ID), anyList()))
                .thenReturn(Collections.singletonList(existingItem));
        when(cartItemRepository.saveAll(anyList())).thenReturn(Collections.singletonList(existingItem));
        when(cartItemMapper.toGetVms(anyList())).thenReturn(Collections.singletonList(cartItemGetVm));

        // When
        List<CartItemGetVm> result;
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);
            result = cartItemService.deleteOrAdjustCartItem(deleteVms);
        }

        // Then
        assertEquals(3, existingItem.getQuantity()); // 5 - 2 = 3
        verify(cartItemRepository, times(1)).saveAll(anyList());
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should throw BadRequestException when duplicate product IDs with different quantities")
    void testDeleteOrAdjustCartItem_DuplicateProductIds() {
        // Given
        List<CartItemDeleteVm> deleteVms = Arrays.asList(
                new CartItemDeleteVm(PRODUCT_ID, 2),
                new CartItemDeleteVm(PRODUCT_ID, 3) // Same product ID, different quantity
        );

        // When & Then
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);
            
            assertThrows(BadRequestException.class, 
                    () -> cartItemService.deleteOrAdjustCartItem(deleteVms));
        }

        verify(cartItemRepository, never()).deleteAll(anyList());
        verify(cartItemRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should successfully delete cart item by product ID")
    void testDeleteCartItem_Success() {
        // When
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);
            cartItemService.deleteCartItem(PRODUCT_ID);
        }

        // Then
        verify(cartItemRepository, times(1)).deleteByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID);
    }

    @Test
    @DisplayName("Should return empty list when no cart items exist for user")
    void testGetCartItems_EmptyCart() {
        // Given
        when(cartItemRepository.findByCustomerIdOrderByCreatedOnDesc(CUSTOMER_ID))
                .thenReturn(Collections.emptyList());
        when(cartItemMapper.toGetVms(Collections.emptyList())).thenReturn(Collections.emptyList());

        // When
        List<CartItemGetVm> result;
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);
            result = cartItemService.getCartItems();
        }

        // Then
        assertNotNull(result);
        assertEquals(0, result.size());
    }
}
