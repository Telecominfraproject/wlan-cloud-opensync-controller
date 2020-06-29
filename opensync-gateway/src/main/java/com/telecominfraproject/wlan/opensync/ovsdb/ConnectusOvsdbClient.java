package com.telecominfraproject.wlan.opensync.ovsdb;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.telecominfraproject.wlan.opensync.external.integration.ConnectusOvsdbClientInterface;
import com.telecominfraproject.wlan.opensync.external.integration.OpensyncExternalIntegrationInterface;
import com.telecominfraproject.wlan.opensync.external.integration.OvsdbSession;
import com.telecominfraproject.wlan.opensync.external.integration.OvsdbSessionMapInterface;
import com.telecominfraproject.wlan.opensync.external.integration.models.ConnectNodeInfo;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPVIFState;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.OvsdbDao;
import com.telecominfraproject.wlan.opensync.util.SslUtil;
import com.vmware.ovsdb.callback.ConnectionCallback;
import com.vmware.ovsdb.callback.MonitorCallback;
import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.protocol.methods.MonitorRequest;
import com.vmware.ovsdb.protocol.methods.MonitorRequests;
import com.vmware.ovsdb.protocol.methods.MonitorSelect;
import com.vmware.ovsdb.protocol.methods.RowUpdate;
import com.vmware.ovsdb.protocol.methods.TableUpdate;
import com.vmware.ovsdb.protocol.methods.TableUpdates;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.service.OvsdbClient;
import com.vmware.ovsdb.service.OvsdbPassiveConnectionListener;

import io.netty.handler.ssl.SslContext;

@Profile("ovsdb_manager")
@Component
public class ConnectusOvsdbClient implements ConnectusOvsdbClientInterface {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectusOvsdbClient.class);

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.listenPort:6640}")
    private int ovsdbListenPort;

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.manager.collectionIntervalSec.deviceStats:120}")
    private long collectionIntervalSecDeviceStats;

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

    @PostConstruct
    private void postCreate() {
        listenForConnections();
    }

    public void listenForConnections() {

        ConnectionCallback connectionCallback = new ConnectionCallback() {
            @Override
            public void connected(OvsdbClient ovsdbClient) {

                String remoteHost = ovsdbClient.getConnectionInfo().getRemoteAddress().getHostAddress();
                int localPort = ovsdbClient.getConnectionInfo().getLocalPort();
                String subjectDn = null;
                try {
                    subjectDn = ((X509Certificate) ovsdbClient.getConnectionInfo().getRemoteCertificate())
                            .getSubjectDN().getName();

                    String clientCn = SslUtil.extractCN(subjectDn);
                    LOG.info("ovsdbClient connecting from {} on port {} clientCn {}", remoteHost, localPort, clientCn);

                    ConnectNodeInfo connectNodeInfo = ovsdbDao.getConnectNodeInfo(ovsdbClient);

                    // successfully connected - register it in our
                    // connectedClients table
                    // In Plume's environment clientCn is not unique that's why
                    // we are augmenting it
                    // with the serialNumber and using it as a key (equivalent
                    // of KDC unique qrCode)
                    String key = clientCn + "_" + connectNodeInfo.serialNumber;
                    ovsdbSessionMapInterface.newSession(key, ovsdbClient);

                    extIntegrationInterface.apConnected(key, connectNodeInfo);

                    // push configuration to AP
                    connectNodeInfo = processConnectRequest(ovsdbClient, clientCn, connectNodeInfo);

                    monitorOvsdbStateTables(ovsdbClient, key);

                    LOG.info("ovsdbClient connected from {} on port {} key {} ", remoteHost, localPort, key);
                    LOG.info("ovsdbClient connectedClients = {}", ovsdbSessionMapInterface.getNumSessions());

                } catch (Exception e) {
                    LOG.error("ovsdbClient error", e);
                    // something is wrong with the SSL
                    ovsdbClient.shutdown();
                    return;
                }

            }

            @Override
            public void disconnected(OvsdbClient ovsdbClient) {
                String remoteHost = ovsdbClient.getConnectionInfo().getRemoteAddress().getHostAddress();
                int localPort = ovsdbClient.getConnectionInfo().getLocalPort();
                String subjectDn = null;
                try {
                    subjectDn = ((X509Certificate) ovsdbClient.getConnectionInfo().getRemoteCertificate())
                            .getSubjectDN().getName();
                } catch (Exception e) {
                    // do nothing
                }

                String clientCn = SslUtil.extractCN(subjectDn);

                // disconnected - deregister ovsdbClient from our
                // connectedClients table
                // unfortunately we only know clientCn at this point, but in
                // Plume's environment
                // they are not unique
                // so we are doing a reverse lookup here, and then if we find
                // the key we will
                // remove the entry from the connectedClients.
                String key = ovsdbSessionMapInterface.lookupClientId(ovsdbClient);

                if (key != null) {
                    try {
                        extIntegrationInterface.apDisconnected(key);
                        ovsdbSessionMapInterface.removeSession(key);
                    } catch (Exception e) {
                        LOG.debug("Unable to process ap disconnect. {}", e.getMessage());
                    } finally {
                        ovsdbClient.shutdown();
                    }
                }

                LOG.info("ovsdbClient disconnected from {} on port {} clientCn {} key {} ", remoteHost, localPort,
                        clientCn, key);
                LOG.info("ovsdbClient connectedClients = {}", ovsdbSessionMapInterface.getNumSessions());
            }

        };

        listener.startListeningWithSsl(ovsdbListenPort, sslContext, connectionCallback).join();

        LOG.info("Manager waiting for connection on port {}...", ovsdbListenPort);
    }

    private ConnectNodeInfo processConnectRequest(OvsdbClient ovsdbClient, String clientCn,
            ConnectNodeInfo connectNodeInfo) {

        LOG.debug("Starting Client connect");
        connectNodeInfo = ovsdbDao.updateConnectNodeInfoOnConnect(ovsdbClient, clientCn, connectNodeInfo);

        String apId = clientCn + "_" + connectNodeInfo.serialNumber;
        OpensyncAPConfig opensyncAPConfig = extIntegrationInterface.getApConfig(apId);

        try {
            ovsdbDao.provisionBridgePortInterface(ovsdbClient);
        } catch (Exception e) {
            LOG.warn("Could not provision Bridge->Port->Interface mapping.", e);
        }

        ovsdbDao.removeAllSsids(ovsdbClient); // always

        if (opensyncAPConfig != null) {
            ovsdbDao.configureWifiRadios(ovsdbClient, opensyncAPConfig);
            ovsdbDao.configureSsids(ovsdbClient, opensyncAPConfig);
        }

        ovsdbDao.removeAllStatsConfigs(ovsdbClient); // always
        ovsdbDao.configureStats(ovsdbClient);

        // Check if device stats is configured in Wifi_Stats_Config table,
        // provision it
        // if needed
        if (ovsdbDao.getDeviceStatsReportingInterval(ovsdbClient) != collectionIntervalSecDeviceStats) {
            ovsdbDao.updateDeviceStatsReportingInterval(ovsdbClient, collectionIntervalSecDeviceStats);
        }

        // ovsdbDao.configureWifiInet(ovsdbClient);

        LOG.debug("Client connect Done");
        return connectNodeInfo;
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
    public String changeRedirectorAddress(String apId, String newRedirectorAddress) {
        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);
        if (ovsdbSession == null) {
            throw new IllegalStateException("AP with id " + apId + " is not connected");
        }

        String ret = ovsdbDao.changeRedirectorAddress(ovsdbSession.getOvsdbClient(), apId, newRedirectorAddress);

        return ret;
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

        if (opensyncAPConfig != null) {
            try {
                ovsdbClient.cancelMonitor(OvsdbDao.awlanNodeDbTable + "_" + apId).join();
                ovsdbClient.cancelMonitor(OvsdbDao.wifiRadioStateDbTable + "_" + apId).join();
                ovsdbClient.cancelMonitor(OvsdbDao.wifiVifStateDbTable + "_" + apId).join();
                ovsdbClient.cancelMonitor(OvsdbDao.wifiVifStateDbTable + "_delete_" + apId).join();
                ovsdbClient.cancelMonitor(OvsdbDao.wifiInetStateDbTable + "_" + apId).join();
                ovsdbClient.cancelMonitor(OvsdbDao.wifiAssociatedClientsDbTable + "_" + apId).join();
                ovsdbClient.cancelMonitor(OvsdbDao.wifiAssociatedClientsDbTable + "_delete_" + apId).join();

                ovsdbDao.removeAllSsids(ovsdbClient);

                ovsdbDao.configureWifiRadios(ovsdbClient, opensyncAPConfig);
                ovsdbDao.configureSsids(ovsdbClient, opensyncAPConfig);

                ovsdbDao.removeAllStatsConfigs(ovsdbClient); // always
                ovsdbDao.configureStats(ovsdbClient);

                if (ovsdbDao.getDeviceStatsReportingInterval(ovsdbClient) != collectionIntervalSecDeviceStats) {
                    ovsdbDao.updateDeviceStatsReportingInterval(ovsdbClient, collectionIntervalSecDeviceStats);
                }

            } catch (OvsdbClientException e) {
                LOG.error("Could not enable/disable table state monitors, cannot proccess config change for AP {}",
                        apId);
            } finally {
                monitorOvsdbStateTables(ovsdbClient, apId);
            }

        } else {
            LOG.warn("Could not get provisioned configuration for AP {}", apId);
        }

        LOG.debug("Finished processConfigChanged for {}", apId);
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
            monitorWifiVifStateDbTableDeletion(ovsdbClient, key);
        } catch (OvsdbClientException e) {
            LOG.debug("Could not enable monitor for deletions to Wifi_VIF_State table. {}", e.getMessage());

        }
        try {
            monitorWifiAssociatedClientsDbTable(ovsdbClient, key);
        } catch (OvsdbClientException e) {
            LOG.debug("Could not enable monitor for Wifi_Associated_Clients table. {}", e.getMessage());
        }
        try {
            monitorWifiAssociatedClientsDbTableDeletion(ovsdbClient, key);
        } catch (OvsdbClientException e) {
            LOG.debug("Could not enable monitor for deletions to Wifi_Associated_Clients table. {}", e.getMessage());

        }
        try {
            monitorAwlanNodeDbTable(ovsdbClient, key);
        } catch (OvsdbClientException e) {
            LOG.debug("Could not enable monitor for deletions to AWLAN_Node table. {}", e.getMessage());

        }

    }

    private void monitorAwlanNodeDbTable(OvsdbClient ovsdbClient, String key) throws OvsdbClientException {
        CompletableFuture<TableUpdates> awCf = ovsdbClient
                .monitor(OvsdbDao.ovsdbName, OvsdbDao.awlanNodeDbTable + "_" + key,
                        new MonitorRequests(ImmutableMap.of(OvsdbDao.awlanNodeDbTable,
                                new MonitorRequest(new MonitorSelect(true, false, false, true)))),
                        new MonitorCallback() {

                            @Override
                            public void update(TableUpdates tableUpdates) {
                                LOG.info("Monitor callback received {}", tableUpdates);

                                extIntegrationInterface.awlanNodeDbTableUpdate(
                                        ovsdbDao.getOpensyncAWLANNode(tableUpdates, key, ovsdbClient), key);
                            }

                        });

        extIntegrationInterface.awlanNodeDbTableUpdate(ovsdbDao.getOpensyncAWLANNode(awCf.join(), key, ovsdbClient),
                key);

    }

    private void monitorWifiAssociatedClientsDbTable(OvsdbClient ovsdbClient, String key) throws OvsdbClientException {
        CompletableFuture<TableUpdates> acCf = ovsdbClient
                .monitor(OvsdbDao.ovsdbName, OvsdbDao.wifiAssociatedClientsDbTable + "_" + key,
                        new MonitorRequests(ImmutableMap.of(OvsdbDao.wifiAssociatedClientsDbTable,
                                new MonitorRequest(new MonitorSelect(true, true, false, true)))),
                        new MonitorCallback() {

                            @Override
                            public void update(TableUpdates tableUpdates) {
                                LOG.info("Monitor callback received {}", tableUpdates);

                                for (TableUpdate tableUpdate : tableUpdates.getTableUpdates().values()) {

                                    extIntegrationInterface.wifiAssociatedClientsDbTableUpdate(
                                            ovsdbDao.getOpensyncWifiAssociatedClients(tableUpdates, key, ovsdbClient),
                                            key);
                                }
                            }

                        });

        extIntegrationInterface.wifiAssociatedClientsDbTableUpdate(
                ovsdbDao.getOpensyncWifiAssociatedClients(acCf.join(), key, ovsdbClient), key);

    }

    private void monitorWifiInetStateDbTable(OvsdbClient ovsdbClient, String key) throws OvsdbClientException {
        CompletableFuture<TableUpdates> isCf = ovsdbClient.monitor(OvsdbDao.ovsdbName,
                OvsdbDao.wifiInetStateDbTable + "_" + key,
                new MonitorRequests(ImmutableMap.of(OvsdbDao.wifiInetStateDbTable,
                        new MonitorRequest(new MonitorSelect(true, true, true, true)))),
                new MonitorCallback() {

                    @Override
                    public void update(TableUpdates tableUpdates) {
                        LOG.info("Monitor callback received {}", tableUpdates);

                        extIntegrationInterface.wifiInetStateDbTableUpdate(
                                ovsdbDao.getOpensyncAPInetState(tableUpdates, key, ovsdbClient), key);

                    }

                });

        extIntegrationInterface
                .wifiInetStateDbTableUpdate(ovsdbDao.getOpensyncAPInetState(isCf.join(), key, ovsdbClient), key);
    }

    private void monitorWifiRadioStateDbTable(OvsdbClient ovsdbClient, String key) throws OvsdbClientException {
        CompletableFuture<TableUpdates> rsCf = ovsdbClient
                .monitor(OvsdbDao.ovsdbName, OvsdbDao.wifiRadioStateDbTable + "_" + key,
                        new MonitorRequests(ImmutableMap.of(OvsdbDao.wifiRadioStateDbTable,
                                new MonitorRequest(new MonitorSelect(true, false, false, true)))),
                        new MonitorCallback() {

                            @Override
                            public void update(TableUpdates tableUpdates) {
                                LOG.info("Monitor callback received {}", tableUpdates);

                                extIntegrationInterface.wifiRadioStatusDbTableUpdate(
                                        ovsdbDao.getOpensyncAPRadioState(tableUpdates, key, ovsdbClient), key);
                            }

                        });
        extIntegrationInterface
                .wifiRadioStatusDbTableUpdate(ovsdbDao.getOpensyncAPRadioState(rsCf.join(), key, ovsdbClient), key);
    }

    private void monitorWifiVifStateDbTableDeletion(OvsdbClient ovsdbClient, String key) throws OvsdbClientException {
        CompletableFuture<TableUpdates> vsdCf = ovsdbClient
                .monitor(OvsdbDao.ovsdbName, OvsdbDao.wifiVifStateDbTable + "_delete_" + key,
                        new MonitorRequests(ImmutableMap.of(OvsdbDao.wifiVifStateDbTable,
                                new MonitorRequest(new MonitorSelect(true, false, true, false)))),
                        new MonitorCallback() {
                            @Override
                            public void update(TableUpdates tableUpdates) {

                                LOG.info("Monitor callback received {}", tableUpdates);

                                // extIntegrationInterface.wifiVIFStateDbTableUpdate(
                                // ovsdbDao.getOpensyncAPVIFState(tableUpdates,
                                // key, ovsdbClient), key);
                                List<OpensyncAPVIFState> vifsToDelete = new ArrayList<>();
                                for (Entry<String, TableUpdate> tableUpdate : tableUpdates.getTableUpdates()
                                        .entrySet()) {

                                    for (Entry<UUID, RowUpdate> rowUpdate : tableUpdate.getValue().getRowUpdates()
                                            .entrySet()) {
                                        if ((rowUpdate.getValue().getOld() != null)
                                                && (rowUpdate.getValue().getNew() == null)) {
                                            Row row = rowUpdate.getValue().getOld();
                                            String ifName = null;
                                            String ssid = null;
                                            if ((row.getColumns().get("ssid") != null)
                                                    && row.getColumns().get("ssid").getClass().equals(
                                                            com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                                                ssid = row.getStringColumn("ssid");
                                            }
                                            if ((row.getColumns().get("if_name") != null)
                                                    && row.getColumns().get("if_name").getClass().equals(
                                                            com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                                                ifName = row.getStringColumn("if_name");
                                            }
                                            if ((ifName != null) && (ssid != null)) {
                                                OpensyncAPVIFState toBeDeleted = new OpensyncAPVIFState();
                                                toBeDeleted.setSsid(ssid);
                                                toBeDeleted.setIfName(ifName);
                                                vifsToDelete.add(toBeDeleted);
                                            }
                                            tableUpdate.getValue().getRowUpdates().remove(rowUpdate.getKey());
                                        }

                                    }

                                    if (tableUpdate.getValue().getRowUpdates().isEmpty()) {
                                        tableUpdates.getTableUpdates().remove(tableUpdate.getKey());
                                    }

                                }

                                if (!vifsToDelete.isEmpty()) {
                                    extIntegrationInterface.wifiVIFStateDbTableDelete(vifsToDelete, key);
                                }

                            }

                        });

        vsdCf.join();
    }

    private void monitorWifiAssociatedClientsDbTableDeletion(OvsdbClient ovsdbClient, String key)
            throws OvsdbClientException {
        CompletableFuture<TableUpdates> acdCf = ovsdbClient
                .monitor(OvsdbDao.ovsdbName, OvsdbDao.wifiAssociatedClientsDbTable + "_delete_" + key,
                        new MonitorRequests(ImmutableMap.of(OvsdbDao.wifiAssociatedClientsDbTable,
                                new MonitorRequest(new MonitorSelect(true, false, true, false)))),
                        new MonitorCallback() {

                            @Override
                            public void update(TableUpdates tableUpdates) {
                                LOG.info("Monitor callback received {}", tableUpdates);

                                for (TableUpdate tableUpdate : tableUpdates.getTableUpdates().values()) {

                                    for (RowUpdate rowUpdate : tableUpdate.getRowUpdates().values()) {
                                        if ((rowUpdate.getOld() != null) && (rowUpdate.getNew() == null)) {
                                            Row row = rowUpdate.getOld();
                                            String deletedClientMac = row.getStringColumn("mac");
                                            extIntegrationInterface.wifiAssociatedClientsDbTableDelete(deletedClientMac,
                                                    key);
                                        }
                                    }

                                }

                            }

                        });
        acdCf.join();
    }

    private void monitorWifiVifStateDbTable(OvsdbClient ovsdbClient, String key) throws OvsdbClientException {
        CompletableFuture<TableUpdates> vsCf = ovsdbClient.monitor(OvsdbDao.ovsdbName,
                OvsdbDao.wifiVifStateDbTable + "_" + key,
                new MonitorRequests(ImmutableMap.of(OvsdbDao.wifiVifStateDbTable,
                        new MonitorRequest(new MonitorSelect(true, true, false, true)))),
                new MonitorCallback() {
                    @Override
                    public void update(TableUpdates tableUpdates) {
                        LOG.info("Monitor callback received {}", tableUpdates);

                        extIntegrationInterface.wifiVIFStateDbTableUpdate(
                                ovsdbDao.getOpensyncAPVIFState(tableUpdates, key, ovsdbClient), key);

                    }

                });

        extIntegrationInterface.wifiVIFStateDbTableUpdate(ovsdbDao.getOpensyncAPVIFState(vsCf.join(), key, ovsdbClient),
                key);

    }

    @Override
    public String closeSession(String apId) {
        OvsdbSession session = ovsdbSessionMapInterface.getSession(apId);
        try {
            session.getOvsdbClient().shutdown();
        } catch (Exception e) {
            LOG.error("Failed to close session to " + apId + " " + e.getLocalizedMessage());
            return "Failed to close session to " + apId + " " + e.getLocalizedMessage();

        }
        LOG.debug("Closed session to " + apId);
        return "Closed session to " + apId;
    }

    @Override
    public String processFirmwareDownload(String apId, String firmwareUrl, String firmwareVersion, String username,
            String validationCode) {
        try {
            OvsdbSession session = ovsdbSessionMapInterface.getSession(apId);

            ovsdbDao.configureFirmwareDownload(session.getOvsdbClient(), apId, firmwareUrl, firmwareVersion, username,
                    validationCode);
        } catch (Exception e) {
            LOG.error("Failed to initialize firmware download to " + apId + " " + e.getLocalizedMessage());
            return "Failed to initialize firmware download to " + apId + " " + e.getLocalizedMessage();

        }
        LOG.debug("Initialized firmware download to " + apId);
        return "Initialized firmware download to " + apId;
    }

}
