# Heimdall 개발 가이드

Heimdall 프로젝트에 기여하고 개발하기 위한 종합 가이드입니다.

## 📋 목차

1. [개발 환경 설정](#개발-환경-설정)
2. [프로젝트 구조](#프로젝트-구조)
3. [코딩 규칙](#코딩-규칙)
4. [테스트 가이드](#테스트-가이드)
5. [디버깅](#디버깅)
6. [성능 최적화](#성능-최적화)
7. [기여 방법](#기여-방법)

---

## 개발 환경 설정

### 필수 요구사항

- **JDK**: 17 이상
- **Gradle**: 8.x 이상
- **Docker**: 20.10 이상
- **Kubernetes**: 1.25 이상 (프로덕션 배포용)
- **IDE**: IntelliJ IDEA 또는 Eclipse (권장: IntelliJ IDEA)

### 로컬 개발 환경 구성

1. **저장소 클론**
```bash
git clone <repository-url>
cd heimdall
```

2. **의존성 설치**
```bash
./gradlew clean build
```

3. **로컬 서비스 시작 (Docker Compose)**
```bash
cd docker
docker-compose up -d
```

이렇게 하면 다음 서비스가 시작됩니다:
- PostgreSQL (localhost:5432)
- Kafka (localhost:9092)
- Elasticsearch (localhost:9200)
- Redis (localhost:6379)

4. **애플리케이션 실행**
```bash
# 개발 프로파일로 실행
./gradlew bootRun --args='--spring.profiles.active=dev'

# 또는 IDE에서 HeimdallApplication 실행
```

5. **접속 확인**
- API: http://localhost:8080/api/v1/logs
- Health: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/prometheus

---

## 프로젝트 구조

```
heimdall/
├── src/
│   ├── main/
│   │   ├── java/com/heimdall/
│   │   │   ├── config/           # 설정 클래스
│   │   │   ├── controller/       # REST 컨트롤러
│   │   │   ├── dto/              # 데이터 전송 객체
│   │   │   ├── entity/           # JPA 엔티티
│   │   │   ├── exception/        # 예외 처리
│   │   │   ├── kafka/            # Kafka 이벤트/리스너
│   │   │   ├── repository/       # 데이터 접근 계층
│   │   │   ├── security/         # 보안 설정
│   │   │   ├── service/          # 비즈니스 로직
│   │   │   └── util/             # 유틸리티 클래스
│   │   └── resources/
│   │       ├── application.yml   # 기본 설정
│   │       ├── application-dev.yml   # 개발 환경
│   │       └── application-prod.yml  # 프로덕션 환경
│   └── test/
│       └── java/com/heimdall/
│           ├── integration/      # 통합 테스트
│           ├── service/          # 서비스 테스트
│           └── controller/       # 컨트롤러 테스트
├── docker/                       # Docker 관련 파일
├── k8s/                          # Kubernetes 매니페스트
├── monitoring/                   # 모니터링 설정
├── scripts/                      # 유틸리티 스크립트
└── docs/                         # 문서
```

### 주요 컴포넌트

#### 1. Controller Layer
- **역할**: REST API 엔드포인트 제공
- **위치**: `src/main/java/com/heimdall/controller/`
- **규칙**:
  - `@RestController` 어노테이션 사용
  - API 버전 포함 (예: `/api/v1/`)
  - DTO를 사용한 요청/응답 처리
  - `@Valid`로 입력 검증

#### 2. Service Layer
- **역할**: 비즈니스 로직 구현
- **위치**: `src/main/java/com/heimdall/service/`
- **규칙**:
  - `@Service` 어노테이션 사용
  - 트랜잭션 관리 (`@Transactional`)
  - 비즈니스 예외 처리

#### 3. Repository Layer
- **역할**: 데이터베이스 접근
- **위치**: `src/main/java/com/heimdall/repository/`
- **규칙**:
  - Spring Data JPA 사용
  - 커스텀 쿼리는 `@Query` 활용
  - 페이징/정렬 지원

#### 4. Kafka Integration
- **역할**: 이벤트 기반 통신
- **위치**: `src/main/java/com/heimdall/kafka/`
- **규칙**:
  - 이벤트 클래스는 불변 객체로 설계
  - 리스너는 멱등성 보장
  - 에러 핸들링 필수

---

## 코딩 규칙

### Java 코드 스타일

1. **네이밍 컨벤션**
   - 클래스: PascalCase (`LogEntry`, `LogService`)
   - 메서드/변수: camelCase (`processLog`, `eventId`)
   - 상수: UPPER_SNAKE_CASE (`MAX_RETRY_COUNT`)
   - 패키지: lowercase (`com.heimdall.service`)

2. **클래스 구조**
```java
public class ExampleService {
    // 1. 상수
    private static final int MAX_SIZE = 100;
    
    // 2. 필드 (final 우선)
    private final ExampleRepository repository;
    
    // 3. 생성자
    public ExampleService(ExampleRepository repository) {
        this.repository = repository;
    }
    
    // 4. public 메서드
    public void publicMethod() { }
    
    // 5. protected/package-private 메서드
    protected void protectedMethod() { }
    
    // 6. private 메서드
    private void privateMethod() { }
}
```

3. **어노테이션 순서**
```java
@Service
@Transactional(readOnly = true)
@Slf4j
public class ExampleService {
    // ...
}
```

4. **로깅**
```java
// Lombok의 @Slf4j 사용
log.info("Processing log entry: eventId={}", eventId);
log.error("Failed to process log: eventId={}", eventId, exception);

// 성능에 민감한 경우 조건 체크
if (log.isDebugEnabled()) {
    log.debug("Detailed debug info: {}", expensiveOperation());
}
```

### 예외 처리

```java
// 커스텀 예외 사용
throw new LogProcessingException("Failed to process log", eventId);

// 체크 예외는 언체크 예외로 래핑
try {
    // ...
} catch (IOException e) {
    throw new HeimdallException("IO error occurred", e);
}
```

### 트랜잭션 관리

```java
@Transactional  // 쓰기 작업
public void updateLog(String eventId) {
    // ...
}

@Transactional(readOnly = true)  // 읽기 전용
public LogEntry findLog(String eventId) {
    // ...
}
```

---

## 테스트 가이드

### 테스트 피라미드

```
       E2E Tests (5%)
      /             \
   Integration Tests (15%)
  /                         \
Unit Tests (80%)
```

### 단위 테스트 (Unit Tests)

**위치**: `src/test/java/com/heimdall/service/`

```java
@ExtendWith(MockitoExtension.class)
class LogServiceTest {
    
    @Mock
    private LogRepository repository;
    
    @InjectMocks
    private LogService service;
    
    @Test
    @DisplayName("로그 저장 - 성공")
    void testSaveLog_Success() {
        // Given
        LogEntry entry = createTestLogEntry();
        when(repository.save(any())).thenReturn(entry);
        
        // When
        LogEntry result = service.saveLog(entry);
        
        // Then
        assertNotNull(result);
        verify(repository).save(any());
    }
}
```

### 통합 테스트 (Integration Tests)

**위치**: `src/test/java/com/heimdall/integration/`

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LogControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testCreateLog_Success() throws Exception {
        mockMvc.perform(post("/api/v1/logs")
            .contentType(MediaType.APPLICATION_JSON)
            .content(testJson))
            .andExpect(status().isCreated());
    }
}
```

### 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 특정 테스트 클래스
./gradlew test --tests LogServiceTest

# 통합 테스트만
./gradlew test --tests *IntegrationTest

# 커버리지 리포트 생성
./gradlew test jacocoTestReport
```

---

## 디버깅

### IDE 디버깅 설정

**IntelliJ IDEA:**
1. Run → Edit Configurations
2. Application 추가
3. Main class: `com.heimdall.HeimdallApplication`
4. VM options: `-Dspring.profiles.active=dev`
5. Environment variables: (필요시 설정)

### 로그 레벨 조정

`application-dev.yml`:
```yaml
logging:
  level:
    com.heimdall: DEBUG
    org.springframework.kafka: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### 원격 디버깅

```bash
# 원격 디버깅 활성화로 애플리케이션 실행
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar heimdall.jar
```

IntelliJ에서:
1. Run → Edit Configurations
2. Remote JVM Debug 추가
3. Host: localhost, Port: 5005

---

## 성능 최적화

### 데이터베이스 최적화

1. **인덱스 사용**
```java
@Entity
@Table(indexes = {
    @Index(name = "idx_event_id", columnList = "eventId"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
public class LogEntry { }
```

2. **N+1 문제 해결**
```java
@Query("SELECT l FROM LogEntry l LEFT JOIN FETCH l.analysisResults WHERE l.id = :id")
Optional<LogEntry> findByIdWithAnalysis(@Param("id") Long id);
```

3. **페이징**
```java
Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
return repository.findAll(pageable);
```

### Kafka 최적화

```yaml
spring:
  kafka:
    producer:
      batch-size: 16384
      linger-ms: 10
      compression-type: snappy
    consumer:
      max-poll-records: 500
      fetch-min-size: 1
```

### 캐싱

```java
@Cacheable(value = "statistics", key = "#hours")
public List<HourlyStatistics> getHourlyStats(int hours) {
    // 비싼 연산
}
```

---

## 기여 방법

### 브랜치 전략

- `main`: 프로덕션 릴리스
- `develop`: 개발 브랜치
- `feature/*`: 새 기능
- `bugfix/*`: 버그 수정
- `hotfix/*`: 긴급 수정

### 커밋 메시지 규칙

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type:**
- `feat`: 새 기능
- `fix`: 버그 수정
- `docs`: 문서 수정
- `style`: 코드 포맷팅
- `refactor`: 코드 리팩토링
- `test`: 테스트 추가/수정
- `chore`: 빌드/설정 변경

**예시:**
```
feat(api): Add log filtering by severity

- Add severity parameter to search endpoint
- Update LogRepository with custom query
- Add integration tests

Closes #123
```

### Pull Request 프로세스

1. Feature 브랜치 생성
2. 코드 작성 및 테스트
3. Commit & Push
4. PR 생성 (develop 브랜치로)
5. 코드 리뷰
6. CI/CD 통과 확인
7. Merge

### 코드 리뷰 체크리스트

- [ ] 모든 테스트 통과
- [ ] 코드 커버리지 80% 이상
- [ ] 문서 업데이트
- [ ] 성능 영향 평가
- [ ] 보안 취약점 체크
- [ ] 로깅 적절히 추가
- [ ] 예외 처리 완료

---

## 추가 리소스

- [Spring Boot 문서](https://spring.io/projects/spring-boot)
- [Kafka 문서](https://kafka.apache.org/documentation/)
- [PostgreSQL 문서](https://www.postgresql.org/docs/)
- [Kubernetes 문서](https://kubernetes.io/docs/)

## 문의

- GitHub Issues: 버그 리포트 및 기능 요청
- Email: dev@example.com
