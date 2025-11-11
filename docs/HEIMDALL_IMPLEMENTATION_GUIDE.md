# 🛡️ Heimdall Implementation Guide

> **개발 시작 가이드** - Bifrost 연동 마이크로서비스 구현 상세 문서

---

## 📋 목차

1. [프로젝트 초기 설정](#-프로젝트-초기-설정)
2. [핵심 구현 가이드](#-핵심-구현-가이드)
3. [Bifrost 연동 구현](#-bifrost-연동-구현)
4. [테스트 전략](#-테스트-전략)
5. [배포 및 운영](#-배포-및-운영)
6. [트러블슈팅](#-트러블슈팅)

---

## 🚀 프로젝트 초기 설정

### 1. 프로젝트 생성

#### Spring Initializr 설정

```
Project: Gradle - Groovy
Language: Java
Spring Boot: 3.2.x
Project Metadata:
  - Group: com.bifrost
  - Artifact: heimdall
  - Name: Heimdall
  - Package: com.bifrost.heimdall
  - Java: 17

Dependencies:
  - Spring Web
  - Spring Data JPA
  - Spring for Apache Kafka
  - Spring Security
  - Spring Boot Actuator
  - PostgreSQL Driver
  - Lombok
  - Validation
  - Micrometer (Prometheus)
```

#### build.gradle 예시

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.bifrost'
version = '1.0.0'
sourceCompatibility = '17'

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    
    // Kafka
    implementation 'org.springframework.kafka:spring-kafka'
    
    // Database
    implementation 'org.postgresql:postgresql'
    implementation 'org.flywaydb:flyway-core'
    
    // Elasticsearch (선택적)
    implementation 'org.springframework.boot:spring-boot-starter-data-elasticsearch'
    
    // Monitoring
    implementation 'io.micrometer:micrometer-registry-prometheus'
    
    // Utilities
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'
    
    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.kafka:spring-kafka-test'
    testImplementation 'com.h2database:h2'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

### 2. 디렉토리 구조 생성

```bash
mkdir -p src/main/java/com/bifrost/heimdall/{config,controller,service,repository,entity,dto,kafka,exception,util}
mkdir -p src/main/resources/{db/migration,static,templates}
mkdir -p src/test/java/com/bifrost/heimdall
```

### 3. application.yml 설정

```yaml
# application.yml
spring:
  application:
    name: heimdall
  
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/heimdall}
    username: ${DATABASE_USERNAME:heimdall}
    password: ${DATABASE_PASSWORD:heimdall123}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        jdbc:
          batch_size: 20
  
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: heimdall-consumer-group
      auto-offset-reset: earliest
      enable-auto-commit: false
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      properties:
        max.poll.records: 100
        session.timeout.ms: 30000
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all
      retries: 3
      properties:
        linger.ms: 10
        batch.size: 16384
  
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration

server:
  port: 8080
  compression:
    enabled: true
  error:
    include-message: always

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  level:
    root: INFO
    com.bifrost.heimdall: DEBUG
    org.springframework.kafka: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# 커스텀 설정
heimdall:
  kafka:
    topics:
      log-ingestion: logs.ingestion
      log-processing: logs.processing
      analysis-request: analysis.request
      analysis-result: analysis.result
      notification: notification.alert
      dlq: dlq.failed
  
  analysis:
    auto-request: true
    severity-threshold: ERROR
    batch-size: 50
  
  security:
    api-key-header: X-API-Key
    rate-limit:
      enabled: true
      requests-per-minute: 100
```

### 4. Flyway Migration 스크립트

```sql
-- src/main/resources/db/migration/V1__Create_initial_schema.sql

-- log_entries 테이블
CREATE TABLE log_entries (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(36) UNIQUE NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    source VARCHAR(100) NOT NULL,
    service_name VARCHAR(100),
    environment VARCHAR(50),
    severity VARCHAR(20) NOT NULL,
    log_content TEXT NOT NULL,
    log_hash VARCHAR(64) NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_log_entries_timestamp ON log_entries (timestamp DESC);
CREATE INDEX idx_log_entries_service_env ON log_entries (service_name, environment);
CREATE INDEX idx_log_entries_severity ON log_entries (severity);
CREATE INDEX idx_log_entries_hash ON log_entries (log_hash);

-- analysis_results 테이블
CREATE TABLE analysis_results (
    id BIGSERIAL PRIMARY KEY,
    log_id BIGINT NOT NULL REFERENCES log_entries(id) ON DELETE CASCADE,
    bifrost_analysis_id BIGINT,
    request_id VARCHAR(36) UNIQUE NOT NULL,
    correlation_id VARCHAR(36),
    summary TEXT,
    root_cause TEXT,
    recommendation TEXT,
    severity VARCHAR(20),
    confidence DECIMAL(3,2),
    model VARCHAR(100),
    duration_seconds DECIMAL(10,2),
    analyzed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_analysis_results_log_id ON analysis_results (log_id);
CREATE INDEX idx_analysis_results_analyzed_at ON analysis_results (analyzed_at DESC);

-- log_statistics 테이블
CREATE TABLE log_statistics (
    id BIGSERIAL PRIMARY KEY,
    date DATE NOT NULL,
    hour SMALLINT NOT NULL,
    service_name VARCHAR(100),
    environment VARCHAR(50),
    severity VARCHAR(20),
    count INTEGER NOT NULL DEFAULT 0,
    avg_size_bytes INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (date, hour, service_name, environment, severity)
);

CREATE INDEX idx_log_statistics_date_hour ON log_statistics (date, hour);

-- notifications 테이블
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    log_id BIGINT REFERENCES log_entries(id),
    analysis_id BIGINT REFERENCES analysis_results(id),
    type VARCHAR(50) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    recipient VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_sent_at ON notifications (sent_at DESC);
CREATE INDEX idx_notifications_status ON notifications (status);
```

---

## 💻 핵심 구현 가이드

### 1. Entity 구현

```java
package com.bifrost.heimdall.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

@Entity
@Table(name = "log_entries")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "event_id", unique = true, nullable = false, length = 36)
    private String eventId;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(nullable = false, length = 100)
    private String source;
    
    @Column(name = "service_name", length = 100)
    private String serviceName;
    
    @Column(length = 50)
    private String environment;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeverityLevel severity;
    
    @Column(name = "log_content", columnDefinition = "TEXT", nullable = false)
    private String logContent;
    
    @Column(name = "log_hash", nullable = false, length = 64)
    private String logHash;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "logEntry", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AnalysisResult> analysisResults;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

```java
package com.bifrost.heimdall.entity;

public enum SeverityLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    FATAL
}
```

```java
package com.bifrost.heimdall.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_results")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id", nullable = false)
    private LogEntry logEntry;
    
    @Column(name = "bifrost_analysis_id")
    private Long bifrostAnalysisId;
    
    @Column(name = "request_id", unique = true, nullable = false, length = 36)
    private String requestId;
    
    @Column(name = "correlation_id", length = 36)
    private String correlationId;
    
    @Column(columnDefinition = "TEXT")
    private String summary;
    
    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;
    
    @Column(columnDefinition = "TEXT")
    private String recommendation;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SeverityLevel severity;
    
    @Column(precision = 3, scale = 2)
    private BigDecimal confidence;
    
    @Column(length = 100)
    private String model;
    
    @Column(name = "duration_seconds", precision = 10, scale = 2)
    private BigDecimal durationSeconds;
    
    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

### 2. Repository 구현

```java
package com.bifrost.heimdall.repository;

import com.bifrost.heimdall.entity.LogEntry;
import com.bifrost.heimdall.entity.SeverityLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
    
    Optional<LogEntry> findByEventId(String eventId);
    
    Page<LogEntry> findByServiceNameAndEnvironment(
        String serviceName, 
        String environment, 
        Pageable pageable
    );
    
    Page<LogEntry> findBySeverity(
        SeverityLevel severity, 
        Pageable pageable
    );
    
    @Query("SELECT l FROM LogEntry l WHERE " +
           "l.timestamp BETWEEN :from AND :to " +
           "AND (:serviceName IS NULL OR l.serviceName = :serviceName) " +
           "AND (:environment IS NULL OR l.environment = :environment) " +
           "AND (:severity IS NULL OR l.severity = :severity)")
    Page<LogEntry> searchLogs(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        @Param("serviceName") String serviceName,
        @Param("environment") String environment,
        @Param("severity") SeverityLevel severity,
        Pageable pageable
    );
    
    @Query("SELECT COUNT(l) FROM LogEntry l WHERE " +
           "l.timestamp BETWEEN :from AND :to " +
           "AND l.serviceName = :serviceName")
    Long countByServiceNameAndTimestamp(
        @Param("serviceName") String serviceName,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );
}
```

### 3. DTO 구현

```java
package com.bifrost.heimdall.dto;

import com.bifrost.heimdall.entity.SeverityLevel;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogIngestionRequest {
    
    @NotBlank(message = "Source is required")
    @Size(max = 100, message = "Source must be less than 100 characters")
    private String source;
    
    @Size(max = 100, message = "Service name must be less than 100 characters")
    private String serviceName;
    
    @Size(max = 50, message = "Environment must be less than 50 characters")
    private String environment;
    
    @NotNull(message = "Severity is required")
    private SeverityLevel severity;
    
    @NotBlank(message = "Log content is required")
    @Size(max = 1000000, message = "Log content must be less than 1MB")
    private String logContent;
    
    private Map<String, Object> metadata;
}
```

```java
package com.bifrost.heimdall.dto;

import com.bifrost.heimdall.entity.SeverityLevel;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogIngestionResponse {
    
    private Long logId;
    private String eventId;
    private LocalDateTime timestamp;
    private String status;
    private Boolean analysisRequested;
}
```

```java
package com.bifrost.heimdall.dto;

import com.bifrost.heimdall.entity.SeverityLevel;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogSearchRequest {
    
    private String serviceName;
    private String environment;
    private SeverityLevel severity;
    private LocalDateTime from;
    private LocalDateTime to;
    private String keyword;
    private Integer page = 0;
    private Integer size = 20;
}
```

### 4. Service 구현

```java
package com.bifrost.heimdall.service;

import com.bifrost.heimdall.dto.*;
import com.bifrost.heimdall.entity.*;
import com.bifrost.heimdall.repository.*;
import com.bifrost.heimdall.kafka.producer.KafkaProducerService;
import com.bifrost.heimdall.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogIngestionService {
    
    private final LogEntryRepository logEntryRepository;
    private final KafkaProducerService kafkaProducerService;
    
    @Value("${heimdall.analysis.auto-request:true}")
    private boolean autoRequestAnalysis;
    
    @Value("${heimdall.analysis.severity-threshold:ERROR}")
    private String severityThreshold;
    
    @Transactional
    public LogIngestionResponse ingestLog(LogIngestionRequest request) {
        log.info("Ingesting log from source: {}", request.getSource());
        
        // 1. 로그 엔트리 생성
        String eventId = UUID.randomUUID().toString();
        String logHash = HashUtil.sha256(request.getLogContent());
        
        LogEntry logEntry = LogEntry.builder()
            .eventId(eventId)
            .timestamp(LocalDateTime.now())
            .source(request.getSource())
            .serviceName(request.getServiceName())
            .environment(request.getEnvironment())
            .severity(request.getSeverity())
            .logContent(request.getLogContent())
            .logHash(logHash)
            .metadata(request.getMetadata())
            .build();
        
        // 2. DB 저장
        logEntry = logEntryRepository.save(logEntry);
        
        // 3. Kafka 이벤트 발행 (logs.ingestion)
        kafkaProducerService.sendLogIngestionEvent(logEntry);
        
        // 4. 조건 충족 시 AI 분석 요청
        boolean analysisRequested = false;
        if (shouldRequestAnalysis(logEntry)) {
            kafkaProducerService.sendAnalysisRequest(logEntry);
            analysisRequested = true;
            log.info("Analysis requested for log ID: {}", logEntry.getId());
        }
        
        // 5. 응답 반환
        return LogIngestionResponse.builder()
            .logId(logEntry.getId())
            .eventId(eventId)
            .timestamp(logEntry.getTimestamp())
            .status("ACCEPTED")
            .analysisRequested(analysisRequested)
            .build();
    }
    
    @Transactional(readOnly = true)
    public Page<LogEntry> searchLogs(LogSearchRequest request) {
        Pageable pageable = PageRequest.of(
            request.getPage(), 
            request.getSize(), 
            Sort.by(Sort.Direction.DESC, "timestamp")
        );
        
        LocalDateTime from = request.getFrom() != null 
            ? request.getFrom() 
            : LocalDateTime.now().minusDays(7);
        
        LocalDateTime to = request.getTo() != null 
            ? request.getTo() 
            : LocalDateTime.now();
        
        return logEntryRepository.searchLogs(
            from,
            to,
            request.getServiceName(),
            request.getEnvironment(),
            request.getSeverity(),
            pageable
        );
    }
    
    private boolean shouldRequestAnalysis(LogEntry logEntry) {
        if (!autoRequestAnalysis) {
            return false;
        }
        
        // 심각도 기준 체크
        SeverityLevel threshold = SeverityLevel.valueOf(severityThreshold);
        return logEntry.getSeverity().ordinal() >= threshold.ordinal();
    }
}
```

### 5. Controller 구현

```java
package com.bifrost.heimdall.controller;

import com.bifrost.heimdall.dto.*;
import com.bifrost.heimdall.entity.LogEntry;
import com.bifrost.heimdall.service.LogIngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class LogController {
    
    private final LogIngestionService logIngestionService;
    
    @PostMapping
    public ResponseEntity<LogIngestionResponse> ingestLog(
        @Valid @RequestBody LogIngestionRequest request
    ) {
        LogIngestionResponse response = logIngestionService.ingestLog(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/search")
    public ResponseEntity<Page<LogEntry>> searchLogs(
        @ModelAttribute LogSearchRequest request
    ) {
        Page<LogEntry> logs = logIngestionService.searchLogs(request);
        return ResponseEntity.ok(logs);
    }
    
    @GetMapping("/{logId}")
    public ResponseEntity<LogEntry> getLog(@PathVariable Long logId) {
        // 구현 생략
        return ResponseEntity.ok().build();
    }
}
```

---

## 🔄 Bifrost 연동 구현

### 1. Kafka Event 클래스

```java
package com.bifrost.heimdall.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisRequestEvent {
    
    private String requestId;
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
```

```java
package com.bifrost.heimdall.kafka.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResultEvent {
    
    private String requestId;
    private String correlationId;
    private LocalDateTime timestamp;
    private Long logId;
    private AnalysisResultData analysisResult;
    private Long bifrostAnalysisId;
    private String model;
    private BigDecimal durationSeconds;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnalysisResultData {
        private String summary;
        private String rootCause;
        private String recommendation;
        private String severity;
        private BigDecimal confidence;
    }
}
```

### 2. Kafka Producer

```java
package com.bifrost.heimdall.kafka.producer;

import com.bifrost.heimdall.entity.LogEntry;
import com.bifrost.heimdall.kafka.event.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${heimdall.kafka.topics.log-ingestion}")
    private String logIngestionTopic;
    
    @Value("${heimdall.kafka.topics.analysis-request}")
    private String analysisRequestTopic;
    
    public void sendLogIngestionEvent(LogEntry logEntry) {
        try {
            String eventJson = objectMapper.writeValueAsString(logEntry);
            
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(logIngestionTopic, logEntry.getEventId(), eventJson);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Log ingestion event sent: logId={}, topic={}, partition={}, offset={}",
                        logEntry.getId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send log ingestion event: logId={}", 
                        logEntry.getId(), ex);
                }
            });
            
        } catch (Exception e) {
            log.error("Error serializing log entry: {}", logEntry.getId(), e);
        }
    }
    
    public void sendAnalysisRequest(LogEntry logEntry) {
        try {
            String requestId = UUID.randomUUID().toString();
            String correlationId = UUID.randomUUID().toString();
            
            AnalysisRequestEvent event = AnalysisRequestEvent.builder()
                .requestId(requestId)
                .timestamp(LocalDateTime.now())
                .logId(logEntry.getId())
                .logContent(logEntry.getLogContent())
                .serviceName(logEntry.getServiceName())
                .environment(logEntry.getEnvironment())
                .analysisType("error")
                .priority("HIGH")
                .callbackTopic("analysis.result")
                .correlationId(correlationId)
                .build();
            
            String eventJson = objectMapper.writeValueAsString(event);
            
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(analysisRequestTopic, requestId, eventJson);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Analysis request sent: requestId={}, logId={}", 
                        requestId, logEntry.getId());
                } else {
                    log.error("Failed to send analysis request: requestId={}", 
                        requestId, ex);
                }
            });
            
        } catch (Exception e) {
            log.error("Error creating analysis request: {}", logEntry.getId(), e);
        }
    }
}
```

### 3. Kafka Consumer

```java
package com.bifrost.heimdall.kafka.listener;

import com.bifrost.heimdall.entity.*;
import com.bifrost.heimdall.kafka.event.AnalysisResultEvent;
import com.bifrost.heimdall.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalysisResultListener {
    
    private final ObjectMapper objectMapper;
    private final LogEntryRepository logEntryRepository;
    private final AnalysisResultRepository analysisResultRepository;
    
    @KafkaListener(
        topics = "${heimdall.kafka.topics.analysis-result}",
        groupId = "heimdall-analysis-result-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleAnalysisResult(
        @Payload String message,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment acknowledgment
    ) {
        log.info("Received analysis result from topic={}, partition={}, offset={}", 
            topic, partition, offset);
        
        try {
            // 1. JSON 파싱
            AnalysisResultEvent event = objectMapper.readValue(
                message, 
                AnalysisResultEvent.class
            );
            
            log.debug("Analysis result event: requestId={}, logId={}", 
                event.getRequestId(), event.getLogId());
            
            // 2. 로그 엔트리 조회
            LogEntry logEntry = logEntryRepository.findById(event.getLogId())
                .orElseThrow(() -> new RuntimeException(
                    "Log entry not found: " + event.getLogId()
                ));
            
            // 3. 분석 결과 저장
            AnalysisResultEvent.AnalysisResultData data = event.getAnalysisResult();
            
            AnalysisResult analysisResult = AnalysisResult.builder()
                .logEntry(logEntry)
                .bifrostAnalysisId(event.getBifrostAnalysisId())
                .requestId(event.getRequestId())
                .correlationId(event.getCorrelationId())
                .summary(data.getSummary())
                .rootCause(data.getRootCause())
                .recommendation(data.getRecommendation())
                .severity(SeverityLevel.valueOf(data.getSeverity()))
                .confidence(data.getConfidence())
                .model(event.getModel())
                .durationSeconds(event.getDurationSeconds())
                .analyzedAt(event.getTimestamp())
                .build();
            
            analysisResultRepository.save(analysisResult);
            
            log.info("Analysis result saved: id={}, logId={}, bifrostAnalysisId={}", 
                analysisResult.getId(), 
                event.getLogId(), 
                event.getBifrostAnalysisId());
            
            // 4. 수동 커밋
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing analysis result: topic={}, partition={}, offset={}", 
                topic, partition, offset, e);
            
            // 에러 처리 로직 (재시도 또는 DLQ 전송)
            // acknowledgment.nack(Duration.ofSeconds(10)); // Nack with delay
        }
    }
}
```

### 4. Bifrost Integration Service

```java
package com.bifrost.heimdall.service;

import com.bifrost.heimdall.entity.*;
import com.bifrost.heimdall.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BifrostIntegrationService {
    
    private final AnalysisResultRepository analysisResultRepository;
    private final LogEntryRepository logEntryRepository;
    
    /**
     * 특정 로그의 분석 결과 조회
     */
    @Transactional(readOnly = true)
    public Optional<AnalysisResult> getAnalysisForLog(Long logId) {
        LogEntry logEntry = logEntryRepository.findById(logId)
            .orElseThrow(() -> new RuntimeException("Log not found: " + logId));
        
        // 가장 최근 분석 결과 반환
        return logEntry.getAnalysisResults().stream()
            .findFirst(); // CreatedAt DESC 정렬 가정
    }
    
    /**
     * Bifrost 분석 ID로 결과 조회
     */
    @Transactional(readOnly = true)
    public Optional<AnalysisResult> getAnalysisByBifrostId(Long bifrostAnalysisId) {
        return analysisResultRepository.findByBifrostAnalysisId(bifrostAnalysisId);
    }
    
    /**
     * 분석 상태 확인
     */
    @Transactional(readOnly = true)
    public boolean hasAnalysis(Long logId) {
        return analysisResultRepository.existsByLogEntryId(logId);
    }
}
```

---

## 🧪 테스트 전략

### 1. Unit Test

```java
package com.bifrost.heimdall.service;

import com.bifrost.heimdall.dto.*;
import com.bifrost.heimdall.entity.*;
import com.bifrost.heimdall.repository.*;
import com.bifrost.heimdall.kafka.producer.KafkaProducerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogIngestionServiceTest {
    
    @Mock
    private LogEntryRepository logEntryRepository;
    
    @Mock
    private KafkaProducerService kafkaProducerService;
    
    @InjectMocks
    private LogIngestionService logIngestionService;
    
    @Test
    void ingestLog_Success() {
        // Given
        LogIngestionRequest request = LogIngestionRequest.builder()
            .source("test-source")
            .serviceName("test-service")
            .environment("dev")
            .severity(SeverityLevel.ERROR)
            .logContent("Test error log")
            .build();
        
        LogEntry savedLogEntry = LogEntry.builder()
            .id(1L)
            .eventId("test-event-id")
            .build();
        
        when(logEntryRepository.save(any(LogEntry.class)))
            .thenReturn(savedLogEntry);
        
        // When
        LogIngestionResponse response = logIngestionService.ingestLog(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getLogId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("ACCEPTED");
        
        verify(logEntryRepository, times(1)).save(any(LogEntry.class));
        verify(kafkaProducerService, times(1)).sendLogIngestionEvent(any());
    }
}
```

### 2. Integration Test

```java
package com.bifrost.heimdall.controller;

import com.bifrost.heimdall.dto.*;
import com.bifrost.heimdall.entity.SeverityLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LogControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void ingestLog_Returns201() throws Exception {
        // Given
        LogIngestionRequest request = LogIngestionRequest.builder()
            .source("integration-test")
            .serviceName("test-service")
            .environment("test")
            .severity(SeverityLevel.ERROR)
            .logContent("Integration test log")
            .build();
        
        // When & Then
        mockMvc.perform(post("/api/v1/logs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.logId").exists())
            .andExpect(jsonPath("$.eventId").exists())
            .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }
}
```

### 3. Kafka Integration Test

```java
package com.bifrost.heimdall.kafka;

import com.bifrost.heimdall.kafka.event.AnalysisResultEvent;
import com.bifrost.heimdall.kafka.listener.AnalysisResultListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
    partitions = 1,
    topics = {"analysis.result"},
    brokerProperties = {"listeners=PLAINTEXT://localhost:9092"}
)
class AnalysisResultListenerTest {
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private AnalysisResultListener listener;
    
    @Test
    void handleAnalysisResult_Success() throws Exception {
        // Given
        AnalysisResultEvent event = AnalysisResultEvent.builder()
            .requestId("test-request-id")
            .logId(1L)
            .bifrostAnalysisId(100L)
            .model("mistral")
            .durationSeconds(BigDecimal.valueOf(2.5))
            .timestamp(LocalDateTime.now())
            .build();
        
        String eventJson = objectMapper.writeValueAsString(event);
        
        // When
        kafkaTemplate.send("analysis.result", event.getRequestId(), eventJson);
        
        // Then
        await()
            .atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                // 검증 로직
            });
    }
}
```

---

## 🚀 배포 및 운영

### 1. Docker Compose 개발 환경

```yaml
# docker-compose.yml
version: '3.8'

services:
  heimdall:
    build: .
    container_name: heimdall-app
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - DATABASE_URL=jdbc:postgresql://postgres:5432/heimdall
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    depends_on:
      - postgres
      - kafka
    networks:
      - heimdall-network
    restart: unless-stopped

  postgres:
    image: postgres:16-alpine
    container_name: heimdall-postgres
    environment:
      - POSTGRES_USER=heimdall
      - POSTGRES_PASSWORD=heimdall123
      - POSTGRES_DB=heimdall
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - heimdall-network

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: heimdall-kafka
    ports:
      - "9092:9092"
    environment:
      - KAFKA_BROKER_ID=1
      - KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181
      - KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092
      - KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1
    depends_on:
      - zookeeper
    networks:
      - heimdall-network

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: heimdall-zookeeper
    environment:
      - ZOOKEEPER_CLIENT_PORT=2181
      - ZOOKEEPER_TICK_TIME=2000
    networks:
      - heimdall-network

volumes:
  postgres-data:

networks:
  heimdall-network:
    driver: bridge
```

### 2. Kubernetes 배포 가이드

```bash
# 1. Namespace 생성
kubectl create namespace bifrost-ecosystem

# 2. Secret 생성
kubectl create secret generic heimdall-secrets \
  --from-literal=database-url="jdbc:postgresql://postgres:5432/heimdall" \
  --from-literal=database-username="heimdall" \
  --from-literal=database-password="heimdall123" \
  --namespace=bifrost-ecosystem

# 3. ConfigMap 생성
kubectl apply -f k8s/configmap.yaml

# 4. Deployment 생성
kubectl apply -f k8s/deployment.yaml

# 5. Service 생성
kubectl apply -f k8s/service.yaml

# 6. 상태 확인
kubectl get pods -n bifrost-ecosystem
kubectl logs -f deployment/heimdall -n bifrost-ecosystem
```

### 3. 모니터링 설정

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'heimdall'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['heimdall:8080']
    scrape_interval: 15s
```

---

## 🔧 트러블슈팅

### 1. Kafka 연결 실패

```bash
# Kafka 상태 확인
kubectl exec -it kafka-0 -- kafka-topics --list --bootstrap-server localhost:9092

# Topic 수동 생성
kubectl exec -it kafka-0 -- kafka-topics --create \
  --topic analysis.request \
  --partitions 3 \
  --replication-factor 1 \
  --bootstrap-server localhost:9092
```

### 2. Database Connection Pool 부족

```yaml
# application.yml 수정
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
```

### 3. Consumer Lag 발생

```bash
# Consumer Group 상태 확인
kubectl exec -it kafka-0 -- kafka-consumer-groups \
  --describe \
  --group heimdall-consumer-group \
  --bootstrap-server localhost:9092

# Concurrency 증가
# KafkaConfig.java
factory.setConcurrency(5); // 3 → 5
```

---

## 📚 참고 자료

### Spring Boot 공식 문서
- https://spring.io/projects/spring-boot
- https://docs.spring.io/spring-boot/docs/current/reference/html/

### Spring Kafka 문서
- https://spring.io/projects/spring-kafka
- https://docs.spring.io/spring-kafka/reference/

### Kafka 베스트 프랙티스
- https://kafka.apache.org/documentation/

---

## ✅ 구현 체크리스트

### Phase 1: 기본 구조 (1주)
- [ ] Spring Boot 프로젝트 생성
- [ ] 데이터베이스 스키마 설계 및 마이그레이션
- [ ] Entity, Repository, Service 기본 구조
- [ ] REST API 엔드포인트 구현
- [ ] 단위 테스트 작성

### Phase 2: Kafka 통합 (1주)
- [ ] Kafka 설정 및 Topic 생성
- [ ] Kafka Producer 구현
- [ ] Kafka Consumer 구현
- [ ] 이벤트 스키마 정의
- [ ] Kafka 통합 테스트

### Phase 3: Bifrost 연동 (1주)
- [ ] 분석 요청 이벤트 발행
- [ ] 분석 결과 수신 처리
- [ ] 양방향 통신 검증
- [ ] 에러 핸들링 및 재시도 로직
- [ ] 통합 테스트

### Phase 4: 고급 기능 (1주)
- [ ] Elasticsearch 통합
- [ ] 통계 및 집계 기능
- [ ] 알림 시스템
- [ ] 보안 강화 (JWT, API Key)
- [ ] 성능 최적화

### Phase 5: 배포 (1주)
- [ ] Docker 이미지 빌드
- [ ] Kubernetes 매니페스트 작성
- [ ] CI/CD 파이프라인 구축
- [ ] 모니터링 대시보드 설정
- [ ] 문서화 완료

---

**문서 버전**: 1.0.0  
**최종 수정**: 2024-11-11  
**작성 목적**: Heimdall 개발팀 구현 가이드  
**예상 개발 기간**: 5주

---

## 🤝 Bifrost 팀과의 협업

### 커뮤니케이션 포인트

1. **Kafka Topic 스키마 합의**
   - 이벤트 포맷 정의
   - 필수/선택 필드 합의
   - 버전 관리 전략

2. **에러 처리 전략**
   - 재시도 정책
   - DLQ 처리 방식
   - 타임아웃 설정

3. **성능 요구사항**
   - 처리 용량 (TPS)
   - 응답 시간 (SLA)
   - 리소스 제약

4. **배포 일정 조율**
   - 개발 환경 연동
   - 스테이징 테스트
   - 프로덕션 배포

### 정기 Sync-up
- 주 2회 기술 미팅
- 이슈 트래커 공유 (Jira/GitHub Issues)
- API 변경 사항 즉시 공유

---

**Happy Coding! 🚀**
