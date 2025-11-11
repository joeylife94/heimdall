package com.heimdall.controller;

import com.heimdall.dto.AnalysisResultResponse;
import com.heimdall.entity.AnalysisResult;
import com.heimdall.repository.AnalysisResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
@Slf4j
public class AnalysisController {
    
    private final AnalysisResultRepository analysisResultRepository;
    
    @GetMapping("/{logId}/analysis")
    public ResponseEntity<AnalysisResultResponse> getAnalysisResult(
        @PathVariable Long logId
    ) {
        log.debug("Get analysis result for logId={}", logId);
        
        AnalysisResult analysisResult = analysisResultRepository
            .findFirstByLogEntry_IdOrderByAnalyzedAtDesc(logId)
            .orElseThrow(() -> new RuntimeException("Analysis result not found for logId: " + logId));
        
        AnalysisResultResponse response = AnalysisResultResponse.builder()
            .analysisId(analysisResult.getId())
            .logId(logId)
            .bifrostAnalysisId(analysisResult.getBifrostAnalysisId())
            .summary(analysisResult.getSummary())
            .rootCause(analysisResult.getRootCause())
            .recommendation(analysisResult.getRecommendation())
            .severity(analysisResult.getSeverity())
            .confidence(analysisResult.getConfidence())
            .model(analysisResult.getModel())
            .analyzedAt(analysisResult.getAnalyzedAt())
            .build();
        
        return ResponseEntity.ok(response);
    }
}
