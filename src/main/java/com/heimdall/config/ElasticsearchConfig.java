package com.heimdall.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch 설정
 * 전문 검색 기능을 위한 설정
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.heimdall.search.repository")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris:localhost:9200}")
    private String elasticsearchUri;

    @Value("${spring.elasticsearch.username:#{null}}")
    private String username;

    @Value("${spring.elasticsearch.password:#{null}}")
    private String password;

    @Value("${spring.elasticsearch.connection-timeout:10s}")
    private String connectionTimeout;

    @Value("${spring.elasticsearch.socket-timeout:60s}")
    private String socketTimeout;

    @Override
    public ClientConfiguration clientConfiguration() {
        ClientConfiguration.MaybeSecureClientConfigurationBuilder builder = ClientConfiguration.builder()
            .connectedTo(elasticsearchUri)
            .withConnectTimeout(java.time.Duration.parse("PT" + connectionTimeout.toUpperCase()))
            .withSocketTimeout(java.time.Duration.parse("PT" + socketTimeout.toUpperCase()));

        // Basic Authentication 설정
        if (username != null && password != null) {
            builder.withBasicAuth(username, password);
        }

        return builder.build();
    }
}
