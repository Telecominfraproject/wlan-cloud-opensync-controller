

package com.telecominfraproject.wlan.opensync.external.integration.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.alarm.AlarmServiceInterface;
import com.telecominfraproject.wlan.alarm.models.Alarm;
import com.telecominfraproject.wlan.alarm.models.AlarmCode;
import com.telecominfraproject.wlan.alarm.models.AlarmDetails;
import com.telecominfraproject.wlan.alarm.models.AlarmScopeType;
import com.telecominfraproject.wlan.alarm.models.OriginatorType;
import com.telecominfraproject.wlan.cloudeventdispatcher.CloudEventDispatcherInterface;
import com.telecominfraproject.wlan.core.model.equipment.ChannelBandwidth;
import com.telecominfraproject.wlan.core.model.equipment.DetectedAuthMode;
import com.telecominfraproject.wlan.core.model.equipment.MacAddress;
import com.telecominfraproject.wlan.core.model.equipment.NeighborScanPacketType;
import com.telecominfraproject.wlan.core.model.equipment.NetworkType;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.core.model.utils.DecibelUtils;
import com.telecominfraproject.wlan.equipment.EquipmentServiceInterface;
import com.telecominfraproject.wlan.equipment.models.Equipment;
import com.telecominfraproject.wlan.opensync.external.integration.OvsdbSession;
import com.telecominfraproject.wlan.opensync.external.integration.OvsdbSessionMapInterface;
import com.telecominfraproject.wlan.opensync.util.OvsdbToWlanCloudTypeMappingUtility;
import com.telecominfraproject.wlan.profile.ProfileServiceInterface;
import com.telecominfraproject.wlan.profile.models.Profile;
import com.telecominfraproject.wlan.profile.models.ProfileContainer;
import com.telecominfraproject.wlan.profile.models.ProfileType;
import com.telecominfraproject.wlan.profile.rf.models.RfConfiguration;
import com.telecominfraproject.wlan.servicemetric.apnode.models.ApNodeMetrics;
import com.telecominfraproject.wlan.servicemetric.apnode.models.ApPerformance;
import com.telecominfraproject.wlan.servicemetric.apnode.models.DnsProbeMetric;
import com.telecominfraproject.wlan.servicemetric.apnode.models.EthernetLinkState;
import com.telecominfraproject.wlan.servicemetric.apnode.models.NetworkProbeMetrics;
import com.telecominfraproject.wlan.servicemetric.apnode.models.PerProcessUtilization;
import com.telecominfraproject.wlan.servicemetric.apnode.models.RadioStatistics;
import com.telecominfraproject.wlan.servicemetric.apnode.models.RadioUtilization;
import com.telecominfraproject.wlan.servicemetric.apnode.models.StateUpDownError;
import com.telecominfraproject.wlan.servicemetric.apssid.models.ApSsidMetrics;
import com.telecominfraproject.wlan.servicemetric.apssid.models.SsidStatistics;
import com.telecominfraproject.wlan.servicemetric.channelinfo.models.ChannelInfo;
import com.telecominfraproject.wlan.servicemetric.channelinfo.models.ChannelInfoReports;
import com.telecominfraproject.wlan.servicemetric.client.models.ClientMetrics;
import com.telecominfraproject.wlan.servicemetric.models.ServiceMetric;
import com.telecominfraproject.wlan.servicemetric.models.ServiceMetricDataType;
import com.telecominfraproject.wlan.servicemetric.neighbourscan.models.NeighbourReport;
import com.telecominfraproject.wlan.servicemetric.neighbourscan.models.NeighbourScanReports;
import com.telecominfraproject.wlan.status.StatusServiceInterface;
import com.telecominfraproject.wlan.status.equipment.report.models.ActiveBSSID;
import com.telecominfraproject.wlan.status.equipment.report.models.ActiveBSSIDs;
import com.telecominfraproject.wlan.status.equipment.report.models.EquipmentCapacityDetails;
import com.telecominfraproject.wlan.status.equipment.report.models.EquipmentPerRadioUtilizationDetails;
import com.telecominfraproject.wlan.status.equipment.report.models.OperatingSystemPerformance;
import com.telecominfraproject.wlan.status.equipment.report.models.RadioUtilizationReport;
import com.telecominfraproject.wlan.status.models.Status;
import com.telecominfraproject.wlan.status.models.StatusCode;
import com.telecominfraproject.wlan.status.models.StatusDataType;
import com.telecominfraproject.wlan.status.network.models.NetworkAdminStatusData;
import com.telecominfraproject.wlan.systemevent.models.SystemEvent;

import sts.OpensyncStats.Client;
import sts.OpensyncStats.ClientReport;
import sts.OpensyncStats.DNSProbeMetric;
import sts.OpensyncStats.Device;
import sts.OpensyncStats.Device.RadioTemp;
import sts.OpensyncStats.Neighbor;
import sts.OpensyncStats.Neighbor.NeighborBss;
import sts.OpensyncStats.NetworkProbe;
import sts.OpensyncStats.RADIUSMetrics;
import sts.OpensyncStats.RadioBandType;
import sts.OpensyncStats.Report;
import sts.OpensyncStats.Survey;
import sts.OpensyncStats.Survey.SurveySample;
import sts.OpensyncStats.SurveyType;
import sts.OpensyncStats.VLANMetrics;

@org.springframework.context.annotation.Profile("opensync_cloud_config")
@Component
public class MqttStatsPublisher implements StatsPublisherInterface {

    private static final Logger LOG = LoggerFactory.getLogger(MqttStatsPublisher.class);

    @Autowired
    private EquipmentServiceInterface equipmentServiceInterface;
    @Autowired
    private OvsdbSessionMapInterface ovsdbSessionMapInterface;
    @Autowired
    private ProfileServiceInterface profileServiceInterface;
    @Autowired
    private StatusServiceInterface statusServiceInterface;
    @Autowired
    private CloudEventDispatcherInterface cloudEventDispatcherInterface;
    @Autowired
    private AlarmServiceInterface alarmServiceInterface;
    @Autowired
    private AsyncPublishService asyncPublishService;

    @Value("${tip.wlan.mqttStatsPublisher.temperatureThresholdInC:80}")
    private int temperatureThresholdInC;

    @Value("${tip.wlan.mqttStatsPublisher.cpuUtilThresholdPct:80}")
    private int cpuUtilThresholdPct;

    @Value("${tip.wlan.mqttStatsPublisher.memoryUtilThresholdPct:70}")
    private int memoryUtilThresholdPct;

    @Value("${tip.wlan.mqttStatsPublisher.reportProcessingThresholdSec:30}")
    public int reportProcessingThresholdSec;

    @Value("${tip.wlan.mqttStatsPublisher.statsTimeDriftThresholdSec:300}")
    public int statsTimeDriftThresholdSec;

    @Override
    public void processMqttMessage(String topic, Report report) {
        // Numerous try/catch blocks to address situations where logs are not being reported due to corrupt or invalid
        // data in mqtt stats causing a crash
        LOG.info("processMqttMessage for {} start", topic);

        long startTime = System.nanoTime();
        String apId = extractApIdFromTopic(topic);
        
        Equipment ce = equipmentServiceInterface.getByInventoryIdOrNull(apId);
        if (ce == null) {
            LOG.error("Cannot get equipment for inventoryId {}. Ignore mqtt message for topic {}. Exiting processMqttMessage without processing report.", apId, topic);
            return;
        }

        int customerId = ce.getCustomerId();
        long equipmentId = ce.getId();
        long locationId = ce.getLocationId();
        long profileId = ce.getProfileId();

        // update timestamp for active customer equipment
        List<ServiceMetric> metricRecordList = new ArrayList<>();

        try {
            long clientMetricsStart = System.nanoTime();
            populateApClientMetrics(metricRecordList, report, customerId, equipmentId, locationId);
            long clientMetricsStop = System.nanoTime();
            if (LOG.isDebugEnabled())
                LOG.debug("Elapsed time for constructing Client metrics record was {} milliseconds for topic {}",
                        TimeUnit.MILLISECONDS.convert(clientMetricsStop - clientMetricsStart, TimeUnit.NANOSECONDS), topic);
        } catch (Exception e) {
            LOG.error("Exception when trying to populateApClientMetrics.", e);
        }

        try {
            long nodeMetricsStart = System.nanoTime();
            populateApNodeMetrics(metricRecordList, report, customerId, equipmentId, locationId);
            long nodeMetricsStop = System.nanoTime();
            if (LOG.isDebugEnabled())
                LOG.debug("Elapsed time for constructing ApNode metrics record was {} milliseconds for topic {}",
                        TimeUnit.MILLISECONDS.convert(nodeMetricsStop - nodeMetricsStart, TimeUnit.NANOSECONDS), topic);
        } catch (Exception e) {
            LOG.error("Exception when trying to populateApNodeMetrics.", e);
        }

        try {
            long neighbourScanStart = System.nanoTime();
            populateNeighbourScanReports(metricRecordList, report, customerId, equipmentId, locationId);
            long neighbourScanStop = System.nanoTime();
            if (LOG.isDebugEnabled())
                LOG.debug("Elapsed time for constructing Neighbour metrics record was {} milliseconds for topic {}",
                        TimeUnit.MILLISECONDS.convert(neighbourScanStop - neighbourScanStart, TimeUnit.NANOSECONDS), topic);
        } catch (Exception e) {
            LOG.error("Exception when trying to populateNeighbourScanReports.", e);
        }

        try {
            long channelInfoStart = System.nanoTime();
            populateChannelInfoReports(metricRecordList, report, customerId, equipmentId, locationId, profileId);
            long channelInfoStop = System.nanoTime();
            if (LOG.isDebugEnabled())
                LOG.debug("Elapsed time for constructing Channel metrics record was {} milliseconds for topic {}",
                        TimeUnit.MILLISECONDS.convert(channelInfoStop - channelInfoStart, TimeUnit.NANOSECONDS), topic);
        } catch (Exception e) {
            LOG.error("Exception when trying to populateChannelInfoReports.", e);
        }

        try {
            long ssidStart = System.nanoTime();
            populateApSsidMetrics(metricRecordList, report, customerId, equipmentId, apId, locationId);
            long ssidStop = System.nanoTime();
            if (LOG.isDebugEnabled())
                LOG.debug("Elapsed time for constructing ApSsid metrics record was {} milliseconds for topic {}",
                        TimeUnit.MILLISECONDS.convert(ssidStop - ssidStart, TimeUnit.NANOSECONDS), topic);
        } catch (Exception e) {
            LOG.error("Exception when trying to populateApSsidMetrics.", e);
        }

        if (!metricRecordList.isEmpty()) {
            long serviceMetricTimestamp = System.currentTimeMillis();
            metricRecordList.stream().forEach(smr -> {
                try {
                    // TODO use serviceMetricTimestamp rather than 0. This is done for now since there are some
                    // channel metrics that have overlapping keys which messes up Cassandra if the same time stamp is
                    // used
                    // and setting it to 0 allows the CloudEventDispatcherController to assign unique time stamps.
                    smr.setCreatedTimestamp(0);
                    if (smr.getLocationId() == 0)
                        smr.setLocationId(locationId);
                    if (smr.getCustomerId() == 0)
                        smr.setCustomerId(customerId);
                    if (smr.getEquipmentId() == 0L)
                        smr.setEquipmentId(equipmentId);

                    long sourceTimestamp = smr.getDetails().getSourceTimestampMs();
                    long diffMillis = serviceMetricTimestamp - sourceTimestamp;
                    long thresholdMillis = statsTimeDriftThresholdSec * 1000L;
                    if (diffMillis > thresholdMillis) {
                        double diffSec = diffMillis / 1000D;
                        LOG.warn("AP {} stats report is {} seconds behind cloud. ServiceMetric {} sourceTimestampMs {} createdTimestampMs {}.", apId, diffSec,
                                smr.getDataType(), sourceTimestamp, serviceMetricTimestamp);
                    }
                } catch (Exception e) {
                    LOG.error("Exception when trying to set ServiceMetric timestamps and base values where not present.", e);
                }

            });

            // Make it asynchronous to decrease processing time
            asyncPublishService.asyncPublishStats(apId, metricRecordList);
        }

        // Make it asynchronous to decrease processing time
        asyncPublishService.asyncPublishEvents(report, customerId, equipmentId, apId, locationId);

        try {
            long endTime = System.nanoTime();
            long elapsedTimeMillis = TimeUnit.MILLISECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS);
            long elapsedTimeSeconds = TimeUnit.SECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS);

            if (elapsedTimeSeconds > reportProcessingThresholdSec) {
                LOG.warn("Processing threshold exceeded for stats messages from AP {}. Elapsed processing time {} seconds. Report: {}", apId,
                        elapsedTimeSeconds, report);
            } else {
                if (elapsedTimeSeconds < 1) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("Total elapsed processing time {} milliseconds for stats messages from AP {}", elapsedTimeMillis, apId);
                } else {
                    if (LOG.isDebugEnabled())
                        LOG.debug("Total elapsed processing time {} seconds for stats messages from AP {}", elapsedTimeSeconds, apId);
                }
            }
        } catch (Exception e) {
            LOG.error("Exception when calculating elapsed time for metrics processing.", e);
        }

        LOG.info("processMqttMessage for {} complete", topic);
    }
    
    @Override
    public void publishSystemEventFromTableStateMonitor(SystemEvent event) {
        LOG.info("Publishing SystemEvent received by TableStateMonitor {}", event);
        cloudEventDispatcherInterface.publishEvent(event);
    }

    void populateApNodeMetrics(List<ServiceMetric> metricRecordList, Report report, int customerId, long equipmentId, long locationId) {
        LOG.info("populateApNodeMetrics for Customer {} Equipment {}", customerId, equipmentId);
        ApNodeMetrics apNodeMetrics = new ApNodeMetrics();
        ServiceMetric smr = new ServiceMetric(customerId, equipmentId);

        smr.setLocationId(locationId);
        metricRecordList.add(smr);
        smr.setDetails(apNodeMetrics);

        for (Device deviceReport : report.getDeviceList()) {

            int avgRadioTemp = 0;
            // The 3 fixed alarm codes
            List<Alarm> alarms = alarmServiceInterface.get(customerId, Set.of(equipmentId), Set.of(AlarmCode.CPUTemperature, AlarmCode.CPUUtilization, AlarmCode.MemoryUtilization));

            ApPerformance apPerformance = new ApPerformance();
            apNodeMetrics.setApPerformance(apPerformance);

            if (deviceReport.getRadioTempCount() > 0) {
                float cpuTemperature = 0;
                int numSamples = 0;
                for (RadioTemp r : deviceReport.getRadioTempList()) {
                    if (r.hasValue()) {
                        cpuTemperature += r.getValue();
                        numSamples++;
                    }
                }
                if (numSamples > 0) {
                    avgRadioTemp = Math.round((cpuTemperature / numSamples));
                    apPerformance.setCpuTemperature(avgRadioTemp);
                }
                if (avgRadioTemp > temperatureThresholdInC) {
                    raiseDeviceThresholdAlarm(customerId, equipmentId, AlarmCode.CPUTemperature, deviceReport.getTimestampMs(), alarms);
                } else {
                    // Clear any existing temperature alarms for this ap
                    clearDeviceThresholdAlarm(customerId, equipmentId, AlarmCode.CPUTemperature, alarms);
                }
            }

            if (deviceReport.hasCpuUtil() && deviceReport.getCpuUtil().hasCpuUtil()) {
                Integer cpuUtilization = deviceReport.getCpuUtil().getCpuUtil();
                apPerformance.setCpuUtilized(new int[] {cpuUtilization});
                if (cpuUtilization > cpuUtilThresholdPct) {
                    raiseDeviceThresholdAlarm(customerId, equipmentId, AlarmCode.CPUUtilization, deviceReport.getTimestampMs(), alarms);
                } else {
                    // Clear any existing cpuUtilization alarms
                    clearDeviceThresholdAlarm(customerId, equipmentId, AlarmCode.CPUUtilization, alarms);
                }
            }

            apPerformance.setEthLinkState(EthernetLinkState.UP1000_FULL_DUPLEX);

            if (deviceReport.hasMemUtil() && deviceReport.getMemUtil().hasMemTotal() && deviceReport.getMemUtil().hasMemUsed()) {
                apPerformance.setFreeMemory(deviceReport.getMemUtil().getMemTotal() - deviceReport.getMemUtil().getMemUsed());

                double usedMemory = deviceReport.getMemUtil().getMemUsed();
                double totalMemory = deviceReport.getMemUtil().getMemTotal();
                if (usedMemory / totalMemory * 100 > memoryUtilThresholdPct) {
                    raiseDeviceThresholdAlarm(customerId, equipmentId, AlarmCode.MemoryUtilization, deviceReport.getTimestampMs(), alarms);
                } else {
                    // Clear any existing cpuUtilization alarms
                    clearDeviceThresholdAlarm(customerId, equipmentId, AlarmCode.MemoryUtilization, alarms);
                }

            }
            apPerformance.setUpTime((long) deviceReport.getUptime());

            List<PerProcessUtilization> cpuPerProcess = new ArrayList<>();
            deviceReport.getPsCpuUtilList().stream().forEach(c -> cpuPerProcess.add(new PerProcessUtilization(c.getPid(), c.getCmd(), c.getUtil())));
            apPerformance.setPsCpuUtil(cpuPerProcess);

            List<PerProcessUtilization> memPerProcess = new ArrayList<>();
            deviceReport.getPsMemUtilList().stream().forEach(c -> memPerProcess.add(new PerProcessUtilization(c.getPid(), c.getCmd(), c.getUtil())));
            apPerformance.setPsMemUtil(memPerProcess);
            apPerformance.setSourceTimestampMs(deviceReport.getTimestampMs());
            // The service metric report's sourceTimestamp will be the most recent timestamp from its contributing stats
            if (apNodeMetrics.getSourceTimestampMs() < deviceReport.getTimestampMs())
                apNodeMetrics.setSourceTimestampMs(deviceReport.getTimestampMs());
            updateDeviceStatusForReport(customerId, equipmentId, deviceReport, avgRadioTemp);
        }

        // statusList.add(status);

        // Main Network dashboard shows Traffic and Capacity values that
        // are calculated from
        // ApNodeMetric properties getPeriodLengthSec, getRxBytes2G,
        // getTxBytes2G, getRxBytes5G, getTxBytes5G

        // go over all the clients to aggregate per-client tx/rx stats -
        // we want to do this
        // only once per batch of ApNodeMetrics - so we do not repeat
        // values over and over again

        for (ClientReport clReport : report.getClientsList()) {

            long rxBytes = 0;
            long txBytes = 0;

            long txFrames = 0;
            long rxFrames = 0;
            int txRetries = 0;
            int rxRetries = 0;
            int rxErrors = 0;
            int rssi = 0;
            Set<String> clientMacs = new HashSet<>();

            for (Client cl : clReport.getClientListList()) {

                if (!cl.hasConnected() || (cl.getConnected() != true)) {
                    // this client is not currently connected, skip it
                    // TODO: how come AP reports disconencted clients? What
                    // if it is a busy coffe shop with thousands of peopele
                    // per day, when do clients disappear from the reports?
                    continue;
                }

                if (cl.hasMacAddress()) {
                    clientMacs.add(cl.getMacAddress());
                }

                if (cl.getStats().hasTxBytes()) {
                    txBytes += cl.getStats().getTxBytes();
                }
                if (cl.getStats().hasRxBytes()) {
                    rxBytes += cl.getStats().getRxBytes();
                }
                if (cl.getStats().hasRssi()) {
                    rssi = cl.getStats().getRssi();
                }
                if (cl.getStats().hasTxFrames()) {
                    txFrames += cl.getStats().getTxFrames();
                }
                if (cl.getStats().hasRxFrames()) {
                    rxFrames += cl.getStats().getRxFrames();
                }
                if (cl.getStats().hasTxRetries()) {
                    txRetries += cl.getStats().getTxRetries();
                }
                if (cl.getStats().hasRxRetries()) {
                    rxRetries += cl.getStats().getRxRetries();
                }
                if (cl.getStats().hasTxErrors()) {
                    cl.getStats().getTxErrors();
                }
                if (cl.getStats().hasRxErrors()) {
                    rxErrors += cl.getStats().getRxErrors();
                }
                if (cl.getStats().hasTxRate()) {
                    cl.getStats().getTxRate();
                }
                if (cl.getStats().hasRxRate()) {
                    cl.getStats().getRxRate();
                }

            }

            RadioType radioType = OvsdbToWlanCloudTypeMappingUtility.getRadioTypeFromOpensyncStatsRadioBandType(clReport.getBand());
            RadioStatistics radioStats = apNodeMetrics.getRadioStats(radioType);
            if (radioStats == null) {
                radioStats = new RadioStatistics();
            }

            radioStats.setNumTxRetryAttemps(txRetries);
            radioStats.setNumTxFramesTransmitted(txFrames);
            radioStats.setNumTxRetryAttemps(txRetries);
            radioStats.setCurChannel(clReport.getChannel());
            if (clReport.getClientListCount() > 0) {
                // radioStats.setRxLastRssi(getNegativeSignedIntFromUnsigned(rssi)
                // / clReport.getClientListCount());
                radioStats.setRxLastRssi(rssi);
            }
            radioStats.setRxDataBytes(rxBytes);
            radioStats.setNumRxDataFrames(rxFrames);
            radioStats.setNumRxErr(rxErrors);
            radioStats.setNumRxRetry(rxRetries);
            radioStats.setSourceTimestampMs(clReport.getTimestampMs());

            // The service metric report's sourceTimestamp will be the most recent timestamp from its contributing stats
            if (apNodeMetrics.getSourceTimestampMs() < clReport.getTimestampMs())
                apNodeMetrics.setSourceTimestampMs(clReport.getTimestampMs());

            apNodeMetrics.setRadioStats(radioType, radioStats);

            apNodeMetrics.setRxBytes(radioType, rxBytes);
            apNodeMetrics.setTxBytes(radioType, txBytes);

            List<MacAddress> clientMacList = new ArrayList<>();
            clientMacs.forEach(macStr -> {
                try {
                    clientMacList.add(MacAddress.valueOf(macStr));
                } catch (RuntimeException e) {
                    LOG.warn("Cannot parse mac address from MQTT ClientReport {} ", macStr);
                }
            });

            apNodeMetrics.setClientMacAddresses(radioType, clientMacList);
            apNodeMetrics.setRadioUtilization(radioType, new ArrayList<RadioUtilization>());

        }

        apNodeMetrics.setPeriodLengthSec(60);

        Map<RadioType, Integer> avgNoiseFloor = new HashMap<>();
        new HashMap<>();
        Map<RadioType, EquipmentCapacityDetails> capacityDetails = new HashMap<>();
        Map<RadioType, EquipmentPerRadioUtilizationDetails> radioUtilizationDetailsMap = new HashMap<>();

        // populate it from report.survey
        for (Survey survey : report.getSurveyList()) {

            if (survey.getSurveyType().equals(SurveyType.ON_CHANNEL)) { // only interested in ON_CHANNEL for this
                                                                        // Metrics Report

                int busyRx = 0; /* Rx = Rx_obss + Rx_errr (self and obss errors) */
                int busyTx = 0; /* Tx */
                int busySelf = 0; /* Rx_self (derived from succesful Rx frames) */
                int busy = 0; /* Busy = Rx + Tx + Interference */
                int totalDurationMs = 0;
                RadioType radioType = RadioType.UNSUPPORTED;

                List<Integer> noiseList = new ArrayList<>();
                for (SurveySample surveySample : survey.getSurveyListList()) {
                    if (surveySample.getDurationMs() == 0) {
                        continue;
                    }
                    // we need to perform a weighted average here because the
                    // samples are in percentage, and may be of different durations
                    busyTx += surveySample.getBusyTx() * surveySample.getDurationMs();
                    busyRx += surveySample.getBusyRx() * surveySample.getDurationMs();
                    busy += surveySample.getBusy() * surveySample.getDurationMs();
                    busySelf += surveySample.getBusySelf() * surveySample.getDurationMs();
                    totalDurationMs += surveySample.getDurationMs();
                    if (surveySample.hasNoise()) {
                        noiseList.add(getNegativeSignedIntFrom8BitUnsigned(surveySample.getNoise()));
                    }
                }

                if (totalDurationMs > 0) {
                    RadioUtilization radioUtil = new RadioUtilization();
                    radioUtil.setTimestampSeconds((int) ((survey.getTimestampMs()) / 1000));
                    radioUtil.setSourceTimestampMs(survey.getTimestampMs());

                    // The service metric report's sourceTimestamp will be the most recent timestamp from its
                    // contributing stats
                    if (apNodeMetrics.getSourceTimestampMs() < survey.getTimestampMs())
                        apNodeMetrics.setSourceTimestampMs(survey.getTimestampMs());

                    int pctBusyTx = busyTx / totalDurationMs;
                    checkIfOutOfBound("pctBusyTx", pctBusyTx, survey, totalDurationMs, busyTx, busyRx, busy, busySelf);

                    radioUtil.setAssocClientTx(pctBusyTx);
                    int pctBusyRx = busySelf / totalDurationMs;
                    checkIfOutOfBound("pctBusyRx", pctBusyRx, survey, totalDurationMs, busyTx, busyRx, busy, busySelf);
                    radioUtil.setAssocClientRx(pctBusyRx);

                    double pctIBSS = (busyTx + busySelf) / totalDurationMs;
                    if (pctIBSS > 100D || pctIBSS < 0D) {
                        LOG.warn(
                                "Calculated value for {} {} is out of bounds on totalDurationMs {} for survey.getBand {}. busyTx {} busyRx {} busy {} busySelf {} "
                                        + " survey.getTimestampMs {}, survey.getSurveyListList {}",
                                "pctIBSS", pctIBSS, totalDurationMs, survey.getBand(), busyTx, busyRx, busy, busySelf, survey.getTimestampMs(),
                                survey.getSurveyListList());
                    }
                    radioUtil.setIbss(pctIBSS);

                    int nonWifi = (busy - (busyTx + busyRx)) / totalDurationMs;
                    checkIfOutOfBound("nonWifi", nonWifi, survey, totalDurationMs, busyTx, busyRx, busy, busySelf);
                    radioUtil.setNonWifi(nonWifi);

                    int pctOBSSAndSelfErrors = (busyRx - busySelf) / totalDurationMs;
                    checkIfOutOfBound("OBSSAndSelfErrors", pctOBSSAndSelfErrors, survey, totalDurationMs, busyTx, busyRx, busy, busySelf);
                    radioUtil.setUnassocClientRx(pctOBSSAndSelfErrors);

                    radioType = OvsdbToWlanCloudTypeMappingUtility.getRadioTypeFromOpensyncStatsRadioBandType(survey.getBand());
                    if (radioType != RadioType.UNSUPPORTED) {
                        if (apNodeMetrics.getRadioUtilization(radioType) == null) {
                            apNodeMetrics.setRadioUtilization(radioType, new ArrayList<>());
                        }
                        apNodeMetrics.getRadioUtilization(radioType).add(radioUtil);
                        if (!noiseList.isEmpty()) {
                            int noiseAvg = (int) Math.round(DecibelUtils.getAverageDecibel(noiseList));
                            avgNoiseFloor.put(radioType, noiseAvg);
                            apNodeMetrics.setNoiseFloor(radioType, noiseAvg);
                        }

                        Long totalUtilization = Math.round((double) busy / totalDurationMs);
                        Long totalNonWifi = totalUtilization - ((busyTx + busyRx) / totalDurationMs);

                        EquipmentCapacityDetails cap = new EquipmentCapacityDetails();
                        cap.setUnavailableCapacity(totalNonWifi.intValue());
                        int availableCapacity = (int) (100 - totalUtilization);
                        cap.setAvailableCapacity(availableCapacity);
                        cap.setUsedCapacity(totalUtilization.intValue());
                        cap.setUnusedCapacity(availableCapacity - totalUtilization.intValue());
                        cap.setTotalCapacity(100);

                        apNodeMetrics.setChannelUtilization(radioType, totalUtilization.intValue());
                        capacityDetails.put(radioType, cap);
                    }
                }

            }
        }

        populateNetworkProbeMetrics(report, apNodeMetrics);
        updateNetworkAdminStatusReport(customerId, equipmentId, apNodeMetrics);

        RadioUtilizationReport radioUtilizationReport = new RadioUtilizationReport();
        radioUtilizationReport.setAvgNoiseFloor(avgNoiseFloor);
        radioUtilizationReport.setRadioUtilization(radioUtilizationDetailsMap);
        radioUtilizationReport.setCapacityDetails(capacityDetails);

        updateDeviceStatusRadioUtilizationReport(customerId, equipmentId, radioUtilizationReport);
    }

    
    void clearDeviceThresholdAlarm(int customerId, long equipmentId, AlarmCode alarmCode, List<Alarm> alarms) {
    	for (Alarm alarm: alarms) {
    		if (alarm.getAlarmCode() == alarmCode) {
    			Alarm removedAlarm = alarmServiceInterface.delete(customerId, equipmentId, alarm.getAlarmCode(), alarm.getCreatedTimestamp());
    			LOG.debug("Cleared device threshold alarm {}", removedAlarm);
    			return;
    		}
    	}
    }

    
    void raiseDeviceThresholdAlarm(int customerId, long equipmentId, AlarmCode alarmCode, long timestampMs, List<Alarm> alarms) {
    	for (Alarm alarm: alarms) {
    		if (alarm.getAlarmCode() == alarmCode) {
    			return;
    		}
    	}
        Alarm alarm = new Alarm();
        alarm.setCustomerId(customerId);
        alarm.setEquipmentId(equipmentId);
        alarm.setAlarmCode(alarmCode);
        alarm.setOriginatorType(OriginatorType.AP);
        alarm.setSeverity(alarmCode.getSeverity());
        alarm.setScopeType(AlarmScopeType.EQUIPMENT);
        alarm.setScopeId("" + equipmentId);
        alarm.setCreatedTimestamp(timestampMs);
        AlarmDetails alarmDetails = new AlarmDetails();
        alarmDetails.setMessage(alarmCode.getDescription());
        alarmDetails.setAffectedEquipmentIds(Collections.singletonList(equipmentId));
        alarm.setDetails(alarmDetails);
        alarm.setCreatedTimestamp(timestampMs);
        alarm = alarmServiceInterface.create(alarm);
    }

    private void checkIfOutOfBound(String checkedType, int checkedValue, Survey survey, int totalDurationMs, int busyTx, int busyRx, int busy, int busySelf) {
        if (checkedValue > 100 || checkedValue < 0) {
            LOG.warn(
                    "Calculated value for {} {} is out of bounds on totalDurationMs {} for survey.getBand {}. busyTx {} busyRx {} busy {} busySelf {} "
                            + " survey.getTimestampMs {}, survey.getSurveyListList {}",
                    checkedType, checkedValue, totalDurationMs, survey.getBand(), busyTx, busyRx, busy, busySelf, survey.getTimestampMs(),
                    survey.getSurveyListList());
        }
    }

    private void updateNetworkAdminStatusReport(int customerId, long equipmentId, ApNodeMetrics apNodeMetrics) {
        apNodeMetrics.getNetworkProbeMetrics().forEach(n -> {

            LOG.debug("Update NetworkAdminStatusReport for NetworkProbeMetrics {}", n.toString());

            Status networkAdminStatus = statusServiceInterface.getOrNull(customerId, equipmentId, StatusDataType.NETWORK_ADMIN);

            if (networkAdminStatus == null) {
                networkAdminStatus = new Status();
                networkAdminStatus.setCustomerId(customerId);
                networkAdminStatus.setEquipmentId(equipmentId);
                networkAdminStatus.setStatusDataType(StatusDataType.NETWORK_ADMIN);
                networkAdminStatus.setDetails(new NetworkAdminStatusData());
                networkAdminStatus = statusServiceInterface.update(networkAdminStatus);
            }

            NetworkAdminStatusData statusData = (NetworkAdminStatusData) networkAdminStatus.getDetails();

            if (n.getDnsState() == null) {
                LOG.trace("No DnsState present in networkProbeMetrics, DnsState and CloudLinkStatus set to 'normal");
                statusData.setDnsStatus(StatusCode.normal);
                statusData.setCloudLinkStatus(StatusCode.normal);
            } else {
                statusData.setDnsStatus(stateUpDownErrorToStatusCode(n.getDnsState()));
                statusData.setCloudLinkStatus(stateUpDownErrorToStatusCode(n.getDnsState()));
            }
            if (n.getDhcpState() == null) {
                LOG.trace("No DhcpState present in networkProbeMetrics, set to 'normal");
                statusData.setDhcpStatus(StatusCode.normal);
            } else {
                statusData.setDhcpStatus(stateUpDownErrorToStatusCode(n.getDhcpState()));
            }
            if (n.getRadiusState() == null) {
                LOG.trace("No RadiusState present in networkProbeMetrics, set to 'normal");
                statusData.setRadiusStatus(StatusCode.normal);
            } else {
                statusData.setRadiusStatus(stateUpDownErrorToStatusCode(n.getRadiusState()));
            }

            networkAdminStatus.setDetails(statusData);

            networkAdminStatus = statusServiceInterface.update(networkAdminStatus);

            LOG.debug("Updated NetworkAdminStatus {}", networkAdminStatus);

        });

    }

    private static StatusCode stateUpDownErrorToStatusCode(StateUpDownError state) {

        switch (state) {
            case enabled:
                return StatusCode.normal;
            case error:
                return StatusCode.error;
            case disabled:
                return StatusCode.disabled;
            case UNSUPPORTED:
                return StatusCode.requiresAttention;
            default:
                return StatusCode.normal;
        }

    }

    void updateDeviceStatusRadioUtilizationReport(int customerId, long equipmentId, RadioUtilizationReport radioUtilizationReport) {
        LOG.info("Processing updateDeviceStatusRadioUtilizationReport for equipmentId {} with RadioUtilizationReport {}", equipmentId, radioUtilizationReport);
        // remove statusServiceInterface.getOrNull() for better performance (i.e. no createdTimestamp)
        LOG.debug("Create new radioUtilizationStatus");
        Status radioUtilizationStatus = new Status();
        radioUtilizationStatus.setCustomerId(customerId);
        radioUtilizationStatus.setEquipmentId(equipmentId);
        radioUtilizationStatus.setStatusDataType(StatusDataType.RADIO_UTILIZATION);
        radioUtilizationStatus.setDetails(radioUtilizationReport);
        statusServiceInterface.update(radioUtilizationStatus);
    }

    void populateNetworkProbeMetrics(Report report, ApNodeMetrics apNodeMetrics) {
        List<NetworkProbeMetrics> networkProbeMetricsList = new ArrayList<>();

        for (NetworkProbe networkProbe : report.getNetworkProbeList()) {
            NetworkProbeMetrics networkProbeMetrics = new NetworkProbeMetrics();
            networkProbeMetrics.setSourceTimestampMs(networkProbe.getTimestampMs());
            List<DnsProbeMetric> dnsProbeResults = new ArrayList<>();
            if (networkProbe.hasDnsProbe()) {
                DNSProbeMetric dnsProbeMetricFromAp = networkProbe.getDnsProbe();
                DnsProbeMetric cloudDnsProbeMetric = new DnsProbeMetric();
                if (dnsProbeMetricFromAp.hasLatency()) {
                    networkProbeMetrics.setDnsLatencyMs(dnsProbeMetricFromAp.getLatency());
                    cloudDnsProbeMetric.setDnsLatencyMs(dnsProbeMetricFromAp.getLatency());
                }
                if (dnsProbeMetricFromAp.hasState()) {
                    StateUpDownError dnsState =
                            OvsdbToWlanCloudTypeMappingUtility.getCloudMetricsStateFromOpensyncStatsStateUpDown(dnsProbeMetricFromAp.getState());

                    networkProbeMetrics.setDnsState(dnsState);
                    cloudDnsProbeMetric.setDnsState(dnsState);
                }
                if (dnsProbeMetricFromAp.hasServerIP()) {
                    InetAddress ipAddress;
                    try {
                        ipAddress = InetAddress.getByName(dnsProbeMetricFromAp.getServerIP());
                        cloudDnsProbeMetric.setDnsServerIp(ipAddress);
                    } catch (UnknownHostException e) {
                        LOG.error("Could not get DNS Server IP from network_probe service_metrics_collection_config", e);
                    }
                }
                dnsProbeResults.add(cloudDnsProbeMetric);
            }

            networkProbeMetrics.setDnsProbeResults(dnsProbeResults);

            for (RADIUSMetrics radiusMetrics : networkProbe.getRadiusProbeList()) {
                if (radiusMetrics.hasLatency()) {
                    networkProbeMetrics.setRadiusLatencyInMs(radiusMetrics.getLatency());
                }
                if (radiusMetrics.hasRadiusState()) {
                    StateUpDownError radiusState =
                            OvsdbToWlanCloudTypeMappingUtility.getCloudMetricsStateFromOpensyncStatsStateUpDown(radiusMetrics.getRadiusState());
                    networkProbeMetrics.setRadiusState(radiusState);
                }
            }

            if (networkProbe.hasVlanProbe()) {
                VLANMetrics vlanMetrics = networkProbe.getVlanProbe();
                if (vlanMetrics.hasVlanIF()) {
                    networkProbeMetrics.setVlanIF(vlanMetrics.getVlanIF());
                }
                if (vlanMetrics.hasDhcpState()) {
                    StateUpDownError dhcpState =
                            OvsdbToWlanCloudTypeMappingUtility.getCloudMetricsStateFromOpensyncStatsStateUpDown(vlanMetrics.getDhcpState());

                    networkProbeMetrics.setDhcpState(dhcpState);

                }
                if (vlanMetrics.hasLatency()) {
                    networkProbeMetrics.setDhcpLatencyMs(vlanMetrics.getLatency());
                }
            }
            networkProbeMetricsList.add(networkProbeMetrics);
        }

        apNodeMetrics.setNetworkProbeMetrics(networkProbeMetricsList);
    }

    void updateDeviceStatusForReport(int customerId, long equipmentId, Device deviceReport, int avgRadioTemp) {
        Status status = new Status();
        status.setCustomerId(customerId);
        status.setEquipmentId(equipmentId);
        OperatingSystemPerformance eqOsPerformance = new OperatingSystemPerformance();
        eqOsPerformance.setUptimeInSeconds(deviceReport.getUptime());
        eqOsPerformance.setAvgCpuTemperature(avgRadioTemp);
        eqOsPerformance.setAvgCpuUtilization(deviceReport.getCpuUtil().getCpuUtil());
        eqOsPerformance.setAvgFreeMemoryKb(deviceReport.getMemUtil().getMemTotal() - deviceReport.getMemUtil().getMemUsed());
        eqOsPerformance.setTotalAvailableMemoryKb(deviceReport.getMemUtil().getMemTotal());
        status.setDetails(eqOsPerformance);
        status = statusServiceInterface.update(status);
        LOG.trace("updated status {}", status);
    }

    void populateApClientMetrics(List<ServiceMetric> metricRecordList, Report report, int customerId, long equipmentId, long locationId) {
        LOG.info("populateApClientMetrics for Customer {} Equipment {}", customerId, equipmentId);

        for (ClientReport clReport : report.getClientsList()) {
            for (Client cl : clReport.getClientListList()) {

                if (cl.getMacAddress() == null) {
                    LOG.info("No mac address for Client {}, cannot set device mac address for client in ClientMetrics.", cl);
                    continue;
                }

                if (cl.hasStats()) {
                    LOG.debug("Processing ClientReport from AP for client device {}", cl.getMacAddress());
                    ServiceMetric smr = new ServiceMetric(customerId, equipmentId, MacAddress.valueOf(cl.getMacAddress()));
                    smr.setLocationId(locationId);
                    metricRecordList.add(smr);
                    smr.setClientMac(MacAddress.valueOf(cl.getMacAddress()).getAddressAsLong());

                    // clReport.getChannel();
                    ClientMetrics cMetrics = new ClientMetrics();
                    smr.setDetails(cMetrics);
                    cMetrics.setSourceTimestampMs(clReport.getTimestampMs());
                    Integer periodLengthSec = 60; // matches what's configured by
                    // OvsdbDao.configureStats(OvsdbClient)
                    cMetrics.setPeriodLengthSec(periodLengthSec);
                    cMetrics.setRadioType(OvsdbToWlanCloudTypeMappingUtility.getRadioTypeFromOpensyncStatsRadioBandType(clReport.getBand()));

                    if (cl.getStats().hasRssi()) {
                        int rssi = cl.getStats().getRssi();
                        cMetrics.setRssi(rssi);
                    }

                    // populate Rx stats
                    if (cl.getStats().hasRxBytes()) {
                        cMetrics.setRxBytes(cl.getStats().getRxBytes());
                    }

                    if (cl.getStats().hasRxRate()) {
                        cMetrics.setAverageRxRate(cl.getStats().getRxRate() / 1000);
                    }

                    if (cl.getStats().hasRxErrors()) {
                        cMetrics.setNumRxNoFcsErr((int) cl.getStats().getRxErrors());
                    }

                    if (cl.getStats().hasRxFrames()) {
                        cMetrics.setNumRxFramesReceived(cl.getStats().getRxFrames());
                        // cMetrics.setNumRxPackets(cl.getStats().getRxFrames());
                    }

                    if (cl.getStats().hasRxRetries()) {
                        cMetrics.setNumRxRetry((int) cl.getStats().getRxRetries());
                    }

                    // populate Tx stats
                    if (cl.getStats().hasTxBytes()) {
                        cMetrics.setNumTxBytes(cl.getStats().getTxBytes());
                    }

                    if (cl.getStats().hasTxRate()) {
                        cMetrics.setAverageTxRate(cl.getStats().getTxRate() / 1000);
                    }
                    
                    if (cl.getStats().hasTxErrors()) {
                        cMetrics.setNumTxDropped((int) cl.getStats().getTxErrors());
                    }

                    if (cl.getStats().hasTxFrames()) {
                        cMetrics.setNumTxFramesTransmitted(cl.getStats().getTxFrames());
                    }

                    if (cl.getStats().hasTxRetries()) {
                        cMetrics.setNumTxDataRetries((int) cl.getStats().getTxRetries());
                    }

                    // Commented to increase performance as it repetitive
                    // LOG.debug("ApClientMetrics Report {}", cMetrics);

                }

            }

        }

    }

    void populateNeighbourScanReports(List<ServiceMetric> metricRecordList, Report report, int customerId, long equipmentId, long locationId) {
        LOG.info("populateNeighbourScanReports for Customer {} Equipment {}", customerId, equipmentId);

        for (Neighbor neighbor : report.getNeighborsList()) {

            ServiceMetric smr = new ServiceMetric(customerId, equipmentId);
            smr.setLocationId(locationId);
            metricRecordList.add(smr);
            NeighbourScanReports neighbourScanReports = new NeighbourScanReports();
            smr.setDetails(neighbourScanReports);
            neighbourScanReports.setSourceTimestampMs(neighbor.getTimestampMs());

            List<NeighbourReport> neighbourReports = new ArrayList<>();
            neighbourScanReports.setNeighbourReports(neighbourReports);

            for (NeighborBss nBss : neighbor.getBssListList()) {
                NeighbourReport nr = new NeighbourReport();
                neighbourReports.add(nr);

                if (neighbor.getBand() == RadioBandType.BAND2G) {
                    nr.setAcMode(false);
                    nr.setbMode(false);
                    nr.setnMode(true);
                    nr.setRadioType(RadioType.is2dot4GHz);
                } else if (neighbor.getBand() == RadioBandType.BAND5G) {
                    nr.setAcMode(true);
                    nr.setbMode(false);
                    nr.setnMode(false);
                    nr.setRadioType(RadioType.is5GHz);
                } else if (neighbor.getBand() == RadioBandType.BAND5GL) {
                    nr.setAcMode(true);
                    nr.setbMode(false);
                    nr.setnMode(false);
                    nr.setRadioType(RadioType.is5GHzL);
                } else if (neighbor.getBand() == RadioBandType.BAND5GU) {
                    nr.setAcMode(true);
                    nr.setbMode(false);
                    nr.setnMode(false);
                    nr.setRadioType(RadioType.is5GHzU);
                }

                nr.setChannel(nBss.getChannel());
                nr.setMacAddress(MacAddress.valueOf(nBss.getBssid()));
                nr.setNetworkType(NetworkType.AP);
                nr.setPacketType(NeighborScanPacketType.BEACON);
                nr.setPrivacy(((nBss.getSsid() == null) || nBss.getSsid().isEmpty()) ? true : false);
                // nr.setRate(rate);
                // we can only get Rssi as an unsigned int from opensync, so
                // some shifting
                int rssi = nBss.getRssi();
                nr.setRssi(rssi);
                nr.setScanTimeInSeconds(neighbor.getTimestampMs() / 1000L);
                nr.setSecureMode(DetectedAuthMode.WPA);
                // nr.setSignal(signal);
                nr.setSsid(nBss.getSsid());
            }

        }
    }

    void populateApSsidMetrics(List<ServiceMetric> metricRecordList, Report report, int customerId, long equipmentId, String apId, long locationId) {
        LOG.debug("populateApSsidMetrics start");

        if (report.getClientsCount() == 0) {
            LOG.info("populateApSsidMetrics no client data present, cannot build {}", ServiceMetricDataType.ApSsid);
            return;
        }

        LOG.debug("populateApSsidMetrics for Customer {} Equipment {} LocationId {} AP {}", customerId, equipmentId, locationId, apId);

        ServiceMetric smr = new ServiceMetric(customerId, equipmentId);
        smr.setLocationId(locationId);
        smr.setDataType(ServiceMetricDataType.ApSsid);
        ApSsidMetrics apSsidMetrics = new ApSsidMetrics();
        smr.setDetails(apSsidMetrics);
        metricRecordList.add(smr);

        for (ClientReport clientReport : report.getClientsList()) {
            RadioType radioType = OvsdbToWlanCloudTypeMappingUtility.getRadioTypeFromOpensyncStatsRadioBandType(clientReport.getBand());

            LOG.debug("populateApSsidMetrics processing clientReport for RadioType {} Channel {}", radioType, clientReport.getChannel());
            Map<String, List<Client>> clientBySsid = clientReport.getClientListList().stream().filter(new Predicate<Client>() {
                @Override
                public boolean test(Client t) {
                    return t.hasSsid() && t.hasStats();
                }
            }).collect(Collectors.groupingBy(c -> c.getSsid()));

            if (LOG.isTraceEnabled())
                LOG.trace("populateApSsidMetrics clientBySsid {}", clientBySsid);

            final List<SsidStatistics> ssidStats = new ArrayList<>();

            clientBySsid.entrySet().stream().forEach(e -> {

                if (LOG.isTraceEnabled())
                    LOG.trace("populateApSsidMetrics processing clients {}", e.getValue());

                SsidStatistics stats = new SsidStatistics();
                stats.setSsid(e.getKey());
                stats.setNumClient(e.getValue().size());
                stats.setSourceTimestampMs(clientReport.getTimestampMs());

                // Get the BSSID (MAC address) for this SSID
                String bssid = getBssidForClientSsid(customerId, equipmentId, apId, e.getKey(), radioType);
                if (bssid != null)
                    stats.setBssid(MacAddress.valueOf(bssid));
                else
                    LOG.warn("Could not get BSSID for customer {} equipment {} apId {}, ssid {} radioType {}", customerId, equipmentId, apId, e.getKey(),
                            radioType);

                long txBytes = e.getValue().stream().mapToLong(c -> c.getStats().getTxBytes()).sum();
                long rxBytes = e.getValue().stream().mapToLong(c -> c.getStats().getRxBytes()).sum();
                long txFrame = e.getValue().stream().mapToLong(c -> c.getStats().getTxFrames()).sum();
                long rxFrame = e.getValue().stream().mapToLong(c -> c.getStats().getRxFrames()).sum();

                long txErrors = e.getValue().stream().mapToLong(c -> c.getStats().getTxErrors()).sum();
                long rxErrors = e.getValue().stream().mapToLong(c -> c.getStats().getRxErrors()).sum();

                long txRetries = e.getValue().stream().mapToLong(c -> c.getStats().getTxRetries()).sum();
                long rxRetries = e.getValue().stream().mapToLong(c -> c.getStats().getRxRetries()).sum();

                int[] rssi = e.getValue().stream().mapToInt(c -> c.getStats().getRssi()).toArray();
                double avgRssi = DecibelUtils.getAverageDecibel(rssi);

                stats.setRxLastRssi(Double.valueOf(avgRssi).intValue());
                stats.setNumRxData(Long.valueOf(rxFrame).intValue());
                stats.setRxBytes(rxBytes - rxErrors - rxRetries);
                stats.setNumTxDataRetries(Long.valueOf(txRetries).intValue());
                stats.setNumRcvFrameForTx(txFrame);
                stats.setNumTxBytesSucc(txBytes - txErrors - txRetries);
                stats.setNumRxRetry(Long.valueOf(rxRetries).intValue());

                if (LOG.isTraceEnabled())
                    LOG.trace("populateApSsidMetrics stats {}", stats.toPrettyString());
                ssidStats.add(stats);

            });

            if (LOG.isTraceEnabled())
                LOG.trace("populateApSsidMetrics ssidStats {}", ssidStats);
            apSsidMetrics.getSsidStats().put(radioType, ssidStats);

        }

        if (LOG.isTraceEnabled())
            LOG.trace("populateApSsidMetrics apSsidMetrics {}", apSsidMetrics);

        LOG.debug("populateApSsidMetrics finished");
    }

    String getBssidForClientSsid(int customerId, long equipmentId, String apId, String ssid, RadioType radioType) {
        try {
            Status activeBssidsStatus = statusServiceInterface.getOrNull(customerId, equipmentId, StatusDataType.ACTIVE_BSSIDS);
            LOG.debug("populateApSsidMetrics get BSSID from activeBssids {}", activeBssidsStatus);
            if (activeBssidsStatus != null) {
                if (activeBssidsStatus.getDetails() != null) {
                    ActiveBSSIDs activeBssids = (ActiveBSSIDs) activeBssidsStatus.getDetails();
                    if (activeBssids.getActiveBSSIDs() != null) {
                        for (ActiveBSSID activeBssid : activeBssids.getActiveBSSIDs()) {
                            if (activeBssid.getRadioType() != null && activeBssid.getRadioType().equals(radioType)) {
                                if (activeBssid.getSsid() != null && activeBssid.getSsid().equals(ssid)) {
                                    if (activeBssid.getBssid() != null) {
                                        return activeBssid.getBssid();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Could not get active BSSIDs for apId {} radioType {}", apId, radioType, e);
        }
        return null;
    }

    ChannelInfo createChannelInfo(long equipmentId, RadioType radioType, List<SurveySample> surveySampleList, ChannelBandwidth channelBandwidth) {

        int busyTx = 0; /* Tx */
        int busySelf = 0; /* Rx_self (derived from succesful Rx frames) */
        int busy = 0; /* Busy = Rx + Tx + Interference */
        long totalDurationMs = 0;
        ChannelInfo channelInfo = new ChannelInfo();

        List<Integer> noiseList = new ArrayList<>();
        for (SurveySample sample : surveySampleList) {
            busyTx += sample.getBusyTx() * sample.getDurationMs();
            busySelf += sample.getBusySelf() * sample.getDurationMs(); // successful
                                                                       // Rx
            busy += sample.getBusy() * sample.getDurationMs();
            channelInfo.setChanNumber(sample.getChannel());
            if (sample.hasNoise()) {
                noiseList.add(getNegativeSignedIntFrom8BitUnsigned(sample.getNoise()));
            }
            totalDurationMs += sample.getDurationMs();
        }

        int iBSS = busyTx + busySelf;

        Long totalUtilization = Math.round((double) busy / totalDurationMs);
        Long totalNonWifi = Math.round(((double) busy - (double) iBSS) / totalDurationMs);

        channelInfo.setTotalUtilization(totalUtilization.intValue());
        channelInfo.setWifiUtilization(totalUtilization.intValue() - totalNonWifi.intValue());
        channelInfo.setBandwidth(channelBandwidth);
        if (!noiseList.isEmpty()) {
            channelInfo.setNoiseFloor((int) Math.round(DecibelUtils.getAverageDecibel(noiseList)));
        }
        return channelInfo;
    }

    void populateChannelInfoReports(List<ServiceMetric> metricRecordList, Report report, int customerId, long equipmentId, long locationId, long profileId) {

        LOG.info("populateChannelInfoReports for Customer {} Equipment {}", customerId, equipmentId);

        ProfileContainer profileContainer = new ProfileContainer(profileServiceInterface.getProfileWithChildren(profileId));

        Profile rfProfile = profileContainer.getChildOfTypeOrNull(profileId, ProfileType.rf);
        RfConfiguration rfConfig = null;
        if (rfProfile != null) {
            rfConfig = (RfConfiguration) profileContainer.getChildOfTypeOrNull(profileId, ProfileType.rf).getDetails();
        }

        if (rfConfig == null) {
            LOG.warn("Cannot get RfConfiguration for customerId {} equipmentId {}", customerId, equipmentId);
            return;
        }

        for (Survey survey : report.getSurveyList()) {

            ServiceMetric smr = new ServiceMetric();
            smr.setCustomerId(customerId);
            smr.setEquipmentId(equipmentId);
            smr.setLocationId(locationId);
            ChannelInfoReports channelInfoReports = new ChannelInfoReports();
            channelInfoReports.setSourceTimestampMs(survey.getTimestampMs());

            Map<RadioType, List<ChannelInfo>> channelInfoMap = channelInfoReports.getChannelInformationReportsPerRadio();

            RadioType radioType = null;

            if (survey.hasBand()) {
                radioType = OvsdbToWlanCloudTypeMappingUtility.getRadioTypeFromOpensyncStatsRadioBandType(survey.getBand());
            } else {
                continue;
            }

            ChannelBandwidth channelBandwidth = rfConfig.getRfConfig(radioType).getChannelBandwidth();

            Map<Integer, List<SurveySample>> sampleByChannelMap = new HashMap<>();

            survey.getSurveyListList().stream().filter(t -> {
                if (survey.getSurveyType().equals(SurveyType.ON_CHANNEL)) {
                    return t.hasDurationMs() && (t.getDurationMs() > 0) && t.hasChannel() && (t.hasBusy() || t.hasBusyTx() || t.hasBusySelf() || t.hasNoise());
                } else {
                    return t.hasDurationMs() && t.hasChannel();
                }
            }).forEach(s -> {
                List<SurveySample> surveySampleList;
                if (sampleByChannelMap.get(s.getChannel()) == null) {
                    surveySampleList = new ArrayList<>();
                } else {
                    surveySampleList = sampleByChannelMap.get(s.getChannel());
                }
                surveySampleList.add(s);
                sampleByChannelMap.put(s.getChannel(), surveySampleList);
            });

            for (List<SurveySample> surveySampleList : sampleByChannelMap.values()) {
                ChannelInfo channelInfo = createChannelInfo(equipmentId, radioType, surveySampleList, channelBandwidth);
                List<ChannelInfo> channelInfoList = channelInfoReports.getRadioInfo(radioType);
                if (channelInfoList == null) {
                    channelInfoList = new ArrayList<>();
                }
                channelInfoList.add(channelInfo);
                channelInfoMap.put(radioType, channelInfoList);
                channelInfoReports.setChannelInformationReportsPerRadio(channelInfoMap);
            }

            if (!channelInfoMap.isEmpty()) {
                channelInfoReports.setChannelInformationReportsPerRadio(channelInfoMap);
                smr.setDetails(channelInfoReports);
                metricRecordList.add(smr);
            }

            LOG.debug("ChannelInfoReports {}", channelInfoReports);

        }

    }

    int getNegativeSignedIntFrom8BitUnsigned(int unsignedValue) {
        byte b = (byte) Integer.parseInt(Integer.toHexString(unsignedValue), 16);
        return b;
    }

    /**
     * @param topic
     * @return apId extracted from the topic name, or null if it cannot be
     *         extracted
     */
    static String extractApIdFromTopic(String topic) {
        // Topic is formatted as
        // "/ap/"+clientCn+"_"+ret.serialNumber+"/opensync"
        if (topic == null) {
            return null;
        }

        String[] parts = topic.split("/");
        if (parts.length < 3) {
            return null;
        }

        // apId is the third element in the topic
        return parts[2];
    }

    /**
     * @param topic
     * @return customerId looked up from the topic name, or -1 if it cannot be
     *         extracted
     */
    int extractCustomerIdFromTopic(String topic) {

        String apId = extractApIdFromTopic(topic);
        if (apId == null) {
            return -1;
        }

        Equipment ce = equipmentServiceInterface.getByInventoryIdOrNull(apId);
        if (ce != null) {
            return ce.getCustomerId();
        }

        return -1;

    }

    long extractEquipmentIdFromTopic(String topic) {

        String apId = extractApIdFromTopic(topic);
        if (apId == null) {
            return -1;
        }

        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession != null) {
            return ovsdbSession.getEquipmentId();
        }

        return -1;

    }

    static int[] toIntArray(List<Integer> values) {
        if (values != null) {
            int returnValue[] = new int[values.size()];
            int index = 0;

            for (Integer value : values) {
                returnValue[index++] = value;
            }

            return returnValue;
        }
        return null;
    }

}
