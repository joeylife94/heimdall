# Heimdall Project Structure

```
heimdall/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── heimdall/
│   │   │           ├── HeimdallApplication.java          # Main application entry point
│   │   │           │
│   │   │           ├── config/                           # Configuration classes
│   │   │           │   ├── AsyncConfig.java
│   │   │           │   ├── JacksonConfig.java
│   │   │           │   ├── KafkaConfig.java
│   │   │           │   ├── MetricsConfig.java
│   │   │           │   └── SecurityConfig.java
│   │   │           │
│   │   │           ├── controller/                       # REST API controllers
│   │   │           │   ├── AnalysisController.java
│   │   │           │   ├── LogController.java
│   │   │           │   ├── SearchController.java
│   │   │           │   └── StatisticsController.java
│   │   │           │
│   │   │           ├── dto/                             # Data Transfer Objects
│   │   │           │   ├── AnalysisResultResponse.java
│   │   │           │   ├── ErrorResponse.java
│   │   │           │   ├── LogIngestionRequest.java
│   │   │           │   ├── LogIngestionResponse.java
│   │   │           │   ├── LogSearchRequest.java
│   │   │           │   ├── LogSearchResponse.java
│   │   │           │   └── StatisticsResponse.java
│   │   │           │
│   │   │           ├── entity/                          # JPA entities
│   │   │           │   ├── AnalysisResult.java
│   │   │           │   ├── LogEntry.java
│   │   │           │   ├── LogStatistics.java
│   │   │           │   └── Notification.java
│   │   │           │
│   │   │           ├── exception/                       # Exception handling
│   │   │           │   ├── GlobalExceptionHandler.java
│   │   │           │   ├── HeimdallException.java
│   │   │           │   └── LogProcessingException.java
│   │   │           │
│   │   │           ├── kafka/                          # Kafka integration
│   │   │           │   ├── event/
│   │   │           │   │   ├── AnalysisRequestEvent.java
│   │   │           │   │   ├── AnalysisResultEvent.java
│   │   │           │   │   └── LogIngestionEvent.java
│   │   │           │   ├── listener/
│   │   │           │   │   ├── AnalysisResultListener.java
│   │   │           │   │   └── LogIngestionListener.java
│   │   │           │   └── producer/
│   │   │           │       └── KafkaProducerService.java
│   │   │           │
│   │   │           ├── repository/                     # Data access layer
│   │   │           │   ├── AnalysisResultRepository.java
│   │   │           │   ├── LogEntryRepository.java
│   │   │           │   ├── LogStatisticsRepository.java
│   │   │           │   └── NotificationRepository.java
│   │   │           │
│   │   │           ├── service/                        # Business logic
│   │   │           │   ├── LogIngestionService.java
│   │   │           │   ├── LogProcessingService.java
│   │   │           │   ├── NotificationService.java
│   │   │           │   ├── SearchService.java
│   │   │           │   └── StatisticsService.java
│   │   │           │
│   │   │           └── util/                           # Utility classes
│   │   │               ├── DateTimeUtil.java
│   │   │               └── HashUtil.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml                         # Main configuration
│   │       ├── application-dev.yml                     # Development profile
│   │       ├── application-prod.yml                    # Production profile
│   │       ├── logback-spring.xml                      # Logging configuration
│   │       └── db/
│   │           └── schema.sql                          # Database schema
│   │
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── heimdall/
│       │           └── HeimdallApplicationTests.java
│       └── resources/
│           └── application-test.yml
│
├── docker/
│   ├── Dockerfile                                      # Docker image definition
│   └── docker-compose.yml                              # Local dev environment
│
├── k8s/                                               # Kubernetes manifests
│   ├── configmap.yaml
│   ├── deployment.yaml
│   ├── hpa.yaml
│   ├── secret.yaml
│   └── service.yaml
│
├── docs/
│   ├── HEIMDALL_ARCHITECTURE.md                       # Architecture documentation
│   └── HEIMDALL_IMPLEMENTATION_GUIDE.md               # Implementation guide
│
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
│
├── .gitignore
├── build.gradle                                        # Gradle build configuration
├── gradlew                                            # Gradle wrapper (Unix)
├── gradlew.bat                                        # Gradle wrapper (Windows)
├── QUICKSTART.md                                      # Quick start guide
├── README.md                                          # Main documentation
├── run.sh                                            # Run script (Unix)
├── run.bat                                           # Run script (Windows)
└── settings.gradle                                    # Gradle settings
```

## Key Components

### 1. **Configuration Layer** (`config/`)
- Spring Boot configuration
- Kafka setup with consumer/producer factories
- Security configuration (JWT/API Key)
- Async processing setup
- Metrics and monitoring

### 2. **API Layer** (`controller/`)
- RESTful endpoints for log ingestion
- Search and query APIs
- Analysis result retrieval
- Statistics endpoints

### 3. **Business Logic** (`service/`)
- Log ingestion and validation
- Event processing from Kafka
- Search and filtering logic
- Statistics aggregation
- Notification management

### 4. **Data Layer** (`repository/`, `entity/`)
- JPA repositories with custom queries
- Entity definitions with relationships
- Database schema mapping

### 5. **Kafka Integration** (`kafka/`)
- Event definitions (POJOs)
- Kafka listeners for consuming messages
- Producer service for publishing events
- Integration with Bifrost via message queues

### 6. **Exception Handling** (`exception/`)
- Custom exception classes
- Global exception handler
- Standardized error responses

## Technology Stack Summary

- **Framework**: Spring Boot 3.2.x
- **Language**: Java 17
- **Build**: Gradle 8.x
- **Database**: PostgreSQL 16
- **Message Queue**: Apache Kafka 3.6
- **Search**: Elasticsearch 8.x
- **Cache**: Redis 7.x
- **Monitoring**: Micrometer + Prometheus
- **Containerization**: Docker
- **Orchestration**: Kubernetes

## Development Workflow

1. **Build**: `./gradlew build`
2. **Test**: `./gradlew test`
3. **Run**: `./gradlew bootRun`
4. **Docker**: `docker-compose up`
5. **Deploy**: `kubectl apply -f k8s/`

## Integration Points

- **Bifrost**: AI analysis service (via Kafka)
- **PostgreSQL**: Primary data store
- **Elasticsearch**: Full-text search
- **Kafka**: Event streaming
- **Prometheus**: Metrics collection
- **Kubernetes**: Container orchestration
