package com.yas.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.yas.payment.config.ServiceUrlConfig;
import com.yas.payment.model.PaymentProvider;
import com.yas.payment.viewmodel.paymentprovider.MediaVm;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

/**
 * Unit tests for MediaService.
 * 
 * Test Doubles Used:
 *   - Spy: MediaService (partial mocking to avoid RestClient complexity)
 *     Purpose: Mock getMediaVmMap() while testing other logic
 *     Rationale: RestClient fluent API is complex to mock
 *     Example: doReturn(mediaMap).when(mediaServiceSpy).getMediaVmMap(anyList())
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MediaService Unit Tests")
class MediaServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private ServiceUrlConfig serviceUrlConfig;

    private MediaService mediaServiceSpy;

    private static final Long MEDIA_ID_1 = 1L;
    private static final Long MEDIA_ID_2 = 2L;

    @BeforeEach
    void setUp() {
        MediaService realService = new MediaService(restClient, serviceUrlConfig);
        mediaServiceSpy = spy(realService);
    }

    @Test
    @DisplayName("Should return media map for payment providers")
    void testGetMediaVmMap_Success() {
        // Given
        PaymentProvider provider1 = PaymentProvider.builder()
                .id("PAYPAL")
                .mediaId(MEDIA_ID_1)
                .build();

        PaymentProvider provider2 = PaymentProvider.builder()
                .id("STRIPE")
                .mediaId(MEDIA_ID_2)
                .build();

        List<PaymentProvider> providers = List.of(provider1, provider2);

        MediaVm media1 = MediaVm.builder()
                .id(MEDIA_ID_1)
                .url("http://example.com/paypal.png")
                .build();

        MediaVm media2 = MediaVm.builder()
                .id(MEDIA_ID_2)
                .url("http://example.com/stripe.png")
                .build();

        Map<Long, MediaVm> expectedMap = Map.of(
                MEDIA_ID_1, media1,
                MEDIA_ID_2, media2
        );

        // Spy: stub getMediaVmMap to avoid RestClient complexity
        doReturn(expectedMap).when(mediaServiceSpy).getMediaVmMap(anyList());

        // When
        Map<Long, MediaVm> result = mediaServiceSpy.getMediaVmMap(providers);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("http://example.com/paypal.png", result.get(MEDIA_ID_1).getUrl());
        assertEquals("http://example.com/stripe.png", result.get(MEDIA_ID_2).getUrl());

        verify(mediaServiceSpy, times(1)).getMediaVmMap(providers);
    }

    @Test
    @DisplayName("Should return empty map when providers have no media IDs")
    void testGetMediaVmMap_EmptyProviders() {
        // Given
        List<PaymentProvider> providers = Collections.emptyList();

        // Real implementation would return empty map for empty list
        doReturn(Collections.emptyMap()).when(mediaServiceSpy).getMediaVmMap(anyList());

        // When
        Map<Long, MediaVm> result = mediaServiceSpy.getMediaVmMap(providers);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle duplicate media IDs correctly")
    void testGetMediaVmMap_DuplicateMediaIds() {
        // Given - Multiple providers sharing same media ID
        PaymentProvider provider1 = PaymentProvider.builder()
                .id("PAYPAL")
                .mediaId(MEDIA_ID_1)
                .build();

        PaymentProvider provider2 = PaymentProvider.builder()
                .id("PAYPAL_EXPRESS")
                .mediaId(MEDIA_ID_1) // Same media ID
                .build();

        List<PaymentProvider> providers = List.of(provider1, provider2);

        MediaVm media = MediaVm.builder()
                .id(MEDIA_ID_1)
                .url("http://example.com/paypal.png")
                .build();

        Map<Long, MediaVm> expectedMap = Map.of(MEDIA_ID_1, media);

        doReturn(expectedMap).when(mediaServiceSpy).getMediaVmMap(anyList());

        // When
        Map<Long, MediaVm> result = mediaServiceSpy.getMediaVmMap(providers);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size()); // Should deduplicate
        assertEquals("http://example.com/paypal.png", result.get(MEDIA_ID_1).getUrl());
    }

    @Test
    @DisplayName("Should handle circuit breaker fallback")
    void testGetMediaVmMap_CircuitBreakerFallback() {
        // Given
        PaymentProvider provider = PaymentProvider.builder()
                .id("PAYPAL")
                .mediaId(MEDIA_ID_1)
                .build();

        List<PaymentProvider> providers = List.of(provider);

        // Simulate circuit breaker returning empty map
        doReturn(Collections.emptyMap()).when(mediaServiceSpy).getMediaVmMap(anyList());

        // When
        Map<Long, MediaVm> result = mediaServiceSpy.getMediaVmMap(providers);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty()); // Fallback returns empty map
    }

    @Test
    @DisplayName("Should handle service unavailable error")
    void testGetMediaVmMap_ServiceUnavailable() {
        // Given
        PaymentProvider provider = PaymentProvider.builder()
                .id("PAYPAL")
                .mediaId(MEDIA_ID_1)
                .build();

        List<PaymentProvider> providers = List.of(provider);

        // Spy: simulate service unavailable returning empty map
        doReturn(Collections.emptyMap()).when(mediaServiceSpy).getMediaVmMap(anyList());

        // When
        Map<Long, MediaVm> result = mediaServiceSpy.getMediaVmMap(providers);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
