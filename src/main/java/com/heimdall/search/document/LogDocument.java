package com.heimdall.search.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Elasticsearch 로그 문서
 * 전문 검색을 위한 로그 인덱스
 */
@Document(indexName = "heimdall-logs")
@Setting(shards = 3, replicas = 1)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogDocument {

    @Id
    private String id; // eventId

    @Field(type = FieldType.Long)
    private Long logId;

    @Field(type = FieldType.Date)
    private LocalDateTime timestamp;

    @Field(type = FieldType.Keyword)
    private String source;

    @Field(type = FieldType.Keyword)
    private String serviceName;

    @Field(type = FieldType.Keyword)
    private String environment;

    @Field(type = FieldType.Keyword)
    private String severity;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String logContent;

    @Field(type = FieldType.Keyword)
    private String logHash;

    @Field(type = FieldType.Object)
    private Map<String, Object> metadata;

    @Field(type = FieldType.Boolean)
    private Boolean hasAnalysis;

    @Field(type = FieldType.Keyword)
    private String analysisStatus;

    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date)
    private LocalDateTime updatedAt;
}
