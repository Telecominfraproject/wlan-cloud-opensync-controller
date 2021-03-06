package com.telecominfraproject.wlan.opensync.ovsdb;

import java.security.cert.X509Certificate;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.Monitors;
import com.netflix.servo.tag.TagList;
import com.telecominfraproject.wlan.cloudmetrics.CloudMetricsTags;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.OvsdbDao;
import com.telecominfraproject.wlan.opensync.util.SslUtil;
import com.vmware.ovsdb.callback.ConnectionCallback;
import com.vmware.ovsdb.service.OvsdbClient;
import com.vmware.ovsdb.service.OvsdbPassiveConnectionListener;

import io.netty.handler.ssl.SslContext;

@Profile("ovsdb_redirector")
@Component
public class TipWlanOvsdbRedirector {

    private static final Logger LOG = LoggerFactory.getLogger(TipWlanOvsdbRedirector.class);
    
    private final TagList tags = CloudMetricsTags.commonTags;

    private final Counter connectionsAttempted = new BasicCounter(
            MonitorConfig.builder("osgw-redirector-connectionsAttempted").withTags(tags).build());

    private final Counter connectionsFailed = new BasicCounter(
            MonitorConfig.builder("osgw-redirector-connectionsFailed").withTags(tags).build());

    private final Counter connectionsCreated = new BasicCounter(
            MonitorConfig.builder("osgw-redirector-connectionsCreated").withTags(tags).build());

    private final Counter connectionsDropped = new BasicCounter(
            MonitorConfig.builder("osgw-redirector-connectionsDropped").withTags(tags).build());

    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.redirector.listenPort:6643}")
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
    
    // dtop: use anonymous constructor to ensure that the following code always
    // get executed,
    // even when somebody adds another constructor in here
    {
        DefaultMonitorRegistry.getInstance().register(connectionsAttempted);
        DefaultMonitorRegistry.getInstance().register(connectionsCreated);
        DefaultMonitorRegistry.getInstance().register(connectionsDropped);
        DefaultMonitorRegistry.getInstance().register(connectionsFailed);
    }

    public void listenForConnections() {

        ConnectionCallback connectionCallback = new ConnectionCallback() {
            public void connected(OvsdbClient ovsdbClient) {
                connectionsAttempted.increment();
                String remoteHost = ovsdbClient.getConnectionInfo().getRemoteAddress().getHostAddress();
                int localPort = ovsdbClient.getConnectionInfo().getLocalPort();
                String subjectDn = null;
                try {
                    subjectDn = ((X509Certificate) ovsdbClient.getConnectionInfo().getRemoteCertificate()).getSubjectDN().getName();
                    
                    String clientCn = SslUtil.extractCN(subjectDn);
                    LOG.info("ovsdbClient redirector connected from {} on port {} clientCn {}", remoteHost, localPort, clientCn);                
                    ovsdbDao.performRedirect(ovsdbClient, clientCn);
                    connectionsCreated.increment();
                } catch (Exception e) {
                    connectionsFailed.increment();
                    //something is wrong with the SSL or with the redirect
                    ovsdbClient.shutdown();
                    return;
                }
            }
            
            public void disconnected(OvsdbClient ovsdbClient) {
                connectionsDropped.increment();
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
        
        LOG.info("redirector waiting for connection on port {} ...", ovsdbRedirectorListenPort);

    }

}
