package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telecominfraproject.wlan.opensync.external.integration.models.ConnectNodeInfo;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.enumerations.DhcpFpDbStatus;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.enumerations.DhcpFpDeviceType;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.enumerations.DhcpFpManufId;
import com.vmware.ovsdb.exception.OvsdbClientException;
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
import com.vmware.ovsdb.protocol.operation.result.UpdateResult;
import com.vmware.ovsdb.service.OvsdbClient;

public class OvsdbDaoBase {

    static final int MAX_VIF_PER_FREQ = 8;
    static final Logger LOG = LoggerFactory.getLogger(OvsdbDaoBase.class);
    public static final String wifiRouteStateDbTable = "Wifi_Route_State";
    public static final String wifiMasterStateDbTable = "Wifi_Master_State";
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
    public static final String nodeConfigTable = "Node_Config";
    public static final String nodeStateTable = "Node_State";
    public static final String dhcpLeasedIpDbTable = "DHCP_leased_IP";
    public static final String commandConfigDbTable = "Command_Config";
    public static final String commandStateDbTable = "Command_State";
    public static final String hotspot20IconConfigDbTable = "Hotspot20_Icon_Config";
    public static final String hotspot20OsuProvidersDbTable = "Hotspot20_OSU_Providers";
    public static final String hotspot20ConfigDbTable = "Hotspot20_Config";
    public static final String radiusConfigDbTable = "radius_config";
    public static final String realmConfigDbTable = "realm_config";
    public static final String StartDebugEngineApCommand = "startPortForwardingSession";
    public static final String StopDebugEngineApCommand = "stopSession";

    public static <T> T getSingleValueFromSet(Row row, String columnName) {

        Set<T> set = row != null ? row.getSetColumn(columnName) : null;
        T ret = (set != null) && !set.isEmpty() ? set.iterator().next() : null;

        return ret;
    }

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

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.managerAddr:3.88.149.10}")
    String managerIpAddr;
    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.region:Ottawa}")
    public String region;
    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.listenPort:6640}")
    int ovsdbListenPort;
    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.externalPort:6640}")
    int ovsdbExternalPort;
    @org.springframework.beans.factory.annotation.Value("${tip.wlan.mqttBroker.address.external:testportal.123wlan.com}")
    String mqttBrokerAddress;
    @org.springframework.beans.factory.annotation.Value("${tip.wlan.mqttBroker.listenPort:1883}")
    int mqttBrokerListenPort;
    @org.springframework.beans.factory.annotation.Value("${tip.wlan.mqttBroker.externalPort:1883}")
    int mqttBrokerExternalPort;
    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.timeoutSec:30}")
    int ovsdbTimeoutSec;
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
    String externalFileStoreURL;
    
    @org.springframework.beans.factory.annotation.Value("${tip.wlan.fileStoreDirectory:/tmp/tip-wlan-filestore}")
    String fileStoreDirectoryName;

    public OvsdbDaoBase() {
    }

    public <T> Set<T> getSet(Row row, String columnName) {

        Set<T> set = row != null ? row.getSetColumn(columnName) : null;

        return set;
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
                    LOG.info("updateEventReportingInterval insert new row result {}", (res));
                    // for insert, make sure it is actually in the table
                    confirmRowExistsInTable(ovsdbClient, ((InsertResult) res).getUuid(), wifiStatsConfigDbTable);
                }
            }

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    void configureWanInterfacesForDhcpSniffing(OvsdbClient ovsdbClient) {
        List<Operation> operations = new ArrayList<>();
        Map<String, Value> updateColumns = new HashMap<>();
        List<Condition> conditions = new ArrayList<>();
        // Going forward this will be only for WAN, and children will inherit
        // conditions.add(new Condition("if_name", Function.EQUALS, new Atom<>(defaultWanInterfaceName)));

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
                    LOG.error("configureWanInterfacesForDhcpSniffing error {}", (res));
                    throw new RuntimeException("configureWanInterfacesForDhcpSniffing " + ((ErrorResult) res).getError()
                            + " " + ((ErrorResult) res).getDetails());
                }
            }
        } catch (OvsdbClientException | InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("OvsdbDao::configureWanInterfaces failed.", e);
            throw new RuntimeException(e);

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
                    LOG.error("confirmRowExistsInTable error {}", (res));
                    throw new RuntimeException("confirmRowExistsInTable " + ((ErrorResult) res).getError() + " "
                            + ((ErrorResult) res).getDetails());
                }
            }
        } catch (OvsdbClientException | InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Unable to confirm existence of row in table {} for Uuid {}", table, rowUuid, e);
            throw new RuntimeException(e);
        }
    }

    void fillInRadioInterfaceNames(OvsdbClient ovsdbClient, ConnectNodeInfo ret) {
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

}