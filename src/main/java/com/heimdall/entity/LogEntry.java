package com.heimdall.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(
    name = "log_entries",
    indexes = {
        @Index(name = "idx_timestamp", columnList = "timestamp"),
        @Index(name = "idx_service_env", columnList = "service_name, environment"),
        @Index(name = "idx_severity", columnList = "severity"),
        @Index(name = "idx_log_hash", columnList = "log_hash")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "event_id", unique = true, nullable = false, length = 36)
    private String eventId;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(nullable = false, length = 100)
    private String source;
    
    @Column(name = "service_name", length = 100)
    private String serviceName;
    
    @Column(length = 50)
    private String environment;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeverityLevel severity;
    
    @Column(name = "log_content", columnDefinition = "TEXT", nullable = false)
    private String logContent;
    
    @Column(name = "log_hash", nullable = false, length = 64)
    private String logHash;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @OneToMany(mappedBy = "logEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnalysisResult> analysisResults = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public enum SeverityLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR,
        FATAL
    }
}
