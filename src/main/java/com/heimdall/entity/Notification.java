package com.heimdall.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_sent_at", columnList = "sent_at"),
        @Index(name = "idx_status", columnList = "status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id")
    private LogEntry logEntry;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id")
    private AnalysisResult analysisResult;
    
    @Column(nullable = false, length = 50)
    private String type;
    
    @Column(nullable = false, length = 50)
    private String channel;
    
    @Column(nullable = false, length = 200)
    private String recipient;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;
    
    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.PENDING;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public enum NotificationStatus {
        PENDING,
        SENT,
        FAILED,
        RETRYING
    }
}
