package com.notifier.wso2notifierv2.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.notifier.wso2notifierv2.dto.CustomRuleRequest;
import com.notifier.wso2notifierv2.dto.CustomRuleValidateResponse;
import com.notifier.wso2notifierv2.entity.NotificationRule;
import com.notifier.wso2notifierv2.entity.Severity;
import com.notifier.wso2notifierv2.entity.UseCaseType;
import com.notifier.wso2notifierv2.repository.NotificationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.StringReader;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/custom-rules")
@RequiredArgsConstructor
public class CustomRuleController {

  private final NotificationRuleRepository ruleRepository;
  private final com.notifier.wso2notifierv2.repository.NotificationTargetRepository targetRepository;
  private final ElasticsearchClient esClient;

  @GetMapping
  public ResponseEntity<List<NotificationRule>> getAllCustomRules() {
    // Return all custom rules (both enabled and disabled)
    List<NotificationRule> allCustom = ruleRepository.findAll().stream()
        .filter(r -> r.getUseCaseType() == UseCaseType.CUSTOM)
        .collect(Collectors.toList());
    return ResponseEntity.ok(allCustom);
  }

  @PostMapping
  public ResponseEntity<NotificationRule> createCustomRule(@RequestBody CustomRuleRequest request) {
    NotificationRule rule = new NotificationRule();
    rule.setUseCaseType(UseCaseType.CUSTOM);
    rule.setEnabled(true);
    rule.setSeverity(request.getSeverity() != null ? request.getSeverity() : Severity.MEDIUM);
    rule.setCustomName(request.getName());
    rule.setCustomEsIndex(request.getEsIndex());
    rule.setCustomEsQuery(request.getEsQuery());
    rule.setGroupingField(request.getGroupingField());
    rule.setLookbackSeconds(request.getLookbackSeconds() != null ? request.getLookbackSeconds() : 60);
    rule.setMinHits(request.getMinHits() != null ? request.getMinHits() : 1);
    rule.setDescription(request.getDescription());

    if (request.getTargetIds() != null && !request.getTargetIds().isEmpty()) {
      rule.setTargets(new java.util.HashSet<>(targetRepository.findAllById(request.getTargetIds())));
    }

    NotificationRule saved = ruleRepository.save(rule);
    return ResponseEntity.ok(saved);
  }

  @PutMapping("/{id}")
  public ResponseEntity<NotificationRule> updateCustomRule(@PathVariable Long id,
      @RequestBody CustomRuleRequest request) {
    return ruleRepository.findById(id)
        .filter(r -> r.getUseCaseType() == UseCaseType.CUSTOM)
        .map(rule -> {
          rule.setCustomName(request.getName());
          rule.setCustomEsIndex(request.getEsIndex());
          rule.setCustomEsQuery(request.getEsQuery());
          rule.setGroupingField(request.getGroupingField());
          rule.setLookbackSeconds(request.getLookbackSeconds() != null ? request.getLookbackSeconds() : 60);
          rule.setMinHits(request.getMinHits() != null ? request.getMinHits() : 1);
          rule.setDescription(request.getDescription());
          if (request.getSeverity() != null)
            rule.setSeverity(request.getSeverity());
          if (request.getEnabled() != null)
            rule.setEnabled(request.getEnabled());

          if (request.getTargetIds() != null) {
            rule.setTargets(new java.util.HashSet<>(targetRepository.findAllById(request.getTargetIds())));
          }

          return ResponseEntity.ok(ruleRepository.save(rule));
        })
        .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCustomRule(@PathVariable Long id) {
    return ruleRepository.findById(id)
        .filter(r -> r.getUseCaseType() == UseCaseType.CUSTOM)
        .map(rule -> {
          ruleRepository.delete(rule);
          return new ResponseEntity<Void>(org.springframework.http.HttpStatus.OK);
        })
        .orElse(new ResponseEntity<>(org.springframework.http.HttpStatus.NOT_FOUND));
  }

  @PatchMapping("/{id}/toggle")
  public ResponseEntity<NotificationRule> toggleCustomRule(@PathVariable Long id, @RequestParam boolean enabled) {
    return ruleRepository.findById(id)
        .filter(r -> r.getUseCaseType() == UseCaseType.CUSTOM)
        .map(rule -> {
          rule.setEnabled(enabled);
          return ResponseEntity.ok(ruleRepository.save(rule));
        })
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Validates/previews the rule query against Elasticsearch without saving.
   * Returns hit count so the user can see how many documents would match.
   */
  @PostMapping("/validate")
  public ResponseEntity<CustomRuleValidateResponse> validateQuery(@RequestBody CustomRuleRequest request) {
    if (request.getEsIndex() == null || request.getEsQuery() == null) {
      return ResponseEntity.badRequest().build();
    }

    try {
      int lookback = request.getLookbackSeconds() != null ? request.getLookbackSeconds() : 60;
      long fromEpochMs = Instant.now().minusSeconds(lookback).toEpochMilli();

      String wrappedQuery = """
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
          """.formatted(request.getEsQuery(), fromEpochMs);

      SearchResponse<Map> response = esClient.search(
          s -> s.index(request.getEsIndex())
              .withJson(new StringReader(wrappedQuery))
              .size(0), // Just count
          Map.class);

      long totalHits = response.hits().total() != null ? response.hits().total().value() : 0;

      return ResponseEntity.ok(CustomRuleValidateResponse.builder()
          .hitCount(totalHits)
          .index(request.getEsIndex())
          .lookbackSeconds(lookback)
          .valid(true)
          .build());

    } catch (Exception e) {
      log.error("Custom rule validation failed: {}", e.getMessage());
      return ResponseEntity.ok(CustomRuleValidateResponse.builder()
          .valid(false)
          .errorMessage(e.getMessage())
          .build());
    }
  }
}
