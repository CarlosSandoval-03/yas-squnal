package com.yas.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.ForbiddenException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.commonlibrary.utils.AuthenticationUtils;
import com.yas.order.mapper.CheckoutMapper;
import com.yas.order.model.Checkout;
import com.yas.order.model.CheckoutItem;
import com.yas.order.model.Order;
import com.yas.order.model.enumeration.CheckoutState;
import com.yas.order.repository.CheckoutRepository;
import com.yas.order.viewmodel.checkout.CheckoutItemPostVm;
import com.yas.order.viewmodel.checkout.CheckoutItemVm;
import com.yas.order.viewmodel.checkout.CheckoutPaymentMethodPutVm;
import com.yas.order.viewmodel.checkout.CheckoutPostVm;
import com.yas.order.viewmodel.checkout.CheckoutStatusPutVm;
import com.yas.order.viewmodel.checkout.CheckoutVm;
import com.yas.order.viewmodel.product.ProductCheckoutListVm;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for CheckoutService.
 * 
 * Test Doubles Used:
 *   - Mock: CheckoutRepository, OrderService, ProductService, CheckoutMapper
 *     Purpose: Records how it was used - verify method calls and arguments
 *     Example: verify(checkoutRepository, times(1)).save(any(Checkout.class))
 *     Can make assertions at the end of the test
 *   
 *   - Stub: AuthenticationUtils via MockedStatic
 *     Purpose: Returns predetermined responses with no additional logic
 *     Example: Always returns "customer-123" for user context
 *     No verification needed - just provides test data
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CheckoutService Unit Tests")
class CheckoutServiceTest {

    @Mock
    private CheckoutRepository checkoutRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private ProductService productService;

    @Mock
    private CheckoutMapper checkoutMapper;

    @InjectMocks
    private CheckoutService checkoutService;

    private static final String CUSTOMER_ID = "customer-123";
    private static final String CHECKOUT_ID = "checkout-456";
    private static final Long PRODUCT_ID_1 = 1L;
    private static final Long PRODUCT_ID_2 = 2L;
    private static final Long ORDER_ID = 100L;

    private CheckoutPostVm checkoutPostVm;
    private Checkout checkout;
    private CheckoutVm checkoutVm;
    private CheckoutItem checkoutItem1;
    private CheckoutItem checkoutItem2;
    private Map<Long, ProductCheckoutListVm> productMap;

    @BeforeEach
    void setUp() {
        // Setup checkout post view model
        CheckoutItemPostVm itemPostVm1 = new CheckoutItemPostVm(PRODUCT_ID_1, null, 2);
        CheckoutItemPostVm itemPostVm2 = new CheckoutItemPostVm(PRODUCT_ID_2, null, 1);
        
        checkoutPostVm = new CheckoutPostVm(
                "customer@test.com",
                "Test order",
                null,
                null,
                null,
                null,
                List.of(itemPostVm1, itemPostVm2)
        );

        // Setup checkout items
        checkoutItem1 = CheckoutItem.builder()
                .id(1L)
                .productId(PRODUCT_ID_1)
                .quantity(2)
                .productPrice(BigDecimal.ZERO)
                .build();

        checkoutItem2 = CheckoutItem.builder()
                .id(2L)
                .productId(PRODUCT_ID_2)
                .quantity(1)
                .productPrice(BigDecimal.ZERO)
                .build();

        // Setup checkout entity
        checkout = Checkout.builder()
                .id(CHECKOUT_ID)
                .customerId(CUSTOMER_ID)
                .email("customer@test.com")
                .checkoutState(CheckoutState.PENDING)
                .checkoutItems(new ArrayList<>())
                .build();
        checkout.setCreatedBy(CUSTOMER_ID);

        // Setup product map
        productMap = new HashMap<>();
        productMap.put(PRODUCT_ID_1, ProductCheckoutListVm.builder()
                .id(PRODUCT_ID_1)
                .name("Product 1")
                .price(50.0)
                .taxClassId(1L)
                .build());
        productMap.put(PRODUCT_ID_2, ProductCheckoutListVm.builder()
                .id(PRODUCT_ID_2)
                .name("Product 2")
                .price(30.0)
                .taxClassId(1L)
                .build());

        // Setup checkout view model
        checkoutVm = CheckoutVm.builder()
                .id(CHECKOUT_ID)
                .email("customer@test.com")
                .checkoutState(CheckoutState.PENDING)
                .build();
    }

    @Test
    @DisplayName("Should successfully create checkout with items and calculate total amount")
    void testCreateCheckout_Success() {
        // Given
        when(checkoutMapper.toModel(checkoutPostVm)).thenReturn(checkout);
        when(checkoutMapper.toModel(any(CheckoutItemPostVm.class))).thenAnswer(invocation -> {
            CheckoutItemPostVm item = invocation.getArgument(0);
            return CheckoutItem.builder()
                    .productId(item.productId())
                    .quantity(item.quantity())
                    .build();
        });
        when(productService.getProductInfomation(anySet(), anyInt(), anyInt())).thenReturn(productMap);
        
        CheckoutItem item1WithPrice = CheckoutItem.builder()
                .id(1L)
                .productId(PRODUCT_ID_1)
                .productName("Product 1")
                .quantity(2)
                .productPrice(new BigDecimal("50.0"))
                .build();
        
        CheckoutItem item2WithPrice = CheckoutItem.builder()
                .id(2L)
                .productId(PRODUCT_ID_2)
                .productName("Product 2")
                .quantity(1)
                .productPrice(new BigDecimal("30.0"))
                .build();
        
        Checkout savedCheckout = Checkout.builder()
                .id(checkout.getId())
                .customerId(checkout.getCustomerId())
                .email(checkout.getEmail())
                .checkoutState(checkout.getCheckoutState())
                .checkoutItems(List.of(item1WithPrice, item2WithPrice))
                .totalAmount(new BigDecimal("130.0"))
                .build();
        savedCheckout.setCreatedBy(CUSTOMER_ID);
        
        when(checkoutRepository.save(any(Checkout.class))).thenReturn(savedCheckout);
        when(checkoutMapper.toVm(any(Checkout.class))).thenReturn(checkoutVm);
        when(checkoutMapper.toVm(any(CheckoutItem.class))).thenReturn(
                new CheckoutItemVm(1L, PRODUCT_ID_1, "Product 1", null, 2, new BigDecimal("50.0"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, CHECKOUT_ID)
        );

        // When
        CheckoutVm result;
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);
            result = checkoutService.createCheckout(checkoutPostVm);
        }

        // Then
        assertNotNull(result);
        assertEquals(CHECKOUT_ID, result.id());
        verify(checkoutRepository, times(1)).save(any(Checkout.class));
        verify(productService, times(1)).getProductInfomation(anySet(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Should throw NotFoundException when product not found during checkout creation")
    void testCreateCheckout_ProductNotFound() {
        // Given
        when(checkoutMapper.toModel(checkoutPostVm)).thenReturn(checkout);
        when(checkoutMapper.toModel(any(CheckoutItemPostVm.class))).thenAnswer(invocation -> {
            CheckoutItemPostVm item = invocation.getArgument(0);
            return CheckoutItem.builder()
                    .productId(item.productId())
                    .quantity(item.quantity())
                    .build();
        });
        
        // Return empty map to simulate product not found
        when(productService.getProductInfomation(anySet(), anyInt(), anyInt())).thenReturn(new HashMap<>());

        // When & Then
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);
            
            assertThrows(NotFoundException.class, () -> checkoutService.createCheckout(checkoutPostVm));
        }
    }

    @Test
    @DisplayName("Should successfully retrieve checkout by ID for authorized user")
    void testGetCheckoutPendingStateWithItemsById_Success() {
        // Given
        checkout.setCheckoutItems(List.of(checkoutItem1, checkoutItem2));
        
        when(checkoutRepository.findByIdAndCheckoutState(CHECKOUT_ID, CheckoutState.PENDING))
                .thenReturn(Optional.of(checkout));
        when(checkoutMapper.toVm(checkout)).thenReturn(checkoutVm);
        when(checkoutMapper.toVm(any(CheckoutItem.class))).thenReturn(
                new CheckoutItemVm(1L, PRODUCT_ID_1, "Product 1", null, 2, new BigDecimal("50.0"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, CHECKOUT_ID)
        );

        // When
        CheckoutVm result;
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);
            result = checkoutService.getCheckoutPendingStateWithItemsById(CHECKOUT_ID);
        }

        // Then
        assertNotNull(result);
        assertEquals(CHECKOUT_ID, result.id());
        verify(checkoutRepository, times(1)).findByIdAndCheckoutState(CHECKOUT_ID, CheckoutState.PENDING);
    }

    @Test
    @DisplayName("Should throw NotFoundException when checkout not found by ID")
    void testGetCheckoutPendingStateWithItemsById_NotFound() {
        // Given
        when(checkoutRepository.findByIdAndCheckoutState(CHECKOUT_ID, CheckoutState.PENDING))
                .thenReturn(Optional.empty());

        // When & Then
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);
            
            assertThrows(NotFoundException.class, 
                    () -> checkoutService.getCheckoutPendingStateWithItemsById(CHECKOUT_ID));
        }
    }

    @Test
    @DisplayName("Should throw ForbiddenException when user tries to view another user's checkout")
    void testGetCheckoutPendingStateWithItemsById_Forbidden() {
        // Given
        checkout.setCreatedBy("different-user-id");
        when(checkoutRepository.findByIdAndCheckoutState(CHECKOUT_ID, CheckoutState.PENDING))
                .thenReturn(Optional.of(checkout));

        // When & Then
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);
            
            assertThrows(ForbiddenException.class, 
                    () -> checkoutService.getCheckoutPendingStateWithItemsById(CHECKOUT_ID));
        }
    }

    @Test
    @DisplayName("Should successfully update checkout status and return order ID")
    void testUpdateCheckoutStatus_Success() {
        // Given
        CheckoutStatusPutVm statusPutVm = new CheckoutStatusPutVm(CHECKOUT_ID, "COMPLETED");
        Order order = Order.builder().id(ORDER_ID).build();

        when(checkoutRepository.findById(CHECKOUT_ID)).thenReturn(Optional.of(checkout));
        when(checkoutRepository.save(checkout)).thenReturn(checkout);
        when(orderService.findOrderByCheckoutId(CHECKOUT_ID)).thenReturn(order);

        // When
        Long result;
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);
            result = checkoutService.updateCheckoutStatus(statusPutVm);
        }

        // Then
        assertNotNull(result);
        assertEquals(ORDER_ID, result);
        assertEquals(CheckoutState.COMPLETED, checkout.getCheckoutState());
        verify(checkoutRepository, times(1)).save(checkout);
        verify(orderService, times(1)).findOrderByCheckoutId(CHECKOUT_ID);
    }

    @Test
    @DisplayName("Should throw NotFoundException when updating non-existent checkout status")
    void testUpdateCheckoutStatus_CheckoutNotFound() {
        // Given
        CheckoutStatusPutVm statusPutVm = new CheckoutStatusPutVm(CHECKOUT_ID, "COMPLETED");
        when(checkoutRepository.findById(CHECKOUT_ID)).thenReturn(Optional.empty());

        // When & Then
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);
            
            assertThrows(NotFoundException.class, 
                    () -> checkoutService.updateCheckoutStatus(statusPutVm));
        }
    }

    @Test
    @DisplayName("Should throw ForbiddenException when unauthorized user updates checkout status")
    void testUpdateCheckoutStatus_Forbidden() {
        // Given
        checkout.setCreatedBy("different-user-id");
        CheckoutStatusPutVm statusPutVm = new CheckoutStatusPutVm(CHECKOUT_ID, "COMPLETED");
        when(checkoutRepository.findById(CHECKOUT_ID)).thenReturn(Optional.of(checkout));

        // When & Then
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);
            
            assertThrows(ForbiddenException.class, 
                    () -> checkoutService.updateCheckoutStatus(statusPutVm));
        }
    }

    @Test
    @DisplayName("Should successfully update checkout payment method")
    void testUpdateCheckoutPaymentMethod_Success() {
        // Given
        String paymentMethodId = "payment-method-123";
        CheckoutPaymentMethodPutVm paymentMethodPutVm = new CheckoutPaymentMethodPutVm(paymentMethodId);
        
        when(checkoutRepository.findById(CHECKOUT_ID)).thenReturn(Optional.of(checkout));
        when(checkoutRepository.save(checkout)).thenReturn(checkout);

        // When
        checkoutService.updateCheckoutPaymentMethod(CHECKOUT_ID, paymentMethodPutVm);

        // Then
        assertEquals(paymentMethodId, checkout.getPaymentMethodId());
        verify(checkoutRepository, times(1)).save(checkout);
    }

    @Test
    @DisplayName("Should throw NotFoundException when updating payment method for non-existent checkout")
    void testUpdateCheckoutPaymentMethod_CheckoutNotFound() {
        // Given
        CheckoutPaymentMethodPutVm paymentMethodPutVm = new CheckoutPaymentMethodPutVm("payment-method-123");
        when(checkoutRepository.findById(CHECKOUT_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, 
                () -> checkoutService.updateCheckoutPaymentMethod(CHECKOUT_ID, paymentMethodPutVm));
    }

    @Test
    @DisplayName("Should return checkout without items when checkout has empty items list")
    void testGetCheckoutPendingStateWithItemsById_EmptyItems() {
        // Given
        checkout.setCheckoutItems(new ArrayList<>());
        
        when(checkoutRepository.findByIdAndCheckoutState(CHECKOUT_ID, CheckoutState.PENDING))
                .thenReturn(Optional.of(checkout));
        when(checkoutMapper.toVm(checkout)).thenReturn(checkoutVm);

        // When
        CheckoutVm result;
        try (MockedStatic<AuthenticationUtils> mockedAuth = Mockito.mockStatic(AuthenticationUtils.class)) {
            mockedAuth.when(AuthenticationUtils::extractUserId).thenReturn(CUSTOMER_ID);
            result = checkoutService.getCheckoutPendingStateWithItemsById(CHECKOUT_ID);
        }

        // Then
        assertNotNull(result);
        assertEquals(CHECKOUT_ID, result.id());
    }
}
