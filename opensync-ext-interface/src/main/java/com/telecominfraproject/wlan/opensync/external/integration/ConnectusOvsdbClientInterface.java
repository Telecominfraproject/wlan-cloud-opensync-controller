package com.telecominfraproject.wlan.opensync.external.integration;

import java.util.Set;

public interface ConnectusOvsdbClientInterface {
    
    static final String[] AWLAN_NODE_DB_TABLE_COLUMNS = new String[] { "device_mode", "id", "model",
            "serial_number", "firmware_version", "platform_version", "redirector_addr" };

    static final String[] WIFI_VIF_STATE_DB_TABLE_COLUMNS = new String[] { "min_hw_mode", "if_name", "security",
            "bridge", "channel", "enabled", "ssid_broadcast", "mac", "ssid" };

    static final String[] WIFI_INET_STATE_DB_TABLE_COLUMNS = new String[] { "hwaddr", "if_type", "if_name",
            "inet_addr", "dhcpc", "network" };

    static final String[] WIFI_RADIO_STATE_DB_TABLE_COLUMNS = new String[] { "ht_mode", "hw_mode", "hw_params",
            "hw_type", "mac", "if_name", "freq_band", "country", "channel" };

    static final String[] WIFI_ASSOCIATED_CLIENTS_DB_TABLE_COLUMNS = new String[] { "mac" };

    
    Set<String> getConnectedClientIds();
    String changeRedirectorAddress(String apId, String newRedirectorAddress);
    void processConfigChanged(String apId);
}
