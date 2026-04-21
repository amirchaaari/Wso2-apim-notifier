package com.notifier.wso2notifierv2.usecase;

import com.notifier.wso2notifierv2.entity.NotificationRule;
import com.notifier.wso2notifierv2.entity.UseCaseType;
import com.notifier.wso2notifierv2.model.AlertMessage;
import com.notifier.wso2notifierv2.model.DeleteEventDocument;
import com.notifier.wso2notifierv2.repository.NotificationRuleRepository;
import com.notifier.wso2notifierv2.service.DeleteEventService;
import com.notifier.wso2notifierv2.service.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteUseCase {

    private final DeleteEventService deleteEventService;
    private final NotificationRuleRepository ruleRepository;
    private final IncidentService incidentService;

    @Scheduled(fixedDelayString = "${usecases.delete.poll-interval-ms}") // Poll every 30s
    public void run() {
        NotificationRule rule = ruleRepository.findByUseCaseType(UseCaseType.DELETE_EVENT)
                .orElse(null);

        if (rule == null || !rule.isEnabled()) {
            return;
        }

        log.debug("DeleteUseCase — polling ES...");

        java.util.Optional<java.time.Instant> globalReset = java.util.Optional
                .ofNullable(incidentService.getGlobalLatestResolutionTime(rule));
        List<DeleteEventDocument> events = deleteEventService.fetchRecentDeleteEvents(rule, globalReset);

        if (events.isEmpty()) {
            return;
        }

        log.info("DeleteUseCase — found {} delete event(s).", events.size());

        for (DeleteEventDocument event : events) {
            String resourceName = event.getInfo() != null ? event.getInfo().getName() : "N/A";

            // Filter events to only include those AFTER the latest resolution
            java.time.Instant latestResolution = incidentService.getLatestResolutionTime(rule, resourceName);
            if (latestResolution != null) {
                try {
                    java.time.Instant eventTime = java.time.Instant.parse(event.getLogTimestamp());
                    if (!eventTime.isAfter(latestResolution)) {
                        log.debug("DeleteUseCase — event for {} occurred at {}, before latest resolution {}. Skipping.",
                                resourceName, event.getLogTimestamp(), latestResolution);
                        continue;
                    }
                } catch (Exception e) {
                    // Fallback to processing if parsing fails
                }
            }

            AlertMessage alert = AlertMessage.builder()
                    .useCaseType(rule.getUseCaseType().name())
                    .severity(rule.getSeverity())
                    .performedBy(event.getPerformedBy())
                    .action(event.getAction())
                    .resourceType(event.getType())
                    .resourceName(resourceName)
                    .timestamp(event.getLogTimestamp())
                    .build();

            // Use resourceName as groupingKey for delete events
            incidentService.handleAlert(rule, resourceName, alert);
        }
    }
}