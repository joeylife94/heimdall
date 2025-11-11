package com.heimdall.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heimdall.dto.LogEntryRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Heimdall 통합 테스트
 * 전체 애플리케이션 컨텍스트를 로드하여 E2E 시나리오를 테스트합니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"log-events", "log-analysis-requests", "log-analysis-results"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HeimdallIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String testEventId;

    @Test
    @Order(1)
    @DisplayName("로그 수집 - 성공")
    public void testLogIngestion_Success() throws Exception {
        // Given
        LogEntryRequest request = createSampleLogRequest();

        // When & Then
        String response = mockMvc.perform(post("/api/v1/logs")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-Key", "test-api-key")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId", notNullValue()))
                .andExpect(jsonPath("$.message", is("Log entry created successfully")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 이후 테스트를 위해 eventId 저장
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        testEventId = (String) responseMap.get("eventId");
    }

    @Test
    @Order(2)
    @DisplayName("로그 검색 - eventId로 조회")
    public void testLogSearch_ByEventId() throws Exception {
        // Given: 이전 테스트에서 생성된 eventId

        // When & Then
        mockMvc.perform(get("/api/v1/logs/search")
                .param("eventId", testEventId)
                .header("X-API-Key", "test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.content[0].eventId", is(testEventId)));
    }

    @Test
    @Order(3)
    @DisplayName("로그 검색 - 날짜 범위로 조회")
    public void testLogSearch_ByDateRange() throws Exception {
        // Given
        Instant startTime = Instant.now().minusSeconds(3600); // 1시간 전
        Instant endTime = Instant.now();

        // When & Then
        mockMvc.perform(get("/api/v1/logs/search")
                .param("startTime", startTime.toString())
                .param("endTime", endTime.toString())
                .param("page", "0")
                .param("size", "20")
                .header("X-API-Key", "test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", isA(java.util.List.class)))
                .andExpect(jsonPath("$.pageable", notNullValue()));
    }

    @Test
    @Order(4)
    @DisplayName("로그 검색 - severity로 필터링")
    public void testLogSearch_BySeverity() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/logs/search")
                .param("severity", "ERROR")
                .param("page", "0")
                .param("size", "10")
                .header("X-API-Key", "test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", isA(java.util.List.class)));
    }

    @Test
    @Order(5)
    @DisplayName("로그 분석 요청 - 성공")
    public void testAnalysisRequest_Success() throws Exception {
        // Given: 이전 테스트에서 생성된 eventId

        // When & Then
        mockMvc.perform(post("/api/v1/analysis/request")
                .param("eventId", testEventId)
                .header("X-API-Key", "test-api-key"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message", containsString("Analysis request submitted")));
    }

    @Test
    @Order(6)
    @DisplayName("로그 분석 결과 조회")
    public void testAnalysisResult_Retrieve() throws Exception {
        // Given: 이전 테스트에서 생성된 eventId
        // Note: 실제 분석 결과는 비동기로 처리되므로 바로 조회되지 않을 수 있음

        // When & Then
        mockMvc.perform(get("/api/v1/analysis/result/{eventId}", testEventId)
                .header("X-API-Key", "test-api-key"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(7)
    @DisplayName("통계 조회 - 전체 통계")
    public void testStatistics_Overall() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/statistics/overall")
                .header("X-API-Key", "test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLogs", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.errorCount", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.warningCount", greaterThanOrEqualTo(0)));
    }

    @Test
    @Order(8)
    @DisplayName("통계 조회 - 시간대별 통계")
    public void testStatistics_Hourly() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/statistics/hourly")
                .param("hours", "24")
                .header("X-API-Key", "test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    @Order(9)
    @DisplayName("인증 실패 - API Key 없음")
    public void testAuthentication_MissingApiKey() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/logs/search"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(10)
    @DisplayName("인증 실패 - 잘못된 API Key")
    public void testAuthentication_InvalidApiKey() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/logs/search")
                .header("X-API-Key", "invalid-key"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(11)
    @DisplayName("유효성 검사 실패 - 필수 필드 누락")
    public void testValidation_MissingRequiredFields() throws Exception {
        // Given
        Map<String, Object> invalidRequest = new HashMap<>();
        invalidRequest.put("source", "test-source");
        // message 필드 누락

        // When & Then
        mockMvc.perform(post("/api/v1/logs")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-Key", "test-api-key")
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(12)
    @DisplayName("Health Check - 정상")
    public void testHealthCheck_Healthy() throws Exception {
        // When & Then
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @Order(13)
    @DisplayName("Metrics 엔드포인트 - 접근 가능")
    public void testMetrics_Accessible() throws Exception {
        // When & Then
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain;version=0.0.4;charset=utf-8"));
    }

    // Helper 메서드
    private LogEntryRequest createSampleLogRequest() {
        LogEntryRequest request = new LogEntryRequest();
        request.setSource("integration-test");
        request.setMessage("Integration test log message");
        request.setSeverity("INFO");
        request.setTimestamp(Instant.now());
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("test", true);
        metadata.put("environment", "test");
        metadata.put("version", "1.0.0");
        request.setMetadata(metadata);
        
        return request;
    }
}
