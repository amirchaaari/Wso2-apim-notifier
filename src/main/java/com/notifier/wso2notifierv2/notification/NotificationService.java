package com.notifier.wso2notifierv2.notification;

import com.notifier.wso2notifierv2.model.AlertMessage;

public interface NotificationService {

    void notify(AlertMessage alert);
}