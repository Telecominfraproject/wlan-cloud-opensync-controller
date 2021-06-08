
package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import com.telecominfraproject.wlan.core.model.equipment.ChannelBandwidth;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.equipment.models.*;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.WifiRadioConfigInfo;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.WifiVifConfigInfo;
import com.telecominfraproject.wlan.profile.rf.models.RfConfiguration;
import com.telecominfraproject.wlan.profile.rf.models.RfElementConfiguration;
import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.protocol.operation.Operation;
import com.vmware.ovsdb.protocol.operation.Update;
import com.vmware.ovsdb.protocol.operation.notation.*;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.service.OvsdbClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class OvsdbRadioConfig extends OvsdbDaoBase {

    @Autowired
    OvsdbGet ovsdbGet;

    void configureWifiRadios(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncAPConfig) {
        String country = opensyncAPConfig.getCountryCode(); // should be the
        ApElementConfiguration apElementConfiguration = (ApElementConfiguration) opensyncAPConfig.getCustomerEquipment().getDetails();
        RfConfiguration rfConfig = (RfConfiguration) opensyncAPConfig.getRfProfile().getDetails();
        Map<String, WifiRadioConfigInfo> provisionedRadioConfigs = ovsdbGet.getProvisionedWifiRadioConfigs(ovsdbClient);
        Map<String, WifiVifConfigInfo> vifConfigs = ovsdbGet.getProvisionedWifiVifConfigs(ovsdbClient);
        List<Operation> operations = new ArrayList<>();
        for (RadioType radioType : apElementConfiguration.getRadioMap().keySet()) {
            Map<String, String> hwConfig = new HashMap<>();
            ElementRadioConfiguration elementRadioConfig = apElementConfiguration.getRadioMap().get(radioType);
            RfElementConfiguration rfElementConfig = rfConfig.getRfConfig(radioType);
            if (elementRadioConfig == null || rfElementConfig == null) {
                continue;
            }
            boolean autoChannelSelection = rfElementConfig.getAutoChannelSelection();
            int channel = elementRadioConfig.getActiveChannel(autoChannelSelection);
            LOG.debug("configureWifiRadios autoChannelSelection {} activeChannel {} getChannelNumber {} ", autoChannelSelection, channel,
                    elementRadioConfig.getChannelNumber());
            ChannelBandwidth bandwidth = rfElementConfig.getChannelBandwidth();
            String ht_mode = getBandwidth(bandwidth);
            RadioConfiguration radioConfig = apElementConfiguration.getAdvancedRadioMap().get(radioType);
            int beaconInterval = rfElementConfig.getBeaconInterval();
            boolean enabled = radioConfig.getRadioAdminState().equals(StateSetting.enabled);
            boolean autoCellSizeSelection = rfElementConfig.getAutoCellSizeSelection();
            int txPower = 0;
            if (autoCellSizeSelection) {
                if (elementRadioConfig.getEirpTxPower() != null) {
                    txPower = elementRadioConfig.getEirpTxPower().getValue();
                }
            } else {
                txPower = rfElementConfig.getEirpTxPower();
            }
            String hwMode = getHwMode(rfElementConfig);
            String freqBand = getHwConfigAndFreq(radioType, hwConfig);
            String radioName = null;
            for (String key : provisionedRadioConfigs.keySet()) {
                if (provisionedRadioConfigs.get(key).freqBand.equals(freqBand)) {
                    radioName = key;
                    break;
                }
            }
            if (radioName == null)
                continue;
            String ifName = null; // for vifConfigs
            if (radioName.equals(radio0)) {
                ifName = defaultRadio0;
            } else if (radioName.equals(radio1)) {
                ifName = defaultRadio1;
            } else if (radioName.equals(radio2)) {
                ifName = defaultRadio2;
            }
            if (ifName == null)
                continue;
            Set<Uuid> vifUuidsForRadio = new HashSet<>();
            for (String key : vifConfigs.keySet()) {
                if (key.contains(ifName))
                    vifUuidsForRadio.add(vifConfigs.get(key).uuid);
            }
            int mimoMode = MimoMode.none.getId();
            if (rfElementConfig.getMimoMode() != null) {
                mimoMode = rfElementConfig.getMimoMode().getId();
            }
            int maxNumClients = 0;
            if (rfElementConfig.getMaxNumClients() != null) {
                maxNumClients = rfElementConfig.getMaxNumClients();
            }
            try {
                configureWifiRadios(freqBand, channel, hwConfig, country.toUpperCase(), beaconInterval, enabled, hwMode, ht_mode, txPower, mimoMode,
                        vifUuidsForRadio, operations, maxNumClients);
            } catch (OvsdbClientException e) {
                LOG.error("ConfigureWifiRadios failed with OvsdbClient exception.", e);
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                LOG.error("ConfigureWifiRadios failed with Timeout.", e);
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                LOG.error("ConfigureWifiRadios excecution failed.", e);
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                LOG.error("ConfigureWifiRadios interrupted.", e);
                throw new RuntimeException(e);
            }
        }
        try {
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            for (OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }
        } catch (OvsdbClientException | ExecutionException | InterruptedException | TimeoutException e) {
            LOG.error("configureWifiRadios error", e);
            throw new RuntimeException(e);
        }

    }

    String getHwConfigAndFreq(RadioType radioType, Map<String, String> hwConfig) {
        switch (radioType) {
            case is2dot4GHz:
                return "2.4G";
            case is5GHz:
                // 802.11h dfs (Dynamic Frequency Selection) aka military
                // and
                // weather radar
                // avoidance protocol
                // Must not be disabled (by law)
                // NA for 2.4GHz
                hwConfig.put("dfs_enable", "1");
                hwConfig.put("dfs_ignorecac", "0");
                hwConfig.put("dfs_usenol", "1");
                return "5G";
            case is5GHzL:
                // 802.11h dfs (Dynamic Frequency Selection) aka military
                // and
                // weather radar
                // avoidance protocol
                // Must not be disabled (by law)
                // NA for 2.4GHz
                hwConfig.put("dfs_enable", "1");
                hwConfig.put("dfs_ignorecac", "0");
                hwConfig.put("dfs_usenol", "1");
                return "5GL";
            case is5GHzU:
                // 802.11h dfs (Dynamic Frequency Selection) aka military
                // and
                // weather radar
                // avoidance protocol
                // Must not be disabled (by law)
                // NA for 2.4GHz
                hwConfig.put("dfs_enable", "1");
                hwConfig.put("dfs_ignorecac", "0");
                hwConfig.put("dfs_usenol", "1");
                return "5GU";
            default: // don't know this interface
                return null;
        }
    }

    private String getBandwidth(ChannelBandwidth bandwidth) {
        String ht_mode;
        switch (bandwidth) {
            case is20MHz:
                ht_mode = "HT20";
                break;
            case is40MHz:
                ht_mode = "HT40";
                break;
            case is80MHz:
                ht_mode = "HT80";
                break;
            case is160MHz:
                ht_mode = "HT160";
                break;
            case auto:
                ht_mode = "0";
                break;
            default:
                ht_mode = null;
        }
        return ht_mode;
    }

    String getHwMode(RfElementConfiguration rfElementConfig) {
        String hwMode = null;
        switch (rfElementConfig.getRadioMode()) {
            case modeA:
                hwMode = "11a";
                break;
            case modeAB:
                hwMode = "11ab";
                break;
            case modeAC:
                hwMode = "11ac";
                break;
            case modeB:
                hwMode = "11b";
                break;
            case modeG:
                hwMode = "11g";
                break;
            case modeAX:
                hwMode = "11ax";
                break;
            case modeN:
                hwMode = "11n";
                break;
            case auto:
                hwMode = "auto";
                break;
            default:
        }
        return hwMode;
    }

    void configureWifiRadios(String freqBand, int channel, Map<String, String> hwConfig, String country, int beaconInterval, boolean enabled, String hwMode,
            String ht_mode, int txPower, int mimoMode, Set<Uuid> vifUuidsForRadio, List<Operation> operations, int maxNumClients)
            throws OvsdbClientException, TimeoutException, ExecutionException, InterruptedException {
        Map<String, Value> updateColumns = new HashMap<>();
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition("freq_band", Function.EQUALS, new Atom<>(freqBand)));
        updateColumns.put("channel", new Atom<>(channel));
        updateColumns.put("country", new Atom<>(country));
        @SuppressWarnings("unchecked")
        com.vmware.ovsdb.protocol.operation.notation.Map<String, String> hwConfigMap = com.vmware.ovsdb.protocol.operation.notation.Map.of(hwConfig);
        updateColumns.put("hw_config", hwConfigMap);
        updateColumns.put("bcn_int", new Atom<>(beaconInterval));
        updateColumns.put("enabled", new Atom<>(enabled));
        if ((ht_mode != null) && !ht_mode.equals("0")) {
            updateColumns.put("ht_mode", new Atom<>(ht_mode));
        } else {
            updateColumns.put("ht_mode", new com.vmware.ovsdb.protocol.operation.notation.Set());
        }
        if (txPower > 0) {
            updateColumns.put("tx_power", new Atom<>(txPower));
        } else {
            updateColumns.put("tx_power", new com.vmware.ovsdb.protocol.operation.notation.Set());
        }
        if (hwMode != null) {
            updateColumns.put("hw_mode", new Atom<>(hwMode));
        }
        configureCustomOptionsMap(maxNumClients, updateColumns);
        setTxAndRxChainmask(mimoMode, updateColumns);
        if (vifUuidsForRadio.size() > 0) {
            com.vmware.ovsdb.protocol.operation.notation.Set vifConfigUuids = com.vmware.ovsdb.protocol.operation.notation.Set.of(vifUuidsForRadio);
            updateColumns.put("vif_configs", vifConfigUuids);
        }
        Row row = new Row(updateColumns);
        operations.add(new Update(wifiRadioConfigDbTable, conditions, row));
    }

    void configureCustomOptionsMap(int maxNumClients, Map<String, Value> updateColumns) {
        Map<String, String> customOptions = new HashMap<>();
        customOptions.put("max_clients", String.valueOf(maxNumClients));
        @SuppressWarnings("unchecked")
        com.vmware.ovsdb.protocol.operation.notation.Map<String, String> customOptionsMap = com.vmware.ovsdb.protocol.operation.notation.Map.of(customOptions);
        updateColumns.put("custom_options", customOptionsMap);
    }

    void setTxAndRxChainmask(int mimoMode, Map<String, Value> updateColumns) {
        /*
         * Chainmask is a bitmask, so map mimo mode values accordingly
         * Note values 0, 1 remain unchanged
         * 
         * mimoMode bitmask
         * 0       0 
         * 1       1        1
         * 2       3        2
         * 3       7        4
         * 4     15        8
         * 5      31     16
         * 6      63     32
         * 7    127     64
         * 8     255   128     
         */
        switch (mimoMode) {
            case 0: // unchanged
                break;
            case 1: // unchanged
                break;
            case 2:
                mimoMode = 3;
                break;
            case 3:
                mimoMode = 7;
                break;
            case 4:
                mimoMode = 15;
                break;
            case 5:
                mimoMode = 31;
                break;
            case 6:
                mimoMode = 63;
                break;
            case 7:
                mimoMode = 127;
                break;
            case 8:
                mimoMode = 255;
                break;
        }
        updateColumns.put("tx_chainmask", new Atom<>(mimoMode));
        updateColumns.put("rx_chainmask", new Atom<>(mimoMode));
    }

}
