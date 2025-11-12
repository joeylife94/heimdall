package com.heimdall.search.repository;

import com.heimdall.search.document.LogDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Elasticsearch 로그 Repository
 * 전문 검색 기능 제공
 */
@Repository
public interface LogSearchRepository extends ElasticsearchRepository<LogDocument, String> {

    /**
     * 서비스명과 환경으로 검색
     */
    Page<LogDocument> findByServiceNameAndEnvironment(
        String serviceName,
        String environment,
        Pageable pageable
    );

    /**
     * 심각도로 검색
     */
    Page<LogDocument> findBySeverity(String severity, Pageable pageable);

    /**
     * 로그 내용 전문 검색
     */
    @Query("{\"bool\": {\"must\": [{\"match\": {\"logContent\": \"?0\"}}]}}")
    Page<LogDocument> searchByContent(String keyword, Pageable pageable);

    /**
     * 복합 검색 (서비스, 환경, 심각도, 시간 범위)
     */
    @Query("{\"bool\": {" +
           "\"must\": [" +
           "{\"range\": {\"timestamp\": {\"gte\": \"?0\", \"lte\": \"?1\"}}}]," +
           "\"filter\": [" +
           "{\"term\": {\"serviceName\": \"?2\"}}," +
           "{\"term\": {\"environment\": \"?3\"}}," +
           "{\"term\": {\"severity\": \"?4\"}}" +
           "]}}")
    Page<LogDocument> advancedSearch(
        LocalDateTime from,
        LocalDateTime to,
        String serviceName,
        String environment,
        String severity,
        Pageable pageable
    );

    /**
     * 분석이 완료된 로그 검색
     */
    Page<LogDocument> findByHasAnalysisTrue(Pageable pageable);

    /**
     * 로그 해시로 중복 검색
     */
    List<LogDocument> findByLogHash(String logHash);
}
