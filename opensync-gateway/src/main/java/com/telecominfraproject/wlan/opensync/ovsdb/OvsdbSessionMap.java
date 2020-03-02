package com.telecominfraproject.wlan.opensync.ovsdb;

import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.opensync.external.integration.OvsdbSession;
import com.telecominfraproject.wlan.opensync.external.integration.OvsdbSessionMapInterface;
import com.vmware.ovsdb.service.OvsdbClient;

@Component
public class OvsdbSessionMap implements OvsdbSessionMapInterface {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbSessionMap.class);
    
    private final ConcurrentHashMap<String, OvsdbSession> connectedClients = new ConcurrentHashMap<>();

    @Override
    public OvsdbSession getSession(String apId) {        
        return connectedClients.get(apId);
    }

    @Override
    public OvsdbSession removeSession(String apId) {        
        return connectedClients.remove(apId);
    }

    @Override
    public void closeSession(String apId) {
        try {
            connectedClients.get(apId).getOvsdbClient().shutdown();
            connectedClients.remove(apId);
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
