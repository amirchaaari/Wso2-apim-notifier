package com.notifier.wso2notifierv2.usecase;

import com.notifier.wso2notifierv2.entity.NotificationRule;
import com.notifier.wso2notifierv2.entity.UseCaseType;
import com.notifier.wso2notifierv2.model.AlertMessage;
import com.notifier.wso2notifierv2.model.HighLatencyEventDocument;
import com.notifier.wso2notifierv2.repository.NotificationRuleRepository;
import com.notifier.wso2notifierv2.service.HighLatencyEventService;
import com.notifier.wso2notifierv2.service.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HighLatencyUseCase {

    private final HighLatencyEventService highLatencyEventService;
    private final NotificationRuleRepository ruleRepository;
    private final IncidentService incidentService;

    @Scheduled(fixedDelay = 30000) // Poll every 30s
    public void run() {
        NotificationRule rule = ruleRepository.findByUseCaseType(UseCaseType.HIGH_LATENCY)
                .orElse(null);

        if (rule == null || !rule.isEnabled()) {
            return;
        }

        log.debug("HighLatencyUseCase — polling ES...");

        List<HighLatencyEventDocument> events = highLatencyEventService.fetchHighLatencyEvents(rule);

        if (events.isEmpty()) {
            return;
        }

        log.info("HighLatencyUseCase — found {} high latency event(s).", events.size());

        for (HighLatencyEventDocument event : events) {
            AlertMessage alert = AlertMessage.builder()
                    .useCaseType(rule.getUseCaseType().name())
                    .severity(rule.getSeverity())
                    .performedBy(event.getUserName())
                    .action("Response latency exceeded " + rule.getThresholdValue() + "ms")
                    .resourceType("API")
                    .resourceName(event.getApiName())
                    .responseLatency(event.getResponseLatency())
                    .backendLatency(event.getBackendLatency())
                    .responseCode(event.getResponseCode())
                    .timestamp(event.getTimestamp())
                    .build();

            // Use apiName as groupingKey for high latency incidents
            incidentService.handleAlert(rule, event.getApiName(), alert);
        }
    }
}