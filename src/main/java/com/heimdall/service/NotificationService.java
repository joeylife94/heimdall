package com.heimdall.service;

import com.heimdall.entity.AnalysisResult;
import com.heimdall.entity.Notification;
import com.heimdall.repository.NotificationRepository;
import com.heimdall.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    
    @Value("${heimdall.notification.enabled:true}")
    private boolean notificationEnabled;
    
    @Transactional
    public void sendAnalysisNotification(AnalysisResult analysisResult) {
        if (!notificationEnabled) {
            log.debug("Notification is disabled");
            return;
        }
        
        try {
            String message = buildNotificationMessage(analysisResult);
            
            Notification notification = new Notification();
            notification.setLogEntry(analysisResult.getLogEntry());
            notification.setAnalysisResult(analysisResult);
            notification.setType("ANALYSIS_ALERT");
            notification.setChannel("EMAIL"); // 기본값
            notification.setRecipient("admin@example.com"); // 설정에서 가져와야 함
            notification.setMessage(message);
            notification.setSentAt(DateTimeUtil.now());
            notification.setStatus(Notification.NotificationStatus.PENDING);
            
            notificationRepository.save(notification);
            
            // 실제 알림 발송 로직 (이메일, Slack 등)
            // TODO: 실제 구현 필요
            
            log.info("Notification created for analysisId={}", analysisResult.getId());
        } catch (Exception e) {
            log.error("Failed to send notification for analysisId={}", 
                analysisResult.getId(), e);
        }
    }
    
    private String buildNotificationMessage(AnalysisResult analysisResult) {
        return String.format(
            "⚠️ Log Analysis Alert\n\n" +
            "Service: %s\n" +
            "Environment: %s\n" +
            "Severity: %s\n\n" +
            "Summary: %s\n" +
            "Root Cause: %s\n" +
            "Recommendation: %s\n\n" +
            "Confidence: %.2f%%",
            analysisResult.getLogEntry().getServiceName(),
            analysisResult.getLogEntry().getEnvironment(),
            analysisResult.getSeverity(),
            analysisResult.getSummary(),
            analysisResult.getRootCause(),
            analysisResult.getRecommendation(),
            analysisResult.getConfidence().doubleValue() * 100
        );
    }
}
