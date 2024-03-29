
package com.telecominfraproject.wlan.opensync.ovsdb;

import com.google.common.collect.ImmutableMap;
import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.Monitors;
import com.netflix.servo.tag.TagList;
import com.telecominfraproject.wlan.cloudmetrics.CloudMetricsTags;
import com.telecominfraproject.wlan.core.model.equipment.MacAddress;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.equipment.models.CellSizeAttributes;
import com.telecominfraproject.wlan.opensync.external.integration.OpensyncExternalIntegrationInterface;
import com.telecominfraproject.wlan.opensync.external.integration.OpensyncExternalIntegrationInterface.RowUpdateOperation;
import com.telecominfraproject.wlan.opensync.external.integration.OvsdbClientInterface;
import com.telecominfraproject.wlan.opensync.external.integration.OvsdbSession;
import com.telecominfraproject.wlan.opensync.external.integration.OvsdbSessionMapInterface;
import com.telecominfraproject.wlan.opensync.external.integration.models.*;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.OvsdbDao;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.OvsdbMonitor;
import com.telecominfraproject.wlan.opensync.ovsdb.metrics.OvsdbClientWithMetrics;
import com.telecominfraproject.wlan.opensync.ovsdb.metrics.OvsdbMetrics;
import com.telecominfraproject.wlan.opensync.util.OvsdbStringConstants;
import com.telecominfraproject.wlan.opensync.util.SslUtil;
import com.vmware.ovsdb.callback.ConnectionCallback;
import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.protocol.methods.*;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Value;
import com.vmware.ovsdb.service.OvsdbClient;
import com.vmware.ovsdb.service.OvsdbPassiveConnectionListener;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Profile("ovsdb_manager")
@Component
public class TipWlanOvsdbClient implements OvsdbClientInterface {

    private static final Logger LOG = LoggerFactory.getLogger(TipWlanOvsdbClient.class);

    private final TagList tags = CloudMetricsTags.commonTags;

    private final Counter connectionsAttempted = new BasicCounter(MonitorConfig.builder("osgw-connectionsAttempted").withTags(tags).build());

    private final Counter connectionsFailed = new BasicCounter(MonitorConfig.builder("osgw-connectionsFailed").withTags(tags).build());

    private final Counter connectionsCreated = new BasicCounter(MonitorConfig.builder("osgw-connectionsCreated").withTags(tags).build());

    private final Counter connectionsDropped = new BasicCounter(MonitorConfig.builder("osgw-connectionsDropped").withTags(tags).build());

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.listenPort:6640}")
    private int ovsdbListenPort;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.manager.collectionIntervalSec.deviceStats:60}")
    private long collectionIntervalSecDeviceStats;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.preventClientCnAlteration:false}")
    private boolean preventClientCnAlteration;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.defaultCommandDurationSec:3600}")
    private long defaultCommandDurationSec;

    @Autowired
    private SslContext sslContext;

    @Autowired
    private OvsdbPassiveConnectionListener listener;

    @Autowired
    private OvsdbDao ovsdbDao;

    @Autowired
    private OpensyncExternalIntegrationInterface extIntegrationInterface;

    @Autowired
    private OvsdbSessionMapInterface ovsdbSessionMapInterface;

    @Autowired
    private OvsdbMetrics ovsdbMetrics;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.manager.collectionIntervalSec.event:60}")
    private long collectionIntervalSecEvent;

    // dtop: use anonymous constructor to ensure that the following code always
    // get executed,
    // even when somebody adds another constructor in here
    {
        DefaultMonitorRegistry.getInstance().register(connectionsAttempted);
        DefaultMonitorRegistry.getInstance().register(connectionsCreated);
        DefaultMonitorRegistry.getInstance().register(connectionsDropped);
        DefaultMonitorRegistry.getInstance().register(connectionsFailed);
    }

    @PostConstruct
    private void postCreate() {
        listenForConnections();
    }

    public void listenForConnections() {

        ConnectionCallback connectionCallback = new ConnectionCallback() {

            @Override
            public void connected(OvsdbClient ovsdbClient) {

                connectionsAttempted.increment();

                if (!(ovsdbClient instanceof OvsdbClientWithMetrics)) {
                    ovsdbClient = new OvsdbClientWithMetrics(ovsdbClient, ovsdbMetrics);
                }

                String remoteHost = ovsdbClient.getConnectionInfo().getRemoteAddress().getHostAddress();
                int localPort = ovsdbClient.getConnectionInfo().getLocalPort();
                String subjectDn;
                try {
                    subjectDn = ((X509Certificate) ovsdbClient.getConnectionInfo().getRemoteCertificate()).getSubjectDN().getName();

                    String clientCn = SslUtil.extractCN(subjectDn);
                    LOG.info("ovsdbClient connecting from {} on port {} clientCn {}", remoteHost, localPort, clientCn);

                    ConnectNodeInfo connectNodeInfo = ovsdbDao.getConnectNodeInfo(ovsdbClient);

                    // successfully connected - register it in our
                    // connectedClients table

                    String key = alterClientCnIfRequired(clientCn, connectNodeInfo);
                    ovsdbSessionMapInterface.newSession(key, ovsdbClient);

                    extIntegrationInterface.apConnected(key, connectNodeInfo);

                    processConnectRequest(ovsdbClient, clientCn, connectNodeInfo);

                    monitorOvsdbStateTables(ovsdbClient, key);

                    connectionsCreated.increment();
                    LOG.info("ovsdbClient connected from {} on port {} AP {} ", remoteHost, localPort, key);
                    LOG.info("ovsdbClient connectedClients = {}", ovsdbSessionMapInterface.getNumSessions());

                } catch (IllegalStateException e) {
                    connectionsFailed.increment();
                    LOG.error("autoprovisioning error {}", e.getMessage(), e);
                    // something is wrong with the SSL
                    ovsdbClient.shutdown();
                } catch (Exception e) {
                    connectionsFailed.increment();
                    LOG.error("ovsdbClient error", e);
                    // something is wrong with the SSL
                    ovsdbClient.shutdown();
                }

            }

            @Override
            public void disconnected(OvsdbClient ovsdbClient) {

                connectionsDropped.increment();

                String remoteHost;
                int localPort;
                String clientCn;

                // disconnected - deregister ovsdbClient from our
                // connectedClients table
                // unfortunately we only know clientCn at this point, but in
                // Plume's environment
                // they are not unique
                // so we are doing a reverse lookup here, and then if we find
                // the key we will
                // remove the entry from the connectedClients.
                String key;

                try {
                    remoteHost = ovsdbClient.getConnectionInfo().getRemoteAddress().getHostAddress();
                    localPort = ovsdbClient.getConnectionInfo().getLocalPort();
                    String subjectDn = null;
                    try {
                        subjectDn = ((X509Certificate) ovsdbClient.getConnectionInfo().getRemoteCertificate()).getSubjectDN().getName();
                    } catch (Exception e) {
                        // do nothing
                    }
                    clientCn = SslUtil.extractCN(subjectDn);
                    key = ovsdbSessionMapInterface.lookupClientId(ovsdbClient);
                    if (key != null) {
                        try {
                            extIntegrationInterface.apDisconnected(key);
                            ovsdbSessionMapInterface.removeSession(key);
                        } catch (Exception e) {
                            LOG.debug("Unable to process ap disconnect. {}", e.getMessage());
                        }
                    }
                    LOG.info("ovsdbClient disconnected from {} on port {} clientCn {} AP {} ", remoteHost, localPort, clientCn, key);
                    LOG.info("ovsdbClient connectedClients = {}", ovsdbSessionMapInterface.getNumSessions());
                } finally {
                    try {
                        ovsdbClient.shutdown();
                    } catch (Exception e) {
                        LOG.error("Caught Exception shutting down ovsdb client, may have already been disconnected", e);
                    }

                }

            }

        };

        listener.startListeningWithSsl(ovsdbListenPort, sslContext, connectionCallback).join();

        LOG.info("Manager waiting for connection on port {}...", ovsdbListenPort);
    }

    private void processConnectRequest(OvsdbClient ovsdbClient, String clientCn, ConnectNodeInfo connectNodeInfo) {

        LOG.debug("Starting Client connect");
        connectNodeInfo = ovsdbDao.updateConnectNodeInfoOnConnect(ovsdbClient, clientCn, connectNodeInfo, preventClientCnAlteration);

        // successfully connected - register it in our
        // connectedClients table
        String apId = alterClientCnIfRequired(clientCn, connectNodeInfo);

        LOG.debug("Client {} connect for AP {}", clientCn, apId);

        ovsdbDao.removeAllPasspointConfigs(ovsdbClient);
        ovsdbDao.removeAllSsids(ovsdbClient); // always
        ovsdbDao.removeAllInetConfigs(ovsdbClient);
        ovsdbDao.removeWifiRrm(ovsdbClient);
        ovsdbDao.removeRadsecRadiusAndRealmConfigs(ovsdbClient);
        ovsdbDao.removeAllStatsConfigs(ovsdbClient); // always

        extIntegrationInterface.clearEquipmentStatus(apId);

        OpensyncAPConfig opensyncAPConfig = extIntegrationInterface.getApConfig(apId);

        if (opensyncAPConfig != null) {
            ovsdbDao.configureNode(ovsdbClient, opensyncAPConfig);
            ovsdbDao.configureWifiRrm(ovsdbClient, opensyncAPConfig);
            ovsdbDao.configureGreTunnels(ovsdbClient, opensyncAPConfig);
            ovsdbDao.createVlanNetworkInterfaces(ovsdbClient, opensyncAPConfig);
            ovsdbDao.configureRadsecRadiusAndRealm(ovsdbClient, opensyncAPConfig);
            ovsdbDao.configureSsids(ovsdbClient, opensyncAPConfig);
            if (opensyncAPConfig.getHotspotConfig() != null) {
                ovsdbDao.configureHotspots(ovsdbClient, opensyncAPConfig);
            }

            ovsdbDao.configureInterfaces(ovsdbClient);
            ovsdbDao.configureWifiRadios(ovsdbClient, opensyncAPConfig);

            ovsdbDao.configureStatsFromProfile(ovsdbClient, opensyncAPConfig);
            if (ovsdbDao.getDeviceStatsReportingInterval(ovsdbClient) != collectionIntervalSecDeviceStats) {
                ovsdbDao.updateDeviceStatsReportingInterval(ovsdbClient, collectionIntervalSecDeviceStats);
            }
            ovsdbDao.enableNetworkProbeForSyntheticClient(ovsdbClient);
            ovsdbDao.updateEventReportingInterval(ovsdbClient, collectionIntervalSecEvent);

        } else {
            LOG.info("No Configuration available for {}", apId);
        }

        LOG.debug("Client connect Done");
    }

    @Override
    public Set<String> getConnectedClientIds() {
        return ovsdbSessionMapInterface.getConnectedClientIds();
    }

    /**
     * @param apId
     * @param newRedirectorAddress
     * @return updated value of the redirector
     */
    @Override
    public String changeRedirectorHost(String apId, String newRedirectorAddress) {
        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);
        if (ovsdbSession == null) {
            throw new IllegalStateException("AP with id " + apId + " is not connected");
        }

        return ovsdbDao.changeRedirectorAddress(ovsdbSession.getOvsdbClient(), apId, newRedirectorAddress);
    }

    public String processBlinkRequest(String apId, boolean blinkAllLEDs) {
        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);
        if (ovsdbSession == null) {
            throw new IllegalStateException("AP with id " + apId + " is not connected");
        }

        return ovsdbDao.processBlinkRequest(ovsdbSession.getOvsdbClient(), apId, blinkAllLEDs);
    }

    @Override
    public void processConfigChanged(String apId) {
        LOG.debug("Starting processConfigChanged for {}", apId);
        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);
        if (ovsdbSession == null) {
            throw new IllegalStateException("AP with id " + apId + " is not connected");
        }

        OvsdbClient ovsdbClient = ovsdbSession.getOvsdbClient();

        OpensyncAPConfig opensyncAPConfig = extIntegrationInterface.getApConfig(apId);

        if (opensyncAPConfig == null) {
            LOG.warn("AP with id " + apId + " does not have a config to apply.");
            return;
        }

        ovsdbDao.removeAllPasspointConfigs(ovsdbClient);
        ovsdbDao.removeAllSsids(ovsdbClient); // always
        ovsdbDao.removeAllInetConfigs(ovsdbClient);
        ovsdbDao.removeWifiRrm(ovsdbClient);
        ovsdbDao.removeRadsecRadiusAndRealmConfigs(ovsdbClient);
        ovsdbDao.removeAllStatsConfigs(ovsdbClient);

        extIntegrationInterface.clearEquipmentStatus(apId);

        ovsdbDao.configureNode(ovsdbClient, opensyncAPConfig);
        ovsdbDao.configureWifiRrm(ovsdbClient, opensyncAPConfig);
        ovsdbDao.configureGreTunnels(ovsdbClient, opensyncAPConfig);
        ovsdbDao.createVlanNetworkInterfaces(ovsdbClient, opensyncAPConfig);
        ovsdbDao.configureRadsecRadiusAndRealm(ovsdbClient, opensyncAPConfig);
        ovsdbDao.configureSsids(ovsdbClient, opensyncAPConfig);
        if (opensyncAPConfig.getHotspotConfig() != null) {
            ovsdbDao.configureHotspots(ovsdbClient, opensyncAPConfig);
        }

        ovsdbDao.configureInterfaces(ovsdbClient);
        ovsdbDao.configureWifiRadios(ovsdbClient, opensyncAPConfig);
        
        ovsdbDao.configureStatsFromProfile(ovsdbClient, opensyncAPConfig);
        if (ovsdbDao.getDeviceStatsReportingInterval(ovsdbClient) != collectionIntervalSecDeviceStats) {
            ovsdbDao.updateDeviceStatsReportingInterval(ovsdbClient, collectionIntervalSecDeviceStats);
        }
        ovsdbDao.enableNetworkProbeForSyntheticClient(ovsdbClient);
        ovsdbDao.updateEventReportingInterval(ovsdbClient, collectionIntervalSecEvent);
        
        LOG.debug("Finished processConfigChanged for {}", apId);
    }

    @Override
    public void processClientBlocklistChange(String apId, List<MacAddress> blockList) {
        LOG.debug("Starting processClientBlocklistChange for {} on blockList {}", apId, blockList);

        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);
        if (ovsdbSession == null) {
            throw new IllegalStateException("AP with id " + apId + " is not connected");
        }

        OvsdbClient ovsdbClient = ovsdbSession.getOvsdbClient();

        OpensyncAPConfig opensyncAPConfig = extIntegrationInterface.getApConfig(apId);

        if (opensyncAPConfig != null) {
            ovsdbDao.configureBlockList(ovsdbClient, opensyncAPConfig, blockList);

        } else {
            LOG.warn("Could not get provisioned configuration for AP {}", apId);
        }

        LOG.debug("Finished processClientBlocklistChange for {}", apId);
    }

    private void monitorOvsdbStateTables(OvsdbClient ovsdbClient, String key) {

        LOG.info("Received ovsdb table state monitor request for {}", key);
        try {
            monitorWifiRadioStateDbTable(ovsdbClient, key);
        } catch (OvsdbClientException e) {
            LOG.debug("Could not enable monitor for Wifi_Radio_State table. {}", e.getMessage());
        }

        try {
            monitorWifiInetStateDbTable(ovsdbClient, key);
        } catch (OvsdbClientException e) {
            LOG.debug("Could not enable monitor for Wifi_Inet_State table. {}", e.getMessage());
        }

        try {
            monitorWifiVifStateDbTable(ovsdbClient, key);
        } catch (OvsdbClientException e) {
            LOG.debug("Could not enable monitor for Wifi_VIF_State table. {}", e.getMessage());

        }

        try {
            monitorWifiAssociatedClientsDbTable(ovsdbClient, key);
        } catch (OvsdbClientException e) {
            LOG.debug("Could not enable monitor for Wifi_Associated_Clients table. {}", e.getMessage());
        }

        try {
            monitorAwlanNodeDbTable(ovsdbClient, key);
        } catch (OvsdbClientException e) {
            LOG.debug("Could not enable monitor for AWLAN_Node table. {}", e.getMessage());

        }

        try {
            monitorDhcpLeasedIpDbTable(ovsdbClient, key);
        } catch (OvsdbClientException e) {
            LOG.debug("Could not enable monitor for DHCP_leased_IP table. {}", e.getMessage());

        }

        try {
            monitorCommandStateDbTable(ovsdbClient, key);
        } catch (OvsdbClientException e) {
            LOG.debug("Could not enable monitor for Command_State table. {}", e.getMessage());

        }

        try {
            monitorNodeStateTable(ovsdbClient, key);
        } catch (OvsdbClientException e) {
            LOG.debug("Could not enable monitor for Node_State table. {}", e.getMessage());
        }

        try {
            if (ovsdbClient.getSchema(OvsdbDao.ovsdbName).get().getTables().containsKey("APC_State")) {
                monitorAPCStateTable(ovsdbClient, key);
            }
        } catch (InterruptedException | ExecutionException | OvsdbClientException e) {
            LOG.debug("Could not enable monitor for APC_State table. {}", e);
        }
        LOG.debug("Finished (re)setting monitors for AP {}", key);
    }

    private void monitorDhcpLeasedIpDbTable(OvsdbClient ovsdbClient, String key) throws OvsdbClientException {

        CompletableFuture<TableUpdates> awCf = ovsdbClient.monitor(OvsdbDao.ovsdbName, OvsdbDao.dhcpLeasedIpDbTable + "_" + key,
                new MonitorRequests(ImmutableMap.of(OvsdbDao.dhcpLeasedIpDbTable, new MonitorRequest(new MonitorSelect(true, true, true, true)))),
                tableUpdates -> {
                    try {
                        LOG.info(OvsdbDao.dhcpLeasedIpDbTable + "_" + key + " monitor callback received {}", tableUpdates);

                        List<Map<String, String>> insert = new ArrayList<>();
                        List<Map<String, String>> delete = new ArrayList<>();
                        List<Map<String, String>> update = new ArrayList<>();

                        for (TableUpdate tableUpdate : tableUpdates.getTableUpdates().values()) {
                            for (RowUpdate rowUpdate : tableUpdate.getRowUpdates().values()) {

                                if (rowUpdate.getNew() == null) {
                                    Map<String, String> rowMap = new HashMap<>();

                                    rowUpdate.getOld().getColumns().entrySet().forEach(c -> OvsdbDao.translateDhcpFpValueToString(c, rowMap));

                                    delete.add(rowMap);
                                    // delete
                                } else if (rowUpdate.getOld() == null) {
                                    // insert
                                    Map<String, String> rowMap = new HashMap<>();

                                    rowUpdate.getNew().getColumns().entrySet().forEach(c -> OvsdbDao.translateDhcpFpValueToString(c, rowMap));

                                    insert.add(rowMap);
                                } else {

                                    // insert
                                    Map<String, String> rowMap = new HashMap<>();

                                    rowUpdate.getOld().getColumns().putAll(rowUpdate.getNew().getColumns());
                                    rowUpdate.getOld().getColumns().entrySet().forEach(c -> OvsdbDao.translateDhcpFpValueToString(c, rowMap));

                                    update.add(rowMap);

                                }
                            }
                        }

                        if (!insert.isEmpty()) {
                            extIntegrationInterface.dhcpLeasedIpDbTableUpdate(insert, key, RowUpdateOperation.INSERT);
                        }

                        if (!delete.isEmpty()) {
                            extIntegrationInterface.dhcpLeasedIpDbTableUpdate(delete, key, RowUpdateOperation.DELETE);

                        }

                        if (!update.isEmpty()) {
                            extIntegrationInterface.dhcpLeasedIpDbTableUpdate(update, key, RowUpdateOperation.MODIFY);

                        }
                    } catch (Exception e) {
                        LOG.error("dhcpLeasedIpDbTableUpdate failed", e);
                    }

                });

        awCf.join();

    }

    private void monitorCommandStateDbTable(OvsdbClient ovsdbClient, String key) throws OvsdbClientException {

        CompletableFuture<TableUpdates> csCf = ovsdbClient.monitor(OvsdbDao.ovsdbName, OvsdbDao.commandStateDbTable + "_" + key,
                new MonitorRequests(ImmutableMap.of(OvsdbDao.commandStateDbTable, new MonitorRequest())), tableUpdates -> {
                    try {
                        LOG.info(OvsdbDao.commandStateDbTable + "_" + key + " monitor callback received {}", tableUpdates);

                        List<Map<String, String>> insert = new ArrayList<>();
                        List<Map<String, String>> delete = new ArrayList<>();
                        List<Map<String, String>> update = new ArrayList<>();

                        for (TableUpdate tableUpdate : tableUpdates.getTableUpdates().values()) {
                            for (RowUpdate rowUpdate : tableUpdate.getRowUpdates().values()) {

                                if (rowUpdate.getNew() == null) {
                                    Map<String, String> rowMap = new HashMap<>();

                                    rowUpdate.getOld().getColumns().forEach((key1, value) -> rowMap.put(key1, value.toString()));

                                    delete.add(rowMap);
                                    // delete
                                } else if (rowUpdate.getOld() == null) {
                                    // insert
                                    Map<String, String> rowMap = new HashMap<>();

                                    rowUpdate.getNew().getColumns().forEach((key1, value) -> rowMap.put(key1, value.toString()));

                                    insert.add(rowMap);
                                } else {

                                    // insert
                                    Map<String, String> rowMap = new HashMap<>();

                                    rowUpdate.getOld().getColumns().putAll(rowUpdate.getNew().getColumns());
                                    rowUpdate.getOld().getColumns().forEach((key1, value) -> rowMap.put(key1, value.toString()));

                                    update.add(rowMap);

                                }
                            }
                        }

                        if (!insert.isEmpty()) {
                            extIntegrationInterface.commandStateDbTableUpdate(insert, key, RowUpdateOperation.INSERT);
                        }

                        if (!delete.isEmpty()) {
                            extIntegrationInterface.commandStateDbTableUpdate(delete, key, RowUpdateOperation.DELETE);

                        }

                        if (!update.isEmpty()) {
                            extIntegrationInterface.commandStateDbTableUpdate(update, key, RowUpdateOperation.MODIFY);

                        }
                    } catch (Exception e) {
                        LOG.error("commandStateDbTableUpdate failed", e);
                    }

                });

        csCf.join();

    }

    private void monitorAwlanNodeDbTable(OvsdbClient ovsdbClient, String key) throws OvsdbClientException {
        CompletableFuture<TableUpdates> awCf = ovsdbClient.monitor(OvsdbDao.ovsdbName, OvsdbDao.awlanNodeDbTable + "_" + key,
                new MonitorRequests(ImmutableMap.of(OvsdbDao.awlanNodeDbTable, new MonitorRequest(new MonitorSelect(true, false, false, true)))),
                tableUpdates -> {
                    try {
                        LOG.info(OvsdbDao.awlanNodeDbTable + "_" + key + " monitor callback received {}", tableUpdates);

                        extIntegrationInterface.awlanNodeDbTableUpdate(ovsdbDao.getOpensyncAWLANNode(tableUpdates, key, ovsdbClient), key);
                    } catch (Exception e) {
                        LOG.error("awlanNodeDbTableUpdate failed", e);
                    }
                });

        extIntegrationInterface.awlanNodeDbTableUpdate(ovsdbDao.getOpensyncAWLANNode(awCf.join(), key, ovsdbClient), key);
    }

    private void monitorWifiAssociatedClientsDbTable(OvsdbClient ovsdbClient, String key) throws OvsdbClientException {
        CompletableFuture<TableUpdates> acCf = ovsdbClient.monitor(OvsdbDao.ovsdbName, OvsdbDao.wifiAssociatedClientsDbTable + "_" + key,
                new MonitorRequests(ImmutableMap.of(OvsdbDao.wifiAssociatedClientsDbTable, new MonitorRequest())), tableUpdates -> {

                    try {
                        LOG.info(OvsdbDao.wifiAssociatedClientsDbTable + "_" + key + " monitor callback received {}", tableUpdates);

                        List<OpensyncWifiAssociatedClients> associatedClients = new ArrayList<>();

                        for (TableUpdate tableUpdate : tableUpdates.getTableUpdates().values()) {

                            for (RowUpdate rowUpdate : tableUpdate.getRowUpdates().values()) {
                                if ((rowUpdate.getOld() != null) && (rowUpdate.getNew() == null)) {
                                    Row row = rowUpdate.getOld();
                                    String deletedClientMac = row.getStringColumn("mac");
                                    // take care of the deletes as we go
                                    // through
                                    // the updates, as we want to delete
                                    // before
                                    // adding anyway.
                                    extIntegrationInterface.wifiAssociatedClientsDbTableDelete(deletedClientMac, key);
                                } else {
                                    associatedClients.addAll(ovsdbDao.getOpensyncWifiAssociatedClients(rowUpdate, key, ovsdbClient));
                                }
                            }

                        }

                        // now address the update/add
                        extIntegrationInterface.wifiAssociatedClientsDbTableUpdate(associatedClients, key);
                    } catch (Exception e) {
                        LOG.error("wifiAssociatedClientsDbTableUpdate failed", e);
                    }

                });

        extIntegrationInterface.wifiAssociatedClientsDbTableUpdate(ovsdbDao.getInitialOpensyncWifiAssociatedClients(acCf.join(), key, ovsdbClient), key);

    }

    private void monitorWifiInetStateDbTable(OvsdbClient ovsdbClient, String key) throws OvsdbClientException {
        CompletableFuture<TableUpdates> isCf = ovsdbClient.monitor(OvsdbDao.ovsdbName, OvsdbDao.wifiInetStateDbTable + "_" + key,
                new MonitorRequests(ImmutableMap.of(OvsdbDao.wifiInetStateDbTable, new MonitorRequest(new MonitorSelect(true, true, true, true)))),
                tableUpdates -> {
                    try {
                        LOG.info(OvsdbDao.wifiInetStateDbTable + "_" + key + " monitor callback received {}", tableUpdates);

                        List<OpensyncAPInetState> inetStateInsertOrUpdate = new ArrayList<>();
                        List<OpensyncAPInetState> inetStateDelete = new ArrayList<>();

                        for (TableUpdate tableUpdate : tableUpdates.getTableUpdates().values()) {

                            for (RowUpdate rowUpdate : tableUpdate.getRowUpdates().values()) {

                                if (rowUpdate.getNew() == null) {
                                    inetStateDelete.addAll(ovsdbDao.getOpensyncApInetStateForRowUpdate(rowUpdate, key, ovsdbClient));
                                } else {
                                    inetStateInsertOrUpdate.addAll(ovsdbDao.getOpensyncApInetStateForRowUpdate(rowUpdate, key, ovsdbClient));
                                }

                            }
                        }

                        // delete first
                        extIntegrationInterface.wifiInetStateDbTableUpdate(inetStateDelete, key);

                        // now process updates and mutations
                        extIntegrationInterface.wifiInetStateDbTableUpdate(inetStateInsertOrUpdate, key);
                    } catch (Exception e) {
                        LOG.error("wifiInetStateDbTableUpdate failed", e);
                    }

                });

        extIntegrationInterface.wifiInetStateDbTableUpdate(ovsdbDao.getInitialOpensyncApInetStateForRowUpdate(isCf.join(), key, ovsdbClient), key);

    }

    private void monitorWifiRadioStateDbTable(OvsdbClient ovsdbClient, String key) throws OvsdbClientException {

        CompletableFuture<TableUpdates> rsCf = ovsdbClient.monitor(OvsdbDao.ovsdbName, OvsdbDao.wifiRadioStateDbTable + "_" + key,
                new MonitorRequests(ImmutableMap.of(OvsdbDao.wifiRadioStateDbTable, new MonitorRequest(new MonitorSelect(true, false, false, true)))),
                tableUpdates -> {
                    try {
                        LOG.info(OvsdbDao.wifiRadioStateDbTable + "_" + key + " monitor callback received {}", tableUpdates);

                        extIntegrationInterface.wifiRadioStatusDbTableUpdate(ovsdbDao.getOpensyncAPRadioState(tableUpdates, key, ovsdbClient), key);
                    } catch (Exception e) {
                        LOG.error("wifiRadioStatusDbTableUpdate failed", e);
                    }
                });
        extIntegrationInterface.wifiRadioStatusDbTableUpdate(ovsdbDao.getOpensyncAPRadioState(rsCf.join(), key, ovsdbClient), key);
    }

    private void monitorWifiVifStateDbTable(OvsdbClient ovsdbClient, String key) throws OvsdbClientException {

        CompletableFuture<TableUpdates> vsCf = ovsdbClient.monitor(OvsdbDao.ovsdbName, OvsdbDao.wifiVifStateDbTable + "_" + key,
                new MonitorRequests(ImmutableMap.of(OvsdbDao.wifiVifStateDbTable, new MonitorRequest(new MonitorSelect(false, true, true, true)))),
                tableUpdates -> {
                    try {
                        LOG.info(OvsdbDao.wifiVifStateDbTable + "_" + key + " monitor callback received {}", tableUpdates);

                        List<OpensyncAPVIFState> vifsToDelete = new ArrayList<>();
                        List<OpensyncAPVIFState> vifsToInsertOrUpdate = new ArrayList<>();
                        for (TableUpdate tableUpdate : tableUpdates.getTableUpdates().values()) {

                            for (RowUpdate rowUpdate : tableUpdate.getRowUpdates().values()) {

                                if (rowUpdate.getNew() == null) {
                                    // this is a deletion
                                    vifsToDelete.addAll(ovsdbDao.getOpensyncApVifStateForRowUpdate(rowUpdate, key, ovsdbClient));

                                } else {
                                    // either an insert or
                                    // mutuate/update
                                    vifsToInsertOrUpdate.addAll(ovsdbDao.getOpensyncApVifStateForRowUpdate(rowUpdate, key, ovsdbClient));

                                }

                            }

                            // delete first, if required
                            if (!vifsToDelete.isEmpty()) {
                                extIntegrationInterface.wifiVIFStateDbTableDelete(vifsToDelete, key);
                            }
                            if (!vifsToInsertOrUpdate.isEmpty()) {
                                extIntegrationInterface.wifiVIFStateDbTableUpdate(vifsToInsertOrUpdate, key);
                            }

                        }
                    } catch (Exception e) {
                        LOG.error("wifiVIFStateDbTableUpdate failed", e);
                    }

                });
        vsCf.join();

    }

    private void monitorNodeStateTable(OvsdbClient ovsdbClient, String key) throws OvsdbClientException {
        CompletableFuture<TableUpdates> nsCf = ovsdbClient.monitor(OvsdbDao.ovsdbName, OvsdbDao.nodeStateTable + "_" + key,
                new MonitorRequests(ImmutableMap.of(OvsdbDao.nodeStateTable, new MonitorRequest(new MonitorSelect(true, true, true, true)))), tableUpdates -> {
                    LOG.info(OvsdbDao.nodeStateTable + "_" + key + " monitor callback received {}");
                    extIntegrationInterface.nodeStateDbTableUpdate(processNodeStateTableUpdate(key, tableUpdates), key);
                    
                });
        extIntegrationInterface.nodeStateDbTableUpdate(processNodeStateTableUpdate(key, nsCf.join()), key);
    }

    private List<Map<String,String>> processNodeStateTableUpdate(String key, TableUpdates tableUpdates) {
        List<Map<String, String>> updates = new ArrayList<>();
        tableUpdates.getTableUpdates().forEach((key1, value) -> {
            LOG.info("TableUpdate for {}", key1);
            value.getRowUpdates().values().forEach(r -> {
                if (r.getOld() != null) {
                    LOG.info("Node_State old row {}", r.getOld().getColumns());
                }
                if (r.getNew() != null) {
                    LOG.info("Node_State new row {}", r.getNew().getColumns());
                    Map<String, Value> columns = r.getNew().getColumns();
                    Map<String, String> update = new HashMap<>();
                    update.put("key", columns.get("key").toString());
                    update.put("module", columns.get("module").toString());
                    update.put("persist", columns.get("persist").toString());
                    update.put("value", columns.get("value").toString());
                    updates.add(update);
                }
            });

        });
        
        return updates;
    }

    private void monitorAPCStateTable(OvsdbClient ovsdbClient, String key) throws OvsdbClientException {
        CompletableFuture<TableUpdates> nsCf = ovsdbClient.monitor(OvsdbDao.ovsdbName, OvsdbDao.apcStateDbTable + "_" + key,
                new MonitorRequests(ImmutableMap.of(OvsdbDao.apcStateDbTable, new MonitorRequest(new MonitorSelect(true, true, true, true)))), tableUpdates -> {
                    LOG.info(OvsdbDao.apcStateDbTable + "_" + key + " monitor callback received {}");
                    tableUpdates.getTableUpdates().forEach((key1, value) -> {
                        LOG.info("TableUpdate for {}", key1);
                        value.getRowUpdates().values().forEach(r -> {

                            Map<String, String> apcStateAttributes = ovsdbDao.getAPCState(r, key);

                            extIntegrationInterface.apcStateDbTableUpdate(apcStateAttributes, key, RowUpdateOperation.MODIFY);

                        });

                    });
                });
        try {
            extIntegrationInterface.apcStateDbTableUpdate(
                    ovsdbDao.getAPCState(nsCf.join().getTableUpdates().entrySet().iterator().next().getValue().getRowUpdates().values().iterator().next(), key),
                    key, RowUpdateOperation.INIT);
        } catch (Exception e) {
            LOG.error("Could not get INIT state for {}", OvsdbDao.apcStateDbTable, e);
        }
    }

    @Override
    public String closeSession(String apId) {
        OvsdbSession session = ovsdbSessionMapInterface.getSession(apId);
        if (session != null) {
            try {
                session.getOvsdbClient().shutdown();
            } catch (Exception e) {
                LOG.error("Failed to close session to " + apId + " " + e.getLocalizedMessage());
                return "Failed to close session to " + apId + " " + e.getLocalizedMessage();

            }
        }
        LOG.debug("Closed session to " + apId);
        return "Closed session to " + apId;
    }

    @Override
    public String processFirmwareDownload(String apId, String firmwareUrl, String firmwareVersion, String username) {
        try {
            OvsdbSession session = ovsdbSessionMapInterface.getSession(apId);

            ovsdbDao.configureFirmwareDownload(session.getOvsdbClient(), apId, firmwareUrl, firmwareVersion, username);
        } catch (Exception e) {
            LOG.error("Failed to initialize firmware download to " + apId + " " + e.getLocalizedMessage());
            return "Failed to initialize firmware download to " + apId + " " + e.getLocalizedMessage();

        }
        LOG.debug("Initialized firmware download to " + apId);
        return "Initialized firmware download to " + apId;
    }

    @Override
    public String processFirmwareFlash(String apId, String firmwareVersion, String username) {
        OvsdbSession session = ovsdbSessionMapInterface.getSession(apId);
        try {
            ovsdbDao.configureFirmwareFlash(session.getOvsdbClient(), apId, firmwareVersion, username);
        } catch (Exception e) {
            LOG.error("Failed to flash firmware for " + apId + " " + e.getLocalizedMessage());
            return "Failed to flash firmware for " + apId + " " + e.getLocalizedMessage();

        }
        LOG.debug("Initiated firmware flash for AP {} to {} ", apId, firmwareVersion);
        return "Initiated firmware flash for AP " + apId + " to " + firmwareVersion;
    }

    @Override
    public String startDebugEngine(String apId, String gatewayHostname, Integer gatewayPort) {
        LOG.debug("TipWlanOvsdbClient::startDebugEngine apId {} gatewayHostname {} gatewayPort {}", apId, gatewayHostname, gatewayPort);

        try {
            OvsdbSession session = ovsdbSessionMapInterface.getSession(apId);
            OvsdbClient ovsdbClient = session.getOvsdbClient();
            Map<String, String> payload = new HashMap<>();
            payload.put("gateway_hostname", gatewayHostname);
            payload.put("gateway_port", gatewayPort.toString());
            ovsdbDao.configureCommands(ovsdbClient, OvsdbDao.StartDebugEngineApCommand, payload, 0L, defaultCommandDurationSec);

            LOG.debug("Started debug engine on AP {} with gateway {} port {}", apId, gatewayHostname, gatewayPort);
            return "Started debug engine on AP " + apId + " with gateway " + gatewayHostname + " port " + gatewayPort;
        } catch (Exception e) {
            LOG.error("TipWlanOvsdbClient::startDebugEngine Failed to start debug engine on AP {} with gateway {} port {}", apId, gatewayHostname, gatewayPort,
                    e);
            return "Failed to start debug engine on AP " + apId + " with gateway " + gatewayHostname + " port " + gatewayPort;
        }
    }

    @Override
    public String stopDebugEngine(String apId) {
        LOG.debug("TipWlanOvsdbClient::stopDebugEngine apId {}", apId);

        try {
            OvsdbSession session = ovsdbSessionMapInterface.getSession(apId);
            OvsdbClient ovsdbClient = session.getOvsdbClient();
            Map<String, String> payload = new HashMap<>();
            ovsdbDao.configureCommands(ovsdbClient, OvsdbDao.StopDebugEngineApCommand, payload, 0L, 0L);

            LOG.debug("TipWlanOvsdbClient::stopDebugEngine Stop debug engine on AP  {}", apId);
            return "Stop debug engine on AP " + apId;
        } catch (Exception e) {
            LOG.error("TipWlanOvsdbClient::stopDebugEngine Failed to request stop debug engine on AP  {}", apId, e);
            return "Failed to request stop debug engine on AP " + apId;
        }
    }

    @Override
    public String processRebootRequest(String apId, boolean switchBanks) {
        try {
            OvsdbSession session = ovsdbSessionMapInterface.getSession(apId);
            OvsdbClient ovsdbClient = session.getOvsdbClient();

            if (switchBanks) {
                LOG.debug("TipWlanOvsdbClient::processRebootRequest Switch software bank on AP {}", apId);

                ovsdbDao.rebootOrResetAp(ovsdbClient, OvsdbStringConstants.OVSDB_AWLAN_AP_SWITCH_SOFTWARE_BANK);
                LOG.debug("TipWlanOvsdbClient::processRebootRequest triggered switch software bank on AP {}", apId);
                return "Switch software bank on AP " + apId;

            } else {
                LOG.debug("TipWlanOvsdbClient::processRebootRequest Reboot AP {}", apId);

                ovsdbDao.rebootOrResetAp(ovsdbClient, OvsdbStringConstants.OVSDB_AWLAN_AP_REBOOT);
                LOG.debug("TipWlanOvsdbClient::processRebootRequest triggered reboot of AP {}", apId);
                return "Reboot AP  " + apId;

            }
        } catch (Exception e) {
            if (switchBanks) {
                LOG.debug("TipWlanOvsdbClient::processRebootRequest failed to trigger switch software bank on AP {}", apId, e);
                return "failed to trigger switch software bank on AP " + apId;
            } else {
                LOG.error("TipWlanOvsdbClient::processRebootRequest failed to trigger reboot of AP {}", apId, e);
                return "Failed to trigger reboot of AP " + apId;
            }
        }

    }

    @Override
    public String processFactoryResetRequest(String apId) {

        LOG.debug("TipWlanOvsdbClient::processFactoryResetRequest for AP {}", apId);

        try {
            OvsdbSession session = ovsdbSessionMapInterface.getSession(apId);
            OvsdbClient ovsdbClient = session.getOvsdbClient();
            ovsdbDao.rebootOrResetAp(ovsdbClient, OvsdbStringConstants.OVSDB_AWLAN_AP_FACTORY_RESET);
            LOG.debug("TipWlanOvsdbClient::processRebootRequest Triggered a factory reset of AP   {}", apId);
            return "Triggered a factory reset of AP  " + apId;
        } catch (Exception e) {
            LOG.error("TipWlanOvsdbClient::processRebootRequest failed to trigger a factory reset of AP {}", apId, e);
            return "failed to trigger a factory reset of AP " + apId;
        }

    }

    public String alterClientCnIfRequired(String clientCn, ConnectNodeInfo connectNodeInfo) {
        String key;
        // can clientCn be altered
        if (preventClientCnAlteration) {
            key = clientCn;
        } else {
            // does clientCn already end with the AP serial number, if so, use
            // this
            if (clientCn.endsWith("_" + connectNodeInfo.serialNumber)) {
                key = clientCn;
            } else {
                // append the serial number
                key = clientCn + "_" + connectNodeInfo.serialNumber;
            }
        }
        return key;
    }

    public String processNewChannelsRequest(String apId, Map<RadioType, Integer> backupChannelMap, Map<RadioType, Integer> primaryChannelMap) {
        LOG.info("TipWlanOvsdbClient::processNewChannelsRequest for AP {}", apId);

        try {
            OvsdbSession session = ovsdbSessionMapInterface.getSession(apId);
            OvsdbClient ovsdbClient = session.getOvsdbClient();
            ovsdbDao.processNewChannelsRequest(ovsdbClient, backupChannelMap, primaryChannelMap);
            LOG.info("TipWlanOvsdbClient::processNewChannelsRequest change backup and/or primary channels for AP {}", apId);
            return " change backup and/or primary channels for AP " + apId;
        } catch (Exception e) {
            LOG.error("TipWlanOvsdbClient::processNewChannelsRequest failed to change backup and/or primary channels for AP {}", apId, e);
            return "failed to change backup and/or primary channels for AP " + apId;
        }
    }

    public String processCellSizeAttributesRequest(String apId, Map<RadioType, CellSizeAttributes> cellSizeAttributeMap) {
        LOG.info("TipWlanOvsdbClient::processCellSizeAttributesRequest for AP {}", apId);

        try {
            OvsdbSession session = ovsdbSessionMapInterface.getSession(apId);
            OvsdbClient ovsdbClient = session.getOvsdbClient();
            ovsdbDao.processCellSizeAttributesRequest(ovsdbClient, cellSizeAttributeMap);
            LOG.info("TipWlanOvsdbClient::processCellSizeAttributesRequest change cellSizeAttributes for AP {}", apId);
            return " change cell size attributes for AP " + apId;
        } catch (Exception e) {
            LOG.error("TipWlanOvsdbClient::processCellSizeAttributesRequest failed to change cell size attributes for AP {}", apId, e);
            return "failed to change cell size attributes for AP " + apId;
        }
    }

}
