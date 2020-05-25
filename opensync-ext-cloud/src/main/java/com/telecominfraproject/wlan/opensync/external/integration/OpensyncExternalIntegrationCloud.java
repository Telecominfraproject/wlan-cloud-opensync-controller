package com.telecominfraproject.wlan.opensync.external.integration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.client.ClientServiceInterface;
import com.telecominfraproject.wlan.client.info.models.ClientInfoDetails;
import com.telecominfraproject.wlan.client.session.models.ClientDhcpDetails;
import com.telecominfraproject.wlan.client.session.models.ClientSession;
import com.telecominfraproject.wlan.client.session.models.ClientSessionDetails;
import com.telecominfraproject.wlan.client.session.models.ClientSessionMetricDetails;
import com.telecominfraproject.wlan.cloudeventdispatcher.CloudEventDispatcherInterface;
import com.telecominfraproject.wlan.core.model.entity.CountryCode;
import com.telecominfraproject.wlan.core.model.equipment.EquipmentType;
import com.telecominfraproject.wlan.core.model.equipment.MacAddress;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.core.model.equipment.SecurityType;
import com.telecominfraproject.wlan.customer.service.CustomerServiceInterface;
import com.telecominfraproject.wlan.equipment.EquipmentServiceInterface;
import com.telecominfraproject.wlan.equipment.models.ApElementConfiguration;
import com.telecominfraproject.wlan.equipment.models.ElementRadioConfiguration;
import com.telecominfraproject.wlan.equipment.models.Equipment;
import com.telecominfraproject.wlan.location.models.Location;
import com.telecominfraproject.wlan.location.service.LocationServiceInterface;
import com.telecominfraproject.wlan.opensync.external.integration.controller.OpensyncCloudGatewayController;
import com.telecominfraproject.wlan.opensync.external.integration.models.ConnectNodeInfo;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPInetState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPRadioState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPVIFState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAWLANNode;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncWifiAssociatedClients;
import com.telecominfraproject.wlan.profile.ProfileServiceInterface;
import com.telecominfraproject.wlan.profile.models.Profile;
import com.telecominfraproject.wlan.profile.models.ProfileType;
import com.telecominfraproject.wlan.profile.network.models.ApNetworkConfiguration;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration.SecureMode;
import com.telecominfraproject.wlan.routing.RoutingServiceInterface;
import com.telecominfraproject.wlan.routing.models.EquipmentRoutingRecord;
import com.telecominfraproject.wlan.servicemetrics.models.ApClientMetrics;
import com.telecominfraproject.wlan.servicemetrics.models.ApNodeMetrics;
import com.telecominfraproject.wlan.servicemetrics.models.ApPerformance;
import com.telecominfraproject.wlan.servicemetrics.models.ApSsidMetrics;
import com.telecominfraproject.wlan.servicemetrics.models.ClientMetrics;
import com.telecominfraproject.wlan.servicemetrics.models.EthernetLinkState;
import com.telecominfraproject.wlan.servicemetrics.models.RadioUtilization;
import com.telecominfraproject.wlan.servicemetrics.models.SingleMetricRecord;
import com.telecominfraproject.wlan.servicemetrics.models.SsidStatistics;
import com.telecominfraproject.wlan.status.StatusServiceInterface;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentAdminStatusData;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentLANStatusData;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentProtocolState;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentProtocolStatusData;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentUpgradeState;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentUpgradeStatusData;
import com.telecominfraproject.wlan.status.equipment.models.VLANStatusData;
import com.telecominfraproject.wlan.status.equipment.report.models.ActiveBSSID;
import com.telecominfraproject.wlan.status.equipment.report.models.ActiveBSSIDs;
import com.telecominfraproject.wlan.status.models.Status;
import com.telecominfraproject.wlan.status.models.StatusCode;
import com.telecominfraproject.wlan.status.models.StatusDataType;
import com.telecominfraproject.wlan.status.network.models.NetworkAdminStatusData;
import com.telecominfraproject.wlan.status.network.models.NetworkAggregateStatusData;
import com.telecominfraproject.wlan.status.network.models.UserDetails;

import sts.PlumeStats.Client;
import sts.PlumeStats.ClientReport;
import sts.PlumeStats.Device;
import sts.PlumeStats.Device.RadioTemp;
import sts.PlumeStats.RadioBandType;
import sts.PlumeStats.Report;
import sts.PlumeStats.Survey;
import sts.PlumeStats.Survey.SurveySample;
import traffic.NetworkMetadata.FlowReport;
import wc.stats.IpDnsTelemetry.WCStatsReport;

@org.springframework.context.annotation.Profile("opensync_cloud_config")
@Component
public class OpensyncExternalIntegrationCloud implements OpensyncExternalIntegrationInterface {

	private static final Logger LOG = LoggerFactory.getLogger(OpensyncExternalIntegrationCloud.class);

	@Autowired
	private CustomerServiceInterface customerServiceInterface;
	@Autowired
	private LocationServiceInterface locationServiceInterface;
	@Autowired
	private OvsdbSessionMapInterface ovsdbSessionMapInterface;
	@Autowired
	private CloudEventDispatcherInterface equipmentMetricsCollectorInterface;
	@Autowired
	private EquipmentServiceInterface equipmentServiceInterface;
	@Autowired
	private RoutingServiceInterface routingServiceInterface;
	@Autowired
	private ProfileServiceInterface profileServiceInterface;
	@Autowired
	private StatusServiceInterface statusServiceInterface;

	@Autowired
	private ClientServiceInterface clientServiceInterface;

	@Autowired
	private OpensyncCloudGatewayController gatewayController;

	@Value("${connectus.ovsdb.autoProvisionedCustomerId:2}")
	private int autoProvisionedCustomerId;
	@Value("${connectus.ovsdb.autoProvisionedLocationId:8}")
	private int autoProvisionedLocationId;
	@Value("${connectus.ovsdb.autoProvisionedProfileId:1}")
	private int autoProvisionedProfileId;
	@Value("${connectus.ovsdb.autoProvisionedSsid:autoProvisionedSsid}")
	private String autoProvisionedSsid;

	@Autowired
	private CacheManager cacheManagerShortLived;
	private Cache cloudEquipmentRecordCache;
	private Map<String, OpensyncNode> opensyncNodeMap;

	@Value("${connectus.ovsdb.configFileName:/Users/mikehansen/git/wlan-cloud-workspace/wlan-cloud-opensync-controller/opensync-ext-cloud/src/main/resources/config_2_ssids.json}")
	private String configFileName;

	@PostConstruct
	private void postCreate() {
		LOG.info("Using Cloud integration");
		cloudEquipmentRecordCache = cacheManagerShortLived.getCache("equipment_record_cache");
		opensyncNodeMap = Collections.synchronizedMap(new HashMap<String, OpensyncNode>());
	}

	public Equipment getCustomerEquipment(String apId) {
		Equipment ce = null;

		try {
			ce = cloudEquipmentRecordCache.get(apId, new Callable<Equipment>() {
				@Override
				public Equipment call() throws Exception {
					return equipmentServiceInterface.getByInventoryIdOrNull(apId);
				}
			});
		} catch (Exception e) {
			// do nothing
		}

		return ce;
	}

	public void apConnected(String apId, ConnectNodeInfo connectNodeInfo) {
		LOG.info("AP {} got connected to the gateway", apId);
		LOG.debug("ConnectNodeInfo {}", connectNodeInfo);

		Equipment ce = null;
		try {
			ce = getCustomerEquipment(apId);
		} catch (Exception e) {
			LOG.error("Caught exception getting equipment for Id {}", apId, e);
		}

		try {

			if (ce == null) {

				ce = new Equipment();
				ce.setCustomerId(autoProvisionedCustomerId);
				ce.setInventoryId(apId);
				ce.setEquipmentType(EquipmentType.AP);
				ce.setName(apId);

				ce.setSerial(connectNodeInfo.serialNumber);
				ApElementConfiguration apElementConfig = ApElementConfiguration.createWithDefaults();
				apElementConfig.setDeviceName(apId);
				ce.setDetails(apElementConfig);
				ce.setLocationId(autoProvisionedLocationId);
				ce.setProfileId(autoProvisionedProfileId);

				ce = equipmentServiceInterface.create(ce);
			}

			Profile apProfile = profileServiceInterface.getOrNull(ce.getProfileId());

			if (apProfile == null || !apProfile.getProfileType().equals(ProfileType.equipment_ap)) {
				apProfile = new Profile();
				apProfile.setCustomerId(ce.getCustomerId());
				apProfile.setName("autoprovisionedApProfile");
				apProfile.setDetails(ApNetworkConfiguration.createWithDefaults());
				apProfile = profileServiceInterface.create(apProfile);

				Profile profileSsid2do4GHz = new Profile();
				profileSsid2do4GHz.setCustomerId(ce.getCustomerId());
				profileSsid2do4GHz.setName("autoProvisionedSsid");
				SsidConfiguration ssidConfig = SsidConfiguration.createWithDefaults();
				Set<RadioType> appliedRadios = new HashSet<RadioType>();
				appliedRadios.add(RadioType.is2dot4GHz);
				ssidConfig.setAppliedRadios(appliedRadios);
				profileSsid2do4GHz.setDetails(ssidConfig);
				profileSsid2do4GHz = profileServiceInterface.create(profileSsid2do4GHz);

				Profile profileSsid5GHzL = new Profile();
				profileSsid5GHzL.setCustomerId(ce.getCustomerId());
				profileSsid5GHzL.setName("autoProvisionedSsid-5l");
				ssidConfig = SsidConfiguration.createWithDefaults();
				appliedRadios = new HashSet<RadioType>();
				appliedRadios.add(RadioType.is5GHzL);
				ssidConfig.setAppliedRadios(appliedRadios);
				profileSsid5GHzL.setDetails(ssidConfig);
				profileSsid5GHzL = profileServiceInterface.create(profileSsid5GHzL);

				Profile profileSsid5GHzU = new Profile();
				profileSsid5GHzU.setCustomerId(ce.getCustomerId());
				profileSsid5GHzU.setName("autoProvisionedSsid-5u");
				ssidConfig = SsidConfiguration.createWithDefaults();
				appliedRadios = new HashSet<RadioType>();
				appliedRadios.add(RadioType.is5GHzU);
				ssidConfig.setAppliedRadios(appliedRadios);
				profileSsid5GHzU.setDetails(ssidConfig);
				profileSsid5GHzU = profileServiceInterface.create(profileSsid5GHzU);

				Set<Long> childProfileIds = new HashSet<Long>();
				childProfileIds.add(profileSsid2do4GHz.getId());
				childProfileIds.add(profileSsid5GHzL.getId());
				childProfileIds.add(profileSsid5GHzU.getId());
				apProfile.setChildProfileIds(childProfileIds);

				apProfile = profileServiceInterface.update(apProfile);

				// update AP only if the apProfile was missing
				ce.setProfileId(apProfile.getId());
				ce = equipmentServiceInterface.update(ce);
			}

			updateApStatus(ce, connectNodeInfo);

			// register equipment routing record
			EquipmentRoutingRecord equipmentRoutingRecord = new EquipmentRoutingRecord();
			equipmentRoutingRecord.setGatewayId(gatewayController.getRegisteredGwId());
			equipmentRoutingRecord.setCustomerId(ce.getCustomerId());
			equipmentRoutingRecord.setEquipmentId(ce.getId());
			equipmentRoutingRecord = routingServiceInterface.create(equipmentRoutingRecord);

			gatewayController.registerCustomerEquipment(ce.getName(), ce.getCustomerId(), ce.getId());

			OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);
			ovsdbSession.setRoutingId(equipmentRoutingRecord.getId());
			ovsdbSession.setEquipmentId(ce.getId());
			ovsdbSession.setCustomerId(ce.getCustomerId());

			LOG.debug("Equipment {}", Equipment.toPrettyJsonString(ce));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void updateApStatus(Equipment ce, ConnectNodeInfo connectNodeInfo) {

		try {

			Status statusRecord = statusServiceInterface.getOrNull(ce.getCustomerId(), ce.getId(),
					StatusDataType.EQUIPMENT_ADMIN);
			if (statusRecord == null) {
				statusRecord = new Status();
				statusRecord.setCustomerId(ce.getCustomerId());
				statusRecord.setEquipmentId(ce.getId());

				EquipmentAdminStatusData statusData = new EquipmentAdminStatusData();
				statusRecord.setDetails(statusData);
			}

			((EquipmentAdminStatusData) statusRecord.getDetails()).setStatusCode(StatusCode.normal);
			// Update the equipment admin status
			statusRecord = statusServiceInterface.update(statusRecord);

			// update LAN status - nothing to do here for now
			statusRecord = statusServiceInterface.getOrNull(ce.getCustomerId(), ce.getId(), StatusDataType.LANINFO);
			if (statusRecord == null) {
				statusRecord = new Status();
				statusRecord.setCustomerId(ce.getCustomerId());
				statusRecord.setEquipmentId(ce.getId());

				EquipmentLANStatusData statusData = new EquipmentLANStatusData();
				statusRecord.setDetails(statusData);
			}

			Map<Integer, VLANStatusData> vlanStatusDataMap = new HashMap<>();
			((EquipmentLANStatusData) statusRecord.getDetails()).setVlanStatusDataMap(vlanStatusDataMap);

			statusServiceInterface.update(statusRecord);

			// update protocol status
			statusRecord = statusServiceInterface.getOrNull(ce.getCustomerId(), ce.getId(), StatusDataType.PROTOCOL);
			if (statusRecord == null) {
				statusRecord = new Status();
				statusRecord.setCustomerId(ce.getCustomerId());
				statusRecord.setEquipmentId(ce.getId());

				EquipmentProtocolStatusData statusData = new EquipmentProtocolStatusData();
				statusRecord.setDetails(statusData);
			}

			EquipmentProtocolStatusData protocolStatusData = ((EquipmentProtocolStatusData) statusRecord.getDetails());
			protocolStatusData.setPoweredOn(true);
			protocolStatusData.setCloudProtocolVersion("1100");
			protocolStatusData.setProtocolState(EquipmentProtocolState.ready);
			protocolStatusData.setBandPlan("FCC");
			protocolStatusData.setBaseMacAddress(MacAddress.valueOf(connectNodeInfo.macAddress));
			protocolStatusData.setCloudCfgDataVersion(42L);
			protocolStatusData.setReportedCfgDataVersion(42L);
			protocolStatusData.setCountryCode("CA");
			protocolStatusData.setReportedCC(CountryCode.ca);
			protocolStatusData.setReportedHwVersion(connectNodeInfo.platformVersion);
			protocolStatusData.setReportedSwVersion(connectNodeInfo.firmwareVersion);
			protocolStatusData.setReportedSwAltVersion(connectNodeInfo.firmwareVersion);
			try {
				protocolStatusData.setReportedIpV4Addr(InetAddress.getByName(connectNodeInfo.ipV4Address));
			} catch (UnknownHostException e) {
				// do nothing here
			}
			if (connectNodeInfo.macAddress != null && MacAddress.valueOf(connectNodeInfo.macAddress) != null) {
				protocolStatusData.setReportedMacAddr(MacAddress.valueOf(connectNodeInfo.macAddress));
			}
			protocolStatusData.setReportedSku(connectNodeInfo.skuNumber);
			protocolStatusData.setSerialNumber(connectNodeInfo.serialNumber);
			protocolStatusData.setSystemName(connectNodeInfo.model);

			statusServiceInterface.update(statusRecord);

			statusRecord = statusServiceInterface.getOrNull(ce.getCustomerId(), ce.getId(), StatusDataType.FIRMWARE);
			if (statusRecord == null) {
				statusRecord = new Status();
				statusRecord.setCustomerId(ce.getCustomerId());
				statusRecord.setEquipmentId(ce.getId());
				EquipmentUpgradeStatusData statusData = new EquipmentUpgradeStatusData();
				statusRecord.setDetails(statusData);
			}
			EquipmentUpgradeStatusData fwUpgradeStatusData = ((EquipmentUpgradeStatusData) statusRecord.getDetails());
			fwUpgradeStatusData.setActiveSwVersion(connectNodeInfo.firmwareVersion);
			fwUpgradeStatusData.setAlternateSwVersion(connectNodeInfo.firmwareVersion);
			fwUpgradeStatusData.setTargetSwVersion(connectNodeInfo.firmwareVersion);
			fwUpgradeStatusData.setUpgradeState(EquipmentUpgradeState.up_to_date);

			statusServiceInterface.update(statusRecord);

			// TODO:
			// equipmentStatusInterface.updateNetworkAdminStatus(networkAdminStatusRecord);
			// dtop: this one populates traffic capacity and usage dial on the
			// main dashboard
			// from APDemoMetric properties getPeriodLengthSec, getRxBytes2G,
			// getTxBytes2G, getRxBytes5G, getTxBytes5G
			Status networkAdminStatusRec = statusServiceInterface.getOrNull(ce.getCustomerId(), 0,
					StatusDataType.NETWORK_ADMIN);
			if (networkAdminStatusRec == null) {
				networkAdminStatusRec = new Status();
				networkAdminStatusRec.setCustomerId(ce.getCustomerId());
				networkAdminStatusRec.setEquipmentId(ce.getId());
				NetworkAdminStatusData statusData = new NetworkAdminStatusData();
				networkAdminStatusRec.setDetails(statusData);
			}

			NetworkAdminStatusData netAdminStatusData = (NetworkAdminStatusData) networkAdminStatusRec.getDetails();
			netAdminStatusData.setDhcpStatus(StatusCode.normal);
			netAdminStatusData.setCloudLinkStatus(StatusCode.normal);
			netAdminStatusData.setDnsStatus(StatusCode.normal);

			networkAdminStatusRec.setDetails(netAdminStatusData);

			statusServiceInterface.update(networkAdminStatusRec);

			Status networkAggStatusRec = statusServiceInterface.getOrNull(ce.getCustomerId(), 0,
					StatusDataType.NETWORK_AGGREGATE);
			if (networkAggStatusRec == null) {
				networkAggStatusRec = new Status();
				networkAggStatusRec.setCustomerId(ce.getCustomerId());
				NetworkAggregateStatusData naStatusData = new NetworkAggregateStatusData();
				networkAggStatusRec.setDetails(naStatusData);
			}

			UserDetails userDetails = ((NetworkAggregateStatusData) networkAggStatusRec.getDetails()).getUserDetails();
			if (userDetails != null)
				LOG.debug("UserDetails {}", userDetails.toPrettyString());

			statusServiceInterface.update(networkAggStatusRec);

		} catch (Exception e) {
			// do nothing
			LOG.debug("Exception in updateApStatus", e);
		}

	}

	public void apDisconnected(String apId) {
		LOG.info("AP {} got disconnected from the gateway", apId);
		try {
			// removed the 'in-memory' cached node
			synchronized (opensyncNodeMap) {
				opensyncNodeMap.remove(apId);
				LOG.info("AP {} and table state data removed from memory cache", apId);
			}

			OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

			if (ovsdbSession != null) {
				routingServiceInterface.delete(ovsdbSession.getRoutingId());
			} else {
				LOG.warn("Cannot find ap {} in inventory", apId);
			}
		} catch (Exception e) {
			LOG.error("Exception when registering ap routing {}", apId, e);
		}

	}

	public OpensyncAPConfig getApConfig(String apId) {
		LOG.info("Retrieving config for AP {} ", apId);
		OpensyncAPConfig ret = null;

		try {

			OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);
			if (ovsdbSession == null) {
				throw new IllegalStateException("AP is not connected " + apId);
			}

			Equipment equipmentConfig = getCustomerEquipment(apId);

			if (equipmentConfig == null) {
				throw new IllegalStateException("Cannot retrieve configuration for " + apId);
			}

			ret = new OpensyncAPConfig();

			ret.setCustomerEquipment(equipmentConfig);

			Location eqLocation = locationServiceInterface.get(equipmentConfig.getLocationId());

			ret.setEquipmentLocation(eqLocation);

			// extract country, radio channels from resolvedEqCfg
			String country = "CA";
			CountryCode countryCode = Location.getCountryCode(eqLocation);
			if (countryCode != null && countryCode != CountryCode.UNSUPPORTED) {
				country = countryCode.toString().toUpperCase();
			}

			int radioChannel24G = 1;
			int radioChannel5LG = 44;
			int radioChannel5HG = 108;

			ApElementConfiguration apElementConfiguration = (ApElementConfiguration) equipmentConfig.getDetails();

			Map<RadioType, ElementRadioConfiguration> erc = apElementConfiguration.getRadioMap();
			if (erc != null) {

				ElementRadioConfiguration erc24 = erc.get(RadioType.is2dot4GHz);
				ElementRadioConfiguration erc5gl = erc.get(RadioType.is5GHzL);
				ElementRadioConfiguration erc5gh = erc.get(RadioType.is5GHzU);

				if (erc24 != null) {
					radioChannel24G = erc24.getChannelNumber();
				}

				if (erc5gl != null) {
					radioChannel5LG = erc5gl.getChannelNumber();
				}

				if (erc5gh != null) {
					radioChannel5HG = erc5gh.getChannelNumber();
				}

			}

			com.telecominfraproject.wlan.profile.models.Profile apProfile = profileServiceInterface
					.getOrNull(equipmentConfig.getProfileId());

			ret.setApProfile(apProfile);

			if (apProfile != null) {
				List<com.telecominfraproject.wlan.profile.models.Profile> ssidProfiles = new ArrayList<com.telecominfraproject.wlan.profile.models.Profile>();

				Set<Long> childProfileIds = apProfile.getChildProfileIds();
				for (Long id : childProfileIds) {
					com.telecominfraproject.wlan.profile.models.Profile profile = profileServiceInterface.get(id);
					if (profile.getProfileType().equals(ProfileType.ssid)) {

						SsidConfiguration ssidCfg = (SsidConfiguration) profile.getDetails();
						ssidCfg.setSsid(profile.getName());

						for (RadioType radioType : ssidCfg.getAppliedRadios()) {
							if (ssidCfg.getSecureMode() == SecureMode.wpa2OnlyPSK
									|| ssidCfg.getSecureMode() == SecureMode.wpa2PSK) {

								ssidCfg.setSecureMode(SecureMode.wpa2PSK);
							} else if (ssidCfg.getSecureMode() == SecureMode.wpaPSK) {

								ssidCfg.setSecureMode(SecureMode.wpaPSK);

							} else {
								LOG.warn("Unsupported encryption mode {} - will use WPA-PSK instead",
										ssidCfg.getSecureMode());

								ssidCfg.setSecureMode(SecureMode.wpa2PSK);
							}

							if (ssidCfg.getKeyStr() == null) {
								ssidCfg.setKeyStr("12345678");
							}

						}
						profile.setDetails(ssidCfg);
						ssidProfiles.add(profile);

					}
				}
				ret.setSsidProfile(ssidProfiles);

				ret.getSsidProfile().stream().forEach(p -> LOG.debug("SSID Profile {}", p.toPrettyString()));

			}

		} catch (Exception e) {
			LOG.error("Cannot read config for AP {}", apId, e);
		}

		LOG.debug("Config content : {}", ret);

		return ret;
	}

	/**
	 * @param topic
	 * @return apId extracted from the topic name, or null if it cannot be extracted
	 */
	public static String extractApIdFromTopic(String topic) {
		// Topic is formatted as
		// "/ap/"+clientCn+"_"+ret.serialNumber+"/opensync"
		if (topic == null) {
			return null;
		}

		String[] parts = topic.split("/");
		if (parts.length < 3) {
			return null;
		}

		// apId is the third element in the topic
		return parts[2];
	}

	/**
	 * @param topic
	 * @return customerId looked up from the topic name, or -1 if it cannot be
	 *         extracted
	 */
	public int extractCustomerIdFromTopic(String topic) {

		String apId = extractApIdFromTopic(topic);
		if (apId == null) {
			return -1;
		}

		OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

		if (ovsdbSession != null) {
			return ovsdbSession.getCustomerId();
		}

		return -1;

	}

	public long extractEquipmentIdFromTopic(String topic) {

		String apId = extractApIdFromTopic(topic);
		if (apId == null) {
			return -1;
		}

		OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

		if (ovsdbSession != null) {
			return ovsdbSession.getEquipmentId();
		}

		return -1;

	}

	public void processMqttMessage(String topic, Report report) {
		LOG.info("Received report on topic {} for ap {}", topic, report.getNodeID());
		int customerId = extractCustomerIdFromTopic(topic);

		long equipmentId = extractEquipmentIdFromTopic(topic);
		if (equipmentId <= 0 || customerId <= 0) {
			LOG.warn("Cannot determine equipment ids from topic {} - customerId {} equipmentId {}", topic, customerId,
					equipmentId);
			return;
		}

		List<SingleMetricRecord> metricRecordList = new ArrayList<>();

		populateApClientMetrics(metricRecordList, report, customerId, equipmentId);
		populateApNodeMetrics(metricRecordList, report, customerId, equipmentId);
		// TODO: populateApNodeMetrics(metricRecordList, report, customerId,
		// equipmentId, extractApIdFromTopic(topic));

		try {
			populateApSsidMetrics(metricRecordList, report, customerId, equipmentId, extractApIdFromTopic(topic));
		} catch (Exception e) {
			LOG.error("Exception when processing populateApSsidMetrics", e);
		}

		if (!metricRecordList.isEmpty()) {
			equipmentMetricsCollectorInterface.publishMetrics(metricRecordList);
		}

	}

	private void populateApNodeMetrics(List<SingleMetricRecord> metricRecordList, Report report, int customerId,
			long equipmentId) {
		{
			LOG.debug("populateApNodeMetrics for Customer {} Equipment {}", customerId, equipmentId);
			ApNodeMetrics apNodeMetrics = null;

			for (Device deviceReport : report.getDeviceList()) {

				SingleMetricRecord smr = new SingleMetricRecord(customerId, equipmentId);
				metricRecordList.add(smr);

				apNodeMetrics = new ApNodeMetrics();
				smr.setData(apNodeMetrics);
				ApPerformance apPerformance = new ApPerformance();
				apNodeMetrics.setApPerformance(apPerformance);

				smr.setCreatedTimestamp(deviceReport.getTimestampMs());
				// data.setChannelUtilization2G(channelUtilization2G);
				// data.setChannelUtilization5G(channelUtilization5G);

				if (deviceReport.getRadioTempCount() > 0) {
					int cpuTemperature = 0;
					int numSamples = 0;
					for (RadioTemp r : deviceReport.getRadioTempList()) {
						if (r.hasValue()) {
							cpuTemperature += r.getValue();
							numSamples++;
						}
					}

					if (numSamples > 0) {
						apPerformance.setCpuTemperature(cpuTemperature / numSamples);
					}
				}

				if (deviceReport.hasCpuUtil() && deviceReport.getCpuUtil().hasCpuUtil()) {
					apPerformance
							.setCpuUtilized(new byte[] { (byte) (deviceReport.getCpuUtil().getCpuUtil()), (byte) (0) });
				}

				apPerformance.setEthLinkState(EthernetLinkState.UP1000_FULL_DUPLEX);

				if (deviceReport.hasMemUtil() && deviceReport.getMemUtil().hasMemTotal()
						&& deviceReport.getMemUtil().hasMemUsed()) {
					apPerformance.setFreeMemory(
							deviceReport.getMemUtil().getMemTotal() - deviceReport.getMemUtil().getMemUsed());
				}
				apPerformance.setUpTime((long) deviceReport.getUptime());
			}
			if (apNodeMetrics != null) {

				// Main Network dashboard shows Traffic and Capacity values that
				// are
				// calculated
				// from
				// ApNodeMetric properties getPeriodLengthSec, getRxBytes2G,
				// getTxBytes2G,
				// getRxBytes5G, getTxBytes5G

				// go over all the clients to aggregate per-client tx/rx stats -
				// we
				// want to do
				// this
				// only once per batch of ApNodeMetrics - so we do not repeat
				// values
				// over and
				// over again
				long rxBytes2g = 0;
				long txBytes2g = 0;

				long rxBytes5g = 0;
				long txBytes5g = 0;

				for (ClientReport clReport : report.getClientsList()) {
					for (Client cl : clReport.getClientListList()) {
						if (clReport.getBand() == RadioBandType.BAND2G) {
							if (cl.getStats().hasTxBytes()) {
								txBytes2g += cl.getStats().getTxBytes();
							}
							if (cl.getStats().hasRxBytes()) {
								rxBytes2g += cl.getStats().getRxBytes();
							}
						} else {
							if (cl.getStats().hasTxBytes()) {
								txBytes5g += cl.getStats().getTxBytes();
							}
							if (cl.getStats().hasRxBytes()) {
								rxBytes5g += cl.getStats().getRxBytes();
							}
						}
					}
				}

				apNodeMetrics.setRxBytes2G(rxBytes2g);
				apNodeMetrics.setTxBytes2G(txBytes2g);
				apNodeMetrics.setRxBytes5G(rxBytes5g);
				apNodeMetrics.setTxBytes5G(txBytes5g);
				apNodeMetrics.setPeriodLengthSec(60);

				// Now try to populate metrics for calculation of radio capacity
				// see
				// com.telecominfraproject.wlan.metrics.streaming.spark.equipmentreport.CapacityDStreamsConfig.toAggregatedStats(int,
				// long, ApNodeMetric data)
				// result.stats2g =
				// toAggregatedRadioStats(data.getNoiseFloor2G(),data.getRadioUtilization2G());
				// result.stats5g =
				// toAggregatedRadioStats(data.getNoiseFloor5G(),data.getRadioUtilization5G());
				// RadioUtilization
				// private Integer assocClientTx;
				// private Integer unassocClientTx;
				// private Integer assocClientRx;
				// private Integer unassocClientRx;
				// private Integer nonWifi;
				// private Integer timestampSeconds;

				// TODO: temporary solution as this was causing Noise Floor to
				// disappear from Dashboard and Access Point rows
				apNodeMetrics.setNoiseFloor2G(Integer.valueOf(-98));
				apNodeMetrics.setNoiseFloor5G(Integer.valueOf(-98));

				apNodeMetrics.setRadioUtilization2G(new ArrayList<>());
				apNodeMetrics.setRadioUtilization5G(new ArrayList<>());

				// populate it from report.survey
				for (Survey survey : report.getSurveyList()) {
					// int oBSS = 0;
					// int iBSS = 0;
					// int totalBusy = 0;
					// int durationMs = 0;
					for (SurveySample surveySample : survey.getSurveyListList()) {
						if (surveySample.getDurationMs() == 0) {
							continue;
						}

						// iBSS += surveySample.getBusySelf() +
						// surveySample.getBusyTx();
						// oBSS += surveySample.getBusyRx();
						// totalBusy += surveySample.getBusy();
						// durationMs += surveySample.getDurationMs();

						RadioUtilization radioUtil = new RadioUtilization();
						radioUtil.setTimestampSeconds(
								(int) ((survey.getTimestampMs() + surveySample.getOffsetMs()) / 1000));
						radioUtil.setAssocClientTx(100 * surveySample.getBusyTx() / surveySample.getDurationMs());
						radioUtil.setAssocClientRx(100 * surveySample.getBusyRx() / surveySample.getDurationMs());
						radioUtil.setNonWifi(
								100 * (surveySample.getBusy() - surveySample.getBusyTx() - surveySample.getBusyRx())
										/ surveySample.getDurationMs());
						if (survey.getBand() == RadioBandType.BAND2G) {
							apNodeMetrics.getRadioUtilization2G().add(radioUtil);
						} else {
							apNodeMetrics.getRadioUtilization5G().add(radioUtil);
						}

					}

					// Double totalUtilization = 100D * totalBusy / durationMs;
					// LOG.trace("Total Utilization {}", totalUtilization);
					// Double totalWifiUtilization = 100D * (iBSS + oBSS) /
					// durationMs;
					// LOG.trace("Total Wifi Utilization {}", totalWifiUtilization);
					// LOG.trace("Total Non-Wifi Utilization {}", totalUtilization -
					// totalWifiUtilization);
					// if (survey.getBand() == RadioBandType.BAND2G) {
					// data.setChannelUtilization2G(totalUtilization.intValue());
					// } else {
					// data.setChannelUtilization5G(totalUtilization.intValue());
					// }
				}

			}
			LOG.debug("ApNodeMetrics Report {}", apNodeMetrics.toPrettyString());

		}

	}

	private void populateApClientMetrics(List<SingleMetricRecord> metricRecordList, Report report, int customerId,
			long equipmentId) {
		LOG.debug("populateApClientMetrics for Customer {} Equipment {}", customerId, equipmentId);

		for (ClientReport clReport : report.getClientsList()) {
			SingleMetricRecord smr = new SingleMetricRecord(customerId, equipmentId);
			metricRecordList.add(smr);

			ApClientMetrics apClientMetrics = new ApClientMetrics();
			smr.setData(apClientMetrics);
			smr.setCreatedTimestamp(clReport.getTimestampMs());

			smr.setCustomerId(customerId);
			smr.setEquipmentId(equipmentId);

			Integer periodLengthSec = 60; // matches what's configured by
											// OvsdbDao.configureStats(OvsdbClient)
			apClientMetrics.setPeriodLengthSec(periodLengthSec);

			List<ClientMetrics> clientMetrics = new ArrayList<>();

			for (Client cl : clReport.getClientListList()) {

				// clReport.getChannel();
				ClientMetrics cMetrics = new ClientMetrics();
				clientMetrics.add(cMetrics);

				RadioType radioType = RadioType.UNSUPPORTED;
				switch (clReport.getBand()) {
				case BAND2G:
					radioType = RadioType.is2dot4GHz;
					break;
				case BAND5G:
					radioType = RadioType.is5GHz;
					break;
				case BAND5GL:
					radioType = RadioType.is5GHzL;
					break;
				case BAND5GU:
					radioType = RadioType.is5GHzU;
					break;
				}
				cMetrics.setRadioType(radioType);
				cMetrics.setDeviceMacAddress(new MacAddress(cl.getMacAddress()));

				if (cl.hasStats()) {
					if (cl.getStats().hasRssi()) {
						cMetrics.setRssi(cl.getStats().getRssi());
					}

					// we'll report each device as having a single (very long)
					// session
					cMetrics.setSessionId(cMetrics.getDeviceMacAddress().getAddressAsLong());

					// populate Rx stats
					if (cl.getStats().hasRxBytes()) {
						cMetrics.setRxBytes(cl.getStats().getRxBytes());
					}

					if (cl.getStats().hasRxRate()) {
						// cMetrics.setAverageRxRate(cl.getStats().getRxRate());
					}

					if (cl.getStats().hasRxErrors()) {
						cMetrics.setNumRxNoFcsErr((int) cl.getStats().getRxErrors());
					}

					if (cl.getStats().hasRxFrames()) {
						// cMetrics.setNumRxFramesReceived(cl.getStats().getRxFrames());
						cMetrics.setNumRxPackets(cl.getStats().getRxFrames());
					}

					if (cl.getStats().hasRxRetries()) {
						cMetrics.setNumRxRetry((int) cl.getStats().getRxRetries());
					}

					// populate Tx stats
					if (cl.getStats().hasTxBytes()) {
						cMetrics.setNumTxBytes(cl.getStats().getTxBytes());
					}

					if (cl.getStats().hasTxRate()) {
						// cMetrics.setAverageTxRate(cl.getStats().getTxRate());
					}

					if (cl.getStats().hasTxRate() && cl.getStats().hasRxRate()) {
						cMetrics.setRates(
								new byte[] { (byte) (cl.getStats().getTxRate()), (byte) (cl.getStats().getRxRate()) });
					}

					if (cl.getStats().hasTxErrors()) {
						cMetrics.setNumTxDropped((int) cl.getStats().getTxErrors());
					}

					if (cl.getStats().hasRxFrames()) {
						// cMetrics.setNumTxFramesTransmitted(cl.getStats().getTxFrames());
						cMetrics.setNumTxPackets(cl.getStats().getRxFrames());
					}

					if (cl.getStats().hasTxRetries()) {
						cMetrics.setNumTxDataRetries((int) cl.getStats().getTxRetries());
					}

				}
			}
			
			switch (clReport.getBand()) {
			case BAND2G:
				apClientMetrics.setClientMetrics2g(clientMetrics.toArray(new ClientMetrics[0]));
				break;
			case BAND5G:
				apClientMetrics.setClientMetrics5g(clientMetrics.toArray(new ClientMetrics[0]));
				break;
			case BAND5GL:
				apClientMetrics.setClientMetrics5g(clientMetrics.toArray(new ClientMetrics[0]));
				break;
			case BAND5GU:
				apClientMetrics.setClientMetrics5g(clientMetrics.toArray(new ClientMetrics[0]));
				break;
			}

			LOG.debug("APClientMetrics Report {}", apClientMetrics.toPrettyString());

		}

	}

	private void populateApSsidMetrics(List<SingleMetricRecord> metricRecordList, Report report, int customerId,
			long equipmentId, String apId) {

		if (report.getClientsCount() == 0) {
			LOG.debug("No clients reported, will not populate ApSsidMetrics report");
			return;
		} else {
			LOG.debug("populateApSsidMetrics for Customer {} Equipment {} AP {}", customerId, equipmentId, apId);
		}

		// Need Radios to get BSSID value
		OpensyncAPRadioState radio2g = null;
		OpensyncAPRadioState radio5GHz = null;
		OpensyncAPRadioState radio5GHzL = null;
		OpensyncAPRadioState radio5GHzU = null;

		OpensyncAPVIFState vif2g = null;
		OpensyncAPVIFState vif5GHz = null;
		OpensyncAPVIFState vif5GHzL = null;
		OpensyncAPVIFState vif5GHzU = null;

		synchronized (opensyncNodeMap) {
			OpensyncNode node = opensyncNodeMap.get(apId);
			if (node == null) {
				LOG.debug("No AP Data present for AP SSID report");
				return;
			}
			radio2g = node.getRadioForBand(RadioType.is2dot4GHz.toString());
			radio5GHz = node.getRadioForBand(RadioType.is5GHz.toString());
			radio5GHzL = node.getRadioForBand(RadioType.is5GHzL.toString());
			radio5GHzU = node.getRadioForBand(RadioType.is5GHzU.toString());

			if (radio2g == null && radio5GHzL == null && radio5GHzU == null && radio5GHz == null) {
				LOG.debug("No Radio Data present for AP SSID report");
				return;
			}

			vif2g = node.getVIFForChannel(radio2g.getChannel());
			vif5GHz = node.getVIFForChannel(radio5GHz.getChannel());
			vif5GHzL = node.getVIFForChannel(radio5GHzL.getChannel());
			vif5GHzU = node.getVIFForChannel(radio5GHzU.getChannel());

			if (vif2g == null && vif5GHzL == null && vif5GHzU == null && vif5GHz == null) {
				LOG.debug("No Wifi SSID Data present for AP SSID report");
				return;
			}
		}

		SingleMetricRecord smr = new SingleMetricRecord(customerId, equipmentId);
		ApSsidMetrics apSsidMetrics = new ApSsidMetrics();
		List<SsidStatistics> ssidStatsList2pt4GHz = new ArrayList<>();
		List<SsidStatistics> ssidStatsList5GHzL = new ArrayList<>();
		List<SsidStatistics> ssidStatsList5GHzU = new ArrayList<>();
		List<SsidStatistics> ssidStatsList5GHz = new ArrayList<>();

		if (vif2g != null)
			apSsidMetrics.getSsidStats().put(RadioType.is2dot4GHz, ssidStatsList2pt4GHz);
		if (vif5GHz != null)
			apSsidMetrics.getSsidStats().put(RadioType.is5GHz, ssidStatsList5GHz);
		if (vif5GHzL != null)
			apSsidMetrics.getSsidStats().put(RadioType.is5GHzL, ssidStatsList5GHzL);
		if (vif5GHzU != null)
			apSsidMetrics.getSsidStats().put(RadioType.is5GHzU, ssidStatsList5GHzU);

		smr.setData(apSsidMetrics);
		metricRecordList.add(smr);

		for (ClientReport clientReport : report.getClientsList()) {

			// Always report the AP radio for that SSID if we are sending a
			// report
			// The '0' values will be overwritten, if applicable, from the
			// aggregated client information
			String encryption = null;
			SsidStatistics ssidStat = new SsidStatistics();
			if (clientReport.getBand() == RadioBandType.BAND2G) {
				ssidStat.setBssid(new MacAddress(radio2g.getMac()));
				ssidStat.setSsid(vif2g.getSsid());
				ssidStat.setNumClient(vif2g.getAssociatedClients().size());
				encryption = vif2g.getSecurity().get("encryption");
			} else if (clientReport.getBand() == RadioBandType.BAND5G) {
				ssidStat.setBssid(new MacAddress(radio5GHz.getMac()));
				ssidStat.setSsid(vif5GHz.getSsid());
				ssidStat.setNumClient(vif5GHz.getAssociatedClients().size());
				encryption = vif5GHz.getSecurity().get("encryption");
			} else if (clientReport.getBand() == RadioBandType.BAND5GL) {
				ssidStat.setBssid(new MacAddress(radio5GHzL.getMac()));
				ssidStat.setSsid(vif5GHzL.getSsid());
				ssidStat.setNumClient(vif5GHzL.getAssociatedClients().size());
				encryption = vif5GHzL.getSecurity().get("encryption");
			} else if (clientReport.getBand() == RadioBandType.BAND5GU) {
				ssidStat.setBssid(new MacAddress(radio5GHzU.getMac()));
				ssidStat.setSsid(vif5GHzU.getSsid());
				ssidStat.setNumClient(vif5GHzU.getAssociatedClients().size());
				encryption = vif5GHzU.getSecurity().get("encryption");
			}

			SecurityType securityType = SecurityType.UNSUPPORTED;
			if (encryption != null) {
				if (encryption.endsWith("PSK")) {
					securityType = SecurityType.PSK;
				} else if (encryption.equals("RADIUS")) {
					securityType = SecurityType.RADIUS;
				} else if (encryption.equals("OPEN")) {
					securityType = SecurityType.OPEN;
				}
			}

			long txBytes = 0L;
			long rxBytes = 0L;
			int txErrors = 0;
			int rxRetries = 0;
			int lastRssi = 0;

			for (Client client : clientReport.getClientListList()) {

				if (client.hasSsid() && client.hasStats()) {

					txBytes += client.getStats().getTxBytes();
					rxBytes += client.getStats().getRxBytes();
					txErrors += client.getStats().getTxErrors();
					rxRetries += client.getStats().getRxRetries();
					lastRssi = client.getStats().getRssi();
					try {
						handleClientSessionUpdate(customerId, equipmentId, apId, clientReport.getChannel(),
								clientReport.getBand(), clientReport.getTimestampMs(), client, securityType);
					} catch (Exception e) {
						LOG.debug("Unabled to update client {} session {}", client, e);
					}
				}

			}

			ssidStat.setRxLastRssi(-1 * lastRssi);
			ssidStat.setRxBytes(rxBytes);
			ssidStat.setNumTxBytesSucc(txBytes - txErrors);
			ssidStat.setNumRxRetry(rxRetries);

			if (clientReport.getBand() == RadioBandType.BAND2G) {
				ssidStatsList2pt4GHz.add(ssidStat);
			} else if (clientReport.getBand() == RadioBandType.BAND5G) {
				ssidStatsList5GHz.add(ssidStat);
			} else if (clientReport.getBand() == RadioBandType.BAND5GL) {
				ssidStatsList5GHzL.add(ssidStat);
			} else if (clientReport.getBand() == RadioBandType.BAND5GU) {
				ssidStatsList5GHzU.add(ssidStat);

			}

		}

		if (apSsidMetrics.getSsidStatsCount(RadioType.is2dot4GHz) == 0 && vif2g != null) {
			SsidStatistics ssidStat = new SsidStatistics();
			ssidStat.setBssid(new MacAddress(radio2g.getMac()));
			ssidStat.setSsid(vif2g.getSsid());
			ssidStat.setNumClient(vif2g.getAssociatedClients().size());
			ssidStatsList2pt4GHz.add(ssidStat);

		}
		if (apSsidMetrics.getSsidStatsCount(RadioType.is5GHz) == 0 && vif5GHz != null) {
			SsidStatistics ssidStat = new SsidStatistics();
			ssidStat.setBssid(new MacAddress(radio5GHz.getMac()));
			ssidStat.setSsid(vif5GHz.getSsid());
			ssidStat.setNumClient(vif5GHz.getAssociatedClients().size());
			ssidStatsList5GHzL.add(ssidStat);
		}
		if (apSsidMetrics.getSsidStatsCount(RadioType.is5GHzL) == 0 && vif5GHzL != null) {
			SsidStatistics ssidStat = new SsidStatistics();
			ssidStat.setBssid(new MacAddress(radio5GHzL.getMac()));
			ssidStat.setSsid(vif5GHzL.getSsid());
			ssidStat.setNumClient(vif5GHzL.getAssociatedClients().size());
			ssidStatsList5GHzL.add(ssidStat);
		}
		if (apSsidMetrics.getSsidStatsCount(RadioType.is5GHzU) == 0 && vif5GHzU != null) {
			SsidStatistics ssidStat = new SsidStatistics();
			ssidStat.setBssid(new MacAddress(radio5GHzU.getMac()));
			ssidStat.setSsid(vif5GHzU.getSsid());
			ssidStat.setNumClient(vif5GHzU.getAssociatedClients().size());
			ssidStatsList5GHzU.add(ssidStat);
		}

		LOG.debug("Created ApSsidMetrics Report {}", apSsidMetrics.toPrettyString());

	}

	private void handleClientSessionUpdate(int customerId, long equipmentId, String apId, int channel,
			RadioBandType band, long timestamp, Client client, SecurityType securityType) {

		boolean found = false;
		List<com.telecominfraproject.wlan.client.models.Client> clientRecords = clientServiceInterface.get(customerId,
				Collections.singleton(new MacAddress(client.getMacAddress())));
		if (!clientRecords.isEmpty()) {
			com.telecominfraproject.wlan.client.models.Client record = clientRecords.get(0);
			LOG.debug("Found Client {}", record.toPrettyString());
			found = true;
		}

		if (!found) {
			com.telecominfraproject.wlan.client.models.Client clientRecord = new com.telecominfraproject.wlan.client.models.Client();
			clientRecord.setCustomerId(customerId);
			clientRecord.setMacAddress(new MacAddress(client.getMacAddress()));
			clientRecord.setCreatedTimestamp(timestamp);

			ClientInfoDetails cid = new ClientInfoDetails();
			cid.setHostName(clientRecord.getMacAddress().getAddressAsString());
			cid.setApFingerprint(apId);
			clientRecord.setDetails(cid);

			try {
				LOG.debug("Created Client {}", clientServiceInterface.create(clientRecord).toPrettyString());
			} catch (Exception e) {
				LOG.error("Unabled to create client for {}", client.getMacAddress(), e);
			}
		}

		try {

			ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId,
					new MacAddress(client.getMacAddress()));
			if (clientSession == null) {
				LOG.debug("No session found for Client {}, creating new one.", client.getMacAddress());
				clientSession = new ClientSession();
				clientSession.setMacAddress(new MacAddress(client.getMacAddress()));
				clientSession.setCustomerId(customerId);
				clientSession.setEquipmentId(equipmentId);
				ClientSessionDetails clientSessionDetails = new ClientSessionDetails();
				clientSession.setDetails(clientSessionDetails);

				RadioType radioType = RadioType.UNSUPPORTED;
				switch (band) {
				case BAND2G:
					radioType = RadioType.is2dot4GHz;
					break;
				case BAND5G:
					radioType = RadioType.is5GHz;
					break;
				case BAND5GL:
					radioType = RadioType.is5GHzL;
					break;
				case BAND5GU:
					radioType = RadioType.is5GHzU;
					break;
				}
				clientSessionDetails.setRadioType(radioType);
				clientSessionDetails.setSessionId(clientSession.getMacAddress().getAddressAsLong());
				clientSessionDetails.setSsid(client.getSsid());
				clientSessionDetails.setAssociationStatus(0);
				clientSessionDetails.setAssocTimestamp(timestamp - client.getConnectOffsetMs());
				clientSessionDetails.setAuthTimestamp(timestamp - client.getConnectOffsetMs());
				clientSessionDetails.setFirstDataRcvdTimestamp(timestamp);
				clientSessionDetails.setFirstDataSentTimestamp(timestamp);
				clientSessionDetails.setLastRxTimestamp(timestamp);
				clientSessionDetails.setHostname(clientSession.getMacAddress().toOuiString());
				clientSessionDetails.setSessionId(clientSession.getMacAddress().getAddressAsLong());
				clientSessionDetails.setSecurityType(securityType);
				ClientDhcpDetails dhcpDetails = new ClientDhcpDetails(clientSessionDetails.getSessionId());
				clientSessionDetails.setDhcpDetails(dhcpDetails);
				ClientSessionMetricDetails metricDetails = new ClientSessionMetricDetails();
				metricDetails.setRssi(client.getStats().getRssi());
				metricDetails.setRxBytes(client.getStats().getRxBytes());
				metricDetails.setTxBytes(client.getStats().getTxBytes());
				metricDetails.setTotalTxPackets(client.getStats().getTxFrames());
				metricDetails.setTotalRxPackets(client.getStats().getRxFrames());
				metricDetails.setTxDataFrames(
						(int) ((int) client.getStats().getTxFrames() - client.getStats().getTxRetries()));
				metricDetails.setRxDataFrames(
						(int) ((int) client.getStats().getRxFrames() - client.getStats().getRxRetries()));
				metricDetails.setRxMbps((float) client.getStats().getRxRate());
				metricDetails.setTxMbps((float) client.getStats().getTxRate());
				clientSessionDetails.setMetricDetails(metricDetails);
			} else {
				ClientSessionDetails clientSessionDetails = clientSession.getDetails();
				clientSessionDetails.setAssociationStatus(0);
				if (clientSessionDetails.getDhcpDetails() == null) {
					ClientDhcpDetails dhcpDetails = new ClientDhcpDetails(clientSessionDetails.getSessionId());
					clientSessionDetails.setDhcpDetails(dhcpDetails);
				}

				clientSessionDetails.setSecurityType(securityType);
				clientSessionDetails.setAssocTimestamp(timestamp - client.getConnectOffsetMs());
				clientSessionDetails.setAuthTimestamp(timestamp - client.getConnectOffsetMs());
				clientSessionDetails.setFirstDataRcvdTimestamp(timestamp);
				clientSessionDetails.setFirstDataSentTimestamp(timestamp);
				clientSessionDetails.setLastRxTimestamp(timestamp);
				clientSessionDetails.setIsReassociation(true);
				clientSession.setCustomerId(customerId);
				clientSession.setEquipmentId(equipmentId);
				clientSessionDetails.setSessionId(clientSession.getMacAddress().getAddressAsLong());
				ClientSessionMetricDetails metricDetails = clientSessionDetails.getMetricDetails();

				if (client.hasStats()) {
					if (metricDetails == null) {

						LOG.debug("No metric details for client {} session {}, creating", clientSession.getMacAddress(),
								clientSessionDetails.getSessionId());
						metricDetails = new ClientSessionMetricDetails();
					}
					metricDetails.setRssi(client.getStats().getRssi());
					metricDetails.setRxBytes(client.getStats().getRxBytes());
					metricDetails.setTxBytes(client.getStats().getTxBytes());
					metricDetails.setTotalTxPackets(client.getStats().getTxFrames());
					metricDetails.setTotalRxPackets(client.getStats().getRxFrames());
					metricDetails.setTxDataFrames(
							(int) ((int) client.getStats().getTxFrames() - client.getStats().getTxRetries()));
					metricDetails.setRxDataFrames(
							(int) ((int) client.getStats().getRxFrames() - client.getStats().getRxRetries()));
					metricDetails.setRxMbps((float) client.getStats().getRxRate());
					metricDetails.setTxMbps((float) client.getStats().getTxRate());

					metricDetails.setLastRxTimestamp(timestamp);
					metricDetails.setLastTxTimestamp(timestamp);

				}
				clientSessionDetails.setMetricDetails(metricDetails);
				LOG.debug("Association State {}", clientSessionDetails.getAssociationState());

			}

			// TODO: We support bulk updates for sessions, which is more efficient way of
			// updating many sessions at once.
			// Need to rewrite this to make use of bulk operation.
			ClientSession session = clientServiceInterface.updateSession(clientSession);

			if (session != null)
				LOG.debug("CreatedOrUpdated clientSession {}", session.toPrettyString());

		} catch (Exception e) {
			LOG.error("Error while attempting to create ClientSession and Info", e);
		}
	}

	public void processMqttMessage(String topic, FlowReport flowReport) {

		LOG.info("Received report on topic {}", topic);
		int customerId = extractCustomerIdFromTopic(topic);

		long equipmentId = extractEquipmentIdFromTopic(topic);
		if (equipmentId <= 0 || customerId <= 0) {
			LOG.warn("Cannot determine equipment ids from topic {} - customerId {} equipmentId {}", topic, customerId,
					equipmentId);
			return;
		}

		String apId = extractApIdFromTopic(topic);

		if (apId == null) {
			LOG.warn("Cannot determine AP id from topic {} - customerId {} equipmentId {} apId {}", topic, customerId,
					equipmentId, apId);
			return;
		}

	}

	public void processMqttMessage(String topic, WCStatsReport wcStatsReport) {
		LOG.debug("Received WCStatsReport {}", wcStatsReport.toString());

		LOG.info("Received report on topic {}", topic);
		int customerId = extractCustomerIdFromTopic(topic);

		long equipmentId = extractEquipmentIdFromTopic(topic);
		if (equipmentId <= 0 || customerId <= 0) {
			LOG.warn("Cannot determine equipment ids from topic {} - customerId {} equipmentId {}", topic, customerId,
					equipmentId);
			return;
		}

		String apId = extractApIdFromTopic(topic);

		if (apId == null) {
			LOG.warn("Cannot determine AP id from topic {} - customerId {} equipmentId {} apId {}", topic, customerId,
					equipmentId, apId);
			return;
		}

	}

	@Override
	public void wifiVIFStateDbTableUpdate(List<OpensyncAPVIFState> vifStateTables, String apId) {
		if (vifStateTables == null || vifStateTables.isEmpty() || apId == null)
			return;

		Equipment ce = getCustomerEquipment(apId);
		if (ce == null) {
			LOG.debug("Cannot get equipmentId {} for apId {}", apId);
			return;
		}

		int customerId = ce.getCustomerId();
		long equipmentId = ce.getId();

		OpensyncNode osNode = null;

		synchronized (opensyncNodeMap) {

			if (opensyncNodeMap.containsKey(apId)) {

				Status activeBssids = statusServiceInterface.getOrNull(customerId, equipmentId,
						StatusDataType.ACTIVE_BSSIDS);
				if (activeBssids == null) {
					activeBssids = new Status();
					activeBssids.setCustomerId(customerId);
					activeBssids.setEquipmentId(equipmentId);
					activeBssids.setStatusDataType(StatusDataType.ACTIVE_BSSIDS);
					activeBssids.setDetails(new ActiveBSSIDs());
					statusServiceInterface.update(activeBssids);
				}
				ActiveBSSIDs bssids = (ActiveBSSIDs) activeBssids.getDetails();
				List<ActiveBSSID> bssidList = bssids.getActiveBSSIDs();

				osNode = opensyncNodeMap.get(apId);
				for (OpensyncAPVIFState vifState : vifStateTables) {
					if (vifState.isEnabled()) {
						osNode.updateVifState(vifState);
						String ssid = vifState.getSsid(); // ssid
						if (ssid != null) {
							int channel = vifState.getChannel(); // channel for this ssid
							int numDevicesConnected = vifState.getAssociatedClients().size();
							if (channel > 0) {
								OpensyncAPRadioState radioState = osNode.getRadioForChannel(channel);
								if (radioState != null) {
									String bssidAsMacString = radioState.getMac();
									RadioType radioType = null;
									if (osNode.getRadioForChannel(channel).getFreqBand().equals("2.4G"))
										radioType = RadioType.is2dot4GHz;
									else if (osNode.getRadioForChannel(channel).getFreqBand().equals("5GL"))
										radioType = RadioType.is5GHzL;
									else if (osNode.getRadioForChannel(channel).getFreqBand().equals("5GU"))
										radioType = RadioType.is5GHzU;
									else if (osNode.getRadioForChannel(channel).getFreqBand().equals("5G"))
										radioType = RadioType.is5GHz;

									if (bssidAsMacString != null && bssidAsMacString != "" && radioType != null) {

										boolean found = false;
										for (ActiveBSSID activeBssid : bssidList) {
											if (activeBssid.getBssid().equals(bssidAsMacString)
													&& activeBssid.getRadioType().equals(radioType)) {
												found = true;
											}
										}
										if (!found) {
											ActiveBSSID newBssid = new ActiveBSSID();
											newBssid.setBssid(bssidAsMacString);
											newBssid.setSsid(ssid);
											newBssid.setRadioType(radioType);
											newBssid.setNumDevicesConnected(numDevicesConnected);
											bssidList.add(newBssid);
											bssids.setActiveBSSIDs(bssidList);
											activeBssids.setDetails(bssids);
											statusServiceInterface.update(activeBssids);

										}
									}
								}
							}
						}
					}
				}
				opensyncNodeMap.put(apId, osNode);
				// LOG.debug("Updated VIF States for AP to NodeMap {}",
				// opensyncNodeMap.get(apId).toPrettyString());
			} else {

				// Do not add Status updates here as this is the only object in this map, and
				// therefore it will not contain the required peer entities to do the status
				// updates. These will be handled in subsequent table status updates.

				osNode = new OpensyncNode(apId, null, customerId, equipmentId);
				for (OpensyncAPVIFState vifState : vifStateTables) {
					if (vifState.isEnabled())
						osNode.updateVifState(vifState);
				}
				opensyncNodeMap.put(apId, osNode);
			}

			osNode = opensyncNodeMap.get(apId);

		}

	}

	@Override
	public void wifiRadioStatusDbTableUpdate(List<OpensyncAPRadioState> radioStateTables, String apId) {

		OvsdbSession session = ovsdbSessionMapInterface.getSession(apId);
		int customerId = session.getCustomerId();
		long equipmentId = session.getEquipmentId();

		if (equipmentId < 0L) {
			LOG.debug("Cannot get equipmentId {} for session {}", equipmentId);
			return;
		}

		if (radioStateTables == null || radioStateTables.isEmpty() || apId == null)
			return;

		// add to RadioStates States Map
		OpensyncNode osNode = null;
		synchronized (opensyncNodeMap) {

			if (opensyncNodeMap.containsKey(apId)) {
				osNode = opensyncNodeMap.get(apId);
				for (OpensyncAPRadioState radioState : radioStateTables) {
					if (radioState.isEnabled())
						osNode.updateRadioState(radioState);
				}
				opensyncNodeMap.put(apId, osNode);
			} else {
				osNode = new OpensyncNode(apId, null, customerId, equipmentId);
				for (OpensyncAPRadioState radioState : radioStateTables) {
					if (radioState.isEnabled())
						osNode.updateRadioState(radioState);
				}
				opensyncNodeMap.put(apId, osNode);

			}
		}
	}

	@Override
	public void wifiInetStateDbTableUpdate(List<OpensyncAPInetState> inetStateTables, String apId) {
		OvsdbSession session = ovsdbSessionMapInterface.getSession(apId);
		int customerId = session.getCustomerId();
		long equipmentId = session.getEquipmentId();

		if (equipmentId < 0L) {
			LOG.debug("Cannot get equipmentId {} for session {}", equipmentId);
			return;
		}

		if (inetStateTables == null || inetStateTables.isEmpty() || apId == null)
			return;

		// add to Inet States Map
		OpensyncNode osNode = null;
		synchronized (opensyncNodeMap) {
			if (opensyncNodeMap.containsKey(apId)) {
				osNode = opensyncNodeMap.get(apId);
				for (OpensyncAPInetState inetState : inetStateTables) {
					if (inetState.isEnabled())
						osNode.updateInetState(inetState);
				}
				opensyncNodeMap.put(apId, osNode);
			} else {
				osNode = new OpensyncNode(apId, null, customerId, equipmentId);
				for (OpensyncAPInetState inetState : inetStateTables) {
					if (inetState.isEnabled())
						osNode.updateInetState(inetState);
				}
				opensyncNodeMap.put(apId, osNode);

			}
		}

	}

	@Override
	public void wifiAssociatedClientsDbTableUpdate(List<OpensyncWifiAssociatedClients> wifiAssociatedClients,
			String apId) {
		OvsdbSession session = ovsdbSessionMapInterface.getSession(apId);
		int customerId = session.getCustomerId();
		long equipmentId = session.getEquipmentId();

		if (equipmentId < 0L) {
			LOG.debug("Cannot get equipmentId {} for session {}", equipmentId);
			return;
		}

		if (wifiAssociatedClients == null || wifiAssociatedClients.isEmpty() || apId == null)
			return;

		OpensyncNode osNode = null;
		synchronized (opensyncNodeMap) {
			if (opensyncNodeMap.containsKey(apId)) {
				osNode = opensyncNodeMap.get(apId);
				for (OpensyncWifiAssociatedClients wifiClient : wifiAssociatedClients) {
					osNode.updateWifiClients(wifiClient);
				}
				opensyncNodeMap.put(apId, osNode);
			} else {

				osNode = new OpensyncNode(apId, null, customerId, equipmentId);
				for (OpensyncWifiAssociatedClients wifiClient : wifiAssociatedClients) {
					osNode.updateWifiClients(wifiClient);
				}
				opensyncNodeMap.put(apId, osNode);
			}

		}
	}

	@Override
	public void awlanNodeDbTableUpdate(OpensyncAWLANNode opensyncAPState, String apId) {

		OvsdbSession session = ovsdbSessionMapInterface.getSession(apId);
		int customerId = session.getCustomerId();
		long equipmentId = session.getEquipmentId();

		if (equipmentId < 0L) {
			LOG.debug("Cannot get equipmentId {} for session {}", equipmentId);
			return;
		}
		if (opensyncAPState == null || apId == null)
			return;

		OpensyncNode osNode = null;
		synchronized (opensyncNodeMap) {
			if (opensyncNodeMap.containsKey(apId)) {
				osNode = opensyncNodeMap.get(apId);
				osNode.updateAWLANNode(opensyncAPState);
				opensyncNodeMap.put(apId, osNode);
				LOG.debug("Updated AWLAN_Node to map {}", osNode.toPrettyString());

			} else {

				osNode = new OpensyncNode(apId, opensyncAPState, customerId, equipmentId);
				opensyncNodeMap.put(apId, osNode);
			}

		}

	}

	@Override
	public void wifiVIFStateDbTableDelete(List<OpensyncAPVIFState> vifStateTables, String apId) {

		Equipment ce = getCustomerEquipment(apId);
		if (ce == null) {
			LOG.debug("Cannot get equipmentId {} for apId {}", apId);
			return;
		}

		int customerId = ce.getCustomerId();
		long equipmentId = ce.getId();

		synchronized (opensyncNodeMap) {

			if (opensyncNodeMap.containsKey(apId)) {
				OpensyncNode osNode = opensyncNodeMap.get(apId);

				for (OpensyncAPVIFState vifToDelete : vifStateTables) {
					if (osNode.deleteVif(vifToDelete)) {
						opensyncNodeMap.put(apId, osNode);
						LOG.debug("Deleted VIF for interface {} ssid {} from AP {}", vifToDelete.getIfName(),
								vifToDelete.getSsid(), apId);
					} else {
						LOG.debug("Cannot find VIF for interface {} ssid {} marked for deletion under AP {}",
								vifToDelete.getIfName(), vifToDelete.getSsid(), apId);
					}
				}

			} else {
				LOG.debug("AP {} is not present in cache, cannot delete VIFs", apId);
			}
		}
	}

	@Override
	public void wifiAssociatedClientsDbTableDelete(String deletedClientMac, String apId) {

		OvsdbSession session = ovsdbSessionMapInterface.getSession(apId);
		long equipmentId = session.getEquipmentId();

		if (equipmentId < 0L) {
			LOG.debug("Cannot get equipmentId {} for session {}", equipmentId);
			return;
		}

		synchronized (opensyncNodeMap) {
			if (opensyncNodeMap.containsKey(apId)) {
				OpensyncNode osNode = opensyncNodeMap.get(apId);
				if (osNode.deleteWifiClient(deletedClientMac)) {
					opensyncNodeMap.put(apId, osNode);
					LOG.debug("Deleted WifiClient {} from AP {}", deletedClientMac, apId);
				} else {
					LOG.debug("Cannot find WifiClient {} marked for deletion under AP {}", deletedClientMac, apId);
				}
			} else {
				LOG.debug("AP {} is not present in cache, cannot delete WifiClient {}", apId, deletedClientMac);
			}
		}

		try {
//			LOG.debug("Deleted AssociatedClient {}",
//					clientServiceInterface.delete(new MacAddress(deletedClientMac).getAddressAsLong()));
		} catch (Exception e) {
			LOG.error("Error deleting AssociatedClient {}", deletedClientMac, e);
		}

	}

}
