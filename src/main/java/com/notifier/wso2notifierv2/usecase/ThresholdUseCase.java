package com.notifier.wso2notifierv2.usecase;

import com.notifier.wso2notifierv2.model.AlertMessage;
import com.notifier.wso2notifierv2.notification.NotificationService;
import com.notifier.wso2notifierv2.service.ThresholdEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThresholdUseCase {

    private final ThresholdEventService thresholdEventService;
    private final NotificationService notificationService;

    @Value("${usecases.threshold.min-count}")
    private int minCount;

    @Value("${usecases.threshold.cooldown-seconds}")
    private long cooldownSeconds;

    // Tracks when the last alert fired per API name
    // key = apiName, value = time the alert last fired
    private final Map<String, Instant> lastAlertTime = new ConcurrentHashMap<>();

    @Scheduled(fixedDelayString = "${usecases.threshold.poll-interval-ms}")
    public void run() {
        log.debug("ThresholdUseCase — polling ES...");

        Map<String, Long> exceededApis = thresholdEventService.fetchApiCallCountsExceedingThreshold();

        if (exceededApis.isEmpty()) {
            log.debug("ThresholdUseCase — no APIs exceeded the threshold.");
            return;
        }

        exceededApis.forEach((apiName, count) -> {
            Instant now = Instant.now();
            Instant lastFired = lastAlertTime.get(apiName);

            // Skip if we already alerted for this API within the cooldown window
            if (lastFired != null && now.isBefore(lastFired.plusSeconds(cooldownSeconds))) {
                log.debug("ThresholdUseCase — [{}] still in cooldown, skipping alert.", apiName);
                return;
            }

            // Fire the alert and record the time
            lastAlertTime.put(apiName, now);

            AlertMessage alert = AlertMessage.builder()
                    .useCaseType("THRESHOLD")
                    .action("API call count reached " + count + " (threshold: " + minCount + ")")
                    .resourceType("API")
                    .resourceName(apiName)
                    .count(count)
                    .timestamp(now.toString())
                    .build();

            notificationService.notify(alert);
        });
    }
}