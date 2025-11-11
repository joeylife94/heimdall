package com.heimdall.repository;

import com.heimdall.entity.LogStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LogStatisticsRepository extends JpaRepository<LogStatistics, Long> {
    
    Optional<LogStatistics> findByDateAndHourAndServiceNameAndEnvironmentAndSeverity(
        LocalDate date,
        Short hour,
        String serviceName,
        String environment,
        String severity
    );
    
    List<LogStatistics> findByDateBetween(LocalDate from, LocalDate to);
    
    List<LogStatistics> findByDateAndServiceName(LocalDate date, String serviceName);
    
    @Query("SELECT s FROM LogStatistics s WHERE " +
           "s.date = :date AND " +
           "s.serviceName = :serviceName AND " +
           "s.environment = :environment " +
           "ORDER BY s.hour ASC")
    List<LogStatistics> findHourlyStats(
        @Param("date") LocalDate date,
        @Param("serviceName") String serviceName,
        @Param("environment") String environment
    );
    
    @Query("SELECT s.serviceName, SUM(s.count) as total FROM LogStatistics s WHERE " +
           "s.date BETWEEN :from AND :to " +
           "GROUP BY s.serviceName " +
           "ORDER BY total DESC")
    List<Object[]> getTopServicesByLogCount(
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );
    
    @Query("SELECT s.severity, SUM(s.count) as total FROM LogStatistics s WHERE " +
           "s.date BETWEEN :from AND :to " +
           "GROUP BY s.severity")
    List<Object[]> getSeverityDistribution(
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );
}
