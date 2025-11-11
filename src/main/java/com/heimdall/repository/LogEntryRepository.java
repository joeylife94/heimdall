package com.heimdall.repository;

import com.heimdall.entity.LogEntry;
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
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
    
    Optional<LogEntry> findByEventId(String eventId);
    
    Page<LogEntry> findByServiceNameAndEnvironment(
        String serviceName,
        String environment,
        Pageable pageable
    );
    
    Page<LogEntry> findByTimestampBetween(
        LocalDateTime from,
        LocalDateTime to,
        Pageable pageable
    );
    
    @Query("SELECT l FROM LogEntry l WHERE " +
           "l.serviceName = :serviceName AND " +
           "l.environment = :environment AND " +
           "l.severity = :severity AND " +
           "l.timestamp BETWEEN :from AND :to")
    Page<LogEntry> searchLogs(
        @Param("serviceName") String serviceName,
        @Param("environment") String environment,
        @Param("severity") LogEntry.SeverityLevel severity,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        Pageable pageable
    );
    
    @Query("SELECT l FROM LogEntry l WHERE " +
           "l.timestamp BETWEEN :from AND :to AND " +
           "LOWER(l.logContent) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<LogEntry> searchByKeyword(
        @Param("keyword") String keyword,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        Pageable pageable
    );
    
    List<LogEntry> findByLogHash(String logHash);
    
    @Query("SELECT COUNT(l) FROM LogEntry l WHERE " +
           "l.serviceName = :serviceName AND " +
           "l.severity = :severity AND " +
           "l.timestamp > :since")
    Long countBySeveritySince(
        @Param("serviceName") String serviceName,
        @Param("severity") LogEntry.SeverityLevel severity,
        @Param("since") LocalDateTime since
    );
    
    void deleteByTimestampBefore(LocalDateTime before);
}
