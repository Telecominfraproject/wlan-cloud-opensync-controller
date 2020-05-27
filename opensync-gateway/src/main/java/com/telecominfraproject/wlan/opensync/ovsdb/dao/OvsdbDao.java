package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.core.model.equipment.ChannelBandwidth;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.equipment.models.ApElementConfiguration;
import com.telecominfraproject.wlan.equipment.models.ElementRadioConfiguration;
import com.telecominfraproject.wlan.equipment.models.RadioConfiguration;
import com.telecominfraproject.wlan.equipment.models.RadioMode;
import com.telecominfraproject.wlan.equipment.models.StateSetting;
import com.telecominfraproject.wlan.opensync.external.integration.models.ConnectNodeInfo;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPInetState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPRadioState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPVIFState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAWLANNode;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncWifiAssociatedClients;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.BridgeInfo;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.InterfaceInfo;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.PortInfo;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.WifiInetConfigInfo;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.WifiRadioConfigInfo;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.WifiStatsConfigInfo;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.WifiVifConfigInfo;
import com.telecominfraproject.wlan.profile.models.Profile;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration;
import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.protocol.methods.RowUpdate;
import com.vmware.ovsdb.protocol.methods.TableUpdate;
import com.vmware.ovsdb.protocol.methods.TableUpdates;
import com.vmware.ovsdb.protocol.operation.Delete;
import com.vmware.ovsdb.protocol.operation.Insert;
import com.vmware.ovsdb.protocol.operation.Operation;
import com.vmware.ovsdb.protocol.operation.Select;
import com.vmware.ovsdb.protocol.operation.Update;
import com.vmware.ovsdb.protocol.operation.notation.Atom;
import com.vmware.ovsdb.protocol.operation.notation.Condition;
import com.vmware.ovsdb.protocol.operation.notation.Function;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;
import com.vmware.ovsdb.protocol.operation.notation.Value;
import com.vmware.ovsdb.protocol.operation.result.InsertResult;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.protocol.operation.result.SelectResult;
import com.vmware.ovsdb.service.OvsdbClient;

@Component
public class OvsdbDao {
	private static final Logger LOG = LoggerFactory.getLogger(OvsdbDao.class);

	@org.springframework.beans.factory.annotation.Value("${connectus.ovsdb.managerAddr:3.88.149.10}")
	private String managerIpAddr;

	@org.springframework.beans.factory.annotation.Value("${connectus.ovsdb.listenPort:6640}")
	private int ovsdbListenPort;

	@org.springframework.beans.factory.annotation.Value("${connectus.mqttBroker.address:testportal.123wlan.com}")
	private String mqttBrokerAddress;

	@org.springframework.beans.factory.annotation.Value("${connectus.mqttBroker.listenPort:1883}")
	private int mqttBrokerListenPort;

	@org.springframework.beans.factory.annotation.Value("${connectus.ovsdb.timeoutSec:30}")
	private int ovsdbTimeoutSec;

	public static final String ovsdbName = "Open_vSwitch";
	public static final String awlanNodeDbTable = "AWLAN_Node";
	public static final String wifiStatsConfigDbTable = "Wifi_Stats_Config";

	public static final String interfaceDbTable = "Interface";
	public static final String portDbTable = "Port";
	public static final String bridgeDbTable = "Bridge";

	public static final String wifiRadioConfigDbTable = "Wifi_Radio_Config";
	public static final String wifiRadioStateDbTable = "Wifi_Radio_State";

	public static final String wifiVifConfigDbTable = "Wifi_VIF_Config";
	public static final String wifiVifStateDbTable = "Wifi_VIF_State";

	public static final String wifiInetConfigDbTable = "Wifi_Inet_Config";
	public static final String wifiInetStateDbTable = "Wifi_Inet_State";

	public static final String wifiAssociatedClientsDbTable = "Wifi_Associated_Clients";

	public ConnectNodeInfo getConnectNodeInfo(OvsdbClient ovsdbClient) {
		ConnectNodeInfo ret = new ConnectNodeInfo();

		try {
			List<Operation> operations = new ArrayList<>();
			List<Condition> conditions = new ArrayList<>();
			List<String> columns = new ArrayList<>();
			columns.add("mqtt_settings");
			columns.add("redirector_addr");
			columns.add("manager_addr");
			columns.add("sku_number");
			columns.add("serial_number");
			columns.add("model");
			columns.add("firmware_version");
			columns.add("platform_version");

			operations.add(new Select(awlanNodeDbTable, conditions, columns));
			CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
			OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

			if (LOG.isDebugEnabled()) {
				LOG.debug("Select from {}:", awlanNodeDbTable);

				for (OperationResult res : result) {
					LOG.debug("Op Result {}", res);
				}
			}

			Row row = null;
			if (result != null && result.length > 0 && !((SelectResult) result[0]).getRows().isEmpty()) {
				row = ((SelectResult) result[0]).getRows().iterator().next();
			}

			ret.mqttSettings = (row != null) ? row.getMapColumn("mqtt_settings") : null;
			ret.redirectorAddr = (row != null) ? row.getStringColumn("redirector_addr") : null;
			ret.managerAddr = (row != null) ? row.getStringColumn("manager_addr") : null;

			ret.platformVersion = (row != null) ? row.getStringColumn("platform_version") : null;
			ret.firmwareVersion = (row != null) ? row.getStringColumn("firmware_version") : null;

			ret.skuNumber = getSingleValueFromSet(row, "sku_number");
			ret.serialNumber = getSingleValueFromSet(row, "serial_number");
			ret.model = getSingleValueFromSet(row, "model");

			// now populate macAddress, ipV4Address from Wifi_Inet_State
			// first look them up for if_name = br-wan
			fillInIpAddressAndMac(ovsdbClient, ret, "br-wan");
			if (ret.ipV4Address == null || ret.macAddress == null) {
				// when not found - look them up for if_name = br-lan
				fillInIpAddressAndMac(ovsdbClient, ret, "br-lan");
			}

		} catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
			throw new RuntimeException(e);
		}

		return ret;
	}

	public void fillInIpAddressAndMac(OvsdbClient ovsdbClient, ConnectNodeInfo connectNodeInfo, String ifName) {
		try {
			List<Operation> operations = new ArrayList<>();
			List<Condition> conditions = new ArrayList<>();
			List<String> columns = new ArrayList<>();
			// populate macAddress, ipV4Address from Wifi_Inet_State

			columns.add("inet_addr");
			columns.add("hwaddr");

			conditions.add(new Condition("if_name", Function.EQUALS, new Atom<>(ifName)));

			operations.add(new Select(wifiInetStateDbTable, conditions, columns));
			CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
			OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

			if (LOG.isDebugEnabled()) {
				LOG.debug("Select from {}:", wifiInetStateDbTable);

				for (OperationResult res : result) {
					LOG.debug("Op Result {}", res);
				}
			}

			Row row = null;
			if (result != null && result.length > 0 && !((SelectResult) result[0]).getRows().isEmpty()) {
				row = ((SelectResult) result[0]).getRows().iterator().next();
				connectNodeInfo.ipV4Address = getSingleValueFromSet(row, "inet_addr");
				connectNodeInfo.macAddress = row.getStringColumn("hwaddr");
			}

		} catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
			throw new RuntimeException(e);
		}

	}

	public ConnectNodeInfo updateConnectNodeInfoOnConnect(OvsdbClient ovsdbClient, String clientCn,
			ConnectNodeInfo incomingConnectNodeInfo) {
		ConnectNodeInfo ret = incomingConnectNodeInfo.clone();

		try {
			List<Operation> operations = new ArrayList<>();
			Map<String, Value> updateColumns = new HashMap<>();

			// set device_mode = cloud - plume's APs do not use it
			// updateColumns.put("device_mode", new Atom<String>("cloud") );

			// update sku_number if it was empty
			if (ret.skuNumber == null || ret.skuNumber.isEmpty()) {
				ret.skuNumber = "connectus.ai_" + ret.serialNumber;
				updateColumns.put("sku_number", new Atom<String>(ret.skuNumber));
			}

			// Configure the MQTT connection
			// ovsh u AWLAN_Node
			// mqtt_settings:ins:'["map",[["broker","testportal.123wlan.com"],["topics","/ap/dev-ap-0300/opensync"],["qos","0"],["port","1883"],["remote_log","1"]]]'
			Map<String, String> newMqttSettings = new HashMap<>();
			newMqttSettings.put("broker", mqttBrokerAddress);
			newMqttSettings.put("topics", "/ap/" + clientCn + "_" + ret.serialNumber + "/opensync");
			newMqttSettings.put("port", "" + mqttBrokerListenPort);
			newMqttSettings.put("compress", "zlib");
			newMqttSettings.put("qos", "0");
			newMqttSettings.put("remote_log", "1");

			if (ret.mqttSettings == null || !ret.mqttSettings.equals(newMqttSettings)) {
				@SuppressWarnings("unchecked")
				com.vmware.ovsdb.protocol.operation.notation.Map<String, String> mgttSettings = com.vmware.ovsdb.protocol.operation.notation.Map
						.of(newMqttSettings);
				ret.mqttSettings = newMqttSettings;
				updateColumns.put("mqtt_settings", mgttSettings);
			}

			if (!updateColumns.isEmpty()) {
				Row row = new Row(updateColumns);
				operations.add(new Update(awlanNodeDbTable, row));

				CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
				OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

				if (LOG.isDebugEnabled()) {
					LOG.debug("Updated {}:", awlanNodeDbTable);

					for (OperationResult res : result) {
						LOG.debug("Op Result {}", res);
					}
				}
			}

		} catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
			throw new RuntimeException(e);
		}

		return ret;
	}

	/**
	 * @param ovsdbClient
	 * @return value of reporting_interval column for the stats_type=device from the
	 *         Wifi_Stats_Config table. If value is not provisioned then return -1.
	 */
	public long getDeviceStatsReportingInterval(OvsdbClient ovsdbClient) {
		long ret = -1;
		try {
			List<Operation> operations = new ArrayList<>();
			List<Condition> conditions = new ArrayList<>();
			List<String> columns = new ArrayList<>();

			columns.add("reporting_interval");
			columns.add("stats_type");
			columns.add("radio_type");

			conditions.add(new Condition("stats_type", Function.EQUALS, new Atom<>("device")));

			operations.add(new Select(wifiStatsConfigDbTable, conditions, columns));
			CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
			OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

			if (LOG.isDebugEnabled()) {
				LOG.debug("Select from {}:", wifiStatsConfigDbTable);

				for (OperationResult res : result) {
					LOG.debug("Op Result {}", res);
				}
			}

			Row row = null;
			if (result != null && result.length > 0 && !((SelectResult) result[0]).getRows().isEmpty()) {
				row = ((SelectResult) result[0]).getRows().iterator().next();
				ret = row.getIntegerColumn("reporting_interval");
				LOG.info("Stats collection for stats_type=device is already configured with reporting_interval = {}",
						ret);
			}

		} catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
			throw new RuntimeException(e);
		}

		return ret;
	}

	/**
	 * @param ovsdbClient
	 * @param value       of reporting_interval column for the stats_type=device
	 *                    from the Wifi_Stats_Config table. If value is not
	 *                    provisioned then return -1.
	 */
	public void updateDeviceStatsReportingInterval(OvsdbClient ovsdbClient, long newValue) {
		try {
			List<Operation> operations = new ArrayList<>();
			Map<String, Value> updateColumns = new HashMap<>();

			// turn on stats collection over MQTT: (reporting_interval is in
			// seconds?)
			// $ ovsh i Wifi_Stats_Config reporting_interval:=10
			// radio_type:="2.4G" stats_type:="device"

			updateColumns.put("reporting_interval", new Atom<Integer>(10));
			updateColumns.put("radio_type", new Atom<String>("2.4G"));
			updateColumns.put("stats_type", new Atom<String>("device"));

			Row row = new Row(updateColumns);
			operations.add(new Insert(wifiStatsConfigDbTable, row));

			CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
			OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

			if (LOG.isDebugEnabled()) {
				LOG.debug("Updated {}:", wifiStatsConfigDbTable);

				for (OperationResult res : result) {
					LOG.debug("Op Result {}", res);
				}
			}

		} catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
			throw new RuntimeException(e);
		}

	}

	public void performRedirect(OvsdbClient ovsdbClient, String clientCn) {

		List<Operation> operations = new ArrayList<>();
		List<Condition> conditions = new ArrayList<>();
		List<String> columns = new ArrayList<>();
		columns.add("manager_addr");
		columns.add("sku_number");
		columns.add("serial_number");
		columns.add("model");
		columns.add("firmware_version");

		try {
			LOG.debug("Starting Redirect");

			operations.add(new Select(awlanNodeDbTable, conditions, columns));
			CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
			OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

			LOG.debug("Select from AWLAN_Node:");

			for (OperationResult res : result) {
				LOG.debug("Op Result {}", res);
			}

			String skuNumber = null;
			String serialNumber = null;
			String model = null;
			String firmwareVersion = null;

			Row row = null;
			if (result != null && result.length > 0 && !((SelectResult) result[0]).getRows().isEmpty()) {
				row = ((SelectResult) result[0]).getRows().iterator().next();
			}

			firmwareVersion = (row != null) ? row.getStringColumn("firmware_version") : null;

			skuNumber = getSingleValueFromSet(row, "sku_number");
			serialNumber = getSingleValueFromSet(row, "serial_number");
			model = getSingleValueFromSet(row, "model");

			LOG.info("Redirecting AP Node: clientCn {} serialNumber {} model {} firmwareVersion {} skuNumber {}",
					clientCn, serialNumber, model, firmwareVersion, skuNumber);

			// Update table AWLAN_Node - set manager_addr
			operations.clear();
			Map<String, Value> updateColumns = new HashMap<>();

			updateColumns.put("manager_addr", new Atom<String>("ssl:" + managerIpAddr + ":" + ovsdbListenPort));

			row = new Row(updateColumns);
			operations.add(new Update(awlanNodeDbTable, row));

			fResult = ovsdbClient.transact(ovsdbName, operations);
			result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

			LOG.debug("Updated AWLAN_Node:");

			for (OperationResult res : result) {
				LOG.debug("Op Result {}", res);
			}

			LOG.debug("Redirect Done");
		} catch (ExecutionException | InterruptedException | OvsdbClientException | TimeoutException e) {
			LOG.error("Error when redirecting AP Node", e);
			throw new RuntimeException(e);
		}

	}

	public <T> T getSingleValueFromSet(Row row, String columnName) {

		Set<T> set = (row != null) ? row.getSetColumn(columnName) : null;
		T ret = (set != null && !set.isEmpty()) ? set.iterator().next() : null;

		return ret;
	}

	public Map<String, InterfaceInfo> getProvisionedInterfaces(OvsdbClient ovsdbClient) {
		Map<String, InterfaceInfo> ret = new HashMap<>();

		List<Operation> operations = new ArrayList<>();
		List<Condition> conditions = new ArrayList<>();
		List<String> columns = new ArrayList<>();
		columns.add("name");
		columns.add("type");
		columns.add("options");
		columns.add("_uuid");
		columns.add("ofport");
		columns.add("mtu");
		columns.add("ifindex");
		columns.add("link_state");
		columns.add("admin_state");

		try {
			LOG.debug("Retrieving Interfaces:");

			operations.add(new Select(interfaceDbTable, conditions, columns));
			CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
			OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

			for (OperationResult res : result) {
				LOG.debug("Op Result {}", res);
			}

			for (Row row : ((SelectResult) result[0]).getRows()) {

				InterfaceInfo interfaceInfo = new InterfaceInfo();
				interfaceInfo.name = row.getStringColumn("name");
				interfaceInfo.type = row.getStringColumn("type");
				interfaceInfo.uuid = row.getUuidColumn("_uuid");

				Long tmp = getSingleValueFromSet(row, "ofport");
				interfaceInfo.ofport = tmp != null ? tmp.intValue() : 0;

				tmp = getSingleValueFromSet(row, "mtu");
				interfaceInfo.mtu = tmp != null ? tmp.intValue() : 0;

				tmp = getSingleValueFromSet(row, "ifindex");
				interfaceInfo.ifIndex = tmp != null ? tmp.intValue() : 0;

				String tmpStr = getSingleValueFromSet(row, "link_state");
				interfaceInfo.linkState = tmpStr != null ? tmpStr : "";

				tmpStr = getSingleValueFromSet(row, "admin_state");
				interfaceInfo.adminState = tmpStr != null ? tmpStr : "";

				interfaceInfo.options = row.getMapColumn("options");

				ret.put(interfaceInfo.name, interfaceInfo);
			}

			LOG.debug("Retrieved Interfaces: {}", ret);

		} catch (ExecutionException | InterruptedException | OvsdbClientException | TimeoutException e) {
			LOG.error("Error in getProvisionedInterfaces", e);

			throw new RuntimeException(e);
		}

		return ret;
	}

	public Map<String, PortInfo> getProvisionedPorts(OvsdbClient ovsdbClient) {
		Map<String, PortInfo> ret = new HashMap<>();

		List<Operation> operations = new ArrayList<>();
		List<Condition> conditions = new ArrayList<>();
		List<String> columns = new ArrayList<>();
		columns.add("name");
		columns.add("_uuid");
		columns.add("interfaces");

		try {
			LOG.debug("Retrieving Ports:");

			operations.add(new Select(portDbTable, conditions, columns));
			CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
			OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

			for (OperationResult res : result) {
				LOG.debug("Op Result {}", res);
			}

			for (Row row : ((SelectResult) result[0]).getRows()) {

				PortInfo portInfo = new PortInfo();
				portInfo.name = row.getStringColumn("name");
				portInfo.uuid = row.getUuidColumn("_uuid");
				portInfo.interfaceUuids = row.getSetColumn("interfaces");

				ret.put(portInfo.name, portInfo);
			}

			LOG.debug("Retrieved Ports: {}", ret);

		} catch (ExecutionException | InterruptedException | OvsdbClientException | TimeoutException e) {
			LOG.error("Error in getProvisionedPorts", e);
			throw new RuntimeException(e);
		}

		return ret;
	}

	public Map<String, BridgeInfo> getProvisionedBridges(OvsdbClient ovsdbClient) {
		Map<String, BridgeInfo> ret = new HashMap<>();

		List<Operation> operations = new ArrayList<>();
		List<Condition> conditions = new ArrayList<>();
		List<String> columns = new ArrayList<>();
		columns.add("name");
		columns.add("_uuid");
		columns.add("ports");

		try {
			LOG.debug("Retrieving Bridges:");

			operations.add(new Select(bridgeDbTable, conditions, columns));
			CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
			OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

			for (OperationResult res : result) {
				LOG.debug("Op Result {}", res);
			}

			for (Row row : ((SelectResult) result[0]).getRows()) {

				BridgeInfo bridgeInfo = new BridgeInfo();
				bridgeInfo.name = row.getStringColumn("name");
				bridgeInfo.uuid = row.getUuidColumn("_uuid");
				bridgeInfo.portUuids = row.getSetColumn("ports");

				ret.put(bridgeInfo.name, bridgeInfo);
			}

			LOG.debug("Retrieved Bridges: {}", ret);

		} catch (ExecutionException | InterruptedException | OvsdbClientException | TimeoutException e) {
			LOG.error("Error in getProvisionedBridges", e);
			throw new RuntimeException(e);
		}

		return ret;
	}

	public Map<String, WifiRadioConfigInfo> getProvisionedWifiRadioConfigs(OvsdbClient ovsdbClient) {
		Map<String, WifiRadioConfigInfo> ret = new HashMap<>();

		List<Operation> operations = new ArrayList<>();
		List<Condition> conditions = new ArrayList<>();
		List<String> columns = new ArrayList<>();

		columns.add("_uuid");
		columns.add("if_name");

		columns.add("channel");
		columns.add("channel_mode");
		columns.add("country");
		columns.add("enabled");
		columns.add("ht_mode");
		columns.add("tx_power");
		columns.add("vif_configs");
		columns.add("freq_band");
		columns.add("hw_config");

		try {
			LOG.debug("Retrieving WifiRadioConfig:");

			operations.add(new Select(wifiRadioConfigDbTable, conditions, columns));
			CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
			OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

			for (OperationResult res : result) {
				LOG.debug("Op Result {}", res);
			}

			for (Row row : ((SelectResult) result[0]).getRows()) {

				WifiRadioConfigInfo wifiRadioConfigInfo = new WifiRadioConfigInfo();
				wifiRadioConfigInfo.uuid = row.getUuidColumn("_uuid");
				wifiRadioConfigInfo.ifName = row.getStringColumn("if_name");
				Long channelTmp = getSingleValueFromSet(row, "channel");
				if (channelTmp == null) {
					channelTmp = -1L;
				}
				wifiRadioConfigInfo.channel = channelTmp.intValue();
				wifiRadioConfigInfo.channelMode = getSingleValueFromSet(row, "channel_mode");
				wifiRadioConfigInfo.country = getSingleValueFromSet(row, "country");
				Boolean tmp = getSingleValueFromSet(row, "enabled");
				wifiRadioConfigInfo.enabled = tmp != null ? tmp : false;
				wifiRadioConfigInfo.htMode = getSingleValueFromSet(row, "ht_mode");
				wifiRadioConfigInfo.txPower = getSingleValueFromSet(row, "txPower");
				wifiRadioConfigInfo.vifConfigUuids = row.getSetColumn("vif_configs");
				wifiRadioConfigInfo.freqBand = row.getStringColumn("freq_band");
				wifiRadioConfigInfo.hwConfig = row.getMapColumn("hw_config");

				ret.put(wifiRadioConfigInfo.ifName, wifiRadioConfigInfo);
			}

			LOG.debug("Retrieved WifiRadioConfig: {}", ret);

		} catch (ExecutionException | InterruptedException | OvsdbClientException | TimeoutException e) {
			LOG.error("Error in getProvisionedWifiRadioConfigs", e);
			throw new RuntimeException(e);
		}

		return ret;
	}

	public Map<String, WifiVifConfigInfo> getProvisionedWifiVifConfigs(OvsdbClient ovsdbClient) {
		Map<String, WifiVifConfigInfo> ret = new HashMap<>();

		List<Operation> operations = new ArrayList<>();
		List<Condition> conditions = new ArrayList<>();
		List<String> columns = new ArrayList<>();
		columns.add("bridge");
		columns.add("_uuid");
		columns.add("btm");
		columns.add("enabled");
		columns.add("ft_psk");
		columns.add("group_rekey");
		columns.add("if_name");
		columns.add("mode");
		columns.add("rrm");
		columns.add("ssid");
		columns.add("ssid_broadcast");
		columns.add("uapsd_enable");
		columns.add("vif_radio_idx");
		columns.add("security");
		columns.add("vlan_id");

		try {
			LOG.debug("Retrieving WifiVifConfig:");

			operations.add(new Select(wifiVifConfigDbTable, conditions, columns));
			CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
			OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

			for (OperationResult res : result) {
				LOG.debug("Op Result {}", res);
			}

			for (Row row : ((SelectResult) result[0]).getRows()) {

				WifiVifConfigInfo wifiVifConfigInfo = new WifiVifConfigInfo();
				wifiVifConfigInfo.bridge = row.getStringColumn("bridge");
				wifiVifConfigInfo.uuid = row.getUuidColumn("_uuid");
				wifiVifConfigInfo.btm = row.getIntegerColumn("btm").intValue();
				wifiVifConfigInfo.enabled = row.getBooleanColumn("enabled");
				wifiVifConfigInfo.ftPsk = row.getIntegerColumn("ft_psk").intValue();
				wifiVifConfigInfo.groupRekey = row.getIntegerColumn("group_rekey").intValue();
				wifiVifConfigInfo.ifName = row.getStringColumn("if_name");
				wifiVifConfigInfo.mode = row.getStringColumn("mode");
				wifiVifConfigInfo.rrm = row.getIntegerColumn("rrm").intValue();
				wifiVifConfigInfo.ssid = row.getStringColumn("ssid");
				wifiVifConfigInfo.ssidBroadcast = row.getStringColumn("ssid_broadcast");
				wifiVifConfigInfo.uapsdEnable = row.getBooleanColumn("uapsd_enable");
				wifiVifConfigInfo.vifRadioIdx = row.getIntegerColumn("vif_radio_idx").intValue();
				wifiVifConfigInfo.security = row.getMapColumn("security");

				if (row.getColumns().get("vlan_id") != null && row.getColumns().get("vlan_id").getClass()
						.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
					wifiVifConfigInfo.vlanId = (row.getIntegerColumn("vlan_id").intValue());
				}
				ret.put(wifiVifConfigInfo.ifName + '_' + wifiVifConfigInfo.ssid, wifiVifConfigInfo);
			}

			LOG.debug("Retrieved WifiVifConfigs: {}", ret);

		} catch (ExecutionException | InterruptedException | OvsdbClientException | TimeoutException e) {
			LOG.error("Error in getProvisionedWifiVifConfigs", e);
			throw new RuntimeException(e);
		}

		return ret;
	}

	public Map<String, WifiInetConfigInfo> getProvisionedWifiInetConfigs(OvsdbClient ovsdbClient) {
		Map<String, WifiInetConfigInfo> ret = new HashMap<>();

		List<Operation> operations = new ArrayList<>();
		List<Condition> conditions = new ArrayList<>();
		List<String> columns = new ArrayList<>();
		columns.add("NAT");
		columns.add("_uuid");
		columns.add("enabled");
		columns.add("if_name");
		columns.add("if_type");
		columns.add("ip_assign_scheme");
		columns.add("network");
		columns.add("vlan_id");

		try {
			LOG.debug("Retrieving WifiInetConfig:");

			operations.add(new Select(wifiInetConfigDbTable, conditions, columns));
			CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
			OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

			for (OperationResult res : result) {
				LOG.debug("Op Result {}", res);
			}

			for (Row row : ((SelectResult) result[0]).getRows()) {

				WifiInetConfigInfo wifiInetConfigInfo = new WifiInetConfigInfo();
				Boolean natTmp = getSingleValueFromSet(row, "NAT");
				wifiInetConfigInfo.nat = natTmp != null ? natTmp : false;

				wifiInetConfigInfo.uuid = row.getUuidColumn("_uuid");
				wifiInetConfigInfo.enabled = row.getBooleanColumn("enabled");
				wifiInetConfigInfo.ifName = row.getStringColumn("if_name");
				wifiInetConfigInfo.ifType = row.getStringColumn("if_type");
				wifiInetConfigInfo.ipAssignScheme = row.getStringColumn("ip_assign_scheme");
				wifiInetConfigInfo.network = row.getBooleanColumn("network");
				if (row.getColumns().get("vlan_id") != null && row.getColumns().get("vlan_id").getClass()
						.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
					wifiInetConfigInfo.vlanId = (row.getIntegerColumn("vlan_id").intValue());
				}
				ret.put(wifiInetConfigInfo.ifName, wifiInetConfigInfo);
			}

			LOG.debug("Retrieved WifiInetConfigs: {}", ret);

		} catch (ExecutionException | InterruptedException | OvsdbClientException | TimeoutException e) {
			LOG.error("Error in getProvisionedWifiInetConfigs", e);
			throw new RuntimeException(e);
		}

		return ret;
	}

	public Map<String, WifiStatsConfigInfo> getProvisionedWifiStatsConfigs(OvsdbClient ovsdbClient) {
		Map<String, WifiStatsConfigInfo> ret = new HashMap<>();

		List<Operation> operations = new ArrayList<>();
		List<Condition> conditions = new ArrayList<>();
		List<String> columns = new ArrayList<>();
		columns.add("channel_list");
		columns.add("radio_type");
		columns.add("reporting_interval");
		columns.add("sampling_interval");
		columns.add("stats_type");
		columns.add("survey_interval_ms");
		columns.add("survey_type");
		columns.add("threshold");
		columns.add("_uuid");

		try {
			LOG.debug("Retrieving WifiStatsConfigs:");

			operations.add(new Select(wifiStatsConfigDbTable, conditions, columns));
			CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
			OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

			for (OperationResult res : result) {
				LOG.debug("Op Result {}", res);
			}

			for (Row row : ((SelectResult) result[0]).getRows()) {

				WifiStatsConfigInfo wifiStatsConfigInfo = new WifiStatsConfigInfo();

				wifiStatsConfigInfo.channelList = row.getSetColumn("channel_list");
				wifiStatsConfigInfo.radioType = row.getStringColumn("radio_type");
				wifiStatsConfigInfo.reportingInterval = row.getIntegerColumn("reporting_interval").intValue();
				wifiStatsConfigInfo.samplingInterval = row.getIntegerColumn("sampling_interval").intValue();
				wifiStatsConfigInfo.statsType = row.getStringColumn("stats_type");
				wifiStatsConfigInfo.surveyType = getSingleValueFromSet(row, "survey_type");
				Long tmp = getSingleValueFromSet(row, "survey_interval_ms");
				wifiStatsConfigInfo.surveyIntervalMs = tmp != null ? tmp.intValue() : 0;
				wifiStatsConfigInfo.threshold = row.getMapColumn("threshold");
				wifiStatsConfigInfo.uuid = row.getUuidColumn("_uuid");

				if (wifiStatsConfigInfo.surveyType == null) {
					ret.put(wifiStatsConfigInfo.radioType + "_" + wifiStatsConfigInfo.statsType, wifiStatsConfigInfo);
				} else {
					ret.put(wifiStatsConfigInfo.radioType + "_" + wifiStatsConfigInfo.statsType + "_"
							+ wifiStatsConfigInfo.surveyType, wifiStatsConfigInfo);
				}
			}

			LOG.debug("Retrieved WifiStatsConfigs: {}", ret);

		} catch (ExecutionException | InterruptedException | OvsdbClientException | TimeoutException e) {
			LOG.error("Error in getProvisionedWifiStatsConfigs", e);

			throw new RuntimeException(e);
		}

		return ret;
	}

	public void provisionSingleBridgePortInterface(OvsdbClient ovsdbClient, String interfaceName, String bridgeName,
			String interfaceType, Map<String, String> interfaceOptions,
			Map<String, InterfaceInfo> provisionedInterfaces, Map<String, PortInfo> provisionedPorts,
			Map<String, BridgeInfo> provisionedBridges)
			throws OvsdbClientException, TimeoutException, ExecutionException, InterruptedException {

		if (!provisionedInterfaces.containsKey(interfaceName)) {
			// Create this interface and link it to the port and the bridge
			List<Operation> operations = new ArrayList<>();
			Map<String, Value> updateColumns = new HashMap<>();
			List<Condition> conditions = new ArrayList<>();

			updateColumns.put("name", new Atom<>(interfaceName));
			if (interfaceType != null) {
				updateColumns.put("type", new Atom<>(interfaceType));
			}
			// updateColumns.put("admin_state", new Atom<String>("up") );
			// updateColumns.put("link_state", new Atom<String>("up") );
			// updateColumns.put("ifindex", new Atom<Integer>(ifIndex) );
			// updateColumns.put("mtu", new Atom<Integer>(1500) );
			// updateColumns.put("ofport", new Atom<Integer>(ofport) );

			if (interfaceOptions != null) {
				@SuppressWarnings("unchecked")
				com.vmware.ovsdb.protocol.operation.notation.Map<String, String> ifOptions = com.vmware.ovsdb.protocol.operation.notation.Map
						.of(interfaceOptions);
				updateColumns.put("options", ifOptions);
			}

			Uuid interfaceUuid = null;

			Row row = new Row(updateColumns);
			operations.add(new Insert(interfaceDbTable, row));

			{
				CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
				OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

				LOG.debug("Provisioned Interface for {}", interfaceName);

				for (OperationResult res : result) {
					LOG.debug("Op Result {}", res);
					if (res instanceof InsertResult) {
						interfaceUuid = ((InsertResult) res).getUuid();
					}
				}
			}

			if (interfaceUuid == null) {
				throw new IllegalStateException("Interface entry was not created successfully");
			}

			Uuid portUuid = null;
			operations = new ArrayList<>();
			// link the interface to the port, create port if necessary
			if (!provisionedPorts.containsKey(interfaceName)) {
				// need to create port
				updateColumns = new HashMap<>();

				// portUuid = new Uuid(new UUID(System.currentTimeMillis(),
				// System.nanoTime())) ;
				updateColumns.put("name", new Atom<String>(interfaceName));
				// updateColumns.put("_uuid", new Atom<Uuid>(portUuid));

				Set<Uuid> portInterfacesSet = new HashSet<>();
				portInterfacesSet.add(interfaceUuid);
				com.vmware.ovsdb.protocol.operation.notation.Set portInterfaces = com.vmware.ovsdb.protocol.operation.notation.Set
						.of(portInterfacesSet);
				updateColumns.put("interfaces", portInterfaces);

				row = new Row(updateColumns);
				operations.add(new Insert(portDbTable, row));

				{
					CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
					OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

					LOG.debug("Provisioned Port for {}", interfaceName);

					for (OperationResult res : result) {
						LOG.debug("Op Result {}", res);
						if (res instanceof InsertResult) {
							portUuid = ((InsertResult) res).getUuid();
						}
					}
				}

			} else {
				// need to update port
				PortInfo existingPort = provisionedPorts.get(interfaceName);
				portUuid = existingPort.uuid;

				conditions = new ArrayList<>();
				updateColumns = new HashMap<>();

				conditions.add(new Condition("name", Function.EQUALS, new Atom<>(interfaceName)));

				Set<Uuid> portInterfacesSet = new HashSet<>();
				if (existingPort.interfaceUuids != null) {
					portInterfacesSet.addAll(existingPort.interfaceUuids);
				}
				portInterfacesSet.add(interfaceUuid);
				com.vmware.ovsdb.protocol.operation.notation.Set portInterfaces = com.vmware.ovsdb.protocol.operation.notation.Set
						.of(portInterfacesSet);
				updateColumns.put("interfaces", portInterfaces);

				row = new Row(updateColumns);
				operations.add(new Update(portDbTable, row));

				{
					CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
					OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

					LOG.debug("Updated Port for {}", interfaceName);

					for (OperationResult res : result) {
						LOG.debug("Op Result {}", res);
					}
				}

			}

			if (portUuid == null) {
				throw new IllegalStateException("Port entry was not created successfully");
			}

			operations = new ArrayList<>();

			// link the port to the bridge
			if (provisionedBridges.containsKey(bridgeName)) {
				BridgeInfo existingBridge = provisionedBridges.get(bridgeName);

				conditions = new ArrayList<>();
				updateColumns = new HashMap<>();

				conditions.add(new Condition("name", Function.EQUALS, new Atom<>(bridgeName)));

				Set<Uuid> bridgePortsSet = new HashSet<>();
				if (existingBridge.portUuids != null) {
					bridgePortsSet.addAll(existingBridge.portUuids);
				}

				bridgePortsSet.add(portUuid);
				com.vmware.ovsdb.protocol.operation.notation.Set bridgePorts = com.vmware.ovsdb.protocol.operation.notation.Set
						.of(bridgePortsSet);
				updateColumns.put("ports", bridgePorts);

				row = new Row(updateColumns);
				operations.add(new Update(bridgeDbTable, row));

			} else {
				LOG.warn("provisionedBridges does not have bridge {} - {} - port will be dangling", bridgeName,
						provisionedBridges.keySet());
			}

			CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
			OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

			if (LOG.isDebugEnabled()) {
				LOG.debug("Finished provisioning Interface/port/bridge for {} / {}", interfaceName, bridgeName);

				for (OperationResult res : result) {
					LOG.debug("Op Result {}", res);
				}
			}

		}
	}

	public static final String homeAp24 = "home-ap-24";
	public static final String homeApL50 = "home-ap-l50";
	public static final String homeApU50 = "home-ap-u50";

	public static final String brHome = "br-home";
	public static final String brWan = "br-wan";
	public static final String brLan = "br-lan";

	public static final String patchW2h = "patch-w2h";
	public static final String patchH2w = "patch-h2w";

	public void provisionBridgePortInterface(OvsdbClient ovsdbClient) {
		try {
			Map<String, InterfaceInfo> provisionedInterfaces = getProvisionedInterfaces(ovsdbClient);
			LOG.debug("Existing Interfaces: {}", provisionedInterfaces.keySet());

			Map<String, PortInfo> provisionedPorts = getProvisionedPorts(ovsdbClient);
			LOG.debug("Existing Ports: {}", provisionedPorts.keySet());

			Map<String, BridgeInfo> provisionedBridges = getProvisionedBridges(ovsdbClient);
			LOG.debug("Existing Bridges: {}", provisionedBridges.keySet());

			Map<String, String> patchH2wOptions = new HashMap<>();
			patchH2wOptions.put("peer", "patch-w2h");

			Map<String, String> patchW2hOptions = new HashMap<>();
			patchH2wOptions.put("peer", "patch-h2w");

			provisionSingleBridgePortInterface(ovsdbClient, patchH2w, brHome, "patch", patchH2wOptions,
					provisionedInterfaces, provisionedPorts, provisionedBridges);
			provisionSingleBridgePortInterface(ovsdbClient, patchW2h, brWan, "patch", patchW2hOptions,
					provisionedInterfaces, provisionedPorts, provisionedBridges);

			provisionSingleBridgePortInterface(ovsdbClient, homeApU50, brHome, null, null, provisionedInterfaces,
					provisionedPorts, provisionedBridges);

			provisionSingleBridgePortInterface(ovsdbClient, homeApL50, brHome, null, null, provisionedInterfaces,
					provisionedPorts, provisionedBridges);

			provisionSingleBridgePortInterface(ovsdbClient, homeAp24, brHome, null, null, provisionedInterfaces,
					provisionedPorts, provisionedBridges);

		} catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
			LOG.error("Error in provisionBridgePortInterface", e);
			throw new RuntimeException(e);
		}

	}

	public void removeOnboardingSsids(OvsdbClient ovsdbClient) {
		try {
			List<Operation> operations = new ArrayList<>();
			List<Condition> conditions = new ArrayList<>();
			conditions.add(new Condition("ssid", Function.EQUALS, new Atom<>("opensync.onboard")));

			operations.add(new Delete(wifiVifConfigDbTable, conditions));

			CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
			OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

			if (LOG.isDebugEnabled()) {
				LOG.debug("Removed onboarding SSIDs from {}:", wifiVifConfigDbTable);

				for (OperationResult res : result) {
					LOG.debug("Op Result {}", res);
				}
			}

		} catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
			LOG.error("Error in removeOnboardingSsids", e);
			throw new RuntimeException(e);
		}

	}

	public void removeAllSsids(OvsdbClient ovsdbClient) {
		try {
			List<Operation> operations = new ArrayList<>();

			operations.add(new Delete(wifiVifConfigDbTable));

			CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
			OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

			if (LOG.isDebugEnabled()) {
				LOG.debug("Removed all existing SSIDs from {}:", wifiVifConfigDbTable);

				for (OperationResult res : result) {
					LOG.debug("Op Result {}", res);
				}
			}

			// Now clean up references in the vif_configs columns
			operations = new ArrayList<>();
			Map<String, Value> updateColumns = new HashMap<>();
			Set<Uuid> vifConfigsSet = new HashSet<>();
			com.vmware.ovsdb.protocol.operation.notation.Set vifConfigs = com.vmware.ovsdb.protocol.operation.notation.Set
					.of(vifConfigsSet);
			updateColumns.put("vif_configs", vifConfigs);

			Row row = new Row(updateColumns);
			operations.add(new Update(wifiRadioConfigDbTable, row));

			fResult = ovsdbClient.transact(ovsdbName, operations);
			result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

			if (LOG.isDebugEnabled()) {
				LOG.debug("Updated WifiRadioConfig ");

				for (OperationResult res : result) {
					LOG.debug("Op Result {}", res);
				}
			}

			LOG.info("Removed all ssids");

		} catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
			LOG.error("Error in removeAllSsids", e);
			throw new RuntimeException(e);
		}

	}

	public void configureWifiRadios(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncAPConfig) {

		Map<String, WifiRadioConfigInfo> provisionedWifiRadios = getProvisionedWifiRadioConfigs(ovsdbClient);
		LOG.debug("Existing WifiRadioConfigs: {}", provisionedWifiRadios.keySet());

		String country = opensyncAPConfig.getCountryCode(); // should be the same for all radios on this AP ;-)

		ApElementConfiguration apElementConfiguration = ((ApElementConfiguration) opensyncAPConfig
				.getCustomerEquipment().getDetails());

		for (RadioType radioType : apElementConfiguration.getRadioMap().keySet()) {
			Map<String, String> hwConfig = new HashMap<>();

			ElementRadioConfiguration elementRadioConfig = apElementConfiguration.getRadioMap().get(radioType);
			int channel = elementRadioConfig.getChannelNumber();
			ChannelBandwidth bandwidth = elementRadioConfig.getChannelBandwidth();
			String ht_mode = null;
			switch (bandwidth) {
			case is20MHz:
				ht_mode = "HT20";
				break;
			case is40MHz:
				ht_mode = "HT40";
				break;
			case is80MHz:
				ht_mode = "HT80";
				break;
			case is160MHz:
				ht_mode = "HT160";
				break;
			default:
				ht_mode = "HT20";
			}
			elementRadioConfig.getAutoChannelSelection();

			RadioConfiguration radioConfig = apElementConfiguration.getAdvancedRadioMap().get(radioType);
			int beaconInterval = radioConfig.getBeaconInterval();
			boolean enabled = radioConfig.getRadioAdminState().equals(StateSetting.enabled);

			int txPower = 0;
			if (!elementRadioConfig.getEirpTxPower().isAuto())
				txPower = elementRadioConfig.getEirpTxPower().getValue();
			String configName = null;
			switch (radioType) {
			case is2dot4GHz:
				configName = "wifi0";
				break;
			case is5GHz:
				hwConfig.put("dfs_enable", "1");
				hwConfig.put("dfs_ignorecac", "0");
				hwConfig.put("dfs_usenol", "1");
				configName = "wifi1";
				break;

			case is5GHzL:
				hwConfig.put("dfs_enable", "1");
				hwConfig.put("dfs_ignorecac", "0");
				hwConfig.put("dfs_usenol", "1");
				configName = "wifi1";
				break;

			case is5GHzU:
				hwConfig.put("dfs_enable", "1");
				hwConfig.put("dfs_ignorecac", "0");
				hwConfig.put("dfs_usenol", "1");
				configName = "wifi2";
				break;
			default: // don't know this interface
				continue;

			}

			if (configName != null) {
				try {
					configureWifiRadios(ovsdbClient, configName, provisionedWifiRadios, channel, hwConfig, country,
							beaconInterval, enabled, ht_mode, txPower);
				} catch (OvsdbClientException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (TimeoutException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		}

	}

	public List<OpensyncAPRadioState> getOpensyncAPRadioState(TableUpdates tableUpdates, String apId,
			OvsdbClient ovsdbClient) {

		List<OpensyncAPRadioState> ret = new ArrayList<OpensyncAPRadioState>();

		try {

			for (Entry<String, TableUpdate> tableUpdate : tableUpdates.getTableUpdates().entrySet()) {

				for (Entry<UUID, RowUpdate> rowUpdate : tableUpdate.getValue().getRowUpdates().entrySet()) {

					Row row = rowUpdate.getValue().getNew();
					// Row old = rowUpdate.getOld();

					if (row != null) {

						OpensyncAPRadioState tableState = new OpensyncAPRadioState();

						Map<String, Value> map = row.getColumns();

						if (map.get("mac") != null && map.get("mac").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setMac(row.getStringColumn("mac"));
						}
						if (map.get("channel") != null && map.get("channel").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setChannel(row.getIntegerColumn("channel").intValue());
						}
						if (map.get("freq_band") != null && map.get("freq_band").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setFreqBand(row.getStringColumn("freq_band"));
						}
						if (map.get("if_name") != null && map.get("if_name").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setIfName(row.getStringColumn("if_name"));
						}
						if (map.get("channel_mode") != null && map.get("channel_mode").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setChannelMode(row.getStringColumn("channel_mode"));
						}
						if (map.get("country") != null && map.get("country").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setCountry(row.getStringColumn("country"));
						}
						if (map.get("enabled") != null && map.get("enabled").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setEnabled(row.getBooleanColumn("enabled"));
						}
						if (map.get("ht_mode") != null && map.get("ht_mode").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setHtMode(row.getStringColumn("ht_mode"));
						}
						if (map.get("tx_power") != null && map.get("tx_power").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setTxPower(row.getIntegerColumn("tx_power").intValue());
						}
						if (map.get("hw_config") != null && map.get("hw_config").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Map.class)) {
							tableState.setHwConfig(row.getMapColumn("hw_config"));
						}
						if (map.get("_version") != null && map.get("_version").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setVersion(row.getUuidColumn("_version"));
						}
						if (map.get("_uuid") != null && map.get("_uuid").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setVersion(row.getUuidColumn("_uuid"));
						}

						ret.add(tableState);
					}
				}
			}

			ret.stream().forEach(wrs -> {
				LOG.debug("Wifi_Radio_State row {}", wrs.toPrettyString());
			});

		} catch (Exception e) {
			LOG.error("Could not parse update for Wifi_Radio_State", e);
		}

		return ret;
	}

	public List<OpensyncAPInetState> getOpensyncAPInetState(TableUpdates tableUpdates, String apId,
			OvsdbClient ovsdbClient) {
		List<OpensyncAPInetState> ret = new ArrayList<OpensyncAPInetState>();

		try {

			for (TableUpdate tableUpdate : tableUpdates.getTableUpdates().values()) {

				for (RowUpdate rowUpdate : tableUpdate.getRowUpdates().values()) {

					Row row = rowUpdate.getNew();
					// Row old = rowUpdate.getOld();

					if (row != null) {

						OpensyncAPInetState tableState = new OpensyncAPInetState();
						Map<String, Value> map = row.getColumns();

						if (map.get("NAT") != null && map.get("NAT").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setNat(row.getBooleanColumn("NAT"));
						}
						if (map.get("enabled") != null && map.get("enabled").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setEnabled(row.getBooleanColumn("enabled"));
						}
						if (map.get("if_name") != null && map.get("if_name").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setIfName(row.getStringColumn("if_name"));
						}
						if (map.get("if_type") != null && map.get("if_type").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setIfType(row.getStringColumn("if_type"));
						}
						if (map.get("ip_assign_scheme") != null && map.get("ip_assign_scheme").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setIpAssignScheme(row.getStringColumn("ip_assign_scheme"));
						}
						if (map.get("network") != null && map.get("network").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setNetwork(row.getBooleanColumn("network"));
						}
						if (map.get("hwaddr") != null && map.get("hwaddr").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setHwAddr(row.getStringColumn("hwaddr"));
						}
						if (map.get("_version") != null && map.get("_version").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setVersion(row.getUuidColumn("_version"));
						}
						if (map.get("_uuid") != null && map.get("_uuid").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setVersion(row.getUuidColumn("_uuid"));
						}
						ret.add(tableState);
					}

				}
			}

			ret.stream().forEach(wrs -> {
				LOG.debug("Wifi_Inet_State row {}", wrs.toPrettyString());
			});

		} catch (Exception e) {
			LOG.error("Could not parse update for Wifi_Inet_State", e);
		}
		return ret;
	}

	public List<OpensyncAPVIFState> getOpensyncAPVIFState(TableUpdates tableUpdates, String apId,
			OvsdbClient ovsdbClient) {
		List<OpensyncAPVIFState> ret = new ArrayList<OpensyncAPVIFState>();
		try {

			for (TableUpdate tableUpdate : tableUpdates.getTableUpdates().values()) {

				for (RowUpdate rowUpdate : tableUpdate.getRowUpdates().values()) {

					Row row = rowUpdate.getNew();
					// Row old = rowUpdate.getOld();

					if (row != null) {

						OpensyncAPVIFState tableState = new OpensyncAPVIFState();

						Map<String, Value> map = row.getColumns();

						if (map.get("mac") != null && map.get("mac").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setMac(row.getStringColumn("mac"));
						}
						if (map.get("bridge") != null && map.get("bridge").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setBridge(row.getStringColumn("bridge"));
						}
						if (map.get("btm") != null && map.get("btm").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setBtm(row.getIntegerColumn("btm").intValue());
						}

						if (map.get("channel") != null && map.get("channel").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setChannel(row.getIntegerColumn("channel").intValue());
						}

						if (map.get("enabled") != null && map.get("enabled").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setEnabled(row.getBooleanColumn("enabled"));
						}

						if (map.get("group_rekey") != null && map.get("group_rekey").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setGroupRekey(row.getIntegerColumn("group_rekey").intValue());
						}
						if (map.get("if_name") != null && map.get("if_name").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setIfName(row.getStringColumn("if_name"));
						}

						if (map.get("mode") != null && map.get("mode").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setMode(row.getStringColumn("mode"));
						}

						if (map.get("rrm") != null && map.get("rrm").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setRrm(row.getIntegerColumn("rrm").intValue());
						}
						if (map.get("ssid") != null && map.get("ssid").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setSsid(row.getStringColumn("ssid"));
						}

						if (map.get("ssid_broadcast") != null && map.get("ssid_broadcast").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setSsidBroadcast(row.getStringColumn("ssid_broadcast"));
						}
						if (map.get("uapsd_enable") != null && map.get("uapsd_enable").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setUapsdEnable(row.getBooleanColumn("uapsd_enable"));
						}
						if (map.get("vif_radio_idx") != null && map.get("vif_radio_idx").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setVifRadioIdx(row.getIntegerColumn("vif_radio_idx").intValue());
						}

						if (map.get("associated_clients") != null)
							tableState.setAssociatedClients(row.getSetColumn("associated_clients"));

						if (map.get("security") != null)
							tableState.setSecurity(row.getMapColumn("security"));

						if (map.get("_version") != null && map.get("_version").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setVersion(row.getUuidColumn("_version"));
						}
						if (map.get("_uuid") != null && map.get("_uuid").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setVersion(row.getUuidColumn("_uuid"));
						}

						ret.add(tableState);

					}

				}
			}

			ret.stream().forEach(wrs -> {
				LOG.debug("Wifi_VIF_State row {}", wrs.toPrettyString());
			});

		} catch (Exception e) {
			LOG.error("Could not parse update for Wifi_VIF_State", e);

		}
		return ret;
	}

	public List<OpensyncWifiAssociatedClients> getOpensyncWifiAssociatedClients(TableUpdates tableUpdates, String apId,
			OvsdbClient ovsdbClient) {
		List<OpensyncWifiAssociatedClients> ret = new ArrayList<OpensyncWifiAssociatedClients>();

		try {

			for (TableUpdate tableUpdate : tableUpdates.getTableUpdates().values()) {

				for (RowUpdate rowUpdate : tableUpdate.getRowUpdates().values()) {

					Row row = rowUpdate.getNew();

					if (row != null) {

						OpensyncWifiAssociatedClients tableState = new OpensyncWifiAssociatedClients();
						Map<String, Value> map = row.getColumns();

						if (map.get("mac") != null && map.get("mac").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setMac(row.getStringColumn("mac"));
						}
						if (row.getSetColumn("capabilities") != null)
							tableState.setCapabilities(row.getSetColumn("capabilities"));
						if (map.get("state") != null && map.get("state").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setState(row.getStringColumn("state"));
						}
						if (map.get("_version") != null && map.get("_version").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setVersion(row.getUuidColumn("_version"));
						}
						if (map.get("_uuid") != null && map.get("_uuid").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setVersion(row.getUuidColumn("_uuid"));
						}

						ret.add(tableState);

					}
				}
			}
			ret.stream().forEach(wrs -> {
				LOG.debug("Wifi_Associated_Clients row {}", wrs.toPrettyString());
			});
		} catch (Exception e) {
			LOG.error("Could not get Wifi_Associated_Clients list from table update", e);
		}

		return ret;
	}

	public OpensyncAWLANNode getOpensyncAWLANNode(TableUpdates tableUpdates, String apId, OvsdbClient ovsdbClient) {
		OpensyncAWLANNode tableState = new OpensyncAWLANNode();

		try {

			for (TableUpdate tableUpdate : tableUpdates.getTableUpdates().values()) {

				for (RowUpdate rowUpdate : tableUpdate.getRowUpdates().values()) {

					Row row = rowUpdate.getNew();

					if (row != null) {

						Map<String, Value> map = row.getColumns();

						if (map.get("mqtt_settings") != null) {
							tableState.setMqttSettings(row.getMapColumn("mqtt_settings"));
						}
						if (map.get("mqtt_headers") != null) {
							tableState.setMqttHeaders(row.getMapColumn("mqtt_headers"));
						}
						if (map.get("mqtt_topics") != null) {
							tableState.setMqttHeaders(row.getMapColumn("mqtt_topics"));
						}

						if (map.get("model") != null && map.get("model").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setModel(row.getStringColumn("model"));
						}
						if (map.get("sku_number") != null && map.get("sku_number").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setSkuNumber(row.getStringColumn("sku_number"));
						}
						if (map.get("id") != null && map.get("id").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setId(row.getStringColumn("id"));
						}

						if (map.get("version_matrix") != null)
							tableState.setVersionMatrix(row.getMapColumn("version_matrix"));
						if (map.get("firmware_version") != null && map.get("firmware_version").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setFirmwareVersion(row.getStringColumn("firmware_version"));
						}
						if (map.get("firmware_url") != null && map.get("firmware_url").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setFirmwareUrl(row.getStringColumn("firmware_url"));
						}

						if (map.get("_uuid") != null && map.get("_uuid").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setVersion(row.getUuidColumn("_uuid"));
						}
						if (map.get("upgrade_dl_timer") != null && map.get("upgrade_dl_timer").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setUpgradeDlTimer(row.getIntegerColumn("upgrade_dl_timer").intValue());
						}
						if (map.get("platform_version") != null && map.get("platform_version").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setPlatformVersion(row.getStringColumn("platform_version"));
						}
						if (map.get("firmware_pass") != null && map.get("firmware_pass").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setFirmwarePass(row.getStringColumn("firmware_pass"));
						}
						if (map.get("upgrade_timer") != null && map.get("upgrade_timer").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setUpgradeTimer(row.getIntegerColumn("upgrade_timer").intValue());
						}
						if (map.get("max_backoff") != null && map.get("max_backoff").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setMaxBackoff(row.getIntegerColumn("max_backoff").intValue());
						}
						if (map.get("led_config") != null)
							tableState.setLedConfig(row.getMapColumn("led_config"));
						if (map.get("redirector_addr") != null && map.get("redirector_addr").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setRedirectorAddr(row.getStringColumn("redirector_addr"));
						}
						if (map.get("serial_number") != null && map.get("serial_number").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setSerialNumber(row.getStringColumn("serial_number"));
						}
						if (map.get("_version") != null && map.get("_version").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setVersion(row.getUuidColumn("_version"));
						}
						if (map.get("upgrade_status") != null && map.get("upgrade_status").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setUpgradeTimer(row.getIntegerColumn("upgrade_status").intValue());
						}
						if (map.get("device_mode") != null && map.get("device_mode").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setDeviceMode(row.getStringColumn("device_mode"));
						}
						if (map.get("min_backoff") != null && map.get("min_backoff").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setMinBackoff(row.getIntegerColumn("min_backoff").intValue());
						}

						if (map.get("revision") != null && map.get("revision").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setRevision(row.getStringColumn("revision"));
						}
						if (map.get("manager_addr") != null && map.get("manager_addr").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setManagerAddr(row.getStringColumn("manager_addr"));
						}
						if (map.get("factory_reset") != null && map.get("factory_reset").getClass()
								.equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
							tableState.setFactoryReset(row.getBooleanColumn("factory_reset"));
						}
					}

				}

			}
		} catch (Exception e) {
			LOG.error("Failed to handle AWLAN_Node update", e);
		}

		return tableState;

	}

	public void configureWifiRadios(OvsdbClient ovsdbClient, String configName,
			Map<String, WifiRadioConfigInfo> provisionedWifiRadios, int channel, Map<String, String> hwConfig,
			String country, int beaconInterval, boolean enabled, String ht_mode, int txPower)
			throws OvsdbClientException, TimeoutException, ExecutionException, InterruptedException {

		WifiRadioConfigInfo existingConfig = provisionedWifiRadios.get(configName);

		if (existingConfig == null) {
			LOG.warn("There is no WifiRadioConfig {}", configName);
			return;
		}

		List<Operation> operations = new ArrayList<>();
		Map<String, Value> updateColumns = new HashMap<>();
		List<Condition> conditions = new ArrayList<>();
		conditions.add(new Condition("if_name", Function.EQUALS, new Atom<>(configName)));

		updateColumns.put("channel", new Atom<Integer>(channel));
		updateColumns.put("country", new Atom<>(country));
		@SuppressWarnings("unchecked")
		com.vmware.ovsdb.protocol.operation.notation.Map<String, String> hwConfigMap = com.vmware.ovsdb.protocol.operation.notation.Map
				.of(hwConfig);
		updateColumns.put("hw_config", hwConfigMap);
		updateColumns.put("bcn_int", new Atom<Integer>(beaconInterval));
		updateColumns.put("enabled", new Atom<Boolean>(enabled));
		updateColumns.put("ht_mode", new Atom<>(ht_mode));
		if (txPower > 0)
			updateColumns.put("tx_power", new Atom<Integer>(txPower));
		else
			updateColumns.put("tx_power", new com.vmware.ovsdb.protocol.operation.notation.Set());

		Row row = new Row(updateColumns);
		operations.add(new Update(wifiRadioConfigDbTable, conditions, row));

		CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
		OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

		LOG.debug("Provisioned channel {} for {}", channel, configName);

		for (OperationResult res : result) {
			LOG.debug("Op Result {}", res);
		}
	}

	public void configureSingleSsid(OvsdbClient ovsdbClient, String bridge, String ifName, String ssid,
			boolean ssidBroadcast, Map<String, String> security,
			Map<String, WifiRadioConfigInfo> provisionedWifiRadioConfigs, String radioIfName, int vlanId,
			int vifRadioIdx, boolean rrmEnabled, String minHwMode, boolean enabled, int keyRefresh, boolean uapsdEnabled, boolean apBridge) {

		List<Operation> operations = new ArrayList<>();
		Map<String, Value> updateColumns = new HashMap<>();

		try {
			updateColumns.put("bridge", new Atom<>(bridge));
			updateColumns.put("btm", new Atom<>(1));
			updateColumns.put("enabled", new Atom<>(enabled));
			updateColumns.put("ft_psk", new Atom<>(0));
			updateColumns.put("if_name", new Atom<>(ifName));
			updateColumns.put("mode", new Atom<>("ap"));
			updateColumns.put("rrm", new Atom<>(rrmEnabled ? 1 : 0));
			updateColumns.put("ssid", new Atom<>(ssid));
			updateColumns.put("ssid_broadcast", new Atom<>(ssidBroadcast ? "enabled" : "disabled"));
			updateColumns.put("uapsd_enable", new Atom<>(true));
			updateColumns.put("vif_radio_idx", new Atom<Integer>(vifRadioIdx));
			updateColumns.put("min_hw_mode", new Atom<>(minHwMode));
			updateColumns.put("vlan_id", new Atom<Integer>(vlanId));
			updateColumns.put("group_rekey", new Atom<Integer>(keyRefresh));
			updateColumns.put("uapsd_enable", new Atom<Boolean>(uapsdEnabled));
			updateColumns.put("ap_bridge", new Atom<Boolean>(apBridge));

			@SuppressWarnings("unchecked")
			com.vmware.ovsdb.protocol.operation.notation.Map<String, String> securityMap = com.vmware.ovsdb.protocol.operation.notation.Map
					.of(security);
			updateColumns.put("security", securityMap);

			Row row = new Row(updateColumns);
			operations.add(new Insert(wifiVifConfigDbTable, row));

			CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
			OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

			LOG.debug("Provisioned SSID {} on {}", ssid, ifName);

			Uuid vifConfigUuid = null;
			for (OperationResult res : result) {
				LOG.debug("Op Result {}", res);
				if (res instanceof InsertResult) {
					vifConfigUuid = ((InsertResult) res).getUuid();
				}
			}

			if (vifConfigUuid == null) {
				throw new IllegalStateException("Wifi_VIF_Config entry was not created successfully");
			}

			// update Wifi_Radio_Config here - add vifConfigUuid
			/// usr/plume/tools/ovsh u Wifi_Radio_Config
			// vif_configs:='["set",[["uuid","98e42897-b567-4186-84a6-4a4e38a51e9d"],["uuid","4314920e-c4e6-42a6-93e3-261142ed9adf"]]]'
			// --where if_name==wifi0
			updateColumns.clear();
			operations.clear();

			WifiRadioConfigInfo wifiRadioConfigInfo = provisionedWifiRadioConfigs.get(radioIfName);
			if (wifiRadioConfigInfo == null) {
				throw new IllegalStateException("missing Wifi_Radio_Config entry " + radioIfName);
			}

			Set<Uuid> vifConfigsSet = new HashSet<>(wifiRadioConfigInfo.vifConfigUuids);
			vifConfigsSet.add(vifConfigUuid);
			com.vmware.ovsdb.protocol.operation.notation.Set vifConfigs = com.vmware.ovsdb.protocol.operation.notation.Set
					.of(vifConfigsSet);
			updateColumns.put("vif_configs", vifConfigs);

			List<Condition> conditions = new ArrayList<>();
			conditions.add(new Condition("if_name", Function.EQUALS, new Atom<>(radioIfName)));

			row = new Row(updateColumns);
			operations.add(new Update(wifiRadioConfigDbTable, conditions, row));

			fResult = ovsdbClient.transact(ovsdbName, operations);
			result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

			if (LOG.isDebugEnabled()) {
				LOG.debug("Updated WifiRadioConfig {} for SSID {}:", radioIfName, ssid);

				for (OperationResult res : result) {
					LOG.debug("Op Result {}", res);
				}
			}
			Map<String, WifiInetConfigInfo> inetConfigs = getProvisionedWifiInetConfigs(ovsdbClient);
			if (inetConfigs.containsKey(ifName))
				updateWifiInetConfig(ovsdbClient, vlanId, ifName, enabled);
			else
				insertWifiInetConfigForVif(ovsdbClient, vlanId, ifName, enabled);

			LOG.info("Provisioned SSID {} on interface {} / {}", ssid, ifName, radioIfName);

		} catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
			LOG.error("Error in configureSingleSsid", e);
			throw new RuntimeException(e);
		}
	}

	public void configureSsids(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig) {

		boolean rrmEnabled = false;
		if (opensyncApConfig.getEquipmentLocation() != null
				&& opensyncApConfig.getEquipmentLocation().getDetails() != null) {
			rrmEnabled = opensyncApConfig.getEquipmentLocation().getDetails().isRrmEnabled();
		}

		for (Profile ssidProfile : opensyncApConfig.getSsidProfile()) {

			Map<String, WifiVifConfigInfo> provisionedWifiVifConfigs = getProvisionedWifiVifConfigs(ovsdbClient);
			LOG.debug("Existing WifiVifConfigs: {}", provisionedWifiVifConfigs.keySet());

			SsidConfiguration ssidConfig = (SsidConfiguration) ssidProfile.getDetails();

			for (RadioType radioType : ssidConfig.getAppliedRadios()) {
				
				int keyRefresh = ssidConfig.getKeyRefresh();
				

				Map<String, WifiRadioConfigInfo> provisionedWifiRadioConfigs = getProvisionedWifiRadioConfigs(
						ovsdbClient);

				boolean ssidBroadcast = ssidConfig.getBroadcastSsid() == StateSetting.enabled;
				Map<String, String> security = new HashMap<>();
				String ssidSecurityMode = ssidConfig.getSecureMode().name();
				String opensyncSecurityMode = "OPEN";

				RadioConfiguration radioConfiguration =  ((ApElementConfiguration) opensyncApConfig.getCustomerEquipment().getDetails())
						.getAdvancedRadioMap().get(radioType);
				if (radioConfiguration == null) continue; // don't have a radio of this kind in the map
				RadioMode radioMode = radioConfiguration.getRadioMode();
				
				boolean uapsdEnabled = (radioConfiguration.getUapsdState() == StateSetting.enabled);
				
				boolean apBridge = (radioConfiguration.getStationIsolation() == StateSetting.enabled);  //stationIsolation

				String minHwMode = "11n"; // min_hw_mode is 11ac, wifi 5, we can also take ++ (11ax) but 2.4GHz only
											// Wifi4 --
				if (!radioType.equals(RadioType.is2dot4GHz))
					minHwMode = "11ac";
				if (!radioType.equals(RadioType.is2dot4GHz) && radioMode.equals(RadioMode.modeX))
					minHwMode = "11x";

				if (ssidSecurityMode.equalsIgnoreCase("wpaPSK") || ssidSecurityMode.equalsIgnoreCase("wpa2PSK"))
					opensyncSecurityMode = "WPA-PSK";
				else if (ssidSecurityMode.equalsIgnoreCase("wep"))
					opensyncSecurityMode = "WEP";

				security.put("encryption", opensyncSecurityMode);
				security.put("key", ssidConfig.getKeyStr());
				security.put("mode", Long.toString(ssidConfig.getSecureMode().getId()));
				String bridge = brHome;
				boolean enabled = ssidConfig.getSsidAdminState().equals(StateSetting.enabled);

				String ifName = null;
				String radioIfName = null;
				int vifRadioIdx = -1;

				if (radioType == RadioType.is2dot4GHz) {
					ifName = homeAp24;
					radioIfName = "wifi0";
					vifRadioIdx = 1;
				} else if (radioType == RadioType.is5GHzL) {
					ifName = homeApL50;
					radioIfName = "wifi1";
					vifRadioIdx = 2;
				} else if (radioType == RadioType.is5GHzU) {
					ifName = homeApU50;
					radioIfName = "wifi2";
					vifRadioIdx = 3;
				}

				if (vifRadioIdx == -1) {
					LOG.debug("Cannot determine vif radio idx radioType {} skipping", radioType);
					continue;
				}

				if (!provisionedWifiVifConfigs.containsKey(ifName + "_" + ssidConfig.getSsid())) {
					try {
						configureSingleSsid(ovsdbClient, bridge, ifName, ssidConfig.getSsid(), ssidBroadcast, security,
								provisionedWifiRadioConfigs, radioIfName, ssidConfig.getVlanId(), vifRadioIdx,
								rrmEnabled, minHwMode, enabled, keyRefresh, uapsdEnabled, apBridge);
						
					} catch (IllegalStateException e) {
						// could not provision this SSID, but still can go on
						LOG.warn("could not provision SSID {} on {}", ssidConfig.getSsid(), radioIfName);
					}
				}

			}
		}

	}

	private void insertWifiInetConfigForVif(OvsdbClient ovsdbClient, int vlanId, String ifName, boolean enabled) {

		List<Operation> operations = new ArrayList<>();
		Map<String, Value> updateColumns = new HashMap<>();
	
		try {
			/// usr/plume/tools/ovsh i Wifi_Inet_Config NAT:=false enabled:=true
			/// if_name:=home-ap-24 if_type:=vif ip_assign_scheme:=none
			/// network:=true

			updateColumns.put("NAT", new Atom<>(false));
			updateColumns.put("enabled", new Atom<Boolean>(enabled));
			updateColumns.put("if_name", new Atom<String>(ifName));
			updateColumns.put("if_type", new Atom<>("vif"));
			updateColumns.put("ip_assign_scheme", new Atom<>("none"));
			updateColumns.put("network", new Atom<>(true));
			updateColumns.put("vlan_id", new Atom<Integer>(vlanId));

			Row row = new Row(updateColumns);
			operations.add(new Insert(wifiInetConfigDbTable, row));
		
			CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
			OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

			LOG.debug("Provisioned WifiInetConfig {}", ifName);

			for (OperationResult res : result) {
				LOG.debug("Op Result {}", res);
			}

		} catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
			LOG.error("Error in configureWifiInet", e);
			throw new RuntimeException(e);
		}

	}
	
	private void updateWifiInetConfig(OvsdbClient ovsdbClient, int vlanId, String ifName, boolean enabled
			) {

		List<Operation> operations = new ArrayList<>();
		Map<String, Value> updateColumns = new HashMap<>();
		List<Condition> conditions = new ArrayList<>();
		conditions.add(new Condition("if_name", Function.EQUALS, new Atom<>(ifName)));

		try {
			/// usr/plume/tools/ovsh i Wifi_Inet_Config NAT:=false enabled:=true
			/// if_name:=home-ap-24 if_type:=vif ip_assign_scheme:=none
			/// network:=true

			updateColumns.put("NAT", new Atom<>(false));
			updateColumns.put("enabled", new Atom<Boolean>(enabled));
			updateColumns.put("if_type", new Atom<>("vif"));
			updateColumns.put("ip_assign_scheme", new Atom<>("none"));
			updateColumns.put("network", new Atom<>(true));
			updateColumns.put("vlan_id", new Atom<Integer>(vlanId));

			Row row = new Row(updateColumns);
			operations.add(new Update(wifiInetConfigDbTable, conditions, row));

			CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
			OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

			LOG.debug("Provisioned WifiInetConfig {}", ifName);

			for (OperationResult res : result) {
				LOG.debug("Op Result {}", res);
			}

		} catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
			LOG.error("Error in configureWifiInet", e);
			throw new RuntimeException(e);
		}

	}

	public void configureWifiInet(OvsdbClient ovsdbClient, Map<String, WifiInetConfigInfo> provisionedWifiInetConfigs,
			String ifName) {
		List<Operation> operations = new ArrayList<>();
		Map<String, Value> updateColumns = new HashMap<>();

		try {
			/// usr/plume/tools/ovsh i Wifi_Inet_Config NAT:=false enabled:=true
			/// if_name:=home-ap-24 if_type:=vif ip_assign_scheme:=none
			/// network:=true

			updateColumns.put("NAT", new Atom<>(false));
			updateColumns.put("enabled", new Atom<>(true));
			updateColumns.put("if_name", new Atom<>(ifName));
			updateColumns.put("if_type", new Atom<>("vif"));
			updateColumns.put("ip_assign_scheme", new Atom<>("none"));
			updateColumns.put("network", new Atom<>(true));

			Row row = new Row(updateColumns);
			operations.add(new Insert(wifiInetConfigDbTable, row));

			CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
			OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

			LOG.debug("Provisioned WifiInetConfig {}", ifName);

			for (OperationResult res : result) {
				LOG.debug("Op Result {}", res);
			}

		} catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
			LOG.error("Error in configureWifiInet", e);
			throw new RuntimeException(e);
		}

	}

	public void configureWifiInetSetNetwork(OvsdbClient ovsdbClient, String ifName) {
		List<Operation> operations = new ArrayList<>();
		Map<String, Value> updateColumns = new HashMap<>();
		List<Condition> conditions = new ArrayList<>();

		try {
			/// usr/plume/tools/ovsh u Wifi_Inet_Config -w if_name=="br-home"
			/// network:=true

			conditions.add(new Condition("if_name", Function.EQUALS, new Atom<>(ifName)));
			updateColumns.put("network", new Atom<>(true));

			Row row = new Row(updateColumns);
			operations.add(new Update(wifiInetConfigDbTable, conditions, row));

			CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
			OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

			LOG.debug("Enabled network on WifiInetConfig {}", ifName);

			for (OperationResult res : result) {
				LOG.debug("Op Result {}", res);
			}

		} catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
			LOG.error("Error in configureWifiInetSetNetwork", e);
			throw new RuntimeException(e);
		}

	}

	public void configureWifiInet(OvsdbClient ovsdbClient) {
		Map<String, WifiInetConfigInfo> provisionedWifiInetConfigs = getProvisionedWifiInetConfigs(ovsdbClient);
		LOG.debug("Existing WifiInetConfigs: {}", provisionedWifiInetConfigs.keySet());

		String ifName = homeAp24;
		if (!provisionedWifiInetConfigs.containsKey(ifName)) {
			configureWifiInet(ovsdbClient, provisionedWifiInetConfigs, ifName);
		}

		ifName = homeApL50;
		if (!provisionedWifiInetConfigs.containsKey(ifName)) {
			configureWifiInet(ovsdbClient, provisionedWifiInetConfigs, ifName);
		}

		ifName = homeApU50;
		if (!provisionedWifiInetConfigs.containsKey(ifName)) {
			configureWifiInet(ovsdbClient, provisionedWifiInetConfigs, ifName);
		}

		if (!provisionedWifiInetConfigs.containsKey(brLan) || !provisionedWifiInetConfigs.get(brLan).network) {
			// set network flag on brHome in wifiInetConfig table
			configureWifiInetSetNetwork(ovsdbClient, brLan);
		}
	}

	public void configureStats(OvsdbClient ovsdbClient) {

		Map<String, WifiStatsConfigInfo> provisionedWifiStatsConfigs = getProvisionedWifiStatsConfigs(ovsdbClient);

		try {
			List<Operation> operations = new ArrayList<>();
			Map<String, Value> updateColumns = new HashMap<>();
			Map<String, Integer> thresholdMap = new HashMap<>();
			thresholdMap.put("max_delay", 600);
			thresholdMap.put("util", 10);

			@SuppressWarnings("unchecked")
			com.vmware.ovsdb.protocol.operation.notation.Map<String, Integer> thresholds = com.vmware.ovsdb.protocol.operation.notation.Map
					.of(thresholdMap);

			provisionWifiStatsConfigDevice(provisionedWifiStatsConfigs, operations, updateColumns);

			provisionWifiStatsConfigSurvey(provisionedWifiStatsConfigs, operations, thresholds);

			provisionWifiStatsConfigNeighbor(provisionedWifiStatsConfigs, operations);

			provisionWifiStatsConfigClient(provisionedWifiStatsConfigs, operations);

			provisionWifiStatsRssi(provisionedWifiStatsConfigs, operations);

			if (!operations.isEmpty()) {
				CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
				OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

				if (LOG.isDebugEnabled()) {
					LOG.debug("Updated {}:", wifiStatsConfigDbTable);

					for (OperationResult res : result) {
						LOG.debug("Op Result {}", res);
					}
				}
			}

		} catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private void provisionWifiStatsRssi(Map<String, WifiStatsConfigInfo> provisionedWifiStatsConfigs,
			List<Operation> operations) {
		Map<String, Value> updateColumns;
		Row row;
		for (String band : new String[] { "2.4G", "5GL", "5GU" }) {
			if (!provisionedWifiStatsConfigs.containsKey(band + "_rssi_on-chan")) {
				updateColumns = new HashMap<>();
				updateColumns.put("radio_type", new Atom<>(band));
				updateColumns.put("reporting_count", new Atom<>(0));
				updateColumns.put("reporting_interval", new Atom<>(60));
				updateColumns.put("sampling_interval", new Atom<>(10));
				updateColumns.put("stats_type", new Atom<>("rssi"));
				updateColumns.put("survey_interval_ms", new Atom<>(0));
				updateColumns.put("survey_type", new Atom<>("on-chan"));
				row = new Row(updateColumns);

				operations.add(new Insert(wifiStatsConfigDbTable, row));
			}

		}
	}

	private void provisionWifiStatsConfigNeighbor(Map<String, WifiStatsConfigInfo> provisionedWifiStatsConfigs,
			List<Operation> operations) {
		Map<String, Value> updateColumns;
		Row row;

		Set<Integer> channelSet2g = new HashSet<>();
		channelSet2g.add(1);
		channelSet2g.add(6);
		channelSet2g.add(11);
		com.vmware.ovsdb.protocol.operation.notation.Set channels2g = com.vmware.ovsdb.protocol.operation.notation.Set
				.of(channelSet2g);

		Set<Integer> channelSet5gl = new HashSet<>();
		channelSet5gl.add(36);
		channelSet5gl.add(44);
		channelSet5gl.add(52);
		com.vmware.ovsdb.protocol.operation.notation.Set channels5gl = com.vmware.ovsdb.protocol.operation.notation.Set
				.of(channelSet5gl);

		Set<Integer> channelSet5gu = new HashSet<>();
		channelSet5gu.add(100);
		channelSet5gu.add(108);
		channelSet5gu.add(116);
		com.vmware.ovsdb.protocol.operation.notation.Set channels5gu = com.vmware.ovsdb.protocol.operation.notation.Set
				.of(channelSet5gu);

		if (!provisionedWifiStatsConfigs.containsKey("2.4G_neighbor_off-chan")) {

			updateColumns = new HashMap<>();
			updateColumns.put("channel_list", channels2g);
			updateColumns.put("radio_type", new Atom<>("2.4G"));
			updateColumns.put("reporting_interval", new Atom<>(120));
			updateColumns.put("sampling_interval", new Atom<>(0));
			updateColumns.put("stats_type", new Atom<>("neighbor"));
			updateColumns.put("survey_interval_ms", new Atom<>(0));
			updateColumns.put("survey_type", new Atom<>("off-chan"));
			// updateColumns.put("threshold", thresholds );

			row = new Row(updateColumns);
			operations.add(new Insert(wifiStatsConfigDbTable, row));

		}

		if (!provisionedWifiStatsConfigs.containsKey("5GL_neighbor_off-chan")) {

			updateColumns = new HashMap<>();
			updateColumns.put("channel_list", channels5gl);
			updateColumns.put("radio_type", new Atom<>("5GL"));
			updateColumns.put("reporting_interval", new Atom<>(120));
			updateColumns.put("sampling_interval", new Atom<>(0));
			updateColumns.put("stats_type", new Atom<>("neighbor"));
			updateColumns.put("survey_interval_ms", new Atom<>(0));
			updateColumns.put("survey_type", new Atom<>("off-chan"));
			// updateColumns.put("threshold", thresholds );

			row = new Row(updateColumns);
			operations.add(new Insert(wifiStatsConfigDbTable, row));

		}

		if (!provisionedWifiStatsConfigs.containsKey("5GU_neighbor_off-chan")) {

			updateColumns = new HashMap<>();
			updateColumns.put("channel_list", channels5gu);
			updateColumns.put("radio_type", new Atom<>("5GU"));
			updateColumns.put("reporting_interval", new Atom<>(120));
			updateColumns.put("sampling_interval", new Atom<>(0));
			updateColumns.put("stats_type", new Atom<>("neighbor"));
			updateColumns.put("survey_interval_ms", new Atom<>(0));
			updateColumns.put("survey_type", new Atom<>("off-chan"));
			// updateColumns.put("threshold", thresholds );

			row = new Row(updateColumns);
			operations.add(new Insert(wifiStatsConfigDbTable, row));

		}

		if (!provisionedWifiStatsConfigs.containsKey("5GU_neighbor_on-chan")) {
			//
			updateColumns = new HashMap<>();
			// updateColumns.put("channel_list", channels );
			updateColumns.put("radio_type", new Atom<>("5GU"));
			updateColumns.put("reporting_interval", new Atom<>(60));
			updateColumns.put("sampling_interval", new Atom<>(0));
			updateColumns.put("stats_type", new Atom<>("neighbor"));
			updateColumns.put("survey_interval_ms", new Atom<>(0));
			updateColumns.put("survey_type", new Atom<>("on-chan"));
			// updateColumns.put("threshold", thresholds );

			row = new Row(updateColumns);
			operations.add(new Insert(wifiStatsConfigDbTable, row));
			//
		}

		if (!provisionedWifiStatsConfigs.containsKey("5GL_neighbor_on-chan")) {
			//
			updateColumns = new HashMap<>();
			// updateColumns.put("channel_list", channels );
			updateColumns.put("radio_type", new Atom<>("5GL"));
			updateColumns.put("reporting_interval", new Atom<>(60));
			updateColumns.put("sampling_interval", new Atom<>(0));
			updateColumns.put("stats_type", new Atom<>("neighbor"));
			updateColumns.put("survey_interval_ms", new Atom<>(0));
			updateColumns.put("survey_type", new Atom<>("on-chan"));
			// updateColumns.put("threshold", thresholds );

			row = new Row(updateColumns);
			operations.add(new Insert(wifiStatsConfigDbTable, row));
			//
		}

		if (!provisionedWifiStatsConfigs.containsKey("2.4G_neighbor_on-chan")) {
			//
			updateColumns = new HashMap<>();
			// updateColumns.put("channel_list", channels );
			updateColumns.put("radio_type", new Atom<>("2.4G"));
			updateColumns.put("reporting_interval", new Atom<>(60));
			updateColumns.put("sampling_interval", new Atom<>(0));
			updateColumns.put("stats_type", new Atom<>("neighbor"));
			updateColumns.put("survey_interval_ms", new Atom<>(0));
			updateColumns.put("survey_type", new Atom<>("on-chan"));
			// updateColumns.put("threshold", thresholds );

			row = new Row(updateColumns);
			operations.add(new Insert(wifiStatsConfigDbTable, row));
			//
		}
	}

	private void provisionWifiStatsConfigSurvey(Map<String, WifiStatsConfigInfo> provisionedWifiStatsConfigs,
			List<Operation> operations, com.vmware.ovsdb.protocol.operation.notation.Map<String, Integer> thresholds) {

		Set<Integer> channelSet2g = new HashSet<>();
		channelSet2g.add(1);
		channelSet2g.add(6);
		channelSet2g.add(11);
		com.vmware.ovsdb.protocol.operation.notation.Set channels2g = com.vmware.ovsdb.protocol.operation.notation.Set
				.of(channelSet2g);

		Set<Integer> channelSet5gl = new HashSet<>();
		channelSet5gl.add(36);
		channelSet5gl.add(44);
		channelSet5gl.add(52);
		com.vmware.ovsdb.protocol.operation.notation.Set channels5gl = com.vmware.ovsdb.protocol.operation.notation.Set
				.of(channelSet5gl);

		Set<Integer> channelSet5gu = new HashSet<>();
		channelSet5gu.add(100);
		channelSet5gu.add(108);
		channelSet5gu.add(116);
		com.vmware.ovsdb.protocol.operation.notation.Set channels5gu = com.vmware.ovsdb.protocol.operation.notation.Set
				.of(channelSet5gu);

		Map<String, Value> updateColumns;
		Row row;

		if (!provisionedWifiStatsConfigs.containsKey("2.4G_survey_on-chan")) {
			//
			updateColumns = new HashMap<>();
			updateColumns.put("radio_type", new Atom<>("2.4G"));
			updateColumns.put("reporting_count", new Atom<>(0));
			updateColumns.put("reporting_interval", new Atom<>(60));
			updateColumns.put("sampling_interval", new Atom<>(10));
			updateColumns.put("stats_type", new Atom<>("survey"));
			updateColumns.put("survey_interval_ms", new Atom<>(0));
			updateColumns.put("survey_type", new Atom<>("on-chan"));
			row = new Row(updateColumns);
			operations.add(new Insert(wifiStatsConfigDbTable, row));
			//
		}

		if (!provisionedWifiStatsConfigs.containsKey("5GL_survey_on-chan")) {
			//
			updateColumns = new HashMap<>();
			updateColumns.put("radio_type", new Atom<>("5GL"));
			updateColumns.put("reporting_count", new Atom<>(0));
			updateColumns.put("reporting_interval", new Atom<>(60));
			updateColumns.put("sampling_interval", new Atom<>(10));
			updateColumns.put("stats_type", new Atom<>("survey"));
			updateColumns.put("survey_interval_ms", new Atom<>(0));
			updateColumns.put("survey_type", new Atom<>("on-chan"));
			row = new Row(updateColumns);
			operations.add(new Insert(wifiStatsConfigDbTable, row));
			//
		}

		if (!provisionedWifiStatsConfigs.containsKey("5GU_survey_on-chan")) {
			updateColumns = new HashMap<>();
			updateColumns.put("radio_type", new Atom<>("5GU"));
			updateColumns.put("reporting_count", new Atom<>(0));
			updateColumns.put("reporting_interval", new Atom<>(60));
			updateColumns.put("sampling_interval", new Atom<>(10));
			updateColumns.put("stats_type", new Atom<>("survey"));
			updateColumns.put("survey_interval_ms", new Atom<>(0));
			updateColumns.put("survey_type", new Atom<>("on-chan"));
			row = new Row(updateColumns);
			operations.add(new Insert(wifiStatsConfigDbTable, row));
		}

		if (!provisionedWifiStatsConfigs.containsKey("2.4G_survey_off-chan")) {
			//
			updateColumns = new HashMap<>();
			updateColumns.put("channel_list", channels2g);
			updateColumns.put("radio_type", new Atom<>("2.4G"));
			updateColumns.put("reporting_interval", new Atom<>(0));
			updateColumns.put("sampling_interval", new Atom<>(0));
			updateColumns.put("stats_type", new Atom<>("survey"));
			updateColumns.put("survey_interval_ms", new Atom<>(10));
			updateColumns.put("survey_type", new Atom<>("off-chan"));
			updateColumns.put("threshold", thresholds);
			row = new Row(updateColumns);
			operations.add(new Insert(wifiStatsConfigDbTable, row));
			//
		}

		if (!provisionedWifiStatsConfigs.containsKey("5GL_survey_off-chan")) {
			//
			updateColumns = new HashMap<>();
			updateColumns.put("channel_list", channels5gl);
			updateColumns.put("radio_type", new Atom<>("5GL"));
			updateColumns.put("reporting_interval", new Atom<>(0));
			updateColumns.put("sampling_interval", new Atom<>(0));
			updateColumns.put("stats_type", new Atom<>("survey"));
			updateColumns.put("survey_interval_ms", new Atom<>(10));
			updateColumns.put("survey_type", new Atom<>("off-chan"));
			updateColumns.put("threshold", thresholds);
			row = new Row(updateColumns);
			operations.add(new Insert(wifiStatsConfigDbTable, row));
			//
		}

		if (!provisionedWifiStatsConfigs.containsKey("5GU_survey_off-chan")) {
			//
			updateColumns = new HashMap<>();
			updateColumns.put("channel_list", channels5gu);
			updateColumns.put("radio_type", new Atom<>("5GU"));
			updateColumns.put("reporting_interval", new Atom<>(0));
			updateColumns.put("sampling_interval", new Atom<>(0));
			updateColumns.put("stats_type", new Atom<>("survey"));
			updateColumns.put("survey_interval_ms", new Atom<>(10));
			updateColumns.put("survey_type", new Atom<>("off-chan"));
			updateColumns.put("threshold", thresholds);
			row = new Row(updateColumns);
			operations.add(new Insert(wifiStatsConfigDbTable, row));
			//
		}

	}

	private void provisionWifiStatsConfigDevice(Map<String, WifiStatsConfigInfo> provisionedWifiStatsConfigs,
			List<Operation> operations, Map<String, Value> updateColumns) {
		Row row;
		if (!provisionedWifiStatsConfigs.containsKey("2.4G_device")) {
			updateColumns.put("radio_type", new Atom<>("2.4G"));
			updateColumns.put("reporting_interval", new Atom<>(900));
			updateColumns.put("sampling_interval", new Atom<>(0));
			updateColumns.put("stats_type", new Atom<>("device"));
			row = new Row(updateColumns);
			operations.add(new Insert(wifiStatsConfigDbTable, row));
		}

		if (!provisionedWifiStatsConfigs.containsKey("5GL_device")) {
			// updateColumns.put("channel_list", channels );
			updateColumns.put("radio_type", new Atom<>("5GL"));
			updateColumns.put("reporting_interval", new Atom<>(900));
			updateColumns.put("sampling_interval", new Atom<>(0));
			updateColumns.put("stats_type", new Atom<>("device"));
			row = new Row(updateColumns);
			operations.add(new Insert(wifiStatsConfigDbTable, row));
		}

		if (!provisionedWifiStatsConfigs.containsKey("5GU_device")) {
			// updateColumns.put("channel_list", channels );
			updateColumns.put("radio_type", new Atom<>("5GU"));
			updateColumns.put("reporting_interval", new Atom<>(900));
			updateColumns.put("sampling_interval", new Atom<>(0));
			updateColumns.put("stats_type", new Atom<>("device"));
			row = new Row(updateColumns);
			operations.add(new Insert(wifiStatsConfigDbTable, row));
		}
	}

	private void provisionWifiStatsConfigClient(Map<String, WifiStatsConfigInfo> provisionedWifiStatsConfigs,
			List<Operation> operations) {
		Map<String, Value> updateColumns;
		Row row;
		if (!provisionedWifiStatsConfigs.containsKey("2.4G_client")) {
			updateColumns = new HashMap<>();
			updateColumns.put("radio_type", new Atom<>("2.4G"));
			updateColumns.put("reporting_count", new Atom<>(0));
			updateColumns.put("reporting_interval", new Atom<>(60));
			updateColumns.put("sampling_interval", new Atom<>(10));
			updateColumns.put("stats_type", new Atom<>("client"));
			row = new Row(updateColumns);
			operations.add(new Insert(wifiStatsConfigDbTable, row));
		}

		if (!provisionedWifiStatsConfigs.containsKey("5GL_client")) {

			updateColumns = new HashMap<>();
			updateColumns.put("radio_type", new Atom<>("5GL"));
			updateColumns.put("reporting_count", new Atom<>(0));
			updateColumns.put("reporting_interval", new Atom<>(60));
			updateColumns.put("sampling_interval", new Atom<>(10));
			updateColumns.put("stats_type", new Atom<>("client"));
			row = new Row(updateColumns);
			operations.add(new Insert(wifiStatsConfigDbTable, row));

		}

		if (!provisionedWifiStatsConfigs.containsKey("5GU_client")) {

			updateColumns = new HashMap<>();
			updateColumns.put("radio_type", new Atom<>("5GU"));
			updateColumns.put("reporting_count", new Atom<>(0));
			updateColumns.put("reporting_interval", new Atom<>(60));
			updateColumns.put("sampling_interval", new Atom<>(10));
			updateColumns.put("stats_type", new Atom<>("client"));
			row = new Row(updateColumns);
			operations.add(new Insert(wifiStatsConfigDbTable, row));
		}
	}

	public String changeRedirectorAddress(OvsdbClient ovsdbClient, String apId, String newRedirectorAddress) {
		try {
			List<Operation> operations = new ArrayList<>();
			Map<String, Value> updateColumns = new HashMap<>();

			updateColumns.put("redirector_addr", new Atom<>(newRedirectorAddress));

			Row row = new Row(updateColumns);
			operations.add(new Update(awlanNodeDbTable, row));

			CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
			OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

			if (LOG.isDebugEnabled()) {
				LOG.debug("Updated {} redirector_addr = {}", awlanNodeDbTable, newRedirectorAddress);

				for (OperationResult res : result) {
					LOG.debug("Op Result {}", res);
				}
			}

		} catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
			throw new RuntimeException(e);
		}

		return newRedirectorAddress;
	}

}
