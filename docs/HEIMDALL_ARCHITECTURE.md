# 🛡️ Heimdall Architecture Guide

> **"The Guardian of the Rainbow Bridge"** - Event-driven Log Processing Microservice

---

## 📋 목차

1. [시스템 개요](#-시스템-개요)
2. [아키텍처 설계](#-아키텍처-설계)
3. [기술 스택](#-기술-스택)
4. [Kafka 통신 설계](#-kafka-통신-설계)
5. [데이터베이스 설계](#-데이터베이스-설계)
6. [API 설계](#-api-설계)
7. [보안 아키텍처](#-보안-아키텍처)
8. [배포 전략](#-배포-전략)

---

## 🎯 시스템 개요

### Mission Statement

**Heimdall**은 Bifrost의 보완 서비스로, 다음 역할을 담당합니다:

- 📊 **로그 수집 및 전처리**: 다양한 소스로부터 로그 수집
- 🔄 **이벤트 기반 처리**: Kafka를 통한 비동기 로그 처리
- 💾 **장기 저장소**: 분석된 로그의 영구 보관
- 📈 **통계 및 집계**: 로그 통계, 트렌드 분석
- 🔍 **검색 엔진**: Elasticsearch 기반 전문 검색
- 📡 **알림 관리**: 임계값 기반 알림 발송

### 시스템 경계

```
┌─────────────────────────────────────────────────────────────┐
│                    Bifrost Ecosystem                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────┐         ┌──────────────────┐        │
│  │    Bifrost       │         │    Heimdall      │        │
│  │  (Python/AI)     │◄───────►│ (Java/Spring)    │        │
│  │                  │  Kafka  │                  │        │
│  │  - AI 분석       │         │  - 로그 수집     │        │
│  │  - 실시간 처리    │         │  - 장기 저장     │        │
│  │  - WebUI        │         │  - 검색/통계     │        │
│  └──────────────────┘         └──────────────────┘        │
│         │                              │                   │
│         │                              │                   │
│    PostgreSQL                     PostgreSQL               │
│    (분석 결과)                    (로그 원본)              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 핵심 설계 원칙

1. **이벤트 기반**: 모든 로그는 Kafka 이벤트로 처리
2. **느슨한 결합**: Bifrost와 독립적으로 운영 가능
3. **확장성**: 수평 확장 가능한 stateless 서비스
4. **내결함성**: 장애 격리 및 자동 복구
5. **관찰성**: 모든 계층에서 메트릭 및 트레이싱

---

## 🏛️ 아키텍처 설계

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Client Layer                           │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                 │
│  │  REST    │  │  gRPC    │  │  Kafka   │                 │
│  │  API     │  │  API     │  │ Consumer │                 │
│  └──────────┘  └──────────┘  └──────────┘                 │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│              Application Layer (Spring Boot)                │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Controllers                                        │   │
│  │  - LogController                                    │   │
│  │  - SearchController                                 │   │
│  │  - StatisticsController                             │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Services (Business Logic)                          │   │
│  │  - LogIngestionService                              │   │
│  │  - LogProcessingService                             │   │
│  │  - SearchService                                    │   │
│  │  - StatisticsService                                │   │
│  │  - NotificationService                              │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Kafka Listeners (Event Consumers)                  │   │
│  │  - AnalysisRequestListener                          │   │
│  │  - AnalysisResultListener                           │   │
│  │  - LogIngestionListener                             │   │
│  └─────────────────────────────────────────────────────┘   │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│              Data Access Layer (Spring Data JPA)            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Repositories │  │   Entities   │  │  Mappers     │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                Infrastructure Layer                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │ PostgreSQL  │  │Elasticsearch│  │   Kafka     │        │
│  │  (Primary)  │  │  (Search)   │  │  (Queue)    │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────────────────────────────────┘
```

### Component Diagram

```
heimdall/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── heimdall/
│       │           ├── HeimdallApplication.java
│       │           │
│       │           ├── config/
│       │           │   ├── KafkaConfig.java
│       │           │   ├── DatabaseConfig.java
│       │           │   ├── ElasticsearchConfig.java
│       │           │   ├── SecurityConfig.java
│       │           │   └── AsyncConfig.java
│       │           │
│       │           ├── controller/
│       │           │   ├── LogController.java
│       │           │   ├── SearchController.java
│       │           │   ├── StatisticsController.java
│       │           │   └── HealthController.java
│       │           │
│       │           ├── service/
│       │           │   ├── LogIngestionService.java
│       │           │   ├── LogProcessingService.java
│       │           │   ├── SearchService.java
│       │           │   ├── StatisticsService.java
│       │           │   ├── NotificationService.java
│       │           │   └── BifrostIntegrationService.java
│       │           │
│       │           ├── kafka/
│       │           │   ├── listener/
│       │           │   │   ├── AnalysisRequestListener.java
│       │           │   │   ├── AnalysisResultListener.java
│       │           │   │   └── LogIngestionListener.java
│       │           │   ├── producer/
│       │           │   │   └── KafkaProducerService.java
│       │           │   └── event/
│       │           │       ├── LogIngestionEvent.java
│       │           │       ├── AnalysisRequestEvent.java
│       │           │       └── AnalysisResultEvent.java
│       │           │
│       │           ├── repository/
│       │           │   ├── LogEntryRepository.java
│       │           │   ├── AnalysisResultRepository.java
│       │           │   ├── LogStatisticsRepository.java
│       │           │   └── NotificationRepository.java
│       │           │
│       │           ├── entity/
│       │           │   ├── LogEntry.java
│       │           │   ├── AnalysisResult.java
│       │           │   ├── LogStatistics.java
│       │           │   └── Notification.java
│       │           │
│       │           ├── dto/
│       │           │   ├── LogIngestionRequest.java
│       │           │   ├── LogSearchRequest.java
│       │           │   ├── LogSearchResponse.java
│       │           │   └── StatisticsResponse.java
│       │           │
│       │           ├── exception/
│       │           │   ├── HeimdallException.java
│       │           │   ├── LogProcessingException.java
│       │           │   └── GlobalExceptionHandler.java
│       │           │
│       │           └── util/
│       │               ├── DateTimeUtil.java
│       │               └── HashUtil.java
│       │
│       └── resources/
│           ├── application.yml
│           ├── application-dev.yml
│           ├── application-prod.yml
│           └── logback-spring.xml
│
├── docker/
│   ├── Dockerfile
│   └── docker-compose.yml
│
├── k8s/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── configmap.yaml
│   └── secret.yaml
│
├── build.gradle (또는 pom.xml)
└── README.md
```

---

## 🔧 기술 스택

### Core Framework

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| **Framework** | Spring Boot | 3.2.x | 애플리케이션 프레임워크 |
| **Language** | Java | 17 (LTS) | 프로그래밍 언어 |
| **Build Tool** | Gradle | 8.x | 빌드 자동화 |

### Spring Ecosystem

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Spring Web** | Spring MVC | REST API |
| **Spring Data JPA** | Hibernate | ORM / 데이터 액세스 |
| **Spring Kafka** | - | Kafka 통합 |
| **Spring Security** | - | 인증/인가 |
| **Spring Actuator** | - | 헬스체크/메트릭 |
| **Spring Validation** | - | 입력 검증 |

### Infrastructure

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Database** | PostgreSQL 16 | 주 데이터 저장소 |
| **Message Queue** | Apache Kafka 3.6 | 이벤트 스트리밍 |
| **Search Engine** | Elasticsearch 8.x | 전문 검색 |
| **Cache** | Redis 7.x | 캐싱 (선택) |
| **Monitoring** | Micrometer + Prometheus | 메트릭 수집 |

### DevOps

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Container** | Docker | 컨테이너화 |
| **Orchestration** | Kubernetes | 컨테이너 오케스트레이션 |
| **CI/CD** | GitHub Actions | 자동화 파이프라인 |

---

## 🔄 Kafka 통신 설계

### Topic 구조

```
bifrost-ecosystem/
├── logs.ingestion         # 로그 수집 요청
├── logs.processing        # 로그 전처리 완료
├── analysis.request       # AI 분석 요청 (→ Bifrost)
├── analysis.result        # AI 분석 결과 (← Bifrost)
├── notification.alert     # 알림 발송
└── dlq.failed            # Dead Letter Queue (실패)
```

### Event Schema

#### 1. logs.ingestion (Heimdall → Kafka)

```json
{
  "eventId": "uuid",
  "timestamp": "2024-10-25T10:30:00Z",
  "source": "k8s-prod",
  "serviceName": "user-service",
  "environment": "production",
  "logContent": "ERROR: Connection timeout...",
  "severity": "ERROR",
  "metadata": {
    "podName": "user-service-abc123",
    "namespace": "production",
    "nodeId": "node-1"
  }
}
```

#### 2. analysis.request (Heimdall → Bifrost)

```json
{
  "requestId": "uuid",
  "timestamp": "2024-10-25T10:30:05Z",
  "logId": 12345,
  "logContent": "ERROR: Connection timeout...",
  "serviceName": "user-service",
  "environment": "production",
  "analysisType": "error",
  "priority": "HIGH",
  "callbackTopic": "analysis.result",
  "correlationId": "correlation-uuid"
}
```

#### 3. analysis.result (Bifrost → Heimdall)

```json
{
  "requestId": "uuid",
  "correlationId": "correlation-uuid",
  "timestamp": "2024-10-25T10:30:15Z",
  "logId": 12345,
  "analysisResult": {
    "summary": "PostgreSQL 연결 타임아웃 발생",
    "rootCause": "Connection pool 고갈",
    "recommendation": "max_connections 증가 권장",
    "severity": "HIGH",
    "confidence": 0.95
  },
  "bifrostAnalysisId": 456,
  "model": "mistral",
  "durationSeconds": 2.5
}
```

### Kafka Consumer 설정

```java
@Configuration
public class KafkaConfig {
    
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, 
                   "kafka:9092");
        config.put(ConsumerConfig.GROUP_ID_CONFIG, 
                   "heimdall-consumer-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, 
                   StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, 
                   StringDeserializer.class);
        
        // At-least-once 보장
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        // 성능 튜닝
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        config.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);
        
        return new DefaultKafkaConsumerFactory<>(config);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> 
        kafkaListenerContainerFactory() {
        
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3); // 병렬 처리
        factory.getContainerProperties()
               .setAckMode(AckMode.MANUAL_IMMEDIATE);
        
        // 에러 핸들링
        factory.setCommonErrorHandler(
            new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate()),
                new FixedBackOff(1000L, 3L)
            )
        );
        
        return factory;
    }
}
```

### Kafka Producer 설정

```java
@Service
public class KafkaProducerService {
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    public void sendAnalysisRequest(AnalysisRequestEvent event) {
        String topic = "analysis.request";
        String key = event.getLogId().toString();
        String value = objectMapper.writeValueAsString(event);
        
        ListenableFuture<SendResult<String, String>> future = 
            kafkaTemplate.send(topic, key, value);
        
        future.addCallback(
            result -> log.info("Message sent: {}", result.getRecordMetadata()),
            ex -> log.error("Failed to send message", ex)
        );
    }
}
```

### Message Flow

```
1. 로그 수집 플로우
   Client → Heimdall REST API
     → logs.ingestion (Kafka)
     → LogIngestionListener
     → PostgreSQL 저장
     → analysis.request (Kafka) → Bifrost

2. 분석 결과 수신 플로우
   Bifrost → analysis.result (Kafka)
     → AnalysisResultListener
     → PostgreSQL 업데이트
     → Elasticsearch 인덱싱
     → notification.alert (조건 만족 시)

3. 에러 처리 플로우
   처리 실패 → dlq.failed (Kafka)
     → DLQListener
     → 재처리 또는 알림
```

---

## 💾 데이터베이스 설계

### ERD (Entity Relationship Diagram)

```
┌─────────────────────┐
│    log_entries      │
├─────────────────────┤
│ id (PK)             │
│ event_id (UK)       │
│ timestamp           │
│ source              │
│ service_name        │
│ environment         │
│ severity            │
│ log_content (TEXT)  │
│ log_hash            │
│ metadata (JSONB)    │
│ created_at          │
└──────────┬──────────┘
           │ 1
           │
           │ N
┌──────────▼──────────┐
│ analysis_results    │
├─────────────────────┤
│ id (PK)             │
│ log_id (FK)         │
│ bifrost_analysis_id │
│ request_id          │
│ correlation_id      │
│ summary             │
│ root_cause          │
│ recommendation      │
│ severity            │
│ confidence          │
│ model               │
│ duration_seconds    │
│ analyzed_at         │
└─────────────────────┘

┌─────────────────────┐
│  log_statistics     │
├─────────────────────┤
│ id (PK)             │
│ date                │
│ hour                │
│ service_name        │
│ environment         │
│ severity            │
│ count               │
│ avg_size_bytes      │
│ created_at          │
└─────────────────────┘

┌─────────────────────┐
│   notifications     │
├─────────────────────┤
│ id (PK)             │
│ log_id (FK)         │
│ analysis_id (FK)    │
│ type                │
│ channel             │
│ recipient           │
│ message             │
│ sent_at             │
│ status              │
└─────────────────────┘
```

### Schema Definition (PostgreSQL)

```sql
-- 로그 엔트리 테이블
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
    
    -- 인덱스
    INDEX idx_timestamp (timestamp DESC),
    INDEX idx_service_env (service_name, environment),
    INDEX idx_severity (severity),
    INDEX idx_log_hash (log_hash)
);

-- 파티셔닝 (선택적)
CREATE TABLE log_entries_2024_10 PARTITION OF log_entries
    FOR VALUES FROM ('2024-10-01') TO ('2024-11-01');

-- 분석 결과 테이블
CREATE TABLE analysis_results (
    id BIGSERIAL PRIMARY KEY,
    log_id BIGINT NOT NULL REFERENCES log_entries(id),
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_log_id (log_id),
    INDEX idx_analyzed_at (analyzed_at DESC),
    INDEX idx_severity (severity)
);

-- 로그 통계 테이블 (집계)
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
    
    UNIQUE (date, hour, service_name, environment, severity),
    INDEX idx_date_hour (date, hour)
);

-- 알림 테이블
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_sent_at (sent_at DESC),
    INDEX idx_status (status)
);
```

### JPA Entity 예시

```java
@Entity
@Table(name = "log_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
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
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @OneToMany(mappedBy = "logEntry", cascade = CascadeType.ALL)
    private List<AnalysisResult> analysisResults;
}
```

---

## 🌐 API 설계

### REST API Endpoints

#### 1. 로그 수집 API

```
POST /api/v1/logs
Content-Type: application/json
Authorization: Bearer <token>

Request Body:
{
  "source": "k8s-prod",
  "serviceName": "user-service",
  "environment": "production",
  "severity": "ERROR",
  "logContent": "ERROR: Connection timeout to database",
  "metadata": {
    "podName": "user-service-abc123",
    "namespace": "production"
  }
}

Response (201 Created):
{
  "logId": 12345,
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2024-10-25T10:30:00Z",
  "status": "ACCEPTED",
  "analysisRequested": true
}
```

#### 2. 로그 검색 API

```
GET /api/v1/logs/search
Authorization: Bearer <token>

Query Parameters:
- serviceName: string
- environment: string
- severity: ERROR|WARN|INFO
- from: ISO8601 timestamp
- to: ISO8601 timestamp
- keyword: string
- page: int (default: 0)
- size: int (default: 20)

Response (200 OK):
{
  "content": [
    {
      "logId": 12345,
      "timestamp": "2024-10-25T10:30:00Z",
      "serviceName": "user-service",
      "severity": "ERROR",
      "logContent": "...",
      "hasAnalysis": true
    }
  ],
  "page": {
    "size": 20,
    "totalElements": 1234,
    "totalPages": 62,
    "number": 0
  }
}
```

#### 3. 분석 결과 조회 API

```
GET /api/v1/logs/{logId}/analysis
Authorization: Bearer <token>

Response (200 OK):
{
  "analysisId": 456,
  "logId": 12345,
  "bifrostAnalysisId": 789,
  "summary": "PostgreSQL 연결 타임아웃 발생",
  "rootCause": "Connection pool 고갈",
  "recommendation": "max_connections 증가 권장",
  "severity": "HIGH",
  "confidence": 0.95,
  "model": "mistral",
  "analyzedAt": "2024-10-25T10:30:15Z"
}
```

#### 4. 통계 API

```
GET /api/v1/statistics
Authorization: Bearer <token>

Query Parameters:
- date: YYYY-MM-DD
- serviceName: string
- environment: string
- groupBy: hour|day|service

Response (200 OK):
{
  "period": {
    "from": "2024-10-25T00:00:00Z",
    "to": "2024-10-25T23:59:59Z"
  },
  "statistics": [
    {
      "timestamp": "2024-10-25T10:00:00Z",
      "serviceName": "user-service",
      "totalLogs": 1523,
      "bySeverity": {
        "ERROR": 45,
        "WARN": 234,
        "INFO": 1244
      },
      "avgSizeBytes": 512
    }
  ]
}
```

#### 5. 헬스체크 API

```
GET /actuator/health

Response (200 OK):
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "SELECT 1"
      }
    },
    "kafka": {
      "status": "UP"
    },
    "elasticsearch": {
      "status": "UP"
    }
  }
}
```

### gRPC API (선택적)

고성능 로그 수집을 위한 gRPC 인터페이스:

```protobuf
syntax = "proto3";

package heimdall.v1;

service LogService {
  rpc IngestLog(LogIngestionRequest) returns (LogIngestionResponse);
  rpc StreamLogs(stream LogIngestionRequest) returns (stream LogIngestionResponse);
}

message LogIngestionRequest {
  string source = 1;
  string service_name = 2;
  string environment = 3;
  string severity = 4;
  string log_content = 5;
  map<string, string> metadata = 6;
  int64 timestamp_millis = 7;
}

message LogIngestionResponse {
  int64 log_id = 1;
  string event_id = 2;
  string status = 3;
}
```

---

## 🔒 보안 아키텍처

### 인증/인가

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().denyAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder()))
            );
        
        return http.build();
    }
    
    @Bean
    public JwtDecoder jwtDecoder() {
        // JWT 검증 로직
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
```

### API Key 인증

```java
@Component
public class ApiKeyFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        
        String apiKey = request.getHeader("X-API-Key");
        
        if (apiKey != null && apiKeyService.validate(apiKey)) {
            // 인증 성공
            filterChain.doFilter(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
```

### Rate Limiting

```java
@Component
public class RateLimitingInterceptor implements HandlerInterceptor {
    
    @Autowired
    private RateLimiter rateLimiter;
    
    @Override
    public boolean preHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler
    ) throws Exception {
        
        String clientId = extractClientId(request);
        
        if (rateLimiter.tryAcquire(clientId)) {
            return true;
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return false;
        }
    }
}
```

---

## 🚀 배포 전략

### Docker 배포

```dockerfile
# Multi-stage build
FROM gradle:8-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle clean build -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Kubernetes 배포

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: heimdall
spec:
  replicas: 3
  selector:
    matchLabels:
      app: heimdall
  template:
    metadata:
      labels:
        app: heimdall
    spec:
      containers:
      - name: heimdall
        image: heimdall:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: KAFKA_BOOTSTRAP_SERVERS
          value: "kafka:9092"
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: heimdall-secrets
              key: database-url
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 5
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
```

### HPA (Horizontal Pod Autoscaler)

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: heimdall-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: heimdall
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

---

## 📊 모니터링 및 관찰성

### Micrometer 메트릭

```java
@Configuration
public class MetricsConfig {
    
    @Bean
    public MeterRegistry meterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
    
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}

@Service
public class LogIngestionService {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Timed(value = "log.ingestion", description = "Time to ingest log")
    public void ingestLog(LogEntry logEntry) {
        // 로직
        
        meterRegistry.counter("logs.ingested.total",
            "service", logEntry.getServiceName(),
            "severity", logEntry.getSeverity().name()
        ).increment();
    }
}
```

### 주요 메트릭

- `logs.ingested.total`: 수집된 로그 수
- `logs.processed.total`: 처리된 로그 수
- `analysis.requested.total`: AI 분석 요청 수
- `analysis.completed.total`: AI 분석 완료 수
- `kafka.consumer.lag`: Kafka consumer lag
- `db.connection.pool.size`: DB 커넥션 풀 크기

---

**문서 버전**: 1.0.0  
**최종 수정**: 2024-11-11  
**작성 목적**: Heimdall 개발팀 아키텍처 가이드
