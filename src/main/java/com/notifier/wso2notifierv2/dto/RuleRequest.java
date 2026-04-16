package com.notifier.wso2notifierv2.dto;

import com.notifier.wso2notifierv2.entity.Severity;
import lombok.Data;

@Data
public class RuleRequest {
    private Integer thresholdValue;
    private Integer lookbackSeconds;
    private String errorCodes;
    private String apiNames;
    private Severity severity;
    private String description;
}
