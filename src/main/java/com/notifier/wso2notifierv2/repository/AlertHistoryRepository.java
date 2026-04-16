package com.notifier.wso2notifierv2.repository;

import com.notifier.wso2notifierv2.entity.AlertHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {
}