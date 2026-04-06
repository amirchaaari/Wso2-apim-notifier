package com.notifier.wso2notifierv2.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginAttemptDocument {

    @JsonProperty("userName")
    private String userName;

    @JsonProperty("remoteAddress")
    private String remoteAddress;

    @JsonProperty("serviceProviderName")
    private String serviceProviderName;

    @JsonProperty("loginResult")
    private String loginResult;

    // Original WSO2 log timestamp (e.g. "2026-04-03 16:49:27,034")
    @JsonProperty("log_timestamp")
    private String logTimestamp;

    // Logstash ingestion timestamp
    @JsonProperty("@timestamp")
    private String timestamp;
}