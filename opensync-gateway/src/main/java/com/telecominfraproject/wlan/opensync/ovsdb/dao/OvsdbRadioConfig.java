package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.core.model.equipment.ChannelBandwidth;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.core.model.equipment.SourceType;
import com.telecominfraproject.wlan.equipment.models.ApElementConfiguration;
import com.telecominfraproject.wlan.equipment.models.ElementRadioConfiguration;
import com.telecominfraproject.wlan.equipment.models.MimoMode;
import com.telecominfraproject.wlan.equipment.models.RadioConfiguration;
import com.telecominfraproject.wlan.equipment.models.StateSetting;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.profile.rf.models.RfConfiguration;
import com.telecominfraproject.wlan.profile.rf.models.RfElementConfiguration;
import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.protocol.operation.Mutate;
import com.vmware.ovsdb.protocol.operation.Operation;
import com.vmware.ovsdb.protocol.operation.Select;
import com.vmware.ovsdb.protocol.operation.Update;
import com.vmware.ovsdb.protocol.operation.notation.Atom;
import com.vmware.ovsdb.protocol.operation.notation.Condition;
import com.vmware.ovsdb.protocol.operation.notation.Function;
import com.vmware.ovsdb.protocol.operation.notation.Mutation;
import com.vmware.ovsdb.protocol.operation.notation.Mutator;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;
import com.vmware.ovsdb.protocol.operation.notation.Value;
import com.vmware.ovsdb.protocol.operation.result.ErrorResult;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.protocol.operation.result.SelectResult;
import com.vmware.ovsdb.protocol.operation.result.UpdateResult;
import com.vmware.ovsdb.service.OvsdbClient;

@Component
public class OvsdbRadioConfig extends OvsdbDaoBase {
    void configureWifiRadios(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncAPConfig) {

        String country = opensyncAPConfig.getCountryCode(); // should be the
        // same for all
        // radios on this AP
        // ;-)

        ApElementConfiguration apElementConfiguration = (ApElementConfiguration) opensyncAPConfig.getCustomerEquipment()
                .getDetails();
        RfConfiguration rfConfig = (RfConfiguration) opensyncAPConfig.getRfProfile().getDetails();

        for (RadioType radioType : apElementConfiguration.getRadioMap().keySet()) {
            Map<String, String> hwConfig = new HashMap<>();

            ElementRadioConfiguration elementRadioConfig = apElementConfiguration.getRadioMap().get(radioType);
            RfElementConfiguration rfElementConfig = rfConfig.getRfConfig(radioType);
            boolean autoChannelSelection = rfElementConfig.getAutoChannelSelection();
            int channel = elementRadioConfig.getActiveChannel(autoChannelSelection);
            LOG.debug("configureWifiRadios autoChannelSelection {} activeChannel {} getChannelNumber {} ",
                    autoChannelSelection, channel, elementRadioConfig.getChannelNumber());
            ChannelBandwidth bandwidth = rfElementConfig.getChannelBandwidth();
            String ht_mode = null;
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

            RadioConfiguration radioConfig = apElementConfiguration.getAdvancedRadioMap().get(radioType);
            int beaconInterval = rfElementConfig.getBeaconInterval();
            boolean enabled = radioConfig.getRadioAdminState().equals(StateSetting.enabled);

            int txPower = 0;
            if (elementRadioConfig.getEirpTxPower().getSource() == SourceType.profile) {
                txPower = rfElementConfig.getEirpTxPower();
            } else {
                txPower = elementRadioConfig.getEirpTxPower().getValue();
            }

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
            default:
            }
            String freqBand = null;
            switch (radioType) {
            case is2dot4GHz:
                freqBand = "2.4G";
                break;
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
                freqBand = "5G";

                break;
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
                freqBand = "5GL";

                break;
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
                freqBand = "5GU";

                break;
            default: // don't know this interface
                continue;

            }

            int mimoMode = MimoMode.none.getId();
            if (rfElementConfig.getMimoMode() != null) {
                mimoMode = rfElementConfig.getMimoMode().getId();
            }

            if (freqBand != null) {
                try {
                    configureWifiRadios(ovsdbClient, freqBand, channel, hwConfig, country.toUpperCase(), beaconInterval,
                            enabled, hwMode, ht_mode, txPower, mimoMode);
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

        }

    }

    void configureWifiRadios(OvsdbClient ovsdbClient, String freqBand, int channel, Map<String, String> hwConfig,
            String country, int beaconInterval, boolean enabled, String hwMode, String ht_mode, int txPower,
            int mimoMode) throws OvsdbClientException, TimeoutException, ExecutionException, InterruptedException {

        List<Operation> operations = new ArrayList<>();
        Map<String, Value> updateColumns = new HashMap<>();
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition("freq_band", Function.EQUALS, new Atom<>(freqBand)));

        updateColumns.put("channel", new Atom<>(channel));
        updateColumns.put("country", new Atom<>(country));
        @SuppressWarnings("unchecked")
        com.vmware.ovsdb.protocol.operation.notation.Map<String, String> hwConfigMap = com.vmware.ovsdb.protocol.operation.notation.Map
                .of(hwConfig);
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
        
        setTxAndRxChainmask(ovsdbClient, mimoMode, updateColumns);

        Row row = new Row(updateColumns);
        operations.add(new Update(wifiRadioConfigDbTable, conditions, row));

        CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
        OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

        LOG.debug("Provisioned channel {} for {}", channel, freqBand);

        for (OperationResult res : result) {
            LOG.debug("Op Result {}", res);
        }
    }

    void setTxAndRxChainmask(OvsdbClient ovsdbClient, int mimoMode, Map<String, Value> updateColumns)
            throws InterruptedException, ExecutionException, OvsdbClientException {
        /*
         * Chainmask is a bitmask, so map mimo mode values accordingly
         * Note values 0, 1 remain unchanged 
         * 
         * mimoMode  bitmask 
         *    0         0 
         *    1         1 
         *    2         3 
         *    3         7 
         *    4        15
         */
        if (mimoMode == 2) {mimoMode = 3;}
        else if (mimoMode == 3) {mimoMode = 7;}
        else if (mimoMode == 4) {mimoMode = 15;}
        updateColumns.put("tx_chainmask", new Atom<>(mimoMode));
        updateColumns.put("rx_chainmask", new Atom<>(mimoMode));
        
    }

    /**
     * Update the vif_configs column of the Wifi_Radio_Config ovsdb table for
     * the given freqBand
     *
     * @param ovsdbClient
     * @param ssid
     * @param radioFreqBand
     * @param vifConfigUuid
     * @throws OvsdbClientException
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    void updateVifConfigsSetForRadio(OvsdbClient ovsdbClient, String ssid, String radioFreqBand, Uuid vifConfigUuid)
            throws OvsdbClientException, InterruptedException, ExecutionException, TimeoutException {
        List<Operation> operations = new ArrayList<>();
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition("freq_band", Function.EQUALS, new Atom<>(radioFreqBand)));

        List<Mutation> mutations = new ArrayList<>();
        Mutation mutation = new Mutation("vif_configs", Mutator.INSERT, new Atom<>(vifConfigUuid));
        mutations.add(mutation);
        operations.add(new Mutate(wifiRadioConfigDbTable, conditions, mutations));

        CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
        OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

        for (OperationResult res : result) {
            if (res instanceof UpdateResult) {
                LOG.debug("updateVifConfigsSetForRadio:result {}", res);
            } else if (res instanceof ErrorResult) {
                LOG.error("updateVifConfigsSetForRadio error {}", (res));
                throw new RuntimeException("updateVifConfigsSetForRadio " + ((ErrorResult) res).getError() + " "
                        + ((ErrorResult) res).getDetails());
            }
        }

        // confirm the table was updated with the new Wifi_VIF_Config Uuid
        operations.clear();
        operations.add(new Select(wifiRadioConfigDbTable, conditions));
        fResult = ovsdbClient.transact(ovsdbName, operations);
        result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

        for (OperationResult res : result) {
            if (res instanceof SelectResult) {
                SelectResult selectResult = (SelectResult) res;
                Row row = selectResult.getRows().get(0);
                if (!row.getSetColumn("vif_configs").contains(vifConfigUuid)) {
                    throw new RuntimeException("Wifi_Radio_Config " + row
                            + "vif_configs table was not updated {} for new Wifi_VIF_Config " + vifConfigUuid);
                }
            } else if (res instanceof ErrorResult) {
                LOG.error("updateVifConfigsSetForRadio error {}", (res));
                throw new RuntimeException("updateVifConfigsSetForRadio " + ((ErrorResult) res).getError() + " "
                        + ((ErrorResult) res).getDetails());
            }
        }
        LOG.info("Updated WifiRadioConfig {} for SSID {}:", radioFreqBand, ssid);

    }

}
