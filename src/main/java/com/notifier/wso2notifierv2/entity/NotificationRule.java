package com.notifier.wso2notifierv2.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Entity
@Table(name = "notification_rule")
public class NotificationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "use_case_type", nullable = false)
    private UseCaseType useCaseType;

    @Column(nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity = Severity.MEDIUM;

    // Comma-separated API names or * for all
    @Column(name = "api_names")
    private String apiNames;

    // Threshold value — meaning depends on use case:
    // HIGH_LATENCY → max response latency in ms
    // THRESHOLD → max API call count
    // BRUTE_FORCE → max failed login attempts
    @Column(name = "threshold_value")
    private Integer thresholdValue;

    @Column(name = "lookback_seconds", nullable = false)
    private Integer lookbackSeconds = 40;

    // Comma-separated error codes for FAULTY use case
    @Column(name = "error_codes")
    private String errorCodes;

    @Column(length = 500)
    private String description;

    // ── Custom rule fields (only used when useCaseType = CUSTOM) ──────────────
    // Human-readable name for the rule (since multiple CUSTOM rules can exist)
    @Column(name = "custom_name")
    private String customName;

    // Target Elasticsearch index (e.g. "apim_event_response")
    @Column(name = "custom_es_index")
    private String customEsIndex;

    // Raw Elasticsearch JSON query body stored as TEXT
    @Column(name = "custom_es_query", columnDefinition = "TEXT")
    private String customEsQuery;

    // Field used to group results and deduplicate alerts (e.g. "apiName.keyword")
    @Column(name = "grouping_field")
    private String groupingField;

    // Minimum number of matching docs to trigger an alert
    @Column(name = "min_hits")
    private Integer minHits = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "rule_target", joinColumns = @JoinColumn(name = "rule_id"), inverseJoinColumns = @JoinColumn(name = "target_id"))
    private Set<NotificationTarget> targets = new HashSet<>();

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Incident> incidents = new java.util.ArrayList<>();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}