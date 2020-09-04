package com.telecominfraproject.wlan.opensync.external.integration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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

import sts.OpensyncStats.Report;
import traffic.NetworkMetadata.FlowReport;
import wc.stats.IpDnsTelemetry.WCStatsReport;

@Profile("opensync_static_config")
@Component
public class OpensyncExternalIntegrationSimple implements OpensyncExternalIntegrationInterface {

	private static final Logger LOG = LoggerFactory.getLogger(OpensyncExternalIntegrationSimple.class);

	@Value("${tip.wlan.ovsdb.customerEquipmentFileName:/app/config/EquipmentExample.json}")
	private String customerEquipmentFileName;

	@Value("${tip.wlan.ovsdb.apProfileFileName:/app/config/ProfileAPExample.json}")
	private String apProfileFileName;

	@Value("${tip.wlan.ovsdb.ssidProfileFileName:/app/config/ProfileSsid.json}")
	private String ssidProfileFileName;

	@Value("${tip.wlan.ovsdb.radiusProfileFileName:/app/config/ProfileRadius.json}")
	private String radiusProfileFileName;
	
	@Value("${tip.wlan.ovsdb.captiveProfileFileName:/app/config/ProfileCaptive.json}")
	private String captiveProfileFileName;
	
	@Value("${tip.wlan.ovsdb.bonjourProfileFileName:/app/config/ProfileBonjour.json}")
	private String bonjourProfileFileName;
	
	@Value("${tip.wlan.ovsdb.locationFileName:/app/config/LocationBuildingExample.json}")
	private String locationFileName;

	private String serialNumber = "";

	@PostConstruct
	private void postCreate() {
		LOG.info("Using Static integration");
	}

	public void apConnected(String apId, ConnectNodeInfo connectNodeInfo) {
		serialNumber = connectNodeInfo.serialNumber;
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

			equipment.setSerial(serialNumber);

			com.telecominfraproject.wlan.profile.models.Profile apProfile = com.telecominfraproject.wlan.profile.models.Profile
					.fromFile(apProfileFileName, com.telecominfraproject.wlan.profile.models.Profile.class);

			List<com.telecominfraproject.wlan.profile.models.Profile> ssidProfiles = com.telecominfraproject.wlan.profile.models.Profile
					.listFromFile(ssidProfileFileName, com.telecominfraproject.wlan.profile.models.Profile.class);

			ssidProfiles.stream().forEach(p -> apProfile.getChildProfileIds().add(p.getId()));

			List<com.telecominfraproject.wlan.profile.models.Profile> radiusProfiles = com.telecominfraproject.wlan.profile.models.Profile
					.listFromFile(radiusProfileFileName, com.telecominfraproject.wlan.profile.models.Profile.class);
			
			
			List<com.telecominfraproject.wlan.profile.models.Profile> captiveProfiles = null;
			File captiveFile = new File(captiveProfileFileName);
			if (captiveFile.exists()) {
				captiveProfiles = com.telecominfraproject.wlan.profile.models.Profile
					.listFromFile(captiveProfileFileName, com.telecominfraproject.wlan.profile.models.Profile.class);
			} else {
				LOG.info("Captive file is not provided");
			}
			
	         List<com.telecominfraproject.wlan.profile.models.Profile> bonjourProfiles = null;
	            File bonjourFile = new File(bonjourProfileFileName);
	            if (bonjourFile.exists()) {
	                bonjourProfiles = com.telecominfraproject.wlan.profile.models.Profile
	                    .listFromFile(bonjourProfileFileName, com.telecominfraproject.wlan.profile.models.Profile.class);
	            } else {
	                LOG.info("Bonjour file is not provided");
	            }

			equipment.setProfileId(apProfile.getId());

			Location location = Location.fromFile(locationFileName, Location.class);

			equipment.setLocationId(location.getId());

			ret = new OpensyncAPConfig();
			ret.setCustomerEquipment(equipment);
			ret.setApProfile(apProfile);
			ret.setSsidProfile(ssidProfiles);
			ret.setRadiusProfiles(radiusProfiles);
			ret.setEquipmentLocation(location);
			ret.setCaptiveProfiles(captiveProfiles);
            ret.setBonjourGatewayProfiles(bonjourProfiles);

		} catch (IOException e) {
			LOG.error("Cannot read config file", e);
		}

		LOG.debug("Config content : {}", ret);

		return ret;
	}

	public void processMqttMessage(String topic, Report report) {
		LOG.info("Received OpensyncStatsReport on topic {} for ap {}\n{}", topic, report.getNodeID(), report);

		report.getEventReportList().stream().forEach(e -> {
		    LOG.info("Received EventReport {}", e);
		});
		
		
	}

	public void processMqttMessage(String topic, FlowReport flowReport) {
		LOG.info("Received FlowReport on topic {} for ap {}", topic, flowReport.getObservationPoint().getNodeId());
	}

	public void processMqttMessage(String topic, WCStatsReport wcStatsReport) {
		LOG.info("Received WCStatsReport on topic {} for ap {}", topic,
				wcStatsReport.getObservationPoint().getNodeId());
	}

	@Override
	public void wifiVIFStateDbTableUpdate(List<OpensyncAPVIFState> vifStateTables, String apId) {
		LOG.info("Received table state update {} for ap {}", vifStateTables, apId);
	}

	@Override
	public void wifiRadioStatusDbTableUpdate(List<OpensyncAPRadioState> radioStateTable, String apId) {
		LOG.info("Received table state update {} for ap {}", radioStateTable, apId);

	}

	@Override
	public void wifiInetStateDbTableUpdate(List<OpensyncAPInetState> inetStateTable, String apId) {
		LOG.info("Received table state update {} for ap {}", inetStateTable, apId);

	}

	@Override
	public void wifiAssociatedClientsDbTableUpdate(List<OpensyncWifiAssociatedClients> wifiAssociatedClients,
			String apId) {
		LOG.info("Received table state update {} for ap {}", wifiAssociatedClients, apId);

	}

	@Override
	public void awlanNodeDbTableUpdate(OpensyncAWLANNode opensyncAPState, String apId) {
		LOG.info("Received table state update {} for ap {}", opensyncAPState, apId);

	}

	@Override
	public void wifiVIFStateDbTableDelete(List<OpensyncAPVIFState> vifStateTables, String apId) {
		LOG.info("Received table delete {} for ap {}", vifStateTables, apId);

	}

	@Override
	public void wifiAssociatedClientsDbTableDelete(String deletedClientMac, String apId) {
		LOG.info("Received Wifi_Associated_Clients row delete {} for ap {}", deletedClientMac, apId);

	}

    @Override
    public void wifiInetStateDbTableDelete(List<OpensyncAPInetState> inetStateTables, String apId) {
        LOG.info("Received Wifi_VIF_State row(s) delete {} for ap {}", inetStateTables, apId);
        
    }

    @Override
    public void dhcpLeasedIpDbTableUpdate(List<Map<String, String>> dhcpAttributes, String apId,
            RowUpdateOperation rowUpdateOperation) {
        LOG.info("Received DHCP_Leased_IP row(s) {} rowUpdateOperation {} for ap {}", dhcpAttributes, rowUpdateOperation,apId);
        
    }

    @Override
    public void commandStateDbTableUpdate(List<Map<String, String>> commandStateAttributes, String apId,
            RowUpdateOperation rowUpdateOperation) {
        LOG.info("Received Command_State row(s) {} rowUpdateOperation {} for ap {}", commandStateAttributes, rowUpdateOperation,apId);
        
    }

}
