package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.WifiRadioConfigInfo;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.WifiStatsConfigInfo;
import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.protocol.operation.Delete;
import com.vmware.ovsdb.protocol.operation.Insert;
import com.vmware.ovsdb.protocol.operation.Operation;
import com.vmware.ovsdb.protocol.operation.Select;
import com.vmware.ovsdb.protocol.operation.notation.Atom;
import com.vmware.ovsdb.protocol.operation.notation.Condition;
import com.vmware.ovsdb.protocol.operation.notation.Function;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Value;
import com.vmware.ovsdb.protocol.operation.result.ErrorResult;
import com.vmware.ovsdb.protocol.operation.result.InsertResult;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.protocol.operation.result.SelectResult;
import com.vmware.ovsdb.protocol.operation.result.UpdateResult;
import com.vmware.ovsdb.service.OvsdbClient;

@Component
public class OvsdbStatsConfig extends OvsdbDaoBase {

    @Autowired
    OvsdbGet ovsdbGet;

    void configureStats(OvsdbClient ovsdbClient) {

        try {
            List<Operation> operations = new ArrayList<>();
            Map<String, Integer> thresholdMap = new HashMap<>();
            thresholdMap.put("max_delay", 600);
            thresholdMap.put("util", 25);

            @SuppressWarnings("unchecked")
            com.vmware.ovsdb.protocol.operation.notation.Map<String, Integer> thresholds = com.vmware.ovsdb.protocol.operation.notation.Map
                    .of(thresholdMap);

            Map<String, WifiRadioConfigInfo> radioConfigs = ovsdbGet.getProvisionedWifiRadioConfigs(ovsdbClient);

            provisionWifiStatsConfigSurvey(ovsdbGet.getAllowedChannels(ovsdbClient), radioConfigs,
                    ovsdbGet.getProvisionedWifiStatsConfigs(ovsdbClient), operations, thresholds);

            provisionWifiStatsConfigNeighbor(ovsdbGet.getAllowedChannels(ovsdbClient), radioConfigs,
                    ovsdbGet.getProvisionedWifiStatsConfigs(ovsdbClient), operations);

            provisionWifiStatsConfigClient(radioConfigs, ovsdbGet.getProvisionedWifiStatsConfigs(ovsdbClient),
                    operations);
            if (!operations.isEmpty()) {
                LOG.debug("Sending batch of operations : {} ", operations);

                CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
                OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Updated {}:", wifiStatsConfigDbTable);

                    for (OperationResult res : result) {
                        LOG.debug("Op Result {}", res);
                    }
                }
            }
            provisionVideoVoiceStats(ovsdbClient);
        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    void configureStatsFromProfile(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig) {
        // TODO: this will be refactored when the opensync profile for stats is
        // re-worked
        configureStats(ovsdbClient);

    }

    /**
     * @param ovsdbClient
     *
     */
    void enableNetworkProbeForSyntheticClient(OvsdbClient ovsdbClient) {
        LOG.debug("Enable network_probe service_metrics_collection_config for synthetic client");

        try {
            List<Operation> operations = new ArrayList<>();
            Map<String, Value> updateColumns = new HashMap<>();

            updateColumns.put("reporting_interval", new Atom<>(defaultReportingIntervalSeconds));
            updateColumns.put("radio_type", new Atom<>("2.4G"));
            updateColumns.put("stats_type", new Atom<>("network_probe"));

            Row row = new Row(updateColumns);
            operations.add(new Insert(wifiStatsConfigDbTable, row));

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            if (LOG.isDebugEnabled()) {

                for (OperationResult res : result) {
                    if (res instanceof InsertResult) {
                        LOG.info("enableNetworkProbeForSyntheticClient insert new row result {}", (res));
                        // for insert, make sure it is actually in the table
                        confirmRowExistsInTable(ovsdbClient, ((InsertResult) res).getUuid(), wifiStatsConfigDbTable);
                    } else if (res instanceof ErrorResult) {
                        LOG.error("Could not update {}:", wifiStatsConfigDbTable);
                        LOG.error("Error: {} Details: {}", ((ErrorResult) res).getError(),
                                ((ErrorResult) res).getDetails());
                    } else {
                        LOG.debug("Updated {}:", wifiStatsConfigDbTable);
                        LOG.debug("Op Result {}", res);
                    }
                }
            }

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * @param ovsdbClient
     * @return value of reporting_interval column for the stats_type=device from
     *         the Wifi_Stats_Config table. If value is not provisioned then
     *         return -1.
     */
    long getDeviceStatsReportingInterval(OvsdbClient ovsdbClient) {
        long ret = -1;
        try {
            List<Operation> operations = new ArrayList<>();
            List<Condition> conditions = new ArrayList<>();
            List<String> columns = new ArrayList<>();

            columns.add("reporting_interval");
            columns.add("stats_type");
            columns.add("radio_type");

            conditions.add(new Condition("stats_type", Function.EQUALS, new Atom<>("device")));

            operations.add(new Select(wifiStatsConfigDbTable, conditions, columns));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Select from {}:", wifiStatsConfigDbTable);

                for (OperationResult res : result) {
                    LOG.debug("Op Result {}", res);
                }
            }

            Row row = null;
            if ((result != null) && (result.length > 0) && !((SelectResult) result[0]).getRows().isEmpty()) {
                row = ((SelectResult) result[0]).getRows().iterator().next();
                ret = row.getIntegerColumn("reporting_interval");
                LOG.info("Stats collection for stats_type=device is already configured with reporting_interval = {}",
                        ret);
            }

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return ret;
    }

    /**
     * @param ovsdbClient
     *
     */
    void provisionVideoVoiceStats(OvsdbClient ovsdbClient) {
        LOG.debug("Enable video_voice_report");

        try {
            List<Operation> operations = new ArrayList<>();
            Map<String, Value> rowColumns = new HashMap<>();
            rowColumns.put("radio_type", new Atom<>("2.4G"));
            rowColumns.put("reporting_interval", new Atom<>(defaultReportingIntervalSeconds));
            rowColumns.put("report_type", new Atom<>("raw"));
            rowColumns.put("sampling_interval", new Atom<>(10));
            rowColumns.put("stats_type", new Atom<>("video_voice"));
            rowColumns.put("survey_interval_ms", new Atom<>(65));
            Row row = new Row(rowColumns);

            operations.add(new Insert(wifiStatsConfigDbTable, row));

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            for (OperationResult res : result) {
                if (res instanceof InsertResult) {
                    LOG.info("provisionVideoVoiceStats insert new row result {}", (res));
                    // for insert, make sure it is actually in the table
                    confirmRowExistsInTable(ovsdbClient, ((InsertResult) res).getUuid(), wifiStatsConfigDbTable);
                } else if (res instanceof UpdateResult) {
                    LOG.info("provisionVideoVoiceStats update row result {}", (res));

                } else if (res instanceof ErrorResult) {
                    LOG.error("Could not update {}:", wifiStatsConfigDbTable);
                    LOG.error("Error: {} Details: {}", ((ErrorResult) res).getError(),
                            ((ErrorResult) res).getDetails());
                } else {
                    LOG.debug("Updated {}:", wifiStatsConfigDbTable);
                    LOG.debug("Op Result {}", res);
                }
            }

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    void provisionWifiStatsConfigClient(Map<String, WifiRadioConfigInfo> radioConfigs,
            Map<String, WifiStatsConfigInfo> provisionedWifiStatsConfigs, List<Operation> operations) {

        radioConfigs.values().stream().forEach(new Consumer<WifiRadioConfigInfo>() {

            @Override
            public void accept(WifiRadioConfigInfo rc) {
                if (!provisionedWifiStatsConfigs.containsKey(rc.freqBand + "_client")) {
                    //
                    Map<String, Value> rowColumns = new HashMap<>();
                    rowColumns.put("radio_type", new Atom<>(rc.freqBand));
                    rowColumns.put("reporting_interval", new Atom<>(defaultReportingIntervalSeconds));
                    rowColumns.put("report_type", new Atom<>("raw"));
                    rowColumns.put("sampling_interval", new Atom<>(10));
                    rowColumns.put("stats_type", new Atom<>("client"));
                    Row updateRow = new Row(rowColumns);
                    operations.add(new Insert(wifiStatsConfigDbTable, updateRow));

                }
            }
        });

    }


    void provisionWifiStatsConfigNeighbor(Map<String, Set<Integer>> allowedChannels,
            Map<String, WifiRadioConfigInfo> radioConfigs, Map<String, WifiStatsConfigInfo> provisionedWifiStatsConfigs,
            List<Operation> operations) {

        radioConfigs.values().stream().forEach(new Consumer<WifiRadioConfigInfo>() {

            @Override
            public void accept(WifiRadioConfigInfo rc) {
                if (!provisionedWifiStatsConfigs.containsKey(rc.freqBand + "_neighbor_off-chan")) {
                    //
                    Map<String, Value> rowColumns = new HashMap<>();
                    com.vmware.ovsdb.protocol.operation.notation.Set channels = com.vmware.ovsdb.protocol.operation.notation.Set
                            .of(allowedChannels.get(rc.freqBand));
                    if (channels == null) {
                        channels = com.vmware.ovsdb.protocol.operation.notation.Set.of(Collections.emptySet());
                    }
                    rowColumns.put("channel_list", channels);

                    rowColumns.put("radio_type", new Atom<>(rc.freqBand));
                    rowColumns.put("reporting_interval", new Atom<>(defaultOffChannelReportingIntervalSeconds));
                    rowColumns.put("stats_type", new Atom<>("neighbor"));
                    rowColumns.put("survey_type", new Atom<>("off-chan"));
                    rowColumns.put("survey_interval_ms", new Atom<>(10));

                    Row updateRow = new Row(rowColumns);
                    operations.add(new Insert(wifiStatsConfigDbTable, updateRow));

                }
            }
        });

        radioConfigs.values().stream().forEach(new Consumer<WifiRadioConfigInfo>() {

            @Override
            public void accept(WifiRadioConfigInfo rc) {
                if (!provisionedWifiStatsConfigs.containsKey(rc.freqBand + "_neighbor_on-chan")) {
                    //
                    Map<String, Value> rowColumns = new HashMap<>();
                    rowColumns.put("radio_type", new Atom<>(rc.freqBand));
                    rowColumns.put("reporting_interval", new Atom<>(defaultReportingIntervalSeconds));
                    rowColumns.put("stats_type", new Atom<>("neighbor"));
                    rowColumns.put("survey_type", new Atom<>("on-chan"));
                    rowColumns.put("survey_interval_ms", new Atom<>(0));

                    Row updateRow = new Row(rowColumns);
                    operations.add(new Insert(wifiStatsConfigDbTable, updateRow));

                }
            }
        });

    }

    void provisionWifiStatsConfigSurvey(Map<String, Set<Integer>> allowedChannels,
            Map<String, WifiRadioConfigInfo> radioConfigs, Map<String, WifiStatsConfigInfo> provisionedWifiStatsConfigs,
            List<Operation> operations, com.vmware.ovsdb.protocol.operation.notation.Map<String, Integer> thresholds) {

        radioConfigs.values().stream().forEach(new Consumer<WifiRadioConfigInfo>() {

            @Override
            public void accept(WifiRadioConfigInfo rc) {
                if (!provisionedWifiStatsConfigs.containsKey(rc.freqBand + "_survey_on-chan")) {

                    Map<String, Value> rowColumns = new HashMap<>();
                    rowColumns.put("radio_type", new Atom<>(rc.freqBand));
                    rowColumns.put("reporting_interval", new Atom<>(defaultReportingIntervalSeconds));
                    rowColumns.put("report_type", new Atom<>("raw"));
                    rowColumns.put("sampling_interval", new Atom<>(10));
                    rowColumns.put("stats_type", new Atom<>("survey"));
                    rowColumns.put("survey_interval_ms", new Atom<>(0));
                    rowColumns.put("survey_type", new Atom<>("on-chan"));

                    Row updateRow = new Row(rowColumns);
                    operations.add(new Insert(wifiStatsConfigDbTable, updateRow));

                }
            }
        });

        radioConfigs.values().stream().forEach(new Consumer<WifiRadioConfigInfo>() {

            @Override
            public void accept(WifiRadioConfigInfo rc) {
                if (!provisionedWifiStatsConfigs.containsKey(rc.freqBand + "_survey_off-chan")) {
                    //
                    Map<String, Value> rowColumns = new HashMap<>();
                    com.vmware.ovsdb.protocol.operation.notation.Set channels = com.vmware.ovsdb.protocol.operation.notation.Set
                            .of(allowedChannels.get(rc.freqBand));
                    if (channels == null) {
                        channels = com.vmware.ovsdb.protocol.operation.notation.Set.of(Collections.emptySet());
                    }
                    rowColumns.put("channel_list", channels);

                    rowColumns.put("radio_type", new Atom<>(rc.freqBand));
                    rowColumns.put("reporting_interval", new Atom<>(defaultOffChannelReportingIntervalSeconds));
                    rowColumns.put("report_type", new Atom<>("raw"));
                    rowColumns.put("stats_type", new Atom<>("survey"));
                    rowColumns.put("survey_type", new Atom<>("off-chan"));
                    rowColumns.put("sampling_interval", new Atom<>(0));
                    rowColumns.put("survey_interval_ms", new Atom<>(10));
                    rowColumns.put("threshold", thresholds);
                    Row updateRow = new Row(rowColumns);
                    operations.add(new Insert(wifiStatsConfigDbTable, updateRow));

                }
            }
        });

    }

    void removeAllStatsConfigs(OvsdbClient ovsdbClient) {

        LOG.info("Remove existing Wifi_Stats_Config table entries");
        try {
            List<Operation> operations = new ArrayList<>();

            operations.add(new Delete(wifiStatsConfigDbTable));

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            LOG.info("Removed all existing config from {}:", wifiStatsConfigDbTable);

            for (OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in removeAllStatsConfigs", e);
            throw new RuntimeException(e);
        }

    }

    /**
     * @param ovsdbClient
     * @param value
     *            of reporting_interval column for the stats_type=device from
     *            the Wifi_Stats_Config table. If value is not provisioned then
     *            return -1.
     */
    void updateDeviceStatsReportingInterval(OvsdbClient ovsdbClient, long newValue) {
        try {
            List<Operation> operations = new ArrayList<>();
            Map<String, Value> updateColumns = new HashMap<>();

            // turn on stats collection over MQTT: (reporting_interval is in
            // seconds?)
            // $ ovsh i Wifi_Stats_Config reporting_interval:=10
            // radio_type:="2.4G" stats_type:="device"

            updateColumns.put("reporting_interval", new Atom<>(newValue));
            updateColumns.put("radio_type", new Atom<>("2.4G"));
            updateColumns.put("stats_type", new Atom<>("device"));

            Row row = new Row(updateColumns);
            operations.add(new Insert(wifiStatsConfigDbTable, row));

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            LOG.debug("Updated {}:", wifiStatsConfigDbTable);

            for (OperationResult res : result) {
                LOG.debug("Op Result {}", res);
                if (res instanceof InsertResult) {
                    LOG.info("updateDeviceStatsReportingInterval insert new row result {}", (res));
                    // for insert, make sure it is actually in the table
                    confirmRowExistsInTable(ovsdbClient, ((InsertResult) res).getUuid(), wifiStatsConfigDbTable);
                }
            }

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

}
