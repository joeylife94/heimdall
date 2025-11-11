# Heimdall 트러블슈팅 가이드

일반적인 문제와 해결 방법을 정리한 가이드입니다.

## 📋 목차

1. [애플리케이션 시작 문제](#애플리케이션-시작-문제)
2. [데이터베이스 연결 문제](#데이터베이스-연결-문제)
3. [Kafka 연결 문제](#kafka-연결-문제)
4. [성능 문제](#성능-문제)
5. [메모리 문제](#메모리-문제)
6. [Kubernetes 배포 문제](#kubernetes-배포-문제)
7. [모니터링 문제](#모니터링-문제)

---

## 애플리케이션 시작 문제

### 증상: "Port 8080 is already in use"

**원인**: 포트가 이미 사용 중

**해결방법**:
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9

# 또는 다른 포트 사용
java -jar heimdall.jar --server.port=8081
```

### 증상: "Failed to configure a DataSource"

**원인**: 데이터베이스 설정 누락 또는 잘못됨

**해결방법**:
1. `application.yml` 확인:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/heimdall
    username: heimdall
    password: your-password
```

2. PostgreSQL이 실행 중인지 확인:
```bash
docker ps | grep postgres
```

3. 연결 테스트:
```bash
psql -h localhost -U heimdall -d heimdall
```

### 증상: "Bean creation exception"

**원인**: 의존성 주입 실패 또는 설정 오류

**해결방법**:
1. 스택 트레이스 확인:
```bash
tail -f logs/heimdall.log
```

2. 순환 의존성 체크:
```java
// 생성자 주입 사용 (권장)
@Service
public class MyService {
    private final OtherService otherService;
    
    public MyService(OtherService otherService) {
        this.otherService = otherService;
    }
}
```

3. 컴포넌트 스캔 경로 확인:
```java
@SpringBootApplication(scanBasePackages = "com.heimdall")
public class HeimdallApplication { }
```

---

## 데이터베이스 연결 문제

### 증상: "Connection refused"

**원인**: PostgreSQL이 시작되지 않았거나 잘못된 호스트/포트

**해결방법**:
```bash
# Docker로 PostgreSQL 시작
docker run -d --name heimdall-postgres \
  -e POSTGRES_DB=heimdall \
  -e POSTGRES_USER=heimdall \
  -e POSTGRES_PASSWORD=heimdall123 \
  -p 5432:5432 \
  postgres:16-alpine

# 연결 확인
docker exec -it heimdall-postgres psql -U heimdall -d heimdall
```

### 증상: "Too many connections"

**원인**: 커넥션 풀 설정 문제

**해결방법**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

PostgreSQL 최대 연결 수 확인:
```sql
SHOW max_connections;
SELECT count(*) FROM pg_stat_activity;
```

### 증상: "Slow queries"

**원인**: 인덱스 부족 또는 비효율적 쿼리

**해결방법**:
1. 느린 쿼리 로그 활성화:
```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

2. EXPLAIN ANALYZE 사용:
```sql
EXPLAIN ANALYZE SELECT * FROM log_entries WHERE event_id = 'xxx';
```

3. 인덱스 추가:
```sql
CREATE INDEX CONCURRENTLY idx_log_entries_timestamp ON log_entries(timestamp DESC);
```

---

## Kafka 연결 문제

### 증상: "Connection to node -1 could not be established"

**원인**: Kafka 브로커에 연결할 수 없음

**해결방법**:
```bash
# Kafka가 실행 중인지 확인
docker ps | grep kafka

# Kafka 재시작
docker-compose restart kafka

# 연결 테스트
kafka-broker-api-versions.sh --bootstrap-server localhost:9092
```

### 증상: "Topic does not exist"

**원인**: 필요한 토픽이 생성되지 않음

**해결방법**:
```bash
# 토픽 목록 확인
kafka-topics.sh --bootstrap-server localhost:9092 --list

# 토픽 생성
kafka-topics.sh --bootstrap-server localhost:9092 \
  --create --topic log-events \
  --partitions 3 --replication-factor 1

kafka-topics.sh --bootstrap-server localhost:9092 \
  --create --topic log-analysis-requests \
  --partitions 3 --replication-factor 1

kafka-topics.sh --bootstrap-server localhost:9092 \
  --create --topic log-analysis-results \
  --partitions 3 --replication-factor 1
```

### 증상: "Consumer lag is high"

**원인**: 컨슈머가 메시지를 빠르게 처리하지 못함

**해결방법**:
1. Consumer Lag 확인:
```bash
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --describe --group heimdall-consumer-group
```

2. 컨슈머 설정 최적화:
```yaml
spring:
  kafka:
    consumer:
      max-poll-records: 500
      fetch-min-size: 1024
      fetch-max-wait-ms: 500
    listener:
      concurrency: 3
```

3. 파티션 수 증가:
```bash
kafka-topics.sh --bootstrap-server localhost:9092 \
  --alter --topic log-events --partitions 6
```

---

## 성능 문제

### 증상: "High response time"

**원인**: 데이터베이스 쿼리, 네트워크, CPU 등

**진단**:
```bash
# 메트릭 확인
curl http://localhost:8080/actuator/prometheus | grep http_server_requests

# 느린 엔드포인트 식별
curl http://localhost:8080/actuator/metrics/http.server.requests
```

**해결방법**:
1. 데이터베이스 쿼리 최적화
2. 캐싱 추가:
```java
@Cacheable(value = "logs", key = "#eventId")
public LogEntry findByEventId(String eventId) {
    return repository.findByEventId(eventId);
}
```

3. 비동기 처리:
```java
@Async
public CompletableFuture<Void> processLogAsync(LogEntry entry) {
    // 비동기 처리
}
```

### 증상: "High CPU usage"

**원인**: 과도한 연산, 무한 루프, GC

**진단**:
```bash
# CPU 사용률 확인
docker stats heimdall

# JVM 스레드 덤프
jstack <PID> > thread-dump.txt
```

**해결방법**:
1. 프로파일링 도구 사용 (JProfiler, VisualVM)
2. 병렬 처리 최적화
3. 알고리즘 개선

### 증상: "Database connection pool exhausted"

**원인**: 너무 많은 동시 요청

**해결방법**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # 증가
      connection-timeout: 30000
      leak-detection-threshold: 60000  # 리크 감지
```

---

## 메모리 문제

### 증상: "OutOfMemoryError: Java heap space"

**원인**: Heap 메모리 부족

**해결방법**:
```bash
# Heap 크기 증가
java -Xms512m -Xmx2g -jar heimdall.jar

# Kubernetes에서:
# deployment.yaml
resources:
  limits:
    memory: 2Gi
  requests:
    memory: 1Gi
```

### 증상: "OutOfMemoryError: Metaspace"

**원인**: Metaspace (클래스 메타데이터) 부족

**해결방법**:
```bash
java -XX:MaxMetaspaceSize=256m -jar heimdall.jar
```

### 증상: "Memory leak detected"

**원인**: 객체가 GC되지 않음

**진단**:
```bash
# Heap 덤프 생성
jmap -dump:live,format=b,file=heap-dump.hprof <PID>

# MAT (Memory Analyzer Tool)로 분석
```

**해결방법**:
1. 리소스 정리 확인 (Connection, Stream 등)
2. 캐시 만료 설정
3. 약한 참조 사용

---

## Kubernetes 배포 문제

### 증상: "CrashLoopBackOff"

**원인**: 컨테이너가 계속 재시작됨

**진단**:
```bash
kubectl get pods -n production
kubectl describe pod <pod-name> -n production
kubectl logs <pod-name> -n production --previous
```

**해결방법**:
1. Liveness/Readiness 프로브 확인:
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 60  # 충분한 시간 부여
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 5
```

2. 리소스 제한 확인:
```yaml
resources:
  limits:
    cpu: 1000m
    memory: 1Gi
  requests:
    cpu: 500m
    memory: 512Mi
```

### 증상: "ImagePullBackOff"

**원인**: 이미지를 풀할 수 없음

**해결방법**:
```bash
# 이미지 존재 확인
docker images | grep heimdall

# 레지스트리 인증
kubectl create secret docker-registry regcred \
  --docker-server=ghcr.io \
  --docker-username=<username> \
  --docker-password=<token> \
  -n production

# Deployment에 추가
spec:
  imagePullSecrets:
  - name: regcred
```

### 증상: "Service Unavailable (503)"

**원인**: Pod가 준비되지 않았거나 서비스 설정 문제

**진단**:
```bash
kubectl get pods -n production
kubectl get svc -n production
kubectl get endpoints -n production
```

**해결방법**:
1. Pod 상태 확인
2. 서비스 셀렉터 확인:
```yaml
selector:
  app: heimdall
```

3. 네트워크 정책 확인

---

## 모니터링 문제

### 증상: "Prometheus metrics not available"

**원인**: Actuator 또는 Micrometer 설정 문제

**해결방법**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true
```

### 증상: "Grafana dashboard shows no data"

**원인**: Prometheus 데이터소스 설정 또는 쿼리 문제

**해결방법**:
1. Prometheus 연결 확인
2. 쿼리 테스트:
```promql
up{job="heimdall"}
rate(http_server_requests_seconds_count[5m])
```

3. Scrape 설정 확인:
```yaml
scrape_configs:
  - job_name: 'heimdall'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['heimdall:8080']
```

### 증상: "Alert not triggering"

**원인**: Alert 규칙 또는 Alertmanager 설정 문제

**해결방법**:
1. Alert 규칙 확인:
```bash
promtool check rules alerts/*.yml
```

2. Alertmanager 로그 확인:
```bash
kubectl logs -f alertmanager-xxx -n monitoring
```

---

## 일반적인 디버깅 명령어

### 애플리케이션 로그
```bash
# Docker
docker logs -f heimdall

# Kubernetes
kubectl logs -f <pod-name> -n production

# 파일 로그
tail -f logs/heimdall.log
```

### 네트워크 연결 테스트
```bash
# 포트 확인
telnet localhost 8080

# HTTP 요청
curl -v http://localhost:8080/actuator/health

# DNS 확인
nslookup postgres
```

### 데이터베이스 연결 테스트
```bash
# PostgreSQL
psql -h localhost -U heimdall -d heimdall -c "SELECT version();"

# 연결 수 확인
psql -h localhost -U heimdall -d heimdall -c "SELECT count(*) FROM pg_stat_activity;"
```

### Kafka 디버깅
```bash
# 토픽 메시지 확인
kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic log-events --from-beginning

# Consumer Group 상태
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --describe --group heimdall-consumer-group
```

---

## 추가 도움말

문제가 해결되지 않으면:
1. GitHub Issues에 문제 등록
2. 로그 파일 첨부
3. 환경 정보 제공 (OS, Java 버전, Kubernetes 버전 등)

**긴급 지원**: support@example.com
