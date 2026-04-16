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

        Map<String, List<LoginAttemptDocument>> suspiciousIps =
                bruteForceLoginService.fetchSuspiciousIps(
                        rule.getThresholdValue(),
                        rule.getLookbackSeconds()
                );

        if (suspiciousIps.isEmpty()) {
            log.debug("BruteForceLoginUseCase — no suspicious IPs found.");
            return;
        }

        log.info("BruteForceLoginUseCase — {} suspicious IP(s) detected.", suspiciousIps.size());

        suspiciousIps.forEach((ip, attempts) -> {

            List<String> portals = attempts.stream()
                    .map(LoginAttemptDocument::getServiceProviderName)
                    .filter(p -> p != null && !p.isBlank())
                    .distinct()
                    .collect(Collectors.toList());

            List<String> usernames = attempts.stream()
                    .map(LoginAttemptDocument::getUserName)
                    .filter(u -> u != null && !u.isBlank())
                    .distinct()
                    .collect(Collectors.toList());

            String lastAttemptTime = attempts.stream()
                    .map(LoginAttemptDocument::getLogTimestamp)
                    .filter(t -> t != null && !t.isBlank())
                    .reduce((first, second) -> second)
                    .map(t -> t.contains("]") ? t.substring(0, t.indexOf("]")).trim() : t)
                    .orElse(attempts.get(0).getTimestamp());

            AlertMessage alert = AlertMessage.builder()
                    .useCaseType("BRUTE_FORCE_LOGIN")
                    .action("Repeated failed login attempts detected")
                    .resourceType("Portal")
                    .resourceName(String.join(", ", portals))
                    .ipAddress(ip)
                    .failedAttempts(attempts.size())
                    .portalsTargeted(portals)
                    .usernamesTried(usernames)
                    .timestamp(lastAttemptTime)
                    .build();

            // groupingKey = IP address — groups all alerts from same IP into one incident
            incidentService.handleAlert(rule, ip, alert);
        });
    }
}