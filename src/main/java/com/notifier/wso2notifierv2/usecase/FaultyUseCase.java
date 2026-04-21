package com.notifier.wso2notifierv2.usecase;

import com.notifier.wso2notifierv2.model.AlertMessage;
import com.notifier.wso2notifierv2.model.FaultyEventDocument;
import com.notifier.wso2notifierv2.entity.NotificationRule;
import com.notifier.wso2notifierv2.service.FaultyEventService;
import com.notifier.wso2notifierv2.entity.UseCaseType;
import com.notifier.wso2notifierv2.repository.NotificationRuleRepository;
import com.notifier.wso2notifierv2.service.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FaultyUseCase {

    private final FaultyEventService faultyEventService;
    private final IncidentService incidentService;
    private final NotificationRuleRepository ruleRepository;

    @Scheduled(fixedDelayString = "${usecases.faulty.poll-interval-ms}")
    public void run() {
        log.debug("FaultyUseCase — polling ES...");

        NotificationRule rule = ruleRepository.findByUseCaseType(UseCaseType.FAULTY).orElse(null);
        if (rule == null || !rule.isEnabled()) {
            return;
        }

        java.util.Optional<java.time.Instant> globalReset = java.util.Optional
                .ofNullable(incidentService.getGlobalLatestResolutionTime(rule));
        Map<String, List<FaultyEventDocument>> groupedEvents = faultyEventService.fetchFaultyEvents(rule, globalReset);

        if (groupedEvents.isEmpty()) {
            return;
        }

        for (Map.Entry<String, List<FaultyEventDocument>> entry : groupedEvents.entrySet()) {
            String apiName = entry.getKey();
            List<FaultyEventDocument> events = entry.getValue();

            // Filter events to only include those AFTER the latest resolution
            java.time.Instant latestResolution = incidentService.getLatestResolutionTime(rule, apiName);
            List<FaultyEventDocument> newEvents = events;

            if (latestResolution != null) {
                newEvents = events.stream()
                        .filter(e -> {
                            try {
                                java.time.Instant eventTime = java.time.Instant.parse(e.getTimestamp());
                                return eventTime.isAfter(latestResolution);
                            } catch (Exception ex) {
                                return true;
                            }
                        })
                        .collect(java.util.stream.Collectors.toList());
            }

            // Apply Database Min Count Threshold!
            Integer threshold = rule.getThresholdValue() != null ? rule.getThresholdValue() : 1;
            if (newEvents.size() >= threshold) {
                // Pick the most recent one for context
                FaultyEventDocument sample = newEvents.get(newEvents.size() - 1);

                AlertMessage alert = AlertMessage.builder()
                        .useCaseType(UseCaseType.FAULTY.name())
                        .severity(rule.getSeverity())
                        .performedBy(sample.getUserName())
                        .action("Faulty API rate exceeded limit. " + newEvents.size() + " errors detected.")
                        .resourceType("API")
                        .resourceName(apiName)
                        .errorCode(sample.getErrorCode())
                        .errorMessage(sample.getErrorMessage())
                        .timestamp(sample.getTimestamp())
                        .count((long) newEvents.size())
                        .build();

                // Deduplication & spam protection through IncidentService
                incidentService.handleAlert(rule, apiName, alert);
            }
        }
    }
}