package com.heimdall.exception;

public class LogProcessingException extends HeimdallException {
    
    public LogProcessingException(String message) {
        super(message);
    }
    
    public LogProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
