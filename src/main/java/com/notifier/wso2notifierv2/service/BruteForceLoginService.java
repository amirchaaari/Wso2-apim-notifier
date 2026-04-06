package com.notifier.wso2notifierv2.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.notifier.wso2notifierv2.model.LoginAttemptDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${usecases.brute-force.min-attempts}")
    private int minAttempts;

    @Value("${usecases.brute-force.lookback-seconds}")
    private int lookbackSeconds;

    /**
     * Queries ES for failed login attempts within the lookback window.
     * Groups by IP address.
     * Returns only IPs that reached or exceeded min-attempts,
     * with the list of documents for that IP (to extract portals/usernames tried).
     */
    public Map<String, List<LoginAttemptDocument>> fetchSuspiciousIps() {
        String from = Instant.now().minusSeconds(lookbackSeconds).toString();

        try {
            SearchResponse<LoginAttemptDocument> response = esClient.search(s -> s
                            .index(INDEX)
                            .query(q -> q
                                    .bool(b -> b
                                            // Only failed login attempts
                                            .must(m -> m
                                                    .term(t -> t
                                                            .field("loginResult.keyword")
                                                            .value("failed")
                                                    )
                                            )
                                            // Only login_attempt events
                                            .must(m -> m
                                                    .term(t -> t
                                                            .field("eventType.keyword")
                                                            .value("login_attempt")
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
                            .size(10000),
                    LoginAttemptDocument.class
            );

            // Group by IP, keep only IPs with >= minAttempts failures
            Map<String, List<LoginAttemptDocument>> suspiciousIps = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(doc -> doc != null && doc.getRemoteAddress() != null)
                    .collect(Collectors.groupingBy(LoginAttemptDocument::getRemoteAddress))
                    .entrySet().stream()
                    .filter(e -> e.getValue().size() >= minAttempts)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            log.debug("Brute force check — {} suspicious IP(s) found (min-attempts: {}, lookback: {}s)",
                    suspiciousIps.size(), minAttempts, lookbackSeconds);
            return suspiciousIps;

        } catch (Exception e) {
            log.error("Failed to query Elasticsearch index [{}]: {}", INDEX, e.getMessage(), e);
            return Map.of();
        }
    }
}