package com.notifier.wso2notifierv2.usecase;

import com.notifier.wso2notifierv2.entity.NotificationRule;
import com.notifier.wso2notifierv2.entity.UseCaseType;
import com.notifier.wso2notifierv2.model.AlertMessage;
import com.notifier.wso2notifierv2.model.LoginAttemptDocument;
import com.notifier.wso2notifierv2.repository.NotificationRuleRepository;
import com.notifier.wso2notifierv2.service.BruteForceLoginService;
import com.notifier.wso2notifierv2.service.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BruteForceLoginUseCase {

        private final BruteForceLoginService bruteForceLoginService;
        private final IncidentService incidentService;
        private final NotificationRuleRepository ruleRepository;

        @Scheduled(fixedDelayString = "${usecases.brute-force.poll-interval-ms}")
        public void run() {
                // Load rule from DB — skip if disabled or not found
                Optional<NotificationRule> ruleOpt = ruleRepository
                                .findByUseCaseTypeAndEnabledTrue(UseCaseType.BRUTE_FORCE_LOGIN);

                if (ruleOpt.isEmpty()) {
                        log.debug("BruteForceLoginUseCase — rule not found or disabled, skipping.");
                        return;
                }

                NotificationRule rule = ruleOpt.get();

                log.debug("BruteForceLoginUseCase — polling ES (minAttempts={}, lookback={}s)...",
                                rule.getThresholdValue(), rule.getLookbackSeconds());

                java.util.Optional<java.time.Instant> globalReset = java.util.Optional
                                .ofNullable(incidentService.getGlobalLatestResolutionTime(rule));

                Map<String, List<LoginAttemptDocument>> suspiciousIps = bruteForceLoginService.fetchSuspiciousIps(
                                rule.getThresholdValue(),
                                rule.getLookbackSeconds(),
                                globalReset);

                if (suspiciousIps.isEmpty()) {
                        log.debug("BruteForceLoginUseCase — no suspicious IPs found.");
                        return;
                }

                log.info("BruteForceLoginUseCase — {} suspicious IP(s) detected.", suspiciousIps.size());

                suspiciousIps.forEach((ip, attempts) -> {
                        // Filter attempts to only include those AFTER the latest resolution
                        Instant latestResolution = incidentService.getLatestResolutionTime(rule, ip);
                        List<LoginAttemptDocument> newAttempts = attempts;

                        if (latestResolution != null) {
                                newAttempts = attempts.stream()
                                                .filter(a -> {
                                                        try {
                                                                Instant attemptTime = Instant
                                                                                .parse(a.getLogTimestamp());
                                                                return attemptTime.isAfter(latestResolution);
                                                        } catch (Exception e) {
                                                                // Fallback to raw timestamp if parsing fails
                                                                return true;
                                                        }
                                                })
                                                .collect(Collectors.toList());
                        }

                        if (newAttempts.size() < rule.getThresholdValue()) {
                                log.debug("BruteForceLoginUseCase — IP {} has {} attempts in ES, but only {} are new since resolution. Skipping.",
                                                ip, attempts.size(), newAttempts.size());
                                return;
                        }

                        List<String> portals = newAttempts.stream()
                                        .map(LoginAttemptDocument::getServiceProviderName)
                                        .filter(p -> p != null && !p.isBlank())
                                        .distinct()
                                        .collect(Collectors.toList());

                        List<String> usernames = newAttempts.stream()
                                        .map(LoginAttemptDocument::getUserName)
                                        .filter(u -> u != null && !u.isBlank())
                                        .distinct()
                                        .collect(Collectors.toList());

                        String lastAttemptTime = newAttempts.stream()
                                        .map(LoginAttemptDocument::getLogTimestamp)
                                        .filter(t -> t != null && !t.isBlank())
                                        .reduce((first, second) -> second)
                                        .map(t -> t.contains("]") ? t.substring(0, t.indexOf("]")).trim() : t)
                                        .orElse(newAttempts.get(0).getTimestamp());

                        AlertMessage alert = AlertMessage.builder()
                                        .useCaseType("BRUTE_FORCE_LOGIN")
                                        .severity(rule.getSeverity())
                                        .action("Repeated failed login attempts detected")
                                        .resourceType("Portal")
                                        .resourceName(String.join(", ", portals))
                                        .ipAddress(ip)
                                        .failedAttempts(newAttempts.size())
                                        .portalsTargeted(portals)
                                        .usernamesTried(usernames)
                                        .timestamp(lastAttemptTime)
                                        .build();

                        // groupingKey = IP address — groups all alerts from same IP into one incident
                        incidentService.handleAlert(rule, ip, alert);
                });
        }
}