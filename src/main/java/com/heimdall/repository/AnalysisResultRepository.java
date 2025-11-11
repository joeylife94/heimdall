package com.heimdall.repository;

import com.heimdall.entity.AnalysisResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
    
    Optional<AnalysisResult> findByRequestId(String requestId);
    
    Optional<AnalysisResult> findByCorrelationId(String correlationId);
    
    List<AnalysisResult> findByLogEntry_Id(Long logId);
    
    Optional<AnalysisResult> findFirstByLogEntry_IdOrderByAnalyzedAtDesc(Long logId);
    
    Page<AnalysisResult> findByAnalyzedAtBetween(
        LocalDateTime from,
        LocalDateTime to,
        Pageable pageable
    );
    
    @Query("SELECT a FROM AnalysisResult a WHERE " +
           "a.severity = :severity AND " +
           "a.analyzedAt BETWEEN :from AND :to")
    Page<AnalysisResult> findBySeverityAndDateRange(
        @Param("severity") String severity,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        Pageable pageable
    );
    
    @Query("SELECT COUNT(a) FROM AnalysisResult a WHERE " +
           "a.severity = :severity AND " +
           "a.analyzedAt > :since")
    Long countBySeveritySince(
        @Param("severity") String severity,
        @Param("since") LocalDateTime since
    );
}
