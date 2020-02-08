package ai.connectus.opensync.ovsdb;

import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.vmware.ovsdb.callback.ConnectionCallback;
import com.vmware.ovsdb.service.OvsdbClient;
import com.vmware.ovsdb.service.OvsdbPassiveConnectionListener;

import ai.connectus.opensync.external.integration.OpensyncExternalIntegrationInterface;
import ai.connectus.opensync.external.integration.models.OpensyncAPConfig;
import ai.connectus.opensync.ovsdb.dao.OvsdbDao;
import ai.connectus.opensync.ovsdb.dao.models.ConnectNodeInfo;
import ai.connectus.opensync.util.SslUtil;
import io.netty.handler.ssl.SslContext;

@Profile("ovsdb_manager")
@Component
public class ConnectusOvsdbClient {

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
    
    private final ConcurrentHashMap<String, OvsdbClient> connectedClients = new ConcurrentHashMap<>();
    
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
                    subjectDn = ((X509Certificate) ovsdbClient.getConnectionInfo().getRemoteCertificate()).getSubjectDN().getName();

                    String clientCn = SslUtil.extractCN(subjectDn);
                    LOG.info("ovsdbClient connecting from {} on port {} clientCn {}", remoteHost, localPort, clientCn);                
                    
                    ConnectNodeInfo connectNodeInfo = processConnectRequest(ovsdbClient, clientCn);
                    
                    //successfully connected - register it in our connectedClients table
                    //In Plume's environment clientCn is not unique that's why we are augmenting it with the serialNumber and using it as a key (equivalent of KDC unique qrCode)
                    String key = clientCn + "_" + connectNodeInfo.serialNumber;
                    ConnectusOvsdbClient.this.connectedClients.put(key, ovsdbClient);
                    extIntegrationInterface.apConnected(key);
                    
                    LOG.info("ovsdbClient connected from {} on port {} key {} ", remoteHost, localPort, key);

                    LOG.info("ovsdbClient connectedClients = {}", ConnectusOvsdbClient.this.connectedClients.size());

                } catch (Exception e) {
                    LOG.error("ovsdbClient error", e);
                    //something is wrong with the SSL 
                    ovsdbClient.shutdown();
                    return;
                }
                
            }
            public void disconnected(OvsdbClient ovsdbClient) {
                String remoteHost = ovsdbClient.getConnectionInfo().getRemoteAddress().getHostAddress();
                int localPort = ovsdbClient.getConnectionInfo().getLocalPort();
                String subjectDn = null;
                try {
                    subjectDn = ((X509Certificate) ovsdbClient.getConnectionInfo().getRemoteCertificate()).getSubjectDN().getName();
                } catch (Exception e) {
                    //do nothing
                }
                
                String clientCn = SslUtil.extractCN(subjectDn);
                
                //disconnected - deregister ovsdbClient from our connectedClients table
                //unfortunately we only know clientCn at this point, but in Plume's environment they are not unique
                //so we are doing a reverse lookup here, and then if we find the key we will remove the entry from the connectedClients.
                String key = ConnectusOvsdbClient.this.connectedClients.searchEntries(1,
                        (Entry<String, OvsdbClient> t) -> { return t.getValue().equals(ovsdbClient) ? t.getKey() : null ;}
                );
                
                if(key!=null) {
                    ConnectusOvsdbClient.this.connectedClients.remove(key);
                    extIntegrationInterface.apDisconnected(key);
                }
                
                ovsdbClient.shutdown();
                
                LOG.info("ovsdbClient disconnected from {} on port {} clientCn {} key {} ", remoteHost, localPort, clientCn, key);
                LOG.info("ovsdbClient connectedClients = {}", ConnectusOvsdbClient.this.connectedClients.size());
            }
        };

        listener.startListeningWithSsl(ovsdbListenPort, sslContext, connectionCallback).join();

        LOG.debug("manager waiting for connection on port {}...", ovsdbListenPort);
    }
    
    private ConnectNodeInfo processConnectRequest(OvsdbClient ovsdbClient, String clientCn) {
        
        LOG.debug("Starting Client connect");
        ConnectNodeInfo connectNodeInfo = ovsdbDao.getConnectNodeInfo(ovsdbClient);
        connectNodeInfo = ovsdbDao.updateConnectNodeInfoOnConnect(ovsdbClient, clientCn, connectNodeInfo);

        String apId = clientCn + "_" + connectNodeInfo.serialNumber;
        OpensyncAPConfig opensyncAPConfig = extIntegrationInterface.getApConfig(apId);
        
        ovsdbDao.configureStats(ovsdbClient);
        
        //Check if device stats is configured in Wifi_Stats_Config table, provision it if needed
        if(ovsdbDao.getDeviceStatsReportingInterval(ovsdbClient) != collectionIntervalSecDeviceStats) {
            ovsdbDao.updateDeviceStatsReportingInterval(ovsdbClient, collectionIntervalSecDeviceStats);
        }            
        
        ovsdbDao.removeOnboardingSsids(ovsdbClient);
        
        if(opensyncAPConfig!=null) {
            ovsdbDao.configureWifiRadios(ovsdbClient, opensyncAPConfig.getRadioConfig());
            ovsdbDao.configureSsids(ovsdbClient, opensyncAPConfig.getSsidConfigs());
        }
        
        ovsdbDao.provisionBridgePortInterface(ovsdbClient);

        ovsdbDao.configureWifiInet(ovsdbClient);
        
        LOG.debug("Client connect Done");
        return connectNodeInfo;
    }
    
    public Set<String> getConnectedClientIds(){
        return new HashSet<>(connectedClients.keySet());
    }

}
