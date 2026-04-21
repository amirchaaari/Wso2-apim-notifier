package com.notifier.wso2notifierv2.config;

import com.notifier.wso2notifierv2.entity.NotificationRule;
import com.notifier.wso2notifierv2.entity.Severity;
import com.notifier.wso2notifierv2.entity.UseCaseType;
import com.notifier.wso2notifierv2.repository.NotificationRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DatabaseSeeder {

    private final NotificationRuleRepository ruleRepository;

    @Bean
    public ApplicationRunner seedDefaultRules() {
        return args -> {
            seedRule(UseCaseType.BRUTE_FORCE_LOGIN, Severity.HIGH, 1, 300, "*", null,
                    "Alert when an IP fails login 3 times in 5 minutes across any portal.");
            seedRule(UseCaseType.FAULTY, Severity.MEDIUM, 5, 60, "*", "900800,900801,900802,900803,900804",
                    "Alert when an API returns 5+ faulty events within 1 minute.");
            seedRule(UseCaseType.HIGH_LATENCY, Severity.MEDIUM, 2000, 60, "*", null,
                    "Alert when API response latency exceeds 2000ms.");
            seedRule(UseCaseType.THRESHOLD, Severity.LOW, 1000, 60, "*", null, "Basic call volume threshold alert.");
            seedRule(UseCaseType.DELETE_EVENT, Severity.CRITICAL, 1, 3600, "*", null,
                    "Instant alert on any API or Application deletion.");
            seedRule(UseCaseType.PENDING_WORKFLOWS, Severity.MEDIUM, 1, 3600, "*", null,
                    "Alert when new pending workflows are detected in WSO2.");
        };
    }

    private void seedRule(UseCaseType type, Severity severity, int threshold, int lookback, String apiNames,
            String errorCodes, String description) {
        if (ruleRepository.findByUseCaseType(type).isPresent()) {
            return; // Already exists — don't overwrite user changes
        }

        NotificationRule rule = new NotificationRule();
        rule.setUseCaseType(type);
        rule.setSeverity(severity);
        rule.setEnabled(type != UseCaseType.THRESHOLD); // THRESHOLD disabled by default
        rule.setThresholdValue(threshold);
        rule.setLookbackSeconds(lookback);
        rule.setApiNames(apiNames);
        rule.setErrorCodes(errorCodes);
        rule.setDescription(description);
        rule.setCreatedAt(Instant.now());
        rule.setUpdatedAt(Instant.now());
        ruleRepository.save(rule);
    }
}
