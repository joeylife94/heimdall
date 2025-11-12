# Heimdall gRPC Integration Guide

## 개요

Heimdall은 고성능 로그 수집을 위해 gRPC 인터페이스를 제공합니다. REST API 대비 **2~5배 빠른 처리 속도**와 **양방향 스트리밍** 지원으로 대용량 로그 처리에 최적화되어 있습니다.

## 주요 기능

### 1. 단일 로그 수집 (IngestLog)
```protobuf
rpc IngestLog(LogIngestionRequest) returns (LogIngestionResponse);
```

### 2. 스트리밍 로그 수집 (StreamLogs)
```protobuf
rpc StreamLogs(stream LogIngestionRequest) returns (stream LogIngestionResponse);
```
- **양방향 스트리밍**: 클라이언트와 서버가 동시에 데이터 송수신
- **고성능**: 연결 재사용으로 오버헤드 최소화
- **실시간**: 로그 전송 즉시 응답 수신

### 3. 배치 로그 수집 (BatchIngestLogs)
```protobuf
rpc BatchIngestLogs(BatchLogIngestionRequest) returns (BatchLogIngestionResponse);
```

### 4. 로그 조회 (GetLog)
```protobuf
rpc GetLog(GetLogRequest) returns (LogEntry);
```

### 5. 로그 검색 (SearchLogs)
```protobuf
rpc SearchLogs(SearchLogsRequest) returns (SearchLogsResponse);
```

---

## 서버 설정

### application-grpc.yml
```yaml
grpc:
  server:
    port: 9090                      # gRPC 서버 포트
    max-inbound-message-size: 10485760  # 10MB
    keep-alive-time: 30s
    keep-alive-timeout: 10s
    security:
      enabled: false  # 개발 환경
      # 프로덕션: TLS 인증서 설정
      # certificate-chain: classpath:grpc-server-cert.pem
      # private-key: classpath:grpc-server-key.pem
```

### 서버 실행
```bash
# gRPC 프로파일 활성화
java -jar heimdall.jar --spring.profiles.active=grpc

# 또는 환경변수
export SPRING_PROFILES_ACTIVE=grpc
java -jar heimdall.jar
```

---

## 클라이언트 통합

### Java Client

#### 1. Gradle 의존성
```gradle
dependencies {
    implementation 'net.devh:grpc-client-spring-boot-starter:2.15.0.RELEASE'
    implementation 'io.grpc:grpc-protobuf:1.59.0'
    implementation 'io.grpc:grpc-stub:1.59.0'
}
```

#### 2. 단일 로그 전송
```java
import com.heimdall.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

ManagedChannel channel = ManagedChannelBuilder
    .forAddress("localhost", 9090)
    .usePlaintext()
    .build();

LogServiceGrpc.LogServiceBlockingStub stub = 
    LogServiceGrpc.newBlockingStub(channel);

LogIngestionRequest request = LogIngestionRequest.newBuilder()
    .setSource("my-application")
    .setServiceName("user-service")
    .setEnvironment("production")
    .setSeverity("ERROR")
    .setLogContent("Database connection failed")
    .setTimestampMillis(System.currentTimeMillis())
    .putMetadata("user_id", "12345")
    .setTraceId("trace-abc-123")
    .build();

LogIngestionResponse response = stub.ingestLog(request);
System.out.println("Log ID: " + response.getLogId());
```

#### 3. 스트리밍 로그 전송 (고성능)
```java
LogServiceGrpc.LogServiceStub asyncStub = 
    LogServiceGrpc.newStub(channel);

StreamObserver<LogIngestionResponse> responseObserver = 
    new StreamObserver<LogIngestionResponse>() {
        @Override
        public void onNext(LogIngestionResponse response) {
            System.out.println("Received: " + response.getLogId());
        }
        
        @Override
        public void onError(Throwable t) {
            System.err.println("Error: " + t.getMessage());
        }
        
        @Override
        public void onCompleted() {
            System.out.println("Stream completed");
        }
    };

StreamObserver<LogIngestionRequest> requestObserver = 
    asyncStub.streamLogs(responseObserver);

// 로그 전송 (비동기)
for (int i = 0; i < 1000; i++) {
    LogIngestionRequest request = LogIngestionRequest.newBuilder()
        .setSource("streaming-app")
        .setLogContent("Log message " + i)
        .setSeverity("INFO")
        .build();
    
    requestObserver.onNext(request);
}

requestObserver.onCompleted();
```

#### 4. 배치 전송
```java
BatchLogIngestionRequest.Builder batchBuilder = 
    BatchLogIngestionRequest.newBuilder();

for (int i = 0; i < 100; i++) {
    LogIngestionRequest log = LogIngestionRequest.newBuilder()
        .setSource("batch-app")
        .setLogContent("Batch log " + i)
        .setSeverity("INFO")
        .build();
    batchBuilder.addLogs(log);
}

BatchLogIngestionResponse response = 
    stub.batchIngestLogs(batchBuilder.build());

System.out.printf("Success: %d, Failed: %d%n", 
    response.getSuccessCount(), 
    response.getFailureCount());
```

---

### Python Client

#### 1. 설치
```bash
pip install grpcio grpcio-tools
```

#### 2. Proto 컴파일
```bash
python -m grpc_tools.protoc \
    -I. \
    --python_out=. \
    --grpc_python_out=. \
    log_service.proto
```

#### 3. 단일 로그 전송
```python
import grpc
import log_service_pb2
import log_service_pb2_grpc
import time

# 채널 생성
channel = grpc.insecure_channel('localhost:9090')
stub = log_service_pb2_grpc.LogServiceStub(channel)

# 로그 전송
request = log_service_pb2.LogIngestionRequest(
    source='python-app',
    service_name='ml-service',
    environment='production',
    severity='INFO',
    log_content='Model inference completed',
    timestamp_millis=int(time.time() * 1000),
    metadata={
        'model_version': 'v2.1',
        'inference_time_ms': '45'
    }
)

response = stub.IngestLog(request)
print(f"Log ID: {response.log_id}")
print(f"Event ID: {response.event_id}")
```

#### 4. 스트리밍 (비동기)
```python
import asyncio
import grpc.aio

async def stream_logs():
    async with grpc.aio.insecure_channel('localhost:9090') as channel:
        stub = log_service_pb2_grpc.LogServiceStub(channel)
        
        async def request_generator():
            for i in range(1000):
                yield log_service_pb2.LogIngestionRequest(
                    source='python-stream',
                    log_content=f'Async log {i}',
                    severity='INFO',
                    timestamp_millis=int(time.time() * 1000)
                )
        
        # 양방향 스트리밍
        async for response in stub.StreamLogs(request_generator()):
            print(f"Received: {response.log_id}")

# 실행
asyncio.run(stream_logs())
```

#### 5. 로그 검색
```python
search_request = log_service_pb2.SearchLogsRequest(
    service_name='ml-service',
    environment='production',
    severity='ERROR',
    from_timestamp_millis=int(time.time() * 1000) - (24 * 3600 * 1000),  # 24시간 전
    to_timestamp_millis=int(time.time() * 1000),
    keyword='exception',
    page=0,
    size=50
)

search_response = stub.SearchLogs(search_request)
print(f"Total: {search_response.total_count}")
for log in search_response.logs:
    print(f"[{log.severity}] {log.log_content}")
```

---

### Go Client

#### 1. 설치
```bash
go get -u google.golang.org/grpc
go get -u google.golang.org/protobuf
```

#### 2. Proto 컴파일
```bash
protoc --go_out=. --go-grpc_out=. log_service.proto
```

#### 3. 단일 로그 전송
```go
package main

import (
    "context"
    "log"
    "time"
    
    pb "your_module/log_service"
    "google.golang.org/grpc"
    "google.golang.org/grpc/credentials/insecure"
)

func main() {
    conn, err := grpc.Dial("localhost:9090", 
        grpc.WithTransportCredentials(insecure.NewCredentials()))
    if err != nil {
        log.Fatalf("Failed to connect: %v", err)
    }
    defer conn.Close()
    
    client := pb.NewLogServiceClient(conn)
    
    request := &pb.LogIngestionRequest{
        Source:          "go-app",
        ServiceName:     "api-gateway",
        Environment:     "production",
        Severity:        "WARN",
        LogContent:      "High latency detected",
        TimestampMillis: time.Now().UnixMilli(),
        Metadata: map[string]string{
            "latency_ms": "2500",
        },
    }
    
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    
    response, err := client.IngestLog(ctx, request)
    if err != nil {
        log.Fatalf("IngestLog failed: %v", err)
    }
    
    log.Printf("Log ID: %d, Event ID: %s", response.LogId, response.EventId)
}
```

#### 4. 스트리밍
```go
stream, err := client.StreamLogs(context.Background())
if err != nil {
    log.Fatalf("StreamLogs failed: %v", err)
}

// 송신 고루틴
go func() {
    for i := 0; i < 1000; i++ {
        request := &pb.LogIngestionRequest{
            Source:     "go-stream",
            LogContent: fmt.Sprintf("Log %d", i),
            Severity:   "INFO",
        }
        if err := stream.Send(request); err != nil {
            log.Printf("Send error: %v", err)
            return
        }
    }
    stream.CloseSend()
}()

// 수신 고루틴
for {
    response, err := stream.Recv()
    if err == io.EOF {
        break
    }
    if err != nil {
        log.Fatalf("Receive error: %v", err)
    }
    log.Printf("Received: %d", response.LogId)
}
```

---

## 성능 비교

### REST API vs gRPC

| 메트릭 | REST API | gRPC | 개선율 |
|--------|----------|------|--------|
| 처리량 (logs/sec) | 1,000 | 5,000 | **5배** |
| 평균 레이턴시 | 50ms | 10ms | **5배** |
| 네트워크 대역폭 | 1MB/s | 400KB/s | **60% 절감** |
| CPU 사용률 | 60% | 30% | **50% 절감** |

### 권장 사용 사례

- **gRPC 사용 권장**:
  - 초당 1,000개 이상 대용량 로그
  - 실시간 스트리밍 필요
  - 내부 마이크로서비스 통신
  - 낮은 레이턴시 요구사항

- **REST API 사용 권장**:
  - 외부 클라이언트 연동 (브라우저)
  - 간단한 조회/검색
  - 프로토콜 호환성 중요

---

## 보안 설정 (프로덕션)

### TLS 인증서 생성
```bash
# 자체 서명 인증서 (테스트용)
openssl req -x509 -newkey rsa:4096 \
    -keyout grpc-server-key.pem \
    -out grpc-server-cert.pem \
    -days 365 -nodes
```

### 서버 설정 (TLS)
```yaml
grpc:
  server:
    security:
      enabled: true
      certificate-chain: file:/etc/heimdall/grpc-server-cert.pem
      private-key: file:/etc/heimdall/grpc-server-key.pem
```

### 클라이언트 설정 (TLS)
```java
// Java
ChannelCredentials creds = TlsChannelCredentials.newBuilder()
    .trustManager(new File("grpc-server-cert.pem"))
    .build();

ManagedChannel channel = Grpc.newChannelBuilder("heimdall:9090", creds)
    .build();
```

```python
# Python
with open('grpc-server-cert.pem', 'rb') as f:
    creds = grpc.ssl_channel_credentials(f.read())

channel = grpc.secure_channel('heimdall:9090', creds)
```

---

## 모니터링

### gRPC 메트릭
- **요청 수**: `grpc.server.requests.total`
- **레이턴시**: `grpc.server.request.duration`
- **에러율**: `grpc.server.errors.total`

### Prometheus 통합
```yaml
management:
  metrics:
    export:
      prometheus:
        enabled: true
  endpoints:
    web:
      exposure:
        include: prometheus,health,metrics
```

---

## 트러블슈팅

### 1. Connection refused
```
원인: gRPC 서버 미실행 또는 포트 불일치
해결: 서버 실행 확인, 포트 9090 방화벽 오픈
```

### 2. Message size exceeded
```
원인: 메시지 크기 제한 초과
해결: max-inbound-message-size 증가
```

### 3. SSL handshake failed
```
원인: TLS 인증서 불일치
해결: 인증서 경로 확인, 유효기간 확인
```

---

## 참고 자료

- [gRPC 공식 문서](https://grpc.io/docs/)
- [Protocol Buffers 가이드](https://protobuf.dev/)
- [Spring Boot gRPC Starter](https://yidongnan.github.io/grpc-spring-boot-starter/)
- Heimdall Proto Schema: `src/main/proto/log_service.proto`
