package com.notifier.wso2notifierv2.usecase;

import com.notifier.wso2notifierv2.model.AlertMessage;
import com.notifier.wso2notifierv2.model.LoginAttemptDocument;
import com.notifier.wso2notifierv2.notification.NotificationService;
import com.notifier.wso2notifierv2.service.BruteForceLoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BruteForceLoginUseCase {

    private final BruteForceLoginService bruteForceLoginService;
    private final NotificationService notificationService;

    @Scheduled(fixedDelayString = "${usecases.brute-force.poll-interval-ms}")
    public void run() {
        log.debug("BruteForceLoginUseCase — polling ES...");

        Map<String, List<LoginAttemptDocument>> suspiciousIps =
                bruteForceLoginService.fetchSuspiciousIps();

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

            // Use the timestamp of the most recent attempt
            String lastAttemptTime = attempts.stream()
                    .map(LoginAttemptDocument::getLogTimestamp)
                    .filter(t -> t != null && !t.isBlank())
                    .reduce((first, second) -> second)
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

            notificationService.notify(alert);
        });
    }
}