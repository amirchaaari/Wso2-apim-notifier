package com.notifier.wso2notifierv2.usecase;

import com.notifier.wso2notifierv2.model.AlertMessage;
import com.notifier.wso2notifierv2.model.DeleteEventDocument;
import com.notifier.wso2notifierv2.notification.NotificationService;
import com.notifier.wso2notifierv2.service.DeleteEventService;
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
    private final NotificationService notificationService;

    @Scheduled(fixedDelayString = "${usecases.delete.poll-interval-ms}")
    public void run() {
        log.debug("DeleteUseCase — polling ES...");

        List<DeleteEventDocument> events = deleteEventService.fetchRecentDeleteEvents();

        if (events.isEmpty()) {
            log.debug("DeleteUseCase — no new delete events found.");
            return;
        }

        log.info("DeleteUseCase — found {} delete event(s).", events.size());

        for (DeleteEventDocument event : events) {
            AlertMessage alert = AlertMessage.builder()
                    .useCaseType("DELETE_EVENT")
                    .performedBy(event.getPerformedBy())
                    .action(event.getAction())
                    .resourceType(event.getType())
                    .resourceName(event.getInfo() != null ? event.getInfo().getName() : "N/A")
                    .timestamp(event.getLogTimestamp())
                    .build();

            notificationService.notify(alert);
        }
    }
}