
package com.telecominfraproject.wlan.opensync.external.integration.utils;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.TypeRegistry;
import com.telecominfraproject.wlan.client.ClientServiceInterface;
import com.telecominfraproject.wlan.client.info.models.ClientInfoDetails;
import com.telecominfraproject.wlan.client.session.models.AssociationState;
import com.telecominfraproject.wlan.client.session.models.ClientDhcpDetails;
import com.telecominfraproject.wlan.client.session.models.ClientEapDetails;
import com.telecominfraproject.wlan.client.session.models.ClientFailureDetails;
import com.telecominfraproject.wlan.client.session.models.ClientSession;
import com.telecominfraproject.wlan.client.session.models.ClientSessionDetails;
import com.telecominfraproject.wlan.client.session.models.ClientSessionMetricDetails;
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

import sts.OpensyncStats.AssocType;
import sts.OpensyncStats.Client;
import sts.OpensyncStats.ClientReport;
import sts.OpensyncStats.DNSProbeMetric;
import sts.OpensyncStats.Device;
import sts.OpensyncStats.Device.RadioTemp;
import sts.OpensyncStats.DeviceType;
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
import sts.OpensyncStats.Survey;
import sts.OpensyncStats.Survey.SurveySample;
import sts.OpensyncStats.SurveyType;
import sts.OpensyncStats.VLANMetrics;
import traffic.NetworkMetadata;
import traffic.NetworkMetadata.FlowReport;
import wc.stats.IpDnsTelemetry;
import wc.stats.IpDnsTelemetry.WCStatsReport;

@org.springframework.context.annotation.Profile("opensync_cloud_config")
@Component
public class MqttStatsPublisher {

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
    private ClientServiceInterface clientServiceInterface;
    @Autowired
    private CloudEventDispatcherInterface cloudEventDispatcherInterface;
    @Autowired
    private RealtimeEventPublisher realtimeEventPublisher;

    public void processMqttMessage(String topic, WCStatsReport wcStatsReport) {
        LOG.info("Received WCStatsReport {}", wcStatsReport.toString());

        LOG.debug("Received report on topic {}", topic);
        int customerId = extractCustomerIdFromTopic(topic);

        long equipmentId = extractEquipmentIdFromTopic(topic);
        if ((equipmentId <= 0) || (customerId <= 0)) {
            LOG.warn("Cannot determine equipment ids from topic {} - customerId {} equipmentId {}", topic, customerId, equipmentId);
            return;
        }

        String apId = extractApIdFromTopic(topic);

        if (apId == null) {
            LOG.warn("Cannot determine AP id from topic {} - customerId {} equipmentId {} apId {}", topic, customerId, equipmentId, apId);
            return;
        }

        if (LOG.isTraceEnabled()) {
            // prepare a JSONPrinter to format protobuf messages as
            // json
            List<Descriptors.Descriptor> protobufDescriptors = new ArrayList<>();
            protobufDescriptors.addAll(IpDnsTelemetry.getDescriptor().getMessageTypes());
            TypeRegistry oldRegistry = TypeRegistry.newBuilder().add(protobufDescriptors).build();
            JsonFormat.Printer jsonPrinter = JsonFormat.printer().preservingProtoFieldNames().includingDefaultValueFields().usingTypeRegistry(oldRegistry);

            try {
                LOG.trace("MQTT IpDnsTelemetry.wcStatsReport = {}", jsonPrinter.print(wcStatsReport));

            } catch (InvalidProtocolBufferException e1) {
                LOG.error("Couldn't parse IpDnsTelemetry.wcStatsReport.", e1);
            }
        }

    }

    public void processMqttMessage(String topic, Report report) {
        LOG.info("Received report on topic {} for ap {}", topic, report.getNodeID());
        int customerId = extractCustomerIdFromTopic(topic);
        String apId = extractApIdFromTopic(topic);
        long equipmentId = extractEquipmentIdFromTopic(topic);
        if ((equipmentId <= 0) || (customerId <= 0)) {
            LOG.warn("Cannot determine equipment ids from topic {} - customerId {} equipmentId {}", topic, customerId, equipmentId);
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

        // prepare a JSONPrinter to format protobuf messages as
        // json
        // List<Descriptors.Descriptor> protobufDescriptors = new ArrayList<>();
        // protobufDescriptors.addAll(OpensyncStats.getDescriptor().getMessageTypes());
        // TypeRegistry oldRegistry = TypeRegistry.newBuilder().add(protobufDescriptors).build();
        // JsonFormat.Printer jsonPrinter =
        // JsonFormat.printer().preservingProtoFieldNames().includingDefaultValueFields().usingTypeRegistry(oldRegistry);
        //
        // try {
        // LOG.trace(jsonPrinter.print(report));
        //
        // } catch (InvalidProtocolBufferException e1) {
        // LOG.error("Couldn't parse OpensyncStats.report.", e1);
        // }

        List<ServiceMetric> metricRecordList = new ArrayList<>();

        try {
            populateApClientMetrics(metricRecordList, report, customerId, equipmentId, locationId);
            populateApNodeMetrics(metricRecordList, report, customerId, equipmentId, locationId);
            populateNeighbourScanReports(metricRecordList, report, customerId, equipmentId, locationId);
            populateChannelInfoReports(metricRecordList, report, customerId, equipmentId, locationId, profileId);
            populateApSsidMetrics(metricRecordList, report, customerId, equipmentId, apId, locationId);

            if (!metricRecordList.isEmpty()) {

                long serviceMetricTimestamp = System.currentTimeMillis();
                LOG.debug("Current timestamp for service metrics is {}", serviceMetricTimestamp);
                metricRecordList.stream().forEach(smr -> {
                    smr.setCreatedTimestamp(serviceMetricTimestamp);
                    if (smr.getLocationId() == 0) smr.setLocationId(locationId);
                    if(smr.getCustomerId() == 0) smr.setCustomerId(customerId);
                    if (smr.getEquipmentId() == 0L) smr.setEquipmentId(equipmentId);
                });
                metricRecordList.stream().forEach(smr -> {
                    LOG.debug("ServiceMetric {}", smr);
                });
                cloudEventDispatcherInterface.publishMetrics(metricRecordList);
            }
            
            publishEvents(report, customerId, equipmentId, apId, locationId);
            // handleRssiMetrics(metricRecordList, report, customerId,
            // equipmentId, locationId);

        } catch (Exception e) {
            LOG.error("Exception when processing stats messages from AP", e);
        }

    }

    public void processMqttMessage(String topic, FlowReport flowReport) {

        LOG.info("Received report on topic {}", topic);
        int customerId = extractCustomerIdFromTopic(topic);

        long equipmentId = extractEquipmentIdFromTopic(topic);
        if ((equipmentId <= 0) || (customerId <= 0)) {
            LOG.warn("Cannot determine equipment ids from topic {} - customerId {} equipmentId {}", topic, customerId, equipmentId);
            return;
        }

        String apId = extractApIdFromTopic(topic);

        if (apId == null) {
            LOG.warn("Cannot determine AP id from topic {} - customerId {} equipmentId {} apId {}", topic, customerId, equipmentId, apId);
            return;
        }

        if (LOG.isTraceEnabled()) {
            // prepare a JSONPrinter to format protobuf messages as
            // json
            List<Descriptors.Descriptor> protobufDescriptors = new ArrayList<>();
            protobufDescriptors.addAll(NetworkMetadata.getDescriptor().getMessageTypes());
            TypeRegistry oldRegistry = TypeRegistry.newBuilder().add(protobufDescriptors).build();
            JsonFormat.Printer jsonPrinter = JsonFormat.printer().preservingProtoFieldNames().includingDefaultValueFields().usingTypeRegistry(oldRegistry);

            try {
                LOG.trace("MQTT NetworkMetadata.flowReport = {}", jsonPrinter.print(flowReport));

            } catch (InvalidProtocolBufferException e1) {
                LOG.error("Couldn't parse NetworkMetadata.flowReport.", e1);
            }
        }

    }

    public void publishSystemEventFromTableStateMonitor(SystemEvent event) {
        LOG.info("Publishing SystemEvent received by TableStateMonitor {}", event);
        cloudEventDispatcherInterface.publishEvent(event);
    }

    void publishEvents(Report report, int customerId, long equipmentId, String apId, long locationId) {

        realtimeEventPublisher.publishSipCallEvents(customerId, equipmentId, locationId, report.getVideoVoiceReportList());

        for (EventReport eventReport : report.getEventReportList()) {

            for (sts.OpensyncStats.EventReport.ClientSession apEventClientSession : eventReport.getClientSessionList()) {

                LOG.debug("Processing EventReport::ClientSession {}", apEventClientSession);

                if (apEventClientSession.hasClientAuthEvent()) {
                    processClientAuthEvent(customerId, equipmentId, locationId, apEventClientSession);
                    realtimeEventPublisher.publishClientAuthSystemEvent(customerId, equipmentId, locationId, apEventClientSession.getClientAuthEvent());
                }

                if (apEventClientSession.hasClientAssocEvent()) {
                    processClientAssocEvent(customerId, equipmentId, locationId, apEventClientSession);
                    realtimeEventPublisher.publishClientAssocEvent(customerId, equipmentId, locationId, apEventClientSession.getClientAssocEvent());
                }

                if (apEventClientSession.hasClientFirstDataEvent()) {
                    processClientFirstDataEvent(customerId, equipmentId, locationId, apEventClientSession);
                    realtimeEventPublisher.publishClientFirstDataEvent(customerId, equipmentId, locationId, apEventClientSession.getClientFirstDataEvent());

                }

                if (apEventClientSession.hasClientIdEvent()) {
                    processClientIdEvent(customerId, equipmentId, locationId, apEventClientSession);
                    realtimeEventPublisher.publishClientIdEvent(customerId, equipmentId, locationId, apEventClientSession.getClientIdEvent());

                }

                if (apEventClientSession.hasClientIpEvent()) {
                    processClientIpEvent(customerId, equipmentId, locationId, apEventClientSession);
                    realtimeEventPublisher.publishClientIpEvent(customerId, equipmentId, locationId, apEventClientSession.getClientIpEvent());

                }

                if (apEventClientSession.hasClientConnectEvent()) {
                    processClientConnectEvent(customerId, equipmentId, locationId, eventReport, apEventClientSession);
                    realtimeEventPublisher.publishClientConnectSuccessEvent(customerId, equipmentId, locationId, apEventClientSession.getClientConnectEvent());

                }

                if (apEventClientSession.hasClientDisconnectEvent()) {
                    processClientDisconnectEvent(customerId, equipmentId, locationId, apEventClientSession);
                    realtimeEventPublisher.publishClientDisconnectEvent(customerId, equipmentId, locationId, apEventClientSession.getClientDisconnectEvent());
                }

                if (apEventClientSession.hasClientTimeoutEvent()) {
                    processClientTimeoutEvent(customerId, equipmentId, locationId, apEventClientSession);
                    realtimeEventPublisher.publishClientTimeoutEvent(customerId, equipmentId, locationId, apEventClientSession.getClientTimeoutEvent());

                }

                if (apEventClientSession.hasClientFailureEvent()) {
                    processClientFailureEvent(customerId, equipmentId, locationId, apEventClientSession);
                    realtimeEventPublisher.publishClientFailureEvent(customerId, equipmentId, locationId, apEventClientSession.getClientFailureEvent());

                }

            }

            realtimeEventPublisher.publishChannelHopEvents(customerId, equipmentId, locationId, eventReport);

            realtimeEventPublisher.publishDhcpTransactionEvents(customerId, equipmentId, locationId, eventReport.getDhcpTransactionList());

        }

    }

    protected void processClientConnectEvent(int customerId, long equipmentId, long locationId, EventReport e,
            sts.OpensyncStats.EventReport.ClientSession apEventClientSession) {
        ClientConnectEvent apClientEvent = apEventClientSession.getClientConnectEvent();

        com.telecominfraproject.wlan.client.models.Client client = clientServiceInterface.getOrNull(customerId, MacAddress.valueOf(apClientEvent.getStaMac()));
        if (client == null) {
            client = new com.telecominfraproject.wlan.client.models.Client();
            client.setCustomerId(customerId);
            client.setMacAddress(MacAddress.valueOf(apClientEvent.getStaMac()));
            client.setDetails(new ClientInfoDetails());
            client = clientServiceInterface.create(client);
        }
        ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId, MacAddress.valueOf(apClientEvent.getStaMac()));
        if (clientSession == null) {
            clientSession = new ClientSession();
            clientSession.setCustomerId(customerId);
            clientSession.setEquipmentId(equipmentId);
            clientSession.setMacAddress(MacAddress.valueOf(apClientEvent.getStaMac()));
            clientSession.setLocationId(locationId);
            clientSession.setDetails(new ClientSessionDetails());
            clientSession.getDetails().setDhcpDetails(new ClientDhcpDetails(apEventClientSession.getSessionId()));
            clientSession.getDetails().setMetricDetails(new ClientSessionMetricDetails());
        }
        if (clientSession.getDetails().getPriorEquipmentId() == null) {
            clientSession.getDetails().setPriorEquipmentId(clientSession.getEquipmentId());
        }
        if (clientSession.getDetails().getPriorSessionId() == null) {
            if (clientSession.getDetails().getSessionId() != apEventClientSession.getSessionId())
                clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        if (clientSession.getDetails().getSessionId() != apEventClientSession.getSessionId()) {
            clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        clientSession.getDetails().setSessionId(apEventClientSession.getSessionId());
        clientSession.getDetails().setRadioType(OvsdbToWlanCloudTypeMappingUtility.getRadioTypeFromOpensyncStatsRadioBandType(apClientEvent.getBand()));
        clientSession.getDetails().setSsid(apClientEvent.getSsid());

        if (apClientEvent.hasEvTimeBootupInUsAssoc()) {
            clientSession.getDetails().setAssocTimestamp(apClientEvent.getEvTimeBootupInUsAssoc());
        }

        if (apClientEvent.hasEvTimeBootupInUsAuth()) {
            clientSession.getDetails().setAuthTimestamp(apClientEvent.getEvTimeBootupInUsAuth());
        }

        if (apClientEvent.hasEvTimeBootupInUsEapol()) {
            ClientEapDetails eapDetails = new ClientEapDetails();
            eapDetails.setEapSuccessTimestamp(apClientEvent.getEvTimeBootupInUsEapol());
            clientSession.getDetails().setEapDetails(eapDetails);
        }

        if (apClientEvent.hasEvTimeBootupInUsFirstRx()) {
            clientSession.getDetails().setFirstDataRcvdTimestamp(apClientEvent.getEvTimeBootupInUsFirstRx());
        }

        if (apClientEvent.hasEvTimeBootupInUsFirstTx()) {
            clientSession.getDetails().setFirstDataSentTimestamp(apClientEvent.getEvTimeBootupInUsFirstTx());
        }

        if (apClientEvent.hasEvTimeBootupInUsIp()) {
            clientSession.getDetails().setIpTimestamp(apClientEvent.getEvTimeBootupInUsIp());
        }

        if (apClientEvent.hasEvTimeBootupInUsPortEnable()) {
            clientSession.getDetails().setPortEnabledTimestamp(apClientEvent.getEvTimeBootupInUsPortEnable());
        }

        if (apClientEvent.hasCltId()) {
            clientSession.getDetails().setHostname(apClientEvent.getCltId());
        }

        if (apClientEvent.hasSecType()) {
            clientSession.getDetails().setSecurityType(OvsdbToWlanCloudTypeMappingUtility.getCloudSecurityTypeFromOpensyncStats(apClientEvent.getSecType()));
        }

        if (apClientEvent.hasAssocType()) {
            clientSession.getDetails().setIsReassociation(apClientEvent.getAssocType().equals(AssocType.REASSOC));
        }

        if (apClientEvent.hasAssocRssi()) {
            clientSession.getDetails().setAssocRssi(apClientEvent.getAssocRssi());
        }

        if (apClientEvent.hasUsing11K()) {
            clientSession.getDetails().setIs11KUsed(apClientEvent.getUsing11K());
        }

        if (apClientEvent.hasUsing11R()) {
            clientSession.getDetails().setIs11RUsed(apClientEvent.getUsing11R());

        }

        if (apClientEvent.hasUsing11V()) {
            clientSession.getDetails().setIs11VUsed(apClientEvent.getUsing11V());
        }

        if (apClientEvent.hasIpAddr()) {
            ByteString ipAddress = apClientEvent.getIpAddr();
            if (ipAddress != null) {
                try {
                    InetAddress inetAddress = InetAddress.getByAddress(ipAddress.toByteArray());
                    LOG.info("ipAddr {} ", inetAddress);
                    if (inetAddress instanceof Inet4Address) {
                        LOG.info("IPv4 address {}", inetAddress);
                        clientSession.getDetails().setIpAddress(inetAddress);
                    } else if (inetAddress instanceof Inet6Address) {
                        LOG.info("IPv6 address {}", inetAddress);
                        clientSession.getDetails().setIpAddress(inetAddress);
                    } else {
                        LOG.error("Invalid IP Address {}", ipAddress);
                    }
                    clientSession.getDetails().setIpTimestamp(apClientEvent.getTimestampMs());
                } catch (UnknownHostException ex) {
                }
            }
        }

        if (clientSession.getDetails().getAssociationState() != null
                && !clientSession.getDetails().getAssociationState().equals(AssociationState._802_11_Associated)) {
            clientSession.getDetails().setAssociationState(AssociationState._802_11_Associated);
            clientSession.getDetails().setAssocTimestamp(apClientEvent.getTimestampMs());
        }
        clientSession.getDetails().setLastEventTimestamp(apClientEvent.getTimestampMs());
        clientSession = clientServiceInterface.updateSession(clientSession);

    }

    protected void processClientDisconnectEvent(int customerId, long equipmentId, long locationId,
            sts.OpensyncStats.EventReport.ClientSession apEventClientSession) {
        ClientDisconnectEvent apClientEvent = apEventClientSession.getClientDisconnectEvent();
        com.telecominfraproject.wlan.client.models.Client client = clientServiceInterface.getOrNull(customerId, MacAddress.valueOf(apClientEvent.getStaMac()));
        if (client == null) {
            client = new com.telecominfraproject.wlan.client.models.Client();
            client.setCustomerId(customerId);
            client.setMacAddress(MacAddress.valueOf(apClientEvent.getStaMac()));
            client.setDetails(new ClientInfoDetails());
            client = clientServiceInterface.create(client);
        }
        ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId, MacAddress.valueOf(apClientEvent.getStaMac()));
        if (clientSession == null) {
            clientSession = new ClientSession();
            clientSession.setCustomerId(customerId);
            clientSession.setEquipmentId(equipmentId);
            clientSession.setMacAddress(MacAddress.valueOf(apClientEvent.getStaMac()));
            clientSession.setLocationId(locationId);
            clientSession.setDetails(new ClientSessionDetails());
            clientSession.getDetails().setDhcpDetails(new ClientDhcpDetails(apEventClientSession.getSessionId()));
            clientSession.getDetails().setMetricDetails(new ClientSessionMetricDetails());
        }
        if (clientSession.getDetails().getPriorEquipmentId() == null) {
            clientSession.getDetails().setPriorEquipmentId(clientSession.getEquipmentId());
        }
        if (clientSession.getDetails().getPriorSessionId() == null) {
            if (clientSession.getDetails().getSessionId() != apEventClientSession.getSessionId())
                clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        if (clientSession.getDetails().getSessionId() != apEventClientSession.getSessionId()) {
            clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        clientSession.getDetails().setSessionId(apEventClientSession.getSessionId());
        clientSession.getDetails().setRadioType(OvsdbToWlanCloudTypeMappingUtility.getRadioTypeFromOpensyncStatsRadioBandType(apClientEvent.getBand()));
        clientSession.getDetails().setSsid(apClientEvent.getSsid());
        if (apClientEvent.hasDevType()) {
            if (apClientEvent.getDevType().equals(DeviceType.DEV_AP)) {
                clientSession.getDetails().setDisconnectByApTimestamp(apClientEvent.getTimestampMs());
                if (apClientEvent.hasInternalRc()) {
                    clientSession.getDetails().setDisconnectByApInternalReasonCode(apClientEvent.getInternalRc());
                }
                if (apClientEvent.hasReason()) {
                    clientSession.getDetails().setDisconnectByApReasonCode(apClientEvent.getReason());
                }
            } else {
                clientSession.getDetails().setDisconnectByClientTimestamp(apClientEvent.getTimestampMs());
                if (apClientEvent.hasInternalRc()) {
                    clientSession.getDetails().setDisconnectByClientInternalReasonCode(apClientEvent.getInternalRc());
                }
                if (apClientEvent.hasReason()) {
                    clientSession.getDetails().setDisconnectByClientReasonCode(apClientEvent.getReason());
                }
            }
        }
        if (apClientEvent.hasFrType()) {
            if (apClientEvent.getFrType().equals(FrameType.FT_DEAUTH)) {
            }
            if (apClientEvent.getFrType().equals(FrameType.FT_DISASSOC)) {
            }
        }
        if (apClientEvent.hasRssi()) {
            clientSession.getDetails().setAssocRssi(apClientEvent.getRssi());
        }
        if (apClientEvent.hasLrcvUpTsInUs()) {
            clientSession.getDetails().setLastRxTimestamp(apClientEvent.getLrcvUpTsInUs());
        }
        if (apClientEvent.hasLsentUpTsInUs()) {
            clientSession.getDetails().setLastTxTimestamp(apClientEvent.getLsentUpTsInUs());
        }
        clientSession.getDetails().setAssociationState(AssociationState.Disconnected);
        clientSession.getDetails().setAssocTimestamp(apClientEvent.getTimestampMs());
        clientSession.getDetails().setLastEventTimestamp(apClientEvent.getTimestampMs());
        clientSession = clientServiceInterface.updateSession(clientSession);

    }

    protected void processClientAuthEvent(int customerId, long equipmentId, long locationId, sts.OpensyncStats.EventReport.ClientSession apEventClientSession) {
        ClientAuthEvent apClientEvent = apEventClientSession.getClientAuthEvent();
        com.telecominfraproject.wlan.client.models.Client client = clientServiceInterface.getOrNull(customerId, MacAddress.valueOf(apClientEvent.getStaMac()));

        if (client == null) {
            client = new com.telecominfraproject.wlan.client.models.Client();
            client.setCustomerId(customerId);
            client.setMacAddress(MacAddress.valueOf(apClientEvent.getStaMac()));
            client.setDetails(new ClientInfoDetails());
            client = clientServiceInterface.create(client);
        }

        ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId, MacAddress.valueOf(apClientEvent.getStaMac()));

        if (clientSession == null) {
            clientSession = new ClientSession();
            clientSession.setCustomerId(customerId);
            clientSession.setEquipmentId(equipmentId);
            clientSession.setMacAddress(MacAddress.valueOf(apClientEvent.getStaMac()));
            clientSession.setLocationId(locationId);
            clientSession.setDetails(new ClientSessionDetails());
            clientSession.getDetails().setDhcpDetails(new ClientDhcpDetails(apEventClientSession.getSessionId()));
            clientSession.getDetails().setMetricDetails(new ClientSessionMetricDetails());
        }
        if (clientSession.getDetails().getPriorEquipmentId() == null) {
            clientSession.getDetails().setPriorEquipmentId(clientSession.getEquipmentId());
        }
        if (clientSession.getDetails().getPriorSessionId() == null) {
            if (clientSession.getDetails().getSessionId() != apEventClientSession.getSessionId())
                clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        if (clientSession.getDetails().getSessionId() != apEventClientSession.getSessionId()) {
            clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        clientSession.getDetails().setSessionId(apEventClientSession.getSessionId());
        clientSession.getDetails().setRadioType(OvsdbToWlanCloudTypeMappingUtility.getRadioTypeFromOpensyncStatsRadioBandType(apClientEvent.getBand()));
        clientSession.getDetails().setSsid(apClientEvent.getSsid());
        if (apClientEvent.hasAuthStatus()) {
            clientSession.getDetails().setAssociationStatus(apClientEvent.getAuthStatus());
        }
        clientSession.getDetails().setAuthTimestamp(apClientEvent.getTimestampMs());
        clientSession.getDetails().setAssociationState(AssociationState._802_11_Authenticated);
        clientSession.getDetails().setLastEventTimestamp(apClientEvent.getTimestampMs());
        clientSession = clientServiceInterface.updateSession(clientSession);
    }

    protected void processClientAssocEvent(int customerId, long equipmentId, long locationId,
            sts.OpensyncStats.EventReport.ClientSession apEventClientSession) {
        ClientAssocEvent apClientEvent = apEventClientSession.getClientAssocEvent();

        com.telecominfraproject.wlan.client.models.Client client = clientServiceInterface.getOrNull(customerId, MacAddress.valueOf(apClientEvent.getStaMac()));

        if (client == null) {
            client = new com.telecominfraproject.wlan.client.models.Client();
            client.setCustomerId(customerId);
            client.setMacAddress(MacAddress.valueOf(apClientEvent.getStaMac()));
            client.setDetails(new ClientInfoDetails());
            client = clientServiceInterface.create(client);
        }

        ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId, MacAddress.valueOf(apClientEvent.getStaMac()));

        if (clientSession == null) {
            clientSession = new ClientSession();
            clientSession.setCustomerId(customerId);
            clientSession.setEquipmentId(equipmentId);
            clientSession.setMacAddress(MacAddress.valueOf(apClientEvent.getStaMac()));
            clientSession.setLocationId(locationId);
            clientSession.setDetails(new ClientSessionDetails());
            clientSession.getDetails().setDhcpDetails(new ClientDhcpDetails(apEventClientSession.getSessionId()));
            clientSession.getDetails().setMetricDetails(new ClientSessionMetricDetails());
        }
        if (clientSession.getDetails().getPriorEquipmentId() == null) {
            clientSession.getDetails().setPriorEquipmentId(clientSession.getEquipmentId());
        }
        if (clientSession.getDetails().getPriorSessionId() == null) {
            if (clientSession.getDetails().getSessionId() != apEventClientSession.getSessionId())
                clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        if (clientSession.getDetails().getSessionId() != apEventClientSession.getSessionId()) {
            clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        clientSession.getDetails().setSessionId(apEventClientSession.getSessionId());
        clientSession.getDetails().setRadioType(OvsdbToWlanCloudTypeMappingUtility.getRadioTypeFromOpensyncStatsRadioBandType(apClientEvent.getBand()));
        clientSession.getDetails().setSsid(apClientEvent.getSsid());
        if (apClientEvent.hasStatus()) {
            clientSession.getDetails().setAssociationStatus(apClientEvent.getStatus());
        }
        clientSession.getDetails().setRadioType(OvsdbToWlanCloudTypeMappingUtility.getRadioTypeFromOpensyncStatsRadioBandType(apClientEvent.getBand()));
        if (apClientEvent.hasUsing11K()) {
            clientSession.getDetails().setIs11KUsed(apClientEvent.getUsing11K());
        }
        if (apClientEvent.hasUsing11R()) {
            clientSession.getDetails().setIs11RUsed(apClientEvent.getUsing11R());
        }
        if (apClientEvent.hasUsing11V()) {
            clientSession.getDetails().setIs11VUsed(apClientEvent.getUsing11V());
        }
        if (apClientEvent.hasAssocType()) {
            clientSession.getDetails().setIsReassociation(apClientEvent.getAssocType().equals(AssocType.REASSOC));
        }
        if (apClientEvent.hasRssi()) {
            clientSession.getDetails().setAssocRssi(apClientEvent.getRssi());
        }
        if (apClientEvent.hasInternalSc()) {
            clientSession.getDetails().setAssocInternalSC(apClientEvent.getInternalSc());
        }
        clientSession.getDetails().setAssocTimestamp(apClientEvent.getTimestampMs());
        clientSession.getDetails().setAssociationState(AssociationState._802_11_Associated);
        clientSession.getDetails().setLastEventTimestamp(apClientEvent.getTimestampMs());
        clientSession = clientServiceInterface.updateSession(clientSession);
    }

    protected void processClientFailureEvent(int customerId, long equipmentId, long locationId,
            sts.OpensyncStats.EventReport.ClientSession apEventClientSession) {
        ClientFailureEvent apClientEvent = apEventClientSession.getClientFailureEvent();
        com.telecominfraproject.wlan.client.models.Client client = clientServiceInterface.getOrNull(customerId, MacAddress.valueOf(apClientEvent.getStaMac()));
        if (client == null) {
            client = new com.telecominfraproject.wlan.client.models.Client();
            client.setCustomerId(customerId);
            client.setMacAddress(MacAddress.valueOf(apClientEvent.getStaMac()));
            client.setDetails(new ClientInfoDetails());
            client = clientServiceInterface.create(client);
        }
        ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId, MacAddress.valueOf(apClientEvent.getStaMac()));
        if (clientSession == null) {
            clientSession = new ClientSession();
            clientSession.setCustomerId(customerId);
            clientSession.setEquipmentId(equipmentId);
            clientSession.setMacAddress(MacAddress.valueOf(apClientEvent.getStaMac()));
            clientSession.setLocationId(locationId);
            clientSession.setDetails(new ClientSessionDetails());
            clientSession.getDetails().setDhcpDetails(new ClientDhcpDetails(apEventClientSession.getSessionId()));
            clientSession.getDetails().setMetricDetails(new ClientSessionMetricDetails());
        }
        if (clientSession.getDetails().getPriorEquipmentId() == null) {
            clientSession.getDetails().setPriorEquipmentId(clientSession.getEquipmentId());
        }
        if (clientSession.getDetails().getPriorSessionId() == null) {
            if (clientSession.getDetails().getSessionId() != apEventClientSession.getSessionId())
                clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        if (clientSession.getDetails().getSessionId() != apEventClientSession.getSessionId()) {
            clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        clientSession.getDetails().setSessionId(apEventClientSession.getSessionId());
        clientSession.getDetails().setSsid(apClientEvent.getSsid());

        ClientFailureDetails clientFailureDetails = new ClientFailureDetails();
        if (apClientEvent.hasReasonStr()) {
            clientFailureDetails.setReason(apClientEvent.getReasonStr());
        }
        if (apClientEvent.hasReasonCode()) {
            clientFailureDetails.setReasonCode(apClientEvent.getReasonCode());
        }
        clientSession.getDetails().setLastFailureDetails(clientFailureDetails);
        clientSession.getDetails().setLastEventTimestamp(apClientEvent.getTimestampMs());
        clientSession = clientServiceInterface.updateSession(clientSession);

    }

    protected void processClientFirstDataEvent(int customerId, long equipmentId, long locationId,
            sts.OpensyncStats.EventReport.ClientSession apEventClientSession) {
        ClientFirstDataEvent apClientEvent = apEventClientSession.getClientFirstDataEvent();
        com.telecominfraproject.wlan.client.models.Client client = clientServiceInterface.getOrNull(customerId, MacAddress.valueOf(apClientEvent.getStaMac()));
        if (client == null) {
            client = new com.telecominfraproject.wlan.client.models.Client();
            client.setCustomerId(customerId);
            client.setMacAddress(MacAddress.valueOf(apClientEvent.getStaMac()));
            client.setDetails(new ClientInfoDetails());
            client = clientServiceInterface.create(client);
        }
        ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId, MacAddress.valueOf(apClientEvent.getStaMac()));
        if (clientSession == null) {
            clientSession = new ClientSession();
            clientSession.setCustomerId(customerId);
            clientSession.setEquipmentId(equipmentId);
            clientSession.setMacAddress(MacAddress.valueOf(apClientEvent.getStaMac()));
            clientSession.setLocationId(locationId);
            clientSession.setDetails(new ClientSessionDetails());
            clientSession.getDetails().setDhcpDetails(new ClientDhcpDetails(apEventClientSession.getSessionId()));
            clientSession.getDetails().setMetricDetails(new ClientSessionMetricDetails());
        }
        if (clientSession.getDetails().getPriorEquipmentId() == null) {
            clientSession.getDetails().setPriorEquipmentId(clientSession.getEquipmentId());
        }
        if (clientSession.getDetails().getPriorSessionId() == null) {
            if (clientSession.getDetails().getSessionId() != apEventClientSession.getSessionId())
                clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        if (clientSession.getDetails().getSessionId() != apEventClientSession.getSessionId()) {
            clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        clientSession.getDetails().setSessionId(apEventClientSession.getSessionId());

        if (apClientEvent.hasFdataRxUpTsInUs()) {
            clientSession.getDetails().setFirstDataRcvdTimestamp(apClientEvent.getFdataRxUpTsInUs());
        }

        if (apClientEvent.hasFdataTxUpTsInUs()) {
            clientSession.getDetails().setFirstDataSentTimestamp(apClientEvent.getFdataTxUpTsInUs());
        }
        clientSession.getDetails().setLastEventTimestamp(apClientEvent.getTimestampMs());
        clientSession = clientServiceInterface.updateSession(clientSession);

    }

    protected void processClientIdEvent(int customerId, long equipmentId, long locationId, sts.OpensyncStats.EventReport.ClientSession apEventClientSession) {
        ClientIdEvent apClientEvent = apEventClientSession.getClientIdEvent();
        com.telecominfraproject.wlan.client.models.Client client = clientServiceInterface.getOrNull(customerId, MacAddress.valueOf(apClientEvent.getCltMac()));
        if (client == null) {
            client = new com.telecominfraproject.wlan.client.models.Client();
            client.setCustomerId(customerId);
            client.setMacAddress(MacAddress.valueOf(apClientEvent.getCltMac()));
            client.setDetails(new ClientInfoDetails());
            client = clientServiceInterface.create(client);
        }
        if (apClientEvent.hasCltId()) {
            ((ClientInfoDetails) client.getDetails()).setHostName(apClientEvent.getCltId());
        }
        client = clientServiceInterface.update(client);

        ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId, MacAddress.valueOf(apClientEvent.getCltMac()));
        if (clientSession == null) {
            clientSession = new ClientSession();
            clientSession.setCustomerId(customerId);
            clientSession.setEquipmentId(equipmentId);
            clientSession.setMacAddress(MacAddress.valueOf(apClientEvent.getCltMac()));
            clientSession.setLocationId(locationId);
            clientSession.setDetails(new ClientSessionDetails());
            clientSession.getDetails().setDhcpDetails(new ClientDhcpDetails(apEventClientSession.getSessionId()));
            clientSession.getDetails().setMetricDetails(new ClientSessionMetricDetails());
        }
        if (clientSession.getDetails().getPriorEquipmentId() == null) {
            clientSession.getDetails().setPriorEquipmentId(clientSession.getEquipmentId());
        }
        if (clientSession.getDetails().getPriorSessionId() == null) {
            if (clientSession.getDetails().getSessionId() != apEventClientSession.getSessionId())
                clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        if (clientSession.getDetails().getSessionId() != apEventClientSession.getSessionId()) {
            clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        clientSession.getDetails().setSessionId(apEventClientSession.getSessionId());
        if (apClientEvent.hasCltId()) {
            clientSession.getDetails().setHostname(apClientEvent.getCltId());
        }
        clientSession.getDetails().setLastEventTimestamp(apClientEvent.getTimestampMs());
        clientSession = clientServiceInterface.updateSession(clientSession);
    }

    protected void processClientIpEvent(int customerId, long equipmentId, long locationId, sts.OpensyncStats.EventReport.ClientSession apEventClientSession) {
        ClientIpEvent apClientEvent = apEventClientSession.getClientIpEvent();
        com.telecominfraproject.wlan.client.models.Client client = clientServiceInterface.getOrNull(customerId, MacAddress.valueOf(apClientEvent.getStaMac()));
        if (client == null) {
            client = new com.telecominfraproject.wlan.client.models.Client();
            client.setCustomerId(customerId);
            client.setMacAddress(MacAddress.valueOf(apClientEvent.getStaMac()));
            client.setDetails(new ClientInfoDetails());
            client = clientServiceInterface.create(client);
        }
        client = clientServiceInterface.update(client);

        ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId, MacAddress.valueOf(apClientEvent.getStaMac()));
        if (clientSession == null) {
            clientSession = new ClientSession();
            clientSession.setCustomerId(customerId);
            clientSession.setEquipmentId(equipmentId);
            clientSession.setMacAddress(MacAddress.valueOf(apClientEvent.getStaMac()));
            clientSession.setLocationId(locationId);
            clientSession.setDetails(new ClientSessionDetails());
            clientSession.getDetails().setDhcpDetails(new ClientDhcpDetails(apEventClientSession.getSessionId()));
            clientSession.getDetails().setMetricDetails(new ClientSessionMetricDetails());
        }
        if (clientSession.getDetails().getPriorEquipmentId() == null) {
            clientSession.getDetails().setPriorEquipmentId(clientSession.getEquipmentId());
        }
        if (clientSession.getDetails().getPriorSessionId() == null) {
            if (clientSession.getDetails().getSessionId() != apEventClientSession.getSessionId())
                clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        if (clientSession.getDetails().getSessionId() != apEventClientSession.getSessionId()) {
            clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        clientSession.getDetails().setSessionId(apEventClientSession.getSessionId());
        if (apClientEvent.hasIpAddr()) {
            ByteString ipAddress = apClientEvent.getIpAddr();
            if (ipAddress != null) {
                try {
                    InetAddress inetAddress = InetAddress.getByAddress(ipAddress.toByteArray());
                    if (inetAddress instanceof Inet4Address) {
                        LOG.info("IPv4 address {}", inetAddress);
                        clientSession.getDetails().setIpAddress(inetAddress);
                    } else if (inetAddress instanceof Inet6Address) {
                        LOG.info("IPv6 address {}", inetAddress);
                        clientSession.getDetails().setIpAddress(inetAddress);
                    } else {
                        LOG.error("Invalid IP Address {}", ipAddress);
                    }
                    clientSession.getDetails().setIpTimestamp(apClientEvent.getTimestampMs());

                } catch (UnknownHostException ex) {
                }
            }
        }
        clientSession.getDetails().setLastEventTimestamp(apClientEvent.getTimestampMs());
        clientSession = clientServiceInterface.updateSession(clientSession);
    }

    protected void processClientTimeoutEvent(int customerId, long equipmentId, long locationId,
            sts.OpensyncStats.EventReport.ClientSession apEventClientSession) {
        ClientTimeoutEvent apClientEvent = apEventClientSession.getClientTimeoutEvent();
        com.telecominfraproject.wlan.client.models.Client client = clientServiceInterface.getOrNull(customerId, MacAddress.valueOf(apClientEvent.getStaMac()));
        if (client == null) {
            client = new com.telecominfraproject.wlan.client.models.Client();
            client.setCustomerId(customerId);
            client.setMacAddress(MacAddress.valueOf(apClientEvent.getStaMac()));
            client.setDetails(new ClientInfoDetails());
            client = clientServiceInterface.create(client);
        }
        client = clientServiceInterface.update(client);
        ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId, MacAddress.valueOf(apClientEvent.getStaMac()));
        if (clientSession == null) {
            clientSession = new ClientSession();
            clientSession.setCustomerId(customerId);
            clientSession.setEquipmentId(equipmentId);
            clientSession.setMacAddress(MacAddress.valueOf(apClientEvent.getStaMac()));
            clientSession.setLocationId(locationId);
            clientSession.setDetails(new ClientSessionDetails());
            clientSession.getDetails().setDhcpDetails(new ClientDhcpDetails(apEventClientSession.getSessionId()));
            clientSession.getDetails().setMetricDetails(new ClientSessionMetricDetails());
        }
        if (clientSession.getDetails().getPriorEquipmentId() == null) {
            clientSession.getDetails().setPriorEquipmentId(clientSession.getEquipmentId());
        }
        if (clientSession.getDetails().getPriorSessionId() == null) {
            if (clientSession.getDetails().getSessionId() != apEventClientSession.getSessionId())
                clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        if (clientSession.getDetails().getSessionId() != apEventClientSession.getSessionId()) {
            clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        clientSession.getDetails().setSessionId(apEventClientSession.getSessionId());
        if (apClientEvent.hasLastRcvUpTsInUs()) {
            clientSession.getDetails().setLastRxTimestamp(apClientEvent.getLastRcvUpTsInUs());
        }
        if (apClientEvent.hasLastSentUpTsInUs()) {
            clientSession.getDetails().setLastTxTimestamp(apClientEvent.getLastSentUpTsInUs());
        }
        clientSession.getDetails().setTimeoutTimestamp(apClientEvent.getTimestampMs());
        clientSession.getDetails().setAssociationState(AssociationState.AP_Timeout);
        clientSession.getDetails().setLastEventTimestamp(apClientEvent.getTimestampMs());
        clientSession = clientServiceInterface.updateSession(clientSession);
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
            }

            if (deviceReport.hasCpuUtil() && deviceReport.getCpuUtil().hasCpuUtil()) {
                Integer cpuUtilization = deviceReport.getCpuUtil().getCpuUtil();
                apPerformance.setCpuUtilized(new int[] {cpuUtilization});
            }

            apPerformance.setEthLinkState(EthernetLinkState.UP1000_FULL_DUPLEX);

            if (deviceReport.hasMemUtil() && deviceReport.getMemUtil().hasMemTotal() && deviceReport.getMemUtil().hasMemUsed()) {
                apPerformance.setFreeMemory(deviceReport.getMemUtil().getMemTotal() - deviceReport.getMemUtil().getMemUsed());
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
                    
                    // The service metric report's sourceTimestamp will be the most recent timestamp from its contributing stats
                    if (apNodeMetrics.getSourceTimestampMs() < survey.getTimestampMs())
                        apNodeMetrics.setSourceTimestampMs(survey.getTimestampMs());
                    
                    int pctBusyTx = busyTx / totalDurationMs;
                    checkIfOutOfBound("pctBusyTx", pctBusyTx, survey, totalDurationMs, busyTx, busyRx, busy, busySelf);

                    radioUtil.setAssocClientTx(pctBusyTx);
                    int pctBusyRx = busyRx / totalDurationMs;
                    checkIfOutOfBound("pctBusyRx", pctBusyRx, survey, totalDurationMs, busyTx, busyRx, busy, busySelf);
                    radioUtil.setAssocClientRx(pctBusyRx);

                    double pctIBSS = (busyTx + busySelf) / totalDurationMs;
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

            LOG.info("Update NetworkAdminStatusReport for NetworkProbeMetrics {}", n.toString());

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

    void updateDeviceStatusRadioUtilizationReport(int customerId, long equipmentId, RadioUtilizationReport radioUtilizationReport) {
        LOG.info("Processing updateDeviceStatusRadioUtilizationReport for equipmentId {} with RadioUtilizationReport {}", equipmentId, radioUtilizationReport);

        Status radioUtilizationStatus = statusServiceInterface.getOrNull(customerId, equipmentId, StatusDataType.RADIO_UTILIZATION);

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
        LOG.debug("updated status {}", status);
    }

    void populateApClientMetrics(List<ServiceMetric> metricRecordList, Report report, int customerId, long equipmentId, long locationId) {
        LOG.info("populateApClientMetrics for Customer {} Equipment {}", customerId, equipmentId);

        for (ClientReport clReport : report.getClientsList()) {
            for (Client cl : clReport.getClientListList()) {

                if (cl.getMacAddress() == null) {
                    LOG.debug("No mac address for Client {}, cannot set device mac address for client in ClientMetrics.", cl);
                    continue;
                }

                LOG.debug("Processing ClientReport from AP {}", cl.getMacAddress());

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

                ClientSession session = clientServiceInterface.getSessionOrNull(customerId, equipmentId, MacAddress.valueOf(cl.getMacAddress()));
                if (session != null) {
                    cMetrics.setSessionId(session.getDetails().getSessionId());
                    LOG.debug("populateApClientMetrics Session Id {}", session.getDetails().getSessionId());
                }
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
                        cMetrics.setRates(new int[] {Double.valueOf(cl.getStats().getTxRate() / 1000).intValue(),
                                Double.valueOf(cl.getStats().getRxRate() / 1000).intValue()});
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
                }

                LOG.debug("ApClientMetrics Report {}", cMetrics);

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

    ClientSession handleClientSessionMetricsUpdate(int customerId, long equipmentId, long locationId, RadioType radioType, long timestamp,
            sts.OpensyncStats.Client client) {
        try

        {
            LOG.info("handleClientSessionUpdate for customerId {} equipmentId {} locationId {} client {} ", customerId, equipmentId, locationId,
                    client.getMacAddress());

            ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId, MacAddress.valueOf(client.getMacAddress()));

            if (clientSession == null) {
                LOG.warn("Cannot get client session for {}", client.getMacAddress());
                return null;
            }

            ClientSessionDetails latestClientSessionDetails = clientSession.getDetails();
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

        LOG.info("calculateClientSessionMetricDetails for Client {} at timestamp {}", client.getMacAddress(), timestamp);

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
            float txBytesFv = Long.valueOf(client.getStats().getTxBytes()).floatValue();
            float txBytesToMb = txBytesFv / 125000F;

            if (LOG.isDebugEnabled())
                LOG.debug("rxBytesToMb {} txBytesToMb {} ", rxBytesToMb, txBytesToMb);

            metricDetails.setRxMbps(rxBytesToMb / durationSec);
            metricDetails.setTxMbps(txBytesToMb / durationSec);
            if (LOG.isDebugEnabled())
                LOG.debug("RxMbps {} TxMbps {} ", metricDetails.getRxMbps(), metricDetails.getTxMbps());

        } else {
            LOG.warn("Cannot calculate tx/rx throughput for Client {} based on duration of {} Ms", client.getMacAddress(), client.getDurationMs());
        }
        metricDetails.setLastMetricTimestamp(timestamp);

        return metricDetails;
    }

    void populateApSsidMetrics(List<ServiceMetric> metricRecordList, Report report, int customerId, long equipmentId, String apId, long locationId) {

        LOG.info("populateApSsidMetrics for Customer {} Equipment {}", customerId, equipmentId);
        ServiceMetric smr = new ServiceMetric(customerId, equipmentId);
        smr.setLocationId(locationId);
        ApSsidMetrics apSsidMetrics = new ApSsidMetrics();

        smr.setDetails(apSsidMetrics);
        metricRecordList.add(smr);

        for (ClientReport clientReport : report.getClientsList()) {

            LOG.debug("ClientReport for channel {} RadioBand {}", clientReport.getChannel(), clientReport.getBand());

            // The service metric report's sourceTimestamp will be the most recent ClientReport timestamp
            if (apSsidMetrics.getSourceTimestampMs() < clientReport.getTimestampMs())
                apSsidMetrics.setSourceTimestampMs(clientReport.getTimestampMs());
            
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

            RadioType radioType = OvsdbToWlanCloudTypeMappingUtility.getRadioTypeFromOpensyncStatsRadioBandType(clientReport.getBand());

            SsidStatistics ssidStatistics = new SsidStatistics();
            // GET the Radio IF MAC (BSSID) from the activeBSSIDs
            ssidStatistics.setSourceTimestampMs(clientReport.getTimestampMs());
            Status activeBssidsStatus = statusServiceInterface.getOrNull(customerId, equipmentId, StatusDataType.ACTIVE_BSSIDS);
            if (activeBssidsStatus != null && activeBssidsStatus.getDetails() != null
                    && ((ActiveBSSIDs) activeBssidsStatus.getDetails()).getActiveBSSIDs() != null) {
                for (ActiveBSSID activeBSSID : ((ActiveBSSIDs) activeBssidsStatus.getDetails()).getActiveBSSIDs()) {
                    if (activeBSSID.getRadioType().equals(radioType)) {
                        ssidStatistics.setBssid(MacAddress.valueOf(activeBSSID.getBssid()));
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
                    rxFrames += client.getStats().getRxFrames();
                    txFrames += client.getStats().getTxFrames();
                    rxRetries += client.getStats().getRxRetries();
                    txRetries += client.getStats().getTxRetries();
                    rxErrors += client.getStats().getRxErrors();
                    txErrors += client.getStats().getTxErrors();
                    lastRssi = client.getStats().getRssi();
                    
                    if (client.hasConnected() && client.getConnected() && client.hasMacAddress()) {
                        numConnectedClients += 1;
                    }
                }

            }

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
