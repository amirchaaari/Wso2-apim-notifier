package com.notifier.wso2notifierv2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifier.wso2notifierv2.entity.*;
import com.notifier.wso2notifierv2.model.AlertMessage;
import com.notifier.wso2notifierv2.notification.NotificationService;
import com.notifier.wso2notifierv2.repository.AlertHistoryRepository;
import com.notifier.wso2notifierv2.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    /**
     * Called by each use case when a condition is detected.
     * - If no OPEN incident exists for this rule + groupingKey → create one and notify
     * - If OPEN incident exists → increment count, update last_seen, skip notification
     */
    @Transactional
    public void handleAlert(NotificationRule rule, String groupingKey, AlertMessage alert) {
        List<Incident> activeIncidents = incidentRepository
                .findByRuleAndGroupingKeyAndStatusIn(rule, groupingKey, List.of(IncidentStatus.OPEN, IncidentStatus.ACKNOWLEDGED));

        if (!activeIncidents.isEmpty()) {
            // Incident already open or acked
            Incident incident = activeIncidents.get(0);
            
            // Apply the same cooldown logic: only increment alertCount and update lastSeen 
            // if the lookback window has passed since we last updated it.
            // This prevents the "Detections" count from spam-incrementing every 30 seconds due to the sliding window matching the same ES logs.
            if (incident.getLastSeen() != null && Instant.now().minusSeconds(rule.getLookbackSeconds()).isBefore(incident.getLastSeen())) {
                log.debug("Skipping alertCount increment for key {} — still within the lookback window of the last detection.", groupingKey);
                return;
            }

            // A full lookback window has passed, meaning these must be new detections (or a persistent attack)
            incident.setAlertCount(incident.getAlertCount() + 1);
            incident.setLastSeen(Instant.now());
            incidentRepository.save(incident);

            logAlertHistory(incident, alert, false);

            log.debug("Incident #{} updated — alertCount={}, groupingKey={}",
                    incident.getId(), incident.getAlertCount(), groupingKey);

        } else {
            // Check if recently resolved to prevent recreating based on old ES logs
            Optional<Incident> recentlyResolved = incidentRepository
                .findFirstByRuleAndGroupingKeyAndStatusOrderByResolvedAtDesc(rule, groupingKey, IncidentStatus.RESOLVED);
            
            if (recentlyResolved.isPresent() && recentlyResolved.get().getResolvedAt() != null) {
                // If now - lookback overlap with resolved time, we skip to wait for old logs to phase out
                if (Instant.now().minusSeconds(rule.getLookbackSeconds()).isBefore(recentlyResolved.get().getResolvedAt())) {
                    log.debug("Skipping incident creation for key {} — previous incident recently resolved and logs are still within the sliding window.", groupingKey);
                    return;
                }
            }

            // New incident — create, log, notify
            Incident incident = new Incident();
            incident.setRule(rule);
            incident.setGroupingKey(groupingKey);
            incident.setStatus(IncidentStatus.OPEN);
            incident.setFirstSeen(Instant.now());
            incident.setLastSeen(Instant.now());
            incident.setAlertCount(1);
            incidentRepository.save(incident);

            logAlertHistory(incident, alert, true);

            notificationService.notify(alert);

            log.info("New incident #{} created — useCase={}, groupingKey={}",
                    incident.getId(), rule.getUseCaseType(), groupingKey);
        }
    }

    private void logAlertHistory(Incident incident, AlertMessage alert, boolean notificationSent) {
        AlertHistory history = new AlertHistory();
        history.setIncident(incident);
        history.setDetectedAt(Instant.now());
        history.setNotificationSent(notificationSent);

        try {
            history.setDetails(objectMapper.writeValueAsString(alert));
        } catch (Exception e) {
            history.setDetails("{}");
        }

        alertHistoryRepository.save(history);
    }
}