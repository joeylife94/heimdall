package com.heimdall.kafka.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisRequestEvent {
    
    private String requestId;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime timestamp;
    
    private Long logId;
    
    private String logContent;
    
    private String serviceName;
    
    private String environment;
    
    private String analysisType;
    
    private String priority;
    
    private String callbackTopic;
    
    private String correlationId;
}
