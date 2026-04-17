package com.example.examauth.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.Map;
import java.util.HashMap;
import org.springframework.http.HttpStatus;
@ControllerAdvice
public class GlobalExceptionHandler {
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
