package com.heimdall.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heimdall.kafka.event.AnalysisRequestEvent;
import com.heimdall.kafka.event.LogIngestionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${kafka.topics.logs-ingestion}")
    private String logsIngestionTopic;
    
    @Value("${kafka.topics.analysis-request}")
    private String analysisRequestTopic;
    
    @Value("${kafka.topics.notification-alert}")
    private String notificationAlertTopic;
    
    public void sendLogIngestion(LogIngestionEvent event) {
        try {
            String key = event.getEventId();
            String value = objectMapper.writeValueAsString(event);
            
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(logsIngestionTopic, key, value);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Log ingestion message sent successfully: eventId={}, offset={}",
                        event.getEventId(), result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send log ingestion message: eventId={}", 
                        event.getEventId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error serializing log ingestion event", e);
        }
    }
    
    public void sendAnalysisRequest(AnalysisRequestEvent event) {
        try {
            String key = event.getLogId().toString();
            String value = objectMapper.writeValueAsString(event);
            
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(analysisRequestTopic, key, value);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Analysis request sent successfully: requestId={}, logId={}, offset={}",
                        event.getRequestId(), event.getLogId(), 
                        result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send analysis request: requestId={}, logId={}", 
                        event.getRequestId(), event.getLogId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error serializing analysis request event", e);
        }
    }
    
    public void sendNotificationAlert(String key, String message) {
        CompletableFuture<SendResult<String, String>> future = 
            kafkaTemplate.send(notificationAlertTopic, key, message);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Notification alert sent: key={}, offset={}",
                    key, result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send notification alert: key={}", key, ex);
            }
        });
    }
}
