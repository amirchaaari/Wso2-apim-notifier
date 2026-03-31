package com.notifier.wso2notifierv2.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.notifier.wso2notifierv2.model.HighLatencyEventDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HighLatencyEventService {

    private final ElasticsearchClient esClient;

    private static final String INDEX = "apim_event_response";

    @Value("${usecases.high-latency.api-name}")
    private String apiName;

    @Value("${usecases.high-latency.threshold-ms}")
    private long thresholdMs;

    @Value("${usecases.high-latency.lookback-seconds}")
    private int lookbackSeconds;

    /**
     * Queries Elasticsearch for response events where:
     * - apiName matches the configured value
     * - responseLatency exceeds the configured threshold
     * - @timestamp is within the lookback window
     */
    public List<HighLatencyEventDocument> fetchHighLatencyEvents() {
        String from = Instant.now().minusSeconds(lookbackSeconds).toString();

        try {
            SearchResponse<HighLatencyEventDocument> response = esClient.search(s -> s
                            .index(INDEX)
                            .query(q -> q
                                    .bool(b -> b
                                            // Match the configured API name
                                            .must(m -> m
                                                    .term(t -> t
                                                            .field("apiName.keyword")
                                                            .value(apiName)
                                                    )
                                            )
                                            // Only events exceeding the latency threshold
                                            .must(m -> m
                                                    .range(r -> r
                                                            .number(n -> n
                                                                    .field("responseLatency")
                                                                    .gt((double) thresholdMs)
                                                            )
                                                    )
                                            )
                                            // Within the lookback window
                                            .must(m -> m
                                                    .range(r -> r
                                                            .date(d -> d
                                                                    .field("@timestamp")
                                                                    .gte(from)
                                                            )
                                                    )
                                            )
                                    )
                            )
                            .size(100),
                    HighLatencyEventDocument.class
            );

            List<HighLatencyEventDocument> events = response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

            log.debug("Fetched {} high latency event(s) from ES (threshold: {}ms, api: {})",
                    events.size(), thresholdMs, apiName);
            return events;

        } catch (Exception e) {
            log.error("Failed to query Elasticsearch index [{}]: {}", INDEX, e.getMessage(), e);
            return List.of();
        }
    }
}