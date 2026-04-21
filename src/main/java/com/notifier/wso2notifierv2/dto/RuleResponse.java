package com.notifier.wso2notifierv2.dto;

import com.notifier.wso2notifierv2.entity.Severity;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class RuleResponse {
    private Long id;
    private String useCaseType;
    private boolean enabled;
    private Severity severity;
    private Integer thresholdValue;
    private Integer lookbackSeconds;
    private String errorCodes;
    private String apiNames;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private java.util.Set<com.notifier.wso2notifierv2.entity.NotificationTarget> targets;
}
