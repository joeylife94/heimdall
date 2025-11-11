package com.heimdall.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatisticsResponse {
    
    private PeriodInfo period;
    
    private List<StatisticEntry> statistics;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PeriodInfo {
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime from;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime to;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatisticEntry {
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime timestamp;
        
        private String serviceName;
        
        private String environment;
        
        private Integer totalLogs;
        
        private Map<String, Integer> bySeverity;
        
        private Integer avgSizeBytes;
    }
}
