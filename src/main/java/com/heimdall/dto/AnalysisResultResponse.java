package com.heimdall.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResultResponse {
    
    private Long analysisId;
    
    private Long logId;
    
    private Long bifrostAnalysisId;
    
    private String summary;
    
    private String rootCause;
    
    private String recommendation;
    
    private String severity;
    
    private BigDecimal confidence;
    
    private String model;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime analyzedAt;
}
