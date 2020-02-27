package ai.connectus.opensync.ovsdb.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.protocol.operation.Delete;
import com.vmware.ovsdb.protocol.operation.Insert;
import com.vmware.ovsdb.protocol.operation.Operation;
import com.vmware.ovsdb.protocol.operation.Select;
import com.vmware.ovsdb.protocol.operation.Update;
import com.vmware.ovsdb.protocol.operation.notation.Atom;
import com.vmware.ovsdb.protocol.operation.notation.Condition;
import com.vmware.ovsdb.protocol.operation.notation.Function;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;
import com.vmware.ovsdb.protocol.operation.notation.Value;
import com.vmware.ovsdb.protocol.operation.result.InsertResult;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.protocol.operation.result.SelectResult;
import com.vmware.ovsdb.service.OvsdbClient;
import com.whizcontrol.core.model.equipment.RadioType;

import ai.connectus.opensync.external.integration.models.ConnectNodeInfo;
import ai.connectus.opensync.external.integration.models.OpensyncAPRadioConfig;
import ai.connectus.opensync.external.integration.models.OpensyncAPSsidConfig;
import ai.connectus.opensync.ovsdb.dao.models.BridgeInfo;
import ai.connectus.opensync.ovsdb.dao.models.InterfaceInfo;
import ai.connectus.opensync.ovsdb.dao.models.PortInfo;
import ai.connectus.opensync.ovsdb.dao.models.WifiInetConfigInfo;
import ai.connectus.opensync.ovsdb.dao.models.WifiRadioConfigInfo;
import ai.connectus.opensync.ovsdb.dao.models.WifiStatsConfigInfo;
import ai.connectus.opensync.ovsdb.dao.models.WifiVifConfigInfo;

@Component
public class OvsdbDao {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbDao.class);
    
    @org.springframework.beans.factory.annotation.Value("${connectus.ovsdb.managerAddr:3.88.149.10}")
    private String managerIpAddr;

    @org.springframework.beans.factory.annotation.Value("${connectus.ovsdb.listenPort:6640}")
    private int ovsdbListenPort;

    @org.springframework.beans.factory.annotation.Value("${connectus.mqttBroker.address:testportal.123wlan.com}")
    private String mqttBrokerAddress;
    
    @org.springframework.beans.factory.annotation.Value("${connectus.mqttBroker.listenPort:1883}")
    private int mqttBrokerListenPort;

    @org.springframework.beans.factory.annotation.Value("${connectus.ovsdb.timeoutSec:30}")
    private int ovsdbTimeoutSec;

    public static final String ovsdbName = "Open_vSwitch";
    public static final String awlanNodeDbTable = "AWLAN_Node";
    public static final String wifiStatsConfigDbTable = "Wifi_Stats_Config";
    
    public static final String interfaceDbTable = "Interface";
    public static final String portDbTable = "Port";
    public static final String bridgeDbTable = "Bridge";
    public static final String wifiRadioConfigDbTable = "Wifi_Radio_Config";
    public static final String wifiVifConfigDbTable = "Wifi_VIF_Config";
    public static final String wifiInetConfigDbTable = "Wifi_Inet_Config";
    public static final String wifiInetStateDbTable = "Wifi_Inet_State";
    
    

    //
    //Note: When talking to OVSDB always use future.get(X, TimeUnit.SECONDS); - to prevent DOS attacks with misbehaving clients
    //

    public ConnectNodeInfo getConnectNodeInfo(OvsdbClient ovsdbClient) {
        ConnectNodeInfo ret = new ConnectNodeInfo();
        
        try {
            List<Operation> operations = new ArrayList<>();
            List<Condition> conditions = new ArrayList<>();        
            List<String> columns = new ArrayList<>(); 
            columns.add("mqtt_settings");
            columns.add("redirector_addr");
            columns.add("manager_addr");
            columns.add("sku_number");
            columns.add("serial_number");
            columns.add("model");
            columns.add("firmware_version");
            columns.add("platform_version");
            
            
            operations.add(new Select(awlanNodeDbTable, conditions , columns ));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Select from {}:", awlanNodeDbTable);
                
                for(OperationResult res : result) {
                    LOG.debug("Op Result {}", res);
                }
            }
    
            Row row = null;
            if (result != null && result.length > 0 && !((SelectResult) result[0]).getRows().isEmpty()) {
                row = ((SelectResult)result[0]).getRows().iterator().next();
            }
            
            ret.mqttSettings = (row!=null)?row.getMapColumn("mqtt_settings"):null;
            ret.redirectorAddr = (row!=null)?row.getStringColumn("redirector_addr"):null;
            ret.managerAddr = (row!=null)?row.getStringColumn("manager_addr"):null;
            
            ret.platformVersion = (row!=null)?row.getStringColumn("platform_version"):null;
            ret.firmwareVersion = (row!=null)?row.getStringColumn("firmware_version"):null;
            
            ret.skuNumber = getSingleValueFromSet(row, "sku_number");
            ret.serialNumber = getSingleValueFromSet(row, "serial_number");            
            ret.model = getSingleValueFromSet(row, "model");
            
            //now populate macAddress, ipV4Address from Wifi_Inet_State
            //first look them up for if_name = br-wan 
            fillInIpAddressAndMac(ovsdbClient, ret, "br-wan");
            if(ret.ipV4Address == null || ret.macAddress==null) {
                //when not found - look them up for if_name = br-lan
                fillInIpAddressAndMac(ovsdbClient, ret, "br-lan");
            }
            
            
        } catch(OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        return ret;
    }
    
    public void fillInIpAddressAndMac(OvsdbClient ovsdbClient, ConnectNodeInfo connectNodeInfo, String ifName) {
        try {
            List<Operation> operations = new ArrayList<>();
            List<Condition> conditions = new ArrayList<>();        
            List<String> columns = new ArrayList<>(); 
            //populate macAddress, ipV4Address from Wifi_Inet_State   

            columns.add("inet_addr");
            columns.add("hwaddr");
            
            conditions.add(new Condition("if_name", Function.EQUALS, new Atom<>(ifName) ));
            
            operations.add(new Select(wifiInetStateDbTable, conditions , columns ));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Select from {}:", wifiInetStateDbTable);
                
                for(OperationResult res : result) {
                    LOG.debug("Op Result {}", res);
                }
            }
            
            Row row = null;
            if (result != null && result.length > 0 && !((SelectResult) result[0]).getRows().isEmpty()) {
                row = ((SelectResult)result[0]).getRows().iterator().next();
                connectNodeInfo.ipV4Address = getSingleValueFromSet(row, "inet_addr");
                connectNodeInfo.macAddress = row.getStringColumn("hwaddr");
            }
            
            
        } catch(OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
    
    public ConnectNodeInfo updateConnectNodeInfoOnConnect(OvsdbClient ovsdbClient, String clientCn, ConnectNodeInfo incomingConnectNodeInfo) {
        ConnectNodeInfo ret = incomingConnectNodeInfo.clone();
        
        try {
            List<Operation> operations = new ArrayList<>();            
            Map<String, Value> updateColumns = new HashMap<>();
            
            //set device_mode = cloud - plume's APs do not use it   
            //updateColumns.put("device_mode", new Atom<String>("cloud") );

            //update sku_number if it was empty
            if(ret.skuNumber == null || ret.skuNumber.isEmpty()) {
                ret.skuNumber = "connectus.ai_"+ ret.serialNumber;
                updateColumns.put("sku_number", new Atom<String>(ret.skuNumber) );
            }
    
            //Configure the MQTT connection 
            //ovsh u AWLAN_Node mqtt_settings:ins:'["map",[["broker","testportal.123wlan.com"],["topics","/ap/dev-ap-0300/opensync"],["qos","0"],["port","1883"],["remote_log","1"]]]'
            Map<String, String> newMqttSettings = new HashMap<>();
            newMqttSettings.put("broker", mqttBrokerAddress);
            newMqttSettings.put("topics", "/ap/"+clientCn+"_"+ret.serialNumber+"/opensync");
            newMqttSettings.put("port", ""+mqttBrokerListenPort);
            newMqttSettings.put("compress","zlib");
            newMqttSettings.put("qos", "0");
            newMqttSettings.put("remote_log", "1");
            
            if(ret.mqttSettings == null || !ret.mqttSettings.equals(newMqttSettings)) {                
                @SuppressWarnings("unchecked")
                com.vmware.ovsdb.protocol.operation.notation.Map<String,String> mgttSettings = com.vmware.ovsdb.protocol.operation.notation.Map.of(newMqttSettings);
                ret.mqttSettings = newMqttSettings;
                updateColumns.put("mqtt_settings", mgttSettings);
            }

            if(!updateColumns.isEmpty()) {
                Row row = new Row(updateColumns );
                operations.add(new Update(awlanNodeDbTable, row ));
                
                CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
                OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
                
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Updated {}:", awlanNodeDbTable);
                    
                    for(OperationResult res : result) {
                        LOG.debug("Op Result {}", res);
                    }
                }
            }
            
        } catch(OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        return ret;
    }
    
    
    /**
     * @param ovsdbClient
     * @return value of reporting_interval column for the stats_type=device from the Wifi_Stats_Config table. If value is not provisioned then return -1.
     */
    public long getDeviceStatsReportingInterval(OvsdbClient ovsdbClient) {
        long ret = -1;
        try {
            List<Operation> operations = new ArrayList<>();
            List<Condition> conditions = new ArrayList<>();        
            List<String> columns = new ArrayList<>(); 

            columns.add("reporting_interval");
            columns.add("stats_type");
            columns.add("radio_type");
            
            conditions.add(new Condition("stats_type", Function.EQUALS, new Atom<>("device") ));
            
            operations.add(new Select(wifiStatsConfigDbTable, conditions , columns ));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Select from {}:", wifiStatsConfigDbTable);
                
                for(OperationResult res : result) {
                    LOG.debug("Op Result {}", res);
                }
            }
            
            Row row = null;
            if (result != null && result.length > 0 && !((SelectResult) result[0]).getRows().isEmpty()) {
                row = ((SelectResult)result[0]).getRows().iterator().next();
                ret = row.getIntegerColumn("reporting_interval");
                LOG.info("Stats collection for stats_type=device is already configured with reporting_interval = {}", ret);
            }

        } catch(OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        return ret;
    }
    
    /**
     * @param ovsdbClient
     * @param value of reporting_interval column for the stats_type=device from the Wifi_Stats_Config table. If value is not provisioned then return -1.
     */
    public void updateDeviceStatsReportingInterval(OvsdbClient ovsdbClient, long newValue) {
        try {
            List<Operation> operations = new ArrayList<>();
            Map<String, Value> updateColumns = new HashMap<>(); 

            //turn on stats collection over MQTT: (reporting_interval is in seconds?)
            //$ ovsh i Wifi_Stats_Config reporting_interval:=10 radio_type:="2.4G" stats_type:="device"
            
            updateColumns.put("reporting_interval", new Atom<Integer>(10) );
            updateColumns.put("radio_type", new Atom<String>("2.4G") );
            updateColumns.put("stats_type", new Atom<String>("device") );

            Row row = new Row(updateColumns );
            operations.add(new Insert(wifiStatsConfigDbTable, row ));
            
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Updated {}:", wifiStatsConfigDbTable);
                
                for(OperationResult res : result) {
                    LOG.debug("Op Result {}", res);
                }
            }
            
        } catch(OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        
    }


    public void performRedirect(OvsdbClient ovsdbClient, String clientCn) {
        
        List<Operation> operations = new ArrayList<>();
        List<Condition> conditions = new ArrayList<>();        
        List<String> columns = new ArrayList<>(); 
        columns.add("manager_addr");
        columns.add("sku_number");
        columns.add("serial_number");
        columns.add("model");
        columns.add("firmware_version");
        
        try {
            LOG.debug("Starting Redirect");     

            operations.add(new Select(awlanNodeDbTable, conditions , columns ));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            
            LOG.debug("Select from AWLAN_Node:");
            
            for(OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }
    
            String skuNumber = null;
            String serialNumber = null;
            String model = null;
            String firmwareVersion = null;
    
            Row row = null;
            if (result != null && result.length > 0 && !((SelectResult) result[0]).getRows().isEmpty()) {
                row = ((SelectResult)result[0]).getRows().iterator().next();
            }
            
            firmwareVersion = (row!=null)?row.getStringColumn("firmware_version"):null;
            
            skuNumber = getSingleValueFromSet(row, "sku_number");
            serialNumber = getSingleValueFromSet(row, "serial_number");
            model = getSingleValueFromSet(row, "model");
                    
            LOG.info("Redirecting AP Node: clientCn {} serialNumber {} model {} firmwareVersion {} skuNumber {}", clientCn, serialNumber, model, firmwareVersion, skuNumber);
            
            //Update table AWLAN_Node - set manager_addr
            operations.clear();
            Map<String, Value> updateColumns = new HashMap<>();
    
            updateColumns.put("manager_addr", new Atom<String>("ssl:" + managerIpAddr + ":" + ovsdbListenPort) );
                        
            row = new Row(updateColumns );
            operations.add(new Update(awlanNodeDbTable, row ));
            
            fResult = ovsdbClient.transact(ovsdbName, operations);
            result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            
            LOG.debug("Updated AWLAN_Node:");
            
            for(OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }
            
            LOG.debug("Redirect Done");     
        } catch (ExecutionException | InterruptedException | OvsdbClientException | TimeoutException e) {
            LOG.error("Error when redirecting AP Node", e);
            throw new RuntimeException(e);
        }
        
    }
    
    public <T> T getSingleValueFromSet(Row row, String columnName){

        Set<T> set = (row!=null)?row.getSetColumn(columnName):null;
        T ret = (set!=null && !set.isEmpty())? set.iterator().next(): null;

        return ret;
    }
    
    public Map<String,InterfaceInfo> getProvisionedInterfaces(OvsdbClient ovsdbClient){
        Map<String, InterfaceInfo> ret = new HashMap<>();
        
        List<Operation> operations = new ArrayList<>();
        List<Condition> conditions = new ArrayList<>();        
        List<String> columns = new ArrayList<>(); 
        columns.add("name");
        columns.add("type");
        columns.add("options");
        columns.add("_uuid");
        columns.add("ofport");
        columns.add("mtu");
        columns.add("ifindex");
        columns.add("link_state");
        columns.add("admin_state");
        
        try {
            LOG.debug("Retrieving Interfaces:");     

            operations.add(new Select(interfaceDbTable, conditions , columns ));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            
            for(OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }
        
            for(Row row :((SelectResult)result[0]).getRows()){
            
                InterfaceInfo interfaceInfo = new InterfaceInfo();
                interfaceInfo.name = row.getStringColumn("name");
                interfaceInfo.type = row.getStringColumn("type");
                interfaceInfo.uuid = row.getUuidColumn("_uuid");
                
                Long tmp = getSingleValueFromSet(row, "ofport");
                interfaceInfo.ofport = tmp!=null?tmp.intValue():0;
                
                tmp = getSingleValueFromSet(row, "mtu");
                interfaceInfo.mtu = tmp!=null?tmp.intValue():0;
                
                tmp = getSingleValueFromSet(row, "ifindex");
                interfaceInfo.ifIndex = tmp!=null?tmp.intValue():0;
                
                String tmpStr = getSingleValueFromSet(row, "link_state");
                interfaceInfo.linkState = tmpStr!=null?tmpStr:"";

                tmpStr = getSingleValueFromSet(row, "admin_state");
                interfaceInfo.adminState = tmpStr!=null?tmpStr:"";
                
                interfaceInfo.options = row.getMapColumn("options");
                
                ret.put(interfaceInfo.name, interfaceInfo);
            }
            
            LOG.debug("Retrieved Interfaces: {}", ret);
            
        } catch (ExecutionException | InterruptedException | OvsdbClientException | TimeoutException e) {
            LOG.error("Error in getProvisionedInterfaces", e);

            throw new RuntimeException(e);
        }

        return ret;
    } 
    
    public Map<String,PortInfo> getProvisionedPorts(OvsdbClient ovsdbClient){
        Map<String, PortInfo> ret = new HashMap<>();
        
        List<Operation> operations = new ArrayList<>();
        List<Condition> conditions = new ArrayList<>();        
        List<String> columns = new ArrayList<>(); 
        columns.add("name");
        columns.add("_uuid");
        columns.add("interfaces");
        
        try {
            LOG.debug("Retrieving Ports:");     

            operations.add(new Select(portDbTable, conditions , columns ));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            
            for(OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }
        
            for(Row row :((SelectResult)result[0]).getRows()){
            
                PortInfo portInfo = new PortInfo();
                portInfo.name = row.getStringColumn("name");
                portInfo.uuid = row.getUuidColumn("_uuid");
                portInfo.interfaceUuids = row.getSetColumn("interfaces");
                
                ret.put(portInfo.name, portInfo);
            }
            
            LOG.debug("Retrieved Ports: {}", ret);
            
        } catch (ExecutionException | InterruptedException | OvsdbClientException | TimeoutException e) {
            LOG.error("Error in getProvisionedPorts", e);
            throw new RuntimeException(e);
        }

        return ret;
    } 
    
    public Map<String,BridgeInfo> getProvisionedBridges(OvsdbClient ovsdbClient){
        Map<String, BridgeInfo> ret = new HashMap<>();
        
        List<Operation> operations = new ArrayList<>();
        List<Condition> conditions = new ArrayList<>();        
        List<String> columns = new ArrayList<>(); 
        columns.add("name");
        columns.add("_uuid");
        columns.add("ports");
        
        try {
            LOG.debug("Retrieving Bridges:");     

            operations.add(new Select(bridgeDbTable, conditions , columns ));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            
            for(OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }
        
            for(Row row :((SelectResult)result[0]).getRows()){
            
                BridgeInfo bridgeInfo = new BridgeInfo();
                bridgeInfo.name = row.getStringColumn("name");
                bridgeInfo.uuid = row.getUuidColumn("_uuid");
                bridgeInfo.portUuids = row.getSetColumn("ports");
                
                ret.put(bridgeInfo.name, bridgeInfo);
            }
            
            LOG.debug("Retrieved Bridges: {}", ret);
            
        } catch (ExecutionException | InterruptedException | OvsdbClientException | TimeoutException e) {
            LOG.error("Error in getProvisionedBridges", e);
            throw new RuntimeException(e);
        }

        return ret;
    }
    
    public Map<String,WifiRadioConfigInfo> getProvisionedWifiRadioConfigs(OvsdbClient ovsdbClient){
        Map<String, WifiRadioConfigInfo> ret = new HashMap<>();
        
        List<Operation> operations = new ArrayList<>();
        List<Condition> conditions = new ArrayList<>();        
        List<String> columns = new ArrayList<>(); 

        columns.add("_uuid");
        columns.add("if_name");
        
        columns.add("channel");
        columns.add("channel_mode");
        columns.add("country");
        columns.add("enabled");
        columns.add("ht_mode");
        columns.add("tx_power");
        columns.add("vif_configs");
        columns.add("freq_band");
        columns.add("hw_config");        
        
        try {
            LOG.debug("Retrieving WifiRadioConfig:");     

            operations.add(new Select(wifiRadioConfigDbTable, conditions , columns ));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            
            for(OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }
        
            for(Row row :((SelectResult)result[0]).getRows()){
                
                WifiRadioConfigInfo wifiRadioConfigInfo = new WifiRadioConfigInfo();
                wifiRadioConfigInfo.uuid = row.getUuidColumn("_uuid");
                wifiRadioConfigInfo.ifName = row.getStringColumn("if_name");
                Long channelTmp = getSingleValueFromSet(row, "channel");
                if (channelTmp==null) {
                    channelTmp = -1L;
                }
                wifiRadioConfigInfo.channel = channelTmp.intValue();
                wifiRadioConfigInfo.channelMode = row.getStringColumn("channel_mode");
                wifiRadioConfigInfo.country = getSingleValueFromSet(row, "country");
                Boolean tmp = getSingleValueFromSet(row, "enabled");
                wifiRadioConfigInfo.enabled = tmp!=null?tmp:false;
                wifiRadioConfigInfo.htMode = getSingleValueFromSet(row, "ht_mode");
                wifiRadioConfigInfo.txPower = getSingleValueFromSet(row, "txPower");
                wifiRadioConfigInfo.vifConfigUuids = row.getSetColumn("vif_configs");
                wifiRadioConfigInfo.freqBand = row.getStringColumn("freq_band");
                wifiRadioConfigInfo.hwConfig = row.getMapColumn("hw_config");
                
                               
                ret.put(wifiRadioConfigInfo.ifName, wifiRadioConfigInfo);
            }
            
            LOG.debug("Retrieved WifiRadioConfig: {}", ret);
            
        } catch (ExecutionException | InterruptedException | OvsdbClientException | TimeoutException e) {
            LOG.error("Error in getProvisionedWifiRadioConfigs", e);
            throw new RuntimeException(e);
        }

        return ret;
    }

    public Map<String,WifiVifConfigInfo> getProvisionedWifiVifConfigs(OvsdbClient ovsdbClient){
        Map<String, WifiVifConfigInfo> ret = new HashMap<>();
        
        List<Operation> operations = new ArrayList<>();
        List<Condition> conditions = new ArrayList<>();        
        List<String> columns = new ArrayList<>(); 
        columns.add("bridge");
        columns.add("_uuid");
        columns.add("btm");
        columns.add("enabled");
        columns.add("ft_psk");
        columns.add("group_rekey");
        columns.add("if_name");
        columns.add("mode");
        columns.add("rrm");
        columns.add("ssid");
        columns.add("ssid_broadcast");
        columns.add("uapsd_enable");
        columns.add("vif_radio_idx");
        columns.add("security");
        
        try {
            LOG.debug("Retrieving WifiVifConfig:");     

            operations.add(new Select(wifiVifConfigDbTable, conditions , columns ));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            
            for(OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }
        
            for(Row row :((SelectResult)result[0]).getRows()){

                WifiVifConfigInfo wifiVifConfigInfo = new WifiVifConfigInfo();
                wifiVifConfigInfo.bridge = row.getStringColumn("bridge");
                wifiVifConfigInfo.uuid = row.getUuidColumn("_uuid");
                wifiVifConfigInfo.btm = row.getIntegerColumn("btm").intValue();
                wifiVifConfigInfo.enabled = row.getBooleanColumn("enabled");
                wifiVifConfigInfo.ftPsk = row.getIntegerColumn("ft_psk").intValue();
                wifiVifConfigInfo.groupRekey = row.getIntegerColumn("group_rekey").intValue();
                wifiVifConfigInfo.ifName = row.getStringColumn("if_name");
                wifiVifConfigInfo.mode = row.getStringColumn("mode");
                wifiVifConfigInfo.rrm = row.getIntegerColumn("rrm").intValue();
                wifiVifConfigInfo.ssid = row.getStringColumn("ssid");
                wifiVifConfigInfo.ssidBroadcast = row.getStringColumn("ssid_broadcast");
                wifiVifConfigInfo.uapsdEnable = row.getBooleanColumn("uapsd_enable");
                wifiVifConfigInfo.vifRadioIdx = row.getIntegerColumn("vif_radio_idx").intValue();
                wifiVifConfigInfo.security = row.getMapColumn("security");
                
                ret.put(wifiVifConfigInfo.ifName + '_' + wifiVifConfigInfo.ssid, wifiVifConfigInfo);
            }
            
            LOG.debug("Retrieved WifiVifConfigs: {}", ret);
            
        } catch (ExecutionException | InterruptedException | OvsdbClientException | TimeoutException e) {
            LOG.error("Error in getProvisionedWifiVifConfigs", e);
            throw new RuntimeException(e);
        }

        return ret;
    }
    
    public Map<String,WifiInetConfigInfo> getProvisionedWifiInetConfigs(OvsdbClient ovsdbClient){
        Map<String, WifiInetConfigInfo> ret = new HashMap<>();
        
        List<Operation> operations = new ArrayList<>();
        List<Condition> conditions = new ArrayList<>();        
        List<String> columns = new ArrayList<>(); 
        columns.add("NAT");
        columns.add("_uuid");
        columns.add("enabled");
        columns.add("if_name");
        columns.add("if_type");
        columns.add("ip_assign_scheme");
        columns.add("network");
        
        try {
            LOG.debug("Retrieving WifiInetConfig:");     

            operations.add(new Select(wifiInetConfigDbTable, conditions , columns ));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            
            for(OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }
        
            for(Row row :((SelectResult)result[0]).getRows()){

                WifiInetConfigInfo wifiInetConfigInfo = new WifiInetConfigInfo();
                Boolean natTmp = getSingleValueFromSet(row, "NAT");
                wifiInetConfigInfo.nat = natTmp!=null?natTmp:false;
                
                wifiInetConfigInfo.uuid = row.getUuidColumn("_uuid");
                wifiInetConfigInfo.enabled = row.getBooleanColumn("enabled");
                wifiInetConfigInfo.ifName = row.getStringColumn("if_name");
                wifiInetConfigInfo.ifType = row.getStringColumn("if_type");
                wifiInetConfigInfo.ipAssignScheme = row.getStringColumn("ip_assign_scheme");
                wifiInetConfigInfo.network = row.getBooleanColumn("network");
                
                ret.put(wifiInetConfigInfo.ifName, wifiInetConfigInfo);
            }
            
            LOG.debug("Retrieved WifiInetConfigs: {}", ret);
            
        } catch (ExecutionException | InterruptedException | OvsdbClientException | TimeoutException e) {
            LOG.error("Error in getProvisionedWifiInetConfigs", e);
            throw new RuntimeException(e);
        }

        return ret;
    }
    
    public Map<String,WifiStatsConfigInfo> getProvisionedWifiStatsConfigs(OvsdbClient ovsdbClient){
        Map<String, WifiStatsConfigInfo> ret = new HashMap<>();
        
        List<Operation> operations = new ArrayList<>();
        List<Condition> conditions = new ArrayList<>();        
        List<String> columns = new ArrayList<>(); 
        columns.add("channel_list");
        columns.add("radio_type");
        columns.add("reporting_interval");
        columns.add("sampling_interval");
        columns.add("stats_type");
        columns.add("survey_interval_ms");
        columns.add("survey_type");
        columns.add("threshold");
        columns.add("_uuid");
        
        try {
            LOG.debug("Retrieving WifiStatsConfigs:");     

            operations.add(new Select(wifiStatsConfigDbTable, conditions , columns ));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            
            for(OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }
        
            for(Row row :((SelectResult)result[0]).getRows()){
            
                WifiStatsConfigInfo wifiStatsConfigInfo = new WifiStatsConfigInfo();

                wifiStatsConfigInfo.channelList = row.getSetColumn("channel_list");
                wifiStatsConfigInfo.radioType = row.getStringColumn("radio_type");
                wifiStatsConfigInfo.reportingInterval = row.getIntegerColumn("reporting_interval").intValue();
                wifiStatsConfigInfo.samplingInterval = row.getIntegerColumn("sampling_interval").intValue();
                wifiStatsConfigInfo.statsType = row.getStringColumn("stats_type");
                wifiStatsConfigInfo.surveyType = getSingleValueFromSet(row, "survey_type");
                Long tmp = getSingleValueFromSet(row, "survey_interval_ms");
                wifiStatsConfigInfo.surveyIntervalMs = tmp!=null? tmp.intValue(): 0;
                wifiStatsConfigInfo.threshold = row.getMapColumn("threshold");                
                wifiStatsConfigInfo.uuid = row.getUuidColumn("_uuid");
                
                ret.put(wifiStatsConfigInfo.radioType + "_" + wifiStatsConfigInfo.statsType + "_" + wifiStatsConfigInfo.surveyType, wifiStatsConfigInfo);
            }
            
            LOG.debug("Retrieved WifiStatsConfigs: {}", ret);
            
        } catch (ExecutionException | InterruptedException | OvsdbClientException | TimeoutException e) {
            LOG.error("Error in getProvisionedWifiStatsConfigs", e);

            throw new RuntimeException(e);
        }

        return ret;
    }     
    
    public void provisionSingleBridgePortInterface(OvsdbClient ovsdbClient, String interfaceName, String bridgeName, String interfaceType, Map<String, String> interfaceOptions, Map<String, InterfaceInfo> provisionedInterfaces, Map<String, PortInfo> provisionedPorts,
            Map<String, BridgeInfo> provisionedBridges)
            throws OvsdbClientException, TimeoutException, ExecutionException, InterruptedException {
        
        if(!provisionedInterfaces.containsKey(interfaceName)) {
            //Create this interface and link it to the port and the bridge
            List<Operation> operations = new ArrayList<>();
            Map<String, Value> updateColumns = new HashMap<>();
            List<Condition> conditions = new ArrayList<>();        


            updateColumns.put("name", new Atom<>(interfaceName) );
            if(interfaceType!=null) {
                updateColumns.put("type", new Atom<>(interfaceType) );
            }
            //updateColumns.put("admin_state", new Atom<String>("up") );
            //updateColumns.put("link_state", new Atom<String>("up") );
            //updateColumns.put("ifindex", new Atom<Integer>(ifIndex) );
            //updateColumns.put("mtu", new Atom<Integer>(1500) );
            //updateColumns.put("ofport", new Atom<Integer>(ofport) );
            
            if(interfaceOptions!=null) {
                @SuppressWarnings("unchecked")
                com.vmware.ovsdb.protocol.operation.notation.Map<String,String> ifOptions = com.vmware.ovsdb.protocol.operation.notation.Map.of(interfaceOptions);
                updateColumns.put("options", ifOptions);
            }

            
            Uuid interfaceUuid = null;
            
            Row row = new Row(updateColumns );
            operations.add(new Insert(interfaceDbTable, row ));
            
            {
                CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
                OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
                
                LOG.debug("Provisioned Interface for {}", interfaceName);
                
                for(OperationResult res : result) {
                    LOG.debug("Op Result {}", res);
                    if(res instanceof InsertResult) {
                        interfaceUuid = ((InsertResult) res).getUuid();
                    }
                }
            }
            
            if (interfaceUuid == null) {
                throw new IllegalStateException("Interface entry was not created successfully");
            }
            
            Uuid portUuid = null;
            operations = new ArrayList<>();
            //link the interface to the port, create port if necessary
            if(!provisionedPorts.containsKey(interfaceName)) {
                //need to create port
                updateColumns = new HashMap<>(); 

                //portUuid = new Uuid(new UUID(System.currentTimeMillis(), System.nanoTime())) ;
                updateColumns.put("name", new Atom<String>(interfaceName) );
                //updateColumns.put("_uuid", new Atom<Uuid>(portUuid));
                
                Set<Uuid> portInterfacesSet = new HashSet<>();
                portInterfacesSet.add(interfaceUuid);
                com.vmware.ovsdb.protocol.operation.notation.Set portInterfaces = com.vmware.ovsdb.protocol.operation.notation.Set.of(portInterfacesSet);
                updateColumns.put("interfaces", portInterfaces );
                
                row = new Row(updateColumns);
                operations.add(new Insert(portDbTable, row ));
                       
                {
                    CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
                    OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
                    
                    LOG.debug("Provisioned Port for {}", interfaceName);
                    
                    for(OperationResult res : result) {
                        LOG.debug("Op Result {}", res);
                        if(res instanceof InsertResult) {
                            portUuid = ((InsertResult) res).getUuid();
                        }
                    }
                }

            } else {
                //need to update port
                PortInfo existingPort = provisionedPorts.get(interfaceName);
                portUuid = existingPort.uuid;
                
                conditions = new ArrayList<>();
                updateColumns = new HashMap<>(); 

                conditions.add(new Condition("name", Function.EQUALS, new Atom<>(interfaceName) ));
                                    
                Set<Uuid> portInterfacesSet = new HashSet<>();
                if(existingPort.interfaceUuids!=null) {
                    portInterfacesSet.addAll(existingPort.interfaceUuids);
                }
                portInterfacesSet.add(interfaceUuid);
                com.vmware.ovsdb.protocol.operation.notation.Set portInterfaces = com.vmware.ovsdb.protocol.operation.notation.Set.of(portInterfacesSet);
                updateColumns.put("interfaces", portInterfaces );
                
                row = new Row(updateColumns);
                operations.add(new Update(portDbTable, row ));
                
                {
                    CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
                    OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
                    
                    LOG.debug("Updated Port for {}", interfaceName);
                    
                    for(OperationResult res : result) {
                        LOG.debug("Op Result {}", res);
                    }
                }

            }
            
            
            if (portUuid == null) {
                throw new IllegalStateException("Port entry was not created successfully");
            }
            

            operations = new ArrayList<>();
            
            //link the port to the bridge
            if(provisionedBridges.containsKey(bridgeName)) {
                BridgeInfo existingBridge = provisionedBridges.get(bridgeName);

                conditions = new ArrayList<>();
                updateColumns = new HashMap<>(); 

                conditions.add(new Condition("name", Function.EQUALS, new Atom<>(bridgeName) ));
                                    
                
                Set<Uuid> bridgePortsSet = new HashSet<>();
                if(existingBridge.portUuids!=null) {
                    bridgePortsSet.addAll(existingBridge.portUuids);
                }
                
                bridgePortsSet.add(portUuid);
                com.vmware.ovsdb.protocol.operation.notation.Set bridgePorts = com.vmware.ovsdb.protocol.operation.notation.Set.of(bridgePortsSet);
                updateColumns.put("ports", bridgePorts );
                
                row = new Row(updateColumns);
                operations.add(new Update(bridgeDbTable, row ));
                
            } else {
                LOG.warn("provisionedBridges does not have bridge {} - {} - port will be dangling", bridgeName, provisionedBridges.keySet());
            }
            
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Finished provisioning Interface/port/bridge for {} / {}", interfaceName, bridgeName);
                
                for(OperationResult res : result) {
                    LOG.debug("Op Result {}", res);
                }
            }

        }
    }

    public static final String homeAp24 = "home-ap-24";
    public static final String homeApL50 = "home-ap-l50";
    public static final String brHome = "br-home";
    public static final String brWan = "br-wan";
    
    public static final String patchW2h = "patch-w2h";
    public static final String patchH2w = "patch-h2w";
    
    public void provisionBridgePortInterface(OvsdbClient ovsdbClient) {
        try {
            Map<String,InterfaceInfo> provisionedInterfaces = getProvisionedInterfaces(ovsdbClient);
            LOG.debug("Existing Interfaces: {}", provisionedInterfaces.keySet());

            Map<String,PortInfo> provisionedPorts = getProvisionedPorts(ovsdbClient);
            LOG.debug("Existing Ports: {}", provisionedPorts.keySet());

            Map<String,BridgeInfo> provisionedBridges = getProvisionedBridges(ovsdbClient);
            LOG.debug("Existing Bridges: {}", provisionedBridges.keySet());

            Map<String, String> patchH2wOptions = new HashMap<>();
            patchH2wOptions.put("peer", "patch-w2h");

            Map<String, String> patchW2hOptions = new HashMap<>();
            patchH2wOptions.put("peer", "patch-h2w");

            provisionSingleBridgePortInterface(ovsdbClient, patchH2w, brHome, "patch", patchH2wOptions, provisionedInterfaces, provisionedPorts, provisionedBridges);
            provisionSingleBridgePortInterface(ovsdbClient, patchW2h, brWan, "patch", patchW2hOptions, provisionedInterfaces, provisionedPorts, provisionedBridges);

            provisionSingleBridgePortInterface(ovsdbClient, homeApL50, brHome, null, null, provisionedInterfaces, provisionedPorts, provisionedBridges);
            provisionSingleBridgePortInterface(ovsdbClient, homeAp24, brHome, null, null, provisionedInterfaces, provisionedPorts, provisionedBridges);

        } catch(OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in provisionBridgePortInterface", e);
            throw new RuntimeException(e);
        }
        
    }
    
    public void removeOnboardingSsids(OvsdbClient ovsdbClient) {
        try {
            List<Operation> operations = new ArrayList<>();
            List<Condition> conditions = new ArrayList<>();
            conditions.add(new Condition("ssid", Function.EQUALS, new Atom<>("opensync.onboard") ));
 
            operations.add(new Delete(wifiVifConfigDbTable, conditions ));
            
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Removed onboarding SSIDs from {}:", wifiVifConfigDbTable);
                
                for(OperationResult res : result) {
                    LOG.debug("Op Result {}", res);
                }
            }
            
        } catch(OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in removeOnboardingSsids", e);
            throw new RuntimeException(e);
        }
        
    }

    public void removeAllSsids(OvsdbClient ovsdbClient) {
        try {
            List<Operation> operations = new ArrayList<>();
 
            operations.add(new Delete(wifiVifConfigDbTable));
            
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Removed all existing SSIDs from {}:", wifiVifConfigDbTable);
                
                for(OperationResult res : result) {
                    LOG.debug("Op Result {}", res);
                }
            }
            
            
            //Now clean up references in the vif_configs columns
            operations = new ArrayList<>();
            Map<String, Value> updateColumns = new HashMap<>();
            Set<Uuid> vifConfigsSet = new HashSet<>();
            com.vmware.ovsdb.protocol.operation.notation.Set vifConfigs = com.vmware.ovsdb.protocol.operation.notation.Set.of(vifConfigsSet);
            updateColumns.put("vif_configs", vifConfigs );
            
            Row row = new Row(updateColumns);
            operations.add(new Update(wifiRadioConfigDbTable, row ));
            
            fResult = ovsdbClient.transact(ovsdbName, operations);
            result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Updated WifiRadioConfig ");
                
                for(OperationResult res : result) {
                    LOG.debug("Op Result {}", res);
                }
            }

            LOG.info("Removed all ssids");            
            
        } catch(OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in removeAllSsids", e);
            throw new RuntimeException(e);
        }
        
    }
    
    public void configureWifiRadios(OvsdbClient ovsdbClient, OpensyncAPRadioConfig opensyncAPRadioConfig) {
        Map<String,WifiRadioConfigInfo> provisionedWifiRadios = getProvisionedWifiRadioConfigs(ovsdbClient);
        LOG.debug("Existing WifiRadioConfigs: {}", provisionedWifiRadios.keySet());
        
        try {
            String country = opensyncAPRadioConfig.getCountry();
            String configName = "wifi0";
            int channel = opensyncAPRadioConfig.getRadioChannel24G();
            Map<String, String> hwConfig = new HashMap<>();
            configureWifiRadios(ovsdbClient, configName, provisionedWifiRadios, channel, hwConfig, country);
            
            configName = "wifi1";
            channel = opensyncAPRadioConfig.getRadioChannel5LG();
            hwConfig = new HashMap<>();
            hwConfig.put("dfs_enable", "1");
            hwConfig.put("dfs_ignorecac", "0");
            hwConfig.put("dfs_usenol", "1");
            
            configureWifiRadios(ovsdbClient, configName, provisionedWifiRadios, channel, hwConfig, country);

            configName = "wifi2";
            channel = opensyncAPRadioConfig.getRadioChannel5HG();
            hwConfig = new HashMap<>();
            hwConfig.put("dfs_enable", "1");
            hwConfig.put("dfs_ignorecac", "0");
            hwConfig.put("dfs_usenol", "1");
            
            configureWifiRadios(ovsdbClient, configName, provisionedWifiRadios, channel, hwConfig, country);

        } catch(OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in configureWifiRadios", e);
            throw new RuntimeException(e);
        }
        
    }
    
    public void configureWifiRadios(OvsdbClient ovsdbClient, String configName,
            Map<String, WifiRadioConfigInfo> provisionedWifiRadios, int channel, Map<String, String> hwConfig, String country )
            throws OvsdbClientException, TimeoutException, ExecutionException, InterruptedException {
        
        WifiRadioConfigInfo existingConfig = provisionedWifiRadios.get(configName);
        
        if(existingConfig==null) {
            LOG.warn("There is no WifiRadioConfig {}", configName);
            return;
        }
        
        List<Operation> operations = new ArrayList<>();
        Map<String, Value> updateColumns = new HashMap<>();
        List<Condition> conditions = new ArrayList<>();        
        conditions.add(new Condition("if_name", Function.EQUALS, new Atom<>(configName) ));

        updateColumns.put("channel", new Atom<Integer>(channel) );
        updateColumns.put("country", new Atom<>(country) );
        @SuppressWarnings("unchecked")
        com.vmware.ovsdb.protocol.operation.notation.Map<String,String> hwConfigMap = com.vmware.ovsdb.protocol.operation.notation.Map.of(hwConfig);
        updateColumns.put("hw_config", hwConfigMap);
        
        Row row = new Row(updateColumns );
        operations.add(new Update(wifiRadioConfigDbTable, conditions, row ));
        
        CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
        OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
        
        LOG.debug("Provisioned channel {} for {}", channel, configName);
        
        for(OperationResult res : result) {
            LOG.debug("Op Result {}", res);
        }
    }

    public void configureSingleSsid(OvsdbClient ovsdbClient, String bridge, String ifName, String ssid, boolean ssidBroadcast, Map<String, String> security, Map<String,WifiRadioConfigInfo> provisionedWifiRadioConfigs, String radioIfName) {
        List<Operation> operations = new ArrayList<>();
        Map<String, Value> updateColumns = new HashMap<>();

        try {
            ///usr/plume/tools/ovsh i Wifi_VIF_Config 
            // bridge:=br-home btm:=1 enabled:=true ft_psk:=0 group_rekey:=86400 
            // if_name:=home-ap-24 mode:=ap rrm:=1 ssid:=ConnectUS-Plume ssid_broadcast:=enabled 
            // uapsd_enable:=true vif_radio_idx:=2 security:='["map",[["encryption","WPA-PSK"],["key","12345678"],["mode","2"]]]'

            updateColumns.put("bridge", new Atom<>(bridge) );
            updateColumns.put("btm", new Atom<>(1) );
            updateColumns.put("enabled", new Atom<>(true) );
            updateColumns.put("ft_psk", new Atom<>(0) );
            updateColumns.put("group_rekey", new Atom<>(86400) );
            updateColumns.put("if_name", new Atom<>(ifName) );
            updateColumns.put("mode", new Atom<>("ap") );
            updateColumns.put("rrm", new Atom<>(1) );
            updateColumns.put("ssid", new Atom<>(ssid) );
            updateColumns.put("ssid_broadcast", new Atom<>(ssidBroadcast?"enabled":"disabled") );
            updateColumns.put("uapsd_enable", new Atom<>(true) );
            updateColumns.put("vif_radio_idx", new Atom<>(2) );
            
            @SuppressWarnings("unchecked")
            com.vmware.ovsdb.protocol.operation.notation.Map<String,String> securityMap = com.vmware.ovsdb.protocol.operation.notation.Map.of(security);
            updateColumns.put("security", securityMap);
                        
            Row row = new Row(updateColumns);
            operations.add(new Insert(wifiVifConfigDbTable, row ));
                   
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            
            LOG.debug("Provisioned SSID {} on {}", ssid, ifName);
            
            Uuid vifConfigUuid = null;
            for(OperationResult res : result) {
                LOG.debug("Op Result {}", res);
                if(res instanceof InsertResult) {
                    vifConfigUuid = ((InsertResult) res).getUuid();
                }
            }
            
            if (vifConfigUuid == null) {
                throw new IllegalStateException("Wifi_VIF_Config entry was not created successfully");
            }

            //update Wifi_Radio_Config here - add vifConfigUuid
            ///usr/plume/tools/ovsh u Wifi_Radio_Config vif_configs:='["set",[["uuid","98e42897-b567-4186-84a6-4a4e38a51e9d"],["uuid","4314920e-c4e6-42a6-93e3-261142ed9adf"]]]' --where if_name==wifi0
            updateColumns.clear();
            operations.clear();
            
            WifiRadioConfigInfo wifiRadioConfigInfo = provisionedWifiRadioConfigs.get(radioIfName);
            if(wifiRadioConfigInfo == null) {
                throw new IllegalStateException("missing Wifi_Radio_Config entry "+ radioIfName);
            }
            
            Set<Uuid> vifConfigsSet = new HashSet<>(wifiRadioConfigInfo.vifConfigUuids);
            vifConfigsSet.add(vifConfigUuid);
            com.vmware.ovsdb.protocol.operation.notation.Set vifConfigs = com.vmware.ovsdb.protocol.operation.notation.Set.of(vifConfigsSet);
            updateColumns.put("vif_configs", vifConfigs );
            
            List<Condition> conditions = new ArrayList<>();
            conditions.add(new Condition("if_name", Function.EQUALS, new Atom<>(radioIfName) ));
 
            row = new Row(updateColumns);
            operations.add(new Update(wifiRadioConfigDbTable, conditions, row ));
            
            fResult = ovsdbClient.transact(ovsdbName, operations);
            result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Updated WifiRadioConfig {} for SSID {}:", radioIfName, ssid);
                
                for(OperationResult res : result) {
                    LOG.debug("Op Result {}", res);
                }
            }

            LOG.info("Provisioned SSID {} on interface {} / {}", ssid, ifName, radioIfName);

        } catch(OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in configureSingleSsid", e);
            throw new RuntimeException(e);
        }
    }
    
    public void configureSsids(OvsdbClient ovsdbClient, List<OpensyncAPSsidConfig> ssidConfigs) {
        if(ssidConfigs==null || ssidConfigs.isEmpty()) {
            LOG.debug("No SSIDs to configure");
            return;
        }
        
        Map<String,WifiVifConfigInfo> provisionedWifiVifConfigs = getProvisionedWifiVifConfigs(ovsdbClient);
        Map<String,WifiRadioConfigInfo> provisionedWifiRadioConfigs = getProvisionedWifiRadioConfigs(ovsdbClient);
        LOG.debug("Existing WifiVifConfigs: {}", provisionedWifiVifConfigs.keySet());
        
        for(OpensyncAPSsidConfig ssidCfg: ssidConfigs) {
            String bridge = brHome;
            String ifName = (ssidCfg.getRadioType() == RadioType.is2dot4GHz)?homeAp24:homeApL50;
            String radioIfName = (ssidCfg.getRadioType() == RadioType.is2dot4GHz)?"wifi0":"wifi1";
            String ssid = ssidCfg.getSsid();
            boolean ssidBroadcast = ssidCfg.isBroadcast();
            Map<String, String> security = new HashMap<>();
            security.put("encryption", ssidCfg.getEncryption());
            security.put("key", ssidCfg.getKey());
            security.put("mode", ssidCfg.getMode());
            
            if(!provisionedWifiVifConfigs.containsKey(ifName+"_"+ssid)){
                try {
                    configureSingleSsid(ovsdbClient, bridge, ifName, ssid, ssidBroadcast, security, provisionedWifiRadioConfigs, radioIfName);
                } catch (IllegalStateException e) {
                    //could not provision this SSID, but still can go on
                    LOG.warn("could not provision SSID {} on {}", ssid, radioIfName);
                }
            }

        }
        
    }

    public void configureWifiInet(OvsdbClient ovsdbClient, Map<String, WifiInetConfigInfo> provisionedWifiInetConfigs, String ifName) {
        List<Operation> operations = new ArrayList<>();
        Map<String, Value> updateColumns = new HashMap<>();

        try {
            ///usr/plume/tools/ovsh i Wifi_Inet_Config NAT:=false enabled:=true if_name:=home-ap-24 if_type:=vif ip_assign_scheme:=none network:=true
            
            updateColumns.put("NAT", new Atom<>(false) );
            updateColumns.put("enabled", new Atom<>(true) );
            updateColumns.put("if_name", new Atom<>(ifName) );
            updateColumns.put("if_type", new Atom<>("vif") );
            updateColumns.put("ip_assign_scheme", new Atom<>("none") );
            updateColumns.put("network", new Atom<>(true) );
            
            Row row = new Row(updateColumns);
            operations.add(new Insert(wifiInetConfigDbTable, row ));
                   
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            
            LOG.debug("Provisioned WifiInetConfig {}", ifName);
            
            for(OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }

        } catch(OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in configureWifiInet", e);
            throw new RuntimeException(e);
        }
        
    }

    public void configureWifiInetSetNetwork(OvsdbClient ovsdbClient, String ifName) {
        List<Operation> operations = new ArrayList<>();
        Map<String, Value> updateColumns = new HashMap<>();
        List<Condition> conditions = new ArrayList<>();

        try {
            ///usr/plume/tools/ovsh u Wifi_Inet_Config -w if_name=="br-home" network:=true
            
            conditions.add(new Condition("if_name", Function.EQUALS, new Atom<>(ifName)));            
            updateColumns.put("network", new Atom<>(true) );
            
            Row row = new Row(updateColumns);
            operations.add(new Update(wifiInetConfigDbTable, conditions , row ));
                   
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            
            LOG.debug("Enabled network on WifiInetConfig {}", ifName);
            
            for(OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }

        } catch(OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in configureWifiInetSetNetwork", e);
            throw new RuntimeException(e);
        }
        
    }

    public void configureWifiInet(OvsdbClient ovsdbClient) {
        Map<String, WifiInetConfigInfo> provisionedWifiInetConfigs = getProvisionedWifiInetConfigs(ovsdbClient);
        LOG.debug("Existing WifiInetConfigs: {}", provisionedWifiInetConfigs.keySet());
        
        String ifName = homeAp24;
        if(!provisionedWifiInetConfigs.containsKey(ifName)) {
            configureWifiInet(ovsdbClient, provisionedWifiInetConfigs, ifName);
        }

        ifName = homeApL50;
        if(!provisionedWifiInetConfigs.containsKey(ifName)) {
            configureWifiInet(ovsdbClient, provisionedWifiInetConfigs, ifName);
        }

        if(!provisionedWifiInetConfigs.containsKey(brHome) || !provisionedWifiInetConfigs.get(brHome).network) {
            //set network flag on brHome in wifiInetConfig table
            configureWifiInetSetNetwork(ovsdbClient, brHome);
        }
    }

    public void configureStats(OvsdbClient ovsdbClient) {
        
        Map<String,WifiStatsConfigInfo> provisionedWifiStatsConfigs = getProvisionedWifiStatsConfigs(ovsdbClient);
        
        try {
            List<Operation> operations = new ArrayList<>();
            Map<String, Value> updateColumns = new HashMap<>(); 
            Row row;
            
            Set<Integer> channelSet = new HashSet<>();
            channelSet.add(1);
            channelSet.add(6);
            channelSet.add(11);
            com.vmware.ovsdb.protocol.operation.notation.Set channels = com.vmware.ovsdb.protocol.operation.notation.Set.of(channelSet);
            
            Map<String, Integer> thresholdMap = new HashMap<>();
            thresholdMap.put("max_delay", 600);
            thresholdMap.put("util", 10);
            
            @SuppressWarnings("unchecked")
            com.vmware.ovsdb.protocol.operation.notation.Map<String, Integer> thresholds = com.vmware.ovsdb.protocol.operation.notation.Map.of(thresholdMap);

            if(!provisionedWifiStatsConfigs.containsKey("2.4G_device_null")) {
                //            
                //updateColumns.put("channel_list", channels );
                updateColumns.put("radio_type", new Atom<>("2.4G") );
                updateColumns.put("reporting_interval", new Atom<>(10) );
                updateColumns.put("sampling_interval", new Atom<>(0) );
                updateColumns.put("stats_type", new Atom<>("device") );
                //updateColumns.put("survey_interval_ms", new Atom<>(10) );
                //updateColumns.put("survey_type", new Atom<>("on-chan") );
                //updateColumns.put("threshold", thresholds );
                
                
                row = new Row(updateColumns );
                operations.add(new Insert(wifiStatsConfigDbTable, row ));
                //
            }
            
            if(!provisionedWifiStatsConfigs.containsKey("5GL_survey_on-chan")) {
                //
                updateColumns = new HashMap<>();
                //updateColumns.put("channel_list", channels );
                updateColumns.put("radio_type", new Atom<>("5GL") );
                updateColumns.put("reporting_interval", new Atom<>(60) );
                updateColumns.put("sampling_interval", new Atom<>(10) );
                updateColumns.put("stats_type", new Atom<>("survey") );
                updateColumns.put("survey_interval_ms", new Atom<>(0) );
                updateColumns.put("survey_type", new Atom<>("on-chan") );
                //updateColumns.put("threshold", thresholds );
                
                
                row = new Row(updateColumns );
                operations.add(new Insert(wifiStatsConfigDbTable, row ));
                //
            }

            if(!provisionedWifiStatsConfigs.containsKey("2.4G_survey_off-chan")) {
                //
                updateColumns = new HashMap<>();
                updateColumns.put("channel_list", channels );
                updateColumns.put("radio_type", new Atom<>("2.4G") );
                updateColumns.put("reporting_interval", new Atom<>(120) );
                updateColumns.put("sampling_interval", new Atom<>(10) );
                updateColumns.put("stats_type", new Atom<>("survey") );
                updateColumns.put("survey_interval_ms", new Atom<>(50) );
                updateColumns.put("survey_type", new Atom<>("off-chan") );
                updateColumns.put("threshold", thresholds );
                
                
                row = new Row(updateColumns );
                operations.add(new Insert(wifiStatsConfigDbTable, row ));
                //
            }
            
            if(!provisionedWifiStatsConfigs.containsKey("2.4G_neighbor_off-chan")) {
                //
                updateColumns = new HashMap<>();
                updateColumns.put("channel_list", channels );
                updateColumns.put("radio_type", new Atom<>("2.4G") );
                updateColumns.put("reporting_interval", new Atom<>(120) );
                updateColumns.put("sampling_interval", new Atom<>(0) );
                updateColumns.put("stats_type", new Atom<>("neighbor") );
                updateColumns.put("survey_interval_ms", new Atom<>(0) );
                updateColumns.put("survey_type", new Atom<>("off-chan") );
                //updateColumns.put("threshold", thresholds );
                
                
                row = new Row(updateColumns );
                operations.add(new Insert(wifiStatsConfigDbTable, row ));
                //
            }

            if(!provisionedWifiStatsConfigs.containsKey("5GU_neighbor_on-chan")) {
                //
                updateColumns = new HashMap<>();
                //updateColumns.put("channel_list", channels );
                updateColumns.put("radio_type", new Atom<>("5GU") );
                updateColumns.put("reporting_interval", new Atom<>(60) );
                updateColumns.put("sampling_interval", new Atom<>(0) );
                updateColumns.put("stats_type", new Atom<>("neighbor") );
                updateColumns.put("survey_interval_ms", new Atom<>(0) );
                updateColumns.put("survey_type", new Atom<>("on-chan") );
                //updateColumns.put("threshold", thresholds );
                
                
                row = new Row(updateColumns );
                operations.add(new Insert(wifiStatsConfigDbTable, row ));
                //
            }
            
            if(!provisionedWifiStatsConfigs.containsKey("5GL_client_null")) {
                //
                updateColumns = new HashMap<>();
                //updateColumns.put("channel_list", channels );
                updateColumns.put("radio_type", new Atom<>("5GL") );
                updateColumns.put("reporting_interval", new Atom<>(60) );
                updateColumns.put("sampling_interval", new Atom<>(10) );
                updateColumns.put("stats_type", new Atom<>("client") );
                //updateColumns.put("survey_interval_ms", new Atom<>(0) );
                //updateColumns.put("survey_type", new Atom<>("on-chan") );
                //updateColumns.put("threshold", thresholds );
                
                
                row = new Row(updateColumns );
                operations.add(new Insert(wifiStatsConfigDbTable, row ));
                //
            }
            
            if(!provisionedWifiStatsConfigs.containsKey("5GU_client_null")) {
                //
                updateColumns = new HashMap<>();
                //updateColumns.put("channel_list", channels );
                updateColumns.put("radio_type", new Atom<>("5GU") );
                updateColumns.put("reporting_interval", new Atom<>(60) );
                updateColumns.put("sampling_interval", new Atom<>(10) );
                updateColumns.put("stats_type", new Atom<>("client") );
                //updateColumns.put("survey_interval_ms", new Atom<>(0) );
                //updateColumns.put("survey_type", new Atom<>("on-chan") );
                //updateColumns.put("threshold", thresholds );
                
                
                row = new Row(updateColumns );
                operations.add(new Insert(wifiStatsConfigDbTable, row ));
                //
            }
            
            if(!provisionedWifiStatsConfigs.containsKey("2.4G_survey_on-chan")) {
                //
                updateColumns = new HashMap<>();
                //updateColumns.put("channel_list", channels );
                updateColumns.put("radio_type", new Atom<>("2.4G") );
                updateColumns.put("reporting_interval", new Atom<>(60) );
                updateColumns.put("sampling_interval", new Atom<>(10) );
                updateColumns.put("stats_type", new Atom<>("survey") );
                updateColumns.put("survey_interval_ms", new Atom<>(0) );
                updateColumns.put("survey_type", new Atom<>("on-chan") );
                //updateColumns.put("threshold", thresholds );
                
                
                row = new Row(updateColumns );
                operations.add(new Insert(wifiStatsConfigDbTable, row ));
                //
            }
            
            if(!provisionedWifiStatsConfigs.containsKey("2.4G_client_null")) {    
                //
                updateColumns = new HashMap<>();
                //updateColumns.put("channel_list", channels );
                updateColumns.put("radio_type", new Atom<>("2.4G") );
                updateColumns.put("reporting_interval", new Atom<>(60) );
                updateColumns.put("sampling_interval", new Atom<>(10) );
                updateColumns.put("stats_type", new Atom<>("client") );
                //updateColumns.put("survey_interval_ms", new Atom<>(0) );
                //updateColumns.put("survey_type", new Atom<>("on-chan") );
                //updateColumns.put("threshold", thresholds );
                
                
                row = new Row(updateColumns );
                operations.add(new Insert(wifiStatsConfigDbTable, row ));
                //
            }
            
            if(!provisionedWifiStatsConfigs.containsKey("2.4G_neighbor_on-chan")) {
                //
                updateColumns = new HashMap<>();
                //updateColumns.put("channel_list", channels );
                updateColumns.put("radio_type", new Atom<>("2.4G") );
                updateColumns.put("reporting_interval", new Atom<>(60) );
                updateColumns.put("sampling_interval", new Atom<>(0) );
                updateColumns.put("stats_type", new Atom<>("neighbor") );
                updateColumns.put("survey_interval_ms", new Atom<>(0) );
                updateColumns.put("survey_type", new Atom<>("on-chan") );
                //updateColumns.put("threshold", thresholds );
                
                
                row = new Row(updateColumns );
                operations.add(new Insert(wifiStatsConfigDbTable, row ));
                //
            }

            if(!provisionedWifiStatsConfigs.containsKey("5GU_survey_on-chan")) {
                //
                updateColumns = new HashMap<>();
                //updateColumns.put("channel_list", channels );
                updateColumns.put("radio_type", new Atom<>("5GU") );
                updateColumns.put("reporting_interval", new Atom<>(60) );
                updateColumns.put("sampling_interval", new Atom<>(10) );
                updateColumns.put("stats_type", new Atom<>("survey") );
                updateColumns.put("survey_interval_ms", new Atom<>(0) );
                updateColumns.put("survey_type", new Atom<>("on-chan") );
                //updateColumns.put("threshold", thresholds );
                
                
                row = new Row(updateColumns );
                operations.add(new Insert(wifiStatsConfigDbTable, row ));
                //
            }
            
            if(!provisionedWifiStatsConfigs.containsKey("5GL_neighbor_on-chan")) {
                //
                updateColumns = new HashMap<>();
                //updateColumns.put("channel_list", channels );
                updateColumns.put("radio_type", new Atom<>("5GL") );
                updateColumns.put("reporting_interval", new Atom<>(60) );
                updateColumns.put("sampling_interval", new Atom<>(0) );
                updateColumns.put("stats_type", new Atom<>("neighbor") );
                updateColumns.put("survey_interval_ms", new Atom<>(0) );
                updateColumns.put("survey_type", new Atom<>("on-chan") );
                //updateColumns.put("threshold", thresholds );
                
                
                row = new Row(updateColumns );
                operations.add(new Insert(wifiStatsConfigDbTable, row ));
                //
            }
            
            if(!operations.isEmpty()) {
                CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
                OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
                
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Updated {}:", wifiStatsConfigDbTable);
                    
                    for(OperationResult res : result) {
                        LOG.debug("Op Result {}", res);
                    }
                }
            }
            
        } catch(OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }        
    }

    public String changeRedirectorAddress(OvsdbClient ovsdbClient, String apId, String newRedirectorAddress) {
        try {
            List<Operation> operations = new ArrayList<>();            
            Map<String, Value> updateColumns = new HashMap<>();
            
            updateColumns.put("redirector_addr", new Atom<>(newRedirectorAddress));
            
            Row row = new Row(updateColumns );
            operations.add(new Update(awlanNodeDbTable, row ));
            
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Updated {} redirector_addr = {}", awlanNodeDbTable, newRedirectorAddress);
                
                for(OperationResult res : result) {
                    LOG.debug("Op Result {}", res);
                }
            }
            
        } catch(OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        return newRedirectorAddress;
    }
}
