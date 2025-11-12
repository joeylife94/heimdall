package com.heimdall.grpc;

import com.heimdall.grpc.client.LogServiceGrpcClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * gRPC 서비스 통합 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LogServiceGrpcIntegrationTest {

    @Autowired
    private LogServiceGrpcClient grpcClient;

    @Test
    void testIngestLog() {
        // Given
        Map<String, String> metadata = new HashMap<>();
        metadata.put("user_id", "test_user");
        metadata.put("ip_address", "192.168.1.1");

        LogIngestionRequest request = LogIngestionRequest.newBuilder()
                .setSource("grpc-test")
                .setServiceName("test-service")
                .setEnvironment("test")
                .setSeverity("INFO")
                .setLogContent("gRPC test log message")
                .setTimestampMillis(Instant.now().toEpochMilli())
                .putAllMetadata(metadata)
                .setTraceId("trace-123")
                .setSpanId("span-456")
                .build();

        // When
        LogIngestionResponse response = grpcClient.sendLog(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getLogId()).isGreaterThan(0);
        assertThat(response.getEventId()).isNotBlank();
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void testBatchIngestLogs() {
        // Given
        BatchLogIngestionRequest.Builder batchBuilder = BatchLogIngestionRequest.newBuilder();

        for (int i = 0; i < 5; i++) {
            LogIngestionRequest request = LogIngestionRequest.newBuilder()
                    .setSource("grpc-batch-test")
                    .setServiceName("test-service")
                    .setEnvironment("test")
                    .setSeverity("INFO")
                    .setLogContent("Batch log message " + i)
                    .setTimestampMillis(Instant.now().toEpochMilli())
                    .build();
            batchBuilder.addLogs(request);
        }

        // When
        BatchLogIngestionResponse response = grpcClient.sendBatchLogs(batchBuilder.build());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTotalCount()).isEqualTo(5);
        assertThat(response.getSuccessCount()).isEqualTo(5);
        assertThat(response.getFailureCount()).isEqualTo(0);
        assertThat(response.getResponsesList()).hasSize(5);
    }

    @Test
    void testGetLog() {
        // Given: 먼저 로그 생성
        LogIngestionRequest createRequest = LogIngestionRequest.newBuilder()
                .setSource("grpc-get-test")
                .setServiceName("test-service")
                .setEnvironment("test")
                .setSeverity("WARN")
                .setLogContent("Log for retrieval test")
                .setTimestampMillis(Instant.now().toEpochMilli())
                .build();

        LogIngestionResponse createResponse = grpcClient.sendLog(createRequest);
        long logId = createResponse.getLogId();

        // When: 로그 조회
        com.heimdall.grpc.LogEntry logEntry = grpcClient.getLog(logId);

        // Then
        assertThat(logEntry).isNotNull();
        assertThat(logEntry.getLogId()).isEqualTo(logId);
        assertThat(logEntry.getSource()).isEqualTo("grpc-get-test");
        assertThat(logEntry.getSeverity()).isEqualTo("WARN");
        assertThat(logEntry.getLogContent()).isEqualTo("Log for retrieval test");
    }

    @Test
    void testSearchLogs() {
        // Given: 검색용 로그 생성
        for (int i = 0; i < 3; i++) {
            LogIngestionRequest request = LogIngestionRequest.newBuilder()
                    .setSource("grpc-search-test")
                    .setServiceName("search-service")
                    .setEnvironment("production")
                    .setSeverity("ERROR")
                    .setLogContent("Searchable log " + i)
                    .setTimestampMillis(Instant.now().toEpochMilli())
                    .build();
            grpcClient.sendLog(request);
        }

        // When: 로그 검색
        SearchLogsResponse response = grpcClient.searchLogs(
                "search-service", 
                "production", 
                0, 
                10
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTotalCount()).isGreaterThanOrEqualTo(3);
        assertThat(response.getLogsList()).isNotEmpty();
        
        com.heimdall.grpc.LogEntry firstLog = response.getLogsList().get(0);
        assertThat(firstLog.getServiceName()).isEqualTo("search-service");
        assertThat(firstLog.getEnvironment()).isEqualTo("production");
    }

    @Test
    void testHighVolumeIngestion() {
        // Given: 대량 로그 생성
        int logCount = 100;
        BatchLogIngestionRequest.Builder batchBuilder = BatchLogIngestionRequest.newBuilder();

        for (int i = 0; i < logCount; i++) {
            LogIngestionRequest request = LogIngestionRequest.newBuilder()
                    .setSource("grpc-performance-test")
                    .setServiceName("perf-service")
                    .setEnvironment("test")
                    .setSeverity(i % 3 == 0 ? "ERROR" : "INFO")
                    .setLogContent("Performance test log " + i)
                    .setTimestampMillis(Instant.now().toEpochMilli())
                    .build();
            batchBuilder.addLogs(request);
        }

        // When
        long startTime = System.currentTimeMillis();
        BatchLogIngestionResponse response = grpcClient.sendBatchLogs(batchBuilder.build());
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(response.getTotalCount()).isEqualTo(logCount);
        assertThat(response.getSuccessCount()).isEqualTo(logCount);
        assertThat(duration).isLessThan(5000); // 5초 이내 완료
        
        System.out.printf("Processed %d logs in %d ms (%.2f logs/sec)%n",
                logCount, duration, (logCount * 1000.0) / duration);
    }
}
