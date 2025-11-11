package com.heimdall.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogSearchRequest {
    
    private String serviceName;
    
    private String environment;
    
    private String severity;
    
    private String from; // ISO8601 timestamp
    
    private String to; // ISO8601 timestamp
    
    private String keyword;
    
    @Builder.Default
    private Integer page = 0;
    
    @Builder.Default
    private Integer size = 20;
}
