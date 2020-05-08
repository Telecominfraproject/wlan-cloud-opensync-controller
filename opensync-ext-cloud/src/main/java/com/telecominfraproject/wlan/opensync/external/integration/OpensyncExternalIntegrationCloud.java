package com.telecominfraproject.wlan.opensync.external.integration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.cloudeventdispatcher.CloudEventDispatcherInterface;
import com.telecominfraproject.wlan.core.model.entity.CountryCode;
import com.telecominfraproject.wlan.core.model.equipment.MacAddress;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.core.model.pagination.ColumnAndSort;
import com.telecominfraproject.wlan.core.model.pagination.PaginationContext;
import com.telecominfraproject.wlan.core.model.pagination.PaginationResponse;
import com.telecominfraproject.wlan.customer.models.Customer;
import com.telecominfraproject.wlan.customer.service.CustomerServiceInterface;
import com.telecominfraproject.wlan.equipment.EquipmentServiceInterface;
import com.telecominfraproject.wlan.equipment.models.ApElementConfiguration;
import com.telecominfraproject.wlan.equipment.models.ElementRadioConfiguration;
import com.telecominfraproject.wlan.equipment.models.Equipment;
import com.telecominfraproject.wlan.equipment.models.StateSetting;
import com.telecominfraproject.wlan.location.models.Location;
import com.telecominfraproject.wlan.location.service.LocationServiceInterface;
import com.telecominfraproject.wlan.opensync.experiment.OpenSyncConnectusController;
import com.telecominfraproject.wlan.opensync.external.integration.models.ConnectNodeInfo;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPInetState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPRadioConfig;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPRadioState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPSsidConfig;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPVIFState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAWLANNode;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncWifiAssociatedClients;
import com.telecominfraproject.wlan.profile.ProfileServiceInterface;
import com.telecominfraproject.wlan.profile.models.ProfileType;
import com.telecominfraproject.wlan.profile.network.models.ApNetworkConfiguration;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration.SecureMode;
import com.telecominfraproject.wlan.servicemetrics.models.ApClientMetrics;
import com.telecominfraproject.wlan.servicemetrics.models.ClientMetrics;
import com.telecominfraproject.wlan.servicemetrics.models.SingleMetricRecord;

import sts.PlumeStats.Client;
import sts.PlumeStats.ClientReport;
import sts.PlumeStats.RadioBandType;
import sts.PlumeStats.Report;
import traffic.NetworkMetadata.FlowReport;
import wc.stats.IpDnsTelemetry.WCStatsReport;

@Profile("opensync_cloud_config")
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
	private ProfileServiceInterface profileServiceInterface;
	@Autowired
	private OpenSyncConnectusController gatewayController;

	@Value("${connectus.ovsdb.autoProvisionedCustomerId:2}")
	private int autoProvisionedCustomerId;
	@Value("${connectus.ovsdb.autoProvisionedEquipmentId:2}")
	private int autoProvisionedEquipmentId;
	@Value("${connectus.ovsdb.autoProvisionedLocationId:5}")
	private int autoProvisionedLocationId;

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
					// TODO: need to be able to get Equipment by AP Id
					return equipmentServiceInterface.getOrNull(autoProvisionedEquipmentId);
				}
			});
		} catch (Exception e) {
			// do nothing
		}

		return ce;
	}

	public void apConnected(String apId, ConnectNodeInfo connectNodeInfo) {
		LOG.info("AP {} got connected to the gateway", apId);

		Customer customer = null;
		try {
			customer = customerServiceInterface.get(autoProvisionedCustomerId);
			LOG.debug("Got Customer {} for apId {}", customer.toPrettyString(), apId);
		} catch (Exception e) {
			LOG.error("Caught exception getting customer for Id {} for apId {}", autoProvisionedCustomerId, apId, e);
		}

		Equipment ce = null;
		try {
			ce = getCustomerEquipment(apId);
			LOG.debug("Got Equipment {} for apId {}", ce.toPrettyString());
			ce.setName(apId);

			ce = equipmentServiceInterface.update(ce);
			LOG.debug("Updated equipment {} for apId {}", ce.toPrettyString(), apId);

		} catch (Exception e) {
			LOG.error("Caught exception getting equipment for Id {} for apId {}", autoProvisionedEquipmentId, apId, e);

		}

		List<Location> locationList = locationServiceInterface.getAllForCustomer(ce.getCustomerId());
		for (Location location : locationList) {
			LOG.debug("Location {} for Customer {}", location.toPrettyString(), ce.getCustomerId());
		}

		PaginationResponse<com.telecominfraproject.wlan.profile.models.Profile> paginationResponse = profileServiceInterface
				.getForCustomer(ce.getCustomerId(), new ArrayList<ColumnAndSort>(),
						new PaginationContext<com.telecominfraproject.wlan.profile.models.Profile>(10));

		for (com.telecominfraproject.wlan.profile.models.Profile profile : paginationResponse.getItems()) {
			LOG.debug("Profile {} for Customer {}", profile.toPrettyString(), ce.getCustomerId());
		}

		try {
			OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);
			ovsdbSession.setEquipmentId(2);
			ovsdbSession.setCustomerId(2);
		} catch (Exception e) {
			LOG.error("Caught exception getting ovsdbSession for apId {}", apId, e);
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
//				routingServiceInterface.delete(ovsdbSession.getRoutingId());
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
			long equipmentId = ovsdbSession.getEquipmentId();
			Equipment resolvedEqCfg = equipmentServiceInterface.get(equipmentId);

			if (resolvedEqCfg == null) {
				throw new IllegalStateException("Cannot retrieve configuration for " + apId);
			}
			ret = new OpensyncAPConfig();
			Location eqLocation = locationServiceInterface.get(resolvedEqCfg.getLocationId());

			// extract country, radio channels from resolvedEqCfg
			String country = "CA";
			CountryCode countryCode = Location.getCountryCode(eqLocation);
			if (countryCode != null && countryCode != CountryCode.UNSUPPORTED) {
				country = countryCode.toString().toUpperCase();
			}

			int radioChannel24G = 1;
			int radioChannel5LG = 44;
			int radioChannel5HG = 108;

			ApElementConfiguration apElementConfiguration = (ApElementConfiguration) resolvedEqCfg.getDetails();

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

			OpensyncAPRadioConfig radioConfig = new OpensyncAPRadioConfig();
			radioConfig.setCountry(country);
			radioConfig.setRadioChannel24G(radioChannel24G);
			radioConfig.setRadioChannel5LG(radioChannel5LG);
			radioConfig.setRadioChannel5HG(radioChannel5HG);

			ret.setRadioConfig(radioConfig);

			// extract ssid parameters from resolvedEqCfg
			List<OpensyncAPSsidConfig> ssidConfigs = new ArrayList<>();

			com.telecominfraproject.wlan.profile.models.Profile apProfile = profileServiceInterface
					.get(resolvedEqCfg.getProfileId());

			Set<Long> childProfileIds = apProfile.getChildProfileIds();
			for (Long id : childProfileIds) {
				com.telecominfraproject.wlan.profile.models.Profile profile = profileServiceInterface.get(id);
				if (profile.getProfileType().equals(ProfileType.ssid)) {
					SsidConfiguration ssidCfg = (SsidConfiguration) profile.getDetails();

					for (RadioType radioType : ssidCfg.getAppliedRadios()) {
						OpensyncAPSsidConfig osSsidCfg = new OpensyncAPSsidConfig();

						osSsidCfg.setSsid(profile.getName());
						osSsidCfg.setRadioType(radioType);
						osSsidCfg.setBroadcast(ssidCfg.getBroadcastSsid() == StateSetting.enabled);

						if (ssidCfg.getSecureMode() == SecureMode.wpa2OnlyPSK
								|| ssidCfg.getSecureMode() == SecureMode.wpa2PSK) {
							osSsidCfg.setEncryption("WPA-PSK");
							osSsidCfg.setMode("2");
						} else if (ssidCfg.getSecureMode() == SecureMode.wpaPSK) {
							osSsidCfg.setEncryption("WPA-PSK");
							osSsidCfg.setMode("1");
						} else {
							LOG.warn("Unsupported encryption mode {} - will use WPA-PSK instead",
									ssidCfg.getSecureMode());
							osSsidCfg.setEncryption("WPA-PSK");
							osSsidCfg.setMode("2");
						}

						if (ssidCfg.getKeyStr() == null) {
							osSsidCfg.setKey("12345678");
						} else {
							osSsidCfg.setKey(ssidCfg.getKeyStr());
						}

						ssidConfigs.add(osSsidCfg);

					}

				}
			}

			ret.setSsidConfigs(ssidConfigs);

			for (OpensyncAPSsidConfig osSsidCfg : ssidConfigs) {
				LOG.debug("Mike OpensyncAPSsidConfig {}", osSsidCfg.toPrettyString());
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
		// TODO: populateApNodeMetrics(metricRecordList, report, customerId,
		// equipmentId, extractApIdFromTopic(topic));

		if (!metricRecordList.isEmpty()) {
			equipmentMetricsCollectorInterface.publishMetrics(metricRecordList);
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

				cMetrics.setRadioType(
						(clReport.getBand() == RadioBandType.BAND2G) ? RadioType.is2dot4GHz : RadioType.is5GHz);
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

			if (clReport.getBand() == RadioBandType.BAND2G) {
				apClientMetrics.setClientMetrics2g(clientMetrics.toArray(new ClientMetrics[0]));
			} else {
				apClientMetrics.setClientMetrics5g(clientMetrics.toArray(new ClientMetrics[0]));
			}
			LOG.debug("APClientMetrics Report {}", apClientMetrics.toPrettyString());

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
		OpensyncNode osNode = null;

		synchronized (opensyncNodeMap) {

			if (opensyncNodeMap.containsKey(apId)) {
				osNode = opensyncNodeMap.get(apId);
				for (OpensyncAPVIFState vifState : vifStateTables) {
					if (vifState.isEnabled())
						osNode.updateVifState(vifState);
				}
				opensyncNodeMap.put(apId, osNode);
				// LOG.debug("Updated VIF States for AP to NodeMap {}",
				// opensyncNodeMap.get(apId).toPrettyString());
			} else {
				OvsdbSession session = ovsdbSessionMapInterface.getSession(apId);

				if (session != null) {
					int customerId = session.getCustomerId();
					long equipmentId = session.getEquipmentId();
					osNode = new OpensyncNode(apId, null, customerId, equipmentId);
					for (OpensyncAPVIFState vifState : vifStateTables) {
						if (vifState.isEnabled())
							osNode.updateVifState(vifState);
					}
					opensyncNodeMap.put(apId, osNode);
				}
			}

			osNode = opensyncNodeMap.get(apId);
			List<OpensyncAPVIFState> vifStates = osNode.getVifStates();
			LOG.debug(
					"BSSID                           SSID                           AUTH MODE        RADIO       DEVICES");

			for (OpensyncAPVIFState vif : vifStates) {
				String ssid = vif.getSsid();
				int channel = vif.getChannel();
				int devices = vif.getAssociatedClients().size();
				String bssid = osNode.getRadioForChannel(channel).getMac();
				String freqBand = osNode.getRadioForChannel(channel).getFreqBand();
				String encryption = vif.getSecurity().get("encryption");

				LOG.debug("{}       {}     {}              {}          {}", bssid, ssid, encryption, freqBand, devices);
			}

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
		OvsdbSession session = ovsdbSessionMapInterface.getSession(apId);
		long equipmentId = session.getEquipmentId();

		if (equipmentId < 0L) {
			LOG.debug("Cannot get equipmentId {} for session {}", equipmentId);
			return;
		}

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
