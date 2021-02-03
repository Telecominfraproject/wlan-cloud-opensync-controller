package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.CommandConfigInfo;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.Hotspot20Config;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.Hotspot20IconConfig;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.Hotspot20OsuProviders;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.InterfaceInfo;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.WifiInetConfigInfo;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.WifiRadioConfigInfo;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.WifiStatsConfigInfo;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.WifiVifConfigInfo;
import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.protocol.operation.Operation;
import com.vmware.ovsdb.protocol.operation.Select;
import com.vmware.ovsdb.protocol.operation.notation.Atom;
import com.vmware.ovsdb.protocol.operation.notation.Condition;
import com.vmware.ovsdb.protocol.operation.notation.Function;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.protocol.operation.result.SelectResult;
import com.vmware.ovsdb.protocol.schema.DatabaseSchema;
import com.vmware.ovsdb.service.OvsdbClient;

@Component
public class OvsdbGet extends OvsdbDaoBase {
    Map<String, Set<Integer>> getAllowedChannels(OvsdbClient ovsdbClient) {

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

    Map<String, CommandConfigInfo> getProvisionedCommandConfigs(OvsdbClient ovsdbClient) {
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

    Map<String, Hotspot20Config> getProvisionedHotspot20Configs(OvsdbClient ovsdbClient) {
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

    Map<String, Hotspot20IconConfig> getProvisionedHotspot20IconConfig(OvsdbClient ovsdbClient) {
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

    Map<String, Hotspot20OsuProviders> getProvisionedHotspot20OsuProviders(OvsdbClient ovsdbClient) {
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

    Map<String, InterfaceInfo> getProvisionedInterfaces(OvsdbClient ovsdbClient) {
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

    Map<String, WifiInetConfigInfo> getProvisionedWifiInetConfigs(OvsdbClient ovsdbClient) {
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

    Map<String, WifiRadioConfigInfo> getProvisionedWifiRadioConfigs(OvsdbClient ovsdbClient) {
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

    Map<String, WifiStatsConfigInfo> getProvisionedWifiStatsConfigs(OvsdbClient ovsdbClient) {
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

    Map<String, WifiVifConfigInfo> getProvisionedWifiVifConfigs(OvsdbClient ovsdbClient) {
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

    List<String> getWifiVifStates(OvsdbClient ovsdbClient, String ssidName) {
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

}
