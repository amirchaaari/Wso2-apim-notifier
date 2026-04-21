package com.notifier.wso2notifierv2.dto;

import com.notifier.wso2notifierv2.entity.Severity;
import lombok.Data;

import java.util.Set;

@Data
public class CustomRuleRequest {
    private Long id;
    private String name;
    private String description;
    private Severity severity;
    private String esIndex;
    private String esQuery;
    private String groupingField;
    private Integer lookbackSeconds;
    private Integer minHits;
    private Boolean enabled;
    private Set<Long> targetIds;
}
