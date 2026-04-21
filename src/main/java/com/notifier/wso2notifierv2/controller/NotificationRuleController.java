package com.notifier.wso2notifierv2.controller;

import com.notifier.wso2notifierv2.dto.RuleRequest;
import com.notifier.wso2notifierv2.dto.RuleResponse;
import com.notifier.wso2notifierv2.entity.NotificationRule;
import com.notifier.wso2notifierv2.entity.UseCaseType;
import com.notifier.wso2notifierv2.repository.NotificationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class NotificationRuleController {

    private final NotificationRuleRepository ruleRepository;
    private final com.notifier.wso2notifierv2.repository.NotificationTargetRepository targetRepository;

    @GetMapping
    public ResponseEntity<List<RuleResponse>> getAllRules() {
        List<RuleResponse> rules = ruleRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/{type}")
    public ResponseEntity<RuleResponse> getRule(@PathVariable String type) {
        try {
            UseCaseType useCaseType = UseCaseType.valueOf(type.toUpperCase());
            return ruleRepository.findByUseCaseType(useCaseType)
                    .map(this::toResponse)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{type}")
    public ResponseEntity<RuleResponse> updateRule(
            @PathVariable String type,
            @RequestBody RuleRequest request) {

        try {
            UseCaseType useCaseType = UseCaseType.valueOf(type.toUpperCase());
            return ruleRepository.findByUseCaseType(useCaseType)
                    .map(rule -> {
                        if (request.getThresholdValue() != null)
                            rule.setThresholdValue(request.getThresholdValue());
                        if (request.getLookbackSeconds() != null)
                            rule.setLookbackSeconds(request.getLookbackSeconds());
                        if (request.getApiNames() != null)
                            rule.setApiNames(request.getApiNames());
                        if (request.getErrorCodes() != null)
                            rule.setErrorCodes(request.getErrorCodes());
                        if (request.getSeverity() != null)
                            rule.setSeverity(request.getSeverity());
                        if (request.getDescription() != null)
                            rule.setDescription(request.getDescription());

                        if (request.getTargetIds() != null) {
                            rule.setTargets(
                                    new java.util.HashSet<>(targetRepository.findAllById(request.getTargetIds())));
                        }

                        NotificationRule saved = ruleRepository.save(rule);
                        log.info("Rule {} updated", type);
                        return ResponseEntity.ok(toResponse(saved));
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{type}/enable")
    public ResponseEntity<RuleResponse> enableRule(@PathVariable String type) {
        return toggleRule(type, true);
    }

    @PatchMapping("/{type}/disable")
    public ResponseEntity<RuleResponse> disableRule(@PathVariable String type) {
        return toggleRule(type, false);
    }

    private ResponseEntity<RuleResponse> toggleRule(String type, boolean enabled) {
        try {
            UseCaseType useCaseType = UseCaseType.valueOf(type.toUpperCase());
            return ruleRepository.findByUseCaseType(useCaseType)
                    .map(rule -> {
                        rule.setEnabled(enabled);
                        NotificationRule saved = ruleRepository.save(rule);
                        log.info("Rule {} {}", type, enabled ? "enabled" : "disabled");
                        return ResponseEntity.ok(toResponse(saved));
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private RuleResponse toResponse(NotificationRule rule) {
        return RuleResponse.builder()
                .id(rule.getId())
                .useCaseType(rule.getUseCaseType().name())
                .enabled(rule.isEnabled())
                .severity(rule.getSeverity())
                .thresholdValue(rule.getThresholdValue())
                .lookbackSeconds(rule.getLookbackSeconds())
                .errorCodes(rule.getErrorCodes())
                .apiNames(rule.getApiNames())
                .description(rule.getDescription())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .targets(rule.getTargets())
                .build();
    }
}
