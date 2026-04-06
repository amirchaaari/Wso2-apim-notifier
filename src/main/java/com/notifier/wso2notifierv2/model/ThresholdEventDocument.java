package com.notifier.wso2notifierv2.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThresholdEventDocument {

    @JsonProperty("apiName")
    private String apiName;

    @JsonProperty("userName")
    private String userName;

    @JsonProperty("@timestamp")
    private String timestamp;
}