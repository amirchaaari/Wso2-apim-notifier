package com.notifier.wso2notifierv2.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.notifier.wso2notifierv2.entity.NotificationRule;
import com.notifier.wso2notifierv2.entity.UseCaseType;
import com.notifier.wso2notifierv2.model.AlertMessage;
import com.notifier.wso2notifierv2.repository.NotificationRuleRepository;
import com.notifier.wso2notifierv2.service.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomRuleUseCase {

  private final NotificationRuleRepository ruleRepository;
  private final IncidentService incidentService;
  private final ElasticsearchClient esClient;

  @Scheduled(fixedDelayString = "${usecases.custom.poll-interval-ms}")
  public void run() {
    List<NotificationRule> customRules = ruleRepository
        .findByUseCaseTypeAndEnabled(UseCaseType.CUSTOM, true);

    if (customRules.isEmpty()) {
      return;
    }

    log.debug("CustomRuleUseCase — evaluating {} custom rule(s)...", customRules.size());

    for (NotificationRule rule : customRules) {
      try {
        evaluate(rule);
      } catch (Exception e) {
        log.error("CustomRuleUseCase — failed to evaluate rule [{}]: {}",
            rule.getCustomName(), e.getMessage(), e);
      }
    }
  }

  private void evaluate(NotificationRule rule) throws Exception {
    if (rule.getCustomEsIndex() == null || rule.getCustomEsQuery() == null) {
      log.warn("CustomRuleUseCase — rule [{}] is missing ES index or query, skipping.", rule.getCustomName());
      return;
    }

    long now = Instant.now().toEpochMilli();
    long lookbackMs = rule.getLookbackSeconds() * 1000L;
    long fromEpochMs = now - lookbackMs;

    // Shift window forward if there's a recent resolution
    Instant globalResolution = incidentService.getGlobalLatestResolutionTime(rule);
    if (globalResolution != null && globalResolution.toEpochMilli() > fromEpochMs) {
      fromEpochMs = globalResolution.toEpochMilli();
    }

    int minHits = rule.getMinHits() != null ? rule.getMinHits() : 1;
    String groupingField = rule.getGroupingField() != null ? rule.getGroupingField() : "_index";

    // Build the query: wrap the user's custom query in a bool + time range filter
    String wrappedQuery = buildWrappedQuery(rule.getCustomEsQuery(), fromEpochMs);

    SearchResponse<Map<String, Object>> response = esClient.search(
        s -> s.index(rule.getCustomEsIndex())
            .withJson(new StringReader(wrappedQuery))
            .size(10000),
        (Class<Map<String, Object>>) (Class<?>) Map.class);

    if (response.hits().hits().isEmpty()) {
      return;
    }

    // Group hits by the configured grouping field
    Map<String, List<co.elastic.clients.elasticsearch.core.search.Hit<Map<String, Object>>>> groups = response.hits()
        .hits().stream()
        .filter(hit -> hit.source() != null)
        .collect(Collectors.groupingBy(
            hit -> {
              Object fieldVal = extractField(hit.source(), groupingField);
              return fieldVal != null ? fieldVal.toString() : "unknown";
            }));

    groups.forEach((groupingKey, hits) -> {
      // Filter hits to only include those AFTER the latest resolution for this key
      Instant latestResolution = incidentService.getLatestResolutionTime(rule, groupingKey);
      List<co.elastic.clients.elasticsearch.core.search.Hit<Map<String, Object>>> newHits = hits;

      if (latestResolution != null) {
        newHits = hits.stream()
            .filter(hit -> {
              Object ts = hit.source().get("@timestamp");
              if (ts == null)
                return true;
              try {
                Instant hitTime;
                if (ts instanceof Long) {
                  hitTime = Instant.ofEpochMilli((Long) ts);
                } else {
                  hitTime = Instant.parse(ts.toString());
                }
                return hitTime.isAfter(latestResolution);
              } catch (Exception e) {
                return true;
              }
            })
            .collect(Collectors.toList());
      }

      long count = newHits.size();
      if (count >= minHits) {
        AlertMessage alert = AlertMessage.builder()
            .useCaseType(UseCaseType.CUSTOM.name())
            .severity(rule.getSeverity())
            .action("Custom rule matched: " + count + " hit(s) in " + rule.getLookbackSeconds() + "s")
            .resourceType("Custom")
            .resourceName(groupingKey)
            .count(count)
            .timestamp(Instant.now().toString())
            .build();

        incidentService.handleAlert(rule, groupingKey, alert);
      } else if (newHits.size() < hits.size()) {
        log.debug(
            "CustomRuleUseCase — rule [{}] group [{}] has {} hits in ES, but only {} are new since resolution. Skipping.",
            rule.getCustomName(), groupingKey, hits.size(), newHits.size());
      }
    });

    log.debug("CustomRuleUseCase — rule [{}] evaluated: {} group(s) matched", rule.getCustomName(), groups.size());
  }

  /**
   * Wraps the user's custom query inside a bool filter that also enforces the
   * lookback time window.
   * Uses epoch_millis for the timestamp range to avoid format issues with WSO2
   * APIM ES indices.
   */
  private String buildWrappedQuery(String userQuery, long fromEpochMs) {
    return """
        {
          "query": {
            "bool": {
              "must": [
                %s
              ],
              "filter": [
                {
                  "range": {
                    "@timestamp": {
                      "gte": %d,
                      "format": "epoch_millis"
                    }
                  }
                }
              ]
            }
          }
        }
        """.formatted(userQuery, fromEpochMs);
  }

  /**
   * Extracts a nested field value from the hit source map.
   * Handles simple field names (e.g., "apiName") only.
   * The .keyword suffix is stripped — Elasticsearch returns the base field in
   * _source.
   */
  private Object extractField(Map<?, ?> source, String fieldName) {
    // Strip the .keyword suffix if present
    String key = fieldName.replace(".keyword", "");
    return source.get(key);
  }
}
