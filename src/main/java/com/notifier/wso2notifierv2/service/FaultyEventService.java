package com.notifier.wso2notifierv2.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.notifier.wso2notifierv2.model.FaultyEventDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaultyEventService {

    private final ElasticsearchClient esClient;

    private static final String INDEX = "apim_event_faulty";
    private static final String WILDCARD = "*";

    // Comma-separated: "PizzaShackAPI,OrderAPI" or "*" for all
    @Value("${usecases.faulty.api-names}")
    private String apiNamesRaw;

    // Comma-separated: "900803,900804"
    @Value("${usecases.faulty.error-codes}")
    private String errorCodesRaw;

    @Value("${usecases.faulty.lookback-seconds}")
    private int lookbackSeconds;

    public List<FaultyEventDocument> fetchFaultyEvents() {
        String from = Instant.now().minusSeconds(lookbackSeconds).toString();

        List<String> apiNames = Arrays.stream(apiNamesRaw.split(","))
                .map(String::trim)
                .collect(Collectors.toList());

        boolean allApis = apiNames.contains(WILDCARD);

        List<FieldValue> apiNameValues = apiNames.stream()
                .map(FieldValue::of)
                .collect(Collectors.toList());

        List<FieldValue> errorCodeValues = Arrays.stream(errorCodesRaw.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .map(FieldValue::of)
                .collect(Collectors.toList());

        try {
            SearchResponse<FaultyEventDocument> response = esClient.search(s -> s
                            .index(INDEX)
                            .query(q -> q
                                    .bool(b -> {
                                        // Error code filter — always applied
                                        b.must(m -> m
                                                .terms(t -> t
                                                        .field("errorCode")
                                                        .terms(tv -> tv.value(errorCodeValues))
                                                )
                                        );
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
                            .size(100),
                    FaultyEventDocument.class
            );

            List<FaultyEventDocument> events = response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

            log.debug("Fetched {} faulty event(s) from ES (errorCodes: {}, apiNames: {})",
                    events.size(), errorCodesRaw, apiNamesRaw);
            return events;

        } catch (Exception e) {
            log.error("Failed to query Elasticsearch index [{}]: {}", INDEX, e.getMessage(), e);
            return List.of();
        }
    }
}