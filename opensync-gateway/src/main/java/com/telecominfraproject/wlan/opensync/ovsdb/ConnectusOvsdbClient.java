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

    @org.springframework.beans.factory.annotation.Value("${connectus.ovsdb.listenPort:6640}")
    private int ovsdbListenPort;

    @org.springframework.beans.factory.annotation.Value("${connectus.manager.collectionIntervalSec.deviceStats:10}")
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
                    ConnectusOvsdbClient.this.ovsdbSessionMapInterface.newSession(key, ovsdbClient);
                    extIntegrationInterface.apConnected(key, connectNodeInfo);

                    // push configuration to AP
                    connectNodeInfo = processConnectRequest(ovsdbClient, clientCn, connectNodeInfo);

                    LOG.info("ovsdbClient connected from {} on port {} key {} ", remoteHost, localPort, key);
                    LOG.info("ovsdbClient connectedClients = {}",
                            ConnectusOvsdbClient.this.ovsdbSessionMapInterface.getNumSessions());

                    monitorOvsdbStateTables(ovsdbClient, key);

                } catch (Exception e) {
                    LOG.error("ovsdbClient error", e);
                    // something is wrong with the SSL
                    ovsdbClient.shutdown();
                    return;
                }

            }

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
                String key = ConnectusOvsdbClient.this.ovsdbSessionMapInterface.lookupClientId(ovsdbClient);

                if (key != null) {
                    
                    // turn off monitor
                    try {
                        ovsdbClient.cancelMonitor(OvsdbDao.wifiRadioStateDbTable + "_" + key);
                        ovsdbClient.cancelMonitor(OvsdbDao.wifiVifStateDbTable + "_" + key);
                        ovsdbClient.cancelMonitor(OvsdbDao.wifiInetStateDbTable + "_" + key);
                        ovsdbClient.cancelMonitor(OvsdbDao.wifiAssociatedClientsDbTable + "_" + key);
                        ovsdbClient.cancelMonitor(OvsdbDao.awlanNodeDbTable + "_" + key);

                    } catch (OvsdbClientException e) {
                        LOG.warn("Could not cancel Monitor {}", e);
                    }
                    
                    extIntegrationInterface.apDisconnected(key);
                    ConnectusOvsdbClient.this.ovsdbSessionMapInterface.removeSession(key);
                }

                ovsdbClient.shutdown();

                LOG.info("ovsdbClient disconnected from {} on port {} clientCn {} key {} ", remoteHost, localPort,
                        clientCn, key);
                LOG.info("ovsdbClient connectedClients = {}",
                        ConnectusOvsdbClient.this.ovsdbSessionMapInterface.getNumSessions());
            }

        };

        listener.startListeningWithSsl(ovsdbListenPort, sslContext, connectionCallback).join();

        LOG.debug("manager waiting for connection on port {}...", ovsdbListenPort);
    }

    private ConnectNodeInfo processConnectRequest(OvsdbClient ovsdbClient, String clientCn,
            ConnectNodeInfo connectNodeInfo) {

        LOG.debug("Starting Client connect");
        connectNodeInfo = ovsdbDao.updateConnectNodeInfoOnConnect(ovsdbClient, clientCn, connectNodeInfo);

        String apId = clientCn + "_" + connectNodeInfo.serialNumber;
        OpensyncAPConfig opensyncAPConfig = extIntegrationInterface.getApConfig(apId);

        ovsdbDao.configureStats(ovsdbClient);

        // Check if device stats is configured in Wifi_Stats_Config table,
        // provision it
        // if needed
        if (ovsdbDao.getDeviceStatsReportingInterval(ovsdbClient) != collectionIntervalSecDeviceStats) {
            ovsdbDao.updateDeviceStatsReportingInterval(ovsdbClient, collectionIntervalSecDeviceStats);
        }

        ovsdbDao.provisionBridgePortInterface(ovsdbClient);

        ovsdbDao.removeAllSsids(ovsdbClient);

        if (opensyncAPConfig != null) {
            ovsdbDao.configureWifiRadios(ovsdbClient, opensyncAPConfig.getRadioConfig());
            ovsdbDao.configureSsids(ovsdbClient, opensyncAPConfig.getSsidConfigs());
        }

        ovsdbDao.configureWifiInet(ovsdbClient);

        LOG.debug("Client connect Done");
        return connectNodeInfo;
    }

    public Set<String> getConnectedClientIds() {
        return ovsdbSessionMapInterface.getConnectedClientIds();
    }

    /**
     * @param apId
     * @param newRedirectorAddress
     * @return updated value of the redirector
     */
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
            ovsdbDao.removeAllSsids(ovsdbClient);
            ovsdbDao.configureWifiRadios(ovsdbClient, opensyncAPConfig.getRadioConfig());
            ovsdbDao.configureSsids(ovsdbClient, opensyncAPConfig.getSsidConfigs());
        }

        LOG.debug("Finished processConfigChanged for {}", apId);
    }

    private void monitorOvsdbStateTables(OvsdbClient ovsdbClient, String key) throws OvsdbClientException {
        CompletableFuture<TableUpdates> rsCf = ovsdbClient
                .monitor(OvsdbDao.ovsdbName, OvsdbDao.wifiRadioStateDbTable + "_" + key,
                        new MonitorRequests(ImmutableMap.of(OvsdbDao.wifiRadioStateDbTable,
                                new MonitorRequest(new MonitorSelect(true, true, false, true)))),
                        new MonitorCallback() {

                            @Override
                            public void update(TableUpdates tableUpdates) {
                                extIntegrationInterface.wifiRadioStatusDbTableUpdate(
                                        ovsdbDao.getOpensyncAPRadioState(tableUpdates, key, ovsdbClient), key);
                            }

                        });

        extIntegrationInterface
                .wifiRadioStatusDbTableUpdate(ovsdbDao.getOpensyncAPRadioState(rsCf.join(), key, ovsdbClient), key);

        CompletableFuture<TableUpdates> isCf = ovsdbClient
                .monitor(OvsdbDao.ovsdbName, OvsdbDao.wifiInetStateDbTable + "_" + key,
                        new MonitorRequests(ImmutableMap.of(OvsdbDao.wifiInetStateDbTable,
                                new MonitorRequest(new MonitorSelect(true, true, false, true)))),
                        new MonitorCallback() {

                            @Override
                            public void update(TableUpdates tableUpdates) {

                                extIntegrationInterface.wifiInetStateDbTableUpdate(
                                        ovsdbDao.getOpensyncAPInetState(tableUpdates, key, ovsdbClient), key);

                            }

                        });

        extIntegrationInterface
                .wifiInetStateDbTableUpdate(ovsdbDao.getOpensyncAPInetState(isCf.join(), key, ovsdbClient), key);

        CompletableFuture<TableUpdates> vsCf = ovsdbClient.monitor(OvsdbDao.ovsdbName,
                OvsdbDao.wifiVifStateDbTable + "_" + key,
                new MonitorRequests(ImmutableMap.of(OvsdbDao.wifiVifStateDbTable,
                        new MonitorRequest(new MonitorSelect(true, true, true, true)))),
                new MonitorCallback() {
                    @Override
                    public void update(TableUpdates tableUpdates) {
                        // extIntegrationInterface.wifiVIFStateDbTableUpdate(
                        // ovsdbDao.getOpensyncAPVIFState(tableUpdates,
                        // key, ovsdbClient), key);
                        List<OpensyncAPVIFState> vifsToDelete = new ArrayList<OpensyncAPVIFState>();
                        for (Entry<String, TableUpdate> tableUpdate : tableUpdates.getTableUpdates().entrySet()) {

                            for (Entry<UUID, RowUpdate> rowUpdate : tableUpdate.getValue().getRowUpdates().entrySet()) {
                                if (rowUpdate.getValue().getOld() != null && rowUpdate.getValue().getNew() == null) {
                                    Row row = rowUpdate.getValue().getOld();
                                    String ifName = row.getStringColumn("if_name");
                                    String ssid = row.getStringColumn("ssid");
                                    if (row.getColumns().get("ssid") != null && row.getColumns().get("ssid").getClass()
                                            .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                                        ssid = row.getStringColumn("ssid");
                                    } 
                                    if (row.getColumns().get("if_name") != null && row.getColumns().get("if_name").getClass()
                                            .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
                                        ifName = row.getStringColumn("if_name");
                                    } 
                                    if (ifName != null && ssid != null) {
                                        OpensyncAPVIFState toBeDeleted = new OpensyncAPVIFState();
                                        toBeDeleted.setSsid(ssid);
                                        toBeDeleted.setIfName(ifName);
                                        vifsToDelete.add(toBeDeleted);
                                    }
                                    tableUpdate.getValue().getRowUpdates().remove(rowUpdate.getKey());
                                }

                            }

                            if (tableUpdate.getValue().getRowUpdates().values().isEmpty()) {
                                tableUpdates.getTableUpdates().remove(tableUpdate.getKey());
                            }

                        }

                        if (!vifsToDelete.isEmpty()) {
                            extIntegrationInterface.wifiVIFStateDbTableDelete(vifsToDelete, key);
                        }
                        if (tableUpdates.getTableUpdates().entrySet().isEmpty())
                            extIntegrationInterface.wifiVIFStateDbTableUpdate(
                                    ovsdbDao.getOpensyncAPVIFState(tableUpdates, key, ovsdbClient), key);

                    }

                });

        extIntegrationInterface.wifiVIFStateDbTableUpdate(ovsdbDao.getOpensyncAPVIFState(vsCf.join(), key, ovsdbClient),
                key);

        CompletableFuture<TableUpdates> acCf = ovsdbClient.monitor(OvsdbDao.ovsdbName,
                OvsdbDao.wifiAssociatedClientsDbTable + "_" + key,
                new MonitorRequests(ImmutableMap.of(OvsdbDao.wifiAssociatedClientsDbTable,
                        new MonitorRequest(new MonitorSelect(true, true, true, true)))),
                new MonitorCallback() {

                    @Override
                    public void update(TableUpdates tableUpdates) {

                        boolean insertOrModify = false;
                        for (TableUpdate tableUpdate : tableUpdates.getTableUpdates().values()) {

                            for (RowUpdate rowUpdate : tableUpdate.getRowUpdates().values()) {
                                if (rowUpdate.getOld() != null && rowUpdate.getNew() == null) {
                                    insertOrModify = false;
                                    Row row = rowUpdate.getOld();
                                    String deletedClientMac = row.getStringColumn("mac");
                                    extIntegrationInterface.wifiAssociatedClientsDbTableDelete(deletedClientMac, key);
                                }
                            }

                        }

                        if (insertOrModify) {
                            extIntegrationInterface.wifiAssociatedClientsDbTableUpdate(
                                    ovsdbDao.getOpensyncWifiAssociatedClients(tableUpdates, key, ovsdbClient), key);
                        }
                    }

                });
        extIntegrationInterface.wifiAssociatedClientsDbTableUpdate(
                ovsdbDao.getOpensyncWifiAssociatedClients(acCf.join(), key, ovsdbClient), key);

        CompletableFuture<TableUpdates> awCf = ovsdbClient
                .monitor(OvsdbDao.ovsdbName, OvsdbDao.awlanNodeDbTable + "_" + key,
                        new MonitorRequests(ImmutableMap.of(OvsdbDao.awlanNodeDbTable,
                                new MonitorRequest(new MonitorSelect(true, false, false, true)))),
                        new MonitorCallback() {

                            @Override
                            public void update(TableUpdates tableUpdates) {
                                extIntegrationInterface.awlanNodeDbTableUpdate(
                                        ovsdbDao.getOpensyncAWLANNode(tableUpdates, key, ovsdbClient), key);
                            }

                        });
        extIntegrationInterface.awlanNodeDbTableUpdate(ovsdbDao.getOpensyncAWLANNode(awCf.join(), key, ovsdbClient),
                key);

    }
}
