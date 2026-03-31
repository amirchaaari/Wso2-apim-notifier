package com.notifier.wso2notifierv2.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HighLatencyEventDocument {

    @JsonProperty("apiName")
    private String apiName;

    @JsonProperty("userName")
    private String userName;

    @JsonProperty("responseLatency")
    private Long responseLatency;

    @JsonProperty("backendLatency")
    private Long backendLatency;

    @JsonProperty("responseCode")
    private Integer responseCode;

    @JsonProperty("@timestamp")
    private String timestamp;
}