package com.notifier.wso2notifierv2.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlertMessage {

    private String useCaseType;   // e.g. "DELETE_EVENT"
    private String performedBy;
    private String action;
    private String resourceType;
    private String resourceName;
    private String timestamp;

    // Optional — populated for latency-related use cases
    private Long responseLatency;
    private Long backendLatency;
    private Integer responseCode;
}