package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.core.model.equipment.MacAddress;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.equipment.models.ApElementConfiguration;
import com.telecominfraproject.wlan.equipment.models.CellSizeAttributes;
import com.telecominfraproject.wlan.opensync.external.integration.models.ConnectNodeInfo;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPInetState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPRadioState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPVIFState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAWLANNode;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncWifiAssociatedClients;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration;
import com.vmware.ovsdb.protocol.methods.RowUpdate;
import com.vmware.ovsdb.protocol.methods.TableUpdates;
import com.vmware.ovsdb.service.OvsdbClient;

@Component
public class OvsdbDao extends OvsdbDaoBase {

    @Autowired
    OvsdbNode ovsdbNode;
    @Autowired
    OvsdbHotspotConfig ovsdbHotspot;
    @Autowired
    OvsdbSsidConfig ovsdbSsid;
    @Autowired
    OvsdbNetworkConfig ovsdbNetwork;
    @Autowired
    OvsdbRrmConfig ovsdbRrm;
    @Autowired
    OvsdbStatsConfig ovsdbStats;
    @Autowired
    OvsdbRadioConfig ovsdbRadio;
    @Autowired
    OvsdbMonitor ovsdbMonitor;
    @Autowired
    OvsdbFirmwareConfig ovsdbFirmware;
    @Autowired
    OvsdbCommandConfig ovsdbCommand;
    @Autowired
    OvsdbNodeConfig ovsdbNodeConfig;
    @Autowired
    OvsdbRadiusProxyConfig ovsdbRadiusProxyConfig;

    public String changeRedirectorAddress(OvsdbClient ovsdbClient, String apId, String newRedirectorAddress) {
        return ovsdbNode.changeRedirectorAddress(ovsdbClient, apId, newRedirectorAddress);
    }

    public void configureBlockList(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncAPConfig,
            List<MacAddress> blockList) {
        ovsdbSsid.configureBlockList(ovsdbClient, blockList);
    }

    public void configureCommands(OvsdbClient ovsdbClient, String startdebugengineapcommand,
            Map<String, String> payload, long l, long defaultCommandDurationSec) {
        ovsdbCommand.configureCommands(ovsdbClient, startdebugengineapcommand, payload, l, defaultCommandDurationSec);
    }

    public void configureFirmwareDownload(OvsdbClient ovsdbClient, String apId, String firmwareUrl,
            String firmwareVersion, String username) throws Exception {
        ovsdbFirmware.configureFirmwareDownload(ovsdbClient, apId, firmwareUrl, firmwareVersion, username);
    }

    public void configureFirmwareFlash(OvsdbClient ovsdbClient, String apId, String firmwareVersion, String username) {
        ovsdbFirmware.configureFirmwareFlash(ovsdbClient, apId, firmwareVersion, username);
    }

    public void configureGreTunnels(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncAPConfig) {
        ovsdbNetwork.configureGreTunnels(ovsdbClient, opensyncAPConfig);
    }

    public void configureHotspots(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncAPConfig) {
        ovsdbHotspot.configureHotspots(ovsdbClient, opensyncAPConfig);
    }

    public void configureInterfaces(OvsdbClient ovsdbClient) {
        ovsdbNetwork.configureInterfaces(ovsdbClient);
    }
    
    public void configureWiredPort(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncAPConfig) {
    	ovsdbNetwork.configureEthernetPorts(ovsdbClient, opensyncAPConfig);
    }
    
    public void configureNode(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncAPConfig) {
        configureNtpServer(ovsdbClient, opensyncAPConfig);
        configureSyslog(ovsdbClient, opensyncAPConfig);
        processBlinkRequest(ovsdbClient, opensyncAPConfig.getCustomerEquipment().getInventoryId(),
                ((ApElementConfiguration) opensyncAPConfig.getCustomerEquipment().getDetails()).isBlinkAllLEDs());
        ovsdbNodeConfig.processLedControlEnabled(ovsdbClient, opensyncAPConfig);
        processApcConfig(ovsdbClient, opensyncAPConfig);
    }
    
    void configureNtpServer(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncAPConfig) {
        ovsdbNodeConfig.configureNtpServer(ovsdbClient, opensyncAPConfig);
    }
    
    void configureSyslog(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncAPConfig) {
        ovsdbNodeConfig.configureSyslog(ovsdbClient, opensyncAPConfig);
    }

    public void configureRadsecRadiusAndRealm(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncAPConfig) {
        ovsdbRadiusProxyConfig.configureRadius(ovsdbClient, opensyncAPConfig);
    }

    public void configureSsids(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncAPConfig) {
        ovsdbSsid.configureSsids(ovsdbClient, opensyncAPConfig);
    }

    public void configureStatsFromProfile(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncAPConfig) {
        ovsdbStats.configureStatsFromProfile(ovsdbClient, opensyncAPConfig);
    }

    public void configureWifiRadios(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncAPConfig) {
        ovsdbRadio.configureWifiRadios(ovsdbClient, opensyncAPConfig);
    }

    public void configureWifiRrm(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncAPConfig) {
        ovsdbRrm.configureWifiRrm(ovsdbClient, opensyncAPConfig);
    }

    public void createVlanNetworkInterfaces(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncAPConfig) {
        ovsdbNetwork.createVlanNetworkInterfaces(ovsdbClient, opensyncAPConfig);
    }

    public void enableNetworkProbeForSyntheticClient(OvsdbClient ovsdbClient) {
        ovsdbStats.enableNetworkProbeForSyntheticClient(ovsdbClient);
    }

    public ConnectNodeInfo getConnectNodeInfo(OvsdbClient ovsdbClient) {
        return ovsdbNode.getConnectNodeInfo(ovsdbClient);
    }

    public long getDeviceStatsReportingInterval(OvsdbClient ovsdbClient) {
        return ovsdbStats.getDeviceStatsReportingInterval(ovsdbClient);
    }

    public List<OpensyncAPInetState> getInitialOpensyncApInetStateForRowUpdate(TableUpdates join, String key,
            OvsdbClient ovsdbClient) {
        return ovsdbMonitor.getInitialOpensyncApInetStateForRowUpdate(join, key, ovsdbClient);
    }

    public List<OpensyncWifiAssociatedClients> getInitialOpensyncWifiAssociatedClients(TableUpdates join, String key,
            OvsdbClient ovsdbClient) {
        return ovsdbMonitor.getInitialOpensyncWifiAssociatedClients(join, key, ovsdbClient);
    }

    public Collection<? extends OpensyncAPInetState> getOpensyncApInetStateForRowUpdate(RowUpdate rowUpdate, String key,
            OvsdbClient ovsdbClient) {
        return ovsdbMonitor.getOpensyncApInetStateForRowUpdate(rowUpdate, key, ovsdbClient);
    }

    public List<OpensyncAPRadioState> getOpensyncAPRadioState(TableUpdates tableUpdates, String key,
            OvsdbClient ovsdbClient) {
        return ovsdbMonitor.getOpensyncAPRadioState(tableUpdates, key, ovsdbClient);
    }

    public Collection<? extends OpensyncAPVIFState> getOpensyncApVifStateForRowUpdate(RowUpdate rowUpdate, String key,
            OvsdbClient ovsdbClient) {
        return ovsdbMonitor.getOpensyncApVifStateForRowUpdate(rowUpdate, key, ovsdbClient);
    }

    public OpensyncAWLANNode getOpensyncAWLANNode(TableUpdates tableUpdates, String key, OvsdbClient ovsdbClient) {
        return ovsdbMonitor.getOpensyncAWLANNode(tableUpdates, key, ovsdbClient);
    }

    public Collection<? extends OpensyncWifiAssociatedClients> getOpensyncWifiAssociatedClients(RowUpdate rowUpdate,
            String key, OvsdbClient ovsdbClient) {
        return ovsdbMonitor.getOpensyncWifiAssociatedClients(rowUpdate, key, ovsdbClient);
    }
    
    public Map<String,String> getAPCState(RowUpdate rowUpdate, String key) {
        return ovsdbMonitor.getAPCState(rowUpdate, key);
    }

    public void getRadiusAccountingConfiguration(OpensyncAPConfig apConfig, SsidConfiguration ssidConfig,
            Map<String, String> security) {
        ovsdbSsid.getRadiusAccountingConfiguration(apConfig, ssidConfig, security);
    }

    public void getRadiusConfiguration(OpensyncAPConfig apConfig, SsidConfiguration ssidConfig,
            Map<String, String> security) {
        ovsdbSsid.getRadiusConfiguration(apConfig, ssidConfig, security);
    }

    public void performRedirect(OvsdbClient ovsdbClient, String clientCn) {
        ovsdbNode.performRedirect(ovsdbClient, clientCn);
    }

    public void processNewChannelsRequest(OvsdbClient ovsdbClient, Map<RadioType, Integer> backupChannelMap,
            Map<RadioType, Integer> primaryChannelMap) {
        ovsdbRrm.processNewChannelsRequest(ovsdbClient, backupChannelMap, primaryChannelMap);
    }
    
    public void processCellSizeAttributesRequest(OvsdbClient ovsdbClient, Map<RadioType, CellSizeAttributes> cellSizeAttributeMap) {
        ovsdbRrm.processCellSizeAttributesRequest(ovsdbClient, cellSizeAttributeMap);
    }

    public void rebootOrResetAp(OvsdbClient ovsdbClient, String ovsdbAwlanApSwitchSoftwareBank) {
        ovsdbNode.rebootOrResetAp(ovsdbClient, ovsdbAwlanApSwitchSoftwareBank);
    }

    public void removeAllInetConfigs(OvsdbClient ovsdbClient) {
        ovsdbNetwork.removeAllInetConfigs(ovsdbClient);
    }
    
    public void resetWiredPorts(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig) {
    	ovsdbNetwork.resetWiredPorts(ovsdbClient, opensyncApConfig);
    }

    public void removeAllPasspointConfigs(OvsdbClient ovsdbClient) {
        ovsdbHotspot.removeAllPasspointConfigs(ovsdbClient);
    }

    public void removeAllSsids(OvsdbClient ovsdbClient) {
        ovsdbSsid.removeAllSsids(ovsdbClient);
    }

    public void removeAllStatsConfigs(OvsdbClient ovsdbClient) {
        ovsdbStats.removeAllStatsConfigs(ovsdbClient);
    }
    
    public void removeRadsecRadiusAndRealmConfigs(OvsdbClient ovsdbClient) {
        ovsdbRadiusProxyConfig.removeRadiusConfigurations(ovsdbClient);
    }

    public void removeWifiRrm(OvsdbClient ovsdbClient) {
        ovsdbRrm.removeWifiRrm(ovsdbClient);
    }

    public ConnectNodeInfo updateConnectNodeInfoOnConnect(OvsdbClient ovsdbClient, String clientCn,
            ConnectNodeInfo connectNodeInfo, boolean preventClientCnAlteration) {
        return ovsdbNode.updateConnectNodeInfoOnConnect(ovsdbClient, clientCn, connectNodeInfo,
                preventClientCnAlteration);
    }

    public void updateDeviceStatsReportingInterval(OvsdbClient ovsdbClient, long collectionIntervalSecDeviceStats) {
        ovsdbStats.updateDeviceStatsReportingInterval(ovsdbClient, collectionIntervalSecDeviceStats);
    }

    @Override
    public void updateEventReportingInterval(OvsdbClient ovsdbClient, long collectionIntervalSecEvent) {
        ovsdbStats.updateEventReportingInterval(ovsdbClient, collectionIntervalSecEvent);
    }

    public String processBlinkRequest(OvsdbClient ovsdbClient, String apId, boolean blinkAllLEDs) {
        return ovsdbNodeConfig.processBlinkRequest(ovsdbClient, apId, blinkAllLEDs);
    }
    
    public void processApcConfig(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncAPConfig) {
        ovsdbNodeConfig.configureDynamicRadiusProxyToAPC(ovsdbClient, opensyncAPConfig);
    }

}
