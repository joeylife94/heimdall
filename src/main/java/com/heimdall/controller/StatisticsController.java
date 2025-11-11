package com.heimdall.controller;

import com.heimdall.dto.StatisticsResponse;
import com.heimdall.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
@Slf4j
public class StatisticsController {
    
    private final StatisticsService statisticsService;
    
    @GetMapping
    public ResponseEntity<StatisticsResponse> getStatistics(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam String serviceName,
        @RequestParam String environment
    ) {
        log.debug("Get statistics: date={}, service={}, environment={}", 
            date, serviceName, environment);
        
        StatisticsResponse response = statisticsService.getStatistics(
            date, serviceName, environment
        );
        
        return ResponseEntity.ok(response);
    }
}
