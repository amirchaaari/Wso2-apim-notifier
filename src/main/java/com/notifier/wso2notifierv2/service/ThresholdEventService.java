package com.notifier.wso2notifierv2.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.notifier.wso2notifierv2.model.ThresholdEventDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThresholdEventService {

    private final ElasticsearchClient esClient;

    private static final String INDEX = "apim_event_response";
    private static final String WILDCARD = "*";

    public Map<String, Long> fetchApiCallCountsExceedingThreshold(com.notifier.wso2notifierv2.entity.NotificationRule rule) {
        int lookbackSeconds = rule.getLookbackSeconds() != null ? rule.getLookbackSeconds() : 60;
        int minCount = rule.getThresholdValue() != null ? rule.getThresholdValue().intValue() : 1000;
        String apiNamesRaw = rule.getApiNames() != null ? rule.getApiNames() : WILDCARD;

        String from = Instant.now().minusSeconds(lookbackSeconds).toString();

        List<String> apiNames = Arrays.stream(apiNamesRaw.split(","))
                .map(String::trim)
                .collect(Collectors.toList());

        boolean allApis = apiNames.contains(WILDCARD);

        List<FieldValue> apiNameValues = apiNames.stream()
                .map(FieldValue::of)
                .collect(Collectors.toList());

        try {
            SearchResponse<ThresholdEventDocument> response = esClient.search(s -> s
                            .index(INDEX)
                            .query(q -> q
                                    .bool(b -> {
                                        // Time window filter — always applied
                                        b.must(m -> m
                                                .range(r -> r
                                                        .date(d -> d
                                                                .field("@timestamp")
                                                                .gte(from)
                                                        )
                                                )
                                        );
                                        // API name filter — skip if wildcard
                                        if (!allApis) {
                                            b.must(m -> m
                                                    .terms(t -> t
                                                            .field("apiName.keyword")
                                                            .terms(tv -> tv.value(apiNameValues))
                                                    )
                                            );
                                        }
                                        return b;
                                    })
                            )
                            .size(10000),
                    ThresholdEventDocument.class
            );

            // Group by apiName, count, keep only those >= minCount
            Map<String, Long> counts = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(doc -> doc != null && doc.getApiName() != null)
                    .collect(Collectors.groupingBy(
                            ThresholdEventDocument::getApiName,
                            Collectors.counting()
                    ))
                    .entrySet().stream()
                    .filter(e -> e.getValue() >= minCount)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            log.debug("Threshold check — {} API(s) exceeded {} calls in last {}s",
                    counts.size(), minCount, lookbackSeconds);
            return counts;

        } catch (Exception e) {
            log.error("Failed to query Elasticsearch index [{}]: {}", INDEX, e.getMessage(), e);
            return Map.of();
        }
    }
}