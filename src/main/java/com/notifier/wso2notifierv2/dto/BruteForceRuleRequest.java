package com.notifier.wso2notifierv2.dto;

import com.notifier.wso2notifierv2.entity.Severity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BruteForceRuleRequest {

    @NotNull
    @Min(value = 1, message = "minAttempts must be at least 1")
    private Integer minAttempts;

    @NotNull
    @Min(value = 10, message = "lookbackSeconds must be at least 10")
    private Integer lookbackSeconds;

    @NotNull
    private Severity severity;

    private String description;
}