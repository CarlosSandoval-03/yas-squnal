package com.yas.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.payment.mapper.CreatePaymentProviderMapper;
import com.yas.payment.mapper.PaymentProviderMapper;
import com.yas.payment.mapper.UpdatePaymentProviderMapper;
import com.yas.payment.model.PaymentProvider;
import com.yas.payment.repository.PaymentProviderRepository;
import com.yas.payment.viewmodel.paymentprovider.CreatePaymentVm;
import com.yas.payment.viewmodel.paymentprovider.MediaVm;
import com.yas.payment.viewmodel.paymentprovider.PaymentProviderVm;
import com.yas.payment.viewmodel.paymentprovider.UpdatePaymentVm;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Unit tests for PaymentProviderService.
 * 
 * Test Doubles Used:
 *   - Mock: PaymentProviderRepository, MediaService, Mappers
 *     Purpose: Verify CRUD operations and media integration
 *     Example: verify(repository, times(1)).save(provider)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentProviderService Unit Tests")
class PaymentProviderServiceTest {

    @Mock
    private MediaService mediaService;

    @Mock
    private PaymentProviderRepository paymentProviderRepository;

    @Mock
    private PaymentProviderMapper paymentProviderMapper;

    @Mock
    private CreatePaymentProviderMapper createPaymentProviderMapper;

    @Mock
    private UpdatePaymentProviderMapper updatePaymentProviderMapper;

    @InjectMocks
    private PaymentProviderService paymentProviderService;

    private static final String PROVIDER_ID = "PAYPAL";
    private static final Long MEDIA_ID = 1L;

    private PaymentProvider paymentProvider;
    private CreatePaymentVm createPaymentVm;
    private UpdatePaymentVm updatePaymentVm;
    private PaymentProviderVm paymentProviderVm;

    @BeforeEach
    void setUp() {
        paymentProvider = PaymentProvider.builder()
                .id(PROVIDER_ID)
                .name("PayPal")
                .mediaId(MEDIA_ID)
                .enabled(true)
                .additionalSettings("{}")
                .build();

        createPaymentVm = new CreatePaymentVm();
        createPaymentVm.setId(PROVIDER_ID);
        createPaymentVm.setName("PayPal");
        createPaymentVm.setMediaId(MEDIA_ID);
        createPaymentVm.setEnabled(true);

        updatePaymentVm = new UpdatePaymentVm();
        updatePaymentVm.setId(PROVIDER_ID);
        updatePaymentVm.setName("PayPal Updated");
        updatePaymentVm.setMediaId(MEDIA_ID);
        updatePaymentVm.setEnabled(false);

        paymentProviderVm = new PaymentProviderVm(PROVIDER_ID, "PayPal", null, 0, MEDIA_ID, null);
    }

    @Test
    @DisplayName("Should successfully create payment provider")
    void testCreate_Success() {
        // Given
        when(createPaymentProviderMapper.toModel(createPaymentVm)).thenReturn(paymentProvider);
        when(paymentProviderRepository.save(paymentProvider)).thenReturn(paymentProvider);
        when(createPaymentProviderMapper.toVmResponse(paymentProvider)).thenReturn(paymentProviderVm);

        // When
        PaymentProviderVm result = paymentProviderService.create(createPaymentVm);

        // Then
        assertNotNull(result);
        assertEquals(PROVIDER_ID, result.getId());
        assertEquals("PayPal", result.getName());
        
        verify(paymentProviderRepository, times(1)).save(paymentProvider);
        verify(createPaymentProviderMapper, times(1)).toModel(createPaymentVm);
        verify(createPaymentProviderMapper, times(1)).toVmResponse(paymentProvider);
    }

    @Test
    @DisplayName("Should successfully update payment provider")
    void testUpdate_Success() {
        // Given
        when(paymentProviderRepository.findById(PROVIDER_ID)).thenReturn(Optional.of(paymentProvider));
        when(paymentProviderRepository.save(paymentProvider)).thenReturn(paymentProvider);
        when(updatePaymentProviderMapper.toVmResponse(paymentProvider)).thenReturn(paymentProviderVm);

        // When
        PaymentProviderVm result = paymentProviderService.update(updatePaymentVm);

        // Then
        assertNotNull(result);
        verify(updatePaymentProviderMapper, times(1)).partialUpdate(paymentProvider, updatePaymentVm);
        verify(paymentProviderRepository, times(1)).save(paymentProvider);
    }

    @Test
    @DisplayName("Should throw NotFoundException when updating non-existent provider")
    void testUpdate_ProviderNotFound() {
        // Given
        when(paymentProviderRepository.findById(PROVIDER_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            paymentProviderService.update(updatePaymentVm);
        });
    }

    @Test
    @DisplayName("Should get additional settings by provider ID")
    void testGetAdditionalSettings_Success() {
        // Given
        when(paymentProviderRepository.findById(PROVIDER_ID)).thenReturn(Optional.of(paymentProvider));

        // When
        String result = paymentProviderService.getAdditionalSettingsByPaymentProviderId(PROVIDER_ID);

        // Then
        assertEquals("{}", result);
        verify(paymentProviderRepository, times(1)).findById(PROVIDER_ID);
    }

    @Test
    @DisplayName("Should throw NotFoundException when getting settings for non-existent provider")
    void testGetAdditionalSettings_ProviderNotFound() {
        // Given
        when(paymentProviderRepository.findById(PROVIDER_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            paymentProviderService.getAdditionalSettingsByPaymentProviderId(PROVIDER_ID);
        });
    }

    @Test
    @DisplayName("Should get enabled payment providers with media")
    void testGetEnabledPaymentProviders_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<PaymentProvider> providers = List.of(paymentProvider);
        
        MediaVm mediaVm = MediaVm.builder()
                .id(MEDIA_ID)
                .url("http://example.com/icon.png")
                .build();
        
        Map<Long, MediaVm> mediaMap = Map.of(MEDIA_ID, mediaVm);
        
        when(paymentProviderRepository.findByEnabledTrue(pageable)).thenReturn(providers);
        when(mediaService.getMediaVmMap(providers)).thenReturn(mediaMap);
        when(paymentProviderMapper.toVm(paymentProvider)).thenReturn(paymentProviderVm);

        // When
        List<PaymentProviderVm> result = paymentProviderService.getEnabledPaymentProviders(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("http://example.com/icon.png", result.get(0).getIconUrl());
        
        verify(paymentProviderRepository, times(1)).findByEnabledTrue(pageable);
        verify(mediaService, times(1)).getMediaVmMap(providers);
    }

    @Test
    @DisplayName("Should return empty list when no enabled providers exist")
    void testGetEnabledPaymentProviders_EmptyList() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(paymentProviderRepository.findByEnabledTrue(pageable)).thenReturn(Collections.emptyList());

        // When
        List<PaymentProviderVm> result = paymentProviderService.getEnabledPaymentProviders(pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(mediaService, times(0)).getMediaVmMap(anyList());
    }

    @Test
    @DisplayName("Should handle missing media gracefully")
    void testGetEnabledPaymentProviders_MissingMedia() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<PaymentProvider> providers = List.of(paymentProvider);
        
        when(paymentProviderRepository.findByEnabledTrue(pageable)).thenReturn(providers);
        when(mediaService.getMediaVmMap(providers)).thenReturn(Collections.emptyMap());
        when(paymentProviderMapper.toVm(paymentProvider)).thenReturn(paymentProviderVm);

        // When
        List<PaymentProviderVm> result = paymentProviderService.getEnabledPaymentProviders(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        // Icon URL should be null or empty when media not found
        verify(paymentProviderRepository, times(1)).findByEnabledTrue(pageable);
    }
}
