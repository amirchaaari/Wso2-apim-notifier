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
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThresholdUseCase {

    private final ThresholdEventService thresholdEventService;
    private final NotificationRuleRepository ruleRepository;
    private final IncidentService incidentService;

    @Scheduled(fixedDelay = 30000) // Poll every 30s
    public void run() {
        NotificationRule rule = ruleRepository.findByUseCaseType(UseCaseType.THRESHOLD)
                .orElse(null);

        if (rule == null || !rule.isEnabled()) {
            return;
        }

        log.debug("ThresholdUseCase — polling ES...");

        Map<String, Long> exceededApis = thresholdEventService.fetchApiCallCountsExceedingThreshold(rule);

        if (exceededApis.isEmpty()) {
            return;
        }

        exceededApis.forEach((apiName, count) -> {
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