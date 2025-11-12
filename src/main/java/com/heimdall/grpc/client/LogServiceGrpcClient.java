package com.heimdall.grpc.client;

import com.heimdall.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * gRPC 클라이언트 예제
 * 테스트 및 외부 서비스 연동용
 */
@Component
@Slf4j
public class LogServiceGrpcClient {

    @Value("${grpc.client.heimdall.host:localhost}")
    private String host;

    @Value("${grpc.client.heimdall.port:9090}")
    private int port;

    private ManagedChannel channel;
    private LogServiceGrpc.LogServiceBlockingStub blockingStub;
    private LogServiceGrpc.LogServiceStub asyncStub;

    @PostConstruct
    public void init() {
        channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext() // 개발 환경용 (프로덕션에서는 TLS 사용)
                .build();

        blockingStub = LogServiceGrpc.newBlockingStub(channel);
        asyncStub = LogServiceGrpc.newStub(channel);

        log.info("gRPC client initialized: {}:{}", host, port);
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (channel != null) {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                log.info("gRPC client shutdown completed");
            }
        } catch (InterruptedException e) {
            log.error("Error shutting down gRPC client", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 단일 로그 전송 (동기)
     */
    public LogIngestionResponse sendLog(LogIngestionRequest request) {
        try {
            return blockingStub.ingestLog(request);
        } catch (Exception e) {
            log.error("Error sending log via gRPC", e);
            throw e;
        }
    }

    /**
     * 배치 로그 전송 (동기)
     */
    public BatchLogIngestionResponse sendBatchLogs(BatchLogIngestionRequest request) {
        try {
            return blockingStub.batchIngestLogs(request);
        } catch (Exception e) {
            log.error("Error sending batch logs via gRPC", e);
            throw e;
        }
    }

    /**
     * 로그 조회
     */
    public com.heimdall.grpc.LogEntry getLog(long logId) {
        try {
            GetLogRequest request = GetLogRequest.newBuilder()
                    .setLogId(logId)
                    .build();
            return blockingStub.getLog(request);
        } catch (Exception e) {
            log.error("Error getting log via gRPC", e);
            throw e;
        }
    }

    /**
     * 로그 검색
     */
    public SearchLogsResponse searchLogs(String serviceName, String environment, int page, int size) {
        try {
            SearchLogsRequest request = SearchLogsRequest.newBuilder()
                    .setServiceName(serviceName)
                    .setEnvironment(environment)
                    .setPage(page)
                    .setSize(size)
                    .build();
            return blockingStub.searchLogs(request);
        } catch (Exception e) {
            log.error("Error searching logs via gRPC", e);
            throw e;
        }
    }

    // Getters for advanced usage
    public LogServiceGrpc.LogServiceBlockingStub getBlockingStub() {
        return blockingStub;
    }

    public LogServiceGrpc.LogServiceStub getAsyncStub() {
        return asyncStub;
    }
}
