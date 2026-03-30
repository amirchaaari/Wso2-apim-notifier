package com.notifier.wso2notifierv2.notification;

import com.notifier.wso2notifierv2.model.AlertMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ConsoleNotifier implements NotificationService {

    @Override
    public void notify(AlertMessage alert) {
        log.info("""
                ╔══════════════════════════════════════╗
                  USE CASE TRIGGERED: {}
                ╠══════════════════════════════════════╣
                  Performed by : {}
                  Action       : {}
                  Resource     : {} / {}
                  Timestamp    : {}
                ╚══════════════════════════════════════╝
                """,
                alert.getUseCaseType(),
                alert.getPerformedBy(),
                alert.getAction(),
                alert.getResourceType(),
                alert.getResourceName(),
                alert.getTimestamp()
        );
    }
}