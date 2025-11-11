package com.heimdall.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "analysis_results",
    indexes = {
        @Index(name = "idx_log_id", columnList = "log_id"),
        @Index(name = "idx_analyzed_at", columnList = "analyzed_at"),
        @Index(name = "idx_severity", columnList = "severity")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id", nullable = false)
    private LogEntry logEntry;
    
    @Column(name = "bifrost_analysis_id")
    private Long bifrostAnalysisId;
    
    @Column(name = "request_id", unique = true, nullable = false, length = 36)
    private String requestId;
    
    @Column(name = "correlation_id", length = 36)
    private String correlationId;
    
    @Column(columnDefinition = "TEXT")
    private String summary;
    
    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;
    
    @Column(columnDefinition = "TEXT")
    private String recommendation;
    
    @Column(length = 20)
    private String severity;
    
    @Column(precision = 3, scale = 2)
    private BigDecimal confidence;
    
    @Column(length = 100)
    private String model;
    
    @Column(name = "duration_seconds", precision = 10, scale = 2)
    private BigDecimal durationSeconds;
    
    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
