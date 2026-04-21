package com.notifier.wso2notifierv2.usecase;

import com.notifier.wso2notifierv2.entity.NotificationRule;
import com.notifier.wso2notifierv2.entity.UseCaseType;
import com.notifier.wso2notifierv2.model.AlertMessage;
import com.notifier.wso2notifierv2.repository.NotificationRuleRepository;
import com.notifier.wso2notifierv2.service.IncidentService;
import com.notifier.wso2notifierv2.service.ThresholdEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThresholdUseCase {

    private final ThresholdEventService thresholdEventService;
    private final NotificationRuleRepository ruleRepository;
    private final IncidentService incidentService;

    @Scheduled(fixedDelayString = "${usecases.threshold.poll-interval-ms}") // Poll every 30s
    public void run() {
        NotificationRule rule = ruleRepository.findByUseCaseType(UseCaseType.THRESHOLD)
                .orElse(null);

        if (rule == null || !rule.isEnabled()) {
            return;
        }

        log.debug("ThresholdUseCase — polling ES...");

        java.util.Optional<java.time.Instant> globalReset = java.util.Optional
                .ofNullable(incidentService.getGlobalLatestResolutionTime(rule));
        Map<String, List<com.notifier.wso2notifierv2.model.ThresholdEventDocument>> apiDocuments = thresholdEventService
                .fetchApiCallDocuments(rule, globalReset);

        if (apiDocuments.isEmpty()) {
            return;
        }

        apiDocuments.forEach((apiName, docs) -> {
            // Filter docs to only include those AFTER the latest resolution
            Instant latestResolution = incidentService.getLatestResolutionTime(rule, apiName);
            List<com.notifier.wso2notifierv2.model.ThresholdEventDocument> newDocs = docs;

            if (latestResolution != null) {
                newDocs = docs.stream()
                        .filter(d -> {
                            try {
                                Instant eventTime = Instant.parse(d.getTimestamp());
                                return eventTime.isAfter(latestResolution);
                            } catch (Exception e) {
                                return true;
                            }
                        })
                        .collect(Collectors.toList());
            }

            long count = newDocs.size();
            if (count < rule.getThresholdValue()) {
                log.debug(
                        "ThresholdUseCase — API {} has {} calls in ES, but only {} are new since resolution. Skipping (threshold={}).",
                        apiName, docs.size(), count, rule.getThresholdValue());
                return;
            }

            AlertMessage alert = AlertMessage.builder()
                    .useCaseType(rule.getUseCaseType().name())
                    .severity(rule.getSeverity())
                    .action("API call count reached " + count + " (threshold: " + rule.getThresholdValue() + ")")
                    .resourceType("API")
                    .resourceName(apiName)
                    .count(count)
                    .timestamp(Instant.now().toString())
                    .build();

            // IncidentService handles the cooldown/lookback logic dynamically
            incidentService.handleAlert(rule, apiName, alert);
        });
    }
}