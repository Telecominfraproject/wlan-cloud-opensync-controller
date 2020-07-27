package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import java.util.ArrayList;
import java.util.Collections;
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
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.core.model.equipment.ChannelBandwidth;
import com.telecominfraproject.wlan.core.model.equipment.MacAddress;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.equipment.models.ApElementConfiguration;
import com.telecominfraproject.wlan.equipment.models.ElementRadioConfiguration;
import com.telecominfraproject.wlan.equipment.models.GettingDNS;
import com.telecominfraproject.wlan.equipment.models.GettingIP;
import com.telecominfraproject.wlan.equipment.models.NetworkForwardMode;
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
import com.telecominfraproject.wlan.profile.radius.models.RadiusProfile;
import com.telecominfraproject.wlan.profile.radius.models.RadiusServer;
import com.telecominfraproject.wlan.profile.radius.models.RadiusServiceRegion;
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
import com.vmware.ovsdb.protocol.operation.result.ErrorResult;
import com.vmware.ovsdb.protocol.operation.result.InsertResult;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.protocol.operation.result.SelectResult;
import com.vmware.ovsdb.service.OvsdbClient;

@Component
public class OvsdbDao {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbDao.class);

    public static final String wifiRouteStateDbTable = "Wifi_Route_State";

    public static final String wifiMasterStateDbTable = "Wifi_Master_State";

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.managerAddr:3.88.149.10}")

    private String managerIpAddr;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.region:Ottawa}")
    public String region;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.listenPort:6640}")
    private int ovsdbListenPort;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.mqttBroker.address.external:testportal.123wlan.com}")
    private String mqttBrokerAddress;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.mqttBroker.listenPort:1883}")
    private int mqttBrokerListenPort;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.timeoutSec:30}")
    private int ovsdbTimeoutSec;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-iface.default_bridge:defaultBridgeForEAPOL}")
    public String bridgeNameVifInterfaces;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-iface.default_lan_type:bridge}")
    public String defaultLanInterfaceType;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-iface.default_wan_type:eth}")
    public String defaultWanInterfaceType;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-iface.default_radio0:home-ap-24}")
    public String defaultRadio0;
    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-iface.default_radio1:home-ap-l50}")
    public String defaultRadio1;
    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-iface.default_radio2:home-ap-u50}")
    public String defaultRadio2;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-device.radio0:wifi0}")
    public String radio0;
    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-device.radio1:wifi1}")
    public String radio1;
    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-device.radio2:wifi2}")
    public String radio2;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-iface.max:8}")
    public int maxInterfacesPerRadio;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.awlan-node.upgrade_dl_timer:60}")
    public long upgradeDlTimerSeconds;
    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.awlan-node.upgrade_timer:90}")
    public long upgradeTimerSeconds;

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
            columns.add("revision");
            columns.add("version_matrix");

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
            if ((result != null) && (result.length > 0) && (result[0] instanceof SelectResult)
                    && !((SelectResult) result[0]).getRows().isEmpty()) {
                row = ((SelectResult) result[0]).getRows().iterator().next();
            }

            ret.mqttSettings = row != null ? row.getMapColumn("mqtt_settings") : null;
            ret.versionMatrix = row != null ? row.getMapColumn("version_matrix") : null;
            ret.redirectorAddr = row != null ? row.getStringColumn("redirector_addr") : null;
            ret.managerAddr = row != null ? row.getStringColumn("manager_addr") : null;

            ret.platformVersion = row != null ? row.getStringColumn("platform_version") : null;
            ret.firmwareVersion = row != null ? row.getStringColumn("firmware_version") : null;

            ret.revision = row != null ? row.getStringColumn("revision") : null;

            ret.skuNumber = getSingleValueFromSet(row, "sku_number");
            ret.serialNumber = getSingleValueFromSet(row, "serial_number");
            ret.model = getSingleValueFromSet(row, "model");

            // now populate macAddress, ipV4Address from Wifi_Inet_State
            // first look them up for if_name = br-wan
            fillInWanIpAddressAndMac(ovsdbClient, ret, defaultWanInterfaceType);
            if ((ret.ipV4Address == null) || (ret.macAddress == null)) {
                // when not found - look them up for if_name = br-lan
                fillInWanIpAddressAndMac(ovsdbClient, ret, defaultLanInterfaceType);

            }
            fillInLanIpAddressAndMac(ovsdbClient, ret, defaultLanInterfaceType);

            fillInRadioInterfaceNames(ovsdbClient, ret);

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        LOG.debug("ConnectNodeInfo created {}", ret);

        return ret;
    }

    private void fillInRadioInterfaceNames(OvsdbClient ovsdbClient, ConnectNodeInfo ret) {
        try {
            List<Operation> operations = new ArrayList<>();
            List<Condition> conditions = new ArrayList<>();
            List<String> columns = new ArrayList<>();

            columns.add("freq_band");
            columns.add("if_name");

            operations.add(new Select(wifiRadioStateDbTable, conditions, columns));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Select from {}:", wifiRadioStateDbTable);

                for (OperationResult res : result) {
                    LOG.debug("Op Result {}", res);
                }
            }

            if ((result != null) && (result.length > 0) && (result[0] instanceof SelectResult)
                    && !((SelectResult) result[0]).getRows().isEmpty()) {

                for (Row row : ((SelectResult) result[0]).getRows()) {
                    ret.wifiRadioStates.put(getSingleValueFromSet(row, "freq_band"),
                            getSingleValueFromSet(row, "if_name"));

                }

            }

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    private void getEnabledRadios(OvsdbClient ovsdbClient, List<RadioType> radios) {
        try {
            List<Operation> operations = new ArrayList<>();
            List<Condition> conditions = new ArrayList<>();
            List<String> columns = new ArrayList<>();

            columns.add("freq_band");

            conditions.add(new Condition("enabled", Function.EQUALS, new Atom<>(true)));

            operations.add(new Select(wifiRadioStateDbTable, conditions, columns));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Select from {}:", wifiRadioStateDbTable);

                for (OperationResult res : result) {
                    LOG.debug("Op Result {}", res);
                }
            }

            if ((result != null) && (result.length > 0) && (result[0] instanceof SelectResult)
                    && !((SelectResult) result[0]).getRows().isEmpty()) {

                for (Row row : ((SelectResult) result[0]).getRows()) {

                    String radioFrequencyBand = getSingleValueFromSet(row, "freq_band");
                    switch (radioFrequencyBand) {
                    case "2.4G":
                        radios.add(RadioType.is2dot4GHz);
                        break;
                    case "5G":
                        radios.add(RadioType.is5GHz);
                        break;
                    case "5GL":
                        radios.add(RadioType.is5GHzL);
                        break;
                    case "5GU":
                        radios.add(RadioType.is5GHzU);
                        break;
                    default:
                        LOG.debug("Unsupported or unrecognized radio band type {}", radioFrequencyBand);

                    }

                }

            }

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    private Map<String, Set<Integer>> getAllowedChannels(OvsdbClient ovsdbClient) {

        Map<String, Set<Integer>> allowedChannels = new HashMap<>();

        try {
            List<Operation> operations = new ArrayList<>();
            List<Condition> conditions = new ArrayList<>();
            List<String> columns = new ArrayList<>();

            columns.add("freq_band");
            columns.add("allowed_channels");

            operations.add(new Select(wifiRadioStateDbTable, conditions, columns));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Select from {}:", wifiRadioStateDbTable);

                for (OperationResult res : result) {
                    LOG.debug("Op Result {}", res);
                }
            }

            if ((result != null) && (result.length > 0) && (result[0] instanceof SelectResult)
                    && !((SelectResult) result[0]).getRows().isEmpty()) {

                for (Row row : ((SelectResult) result[0]).getRows()) {
                    allowedChannels.put(getSingleValueFromSet(row, "freq_band"), row.getSetColumn("allowed_channels"));

                }

            }

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return allowedChannels;

    }

    public void fillInLanIpAddressAndMac(OvsdbClient ovsdbClient, ConnectNodeInfo connectNodeInfo, String ifType) {
        try {
            List<Operation> operations = new ArrayList<>();
            List<Condition> conditions = new ArrayList<>();
            List<String> columns = new ArrayList<>();
            // populate macAddress, ipV4Address from Wifi_Inet_State

            columns.add("inet_addr");
            columns.add("hwaddr");
            columns.add("if_type");
            columns.add("if_name");

            conditions.add(new Condition("if_type", Function.EQUALS, new Atom<>(ifType)));

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
            if ((result != null) && (result.length > 0) && (result[0] instanceof SelectResult)
                    && !((SelectResult) result[0]).getRows().isEmpty()) {
                row = ((SelectResult) result[0]).getRows().iterator().next();
                connectNodeInfo.lanIpV4Address = getSingleValueFromSet(row, "inet_addr");
                connectNodeInfo.lanIfName = row.getStringColumn("if_name");
                connectNodeInfo.lanIfType = getSingleValueFromSet(row, "if_type");
                connectNodeInfo.lanMacAddress = getSingleValueFromSet(row, "hwaddr");

            } else if ((result != null) && (result.length > 0) && (result[0] instanceof ErrorResult)) {
                LOG.warn("Error reading from {} table: {}", wifiInetStateDbTable, result[0]);
            }

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    public void fillInWanIpAddressAndMac(OvsdbClient ovsdbClient, ConnectNodeInfo connectNodeInfo, String ifType) {
        try {
            List<Operation> operations = new ArrayList<>();
            List<Condition> conditions = new ArrayList<>();
            List<String> columns = new ArrayList<>();
            // populate macAddress, ipV4Address from Wifi_Inet_State

            columns.add("inet_addr");
            columns.add("hwaddr");
            columns.add("if_name");
            columns.add("if_type");

            conditions.add(new Condition("if_type", Function.EQUALS, new Atom<>(ifType)));

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
            if ((result != null) && (result.length > 0) && (result[0] instanceof SelectResult)
                    && !((SelectResult) result[0]).getRows().isEmpty()) {
                row = ((SelectResult) result[0]).getRows().iterator().next();
                connectNodeInfo.ipV4Address = getSingleValueFromSet(row, "inet_addr");
                connectNodeInfo.ifName = row.getStringColumn("if_name");
                connectNodeInfo.ifType = getSingleValueFromSet(row, "if_type");
                connectNodeInfo.macAddress = getSingleValueFromSet(row, "hwaddr");

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
            if ((ret.skuNumber == null) || ret.skuNumber.isEmpty()) {
                ret.skuNumber = "tip.wlan_" + ret.serialNumber;
                updateColumns.put("sku_number", new Atom<>(ret.skuNumber));
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

            if ((ret.mqttSettings == null) || !ret.mqttSettings.equals(newMqttSettings)) {
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
     * @return value of reporting_interval column for the stats_type=device from
     *         the Wifi_Stats_Config table. If value is not provisioned then
     *         return -1.
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
            if ((result != null) && (result.length > 0) && !((SelectResult) result[0]).getRows().isEmpty()) {
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
     * @param value
     *            of reporting_interval column for the stats_type=device from
     *            the Wifi_Stats_Config table. If value is not provisioned then
     *            return -1.
     */
    public void updateDeviceStatsReportingInterval(OvsdbClient ovsdbClient, long newValue) {
        try {
            List<Operation> operations = new ArrayList<>();
            Map<String, Value> updateColumns = new HashMap<>();

            // turn on stats collection over MQTT: (reporting_interval is in
            // seconds?)
            // $ ovsh i Wifi_Stats_Config reporting_interval:=10
            // radio_type:="2.4G" stats_type:="device"

            updateColumns.put("reporting_interval", new Atom<>(newValue));
            updateColumns.put("radio_type", new Atom<>("2.4G"));
            updateColumns.put("stats_type", new Atom<>("device"));

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

    /**
     * @param ovsdbClient
     * @param value
     *            of reporting_interval column for the stats_type=device from
     *            the Wifi_Stats_Config table. If value is not provisioned then
     *            return -1.
     */
    public void enableNetworkProbeForSyntheticClient(OvsdbClient ovsdbClient) {
        LOG.debug("Enable network_probe metrics for synthetic client");

        try {
            List<Operation> operations = new ArrayList<>();
            Map<String, Value> updateColumns = new HashMap<>();

            updateColumns.put("reporting_interval", new Atom<>(60));
            updateColumns.put("radio_type", new Atom<>("2.4G"));
            updateColumns.put("stats_type", new Atom<>("network_probe"));

            Row row = new Row(updateColumns);
            operations.add(new Insert(wifiStatsConfigDbTable, row));

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            if (LOG.isDebugEnabled()) {

                for (OperationResult res : result) {

                    if (res instanceof ErrorResult) {
                        LOG.error("Could not update {}:", wifiStatsConfigDbTable);
                        LOG.error("Error: {} Details: {}", ((ErrorResult) res).getError(),
                                ((ErrorResult) res).getDetails());
                    } else {
                        LOG.debug("Updated {}:", wifiStatsConfigDbTable);
                        LOG.debug("Op Result {}", res);
                    }
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
            if ((result != null) && (result.length > 0) && !((SelectResult) result[0]).getRows().isEmpty()) {
                row = ((SelectResult) result[0]).getRows().iterator().next();
            }

            firmwareVersion = row != null ? row.getStringColumn("firmware_version") : null;

            skuNumber = getSingleValueFromSet(row, "sku_number");
            serialNumber = getSingleValueFromSet(row, "serial_number");
            model = getSingleValueFromSet(row, "model");

            LOG.info("Redirecting AP Node: clientCn {} serialNumber {} model {} firmwareVersion {} skuNumber {}",
                    clientCn, serialNumber, model, firmwareVersion, skuNumber);

            // Update table AWLAN_Node - set manager_addr
            operations.clear();
            Map<String, Value> updateColumns = new HashMap<>();

            updateColumns.put("manager_addr", new Atom<>("ssl:" + managerIpAddr + ":" + ovsdbListenPort));

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

    public <T> Set<T> getSet(Row row, String columnName) {

        Set<T> set = row != null ? row.getSetColumn(columnName) : null;

        return set;
    }

    public <T> T getSingleValueFromSet(Row row, String columnName) {

        Set<T> set = row != null ? row.getSetColumn(columnName) : null;
        T ret = (set != null) && !set.isEmpty() ? set.iterator().next() : null;

        return ret;
    }

    public void getWifiVIFsForRadioByUuid(OvsdbClient ovsdbClient, List<OpensyncAPVIFState> vifList, Uuid uuid) {
        List<Operation> operations = new ArrayList<>();
        List<Condition> conditions = new ArrayList<>();

        conditions.add(new Condition("_uuid", Function.EQUALS, new Atom<>(uuid)));
        operations.add(new Select(wifiVifStateDbTable, conditions));

        try {
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            if ((result != null) && (result.length > 0) && result[0] instanceof SelectResult
                    && !((SelectResult) result[0]).getRows().isEmpty()) {

                Row row = ((SelectResult) result[0]).getRows().iterator().next();

                OpensyncAPVIFState tableState = processWifiVIFStateColumn(ovsdbClient, row, false);

                vifList.add(tableState);

            }

        } catch (Exception e) {

            LOG.info("Could not get Wifi_VIF_State for UUID {}. {}", uuid, e.getMessage());

        }
    }

    private OpensyncAPVIFState processWifiVIFStateColumn(OvsdbClient ovsdbClient, Row row, boolean update) {
        OpensyncAPVIFState tableState = new OpensyncAPVIFState();

        Map<String, Value> map = row.getColumns();

        if ((map.get("mac") != null)
                && map.get("mac").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            tableState.setMac(row.getStringColumn("mac"));
        }
        if ((map.get("bridge") != null)
                && map.get("bridge").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            tableState.setBridge(row.getStringColumn("bridge"));
        }
        if ((map.get("btm") != null)
                && map.get("btm").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            tableState.setBtm(row.getIntegerColumn("btm").intValue());
        }

        if ((map.get("channel") != null)
                && map.get("channel").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            tableState.setChannel(row.getIntegerColumn("channel").intValue());
        }

        if ((map.get("enabled") != null)
                && map.get("enabled").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            tableState.setEnabled(row.getBooleanColumn("enabled"));
        }

        Long ftPsk = getSingleValueFromSet(row, "ft_psk");
        if (ftPsk != null) {
            tableState.setFtPsk(ftPsk.intValue());
        }

        Long ftMobilityDomain = getSingleValueFromSet(row, "ft_mobility_domain");
        if (ftMobilityDomain != null) {
            tableState.setFtMobilityDomain(ftMobilityDomain.intValue());
        }

        if ((map.get("group_rekey") != null)
                && map.get("group_rekey").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            tableState.setGroupRekey(row.getIntegerColumn("group_rekey").intValue());
        }
        if ((map.get("if_name") != null)
                && map.get("if_name").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            tableState.setIfName(row.getStringColumn("if_name"));
        }

        if ((map.get("mode") != null)
                && map.get("mode").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            tableState.setMode(row.getStringColumn("mode"));
        }

        if ((map.get("rrm") != null)
                && map.get("rrm").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            tableState.setRrm(row.getIntegerColumn("rrm").intValue());
        }
        if ((map.get("ssid") != null)
                && map.get("ssid").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            tableState.setSsid(row.getStringColumn("ssid"));
        }

        if ((map.get("ssid_broadcast") != null) && map.get("ssid_broadcast").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            tableState.setSsidBroadcast(row.getStringColumn("ssid_broadcast"));
        }
        if ((map.get("uapsd_enable") != null)
                && map.get("uapsd_enable").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            tableState.setUapsdEnable(row.getBooleanColumn("uapsd_enable"));
        }
        if ((map.get("vif_radio_idx") != null) && map.get("vif_radio_idx").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            tableState.setVifRadioIdx(row.getIntegerColumn("vif_radio_idx").intValue());
        }

        List<Uuid> associatedClientsList = new ArrayList<>();

        Set<Uuid> clients = row.getSetColumn("associated_clients");
        associatedClientsList.addAll(clients);

        tableState.setAssociatedClients(associatedClientsList);

        if (map.get("security") != null) {
            tableState.setSecurity(row.getMapColumn("security"));
        }

        if ((map.get("_version") != null)
                && map.get("_version").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            tableState.setVersion(row.getUuidColumn("_version"));
        }
        if ((map.get("_uuid") != null)
                && map.get("_uuid").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            tableState.setVersion(row.getUuidColumn("_uuid"));
        }
        return tableState;
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
        columns.add("bcn_int");
        columns.add("channel");
        columns.add("channel_mode");
        columns.add("country");
        columns.add("enabled");
        columns.add("ht_mode");
        columns.add("tx_power");
        columns.add("vif_configs");
        columns.add("freq_band");
        columns.add("hw_config");
        columns.add("hw_type");

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
                Long beaconTmp = getSingleValueFromSet(row, "bcn_int");
                if (beaconTmp == null) {
                    beaconTmp = 0L;
                }
                wifiRadioConfigInfo.beaconInterval = beaconTmp.intValue();
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
                wifiRadioConfigInfo.hwType = row.getStringColumn("hw_type");
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
        columns.add("ap_bridge");
        columns.add("_uuid");
        columns.add("btm");
        columns.add("enabled");
        columns.add("ft_psk");
        columns.add("group_rekey");
        columns.add("if_name");
        columns.add("min_hw_mode");
        columns.add("mode");
        columns.add("rrm");
        columns.add("ssid");
        columns.add("ssid_broadcast");
        columns.add("uapsd_enable");
        columns.add("vif_radio_idx");
        columns.add("security");
        columns.add("vlan_id");
        columns.add("mac_list");
        columns.add("mac_list_type");

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

                String bridge = getSingleValueFromSet(row, "bridge");
                if (bridge != null) {
                    wifiVifConfigInfo.bridge = bridge;
                }

                Boolean apBridge = getSingleValueFromSet(row, "ap_bridge");
                if (apBridge != null) {
                    wifiVifConfigInfo.apBridge = apBridge;
                }

                wifiVifConfigInfo.uuid = row.getUuidColumn("_uuid");

                Long btm = getSingleValueFromSet(row, "btm");
                if (btm != null) {
                    wifiVifConfigInfo.btm = btm.intValue();
                }

                Boolean enabled = getSingleValueFromSet(row, "enabled");
                if (enabled != null) {
                    wifiVifConfigInfo.enabled = enabled;
                }

                Long ftPsk = getSingleValueFromSet(row, "ft_psk");
                if (ftPsk != null) {
                    wifiVifConfigInfo.ftPsk = ftPsk.intValue();
                }

                Long groupRekey = getSingleValueFromSet(row, "group_rekey");
                if (groupRekey != null) {
                    wifiVifConfigInfo.groupRekey = groupRekey.intValue();
                }

                String minHwMode = getSingleValueFromSet(row, "min_hw_mode");
                if (minHwMode != null) {
                    wifiVifConfigInfo.bridge = minHwMode;
                }

                wifiVifConfigInfo.ifName = row.getStringColumn("if_name");

                String mode = getSingleValueFromSet(row, "mode");
                if (mode != null) {
                    wifiVifConfigInfo.mode = mode;
                }

                Long rrm = getSingleValueFromSet(row, "rrm");
                if (rrm != null) {
                    wifiVifConfigInfo.rrm = rrm.intValue();
                }

                String ssid = getSingleValueFromSet(row, "ssid");
                if (ssid != null) {
                    wifiVifConfigInfo.ssid = ssid;
                }

                String ssidBroadcast = getSingleValueFromSet(row, "ssid_broadcast");
                if (ssid != null) {
                    wifiVifConfigInfo.ssidBroadcast = ssidBroadcast;
                }
                Boolean uapsdEnable = getSingleValueFromSet(row, "uapsd_enable");
                if (uapsdEnable != null) {
                    wifiVifConfigInfo.uapsdEnable = uapsdEnable;
                }

                Long vifRadioIdx = getSingleValueFromSet(row, "vif_radio_idx");
                if (vifRadioIdx != null) {
                    wifiVifConfigInfo.vifRadioIdx = vifRadioIdx.intValue();
                }

                wifiVifConfigInfo.security = row.getMapColumn("security");

                Long vlanId = getSingleValueFromSet(row, "vlan_id");
                if (vlanId != null) {
                    wifiVifConfigInfo.vlanId = vlanId.intValue();
                }

                wifiVifConfigInfo.macList = row.getSetColumn("mac_list");

                if (row.getColumns().get("mac_list_type") != null && row.getColumns().get("mac_list_type").getClass()
                        .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                    wifiVifConfigInfo.macListType = row.getStringColumn("mac_list_type");
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
        columns.add("broadcast");
        columns.add("enabled");
        columns.add("if_name");
        columns.add("if_type");
        columns.add("ip_assign_scheme");
        columns.add("network");
        columns.add("inet_addr");
        columns.add("mtu");
        columns.add("netmask");
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
                if ((row.getColumns().get("broadcast") != null) && row.getColumns().get("broadcast").getClass()
                        .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                    wifiInetConfigInfo.broadcast = row.getStringColumn("broadcast");
                }
                wifiInetConfigInfo.enabled = row.getBooleanColumn("enabled");
                wifiInetConfigInfo.ifName = row.getStringColumn("if_name");
                wifiInetConfigInfo.ifType = row.getStringColumn("if_type");
                wifiInetConfigInfo.ipAssignScheme = row.getStringColumn("ip_assign_scheme");
                wifiInetConfigInfo.network = row.getBooleanColumn("network");
                if ((row.getColumns().get("inet_addr") != null) && row.getColumns().get("inet_addr").getClass()
                        .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                    wifiInetConfigInfo.inetAddr = row.getStringColumn("inet_addr");
                }
                if ((row.getColumns().get("mtu") != null) && row.getColumns().get("mtu").getClass()
                        .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                    wifiInetConfigInfo.mtu = row.getIntegerColumn("mtu").intValue();
                }
                if ((row.getColumns().get("netmask") != null) && row.getColumns().get("netmask").getClass()
                        .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                    wifiInetConfigInfo.netmask = row.getStringColumn("netmask");
                }
                if ((row.getColumns().get("vlan_id") != null) && row.getColumns().get("vlan_id").getClass()
                        .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                    wifiInetConfigInfo.vlanId = row.getIntegerColumn("vlan_id").intValue();
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
                if (wifiStatsConfigInfo.channelList == null) {
                    wifiStatsConfigInfo.channelList = Collections.emptySet();
                }
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

        LOG.debug("InterfaceName {} BridgeName {} InterfaceType {}", interfaceName, bridgeName, interfaceType);
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
                updateColumns.put("name", new Atom<>(interfaceName));
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

            LOG.debug("Sending batch of operations : {} ", operations);

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

    public void provisionBridgePortInterface(OvsdbClient ovsdbClient) {
        try {

            String patchW2h = "patch-w2h";
            String patchH2w = "patch-h2w";
            Map<String, InterfaceInfo> provisionedInterfaces = getProvisionedInterfaces(ovsdbClient);
            if (provisionedInterfaces.isEmpty()) {
                LOG.debug("No Interfaces defined in Ovsdb. Cannot perform Bridge/Port/Interface mapping.");
                return;
            }
            LOG.debug("Existing Interfaces: {}", provisionedInterfaces.keySet());

            Map<String, PortInfo> provisionedPorts = getProvisionedPorts(ovsdbClient);
            if (provisionedInterfaces.isEmpty()) {
                LOG.debug("No Ports defined in Ovsdb. Cannot perform Bridge/Port/Interface mapping.");
                return;
            }
            LOG.debug("Existing Ports: {}", provisionedPorts.keySet());

            Map<String, BridgeInfo> provisionedBridges = getProvisionedBridges(ovsdbClient);
            if (provisionedInterfaces.isEmpty()) {
                LOG.debug("No Bridges defined in Ovsdb. Cannot perform Bridge/Port/Interface mapping.");
                return;
            }
            LOG.debug("Existing Bridges: {}", provisionedBridges.keySet());

            Map<String, String> patchH2wOptions = new HashMap<>();
            patchH2wOptions.put("peer", "patch-w2h");

            Map<String, String> patchW2hOptions = new HashMap<>();
            patchH2wOptions.put("peer", "patch-h2w");

            provisionSingleBridgePortInterface(ovsdbClient, patchH2w, bridgeNameVifInterfaces, "patch", patchH2wOptions,
                    provisionedInterfaces, provisionedPorts, provisionedBridges);
            provisionSingleBridgePortInterface(ovsdbClient, patchW2h, defaultWanInterfaceType, "patch", patchW2hOptions,
                    provisionedInterfaces, provisionedPorts, provisionedBridges);
            provisionSingleBridgePortInterface(ovsdbClient, defaultRadio0, bridgeNameVifInterfaces, "vif", null,
                    provisionedInterfaces, provisionedPorts, provisionedBridges);
            provisionSingleBridgePortInterface(ovsdbClient, defaultRadio1, bridgeNameVifInterfaces, "vif", null,
                    provisionedInterfaces, provisionedPorts, provisionedBridges);
            provisionSingleBridgePortInterface(ovsdbClient, defaultRadio2, bridgeNameVifInterfaces, "vif", null,
                    provisionedInterfaces, provisionedPorts, provisionedBridges);

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

        String country = opensyncAPConfig.getCountryCode(); // should be the
                                                            // same for all
                                                            // radios on this AP
                                                            // ;-)

        ApElementConfiguration apElementConfiguration = (ApElementConfiguration) opensyncAPConfig.getCustomerEquipment()
                .getDetails();

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
            case auto:
                ht_mode = "0";
                break;
            default:
                ht_mode = null;
            }
            elementRadioConfig.getAutoChannelSelection();

            RadioConfiguration radioConfig = apElementConfiguration.getAdvancedRadioMap().get(radioType);
            int beaconInterval = radioConfig.getBeaconInterval();
            boolean enabled = radioConfig.getRadioAdminState().equals(StateSetting.enabled);

            int txPower = 0;
            if (!elementRadioConfig.getEirpTxPower().isAuto()) {
                txPower = elementRadioConfig.getEirpTxPower().getValue();
            }
            String freqBand = null;
            switch (radioType) {
            case is2dot4GHz:
                freqBand = "2.4G";
                break;
            case is5GHz:
                // 802.11h dfs (Dynamic Frequency Selection) aka military and
                // weather radar
                // avoidance protocol
                // Must not be disabled (by law)
                // NA for 2.4GHz
                hwConfig.put("dfs_enable", "1");
                hwConfig.put("dfs_ignorecac", "0");
                hwConfig.put("dfs_usenol", "1");
                freqBand = "5G";

                break;
            case is5GHzL:
                // 802.11h dfs (Dynamic Frequency Selection) aka military and
                // weather radar
                // avoidance protocol
                // Must not be disabled (by law)
                // NA for 2.4GHz
                hwConfig.put("dfs_enable", "1");
                hwConfig.put("dfs_ignorecac", "0");
                hwConfig.put("dfs_usenol", "1");
                freqBand = "5GL";

                break;
            case is5GHzU:
                // 802.11h dfs (Dynamic Frequency Selection) aka military and
                // weather radar
                // avoidance protocol
                // Must not be disabled (by law)
                // NA for 2.4GHz
                hwConfig.put("dfs_enable", "1");
                hwConfig.put("dfs_ignorecac", "0");
                hwConfig.put("dfs_usenol", "1");
                freqBand = "5GU";

                break;
            default: // don't know this interface
                continue;

            }

            if (freqBand != null) {
                try {
                    configureWifiRadios(ovsdbClient, freqBand, channel, hwConfig, country.toUpperCase(), beaconInterval,
                            enabled, ht_mode, txPower);
                } catch (OvsdbClientException e) {
                    LOG.error("ConfigureWifiRadios failed with OvsdbClient exception.", e);
                } catch (TimeoutException e) {
                    LOG.error("ConfigureWifiRadios failed with Timeout.", e);

                } catch (ExecutionException e) {
                    LOG.error("ConfigureWifiRadios excecution failed.", e);

                } catch (InterruptedException e) {
                    LOG.error("ConfigureWifiRadios interrupted.", e);
                }

            }

        }

    }

    public List<OpensyncAPRadioState> getOpensyncAPRadioState(TableUpdates tableUpdates, String apId,
            OvsdbClient ovsdbClient) {

        List<OpensyncAPRadioState> ret = new ArrayList<>();

        try {

            for (Entry<String, TableUpdate> tableUpdate : tableUpdates.getTableUpdates().entrySet()) {

                for (Entry<UUID, RowUpdate> rowUpdate : tableUpdate.getValue().getRowUpdates().entrySet()) {

                    Row row = rowUpdate.getValue().getNew();
                    // Row old = rowUpdate.getOld();

                    if (row != null) {

                        OpensyncAPRadioState tableState = new OpensyncAPRadioState();

                        Map<String, Value> map = row.getColumns();

                        if ((map.get("mac") != null) && map.get("mac").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setMac(row.getStringColumn("mac"));
                        }
                        if ((map.get("channel") != null) && map.get("channel").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setChannel(row.getIntegerColumn("channel").intValue());
                        }
                        if ((map.get("freq_band") != null) && map.get("freq_band").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            String frequencyBand = row.getStringColumn("freq_band");
                            switch (frequencyBand) {
                            case "2.4G":
                                tableState.setFreqBand(RadioType.is2dot4GHz);
                                break;
                            case "5G":
                                tableState.setFreqBand(RadioType.is5GHz);
                                break;
                            case "5GL":
                                tableState.setFreqBand(RadioType.is5GHzL);
                                break;
                            case "5GU":
                                tableState.setFreqBand(RadioType.is5GHzU);
                                break;
                            default:
                                tableState.setFreqBand(RadioType.UNSUPPORTED);
                            }
                        }
                        if ((map.get("if_name") != null) && map.get("if_name").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setIfName(row.getStringColumn("if_name"));
                        }
                        if ((map.get("channel_mode") != null) && map.get("channel_mode").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setChannelMode(row.getStringColumn("channel_mode"));
                        }
                        if ((map.get("country") != null) && map.get("country").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setCountry(row.getStringColumn("country").toUpperCase());
                        }
                        if ((map.get("enabled") != null) && map.get("enabled").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setEnabled(row.getBooleanColumn("enabled"));
                        }
                        if ((map.get("ht_mode") != null) && map.get("ht_mode").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setHtMode(row.getStringColumn("ht_mode"));
                        }
                        if ((map.get("tx_power") != null) && map.get("tx_power").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setTxPower(row.getIntegerColumn("tx_power").intValue());
                        }
                        if ((map.get("hw_config") != null) && map.get("hw_config").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Map.class)) {
                            tableState.setHwConfig(row.getMapColumn("hw_config"));
                        }
                        if ((map.get("_version") != null) && map.get("_version").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setVersion(row.getUuidColumn("_version"));
                        }
                        if ((map.get("_uuid") != null) && map.get("_uuid").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setVersion(row.getUuidColumn("_uuid"));
                        }
                        if (map.get("allowed_channels") != null) {

                            Set<Long> allowedChannels = getSet(row, "allowed_channels");

                            Set<Integer> allowed = new HashSet<>();
                            for (Long channel : allowedChannels) {
                                allowed.add(channel.intValue());
                            }
                            tableState.setAllowedChannels(allowed);
                        }

                        Set<Uuid> vifStates = row.getSetColumn("vif_states");
                        List<OpensyncAPVIFState> vifStatesList = new ArrayList<>();
                        if (vifStates != null) {
                            for (Uuid uuid : vifStates) {
                                getWifiVIFsForRadioByUuid(ovsdbClient, vifStatesList, uuid);
                            }
                        }

                        tableState.setVifStates(vifStatesList);

                        ret.add(tableState);
                    }
                }
            }

            ret.stream().forEach(new Consumer<OpensyncAPRadioState>() {
                @Override
                public void accept(OpensyncAPRadioState wrs) {
                    LOG.debug("Wifi_Radio_State row {}", wrs);
                }
            });

        } catch (Exception e) {
            LOG.error("Could not parse update for Wifi_Radio_State", e);
        }

        return ret;
    }

    public List<OpensyncAPInetState> getOpensyncAPInetState(TableUpdates tableUpdates, String apId,
            OvsdbClient ovsdbClient) {
        List<OpensyncAPInetState> ret = new ArrayList<>();

        try {

            for (TableUpdate tableUpdate : tableUpdates.getTableUpdates().values()) {

                for (RowUpdate rowUpdate : tableUpdate.getRowUpdates().values()) {

                    Row row = rowUpdate.getNew();
                    // Row old = rowUpdate.getOld();

                    if (row != null) {

                        OpensyncAPInetState tableState = new OpensyncAPInetState();
                        Map<String, Value> map = row.getColumns();

                        if ((map.get("NAT") != null) && map.get("NAT").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setNat(row.getBooleanColumn("NAT"));
                        }
                        if ((map.get("enabled") != null) && map.get("enabled").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setEnabled(row.getBooleanColumn("enabled"));
                        }
                        if ((map.get("if_name") != null) && map.get("if_name").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setIfName(row.getStringColumn("if_name"));
                        }
                        if ((map.get("if_type") != null) && map.get("if_type").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setIfType(row.getStringColumn("if_type"));
                        }
                        if ((map.get("ip_assign_scheme") != null) && map.get("ip_assign_scheme").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setIpAssignScheme(row.getStringColumn("ip_assign_scheme"));
                        }
                        if ((map.get("network") != null) && map.get("network").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setNetwork(row.getBooleanColumn("network"));
                        }
                        if ((map.get("hwaddr") != null) && map.get("hwaddr").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setHwAddr(row.getStringColumn("hwaddr"));
                        }
                        if ((map.get("_version") != null) && map.get("_version").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setVersion(row.getUuidColumn("_version"));
                        }
                        if ((map.get("_uuid") != null) && map.get("_uuid").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setVersion(row.getUuidColumn("_uuid"));
                        }
                        ret.add(tableState);
                    }

                }
            }

            ret.stream().forEach(new Consumer<OpensyncAPInetState>() {
                @Override
                public void accept(OpensyncAPInetState wrs) {
                    LOG.debug("Wifi_Inet_State row {}", wrs);
                }
            });

        } catch (Exception e) {
            LOG.error("Could not parse update for Wifi_Inet_State", e);
        }
        return ret;
    }

    public List<OpensyncAPVIFState> getOpensyncAPVIFState(TableUpdates tableUpdates, String apId,
            OvsdbClient ovsdbClient) {
        List<OpensyncAPVIFState> ret = new ArrayList<>();
        try {

            for (TableUpdate tableUpdate : tableUpdates.getTableUpdates().values()) {

                for (RowUpdate rowUpdate : tableUpdate.getRowUpdates().values()) {

                    Row row = rowUpdate.getNew();

                    if (rowUpdate.getOld() != null) {
                        LOG.debug("Wifi_VIF_State Columns changed {}", rowUpdate.getOld().getColumns().keySet());
                    }

                    if (row != null) {

                        OpensyncAPVIFState tableState = processWifiVIFStateColumn(ovsdbClient, row,
                                (rowUpdate.getOld() != null));

                        LOG.debug("Updated table state {}", tableState.toPrettyString());

                        ret.add(tableState);

                    }

                }
            }

        } catch (Exception e) {
            LOG.error("Could not parse update for Wifi_VIF_State", e);

        }
        return ret;
    }

    public List<OpensyncWifiAssociatedClients> getOpensyncWifiAssociatedClients(TableUpdates tableUpdates, String apId,
            OvsdbClient ovsdbClient) {
        List<OpensyncWifiAssociatedClients> ret = new ArrayList<>();

        try {

            for (TableUpdate tableUpdate : tableUpdates.getTableUpdates().values()) {

                for (RowUpdate rowUpdate : tableUpdate.getRowUpdates().values()) {

                    Row row = rowUpdate.getNew();

                    if (row != null) {

                        OpensyncWifiAssociatedClients tableState = new OpensyncWifiAssociatedClients();
                        Map<String, Value> map = row.getColumns();

                        if ((map.get("mac") != null) && map.get("mac").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setMac(row.getStringColumn("mac"));
                        }
                        if (row.getSetColumn("capabilities") != null) {
                            tableState.setCapabilities(row.getSetColumn("capabilities"));
                        }
                        if ((map.get("state") != null) && map.get("state").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setState(row.getStringColumn("state"));
                        }
                        if ((map.get("_version") != null) && map.get("_version").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setVersion(row.getUuidColumn("_version"));
                        }
                        if ((map.get("_uuid") != null) && map.get("_uuid").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setVersion(row.getUuidColumn("_uuid"));
                        }

                        ret.add(tableState);

                    }
                }
            }
            ret.stream().forEach(new Consumer<OpensyncWifiAssociatedClients>() {
                @Override
                public void accept(OpensyncWifiAssociatedClients wrs) {
                    LOG.debug("Wifi_Associated_Clients row {}", wrs);
                }
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

                        if ((map.get("model") != null) && map.get("model").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setModel(row.getStringColumn("model"));
                        }
                        if ((map.get("sku_number") != null) && map.get("sku_number").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setSkuNumber(row.getStringColumn("sku_number"));
                        }
                        if ((map.get("id") != null) && map.get("id").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setId(row.getStringColumn("id"));
                        }

                        if (map.get("version_matrix") != null) {
                            tableState.setVersionMatrix(row.getMapColumn("version_matrix"));
                        }
                        if ((map.get("firmware_version") != null) && map.get("firmware_version").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setFirmwareVersion(row.getStringColumn("firmware_version"));
                        }
                        if ((map.get("firmware_url") != null) && map.get("firmware_url").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setFirmwareUrl(row.getStringColumn("firmware_url"));
                        }

                        if ((map.get("_uuid") != null) && map.get("_uuid").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setVersion(row.getUuidColumn("_uuid"));
                        }
                        if ((map.get("upgrade_dl_timer") != null) && map.get("upgrade_dl_timer").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setUpgradeDlTimer(row.getIntegerColumn("upgrade_dl_timer").intValue());
                        }
                        if ((map.get("platform_version") != null) && map.get("platform_version").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setPlatformVersion(row.getStringColumn("platform_version"));
                        }
                        if ((map.get("firmware_pass") != null) && map.get("firmware_pass").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setFirmwarePass(row.getStringColumn("firmware_pass"));
                        }
                        if ((map.get("upgrade_timer") != null) && map.get("upgrade_timer").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setUpgradeTimer(row.getIntegerColumn("upgrade_timer").intValue());
                        }
                        if ((map.get("max_backoff") != null) && map.get("max_backoff").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setMaxBackoff(row.getIntegerColumn("max_backoff").intValue());
                        }
                        if (map.get("led_config") != null) {
                            tableState.setLedConfig(row.getMapColumn("led_config"));
                        }
                        if ((map.get("redirector_addr") != null) && map.get("redirector_addr").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setRedirectorAddr(row.getStringColumn("redirector_addr"));
                        }
                        if ((map.get("serial_number") != null) && map.get("serial_number").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setSerialNumber(row.getStringColumn("serial_number"));
                        }
                        if ((map.get("_version") != null) && map.get("_version").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setVersion(row.getUuidColumn("_version"));
                        }

                        tableState.setUpgradeStatus(row.getIntegerColumn("upgrade_status").intValue());

                        if ((map.get("device_mode") != null) && map.get("device_mode").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setDeviceMode(row.getStringColumn("device_mode"));
                        }
                        if ((map.get("min_backoff") != null) && map.get("min_backoff").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setMinBackoff(row.getIntegerColumn("min_backoff").intValue());
                        }

                        if ((map.get("revision") != null) && map.get("revision").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setRevision(row.getStringColumn("revision"));
                        }
                        if ((map.get("manager_addr") != null) && map.get("manager_addr").getClass()
                                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                            tableState.setManagerAddr(row.getStringColumn("manager_addr"));
                        }
                        if ((map.get("factory_reset") != null) && map.get("factory_reset").getClass()
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

    public void configureWifiRadios(OvsdbClient ovsdbClient, String freqBand, int channel, Map<String, String> hwConfig,
            String country, int beaconInterval, boolean enabled, String ht_mode, int txPower)
            throws OvsdbClientException, TimeoutException, ExecutionException, InterruptedException {

        List<Operation> operations = new ArrayList<>();
        Map<String, Value> updateColumns = new HashMap<>();
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition("freq_band", Function.EQUALS, new Atom<>(freqBand)));

        updateColumns.put("channel", new Atom<>(channel));
        updateColumns.put("country", new Atom<>(country));
        @SuppressWarnings("unchecked")
        com.vmware.ovsdb.protocol.operation.notation.Map<String, String> hwConfigMap = com.vmware.ovsdb.protocol.operation.notation.Map
                .of(hwConfig);
        updateColumns.put("hw_config", hwConfigMap);
        updateColumns.put("bcn_int", new Atom<>(beaconInterval));
        updateColumns.put("enabled", new Atom<>(enabled));
        if ((ht_mode != null) && !ht_mode.equals("0")) {
            updateColumns.put("ht_mode", new Atom<>(ht_mode));
        } else {
            updateColumns.put("ht_mode", new com.vmware.ovsdb.protocol.operation.notation.Set());
        }
        if (txPower > 0) {
            updateColumns.put("tx_power", new Atom<>(txPower));
        } else {
            updateColumns.put("tx_power", new com.vmware.ovsdb.protocol.operation.notation.Set());
        }

        Row row = new Row(updateColumns);
        operations.add(new Update(wifiRadioConfigDbTable, conditions, row));

        CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
        OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

        LOG.debug("Provisioned channel {} for {}", channel, freqBand);

        for (OperationResult res : result) {
            LOG.debug("Op Result {}", res);
        }
    }

    public void configureSingleSsid(OvsdbClient ovsdbClient, String ifName, String ssid, boolean ssidBroadcast,
            Map<String, String> security, String radioFreqBand, int vlanId, boolean rrmEnabled, boolean enable80211r,
            boolean enable80211v, String minHwMode, boolean enabled, int keyRefresh, boolean uapsdEnabled,
            boolean apBridge, NetworkForwardMode networkForwardMode, String gateway, String inet,
            Map<String, String> dns, String ipAssignScheme, List<MacAddress> macBlockList) {

        List<Operation> operations = new ArrayList<>();
        Map<String, Value> updateColumns = new HashMap<>();

        try {
            // If we are doing a NAT SSID, no bridge, else yes
            if (networkForwardMode == NetworkForwardMode.NAT) {
                updateColumns.put("bridge", new Atom<>("lan"));
            } else {
                updateColumns.put("bridge", new Atom<>("wan"));
            }

            if (enable80211v) {
                updateColumns.put("btm", new Atom<>(1));
            } else {
                updateColumns.put("btm", new Atom<>(0));
            }
            updateColumns.put("enabled", new Atom<>(enabled));
            if (enable80211r) {
                updateColumns.put("ft_psk", new Atom<>(1));
                updateColumns.put("ft_mobility_domain", new Atom<>(17911));
            } else {
                updateColumns.put("ft_psk", new Atom<>(0));
                updateColumns.put("ft_mobility_domain", new com.vmware.ovsdb.protocol.operation.notation.Set());
            }
            updateColumns.put("if_name", new Atom<>(ifName));
            updateColumns.put("mode", new Atom<>("ap"));
            updateColumns.put("rrm", new Atom<>(rrmEnabled ? 1 : 0));
            updateColumns.put("ssid", new Atom<>(ssid));
            updateColumns.put("ssid_broadcast", new Atom<>(ssidBroadcast ? "enabled" : "disabled"));
            updateColumns.put("uapsd_enable", new Atom<>(true));
            // updateColumns.put("vif_radio_idx", new
            // Atom<Integer>(vifRadioIdx));
            updateColumns.put("min_hw_mode", new Atom<>(minHwMode));
            if (vlanId > 0) {
                updateColumns.put("vlan_id", new Atom<>(vlanId));
                if (vlanId > 1) {
                    updateColumns.put("bridge", new Atom<>("vlan" + vlanId));
                }
            } else {
                updateColumns.put("vlan_id", new com.vmware.ovsdb.protocol.operation.notation.Set());
            }
            updateColumns.put("group_rekey", new Atom<>(keyRefresh));
            updateColumns.put("uapsd_enable", new Atom<>(uapsdEnabled));
            updateColumns.put("ap_bridge", new Atom<>(apBridge));

            @SuppressWarnings("unchecked")
            com.vmware.ovsdb.protocol.operation.notation.Map<String, String> securityMap = com.vmware.ovsdb.protocol.operation.notation.Map
                    .of(security);
            updateColumns.put("security", securityMap);

            updateBlockList(updateColumns, macBlockList);

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
            updateColumns.clear();
            operations.clear();

            updateVifConfigsSetForRadio(ovsdbClient, ssid, radioFreqBand, operations, updateColumns, vifConfigUuid);

            Map<String, WifiInetConfigInfo> inetConfigs = getProvisionedWifiInetConfigs(ovsdbClient);

            if (inetConfigs.containsKey(ifName)) {
                updateWifiInetConfig(ovsdbClient, vlanId, ifName, enabled, networkForwardMode == NetworkForwardMode.NAT,
                        "vif", gateway, inet, dns, ipAssignScheme, vifConfigUuid);
            } else {
                LOG.debug("No corresponding WifiInetConfig for this Interface");
                insertWifiInetConfigForVif(ovsdbClient, vlanId, ifName, enabled,
                        networkForwardMode == NetworkForwardMode.NAT, "vif", gateway, inet, dns, ipAssignScheme,
                        vifConfigUuid);
            }

            LOG.info("Provisioned SSID {} on interface {} / {}", ssid, ifName, radioFreqBand);

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in configureSingleSsid", e);
            throw new RuntimeException(e);
        }
    }

    private void updateBlockList(Map<String, Value> updateColumns, List<MacAddress> macBlockList) {

        if (macBlockList != null && !macBlockList.isEmpty()) {
            updateColumns.put("mac_list_type", new Atom<>("blacklist"));
            Set<Atom<String>> atomMacList = new HashSet<>();
            for (MacAddress mac : macBlockList) {
                atomMacList.add(new Atom<>(mac.getAddressAsString()));
            }
            com.vmware.ovsdb.protocol.operation.notation.Set macListSet = com.vmware.ovsdb.protocol.operation.notation.Set
                    .of(atomMacList);
            updateColumns.put("mac_list", macListSet);
        } else {
            updateColumns.put("mac_list_type", new Atom<>("none"));
            updateColumns.put("mac_list", new com.vmware.ovsdb.protocol.operation.notation.Set());
        }
    }

    public void configureBlockList(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig,
            List<MacAddress> macBlockList) {

        LOG.debug("Starting configureBlockList {}", macBlockList);

        List<Operation> operations = new ArrayList<>();
        Map<String, Value> updateColumns = new HashMap<>();

        try {
            updateBlockList(updateColumns, macBlockList);

            Row row = new Row(updateColumns);
            List<Condition> conditions = new ArrayList<>(); // No condition,
                                                            // apply all ssid
            operations.add(new Update(wifiVifConfigDbTable, conditions, row));

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            for (OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }

            LOG.debug("Provisioned blockList {}", macBlockList);

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in configureSingleSsid", e);
            throw new RuntimeException(e);
        }
    }

    private void updateVifConfigsSetForRadio(OvsdbClient ovsdbClient, String ssid, String radioFreqBand,
            List<Operation> operations, Map<String, Value> updateColumns, Uuid vifConfigUuid)
            throws OvsdbClientException, InterruptedException, ExecutionException, TimeoutException {
        Row row;
        CompletableFuture<OperationResult[]> fResult;
        OperationResult[] result;
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition("freq_band", Function.EQUALS, new Atom<>(radioFreqBand)));

        List<String> columns = new ArrayList<>();
        columns.add("vif_configs");
        operations.add(new Select(wifiRadioConfigDbTable, conditions, columns));
        fResult = ovsdbClient.transact(ovsdbName, operations);
        result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

        Set<Uuid> vifConfigsSet = new HashSet<>();

        if ((result != null) && (result.length > 0) && !((SelectResult) result[0]).getRows().isEmpty()) {
            row = ((SelectResult) result[0]).getRows().iterator().next();
            if (row != null) {
                vifConfigsSet = row.getSetColumn("vif_configs");
                if (vifConfigsSet == null) {
                    vifConfigsSet = new HashSet<>();
                }
            }
        }

        if (vifConfigUuid != null) {
            vifConfigsSet.add(vifConfigUuid);
        }

        com.vmware.ovsdb.protocol.operation.notation.Set vifConfigs = com.vmware.ovsdb.protocol.operation.notation.Set
                .of(vifConfigsSet);

        updateColumns.put("vif_configs", vifConfigs);

        row = new Row(updateColumns);
        operations.add(new Update(wifiRadioConfigDbTable, conditions, row));

        LOG.debug("Sending batch of operations : {} ", operations);

        fResult = ovsdbClient.transact(ovsdbName, operations);
        result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Updated WifiRadioConfig {} for SSID {}:", radioFreqBand, ssid);

            for (OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }
        }
    }

    public void configureSsids(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig) {

        boolean rrmEnabled = false;
        if ((opensyncApConfig.getEquipmentLocation() != null)
                && (opensyncApConfig.getEquipmentLocation().getDetails() != null)) {
            rrmEnabled = opensyncApConfig.getEquipmentLocation().getDetails().isRrmEnabled();
        }
        List<MacAddress> macBlockList = opensyncApConfig.getBlockedClients();
        LOG.debug("configureSsids with blockList {}", macBlockList);

        List<RadioType> enabledRadiosFromAp = new ArrayList<>();
        getEnabledRadios(ovsdbClient, enabledRadiosFromAp);

        for (Profile ssidProfile : opensyncApConfig.getSsidProfile()) {

            SsidConfiguration ssidConfig = (SsidConfiguration) ssidProfile.getDetails();
            ApElementConfiguration apElementConfig = (ApElementConfiguration) opensyncApConfig.getCustomerEquipment()
                    .getDetails();
            for (RadioType radioType : ssidConfig.getAppliedRadios()) {
                // Still put profiles on disabled radios for now.
                //
                // if (!enabledRadiosFromAp.contains(radioType)) {
                // // Not on this AP
                // LOG.debug(
                // "AP {} does not have a radio where frequency band is {}.
                // Cannot provision this radio profile on AP.",
                // opensyncApConfig.getCustomerEquipment().getInventoryId(),
                // radioType);
                // continue;
                // }

                Map<String, WifiRadioConfigInfo> provisionedRadioConfigs = getProvisionedWifiRadioConfigs(ovsdbClient);
                String freqBand = null;
                String ifName = null;
                String radioName = null;
                for (Entry<String, WifiRadioConfigInfo> entry : provisionedRadioConfigs.entrySet()) {
                    if (radioType == RadioType.is2dot4GHz && entry.getValue().freqBand.equals("2.4G")) {
                        freqBand = "2.4G";
                        radioName = entry.getKey();
                        break;
                    } else if (radioType == RadioType.is5GHzL && entry.getValue().freqBand.equals("5GL")) {
                        freqBand = "5GL";
                        radioName = entry.getKey();
                        break;
                    } else if (radioType == RadioType.is5GHzU && entry.getValue().freqBand.equals("5GU")) {
                        freqBand = "5GU";
                        radioName = entry.getKey();
                        break;
                    } else if (radioType == RadioType.is5GHz && entry.getValue().freqBand.equals("5G")) {
                        freqBand = "5G";
                        radioName = entry.getKey();
                        break;
                    }
                }
                if (radioName == null || freqBand == null) {
                    LOG.debug("Cannot provision SSID with radio if_name {} and freqBand {}", radioName, freqBand);
                    continue;
                }
                if (radioName.equals(radio0)) {
                    ifName = defaultRadio0;
                } else if (radioName.equals(radio1)) {
                    ifName = defaultRadio1;
                } else if (radioName.equals(radio2)) {
                    ifName = defaultRadio2;
                }
                if (ifName == null) {
                    LOG.debug("Cannot provision SSID for radio {} freqBand {} with VIF if_name {}", radioName, freqBand,
                            ifName);
                    continue;
                }

                int keyRefresh = ssidConfig.getKeyRefresh();

                boolean ssidBroadcast = ssidConfig.getBroadcastSsid() == StateSetting.enabled;

                String ipAssignScheme = "none";
                // the following 5 attributes only applicable to static config,
                // else they are
                // ignored
                String gateway = null;
                String inet = null;
                Map<String, String> dns = null;
                if (apElementConfig.getStaticIP() != null) {
                    ipAssignScheme = "static";
                    inet = apElementConfig.getStaticIP().getHostAddress();
                    gateway = apElementConfig.getStaticIpGw().getHostAddress();
                    dns = new HashMap<>();
                    dns.put(apElementConfig.getStaticDnsIp1().getHostName(),
                            apElementConfig.getStaticDnsIp1().getHostAddress());
                    dns.put(apElementConfig.getStaticDnsIp2().getHostName(),
                            apElementConfig.getStaticDnsIp2().getHostAddress());
                } else if (apElementConfig.getGettingIP().equals(GettingIP.dhcp)
                        || apElementConfig.getGettingDNS().equals(GettingDNS.dhcp)) {
                    ipAssignScheme = "dhcp";
                }

                RadioConfiguration radioConfiguration = apElementConfig.getAdvancedRadioMap().get(radioType);
                if (radioConfiguration == null) {
                    continue; // don't have a radio of this kind in the map
                }

                RadioMode radioMode = radioConfiguration.getRadioMode();
                String minHwMode = "11n"; // min_hw_mode is 11ac, wifi 5, we can
                // also take ++ (11ax) but 2.4GHz only
                // Wifi4 --
                if (!radioType.equals(RadioType.is2dot4GHz)) {
                    minHwMode = "11ac";
                }
                if (!radioType.equals(RadioType.is2dot4GHz) && radioMode.equals(RadioMode.modeX)) {
                    minHwMode = "11x";
                }

                boolean uapsdEnabled = radioConfiguration.getUapsdState() == StateSetting.enabled;

                boolean apBridge = radioConfiguration.getStationIsolation() == StateSetting.enabled; // stationIsolation
                // off by default
                boolean enable80211r = false;
                // on by default
                boolean enable80211v = true;

                if (ssidConfig.getRadioBasedConfigs() != null) {
                    if (ssidConfig.getRadioBasedConfigs().containsKey(radioType)
                            && (ssidConfig.getRadioBasedConfigs().get(radioType) != null)) {
                        if (ssidConfig.getRadioBasedConfigs().get(radioType).getEnable80211r() != null) {
                            enable80211r = ssidConfig.getRadioBasedConfigs().get(radioType).getEnable80211r();
                        }
                        if (ssidConfig.getRadioBasedConfigs().get(radioType).getEnable80211v() != null) {
                            enable80211v = ssidConfig.getRadioBasedConfigs().get(radioType).getEnable80211v();
                        }
                    }
                }

                Map<String, String> security = new HashMap<>();
                String ssidSecurityMode = ssidConfig.getSecureMode().name();
                String opensyncSecurityMode = "OPEN";

                if (ssidSecurityMode.equals("wpaPSK") || ssidSecurityMode.equals("wpa2PSK")
                        || ssidSecurityMode.equals("wpa2OnlyPSK")) {
                    opensyncSecurityMode = "WPA-PSK";
                } else if (ssidSecurityMode.equals("wep")) {
                    opensyncSecurityMode = "WEP";
                } else if (ssidSecurityMode.equals("wpaEAP") || ssidSecurityMode.equals("wpa2EAP")
                        || ssidSecurityMode.equals("wpa2OnlyEAP")) {
                    opensyncSecurityMode = "WPA-EAP";
                }

                security.put("encryption", opensyncSecurityMode);
                // key and mode is N/A for OPEN security
                if (!opensyncSecurityMode.equals("OPEN")) {
                    if (ssidSecurityMode.equals("wpa2PSK")) {
                        security.put("key", ssidConfig.getKeyStr());
                        security.put("mode", "mixed");
                    } else if (ssidSecurityMode.equals("wpa2OnlyPSK")) {
                        security.put("key", ssidConfig.getKeyStr());
                        security.put("mode", "2");
                    } else if (ssidSecurityMode.equals("wpaPSK")) {
                        security.put("key", ssidConfig.getKeyStr());
                        security.put("mode", "1");
                    } else if (ssidSecurityMode.equals("wpa2OnlyEAP")) {
                        security.put("mode", "2");
                        getRadiusConfiguration(opensyncApConfig, ssidConfig, security);
                    } else if (ssidSecurityMode.equals("wpa2EAP")) {
                        security.put("mode", "mixed");
                        getRadiusConfiguration(opensyncApConfig, ssidConfig, security);
                    } else if (ssidSecurityMode.equals("wpaEAP")) {
                        security.put("mode", "1");
                        getRadiusConfiguration(opensyncApConfig, ssidConfig, security);
                    } else if (ssidSecurityMode.equals("wep")) {
                        security.put("key", ssidConfig.getKeyStr());
                        security.put("mode", "1");
                    }
                }

                boolean enabled = ssidConfig.getSsidAdminState().equals(StateSetting.enabled);

                int numberOfInterfaces = 0;
                for (String key : getProvisionedWifiVifConfigs(ovsdbClient).keySet()) {
                    if (key.startsWith(ifName)) {
                        numberOfInterfaces++;
                    }
                }

                try {

                    if (numberOfInterfaces >= maxInterfacesPerRadio) {
                        // this cannot occur, log error, do not try to provision
                        throw new IllegalStateException(
                                "Cannot provision more than " + maxInterfacesPerRadio + " interfaces per Wifi Radio");
                    }
                    if (numberOfInterfaces > 0) {
                        // 1st interface has no number, 2nd has '-1', 3rd has
                        // '-2' etc.
                        ifName = ifName + "-" + numberOfInterfaces;
                    }

                    configureSingleSsid(ovsdbClient, ifName, ssidConfig.getSsid(), ssidBroadcast, security, freqBand,
                            ssidConfig.getVlanId(), rrmEnabled, enable80211r, enable80211v, minHwMode, enabled,
                            keyRefresh, uapsdEnabled, apBridge, ssidConfig.getForwardMode(), gateway, inet, dns,
                            ipAssignScheme, macBlockList);

                } catch (IllegalStateException e) {
                    // could not provision this SSID, but still can go on
                    LOG.warn("could not provision SSID {} on {}", ssidConfig.getSsid(), freqBand);
                }

            }
        }

    }

    private void getRadiusConfiguration(OpensyncAPConfig opensyncApConfig, SsidConfiguration ssidConfig,
            Map<String, String> security) {
        List<Profile> radiusServiceList = new ArrayList<>();
        radiusServiceList = opensyncApConfig.getRadiusProfiles().stream().filter(new Predicate<Profile>() {
            @Override
            public boolean test(Profile p) {
                return p.getName().equals((ssidConfig.getRadiusServiceName()));
            }
        }).collect(Collectors.toList());
        if (!radiusServiceList.isEmpty()) {
            Profile profileRadius = radiusServiceList.get(0);
            String region = opensyncApConfig.getEquipmentLocation().getName();
            List<RadiusServer> radiusServerList = new ArrayList<>();
            RadiusProfile radiusProfileDetails = ((RadiusProfile) profileRadius.getDetails());
            RadiusServiceRegion radiusServiceRegion = radiusProfileDetails.findServiceRegion(region);
            radiusServerList = radiusServiceRegion.findServerConfiguration(ssidConfig.getRadiusServiceName());
            if (!radiusServerList.isEmpty()) {
                RadiusServer rServer = radiusServerList.get(0);
                security.put("radius_server_ip", rServer.getIpAddress().getHostAddress());
                security.put("radius_server_port", String.valueOf(rServer.getAuthPort()));
                security.put("radius_server_secret", rServer.getSecret());
            }
        }
    }

    private void updateWifiInetConfig(OvsdbClient ovsdbClient, int vlanId, String ifName, boolean enabled,
            boolean isNAT, String ifType, String gateway, String inet, Map<String, String> dns, String ipAssignScheme,
            Uuid vifConfigUuid) {

        List<Operation> operations = new ArrayList<>();
        Map<String, Value> updateColumns = new HashMap<>();
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition("if_name", Function.EQUALS, new Atom<>(ifName)));

        try {
            /// usr/plume/tools/ovsh i Wifi_Inet_Config NAT:=false enabled:=true
            /// if_name:=home-ap-24 if_type:=vif ip_assign_scheme:=none
            /// network:=true
            // dhcpd
            updateColumns.put("if_name", new Atom<>(ifName));
            updateColumns.put("if_type", new Atom<>(ifType));
            updateColumns.put("if_uuid", new Atom<>(vifConfigUuid.toString()));
            updateColumns.put("enabled", new Atom<>(enabled));
            updateColumns.put("NAT", new Atom<>(isNAT));

            // mtu // specified in interface, should take that value when
            // implemented
            updateColumns.put("mtu", new Atom<>(1500));
            updateColumns.put("network", new Atom<>(true));

            updateColumns.put("ip_assign_scheme", new Atom<>(ipAssignScheme));

            if (ipAssignScheme.equals("static")) {
                updateColumns.put("dns", com.vmware.ovsdb.protocol.operation.notation.Map.of(dns));
                updateColumns.put("inet_addr", new Atom<>(inet));
                updateColumns.put("gateway", new Atom<>(gateway));
                // netmask
                // broadcast
            }
            if (ipAssignScheme.equals("dhcp")) {
                updateColumns.put("dhcp_sniff", new Atom<>(true));
            } else {
                updateColumns.put("dhcp_sniff", new Atom<>(false));
            }

            if (ifType.equals("vlan")) {
                updateColumns.put("vlan_id", new Atom<>(vlanId));
            } else {
                updateColumns.put("vlan_id", new com.vmware.ovsdb.protocol.operation.notation.Set());
            }

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

    private void insertWifiInetConfigForVif(OvsdbClient ovsdbClient, int vlanId, String ifName, boolean enabled,
            boolean isNAT, String ifType, String gateway, String inet, Map<String, String> dns, String ipAssignScheme,
            Uuid vifConfigUuid) {

        List<Operation> operations = new ArrayList<>();
        Map<String, Value> updateColumns = new HashMap<>();

        try {
            /// usr/plume/tools/ovsh i Wifi_Inet_Config NAT:=false enabled:=true
            /// if_name:=home-ap-24 if_type:=vif ip_assign_scheme:=none
            /// network:=true
            // dhcpd
            updateColumns.put("if_name", new Atom<>(ifName));
            updateColumns.put("if_type", new Atom<>(ifType));
            updateColumns.put("if_uuid", new Atom<>(vifConfigUuid.toString()));
            updateColumns.put("enabled", new Atom<>(enabled));
            updateColumns.put("NAT", new Atom<>(isNAT));

            // mtu // specified in interface, should take that value when
            // implemented
            updateColumns.put("mtu", new Atom<>(1500));
            updateColumns.put("network", new Atom<>(true));

            updateColumns.put("ip_assign_scheme", new Atom<>(ipAssignScheme));

            if (ipAssignScheme.equals("static")) {
                updateColumns.put("dns", com.vmware.ovsdb.protocol.operation.notation.Map.of(dns));
                updateColumns.put("inet_addr", new Atom<>(inet));
                updateColumns.put("gateway", new Atom<>(gateway));
                // netmask
                // broadcast
            }
            if (ipAssignScheme.equals("dhcp")) {
                updateColumns.put("dhcp_sniff", new Atom<>(true));
            } else {
                updateColumns.put("dhcp_sniff", new Atom<>(false));
            }

            if (ifType.equals("vlan")) {
                updateColumns.put("vlan_id", new Atom<>(vlanId));
            } else {
                updateColumns.put("vlan_id", new com.vmware.ovsdb.protocol.operation.notation.Set());
            }

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

    @Deprecated
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

    public void configureStats(OvsdbClient ovsdbClient) {

        try {
            List<Operation> operations = new ArrayList<>();
            Map<String, Integer> thresholdMap = new HashMap<>();
            thresholdMap.put("max_delay", 600);
            thresholdMap.put("util", 10);

            @SuppressWarnings("unchecked")
            com.vmware.ovsdb.protocol.operation.notation.Map<String, Integer> thresholds = com.vmware.ovsdb.protocol.operation.notation.Map
                    .of(thresholdMap);

            Map<String, WifiRadioConfigInfo> radioConfigs = getProvisionedWifiRadioConfigs(ovsdbClient);

            provisionWifiStatsConfigSurvey(getAllowedChannels(ovsdbClient), radioConfigs,
                    getProvisionedWifiStatsConfigs(ovsdbClient), operations, thresholds);

            provisionWifiStatsConfigNeighbor(getAllowedChannels(ovsdbClient), radioConfigs,
                    getProvisionedWifiStatsConfigs(ovsdbClient), operations);

            provisionWifiStatsConfigClient(radioConfigs, getProvisionedWifiStatsConfigs(ovsdbClient), operations);

            if (!operations.isEmpty()) {
                LOG.debug("Sending batch of operations : {} ", operations);

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

    private void provisionWifiStatsConfigNeighbor(Map<String, Set<Integer>> allowedChannels,
            Map<String, WifiRadioConfigInfo> radioConfigs, Map<String, WifiStatsConfigInfo> provisionedWifiStatsConfigs,
            List<Operation> operations) {

        radioConfigs.values().stream().forEach(new Consumer<WifiRadioConfigInfo>() {
            @Override
            public void accept(WifiRadioConfigInfo rc) {
                if (!provisionedWifiStatsConfigs.containsKey(rc.freqBand + "_neighbor_off-chan")) {
                    //
                    Map<String, Value> rowColumns = new HashMap<>();
                    com.vmware.ovsdb.protocol.operation.notation.Set channels = com.vmware.ovsdb.protocol.operation.notation.Set
                            .of(allowedChannels.get(rc.freqBand));
                    if (channels == null) {
                        channels = com.vmware.ovsdb.protocol.operation.notation.Set.of(Collections.emptySet());
                    }
                    rowColumns.put("channel_list", channels);

                    rowColumns.put("radio_type", new Atom<>(rc.freqBand));
                    rowColumns.put("reporting_interval", new Atom<>(60));
                    rowColumns.put("stats_type", new Atom<>("neighbor"));
                    rowColumns.put("survey_type", new Atom<>("off-chan"));

                    Row updateRow = new Row(rowColumns);
                    operations.add(new Insert(wifiStatsConfigDbTable, updateRow));

                }
            }
        });

        radioConfigs.values().stream().forEach(new Consumer<WifiRadioConfigInfo>() {
            @Override
            public void accept(WifiRadioConfigInfo rc) {
                if (!provisionedWifiStatsConfigs.containsKey(rc.freqBand + "_neighbor_on-chan")) {
                    //
                    Map<String, Value> rowColumns = new HashMap<>();
                    rowColumns.put("radio_type", new Atom<>(rc.freqBand));
                    rowColumns.put("reporting_interval", new Atom<>(60));
                    rowColumns.put("stats_type", new Atom<>("neighbor"));
                    rowColumns.put("survey_type", new Atom<>("on-chan"));

                    Row updateRow = new Row(rowColumns);
                    operations.add(new Insert(wifiStatsConfigDbTable, updateRow));

                }
            }
        });

    }

    private void provisionWifiStatsConfigSurvey(Map<String, Set<Integer>> allowedChannels,
            Map<String, WifiRadioConfigInfo> radioConfigs, Map<String, WifiStatsConfigInfo> provisionedWifiStatsConfigs,
            List<Operation> operations, com.vmware.ovsdb.protocol.operation.notation.Map<String, Integer> thresholds) {

        radioConfigs.values().stream().forEach(new Consumer<WifiRadioConfigInfo>() {
            @Override
            public void accept(WifiRadioConfigInfo rc) {
                if (!provisionedWifiStatsConfigs.containsKey(rc.freqBand + "_survey_on-chan")) {

                    Map<String, Value> rowColumns = new HashMap<>();
                    rowColumns.put("radio_type", new Atom<>(rc.freqBand));
                    rowColumns.put("reporting_interval", new Atom<>(60));
                    rowColumns.put("report_type", new Atom<>("raw"));
                    rowColumns.put("sampling_interval", new Atom<>(6));
                    rowColumns.put("stats_type", new Atom<>("survey"));
                    rowColumns.put("survey_interval_ms", new Atom<>(65));
                    rowColumns.put("survey_type", new Atom<>("on-chan"));

                    Row updateRow = new Row(rowColumns);
                    operations.add(new Insert(wifiStatsConfigDbTable, updateRow));

                }
            }
        });

        radioConfigs.values().stream().forEach(new Consumer<WifiRadioConfigInfo>() {
            @Override
            public void accept(WifiRadioConfigInfo rc) {
                if (!provisionedWifiStatsConfigs.containsKey(rc.freqBand + "_survey_off-chan")) {
                    //
                    Map<String, Value> rowColumns = new HashMap<>();
                    com.vmware.ovsdb.protocol.operation.notation.Set channels = com.vmware.ovsdb.protocol.operation.notation.Set
                            .of(allowedChannels.get(rc.freqBand));
                    if (channels == null) {
                        channels = com.vmware.ovsdb.protocol.operation.notation.Set.of(Collections.emptySet());
                    }
                    rowColumns.put("channel_list", channels);

                    rowColumns.put("radio_type", new Atom<>(rc.freqBand));
                    rowColumns.put("reporting_interval", new Atom<>(60));
                    rowColumns.put("stats_type", new Atom<>("survey"));
                    rowColumns.put("survey_type", new Atom<>("off-chan"));
                    rowColumns.put("report_type", new Atom<>("raw"));
                    rowColumns.put("sampling_interval", new Atom<>(60));
                    rowColumns.put("survey_interval_ms", new Atom<>(65));
                    rowColumns.put("threshold", thresholds);
                    Row updateRow = new Row(rowColumns);
                    operations.add(new Insert(wifiStatsConfigDbTable, updateRow));

                }
            }
        });

    }

    private void provisionWifiStatsConfigClient(Map<String, WifiRadioConfigInfo> radioConfigs,
            Map<String, WifiStatsConfigInfo> provisionedWifiStatsConfigs, List<Operation> operations) {

        radioConfigs.values().stream().forEach(new Consumer<WifiRadioConfigInfo>() {
            @Override
            public void accept(WifiRadioConfigInfo rc) {
                if (!provisionedWifiStatsConfigs.containsKey(rc.freqBand + "_client")) {
                    //
                    Map<String, Value> rowColumns = new HashMap<>();
                    rowColumns.put("radio_type", new Atom<>(rc.freqBand));
                    rowColumns.put("reporting_interval", new Atom<>(60));
                    rowColumns.put("report_type", new Atom<>("raw"));
                    rowColumns.put("sampling_interval", new Atom<>(10));
                    rowColumns.put("stats_type", new Atom<>("client"));
                    rowColumns.put("survey_interval_ms", new Atom<>(65));
                    Row updateRow = new Row(rowColumns);
                    operations.add(new Insert(wifiStatsConfigDbTable, updateRow));

                }
            }
        });

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

    public void configureFirmwareDownload(OvsdbClient ovsdbClient, String apId, String firmwareUrl,
            String firmwareVersion, String username, String validationCode) throws Exception {

        try {
            LOG.debug("configureFirmwareDownload for {} to version {} url {} validationCode {} username {}", apId,
                    firmwareVersion, firmwareUrl, validationCode, username);
            List<Operation> operations = new ArrayList<>();
            Map<String, Value> updateColumns = new HashMap<>();
            updateColumns.put("upgrade_dl_timer", new Atom<>(upgradeDlTimerSeconds));
            updateColumns.put("firmware_pass", new Atom<>(validationCode));
            updateColumns.put("firmware_url", new Atom<>(firmwareUrl));
            updateColumns.put("upgrade_timer", new Atom<>(upgradeTimerSeconds));

            Row row = new Row(updateColumns);
            operations.add(new Update(awlanNodeDbTable, row));

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            for (OperationResult r : result) {
                LOG.debug("Op Result {}", r);

            }

        } catch (Exception e) {
            LOG.error("Could not download firmware {} to AP {}", firmwareVersion, apId, e);
        }

    }

    public void configureFirmwareFlash(OvsdbClient ovsdbClient, String apId, String firmwareVersion, String username) {
        try {
            LOG.debug("configureFirmwareFlash on AP {} to load {} setting timer for {} seconds.", apId, firmwareVersion,
                    upgradeTimerSeconds);

            List<Operation> operations = new ArrayList<>();
            Map<String, Value> updateColumns = new HashMap<>();
            updateColumns.put("upgrade_timer", new Atom<>(upgradeTimerSeconds));

            Row row = new Row(updateColumns);
            operations.add(new Update(awlanNodeDbTable, row));

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);

            OperationResult[] result = fResult.join();
            for (OperationResult r : result) {
                LOG.debug("Op Result {}", r);
            }

        } catch (Exception e) {
            LOG.error("Could not configure timer for flashing firmware {} on AP {}", firmwareVersion, apId, e);
        }
    }

    public void removeAllStatsConfigs(OvsdbClient ovsdbClient) {

        LOG.info("Remove existing Wifi_Stats_Config table entries");
        try {
            List<Operation> operations = new ArrayList<>();

            operations.add(new Delete(wifiStatsConfigDbTable));

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Removed all existing config from {}:", wifiStatsConfigDbTable);

                for (OperationResult res : result) {
                    LOG.debug("Op Result {}", res);
                }
            }

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in removeAllStatsConfigs", e);
            throw new RuntimeException(e);
        }

    }

}
