package com.telecominfraproject.wlan.opensync.ovsdb;

import java.security.cert.X509Certificate;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.opensync.external.integration.ConnectusOvsdbClientInterface;
import com.telecominfraproject.wlan.opensync.external.integration.OpensyncExternalIntegrationInterface;
import com.telecominfraproject.wlan.opensync.external.integration.OvsdbSession;
import com.telecominfraproject.wlan.opensync.external.integration.OvsdbSessionMapInterface;
import com.telecominfraproject.wlan.opensync.external.integration.models.ConnectNodeInfo;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.OvsdbDao;
import com.telecominfraproject.wlan.opensync.util.SslUtil;
import com.vmware.ovsdb.callback.ConnectionCallback;
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

					// successfully connected - register it in our connectedClients table
					// In Plume's environment clientCn is not unique that's why we are augmenting it
					// with the serialNumber and using it as a key (equivalent of KDC unique qrCode)
					String key = clientCn + "_" + connectNodeInfo.serialNumber;
					ConnectusOvsdbClient.this.ovsdbSessionMapInterface.newSession(key, ovsdbClient);
					extIntegrationInterface.apConnected(key, connectNodeInfo);

					// push configuration to AP
					connectNodeInfo = processConnectRequest(ovsdbClient, clientCn, connectNodeInfo);
					LOG.info("ovsdbClient connected from {} on port {} key {} ", remoteHost, localPort, key);
					LOG.info("ovsdbClient connectedClients = {}",
							ConnectusOvsdbClient.this.ovsdbSessionMapInterface.getNumSessions());

					// monitor radio config state
					ovsdbDao.monitorRadioConfigState(ovsdbClient, new ConnectusMonitorCallback(ovsdbClient, key));
					// monitor inet state
					ovsdbDao.monitorInetState(ovsdbClient, new ConnectusMonitorCallback(ovsdbClient, key));
					// monitor vif state
					ovsdbDao.monitorVIFState(ovsdbClient, new ConnectusMonitorCallback(ovsdbClient, key));
					// monitor Route state
					ovsdbDao.monitorRouteState(ovsdbClient, new ConnectusMonitorCallback(ovsdbClient, key));
					// monitor Master State
					ovsdbDao.monitorMasterState(ovsdbClient, new ConnectusMonitorCallback(ovsdbClient, key));
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

				// disconnected - deregister ovsdbClient from our connectedClients table
				// unfortunately we only know clientCn at this point, but in Plume's environment
				// they are not unique
				// so we are doing a reverse lookup here, and then if we find the key we will
				// remove the entry from the connectedClients.
				String key = ConnectusOvsdbClient.this.ovsdbSessionMapInterface.lookupClientId(ovsdbClient);

				if (key != null) {
					extIntegrationInterface.apDisconnected(key);
					ConnectusOvsdbClient.this.ovsdbSessionMapInterface.removeSession(key);
				}
				// turn off monitor
				ovsdbDao.cancelMonitors(ovsdbClient);
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

		// Check if device stats is configured in Wifi_Stats_Config table, provision it
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
}
