package com.telecominfraproject.wlan.opensync.external.integration;

import java.util.Set;

public interface OvsdbClientInterface {

    Set<String> getConnectedClientIds();

    String changeRedirectorAddress(String apId, String newRedirectorAddress);

    void processConfigChanged(String apId);

    String processFirmwareDownload(String apId, String firmwareUrl, String firmwareVersion, String username,
            String validationCode);
  
    String closeSession(String apId);
}
