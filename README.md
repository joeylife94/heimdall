# 🛡️ Heimdall - Event-driven Log Processing Microservice

> **"The Guardian of the Rainbow Bridge"** - Companion service to Bifrost for log collection, processing, and analysis

## 🎉 Verified Working - MSA Integration

- ✅ **Tested on:** 2024-11-17
- ✅ **Full MSA Stack:** Working with Bifrost via Kafka
- ✅ **E2E Tests:** Available in `../tests/e2e/`
- ✅ **Docker Compose:** `../docker-compose.msa.yml` ready
- ✅ **Quick Start:** See [MSA_QUICKSTART.md](../MSA_QUICKSTART.md)

## 📋 Overview

Heimdall is a Spring Boot-based microservice designed to handle log collection, processing, storage, and analysis in conjunction with the Bifrost AI analysis system. It provides a robust, scalable, and fault-tolerant platform for managing application logs across your infrastructure.

### Key Features

- 📊 **Log Collection & Ingestion**: REST API and Kafka-based log ingestion
- 🔄 **Event-Driven Architecture**: Asynchronous processing using Apache Kafka
- 💾 **Persistent Storage**: PostgreSQL for structured data, Elasticsearch for search
- 🤖 **AI Integration**: Seamless integration with Bifrost for log analysis
- 📈 **Statistics & Analytics**: Real-time log statistics and trend analysis
- 🔍 **Advanced Search**: Full-text search capabilities via Elasticsearch
- 📡 **Notification System**: Alert notifications based on analysis results
- 📊 **Observability**: Built-in metrics, health checks, and monitoring

## 🏗️ Architecture

### Technology Stack

- **Framework**: Spring Boot 3.2.x
- **Language**: Java 17 (LTS)
- **Build Tool**: Gradle 8.x
- **Database**: PostgreSQL 16
- **Message Queue**: Apache Kafka 3.6
- **Search Engine**: Elasticsearch 8.x
- **Cache**: Redis 7.x (optional)
- **Monitoring**: Micrometer + Prometheus

### System Components

```
┌─────────────────────────────────────────────────────────────┐
│                      Client Layer                           │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                 │
│  │  REST    │  │  Kafka   │  │  gRPC    │                 │
│  │  API     │  │ Consumer │  │  (opt)   │                 │
│  └──────────┘  └──────────┘  └──────────┘                 │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│              Application Layer (Spring Boot)                │
│  Controllers → Services → Repositories                      │
│  Kafka Listeners → Event Processing                         │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                Infrastructure Layer                         │
│  PostgreSQL | Elasticsearch | Kafka | Redis                │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 Getting Started

### Prerequisites

- Java 17 or higher
- Docker & Docker Compose
- Gradle 8.x (or use the wrapper)

### Quick Start with Docker Compose

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd heimdall
   ```

2. **Start all services**
   ```bash
   cd docker
   docker-compose up -d
   ```

   This will start:
   - PostgreSQL (port 5432)
   - Kafka + Zookeeper (port 9092)
   - Elasticsearch (port 9200)
   - Redis (port 6379)
   - Heimdall application (port 8080)

3. **Verify the application**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

### Local Development Setup

1. **Start infrastructure services**
   ```bash
   cd docker
   docker-compose up -d postgres kafka elasticsearch redis
   ```

2. **Build the application**
   ```bash
   ./gradlew clean build
   ```

3. **Run the application**
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```

## 📡 API Documentation

### Log Ingestion

**POST** `/api/v1/logs`

```bash
curl -X POST http://localhost:8080/api/v1/logs \
  -H "Content-Type: application/json" \
  -d '{
    "source": "k8s-prod",
    "serviceName": "user-service",
    "environment": "production",
    "severity": "ERROR",
    "logContent": "ERROR: Connection timeout to database",
    "metadata": {
      "podName": "user-service-abc123",
      "namespace": "production"
    }
  }'
```

**Response (201 Created)**
```json
{
  "logId": 12345,
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2024-10-25T10:30:00Z",
  "status": "ACCEPTED",
  "analysisRequested": true
}
```

### Log Search

**GET** `/api/v1/logs/search`

```bash
curl "http://localhost:8080/api/v1/logs/search?\
serviceName=user-service&\
environment=production&\
severity=ERROR&\
from=2024-10-25T00:00:00Z&\
to=2024-10-25T23:59:59Z&\
page=0&\
size=20"
```

### Analysis Result

**GET** `/api/v1/logs/{logId}/analysis`

```bash
curl http://localhost:8080/api/v1/logs/12345/analysis
```

### Statistics

**GET** `/api/v1/statistics`

```bash
curl "http://localhost:8080/api/v1/statistics?\
date=2024-10-25&\
serviceName=user-service&\
environment=production"
```

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev` |
| `DATABASE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/heimdall` |
| `DATABASE_USERNAME` | Database username | `heimdall` |
| `DATABASE_PASSWORD` | Database password | `heimdall` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker addresses | `localhost:9092` |
| `ELASTICSEARCH_URIS` | Elasticsearch URIs | `http://localhost:9200` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |

### Application Properties

Key configuration properties in `application.yml`:

```yaml
heimdall:
  log:
    retention-days: 90
  analysis:
    enabled: true
    auto-request: true
  notification:
    enabled: true
    channels: [email, slack]
```

## 🔄 Kafka Topics

| Topic | Purpose | Producer | Consumer |
|-------|---------|----------|----------|
| `logs.ingestion` | Log collection | Heimdall API | Heimdall Listener |
| `logs.processing` | Log preprocessing | Heimdall | - |
| `analysis.request` | AI analysis requests | Heimdall | Bifrost |
| `analysis.result` | AI analysis results | Bifrost | Heimdall |
| `notification.alert` | Alert notifications | Heimdall | Notification Service |
| `dlq.failed` | Dead letter queue | Heimdall | DLQ Handler |

## 🗄️ Database Schema

### Main Tables

- **log_entries**: Raw log data storage
- **analysis_results**: AI analysis results from Bifrost
- **log_statistics**: Aggregated log statistics
- **notifications**: Notification history

### Database Initialization

Run the following SQL to create the schema:

```bash
psql -U heimdall -d heimdall_dev -f src/main/resources/db/schema.sql
```

## 📊 Monitoring

### Health Checks

- **Liveness**: `GET /actuator/health/liveness`
- **Readiness**: `GET /actuator/health/readiness`
- **Full Health**: `GET /actuator/health`

### Metrics

Prometheus metrics available at:
- `GET /actuator/prometheus`

Key metrics:
- `logs.ingested.total`: Total logs ingested
- `analysis.requested.total`: Total analysis requests
- `analysis.completed.total`: Total completed analyses
- `kafka.consumer.lag`: Kafka consumer lag

## 🐳 Docker Deployment

### Build Docker Image

```bash
docker build -f docker/Dockerfile -t heimdall:latest .
```

### Run with Docker

```bash
docker run -d \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:postgresql://postgres:5432/heimdall \
  --name heimdall \
  heimdall:latest
```

## ☸️ Kubernetes Deployment

### Deploy to Kubernetes

```bash
# Apply ConfigMap and Secret
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml

# Deploy application
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml

# Enable auto-scaling
kubectl apply -f k8s/hpa.yaml
```

### Verify Deployment

```bash
kubectl get pods -l app=heimdall
kubectl get svc heimdall
kubectl logs -f deployment/heimdall
```

## 🧪 Testing

### Run Unit Tests

```bash
./gradlew test
```

### Run Integration Tests

```bash
./gradlew integrationTest
```

### Load Testing

```bash
# Install k6
# https://k6.io/docs/getting-started/installation/

k6 run tests/load/log-ingestion.js
```

## 🔐 Security

### Authentication

Currently supports:
- API Key authentication (header: `X-API-Key`)
- JWT Bearer tokens (OAuth2/OpenID Connect ready)

### Security Best Practices

- Use HTTPS in production
- Rotate API keys regularly
- Enable rate limiting
- Keep dependencies updated
- Use secrets management (Kubernetes Secrets, Vault)

## 📈 Performance Tuning

### JVM Options

```bash
JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200"
```

### Kafka Consumer Tuning

```yaml
spring.kafka.consumer:
  max-poll-records: 100
  fetch-min-size: 1024
```

### Database Connection Pool

```yaml
spring.datasource.hikari:
  maximum-pool-size: 20
  minimum-idle: 5
```

## 🛠️ Troubleshooting

### Common Issues

1. **Kafka connection refused**
   - Ensure Kafka is running: `docker-compose ps kafka`
   - Check bootstrap servers configuration

2. **Database connection timeout**
   - Verify PostgreSQL is accessible
   - Check credentials in configuration

3. **Out of Memory errors**
   - Increase JVM heap size
   - Adjust `MaxRAMPercentage`

### Logs

```bash
# View application logs
tail -f logs/heimdall.log

# Docker logs
docker logs -f heimdall-app

# Kubernetes logs
kubectl logs -f deployment/heimdall
```

## 🤝 Integration with Bifrost

Heimdall works in tandem with Bifrost for AI-powered log analysis:

1. **Log Collection**: Heimdall receives logs via REST API
2. **Storage**: Logs stored in PostgreSQL
3. **Analysis Request**: Critical logs sent to Bifrost via Kafka
4. **AI Processing**: Bifrost analyzes logs using LLM
5. **Results**: Analysis results sent back to Heimdall
6. **Notification**: Alerts sent based on severity

## 📚 Additional Resources

- [Architecture Documentation](docs/HEIMDALL_ARCHITECTURE.md)
- [Implementation Guide](docs/HEIMDALL_IMPLEMENTATION_GUIDE.md)
- [API Reference](docs/API_REFERENCE.md) (TODO)
- [Deployment Guide](docs/DEPLOYMENT_GUIDE.md) (TODO)

## 🐛 Known Issues

- Elasticsearch integration is optional and not fully tested
- Redis caching layer is not yet implemented
- Notification service requires external SMTP/Slack configuration

## 🗺️ Roadmap

- [ ] Implement Redis caching layer
- [ ] Add gRPC support for high-performance ingestion
- [ ] Implement log aggregation and batching
- [ ] Add support for multiple notification channels
- [ ] Implement log retention policies
- [ ] Add Grafana dashboards
- [ ] Implement rate limiting per API key

## 📄 License

This project is part of the Bifrost ecosystem.

## 👥 Contributing

Contributions are welcome! Please read the contributing guidelines before submitting PRs.

---

**Built with ❤️ using Spring Boot and Kafka**

**Version**: 1.0.0  
**Last Updated**: 2024-11-11
