package com.yas.cart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for AbstractCircuitBreakFallbackHandler.
 * 
 * Test Doubles Used:
 *   - Concrete Test Implementation: TestCircuitBreakFallbackHandler
 *     Purpose: Test abstract class behavior through concrete implementation
 *     Type: This is a "Test-Specific Subclass" pattern
 *     Rationale: Cannot instantiate abstract class directly
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AbstractCircuitBreakFallbackHandler Unit Tests")
class AbstractCircuitBreakFallbackHandlerTest {

    private TestCircuitBreakFallbackHandler handler;

    /**
     * Concrete implementation for testing the abstract class.
     * This is a common pattern for testing abstract classes.
     */
    private static class TestCircuitBreakFallbackHandler extends AbstractCircuitBreakFallbackHandler {
        // Exposes protected methods for testing
        
        @Override
        public void handleBodilessFallback(Throwable throwable) throws Throwable {
            super.handleBodilessFallback(throwable);
        }

        @Override
        public <T> T handleTypedFallback(Throwable throwable) throws Throwable {
            return super.handleTypedFallback(throwable);
        }
    }

    @BeforeEach
    void setUp() {
        handler = new TestCircuitBreakFallbackHandler();
    }

    @Test
    @DisplayName("Should rethrow exception in handleBodilessFallback")
    void testHandleBodilessFallback_RethrowsException() {
        // Given
        RuntimeException originalException = new RuntimeException("Circuit breaker error");

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            handler.handleBodilessFallback(originalException);
        });

        assertEquals("Circuit breaker error", thrown.getMessage());
        assertEquals(originalException, thrown);
    }

    @Test
    @DisplayName("Should rethrow exception and return null in handleTypedFallback")
    void testHandleTypedFallback_RethrowsException() {
        // Given
        RuntimeException originalException = new RuntimeException("Service unavailable");

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            handler.handleTypedFallback(originalException);
        });

        assertEquals("Service unavailable", thrown.getMessage());
        assertEquals(originalException, thrown);
    }

    @Test
    @DisplayName("Should handle different exception types in handleBodilessFallback")
    void testHandleBodilessFallback_DifferentExceptionTypes() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

        // When & Then
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            handler.handleBodilessFallback(exception);
        });

        assertEquals("Invalid argument", thrown.getMessage());
    }

    @Test
    @DisplayName("Should handle different exception types in handleTypedFallback")
    void testHandleTypedFallback_DifferentExceptionTypes() {
        // Given
        IllegalStateException exception = new IllegalStateException("Invalid state");

        // When & Then
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
            String result = handler.handleTypedFallback(exception);
        });

        assertEquals("Invalid state", thrown.getMessage());
    }

    @Test
    @DisplayName("Should handle exception with null message")
    void testHandleBodilessFallback_NullMessage() {
        // Given
        RuntimeException exception = new RuntimeException((String) null);

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            handler.handleBodilessFallback(exception);
        });

        assertNull(thrown.getMessage());
    }

    @Test
    @DisplayName("Should handle exception with cause")
    void testHandleTypedFallback_WithCause() {
        // Given
        Throwable cause = new IllegalArgumentException("Root cause");
        RuntimeException exception = new RuntimeException("Wrapper exception", cause);

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            handler.handleTypedFallback(exception);
        });

        assertEquals("Wrapper exception", thrown.getMessage());
        assertEquals(cause, thrown.getCause());
    }

    @Test
    @DisplayName("Should preserve exception stack trace")
    void testHandleBodilessFallback_PreservesStackTrace() {
        // Given
        RuntimeException exception = new RuntimeException("Original error");
        StackTraceElement[] originalStackTrace = exception.getStackTrace();

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            handler.handleBodilessFallback(exception);
        });

        // Verify stack trace is preserved
        assertNotNull(thrown.getStackTrace());
        assertEquals(originalStackTrace.length, thrown.getStackTrace().length);
    }

    private void assertNotNull(StackTraceElement[] stackTrace) {
        if (stackTrace == null) {
            throw new AssertionError("Stack trace should not be null");
        }
    }
}
