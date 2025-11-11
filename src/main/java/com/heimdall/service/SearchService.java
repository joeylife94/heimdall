package com.heimdall.service;

import com.heimdall.dto.LogSearchRequest;
import com.heimdall.dto.LogSearchResponse;
import com.heimdall.entity.LogEntry;
import com.heimdall.repository.LogEntryRepository;
import com.heimdall.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {
    
    private final LogEntryRepository logEntryRepository;
    
    @Transactional(readOnly = true)
    public LogSearchResponse searchLogs(LogSearchRequest request) {
        log.debug("Searching logs: {}", request);
        
        Pageable pageable = PageRequest.of(
            request.getPage(),
            request.getSize(),
            Sort.by(Sort.Direction.DESC, "timestamp")
        );
        
        Page<LogEntry> logPage;
        
        if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            // 키워드 검색
            LocalDateTime from = request.getFrom() != null ? 
                DateTimeUtil.parseIso(request.getFrom()) : LocalDateTime.now().minusDays(7);
            LocalDateTime to = request.getTo() != null ? 
                DateTimeUtil.parseIso(request.getTo()) : LocalDateTime.now();
            
            logPage = logEntryRepository.searchByKeyword(
                request.getKeyword(),
                from,
                to,
                pageable
            );
        } else if (request.getServiceName() != null && 
                   request.getEnvironment() != null &&
                   request.getSeverity() != null) {
            // 상세 검색
            LocalDateTime from = request.getFrom() != null ? 
                DateTimeUtil.parseIso(request.getFrom()) : LocalDateTime.now().minusDays(7);
            LocalDateTime to = request.getTo() != null ? 
                DateTimeUtil.parseIso(request.getTo()) : LocalDateTime.now();
            
            logPage = logEntryRepository.searchLogs(
                request.getServiceName(),
                request.getEnvironment(),
                LogEntry.SeverityLevel.valueOf(request.getSeverity()),
                from,
                to,
                pageable
            );
        } else if (request.getServiceName() != null && request.getEnvironment() != null) {
            // 서비스 + 환경 검색
            logPage = logEntryRepository.findByServiceNameAndEnvironment(
                request.getServiceName(),
                request.getEnvironment(),
                pageable
            );
        } else if (request.getFrom() != null && request.getTo() != null) {
            // 시간 범위 검색
            LocalDateTime from = DateTimeUtil.parseIso(request.getFrom());
            LocalDateTime to = DateTimeUtil.parseIso(request.getTo());
            
            logPage = logEntryRepository.findByTimestampBetween(from, to, pageable);
        } else {
            // 전체 조회
            logPage = logEntryRepository.findAll(pageable);
        }
        
        // DTO 변환
        List<LogSearchResponse.LogEntryDto> content = logPage.getContent().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
        
        LogSearchResponse.PageInfo pageInfo = LogSearchResponse.PageInfo.builder()
            .size(logPage.getSize())
            .totalElements(logPage.getTotalElements())
            .totalPages(logPage.getTotalPages())
            .number(logPage.getNumber())
            .build();
        
        return LogSearchResponse.builder()
            .content(content)
            .page(pageInfo)
            .build();
    }
    
    private LogSearchResponse.LogEntryDto convertToDto(LogEntry logEntry) {
        return LogSearchResponse.LogEntryDto.builder()
            .logId(logEntry.getId())
            .timestamp(logEntry.getTimestamp())
            .serviceName(logEntry.getServiceName())
            .environment(logEntry.getEnvironment())
            .severity(logEntry.getSeverity().name())
            .logContent(logEntry.getLogContent())
            .hasAnalysis(!logEntry.getAnalysisResults().isEmpty())
            .build();
    }
}
