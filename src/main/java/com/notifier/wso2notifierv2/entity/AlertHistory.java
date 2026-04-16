package com.notifier.wso2notifierv2.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "alert_history")
public class AlertHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt = Instant.now();

    // Raw JSON details of what triggered this alert
    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "notification_sent", nullable = false)
    private boolean notificationSent = false;
}