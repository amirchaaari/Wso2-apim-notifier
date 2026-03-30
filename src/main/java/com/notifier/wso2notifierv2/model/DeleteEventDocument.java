package com.notifier.wso2notifierv2.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeleteEventDocument {

    @JsonProperty("performedBy")
    private String performedBy;

    @JsonProperty("action")
    private String action;

    @JsonProperty("type")
    private String type;

    @JsonProperty("info")
    private Info info;

    @JsonProperty("log_timestamp")
    private String logTimestamp;

    @JsonProperty("@timestamp")
    private String timestamp;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Info {
        @JsonProperty("name")
        private String name;
    }
}