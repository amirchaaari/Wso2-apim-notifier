package com.notifier.wso2notifierv2.usecase;

import com.notifier.wso2notifierv2.model.AlertMessage;
import com.notifier.wso2notifierv2.model.FaultyEventDocument;
import com.notifier.wso2notifierv2.notification.NotificationService;
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

        Map<String, List<FaultyEventDocument>> groupedEvents = faultyEventService.fetchFaultyEvents(rule);

        if (groupedEvents.isEmpty()) {
            return;
        }

        for (Map.Entry<String, List<FaultyEventDocument>> entry : groupedEvents.entrySet()) {
            String apiName = entry.getKey();
            List<FaultyEventDocument> events = entry.getValue();
            
            // Apply Database Min Count Threshold!
            Integer threshold = rule.getThresholdValue() != null ? rule.getThresholdValue() : 1;
            if (events.size() >= threshold) {
                // Pick the most recent one for context
                FaultyEventDocument sample = events.get(events.size() - 1);
                
                AlertMessage alert = AlertMessage.builder()
                        .useCaseType(UseCaseType.FAULTY.name())
                        .performedBy(sample.getUserName())
                        .action("Faulty API rate exceeded limit. " + events.size() + " errors detected.")
                        .resourceType("API")
                        .resourceName(apiName)
                        .errorCode(sample.getErrorCode())
                        .errorMessage(sample.getErrorMessage())
                        .timestamp(sample.getTimestamp())
                        .count((long) events.size())
                        .build();

                // Deduplication & spam protection through IncidentService
                incidentService.handleAlert(rule, apiName, alert);
            }
        }
    }
}