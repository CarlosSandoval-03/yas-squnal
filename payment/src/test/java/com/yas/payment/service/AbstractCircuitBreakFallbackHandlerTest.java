package com.yas.payment.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

/**
 * Unit tests for AbstractCircuitBreakFallbackHandler.
 * 
 * Test Doubles Used:
 *   - Test-Specific Subclass: ConcreteCircuitBreakFallbackHandler
 *     Purpose: Test abstract class methods
 *     Rationale: Cannot instantiate abstract class directly
 *     Example: new ConcreteCircuitBreakFallbackHandler()
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AbstractCircuitBreakFallbackHandler Unit Tests")
class AbstractCircuitBreakFallbackHandlerTest {

    /**
     * Test-Specific Subclass to test abstract class.
     */
    private static class ConcreteCircuitBreakFallbackHandler extends AbstractCircuitBreakFallbackHandler {
    }

    private final ConcreteCircuitBreakFallbackHandler handler = new ConcreteCircuitBreakFallbackHandler();

    @Test
    @DisplayName("Should throw exception for client error (4xx) in bodiless fallback")
    void testHandleBodilessFallback_ClientError() {
        // Given
        Throwable exception = new HttpClientErrorException(HttpStatus.NOT_FOUND, "Resource not found");

        // When & Then
        assertThrows(Throwable.class, () -> handler.handleBodilessFallback(exception));
    }

    @Test
    @DisplayName("Should throw exception for server error (5xx) in bodiless fallback")
    void testHandleBodilessFallback_ServerError() {
        // Given
        Throwable exception = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server error");

        // When & Then
        assertThrows(Throwable.class, () -> handler.handleBodilessFallback(exception));
    }

    @Test
    @DisplayName("Should throw exception for unauthorized error in bodiless fallback")
    void testHandleBodilessFallback_Unauthorized() {
        // Given
        Throwable exception = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);

        // When & Then
        assertThrows(Throwable.class, () -> handler.handleBodilessFallback(exception));
    }

    @Test
    @DisplayName("Should throw exception for non-HTTP errors in bodiless fallback")
    void testHandleBodilessFallback_NonHttpError() {
        // Given
        Throwable exception = new RuntimeException("Generic error");

        // When & Then
        assertThrows(Throwable.class, () -> handler.handleBodilessFallback(exception));
    }

    @Test
    @DisplayName("Should throw exception for typed fallback with client error")
    void testHandleTypedFallback_ClientError() {
        // Given
        Throwable exception = new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad request");

        // When & Then
        assertThrows(Throwable.class, () -> handler.handleTypedFallback(exception));
    }

    @Test
    @DisplayName("Should throw exception for typed fallback with server error")
    void testHandleTypedFallback_ServerError() {
        // Given
        Throwable exception = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "Service unavailable");

        // When & Then
        assertThrows(Throwable.class, () -> handler.handleTypedFallback(exception));
    }

    @Test
    @DisplayName("Should throw exception for typed fallback with non-HTTP errors")
    void testHandleTypedFallback_NonHttpError() {
        // Given
        Throwable exception = new NullPointerException("Null pointer");

        // When & Then
        assertThrows(Throwable.class, () -> handler.handleTypedFallback(exception));
    }
}
