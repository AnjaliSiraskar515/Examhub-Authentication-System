package com.example.examauth.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<?> handleAll(Throwable t) {
        t.printStackTrace();
        return ResponseEntity.status(500).body(Map.of(
                "error", "Global Catch: " + t.getMessage(),
                "exception_class", t.getClass().getName()));
    }
}
