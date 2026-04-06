package com.notifier.wso2notifierv2.usecase;

import com.notifier.wso2notifierv2.model.AlertMessage;
import com.notifier.wso2notifierv2.model.FaultyEventDocument;
import com.notifier.wso2notifierv2.notification.NotificationService;
import com.notifier.wso2notifierv2.service.FaultyEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FaultyUseCase {

    private final FaultyEventService faultyEventService;
    private final NotificationService notificationService;

    @Scheduled(fixedDelayString = "${usecases.faulty.poll-interval-ms}")
    public void run() {
        log.debug("FaultyUseCase — polling ES...");

        List<FaultyEventDocument> events = faultyEventService.fetchFaultyEvents();

        if (events.isEmpty()) {
            log.debug("FaultyUseCase — no faulty events found.");
            return;
        }

        log.info("FaultyUseCase — found {} faulty event(s).", events.size());

        for (FaultyEventDocument event : events) {
            AlertMessage alert = AlertMessage.builder()
                    .useCaseType("FAULTY")
                    .performedBy(event.getUserName())
                    .action("Faulty request — error code " + event.getErrorCode())
                    .resourceType("API")
                    .resourceName(event.getApiName())
                    .errorCode(event.getErrorCode())
                    .errorMessage(event.getErrorMessage())
                    .timestamp(event.getTimestamp())
                    .build();

            notificationService.notify(alert);
        }
    }
}