package com.heimdall.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "log_statistics",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_date_hour_service_env_severity",
            columnNames = {"date", "hour", "service_name", "environment", "severity"}
        )
    },
    indexes = {
        @Index(name = "idx_date_hour", columnList = "date, hour")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogStatistics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private LocalDate date;
    
    @Column(nullable = false)
    private Short hour;
    
    @Column(name = "service_name", length = 100)
    private String serviceName;
    
    @Column(length = 50)
    private String environment;
    
    @Column(length = 20)
    private String severity;
    
    @Column(nullable = false)
    private Integer count = 0;
    
    @Column(name = "avg_size_bytes")
    private Integer avgSizeBytes;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
