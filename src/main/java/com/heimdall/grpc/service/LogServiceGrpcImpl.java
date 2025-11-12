package com.heimdall.grpc.service;

import com.heimdall.dto.LogEntryRequest;
import com.heimdall.entity.LogEntry;
import com.heimdall.grpc.*;
import com.heimdall.service.LogIngestionService;
import com.heimdall.service.SearchService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * gRPC 로그 서비스 구현
 * 고성능 로그 수집을 위한 gRPC 인터페이스
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class LogServiceGrpcImpl extends LogServiceGrpc.LogServiceImplBase {

    private final LogIngestionService logIngestionService;
    private final SearchService searchService;

    /**
     * 단일 로그 수집
     */
    @Override
    public void ingestLog(LogIngestionRequest request, StreamObserver<LogIngestionResponse> responseObserver) {
        try {
            log.debug("gRPC IngestLog called: source={}, severity={}", request.getSource(), request.getSeverity());

            // gRPC 요청을 내부 DTO로 변환
            LogEntryRequest logRequest = convertToLogEntryRequest(request);

            // 로그 수집 처리
            com.heimdall.dto.LogIngestionResponse response = logIngestionService.ingestLog(logRequest);

            // gRPC 응답 생성
            LogIngestionResponse grpcResponse = LogIngestionResponse.newBuilder()
                    .setLogId(response.getLogId())
                    .setEventId(response.getEventId())
                    .setTimestampMillis(response.getTimestamp().toInstant(ZoneOffset.UTC).toEpochMilli())
                    .setStatus(response.getStatus())
                    .setAnalysisRequested(response.getAnalysisRequested() != null && response.getAnalysisRequested())
                    .build();

            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in gRPC IngestLog", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    /**
     * 스트리밍 로그 수집 (고성능)
     * 양방향 스트리밍으로 대량 로그 처리
     */
    @Override
    public StreamObserver<LogIngestionRequest> streamLogs(StreamObserver<LogIngestionResponse> responseObserver) {
        return new StreamObserver<LogIngestionRequest>() {
            private final AtomicInteger count = new AtomicInteger(0);

            @Override
            public void onNext(LogIngestionRequest request) {
                try {
                    // 로그 수집 처리
                    LogEntryRequest logRequest = convertToLogEntryRequest(request);
                    com.heimdall.dto.LogIngestionResponse response = logIngestionService.ingestLog(logRequest);

                    // 응답 전송
                    LogIngestionResponse grpcResponse = LogIngestionResponse.newBuilder()
                            .setLogId(response.getLogId())
                            .setEventId(response.getEventId())
                            .setTimestampMillis(response.getTimestamp().toInstant(ZoneOffset.UTC).toEpochMilli())
                            .setStatus(response.getStatus())
                            .setAnalysisRequested(response.getAnalysisRequested() != null && response.getAnalysisRequested())
                            .build();

                    responseObserver.onNext(grpcResponse);
                    count.incrementAndGet();

                } catch (Exception e) {
                    log.error("Error processing stream log", e);
                    LogIngestionResponse errorResponse = LogIngestionResponse.newBuilder()
                            .setStatus("ERROR")
                            .setErrorMessage(e.getMessage())
                            .build();
                    responseObserver.onNext(errorResponse);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error in stream logs", t);
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                log.info("Stream logs completed. Total processed: {}", count.get());
                responseObserver.onCompleted();
            }
        };
    }

    /**
     * 배치 로그 수집
     */
    @Override
    public void batchIngestLogs(BatchLogIngestionRequest request, StreamObserver<BatchLogIngestionResponse> responseObserver) {
        try {
            log.info("gRPC BatchIngestLogs called: count={}", request.getLogsCount());

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            BatchLogIngestionResponse.Builder responseBuilder = BatchLogIngestionResponse.newBuilder()
                    .setTotalCount(request.getLogsCount());

            // 각 로그 처리
            for (LogIngestionRequest logRequest : request.getLogsList()) {
                try {
                    LogEntryRequest logEntryRequest = convertToLogEntryRequest(logRequest);
                    com.heimdall.dto.LogIngestionResponse response = logIngestionService.ingestLog(logEntryRequest);

                    LogIngestionResponse grpcResponse = LogIngestionResponse.newBuilder()
                            .setLogId(response.getLogId())
                            .setEventId(response.getEventId())
                            .setTimestampMillis(response.getTimestamp().toInstant(ZoneOffset.UTC).toEpochMilli())
                            .setStatus(response.getStatus())
                            .setAnalysisRequested(response.getAnalysisRequested() != null && response.getAnalysisRequested())
                            .build();

                    responseBuilder.addResponses(grpcResponse);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    log.error("Error processing batch log", e);
                    LogIngestionResponse errorResponse = LogIngestionResponse.newBuilder()
                            .setStatus("ERROR")
                            .setErrorMessage(e.getMessage())
                            .build();
                    responseBuilder.addResponses(errorResponse);
                    failureCount.incrementAndGet();
                }
            }

            BatchLogIngestionResponse response = responseBuilder
                    .setSuccessCount(successCount.get())
                    .setFailureCount(failureCount.get())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in gRPC BatchIngestLogs", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    /**
     * 로그 조회
     */
    @Override
    public void getLog(GetLogRequest request, StreamObserver<com.heimdall.grpc.LogEntry> responseObserver) {
        try {
            LogEntry logEntry;

            if (request.hasLogId()) {
                logEntry = searchService.findById(request.getLogId());
            } else if (request.hasEventId()) {
                logEntry = searchService.findByEventId(request.getEventId());
            } else {
                throw new IllegalArgumentException("Either log_id or event_id must be provided");
            }

            if (logEntry == null) {
                responseObserver.onError(io.grpc.Status.NOT_FOUND
                        .withDescription("Log not found")
                        .asRuntimeException());
                return;
            }

            com.heimdall.grpc.LogEntry grpcLogEntry = convertToGrpcLogEntry(logEntry);
            responseObserver.onNext(grpcLogEntry);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in gRPC GetLog", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    /**
     * 로그 검색
     */
    @Override
    public void searchLogs(SearchLogsRequest request, StreamObserver<SearchLogsResponse> responseObserver) {
        try {
            log.debug("gRPC SearchLogs called: service={}, environment={}", 
                    request.getServiceName(), request.getEnvironment());

            // 검색 파라미터 변환
            com.heimdall.dto.LogSearchRequest searchRequest = new com.heimdall.dto.LogSearchRequest();
            searchRequest.setServiceName(request.getServiceName().isEmpty() ? null : request.getServiceName());
            searchRequest.setEnvironment(request.getEnvironment().isEmpty() ? null : request.getEnvironment());
            searchRequest.setSeverity(request.getSeverity().isEmpty() ? null : 
                    com.heimdall.entity.SeverityLevel.valueOf(request.getSeverity()));
            
            if (request.getFromTimestampMillis() > 0) {
                searchRequest.setFrom(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(request.getFromTimestampMillis()), ZoneOffset.UTC));
            }
            if (request.getToTimestampMillis() > 0) {
                searchRequest.setTo(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(request.getToTimestampMillis()), ZoneOffset.UTC));
            }
            
            searchRequest.setKeyword(request.getKeyword().isEmpty() ? null : request.getKeyword());
            searchRequest.setPage(request.getPage());
            searchRequest.setSize(request.getSize() > 0 ? request.getSize() : 20);

            // 검색 실행
            Page<LogEntry> searchResults = logIngestionService.searchLogs(searchRequest);

            // gRPC 응답 생성
            SearchLogsResponse.Builder responseBuilder = SearchLogsResponse.newBuilder()
                    .setTotalCount((int) searchResults.getTotalElements())
                    .setPage(searchResults.getNumber())
                    .setSize(searchResults.getSize())
                    .setTotalPages(searchResults.getTotalPages());

            for (LogEntry logEntry : searchResults.getContent()) {
                responseBuilder.addLogs(convertToGrpcLogEntry(logEntry));
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in gRPC SearchLogs", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    // Helper methods

    private LogEntryRequest convertToLogEntryRequest(LogIngestionRequest grpcRequest) {
        LogEntryRequest request = new LogEntryRequest();
        request.setSource(grpcRequest.getSource());
        request.setServiceName(grpcRequest.getServiceName());
        request.setEnvironment(grpcRequest.getEnvironment());
        request.setSeverity(grpcRequest.getSeverity());
        request.setMessage(grpcRequest.getLogContent());
        
        if (grpcRequest.getTimestampMillis() > 0) {
            request.setTimestamp(Instant.ofEpochMilli(grpcRequest.getTimestampMillis()));
        } else {
            request.setTimestamp(Instant.now());
        }
        
        Map<String, Object> metadata = new HashMap<>(grpcRequest.getMetadataMap());
        if (!grpcRequest.getTraceId().isEmpty()) {
            metadata.put("trace_id", grpcRequest.getTraceId());
        }
        if (!grpcRequest.getSpanId().isEmpty()) {
            metadata.put("span_id", grpcRequest.getSpanId());
        }
        request.setMetadata(metadata);
        
        return request;
    }

    private com.heimdall.grpc.LogEntry convertToGrpcLogEntry(LogEntry logEntry) {
        com.heimdall.grpc.LogEntry.Builder builder = com.heimdall.grpc.LogEntry.newBuilder()
                .setLogId(logEntry.getId())
                .setEventId(logEntry.getEventId())
                .setTimestampMillis(logEntry.getTimestamp().toInstant(ZoneOffset.UTC).toEpochMilli())
                .setSource(logEntry.getSource())
                .setSeverity(logEntry.getSeverity().name())
                .setLogContent(logEntry.getMessage())
                .setLogHash(logEntry.getLogHash())
                .setHasAnalysis(logEntry.getAnalysisResults() != null && !logEntry.getAnalysisResults().isEmpty())
                .setCreatedAtMillis(logEntry.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli());

        if (logEntry.getServiceName() != null) {
            builder.setServiceName(logEntry.getServiceName());
        }
        if (logEntry.getEnvironment() != null) {
            builder.setEnvironment(logEntry.getEnvironment());
        }
        if (logEntry.getMetadata() != null) {
            logEntry.getMetadata().forEach((key, value) -> 
                    builder.putMetadata(key, value != null ? value.toString() : ""));
        }

        return builder.build();
    }
}
