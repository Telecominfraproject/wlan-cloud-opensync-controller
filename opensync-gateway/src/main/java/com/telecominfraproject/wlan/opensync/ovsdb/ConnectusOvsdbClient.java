package com.telecominfraproject.wlan.opensync.ovsdb;

import java.security.cert.X509Certificate;
import java.util.Set;
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
import com.telecominfraproject.wlan.opensync.ovsdb.dao.OvsdbDao;
import com.telecominfraproject.wlan.opensync.util.SslUtil;
import com.vmware.ovsdb.callback.ConnectionCallback;
import com.vmware.ovsdb.callback.MonitorCallback;
import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.protocol.methods.MonitorRequest;
import com.vmware.ovsdb.protocol.methods.MonitorRequests;
import com.vmware.ovsdb.protocol.methods.MonitorSelect;
import com.vmware.ovsdb.protocol.methods.TableUpdates;
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
                    extIntegrationInterface.apDisconnected(key);
                    ConnectusOvsdbClient.this.ovsdbSessionMapInterface.removeSession(key);
                }
                // turn off monitor
                try {
                    ovsdbClient.cancelMonitor(OvsdbDao.wifiRadioStateDbTable);
                    ovsdbClient.cancelMonitor(OvsdbDao.wifiVifStateDbTable);
                    ovsdbClient.cancelMonitor(OvsdbDao.wifiInetStateDbTable);
                    ovsdbClient.cancelMonitor(OvsdbDao.wifiAssociatedClientsDbTable);
                } catch (OvsdbClientException e) {
                    LOG.warn("Could not cancel Monitor {}", e);
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
                .monitor(OvsdbDao.ovsdbName, OvsdbDao.wifiRadioStateDbTable,
                        new MonitorRequests(ImmutableMap.of(OvsdbDao.wifiRadioStateDbTable,
                                new MonitorRequest(new MonitorSelect(true, false, false, true)))),
                        new MonitorCallback() {

                            @Override
                            public void update(TableUpdates tableUpdates) {
                                extIntegrationInterface.wifiRadioStatusDbTableUpdate(tableUpdates, key);

                            }

                        });

        extIntegrationInterface.wifiRadioStatusDbTableUpdate(rsCf.join(), key);

        CompletableFuture<TableUpdates> isCf = ovsdbClient
                .monitor(OvsdbDao.ovsdbName, OvsdbDao.wifiInetStateDbTable,
                        new MonitorRequests(ImmutableMap.of(OvsdbDao.wifiInetStateDbTable,
                                new MonitorRequest(new MonitorSelect(true, false, false, true)))),
                        new MonitorCallback() {

                            @Override
                            public void update(TableUpdates tableUpdates) {
                                extIntegrationInterface.wifiInetStateDbTableUpdate(tableUpdates, key);

                            }

                        });

        extIntegrationInterface.wifiInetStateDbTableUpdate(isCf.join(), key);

        CompletableFuture<TableUpdates> vsCf = ovsdbClient
                .monitor(OvsdbDao.ovsdbName, OvsdbDao.wifiVifStateDbTable,
                        new MonitorRequests(ImmutableMap.of(OvsdbDao.wifiVifStateDbTable,
                                new MonitorRequest(new MonitorSelect(true, false, false, true)))),
                        new MonitorCallback() {
                            @Override
                            public void update(TableUpdates tableUpdates) {

                                extIntegrationInterface.wifiVIFStateDbTableUpdate(tableUpdates, key);

                            }

                        });

        extIntegrationInterface.wifiVIFStateDbTableUpdate(vsCf.join(), key);

        CompletableFuture<TableUpdates> acCf = ovsdbClient.monitor(OvsdbDao.ovsdbName,
                OvsdbDao.wifiAssociatedClientsDbTable,
                new MonitorRequests(ImmutableMap.of(OvsdbDao.wifiAssociatedClientsDbTable, new MonitorRequest())),
                new MonitorCallback() {

                    @Override
                    public void update(TableUpdates tableUpdates) {
                        extIntegrationInterface.wifiAssociatedClientsDbTableUpdate(tableUpdates, key);
                    }

                });
        extIntegrationInterface.wifiAssociatedClientsDbTableUpdate(acCf.join(), key);

        CompletableFuture<TableUpdates> awCf = ovsdbClient.monitor(OvsdbDao.ovsdbName, OvsdbDao.awlanNodeDbTable,
                new MonitorRequests(ImmutableMap.of(OvsdbDao.awlanNodeDbTable, new MonitorRequest())),
                new MonitorCallback() {

                    @Override
                    public void update(TableUpdates tableUpdates) {
                        extIntegrationInterface.awlanNodeDbTableUpdate(tableUpdates, key);
                    }

                });
        extIntegrationInterface.awlanNodeDbTableUpdate(awCf.join(), key);

    }
}
