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

        if (alert.getPerformedBy() != null) {
            sb.append(String.format("  Performed by  : %s%n", alert.getPerformedBy()));
        }

        sb.append(String.format("  Action        : %s%n", alert.getAction()));

        if (alert.getResourceType() != null) {
            sb.append(String.format("  Resource      : %s / %s%n", alert.getResourceType(), alert.getResourceName()));
        }

        // THRESHOLD
        if (alert.getCount() != null) {
            sb.append(String.format("  Call count    : %d%n", alert.getCount()));
        }

        // HIGH_LATENCY
        if (alert.getResponseLatency() != null) {
            sb.append(String.format("  Response time : %d ms%n", alert.getResponseLatency()));
        }
        if (alert.getBackendLatency() != null) {
            sb.append(String.format("  Backend time  : %d ms%n", alert.getBackendLatency()));
        }
        if (alert.getResponseCode() != null) {
            sb.append(String.format("  Response code : %d%n", alert.getResponseCode()));
        }

        // FAULTY
        if (alert.getErrorCode() != null) {
            sb.append(String.format("  Error code    : %d%n", alert.getErrorCode()));
        }
        if (alert.getErrorMessage() != null) {
            sb.append(String.format("  Error message : %s%n", alert.getErrorMessage()));
        }

        // BRUTE_FORCE_LOGIN
        if (alert.getIpAddress() != null) {
            sb.append(String.format("  IP address    : %s%n", alert.getIpAddress()));
        }
        if (alert.getFailedAttempts() != null) {
            sb.append(String.format("  Failed attempts: %d%n", alert.getFailedAttempts()));
        }
        if (alert.getPortalsTargeted() != null && !alert.getPortalsTargeted().isEmpty()) {
            sb.append(String.format("  Portals tried : %s%n", String.join(", ", alert.getPortalsTargeted())));
        }
        if (alert.getUsernamesTried() != null && !alert.getUsernamesTried().isEmpty()) {
            sb.append(String.format("  Usernames tried: %s%n", String.join(", ", alert.getUsernamesTried())));
        }

        sb.append(String.format("  Timestamp     : %s%n", alert.getTimestamp()));
        sb.append("╚══════════════════════════════════════╝");

        log.info(sb.toString());
    }
}