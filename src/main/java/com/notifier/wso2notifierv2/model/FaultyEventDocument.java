package com.notifier.wso2notifierv2.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FaultyEventDocument {

    @JsonProperty("apiName")
    private String apiName;

    @JsonProperty("userName")
    private String userName;

    @JsonProperty("errorCode")
    private Integer errorCode;

    @JsonProperty("errorMessage")
    private String errorMessage;

    @JsonProperty("nextAccessTime")
    private String nextAccessTime;

    @JsonProperty("@timestamp")
    private String timestamp;
}