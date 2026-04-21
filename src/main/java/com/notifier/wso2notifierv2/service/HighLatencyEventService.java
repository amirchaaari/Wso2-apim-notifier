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
        private static final String WILDCARD = "*";

        /**
         * Queries Elasticsearch for response events where:
         * - apiName matches the configured value
         * - responseLatency exceeds the configured threshold
         * - @timestamp is within the lookback window
         */
        public List<HighLatencyEventDocument> fetchHighLatencyEvents(
                        com.notifier.wso2notifierv2.entity.NotificationRule rule,
                        java.util.Optional<Instant> earliestTimestamp) {
                int lookbackSeconds = rule.getLookbackSeconds() != null ? rule.getLookbackSeconds() : 60;
                long thresholdMs = rule.getThresholdValue() != null ? rule.getThresholdValue() : 2000L;

                Instant fromInstant = Instant.now().minusSeconds(lookbackSeconds);
                if (earliestTimestamp.isPresent() && earliestTimestamp.get().isAfter(fromInstant)) {
                        fromInstant = earliestTimestamp.get();
                }
                String from = fromInstant.toString();

                String apiNamesRaw = rule.getApiNames() != null ? rule.getApiNames() : WILDCARD;
                List<String> apiNames = java.util.Arrays.stream(apiNamesRaw.split(","))
                                .map(String::trim)
                                .collect(Collectors.toList());

                boolean allApis = apiNames.contains(WILDCARD);

                List<co.elastic.clients.elasticsearch._types.FieldValue> apiNameValues = apiNames.stream()
                                .map(co.elastic.clients.elasticsearch._types.FieldValue::of)
                                .collect(Collectors.toList());

                try {
                        SearchResponse<HighLatencyEventDocument> response = esClient.search(s -> s
                                        .index(INDEX)
                                        .query(q -> q
                                                        .bool(b -> {
                                                                // Match the configured API name
                                                                if (!allApis) {
                                                                        b.must(m -> m
                                                                                        .terms(t -> t
                                                                                                        .field("apiName.keyword")
                                                                                                        .terms(tv -> tv.value(
                                                                                                                        apiNameValues))));
                                                                }
                                                                // Only events exceeding the latency threshold
                                                                b.must(m -> m
                                                                                .range(r -> r
                                                                                                .number(n -> n
                                                                                                                .field("responseLatency")
                                                                                                                .gt((double) thresholdMs))));
                                                                // Within the lookback window
                                                                b.must(m -> m
                                                                                .range(r -> r
                                                                                                .date(d -> d
                                                                                                                .field("@timestamp")
                                                                                                                .gte(from))));
                                                                return b;
                                                        }))
                                        .size(100),
                                        HighLatencyEventDocument.class);

                        List<HighLatencyEventDocument> events = response.hits().hits().stream()
                                        .map(Hit::source)
                                        .collect(Collectors.toList());

                        log.debug("Fetched {} high latency event(s) from ES (threshold: {}ms, apiNames: {})",
                                        events.size(), thresholdMs, apiNamesRaw);
                        return events;

                } catch (Exception e) {
                        log.error("Failed to query Elasticsearch index [{}]: {}", INDEX, e.getMessage(), e);
                        return List.of();
                }
        }
}