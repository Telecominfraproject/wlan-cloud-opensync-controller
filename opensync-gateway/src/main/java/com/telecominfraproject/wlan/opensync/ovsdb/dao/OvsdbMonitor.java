package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPInetState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPRadioState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPVIFState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAWLANNode;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncWifiAssociatedClients;
import com.vmware.ovsdb.protocol.methods.RowUpdate;
import com.vmware.ovsdb.protocol.methods.TableUpdate;
import com.vmware.ovsdb.protocol.methods.TableUpdates;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.service.OvsdbClient;

@Component
public class OvsdbMonitor extends OvsdbDaoBase {

    List<OpensyncAPInetState> getInitialOpensyncApInetStateForRowUpdate(TableUpdates tableUpdates, String apId,
            OvsdbClient ovsdbClient) {
        LOG.debug("getInitialOpensyncApInetStateForRowUpdate:");
        List<OpensyncAPInetState> ret = new ArrayList<>();
        try {
            LOG.debug(wifiInetStateDbTable + "_" + apId + " initial monitor table state received {}", tableUpdates);
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
        LOG.debug("getInitialOpensyncApVifStateForTableUpdates:");
        List<OpensyncAPVIFState> ret = new ArrayList<>();
        try {
            LOG.debug(wifiVifStateDbTable + "_" + apId + " initial monitor table state received {}", tableUpdates);
            for (TableUpdate tableUpdate : tableUpdates.getTableUpdates().values()) {
                for (RowUpdate rowUpdate : tableUpdate.getRowUpdates().values()) {
                    if (rowUpdate.getNew() != null) {
                        ret.add(new OpensyncAPVIFState(rowUpdate.getNew()));
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
        LOG.debug("getInitialOpensyncWifiAssociatedClients:");
        List<OpensyncWifiAssociatedClients> ret = new ArrayList<>();
        try {
            LOG.debug(wifiAssociatedClientsDbTable + "_" + apId + " initial monitor table state received {}",
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
        LOG.debug("OvsdbDao::getOpensyncApInetStateForRowUpdate {} for apId {}", rowUpdate, apId);
        Row row = null;
        if (rowUpdate.getNew() != null) {
            if (rowUpdate.getOld() != null) {
                row = rowUpdate.getOld();
                row.getColumns().putAll(rowUpdate.getNew().getColumns());
            } else {
            	row = rowUpdate.getNew();
            }
        } else {
            row = rowUpdate.getOld();
        }
        if (row != null) {
            ret.add(new OpensyncAPInetState(row));
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
                    if (row != null) {
                        ret.add(new OpensyncAPRadioState(row));
                    }
                }
            }
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
            if (row != null) {
                ret.add(new OpensyncAPVIFState(row));
            }
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
                        tableState = new OpensyncAWLANNode(row);
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
        Row row = rowUpdate.getNew();
        if (row == null) {
            row = rowUpdate.getOld();
        }
        if (row != null) {
            return List.of(new OpensyncWifiAssociatedClients(row));
        } else {
            return List.of();
        }
    }   
    
    Map<String, String> getAPCState(RowUpdate rowUpdate, String apId) {
        Map<String, String> ret = new HashMap<>();
        if (rowUpdate.getNew() != null) {
            Row row = rowUpdate.getNew();
            ret.put("designatedRouterIp", getSingleValueFromSet(row, "dr_addr"));
            ret.put("backupDesignatedRouterIp", getSingleValueFromSet(row, "bdr_addr"));
            ret.put("enabled", getSingleValueFromSet(row, "enabled").toString());
            ret.put("mode", getSingleValueFromSet(row, "mode"));
        }
        return ret;
    }
    
}

