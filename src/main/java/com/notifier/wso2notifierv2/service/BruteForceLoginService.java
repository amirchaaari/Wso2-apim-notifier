package com.notifier.wso2notifierv2.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.notifier.wso2notifierv2.model.LoginAttemptDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BruteForceLoginService {

    private final ElasticsearchClient esClient;

    private static final String INDEX = "wso2_audit_logs";

    /**
     * Params come from the DB rule — not hardcoded via @Value.
     */
    public Map<String, List<LoginAttemptDocument>> fetchSuspiciousIps(
            int minAttempts, int lookbackSeconds) {

        String from = Instant.now().minusSeconds(lookbackSeconds).toString();

        try {
            SearchResponse<LoginAttemptDocument> response = esClient.search(s -> s
                            .index(INDEX)
                            .query(q -> q
                                    .bool(b -> b
                                            .must(m -> m
                                                    .term(t -> t
                                                            .field("loginResult.keyword")
                                                            .value("failed")
                                                    )
                                            )
                                            .must(m -> m
                                                    .term(t -> t
                                                            .field("eventType.keyword")
                                                            .value("login_attempt")
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
                            .size(10000),
                    LoginAttemptDocument.class
            );

            Map<String, List<LoginAttemptDocument>> suspiciousIps = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(doc -> doc != null && doc.getRemoteAddress() != null)
                    .collect(Collectors.groupingBy(LoginAttemptDocument::getRemoteAddress))
                    .entrySet().stream()
                    .filter(e -> e.getValue().size() >= minAttempts)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            log.debug("Brute force check — {} suspicious IP(s) (minAttempts={}, lookback={}s)",
                    suspiciousIps.size(), minAttempts, lookbackSeconds);
            return suspiciousIps;

        } catch (Exception e) {
            log.error("Failed to query ES index [{}]: {}", INDEX, e.getMessage(), e);
            return Map.of();
        }
    }
}