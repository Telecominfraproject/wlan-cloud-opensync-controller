package ai.connectus.opensync.external.integration;

import java.util.Set;

import com.vmware.ovsdb.service.OvsdbClient;

public interface OvsdbSessionMapInterface {
    OvsdbSession getSession(String apId);
    OvsdbSession removeSession(String apId);
    void closeSession(String apId);
    OvsdbSession newSession(String apId, OvsdbClient ovsdbClient);
    int getNumSessions();
    Set<String> getConnectedClientIds();
    String lookupClientId(OvsdbClient ovsdbClient);
}
