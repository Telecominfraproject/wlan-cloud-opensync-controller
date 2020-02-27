package ai.connectus.opensync.ovsdb;

import java.security.cert.X509Certificate;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.vmware.ovsdb.callback.ConnectionCallback;
import com.vmware.ovsdb.service.OvsdbClient;
import com.vmware.ovsdb.service.OvsdbPassiveConnectionListener;

import ai.connectus.opensync.ovsdb.dao.OvsdbDao;
import ai.connectus.opensync.util.SslUtil;
import io.netty.handler.ssl.SslContext;

@Profile("ovsdb_redirector")
@Component
public class ConnectusOvsdbRedirector {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectusOvsdbRedirector.class);
    
    @org.springframework.beans.factory.annotation.Value("${connectus.ovsdb.redirector.listenPort:6643}")
    private int ovsdbRedirectorListenPort;

    @Autowired
    private SslContext sslContext;
    
    @Autowired
    private OvsdbDao ovsdbDao;
    
    @Autowired
    private OvsdbPassiveConnectionListener listener;
    
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
                    LOG.info("ovsdbClient redirector connected from {} on port {} clientCn {}", remoteHost, localPort, clientCn);                
                    ovsdbDao.performRedirect(ovsdbClient, clientCn);

                } catch (Exception e) {
                    //something is wrong with the SSL or with the redirect
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
                LOG.info("ovsdbClient redirector disconnected from {} on port {} clientCn {}", remoteHost, localPort, clientCn);
                ovsdbClient.shutdown();
            }
        };

        listener.startListeningWithSsl(ovsdbRedirectorListenPort, sslContext, connectionCallback).join();
        
        LOG.debug("redirector waiting for connection on port {} ...", ovsdbRedirectorListenPort);

    }

}
