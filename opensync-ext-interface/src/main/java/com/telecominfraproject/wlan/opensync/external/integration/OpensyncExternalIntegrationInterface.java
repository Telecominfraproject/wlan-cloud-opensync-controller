package com.telecominfraproject.wlan.opensync.external.integration;

import java.util.List;

import com.telecominfraproject.wlan.opensync.external.integration.models.ConnectNodeInfo;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPInetState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPRadioState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPVIFState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAWLANNode;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncWifiAssociatedClients;

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

    void wifiVIFStateDbTableUpdate(List<OpensyncAPVIFState> vifStateTables, String apId);

    void wifiRadioStatusDbTableUpdate(List<OpensyncAPRadioState> radioStateTables, String apId);

    void wifiInetStateDbTableUpdate(List<OpensyncAPInetState> inetStateTables, String apId);

    void processMqttMessage(String topic, Report report);

    void processMqttMessage(String topic, FlowReport flowReport);

    void processMqttMessage(String topic, WCStatsReport wcStatsReport);

    void wifiAssociatedClientsDbTableUpdate(List<OpensyncWifiAssociatedClients> wifiAssociatedClients, String apId);

    void awlanNodeDbTableUpdate(OpensyncAWLANNode opensyncAPState, String apId);
}
