package ai.connectus.opensync.external.integration;

import ai.connectus.opensync.external.integration.models.ConnectNodeInfo;
import ai.connectus.opensync.external.integration.models.OpensyncAPConfig;
import sts.PlumeStats.Report;
import traffic.NetworkMetadata.FlowReport;
import wc.stats.IpDnsTelemetry.WCStatsReport;

public interface OpensyncExternalIntegrationInterface {
    void apConnected(String apId, ConnectNodeInfo connectNodeInfo);
    void apDisconnected(String apId);
    OpensyncAPConfig getApConfig(String apId);
    void processMqttMessage(String topic, Report report);
    void processMqttMessage(String topic, FlowReport flowReport);
    void processMqttMessage(String topic, WCStatsReport wcStatsReport);
}
