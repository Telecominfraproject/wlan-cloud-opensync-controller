package com.telecominfraproject.wlan.opensync.external.integration;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.telecominfraproject.wlan.core.model.equipment.LedStatus;
import com.telecominfraproject.wlan.core.model.equipment.MacAddress;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.equipment.models.CellSizeAttributes;

public interface OvsdbClientInterface {

    Set<String> getConnectedClientIds();

    String changeRedirectorHost(String apId, String newRedirectorHost);

    String startDebugEngine(String apId, String gatewayHostname, Integer gatewayPort);

    String stopDebugEngine(String apId);
    
    String processBlinkRequest(String apId, boolean blinkAllLEDs);

    void processConfigChanged(String apId);

    void processClientBlocklistChange(String apId, List<MacAddress> blockList);

    String processFirmwareDownload(String apId, String firmwareUrl, String firmwareVersion, String username);

    String closeSession(String apId);

    String processFirmwareFlash(String apId, String firmwareVersion, String username);

    String processRebootRequest(String apId, boolean switchBanks);
    
    String processFactoryResetRequest(String apId);
    
    String processNewChannelsRequest(String apId, Map<RadioType,Integer> backupChannelMap, Map<RadioType,Integer> primaryChannelMap);

    String processCellSizeAttributesRequest(String apId, Map<RadioType, CellSizeAttributes> cellSizeAttributeMap);

}
