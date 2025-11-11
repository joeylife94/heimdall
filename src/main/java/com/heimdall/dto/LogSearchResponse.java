package com.heimdall.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogSearchResponse {
    
    private List<LogEntryDto> content;
    
    private PageInfo page;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LogEntryDto {
        private Long logId;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime timestamp;
        
        private String serviceName;
        
        private String environment;
        
        private String severity;
        
        private String logContent;
        
        private Boolean hasAnalysis;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PageInfo {
        private Integer size;
        private Long totalElements;
        private Integer totalPages;
        private Integer number;
    }
}
