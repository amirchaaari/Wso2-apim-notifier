package com.notifier.wso2notifierv2.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "notification_target")
public class NotificationTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g. "dev_team", "admin", "security_team"
    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    // Email address or Telegram chat_id depending on channel
    @Column(nullable = false)
    private String contact;

    @Column(nullable = false)
    private boolean enabled = true;
}