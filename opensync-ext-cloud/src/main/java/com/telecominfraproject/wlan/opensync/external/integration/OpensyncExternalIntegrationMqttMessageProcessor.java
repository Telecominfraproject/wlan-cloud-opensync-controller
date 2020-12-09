package com.telecominfraproject.wlan.opensync.external.integration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.TypeRegistry;
import com.telecominfraproject.wlan.client.ClientServiceInterface;
import com.telecominfraproject.wlan.client.info.models.ClientInfoDetails;
import com.telecominfraproject.wlan.client.session.models.AssociationState;
import com.telecominfraproject.wlan.client.session.models.ClientEapDetails;
import com.telecominfraproject.wlan.client.session.models.ClientFailureDetails;
import com.telecominfraproject.wlan.client.session.models.ClientSession;
import com.telecominfraproject.wlan.client.session.models.ClientSessionDetails;
import com.telecominfraproject.wlan.client.session.models.ClientSessionMetricDetails;
import com.telecominfraproject.wlan.cloudeventdispatcher.CloudEventDispatcherInterface;
import com.telecominfraproject.wlan.core.model.equipment.ChannelBandwidth;
import com.telecominfraproject.wlan.core.model.equipment.ChannelHopReason;
import com.telecominfraproject.wlan.core.model.equipment.DetectedAuthMode;
import com.telecominfraproject.wlan.core.model.equipment.MacAddress;
import com.telecominfraproject.wlan.core.model.equipment.NeighborScanPacketType;
import com.telecominfraproject.wlan.core.model.equipment.NetworkType;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.core.model.equipment.SecurityType;
import com.telecominfraproject.wlan.core.model.equipment.WiFiSessionUtility;
import com.telecominfraproject.wlan.core.model.utils.DecibelUtils;
import com.telecominfraproject.wlan.equipment.EquipmentServiceInterface;
import com.telecominfraproject.wlan.equipment.models.ApElementConfiguration;
import com.telecominfraproject.wlan.equipment.models.Equipment;
import com.telecominfraproject.wlan.opensync.util.OvsdbToWlanCloudTypeMappingUtility;
import com.telecominfraproject.wlan.profile.ProfileServiceInterface;
import com.telecominfraproject.wlan.profile.models.Profile;
import com.telecominfraproject.wlan.profile.models.ProfileContainer;
import com.telecominfraproject.wlan.profile.models.ProfileType;
import com.telecominfraproject.wlan.profile.rf.models.RfConfiguration;
import com.telecominfraproject.wlan.profile.ssid.models.RadioBasedSsidConfiguration;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration.SecureMode;
import com.telecominfraproject.wlan.servicemetric.apnode.models.ApNodeMetrics;
import com.telecominfraproject.wlan.servicemetric.apnode.models.ApPerformance;
import com.telecominfraproject.wlan.servicemetric.apnode.models.DnsProbeMetric;
import com.telecominfraproject.wlan.servicemetric.apnode.models.EthernetLinkState;
import com.telecominfraproject.wlan.servicemetric.apnode.models.NetworkProbeMetrics;
import com.telecominfraproject.wlan.servicemetric.apnode.models.RadioStatistics;
import com.telecominfraproject.wlan.servicemetric.apnode.models.RadioUtilization;
import com.telecominfraproject.wlan.servicemetric.apnode.models.StateUpDownError;
import com.telecominfraproject.wlan.servicemetric.apssid.models.ApSsidMetrics;
import com.telecominfraproject.wlan.servicemetric.apssid.models.SsidStatistics;
import com.telecominfraproject.wlan.servicemetric.channelinfo.models.ChannelInfo;
import com.telecominfraproject.wlan.servicemetric.channelinfo.models.ChannelInfoReports;
import com.telecominfraproject.wlan.servicemetric.client.models.ClientMetrics;
import com.telecominfraproject.wlan.servicemetric.models.ServiceMetric;
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
import com.telecominfraproject.wlan.systemevent.equipment.realtime.RealTimeChannelHopEvent;
import com.telecominfraproject.wlan.systemevent.equipment.realtime.RealTimeEventType;
import com.telecominfraproject.wlan.systemevent.equipment.realtime.RealTimeSipCallReportEvent;
import com.telecominfraproject.wlan.systemevent.equipment.realtime.RealTimeSipCallStartEvent;
import com.telecominfraproject.wlan.systemevent.equipment.realtime.RealTimeSipCallStopEvent;
import com.telecominfraproject.wlan.systemevent.equipment.realtime.RealTimeStreamingStartEvent;
import com.telecominfraproject.wlan.systemevent.equipment.realtime.RealTimeStreamingStartSessionEvent;
import com.telecominfraproject.wlan.systemevent.equipment.realtime.RealTimeStreamingStopEvent;
import com.telecominfraproject.wlan.systemevent.equipment.realtime.SIPCallReportReason;
import com.telecominfraproject.wlan.systemevent.equipment.realtime.SipCallStopReason;
import com.telecominfraproject.wlan.systemevent.models.SystemEvent;

import sts.OpensyncStats;
import sts.OpensyncStats.AssocType;
import sts.OpensyncStats.CallReport;
import sts.OpensyncStats.CallStart;
import sts.OpensyncStats.CallStop;
import sts.OpensyncStats.ChannelSwitchReason;
import sts.OpensyncStats.Client;
import sts.OpensyncStats.ClientReport;
import sts.OpensyncStats.DNSProbeMetric;
import sts.OpensyncStats.Device;
import sts.OpensyncStats.Device.RadioTemp;
import sts.OpensyncStats.EventReport;
import sts.OpensyncStats.EventReport.ClientAssocEvent;
import sts.OpensyncStats.EventReport.ClientAuthEvent;
import sts.OpensyncStats.EventReport.ClientConnectEvent;
import sts.OpensyncStats.EventReport.ClientDisconnectEvent;
import sts.OpensyncStats.EventReport.ClientFailureEvent;
import sts.OpensyncStats.EventReport.ClientFirstDataEvent;
import sts.OpensyncStats.EventReport.ClientIdEvent;
import sts.OpensyncStats.EventReport.ClientIpEvent;
import sts.OpensyncStats.EventReport.ClientTimeoutEvent;
import sts.OpensyncStats.FrameType;
import sts.OpensyncStats.Neighbor;
import sts.OpensyncStats.Neighbor.NeighborBss;
import sts.OpensyncStats.NetworkProbe;
import sts.OpensyncStats.RADIUSMetrics;
import sts.OpensyncStats.RadioBandType;
import sts.OpensyncStats.Report;
import sts.OpensyncStats.RtpFlowStats;
import sts.OpensyncStats.StreamingVideoServerDetected;
import sts.OpensyncStats.StreamingVideoSessionStart;
import sts.OpensyncStats.StreamingVideoStop;
import sts.OpensyncStats.Survey;
import sts.OpensyncStats.Survey.SurveySample;
import sts.OpensyncStats.SurveyType;
import sts.OpensyncStats.VLANMetrics;
import sts.OpensyncStats.VideoVoiceReport;
import traffic.NetworkMetadata;
import traffic.NetworkMetadata.FlowReport;
import wc.stats.IpDnsTelemetry;
import wc.stats.IpDnsTelemetry.WCStatsReport;

@org.springframework.context.annotation.Profile("opensync_cloud_config")
@Component
public class OpensyncExternalIntegrationMqttMessageProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(OpensyncExternalIntegrationMqttMessageProcessor.class);

    @Autowired
    private EquipmentServiceInterface equipmentServiceInterface;
    @Autowired
    private OvsdbSessionMapInterface ovsdbSessionMapInterface;
    @Autowired
    private ProfileServiceInterface profileServiceInterface;
    @Autowired
    private StatusServiceInterface statusServiceInterface;
    @Autowired
    private ClientServiceInterface clientServiceInterface;
    @Autowired
    private CloudEventDispatcherInterface equipmentMetricsCollectorInterface;

    void processMqttMessage(String topic, WCStatsReport wcStatsReport) {
        LOG.info("Received WCStatsReport {}", wcStatsReport.toString());

        LOG.debug("Received report on topic {}", topic);
        int customerId = extractCustomerIdFromTopic(topic);

        long equipmentId = extractEquipmentIdFromTopic(topic);
        if ((equipmentId <= 0) || (customerId <= 0)) {
            LOG.warn("Cannot determine equipment ids from topic {} - customerId {} equipmentId {}", topic, customerId,
                    equipmentId);
            return;
        }

        String apId = extractApIdFromTopic(topic);

        if (apId == null) {
            LOG.warn("Cannot determine AP id from topic {} - customerId {} equipmentId {} apId {}", topic, customerId,
                    equipmentId, apId);
            return;
        }

        if (LOG.isTraceEnabled()) {
            // prepare a JSONPrinter to format protobuf messages as
            // json
            List<Descriptors.Descriptor> protobufDescriptors = new ArrayList<>();
            protobufDescriptors.addAll(IpDnsTelemetry.getDescriptor().getMessageTypes());
            TypeRegistry oldRegistry = TypeRegistry.newBuilder().add(protobufDescriptors).build();
            JsonFormat.Printer jsonPrinter = JsonFormat.printer().preservingProtoFieldNames()
                    .includingDefaultValueFields().usingTypeRegistry(oldRegistry);

            try {
                LOG.trace("MQTT IpDnsTelemetry.wcStatsReport = {}", jsonPrinter.print(wcStatsReport));

            } catch (InvalidProtocolBufferException e1) {
                LOG.error("Couldn't parse IpDnsTelemetry.wcStatsReport.", e1);
            }
        }

    }

    void processMqttMessage(String topic, Report report) {
        LOG.info("Received report on topic {} for ap {}", topic, report.getNodeID());
        int customerId = extractCustomerIdFromTopic(topic);
        String apId = extractApIdFromTopic(topic);
        long equipmentId = extractEquipmentIdFromTopic(topic);
        if ((equipmentId <= 0) || (customerId <= 0)) {
            LOG.warn("Cannot determine equipment ids from topic {} - customerId {} equipmentId {}", topic, customerId,
                    equipmentId);
            return;
        }

        // gatewayController.updateActiveCustomer(customerId);

        Equipment ce = equipmentServiceInterface.getOrNull(equipmentId);
        if (ce == null) {
            LOG.warn("Cannot read equipment {}", apId);
            return;
        }

        long locationId = ce.getLocationId();
        long profileId = ce.getProfileId();

        if (LOG.isTraceEnabled()) {
            // prepare a JSONPrinter to format protobuf messages as
            // json
            List<Descriptors.Descriptor> protobufDescriptors = new ArrayList<>();
            protobufDescriptors.addAll(OpensyncStats.getDescriptor().getMessageTypes());
            TypeRegistry oldRegistry = TypeRegistry.newBuilder().add(protobufDescriptors).build();
            JsonFormat.Printer jsonPrinter = JsonFormat.printer().preservingProtoFieldNames()
                    .includingDefaultValueFields().usingTypeRegistry(oldRegistry);

            try {
                LOG.trace("MQTT OpensyncStats.report = {}", jsonPrinter.print(report));

            } catch (InvalidProtocolBufferException e1) {
                LOG.error("Couldn't parse OpensyncStats.report.", e1);
            }
        }

        List<ServiceMetric> metricRecordList = new ArrayList<>();

        try {
            populateApClientMetrics(metricRecordList, report, customerId, equipmentId, locationId);
            populateApNodeMetrics(metricRecordList, report, customerId, equipmentId, locationId);
            populateNeighbourScanReports(metricRecordList, report, customerId, equipmentId, locationId);
            populateChannelInfoReports(metricRecordList, report, customerId, equipmentId, locationId, profileId);
            populateApSsidMetrics(metricRecordList, report, customerId, equipmentId, apId, locationId);
            // TODO: uncomment when AP support present
            populateSipCallReport(metricRecordList, report, customerId, equipmentId, apId, locationId);
            processEventReport(report, customerId, equipmentId, apId, locationId);
            // handleRssiMetrics(metricRecordList, report, customerId,
            // equipmentId, locationId);

        } catch (Exception e) {
            LOG.error("Exception when processing populateApSsidMetrics", e);
        }

        if (!metricRecordList.isEmpty()) {
            LOG.debug("Publishing Metrics {}", metricRecordList);
            equipmentMetricsCollectorInterface.publishMetrics(metricRecordList);
        }

    }

    void processMqttMessage(String topic, FlowReport flowReport) {

        LOG.info("Received report on topic {}", topic);
        int customerId = extractCustomerIdFromTopic(topic);

        long equipmentId = extractEquipmentIdFromTopic(topic);
        if ((equipmentId <= 0) || (customerId <= 0)) {
            LOG.warn("Cannot determine equipment ids from topic {} - customerId {} equipmentId {}", topic, customerId,
                    equipmentId);
            return;
        }

        String apId = extractApIdFromTopic(topic);

        if (apId == null) {
            LOG.warn("Cannot determine AP id from topic {} - customerId {} equipmentId {} apId {}", topic, customerId,
                    equipmentId, apId);
            return;
        }

        if (LOG.isTraceEnabled()) {
            // prepare a JSONPrinter to format protobuf messages as
            // json
            List<Descriptors.Descriptor> protobufDescriptors = new ArrayList<>();
            protobufDescriptors.addAll(NetworkMetadata.getDescriptor().getMessageTypes());
            TypeRegistry oldRegistry = TypeRegistry.newBuilder().add(protobufDescriptors).build();
            JsonFormat.Printer jsonPrinter = JsonFormat.printer().preservingProtoFieldNames()
                    .includingDefaultValueFields().usingTypeRegistry(oldRegistry);

            try {
                LOG.trace("MQTT NetworkMetadata.flowReport = {}", jsonPrinter.print(flowReport));

            } catch (InvalidProtocolBufferException e1) {
                LOG.error("Couldn't parse NetworkMetadata.flowReport.", e1);
            }
        }

    }

    void processEventReport(Report report, int customerId, long equipmentId, String apId, long locationId) {

        report.getEventReportList().stream().forEach(e -> {

            for (sts.OpensyncStats.EventReport.ClientSession apEventClientSession : e.getClientSessionList()) {

                LOG.debug("Processing EventReport::ClientSession {}", apEventClientSession);

                processClientConnectEvent(customerId, equipmentId, locationId, e, apEventClientSession);

                processClientDisconnectEvent(customerId, equipmentId, locationId, apEventClientSession);

                processClientAuthEvent(customerId, equipmentId, locationId, apEventClientSession);

                processClientAssocEvent(customerId, equipmentId, locationId, apEventClientSession);

                processClientFailureEvent(customerId, equipmentId, locationId, apEventClientSession);

                processClientFirstDataEvent(customerId, equipmentId, locationId, apEventClientSession);

                processClientIdEvent(customerId, equipmentId, locationId, apEventClientSession);

                processClientIpEvent(customerId, equipmentId, locationId, apEventClientSession);

                processClientTimeoutEvent(customerId, equipmentId, locationId, apEventClientSession);

            }

            publishChannelHopEvents(customerId, equipmentId, e);

        });

    }

    private void publishChannelHopEvents(int customerId, long equipmentId, EventReport e) {
        
        LOG.info("publishChannelHopEvents for customerId {} equipmentId {}");
        
        List<SystemEvent> events = new ArrayList<>();
        
        for (sts.OpensyncStats.EventReport.ChannelSwitchEvent channelSwitchEvent : e.getChannelSwitchList()) {
            Equipment equipment = equipmentServiceInterface.getOrNull(equipmentId);
            if (equipment == null)
                continue;
            RadioType radioType = null;
            Long timestamp = null;
            ChannelHopReason reason = null;
            Integer channel = null;
            if (channelSwitchEvent.hasBand()) {                   
               radioType = OvsdbToWlanCloudTypeMappingUtility
                .getRadioTypeFromOpensyncStatsRadioBandType(channelSwitchEvent.getBand());            
            }
            if (RadioType.isUnsupported(radioType)) {
                LOG.warn("publishChannelHopEvents:RadioType {} is unsupported, cannot send RealTimeChannelHopEvent for {}", radioType, channelSwitchEvent);
                continue;
            }
            if (channelSwitchEvent.hasTimestampMs()) {
                timestamp = channelSwitchEvent.getTimestampMs();
            }
            if (timestamp == null) {
                LOG.warn("publishChannelHopEvents:timestamp is null, cannot send RealTimeChannelHopEvent for {}", channelSwitchEvent);
                continue;  
            }
            
            if (channelSwitchEvent.hasReason()) {
                if (channelSwitchEvent.getReason().equals(ChannelSwitchReason.high_interference)) reason = ChannelHopReason.HighInterference;
                else if (channelSwitchEvent.getReason().equals(ChannelSwitchReason.radar_detected)) reason = ChannelHopReason.RadarDetected;
            }
            if (ChannelHopReason.isUnsupported(reason)) {
                LOG.warn("publishChannelHopEvents:reason {} is unsupported, cannot send RealTimeChannelHopEvent for {}", channelSwitchEvent.getReason(), channelSwitchEvent);
                continue; 
            }
            if (channelSwitchEvent.hasChannel()) {
                channel = channelSwitchEvent.getChannel();
            }
            if (channel == null) {
                LOG.warn("publishChannelHopEvents:channel is null, cannot send RealTimeChannelHopEvent for {}", channelSwitchEvent);
                continue; 
            }

            RealTimeChannelHopEvent channelHopEvent = new RealTimeChannelHopEvent(RealTimeEventType.Channel_Hop, customerId, equipmentId, radioType, channel, ((ApElementConfiguration)equipment.getDetails()).getRadioMap().get(radioType).getChannelNumber(), reason, timestamp);
            
            events.add(channelHopEvent);
            
            LOG.debug("publishChannelHopEvents:Adding ChannelHopEvent to bulk list {}", channelHopEvent);
        }
        

        if (events.size() > 0) {
            LOG.info("publishChannelHopEvents:publishEventsBulk: {}", events);
            equipmentMetricsCollectorInterface.publishEventsBulk(events);
        } else {
            LOG.info("publishChannelHopEvents:No ChannelHopEvents in report");
        }
    }

    protected void processClientConnectEvent(int customerId, long equipmentId, long locationId, EventReport e,
            sts.OpensyncStats.EventReport.ClientSession apEventClientSession) {
        for (ClientConnectEvent clientConnectEvent : apEventClientSession.getClientConnectEventList()) {

            if (clientConnectEvent.hasStaMac()) {
                com.telecominfraproject.wlan.client.models.Client client = clientServiceInterface.getOrNull(customerId,
                        new MacAddress(clientConnectEvent.getStaMac()));
                if (client == null) {
                    client = new com.telecominfraproject.wlan.client.models.Client();

                    client.setCustomerId(customerId);
                    client.setMacAddress(new MacAddress(clientConnectEvent.getStaMac()));

                    client.setDetails(new ClientInfoDetails());

                    client = clientServiceInterface.create(client);

                }

                ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId,
                        new MacAddress(clientConnectEvent.getStaMac()));

                if (clientSession == null) {
                    clientSession = new ClientSession();
                }

                clientSession.setCustomerId(customerId);
                clientSession.setEquipmentId(equipmentId);
                clientSession.setLocationId(locationId);
                clientSession.setMacAddress(new MacAddress(clientConnectEvent.getStaMac()));

                ClientSessionDetails clientSessionDetails = new ClientSessionDetails();
                clientSessionDetails.setSessionId(apEventClientSession.getSessionId());

                if (clientConnectEvent.hasFbtUsed()) {

                    // TODO: mapping?

                }
                if (clientConnectEvent.hasEvTimeBootupInUsAssoc()) {
                    clientSessionDetails.setAssocTimestamp(clientConnectEvent.getEvTimeBootupInUsAssoc());
                }

                if (clientConnectEvent.hasEvTimeBootupInUsAuth()) {
                    clientSessionDetails.setAuthTimestamp(clientConnectEvent.getEvTimeBootupInUsAuth());
                }

                if (clientConnectEvent.hasEvTimeBootupInUsEapol()) {
                    ClientEapDetails eapDetails = new ClientEapDetails();
                    eapDetails.setEapSuccessTimestamp(clientConnectEvent.getEvTimeBootupInUsEapol());
                    clientSessionDetails.setEapDetails(eapDetails);
                }

                if (clientConnectEvent.hasEvTimeBootupInUsFirstRx()) {
                    clientSessionDetails.setFirstDataRcvdTimestamp(clientConnectEvent.getEvTimeBootupInUsFirstRx());
                }

                if (clientConnectEvent.hasEvTimeBootupInUsFirstTx()) {
                    clientSessionDetails.setFirstDataSentTimestamp(clientConnectEvent.getEvTimeBootupInUsFirstTx());
                }

                if (clientConnectEvent.hasEvTimeBootupInUsIp()) {
                    clientSessionDetails.setIpTimestamp(clientConnectEvent.getEvTimeBootupInUsIp());
                }

                if (clientConnectEvent.hasEvTimeBootupInUsPortEnable()) {
                    clientSessionDetails.setPortEnabledTimestamp(clientConnectEvent.getEvTimeBootupInUsPortEnable());
                }

                if (clientConnectEvent.hasCltId()) {
                    clientSessionDetails.setHostname(clientConnectEvent.getCltId());
                }

                if (clientConnectEvent.hasSecType()) {
                    clientSessionDetails.setSecurityType(OvsdbToWlanCloudTypeMappingUtility
                            .getCloudSecurityTypeFromOpensyncStats(clientConnectEvent.getSecType()));
                }

                if (clientConnectEvent.hasBand()) {
                    clientSessionDetails.setRadioType(OvsdbToWlanCloudTypeMappingUtility
                            .getRadioTypeFromOpensyncStatsRadioBandType(clientConnectEvent.getBand()));
                }

                if (clientConnectEvent.hasAssocType()) {
                    clientSessionDetails
                            .setIsReassociation(clientConnectEvent.getAssocType().equals(AssocType.REASSOC));

                }

                if (clientConnectEvent.hasAssocRssi()) {
                    clientSessionDetails.setAssocRssi(clientConnectEvent.getAssocRssi());
                }

                if (clientConnectEvent.hasSsid()) {
                    clientSessionDetails.setSsid(clientConnectEvent.getSsid());
                }

                if (clientConnectEvent.hasUsing11K()) {
                    clientSessionDetails.setIs11KUsed(clientConnectEvent.getUsing11K());
                }

                if (clientConnectEvent.hasUsing11R()) {
                    clientSessionDetails.setIs11RUsed(clientConnectEvent.getUsing11R());

                }

                if (clientConnectEvent.hasUsing11V()) {
                    clientSessionDetails.setIs11VUsed(clientConnectEvent.getUsing11V());
                }

                if (clientConnectEvent.hasIpAddr()) {
                    try {
                        clientSessionDetails
                                .setIpAddress(InetAddress.getByAddress(clientConnectEvent.getIpAddr().toByteArray()));
                    } catch (UnknownHostException e1) {
                        LOG.error("Invalid Ip Address for client {}", clientConnectEvent.getIpAddr(), e);
                    }
                }
                clientSessionDetails.setAssociationState(AssociationState._802_11_Associated);

                if (clientSession.getDetails() == null) {
                    clientSession.setDetails(clientSessionDetails);
                } else {
                    clientSession.getDetails().mergeSession(clientSessionDetails);
                }

                clientSession = clientServiceInterface.updateSession(clientSession);

            }

        }
    }

    protected void processClientDisconnectEvent(int customerId, long equipmentId, long locationId,
            sts.OpensyncStats.EventReport.ClientSession apEventClientSession) {
        for (ClientDisconnectEvent clientDisconnectEvent : apEventClientSession.getClientDisconnectEventList()) {

            if (clientDisconnectEvent.hasStaMac()) {

                com.telecominfraproject.wlan.client.models.Client client = clientServiceInterface.getOrNull(customerId,
                        new MacAddress(clientDisconnectEvent.getStaMac()));
                if (client == null) {
                    client = new com.telecominfraproject.wlan.client.models.Client();

                    client.setCustomerId(customerId);
                    client.setMacAddress(new MacAddress(clientDisconnectEvent.getStaMac()));

                    client.setDetails(new ClientInfoDetails());

                    client = clientServiceInterface.create(client);

                }

                ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId,
                        new MacAddress(clientDisconnectEvent.getStaMac()));

                if (clientSession == null) {
                    clientSession = new ClientSession();
                }

                clientSession.setCustomerId(customerId);
                clientSession.setEquipmentId(equipmentId);
                clientSession.setLocationId(locationId);
                clientSession.setMacAddress(new MacAddress(clientDisconnectEvent.getStaMac()));

                ClientSessionDetails clientSessionDetails = new ClientSessionDetails();

                if (clientDisconnectEvent.hasBand()) {
                    clientSessionDetails.setRadioType(OvsdbToWlanCloudTypeMappingUtility
                            .getRadioTypeFromOpensyncStatsRadioBandType(clientDisconnectEvent.getBand()));
                }
                if (clientDisconnectEvent.hasDevType()) {
                }
                if (clientDisconnectEvent.hasFrType()) {
                    if (clientDisconnectEvent.getFrType().equals(FrameType.FT_DEAUTH)) {
                    }
                    if (clientDisconnectEvent.getFrType().equals(FrameType.FT_DISASSOC)) {
                    }
                }
                if (clientDisconnectEvent.hasRssi()) {
                    clientSessionDetails.setAssocRssi(clientDisconnectEvent.getRssi());
                }

                clientSessionDetails.setSessionId(apEventClientSession.getSessionId());

                if (clientDisconnectEvent.hasLrcvUpTsInUs()) {
                    clientSessionDetails.setLastRxTimestamp(clientDisconnectEvent.getLrcvUpTsInUs());
                }

                if (clientDisconnectEvent.hasLsentUpTsInUs()) {
                    clientSessionDetails.setLastTxTimestamp(clientDisconnectEvent.getLsentUpTsInUs());
                }

                if (clientDisconnectEvent.hasInternalRc()) {
                    clientSessionDetails.setDisconnectByClientInternalReasonCode(clientDisconnectEvent.getInternalRc());
                }
                if (clientDisconnectEvent.hasReason()) {
                    clientSessionDetails.setDisconnectByClientReasonCode(clientDisconnectEvent.getReason());

                }
                clientSessionDetails.setAssociationState(AssociationState.Disconnected);

                if (clientSession.getDetails() == null) {
                    clientSession.setDetails(clientSessionDetails);
                } else {
                    clientSession.getDetails().mergeSession(clientSessionDetails);
                }

                clientSession = clientServiceInterface.updateSession(clientSession);

            } else {
                LOG.warn("Cannot update client or client session when no client mac address is present");
            }
        }
    }

    protected void processClientAuthEvent(int customerId, long equipmentId, long locationId,
            sts.OpensyncStats.EventReport.ClientSession apEventClientSession) {
        for (ClientAuthEvent clientAuthEvent : apEventClientSession.getClientAuthEventList()) {
            if (clientAuthEvent.hasStaMac()) {

                com.telecominfraproject.wlan.client.models.Client client = clientServiceInterface.getOrNull(customerId,
                        new MacAddress(clientAuthEvent.getStaMac()));
                if (client == null) {
                    client = new com.telecominfraproject.wlan.client.models.Client();

                    client.setCustomerId(customerId);
                    client.setMacAddress(new MacAddress(clientAuthEvent.getStaMac()));

                    client.setDetails(new ClientInfoDetails());

                    client = clientServiceInterface.create(client);

                }

                ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId,
                        new MacAddress(clientAuthEvent.getStaMac()));

                if (clientSession == null) {
                    clientSession = new ClientSession();
                }

                clientSession.setCustomerId(customerId);
                clientSession.setEquipmentId(equipmentId);
                clientSession.setLocationId(locationId);
                clientSession.setMacAddress(new MacAddress(clientAuthEvent.getStaMac()));

                ClientSessionDetails clientSessionDetails = new ClientSessionDetails();

                clientSessionDetails.setSessionId(apEventClientSession.getSessionId());

                if (clientAuthEvent.hasBand()) {
                    clientSessionDetails.setRadioType(OvsdbToWlanCloudTypeMappingUtility
                            .getRadioTypeFromOpensyncStatsRadioBandType(clientAuthEvent.getBand()));
                }
                if (clientAuthEvent.hasSsid()) {
                    clientSessionDetails.setSsid(clientAuthEvent.getSsid());
                }
                if (clientAuthEvent.hasAuthStatus()) {
                    clientSessionDetails.setAssociationState(AssociationState._802_11_Authenticated);
                }

                if (clientSession.getDetails() == null) {
                    clientSession.setDetails(clientSessionDetails);
                } else {
                    clientSession.getDetails().mergeSession(clientSessionDetails);
                }

                clientSession = clientServiceInterface.updateSession(clientSession);

            } else {
                LOG.warn("Cannot update client or client session when no client mac address is present");
            }
        }
    }

    protected void processClientAssocEvent(int customerId, long equipmentId, long locationId,
            sts.OpensyncStats.EventReport.ClientSession apEventClientSession) {
        for (ClientAssocEvent clientAssocEvent : apEventClientSession.getClientAssocEventList()) {
            if (clientAssocEvent.hasStaMac()) {
                com.telecominfraproject.wlan.client.models.Client client = clientServiceInterface.getOrNull(customerId,
                        new MacAddress(clientAssocEvent.getStaMac()));
                if (client == null) {
                    client = new com.telecominfraproject.wlan.client.models.Client();

                    client.setCustomerId(customerId);
                    client.setMacAddress(new MacAddress(clientAssocEvent.getStaMac()));

                    client.setDetails(new ClientInfoDetails());

                    client = clientServiceInterface.create(client);

                }

                ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId,
                        new MacAddress(clientAssocEvent.getStaMac()));

                if (clientSession == null) {
                    clientSession = new ClientSession();
                }
                clientSession.setCustomerId(customerId);
                clientSession.setEquipmentId(equipmentId);
                clientSession.setLocationId(locationId);
                clientSession.setMacAddress(new MacAddress(clientAssocEvent.getStaMac()));

                ClientSessionDetails clientSessionDetails = new ClientSessionDetails();
                if (clientAssocEvent.hasUsing11K()) {
                    clientSessionDetails.setIs11KUsed(clientAssocEvent.getUsing11K());
                }
                if (clientAssocEvent.hasUsing11R()) {
                    clientSessionDetails.setIs11RUsed(clientAssocEvent.getUsing11R());
                }
                if (clientAssocEvent.hasUsing11V()) {
                    clientSessionDetails.setIs11VUsed(clientAssocEvent.getUsing11V());
                }
                if (clientAssocEvent.hasAssocType()) {
                    clientSessionDetails.setIsReassociation(clientAssocEvent.getAssocType().equals(AssocType.REASSOC));
                }
                if (clientAssocEvent.hasBand()) {
                    clientSessionDetails.setRadioType(OvsdbToWlanCloudTypeMappingUtility
                            .getRadioTypeFromOpensyncStatsRadioBandType(clientAssocEvent.getBand()));
                }
                if (clientAssocEvent.hasRssi()) {
                    clientSessionDetails.setAssocRssi(clientAssocEvent.getRssi());
                }
                clientSessionDetails.setSessionId(apEventClientSession.getSessionId());

                if (clientAssocEvent.hasSsid()) {
                    clientSessionDetails.setSsid(clientAssocEvent.getSsid());
                }
                if (clientAssocEvent.hasStatus()) {
                    clientSessionDetails.setAssociationStatus(clientAssocEvent.getStatus());
                    clientSessionDetails.setAssociationState(AssociationState._802_11_Associated);
                }

                if (clientSession.getDetails() == null) {
                    clientSession.setDetails(clientSessionDetails);
                } else {
                    clientSession.getDetails().mergeSession(clientSessionDetails);
                }

                clientSession = clientServiceInterface.updateSession(clientSession);

            } else {
                LOG.warn("Cannot update client or client session when no client mac address is present");
            }
        }
    }

    protected void processClientFailureEvent(int customerId, long equipmentId, long locationId,
            sts.OpensyncStats.EventReport.ClientSession apEventClientSession) {
        for (ClientFailureEvent clientFailureEvent : apEventClientSession.getClientFailureEventList()) {
            if (clientFailureEvent.hasStaMac()) {

                com.telecominfraproject.wlan.client.models.Client client = clientServiceInterface.getOrNull(customerId,
                        new MacAddress(clientFailureEvent.getStaMac()));
                if (client == null) {
                    client = new com.telecominfraproject.wlan.client.models.Client();

                    client.setCustomerId(customerId);
                    client.setMacAddress(new MacAddress(clientFailureEvent.getStaMac()));

                    client.setDetails(new ClientInfoDetails());

                    client = clientServiceInterface.create(client);

                }

                ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId,
                        new MacAddress(clientFailureEvent.getStaMac()));

                if (clientSession == null) {
                    clientSession = new ClientSession();
                }

                clientSession.setCustomerId(customerId);
                clientSession.setEquipmentId(equipmentId);
                clientSession.setLocationId(locationId);
                clientSession.setMacAddress(new MacAddress(clientFailureEvent.getStaMac()));

                ClientSessionDetails clientSessionDetails = new ClientSessionDetails();

                if (clientFailureEvent.hasSsid()) {
                    clientSessionDetails.setSsid(clientFailureEvent.getSsid());
                }

                clientSessionDetails.setSessionId(apEventClientSession.getSessionId());

                ClientFailureDetails clientFailureDetails = new ClientFailureDetails();
                if (clientFailureEvent.hasReasonStr()) {
                    clientFailureDetails.setReason(clientFailureEvent.getReasonStr());
                }
                if (clientFailureEvent.hasReasonCode()) {
                    clientFailureDetails.setReasonCode(clientFailureEvent.getReasonCode());
                }
                clientSessionDetails.setLastFailureDetails(clientFailureDetails);

                if (clientSession.getDetails() == null) {
                    clientSession.setDetails(clientSessionDetails);
                } else {
                    clientSession.getDetails().mergeSession(clientSessionDetails);
                }

                clientSession = clientServiceInterface.updateSession(clientSession);

            } else {
                LOG.warn("Cannot update client or client session when no client mac address is present");
            }
        }
    }

    protected void processClientFirstDataEvent(int customerId, long equipmentId, long locationId,
            sts.OpensyncStats.EventReport.ClientSession apEventClientSession) {
        for (ClientFirstDataEvent clientFirstDataEvent : apEventClientSession.getClientFirstDataEventList()) {
            if (clientFirstDataEvent.hasStaMac()) {

                com.telecominfraproject.wlan.client.models.Client client = clientServiceInterface.getOrNull(customerId,
                        new MacAddress(clientFirstDataEvent.getStaMac()));
                if (client == null) {
                    client = new com.telecominfraproject.wlan.client.models.Client();

                    client.setCustomerId(customerId);
                    client.setMacAddress(new MacAddress(clientFirstDataEvent.getStaMac()));

                    client.setDetails(new ClientInfoDetails());

                    client = clientServiceInterface.create(client);

                }

                ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId,
                        new MacAddress(clientFirstDataEvent.getStaMac()));

                if (clientSession == null) {
                    clientSession = new ClientSession();
                }

                clientSession.setCustomerId(customerId);
                clientSession.setEquipmentId(equipmentId);
                clientSession.setLocationId(locationId);
                clientSession.setMacAddress(new MacAddress(clientFirstDataEvent.getStaMac()));

                ClientSessionDetails clientSessionDetails = new ClientSessionDetails();

                if (clientFirstDataEvent.hasFdataRxUpTsInUs()) {
                    clientSessionDetails.setFirstDataRcvdTimestamp(clientFirstDataEvent.getFdataRxUpTsInUs());
                }

                if (clientFirstDataEvent.hasFdataTxUpTsInUs()) {
                    clientSessionDetails.setFirstDataSentTimestamp(clientFirstDataEvent.getFdataTxUpTsInUs());
                }

                clientSessionDetails.setSessionId(apEventClientSession.getSessionId());

                clientSessionDetails.setAssociationState(AssociationState.Active_Data);

                if (clientSession.getDetails() == null) {
                    clientSession.setDetails(clientSessionDetails);
                } else {
                    clientSession.getDetails().mergeSession(clientSessionDetails);
                }

                clientSession = clientServiceInterface.updateSession(clientSession);

            } else {
                LOG.warn("Cannot update client or client session when no client mac address is present");
            }
        }
    }

    protected void processClientIdEvent(int customerId, long equipmentId, long locationId,
            sts.OpensyncStats.EventReport.ClientSession apEventClientSession) {
        for (ClientIdEvent clientIdEvent : apEventClientSession.getClientIdEventList()) {
            if (clientIdEvent.hasCltMac()) {

                com.telecominfraproject.wlan.client.models.Client client = clientServiceInterface.getOrNull(customerId,
                        new MacAddress(clientIdEvent.getCltMac()));
                if (client == null) {
                    client = new com.telecominfraproject.wlan.client.models.Client();

                    client.setCustomerId(customerId);
                    client.setMacAddress(new MacAddress(clientIdEvent.getCltMac()));

                    client.setDetails(new ClientInfoDetails());

                    client = clientServiceInterface.create(client);

                }

                ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId,
                        new MacAddress(clientIdEvent.getCltMac()));

                if (clientSession == null) {
                    clientSession = new ClientSession();
                }

                clientSession.setCustomerId(customerId);
                clientSession.setEquipmentId(equipmentId);
                clientSession.setLocationId(locationId);
                clientSession.setMacAddress(new MacAddress(clientIdEvent.getCltMac()));

                ClientSessionDetails clientSessionDetails = new ClientSessionDetails();

                clientSessionDetails.setSessionId(apEventClientSession.getSessionId());

                if (clientSession.getDetails() == null) {
                    clientSession.setDetails(clientSessionDetails);
                } else {
                    clientSession.getDetails().mergeSession(clientSessionDetails);
                }

                clientSession = clientServiceInterface.updateSession(clientSession);

            } else {
                LOG.warn("Cannot update client or client session when no client mac address is present");
            }
        }
    }

    protected void processClientIpEvent(int customerId, long equipmentId, long locationId,
            sts.OpensyncStats.EventReport.ClientSession apEventClientSession) {
        for (ClientIpEvent clientIpEvent : apEventClientSession.getClientIpEventList()) {
            if (clientIpEvent.hasStaMac()) {

                com.telecominfraproject.wlan.client.models.Client client = clientServiceInterface.getOrNull(customerId,
                        new MacAddress(clientIpEvent.getStaMac()));
                if (client == null) {
                    client = new com.telecominfraproject.wlan.client.models.Client();

                    client.setCustomerId(customerId);
                    client.setMacAddress(new MacAddress(clientIpEvent.getStaMac()));

                    client.setDetails(new ClientInfoDetails());

                    client = clientServiceInterface.create(client);

                }

                ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId,
                        new MacAddress(clientIpEvent.getStaMac()));

                if (clientSession == null) {
                    clientSession = new ClientSession();
                }

                clientSession.setCustomerId(customerId);
                clientSession.setEquipmentId(equipmentId);
                clientSession.setLocationId(locationId);
                clientSession.setMacAddress(new MacAddress(clientIpEvent.getStaMac()));

                ClientSessionDetails clientSessionDetails = new ClientSessionDetails();

                clientSessionDetails.setSessionId(apEventClientSession.getSessionId());

                try {
                    clientSessionDetails
                            .setIpAddress(InetAddress.getByAddress(clientIpEvent.getIpAddr().toByteArray()));
                } catch (UnknownHostException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

                if (clientSession.getDetails() == null) {
                    clientSession.setDetails(clientSessionDetails);
                } else {
                    clientSession.getDetails().mergeSession(clientSessionDetails);
                }

                clientSession = clientServiceInterface.updateSession(clientSession);

            } else {
                LOG.warn("Cannot update client or client session when no clientmac address is present");
            }
        }
    }

    protected void processClientTimeoutEvent(int customerId, long equipmentId, long locationId,
            sts.OpensyncStats.EventReport.ClientSession apEventClientSession) {
        for (ClientTimeoutEvent clientTimeoutEvent : apEventClientSession.getClientTimeoutEventList()) {
            if (clientTimeoutEvent.hasStaMac()) {

                com.telecominfraproject.wlan.client.models.Client client = clientServiceInterface.getOrNull(customerId,
                        new MacAddress(clientTimeoutEvent.getStaMac()));
                if (client == null) {
                    client = new com.telecominfraproject.wlan.client.models.Client();

                    client.setCustomerId(customerId);
                    client.setMacAddress(new MacAddress(clientTimeoutEvent.getStaMac()));

                    client.setDetails(new ClientInfoDetails());

                    client = clientServiceInterface.create(client);

                }

                ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId,
                        new MacAddress(clientTimeoutEvent.getStaMac()));

                if (clientSession == null) {
                    clientSession = new ClientSession();
                }

                clientSession.setCustomerId(customerId);
                clientSession.setEquipmentId(equipmentId);
                clientSession.setLocationId(locationId);
                clientSession.setMacAddress(new MacAddress(clientTimeoutEvent.getStaMac()));

                ClientSessionDetails clientSessionDetails = new ClientSessionDetails();

                clientSessionDetails.setSessionId(apEventClientSession.getSessionId());

                long timeoutTimestamp = 0L;
                if (clientTimeoutEvent.hasLastRcvUpTsInUs()) {
                    clientSessionDetails.setLastRxTimestamp(clientTimeoutEvent.getLastRcvUpTsInUs());

                    timeoutTimestamp = clientTimeoutEvent.getLastRcvUpTsInUs();
                }

                if (clientTimeoutEvent.hasLastSentUpTsInUs()) {
                    clientSessionDetails.setLastTxTimestamp(clientTimeoutEvent.getLastSentUpTsInUs());
                    if (clientTimeoutEvent.getLastSentUpTsInUs() > timeoutTimestamp) {
                        timeoutTimestamp = clientTimeoutEvent.getLastSentUpTsInUs();
                    }
                }

                if (timeoutTimestamp > 0L) {
                    clientSessionDetails.setTimeoutTimestamp(timeoutTimestamp);
                }

                if (clientSession.getDetails() == null) {
                    clientSession.setDetails(clientSessionDetails);
                } else {
                    clientSession.getDetails().mergeSession(clientSessionDetails);
                }

                clientSession = clientServiceInterface.updateSession(clientSession);

            } else {
                LOG.warn("Cannot update client or client session when no client mac address is present");
            }
        }
    }

    void populateSipCallReport(List<ServiceMetric> metricRecordList, Report report, int customerId, long equipmentId,
            String apId, long locationId) {
        // only in case it is not there, we will just use the time when we
        // received the report/event
        long eventTimestamp = System.currentTimeMillis();

        List<SystemEvent> eventsList = new ArrayList<>();
        for (VideoVoiceReport videoVoiceReport : report.getVideoVoiceReportList()) {

            if (videoVoiceReport.hasTimestampMs()) {
                eventTimestamp = videoVoiceReport.getTimestampMs();
            }

            LOG.debug("Received VideoVoiceReport {} for SIP call", videoVoiceReport);

            processRealTimeSipCallReportEvent(customerId, equipmentId, eventTimestamp, eventsList, videoVoiceReport);

            processRealTimeSipCallStartEvent(customerId, equipmentId, eventTimestamp, eventsList, videoVoiceReport);

            processRealTimeSipCallStopEvent(customerId, equipmentId, eventTimestamp, eventsList, videoVoiceReport);

            processRtsStartEvent(customerId, equipmentId, eventTimestamp, eventsList, videoVoiceReport);

            processRtsStartSessionEvent(customerId, equipmentId, eventTimestamp, eventsList, videoVoiceReport);

            processRtsStopEvent(customerId, equipmentId, eventTimestamp, eventsList, videoVoiceReport);

        }

        if (eventsList.size() > 0) {
            equipmentMetricsCollectorInterface.publishEventsBulk(eventsList);
        }

    }

    protected void processRealTimeSipCallReportEvent(int customerId, long equipmentId, long eventTimestamp,
                                                     List<SystemEvent> eventsList, VideoVoiceReport videoVoiceReport) {
        if (videoVoiceReport.hasCallReport()) {

            CallReport callReport = videoVoiceReport.getCallReport();

            RealTimeSipCallReportEvent cloudSipCallReportEvent = new RealTimeSipCallReportEvent(customerId, equipmentId,
                    eventTimestamp);

            if (callReport.hasClientMac() && callReport.getClientMac().isValidUtf8()) {
                cloudSipCallReportEvent.setClientMacAddress(MacAddress.valueOf(callReport.getClientMac().toStringUtf8()));
            }
            cloudSipCallReportEvent.setStatuses(processRtpFlowStats(callReport.getStatsList()));
            cloudSipCallReportEvent.setEventType(RealTimeEventType.SipCallReport);

            cloudSipCallReportEvent.setSipCallId(callReport.getWifiSessionId());
            cloudSipCallReportEvent.setAssociationId(callReport.getSessionId());

            if (callReport.hasReason()) {
                cloudSipCallReportEvent.setReportReason(getCallReportReason(callReport.getReason()));
            }
            if (callReport.hasProviderDomain()) {
                cloudSipCallReportEvent.setProviderDomain(callReport.getProviderDomain());
            }

            if (callReport.getCodecsCount() > 0) {
                cloudSipCallReportEvent.setCodecs(callReport.getCodecsList());
            }

            if (callReport.hasChannel()) {
                cloudSipCallReportEvent.setChannel(callReport.getChannel());
            }

            if (callReport.hasBand()) {
                cloudSipCallReportEvent.setRadioType(OvsdbToWlanCloudTypeMappingUtility
                        .getRadioTypeFromOpensyncStatsRadioBandType(callReport.getBand()));
            }

            eventsList.add(cloudSipCallReportEvent);

        }
    }

    private SIPCallReportReason getCallReportReason(CallReport.CallReportReason reason) {
        if (reason != null) {
            switch (reason) {
                case ROAMED_TO:
                    return SIPCallReportReason.ROAMED_TO;
                case GOT_PUBLISH:
                    return SIPCallReportReason.GOT_PUBLISH;
                case ROAMED_FROM:
                    return SIPCallReportReason.ROAMED_FROM;
                default:
                    return SIPCallReportReason.UNSUPPORTED;
            }
        }
        return SIPCallReportReason.UNSUPPORTED;
    }

    protected void processRealTimeSipCallStartEvent(int customerId, long equipmentId, long eventTimestamp,
            List<SystemEvent> eventsList, VideoVoiceReport videoVoiceReport) {
        if (videoVoiceReport.hasCallStart()) {

            CallStart apCallStart = videoVoiceReport.getCallStart();

            RealTimeSipCallStartEvent cloudSipCallStartEvent = new RealTimeSipCallStartEvent(customerId, equipmentId,
                    eventTimestamp);

            if (apCallStart.hasClientMac() && apCallStart.getClientMac().isValidUtf8()) {
                cloudSipCallStartEvent.setClientMacAddress(MacAddress.valueOf(apCallStart.getClientMac().toStringUtf8()));
            }

            if (apCallStart.hasDeviceInfo()) {
                cloudSipCallStartEvent.setDeviceInfo(apCallStart.getDeviceInfo());
            }

            if (apCallStart.hasProviderDomain()) {
                cloudSipCallStartEvent.setProviderDomain(apCallStart.getProviderDomain());
            }

            if (apCallStart.hasSessionId()) {
                cloudSipCallStartEvent.setAssociationId(apCallStart.getSessionId());
            }

            if (apCallStart.hasWifiSessionId()) {
                cloudSipCallStartEvent.setAssociationId(apCallStart.getWifiSessionId());
            }

            if (apCallStart.getCodecsCount() > 0) {
                cloudSipCallStartEvent.setCodecs(apCallStart.getCodecsList());
            }

            if (apCallStart.hasChannel()) {
                cloudSipCallStartEvent.setChannel(apCallStart.getChannel());
            }

            if (apCallStart.hasBand()) {
                cloudSipCallStartEvent.setRadioType(OvsdbToWlanCloudTypeMappingUtility
                        .getRadioTypeFromOpensyncStatsRadioBandType(apCallStart.getBand()));
            }

            eventsList.add(cloudSipCallStartEvent);

        }
    }

    protected void processRealTimeSipCallStopEvent(int customerId, long equipmentId, long eventTimestamp,
            List<SystemEvent> eventsList, VideoVoiceReport videoVoiceReport) {
        if (videoVoiceReport.hasCallStop()) {

            CallStop apCallStop = videoVoiceReport.getCallStop();

            RealTimeSipCallStopEvent cloudSipCallStopEvent = new RealTimeSipCallStopEvent(customerId, equipmentId,
                    eventTimestamp);

            if (apCallStop.hasCallDuration()) {

                cloudSipCallStopEvent.setCallDuration(apCallStop.getCallDuration());

            }

            if (apCallStop.hasClientMac() && apCallStop.getClientMac().isValidUtf8()) {

                cloudSipCallStopEvent.setClientMacAddress(MacAddress.valueOf(apCallStop.getClientMac().toStringUtf8()));

            }

            if (apCallStop.hasReason()) {

                switch (apCallStop.getReason()) {
                case BYE_OK:
                    cloudSipCallStopEvent.setReason(SipCallStopReason.BYE_OK);
                    break;
                case CALL_DROPPED:
                    cloudSipCallStopEvent.setReason(SipCallStopReason.DROPPED);
                    break;

                default:
                    cloudSipCallStopEvent.setReason(SipCallStopReason.UNSUPPORTED);
                }

            }

            if (apCallStop.hasSessionId()) {

                cloudSipCallStopEvent.setAssociationId(apCallStop.getSessionId());

            }

            if (apCallStop.hasWifiSessionId()) {
                cloudSipCallStopEvent.setSipCallId(apCallStop.getWifiSessionId());

            }

            if (apCallStop.getStatsCount() > 0) {
                cloudSipCallStopEvent.setStatuses(processRtpFlowStats(apCallStop.getStatsList()));
            }

            if (apCallStop.hasProviderDomain()) {
                cloudSipCallStopEvent.setProviderDomain(apCallStop.getProviderDomain());
            }

            if (apCallStop.getCodecsCount() > 0) {
                cloudSipCallStopEvent.setCodecs(apCallStop.getCodecsList());
            }

            if (apCallStop.hasChannel()) {
                cloudSipCallStopEvent.setChannel(apCallStop.getChannel());
            }

            if (apCallStop.hasBand()) {
                cloudSipCallStopEvent.setRadioType(OvsdbToWlanCloudTypeMappingUtility
                        .getRadioTypeFromOpensyncStatsRadioBandType(apCallStop.getBand()));
            }

            eventsList.add(cloudSipCallStopEvent);

        }
    }

    protected void processRtsStartEvent(int customerId, long equipmentId, long eventTimestamp,
            List<SystemEvent> eventsList, VideoVoiceReport videoVoiceReport) {
        if (videoVoiceReport.hasStreamVideoServer()) {
            StreamingVideoServerDetected apStreamVideoServer = videoVoiceReport.getStreamVideoServer();
            RealTimeStreamingStartEvent rtsStartEvent = new RealTimeStreamingStartEvent(customerId, equipmentId,
                    eventTimestamp);
            if (apStreamVideoServer.hasServerIp()) {
                try {
                    rtsStartEvent
                            .setServerIp(InetAddress.getByAddress(apStreamVideoServer.getServerIp().toByteArray()));
                } catch (UnknownHostException e) {
                    LOG.error("Cannot get IP Address from {}", apStreamVideoServer.getServerIp(), e);
                }
            }
            if (apStreamVideoServer.hasStreamingVideoType()) {
                rtsStartEvent.setType(OvsdbToWlanCloudTypeMappingUtility
                        .getCloudStreamingVideoTypeFromApReport(apStreamVideoServer.getStreamingVideoType()));
            }

            if (apStreamVideoServer.hasServerDnsName()) {
                rtsStartEvent.setServerDnsName(apStreamVideoServer.getServerDnsName());
            }

            if (apStreamVideoServer.hasClientMac()) {
                rtsStartEvent.setClientMacAddress(MacAddress.valueOf(apStreamVideoServer.getClientMac().toByteArray()));
            }

            if (apStreamVideoServer.hasSessionId()) {
                rtsStartEvent.setSessionId(apStreamVideoServer.getSessionId());
            }

            if (apStreamVideoServer.hasVideoSessionId()) {
                rtsStartEvent.setVideoSessionId(apStreamVideoServer.getVideoSessionId());
            }

            eventsList.add(rtsStartEvent);

        }
    }

    private List<com.telecominfraproject.wlan.systemevent.equipment.realtime.RtpFlowStats> processRtpFlowStats(List<OpensyncStats.RtpFlowStats> stats) {
        List<com.telecominfraproject.wlan.systemevent.equipment.realtime.RtpFlowStats> cloudRtpFlowStatsList = new ArrayList<>();
        for (RtpFlowStats apRtpFlowStats : stats) {

            com.telecominfraproject.wlan.systemevent.equipment.realtime.RtpFlowStats cloudRtpStats = new com.telecominfraproject.wlan.systemevent.equipment.realtime.RtpFlowStats();

            if (apRtpFlowStats.hasCodec()) {
                cloudRtpStats.setCodec(apRtpFlowStats.getCodec());
            }

            if (apRtpFlowStats.hasBlockCodecs()) {
                cloudRtpStats.setBlockCodecs(apRtpFlowStats.getBlockCodecs().toByteArray());
            }

            if (apRtpFlowStats.hasLatency()) {
                cloudRtpStats.setLatency(apRtpFlowStats.getLatency());
            }

            if (apRtpFlowStats.hasRtpSeqFirst()) {
                cloudRtpStats.setFirstRTPSeq(apRtpFlowStats.getRtpSeqFirst());
            }

            if (apRtpFlowStats.hasRtpSeqLast()) {
                cloudRtpStats.setLastRTPSeq(apRtpFlowStats.getRtpSeqLast());
            }

            if (apRtpFlowStats.hasDirection()) {
                switch (apRtpFlowStats.getDirection()) {
                    case RTP_DOWNSTREAM:
                        cloudRtpStats.setDirection(
                                com.telecominfraproject.wlan.systemevent.equipment.realtime.RtpFlowDirection.DOWNSTREAM);
                        break;
                    case RTP_UPSTREAM:
                        cloudRtpStats.setDirection(
                                com.telecominfraproject.wlan.systemevent.equipment.realtime.RtpFlowDirection.UPSTREAM);
                        break;
                    default:
                        cloudRtpStats.setDirection(
                                com.telecominfraproject.wlan.systemevent.equipment.realtime.RtpFlowDirection.UNSUPPORTED);
                }
            }

            if (apRtpFlowStats.hasRtpFlowType()) {
                switch (apRtpFlowStats.getRtpFlowType()) {
                    case RTP_VIDEO:
                        cloudRtpStats.setFlowType(
                                com.telecominfraproject.wlan.systemevent.equipment.realtime.RtpFlowType.VIDEO);
                        break;
                    case RTP_VOICE:
                        cloudRtpStats.setFlowType(
                                com.telecominfraproject.wlan.systemevent.equipment.realtime.RtpFlowType.VOICE);
                        break;
                    default:
                        cloudRtpStats.setFlowType(
                                com.telecominfraproject.wlan.systemevent.equipment.realtime.RtpFlowType.UNSUPPORTED);
                        break;
                }
            }

            if (apRtpFlowStats.hasJitter()) {
                cloudRtpStats.setJitter(apRtpFlowStats.getJitter());
            }

            if (apRtpFlowStats.hasTotalPacketsSent()) {
                cloudRtpStats.setTotalPacket(apRtpFlowStats.getTotalPacketsSent());
            }

            if (apRtpFlowStats.hasTotalPacketsLost()) {
                cloudRtpStats.setTotalPacketLost(apRtpFlowStats.getTotalPacketsLost());
            }

            if (apRtpFlowStats.hasMosx100()) {
                cloudRtpStats.setMosMultipliedBy100(apRtpFlowStats.getMosx100());
            }

            if (apRtpFlowStats.hasPacketLossConsec()) {
                cloudRtpStats.setPacketLossConsecutive(apRtpFlowStats.getPacketLossConsec());
            }

            if (apRtpFlowStats.hasPacketLossPercent()) {
                cloudRtpStats.setPacketLossPercentage(apRtpFlowStats.getPacketLossPercent());
            }

            cloudRtpFlowStatsList.add(cloudRtpStats);

        }
        return cloudRtpFlowStatsList;
    }

    protected void processRtsStartSessionEvent(int customerId, long equipmentId, long eventTimestamp,
            List<SystemEvent> eventsList, VideoVoiceReport videoVoiceReport) {
        if (videoVoiceReport.hasStreamVideoSessionStart()) {
            StreamingVideoSessionStart apStreamVideoSessionStart = videoVoiceReport.getStreamVideoSessionStart();
            RealTimeStreamingStartSessionEvent rtsStartSessionEvent = new RealTimeStreamingStartSessionEvent(customerId,
                    equipmentId, eventTimestamp);
            if (apStreamVideoSessionStart.hasClientMac()) {
                rtsStartSessionEvent.setClientMacAddress(
                        MacAddress.valueOf(apStreamVideoSessionStart.getClientMac().toByteArray()));
            }

            if (apStreamVideoSessionStart.hasServerIp()) {
                try {
                    rtsStartSessionEvent.setServerIp(
                            InetAddress.getByAddress(apStreamVideoSessionStart.getServerIp().toByteArray()));
                } catch (UnknownHostException e) {
                    LOG.error("Cannot get IP Address from {}", apStreamVideoSessionStart.getServerIp(), e);

                }
            }

            if (apStreamVideoSessionStart.hasSessionId()) {
                rtsStartSessionEvent.setSessionId(apStreamVideoSessionStart.getSessionId());

            }

            if (apStreamVideoSessionStart.hasStreamingVideoType()) {
                rtsStartSessionEvent.setType(OvsdbToWlanCloudTypeMappingUtility
                        .getCloudStreamingVideoTypeFromApReport(apStreamVideoSessionStart.getStreamingVideoType()));

            }

            if (apStreamVideoSessionStart.hasVideoSessionId()) {
                rtsStartSessionEvent.setVideoSessionId(apStreamVideoSessionStart.getVideoSessionId());
            }
            eventsList.add(rtsStartSessionEvent);
        }
    }

    protected void processRtsStopEvent(int customerId, long equipmentId, long eventTimestamp,
            List<SystemEvent> eventsList, VideoVoiceReport videoVoiceReport) {
        if (videoVoiceReport.hasStreamVideoStop()) {
            StreamingVideoStop apStreamVideoStop = videoVoiceReport.getStreamVideoStop();
            RealTimeStreamingStopEvent rtsStopEvent = new RealTimeStreamingStopEvent(customerId, equipmentId,
                    eventTimestamp);
            if (apStreamVideoStop.hasClientMac()) {
                rtsStopEvent.setClientMacAddress(MacAddress.valueOf(apStreamVideoStop.getClientMac().toByteArray()));
            }

            if (apStreamVideoStop.hasDurationSec()) {
                rtsStopEvent.setDurationInSecs(apStreamVideoStop.getDurationSec());
            }

            if (apStreamVideoStop.hasServerIp()) {
                try {
                    rtsStopEvent.setServerIp(InetAddress.getByAddress(apStreamVideoStop.getServerIp().toByteArray()));
                } catch (UnknownHostException e) {
                    LOG.error("Cannot get IP Address from {}", apStreamVideoStop.getServerIp(), e);

                }
            }

            if (apStreamVideoStop.hasSessionId()) {
                rtsStopEvent.setSessionId(apStreamVideoStop.getSessionId());
            }

            if (apStreamVideoStop.hasStreamingVideoType()) {

                rtsStopEvent.setType(OvsdbToWlanCloudTypeMappingUtility
                        .getCloudStreamingVideoTypeFromApReport(apStreamVideoStop.getStreamingVideoType()));

            }

            if (apStreamVideoStop.hasTotalBytes()) {
                rtsStopEvent.setTotalBytes(apStreamVideoStop.getTotalBytes());
            }

            if (apStreamVideoStop.hasVideoSessionId()) {
                rtsStopEvent.setVideoSessionId(apStreamVideoStop.getVideoSessionId());
            }

            eventsList.add(rtsStopEvent);

        }
    }

    void populateApNodeMetrics(List<ServiceMetric> metricRecordList, Report report, int customerId, long equipmentId,
            long locationId) {
        LOG.info("populateApNodeMetrics for Customer {} Equipment {}", customerId, equipmentId);
        ApNodeMetrics apNodeMetrics = new ApNodeMetrics();
        ServiceMetric smr = new ServiceMetric(customerId, equipmentId);

        smr.setLocationId(locationId);

        metricRecordList.add(smr);
        smr.setDetails(apNodeMetrics);

        for (Device deviceReport : report.getDeviceList()) {

            int avgRadioTemp = 0;

            ApPerformance apPerformance = new ApPerformance();
            apNodeMetrics.setApPerformance(apPerformance);

            smr.setCreatedTimestamp(deviceReport.getTimestampMs());

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
            }

            if (deviceReport.hasCpuUtil() && deviceReport.getCpuUtil().hasCpuUtil()) {
                Integer cpuUtilization = deviceReport.getCpuUtil().getCpuUtil();
                apPerformance.setCpuUtilized(new int[] { cpuUtilization });
            }

            apPerformance.setEthLinkState(EthernetLinkState.UP1000_FULL_DUPLEX);

            if (deviceReport.hasMemUtil() && deviceReport.getMemUtil().hasMemTotal()
                    && deviceReport.getMemUtil().hasMemUsed()) {
                apPerformance.setFreeMemory(
                        deviceReport.getMemUtil().getMemTotal() - deviceReport.getMemUtil().getMemUsed());
            }
            apPerformance.setUpTime((long) deviceReport.getUptime());

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

            RadioType radioType = OvsdbToWlanCloudTypeMappingUtility
                    .getRadioTypeFromOpensyncStatsRadioBandType(clReport.getBand());
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

            apNodeMetrics.setRadioStats(radioType, radioStats);

            apNodeMetrics.setRxBytes(radioType, rxBytes);
            apNodeMetrics.setTxBytes(radioType, txBytes);

            List<MacAddress> clientMacList = new ArrayList<>();
            clientMacs.forEach(macStr -> {
                try {
                    clientMacList.add(new MacAddress(macStr));
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
                noiseList.add(getNegativeSignedIntFrom8BitUnsigned(surveySample.getNoise()));

            }

            if (totalDurationMs > 0) {
                RadioUtilization radioUtil = new RadioUtilization();
                radioUtil.setTimestampSeconds((int) ((survey.getTimestampMs()) / 1000));
                int pctBusyTx = busyTx / totalDurationMs;
                radioUtil.setAssocClientTx(pctBusyTx);
                int pctBusyRx = busyRx / totalDurationMs;
                radioUtil.setAssocClientRx(pctBusyRx);
                double pctIBSS = (busyTx + busySelf) / totalDurationMs;
                radioUtil.setIbss(pctIBSS);
                int nonWifi = (busy - (busyTx + busyRx)) / totalDurationMs;
                radioUtil.setNonWifi(nonWifi);

                radioType = OvsdbToWlanCloudTypeMappingUtility
                        .getRadioTypeFromOpensyncStatsRadioBandType(survey.getBand());
                if (radioType != RadioType.UNSUPPORTED) {
                    if (apNodeMetrics.getRadioUtilization(radioType) == null) {
                        apNodeMetrics.setRadioUtilization(radioType, new ArrayList<>());
                    }
                    apNodeMetrics.getRadioUtilization(radioType).add(radioUtil);
                    int noiseAvg = (int) Math.round(DecibelUtils.getAverageDecibel(toIntArray(noiseList)));
                    avgNoiseFloor.put(radioType, noiseAvg);
                    apNodeMetrics.setNoiseFloor(radioType, noiseAvg);

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

        populateNetworkProbeMetrics(report, apNodeMetrics);
        updateNetworkAdminStatusReport(customerId, equipmentId, apNodeMetrics);

    }

    private void updateNetworkAdminStatusReport(int customerId, long equipmentId, ApNodeMetrics apNodeMetrics) {
        apNodeMetrics.getNetworkProbeMetrics().forEach(n -> {

            
            LOG.info("Update NetworkAdminStatusReport for NetworkProbeMetrics {}", n.toString());
            
            Status networkAdminStatus = statusServiceInterface.getOrNull(customerId, equipmentId,
                    StatusDataType.NETWORK_ADMIN);

            if (networkAdminStatus == null) {
                networkAdminStatus = new Status();
                networkAdminStatus.setCustomerId(customerId);
                networkAdminStatus.setEquipmentId(equipmentId);
                networkAdminStatus.setCreatedTimestamp(System.currentTimeMillis());
                networkAdminStatus.setStatusDataType(StatusDataType.NETWORK_ADMIN);
                networkAdminStatus.setDetails(new NetworkAdminStatusData());
                networkAdminStatus = statusServiceInterface.update(networkAdminStatus);
            }
            
            NetworkAdminStatusData statusData = (NetworkAdminStatusData) networkAdminStatus.getDetails();
            
            if (n.getDnsState() == null) {
                LOG.debug("No DnsState present in networkProbeMetrics, DnsState and CloudLinkStatus set to 'normal");
                statusData.setDnsStatus(StatusCode.normal);
                statusData.setCloudLinkStatus(StatusCode.normal);
            } else {
                statusData.setDnsStatus(stateUpDownErrorToStatusCode(n.getDnsState()));
                statusData.setCloudLinkStatus(stateUpDownErrorToStatusCode(n.getDnsState()));
            }
            if (n.getDhcpState() == null) {
                LOG.debug("No DhcpState present in networkProbeMetrics, set to 'normal");
                statusData.setDhcpStatus(StatusCode.normal);
            } else {
                statusData.setDhcpStatus(stateUpDownErrorToStatusCode(n.getDhcpState()));
            }
            if (n.getRadiusState() == null) {
                LOG.debug("No RadiusState present in networkProbeMetrics, set to 'normal");
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

    void updateDeviceStatusRadioUtilizationReport(int customerId, long equipmentId,
            RadioUtilizationReport radioUtilizationReport) {
        LOG.info(
                "Processing updateDeviceStatusRadioUtilizationReport for equipmentId {} with RadioUtilizationReport {}",
                equipmentId, radioUtilizationReport);

        Status radioUtilizationStatus = statusServiceInterface.getOrNull(customerId, equipmentId,
                StatusDataType.RADIO_UTILIZATION);

        if (radioUtilizationStatus == null) {
            LOG.debug("Create new radioUtilizationStatus");
            radioUtilizationStatus = new Status();
            radioUtilizationStatus.setCustomerId(customerId);
            radioUtilizationStatus.setEquipmentId(equipmentId);
            radioUtilizationStatus.setStatusDataType(StatusDataType.RADIO_UTILIZATION);
        }

        radioUtilizationStatus.setDetails(radioUtilizationReport);

        statusServiceInterface.update(radioUtilizationStatus);
    }

    void populateNetworkProbeMetrics(Report report, ApNodeMetrics apNodeMetrics) {
        List<NetworkProbeMetrics> networkProbeMetricsList = new ArrayList<>();

        for (NetworkProbe networkProbe : report.getNetworkProbeList()) {

            NetworkProbeMetrics networkProbeMetrics = new NetworkProbeMetrics();

            List<DnsProbeMetric> dnsProbeResults = new ArrayList<>();
            if (networkProbe.hasDnsProbe()) {

                DNSProbeMetric dnsProbeMetricFromAp = networkProbe.getDnsProbe();

                DnsProbeMetric cloudDnsProbeMetric = new DnsProbeMetric();

                if (dnsProbeMetricFromAp.hasLatency()) {
                    networkProbeMetrics.setDnsLatencyMs(dnsProbeMetricFromAp.getLatency());
                    cloudDnsProbeMetric.setDnsLatencyMs(dnsProbeMetricFromAp.getLatency());
                }

                if (dnsProbeMetricFromAp.hasState()) {
                    StateUpDownError dnsState = OvsdbToWlanCloudTypeMappingUtility
                            .getCloudMetricsStateFromOpensyncStatsStateUpDown(dnsProbeMetricFromAp.getState());

                    networkProbeMetrics.setDnsState(dnsState);
                    cloudDnsProbeMetric.setDnsState(dnsState);

                }

                if (dnsProbeMetricFromAp.hasServerIP()) {
                    InetAddress ipAddress;
                    try {
                        ipAddress = InetAddress.getByName(dnsProbeMetricFromAp.getServerIP());
                        cloudDnsProbeMetric.setDnsServerIp(ipAddress);
                    } catch (UnknownHostException e) {
                        LOG.error("Could not get DNS Server IP from network_probe service_metrics_collection_config",
                                e);
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
                    StateUpDownError radiusState = OvsdbToWlanCloudTypeMappingUtility
                            .getCloudMetricsStateFromOpensyncStatsStateUpDown(radiusMetrics.getRadiusState());

                    networkProbeMetrics.setRadiusState(radiusState);

                }


            }

            if (networkProbe.hasVlanProbe()) {
                VLANMetrics vlanMetrics = networkProbe.getVlanProbe();
                if (vlanMetrics.hasVlanIF()) {
                    networkProbeMetrics.setVlanIF(vlanMetrics.getVlanIF());
                }
                if (vlanMetrics.hasDhcpState()) {
                    StateUpDownError dhcpState = OvsdbToWlanCloudTypeMappingUtility
                            .getCloudMetricsStateFromOpensyncStatsStateUpDown(vlanMetrics.getDhcpState());

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
        eqOsPerformance
                .setAvgFreeMemoryKb(deviceReport.getMemUtil().getMemTotal() - deviceReport.getMemUtil().getMemUsed());
        eqOsPerformance.setTotalAvailableMemoryKb(deviceReport.getMemUtil().getMemTotal());
        status.setDetails(eqOsPerformance);
        status = statusServiceInterface.update(status);
        LOG.debug("updated status {}", status);
    }

    void populateApClientMetrics(List<ServiceMetric> metricRecordList, Report report, int customerId, long equipmentId,
            long locationId) {
        LOG.info("populateApClientMetrics for Customer {} Equipment {}", customerId, equipmentId);

        for (ClientReport clReport : report.getClientsList()) {
            for (Client cl : clReport.getClientListList()) {

                if (cl.getMacAddress() == null) {
                    LOG.debug("No mac address for Client {}, cannot set device mac address for client in ClientMetrics.",
                            cl);
                    continue;
                }

                LOG.debug("Processing ClientReport from AP {}", cl.getMacAddress());

                ServiceMetric smr = new ServiceMetric(customerId, equipmentId, new MacAddress(cl.getMacAddress()));
                smr.setLocationId(locationId);
                metricRecordList.add(smr);

                smr.setCreatedTimestamp(clReport.getTimestampMs());
                smr.setClientMac(new MacAddress(cl.getMacAddress()).getAddressAsLong());

                // clReport.getChannel();
                ClientMetrics cMetrics = new ClientMetrics();
                smr.setDetails(cMetrics);

                Integer periodLengthSec = 60; // matches what's configured by
                // OvsdbDao.configureStats(OvsdbClient)
                cMetrics.setPeriodLengthSec(periodLengthSec);

                cMetrics.setRadioType(OvsdbToWlanCloudTypeMappingUtility
                        .getRadioTypeFromOpensyncStatsRadioBandType(clReport.getBand()));
                // we'll report each device as having a single (very long)
                // session
                long sessionId = WiFiSessionUtility.encodeWiFiAssociationId(clReport.getTimestampMs() / 1000L,
                        MacAddress.convertMacStringToLongValue(cl.getMacAddress()));

                LOG.debug("populateApClientMetrics Session Id {}", sessionId);
                cMetrics.setSessionId(sessionId);

                if (cl.hasStats()) {
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

                    if (cl.getStats().hasTxRate() && cl.getStats().hasRxRate()) {
                        cMetrics.setRates(new int[] { Double.valueOf(cl.getStats().getTxRate() / 1000).intValue(),
                                Double.valueOf(cl.getStats().getRxRate() / 1000).intValue() });
                    }

                    if (cl.getStats().hasTxErrors()) {
                        cMetrics.setNumTxDropped((int) cl.getStats().getTxErrors());
                    }

                    if (cl.getStats().hasRxFrames()) {
                        cMetrics.setNumTxFramesTransmitted(cl.getStats().getTxFrames());
                    }

                    if (cl.getStats().hasTxRetries()) {
                        cMetrics.setNumTxDataRetries((int) cl.getStats().getTxRetries());
                    }
                }

                LOG.debug("ApClientMetrics Report {}", cMetrics);

            }

        }

    }

    void populateNeighbourScanReports(List<ServiceMetric> metricRecordList, Report report, int customerId,
            long equipmentId, long locationId) {
        LOG.info("populateNeighbourScanReports for Customer {} Equipment {}", customerId, equipmentId);

        for (Neighbor neighbor : report.getNeighborsList()) {

            ServiceMetric smr = new ServiceMetric(customerId, equipmentId);
            smr.setLocationId(locationId);

            metricRecordList.add(smr);
            NeighbourScanReports neighbourScanReports = new NeighbourScanReports();
            smr.setDetails(neighbourScanReports);
            smr.setCreatedTimestamp(neighbor.getTimestampMs());

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
                nr.setMacAddress(new MacAddress(nBss.getBssid()));
                nr.setNetworkType(NetworkType.AP);
                nr.setPacketType(NeighborScanPacketType.BEACON);
                nr.setPrivacy(((nBss.getSsid() == null) || nBss.getSsid().isEmpty()) ? true : false);
                // nr.setRate(rate);
                // we can only get Rssi as an unsigned int from opensync, so
                // some shifting
                int rssi = nBss.getRssi();
                nr.setRssi(rssi);
                // nr.setScanTimeInSeconds(scanTimeInSeconds);
                nr.setSecureMode(DetectedAuthMode.WPA);
                // nr.setSignal(signal);
                nr.setSsid(nBss.getSsid());
            }

        }
    }

    ClientSession handleClientSessionMetricsUpdate(int customerId, long equipmentId, long locationId,
            RadioType radioType, long timestamp, sts.OpensyncStats.Client client) {
        try

        {
            LOG.info("handleClientSessionUpdate for customerId {} equipmentId {} locationId {} client {} ", customerId,
                    equipmentId, locationId, client.getMacAddress());

            com.telecominfraproject.wlan.client.models.Client clientInstance = clientServiceInterface
                    .getOrNull(customerId, new MacAddress(client.getMacAddress()));

            boolean isReassociation = true;
            if (clientInstance == null) {

                LOG.debug("Cannot get client instance for {}", client.getMacAddress());
                return null;

            }

            LOG.debug("Client {}", clientInstance);

            ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId,
                    clientInstance.getMacAddress());

            if (clientSession == null) {
                LOG.warn("Cannot get client session for {}", clientInstance.getMacAddress());
                return null;
            }

            ClientSessionDetails latestClientSessionDetails = clientSession.getDetails();
            latestClientSessionDetails.setIsReassociation(isReassociation);
            latestClientSessionDetails.setSsid(client.getSsid());
            latestClientSessionDetails.setRadioType(radioType);
            latestClientSessionDetails.setSessionId(clientInstance.getMacAddress().getAddressAsLong());
            latestClientSessionDetails.setLastEventTimestamp(timestamp);
            latestClientSessionDetails.setFirstDataSentTimestamp(timestamp - client.getDurationMs());
            latestClientSessionDetails.setFirstDataRcvdTimestamp(timestamp - client.getDurationMs());
            latestClientSessionDetails.setAssociationState(AssociationState.Active_Data);
            int rssi = client.getStats().getRssi();
            latestClientSessionDetails.setAssocRssi(rssi);
            latestClientSessionDetails.setLastRxTimestamp(timestamp);
            latestClientSessionDetails.setLastTxTimestamp(timestamp);

            Equipment ce = equipmentServiceInterface.get(equipmentId);
            ProfileContainer profileContainer = new ProfileContainer(
                    profileServiceInterface.getProfileWithChildren(ce.getProfileId()));

            List<Profile> ssidConfigList = new ArrayList<>();
            ssidConfigList = profileContainer.getChildrenOfType(ce.getProfileId(), ProfileType.ssid).stream()
                    .filter(t -> {

                        SsidConfiguration ssidConfig = (SsidConfiguration) t.getDetails();
                        return ssidConfig.getSsid().equals(client.getSsid())
                                && ssidConfig.getAppliedRadios().contains(radioType);
                    }).collect(Collectors.toList());

            if (!ssidConfigList.isEmpty()) {
                Profile ssidProfile = ssidConfigList.iterator().next();
                SsidConfiguration ssidConfig = (SsidConfiguration) ssidProfile.getDetails();
                if (ssidConfig.getSecureMode().equals(SecureMode.open)) {
                    latestClientSessionDetails.setSecurityType(SecurityType.OPEN);
                } else if (ssidConfig.getSecureMode().equals(SecureMode.wpaPSK)
                        || ssidConfig.getSecureMode().equals(SecureMode.wpa2PSK)
                        || ssidConfig.getSecureMode().equals(SecureMode.wpa2OnlyPSK)) {
                    latestClientSessionDetails.setSecurityType(SecurityType.PSK);
                } else if (ssidConfig.getSecureMode().equals(SecureMode.wpa3OnlySAE)
                        || ssidConfig.getSecureMode().equals(SecureMode.wpa3MixedSAE)){
                    latestClientSessionDetails.setSecurityType(SecurityType.SAE);
                } else if (ssidConfig.getSecureMode().equals(SecureMode.wpa2Radius)
                        || ssidConfig.getSecureMode().equals(SecureMode.wpaRadius)
                        || ssidConfig.getSecureMode().equals(SecureMode.wpa2OnlyRadius)) {
                    latestClientSessionDetails.setSecurityType(SecurityType.RADIUS);
                    latestClientSessionDetails.setEapDetails(new ClientEapDetails());
                } else if (ssidConfig.getSecureMode().equals(SecureMode.wpaEAP)
                        || ssidConfig.getSecureMode().equals(SecureMode.wpa2EAP)
                        || ssidConfig.getSecureMode().equals(SecureMode.wpa2OnlyEAP)
                        || ssidConfig.getSecureMode().equals(SecureMode.wpa3OnlyEAP)
                        || ssidConfig.getSecureMode().equals(SecureMode.wpa3MixedEAP)) {
                    latestClientSessionDetails.setSecurityType(SecurityType.RADIUS);
                    latestClientSessionDetails.setEapDetails(new ClientEapDetails());
                } else {
                    latestClientSessionDetails.setSecurityType(SecurityType.UNSUPPORTED);
                }

                if (ssidConfig.getVlanId() > 1) {
                    latestClientSessionDetails.setDynamicVlan(ssidConfig.getVlanId());
                }

                RadioBasedSsidConfiguration radioConfig = ssidConfig.getRadioBasedConfigs().get(radioType);
                latestClientSessionDetails
                        .setIs11KUsed(radioConfig.getEnable80211k() != null ? radioConfig.getEnable80211k() : false);
                latestClientSessionDetails
                        .setIs11RUsed(radioConfig.getEnable80211r() != null ? radioConfig.getEnable80211r() : false);
                latestClientSessionDetails
                        .setIs11VUsed(radioConfig.getEnable80211v() != null ? radioConfig.getEnable80211v() : false);

            }

            latestClientSessionDetails.setMetricDetails(calculateClientSessionMetricDetails(client, timestamp));

            clientSession.getDetails().mergeSession(latestClientSessionDetails);
            clientSession.setLastModifiedTimestamp(timestamp);

            clientSession = clientServiceInterface.updateSession(clientSession);

            LOG.debug("Updated client session {}", clientSession);

            return clientSession;
        } catch (Exception e) {
            LOG.error("Error while attempting to create ClientSession and Info", e);
        }
        return null;
    }

    ClientSessionMetricDetails calculateClientSessionMetricDetails(sts.OpensyncStats.Client client, long timestamp) {

        LOG.info("calculateClientSessionMetricDetails for Client {} at timestamp {}", client.getMacAddress(),
                timestamp);

        ClientSessionMetricDetails metricDetails = new ClientSessionMetricDetails();

        if (LOG.isDebugEnabled())
            LOG.debug("Stats: {} DurationMs {}", client.getStats(), client.getDurationMs());
        int rssi = client.getStats().getRssi();
        metricDetails.setRssi(rssi);
        metricDetails.setRxBytes(client.getStats().getRxBytes());
        metricDetails.setTxBytes(client.getStats().getTxBytes());

        // Frames : data chunk sent over data-link layer (Ethernet, ATM)
        // Packets : data chunk sent over IP layer.
        // in Wifi, these are the same size, so number of packets is equal to
        // number of frames
        metricDetails.setTotalTxPackets(client.getStats().getTxFrames());
        metricDetails.setTotalRxPackets(client.getStats().getRxFrames());
        metricDetails.setTxDataFrames((int) client.getStats().getTxFrames());
        metricDetails.setRxDataFrames((int) client.getStats().getRxFrames());

        metricDetails.setRxRateKbps((long) client.getStats().getRxRate());
        metricDetails.setTxRateKbps((long) client.getStats().getTxRate());
        if (LOG.isDebugEnabled())
            LOG.debug("RxRateKbps {} TxRateKbps {}", metricDetails.getRxRateKbps(), metricDetails.getTxRateKbps());

        // Throughput, do rate / duration
        if (client.getDurationMs() > 1000) {
            int durationSec = client.getDurationMs() / 1000;
            // 1 Mbit = 125000 B

            float rxBytesFv = Long.valueOf(client.getStats().getRxBytes()).floatValue();
            float rxBytesToMb = rxBytesFv / 125000F;
            float txBytesFv = Long.valueOf(client.getStats().getRxBytes()).floatValue();
            float txBytesToMb = txBytesFv / 125000F;

            if (LOG.isDebugEnabled())
                LOG.debug("rxBytesToMb {} txBytesToMb {} ", rxBytesToMb, txBytesToMb);

            metricDetails.setRxMbps(rxBytesToMb / durationSec);
            metricDetails.setTxMbps(txBytesToMb / durationSec);
            if (LOG.isDebugEnabled())
                LOG.debug("RxMbps {} TxMbps {} ", metricDetails.getRxMbps(), metricDetails.getTxMbps());

        } else {
            LOG.warn("Cannot calculate tx/rx throughput for Client {} based on duration of {} Ms",
                    client.getMacAddress(), client.getDurationMs());
        }
        metricDetails.setLastMetricTimestamp(timestamp);

        return metricDetails;
    }

    void populateApSsidMetrics(List<ServiceMetric> metricRecordList, Report report, int customerId, long equipmentId,
            String apId, long locationId) {

        LOG.info("populateApSsidMetrics for Customer {} Equipment {}", customerId, equipmentId);
        ServiceMetric smr = new ServiceMetric(customerId, equipmentId);
        smr.setLocationId(locationId);
        ApSsidMetrics apSsidMetrics = new ApSsidMetrics();

        smr.setDetails(apSsidMetrics);
        metricRecordList.add(smr);

        for (ClientReport clientReport : report.getClientsList()) {

            LOG.debug("ClientReport for channel {} RadioBand {}", clientReport.getChannel(), clientReport.getBand());

            if (smr.getCreatedTimestamp() < clientReport.getTimestampMs()) {
                smr.setCreatedTimestamp(clientReport.getTimestampMs());
            }

            long txBytes = 0L;
            long rxBytes = 0L;
            long txFrames = 0L;
            long rxFrames = 0L;

            int txErrors = 0;
            int rxErrors = 0;

            int txRetries = 0;
            int rxRetries = 0;

            int lastRssi = 0;
            String ssid = null;

            Set<String> clientMacs = new HashSet<>();

            RadioType radioType = OvsdbToWlanCloudTypeMappingUtility
                    .getRadioTypeFromOpensyncStatsRadioBandType(clientReport.getBand());

            SsidStatistics ssidStatistics = new SsidStatistics();
            // GET the Radio IF MAC (BSSID) from the activeBSSIDs

            Status activeBssidsStatus = statusServiceInterface.getOrNull(customerId, equipmentId,
                    StatusDataType.ACTIVE_BSSIDS);
            ActiveBSSIDs statusDetails = null;
            if (activeBssidsStatus != null) {
                statusDetails = (ActiveBSSIDs) activeBssidsStatus.getDetails();
                for (ActiveBSSID activeBSSID : ((ActiveBSSIDs) activeBssidsStatus.getDetails()).getActiveBSSIDs()) {
                    if (activeBSSID.getRadioType().equals(radioType)) {
                        ssidStatistics.setBssid(new MacAddress(activeBSSID.getBssid()));
                        // ssid value, in case not in stats, else will take
                        // stats value after
                        ssid = activeBSSID.getSsid();
                        statusDetails.getActiveBSSIDs().indexOf(activeBSSID);
                    }
                }
            }
            LOG.debug("Client Report Date is {}", new Date(clientReport.getTimestampMs()));
            int numConnectedClients = 0;
            for (Client client : clientReport.getClientListList()) {
                if (client.hasStats()) {

                    if (client.hasSsid()) {
                        ssid = client.getSsid();
                    }

                    if (client.hasMacAddress()) {
                        clientMacs.add(client.getMacAddress());

                    } else {
                        continue; // cannot have a session without a MAC address
                    }

                    rxBytes += client.getStats().getRxBytes();
                    txBytes += client.getStats().getTxBytes();
                    txFrames += client.getStats().getRxFrames();
                    rxFrames += client.getStats().getTxFrames();
                    rxRetries += client.getStats().getRxRetries();
                    txRetries += client.getStats().getTxRetries();
                    rxErrors += client.getStats().getRxErrors();
                    txErrors += client.getStats().getTxErrors();
                    lastRssi = client.getStats().getRssi();

                    try {

                        if (client.hasConnected() && client.getConnected() && client.hasMacAddress()) {
                            // update service_metrics_collection_config for
                            // connected client
                            ClientSession session = handleClientSessionMetricsUpdate(customerId, equipmentId,
                                    locationId, radioType, clientReport.getTimestampMs(), client);
                            if (session != null) {
                                numConnectedClients += 1;
                            }
                        } else {
                            // Make sure, if we have a session for this client,
                            // it
                            // shows disconnected.
                            // update any service_metrics_collection_config that
                            // need update if the
                            // disconnect occured during this window
                            if (client.hasMacAddress()) {
                                ClientSession session = clientServiceInterface.getSessionOrNull(customerId, equipmentId,
                                        new MacAddress(client.getMacAddress()));

                                if (session != null) {
                                    if ((session.getDetails().getAssociationState() != null) && !session.getDetails()
                                            .getAssociationState().equals(AssociationState.Disconnected)) {

                                        ClientSessionDetails latestSessionDetails = new ClientSessionDetails();
                                        latestSessionDetails.setAssociationState(AssociationState.Disconnected);
                                        latestSessionDetails.setLastEventTimestamp(clientReport.getTimestampMs());
                                        latestSessionDetails
                                                .setDisconnectByClientTimestamp(client.hasDisconnectOffsetMs()
                                                        ? clientReport.getTimestampMs() - client.getDisconnectOffsetMs()
                                                        : clientReport.getTimestampMs());
                                        if (client.hasSsid()) {
                                            latestSessionDetails.setSsid(client.getSsid());
                                        }

                                        // could still be values from before
                                        // disconnect occured.
                                        latestSessionDetails.setMetricDetails(calculateClientSessionMetricDetails(
                                                client, clientReport.getTimestampMs()));

                                        session.getDetails().mergeSession(latestSessionDetails);

                                        clientServiceInterface.updateSession(session);

                                    }
                                }

                            }

                            continue; // not connected
                        }

                    } catch (Exception e) {
                        LOG.error("Unabled to update client {}", client, e);
                    }

                }

            }

            // we can only get Rssi as an unsigned int from opensync, so some
            // shifting
            ssidStatistics.setRxLastRssi(lastRssi);
            ssidStatistics.setNumRxData(Long.valueOf(rxFrames).intValue());
            ssidStatistics.setRxBytes(rxBytes - rxErrors - rxRetries);
            ssidStatistics.setNumTxDataRetries(txRetries);
            ssidStatistics.setNumRcvFrameForTx(txFrames);
            ssidStatistics.setNumTxBytesSucc(txBytes - txErrors - txRetries);
            ssidStatistics.setNumRxRetry(rxRetries);
            ssidStatistics.setNumClient(numConnectedClients);
            ssidStatistics.setSsid(ssid);

            if (radioType != null) {
                List<SsidStatistics> ssidStatsList = apSsidMetrics.getSsidStats().get(radioType);
                if (ssidStatsList == null) {
                    ssidStatsList = new ArrayList<>();
                }
                ssidStatsList.add(ssidStatistics);
                apSsidMetrics.getSsidStats().put(radioType, ssidStatsList);
            }

        }

        LOG.debug("ApSsidMetrics {}", apSsidMetrics);

    }

    ChannelInfo createChannelInfo(long equipmentId, RadioType radioType, List<SurveySample> surveySampleList,
            ChannelBandwidth channelBandwidth) {

        int busyTx = 0; /* Tx */
        int busySelf = 0; /* Rx_self (derived from succesful Rx frames) */
        int busy = 0; /* Busy = Rx + Tx + Interference */
        long totalDurationMs = 0;
        ChannelInfo channelInfo = new ChannelInfo();

        int[] noiseArray = new int[surveySampleList.size()];
        int index = 0;
        for (SurveySample sample : surveySampleList) {
            busyTx += sample.getBusyTx() * sample.getDurationMs();
            busySelf += sample.getBusySelf() * sample.getDurationMs(); // successful
                                                                       // Rx
            busy += sample.getBusy() * sample.getDurationMs();
            channelInfo.setChanNumber(sample.getChannel());
            noiseArray[index++] = getNegativeSignedIntFrom8BitUnsigned(sample.getNoise());
            totalDurationMs += sample.getDurationMs();
        }

        int iBSS = busyTx + busySelf;

        Long totalUtilization = Math.round((double) busy / totalDurationMs);
        Long totalNonWifi = Math.round(((double) busy - (double) iBSS) / totalDurationMs);

        channelInfo.setTotalUtilization(totalUtilization.intValue());
        channelInfo.setWifiUtilization(totalUtilization.intValue() - totalNonWifi.intValue());
        channelInfo.setBandwidth(channelBandwidth);
        if (surveySampleList.size() > 0) {
            channelInfo.setNoiseFloor((int) Math.round(DecibelUtils.getAverageDecibel(noiseArray)));
        }
        return channelInfo;
    }

    void populateChannelInfoReports(List<ServiceMetric> metricRecordList, Report report, int customerId,
            long equipmentId, long locationId, long profileId) {

        LOG.info("populateChannelInfoReports for Customer {} Equipment {}", customerId, equipmentId);

        ProfileContainer profileContainer = new ProfileContainer(
                profileServiceInterface.getProfileWithChildren(profileId));
        
        Profile rfProfile = profileContainer.getChildOfTypeOrNull(profileId, ProfileType.rf);
        RfConfiguration rfConfig = null;
        if (rfProfile != null) {
         rfConfig = (RfConfiguration) profileContainer.getChildOfTypeOrNull(profileId, ProfileType.rf)
                .getDetails();
        }
        
        if (rfConfig == null) {
            LOG.warn("Cannot get RfConfiguration for customerId {} equipmentId {}", customerId,equipmentId);
            return;
        }

        for (Survey survey : report.getSurveyList()) {

            ServiceMetric smr = new ServiceMetric();
            smr.setCustomerId(customerId);
            smr.setEquipmentId(equipmentId);
            smr.setLocationId(locationId);

            ChannelInfoReports channelInfoReports = new ChannelInfoReports();
            Map<RadioType, List<ChannelInfo>> channelInfoMap = channelInfoReports
                    .getChannelInformationReportsPerRadio();

            RadioType radioType = null;

            if (survey.hasBand()) {
                radioType = OvsdbToWlanCloudTypeMappingUtility
                        .getRadioTypeFromOpensyncStatsRadioBandType(survey.getBand());
            } else {
                continue;
            }

            ChannelBandwidth channelBandwidth = rfConfig.getRfConfig(radioType).getChannelBandwidth();

            Map<Integer, List<SurveySample>> sampleByChannelMap = new HashMap<>();

            survey.getSurveyListList().stream().filter(t -> {
                if (survey.getSurveyType().equals(SurveyType.ON_CHANNEL)) {
                    return t.hasDurationMs() && (t.getDurationMs() > 0) && t.hasChannel() && t.hasBusy()
                            && t.hasBusyTx() && t.hasNoise();
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

            channelInfoReports.setChannelInformationReportsPerRadio(channelInfoMap);
            smr.setDetails(channelInfoReports);
            smr.setCreatedTimestamp(survey.getTimestampMs());
            metricRecordList.add(smr);

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

        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession != null) {
            return ovsdbSession.getCustomerId();
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
