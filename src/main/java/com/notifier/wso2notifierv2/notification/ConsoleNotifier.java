package com.notifier.wso2notifierv2.notification;

import com.notifier.wso2notifierv2.model.AlertMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ConsoleNotifier implements NotificationService {

    @Override
    public void notify(AlertMessage alert) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════════════════════╗\n");
        sb.append(String.format("  USE CASE TRIGGERED: %s%n", alert.getUseCaseType()));
        sb.append("╠══════════════════════════════════════╣\n");
        sb.append(String.format("  Performed by  : %s%n", alert.getPerformedBy()));
        sb.append(String.format("  Action        : %s%n", alert.getAction()));
        sb.append(String.format("  Resource      : %s / %s%n", alert.getResourceType(), alert.getResourceName()));

        // Print latency details if present
        if (alert.getResponseLatency() != null) {
            sb.append(String.format("  Response time : %d ms%n", alert.getResponseLatency()));
        }
        if (alert.getBackendLatency() != null) {
            sb.append(String.format("  Backend time  : %d ms%n", alert.getBackendLatency()));
        }
        if (alert.getResponseCode() != null) {
            sb.append(String.format("  Response code : %d%n", alert.getResponseCode()));
        }

        sb.append(String.format("  Timestamp     : %s%n", alert.getTimestamp()));
        sb.append("╚══════════════════════════════════════╝");

        log.info(sb.toString());
    }
}