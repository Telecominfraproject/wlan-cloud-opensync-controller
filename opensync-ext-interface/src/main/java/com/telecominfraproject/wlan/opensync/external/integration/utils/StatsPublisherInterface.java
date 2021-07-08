
package com.telecominfraproject.wlan.opensync.external.integration.utils;

import com.telecominfraproject.wlan.systemevent.models.SystemEvent;

import sts.OpensyncStats.Report;

public interface StatsPublisherInterface {

    void processMqttMessage(String topic, Report report);

    void publishSystemEventFromTableStateMonitor(SystemEvent event);

}
