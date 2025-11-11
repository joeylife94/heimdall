# Heimdall - Quick Start Guide

## Prerequisites Check

Before starting, ensure you have:

- [ ] Java 17 or higher installed (`java -version`)
- [ ] Docker and Docker Compose installed (`docker --version`)
- [ ] At least 4GB of free RAM
- [ ] Ports available: 5432, 9092, 9200, 6379, 8080

## 🚀 Quick Start (5 minutes)

### Option 1: Using Docker Compose (Recommended)

```bash
# 1. Start all services
cd docker
docker-compose up -d

# 2. Verify services are running
docker-compose ps

# 3. Check Heimdall health
curl http://localhost:8080/actuator/health

# 4. Test log ingestion
curl -X POST http://localhost:8080/api/v1/logs \
  -H "Content-Type: application/json" \
  -d '{
    "source": "test",
    "serviceName": "test-service",
    "environment": "dev",
    "severity": "INFO",
    "logContent": "This is a test log"
  }'
```

### Option 2: Local Development

```bash
# 1. Start infrastructure
cd docker
docker-compose up -d postgres kafka zookeeper elasticsearch redis

# 2. Build the application
./gradlew clean build

# 3. Run the application
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Option 3: Using Helper Scripts

**Linux/Mac:**
```bash
chmod +x run.sh
./run.sh
# Choose option 5 for full setup
```

**Windows:**
```cmd
run.bat
REM Choose option 5 for full setup
```

## 📊 Verify Installation

### 1. Check Service Health

```bash
# Application health
curl http://localhost:8080/actuator/health

# Expected response: {"status":"UP"}
```

### 2. Check Database Connection

```bash
docker exec -it heimdall-postgres psql -U heimdall_dev -d heimdall_dev -c "\dt"
```

### 3. Check Kafka Topics

```bash
docker exec -it heimdall-kafka kafka-topics --list --bootstrap-server localhost:9092
```

### 4. Check Elasticsearch

```bash
curl http://localhost:9200/_cluster/health
```

## 🧪 Test the API

### Ingest a Log

```bash
curl -X POST http://localhost:8080/api/v1/logs \
  -H "Content-Type: application/json" \
  -d '{
    "source": "k8s-prod",
    "serviceName": "user-service",
    "environment": "production",
    "severity": "ERROR",
    "logContent": "ERROR: Database connection timeout",
    "metadata": {
      "podName": "user-service-abc123",
      "namespace": "production"
    }
  }'
```

### Search Logs

```bash
curl "http://localhost:8080/api/v1/logs/search?serviceName=user-service&environment=production"
```

### View Metrics

```bash
curl http://localhost:8080/actuator/prometheus | grep logs_ingested_total
```

## 🐛 Troubleshooting

### Port Already in Use

```bash
# Check what's using the port
netstat -ano | findstr :8080  # Windows
lsof -i :8080                 # Linux/Mac

# Change Heimdall port
export SERVER_PORT=8081
./gradlew bootRun
```

### Database Connection Failed

```bash
# Verify PostgreSQL is running
docker ps | grep postgres

# Check logs
docker logs heimdall-postgres

# Restart PostgreSQL
docker-compose restart postgres
```

### Kafka Connection Issues

```bash
# Check Kafka logs
docker logs heimdall-kafka

# Verify Zookeeper is running
docker ps | grep zookeeper

# Restart Kafka stack
docker-compose restart zookeeper kafka
```

### Out of Memory

```bash
# Increase Docker memory limit (Docker Desktop)
# Settings → Resources → Memory → Increase to 4GB+

# Or reduce service memory
# Edit docker-compose.yml and reduce memory limits
```

## 📚 Next Steps

1. **Review Architecture**: Read `docs/HEIMDALL_ARCHITECTURE.md`
2. **Integrate with Bifrost**: Set up the AI analysis companion service
3. **Configure Monitoring**: Set up Prometheus and Grafana
4. **Production Setup**: Follow `docs/DEPLOYMENT_GUIDE.md`

## 🆘 Getting Help

- Check logs: `docker logs heimdall-app`
- Review issues: GitHub Issues
- Read docs: `docs/` directory

## 🎉 Success!

If you see logs being ingested and can query them back, congratulations! 🛡️ Heimdall is running successfully.
