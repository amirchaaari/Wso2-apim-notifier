package com.notifier.wso2notifierv2.dto;

import com.notifier.wso2notifierv2.entity.IncidentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class IncidentResponse {
    private Long id;
    private String ruleName;    // Use case type name
    private String groupingKey; // IP, API name, etc.
    private IncidentStatus status;
    private Integer alertCount;
    private Instant firstSeen;
    private Instant lastSeen;
    private String acknowledgedBy;
    private Instant acknowledgedAt;
    private Instant resolvedAt;
}
