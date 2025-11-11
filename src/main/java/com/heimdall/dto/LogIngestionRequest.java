package com.heimdall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogIngestionRequest {
    
    @NotBlank(message = "Source is required")
    private String source;
    
    private String serviceName;
    
    private String environment;
    
    @NotBlank(message = "Severity is required")
    private String severity;
    
    @NotBlank(message = "Log content is required")
    private String logContent;
    
    private Map<String, Object> metadata;
}
