package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPInetState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPRadioState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPVIFState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAWLANNode;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncWifiAssociatedClients;
import com.vmware.ovsdb.protocol.methods.RowUpdate;
import com.vmware.ovsdb.protocol.methods.TableUpdate;
import com.vmware.ovsdb.protocol.methods.TableUpdates;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;
import com.vmware.ovsdb.protocol.operation.notation.Value;
import com.vmware.ovsdb.service.OvsdbClient;

@Component
public class OvsdbMonitor extends OvsdbDaoBase {
    List<OpensyncAPInetState> getInitialOpensyncApInetStateForRowUpdate(TableUpdates tableUpdates, String apId,
            OvsdbClient ovsdbClient) {

        LOG.info("getInitialOpensyncApInetStateForRowUpdate:");
        List<OpensyncAPInetState> ret = new ArrayList<>();
        try {
            LOG.info(wifiInetStateDbTable + "_" + apId + " initial monitor table state received {}", tableUpdates);

            for (TableUpdate tableUpdate : tableUpdates.getTableUpdates().values()) {

                for (RowUpdate rowUpdate : tableUpdate.getRowUpdates().values()) {

                    if (rowUpdate.getNew() != null) {
                        ret.addAll(getOpensyncApInetStateForRowUpdate(rowUpdate, apId, ovsdbClient));
                    }
                }

            }
        } catch (Exception e) {
            throw (e);
        }
        return ret;

    }

    List<OpensyncAPVIFState> getInitialOpensyncApVifStateForTableUpdates(TableUpdates tableUpdates, String apId,
            OvsdbClient ovsdbClient) {

        LOG.info("getInitialOpensyncApVifStateForTableUpdates:");
        List<OpensyncAPVIFState> ret = new ArrayList<>();
        try {
            LOG.info(wifiVifStateDbTable + "_" + apId + " initial monitor table state received {}", tableUpdates);

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

    List<OpensyncWifiAssociatedClients> getInitialOpensyncWifiAssociatedClients(TableUpdates tableUpdates, String apId,
            OvsdbClient ovsdbClient) {

        LOG.info("getInitialOpensyncWifiAssociatedClients:");
        List<OpensyncWifiAssociatedClients> ret = new ArrayList<>();
        try {
            LOG.info(wifiAssociatedClientsDbTable + "_" + apId + " initial monitor table state received {}",
                    tableUpdates);

            for (TableUpdate tableUpdate : tableUpdates.getTableUpdates().values()) {

                for (RowUpdate rowUpdate : tableUpdate.getRowUpdates().values()) {

                    if (rowUpdate.getNew() != null) {
                        ret.addAll(getOpensyncWifiAssociatedClients(rowUpdate, apId, ovsdbClient));
                    }
                }

            }
        } catch (Exception e) {
            throw (e);
        }
        return ret;

    }

    List<OpensyncAPInetState> getOpensyncApInetStateForRowUpdate(RowUpdate rowUpdate, String apId,
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

    List<OpensyncAPRadioState> getOpensyncAPRadioState(TableUpdates tableUpdates, String apId,
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
                        if (map.get("channels") != null) {

                            Map<String, String> channels = row.getMapColumn("channels");
                            tableState.setChannels(channels);
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

    List<OpensyncAPVIFState> getOpensyncApVifStateForRowUpdate(RowUpdate rowUpdate, String apId,
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

    OpensyncAWLANNode getOpensyncAWLANNode(TableUpdates tableUpdates, String apId, OvsdbClient ovsdbClient) {
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

    List<OpensyncWifiAssociatedClients> getOpensyncWifiAssociatedClients(RowUpdate rowUpdate, String apId,
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

    OpensyncAPVIFState processWifiVIFStateColumn(OvsdbClient ovsdbClient, Row row) {
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

}
