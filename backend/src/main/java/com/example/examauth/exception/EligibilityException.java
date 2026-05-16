package com.example.examauth.exception;

public class EligibilityException extends RuntimeException {
    
    private final String reason;

    public EligibilityException(String reason) {
        super(reason);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
