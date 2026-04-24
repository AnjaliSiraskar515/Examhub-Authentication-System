package com.example.examauth.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.Map;
import java.util.HashMap;
import org.springframework.http.HttpStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Spring Security's @PreAuthorize throws AccessDeniedException when the
     * authenticated principal does not have the required role.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(Map.of(
                "error", "Access Denied: You do not have permission to perform this action.",
                "exception_class", ex.getClass().getName()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(401).body(Map.of(
                "error", "Unauthorized: " + ex.getMessage(),
                "exception_class", ex.getClass().getName()));
    }

    @ExceptionHandler(EligibilityException.class)
    public ResponseEntity<Map<String, String>> handleEligibilityException(EligibilityException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "BLOCKED");
        response.put("reason", ex.getReason());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<?> handleAll(Throwable t) {
        t.printStackTrace();
        return ResponseEntity.status(500).body(Map.of(
                "error", "Global Catch: " + t.getMessage(),
                "exception_class", t.getClass().getName()));
    }
}
