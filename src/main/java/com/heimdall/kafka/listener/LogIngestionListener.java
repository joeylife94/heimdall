package com.heimdall.kafka.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heimdall.kafka.event.LogIngestionEvent;
import com.heimdall.service.LogIngestionService;
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
public class LogIngestionListener {
    
    private final LogIngestionService logIngestionService;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(
        topics = "${kafka.topics.logs-ingestion}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleLogIngestion(
        @Payload String message,
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment acknowledgment
    ) {
        try {
            log.debug("Received log ingestion message: key={}, partition={}, offset={}", 
                key, partition, offset);
            
            LogIngestionEvent event = objectMapper.readValue(message, LogIngestionEvent.class);
            logIngestionService.processLogIngestion(event);
            
            // 수동 커밋
            acknowledgment.acknowledge();
            
            log.info("Successfully processed log ingestion: eventId={}", event.getEventId());
        } catch (Exception e) {
            log.error("Error processing log ingestion message: key={}, offset={}", 
                key, offset, e);
            // 에러 발생 시에도 acknowledge하여 재처리 방지 (DLQ로 보내기)
            acknowledgment.acknowledge();
        }
    }
}
