package com.heimdall.service;

import com.heimdall.entity.AnalysisResult;
import com.heimdall.entity.LogEntry;
import com.heimdall.kafka.event.AnalysisResultEvent;
import com.heimdall.repository.AnalysisResultRepository;
import com.heimdall.repository.LogEntryRepository;
import com.heimdall.util.DateTimeUtil;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogProcessingService {
    
    private final LogEntryRepository logEntryRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final NotificationService notificationService;
    private final MeterRegistry meterRegistry;
    
    @Transactional
    public void processAnalysisResult(AnalysisResultEvent event) {
        log.info("Processing analysis result: requestId={}, logId={}", 
            event.getRequestId(), event.getLogId());
        
        // 로그 엔트리 조회
        LogEntry logEntry = logEntryRepository.findById(event.getLogId())
            .orElseThrow(() -> new RuntimeException("LogEntry not found: " + event.getLogId()));
        
        // 분석 결과 생성
        AnalysisResult analysisResult = new AnalysisResult();
        analysisResult.setLogEntry(logEntry);
        analysisResult.setBifrostAnalysisId(event.getBifrostAnalysisId());
        analysisResult.setRequestId(event.getRequestId());
        analysisResult.setCorrelationId(event.getCorrelationId());
        analysisResult.setSummary(event.getAnalysisResult().getSummary());
        analysisResult.setRootCause(event.getAnalysisResult().getRootCause());
        analysisResult.setRecommendation(event.getAnalysisResult().getRecommendation());
        analysisResult.setSeverity(event.getAnalysisResult().getSeverity());
        analysisResult.setConfidence(event.getAnalysisResult().getConfidence());
        analysisResult.setModel(event.getModel());
        analysisResult.setDurationSeconds(event.getDurationSeconds());
        analysisResult.setAnalyzedAt(event.getTimestamp());
        analysisResult.setCreatedAt(DateTimeUtil.now());
        
        // 데이터베이스 저장
        AnalysisResult savedResult = analysisResultRepository.save(analysisResult);
        
        // 메트릭 기록
        meterRegistry.counter("analysis.completed.total",
            "service", logEntry.getServiceName() != null ? logEntry.getServiceName() : "unknown",
            "severity", event.getAnalysisResult().getSeverity()
        ).increment();
        
        meterRegistry.timer("analysis.duration",
            "service", logEntry.getServiceName() != null ? logEntry.getServiceName() : "unknown"
        ).record(java.time.Duration.ofMillis(
            event.getDurationSeconds().multiply(java.math.BigDecimal.valueOf(1000)).longValue()
        ));
        
        // 알림 처리 (조건 충족 시)
        if (shouldSendNotification(savedResult)) {
            notificationService.sendAnalysisNotification(savedResult);
        }
        
        log.info("Analysis result processed: analysisId={}, logId={}", 
            savedResult.getId(), logEntry.getId());
    }
    
    private boolean shouldSendNotification(AnalysisResult analysisResult) {
        // HIGH 또는 CRITICAL 심각도인 경우 알림 발송
        return "HIGH".equals(analysisResult.getSeverity()) ||
               "CRITICAL".equals(analysisResult.getSeverity());
    }
}
