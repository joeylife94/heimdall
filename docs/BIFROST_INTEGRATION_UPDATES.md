# 🌈 Bifrost Integration Updates Guide

> **Heimdall 연동을 위한 Bifrost 프로젝트 업데이트 가이드**

---

## 📋 목차

1. [개요](#개요)
2. [Kafka 토픽 설정](#kafka-토픽-설정)
3. [이벤트 스키마 구현](#이벤트-스키마-구현)
4. [Kafka Consumer 구현](#kafka-consumer-구현)
5. [Kafka Producer 구현](#kafka-producer-구현)
6. [설정 파일 업데이트](#설정-파일-업데이트)
7. [API 엔드포인트 추가](#api-엔드포인트-추가)
8. [테스트 가이드](#테스트-가이드)

---

## 개요

Heimdall과의 통합을 위해 Bifrost 프로젝트에 다음 기능을 추가해야 합니다:

### 필수 구현 사항
- ✅ Kafka Consumer: `analysis.request` 토픽에서 분석 요청 수신
- ✅ Kafka Producer: `analysis.result` 토픽으로 분석 결과 발행
- ✅ 이벤트 스키마 정의 (JSON 기반)
- ✅ 에러 핸들링 및 DLQ(Dead Letter Queue) 처리
- ✅ 상태 관리 및 로깅

### 선택적 구현 사항
- 🔲 REST API: Heimdall이 직접 분석 요청할 수 있는 HTTP 엔드포인트
- 🔲 WebSocket: 실시간 분석 상태 업데이트
- 🔲 gRPC: 고성능 양방향 통신

---

## Kafka 토픽 설정

### 1. 필요한 토픽 목록

```bash
# Kafka 토픽 생성 스크립트
kafka-topics.sh --create --bootstrap-server localhost:9092 \
  --topic analysis.request \
  --partitions 3 \
  --replication-factor 2 \
  --config retention.ms=604800000

kafka-topics.sh --create --bootstrap-server localhost:9092 \
  --topic analysis.result \
  --partitions 3 \
  --replication-factor 2 \
  --config retention.ms=604800000

kafka-topics.sh --create --bootstrap-server localhost:9092 \
  --topic dlq.failed \
  --partitions 1 \
  --replication-factor 2 \
  --config retention.ms=2592000000
```

### 2. 토픽 설정 권장사항

| 토픽 | Partitions | Replication | Retention |
|------|------------|-------------|-----------|
| `analysis.request` | 3 | 2 | 7 days |
| `analysis.result` | 3 | 2 | 7 days |
| `dlq.failed` | 1 | 2 | 30 days |

---

## 이벤트 스키마 구현

### 1. 분석 요청 이벤트 (Bifrost가 수신)

**Topic**: `analysis.request`

**Python 모델**:
```python
from pydantic import BaseModel, Field
from typing import Dict, Any, Optional
from datetime import datetime
from enum import Enum

class AnalysisPriority(str, Enum):
    LOW = "LOW"
    NORMAL = "NORMAL"
    HIGH = "HIGH"
    CRITICAL = "CRITICAL"

class AnalysisRequestEvent(BaseModel):
    """Heimdall로부터 받는 분석 요청 이벤트"""
    
    request_id: str = Field(..., description="요청 고유 ID (UUID)")
    timestamp: datetime = Field(default_factory=datetime.utcnow)
    log_id: int = Field(..., description="Heimdall의 로그 ID")
    log_content: str = Field(..., description="분석할 로그 내용")
    service_name: str = Field(..., description="로그 출처 서비스명")
    environment: str = Field(..., description="환경 (dev/staging/prod)")
    analysis_type: str = Field(default="error", description="분석 유형")
    priority: AnalysisPriority = Field(default=AnalysisPriority.NORMAL)
    callback_topic: str = Field(default="analysis.result")
    correlation_id: str = Field(..., description="추적용 Correlation ID")
    metadata: Optional[Dict[str, Any]] = Field(default_factory=dict)
    
    class Config:
        json_schema_extra = {
            "example": {
                "request_id": "550e8400-e29b-41d4-a716-446655440000",
                "timestamp": "2024-11-12T10:30:00Z",
                "log_id": 12345,
                "log_content": "ERROR: Connection timeout to database",
                "service_name": "user-service",
                "environment": "production",
                "analysis_type": "error",
                "priority": "HIGH",
                "callback_topic": "analysis.result",
                "correlation_id": "corr-123456"
            }
        }
```

### 2. 분석 결과 이벤트 (Bifrost가 발행)

**Topic**: `analysis.result`

**Python 모델**:
```python
from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime
from decimal import Decimal

class AnalysisResultData(BaseModel):
    """AI 분석 결과 데이터"""
    summary: str = Field(..., description="분석 요약")
    root_cause: str = Field(..., description="근본 원인")
    recommendation: str = Field(..., description="해결 권장사항")
    severity: str = Field(..., description="심각도 (LOW/MEDIUM/HIGH/CRITICAL)")
    confidence: Decimal = Field(..., ge=0, le=1, description="신뢰도 (0~1)")

class AnalysisResultEvent(BaseModel):
    """Heimdall로 보내는 분석 결과 이벤트"""
    
    request_id: str = Field(..., description="원본 요청 ID")
    correlation_id: str = Field(..., description="Correlation ID")
    timestamp: datetime = Field(default_factory=datetime.utcnow)
    log_id: int = Field(..., description="Heimdall의 로그 ID")
    analysis_result: AnalysisResultData
    bifrost_analysis_id: int = Field(..., description="Bifrost 분석 ID")
    model: str = Field(..., description="사용된 AI 모델")
    duration_seconds: Decimal = Field(..., description="분석 소요 시간")
    
    class Config:
        json_schema_extra = {
            "example": {
                "request_id": "550e8400-e29b-41d4-a716-446655440000",
                "correlation_id": "corr-123456",
                "timestamp": "2024-11-12T10:30:15Z",
                "log_id": 12345,
                "analysis_result": {
                    "summary": "PostgreSQL 연결 타임아웃 발생",
                    "root_cause": "Connection pool 고갈로 인한 연결 대기 시간 초과",
                    "recommendation": "max_connections 설정 증가 및 connection pool 크기 조정 권장",
                    "severity": "HIGH",
                    "confidence": 0.95
                },
                "bifrost_analysis_id": 789,
                "model": "mistral-7b",
                "duration_seconds": 2.5
            }
        }
```

---

## Kafka Consumer 구현

### Python (aiokafka 사용)

```python
import asyncio
import json
from aiokafka import AIOKafkaConsumer
from typing import Optional
import logging

logger = logging.getLogger(__name__)

class AnalysisRequestConsumer:
    """분석 요청을 소비하는 Kafka Consumer"""
    
    def __init__(
        self,
        bootstrap_servers: str = "localhost:9092",
        group_id: str = "bifrost-consumer-group",
        topics: list = None
    ):
        self.bootstrap_servers = bootstrap_servers
        self.group_id = group_id
        self.topics = topics or ["analysis.request"]
        self.consumer: Optional[AIOKafkaConsumer] = None
        
    async def start(self):
        """Consumer 시작"""
        self.consumer = AIOKafkaConsumer(
            *self.topics,
            bootstrap_servers=self.bootstrap_servers,
            group_id=self.group_id,
            auto_offset_reset='earliest',
            enable_auto_commit=False,  # Manual commit
            value_deserializer=lambda m: json.loads(m.decode('utf-8'))
        )
        await self.consumer.start()
        logger.info(f"Kafka consumer started for topics: {self.topics}")
        
    async def stop(self):
        """Consumer 종료"""
        if self.consumer:
            await self.consumer.stop()
            logger.info("Kafka consumer stopped")
    
    async def consume_messages(self, processor_func):
        """
        메시지 소비 및 처리
        
        Args:
            processor_func: 메시지 처리 함수 (async)
        """
        try:
            async for message in self.consumer:
                try:
                    # 이벤트 파싱
                    event = AnalysisRequestEvent(**message.value)
                    logger.info(f"Received analysis request: {event.request_id}")
                    
                    # 비즈니스 로직 처리
                    await processor_func(event)
                    
                    # Manual commit
                    await self.consumer.commit()
                    
                except Exception as e:
                    logger.error(f"Error processing message: {e}", exc_info=True)
                    # DLQ로 전송 (구현 필요)
                    await self._send_to_dlq(message)
                    
        except Exception as e:
            logger.error(f"Consumer error: {e}", exc_info=True)
            raise
    
    async def _send_to_dlq(self, message):
        """실패한 메시지를 DLQ로 전송"""
        # DLQ Producer 구현 필요
        pass

# 사용 예시
async def process_analysis_request(event: AnalysisRequestEvent):
    """분석 요청 처리 로직"""
    logger.info(f"Processing log_id: {event.log_id}")
    
    # 1. AI 모델로 로그 분석
    analysis_result = await analyze_log_with_ai(event.log_content)
    
    # 2. 결과를 Kafka로 발행
    await send_analysis_result(event, analysis_result)

async def main():
    consumer = AnalysisRequestConsumer()
    await consumer.start()
    try:
        await consumer.consume_messages(process_analysis_request)
    finally:
        await consumer.stop()

if __name__ == "__main__":
    asyncio.run(main())
```

---

## Kafka Producer 구현

### Python (aiokafka 사용)

```python
import json
from aiokafka import AIOKafkaProducer
from typing import Optional
import logging

logger = logging.getLogger(__name__)

class AnalysisResultProducer:
    """분석 결과를 발행하는 Kafka Producer"""
    
    def __init__(self, bootstrap_servers: str = "localhost:9092"):
        self.bootstrap_servers = bootstrap_servers
        self.producer: Optional[AIOKafkaProducer] = None
        
    async def start(self):
        """Producer 시작"""
        self.producer = AIOKafkaProducer(
            bootstrap_servers=self.bootstrap_servers,
            value_serializer=lambda v: json.dumps(v, default=str).encode('utf-8'),
            acks='all',  # 모든 replica 확인
            retries=3,
            max_in_flight_requests_per_connection=1  # Ordering 보장
        )
        await self.producer.start()
        logger.info("Kafka producer started")
        
    async def stop(self):
        """Producer 종료"""
        if self.producer:
            await self.producer.stop()
            logger.info("Kafka producer stopped")
    
    async def send_analysis_result(
        self,
        result_event: AnalysisResultEvent,
        topic: str = "analysis.result"
    ) -> bool:
        """
        분석 결과 발행
        
        Args:
            result_event: 분석 결과 이벤트
            topic: 발행할 토픽
            
        Returns:
            성공 여부
        """
        try:
            # Pydantic 모델을 dict로 변환
            message = result_event.model_dump()
            
            # Key는 log_id로 설정 (파티셔닝)
            key = str(result_event.log_id).encode('utf-8')
            
            # 메시지 발행
            await self.producer.send_and_wait(topic, value=message, key=key)
            
            logger.info(
                f"Sent analysis result: request_id={result_event.request_id}, "
                f"log_id={result_event.log_id}"
            )
            return True
            
        except Exception as e:
            logger.error(f"Failed to send analysis result: {e}", exc_info=True)
            return False

# 사용 예시
async def send_analysis_result(
    request_event: AnalysisRequestEvent,
    analysis_data: AnalysisResultData
):
    """분석 결과 전송"""
    producer = AnalysisResultProducer()
    await producer.start()
    
    try:
        result_event = AnalysisResultEvent(
            request_id=request_event.request_id,
            correlation_id=request_event.correlation_id,
            log_id=request_event.log_id,
            analysis_result=analysis_data,
            bifrost_analysis_id=123456,  # DB에서 생성된 ID
            model="mistral-7b",
            duration_seconds=2.5
        )
        
        await producer.send_analysis_result(result_event)
        
    finally:
        await producer.stop()
```

---

## 설정 파일 업데이트

### config.yaml (Bifrost 설정)

```yaml
# Kafka 설정
kafka:
  bootstrap_servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
  consumer:
    group_id: bifrost-consumer-group
    auto_offset_reset: earliest
    enable_auto_commit: false
    max_poll_records: 100
    session_timeout_ms: 30000
  producer:
    acks: all
    retries: 3
    max_in_flight_requests_per_connection: 1
    compression_type: snappy
  topics:
    analysis_request: analysis.request
    analysis_result: analysis.result
    dlq: dlq.failed

# Heimdall 연동 설정
heimdall:
  enabled: true
  callback_topic: analysis.result
  timeout_seconds: 60
  retry_attempts: 3
  retry_backoff_seconds: 5

# AI 모델 설정
ai:
  model: mistral-7b
  max_tokens: 2048
  temperature: 0.7
  timeout: 30
```

### .env 파일

```env
# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# Database
DATABASE_URL=postgresql://bifrost:password@postgres:5432/bifrost

# AI Model
AI_MODEL=mistral-7b
AI_API_KEY=your-api-key-here

# Logging
LOG_LEVEL=INFO
```

---

## API 엔드포인트 추가

### FastAPI 엔드포인트 (선택적)

```python
from fastapi import APIRouter, HTTPException, BackgroundTasks
from pydantic import BaseModel

router = APIRouter(prefix="/api/v1/analysis", tags=["analysis"])

class AnalysisRequest(BaseModel):
    log_content: str
    service_name: str
    environment: str
    priority: str = "NORMAL"

class AnalysisResponse(BaseModel):
    analysis_id: int
    status: str
    message: str

@router.post("/request", response_model=AnalysisResponse)
async def request_analysis(
    request: AnalysisRequest,
    background_tasks: BackgroundTasks
):
    """
    Heimdall이 직접 호출할 수 있는 분석 요청 API
    (Kafka 대신 HTTP로 요청)
    """
    try:
        # 분석 작업을 백그라운드로 실행
        analysis_id = await create_analysis_job(request)
        background_tasks.add_task(process_analysis, analysis_id)
        
        return AnalysisResponse(
            analysis_id=analysis_id,
            status="ACCEPTED",
            message="Analysis request accepted"
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/result/{analysis_id}")
async def get_analysis_result(analysis_id: int):
    """분석 결과 조회"""
    result = await fetch_analysis_result(analysis_id)
    if not result:
        raise HTTPException(status_code=404, detail="Analysis not found")
    return result
```

---

## 테스트 가이드

### 1. Kafka 연동 테스트

```python
import pytest
import asyncio
from unittest.mock import AsyncMock, patch

@pytest.mark.asyncio
async def test_consume_analysis_request():
    """분석 요청 소비 테스트"""
    consumer = AnalysisRequestConsumer()
    
    # Mock processor
    mock_processor = AsyncMock()
    
    # Mock 메시지
    mock_message = {
        "request_id": "test-123",
        "log_id": 12345,
        "log_content": "ERROR: Test error",
        "service_name": "test-service",
        "environment": "test",
        "analysis_type": "error",
        "priority": "NORMAL",
        "callback_topic": "analysis.result",
        "correlation_id": "corr-123"
    }
    
    # 테스트 실행
    with patch.object(consumer, 'consumer') as mock_consumer:
        mock_consumer.__aiter__.return_value = [mock_message]
        await consumer.consume_messages(mock_processor)
        
    # 검증
    mock_processor.assert_called_once()

@pytest.mark.asyncio
async def test_send_analysis_result():
    """분석 결과 발행 테스트"""
    producer = AnalysisResultProducer()
    await producer.start()
    
    result_event = AnalysisResultEvent(
        request_id="test-123",
        correlation_id="corr-123",
        log_id=12345,
        analysis_result=AnalysisResultData(
            summary="Test summary",
            root_cause="Test cause",
            recommendation="Test recommendation",
            severity="LOW",
            confidence=0.9
        ),
        bifrost_analysis_id=789,
        model="test-model",
        duration_seconds=1.0
    )
    
    success = await producer.send_analysis_result(result_event)
    assert success is True
    
    await producer.stop()
```

### 2. 통합 테스트

```bash
# Docker Compose로 Kafka 환경 실행
docker-compose -f docker-compose.test.yml up -d

# Python 테스트 실행
pytest tests/integration/test_heimdall_integration.py -v

# Kafka 메시지 확인
kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic analysis.result --from-beginning
```

---

## 체크리스트

### 구현 완료 확인

- [ ] Kafka Consumer 구현 (`analysis.request` 구독)
- [ ] Kafka Producer 구현 (`analysis.result` 발행)
- [ ] 이벤트 스키마 정의 (Pydantic 모델)
- [ ] 에러 핸들링 및 DLQ 처리
- [ ] 로깅 및 모니터링 추가
- [ ] 단위 테스트 작성
- [ ] 통합 테스트 작성
- [ ] 설정 파일 업데이트
- [ ] 문서화 완료

### 배포 전 확인

- [ ] Kafka 토픽 생성 완료
- [ ] Heimdall과 네트워크 연결 확인
- [ ] 메시지 스키마 호환성 확인
- [ ] 성능 테스트 완료 (부하 테스트)
- [ ] 장애 시나리오 테스트 (Kafka 다운, 네트워크 끊김)
- [ ] 모니터링 대시보드 설정
- [ ] 알림 설정 (Consumer Lag, 에러율)

---

## 추가 참고 자료

- [Kafka Python Client (aiokafka)](https://aiokafka.readthedocs.io/)
- [Pydantic 문서](https://docs.pydantic.dev/)
- [FastAPI 백그라운드 작업](https://fastapi.tiangolo.com/tutorial/background-tasks/)
- [Kafka 베스트 프랙티스](https://kafka.apache.org/documentation/#bestpractices)

---

**문서 버전**: 1.0.0  
**최종 수정**: 2024-11-12  
**작성자**: Heimdall Development Team  
**문의**: 통합 관련 문의는 GitHub Issues로 등록해주세요.
