package com.notifier.wso2notifierv2.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomRuleValidateResponse {
    private boolean valid;
    private long hitCount;
    private String index;
    private Integer lookbackSeconds;
    private String errorMessage;
}
