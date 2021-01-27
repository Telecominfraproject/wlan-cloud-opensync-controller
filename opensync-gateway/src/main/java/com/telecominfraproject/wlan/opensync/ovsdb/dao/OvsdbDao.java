package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.core.model.equipment.AutoOrManualValue;
import com.telecominfraproject.wlan.core.model.equipment.ChannelBandwidth;
import com.telecominfraproject.wlan.core.model.equipment.MacAddress;
import com.telecominfraproject.wlan.core.model.equipment.RadioBestApSettings;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.core.model.equipment.SourceType;
import com.telecominfraproject.wlan.equipment.models.ApElementConfiguration;
import com.telecominfraproject.wlan.equipment.models.ElementRadioConfiguration;
import com.telecominfraproject.wlan.equipment.models.ManagementRate;
import com.telecominfraproject.wlan.equipment.models.MulticastRate;
import com.telecominfraproject.wlan.equipment.models.NetworkForwardMode;
import com.telecominfraproject.wlan.equipment.models.RadioConfiguration;
import com.telecominfraproject.wlan.equipment.models.RadioMode;
import com.telecominfraproject.wlan.equipment.models.StateSetting;
import com.telecominfraproject.wlan.opensync.external.integration.models.ConnectNodeInfo;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPHotspot20Config;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPInetState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPRadioState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPVIFState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAWLANNode;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncWifiAssociatedClients;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.BridgeInfo;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.CommandConfigInfo;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.Hotspot20Config;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.Hotspot20IconConfig;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.Hotspot20OsuProviders;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.InterfaceInfo;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.PortInfo;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.WifiInetConfigInfo;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.WifiRadioConfigInfo;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.WifiStatsConfigInfo;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.WifiVifConfigInfo;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.enumerations.DhcpFpDbStatus;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.enumerations.DhcpFpDeviceType;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.enumerations.DhcpFpManufId;
import com.telecominfraproject.wlan.opensync.util.OvsdbToWlanCloudTypeMappingUtility;
import com.telecominfraproject.wlan.profile.bonjour.models.BonjourGatewayProfile;
import com.telecominfraproject.wlan.profile.bonjour.models.BonjourServiceSet;
import com.telecominfraproject.wlan.profile.captiveportal.models.CaptivePortalAuthenticationType;
import com.telecominfraproject.wlan.profile.captiveportal.models.CaptivePortalConfiguration;
import com.telecominfraproject.wlan.profile.models.Profile;
import com.telecominfraproject.wlan.profile.models.common.ManagedFileInfo;
import com.telecominfraproject.wlan.profile.network.models.ApNetworkConfiguration;
import com.telecominfraproject.wlan.profile.network.models.GreTunnelConfiguration;
import com.telecominfraproject.wlan.profile.passpoint.models.PasspointDuple;
import com.telecominfraproject.wlan.profile.passpoint.models.PasspointIPv4AddressType;
import com.telecominfraproject.wlan.profile.passpoint.models.PasspointIPv6AddressType;
import com.telecominfraproject.wlan.profile.passpoint.models.PasspointMccMnc;
import com.telecominfraproject.wlan.profile.passpoint.models.PasspointProfile;
import com.telecominfraproject.wlan.profile.passpoint.models.operator.PasspointOperatorProfile;
import com.telecominfraproject.wlan.profile.passpoint.models.provider.PasspointEapMethods;
import com.telecominfraproject.wlan.profile.passpoint.models.provider.PasspointNaiRealmEapAuthInnerNonEap;
import com.telecominfraproject.wlan.profile.passpoint.models.provider.PasspointNaiRealmEapAuthParam;
import com.telecominfraproject.wlan.profile.passpoint.models.provider.PasspointNaiRealmEapCredType;
import com.telecominfraproject.wlan.profile.passpoint.models.provider.PasspointOsuIcon;
import com.telecominfraproject.wlan.profile.passpoint.models.provider.PasspointOsuProviderProfile;
import com.telecominfraproject.wlan.profile.passpoint.models.venue.PasspointVenueName;
import com.telecominfraproject.wlan.profile.passpoint.models.venue.PasspointVenueProfile;
import com.telecominfraproject.wlan.profile.passpoint.models.venue.PasspointVenueTypeAssignment;
import com.telecominfraproject.wlan.profile.radius.models.RadiusProfile;
import com.telecominfraproject.wlan.profile.radius.models.RadiusServer;
import com.telecominfraproject.wlan.profile.rf.models.RfConfiguration;
import com.telecominfraproject.wlan.profile.rf.models.RfElementConfiguration;
import com.telecominfraproject.wlan.profile.ssid.models.NasIdType;
import com.telecominfraproject.wlan.profile.ssid.models.NasIpType;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration;
import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.protocol.methods.RowUpdate;
import com.vmware.ovsdb.protocol.methods.TableUpdate;
import com.vmware.ovsdb.protocol.methods.TableUpdates;
import com.vmware.ovsdb.protocol.operation.Delete;
import com.vmware.ovsdb.protocol.operation.Insert;
import com.vmware.ovsdb.protocol.operation.Mutate;
import com.vmware.ovsdb.protocol.operation.Operation;
import com.vmware.ovsdb.protocol.operation.Select;
import com.vmware.ovsdb.protocol.operation.Update;
import com.vmware.ovsdb.protocol.operation.notation.Atom;
import com.vmware.ovsdb.protocol.operation.notation.Condition;
import com.vmware.ovsdb.protocol.operation.notation.Function;
import com.vmware.ovsdb.protocol.operation.notation.Mutation;
import com.vmware.ovsdb.protocol.operation.notation.Mutator;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;
import com.vmware.ovsdb.protocol.operation.notation.Value;
import com.vmware.ovsdb.protocol.operation.result.ErrorResult;
import com.vmware.ovsdb.protocol.operation.result.InsertResult;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.protocol.operation.result.SelectResult;
import com.vmware.ovsdb.protocol.operation.result.UpdateResult;
import com.vmware.ovsdb.protocol.schema.DatabaseSchema;
import com.vmware.ovsdb.service.OvsdbClient;

@Component
public class OvsdbDao {

    private static final int MAX_VIF_PER_FREQ = 8;

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbDao.class);

    public static final String wifiRouteStateDbTable = "Wifi_Route_State";

    public static final String wifiMasterStateDbTable = "Wifi_Master_State";

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.managerAddr:3.88.149.10}")

    private String managerIpAddr;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.region:Ottawa}")
    public String region;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.listenPort:6640}")
    private int ovsdbListenPort;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.externalPort:6640}")
    private int ovsdbExternalPort;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.mqttBroker.address.external:testportal.123wlan.com}")
    private String mqttBrokerAddress;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.mqttBroker.listenPort:1883}")
    private int mqttBrokerListenPort;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.mqttBroker.externalPort:1883}")
    private int mqttBrokerExternalPort;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.timeoutSec:30}")
    private int ovsdbTimeoutSec;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-iface.default_bridge:lan}")
    public String bridgeNameVifInterfaces;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-iface.default_lan_type:bridge}")
    public String defaultLanInterfaceType;
    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-iface.default_lan_name:lan}")
    public String defaultLanInterfaceName;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-iface.default_wan_type:bridge}")
    public String defaultWanInterfaceType;
    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-iface.default_wan_name:wan}")
    public String defaultWanInterfaceName;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-iface.default_radio0:wlan0}")
    public String defaultRadio0;
    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-iface.default_radio1:wlan1}")
    public String defaultRadio1;
    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-iface.default_radio2:wlan2}")
    public String defaultRadio2;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-device.radio0:radio0}")
    public String radio0;
    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-device.radio1:radio1}")
    public String radio1;
    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-device.radio2:radio2}")
    public String radio2;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-iface.max:8}")
    public int maxInterfacesPerRadio;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.awlan-node.upgrade_dl_timer:60}")
    public long upgradeDlTimerSeconds;
    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.awlan-node.upgrade_timer:90}")
    public long upgradeTimerSeconds;
    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.awlan-node.reboot_or_reset_timer:10}")
    public long rebootOrResetTimerSeconds;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.externalFileStoreURL:https://localhost:9096}")
    private String externalFileStoreURL;

    public static final String FILESTORE = "filestore/";
    public static final String HTTP = "http";

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

    public static final String wifiRrmConfigDbTable = "Wifi_RRM_Config";

    public static final String dhcpLeasedIpDbTable = "DHCP_leased_IP";

    public static final String commandConfigDbTable = "Command_Config";

    public static final String commandStateDbTable = "Command_State";
    public static final String hotspot20IconConfigDbTable = "Hotspot20_Icon_Config";

    public static final String hotspot20OsuProvidersDbTable = "Hotspot20_OSU_Providers";

    public static final String hotspot20ConfigDbTable = "Hotspot20_Config";

    public static final String StartDebugEngineApCommand = "startPortForwardingSession";

    public static final String StopDebugEngineApCommand = "stopSession";

    public static void translateDhcpFpValueToString(Entry<String, Value> c, Map<String, String> rowMap) {
        if (c.getKey().equals("manuf_id")) {
            rowMap.put(c.getKey(), DhcpFpManufId.getById(Integer.valueOf(c.getValue().toString())).getName());
        } else if (c.getKey().equals("device_type")) {
            rowMap.put(c.getKey(), DhcpFpDeviceType.getById(Integer.valueOf(c.getValue().toString())).getName());
        } else if (c.getKey().equals("db_status")) {
            rowMap.put(c.getKey(), DhcpFpDbStatus.getById(Integer.valueOf(c.getValue().toString())).getName());
        } else {
            rowMap.put(c.getKey(), c.getValue().toString());
        }
    }

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
            fillInWanIpAddressAndMac(ovsdbClient, ret, defaultWanInterfaceType, defaultWanInterfaceName);
            if ((ret.ipV4Address == null) || (ret.macAddress == null)) {
                // when not found - look them up for if_name = br-lan
                fillInWanIpAddressAndMac(ovsdbClient, ret, defaultLanInterfaceType, defaultLanInterfaceName);

                if (ret.ipV4Address == null)
                    throw new RuntimeException(
                            "Could not get inet address for Lan and Wan network interfaces. Node is not ready to connect.");
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
            conditions.add(new Condition("if_name", Function.EQUALS, new Atom<>(defaultLanInterfaceName)));

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

    public void fillInWanIpAddressAndMac(OvsdbClient ovsdbClient, ConnectNodeInfo connectNodeInfo, String ifType,
            String ifName) {
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
            ConnectNodeInfo incomingConnectNodeInfo, boolean preventCnAlteration) {
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
            String mqttClientName = OvsdbToWlanCloudTypeMappingUtility.getAlteredClientCnIfRequired(clientCn,
                    incomingConnectNodeInfo, preventCnAlteration);
            newMqttSettings.put("topics", "/ap/" + mqttClientName + "/opensync");
            newMqttSettings.put("port", "" + mqttBrokerExternalPort);
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

            LOG.debug("Updated {}:", wifiStatsConfigDbTable);

            for (OperationResult res : result) {
                LOG.debug("Op Result {}", res);
                if (res instanceof InsertResult) {
                    LOG.info("updateDeviceStatsReportingInterval insert new row result {}", ((InsertResult) res));
                    // for insert, make sure it is actually in the table
                    confirmRowExistsInTable(ovsdbClient, ((InsertResult) res).getUuid(), wifiStatsConfigDbTable);
                }
            }

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * @param ovsdbClient
     *
     */
    public void enableNetworkProbeForSyntheticClient(OvsdbClient ovsdbClient) {
        LOG.debug("Enable network_probe service_metrics_collection_config for synthetic client");

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
                    if (res instanceof InsertResult) {
                        LOG.info("enableNetworkProbeForSyntheticClient insert new row result {}", ((InsertResult) res));
                        // for insert, make sure it is actually in the table
                        confirmRowExistsInTable(ovsdbClient, ((InsertResult) res).getUuid(), wifiStatsConfigDbTable);
                    } else if (res instanceof ErrorResult) {
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

            String skuNumber = null;
            String serialNumber = null;
            String model = null;
            String firmwareVersion = null;

            Row row = null;
            if ((result != null) && (result.length > 0) && !((SelectResult) result[0]).getRows().isEmpty()) {
                row = ((SelectResult) result[0]).getRows().iterator().next();
                for (OperationResult res : result) {
                    LOG.debug("Op Result {}", res);
                }
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

            updateColumns.put("manager_addr", new Atom<>("ssl:" + managerIpAddr + ":" + ovsdbExternalPort));

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

    public static <T> T getSingleValueFromSet(Row row, String columnName) {

        Set<T> set = row != null ? row.getSetColumn(columnName) : null;
        T ret = (set != null) && !set.isEmpty() ? set.iterator().next() : null;

        return ret;
    }

    private OpensyncAPVIFState processWifiVIFStateColumn(OvsdbClient ovsdbClient, Row row) {
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

    public Map<String, CommandConfigInfo> getProvisionedCommandConfigs(OvsdbClient ovsdbClient) {
        Map<String, CommandConfigInfo> ret = new HashMap<>();

        List<Operation> operations = new ArrayList<>();
        List<Condition> conditions = new ArrayList<>();
        List<String> columns = new ArrayList<>();

        columns.add("_uuid");
        columns.add("delay");
        columns.add("duration");
        columns.add("command");
        columns.add("payload");
        columns.add("timestamp");

        try {
            LOG.debug("Retrieving CommandConfig:");

            operations.add(new Select(commandConfigDbTable, conditions, columns));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            for (OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }

            for (Row row : ((SelectResult) result[0]).getRows()) {

                CommandConfigInfo commandConfigInfo = new CommandConfigInfo();
                commandConfigInfo.uuid = row.getUuidColumn("_uuid");
                commandConfigInfo.delay = row.getIntegerColumn("delay");
                commandConfigInfo.duration = row.getIntegerColumn("duration");
                commandConfigInfo.command = row.getStringColumn("command");
                commandConfigInfo.payload = row.getMapColumn("payload");
                commandConfigInfo.timestamp = row.getIntegerColumn("timestamp");

                ret.put(commandConfigInfo.command, commandConfigInfo);
            }

            LOG.debug("Retrieved CommandConfig: {}", ret);

        } catch (ExecutionException | InterruptedException | OvsdbClientException | TimeoutException e) {

            LOG.error("Error in getProvisionedCommandConfigs", e);

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

    public List<String> getWifiVifStates(OvsdbClient ovsdbClient, String ssidName) {
        List<String> ret = new ArrayList<>();

        List<Operation> operations = new ArrayList<>();
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition("ssid", Function.EQUALS, new Atom<>(ssidName)));
        List<String> columns = new ArrayList<>();
        columns.add("mac");

        try {
            LOG.debug("Retrieving WifiVifState:");

            operations.add(new Select(wifiVifStateDbTable, conditions, columns));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            for (OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }

            for (Row row : ((SelectResult) result[0]).getRows()) {

                String mac = getSingleValueFromSet(row, "mac");
                if (mac != null) {
                    ret.add(mac);
                }

            }

            LOG.debug("Retrieved WifiVifState: {}", ret);

        } catch (ExecutionException | InterruptedException | OvsdbClientException | TimeoutException e) {
            LOG.error("Error in getWifiVifStates", e);
            throw new RuntimeException(e);
        }

        return ret;
    }

    public Map<String, WifiVifConfigInfo> getProvisionedWifiVifConfigs(OvsdbClient ovsdbClient) {
        Map<String, WifiVifConfigInfo> ret = new HashMap<>();

        List<Operation> operations = new ArrayList<>();
        List<Condition> conditions = new ArrayList<>();

        try {
            LOG.debug("Retrieving WifiVifConfig:");

            operations.add(new Select(wifiVifConfigDbTable, conditions));
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

                Long ftMobilityDomain = getSingleValueFromSet(row, "ft_mobility_domain");
                if (ftMobilityDomain != null) {
                    wifiVifConfigInfo.ftMobilityDomain = ftMobilityDomain.intValue();
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

                if ((row.getColumns().get("mac_list_type") != null) && row.getColumns().get("mac_list_type").getClass()
                        .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                    wifiVifConfigInfo.macListType = row.getStringColumn("mac_list_type");
                }

                wifiVifConfigInfo.customOptions = row.getMapColumn("custom_options");
                wifiVifConfigInfo.captiveAllowlist = row.getSetColumn("captive_allowlist");
                wifiVifConfigInfo.captivePortal = row.getMapColumn("captive_portal");

                Boolean wpsPbc = getSingleValueFromSet(row, "wps_pbc");
                if (wpsPbc != null) {
                    wifiVifConfigInfo.wpsPbc = wpsPbc;
                } else {
                    wifiVifConfigInfo.wpsPbc = false;
                }

                Boolean wps = getSingleValueFromSet(row, "wps");
                if (wps != null) {
                    wifiVifConfigInfo.wps = wps;
                } else {
                    wifiVifConfigInfo.wps = false;
                }

                Boolean wds = getSingleValueFromSet(row, "wds");
                if (wds != null) {
                    wifiVifConfigInfo.wds = wds;
                } else {
                    wifiVifConfigInfo.wds = false;
                }

                wifiVifConfigInfo.wpsPbcKeyId = row.getStringColumn("wps_pbc_key_id");

                Boolean mcast2ucast = getSingleValueFromSet(row, "mcast2ucast");
                if (mcast2ucast != null) {
                    wifiVifConfigInfo.mcast2ucast = mcast2ucast;
                } else {
                    wifiVifConfigInfo.mcast2ucast = false;
                }

                Boolean dynamicBeacon = getSingleValueFromSet(row, "dynamic_beacon");
                if (dynamicBeacon != null) {
                    wifiVifConfigInfo.dynamicBeacon = dynamicBeacon;
                } else {
                    wifiVifConfigInfo.dynamicBeacon = false;
                }

                Long vifDbgLvl = getSingleValueFromSet(row, "vif_dbg_lvl");
                if (vifDbgLvl != null) {
                    wifiVifConfigInfo.vifDbgLvl = vifDbgLvl.intValue();
                } else {
                    wifiVifConfigInfo.vifDbgLvl = 0;
                }

                if (row.getColumns().containsKey("mesh_options")) {
                    wifiVifConfigInfo.meshOptions = row.getMapColumn("mesh_options");
                }

                wifiVifConfigInfo.credentialConfigs = row.getSetColumn("credential_configs");

                String parent = getSingleValueFromSet(row, "parent");
                if (parent != null) {
                    wifiVifConfigInfo.parent = parent;
                }

                String multiAp = getSingleValueFromSet(row, "multi_ap");
                if (multiAp != null) {
                    wifiVifConfigInfo.multiAp = multiAp;
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
        columns.add("gateway");
        columns.add("dns");
        columns.add("dhcpd");

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
                String ipAssignSchemeTemp = getSingleValueFromSet(row, "ip_assign_scheme");
                if (ipAssignSchemeTemp != null) {
                    wifiInetConfigInfo.ipAssignScheme = ipAssignSchemeTemp;
                } else {
                    wifiInetConfigInfo.ipAssignScheme = "none";
                }
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
                wifiInetConfigInfo.dns = row.getMapColumn("dns");
                wifiInetConfigInfo.dhcpd = row.getMapColumn("dhcpd");
                wifiInetConfigInfo.gateway = getSingleValueFromSet(row, "gateway");
                ret.put(wifiInetConfigInfo.ifName, wifiInetConfigInfo);
            }

            LOG.debug("Retrieved WifiInetConfigs: {}", ret);

        } catch (ExecutionException | InterruptedException | OvsdbClientException | TimeoutException e) {
            LOG.error("Error in getProvisionedWifiInetConfigs", e);
            throw new RuntimeException(e);
        }

        return ret;
    }

    public Map<String, Hotspot20Config> getProvisionedHotspot20Configs(OvsdbClient ovsdbClient) {
        Map<String, Hotspot20Config> ret = new HashMap<>();

        List<Operation> operations = new ArrayList<>();
        List<Condition> conditions = new ArrayList<>();
        List<String> columns = new ArrayList<>();
        columns.addAll(Arrays.asList(Hotspot20Config.ovsdbColumns));

        try {
            LOG.debug("Retrieving Hotspot20Config:");

            operations.add(new Select(hotspot20ConfigDbTable, conditions, columns));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            for (OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }

            for (Row row : ((SelectResult) result[0]).getRows()) {
                Hotspot20Config hotspot20Config = new Hotspot20Config(row);
                ret.put(hotspot20Config.osuSsid, hotspot20Config);
            }

            LOG.debug("Retrieved Hotspot20Config: {}", ret);

        } catch (ExecutionException | InterruptedException | OvsdbClientException | TimeoutException e) {
            LOG.error("Error in getWifiPasspointConfigs", e);
            throw new RuntimeException(e);
        }
        return ret;
    }

    public Map<String, Hotspot20OsuProviders> getProvisionedHotspot20OsuProviders(OvsdbClient ovsdbClient) {
        Map<String, Hotspot20OsuProviders> ret = new HashMap<>();
        List<Operation> operations = new ArrayList<>();
        List<Condition> conditions = new ArrayList<>();
        List<String> columns = new ArrayList<>();
        columns.addAll(Arrays.asList(Hotspot20OsuProviders.ovsdbColumns));

        try {
            LOG.debug("Retrieving Hotspot20_OSU_Providers:");

            operations.add(new Select(hotspot20OsuProvidersDbTable, conditions, columns));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            for (OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }

            for (Row row : ((SelectResult) result[0]).getRows()) {
                Hotspot20OsuProviders hotspot20OsuProviders = new Hotspot20OsuProviders(row);
                ret.put(hotspot20OsuProviders.osuProviderName, hotspot20OsuProviders);
            }

            LOG.debug("Retrieved Hotspot20_OSU_Providers: {}", ret);

        } catch (ExecutionException | InterruptedException | OvsdbClientException | TimeoutException e) {
            LOG.error("Error in getWifiOsuProviders", e);
            throw new RuntimeException(e);
        }

        return ret;
    }

    public Map<String, Hotspot20IconConfig> getProvisionedHotspot20IconConfig(OvsdbClient ovsdbClient) {
        Map<String, Hotspot20IconConfig> ret = new HashMap<>();
        List<Operation> operations = new ArrayList<>();
        List<Condition> conditions = new ArrayList<>();
        List<String> columns = new ArrayList<>();
        columns.addAll(Arrays.asList(Hotspot20IconConfig.ovsdbColumns));

        try {
            LOG.debug("Retrieving Hotspot20_Icon_Config:");
            DatabaseSchema schema = ovsdbClient.getSchema(ovsdbName).get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            if (!schema.getTables().get(hotspot20ConfigDbTable).getColumns().containsKey("icon_config_name")) {
                LOG.info("Removed icon_config_name from Hotspot20_Icon_Config columns {}",
                        columns.remove("icon_config_name"));
            }
            operations.add(new Select(hotspot20IconConfigDbTable, conditions, columns));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            for (OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }

            for (Row row : ((SelectResult) result[0]).getRows()) {
                Hotspot20IconConfig hotspot20IconConfig = new Hotspot20IconConfig(row);
                ret.put(hotspot20IconConfig.url, hotspot20IconConfig);
            }

            LOG.debug("Retrieved Hotspot20_Icon_Config: {}", ret);

        } catch (ExecutionException | InterruptedException | OvsdbClientException | TimeoutException e) {
            LOG.error("Error in getProvisionedHotspot20IconConfig", e);
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

    public void removeAllInetConfigs(OvsdbClient ovsdbClient) {
        try {
            Collection<WifiInetConfigInfo> provisionedWifiInetConfigs = getProvisionedWifiInetConfigs(ovsdbClient)
                    .values();
            List<Operation> operations = new ArrayList<>();
            List<Condition> conditions = new ArrayList<>();

            for (WifiInetConfigInfo wifiInetConfigInfo : provisionedWifiInetConfigs) {

                if (wifiInetConfigInfo.vlanId > 1 || wifiInetConfigInfo.ifType.equals("vif")
                        || wifiInetConfigInfo.ifType.equals("gre")) {
                    conditions = new ArrayList<>();
                    conditions.add(new Condition("if_name", Function.EQUALS, new Atom<>(wifiInetConfigInfo.ifName)));
                    operations.add(new Delete(wifiInetConfigDbTable, conditions));
                }
            }
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            LOG.info("Removed all existing vif, vlan, and gre interface configs from {}:", wifiInetConfigDbTable);

            for (OperationResult res : result) {
                LOG.info("Op Result {}", res);
            }

            provisionedWifiInetConfigs = getProvisionedWifiInetConfigs(ovsdbClient).values();

            for (WifiInetConfigInfo inetConfigInfo : provisionedWifiInetConfigs) {
                if (inetConfigInfo.ifType.equals("vif") || inetConfigInfo.ifType.equals("gre")) {
                    throw new RuntimeException(
                            "Failed to remove all vif and gre interface configurations from Wifi_Inet_Config dbTable, still has "
                                    + provisionedWifiInetConfigs.stream().filter(new Predicate<WifiInetConfigInfo>() {

                                        @Override
                                        public boolean test(WifiInetConfigInfo t) {
                                            if ((t.ifType.equals("vif")) || (t.ifType.equals("gre")))
                                                return true;
                                            return false;
                                        }

                                    }).collect(Collectors.toList()));

                }
            }

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in removeAllInetConfigs", e);
            throw new RuntimeException(e);
        }

    }

    public void removeAllSsids(OvsdbClient ovsdbClient) {
        LOG.info("removeAllSsids from {}:", wifiVifConfigDbTable);

        try {

            List<Operation> operations = new ArrayList<>();
            operations.add(new Delete(wifiVifConfigDbTable));

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            for (OperationResult res : result) {
                LOG.info("Op Result {}", res);
                if (res instanceof UpdateResult) {
                    LOG.info("removeAllSsids:result {}", ((UpdateResult) res).toString());
                } else if (res instanceof ErrorResult) {
                    LOG.error("removeAllSsids:result error {}", ((ErrorResult) res));
                    throw new RuntimeException("removeAllSsids " + ((ErrorResult) res).getError() + " "
                            + ((ErrorResult) res).getDetails());
                }
            }

            Map<String, WifiVifConfigInfo> provisionedVifConfigs = getProvisionedWifiVifConfigs(ovsdbClient);
            // this should be empty
            if (!provisionedVifConfigs.isEmpty()) {
                throw new RuntimeException(
                        "Failed to remove all vif configurations from Wifi_VIF_Config dbTable, still has "
                                + provisionedVifConfigs.values());
            }
            ;

            LOG.info("Removed all ssids");

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in removeAllSsids", e);
            throw new RuntimeException(e);
        }

    }

    public void removeAllPasspointConfigs(OvsdbClient ovsdbClient) {
        removeAllHotspot20Config(ovsdbClient);
        removeAllHotspot20OsuProviders(ovsdbClient);
        removeAllHotspot20IconConfig(ovsdbClient);
    }

    public void configureWifiRadios(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncAPConfig) {

        String country = opensyncAPConfig.getCountryCode(); // should be the
        // same for all
        // radios on this AP
        // ;-)

        ApElementConfiguration apElementConfiguration = (ApElementConfiguration) opensyncAPConfig.getCustomerEquipment()
                .getDetails();
        RfConfiguration rfConfig = (RfConfiguration) opensyncAPConfig.getRfProfile().getDetails();

        for (RadioType radioType : apElementConfiguration.getRadioMap().keySet()) {
            Map<String, String> hwConfig = new HashMap<>();

            ElementRadioConfiguration elementRadioConfig = apElementConfiguration.getRadioMap().get(radioType);
            RfElementConfiguration rfElementConfig = rfConfig.getRfConfig(radioType);
            int channel = elementRadioConfig.getChannelNumber();
            ChannelBandwidth bandwidth = rfElementConfig.getChannelBandwidth();
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
            rfElementConfig.getAutoChannelSelection();

            RadioConfiguration radioConfig = apElementConfiguration.getAdvancedRadioMap().get(radioType);
            int beaconInterval = rfElementConfig.getBeaconInterval();
            boolean enabled = radioConfig.getRadioAdminState().equals(StateSetting.enabled);

            int txPower = 0;
            if (elementRadioConfig.getEirpTxPower().getSource() == SourceType.profile) {
                txPower = rfElementConfig.getEirpTxPower();
            } else {
                txPower = (int) elementRadioConfig.getEirpTxPower().getValue();
            }

            String hwMode = null;
            switch (rfElementConfig.getRadioMode()) {
            case modeA:
                hwMode = "11a";
                break;
            case modeAB:
                hwMode = "11ab";
                break;
            case modeAC:
                hwMode = "11ac";
                break;
            case modeB:
                hwMode = "11b";
                break;
            case modeG:
                hwMode = "11g";
                break;
            case modeX:
                hwMode = "11ax";
                break;
            case modeN:
                hwMode = "11n";
                break;
            default:
            }
            String freqBand = null;
            switch (radioType) {
            case is2dot4GHz:
                freqBand = "2.4G";
                break;
            case is5GHz:
                // 802.11h dfs (Dynamic Frequency Selection) aka military
                // and
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
                // 802.11h dfs (Dynamic Frequency Selection) aka military
                // and
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
                // 802.11h dfs (Dynamic Frequency Selection) aka military
                // and
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
                            enabled, hwMode, ht_mode, txPower);
                } catch (OvsdbClientException e) {
                    LOG.error("ConfigureWifiRadios failed with OvsdbClient exception.", e);
                    throw new RuntimeException(e);

                } catch (TimeoutException e) {
                    LOG.error("ConfigureWifiRadios failed with Timeout.", e);
                    throw new RuntimeException(e);

                } catch (ExecutionException e) {
                    LOG.error("ConfigureWifiRadios excecution failed.", e);
                    throw new RuntimeException(e);

                } catch (InterruptedException e) {
                    LOG.error("ConfigureWifiRadios interrupted.", e);
                    throw new RuntimeException(e);

                }

            }

        }

    }

    public void configureInterfaces(OvsdbClient ovsdbClient) {

        configureWanInterfacesForDhcpSniffing(ovsdbClient);
        configureLanInterfacesforDhcpSniffing(ovsdbClient);

    }

    private void configureLanInterfacesforDhcpSniffing(OvsdbClient ovsdbClient) {
        List<Operation> operations = new ArrayList<>();
        Map<String, Value> updateColumns = new HashMap<>();
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition("if_name", Function.NOT_EQUALS, new Atom<>(defaultWanInterfaceName)));
        conditions.add(new Condition("parent_ifname", Function.NOT_EQUALS, new Atom<>(defaultWanInterfaceName)));
        updateColumns.put("dhcp_sniff", new Atom<>(true));

        Row row = new Row(updateColumns);
        operations.add(new Update(wifiInetConfigDbTable, conditions, row));

        try {
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            for (OperationResult res : result) {
                if (res instanceof UpdateResult) {
                    LOG.info("configureLanInterfacesForDhcpSniffing {}", ((UpdateResult) res).toString());
                } else if (res instanceof ErrorResult) {
                    LOG.error("configureLanInterfacesForDhcpSniffing error {}", ((ErrorResult) res));
                    throw new RuntimeException("configureLanInterfacesForDhcpSniffing " + ((ErrorResult) res).getError()
                            + " " + ((ErrorResult) res).getDetails());
                }
            }
        } catch (OvsdbClientException | InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("OvsdbDao::configureLanInterfaces failed.", e);
            throw new RuntimeException(e);

        }

    }

    private void configureWanInterfacesForDhcpSniffing(OvsdbClient ovsdbClient) {
        List<Operation> operations = new ArrayList<>();
        Map<String, Value> updateColumns = new HashMap<>();
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition("if_name", Function.NOT_EQUALS, new Atom<>(defaultLanInterfaceName)));
        conditions.add(new Condition("parent_ifname", Function.NOT_EQUALS, new Atom<>(defaultLanInterfaceName)));

        updateColumns.put("dhcp_sniff", new Atom<>(true));

        Row row = new Row(updateColumns);
        operations.add(new Update(wifiInetConfigDbTable, conditions, row));

        try {
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            for (OperationResult res : result) {
                if (res instanceof UpdateResult) {
                    LOG.info("configureWanInterfacesForDhcpSniffing {}", ((UpdateResult) res).toString());
                } else if (res instanceof ErrorResult) {
                    LOG.error("configureWanInterfacesForDhcpSniffing error {}", ((ErrorResult) res));
                    throw new RuntimeException("configureWanInterfacesForDhcpSniffing " + ((ErrorResult) res).getError()
                            + " " + ((ErrorResult) res).getDetails());
                }
            }
        } catch (OvsdbClientException | InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("OvsdbDao::configureWanInterfaces failed.", e);
            throw new RuntimeException(e);

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
                        tableState.setVifStates(vifStates);

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
            throw new RuntimeException(e);

        }

        return ret;
    }

    public List<OpensyncAPInetState> getInitialOpensyncApInetStateForRowUpdate(TableUpdates tableUpdates, String apId,
            OvsdbClient ovsdbClient) {

        LOG.info("getInitialOpensyncApInetStateForRowUpdate:");
        List<OpensyncAPInetState> ret = new ArrayList<>();
        try {
            LOG.info(OvsdbDao.wifiInetStateDbTable + "_" + apId + " initial monitor table state received {}",
                    tableUpdates);

            for (TableUpdate tableUpdate : tableUpdates.getTableUpdates().values()) {

                for (RowUpdate rowUpdate : tableUpdate.getRowUpdates().values()) {

                    if (rowUpdate.getNew() != null)
                        ret.addAll(getOpensyncApInetStateForRowUpdate(rowUpdate, apId, ovsdbClient));
                }

            }
        } catch (Exception e) {
            throw (e);
        }
        return ret;

    }

    public List<OpensyncAPInetState> getOpensyncApInetStateForRowUpdate(RowUpdate rowUpdate, String apId,
            OvsdbClient ovsdbClient) {
        List<OpensyncAPInetState> ret = new ArrayList<>();

        LOG.info("OvsdbDao::getOpensyncApInetStateForRowUpdate {} for apId {}", rowUpdate, apId);

        try {

            Row row = rowUpdate.getNew();
            if (row == null) {
                row = rowUpdate.getOld();
            }

            OpensyncAPInetState tableState = new OpensyncAPInetState();
            Map<String, Value> map = row.getColumns();

            if ((map.get("NAT") != null)
                    && map.get("NAT").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                tableState.setNat(row.getBooleanColumn("NAT"));
            }
            if ((map.get("enabled") != null)
                    && map.get("enabled").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                tableState.setEnabled(row.getBooleanColumn("enabled"));
            }
            if ((map.get("if_name") != null)
                    && map.get("if_name").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                tableState.setIfName(row.getStringColumn("if_name"));
            }
            if ((map.get("if_type") != null)
                    && map.get("if_type").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                tableState.setIfType(row.getStringColumn("if_type"));
            }

            if (map.containsKey("dhcpc")) {
                tableState.setDhcpc(row.getMapColumn("dhcpc"));
            }
            if (map.containsKey("dhcpd")) {
                tableState.setDhcpd(row.getMapColumn("dhcpd"));
            }
            if (map.containsKey("dns")) {
                tableState.setDns(row.getMapColumn("dns"));
            }
            if (map.get("inet_addr") != null && map.get("inet_addr").getClass()
                    .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                tableState.setInetAddr(row.getStringColumn("inet_addr"));
            }
            if (map.containsKey("netmask")) {
                tableState.setNetmask(getSingleValueFromSet(row, "netmask"));
            }
            if (map.get("vlan_id") != null
                    && map.get("vlan_id").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                tableState.setVlanId(row.getIntegerColumn("vlan_id").intValue());
            }
            if (map.get("gre_ifname") != null && map.get("gre_ifname").getClass()
                    .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                tableState.setGreIfName(row.getStringColumn("gre_ifname"));
            }
            if (map.get("gre_remote_inet_addr") != null && map.get("gre_remote_inet_addr").getClass()
                    .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                tableState.setGreRemoteInetAddr(row.getStringColumn("gre_remote_inet_addr"));
            }
            if (map.get("gre_local_inet_addr") != null && map.get("gre_local_inet_addr").getClass()
                    .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                tableState.setGreLocalInetAddr(row.getStringColumn("gre_local_inet_addr"));
            }
            if (map.get("gre_remote_mac_addr") != null && map.get("gre_remote_mac_addr").getClass()
                    .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                tableState.setGreRemoteMacAddr(row.getStringColumn("gre_remote_mac_addr"));
            }

            if ((map.get("ip_assign_scheme") != null) && map.get("ip_assign_scheme").getClass()
                    .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                tableState.setIpAssignScheme(row.getStringColumn("ip_assign_scheme"));
            }
            if ((map.get("network") != null)
                    && map.get("network").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                tableState.setNetwork(row.getBooleanColumn("network"));
            }
            if ((map.get("hwaddr") != null)
                    && map.get("hwaddr").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                tableState.setHwAddr(row.getStringColumn("hwaddr"));
            }
            if ((map.get("_version") != null)
                    && map.get("_version").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                tableState.setVersion(row.getUuidColumn("_version"));
            }
            if ((map.get("_uuid") != null)
                    && map.get("_uuid").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                tableState.setVersion(row.getUuidColumn("_uuid"));
            }
            ret.add(tableState);

        } catch (Exception e) {
            LOG.error("Could not parse update for Wifi_Inet_State", e);
            throw new RuntimeException(e);

        }
        return ret;
    }

    public List<OpensyncAPVIFState> getInitialOpensyncApVifStateForTableUpdates(TableUpdates tableUpdates, String apId,
            OvsdbClient ovsdbClient) {

        LOG.info("getInitialOpensyncApVifStateForTableUpdates:");
        List<OpensyncAPVIFState> ret = new ArrayList<>();
        try {
            LOG.info(OvsdbDao.wifiVifStateDbTable + "_" + apId + " initial monitor table state received {}",
                    tableUpdates);

            for (TableUpdate tableUpdate : tableUpdates.getTableUpdates().values()) {

                for (RowUpdate rowUpdate : tableUpdate.getRowUpdates().values()) {

                    if (rowUpdate.getNew() != null) {
                        OpensyncAPVIFState tableState = processWifiVIFStateColumn(ovsdbClient, rowUpdate.getNew());

                        ret.add(tableState);
                    }

                }

            }
        } catch (Exception e) {
            throw (e);
        }
        return ret;

    }

    public List<OpensyncAPVIFState> getOpensyncApVifStateForRowUpdate(RowUpdate rowUpdate, String apId,
            OvsdbClient ovsdbClient) {
        List<OpensyncAPVIFState> ret = new ArrayList<>();
        try {

            Row row = rowUpdate.getNew(); // add/modify/init
            if (row == null) {
                row = rowUpdate.getOld(); // delete/modify
            }

            OpensyncAPVIFState tableState = processWifiVIFStateColumn(ovsdbClient, row);

            ret.add(tableState);

        } catch (Exception e) {
            LOG.error("Could not parse update for Wifi_VIF_State", e);
            throw new RuntimeException(e);

        }
        return ret;
    }

    public List<OpensyncWifiAssociatedClients> getInitialOpensyncWifiAssociatedClients(TableUpdates tableUpdates,
            String apId, OvsdbClient ovsdbClient) {

        LOG.info("getInitialOpensyncWifiAssociatedClients:");
        List<OpensyncWifiAssociatedClients> ret = new ArrayList<>();
        try {
            LOG.info(OvsdbDao.wifiAssociatedClientsDbTable + "_" + apId + " initial monitor table state received {}",
                    tableUpdates);

            for (TableUpdate tableUpdate : tableUpdates.getTableUpdates().values()) {

                for (RowUpdate rowUpdate : tableUpdate.getRowUpdates().values()) {

                    if (rowUpdate.getNew() != null)
                        ret.addAll(getOpensyncWifiAssociatedClients(rowUpdate, apId, ovsdbClient));
                }

            }
        } catch (Exception e) {
            throw (e);
        }
        return ret;

    }

    public List<OpensyncWifiAssociatedClients> getOpensyncWifiAssociatedClients(RowUpdate rowUpdate, String apId,
            OvsdbClient ovsdbClient) {
        List<OpensyncWifiAssociatedClients> ret = new ArrayList<>();

        Row row = rowUpdate.getNew();
        if (row == null) {
            row = rowUpdate.getOld();
        }

        OpensyncWifiAssociatedClients tableState = new OpensyncWifiAssociatedClients();
        Map<String, Value> map = row.getColumns();

        if ((map.get("mac") != null)
                && map.get("mac").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            tableState.setMac(row.getStringColumn("mac"));
        }
        if (row.getSetColumn("capabilities") != null) {
            tableState.setCapabilities(row.getSetColumn("capabilities"));
        }
        if ((map.get("state") != null)
                && map.get("state").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            tableState.setState(row.getStringColumn("state"));
        }
        if ((map.get("_version") != null)
                && map.get("_version").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            tableState.setVersion(row.getUuidColumn("_version"));
        }
        if ((map.get("_uuid") != null)
                && map.get("_uuid").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            tableState.setVersion(row.getUuidColumn("_uuid"));
        }

        ret.add(tableState);

        ret.stream().forEach(new Consumer<OpensyncWifiAssociatedClients>() {

            @Override
            public void accept(OpensyncWifiAssociatedClients wrs) {
                LOG.debug("Wifi_Associated_Clients row {}", wrs);
            }
        });

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
            throw new RuntimeException(e);

        }

        return tableState;

    }

    public void configureCommands(OvsdbClient ovsdbClient, String command, Map<String, String> payload, Long delay,
            Long duration) {

        LOG.debug("OvsdbDao::configureCommands command {}, payload {}, delay {} duration {}", command, payload, delay,
                duration);

        List<Operation> operations = new ArrayList<>();
        Map<String, Value> commandConfigColumns = new HashMap<>();
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition("command", Function.EQUALS, new Atom<>(command)));

        commandConfigColumns.put("command", new Atom<>(command));
        commandConfigColumns.put("payload", com.vmware.ovsdb.protocol.operation.notation.Map.of(payload));

        commandConfigColumns.put("delay", new Atom<>(delay));
        commandConfigColumns.put("duration", new Atom<>(delay));

        Row row = new Row(commandConfigColumns);
        if (getProvisionedCommandConfigs(ovsdbClient).containsKey(command)) {
            operations.add(new Update(commandConfigDbTable, conditions, row));
        } else {
            operations.add(new Insert(commandConfigDbTable, row));
        }

        try {
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            LOG.debug("OvsdbDao::configureCommands successfully configured command {} for duration {} payload {}",
                    command, duration, payload);

            for (OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }
        } catch (OvsdbClientException | InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("OvsdbDao::configureCommands failed.", e);
            throw new RuntimeException(e);

        }

    }

    private void configureWifiRadios(OvsdbClient ovsdbClient, String freqBand, int channel,
            Map<String, String> hwConfig, String country, int beaconInterval, boolean enabled, String hwMode,
            String ht_mode, int txPower)
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
        if (hwMode != null) {
            updateColumns.put("hw_mode", new Atom<>(hwMode));
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

    private Uuid configureSingleSsid(OvsdbClient ovsdbClient, String vifInterfaceName, String ssid,
            boolean ssidBroadcast, Map<String, String> security, String radioFreqBand, int vlanId, boolean rrmEnabled,
            boolean enable80211r, int mobilityDomain, boolean enable80211v, boolean enable80211k, String minHwMode,
            boolean enabled, int keyRefresh, boolean uapsdEnabled, boolean apBridge,
            NetworkForwardMode networkForwardMode, String gateway, String inet, Map<String, String> dns,
            String ipAssignScheme, List<MacAddress> macBlockList, boolean rateLimitEnable, int ssidDlLimit,
            int ssidUlLimit, int clientDlLimit, int clientUlLimit, int rtsCtsThreshold, int fragThresholdBytes,
            int dtimPeriod, Map<String, String> captiveMap, List<String> walledGardenAllowlist,
            Map<Short, Set<String>> bonjourServiceMap, String radiusNasId, String radiusNasIp,
            String radiusOperatorName) {

        List<Operation> operations = new ArrayList<>();
        Map<String, Value> updateColumns = new HashMap<>();

        try {

            // If we are doing a NAT SSID, no bridge, else yes
            String bridgeInterfaceName = defaultWanInterfaceName;
            if (networkForwardMode == NetworkForwardMode.NAT) {
                bridgeInterfaceName = defaultLanInterfaceName;
            }

            if (vlanId > 1) {
                // createVlanNetworkInterfaces(ovsdbClient, vlanId);
                updateColumns.put("vlan_id", new Atom<>(vlanId));
            } else {
                updateColumns.put("vlan_id", new Atom<>(1));
            }

            updateColumns.put("mode", new Atom<>("ap"));
            @SuppressWarnings("unchecked")
            com.vmware.ovsdb.protocol.operation.notation.Map<String, String> captivePortalMap = com.vmware.ovsdb.protocol.operation.notation.Map
                    .of(captiveMap);
            updateColumns.put("captive_portal", captivePortalMap);

            if (walledGardenAllowlist != null && !walledGardenAllowlist.isEmpty()) {
                Set<Atom<String>> atomMacList = new HashSet<>();
                walledGardenAllowlist.stream().forEach(allow -> atomMacList.add(new Atom<>(allow)));
                com.vmware.ovsdb.protocol.operation.notation.Set allowListSet = com.vmware.ovsdb.protocol.operation.notation.Set
                        .of(atomMacList);
                updateColumns.put("captive_allowlist", allowListSet);
            } else {
                updateColumns.put("captive_allowlist", new com.vmware.ovsdb.protocol.operation.notation.Set());
            }

            // TODO: when AP support for Bonjour Gateway set values
            if (bonjourServiceMap != null && bonjourServiceMap.size() > 0) {
                LOG.info("SSID {} Bonjour Services per vlan {}", ssid, bonjourServiceMap);
            }

            updateColumns.put("bridge", new Atom<>(bridgeInterfaceName));

            if (enable80211v) {
                updateColumns.put("btm", new Atom<>(1));
            } else {
                updateColumns.put("btm", new Atom<>(0));
            }
            updateColumns.put("enabled", new Atom<>(enabled));
            if (enable80211r) {
                updateColumns.put("ft_psk", new Atom<>(1));
                updateColumns.put("ft_mobility_domain", new Atom<>(mobilityDomain));
            } else {
                updateColumns.put("ft_psk", new Atom<>(0));
                updateColumns.put("ft_mobility_domain", new com.vmware.ovsdb.protocol.operation.notation.Set());
            }
            updateColumns.put("if_name", new Atom<>(vifInterfaceName));
            updateColumns.put("rrm", new Atom<>(rrmEnabled ? 1 : 0));
            updateColumns.put("ssid", new Atom<>(ssid));
            updateColumns.put("ssid_broadcast", new Atom<>(ssidBroadcast ? "enabled" : "disabled"));
            updateColumns.put("uapsd_enable", new Atom<>(uapsdEnabled));

            updateColumns.put("min_hw_mode", new Atom<>(minHwMode));

            updateColumns.put("group_rekey", new Atom<>(keyRefresh));
            updateColumns.put("uapsd_enable", new Atom<>(uapsdEnabled));
            updateColumns.put("ap_bridge", new Atom<>(apBridge));

            @SuppressWarnings("unchecked")
            com.vmware.ovsdb.protocol.operation.notation.Map<String, String> securityMap = com.vmware.ovsdb.protocol.operation.notation.Map
                    .of(security);
            updateColumns.put("security", securityMap);

            configureCustomOptionsForSsid(ovsdbClient, enable80211k, rateLimitEnable, ssidDlLimit, ssidUlLimit,
                    clientDlLimit, clientUlLimit, rtsCtsThreshold, fragThresholdBytes, dtimPeriod, radiusNasId,
                    radiusNasIp, radiusOperatorName, updateColumns);

            updateBlockList(updateColumns, macBlockList);
            Row row = new Row(updateColumns);

            operations.add(new Insert(wifiVifConfigDbTable, row));

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            Uuid vifConfigUuid = null;
            for (OperationResult res : result) {
                InsertResult insertResult = null;
                if (res instanceof InsertResult) {
                    insertResult = (InsertResult) res;
                    LOG.info("configureSingleSsid:InsertResult {}", insertResult);
                    vifConfigUuid = ((InsertResult) res).getUuid();
                } else if (res instanceof ErrorResult) {
                    LOG.error("configureSingleSsid: error {}", ((ErrorResult) res));
                    throw new RuntimeException("configureSingleSsid " + ((ErrorResult) res).getError() + " "
                            + ((ErrorResult) res).getDetails());
                }
            }
            if (vifConfigUuid == null) {
                throw new IllegalStateException("Wifi_VIF_Config entry was not created successfully");
            }

            confirmRowExistsInTable(ovsdbClient, vifConfigUuid, wifiVifConfigDbTable);

            LOG.info("configureSingleSsid:Provisioned SSID {} on interface {} / {}", ssid, vifInterfaceName,
                    radioFreqBand);

            return vifConfigUuid;

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in configureSingleSsid", e);
            throw new RuntimeException(e);
        }

    }

    /**
     * Populate the various <K,V> fields in the custom_options column of the
     * Wifi_VIF_Config ovsdb table.
     * 
     * @param ovsdbClient
     * @param enable80211k
     * @param rateLimitEnable
     * @param ssidDlLimit
     * @param ssidUlLimit
     * @param clientDlLimit
     * @param clientUlLimit
     * @param rtsCtsThreshold
     * @param fragThresholdBytes
     *            TODO
     * @param dtimPeriod
     * @param radiusNasId
     * @param radiusNasIp
     * @param radiusOperatorName
     * @param updateColumns
     */
    void configureCustomOptionsForSsid(OvsdbClient ovsdbClient, boolean enable80211k, boolean rateLimitEnable,
            int ssidDlLimit, int ssidUlLimit, int clientDlLimit, int clientUlLimit, int rtsCtsThreshold,
            int fragThresholdBytes, int dtimPeriod, String radiusNasId, String radiusNasIp, String radiusOperatorName,
            Map<String, Value> updateColumns) {
        Map<String, String> customOptions = new HashMap<>();
        configureCustomOptionsForRatesAndLimits(rateLimitEnable, ssidDlLimit, ssidUlLimit, clientDlLimit, clientUlLimit,
                rtsCtsThreshold, customOptions);

        configureCustomOptionsForRadiusNas(ovsdbClient, radiusNasId, radiusNasIp, radiusOperatorName, customOptions);

        configureCustomOptionsForDtimFragAnd80211k(enable80211k, dtimPeriod, fragThresholdBytes, customOptions);

        @SuppressWarnings("unchecked")
        com.vmware.ovsdb.protocol.operation.notation.Map<String, String> customMap = com.vmware.ovsdb.protocol.operation.notation.Map
                .of(customOptions);
        updateColumns.put("custom_options", customMap);
    }

    /**
     * 
     * @param enable80211k
     * @param dtimPeriod
     * @param fragThresholdBytes
     *            TODO
     * @param customOptions
     */
    private void configureCustomOptionsForDtimFragAnd80211k(boolean enable80211k, int dtimPeriod,
            int fragThresholdBytes, Map<String, String> customOptions) {

        // TODO: the frag_threshold is not supported on the AP
        // customOptions.put("frag_threshold",
        // String.valueOf(fragThresholdBytes));

        customOptions.put("dtim_period", String.valueOf(dtimPeriod));

        if (enable80211k) {
            customOptions.put("ieee80211k", String.valueOf(1));
        } else {
            customOptions.put("ieee80211k", String.valueOf(0));
        }
    }

    /**
     * 
     * @param rateLimitEnable
     * @param ssidDlLimit
     * @param ssidUlLimit
     * @param clientDlLimit
     * @param clientUlLimit
     * @param rtsCtsThreshold
     * @param customOptions
     */
    private void configureCustomOptionsForRatesAndLimits(boolean rateLimitEnable, int ssidDlLimit, int ssidUlLimit,
            int clientDlLimit, int clientUlLimit, int rtsCtsThreshold, Map<String, String> customOptions) {
        customOptions.put("rate_limit_en", rateLimitEnable ? "1" : "0");
        customOptions.put("ssid_ul_limit", String.valueOf(ssidUlLimit * 1000));
        customOptions.put("ssid_dl_limit", String.valueOf(ssidDlLimit * 1000));
        customOptions.put("client_dl_limit", String.valueOf(clientDlLimit * 1000));
        customOptions.put("client_ul_limit", String.valueOf(clientUlLimit * 1000));
        customOptions.put("rts_threshold", String.valueOf(rtsCtsThreshold));
    }

    /**
     * 
     * @param ovsdbClient
     * @param radiusNasId
     * @param radiusNasIp
     * @param radiusOperatorName
     * @param customOptions
     */
    void configureCustomOptionsForRadiusNas(OvsdbClient ovsdbClient, String radiusNasId, String radiusNasIp,
            String radiusOperatorName, Map<String, String> customOptions) {
        ConnectNodeInfo partialConnectNode = new ConnectNodeInfo();
        fillInWanIpAddressAndMac(ovsdbClient, partialConnectNode, defaultWanInterfaceType, defaultWanInterfaceName);

        if (radiusNasId != null) {
            if (radiusNasId.equals(NasIdType.DEFAULT.toString())) {
                customOptions.put("radius_nas_id", partialConnectNode.macAddress);
            } else {
                customOptions.put("radius_nas_id", radiusNasId);
            }
        }
        if (radiusNasIp != null) {
            if (radiusNasIp.equals(NasIpType.WAN_IP.toString())) {
                customOptions.put("radius_nas_ip", partialConnectNode.ipV4Address);
            } else {
                customOptions.put("radius_nas_ip", radiusNasIp);
            }
        }
        if (radiusOperatorName != null) {
            customOptions.put("radius_oper_name", radiusOperatorName);
        }
    }

    /**
     * Check existence of row with a given Uuid in the specified ovsdb table
     * Used primarily for operation validation.
     * 
     * @param ovsdbClient
     * @param rowUuid
     * @param table
     */
    void confirmRowExistsInTable(OvsdbClient ovsdbClient, Uuid rowUuid, String table) {
        try {
            List<Condition> conditions = new ArrayList<>();
            conditions.add(new Condition("_uuid", Function.EQUALS, new Atom<>(rowUuid)));
            List<Operation> operations = new ArrayList<>();
            operations.add(new Select(table, conditions));

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            for (OperationResult res : result) {
                if (res instanceof SelectResult) {
                    LOG.info("Select Result for confirmRowExistsInTable {} with Uuid {} {}", table, rowUuid,
                            ((SelectResult) res).getRows());
                } else if (res instanceof ErrorResult) {
                    LOG.error("confirmRowExistsInTable error {}", ((ErrorResult) res));
                    throw new RuntimeException("confirmRowExistsInTable " + ((ErrorResult) res).getError() + " "
                            + ((ErrorResult) res).getDetails());
                }
            }
        } catch (OvsdbClientException | InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Unable to confirm existence of row in table {} for Uuid {}", table, rowUuid, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 
     * @param ovsdbClient
     * @param vifInterfaceName
     * @param enabled
     * @param networkForwardMode
     */
    void configureInetVifInterface(OvsdbClient ovsdbClient, String vifInterfaceName, boolean enabled,
            NetworkForwardMode networkForwardMode) {
        Map<String, WifiInetConfigInfo> inetConfigs = getProvisionedWifiInetConfigs(ovsdbClient);

        if (inetConfigs.containsKey(vifInterfaceName)) {
            configureInetInterface(ovsdbClient, vifInterfaceName, enabled, "vif", true,
                    (networkForwardMode == NetworkForwardMode.NAT));
        } else {
            configureInetInterface(ovsdbClient, vifInterfaceName, enabled, "vif", false,
                    (networkForwardMode == NetworkForwardMode.NAT));
        }
    }

    private void updateBlockList(Map<String, Value> updateColumns, List<MacAddress> macBlockList) {

        if ((macBlockList != null) && !macBlockList.isEmpty()) {
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

    /**
     * Update the vif_configs column of the Wifi_Radio_Config ovsdb table for
     * the given freqBand
     * 
     * @param ovsdbClient
     * @param ssid
     * @param radioFreqBand
     * @param vifConfigUuid
     * @throws OvsdbClientException
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    void updateVifConfigsSetForRadio(OvsdbClient ovsdbClient, String ssid, String radioFreqBand, Uuid vifConfigUuid)
            throws OvsdbClientException, InterruptedException, ExecutionException, TimeoutException {
        List<Operation> operations = new ArrayList<>();
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition("freq_band", Function.EQUALS, new Atom<>(radioFreqBand)));

        List<Mutation> mutations = new ArrayList<>();
        Mutation mutation = new Mutation("vif_configs", Mutator.INSERT, new Atom<>(vifConfigUuid));
        mutations.add(mutation);
        operations.add(new Mutate(wifiRadioConfigDbTable, conditions, mutations));

        CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
        OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

        for (OperationResult res : result) {
            if (res instanceof UpdateResult) {
                LOG.debug("updateVifConfigsSetForRadio:result {}", (UpdateResult) res);
            } else if (res instanceof ErrorResult) {
                LOG.error("updateVifConfigsSetForRadio error {}", ((ErrorResult) res));
                throw new RuntimeException("updateVifConfigsSetForRadio " + ((ErrorResult) res).getError() + " "
                        + ((ErrorResult) res).getDetails());
            }
        }

        // confirm the table was updated with the new Wifi_VIF_Config Uuid
        operations.clear();
        operations.add(new Select(wifiRadioConfigDbTable, conditions));
        fResult = ovsdbClient.transact(ovsdbName, operations);
        result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

        for (OperationResult res : result) {
            if (res instanceof SelectResult) {
                SelectResult selectResult = (SelectResult) res;
                Row row = selectResult.getRows().get(0);
                if (!row.getSetColumn("vif_configs").contains(vifConfigUuid)) {
                    throw new RuntimeException("Wifi_Radio_Config " + row
                            + "vif_configs table was not updated {} for new Wifi_VIF_Config " + vifConfigUuid);
                }
            } else if (res instanceof ErrorResult) {
                LOG.error("updateVifConfigsSetForRadio error {}", ((ErrorResult) res));
                throw new RuntimeException("updateVifConfigsSetForRadio " + ((ErrorResult) res).getError() + " "
                        + ((ErrorResult) res).getDetails());
            }
        }
        LOG.info("Updated WifiRadioConfig {} for SSID {}:", radioFreqBand, ssid);

    }

    public void configureSsids(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig) {

        boolean rrmEnabled = false;
        if ((opensyncApConfig.getEquipmentLocation() != null)
                && (opensyncApConfig.getEquipmentLocation().getDetails() != null)) {
            rrmEnabled = opensyncApConfig.getEquipmentLocation().getDetails().isRrmEnabled();
        }
        List<MacAddress> macBlockList = opensyncApConfig.getBlockedClients();
        LOG.debug("configureSsids {} with blockList {}", opensyncApConfig.getSsidProfile(), macBlockList);

        List<RadioType> enabledRadiosFromAp = new ArrayList<>();
        getEnabledRadios(ovsdbClient, enabledRadiosFromAp);

        for (Profile ssidProfile : opensyncApConfig.getSsidProfile()) {

            SsidConfiguration ssidConfig = (SsidConfiguration) ssidProfile.getDetails();
            ApElementConfiguration apElementConfig = (ApElementConfiguration) opensyncApConfig.getCustomerEquipment()
                    .getDetails();
            RfConfiguration rfConfig = (RfConfiguration) opensyncApConfig.getRfProfile().getDetails();

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

                // custom_options:='["map",[["rate_limit_en","1"],["ssid_ul_limit","1024"],["ssid_dl_limit","1024"],["client_dl_limit","200"],["client_ul_limit","200"]]]'

                boolean rateLimitEnable = false;
                int ssidUlLimit = 0;
                int ssidDlLimit = 0;
                int clientDlLimit = 0;
                int clientUlLimit = 0;

                if (((ssidConfig.getBandwidthLimitDown() != null) && (ssidConfig.getBandwidthLimitUp() > 0))
                        || ((ssidConfig.getBandwidthLimitUp() != null) && (ssidConfig.getBandwidthLimitUp() > 0))) {
                    rateLimitEnable = true;
                    ssidUlLimit = ssidConfig.getBandwidthLimitUp();
                    ssidDlLimit = ssidConfig.getBandwidthLimitDown();
                    clientDlLimit = ssidConfig.getClientBandwidthLimitDown();
                    clientUlLimit = ssidConfig.getClientBandwidthLimitUp();
                }

                Map<String, WifiRadioConfigInfo> provisionedRadioConfigs = getProvisionedWifiRadioConfigs(ovsdbClient);
                String freqBand = null;
                String ifName = null;
                String radioName = null;
                for (Entry<String, WifiRadioConfigInfo> entry : provisionedRadioConfigs.entrySet()) {
                    if ((radioType == RadioType.is2dot4GHz) && entry.getValue().freqBand.equals("2.4G")) {
                        freqBand = "2.4G";
                        radioName = entry.getKey();
                        break;
                    } else if ((radioType == RadioType.is5GHzL) && entry.getValue().freqBand.equals("5GL")) {
                        freqBand = "5GL";
                        radioName = entry.getKey();
                        break;
                    } else if ((radioType == RadioType.is5GHzU) && entry.getValue().freqBand.equals("5GU")) {
                        freqBand = "5GU";
                        radioName = entry.getKey();
                        break;
                    } else if ((radioType == RadioType.is5GHz) && entry.getValue().freqBand.equals("5G")) {
                        freqBand = "5G";
                        radioName = entry.getKey();
                        break;
                    }
                }
                if ((radioName == null) || (freqBand == null)) {
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

                String ipAssignScheme = apElementConfig.getGettingIP().toString();
                // the following 5 attributes only applicable to static
                // ipAssignScheme,
                // else they are
                // ignored
                String gateway = null;
                String inet = null;
                Map<String, String> dns = null;
                if (ipAssignScheme.equals("manual")) {
                    if (apElementConfig.getStaticIP() != null) {
                        ipAssignScheme = "static";
                        inet = apElementConfig.getStaticIP().getHostAddress();
                        gateway = apElementConfig.getStaticIpGw().getHostAddress();
                        dns = new HashMap<>();
                        dns.put(apElementConfig.getStaticDnsIp1().getHostName(),
                                apElementConfig.getStaticDnsIp1().getHostAddress());
                        dns.put(apElementConfig.getStaticDnsIp2().getHostName(),
                                apElementConfig.getStaticDnsIp2().getHostAddress());
                    } else {
                        ipAssignScheme = "none";
                    }
                }

                RfElementConfiguration rfElementConfig = rfConfig.getRfConfig(radioType);
                int rtsCtsThreshold = rfElementConfig.getRtsCtsThreshold();
                RadioMode radioMode = rfElementConfig.getRadioMode();

                int dtimPeriod = 2;
                int fragThresholdBytes = 2346;
                boolean uapsdEnabled = true;
                boolean apBridge = true;
                RadioConfiguration radioConfiguration = apElementConfig.getAdvancedRadioMap().get(radioType);
                if (radioConfiguration != null) {
                    dtimPeriod = radioConfiguration.getDtimPeriod();
                    uapsdEnabled = radioConfiguration.getUapsdState() == StateSetting.enabled;
                    apBridge = radioConfiguration.getStationIsolation() == StateSetting.disabled; // stationIsolation
                    fragThresholdBytes = radioConfiguration.getFragmentationThresholdBytes();
                }
                String minHwMode = "11n"; // min_hw_mode is 11ac, wifi 5, we can
                // also take ++ (11ax) but 2.4GHz only
                // Wifi4 --
                if (!radioType.equals(RadioType.is2dot4GHz)) {
                    minHwMode = "11ac";
                }
                if (!radioType.equals(RadioType.is2dot4GHz) && radioMode.equals(RadioMode.modeX)) {
                    minHwMode = "11x";
                }

                // off by default
                boolean enable80211r = false;
                int mobilityDomain = 0;
                // on by default
                boolean enable80211v = true;
                // on by default
                boolean enable80211k = true;

                if (ssidConfig.getRadioBasedConfigs() != null) {
                    if (ssidConfig.getRadioBasedConfigs().containsKey(radioType)
                            && (ssidConfig.getRadioBasedConfigs().get(radioType) != null)) {
                        if (ssidConfig.getRadioBasedConfigs().get(radioType).getEnable80211r() != null) {
                            enable80211r = ssidConfig.getRadioBasedConfigs().get(radioType).getEnable80211r();
                            if (enable80211r) {
                                mobilityDomain = opensyncApConfig.getCustomerEquipment().getCustomerId(); // for
                                                                                                          // uniqueness,
                                                                                                          // mobility
                                                                                                          // domain
                                                                                                          // is
                                                                                                          // per
                                                                                                          // customer
                            }
                        }
                        if (ssidConfig.getRadioBasedConfigs().get(radioType).getEnable80211v() != null) {
                            enable80211v = ssidConfig.getRadioBasedConfigs().get(radioType).getEnable80211v();
                        }
                        if (ssidConfig.getRadioBasedConfigs().get(radioType).getEnable80211k() != null) {
                            enable80211k = ssidConfig.getRadioBasedConfigs().get(radioType).getEnable80211k();
                        }
                    }
                }

                Map<String, String> security = new HashMap<>();
                String ssidSecurityMode = ssidConfig.getSecureMode().name();
                String opensyncSecurityMode = "OPEN";

                String radiusNasId = null;
                String radiusNasIp = null;
                String radiusOperName = null;

                opensyncSecurityMode = getOpensyncSecurityMode(ssidSecurityMode, opensyncSecurityMode);
                populateSecurityMap(opensyncApConfig, ssidConfig, security, ssidSecurityMode, opensyncSecurityMode);

                if (opensyncSecurityMode.endsWith("EAP")) {
                    if (ssidConfig.getRadiusClientConfiguration() != null) {
                        radiusNasId = ssidConfig.getRadiusClientConfiguration().getNasClientId()
                                .equals(NasIdType.USER_DEFINED)
                                        ? ssidConfig.getRadiusClientConfiguration().getUserDefinedNasId()
                                        : ssidConfig.getRadiusClientConfiguration().getNasClientId().toString();
                        radiusNasIp = ssidConfig.getRadiusClientConfiguration().getNasClientIp()
                                .equals(NasIpType.USER_DEFINED)
                                        ? ssidConfig.getRadiusClientConfiguration().getUserDefinedNasIp()
                                        : ssidConfig.getRadiusClientConfiguration().getNasClientIp().toString();
                        radiusOperName = ssidConfig.getRadiusClientConfiguration().getOperatorId();
                    } else {
                        radiusNasId = NasIdType.DEFAULT.toString();
                        radiusNasIp = NasIpType.WAN_IP.toString();
                    }
                }

                // TODO put into AP captive parameter
                Map<String, String> captiveMap = new HashMap<>();
                List<String> walledGardenAllowlist = new ArrayList<>();
                getCaptiveConfiguration(opensyncApConfig, ssidConfig, captiveMap, walledGardenAllowlist);

                Map<Short, Set<String>> bonjourServiceMap = new HashMap<>();
                getBonjourGatewayConfiguration(opensyncApConfig, ssidConfig, bonjourServiceMap);

                boolean enabled = ssidConfig.getSsidAdminState().equals(StateSetting.enabled);

                try {

                    ifName = getInterfaceNameForVifConfig(ovsdbClient, opensyncApConfig, ssidConfig, freqBand, ifName);

                    Uuid vifConfigUuid = configureSingleSsid(ovsdbClient, ifName, ssidConfig.getSsid(), ssidBroadcast,
                            security, freqBand, ssidConfig.getVlanId() != null ? ssidConfig.getVlanId() : 1, rrmEnabled,
                            enable80211r, mobilityDomain, enable80211v, enable80211k, minHwMode, enabled, keyRefresh,
                            uapsdEnabled, apBridge, ssidConfig.getForwardMode(), gateway, inet, dns, ipAssignScheme,
                            macBlockList, rateLimitEnable, ssidDlLimit, ssidUlLimit, clientDlLimit, clientUlLimit,
                            rtsCtsThreshold, fragThresholdBytes, dtimPeriod, captiveMap, walledGardenAllowlist,
                            bonjourServiceMap, radiusNasId, radiusNasIp, radiusOperName);

                    updateVifConfigsSetForRadio(ovsdbClient, ssidConfig.getSsid(), freqBand, vifConfigUuid);

                    configureInetVifInterface(ovsdbClient, ifName, enabled, ssidConfig.getForwardMode());

                } catch (IllegalStateException | OvsdbClientException | InterruptedException | ExecutionException
                        | TimeoutException e) {
                    // could not provision this SSID, but still can go on
                    LOG.warn("could not provision SSID {} on {}", ssidConfig.getSsid(), freqBand);
                }

            }

        }

    }

    /**
     * Get the interface name for the new Wifi_VIF_Config. Maximum of 8
     * interface names per frequencyBand
     * 
     * @param ovsdbClient
     * @param opensyncApConfig
     * @param ssidConfig
     * @param freqBand
     * @param ifName
     * @return the interface name, i.e. wlan0, wlan0_1, etc
     */
    String getInterfaceNameForVifConfig(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig,
            SsidConfiguration ssidConfig, String freqBand, String ifName) {
        Map<String, WifiVifConfigInfo> provisionedVifs = getProvisionedWifiVifConfigs(ovsdbClient);

        List<String> interfaces = new ArrayList<>();
        interfaces.add(ifName);
        for (int i = 1; i < MAX_VIF_PER_FREQ; i++) {
            interfaces.add(ifName + "_" + Integer.toString(i));
        }
        for (String key : provisionedVifs.keySet()) {
            if (key.contains(ifName)) {
                String provisionedIfName = provisionedVifs.get(key).ifName;
                if (interfaces.remove(provisionedIfName)) {
                    LOG.info("Interface {} already in use on Radio {}, cannot be used for new Wifi_VIF_Config.",
                            provisionedIfName, freqBand);
                }
            }
        }
        if (interfaces.isEmpty()) {
            throw new RuntimeException("No more available interfaces on AP "
                    + opensyncApConfig.getCustomerEquipment().getName() + " for frequency band " + freqBand);
        } else {
            // take the first available interface for this band
            ifName = interfaces.get(0);
            LOG.info("Configuring new Wifi_VIF_Config for ssid {} with if_name {}", ssidConfig.getSsid(), ifName);
        }
        return ifName;
    }

    /**
     * Constructs the map to place in the Wifi_VIF_Config ovsdb table's security
     * column.
     * 
     * @param opensyncApConfig
     * @param ssidConfig
     * @param security
     * @param ssidSecurityMode
     * @param opensyncSecurityMode
     */
    void populateSecurityMap(OpensyncAPConfig opensyncApConfig, SsidConfiguration ssidConfig,
            Map<String, String> security, String ssidSecurityMode, String opensyncSecurityMode) {
        security.put("encryption", opensyncSecurityMode);
        // key and mode is N/A for OPEN security
        if (!opensyncSecurityMode.equals("OPEN")) {
            if (ssidSecurityMode.equals("wpa2PSK") || ssidSecurityMode.equals("wpa3MixedSAE")) {
                security.put("key", ssidConfig.getKeyStr());
                security.put("mode", "mixed");
            } else if (ssidSecurityMode.equals("wpa2OnlyPSK")) {
                security.put("key", ssidConfig.getKeyStr());
                security.put("mode", "2");
            } else if (ssidSecurityMode.equals("wpa3OnlySAE")) {
                security.put("key", ssidConfig.getKeyStr());
                security.put("mode", "3");
            } else if (ssidSecurityMode.equals("wpaPSK")) {
                security.put("key", ssidConfig.getKeyStr());
                security.put("mode", "1");
            } else if (ssidSecurityMode.equals("wpa2OnlyEAP") || ssidSecurityMode.equals("wpa2OnlyRadius")) {
                security.put("mode", "2");
                getRadiusConfiguration(opensyncApConfig, ssidConfig, security);
                getRadiusAccountingConfiguration(opensyncApConfig, ssidConfig, security);
            } else if (ssidSecurityMode.equals("wpa3OnlyEAP")) {
                security.put("mode", "3");
                getRadiusConfiguration(opensyncApConfig, ssidConfig, security);
                getRadiusAccountingConfiguration(opensyncApConfig, ssidConfig, security);

            } else if (ssidSecurityMode.equals("wpa2EAP") || ssidSecurityMode.equals("wpa2Radius")
                    || ssidSecurityMode.equals("wpa3MixedEAP")) {
                security.put("mode", "mixed");
                getRadiusConfiguration(opensyncApConfig, ssidConfig, security);
                getRadiusAccountingConfiguration(opensyncApConfig, ssidConfig, security);

            } else if (ssidSecurityMode.equals("wpaEAP") || ssidSecurityMode.equals("wpaRadius")) {
                security.put("mode", "1");
                getRadiusConfiguration(opensyncApConfig, ssidConfig, security);
                getRadiusAccountingConfiguration(opensyncApConfig, ssidConfig, security);

            } else if (ssidSecurityMode.equals("wep")) {
                security.put("key", ssidConfig.getKeyStr());
                security.put("mode", "1");
            }
        }
    }

    /**
     * Maps between the osvdb security definitions and the cloud's security mode
     * for the give SSID being configured.
     * 
     * @param ssidSecurityMode
     * @param opensyncSecurityMode
     * @return
     */
    String getOpensyncSecurityMode(String ssidSecurityMode, String opensyncSecurityMode) {
        if (ssidSecurityMode.equals("wpaPSK") || ssidSecurityMode.equals("wpa2PSK")
                || ssidSecurityMode.equals("wpa2OnlyPSK")) {
            opensyncSecurityMode = "WPA-PSK";
        } else if (ssidSecurityMode.equals("wep")) {
            opensyncSecurityMode = "WEP";
        } else if (ssidSecurityMode.equals("wpaEAP") || ssidSecurityMode.equals("wpa2EAP")
                || ssidSecurityMode.equals("wpa2OnlyEAP")) {
            opensyncSecurityMode = "WPA-EAP";
        } else if (ssidSecurityMode.equals("wpaRadius") || ssidSecurityMode.equals("wpa2OnlyRadius")
                || ssidSecurityMode.equals("wpa2Radius")) {
            opensyncSecurityMode = "WPA-EAP";
        } else if (ssidSecurityMode.equals("wpa3OnlySAE") || ssidSecurityMode.equals("wpa3MixedSAE")) {
            opensyncSecurityMode = "WPA-SAE";
        } else if (ssidSecurityMode.equals("wpa3OnlyEAP") || ssidSecurityMode.equals("wpa3MixedEAP")) {
            opensyncSecurityMode = "WPA3-EAP";
        }
        return opensyncSecurityMode;
    }

    public void configureHotspots(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig) {

        provisionHotspot2IconConfig(ovsdbClient, opensyncApConfig);
        provisionHotspot20OsuProviders(ovsdbClient, opensyncApConfig);
        provisionHotspot20Config(ovsdbClient, opensyncApConfig);

    }

    public void configureGreTunnels(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig) {

        LOG.info("Configure Gre tunnels {}", opensyncApConfig.getApProfile());
        if (opensyncApConfig.getApProfile() != null) {
            configureGreTunnel(ovsdbClient, opensyncApConfig.getApProfile());
        }

    }

    private void configureGreTunnel(OvsdbClient ovsdbClient, Profile apNetworkConfiguration) {

        // NAT false
        // if_name gre1
        // if_type gre
        // gre_local_inet_addr (Not needed): WAN IP shall be used
        // gre_remote_inet_addr <Tunnel end point IP address>
        // gre_ifname: Not needed

        try {
            LOG.debug("Configure Gre Tunnel {}", apNetworkConfiguration);
            List<Operation> operations = new ArrayList<>();

            ApNetworkConfiguration details = (ApNetworkConfiguration) apNetworkConfiguration.getDetails();

            for (GreTunnelConfiguration greTunnelConfiguration : details.getGreTunnelConfigurations()) {

                if (greTunnelConfiguration.getGreRemoteInetAddr() == null) {
                    LOG.info("Cannot configure GRE profile without gre_remote_inet_addr");
                    continue;
                }

                if (greTunnelConfiguration.getGreTunnelName() == null) {
                    LOG.info("Cannot configure GRE profile without if_name");
                    continue;
                }

                Map<String, Value> tableColumns = new HashMap<>();
                tableColumns.put("gre_remote_inet_addr",
                        new Atom<>(greTunnelConfiguration.getGreRemoteInetAddr().getHostAddress()));
                tableColumns.put("if_name", new Atom<>(greTunnelConfiguration.getGreTunnelName()));
                tableColumns.put("if_type", new Atom<>("gre"));
                tableColumns.put("enabled", new Atom<>(true));
                tableColumns.put("network", new Atom<>(true));
                tableColumns.put("dhcp_sniff", new Atom<>(true));
                tableColumns.put("NAT", new Atom<>(false));

                operations.add(new Insert(wifiInetConfigDbTable, new Row(tableColumns)));

            }

            if (operations.isEmpty()) {
                LOG.info("No GRE tunnels to be configured.");
                return;
            }
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            for (OperationResult res : result) {
                if (res instanceof InsertResult) {
                    LOG.info("configureGreTunnel {}", ((InsertResult) res).toString());
                } else if (res instanceof UpdateResult) {
                    LOG.info("configureGreTunnel {}", ((UpdateResult) res).toString());
                } else if (res instanceof ErrorResult) {
                    LOG.error("configureGreTunnel error {}", ((ErrorResult) res));
                    throw new RuntimeException("configureGreTunnel " + ((ErrorResult) res).getError() + " "
                            + ((ErrorResult) res).getDetails());
                }
            }
        } catch (OvsdbClientException | InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Couldn't configure Gre Tunnel {}", apNetworkConfiguration, e);
            throw new RuntimeException(e);
        }
    }

    public void createVlanNetworkInterfaces(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig) {
        Set<Integer> vlans = new HashSet<>();
        for (Profile ssidProfile : opensyncApConfig.getSsidProfile()) {
            Integer vlanId = ((SsidConfiguration) ssidProfile.getDetails()).getVlanId();
            if (vlanId != null && vlanId > 1) {
                vlans.add(vlanId);
            }
        }
        for (Integer vlanId : vlans) {

            Optional<GreTunnelConfiguration> tunnelConfiguration = ((ApNetworkConfiguration) opensyncApConfig
                    .getApProfile().getDetails()).getGreTunnelConfigurations().stream()
                            .filter(new Predicate<GreTunnelConfiguration>() {

                                @Override
                                public boolean test(GreTunnelConfiguration t) {

                                    return t.getVlanIdsInGreTunnel().contains(vlanId);
                                }

                            }).findFirst();

            if (tunnelConfiguration.isPresent()) {
                createVlanInterfaceInGreTunnel(ovsdbClient, vlanId, tunnelConfiguration.get().getGreTunnelName());
            } else {
                createVlanNetworkInterfaces(ovsdbClient, vlanId);
            }

        }
    }

    private void createVlanInterfaceInGreTunnel(OvsdbClient ovsdbClient, int vlanId, String greTunnel) {
        try {

            // if_name gre_<vlan id>
            // if_type vlan
            // parent_ifname gre
            // vlan_id <vlan id>

            List<Operation> operations = new ArrayList<>();
            Map<String, Value> tableColumns = new HashMap<>();

            Map<String, WifiInetConfigInfo> inetConfigMap = getProvisionedWifiInetConfigs(ovsdbClient);

            WifiInetConfigInfo parentTunnel = inetConfigMap.get(greTunnel);
            if (parentTunnel == null)
                throw new RuntimeException("Cannot get tunnel interface " + parentTunnel + " for vlan " + vlanId);

            tableColumns = new HashMap<>();

            tableColumns.put("if_type", new Atom<>("vlan"));
            tableColumns.put("vlan_id", new Atom<>(vlanId));
            tableColumns.put("if_name", new Atom<>("gre_" + Integer.toString(vlanId)));
            tableColumns.put("parent_ifname", new Atom<>("gre"));
            tableColumns.put("enabled", new Atom<>(true));
            tableColumns.put("network", new Atom<>(true));
            tableColumns.put("dhcp_sniff", new Atom<>(true));

            Row row = new Row(tableColumns);

            if (inetConfigMap.containsKey("gre_" + Integer.toString(vlanId))) {
                List<Condition> conditions = new ArrayList<>();
                conditions.add(new Condition("vlan_id", Function.EQUALS, new Atom<>(vlanId)));
                conditions.add(new Condition("parent_ifname", Function.EQUALS, new Atom<>("gre")));
            } else {
                operations.add(new Insert(wifiInetConfigDbTable, row));
            }

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            for (OperationResult res : result) {

                if (res instanceof InsertResult) {
                    LOG.info("createVlanNetworkInterfaces {}", ((InsertResult) res).toString());
                } else if (res instanceof UpdateResult) {

                    LOG.info("createVlanNetworkInterfaces {}", ((UpdateResult) res).toString());
                } else if (res instanceof ErrorResult) {
                    LOG.error("createVlanNetworkInterfaces error {}", ((ErrorResult) res));
                    throw new RuntimeException("createVlanNetworkInterfaces " + ((ErrorResult) res).getError() + " "
                            + ((ErrorResult) res).getDetails());
                }
            }

            inetConfigMap = getProvisionedWifiInetConfigs(ovsdbClient);

            LOG.debug("Provisioned vlan on greTunnel {}",
                    inetConfigMap.get(parentTunnel.ifName + "_" + Integer.toString(vlanId)));

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in provisioning Vlan", e);
            throw new RuntimeException(e);
        }

    }

    private void createVlanNetworkInterfaces(OvsdbClient ovsdbClient, int vlanId) {
        try {

            List<Operation> operations = new ArrayList<>();
            Map<String, Value> tableColumns = new HashMap<>();

            Map<String, WifiInetConfigInfo> inetConfigMap = getProvisionedWifiInetConfigs(ovsdbClient);

            WifiInetConfigInfo parentLanInterface = inetConfigMap.get(defaultLanInterfaceName);
            if (parentLanInterface == null)
                throw new RuntimeException(
                        "Cannot get lan interface " + defaultLanInterfaceName + " for vlan " + vlanId);

            tableColumns.put("if_type", new Atom<>("vlan"));
            tableColumns.put("vlan_id", new Atom<>(vlanId));
            tableColumns.put("if_name", new Atom<>(parentLanInterface.ifName + "_" + Integer.toString(vlanId)));
            tableColumns.put("parent_ifname", new Atom<>(parentLanInterface.ifName));
            tableColumns.put("enabled", new Atom<>(true));
            tableColumns.put("network", new Atom<>(true));
            tableColumns.put("dhcp_sniff", new Atom<>(true));

            tableColumns.put("ip_assign_scheme", new Atom<>(parentLanInterface.ipAssignScheme));
            tableColumns.put("NAT", new Atom<>(parentLanInterface.nat));
            tableColumns.put("mtu", new Atom<>(1500));

            String[] inetAddress = parentLanInterface.inetAddr.split("\\.");
            String vlanAddress = inetAddress[0] + "." + inetAddress[1] + "." + vlanId + "." + inetAddress[3];
            tableColumns.put("inet_addr", new Atom<>(vlanAddress));
            tableColumns.put("netmask", new Atom<>(parentLanInterface.netmask));
            tableColumns.put("dhcpd", com.vmware.ovsdb.protocol.operation.notation.Map.of(parentLanInterface.dhcpd));

            Row row = new Row(tableColumns);

            if (inetConfigMap.containsKey(parentLanInterface.ifName + "_" + Integer.toString(vlanId))) {
                List<Condition> conditions = new ArrayList<>();
                conditions.add(new Condition("vlan_id", Function.EQUALS, new Atom<>(vlanId)));
                conditions.add(new Condition("parent_ifname", Function.EQUALS, new Atom<>(parentLanInterface.ifName)));

                operations.add(new Update(wifiInetConfigDbTable, conditions, row));
            } else {
                operations.add(new Insert(wifiInetConfigDbTable, row));
            }

            WifiInetConfigInfo parentWanInterface = inetConfigMap.get(defaultWanInterfaceName);
            if (parentWanInterface == null)
                throw new RuntimeException(
                        "Cannot get wan interface " + defaultWanInterfaceName + " for vlan " + vlanId);

            tableColumns = new HashMap<>();

            tableColumns.put("if_type", new Atom<>("vlan"));
            tableColumns.put("vlan_id", new Atom<>(vlanId));
            tableColumns.put("if_name", new Atom<>(parentWanInterface.ifName + "_" + Integer.toString(vlanId)));
            tableColumns.put("parent_ifname", new Atom<>(parentWanInterface.ifName));
            tableColumns.put("enabled", new Atom<>(true));
            tableColumns.put("network", new Atom<>(true));
            tableColumns.put("dhcp_sniff", new Atom<>(true));
            tableColumns.put("ip_assign_scheme", new Atom<>(parentWanInterface.ipAssignScheme));
            tableColumns.put("NAT", new Atom<>(parentWanInterface.nat));

            tableColumns.put("mtu", new Atom<>(1500));

            row = new Row(tableColumns);

            if (inetConfigMap.containsKey(parentWanInterface.ifName + "_" + Integer.toString(vlanId))) {
                List<Condition> conditions = new ArrayList<>();
                conditions.add(new Condition("vlan_id", Function.EQUALS, new Atom<>(vlanId)));
                conditions.add(new Condition("parent_ifname", Function.EQUALS, new Atom<>(parentWanInterface.ifName)));
            } else {
                operations.add(new Insert(wifiInetConfigDbTable, row));
            }

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            for (OperationResult res : result) {

                if (res instanceof InsertResult) {
                    LOG.info("createVlanNetworkInterfaces {}", ((InsertResult) res).toString());
                } else if (res instanceof UpdateResult) {

                    LOG.info("createVlanNetworkInterfaces {}", ((UpdateResult) res).toString());
                } else if (res instanceof ErrorResult) {
                    LOG.error("createVlanNetworkInterfaces error {}", ((ErrorResult) res));
                    throw new RuntimeException("createVlanNetworkInterfaces " + ((ErrorResult) res).getError() + " "
                            + ((ErrorResult) res).getDetails());
                }
            }

            inetConfigMap = getProvisionedWifiInetConfigs(ovsdbClient);

            LOG.debug("Provisioned vlan on wan {} and lan {}",
                    inetConfigMap.get(parentWanInterface.ifName + "_" + Integer.toString(vlanId)),
                    inetConfigMap.get(parentLanInterface.ifName + "_" + Integer.toString(vlanId)));

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in provisioning Vlan", e);
            throw new RuntimeException(e);
        }

    }

    void getRadiusAccountingConfiguration(OpensyncAPConfig opensyncApConfig, SsidConfiguration ssidConfig,
            Map<String, String> security) {

        LOG.debug("getRadiusAccountingConfiguration for ssidConfig {} from radiusProfiles {}", ssidConfig,
                opensyncApConfig.getRadiusProfiles());

        LOG.debug("Radius Accounting Profiles {}", opensyncApConfig.getRadiusProfiles());

        List<Profile> radiusProfileList = opensyncApConfig.getRadiusProfiles().stream()
                .filter(new Predicate<Profile>() {

                    @Override
                    public boolean test(Profile t) {
                        return t.getId() == ssidConfig.getRadiusServiceId();
                    }

                }).collect(Collectors.toList());

        if (radiusProfileList != null && radiusProfileList.size() > 0) {
            Profile profileRadius = radiusProfileList.get(0);
            RadiusProfile profileDetails = ((RadiusProfile) profileRadius.getDetails());
            RadiusServer rServer = profileDetails.getPrimaryRadiusAccountingServer();
            if (rServer != null) {
                security.put("radius_acct_ip",
                        rServer.getIpAddress() != null ? rServer.getIpAddress().getHostAddress() : null);
                security.put("radius_acct_port", rServer.getPort() != null ? String.valueOf(rServer.getPort()) : null);
                security.put("radius_acct_secret", rServer.getSecret());
                if (ssidConfig.getRadiusAcountingServiceInterval() != null) {
                    // if the value is present, use the
                    // radius_acct_interval
                    security.put("radius_acct_interval", ssidConfig.getRadiusAcountingServiceInterval().toString());

                } else {
                    LOG.info("No radius_acct_interval defined for ssid {}, Setting radius_acct_interval to 0",
                            ssidConfig.getSsid(), rServer);
                    security.put("radius_acct_interval", "0");
                }
                LOG.info(
                        "set Radius Accounting server attributes radius_acct_ip {} radius_acct_port {} radius_acct_secret {} radius_acct_interval {}",
                        security.get("radius_acct_ip"), security.get("radius_acct_port"),
                        security.get("radius_acct_secret"), security.get("radius_acct_interval"));
            } else {
                LOG.info("No Radius Accounting Server defined in Radius Profile");
            }

        } else {
            LOG.warn("Could not find radius profile {} in {}", ssidConfig.getRadiusServiceId(),
                    opensyncApConfig.getRadiusProfiles());
        }

    }

    void getRadiusConfiguration(OpensyncAPConfig opensyncApConfig, SsidConfiguration ssidConfig,
            Map<String, String> security) {

        LOG.debug("getRadiusConfiguration for ssidConfig {} from radiusProfiles {}", ssidConfig,
                opensyncApConfig.getRadiusProfiles());

        LOG.debug("Radius Profiles {}", opensyncApConfig.getRadiusProfiles());

        List<Profile> radiusProfileList = opensyncApConfig.getRadiusProfiles().stream()
                .filter(new Predicate<Profile>() {

                    @Override
                    public boolean test(Profile t) {
                        return t.getId() == ssidConfig.getRadiusServiceId();
                    }

                }).collect(Collectors.toList());

        if (radiusProfileList != null && radiusProfileList.size() > 0) {
            Profile profileRadius = radiusProfileList.get(0);
            RadiusProfile profileDetails = ((RadiusProfile) profileRadius.getDetails());
            RadiusServer radiusServer = profileDetails.getPrimaryRadiusAuthServer();
            security.put("radius_server_ip",
                    radiusServer.getIpAddress() != null ? radiusServer.getIpAddress().getHostAddress() : null);
            security.put("radius_server_port",
                    radiusServer.getPort() != null ? String.valueOf(radiusServer.getPort()) : null);
            security.put("radius_server_secret", radiusServer.getSecret());
            LOG.info("set Radius server attributes radius_server_ip {} radius_server_port {} radius_server_secret {}",
                    security.get("radius_server_ip"), security.get("radius_server_port"),
                    security.get("radius_server_secret"));

        } else {
            LOG.warn("Could not find radius profile {} in {}", ssidConfig.getRadiusServiceId(),
                    opensyncApConfig.getRadiusProfiles());
        }
    }

    /**
     * 
     * @param opensyncApConfig
     * @param ssidConfig
     * @param captiveMap
     * @param walledGardenAllowlist
     */
    void getCaptiveConfiguration(OpensyncAPConfig opensyncApConfig, SsidConfiguration ssidConfig,
            Map<String, String> captiveMap, List<String> walledGardenAllowlist) {
        if ((ssidConfig.getCaptivePortalId() != null) && (opensyncApConfig.getCaptiveProfiles() != null)) {
            for (Profile profileCaptive : opensyncApConfig.getCaptiveProfiles()) {
                if ((ssidConfig.getCaptivePortalId() == profileCaptive.getId())
                        && (profileCaptive.getDetails() != null)) {
                    CaptivePortalConfiguration captiveProfileDetails = ((CaptivePortalConfiguration) profileCaptive
                            .getDetails());

                    // +#define SCHEMA_CONSTS_PAGE_TITLE "page_title"
                    if (captiveProfileDetails.getBrowserTitle() != null) {
                        captiveMap.put("session_timeout",
                                String.valueOf(captiveProfileDetails.getSessionTimeoutInMinutes()));
                        captiveMap.put("page_title", captiveProfileDetails.getBrowserTitle());
                    }
                    if (captiveProfileDetails.getAuthenticationType().equals(CaptivePortalAuthenticationType.radius)) {
                        Optional<Profile> optional = opensyncApConfig.getRadiusProfiles().stream()
                                .filter(p -> p.getId() == captiveProfileDetails.getRadiusServiceId()).findFirst();
                        if (optional != null && optional.isPresent()) {
                            Profile profile = optional.get();
                            RadiusProfile radiusProfile = (RadiusProfile) profile.getDetails();
                            captiveMap.put("radius_server_ip", String.valueOf(
                                    radiusProfile.getPrimaryRadiusAuthServer().getIpAddress().getHostAddress()));

                            captiveMap.put("radius_server_port",
                                    String.valueOf(radiusProfile.getPrimaryRadiusAuthServer().getPort()));

                            captiveMap.put("radius_server_secret",
                                    String.valueOf(radiusProfile.getPrimaryRadiusAuthServer().getSecret()));
                            if (captiveProfileDetails.getRadiusAuthMethod() != null) {

                                captiveMap.put("radius_auth_type",
                                        String.valueOf(captiveProfileDetails.getRadiusAuthMethod()));
                            }
                        }
                    }

                    if (captiveProfileDetails.getRedirectURL() != null) {
                        captiveMap.put("redirect_url", captiveProfileDetails.getRedirectURL());
                    }
                    captiveMap.put("session_timeout",
                            String.valueOf(captiveProfileDetails.getSessionTimeoutInMinutes()));

                    captiveMap.put("browser_title", captiveProfileDetails.getBrowserTitle());
                    captiveMap.put("splash_page_title", captiveProfileDetails.getHeaderContent());
                    captiveMap.put("acceptance_policy", captiveProfileDetails.getUserAcceptancePolicy());
                    captiveMap.put("login_success_text", captiveProfileDetails.getSuccessPageMarkdownText());
                    captiveMap.put("authentication",
                            getCaptiveAuthentication(captiveProfileDetails.getAuthenticationType()));
                    if (captiveProfileDetails.getUsernamePasswordFile() != null) {
                        captiveMap
                                .put("username_password_file",
                                        ManagedFileInfo
                                                .resolveWithPopulatedHostname(
                                                        captiveProfileDetails.getUsernamePasswordFile(), FILESTORE)
                                                .getApExportUrl());
                    }
                    if (captiveProfileDetails.getLogoFile() != null) {
                        captiveMap.put("splash_page_logo",
                                ManagedFileInfo
                                        .resolveWithPopulatedHostname(captiveProfileDetails.getLogoFile(), FILESTORE)
                                        .getApExportUrl());
                    }
                    if (captiveProfileDetails.getBackgroundFile() != null) {
                        captiveMap.put("splash_page_background_logo", ManagedFileInfo
                                .resolveWithPopulatedHostname(captiveProfileDetails.getBackgroundFile(), FILESTORE)
                                .getApExportUrl());
                    }
                    LOG.debug("captiveMap {}", captiveMap);
                    walledGardenAllowlist.addAll(captiveProfileDetails.getWalledGardenAllowlist());

                }
            }
        }
    }

    /**
     * 
     * @param authentication
     * @return ovsdb value for captive portal authentication map entry based on
     *         cloud type.
     */
    String getCaptiveAuthentication(CaptivePortalAuthenticationType authentication) {
        switch (authentication) {
        case guest:
            return "None";
        case username:
            return "Captive Portal User List";
        case radius:
            return "RADIUS";
        default:
            LOG.error("Unsupported captive portal authentication {}", authentication);
            return "None";
        }
    }

    private void getBonjourGatewayConfiguration(OpensyncAPConfig opensyncApConfig, SsidConfiguration ssidConfig,
            Map<Short, Set<String>> bonjourServiceMap) {
        if ((ssidConfig.getBonjourGatewayProfileId() != null)
                && (opensyncApConfig.getBonjourGatewayProfiles() != null)) {
            for (Profile profileBonjour : opensyncApConfig.getBonjourGatewayProfiles()) {
                if ((ssidConfig.getBonjourGatewayProfileId() == profileBonjour.getId())
                        && (profileBonjour.getDetails() != null)) {

                    BonjourGatewayProfile bonjourGatewayConfiguration = (BonjourGatewayProfile) profileBonjour
                            .getDetails();

                    Collection<BonjourServiceSet> bonjourServicesCollection = bonjourGatewayConfiguration
                            .getBonjourServices();
                    bonjourServicesCollection.stream().forEach(b -> {
                        Set<String> serviceSet = new HashSet<>();
                        if (bonjourServiceMap.containsKey(b.getVlanId())) {
                            serviceSet.addAll(bonjourServiceMap.get(b.getVlanId()));
                        }
                        serviceSet.addAll(b.getServiceNames());
                        bonjourServiceMap.put(b.getVlanId(), serviceSet);
                    });

                    LOG.debug("bonjourServiceMap {}", bonjourServiceMap);
                }
            }
        }
    }

    /**
     * Insert or update Wifi_Inet_Interface for Wifi_VIF_Config table entry
     * 
     * @param ovsdbClient
     * @param ifName
     * @param enabled
     * @param ifType
     * @param isUpdate
     * @param isNat
     */
    private void configureInetInterface(OvsdbClient ovsdbClient, String ifName, boolean enabled, String ifType,
            boolean isUpdate, boolean isNat) {

        try {

            List<Operation> operations = new ArrayList<>();
            Map<String, Value> tableColumns = new HashMap<>();

            tableColumns.put("if_type", new Atom<>(ifType));
            tableColumns.put("enabled", new Atom<>(enabled));
            tableColumns.put("network", new Atom<>(true));
            tableColumns.put("if_name", new Atom<>(ifName));
            tableColumns.put("NAT", new Atom<>(isNat));
            tableColumns.put("dhcp_sniff", new Atom<>(true));

            Row row = new Row(tableColumns);
            if (isUpdate) {
                List<Condition> conditions = new ArrayList<>();
                conditions.add(new Condition("if_name", Function.EQUALS, new Atom<>(ifName)));
                operations.add(new Update(wifiInetConfigDbTable, conditions, row));
            } else {
                operations.add(new Insert(wifiInetConfigDbTable, row));
            }
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            LOG.debug("Updated Inet {}", ifName);

            for (OperationResult res : result) {

                if (res instanceof InsertResult) {
                    LOG.info("configureInetInterface insert new row result {}", ((InsertResult) res));
                    // for insert, make sure it is actually in the table
                    confirmRowExistsInTable(ovsdbClient, ((InsertResult) res).getUuid(), wifiInetConfigDbTable);
                } else if (res instanceof UpdateResult) {
                    LOG.info("configureInetInterface update new row result {}", ((UpdateResult) res));
                } else if (res instanceof ErrorResult) {
                    LOG.error("configureInetInterface error {}", ((ErrorResult) res));
                    throw new RuntimeException("configureInetInterface " + ((ErrorResult) res).getError() + " "
                            + ((ErrorResult) res).getDetails());
                }

            }

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in updateWifiInetConfig", e);
            throw new RuntimeException(e);
        }

    }

    /**
     * Configure a Hotspot20 Passpoint for AP
     * 
     * @param ovsdbClient
     * @param opensyncApConfig
     */
    public void provisionHotspot20Config(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig) {
        try {
            DatabaseSchema schema = ovsdbClient.getSchema(ovsdbName).get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            if (schema.getTables().containsKey(hotspot20ConfigDbTable)
                    && schema.getTables().get(hotspot20ConfigDbTable) != null) {
                Map<String, Hotspot20Config> hotspot20ConfigMap = getProvisionedHotspot20Configs(ovsdbClient);

                OpensyncAPHotspot20Config hs20cfg = opensyncApConfig.getHotspotConfig();

                if (hs20cfg.getHotspot20ProfileSet() != null) {
                    List<Operation> operations = new ArrayList<>();
                    for (Profile hotspotProfile : hs20cfg.getHotspot20ProfileSet()) {

                        Map<String, Value> rowColumns = new HashMap<>();

                        PasspointProfile hs2Profile = (PasspointProfile) hotspotProfile.getDetails();

                        getPasspointConfigurationInformationFromDependencies(ovsdbClient, opensyncApConfig, hs20cfg,
                                hotspotProfile, rowColumns, hs2Profile);

                        rowColumns.put("deauth_request_timeout", new Atom<>(hs2Profile.getDeauthRequestTimeout()));
                        rowColumns.put("tos", new Atom<>(hs2Profile.getTermsAndConditionsFile().getApExportUrl()));
                        rowColumns.put("enable", new Atom<>(hs2Profile.isEnableInterworkingAndHs20()));
                        rowColumns.put("network_auth_type",
                                new Atom<>("0" + hs2Profile.getNetworkAuthenticationType().getId()));
                        rowColumns.put("gas_addr3_behavior", new Atom<>(hs2Profile.getGasAddr3Behaviour().getId()));
                        rowColumns.put("operating_class", new Atom<>(hs2Profile.getOperatingClass()));
                        rowColumns.put("anqp_domain_id", new Atom<>(hs2Profile.getAnqpDomainId()));

                        Set<Atom<String>> connectionCapabilities = new HashSet<>();
                        hs2Profile.getConnectionCapabilitySet().stream()
                                .forEach(c -> connectionCapabilities
                                        .add(new Atom<>(c.getConnectionCapabilitiesIpProtocol().getId() + ":"
                                                + c.getConnectionCapabilitiesPortNumber() + ":"
                                                + c.getConnectionCapabilitiesStatus().getId())));
                        com.vmware.ovsdb.protocol.operation.notation.Set connectionCapabilitySet = com.vmware.ovsdb.protocol.operation.notation.Set
                                .of(connectionCapabilities);
                        rowColumns.put("connection_capability", connectionCapabilitySet);

                        // # format: <1-octet encoded value as hex str>
                        // # (ipv4_type & 0x3f) << 2 | (ipv6_type & 0x3) << 2
                        // 0x3f = 63 in decimal
                        // 0x3 = 3 in decimal
                        if (PasspointIPv6AddressType.getByName(
                                hs2Profile.getIpAddressTypeAvailability()) != PasspointIPv6AddressType.UNSUPPORTED) {
                            int availability = PasspointIPv6AddressType
                                    .getByName(hs2Profile.getIpAddressTypeAvailability()).getId();
                            String hexString = Integer.toHexString((availability & 3) << 2);
                            rowColumns.put("ipaddr_type_availability", new Atom<>(hexString));
                        } else if (PasspointIPv4AddressType.getByName(
                                hs2Profile.getIpAddressTypeAvailability()) != PasspointIPv4AddressType.UNSUPPORTED) {
                            int availability = PasspointIPv4AddressType
                                    .getByName(hs2Profile.getIpAddressTypeAvailability()).getId();
                            String hexString = Integer.toHexString((availability & 63) << 2);
                            rowColumns.put("ipaddr_type_availability", new Atom<>(hexString));
                        }

                        Row row = new Row(rowColumns);

                        Insert newHs20Config = new Insert(hotspot20ConfigDbTable, row);

                        operations.add(newHs20Config);

                    }

                    CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
                    OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

                    for (OperationResult res : result) {
                        LOG.debug("provisionHotspot20Config Op Result {}", res);
                        if (res instanceof InsertResult) {
                            LOG.info("provisionHotspot20Config insert new row result {}", ((InsertResult) res));
                            // for insert, make sure it is actually in the table
                            confirmRowExistsInTable(ovsdbClient, ((InsertResult) res).getUuid(),
                                    hotspot20ConfigDbTable);
                        }
                    }

                }

                LOG.info("Current Hotspot20_Config {}", hotspot20ConfigMap);
            } else {
                LOG.info("Table {} not present in {}. Cannot provision Hotspot20_Config", hotspot20ConfigDbTable,
                        ovsdbName);
            }
        } catch (InterruptedException | ExecutionException | TimeoutException | OvsdbClientException e) {
            LOG.error("Error in provisionHotspot20Config", e);
            throw new RuntimeException(e);
        }

    }

    /**
     * Passpoint configuration requires profile information from children, as
     * well as information from parent VIF Configurations These values are
     * placed in the rowColumns map to be passed into the transaction creating
     * the Hotspot20_Config in ovsdb.
     * 
     * @param ovsdbClient
     * @param opensyncApConfig
     * @param hs20cfg
     * @param hotspotProfile
     * @param rowColumns
     * @param hs2Profile
     */
    void getPasspointConfigurationInformationFromDependencies(OvsdbClient ovsdbClient,
            OpensyncAPConfig opensyncApConfig, OpensyncAPHotspot20Config hs20cfg, Profile hotspotProfile,
            Map<String, Value> rowColumns, PasspointProfile hs2Profile) {

        getOperatorInformationForPasspointConfiguration(hs20cfg, rowColumns, hs2Profile);

        getVenueInformationForPasspointConfiguration(hs20cfg, rowColumns, hs2Profile);

        getOsuIconUuidsForPasspointConfiguration(ovsdbClient, hs20cfg, hotspotProfile, rowColumns);

        getVifInformationForPasspointConfiguration(ovsdbClient, opensyncApConfig, rowColumns, hs2Profile);
    }

    /**
     * Get the UUIDs for the associated access Wifi_VIF_Config parents, as well
     * as the osu_ssid for the "OPEN" Wifi_VIF_Config used to connect to the
     * passpoint
     * 
     * @param ovsdbClient
     * @param opensyncApConfig
     * @param rowColumns
     * @param hs2Profile
     */
    void getVifInformationForPasspointConfiguration(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig,
            Map<String, Value> rowColumns, PasspointProfile hs2Profile) {
        Map<String, WifiVifConfigInfo> vifConfigMap = getProvisionedWifiVifConfigs(ovsdbClient);

        Set<Uuid> vifConfigs = new HashSet<>();
        List<Atom<String>> hessids = new ArrayList<>();

        for (Profile ssidProfile : opensyncApConfig.getSsidProfile()) {
            if (hs2Profile.getAssociatedAccessSsidProfileIds().contains(ssidProfile.getId())) {

                String accessSsidProfileName = ((SsidConfiguration) ssidProfile.getDetails()).getSsid();

                for (WifiVifConfigInfo vifConfig : vifConfigMap.values()) {
                    if (vifConfig.ssid.equals(accessSsidProfileName)) {
                        vifConfigs.add(vifConfig.uuid);
                    }
                }

                List<String> vifStates = getWifiVifStates(ovsdbClient, accessSsidProfileName);
                for (String mac : vifStates) {
                    hessids.add(new Atom<>(mac));
                }

            }
        }

        if (vifConfigs.size() > 0) {
            com.vmware.ovsdb.protocol.operation.notation.Set vifConfigUuids = com.vmware.ovsdb.protocol.operation.notation.Set
                    .of(vifConfigs);
            rowColumns.put("vif_config", vifConfigUuids);
        }

        if (hessids.size() > 0) {
            rowColumns.put("hessid", new Atom<>(hessids.get(0)));
        }

        for (Profile ssidProfile : opensyncApConfig.getSsidProfile()) {
            if (ssidProfile.getId() == hs2Profile.getOsuSsidProfileId()) {
                rowColumns.put("osu_ssid", new Atom<>(((SsidConfiguration) ssidProfile.getDetails()).getSsid()));
                break;
            }
        }
    }

    /**
     * Adds map entries the UUIDs for the OSU Providers and Icons based on the
     * entries in the for providers that are associated with this
     * hotspotProfile.
     * 
     * @param ovsdbClient
     * @param hs20cfg
     * @param hotspotProfile
     * @param rowColumns
     */
    void getOsuIconUuidsForPasspointConfiguration(OvsdbClient ovsdbClient, OpensyncAPHotspot20Config hs20cfg,
            Profile hotspotProfile, Map<String, Value> rowColumns) {
        Set<Uuid> osuIconUuids = getOsuProvidersInfoForPasspointConfiguration(ovsdbClient, hs20cfg, hotspotProfile,
                rowColumns);

        if (osuIconUuids.size() > 0) {
            com.vmware.ovsdb.protocol.operation.notation.Set iconUuids = com.vmware.ovsdb.protocol.operation.notation.Set
                    .of(osuIconUuids);
            rowColumns.put("operator_icons", iconUuids);
        }
    }

    /**
     * Add the operator specific information, taken from the operator profile
     * for the given hotspotProfile being configured.
     * 
     * @param hs20cfg
     * @param rowColumns
     * @param hs2Profile
     */
    void getOperatorInformationForPasspointConfiguration(OpensyncAPHotspot20Config hs20cfg,
            Map<String, Value> rowColumns, PasspointProfile hs2Profile) {
        PasspointOperatorProfile passpointOperatorProfile = getOperatorProfileForPasspoint(hs20cfg, hs2Profile);

        Set<Atom<String>> domainNames = new HashSet<>();
        for (String domainName : passpointOperatorProfile.getDomainNameList()) {
            domainNames.add(new Atom<>(domainName));
        }
        if (domainNames.size() > 0) {
            com.vmware.ovsdb.protocol.operation.notation.Set domainNameSet = com.vmware.ovsdb.protocol.operation.notation.Set
                    .of(domainNames);
            rowColumns.put("domain_name", domainNameSet);
        }
        rowColumns.put("osen", new Atom<>(passpointOperatorProfile.isServerOnlyAuthenticatedL2EncryptionNetwork()));

        Set<Atom<String>> operatorFriendlyName = new HashSet<>();
        passpointOperatorProfile.getOperatorFriendlyName().stream()
                .forEach(c -> operatorFriendlyName.add(new Atom<>(c.getAsDuple())));
        com.vmware.ovsdb.protocol.operation.notation.Set operatorFriendlyNameSet = com.vmware.ovsdb.protocol.operation.notation.Set
                .of(operatorFriendlyName);
        rowColumns.put("operator_friendly_name", operatorFriendlyNameSet);

    }

    /**
     * Add the venue specific information, taken from the venue profile for the
     * given hotspotProfile being configured.
     * 
     * @param hs20cfg
     * @param rowColumns
     * @param hs2Profile
     */
    void getVenueInformationForPasspointConfiguration(OpensyncAPHotspot20Config hs20cfg, Map<String, Value> rowColumns,
            PasspointProfile hs2Profile) {
        PasspointVenueProfile passpointVenueProfile = getVenueProfileForPasspoint(hs20cfg, hs2Profile);
        Set<Atom<String>> venueNames = new HashSet<>();
        Set<Atom<String>> venueUrls = new HashSet<>();
        int index = 1;
        for (PasspointVenueName passpointVenueName : passpointVenueProfile.getVenueNameSet()) {
            venueNames.add(new Atom<String>(passpointVenueName.getAsDuple()));
            String url = String.valueOf(index) + ":" + passpointVenueName.getVenueUrl();
            venueUrls.add(new Atom<String>(url));
            index++;
        }
        com.vmware.ovsdb.protocol.operation.notation.Set venueNameSet = com.vmware.ovsdb.protocol.operation.notation.Set
                .of(venueNames);
        com.vmware.ovsdb.protocol.operation.notation.Set venueUrlSet = com.vmware.ovsdb.protocol.operation.notation.Set
                .of(venueUrls);
        rowColumns.put("venue_name", venueNameSet);
        rowColumns.put("venue_url", venueUrlSet);

        PasspointVenueTypeAssignment passpointVenueTypeAssignment = passpointVenueProfile.getVenueTypeAssignment();
        String groupType = String.valueOf(passpointVenueTypeAssignment.getVenueGroupId()) + ":"
                + passpointVenueTypeAssignment.getVenueTypeId();

        rowColumns.put("venue_group_type", new Atom<>(groupType));
    }

    /**
     * Get's the OSU Provider related information for a given hotspot, the osu
     * providers being configured on the ovsdb in Hotspot20_OSU_Providers and
     * defined as children of the hotspot profile
     * 
     * @param ovsdbClient
     * @param hs20cfg
     * @param hotspotProfile
     * @param rowColumns
     * @return
     */
    Set<Uuid> getOsuProvidersInfoForPasspointConfiguration(OvsdbClient ovsdbClient, OpensyncAPHotspot20Config hs20cfg,
            Profile hotspotProfile, Map<String, Value> rowColumns) {
        Map<String, Hotspot20OsuProviders> osuProviders = getProvisionedHotspot20OsuProviders(ovsdbClient);
        List<Profile> providerList = getOsuProvidersForPasspoint(hs20cfg, hotspotProfile);

        Set<Uuid> osuProvidersUuids = new HashSet<>();
        Set<Uuid> osuIconUuids = new HashSet<>();

        StringBuffer mccMncBuffer = new StringBuffer();
        Set<Atom<String>> naiRealms = new HashSet<>();
        Set<Atom<String>> roamingOis = new HashSet<>();
        for (Profile provider : providerList) {
            PasspointOsuProviderProfile providerProfile = (PasspointOsuProviderProfile) provider.getDetails();

            osuProviders.keySet().stream().filter(new Predicate<String>() {

                @Override
                public boolean test(String providerNameOnAp) {
                    return providerNameOnAp.startsWith(OvsdbToWlanCloudTypeMappingUtility
                            .getApOsuProviderStringFromOsuProviderName(provider.getName()));
                }

            }).forEach(p -> {
                providerProfile.getRoamingOi().stream().forEach(o -> {
                    roamingOis.add(new Atom<>(o));
                });
                osuProvidersUuids.add(osuProviders.get(p).uuid);
                osuIconUuids.addAll(osuProviders.get(p).osuIcons);
                getNaiRealms(providerProfile, naiRealms);

                for (PasspointMccMnc passpointMccMnc : providerProfile.getMccMncList()) {
                    mccMncBuffer.append(passpointMccMnc.getMccMncPairing());
                    mccMncBuffer.append(";");
                }
            });

        }

        String mccMncString = mccMncBuffer.toString();
        if (mccMncString.endsWith(";")) {
            mccMncString = mccMncString.substring(0, mccMncString.lastIndexOf(";"));
        }

        rowColumns.put("mcc_mnc", new Atom<>(mccMncString));

        com.vmware.ovsdb.protocol.operation.notation.Set roamingOiSet = com.vmware.ovsdb.protocol.operation.notation.Set
                .of(roamingOis);
        rowColumns.put("roaming_oi", roamingOiSet);

        com.vmware.ovsdb.protocol.operation.notation.Set naiRealmsSet = com.vmware.ovsdb.protocol.operation.notation.Set
                .of(naiRealms);
        rowColumns.put("nai_realm", naiRealmsSet);

        if (osuProvidersUuids.size() > 0) {
            com.vmware.ovsdb.protocol.operation.notation.Set providerUuids = com.vmware.ovsdb.protocol.operation.notation.Set
                    .of(osuProvidersUuids);
            rowColumns.put("osu_providers", providerUuids);
        }
        return osuIconUuids;
    }

    /**
     * Get providers profiles. Helper method.
     * 
     * @param hs20cfg
     * @param hotspotProfile
     * @return
     */
    List<Profile> getOsuProvidersForPasspoint(OpensyncAPHotspot20Config hs20cfg, Profile hotspotProfile) {
        List<Profile> providerList = new ArrayList<>();
        if (hs20cfg.getHotspot20ProviderSet() != null) {
            providerList = hs20cfg.getHotspot20ProviderSet().stream().filter(new Predicate<Profile>() {

                @Override
                public boolean test(Profile t) {
                    return hotspotProfile.getChildProfileIds().contains(t.getId());
                }
            }).collect(Collectors.toList());

        }
        return providerList;
    }

    /**
     * Get's the Venue Profile for the hotspot, helper method
     * 
     * @param hs20cfg
     * @param hs2Profile
     * @return
     */
    PasspointVenueProfile getVenueProfileForPasspoint(OpensyncAPHotspot20Config hs20cfg, PasspointProfile hs2Profile) {
        Profile venue = hs20cfg.getHotspot20VenueSet().stream().filter(new Predicate<Profile>() {

            @Override
            public boolean test(Profile t) {
                return t.getId() == hs2Profile.getPasspointVenueProfileId();
            }

        }).findFirst().get();

        PasspointVenueProfile passpointVenueProfile = (PasspointVenueProfile) venue.getDetails();
        return passpointVenueProfile;
    }

    PasspointOperatorProfile getOperatorProfileForPasspoint(OpensyncAPHotspot20Config hs20cfg,
            PasspointProfile hs2Profile) {
        Profile operator = hs20cfg.getHotspot20OperatorSet().stream().filter(new Predicate<Profile>() {

            @Override
            public boolean test(Profile t) {
                return t.getId() == hs2Profile.getPasspointOperatorProfileId();
            }

        }).findFirst().get();

        PasspointOperatorProfile passpointOperatorProfile = (PasspointOperatorProfile) operator.getDetails();
        return passpointOperatorProfile;
    }

    /**
     * Provision the OSU Providers in the Hotspot20_OSU_Providers ovsdb table.
     * 
     * @param ovsdbClient
     * @param opensyncApConfig
     */
    public void provisionHotspot20OsuProviders(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig) {
        try {
            DatabaseSchema schema = ovsdbClient.getSchema(ovsdbName).get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            if (schema.getTables().containsKey(hotspot20OsuProvidersDbTable)
                    && schema.getTables().get(hotspot20OsuProvidersDbTable) != null) {
                Map<String, Hotspot20OsuProviders> osuProviders = getProvisionedHotspot20OsuProviders(ovsdbClient);

                OpensyncAPHotspot20Config hs20cfg = opensyncApConfig.getHotspotConfig();
                Set<Operation> operations = new HashSet<>();
                if (hs20cfg.getHotspot20ProviderSet() != null && hs20cfg.getHotspot20ProviderSet().size() > 0) {

                    for (Profile provider : hs20cfg.getHotspot20ProviderSet()) {
                        PasspointOsuProviderProfile providerProfile = (PasspointOsuProviderProfile) provider
                                .getDetails();
                        String apOsuProviderName = OvsdbToWlanCloudTypeMappingUtility
                                .getApOsuProviderStringFromOsuProviderName(provider.getName());
                        Map<String, Value> rowColumns = new HashMap<>();
                        rowColumns.put("osu_nai", new Atom<>(providerProfile.getOsuNaiStandalone()));

                        rowColumns.put("osu_nai2", new Atom<>(providerProfile.getOsuNaiShared()));

                        rowColumns.put("osu_provider_name", new Atom<>(apOsuProviderName));

                        getOsuIconUuidsForOsuProvider(ovsdbClient, providerProfile, rowColumns);
                        getOsuProviderFriendlyNames(providerProfile, rowColumns);
                        getOsuProviderMethodList(providerProfile, rowColumns);
                        if (providerProfile.getOsuServerUri() != null) {
                            rowColumns.put("server_uri", new Atom<>(providerProfile.getOsuServerUri()));
                        }
                        getOsuProviderServiceDescriptions(providerProfile, rowColumns);

                        Row row = new Row(rowColumns);

                        if (!osuProviders.containsKey(apOsuProviderName)) {
                            Insert newOsuProvider = new Insert(hotspot20OsuProvidersDbTable, row);
                            operations.add(newOsuProvider);
                        } else {
                            List<Condition> conditions = new ArrayList<>();
                            conditions.add(new Condition("server_uri", Function.EQUALS,
                                    new Atom<>(providerProfile.getOsuServerUri())));
                            Update updatedOsuProvider = new Update(hotspot20OsuProvidersDbTable, conditions, row);
                            operations.add(updatedOsuProvider);
                        }

                    }

                }

                if (operations.size() > 0) {
                    CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName,
                            List.copyOf(operations));
                    OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

                    for (OperationResult res : result) {
                        LOG.debug("provisionHotspot20OsuProviders Op Result {}", res);
                        if (res instanceof InsertResult) {
                            LOG.info("provisionHotspot20OsuProviders insert new row result {}", ((InsertResult) res));
                            // for insert, make sure it is actually in the table
                            confirmRowExistsInTable(ovsdbClient, ((InsertResult) res).getUuid(),
                                    hotspot20OsuProvidersDbTable);
                        } else if (res instanceof UpdateResult) {
                            LOG.info("provisionHotspot20OsuProviders update row result {}", ((UpdateResult) res));

                        }
                    }
                }

            } else {
                LOG.info("Table {} not present in {}. Cannot provision Hotspot20_OSU_Providers",
                        hotspot20OsuProvidersDbTable, ovsdbName);
            }
        } catch (InterruptedException | ExecutionException | TimeoutException | OvsdbClientException e) {
            LOG.error("Error in provisionHotspot20OsuProviders", e);
            throw new RuntimeException(e);
        }

    }

    protected void getOsuProviderServiceDescriptions(PasspointOsuProviderProfile providerProfile,
            Map<String, Value> rowColumns) {
        Set<Atom<String>> serviceDescriptions = new HashSet<>();
        for (PasspointDuple serviceDescription : providerProfile.getOsuServiceDescription()) {
            serviceDescriptions.add(new Atom<String>(serviceDescription.getAsDuple()));
        }

        if (serviceDescriptions.size() > 0) {
            com.vmware.ovsdb.protocol.operation.notation.Set serviceDescriptionSet = com.vmware.ovsdb.protocol.operation.notation.Set
                    .of(serviceDescriptions);
            rowColumns.put("service_description", serviceDescriptionSet);
        }
    }

    protected void getOsuProviderMethodList(PasspointOsuProviderProfile providerProfile,
            Map<String, Value> rowColumns) {
        Set<Atom<Integer>> methods = new HashSet<>();
        for (Integer method : providerProfile.getOsuMethodList()) {
            methods.add(new Atom<Integer>(method));
        }
        if (methods.size() > 0) {
            com.vmware.ovsdb.protocol.operation.notation.Set methodsSet = com.vmware.ovsdb.protocol.operation.notation.Set
                    .of(methods);
            rowColumns.put("method_list", methodsSet);
        }
    }

    protected void getOsuProviderFriendlyNames(PasspointOsuProviderProfile providerProfile,
            Map<String, Value> rowColumns) {
        Set<Atom<String>> providerFriendlyNames = new HashSet<>();
        for (PasspointDuple friendlyName : providerProfile.getOsuFriendlyName()) {
            providerFriendlyNames.add(new Atom<String>(friendlyName.getAsDuple()));
        }

        if (providerFriendlyNames.size() > 0) {
            com.vmware.ovsdb.protocol.operation.notation.Set providerFriendlyNamesSet = com.vmware.ovsdb.protocol.operation.notation.Set
                    .of(providerFriendlyNames);
            rowColumns.put("osu_friendly_name", providerFriendlyNamesSet);
        }
    }

    protected void getOsuIconUuidsForOsuProvider(OvsdbClient ovsdbClient, PasspointOsuProviderProfile providerProfile,
            Map<String, Value> rowColumns) {
        Map<String, Hotspot20IconConfig> osuIconsMap = getProvisionedHotspot20IconConfig(ovsdbClient);
        Set<Uuid> iconsSet = new HashSet<>();
        if (osuIconsMap.size() > 0) {
            for (PasspointOsuIcon icon : providerProfile.getOsuIconList()) {
                if (osuIconsMap.containsKey(icon.getImageUrl())) {
                    iconsSet.add(osuIconsMap.get(icon.getImageUrl()).uuid);
                }
            }
        }

        if (iconsSet.size() > 0) {
            com.vmware.ovsdb.protocol.operation.notation.Set iconUuidSet = com.vmware.ovsdb.protocol.operation.notation.Set
                    .of(iconsSet);
            rowColumns.put("osu_icons", iconUuidSet);
        }
    }

    protected void getNaiRealms(PasspointOsuProviderProfile providerProfile, Set<Atom<String>> naiRealms) {
        providerProfile.getNaiRealmList().stream().forEach(c -> {

            StringBuffer naiBuffer = new StringBuffer();
            naiBuffer.append(Integer.toString(c.getEncoding()));
            naiBuffer.append(",");
            Iterator<String> realmsIterator = c.getNaiRealms().iterator();
            if (realmsIterator != null) {
                while (realmsIterator.hasNext()) {
                    String realm = realmsIterator.next();
                    naiBuffer.append(realm);
                    if (realmsIterator.hasNext()) {
                        naiBuffer.append(";");
                    }
                }

            }

            if (c.getEapMap() == null || c.getEapMap().isEmpty()) {
                naiRealms.add(new Atom<String>(naiBuffer.toString()));
            } else {
                naiBuffer.append(",");

                Map<String, Set<String>> eapMap = c.getEapMap();
                eapMap.entrySet().stream().forEach(e -> {

                    String eapMethodName = e.getKey();
                    String eapMethodId = String.valueOf(PasspointEapMethods.getByName(eapMethodName).getId());
                    naiBuffer.append(eapMethodId);

                    for (String credential : e.getValue()) {

                        String[] keyValue = credential.split(":");
                        String keyId = String.valueOf(PasspointNaiRealmEapAuthParam.getByName(keyValue[0]).getId());
                        if (keyValue[0].equals(
                                PasspointNaiRealmEapAuthParam.NAI_REALM_EAP_AUTH_NON_EAP_INNER_AUTH.getName())) {

                            String valueId = String
                                    .valueOf(PasspointNaiRealmEapAuthInnerNonEap.getByName(keyValue[1]).getId());

                            naiBuffer.append("[");
                            naiBuffer.append(keyId);
                            naiBuffer.append(":");
                            naiBuffer.append(valueId);
                            naiBuffer.append("]");

                        } else if (keyValue[0]
                                .equals(PasspointNaiRealmEapAuthParam.NAI_REALM_EAP_AUTH_CRED_TYPE.getName())
                                || keyValue[0]
                                        .equals(PasspointNaiRealmEapAuthParam.NAI_REALM_EAP_AUTH_TUNNELED_CRED_TYPE
                                                .getName())) {

                            String valueId = String
                                    .valueOf(PasspointNaiRealmEapCredType.getByName(keyValue[1]).getId());

                            naiBuffer.append("[");
                            naiBuffer.append(keyId);
                            naiBuffer.append(":");
                            naiBuffer.append(valueId);
                            naiBuffer.append("]");

                        }
                    }
                    naiBuffer.append(",");

                });
                String naiRealm = naiBuffer.toString();
                if (naiRealm.endsWith(",")) {
                    naiRealm = naiRealm.substring(0, naiRealm.lastIndexOf(","));
                }
                naiRealms.add(new Atom<String>(naiRealm));

            }

        });

    }

    public void provisionHotspot2IconConfig(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig) {
        try {
            DatabaseSchema schema = ovsdbClient.getSchema(ovsdbName).get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            if (schema.getTables().containsKey(hotspot20IconConfigDbTable)
                    && schema.getTables().get(hotspot20IconConfigDbTable) != null) {
                Map<String, Hotspot20IconConfig> osuIconConfigs = getProvisionedHotspot20IconConfig(ovsdbClient);

                OpensyncAPHotspot20Config hs20cfg = opensyncApConfig.getHotspotConfig();
                Set<Operation> operations = new HashSet<>();
                if (hs20cfg.getHotspot20ProviderSet() != null && hs20cfg.getHotspot20ProviderSet().size() > 0) {

                    for (Profile provider : hs20cfg.getHotspot20ProviderSet()) {
                        PasspointOsuProviderProfile providerProfile = (PasspointOsuProviderProfile) provider
                                .getDetails();
                        for (PasspointOsuIcon passpointOsuIcon : providerProfile.getOsuIconList()) {
                            // ovsdbColumns = { "name", "path", "url",
                            // "lang_code", "height", "img_type", "width" };
                            Map<String, Value> rowColumns = new HashMap<>();
                            rowColumns.put("name", new Atom<>(passpointOsuIcon.getIconName()));
                            if (schema.getTables().get(hotspot20IconConfigDbTable).getColumns().containsKey("path")) {
                                rowColumns.put("path", new Atom<>(passpointOsuIcon.getFilePath()));
                            }
                            rowColumns.put("url", new Atom<>(passpointOsuIcon.getImageUrl()));
                            rowColumns.put("lang_code", new Atom<>(passpointOsuIcon.getLanguageCode()));
                            rowColumns.put("height", new Atom<>(passpointOsuIcon.getIconHeight()));
                            rowColumns.put("img_type", new Atom<>(PasspointOsuIcon.ICON_TYPE));
                            rowColumns.put("width", new Atom<>(passpointOsuIcon.getIconWidth()));
                            if (passpointOsuIcon.getImageUrl() != null) {

                                String md5Hex = DigestUtils.md5Hex(passpointOsuIcon.getImageUrl()).toUpperCase();
                                if (schema.getTables().get(hotspot20IconConfigDbTable).getColumns()
                                        .containsKey("icon_config_name")) {
                                    rowColumns.put("icon_config_name", new Atom<>(md5Hex));
                                }
                            }
                            Row row = new Row(rowColumns);

                            if (!osuIconConfigs.containsKey(passpointOsuIcon.getImageUrl())) {
                                Insert newHs20Config = new Insert(hotspot20IconConfigDbTable, row);
                                operations.add(newHs20Config);
                            } else {
                                List<Condition> conditions = new ArrayList<>();
                                conditions.add(new Condition("url", Function.EQUALS,
                                        new Atom<>(passpointOsuIcon.getImageUrl())));
                                Update newHs20Config = new Update(hotspot20IconConfigDbTable, conditions, row);
                                operations.add(newHs20Config);
                            }

                        }
                    }

                }
                if (operations.size() > 0) {
                    CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName,
                            List.copyOf(operations));
                    OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

                    for (OperationResult res : result) {
                        LOG.debug("provisionHotspot20Config Op Result {}", res);
                        if (res instanceof InsertResult) {
                            LOG.info("provisionHotspot20Config insert new row result {}", ((InsertResult) res));
                            // for insert, make sure it is actually in the table
                            confirmRowExistsInTable(ovsdbClient, ((InsertResult) res).getUuid(),
                                    hotspot20IconConfigDbTable);
                        } else if (res instanceof UpdateResult) {
                            LOG.info("provisionHotspot20Config update row result {}", ((UpdateResult) res));

                        }
                    }
                }

            } else {
                LOG.info("Table {} not present in {}. Cannot provision Hotspot20_Icon_Config",
                        hotspot20IconConfigDbTable, ovsdbName);
            }
        } catch (InterruptedException | ExecutionException | TimeoutException | OvsdbClientException e) {
            LOG.error("Error in provisionHotspot2IconConfig", e);
            throw new RuntimeException(e);
        }

    }

    public void removeAllHotspot20Config(OvsdbClient ovsdbClient) {
        try {
            DatabaseSchema schema = ovsdbClient.getSchema(ovsdbName).get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            if (schema.getTables().containsKey(hotspot20ConfigDbTable)
                    && schema.getTables().get(hotspot20ConfigDbTable) != null) {
                List<Operation> operations = new ArrayList<>();

                operations.add(new Delete(hotspot20ConfigDbTable));

                CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
                OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Removed all existing hotspot configs from {}:", hotspot20ConfigDbTable);

                    for (OperationResult res : result) {
                        LOG.debug("Op Result {}", res);
                    }
                }
            }
        } catch (InterruptedException | ExecutionException | TimeoutException | OvsdbClientException e) {
            LOG.error("Error in removeAllHotspot20Config", e);
            throw new RuntimeException(e);
        }

    }

    public void removeAllHotspot20OsuProviders(OvsdbClient ovsdbClient) {
        try {
            DatabaseSchema schema = ovsdbClient.getSchema(ovsdbName).get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            if (schema.getTables().containsKey(hotspot20OsuProvidersDbTable)
                    && schema.getTables().get(hotspot20OsuProvidersDbTable) != null) {
                List<Operation> operations = new ArrayList<>();

                operations.add(new Delete(hotspot20OsuProvidersDbTable));

                CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
                OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Removed all existing hotspot osu providers from {}:", hotspot20OsuProvidersDbTable);

                    for (OperationResult res : result) {
                        LOG.debug("Op Result {}", res);
                    }
                }
            }
        } catch (InterruptedException | ExecutionException | TimeoutException | OvsdbClientException e) {
            LOG.error("Error in removeAllHotspot20OsuProviders", e);
            throw new RuntimeException(e);
        }

    }

    public void removeAllHotspot20IconConfig(OvsdbClient ovsdbClient) {
        try {
            DatabaseSchema schema = ovsdbClient.getSchema(ovsdbName).get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            if (schema.getTables().containsKey(hotspot20IconConfigDbTable)
                    && schema.getTables().get(hotspot20IconConfigDbTable) != null) {
                List<Operation> operations = new ArrayList<>();

                operations.add(new Delete(hotspot20IconConfigDbTable));

                CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
                OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Removed all existing hotspot icon configs from {}:", hotspot20IconConfigDbTable);

                    for (OperationResult res : result) {
                        LOG.debug("Op Result {}", res);
                    }
                }
            }
        } catch (InterruptedException | ExecutionException | TimeoutException | OvsdbClientException e) {
            LOG.error("Error in removeAllHotspot20IconConfig", e);
            throw new RuntimeException(e);
        }

    }

    public void configureStatsFromProfile(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig) {
        // TODO: this will be refactored when the opensync profile for stats is
        // re-worked
        configureStats(ovsdbClient);

    }

    public void configureStats(OvsdbClient ovsdbClient) {

        try {
            List<Operation> operations = new ArrayList<>();
            Map<String, Integer> thresholdMap = new HashMap<>();
            thresholdMap.put("max_delay", 600);
            thresholdMap.put("util", 25);

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

            // TODO: when schema support is added, these should be part of the
            // bulk provisioning operation above.
            provisionVideoVoiceStats(ovsdbClient);

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
                    rowColumns.put("sampling_interval", new Atom<>(10));
                    rowColumns.put("stats_type", new Atom<>("survey"));
                    rowColumns.put("survey_interval_ms", new Atom<>(0));
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
                    rowColumns.put("reporting_interval", new Atom<>(120));
                    rowColumns.put("report_type", new Atom<>("raw"));
                    rowColumns.put("stats_type", new Atom<>("survey"));
                    rowColumns.put("survey_type", new Atom<>("off-chan"));
                    rowColumns.put("sampling_interval", new Atom<>(10));
                    rowColumns.put("survey_interval_ms", new Atom<>(50));
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

    /**
     * @param ovsdbClient
     *
     */
    public void provisionVideoVoiceStats(OvsdbClient ovsdbClient) {
        LOG.debug("Enable video_voice_report");

        try {
            List<Operation> operations = new ArrayList<>();
            Map<String, Value> rowColumns = new HashMap<>();
            rowColumns.put("radio_type", new Atom<>("2.4G"));
            rowColumns.put("reporting_interval", new Atom<>(60));
            rowColumns.put("report_type", new Atom<>("raw"));
            rowColumns.put("sampling_interval", new Atom<>(10));
            rowColumns.put("stats_type", new Atom<>("video_voice"));
            rowColumns.put("survey_interval_ms", new Atom<>(65));
            Row row = new Row(rowColumns);

            operations.add(new Insert(wifiStatsConfigDbTable, row));

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            for (OperationResult res : result) {
                if (res instanceof InsertResult) {
                    LOG.info("provisionVideoVoiceStats insert new row result {}", ((InsertResult) res));
                    // for insert, make sure it is actually in the table
                    confirmRowExistsInTable(ovsdbClient, ((InsertResult) res).getUuid(), wifiStatsConfigDbTable);
                } else if (res instanceof UpdateResult) {
                    LOG.info("provisionVideoVoiceStats update row result {}", ((UpdateResult) res));

                } else if (res instanceof ErrorResult) {
                    LOG.error("Could not update {}:", wifiStatsConfigDbTable);
                    LOG.error("Error: {} Details: {}", ((ErrorResult) res).getError(),
                            ((ErrorResult) res).getDetails());
                } else {
                    LOG.debug("Updated {}:", wifiStatsConfigDbTable);
                    LOG.debug("Op Result {}", res);
                }
            }

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
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
            throw new RuntimeException(e);

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
            throw new RuntimeException(e);

        }
    }

    public void rebootOrResetAp(OvsdbClient ovsdbClient, String desiredApAction) {
        try {
            LOG.debug("rebootOrResetAp on AP perform {}, setting timer for {} seconds.", desiredApAction,
                    rebootOrResetTimerSeconds);
            List<Operation> operations = new ArrayList<>();
            Map<String, Value> updateColumns = new HashMap<>();
            updateColumns.put("firmware_url", new Atom<>(desiredApAction));
            updateColumns.put("upgrade_timer", new Atom<>(rebootOrResetTimerSeconds));
            Row row = new Row(updateColumns);
            operations.add(new Update(awlanNodeDbTable, row));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);

            OperationResult[] result = fResult.join();
            for (OperationResult r : result) {
                LOG.debug("Op Result {}", r);
            }
        } catch (OvsdbClientException e) {
            LOG.error("Could not trigger {}", desiredApAction, e);
            throw new RuntimeException(e);

        }

    }

    public void removeAllStatsConfigs(OvsdbClient ovsdbClient) {

        LOG.info("Remove existing Wifi_Stats_Config table entries");
        try {
            List<Operation> operations = new ArrayList<>();

            operations.add(new Delete(wifiStatsConfigDbTable));

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            LOG.info("Removed all existing config from {}:", wifiStatsConfigDbTable);

            for (OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in removeAllStatsConfigs", e);
            throw new RuntimeException(e);
        }

    }

    public void configureWifiRrm(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig) {

        ApElementConfiguration apElementConfig = (ApElementConfiguration) opensyncApConfig.getCustomerEquipment()
                .getDetails();
        RfConfiguration rfConfig = (RfConfiguration) opensyncApConfig.getRfProfile().getDetails();
        for (RadioType radioType : apElementConfig.getRadioMap().keySet()) {
            String freqBand = null;
            if (radioType == RadioType.is2dot4GHz) {
                freqBand = "2.4G";
            } else if (radioType == RadioType.is5GHzL) {
                freqBand = "5GL";
            } else if (radioType == RadioType.is5GHzU) {
                freqBand = "5GU";
            } else if (radioType == RadioType.is5GHz) {
                freqBand = "5G";
            }

            if (rfConfig == null) {
                continue;
            }

            ElementRadioConfiguration elementRadioConfig = apElementConfig.getRadioMap().get(radioType);
            RfElementConfiguration rfElementConfig = rfConfig.getRfConfig(radioType);
            if (elementRadioConfig == null || rfElementConfig == null) {
                continue; // don't have a radio of this kind in the map
            }
            AutoOrManualValue probeResponseThresholdDb = null;
            AutoOrManualValue clientDisconnectThresholdDb = null;
            if (elementRadioConfig != null && rfElementConfig != null) {
                probeResponseThresholdDb = getSourcedValue(elementRadioConfig.getProbeResponseThresholdDb().getSource(),
                        rfElementConfig.getProbeResponseThresholdDb(),
                        elementRadioConfig.getProbeResponseThresholdDb().getValue());

                clientDisconnectThresholdDb = getSourcedValue(
                        elementRadioConfig.getClientDisconnectThresholdDb().getSource(),
                        rfElementConfig.getClientDisconnectThresholdDb(),
                        elementRadioConfig.getClientDisconnectThresholdDb().getValue());
            }

            RadioConfiguration radioConfig = apElementConfig.getAdvancedRadioMap().get(radioType);
            MulticastRate multicastRate = null;
            ManagementRate managementRate = null;
            RadioBestApSettings bestApSettings = null;
            if (radioConfig != null && rfElementConfig != null) {
                multicastRate = radioConfig.getMulticastRate().getSource() == SourceType.profile
                        ? rfElementConfig.getMulticastRate()
                        : radioConfig.getMulticastRate().getValue();

                managementRate = radioConfig.getManagementRate().getSource() == SourceType.profile
                        ? rfElementConfig.getManagementRate()
                        : radioConfig.getManagementRate().getValue();

                bestApSettings = radioConfig.getBestApSettings().getSource() == SourceType.profile
                        ? rfElementConfig.getBestApSettings()
                        : radioConfig.getBestApSettings().getValue();
            }

            int multicastRateMbps = 0;
            if (multicastRate != null) {
                switch (multicastRate) {
                case rate6mbps:
                    multicastRateMbps = 6;
                    break;
                case rate9mbps:
                    multicastRateMbps = 9;
                    break;
                case rate12mbps:
                    multicastRateMbps = 12;
                    break;
                case rate18mbps:
                    multicastRateMbps = 18;
                    break;
                case rate24mbps:
                    multicastRateMbps = 24;
                    break;
                case rate36mbps:
                    multicastRateMbps = 36;
                    break;
                case rate48mbps:
                    multicastRateMbps = 48;
                    break;
                case rate54mbps:
                    multicastRateMbps = 54;
                    break;
                case auto:
                default:
                    multicastRateMbps = 0;
                }
            }

            if (freqBand != null) {
                try {
                    configureWifiRrm(ovsdbClient, freqBand, elementRadioConfig.getBackupChannelNumber(),
                            probeResponseThresholdDb, clientDisconnectThresholdDb, managementRate, bestApSettings,
                            multicastRateMbps);
                } catch (OvsdbClientException e) {
                    LOG.error("configureRrm failed with OvsdbClient exception.", e);
                    throw new RuntimeException(e);
                } catch (TimeoutException e) {
                    LOG.error("configureRrm failed with Timeout.", e);
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    LOG.error("configureRrm excecution failed.", e);
                } catch (InterruptedException e) {
                    LOG.error("configureRrm interrupted.", e);
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void configureWifiRrm(OvsdbClient ovsdbClient, String freqBand, int backupChannel,
            AutoOrManualValue probeResponseThreshold, AutoOrManualValue clientDisconnectThreshold,
            ManagementRate managementRate, RadioBestApSettings bestApSettings, int multicastRate)
            throws OvsdbClientException, TimeoutException, ExecutionException, InterruptedException {

        List<Operation> operations = new ArrayList<>();
        Map<String, Value> updateColumns = new HashMap<>();

        updateColumns.put("freq_band", new Atom<>(freqBand));
        updateColumns.put("backup_channel", new Atom<>(backupChannel));

        if (ovsdbClient.getSchema(ovsdbName).get().getTables().get(wifiRadioConfigDbTable).getColumns()
                .containsKey("mcast_rate")) {

            updateColumns.put("mcast_rate", new Atom<>(multicastRate));
        }

        if (probeResponseThreshold == null || probeResponseThreshold.isAuto()) {
            updateColumns.put("probe_resp_threshold", new com.vmware.ovsdb.protocol.operation.notation.Set());
        } else {
            updateColumns.put("probe_resp_threshold", new Atom<>(probeResponseThreshold.getValue()));
        }

        if (probeResponseThreshold == null || clientDisconnectThreshold.isAuto()) {
            updateColumns.put("client_disconnect_threshold", new com.vmware.ovsdb.protocol.operation.notation.Set());
        } else {
            updateColumns.put("client_disconnect_threshold", new Atom<>(clientDisconnectThreshold.getValue()));
        }

        if (ovsdbClient.getSchema(ovsdbName).get().getTables().get(wifiRadioConfigDbTable).getColumns()
                .containsKey("beacon_rate")) {
            if (managementRate == null || managementRate == ManagementRate.auto) {
                updateColumns.put("beacon_rate", new Atom<>(0));
            } else {
                updateColumns.put("beacon_rate", new Atom<>(managementRate.getId() * 10));
            }
        }

        if (bestApSettings == null) {
            updateColumns.put("min_load", new com.vmware.ovsdb.protocol.operation.notation.Set());
            updateColumns.put("snr_percentage_drop", new com.vmware.ovsdb.protocol.operation.notation.Set());
        } else {
            if (bestApSettings.getDropInSnrPercentage() == null) {
                updateColumns.put("snr_percentage_drop", new com.vmware.ovsdb.protocol.operation.notation.Set());
            } else {
                updateColumns.put("snr_percentage_drop", new Atom<>(bestApSettings.getDropInSnrPercentage()));
            }
            if (bestApSettings.getMinLoadFactor() == null) {
                updateColumns.put("min_load", new com.vmware.ovsdb.protocol.operation.notation.Set());
            } else {
                updateColumns.put("min_load", new Atom<>(bestApSettings.getMinLoadFactor()));
            }
        }

        Row row = new Row(updateColumns);
        operations.add(new Insert(wifiRrmConfigDbTable, row));

        CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
        OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

        LOG.debug("Provisioned rrm config with multicastRate {} Mbps for {}", multicastRate, freqBand);

        for (OperationResult res : result) {
            LOG.debug("Op Result {}", res);

            if (res instanceof InsertResult) {
                LOG.info("configureWifiRrm insert new row result {}", ((InsertResult) res));
                // for insert, make sure it is actually in the table
                confirmRowExistsInTable(ovsdbClient, ((InsertResult) res).getUuid(), wifiRrmConfigDbTable);
            } else if (res instanceof ErrorResult) {
                LOG.error("configureWifiRrm error {}", ((ErrorResult) res));
                throw new RuntimeException(
                        "configureWifiRrm " + ((ErrorResult) res).getError() + " " + ((ErrorResult) res).getDetails());
            }

        }
    }

    public void removeWifiRrm(OvsdbClient ovsdbClient) {
        try {
            List<Operation> operations = new ArrayList<>();

            operations.add(new Delete(wifiRrmConfigDbTable));

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            LOG.info("Removed rrm from {}:", wifiRrmConfigDbTable);

            for (OperationResult res : result) {
                if (res instanceof UpdateResult) {
                    LOG.info("removeWifiRrm result {}", (UpdateResult) res);
                }
            }

        } catch (ExecutionException | OvsdbClientException | TimeoutException | InterruptedException e) {
            LOG.error("Error in removeRrm", e);
            throw new RuntimeException(e);
        }
    }

    public void processNewChannelsRequest(OvsdbClient ovsdbClient, Map<RadioType, Integer> backupChannelMap,
            Map<RadioType, Integer> primaryChannelMap) {

        LOG.info("OvsdbDao::processNewChannelsRequest backup {} primary {}", backupChannelMap, primaryChannelMap);
        try {
            List<Operation> operations = new ArrayList<>();

            backupChannelMap.entrySet().stream().forEach(c -> {
                String freqBand = OvsdbToWlanCloudTypeMappingUtility.getOvsdbRadioFreqBandForRadioType(c.getKey());
                List<Condition> conditions = new ArrayList<>();
                conditions.add(new Condition("freq_band", Function.EQUALS, new Atom<>(freqBand)));
                Map<String, Value> updateColumns = new HashMap<>();
                updateColumns.put("backup_channel", new Atom<>(c.getValue()));
                Row row = new Row(updateColumns);
                operations.add(new Update(wifiRrmConfigDbTable, conditions, row));
            });

            primaryChannelMap.entrySet().stream().forEach(c -> {
                String freqBand = OvsdbToWlanCloudTypeMappingUtility.getOvsdbRadioFreqBandForRadioType(c.getKey());
                List<Condition> conditions = new ArrayList<>();
                conditions.add(new Condition("freq_band", Function.EQUALS, new Atom<>(freqBand)));
                Map<String, Value> updateColumns = new HashMap<>();
                updateColumns.put("channel", new Atom<>(c.getValue()));
                Row row = new Row(updateColumns);
                operations.add(new Update(wifiRadioConfigDbTable, conditions, row));
            });

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            for (OperationResult res : result) {
                LOG.info("Op Result {}", res);
            }

            LOG.info("Updated ovsdb config for primary and backup channels.");
        } catch (ExecutionException e) {
            LOG.error("Error in processNewChannelsRequest", e);
        } catch (OvsdbClientException | TimeoutException | InterruptedException e) {
            LOG.error("Error in processNewChannelsRequest", e);
            throw new RuntimeException(e);
        }

    }

    public AutoOrManualValue getSourcedValue(SourceType source, int profileValue, int equipmentValue) {
        if (source == SourceType.profile) {
            return AutoOrManualValue.createManualInstance(profileValue);
        } else if (source == SourceType.auto) {
            return AutoOrManualValue.createAutomaticInstance(equipmentValue);
        }
        return AutoOrManualValue.createManualInstance(equipmentValue);
    }

    public void updateEventReportingInterval(OvsdbClient ovsdbClient, long eventReportingIntervalSeconds) {

        try {
            List<Operation> operations = new ArrayList<>();
            Map<String, Value> updateColumns = new HashMap<>();

            // turn on stats collection over MQTT: (reporting_interval is in
            // seconds)
            // $ ovsh i Wifi_Stats_Config reporting_interval:=10
            // radio_type:="2.4G" stats_type:="device"

            updateColumns.put("reporting_interval", new Atom<>(eventReportingIntervalSeconds));
            updateColumns.put("radio_type", new Atom<>("2.4G"));
            updateColumns.put("stats_type", new Atom<>("event"));

            Row row = new Row(updateColumns);
            operations.add(new Insert(wifiStatsConfigDbTable, row));

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            LOG.debug("Updated {}:", wifiStatsConfigDbTable);

            for (OperationResult res : result) {
                LOG.debug("updateEventReportingInterval Result {}", res);
                if (res instanceof InsertResult) {
                    LOG.info("updateEventReportingInterval insert new row result {}", ((InsertResult) res));
                    // for insert, make sure it is actually in the table
                    confirmRowExistsInTable(ovsdbClient, ((InsertResult) res).getUuid(), wifiStatsConfigDbTable);
                }
            }

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

}
