package com.notifier.wso2notifierv2.model;

import com.notifier.wso2notifierv2.entity.Severity;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AlertMessage {

    private String useCaseType; // "DELETE_EVENT", "HIGH_LATENCY", "THRESHOLD", "FAULTY", "BRUTE_FORCE_LOGIN"
    private Severity severity;
    private String performedBy;
    private String action;
    private String description;
    private String resourceType;
    private String resourceName;
    private String timestamp;

    // HIGH_LATENCY
    private Long responseLatency;
    private Long backendLatency;
    private Integer responseCode;

    // THRESHOLD
    private Long count;

    // FAULTY
    private Integer errorCode;
    private String errorMessage;

    // BRUTE_FORCE_LOGIN
    private String ipAddress;
    private Integer failedAttempts;
    private List<String> portalsTargeted;
    private List<String> usernamesTried;
}