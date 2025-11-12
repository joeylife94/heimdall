package com.heimdall.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j 설정
 * Circuit Breaker, Rate Limiter, Retry, Bulkhead 패턴 구현
 */
@Configuration
public class Resilience4jConfig {

    /**
     * Circuit Breaker 기본 설정
     */
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
            .timeLimiterConfig(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(10))
                .build())
            .circuitBreakerConfig(CircuitBreakerConfig.custom()
                // 실패율이 50%를 넘으면 Circuit Open
                .failureRateThreshold(50)
                // 최소 10개의 호출 후 평가
                .minimumNumberOfCalls(10)
                // Half-Open 상태에서 5개 호출로 테스트
                .permittedNumberOfCallsInHalfOpenState(5)
                // Open 상태 유지 시간 (60초)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                // Sliding Window 크기 (100개 호출)
                .slidingWindowSize(100)
                .build())
            .build());
    }

    /**
     * Bifrost 연동용 Circuit Breaker
     */
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> bifrostCircuitBreakerCustomizer() {
        return factory -> factory.configure(builder -> builder
            .timeLimiterConfig(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(30)) // AI 분석은 더 긴 타임아웃
                .build())
            .circuitBreakerConfig(CircuitBreakerConfig.custom()
                .failureRateThreshold(60) // Bifrost는 더 관대한 임계값
                .minimumNumberOfCalls(5)
                .waitDurationInOpenState(Duration.ofMinutes(2))
                .build())
            .build(), "bifrost-service");
    }
}
