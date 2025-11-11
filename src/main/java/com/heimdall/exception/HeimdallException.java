package com.heimdall.exception;

public class HeimdallException extends RuntimeException {
    
    public HeimdallException(String message) {
        super(message);
    }
    
    public HeimdallException(String message, Throwable cause) {
        super(message, cause);
    }
}
