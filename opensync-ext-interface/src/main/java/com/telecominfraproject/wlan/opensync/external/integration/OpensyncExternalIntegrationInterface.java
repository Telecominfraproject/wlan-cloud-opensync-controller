package com.telecominfraproject.wlan.opensync.external.integration;

import java.util.List;
import java.util.Map;

import com.telecominfraproject.wlan.opensync.external.integration.models.ConnectNodeInfo;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPInetState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPRadioState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPVIFState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAWLANNode;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncWifiAssociatedClients;

import sts.OpensyncStats.Report;
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

    void wifiVIFStateDbTableDelete(List<OpensyncAPVIFState> vifStateTables, String apId);

    void wifiRadioStatusDbTableUpdate(List<OpensyncAPRadioState> radioStateTables, String apId);

    void wifiInetStateDbTableUpdate(List<OpensyncAPInetState> inetStateTables, String apId);

    void wifiInetStateDbTableDelete(List<OpensyncAPInetState> inetStateTables, String apId);

    void wifiAssociatedClientsDbTableUpdate(List<OpensyncWifiAssociatedClients> wifiAssociatedClients, String apId);

    void wifiAssociatedClientsDbTableDelete(String deletedClientMac, String apId);

    void awlanNodeDbTableUpdate(OpensyncAWLANNode opensyncAPState, String apId);

    void dhcpLeasedIpDbTableUpdate(List<Map<String, String>> dhcpAttributes, String apId,
            RowUpdateOperation rowUpdateOperation);

    void commandStateDbTableUpdate(List<Map<String, String>> commandStateAttributes, String apId,
            RowUpdateOperation rowUpdateOperation);

    void apcStateDbTableUpdate(Map<String, String> apcStateAttributes, String apId,
            RowUpdateOperation rowUpdateOperation);
    
    void nodeStateDbTableUpdate(List<Map<String, String>> nodeStateAttributes, String apId);

    void clearEquipmentStatus(String apId);
    
    void processMqttMessage(String topic, Report report);
}
