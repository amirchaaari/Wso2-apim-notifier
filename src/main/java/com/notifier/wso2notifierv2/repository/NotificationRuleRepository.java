package com.notifier.wso2notifierv2.repository;

import com.notifier.wso2notifierv2.entity.NotificationRule;
import com.notifier.wso2notifierv2.entity.UseCaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRuleRepository extends JpaRepository<NotificationRule, Long> {

    Optional<NotificationRule> findByUseCaseTypeAndEnabledTrue(UseCaseType useCaseType);
    // Used by the CRUD API — returns rule regardless of enabled state
    Optional<NotificationRule> findByUseCaseType(UseCaseType useCaseType);

    // Used by the CUSTOM rule scheduler
    List<NotificationRule> findByUseCaseTypeAndEnabled(UseCaseType useCaseType, boolean enabled);
}