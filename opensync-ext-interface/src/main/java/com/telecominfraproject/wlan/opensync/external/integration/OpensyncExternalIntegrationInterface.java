package com.telecominfraproject.wlan.opensync.external.integration;

import com.telecominfraproject.wlan.opensync.external.integration.models.ConnectNodeInfo;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.vmware.ovsdb.protocol.methods.TableUpdates;

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

    void wifiVIFStateDbTableUpdate(TableUpdates tableUpdates, String apId);

    void wifiRadioStatusDbTableUpdate(TableUpdates tableUpdates, String apId);

    void wifiInetStateDbTableUpdate(TableUpdates tableUpdates, String apId);

    void processMqttMessage(String topic, Report report);

    void processMqttMessage(String topic, FlowReport flowReport);

    void processMqttMessage(String topic, WCStatsReport wcStatsReport);

    void wifiAssociatedClientsDbTableUpdate(TableUpdates tableUpdates, String apId);

    void awlanNodeDbTableUpdate(TableUpdates tableUpdates, String connectedClientId);
}
