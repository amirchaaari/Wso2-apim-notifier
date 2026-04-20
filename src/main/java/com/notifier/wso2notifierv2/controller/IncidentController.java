package com.notifier.wso2notifierv2.controller;

import com.notifier.wso2notifierv2.dto.IncidentResponse;
import com.notifier.wso2notifierv2.entity.Incident;
import com.notifier.wso2notifierv2.entity.IncidentStatus;
import com.notifier.wso2notifierv2.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentRepository incidentRepository;

    @GetMapping
    public ResponseEntity<List<IncidentResponse>> getAllIncidents(@RequestParam(required = false) String rule) {
        List<Incident> incidents;
        if (rule != null && !rule.isEmpty()) {
            try {
                com.notifier.wso2notifierv2.entity.UseCaseType useCaseType = com.notifier.wso2notifierv2.entity.UseCaseType.valueOf(rule.toUpperCase());
                incidents = incidentRepository.findByRuleUseCaseType(useCaseType);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            incidents = incidentRepository.findAll();
        }

        List<IncidentResponse> response = incidents.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/acknowledge")
    public ResponseEntity<IncidentResponse> acknowledge(@PathVariable Long id, @RequestParam(required = false, defaultValue = "admin") String user) {
        return incidentRepository.findById(id)
                .map(incident -> {
                    incident.setStatus(IncidentStatus.ACKNOWLEDGED);
                    incident.setAcknowledgedBy(user);
                    incident.setAcknowledgedAt(Instant.now());
                    Incident saved = incidentRepository.save(incident);
                    return ResponseEntity.ok(toResponse(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<IncidentResponse> resolve(@PathVariable Long id) {
        return incidentRepository.findById(id)
                .map(incident -> {
                    incident.setStatus(IncidentStatus.RESOLVED);
                    incident.setResolvedAt(Instant.now());
                    Incident saved = incidentRepository.save(incident);
                    return ResponseEntity.ok(toResponse(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private IncidentResponse toResponse(Incident incident) {
        String latestDetails = null;
        if (incident.getAlertHistory() != null && !incident.getAlertHistory().isEmpty()) {
            latestDetails = incident.getAlertHistory().get(incident.getAlertHistory().size() - 1).getDetails();
        }

        return IncidentResponse.builder()
                .id(incident.getId())
                .ruleName(incident.getRule().getUseCaseType().name())
                .groupingKey(incident.getGroupingKey())
                .status(incident.getStatus())
                .alertCount(incident.getAlertCount())
                .firstSeen(incident.getFirstSeen())
                .lastSeen(incident.getLastSeen())
                .acknowledgedBy(incident.getAcknowledgedBy())
                .acknowledgedAt(incident.getAcknowledgedAt())
                .resolvedAt(incident.getResolvedAt())
                .details(latestDetails)
                .build();
    }
}
