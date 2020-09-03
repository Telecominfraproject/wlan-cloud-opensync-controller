package com.telecominfraproject.wlan.opensync.external.integration;

import java.util.List;
import java.util.Set;

import com.telecominfraproject.wlan.core.model.equipment.MacAddress;

public interface OvsdbClientInterface {

    Set<String> getConnectedClientIds();

    String changeRedirectorHost(String apId, String newRedirectorHost);
    
    String startDebugEngine(String apId, String gatewayHostname, Integer gatewayPort);
    
    String stopDebugEngine(String apId);

    void processConfigChanged(String apId);

    void processClientBlocklistChange(String apId, List<MacAddress> blockList);

    String processFirmwareDownload(String apId, String firmwareUrl, String firmwareVersion, String username,
            String validationCode);

    String closeSession(String apId);

    String processFirmwareFlash(String apId, String firmwareVersion, String username);
}
