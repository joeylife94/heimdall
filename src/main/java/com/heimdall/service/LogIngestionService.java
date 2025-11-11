package com.heimdall.service;

import com.heimdall.entity.LogEntry;
import com.heimdall.kafka.event.AnalysisRequestEvent;
import com.heimdall.kafka.event.LogIngestionEvent;
import com.heimdall.kafka.producer.KafkaProducerService;
import com.heimdall.repository.LogEntryRepository;
import com.heimdall.util.DateTimeUtil;
import com.heimdall.util.HashUtil;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogIngestionService {
    
    private final LogEntryRepository logEntryRepository;
    private final KafkaProducerService kafkaProducerService;
    private final MeterRegistry meterRegistry;
    
    @Value("${heimdall.analysis.enabled:true}")
    private boolean analysisEnabled;
    
    @Value("${heimdall.analysis.auto-request:true}")
    private boolean autoRequestAnalysis;
    
    @Transactional
    public LogEntry processLogIngestion(LogIngestionEvent event) {
        log.info("Processing log ingestion: eventId={}, source={}, severity={}", 
            event.getEventId(), event.getSource(), event.getSeverity());
        
        // 로그 엔트리 생성
        LogEntry logEntry = new LogEntry();
        logEntry.setEventId(event.getEventId());
        logEntry.setTimestamp(event.getTimestamp());
        logEntry.setSource(event.getSource());
        logEntry.setServiceName(event.getServiceName());
        logEntry.setEnvironment(event.getEnvironment());
        logEntry.setSeverity(LogEntry.SeverityLevel.valueOf(event.getSeverity()));
        logEntry.setLogContent(event.getLogContent());
        logEntry.setLogHash(HashUtil.sha256(event.getLogContent()));
        logEntry.setMetadata(event.getMetadata());
        logEntry.setCreatedAt(DateTimeUtil.now());
        
        // 데이터베이스 저장
        LogEntry savedEntry = logEntryRepository.save(logEntry);
        
        // 메트릭 기록
        meterRegistry.counter("logs.ingested.total",
            "service", event.getServiceName() != null ? event.getServiceName() : "unknown",
            "severity", event.getSeverity()
        ).increment();
        
        // AI 분석 요청 (조건 충족 시)
        if (shouldRequestAnalysis(savedEntry)) {
            requestAnalysis(savedEntry);
        }
        
        log.info("Log ingestion completed: logId={}, eventId={}", 
            savedEntry.getId(), savedEntry.getEventId());
        
        return savedEntry;
    }
    
    private boolean shouldRequestAnalysis(LogEntry logEntry) {
        if (!analysisEnabled || !autoRequestAnalysis) {
            return false;
        }
        
        // ERROR 이상의 심각도만 분석 요청
        return logEntry.getSeverity() == LogEntry.SeverityLevel.ERROR ||
               logEntry.getSeverity() == LogEntry.SeverityLevel.FATAL;
    }
    
    private void requestAnalysis(LogEntry logEntry) {
        try {
            AnalysisRequestEvent analysisRequest = AnalysisRequestEvent.builder()
                .requestId(UUID.randomUUID().toString())
                .timestamp(DateTimeUtil.now())
                .logId(logEntry.getId())
                .logContent(logEntry.getLogContent())
                .serviceName(logEntry.getServiceName())
                .environment(logEntry.getEnvironment())
                .analysisType("error")
                .priority(determinePriority(logEntry))
                .callbackTopic("analysis.result")
                .correlationId(logEntry.getEventId())
                .build();
            
            kafkaProducerService.sendAnalysisRequest(analysisRequest);
            
            meterRegistry.counter("analysis.requested.total",
                "service", logEntry.getServiceName() != null ? logEntry.getServiceName() : "unknown",
                "severity", logEntry.getSeverity().name()
            ).increment();
            
            log.info("Analysis requested for logId={}", logEntry.getId());
        } catch (Exception e) {
            log.error("Failed to request analysis for logId={}", logEntry.getId(), e);
        }
    }
    
    private String determinePriority(LogEntry logEntry) {
        return switch (logEntry.getSeverity()) {
            case FATAL -> "CRITICAL";
            case ERROR -> "HIGH";
            case WARN -> "MEDIUM";
            default -> "LOW";
        };
    }
}
