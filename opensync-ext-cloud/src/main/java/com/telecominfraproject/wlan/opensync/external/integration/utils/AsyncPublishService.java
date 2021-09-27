package com.telecominfraproject.wlan.opensync.external.integration.utils;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.common.base.Objects;
import com.google.protobuf.ByteString;
import com.telecominfraproject.wlan.client.ClientServiceInterface;
import com.telecominfraproject.wlan.client.info.models.ClientInfoDetails;
import com.telecominfraproject.wlan.client.session.models.AssociationState;
import com.telecominfraproject.wlan.client.session.models.ClientDhcpDetails;
import com.telecominfraproject.wlan.client.session.models.ClientSession;
import com.telecominfraproject.wlan.client.session.models.ClientSessionDetails;
import com.telecominfraproject.wlan.cloudeventdispatcher.CloudEventDispatcherInterface;
import com.telecominfraproject.wlan.core.model.equipment.MacAddress;
import com.telecominfraproject.wlan.opensync.util.OvsdbToWlanCloudTypeMappingUtility;
import com.telecominfraproject.wlan.servicemetric.models.ServiceMetric;

import sts.OpensyncStats.AssocType;
import sts.OpensyncStats.DeviceType;
import sts.OpensyncStats.EventReport;
import sts.OpensyncStats.FrameType;
import sts.OpensyncStats.Report;
import sts.OpensyncStats.EventReport.ClientAssocEvent;
import sts.OpensyncStats.EventReport.ClientAuthEvent;
import sts.OpensyncStats.EventReport.ClientDisconnectEvent;
import sts.OpensyncStats.EventReport.ClientIpEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * MqttStatsPublisher will use this class to publish Stats and Events asynchronously.
 * We use async to decrease the processing time for the gateway.
 * 
 * Note: @Async only applies on public method and cannot have self-invocation (i.e. cannot
 * calling the async method from within the same class)
 */
@Service
public class AsyncPublishService {
	
	private static final Logger LOG = LoggerFactory.getLogger(MqttStatsPublisher.class);
	
    @Autowired
    private CloudEventDispatcherInterface cloudEventDispatcherInterface;
    
    @Autowired
    private RealtimeEventPublisher realtimeEventPublisher;
    
    @Autowired
    private ClientServiceInterface clientServiceInterface;
    
    
    @Async
    public void asyncPublishStats(String apId, List<ServiceMetric> metricRecordList) {
        try {
            long publishStart = System.nanoTime();
            cloudEventDispatcherInterface.publishMetrics(metricRecordList);
            long publishStop = System.nanoTime();
            if (LOG.isDebugEnabled())
                LOG.debug("Elapsed publishing time for metrics records from AP {} is {} milliseconds", apId,
                        TimeUnit.MILLISECONDS.convert(publishStop - publishStart, TimeUnit.NANOSECONDS));
        } catch (Exception e) {
            LOG.error("Exception when trying to publishServiceMetrics.", e);
        }
    }

    @Async
    public void asyncPublishEvents(Report report, int customerId, long equipmentId, String apId, long locationId) {
        try {
            long mqttEventsStart = System.nanoTime();
            publishEvents(report, customerId, equipmentId, apId, locationId);
            long mqttEventsStop = System.nanoTime();
            if (LOG.isDebugEnabled())
                LOG.debug("Elapsed publishing time for mqtt events from AP {} is {} milliseconds", apId,
                        TimeUnit.MILLISECONDS.convert(mqttEventsStop - mqttEventsStart, TimeUnit.NANOSECONDS));
        } catch (Exception e) {
            LOG.error("Exception when trying to publishEvents.", e);
        }
    }
    
    void publishEvents(Report report, int customerId, long equipmentId, String apId, long locationId) {

        realtimeEventPublisher.publishSipCallEvents(customerId, equipmentId, locationId, report.getVideoVoiceReportList());

        for (EventReport eventReport : report.getEventReportList()) {

            for (sts.OpensyncStats.EventReport.ClientSession apEventClientSession : eventReport.getClientSessionList()) {

                LOG.debug("Processing EventReport::ClientSession for AP {}", apId);
                // for the following MQTT events, the client/client session is first updated, then the real time event
                // is published.
                if (apEventClientSession.hasClientAuthEvent()) {
                    processClientAuthEvent(customerId, equipmentId, locationId, apEventClientSession);
                }
                if (apEventClientSession.hasClientAssocEvent()) {
                    processClientAssocEvent(customerId, equipmentId, locationId, apEventClientSession);
                }
                if (apEventClientSession.hasClientIpEvent()) {
                    processClientIpEvent(customerId, equipmentId, locationId, apEventClientSession);
                }
                if (apEventClientSession.hasClientDisconnectEvent()) {
                    processClientDisconnectEvent(customerId, equipmentId, locationId, apEventClientSession);
                }
            }
            realtimeEventPublisher.publishChannelHopEvents(customerId, equipmentId, locationId, eventReport);
        }

    }
    
    private void processClientAuthEvent(int customerId, long equipmentId, long locationId, sts.OpensyncStats.EventReport.ClientSession apEventClientSession) {
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
            clientSession.getDetails().setDhcpDetails(new ClientDhcpDetails(Long.toUnsignedString(apEventClientSession.getSessionId())));
        }
        if (clientSession.getDetails().getPriorEquipmentId() == null) {
            clientSession.getDetails().setPriorEquipmentId(clientSession.getEquipmentId());
        }
        if (clientSession.getDetails().getPriorSessionId() == null) {
            if (!Objects.equal(clientSession.getDetails().getSessionId(), Long.toUnsignedString(apEventClientSession.getSessionId())))
                clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        if (!Objects.equal(clientSession.getDetails().getSessionId(), Long.toUnsignedString(apEventClientSession.getSessionId()))) {
            clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        clientSession.getDetails().setSessionId(Long.toUnsignedString(apEventClientSession.getSessionId()));
        clientSession.getDetails().setRadioType(OvsdbToWlanCloudTypeMappingUtility.getRadioTypeFromOpensyncStatsRadioBandType(apClientEvent.getBand()));
        clientSession.getDetails().setSsid(apClientEvent.getSsid());
        if (apClientEvent.hasAuthStatus()) {
            clientSession.getDetails().setAssociationStatus(apClientEvent.getAuthStatus());
        }
        clientSession.getDetails().setAuthTimestamp(apClientEvent.getTimestampMs());
        clientSession.getDetails().setAssociationState(AssociationState._802_11_Authenticated);
        clientSession.getDetails().setLastEventTimestamp(apClientEvent.getTimestampMs());
        clientSession = clientServiceInterface.updateSession(clientSession);

        realtimeEventPublisher.publishClientAuthSystemEvent(customerId, equipmentId, locationId, apEventClientSession.getClientAuthEvent());

    }
    
    private void processClientAssocEvent(int customerId, long equipmentId, long locationId,
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
            clientSession.getDetails().setDhcpDetails(new ClientDhcpDetails(Long.toUnsignedString(apEventClientSession.getSessionId())));
        }
        if (clientSession.getDetails().getPriorEquipmentId() == null) {
            clientSession.getDetails().setPriorEquipmentId(clientSession.getEquipmentId());
        }
        if (clientSession.getDetails().getPriorSessionId() == null) {
            if (!Objects.equal(clientSession.getDetails().getSessionId(), Long.toUnsignedString(apEventClientSession.getSessionId())))
                clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        if (!Objects.equal(clientSession.getDetails().getSessionId(), Long.toUnsignedString(apEventClientSession.getSessionId()))) {
            clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        clientSession.getDetails().setSessionId(Long.toUnsignedString(apEventClientSession.getSessionId()));
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
        realtimeEventPublisher.publishClientAssocEvent(customerId, equipmentId, locationId, apEventClientSession.getClientAssocEvent());

    }
    
    private void processClientIpEvent(int customerId, long equipmentId, long locationId, sts.OpensyncStats.EventReport.ClientSession apEventClientSession) {
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
            clientSession.getDetails().setDhcpDetails(new ClientDhcpDetails(Long.toUnsignedString(apEventClientSession.getSessionId())));
        }
        if (clientSession.getDetails().getPriorEquipmentId() == null) {
            clientSession.getDetails().setPriorEquipmentId(clientSession.getEquipmentId());
        }
        if (clientSession.getDetails().getPriorSessionId() == null) {
            if (!Objects.equal(clientSession.getDetails().getSessionId(), Long.toUnsignedString(apEventClientSession.getSessionId())))
                clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        if (!Objects.equal(clientSession.getDetails().getSessionId(), Long.toUnsignedString(apEventClientSession.getSessionId()))) {
            clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        clientSession.getDetails().setSessionId(Long.toUnsignedString(apEventClientSession.getSessionId()));
        if (apClientEvent.hasIpAddr()) {
            ByteString ipAddress = apClientEvent.getIpAddr();
            if (ipAddress != null) {
                try {
                    InetAddress inetAddress = InetAddress.getByAddress(ipAddress.toByteArray());
                    if (inetAddress instanceof Inet4Address) {
                        clientSession.getDetails().setIpAddress(inetAddress);
                    } else if (inetAddress instanceof Inet6Address) {
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
        realtimeEventPublisher.publishClientIpEvent(customerId, equipmentId, locationId, apEventClientSession.getClientIpEvent());
    }
    
    private void processClientDisconnectEvent(int customerId, long equipmentId, long locationId,
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
            clientSession.getDetails().setDhcpDetails(new ClientDhcpDetails(Long.toUnsignedString(apEventClientSession.getSessionId())));
        }

        if (clientSession.getDetails().getPriorSessionId() == null) {
            if (!Objects.equal(clientSession.getDetails().getSessionId(), Long.toUnsignedString(apEventClientSession.getSessionId())))
                clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        if (!Objects.equal(clientSession.getDetails().getSessionId(), Long.toUnsignedString(apEventClientSession.getSessionId()))) {
            clientSession.getDetails().setPriorSessionId(clientSession.getDetails().getSessionId());
        }
        clientSession.getDetails().setSessionId(Long.toUnsignedString(apEventClientSession.getSessionId()));
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
        realtimeEventPublisher.publishClientDisconnectEvent(customerId, equipmentId, locationId, apEventClientSession.getClientDisconnectEvent());

    }

}
