package com.telecominfraproject.wlan.opensync.external.integration;

import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

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
		LOG.info("Received PlumeStatsReport on topic {} for ap {}\n{}", topic, report.getNodeID(),report);
	}

	public void processMqttMessage(String topic, FlowReport flowReport) {
		LOG.info("Received flowReport on topic {} for ap {}", topic, flowReport.getObservationPoint().getNodeId());
	}

	public void processMqttMessage(String topic, WCStatsReport wcStatsReport) {
		LOG.info("Received wcStatsReport on topic {} for ap {}", topic,
				wcStatsReport.getObservationPoint().getNodeId());
	}

	@Override
	public void wifiVIFStateDbTableUpdate(List<OpensyncAPVIFState> vifStateTables, String apId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void wifiRadioStatusDbTableUpdate(List<OpensyncAPRadioState> radioStateTable, String apId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void wifiInetStateDbTableUpdate(List<OpensyncAPInetState> inetStateTable, String apId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void wifiAssociatedClientsDbTableUpdate(List<OpensyncWifiAssociatedClients> wifiAssociatedClients,
			String apId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void awlanNodeDbTableUpdate(OpensyncAWLANNode opensyncAPState, String apId) {
		// TODO Auto-generated method stub

	}

    @Override
    public void wifiVIFStateDbTableDelete(List<OpensyncAPVIFState> vifStateTables, String apId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void wifiAssociatedClientsDbTableDelete(String deletedClientMac, String apId) {
        // TODO Auto-generated method stub
        
    }

}
