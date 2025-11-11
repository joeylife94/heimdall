package com.heimdall.kafka.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heimdall.kafka.event.AnalysisResultEvent;
import com.heimdall.service.LogProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalysisResultListener {
    
    private final LogProcessingService logProcessingService;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(
        topics = "${kafka.topics.analysis-result}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAnalysisResult(
        @Payload String message,
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment acknowledgment
    ) {
        try {
            log.debug("Received analysis result message: key={}, partition={}, offset={}", 
                key, partition, offset);
            
            AnalysisResultEvent event = objectMapper.readValue(message, AnalysisResultEvent.class);
            logProcessingService.processAnalysisResult(event);
            
            // 수동 커밋
            acknowledgment.acknowledge();
            
            log.info("Successfully processed analysis result: requestId={}, logId={}", 
                event.getRequestId(), event.getLogId());
        } catch (Exception e) {
            log.error("Error processing analysis result message: key={}, offset={}", 
                key, offset, e);
            // 에러 발생 시에도 acknowledge
            acknowledgment.acknowledge();
        }
    }
}
