package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.NodeConfigInfo;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.RadiusProxyConfigInfo;
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
import com.vmware.ovsdb.service.OvsdbClient;

@Component
public class OvsdbGet extends OvsdbDaoBase {

    Map<String, Set<Integer>> getAllowedChannels(OvsdbClient ovsdbClient) {
        Map<String, Set<Integer>> allowedChannels = new HashMap<>();
        for (Row row : getOvsdbTableRowsForCondition(ovsdbClient, wifiRadioStateDbTable, null)) {
            allowedChannels.put(getSingleValueFromSet(row, "freq_band"), row.getSetColumn("allowed_channels"));
        }
        return allowedChannels;
    }

    /**
     * Get all Rows from given table that satisfy the conditions.
     * 
     * @param ovsdbClient
     * @param ovsdbTableName
     * @param conditions
     * @return Set of Rows in the table that satisfy the conditions, or all rows
     *         in table if conditions is null or empty.
     */
    Set<Row> getOvsdbTableRowsForCondition(OvsdbClient ovsdbClient, String ovsdbTableName, List<Condition> conditions) {
        Set<Row> ret = new HashSet<>();
        List<Operation> operations = new ArrayList<>();
        if (conditions == null || conditions.isEmpty()) {
            operations.add(new Select(ovsdbTableName));
        } else {
            operations.add(new Select(ovsdbTableName, conditions));
        }
        try {
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            for (Row row : ((SelectResult) result[0]).getRows()) {
                ret.add(row);
            }
        } catch (OvsdbClientException | InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Transaction exception getting rows from {}.", ovsdbTableName, e);
            throw new RuntimeException(e);
        }
        return ret;
    }

    NodeConfigInfo getNodeConfigInfo(OvsdbClient ovsdbClient) {
        return new NodeConfigInfo(getOvsdbTableRowsForCondition(ovsdbClient, nodeConfigTable,
                List.of(new Condition("module", Function.EQUALS, new Atom<>("ntp")))).iterator().next());
    }

    Map<String, CommandConfigInfo> getProvisionedCommandConfigs(OvsdbClient ovsdbClient) {
        Map<String, CommandConfigInfo> ret = new HashMap<>();
        for (Row row : getOvsdbTableRowsForCondition(ovsdbClient, commandConfigDbTable, null)) {
            CommandConfigInfo commandConfigInfo = new CommandConfigInfo(row);
            ret.put(commandConfigInfo.command, commandConfigInfo);
        }
        return ret;
    }

    Map<String, Hotspot20Config> getProvisionedHotspot20Configs(OvsdbClient ovsdbClient) {
        Map<String, Hotspot20Config> ret = new HashMap<>();
        for (Row row : getOvsdbTableRowsForCondition(ovsdbClient, hotspot20ConfigDbTable, null)) {
            Hotspot20Config hotspot20Config = new Hotspot20Config(row);
            ret.put(hotspot20Config.osuSsid, hotspot20Config);
        }
        return ret;
    }

    Map<String, Hotspot20IconConfig> getProvisionedHotspot20IconConfig(OvsdbClient ovsdbClient) {
        Map<String, Hotspot20IconConfig> ret = new HashMap<>();
        for (Row row : getOvsdbTableRowsForCondition(ovsdbClient, hotspot20IconConfigDbTable, null)) {
            Hotspot20IconConfig hotspot20IconConfig = new Hotspot20IconConfig(row);
            ret.put(hotspot20IconConfig.url, hotspot20IconConfig);
        }
        return ret;
    }

    Map<String, Hotspot20OsuProviders> getProvisionedHotspot20OsuProviders(OvsdbClient ovsdbClient) {
        Map<String, Hotspot20OsuProviders> ret = new HashMap<>();
        for (Row row : getOvsdbTableRowsForCondition(ovsdbClient, hotspot20OsuProvidersDbTable, null)) {
            Hotspot20OsuProviders hotspot20OsuProviders = new Hotspot20OsuProviders(row);
            ret.put(hotspot20OsuProviders.osuProviderName, hotspot20OsuProviders);
        }
        return ret;
    }

    Map<String, WifiInetConfigInfo> getProvisionedWifiInetConfigs(OvsdbClient ovsdbClient) {
        Map<String, WifiInetConfigInfo> ret = new HashMap<>();
        for (Row row : getOvsdbTableRowsForCondition(ovsdbClient, wifiInetConfigDbTable, null)) {
            WifiInetConfigInfo wifiInetConfigInfo = new WifiInetConfigInfo(row);
            ret.put(wifiInetConfigInfo.ifName, wifiInetConfigInfo);
        }
        return ret;
    }

    Map<String, WifiRadioConfigInfo> getProvisionedWifiRadioConfigs(OvsdbClient ovsdbClient) {
        Map<String, WifiRadioConfigInfo> ret = new HashMap<>();
        for (Row row : getOvsdbTableRowsForCondition(ovsdbClient, wifiRadioConfigDbTable, null)) {
            WifiRadioConfigInfo wifiRadioConfigInfo = new WifiRadioConfigInfo(row);
            ret.put(wifiRadioConfigInfo.ifName, wifiRadioConfigInfo);
        }
        return ret;
    }

    Map<String, WifiStatsConfigInfo> getProvisionedWifiStatsConfigs(OvsdbClient ovsdbClient) {
        Map<String, WifiStatsConfigInfo> ret = new HashMap<>();
        for (Row row : getOvsdbTableRowsForCondition(ovsdbClient, wifiStatsConfigDbTable, null)) {
            WifiStatsConfigInfo wifiStatsConfigInfo = new WifiStatsConfigInfo(row);
            if (wifiStatsConfigInfo.surveyType == null) {
                ret.put(wifiStatsConfigInfo.radioType + "_" + wifiStatsConfigInfo.statsType, wifiStatsConfigInfo);
            } else {
                ret.put(wifiStatsConfigInfo.radioType + "_" + wifiStatsConfigInfo.statsType + "_"
                        + wifiStatsConfigInfo.surveyType, wifiStatsConfigInfo);
            }
        }
        return ret;
    }

    Map<String, WifiVifConfigInfo> getProvisionedWifiVifConfigs(OvsdbClient ovsdbClient) {
        Map<String, WifiVifConfigInfo> ret = new HashMap<>();
        for (Row row : getOvsdbTableRowsForCondition(ovsdbClient, wifiVifConfigDbTable, null)) {
            WifiVifConfigInfo wifiVifConfigInfo = new WifiVifConfigInfo(row);
            ret.put(wifiVifConfigInfo.ifName + '_' + wifiVifConfigInfo.ssid, wifiVifConfigInfo);
        }
        return ret;
    }

    List<String> getWifiVifStates(OvsdbClient ovsdbClient, String ssidName) {
        List<String> ret = new ArrayList<>();
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition("ssid", Function.EQUALS, new Atom<>(ssidName)));
        for (Row row : getOvsdbTableRowsForCondition(ovsdbClient, wifiVifStateDbTable, null)) {
            String mac = getSingleValueFromSet(row, "mac");
            if (mac != null) {
                ret.add(mac);
            }
        }
        return ret;
    }

    Map<String, RadiusProxyConfigInfo> getProvisionedRadiusConfigs(OvsdbClient ovsdbClient) {
        Map<String, RadiusProxyConfigInfo> ret = new HashMap<>();
        for (Row row : getOvsdbTableRowsForCondition(ovsdbClient, radiusConfigDbTable, null)) {
            RadiusProxyConfigInfo radiusProxyConfigInfo = new RadiusProxyConfigInfo(row);
            ret.put(radiusProxyConfigInfo.radiusConfigName, radiusProxyConfigInfo);
        }
        return ret;
    }

}
