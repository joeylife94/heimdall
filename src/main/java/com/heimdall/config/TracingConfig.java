package com.heimdall.config;

import brave.sampler.Sampler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Distributed Tracing 설정
 * Zipkin/Jaeger를 통한 분산 추적
 */
@Configuration
public class TracingConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${management.tracing.sampling.probability:0.1}")
    private float samplingProbability;

    /**
     * 샘플링 전략 설정
     * 기본 10% 샘플링 (성능 고려)
     */
    @Bean
    public Sampler defaultSampler() {
        return Sampler.create(samplingProbability);
    }
}
