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

import com.telecominfraproject.wlan.core.model.equipment.AutoOrManualValue;
import com.telecominfraproject.wlan.core.model.equipment.RadioBestApSettings;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.core.model.equipment.SourceType;
import com.telecominfraproject.wlan.equipment.models.ApElementConfiguration;
import com.telecominfraproject.wlan.equipment.models.CellSizeAttributes;
import com.telecominfraproject.wlan.equipment.models.ElementRadioConfiguration;
import com.telecominfraproject.wlan.equipment.models.ManagementRate;
import com.telecominfraproject.wlan.equipment.models.MulticastRate;
import com.telecominfraproject.wlan.equipment.models.RadioConfiguration;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.opensync.util.OvsdbToWlanCloudTypeMappingUtility;
import com.telecominfraproject.wlan.profile.rf.models.RfConfiguration;
import com.telecominfraproject.wlan.profile.rf.models.RfElementConfiguration;
import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.protocol.operation.Delete;
import com.vmware.ovsdb.protocol.operation.Insert;
import com.vmware.ovsdb.protocol.operation.Operation;
import com.vmware.ovsdb.protocol.operation.Update;
import com.vmware.ovsdb.protocol.operation.notation.Atom;
import com.vmware.ovsdb.protocol.operation.notation.Condition;
import com.vmware.ovsdb.protocol.operation.notation.Function;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Value;
import com.vmware.ovsdb.protocol.operation.result.ErrorResult;
import com.vmware.ovsdb.protocol.operation.result.InsertResult;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.protocol.operation.result.UpdateResult;
import com.vmware.ovsdb.protocol.schema.DatabaseSchema;
import com.vmware.ovsdb.service.OvsdbClient;

@Component
public class OvsdbRrmConfig extends OvsdbDaoBase {
    void configureWifiRrm(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig) {

        ApElementConfiguration apElementConfig = (ApElementConfiguration) opensyncApConfig.getCustomerEquipment()
                .getDetails();
        RfConfiguration rfConfig = (RfConfiguration) opensyncApConfig.getRfProfile().getDetails();
        for (RadioType radioType : apElementConfig.getRadioMap().keySet()) {
            String freqBand = null;
            if (radioType == RadioType.is2dot4GHz) {
                freqBand = "2.4G";
            } else if (radioType == RadioType.is5GHzL) {
                freqBand = "5GL";
            } else if (radioType == RadioType.is5GHzU) {
                freqBand = "5GU";
            } else if (radioType == RadioType.is5GHz) {
                freqBand = "5G";
            }

            if (rfConfig == null) {
                continue;
            }

            ElementRadioConfiguration elementRadioConfig = apElementConfig.getRadioMap().get(radioType);
            RfElementConfiguration rfElementConfig = rfConfig.getRfConfig(radioType);
            if (elementRadioConfig == null || rfElementConfig == null) {
            	continue; // don't have a radio of this kind in the map
            }
            
            boolean autoChannelSelection = rfElementConfig.getAutoChannelSelection();
            int backupChannel = elementRadioConfig.getActiveBackupChannel(autoChannelSelection);
            LOG.debug("configureWifiRadios autoChannelSelection {} activeBackupChannel {}",
                    autoChannelSelection, backupChannel);
            
            AutoOrManualValue probeResponseThresholdDb = null;
            AutoOrManualValue clientDisconnectThresholdDb = null;
            
            if (elementRadioConfig.getProbeResponseThresholdDb() != null) {
	            probeResponseThresholdDb = getSourcedValue(elementRadioConfig.getProbeResponseThresholdDb().getSource(),
	                    rfElementConfig.getProbeResponseThresholdDb(),
	                    elementRadioConfig.getProbeResponseThresholdDb().getValue());
            }
            
            if (elementRadioConfig.getClientDisconnectThresholdDb() != null) {
	            clientDisconnectThresholdDb = getSourcedValue(
	                    elementRadioConfig.getClientDisconnectThresholdDb().getSource(),
	                    rfElementConfig.getClientDisconnectThresholdDb(),
	                    elementRadioConfig.getClientDisconnectThresholdDb().getValue());
            }

            RadioConfiguration radioConfig = apElementConfig.getAdvancedRadioMap().get(radioType);
            MulticastRate multicastRate = null;
            ManagementRate managementRate = null;
            RadioBestApSettings bestApSettings = null;
            if (radioConfig != null) {
            	if (radioConfig.getMulticastRate() != null) {
            		multicastRate = radioConfig.getMulticastRate().getSource() == SourceType.profile
                        ? rfElementConfig.getMulticastRate()
                        : radioConfig.getMulticastRate().getValue();
            	}
            	
            	if (radioConfig.getManagementRate() != null) {
                	managementRate = radioConfig.getManagementRate().getSource() == SourceType.profile
                        ? rfElementConfig.getManagementRate()
                        : radioConfig.getManagementRate().getValue();
            	}
            	
            	if (radioConfig.getBestApSettings() != null) {
                	bestApSettings = radioConfig.getBestApSettings().getSource() == SourceType.profile
                        ? rfElementConfig.getBestApSettings()
                        : radioConfig.getBestApSettings().getValue();
            	}
            }

            if (freqBand != null) {
                try {
                    configureWifiRrm(ovsdbClient, freqBand, backupChannel, probeResponseThresholdDb, 
                    		clientDisconnectThresholdDb, managementRate, bestApSettings, multicastRate);
                } catch (OvsdbClientException e) {
                    LOG.error("configureRrm failed with OvsdbClient exception.", e);
                    throw new RuntimeException(e);
                } catch (TimeoutException e) {
                    LOG.error("configureRrm failed with Timeout.", e);
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    LOG.error("configureRrm excecution failed.", e);
                } catch (InterruptedException e) {
                    LOG.error("configureRrm interrupted.", e);
                    throw new RuntimeException(e);
                }
            }
        }
    }

    void configureWifiRrm(OvsdbClient ovsdbClient, String freqBand, int backupChannel,
            AutoOrManualValue probeResponseThreshold, AutoOrManualValue clientDisconnectThreshold,
            ManagementRate managementRate, RadioBestApSettings bestApSettings, MulticastRate multicastRate)
                    throws OvsdbClientException, TimeoutException, ExecutionException, InterruptedException {

        CompletableFuture<DatabaseSchema> cfDatabaseSchema = ovsdbClient.getSchema(ovsdbName);
        DatabaseSchema databaseSchema = cfDatabaseSchema.get();
        Set<String> columnNames = databaseSchema.getTables().get(wifiRrmConfigDbTable).getColumns().keySet();

        List<Operation> operations = new ArrayList<>();
        Map<String, Value> updateColumns = new HashMap<>();

        updateColumns.put("freq_band", new Atom<>(freqBand));
        updateColumns.put("backup_channel", new Atom<>(backupChannel));
        if (columnNames.contains("freq_band")) updateColumns.put("freq_band", new Atom<>(freqBand));
        if (columnNames.contains("backup_channel")) updateColumns.put("backup_channel", new Atom<>(backupChannel));
        if (columnNames.contains("mcast_rate")) {
            if (multicastRate == null || multicastRate == MulticastRate.auto) {
                updateColumns.put("mcast_rate", new Atom<>(0));
            } else {
                updateColumns.put("mcast_rate", new Atom<>(managementRate.getId()));
            }
        }
        if (columnNames.contains("probe_resp_threshold")) {
            if (probeResponseThreshold == null || probeResponseThreshold.isAuto()) {
                updateColumns.put("probe_resp_threshold", new com.vmware.ovsdb.protocol.operation.notation.Set());
            } else {
                updateColumns.put("probe_resp_threshold", new Atom<>(probeResponseThreshold.getValue()));
            }
        }
        if (columnNames.contains("client_disconnect_threshold")) {
            if (clientDisconnectThreshold == null || clientDisconnectThreshold.isAuto()) {
                updateColumns.put("client_disconnect_threshold", new com.vmware.ovsdb.protocol.operation.notation.Set());
            } else {
                updateColumns.put("client_disconnect_threshold", new Atom<>(clientDisconnectThreshold.getValue()));
            }
        }
        if (columnNames.contains("beacon_rate")) {
            if (managementRate == null || managementRate == ManagementRate.auto) {
                updateColumns.put("beacon_rate", new Atom<>(0));
            } else {
                updateColumns.put("beacon_rate", new Atom<>(managementRate.getId() * 10));
            }
        }
        if (columnNames.contains("min_load") && columnNames.contains("snr_percentage_drop")) {
            if (bestApSettings == null) {
                updateColumns.put("min_load", new com.vmware.ovsdb.protocol.operation.notation.Set());
                updateColumns.put("snr_percentage_drop", new com.vmware.ovsdb.protocol.operation.notation.Set());
            } else {
                if (bestApSettings.getDropInSnrPercentage() == null) {
                    updateColumns.put("snr_percentage_drop", new com.vmware.ovsdb.protocol.operation.notation.Set());
                } else {
                    updateColumns.put("snr_percentage_drop", new Atom<>(bestApSettings.getDropInSnrPercentage()));
                }
                if (bestApSettings.getMinLoadFactor() == null) {
                    updateColumns.put("min_load", new com.vmware.ovsdb.protocol.operation.notation.Set());
                } else {
                    updateColumns.put("min_load", new Atom<>(bestApSettings.getMinLoadFactor()));
                }
            }
        }
        Row row = new Row(updateColumns);
        operations.add(new Insert(wifiRrmConfigDbTable, row));

        CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
        OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

        LOG.debug("Provisioned rrm config with multicastRate {} Mbps for {}", multicastRate, freqBand);

        for (OperationResult res : result) {
            LOG.debug("Op Result {}", res);

            if (res instanceof InsertResult) {
                LOG.info("configureWifiRrm insert new row result {}", (res));
                // for insert, make sure it is actually in the table
                confirmRowExistsInTable(ovsdbClient, ((InsertResult) res).getUuid(), wifiRrmConfigDbTable);
            } else if (res instanceof ErrorResult) {
                LOG.error("configureWifiRrm error {}", (res));
                throw new RuntimeException("configureWifiRrm " + ((ErrorResult) res).getError() + " " + ((ErrorResult) res).getDetails());
            }
        }
    }

    AutoOrManualValue getSourcedValue(SourceType source, int profileValue, int equipmentValue) {
        if (source == SourceType.profile) {
            return AutoOrManualValue.createManualInstance(profileValue);
        } else if (source == SourceType.auto) {
            return AutoOrManualValue.createAutomaticInstance(equipmentValue);
        }
        return AutoOrManualValue.createManualInstance(equipmentValue);
    }

    void processNewChannelsRequest(OvsdbClient ovsdbClient, Map<RadioType, Integer> backupChannelMap,
            Map<RadioType, Integer> primaryChannelMap) {

        LOG.info("OvsdbDao::processNewChannelsRequest backup {} primary {}", backupChannelMap, primaryChannelMap);
        try {
            List<Operation> operations = new ArrayList<>();

            backupChannelMap.entrySet().stream().forEach(c -> {
                String freqBand = OvsdbToWlanCloudTypeMappingUtility.getOvsdbRadioFreqBandForRadioType(c.getKey());
                List<Condition> conditions = new ArrayList<>();
                conditions.add(new Condition("freq_band", Function.EQUALS, new Atom<>(freqBand)));
                Map<String, Value> updateColumns = new HashMap<>();
                updateColumns.put("backup_channel", new Atom<>(c.getValue()));
                Row row = new Row(updateColumns);
                operations.add(new Update(wifiRrmConfigDbTable, conditions, row));
            });

            primaryChannelMap.entrySet().stream().forEach(c -> {
                String freqBand = OvsdbToWlanCloudTypeMappingUtility.getOvsdbRadioFreqBandForRadioType(c.getKey());
                List<Condition> conditions = new ArrayList<>();
                conditions.add(new Condition("freq_band", Function.EQUALS, new Atom<>(freqBand)));
                Map<String, Value> updateColumns = new HashMap<>();
                updateColumns.put("channel", new Atom<>(c.getValue()));
                Row row = new Row(updateColumns);
                operations.add(new Update(wifiRadioConfigDbTable, conditions, row));
            });

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            for (OperationResult res : result) {
                LOG.info("Op Result {}", res);
            }

            LOG.info("Updated ovsdb config for primary and backup channels.");
        } catch (ExecutionException e) {
            LOG.error("Error in processNewChannelsRequest", e);
        } catch (OvsdbClientException | TimeoutException | InterruptedException e) {
            LOG.error("Error in processNewChannelsRequest", e);
            throw new RuntimeException(e);
        }

    }
    
    void processCellSizeAttributesRequest(OvsdbClient ovsdbClient, Map<RadioType, CellSizeAttributes> cellSizeAttributesMap) {

        LOG.info("OvsdbDao::processCellSizeAttributesRequest cellSizeAttributes {}", cellSizeAttributesMap);
        try {
            List<Operation> operations = new ArrayList<>();

            cellSizeAttributesMap.entrySet().stream().forEach(c -> {
                String freqBand = OvsdbToWlanCloudTypeMappingUtility.getOvsdbRadioFreqBandForRadioType(c.getKey());
                List<Condition> conditions = new ArrayList<>();
                conditions.add(new Condition("freq_band", Function.EQUALS, new Atom<>(freqBand)));
                Map<String, Value> updateRrmColumns = new HashMap<>();
                
                CellSizeAttributes cellSizeAttributes = c.getValue();
                MulticastRate multicastRate = cellSizeAttributes.getMulticastRate();
                if (multicastRate == null || multicastRate == MulticastRate.auto) {
                    updateRrmColumns.put("mcast_rate", new Atom<>(0));
                } else {
                    updateRrmColumns.put("mcast_rate", new Atom<>(multicastRate.getId()));
                }

                Integer probeResponseThreshold = cellSizeAttributes.getProbeResponseThresholdDb();
                if (probeResponseThreshold == null) {
                    updateRrmColumns.put("probe_resp_threshold", new com.vmware.ovsdb.protocol.operation.notation.Set());
                } else {
                    updateRrmColumns.put("probe_resp_threshold", new Atom<>(probeResponseThreshold.intValue()));
                }
                
                Integer clientDisconnectThreshold = cellSizeAttributes.getClientDisconnectThresholdDb();
                if (clientDisconnectThreshold == null) {
                    updateRrmColumns.put("client_disconnect_threshold", new com.vmware.ovsdb.protocol.operation.notation.Set());
                } else {
                    updateRrmColumns.put("client_disconnect_threshold", new Atom<>(clientDisconnectThreshold.intValue()));
                }

                ManagementRate managementRate = cellSizeAttributes.getManagementRate();
                if (managementRate == null || managementRate == ManagementRate.auto) {
                    updateRrmColumns.put("beacon_rate", new Atom<>(0));
                } else {
                    updateRrmColumns.put("beacon_rate", new Atom<>(managementRate.getId() * 10));
                }
                
                Row rowRrm = new Row(updateRrmColumns);
                operations.add(new Update(wifiRrmConfigDbTable, conditions, rowRrm));
                
                Map<String, Value> updateRadioColumns = new HashMap<>();
                Integer txPower = cellSizeAttributes.getEirpTxPowerDb();
                if (txPower > 0) {
                    updateRadioColumns.put("tx_power", new Atom<>(txPower));
                } else {
                    updateRadioColumns.put("tx_power", new com.vmware.ovsdb.protocol.operation.notation.Set());
                }
                Row rowRadio = new Row(updateRadioColumns);
                operations.add(new Update(wifiRadioConfigDbTable, conditions, rowRadio));
            });

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            for (OperationResult res : result) {
                LOG.info("Op Result {}", res);
                
                if (res instanceof InsertResult) {
                    LOG.info("processCellSizeAttributesRequest insert new row result {}", (res));
                    // for insert, make sure it is actually in the table
                    confirmRowExistsInTable(ovsdbClient, ((InsertResult) res).getUuid(), wifiRrmConfigDbTable);
                } else if (res instanceof ErrorResult) {
                    LOG.error("processCellSizeAttributesRequest error {}", (res));
                    throw new RuntimeException("processCellSizeAttributesRequest " + ((ErrorResult) res).getError() +
                            " " + ((ErrorResult) res).getDetails());
                }
            }

            LOG.info("Updated ovsdb config for cell size attributes.");
        } catch (ExecutionException e) {
            LOG.error("Error in processCellSizeAttributesRequest", e);
        } catch (OvsdbClientException | TimeoutException | InterruptedException e) {
            LOG.error("Error in processCellSizeAttributesRequest", e);
            throw new RuntimeException(e);
        }

    }

    void removeWifiRrm(OvsdbClient ovsdbClient) {
        try {
            List<Operation> operations = new ArrayList<>();

            operations.add(new Delete(wifiRrmConfigDbTable));

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            LOG.info("Removed rrm from {}:", wifiRrmConfigDbTable);

            for (OperationResult res : result) {
                if (res instanceof UpdateResult) {
                    LOG.info("removeWifiRrm result {}", res);
                }
            }

        } catch (ExecutionException | OvsdbClientException | TimeoutException | InterruptedException e) {
            LOG.error("Error in removeRrm", e);
            throw new RuntimeException(e);
        }
    }
}
