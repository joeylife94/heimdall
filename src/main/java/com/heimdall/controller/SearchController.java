package com.heimdall.controller;

import com.heimdall.dto.LogSearchRequest;
import com.heimdall.dto.LogSearchResponse;
import com.heimdall.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
@Slf4j
public class SearchController {
    
    private final SearchService searchService;
    
    @GetMapping("/search")
    public ResponseEntity<LogSearchResponse> searchLogs(
        @RequestParam(required = false) String serviceName,
        @RequestParam(required = false) String environment,
        @RequestParam(required = false) String severity,
        @RequestParam(required = false) String from,
        @RequestParam(required = false) String to,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer size
    ) {
        log.debug("Search logs request: service={}, environment={}, severity={}, keyword={}", 
            serviceName, environment, severity, keyword);
        
        LogSearchRequest request = LogSearchRequest.builder()
            .serviceName(serviceName)
            .environment(environment)
            .severity(severity)
            .from(from)
            .to(to)
            .keyword(keyword)
            .page(page)
            .size(size)
            .build();
        
        LogSearchResponse response = searchService.searchLogs(request);
        
        return ResponseEntity.ok(response);
    }
}
