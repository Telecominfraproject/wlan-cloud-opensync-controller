package com.telecominfraproject.wlan.opensync.external.integration;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.opensync.external.integration.models.ConnectNodeInfo;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;

import sts.PlumeStats.Report;
import traffic.NetworkMetadata.FlowReport;
import wc.stats.IpDnsTelemetry.WCStatsReport;

@Profile("opensync_static_config")
@Component
public class OpensyncExternalIntegrationSimple implements OpensyncExternalIntegrationInterface {

    private static final Logger LOG = LoggerFactory.getLogger(OpensyncExternalIntegrationSimple.class);

    @Value("${connectus.ovsdb.configFileName:/Users/dtop/Documents/TIP_WLAN_repos/opensync_wifi_controller/opensync_ext_static/src/main/resources/config_2_ssids.json}")
    private String configFileName;

    @PostConstruct
    private void postCreate() {
        LOG.info("Using Static integration");
    }

    public void apConnected(String apId, ConnectNodeInfo connectNodeInfo) {
        LOG.info("AP {} got connected to the gateway", apId);
        LOG.info("ConnectNodeInfo {}", connectNodeInfo);

    }

    public void apDisconnected(String apId) {
        LOG.info("AP {} got disconnected from the gateway", apId);
    }

 

    public OpensyncAPConfig getApConfig(String apId) {
        LOG.info("Retrieving config for AP {} from file {}", apId, configFileName);
        OpensyncAPConfig ret = null;

        try {
            ret = OpensyncAPConfig.fromFile(configFileName, OpensyncAPConfig.class);
        } catch (IOException e) {
            LOG.error("Cannot read config from {}", configFileName, e);
        }

        LOG.debug("Config content : {}", ret);

        return ret;
    }

    public void processMqttMessage(String topic, Report report) {

        LOG.info("Received PlumeStatsReport on topic {} for ap {}", topic, report.getNodeID());

        if (report.getClientsCount() > 0) {
            LOG.debug("Received {} client reports for AP {}", report.getClientsCount(), report.getNodeID());
            report.getClientsList().forEach(c -> LOG.trace("ClientReport {}", c));
        }
        if (report.getNeighborsCount() > 0) {
            LOG.debug("Received {} neighbor reports for AP {}", report.getNeighborsCount(), report.getNodeID());
            report.getNeighborsList().forEach(c -> LOG.trace("NeighborReport {}", c));
        }
        if (report.getDeviceCount() > 0) {
            LOG.debug("Received {} device reports for AP {}", report.getDeviceCount(), report.getNodeID());
            report.getDeviceList().forEach(c -> LOG.trace("DeviceReport {}", c));
        }
        if (report.getSurveyCount() > 0) {
            LOG.debug("Received {} survey reports for AP {}", report.getSurveyCount(), report.getNodeID());
            report.getSurveyList().forEach(c -> LOG.trace("SurveyReport {}", c));
        }
        if (report.getRssiReportCount() > 0) {
            LOG.debug("Received {} rssi reports for AP {}", report.getRssiReportCount(), report.getNodeID());
            report.getRssiReportList().forEach(c -> LOG.trace("RSSI Report {}", c));
        }
    }

    public void processMqttMessage(String topic, FlowReport flowReport) {
        LOG.info("Received flowReport on topic {} for ap {}", topic, flowReport.getObservationPoint().getNodeId());
    }

    public void processMqttMessage(String topic, WCStatsReport wcStatsReport) {
        LOG.info("Received wcStatsReport on topic {} for ap {}", topic,
                wcStatsReport.getObservationPoint().getNodeId());
    }

    @Override
    public void wirelessStatusChanged(Map<String, com.vmware.ovsdb.protocol.operation.notation.Value> row,
            String apId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void deviceStatusChanged(Map<String, com.vmware.ovsdb.protocol.operation.notation.Value> row, String apId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void networkStatusChanged(Map<String, com.vmware.ovsdb.protocol.operation.notation.Value> row, String apId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void handleClientsChanged(Map<String, com.vmware.ovsdb.protocol.operation.notation.Value> row,
            String connectedClientId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void awlanChanged(Map<String, com.vmware.ovsdb.protocol.operation.notation.Value> row,
            String connectedClientId) {
        // TODO Auto-generated method stub
        
    }

   

}
