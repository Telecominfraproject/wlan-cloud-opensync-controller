package com.telecominfraproject.wlan.opensync.external.integration;

import java.util.Map;

import com.telecominfraproject.wlan.opensync.external.integration.models.ConnectNodeInfo;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.vmware.ovsdb.protocol.operation.notation.Value;

import sts.PlumeStats.Report;
import traffic.NetworkMetadata.FlowReport;
import wc.stats.IpDnsTelemetry.WCStatsReport;

public interface OpensyncExternalIntegrationInterface {
    void apConnected(String apId, ConnectNodeInfo connectNodeInfo);

    void apDisconnected(String apId);

    OpensyncAPConfig getApConfig(String apId);

    void wifiVIFStateDbTableUpdate(Map <String,Value> row,String apId);

    void wifiRadioStatusDbTableUpdate(Map <String,Value> row,String apId);

    void wifiInetStateDbTableUpdate(Map <String,Value> row,String apId);

    void processMqttMessage(String topic, Report report);

    void processMqttMessage(String topic, FlowReport flowReport);

    void processMqttMessage(String topic, WCStatsReport wcStatsReport);

    void handleClientsChanged(Map <String,Value> row, String connectedClientId);
    
    void awlanChanged(Map <String,Value> row, String connectedClientId);
}
