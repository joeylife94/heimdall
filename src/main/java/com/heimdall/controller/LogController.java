package com.heimdall.controller;

import com.heimdall.dto.LogIngestionRequest;
import com.heimdall.dto.LogIngestionResponse;
import com.heimdall.entity.LogEntry;
import com.heimdall.kafka.event.LogIngestionEvent;
import com.heimdall.service.LogIngestionService;
import com.heimdall.util.DateTimeUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
@Slf4j
public class LogController {
    
    private final LogIngestionService logIngestionService;
    
    @PostMapping
    public ResponseEntity<LogIngestionResponse> ingestLog(
        @Valid @RequestBody LogIngestionRequest request
    ) {
        log.info("Received log ingestion request: source={}, severity={}", 
            request.getSource(), request.getSeverity());
        
        // 이벤트 생성
        LogIngestionEvent event = LogIngestionEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .timestamp(DateTimeUtil.now())
            .source(request.getSource())
            .serviceName(request.getServiceName())
            .environment(request.getEnvironment())
            .severity(request.getSeverity())
            .logContent(request.getLogContent())
            .metadata(request.getMetadata())
            .build();
        
        // 로그 처리
        LogEntry logEntry = logIngestionService.processLogIngestion(event);
        
        // 응답 생성
        LogIngestionResponse response = LogIngestionResponse.builder()
            .logId(logEntry.getId())
            .eventId(logEntry.getEventId())
            .timestamp(logEntry.getTimestamp())
            .status("ACCEPTED")
            .analysisRequested(!logEntry.getAnalysisResults().isEmpty())
            .build();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
