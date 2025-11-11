package com.heimdall.kafka.event;

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
public class AnalysisResultEvent {
    
    private String requestId;
    
    private String correlationId;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime timestamp;
    
    private Long logId;
    
    private AnalysisResultDetail analysisResult;
    
    private Long bifrostAnalysisId;
    
    private String model;
    
    private BigDecimal durationSeconds;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnalysisResultDetail {
        private String summary;
        private String rootCause;
        private String recommendation;
        private String severity;
        private BigDecimal confidence;
    }
}
