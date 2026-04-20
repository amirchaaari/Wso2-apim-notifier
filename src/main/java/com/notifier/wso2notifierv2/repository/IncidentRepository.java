package com.notifier.wso2notifierv2.repository;

import com.notifier.wso2notifierv2.entity.Incident;
import com.notifier.wso2notifierv2.entity.IncidentStatus;
import com.notifier.wso2notifierv2.entity.NotificationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByRuleAndGroupingKeyAndStatusIn(
            NotificationRule rule,
            String groupingKey,
            java.util.List<IncidentStatus> statuses
    );

    Optional<Incident> findFirstByRuleAndGroupingKeyAndStatusOrderByResolvedAtDesc(
            NotificationRule rule,
            String groupingKey,
            IncidentStatus status
    );

    List<Incident> findByRuleUseCaseType(com.notifier.wso2notifierv2.entity.UseCaseType useCaseType);
}