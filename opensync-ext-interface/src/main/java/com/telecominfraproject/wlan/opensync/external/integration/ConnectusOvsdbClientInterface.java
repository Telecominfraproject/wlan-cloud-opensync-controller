package com.telecominfraproject.wlan.opensync.external.integration;

import java.util.Set;

public interface ConnectusOvsdbClientInterface {
    Set<String> getConnectedClientIds();
    String changeRedirectorAddress(String apId, String newRedirectorAddress);
    void processConfigChanged(String apId);
}
