package com.notifier.wso2notifierv2.usecase;

import com.notifier.wso2notifierv2.model.AlertMessage;
import com.notifier.wso2notifierv2.model.HighLatencyEventDocument;
import com.notifier.wso2notifierv2.notification.NotificationService;
import com.notifier.wso2notifierv2.service.HighLatencyEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HighLatencyUseCase {

    private final HighLatencyEventService highLatencyEventService;
    private final NotificationService notificationService;

    @Value("${usecases.high-latency.threshold-ms}")
    private long thresholdMs;

    @Scheduled(fixedDelayString = "${usecases.high-latency.poll-interval-ms}")
    public void run() {
        log.debug("HighLatencyUseCase — polling ES...");

        List<HighLatencyEventDocument> events = highLatencyEventService.fetchHighLatencyEvents();

        if (events.isEmpty()) {
            log.debug("HighLatencyUseCase — no high latency events found.");
            return;
        }

        log.info("HighLatencyUseCase — found {} high latency event(s).", events.size());

        for (HighLatencyEventDocument event : events) {
            AlertMessage alert = AlertMessage.builder()
                    .useCaseType("HIGH_LATENCY")
                    .performedBy(event.getUserName())
                    .action("Response latency exceeded " + thresholdMs + "ms")
                    .resourceType("API")
                    .resourceName(event.getApiName())
                    .responseLatency(event.getResponseLatency())
                    .backendLatency(event.getBackendLatency())
                    .responseCode(event.getResponseCode())
                    .timestamp(event.getTimestamp())
                    .build();

            notificationService.notify(alert);
        }
    }
}