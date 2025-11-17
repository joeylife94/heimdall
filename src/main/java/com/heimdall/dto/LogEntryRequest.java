package com.heimdall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * DTO for log entry requests
 * Used for gRPC to REST API conversion
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEntryRequest {
    
    @NotBlank(message = "Source is required")
    private String source;
    
    @NotBlank(message = "Service name is required")
    private String serviceName;
    
    private String environment;
    
    @NotBlank(message = "Severity is required")
    private String severity;
    
    @NotBlank(message = "Message is required")
    private String message;
    
    @NotNull(message = "Timestamp is required")
    private Instant timestamp;
    
    private Map<String, Object> metadata;
}
