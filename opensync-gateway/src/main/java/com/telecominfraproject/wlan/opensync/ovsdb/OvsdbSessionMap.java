package com.telecominfraproject.wlan.opensync.ovsdb;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.Monitors;
import com.netflix.servo.monitor.NumberGauge;
import com.netflix.servo.tag.TagList;
import com.telecominfraproject.wlan.cloudmetrics.CloudMetricsTags;
import com.telecominfraproject.wlan.opensync.external.integration.OvsdbSession;
import com.telecominfraproject.wlan.opensync.external.integration.OvsdbSessionMapInterface;
import com.vmware.ovsdb.service.OvsdbClient;

@Component
public class OvsdbSessionMap implements OvsdbSessionMapInterface {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbSessionMap.class);

    private final TagList tags = CloudMetricsTags.commonTags;
    
    private AtomicInteger totalEquipmentConnections = new AtomicInteger(0);
        
    private final NumberGauge totalEquipmentConnectionsGauge = new NumberGauge(
            MonitorConfig.builder("osgw-totalEquipmentConnections").withTags(tags).build(), totalEquipmentConnections);
    
    private final ConcurrentHashMap<String, OvsdbSession> connectedClients = new ConcurrentHashMap<>();

    // dtop: use anonymous constructor to ensure that the following code always
    // get executed,
    // even when somebody adds another constructor in here
    {
        DefaultMonitorRegistry.getInstance().register(totalEquipmentConnectionsGauge);
    }

    @Override
    public OvsdbSession getSession(String apId) {
        LOG.info("Get session for AP {}", apId);
        return connectedClients.get(apId);
    }

    @Override
    public OvsdbSession removeSession(String apId) { 
        LOG.info("Removing session for AP {}", apId);
        OvsdbSession ret = connectedClients.remove(apId);
        if(ret!=null) {
            totalEquipmentConnections.decrementAndGet();
        }
        return ret;
    }

    @Override
    public void closeSession(String apId) {
        try {
            LOG.info("Close session for AP {}", apId);
            connectedClients.get(apId).getOvsdbClient().shutdown();
            removeSession(apId);
            LOG.info("Closed ovsdb session for {}", apId);
        }catch (Exception e) {
            // do nothing
        }
    }

    @Override
    public OvsdbSession newSession(String apId, OvsdbClient ovsdbClient) {
        OvsdbSession ret = new OvsdbSession();
        ret.setApId(apId);
        ret.setOvsdbClient(ovsdbClient);
        OvsdbSession oldSession = connectedClients.put(apId, ret);
        
        if(oldSession!=null) {
            try {
                oldSession.getOvsdbClient().shutdown();
                LOG.info("Closed old ovsdb session for {}", apId);
            }catch (Exception e) {
                // do nothing
            }            
        } else {
            totalEquipmentConnections.incrementAndGet();
        }
        
        LOG.info("Created new ovsdb session for {}", apId);

        return ret;
    }
    
    @Override
    public int getNumSessions() {
        return connectedClients.size();
    }
    
    @Override
    public Set<String> getConnectedClientIds() {
        return new HashSet<String>(connectedClients.keySet());
    }
    
    @Override
    public String lookupClientId(OvsdbClient ovsdbClient) {
        String key = connectedClients.searchEntries(1,
                (Entry<String, OvsdbSession> t) -> { return t.getValue().getOvsdbClient().equals(ovsdbClient) ? t.getKey() : null ;}
        );
        
        return key;
    }
}
