package com.telecominfraproject.wlan.opensync.external.integration;

import java.util.Map;

import com.telecominfraproject.wlan.opensync.external.integration.models.ConnectNodeInfo;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;

import sts.PlumeStats.Report;
import traffic.NetworkMetadata.FlowReport;
import wc.stats.IpDnsTelemetry.WCStatsReport;

public interface OpensyncExternalIntegrationInterface {
    void apConnected(String apId, ConnectNodeInfo connectNodeInfo);

    public enum RowUpdateOperation {
        INIT, INSERT, DELETE, MODIFY
    }

    void apDisconnected(String apId);

    OpensyncAPConfig getApConfig(String apId);

    void wifiVIFStateDbTableUpdate(Map<String, String> row, String apId, RowUpdateOperation operation);

    void wifiRadioStatusDbTableUpdate(Map<String, String> row, String apId, RowUpdateOperation operation);

    void wifiInetStateDbTableUpdate(Map<String, String> row, String apId, RowUpdateOperation operation);

    void processMqttMessage(String topic, Report report);

    void processMqttMessage(String topic, FlowReport flowReport);

    void processMqttMessage(String topic, WCStatsReport wcStatsReport);

    void wifiAssociatedClientsDbTableUpdate(Map<String, String> row, String apId, RowUpdateOperation operation);

    void awlan_NodeDbTableUpdate(Map<String, String> row, String connectedClientId, RowUpdateOperation operation);
}
