package com.telecominfraproject.wlan.opensync.external.integration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.equipment.models.Equipment;
import com.telecominfraproject.wlan.location.models.Location;
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

	@Value("${connectus.ovsdb.customerEquipmentFileName:/Users/mikehansen/git/wlan-cloud-workspace/wlan-cloud-opensync-controller/opensync-ext-static/src/main/resources/EquipmentExample.json}")
	private String customerEquipmentFileName;

	@Value("${connectus.ovsdb.apProfileFileName:/Users/mikehansen/git/wlan-cloud-workspace/wlan-cloud-opensync-controller/opensync-ext-static/src/main/resources/ProfileAPExample.json}")
	private String apProfileFileName;

	@Value("${connectus.ovsdb.ssidProfileFileName:/Users/mikehansen/git/wlan-cloud-workspace/wlan-cloud-opensync-controller/opensync-ext-static/src/main/resources/ProfileSsid.json}")
	private String ssidProfileFileName;

	@Value("${connectus.ovsdb.locationFileName:/Users/mikehansen/git/wlan-cloud-workspace/wlan-cloud-opensync-controller/opensync-ext-static/src/main/resources/LocationBuildingExample.json}")
	private String locationFileName;

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
		LOG.info("Retrieving config for AP {}", apId);
		OpensyncAPConfig ret = null;

		try {

			Equipment equipment = Equipment.fromFile(customerEquipmentFileName, Equipment.class);
			equipment.setInventoryId(apId);
			equipment.setName(apId);

			com.telecominfraproject.wlan.profile.models.Profile apProfile = com.telecominfraproject.wlan.profile.models.Profile
					.fromFile(apProfileFileName, com.telecominfraproject.wlan.profile.models.Profile.class);
			com.telecominfraproject.wlan.profile.models.Profile ssidProfile = com.telecominfraproject.wlan.profile.models.Profile
					.fromFile(ssidProfileFileName, com.telecominfraproject.wlan.profile.models.Profile.class);

			Location location = Location.fromFile(locationFileName, Location.class);

			ret = new OpensyncAPConfig();
			ret.setCustomerEquipment(equipment);
			ret.setApProfile(apProfile);
			List<com.telecominfraproject.wlan.profile.models.Profile> ssidProfiles = new ArrayList<com.telecominfraproject.wlan.profile.models.Profile>();
			ssidProfiles.add(ssidProfile);
			ret.setSsidProfile(ssidProfiles);
			ret.setEquipmentLocation(location);

		} catch (IOException e) {
			LOG.error("Cannot read config file", e);
		}

		LOG.debug("Config content : {}", ret);

		return ret;
	}

	public void processMqttMessage(String topic, Report report) {
		LOG.info("Received PlumeStatsReport on topic {} for ap {}\n{}", topic, report.getNodeID(), report);
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
