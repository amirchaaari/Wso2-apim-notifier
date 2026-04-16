package com.notifier.wso2notifierv2.repository;

import com.notifier.wso2notifierv2.entity.NotificationTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationTargetRepository extends JpaRepository<NotificationTarget, Long> {
}