package com.notifier.wso2notifierv2.controller;

import com.notifier.wso2notifierv2.dto.BruteForceRuleRequest;
import com.notifier.wso2notifierv2.dto.BruteForceRuleResponse;
import com.notifier.wso2notifierv2.entity.NotificationRule;
import com.notifier.wso2notifierv2.entity.UseCaseType;
import com.notifier.wso2notifierv2.repository.NotificationRuleRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/rules/brute-force")
@RequiredArgsConstructor
public class BruteForceRuleController {

    private final NotificationRuleRepository ruleRepository;

    // ============================
    // GET — current rule
    // ============================
    @GetMapping
    public ResponseEntity<BruteForceRuleResponse> getRule() {
        return ruleRepository.findByUseCaseType(UseCaseType.BRUTE_FORCE_LOGIN)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ============================
    // PUT — update rule config
    // ============================
    @PutMapping
    public ResponseEntity<BruteForceRuleResponse> updateRule(
            @Valid @RequestBody BruteForceRuleRequest request) {

        return ruleRepository.findByUseCaseType(UseCaseType.BRUTE_FORCE_LOGIN)
                .map(rule -> {
                    rule.setThresholdValue(request.getMinAttempts());
                    rule.setLookbackSeconds(request.getLookbackSeconds());
                    rule.setSeverity(request.getSeverity());
                    rule.setDescription(request.getDescription());
                    NotificationRule saved = ruleRepository.save(rule);
                    log.info("BruteForce rule updated — minAttempts={}, lookback={}s",
                            saved.getThresholdValue(), saved.getLookbackSeconds());
                    return ResponseEntity.ok(toResponse(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ============================
    // PATCH — enable
    // ============================
    @PatchMapping("/enable")
    public ResponseEntity<BruteForceRuleResponse> enableRule() {
        return ruleRepository.findByUseCaseType(UseCaseType.BRUTE_FORCE_LOGIN)
                .map(rule -> {
                    rule.setEnabled(true);
                    NotificationRule saved = ruleRepository.save(rule);
                    log.info("BruteForce rule enabled.");
                    return ResponseEntity.ok(toResponse(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ============================
    // PATCH — disable
    // ============================
    @PatchMapping("/disable")
    public ResponseEntity<BruteForceRuleResponse> disableRule() {
        return ruleRepository.findByUseCaseType(UseCaseType.BRUTE_FORCE_LOGIN)
                .map(rule -> {
                    rule.setEnabled(false);
                    NotificationRule saved = ruleRepository.save(rule);
                    log.info("BruteForce rule disabled.");
                    return ResponseEntity.ok(toResponse(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ============================
    // Mapper — entity to response
    // ============================
    private BruteForceRuleResponse toResponse(NotificationRule rule) {
        return BruteForceRuleResponse.builder()
                .id(rule.getId())
                .useCaseType(rule.getUseCaseType().name())
                .enabled(rule.isEnabled())
                .severity(rule.getSeverity())
                .minAttempts(rule.getThresholdValue())
                .lookbackSeconds(rule.getLookbackSeconds())
                .description(rule.getDescription())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}