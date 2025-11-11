package com.heimdall.repository;

import com.heimdall.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByStatus(Notification.NotificationStatus status);
    
    Page<Notification> findBySentAtBetween(
        LocalDateTime from,
        LocalDateTime to,
        Pageable pageable
    );
    
    List<Notification> findByLogEntry_Id(Long logId);
    
    List<Notification> findByAnalysisResult_Id(Long analysisId);
    
    @Query("SELECT n FROM Notification n WHERE " +
           "n.status = :status AND " +
           "n.sentAt < :before")
    List<Notification> findOldPendingNotifications(
        @Param("status") Notification.NotificationStatus status,
        @Param("before") LocalDateTime before
    );
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE " +
           "n.status = :status AND " +
           "n.sentAt > :since")
    Long countByStatusSince(
        @Param("status") Notification.NotificationStatus status,
        @Param("since") LocalDateTime since
    );
}
