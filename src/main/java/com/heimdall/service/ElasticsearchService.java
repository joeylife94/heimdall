package com.heimdall.service;

import com.heimdall.search.document.LogDocument;
import com.heimdall.search.repository.LogSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Elasticsearch 기반 검색 서비스
 * 전문 검색 및 고급 필터링 기능 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchService {

    private final LogSearchRepository logSearchRepository;

    /**
     * 로그 내용 전문 검색
     */
    public Page<LogDocument> searchByKeyword(String keyword, Pageable pageable) {
        log.info("Searching logs by keyword: {}", keyword);
        return logSearchRepository.searchByContent(keyword, pageable);
    }

    /**
     * 고급 검색 (복합 조건)
     */
    public Page<LogDocument> advancedSearch(
        LocalDateTime from,
        LocalDateTime to,
        String serviceName,
        String environment,
        String severity,
        Pageable pageable
    ) {
        log.info("Advanced search: service={}, env={}, severity={}", serviceName, environment, severity);
        return logSearchRepository.advancedSearch(from, to, serviceName, environment, severity, pageable);
    }

    /**
     * 로그 문서 인덱싱
     */
    public LogDocument indexLog(LogDocument logDocument) {
        log.debug("Indexing log document: {}", logDocument.getId());
        return logSearchRepository.save(logDocument);
    }

    /**
     * 중복 로그 검색 (해시 기반)
     */
    public boolean isDuplicate(String logHash) {
        return !logSearchRepository.findByLogHash(logHash).isEmpty();
    }

    /**
     * 분석이 완료된 로그 조회
     */
    public Page<LogDocument> findAnalyzedLogs(Pageable pageable) {
        return logSearchRepository.findByHasAnalysisTrue(pageable);
    }
}
