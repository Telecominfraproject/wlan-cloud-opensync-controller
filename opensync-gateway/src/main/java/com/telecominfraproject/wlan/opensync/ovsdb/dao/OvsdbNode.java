
package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.opensync.external.integration.models.ConnectNodeInfo;
import com.telecominfraproject.wlan.opensync.util.OvsdbToWlanCloudTypeMappingUtility;
import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.protocol.operation.Operation;
import com.vmware.ovsdb.protocol.operation.Select;
import com.vmware.ovsdb.protocol.operation.Update;
import com.vmware.ovsdb.protocol.operation.notation.Atom;
import com.vmware.ovsdb.protocol.operation.notation.Condition;
import com.vmware.ovsdb.protocol.operation.notation.Function;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Value;
import com.vmware.ovsdb.protocol.operation.result.ErrorResult;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.protocol.operation.result.SelectResult;
import com.vmware.ovsdb.protocol.schema.DatabaseSchema;
import com.vmware.ovsdb.service.OvsdbClient;

@Component
public class OvsdbNode extends OvsdbDaoBase {
	
	@org.springframework.beans.factory.annotation.Value("${tip.wlan.internalHostName:localhost}") 
    private String internalHostName;
	
    String changeRedirectorAddress(OvsdbClient ovsdbClient, String apId, String newRedirectorAddress) {
        try {
            List<Operation> operations = new ArrayList<>();
            Map<String, Value> updateColumns = new HashMap<>();

            updateColumns.put("redirector_addr", new Atom<>(newRedirectorAddress));

            Row row = new Row(updateColumns);
            operations.add(new Update(awlanNodeDbTable, row));

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Updated {} redirector_addr = {}", awlanNodeDbTable, newRedirectorAddress);

                for (OperationResult res : result) {
                    LOG.debug("Op Result {}", res);
                }
            }

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return newRedirectorAddress;
    }

    void fillInLanIpAddressAndMac(OvsdbClient ovsdbClient, ConnectNodeInfo connectNodeInfo, String ifType) {
        try {
            List<Operation> operations = new ArrayList<>();
            List<Condition> conditions = new ArrayList<>();
            List<String> columns = new ArrayList<>();
            // populate macAddress, ipV4Address from Wifi_Inet_State

            columns.add("inet_addr");
            columns.add("hwaddr");
            columns.add("if_type");
            columns.add("if_name");

            conditions.add(new Condition("if_type", Function.EQUALS, new Atom<>(ifType)));
            conditions.add(new Condition("if_name", Function.EQUALS, new Atom<>(defaultLanInterfaceName)));

            operations.add(new Select(wifiInetStateDbTable, conditions, columns));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Select from {}:", wifiInetStateDbTable);

                for (OperationResult res : result) {
                    LOG.debug("Op Result {}", res);
                }
            }

            Row row = null;
            if ((result != null) && (result.length > 0) && (result[0] instanceof SelectResult) && !((SelectResult) result[0]).getRows().isEmpty()) {
                row = ((SelectResult) result[0]).getRows().iterator().next();
                connectNodeInfo.lanIpV4Address = getSingleValueFromSet(row, "inet_addr");
                connectNodeInfo.lanIfName = row.getStringColumn("if_name");
                connectNodeInfo.lanIfType = getSingleValueFromSet(row, "if_type");
                connectNodeInfo.lanMacAddress = getSingleValueFromSet(row, "hwaddr");

            } else if ((result != null) && (result.length > 0) && (result[0] instanceof ErrorResult)) {
                LOG.warn("Error reading from {} table: {}", wifiInetStateDbTable, result[0]);
            }

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    void fillInWanIpAddressAndMac(OvsdbClient ovsdbClient, ConnectNodeInfo connectNodeInfo, String ifType, String ifName) {
        try {
            List<Operation> operations = new ArrayList<>();
            List<Condition> conditions = new ArrayList<>();
            List<String> columns = new ArrayList<>();
            // populate macAddress, ipV4Address from Wifi_Inet_State

            columns.add("inet_addr");
            columns.add("hwaddr");
            columns.add("if_name");
            columns.add("if_type");

            conditions.add(new Condition("if_type", Function.EQUALS, new Atom<>(ifType)));
            conditions.add(new Condition("if_name", Function.EQUALS, new Atom<>(ifName)));

            operations.add(new Select(wifiInetStateDbTable, conditions, columns));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Select from {}:", wifiInetStateDbTable);

                for (OperationResult res : result) {
                    LOG.debug("Op Result {}", res);
                }
            }

            Row row = null;
            if ((result != null) && (result.length > 0) && (result[0] instanceof SelectResult) && !((SelectResult) result[0]).getRows().isEmpty()) {
                row = ((SelectResult) result[0]).getRows().iterator().next();
                connectNodeInfo.ipV4Address = getSingleValueFromSet(row, "inet_addr");
                connectNodeInfo.ifName = row.getStringColumn("if_name");
                connectNodeInfo.ifType = getSingleValueFromSet(row, "if_type");
                connectNodeInfo.macAddress = getSingleValueFromSet(row, "hwaddr");

            }

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    ConnectNodeInfo getConnectNodeInfo(OvsdbClient ovsdbClient) {

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
            columns.add("revision");
            columns.add("version_matrix");
            columns.add("id");

            DatabaseSchema dbSchema = ovsdbClient.getSchema(ovsdbName).get();
            Set<String> keys = dbSchema.getTables().get(awlanNodeDbTable).getColumns().keySet();
            if (keys.containsAll(Set.of("reference_design", "model_description", "manufacturer_url", "manufacturer_name", "manufacturer_date",
                    "certification_region"))) {
                columns.addAll(Set.of("reference_design", "model_description", "manufacturer_url", "manufacturer_name", "manufacturer_date",
                        "certification_region"));
            }

            if (keys.contains("qr_code")) {
                columns.add("qr_code");
            }
            
            operations.add(new Select(awlanNodeDbTable, conditions, columns));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Select from {}:", awlanNodeDbTable);

                for (OperationResult res : result) {
                    LOG.debug("Op Result {}", res);
                }
            }

            Row row = null;
            if ((result != null) && (result.length > 0) && (result[0] instanceof SelectResult) && !((SelectResult) result[0]).getRows().isEmpty()) {
                row = ((SelectResult) result[0]).getRows().iterator().next();
            }

            ret.mqttSettings = row != null ? row.getMapColumn("mqtt_settings") : null;
            ret.versionMatrix = row != null ? row.getMapColumn("version_matrix") : null;
            ret.redirectorAddr = row != null ? row.getStringColumn("redirector_addr") : null;
            ret.managerAddr = row != null ? row.getStringColumn("manager_addr") : null;

            ret.platformVersion = row != null ? row.getStringColumn("platform_version") : null;
            ret.firmwareVersion = row != null ? row.getStringColumn("firmware_version") : null;

            ret.revision = row != null ? row.getStringColumn("revision") : null;

            ret.skuNumber = getSingleValueFromSet(row, "sku_number");
            ret.serialNumber = getSingleValueFromSet(row, "serial_number");
            ret.model = getSingleValueFromSet(row, "model");

            if (keys.containsAll(Set.of("reference_design", "model_description", "manufacturer_url", "manufacturer_name", "manufacturer_date",
                    "certification_region"))) {
                    ret.referenceDesign = row.getStringColumn("reference_design");
                    ret.modelDescription = row.getStringColumn("model_description");
                    ret.manufacturerUrl = row.getStringColumn("manufacturer_url");
                    ret.manufacturerName = row.getStringColumn("manufacturer_name");
                    ret.manufacturerDate =  row.getStringColumn("manufacturer_date");
                    ret.certificationRegion =  row.getStringColumn("certification_region");
            }

            if (keys.contains("qr_code")) {
                ret.qrCode =  row.getMapColumn("qr_code");
            }
            
            // now populate macAddress, ipV4Address from Wifi_Inet_State
            // first look them up for if_name = br-wan
            fillInWanIpAddressAndMac(ovsdbClient, ret, defaultWanInterfaceType, defaultWanInterfaceName);
            if ((ret.ipV4Address == null) || (ret.macAddress == null)) {
                // when not found - look them up for if_name = br-lan
                fillInWanIpAddressAndMac(ovsdbClient, ret, defaultLanInterfaceType, defaultLanInterfaceName);

                if (ret.ipV4Address == null) {
                    throw new RuntimeException("Could not get inet address for Lan and Wan network interfaces. Node is not ready to connect.");
                }
            }
            fillInLanIpAddressAndMac(ovsdbClient, ret, defaultLanInterfaceType);

            fillInRadioInterfaceNamesAndCountry(ovsdbClient, ret);

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        LOG.debug("ConnectNodeInfo created {}", ret);

        return ret;
    }

    void performRedirect(OvsdbClient ovsdbClient, String clientCn) {

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

            operations.add(new Select(awlanNodeDbTable, conditions, columns));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            LOG.debug("Select from AWLAN_Node:");

            String skuNumber = null;
            String serialNumber = null;
            String model = null;
            String firmwareVersion = null;

            Row row = null;
            if ((result != null) && (result.length > 0) && !((SelectResult) result[0]).getRows().isEmpty()) {
                row = ((SelectResult) result[0]).getRows().iterator().next();
                for (OperationResult res : result) {
                    LOG.debug("Op Result {}", res);
                }
            }

            firmwareVersion = row != null ? row.getStringColumn("firmware_version") : null;

            skuNumber = getSingleValueFromSet(row, "sku_number");
            serialNumber = getSingleValueFromSet(row, "serial_number");
            model = getSingleValueFromSet(row, "model");

            LOG.info("Redirecting AP Node: clientCn {} serialNumber {} model {} firmwareVersion {} skuNumber {}", clientCn, serialNumber, model,
                    firmwareVersion, skuNumber);

            // Update table AWLAN_Node - set manager_addr
            operations.clear();
            Map<String, Value> updateColumns = new HashMap<>();

            updateColumns.put("manager_addr", new Atom<>("ssl:" + managerIpAddr + ":" + ovsdbExternalPort));

            row = new Row(updateColumns);
            operations.add(new Update(awlanNodeDbTable, row));

            fResult = ovsdbClient.transact(ovsdbName, operations);
            result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            LOG.debug("Updated AWLAN_Node:");

            for (OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }

            LOG.debug("Redirect Done");
        } catch (ExecutionException | InterruptedException | OvsdbClientException | TimeoutException e) {
            LOG.error("Error when redirecting AP Node", e);
            throw new RuntimeException(e);
        }

    }

    void rebootOrResetAp(OvsdbClient ovsdbClient, String desiredApAction) {
        try {
            LOG.debug("rebootOrResetAp on AP perform {}, setting timer for {} seconds.", desiredApAction, rebootOrResetTimerSeconds);
            List<Operation> operations = new ArrayList<>();
            Map<String, Value> updateColumns = new HashMap<>();
            updateColumns.put("firmware_url", new Atom<>(desiredApAction));
            updateColumns.put("upgrade_timer", new Atom<>(rebootOrResetTimerSeconds));
            Row row = new Row(updateColumns);
            operations.add(new Update(awlanNodeDbTable, row));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);

            OperationResult[] result = fResult.join();
            for (OperationResult r : result) {
                LOG.debug("Op Result {}", r);
            }
        } catch (OvsdbClientException e) {
            LOG.error("Could not trigger {}", desiredApAction, e);
            throw new RuntimeException(e);

        }

    }

    ConnectNodeInfo updateConnectNodeInfoOnConnect(OvsdbClient ovsdbClient, String clientCn, ConnectNodeInfo incomingConnectNodeInfo,
            boolean preventCnAlteration) {
        ConnectNodeInfo ret = incomingConnectNodeInfo.clone();

        try {
            List<Operation> operations = new ArrayList<>();
            Map<String, Value> updateColumns = new HashMap<>();

            // set device_mode = cloud - plume's APs do not use it
            // updateColumns.put("device_mode", new Atom<String>("cloud") );

            // update sku_number if it was empty
            if (( ret.skuNumber == null) || ret.skuNumber.isEmpty()  || ret.skuNumber.equals("unknown") || ret.skuNumber.startsWith("tip.wlan_")) {              
                if ((ret.certificationRegion != null && !ret.certificationRegion.equals("unknown") ) && (ret.model != null && !ret.model.equals("unknown"))) {
                    if (ret.model.endsWith("-" + ret.certificationRegion)) {
                        updateColumns.put("sku_number", new Atom<>("TIP-" + ret.model));
                        ret.skuNumber = "TIP-" + ret.model;
                    } else {
                        updateColumns.put("sku_number", new Atom<>("TIP-" + ret.model + "-" + ret.certificationRegion));
                        ret.skuNumber = "TIP-" + ret.model + "-" + ret.certificationRegion;
                    }
                } else if ((ret.country != null ) && (ret.model != null && !ret.model.equals("unknown"))) {
                    if (ret.model.endsWith("-" + ret.country)) {
                        updateColumns.put("sku_number", new Atom<>("TIP-" + ret.model));
                        ret.skuNumber = "TIP-" + ret.model;
                    } else {
                        updateColumns.put("sku_number", new Atom<>("TIP-" + ret.model + "-" + ret.country));
                        ret.skuNumber = "TIP-" + ret.model + "-" + ret.country;
                    }
                } else if (ret.model != null && !ret.model.equals("unknown")){
                    updateColumns.put("sku_number", new Atom<>("TIP-" + ret.model));
                    ret.skuNumber = "TIP-" + ret.model;
                }
            }

            // Configure the MQTT connection
            // ovsh u AWLAN_Node
            // mqtt_settings:ins:'["map",[["broker","testportal.123wlan.com"],["topics","/ap/dev-ap-0300/opensync"],["qos","0"],["port","1883"],["remote_log","1"]]]'
            Map<String, String> newMqttSettings = new HashMap<>();
            newMqttSettings.put("broker", mqttBrokerAddress);
            String mqttClientName = OvsdbToWlanCloudTypeMappingUtility.getAlteredClientCnIfRequired(clientCn, incomingConnectNodeInfo, preventCnAlteration);
            newMqttSettings.put("topics", "/ap/opensync_mqtt_" + internalHostName + "/" + mqttClientName + "/opensync");
            newMqttSettings.put("port", "" + mqttBrokerExternalPort);
            newMqttSettings.put("compress", "zlib");
            newMqttSettings.put("qos", "0");
            newMqttSettings.put("remote_log", "1");

            if ((ret.mqttSettings == null) || !ret.mqttSettings.equals(newMqttSettings)) {
                @SuppressWarnings("unchecked")
                com.vmware.ovsdb.protocol.operation.notation.Map<String, String> mgttSettings =
                        com.vmware.ovsdb.protocol.operation.notation.Map.of(newMqttSettings);
                ret.mqttSettings = newMqttSettings;
                updateColumns.put("mqtt_settings", mgttSettings);
            }

            if (!updateColumns.isEmpty()) {
                Row row = new Row(updateColumns);
                operations.add(new Update(awlanNodeDbTable, row));

                CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
                OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Updated {}:", awlanNodeDbTable);

                    for (OperationResult res : result) {
                        LOG.debug("Op Result {}", res);
                    }
                }
            }

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return ret;
    }

    public long getConfigVersionFromNode(OvsdbClient ovsdbClient) {
        
        long ret = 0;
        
        Map<String, String> versionMatrix = getVersionMatrixFromNode(ovsdbClient);

        try {
            ret = Long.parseLong(versionMatrix.get(ConnectNodeInfo.CONFIG_VERSION_PROPERTY_NAME));
        } catch(Exception e) {
            //do nothing
        }
        
        LOG.debug("getConfigVersionFromNode {}", ret);
        
        return ret;
    }

    public Map<String, String> getVersionMatrixFromNode(OvsdbClient ovsdbClient) {
        
        Map<String, String> ret = new HashMap<>();
        
        try {
            List<Operation> operations = new ArrayList<>();
            List<Condition> conditions = new ArrayList<>();
            List<String> columns = new ArrayList<>();
            columns.add("version_matrix");
            columns.add("id");

            operations.add(new Select(awlanNodeDbTable, conditions, columns));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Select from {}:", awlanNodeDbTable);

                for (OperationResult res : result) {
                    LOG.debug("Op Result {}", res);
                }
            }

            Row row = null;
            if ((result != null) && (result.length > 0) && (result[0] instanceof SelectResult) && !((SelectResult) result[0]).getRows().isEmpty()) {
                row = ((SelectResult) result[0]).getRows().iterator().next();
            }

            if( row != null ) {
                ret = row.getMapColumn("version_matrix");
            }

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        LOG.debug("getVersionMatrixFromNode {}", ret);
        
        return ret;
    }
    public void updateConfigVersionInNode(OvsdbClient ovsdbClient, long configVersionFromProfiles) {

        try {
            //get original version_matrix map value
            Map<String, String> versionMatrix = getVersionMatrixFromNode(ovsdbClient);

            //update our config version in version_matrix map
            versionMatrix.put(ConnectNodeInfo.CONFIG_VERSION_PROPERTY_NAME, Long.toString(configVersionFromProfiles));
            
            //update the version_matrix column
            List<Operation> operations = new ArrayList<>();
            Map<String, Value> updateColumns = new HashMap<>();
            
            @SuppressWarnings("unchecked")
            com.vmware.ovsdb.protocol.operation.notation.Map<String, String> ovsdbVersionMatrix =
                    com.vmware.ovsdb.protocol.operation.notation.Map.of(versionMatrix);
            updateColumns.put("version_matrix", ovsdbVersionMatrix);
            
            Row row = new Row(updateColumns);
            operations.add(new Update(awlanNodeDbTable, row));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            for (OperationResult r : result) {
                LOG.debug("Op Result {}", r);
            }
    
            LOG.debug("updateConfigVersionInNode {}", configVersionFromProfiles);

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException  e) {
            throw new RuntimeException(e);
        }

    }

}
