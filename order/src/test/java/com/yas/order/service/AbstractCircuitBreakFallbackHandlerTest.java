package com.yas.order.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

/**
 * Unit tests for AbstractCircuitBreakFallbackHandler.
 * 
 * Test Doubles Used:
 *   - Test-Specific Subclass: ConcreteCircuitBreakFallbackHandler
 *     Purpose: Test abstract class behavior without mocking
 *     Pattern: Create concrete implementation in same package to test abstract methods
 */
@DisplayName("AbstractCircuitBreakFallbackHandler Unit Tests")
class AbstractCircuitBreakFallbackHandlerTest {

    private ConcreteCircuitBreakFallbackHandler handler;

    /**
     * Test-specific subclass to test abstract handler.
     * Must be in same package as AbstractCircuitBreakFallbackHandler (package-private access).
     */
    static class ConcreteCircuitBreakFallbackHandler extends AbstractCircuitBreakFallbackHandler {
        // Expose protected methods for testing
        public void testHandleBodilessFallback(Throwable throwable) throws Throwable {
            handleBodilessFallback(throwable);
        }

        public <T> T testHandleTypedFallback(Throwable throwable) throws Throwable {
            return handleTypedFallback(throwable);
        }
    }

    @BeforeEach
    void setUp() {
        handler = new ConcreteCircuitBreakFallbackHandler();
    }

    @Test
    @DisplayName("Should rethrow exception when handleBodilessFallback is called with HttpClientErrorException")
    void testHandleBodilessFallback_HttpClientError() {
        // Given
        Throwable error = HttpClientErrorException.BadRequest.create(
                HttpStatus.BAD_REQUEST, "Bad request", null, null, null);

        // When & Then
        assertThrows(Throwable.class, () -> handler.testHandleBodilessFallback(error));
    }

    @Test
    @DisplayName("Should rethrow exception when handleBodilessFallback is called with HttpServerErrorException")
    void testHandleBodilessFallback_HttpServerError() {
        // Given
        Throwable error = HttpServerErrorException.InternalServerError.create(
                HttpStatus.INTERNAL_SERVER_ERROR, "Server error", null, null, null);

        // When & Then
        assertThrows(Throwable.class, () -> handler.testHandleBodilessFallback(error));
    }

    @Test
    @DisplayName("Should rethrow exception when handleBodilessFallback is called with generic exception")
    void testHandleBodilessFallback_GenericException() {
        // Given
        Throwable error = new RuntimeException("Generic error");

        // When & Then
        assertThrows(Throwable.class, () -> handler.testHandleBodilessFallback(error));
    }

    @Test
    @DisplayName("Should rethrow exception when handleTypedFallback is called")
    void testHandleTypedFallback_ThrowsException() {
        // Given
        Throwable error = new RuntimeException("Service unavailable");

        // When & Then
        assertThrows(Throwable.class, () -> handler.testHandleTypedFallback(error));
    }

    @Test
    @DisplayName("Should handle NullPointerException in fallback")
    void testHandleBodilessFallback_NullPointerException() {
        // Given
        Throwable error = new NullPointerException("Null pointer");

        // When & Then
        assertThrows(Throwable.class, () -> handler.testHandleBodilessFallback(error));
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException in typed fallback")
    void testHandleTypedFallback_IllegalArgumentException() {
        // Given
        Throwable error = new IllegalArgumentException("Invalid argument");

        // When & Then
        assertThrows(Throwable.class, () -> handler.testHandleTypedFallback(error));
    }

    @Test
    @DisplayName("Should handle exception with null message")
    void testHandleBodilessFallback_NullMessage() {
        // Given
        Throwable error = new RuntimeException((String) null);

        // When & Then
        assertThrows(Throwable.class, () -> handler.testHandleBodilessFallback(error));
    }
}
