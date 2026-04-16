package com.notifier.wso2notifierv2.dto;

import com.notifier.wso2notifierv2.entity.Severity;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class BruteForceRuleResponse {

    private Long id;
    private String useCaseType;
    private boolean enabled;
    private Severity severity;
    private Integer minAttempts;
    private Integer lookbackSeconds;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}