package com.notifier.wso2notifierv2.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.notifier.wso2notifierv2.model.DeleteEventDocument;
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
public class DeleteEventService {

    private final ElasticsearchClient esClient;

    private static final String INDEX = "wso2_audit_delete";

    @Value("${scheduler.delete-use-case.lookback-seconds}")
    private int lookbackSeconds;

    public List<DeleteEventDocument> fetchRecentDeleteEvents() {
        String from = Instant.now().minusSeconds(lookbackSeconds).toString();

        try {
            SearchResponse<DeleteEventDocument> response = esClient.search(s -> s
                            .index(INDEX)
                            .query(q -> q
                                    .bool(b -> b
                                            .must(m -> m
                                                    .term(t -> t
                                                            .field("eventType.keyword")
                                                            .value("delete_confirmed")
                                                    )
                                            )
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
                    DeleteEventDocument.class
            );

            List<DeleteEventDocument> events = response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

            log.debug("Fetched {} delete_confirmed event(s) from ES", events.size());
            return events;

        } catch (Exception e) {
            log.error("Failed to query Elasticsearch index [{}]: {}", INDEX, e.getMessage(), e);
            return List.of();
        }
    }
}