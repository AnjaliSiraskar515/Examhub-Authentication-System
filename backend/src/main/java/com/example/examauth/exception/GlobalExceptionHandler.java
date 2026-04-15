package com.example.examauth.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Spring Security's @PreAuthorize throws AccessDeniedException when the
     * authenticated principal does not have the required role.
     * Must be handled BEFORE the Throwable catch-all, otherwise it gets
     * wrapped as HTTP 500 instead of HTTP 403.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(Map.of(
                "error", "Access Denied: You do not have permission to perform this action.",
                "exception_class", ex.getClass().getName()));
    }

    /**
     * Handles unauthenticated access (missing / invalid token at controller level).
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(401).body(Map.of(
                "error", "Unauthorized: " + ex.getMessage(),
                "exception_class", ex.getClass().getName()));
    }

    /**
     * Catch-all for any other unexpected exceptions — unchanged behaviour.
     */
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<?> handleAll(Throwable t) {
        t.printStackTrace();
        return ResponseEntity.status(500).body(Map.of(
                "error", "Global Catch: " + t.getMessage(),
                "exception_class", t.getClass().getName()));
    }
}
