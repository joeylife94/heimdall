package com.heimdall.service;

import com.heimdall.dto.StatisticsResponse;
import com.heimdall.entity.LogStatistics;
import com.heimdall.repository.LogStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsService {
    
    private final LogStatisticsRepository logStatisticsRepository;
    
    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics(LocalDate date, String serviceName, String environment) {
        log.debug("Getting statistics: date={}, service={}, environment={}", 
            date, serviceName, environment);
        
        List<LogStatistics> stats = logStatisticsRepository.findHourlyStats(
            date, serviceName, environment
        );
        
        // 시간별로 그룹화
        Map<Short, List<LogStatistics>> hourlyMap = stats.stream()
            .collect(Collectors.groupingBy(LogStatistics::getHour));
        
        List<StatisticsResponse.StatisticEntry> entries = new ArrayList<>();
        
        for (short hour = 0; hour < 24; hour++) {
            List<LogStatistics> hourStats = hourlyMap.getOrDefault(hour, new ArrayList<>());
            
            if (!hourStats.isEmpty()) {
                StatisticsResponse.StatisticEntry entry = buildStatisticEntry(
                    date, hour, serviceName, environment, hourStats
                );
                entries.add(entry);
            }
        }
        
        StatisticsResponse.PeriodInfo periodInfo = StatisticsResponse.PeriodInfo.builder()
            .from(date.atStartOfDay())
            .to(date.atTime(23, 59, 59))
            .build();
        
        return StatisticsResponse.builder()
            .period(periodInfo)
            .statistics(entries)
            .build();
    }
    
    private StatisticsResponse.StatisticEntry buildStatisticEntry(
        LocalDate date,
        short hour,
        String serviceName,
        String environment,
        List<LogStatistics> stats
    ) {
        int totalLogs = stats.stream()
            .mapToInt(LogStatistics::getCount)
            .sum();
        
        Map<String, Integer> bySeverity = new HashMap<>();
        for (LogStatistics stat : stats) {
            bySeverity.put(stat.getSeverity(), stat.getCount());
        }
        
        int avgSizeBytes = stats.stream()
            .filter(s -> s.getAvgSizeBytes() != null)
            .mapToInt(LogStatistics::getAvgSizeBytes)
            .findFirst()
            .orElse(0);
        
        return StatisticsResponse.StatisticEntry.builder()
            .timestamp(date.atTime(hour, 0))
            .serviceName(serviceName)
            .environment(environment)
            .totalLogs(totalLogs)
            .bySeverity(bySeverity)
            .avgSizeBytes(avgSizeBytes)
            .build();
    }
    
    @Transactional
    public void updateStatistics(LogStatistics statistics) {
        logStatisticsRepository.save(statistics);
    }
}
