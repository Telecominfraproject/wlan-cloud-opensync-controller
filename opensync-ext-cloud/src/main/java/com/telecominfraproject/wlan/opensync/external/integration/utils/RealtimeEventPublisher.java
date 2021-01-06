package com.telecominfraproject.wlan.opensync.external.integration.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.client.models.events.realtime.ClientConnectSuccessEvent;
import com.telecominfraproject.wlan.client.models.events.realtime.ClientDisconnectEvent.DisconnectFrameType;
import com.telecominfraproject.wlan.client.models.events.realtime.ClientDisconnectEvent.DisconnectInitiator;
import com.telecominfraproject.wlan.client.models.events.realtime.ClientTimeoutEvent.ClientTimeoutReason;
import com.telecominfraproject.wlan.cloudeventdispatcher.CloudEventDispatcherInterface;
import com.telecominfraproject.wlan.core.model.equipment.ChannelHopReason;
import com.telecominfraproject.wlan.core.model.equipment.MacAddress;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.equipment.EquipmentServiceInterface;
import com.telecominfraproject.wlan.equipment.models.ApElementConfiguration;
import com.telecominfraproject.wlan.equipment.models.Equipment;
import com.telecominfraproject.wlan.opensync.util.OvsdbToWlanCloudTypeMappingUtility;
import com.telecominfraproject.wlan.systemevent.equipment.realtime.RealTimeChannelHopEvent;
import com.telecominfraproject.wlan.systemevent.equipment.realtime.RealTimeEventType;
import com.telecominfraproject.wlan.systemevent.models.SystemEvent;

import sts.OpensyncStats.AssocType;
import sts.OpensyncStats.CTReasonType;
import sts.OpensyncStats.ChannelSwitchReason;
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
import sts.OpensyncStats.EventReport.DhcpAckEvent;
import sts.OpensyncStats.EventReport.DhcpDeclineEvent;
import sts.OpensyncStats.EventReport.DhcpDiscoverEvent;
import sts.OpensyncStats.EventReport.DhcpInformEvent;
import sts.OpensyncStats.EventReport.DhcpNakEvent;
import sts.OpensyncStats.EventReport.DhcpOfferEvent;
import sts.OpensyncStats.EventReport.DhcpRequestEvent;
import sts.OpensyncStats.EventReport.DhcpTransaction;
import sts.OpensyncStats.FrameType;

@org.springframework.context.annotation.Profile("opensync_cloud_config")
@Component
public class RealtimeEventPublisher {

    @Autowired
    private CloudEventDispatcherInterface cloudEventDispatcherInterface;

    @Autowired
    private EquipmentServiceInterface equipmentServiceInterface;

    private static final Logger LOG = LoggerFactory.getLogger(RealtimeEventPublisher.class);

    void publishChannelHopEvents(int customerId, long equipmentId, EventReport e) {

        LOG.info("publishChannelHopEvents for customerId {} equipmentId {}");
        List<SystemEvent> events = new ArrayList<>();

        for (sts.OpensyncStats.EventReport.ChannelSwitchEvent channelSwitchEvent : e.getChannelSwitchList()) {

            LOG.info("Received ClientEvent {} for customerId {} equipmentId {}", e, customerId, equipmentId);

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
                LOG.warn(
                        "publishChannelHopEvents:RadioType {} is unsupported, cannot send RealTimeChannelHopEvent for {}",
                        radioType, channelSwitchEvent);
                continue;
            }
            if (channelSwitchEvent.hasTimestampMs()) {
                timestamp = channelSwitchEvent.getTimestampMs();
            }
            if (timestamp == null) {
                LOG.warn("publishChannelHopEvents:timestamp is null, cannot send RealTimeChannelHopEvent for {}",
                        channelSwitchEvent);
                continue;
            }

            if (channelSwitchEvent.hasReason()) {
                if (channelSwitchEvent.getReason().equals(ChannelSwitchReason.high_interference))
                    reason = ChannelHopReason.HighInterference;
                else if (channelSwitchEvent.getReason().equals(ChannelSwitchReason.radar_detected))
                    reason = ChannelHopReason.RadarDetected;
            }
            if (ChannelHopReason.isUnsupported(reason)) {
                LOG.warn("publishChannelHopEvents:reason {} is unsupported, cannot send RealTimeChannelHopEvent for {}",
                        channelSwitchEvent.getReason(), channelSwitchEvent);
                continue;
            }
            if (channelSwitchEvent.hasChannel()) {
                channel = channelSwitchEvent.getChannel();
            }
            if (channel == null) {
                LOG.warn("publishChannelHopEvents:channel is null, cannot send RealTimeChannelHopEvent for {}",
                        channelSwitchEvent);
                continue;
            }

            RealTimeChannelHopEvent channelHopEvent = new RealTimeChannelHopEvent(RealTimeEventType.Channel_Hop,
                    customerId, equipmentId, radioType, channel,
                    ((ApElementConfiguration) equipment.getDetails()).getRadioMap().get(radioType).getChannelNumber(),
                    reason, timestamp);

            events.add(channelHopEvent);
            LOG.debug("publishChannelHopEvents:Adding ChannelHopEvent to bulk list {}", channelHopEvent);
        }

        if (events.size() > 0) {
            LOG.info("publishChannelHopEvents:publishEventsBulk: {}", events);
            cloudEventDispatcherInterface.publishEventsBulk(events);
        }
    }

    void publishClientConnectSuccessEvent(int customerId, long equipmentId, ClientConnectEvent clientConnectEvent) {

        LOG.info("Received ClientEvent {} for customerId {} equipmentId {}", clientConnectEvent, customerId,
                equipmentId);
        ClientConnectSuccessEvent clientEvent;

        if (clientConnectEvent.hasTimestampMs()) {
            clientEvent = new ClientConnectSuccessEvent(Long.valueOf(clientConnectEvent.getTimestampMs()));
        } else {
            clientEvent = new ClientConnectSuccessEvent(System.currentTimeMillis());
        }

        clientEvent.setMacAddress(MacAddress.valueOf(clientConnectEvent.getStaMac()));
        clientEvent.setRadioType(OvsdbToWlanCloudTypeMappingUtility
                .getRadioTypeFromOpensyncStatsRadioBandType(clientConnectEvent.getBand()));
        clientEvent.setSsid(clientConnectEvent.getSsid());
        clientEvent.setSessionId(clientConnectEvent.getSessionId());

        if (clientConnectEvent.hasFbtUsed()) {
            clientEvent.setFbtUsed(clientConnectEvent.getFbtUsed());
        }
        if (clientConnectEvent.hasEvTimeBootupInUsAssoc()) {
            clientEvent.setAssocTs(clientConnectEvent.getEvTimeBootupInUsAssoc());
        }

        if (clientConnectEvent.hasEvTimeBootupInUsAuth()) {
            clientEvent.setAuthTs(clientConnectEvent.getEvTimeBootupInUsAuth());
        }

        if (clientConnectEvent.hasEvTimeBootupInUsEapol()) {
            clientEvent.setEapolTs(clientConnectEvent.getEvTimeBootupInUsEapol());
        }

        if (clientConnectEvent.hasEvTimeBootupInUsFirstRx()) {
            clientEvent.setFirstDataRxTs(clientConnectEvent.getEvTimeBootupInUsFirstRx());
        }

        if (clientConnectEvent.hasEvTimeBootupInUsFirstTx()) {
            clientEvent.setFirstDataTxTs(clientConnectEvent.getEvTimeBootupInUsFirstTx());
        }

        if (clientConnectEvent.hasEvTimeBootupInUsIp()) {
            clientEvent.setIpAcquisitionTs(clientConnectEvent.getEvTimeBootupInUsIp());
        }

        if (clientConnectEvent.hasEvTimeBootupInUsPortEnable()) {
            clientEvent.setPortEnabledTs(clientConnectEvent.getEvTimeBootupInUsPortEnable());
        }

        if (clientConnectEvent.hasCltId()) {
            clientEvent.setHostName(clientConnectEvent.getCltId());
        }

        if (clientConnectEvent.hasSecType()) {
            clientEvent.setSecurityType(OvsdbToWlanCloudTypeMappingUtility
                    .getCloudSecurityTypeFromOpensyncStats(clientConnectEvent.getSecType()));
        }

        if (clientConnectEvent.hasAssocType()) {
            clientEvent.setReassociation(clientConnectEvent.getAssocType().equals(AssocType.REASSOC));
        }

        if (clientConnectEvent.hasAssocRssi()) {
            clientEvent.setAssocRSSI(clientConnectEvent.getAssocRssi());
        }

        if (clientConnectEvent.hasUsing11K()) {
            clientEvent.setUsing11k(clientConnectEvent.getUsing11K());
        }

        if (clientConnectEvent.hasUsing11R()) {
            clientEvent.setUsing11r(clientConnectEvent.getUsing11R());
        }

        if (clientConnectEvent.hasUsing11V()) {
            clientEvent.setUsing11v(clientConnectEvent.getUsing11V());
        }

        if (clientConnectEvent.hasIpAddr()) {
            try {
                clientEvent.setIpAddr(InetAddress.getByAddress(clientConnectEvent.getIpAddr().toByteArray()));

            } catch (UnknownHostException e1) {
                LOG.error("Invalid Ip Address for client {}", clientConnectEvent.getIpAddr(), e1);
            }
        }

        clientEvent.setCustomerId(customerId);
        clientEvent.setEquipmentId(equipmentId);

        LOG.info("publishing client event {} to cloud", clientEvent);
        cloudEventDispatcherInterface.publishEvent(clientEvent);

    }

    void publishClientDisconnectEvent(int customerId, long equipmentId, ClientDisconnectEvent clientDisconnectEvent) {

        LOG.info("Received ClientEvent {} for customerId {} equipmentId {}", clientDisconnectEvent, customerId,
                equipmentId);

        com.telecominfraproject.wlan.client.models.events.realtime.ClientDisconnectEvent clientEvent;

        if (clientDisconnectEvent.hasTimestampMs()) {
            clientEvent = new com.telecominfraproject.wlan.client.models.events.realtime.ClientDisconnectEvent(
                    Long.valueOf(clientDisconnectEvent.getTimestampMs()));
        } else {
            clientEvent = new com.telecominfraproject.wlan.client.models.events.realtime.ClientDisconnectEvent(
                    System.currentTimeMillis());

        }

        clientEvent.setDeviceMacAddress(MacAddress.valueOf(clientDisconnectEvent.getStaMac()));
        clientEvent.setSessionId(clientDisconnectEvent.getSessionId());
        clientEvent.setRadioType(OvsdbToWlanCloudTypeMappingUtility
                .getRadioTypeFromOpensyncStatsRadioBandType(clientDisconnectEvent.getBand()));
        clientEvent.setSsid(clientDisconnectEvent.getSsid());

        if (clientDisconnectEvent.hasDevType()) {

            clientEvent.setInitiator(
                    clientDisconnectEvent.getDevType().equals(DeviceType.DEV_AP) ? DisconnectInitiator.AccessPoint
                            : DisconnectInitiator.Client);

        }
        if (clientDisconnectEvent.hasFrType()) {
            clientEvent.setFrameType(
                    clientDisconnectEvent.getFrType().equals(FrameType.FT_DEAUTH) ? DisconnectFrameType.Deauth
                            : DisconnectFrameType.Disassoc);
        }
        if (clientDisconnectEvent.hasInternalRc()) {
            clientEvent.setInternalReasonCode(clientDisconnectEvent.getInternalRc());
        }
        if (clientDisconnectEvent.hasLrcvUpTsInUs()) {
            clientEvent.setLastRecvTime(clientDisconnectEvent.getLrcvUpTsInUs());
        }
        if (clientDisconnectEvent.hasLsentUpTsInUs()) {
            clientEvent.setLastSentTime(clientDisconnectEvent.getLsentUpTsInUs());
        }
        if (clientDisconnectEvent.hasReason()) {
            clientEvent.setReasonCode(clientDisconnectEvent.getReason());
        }
        if (clientDisconnectEvent.hasRssi()) {
            clientEvent.setRssi(clientDisconnectEvent.getRssi());
        }

        clientEvent.setCustomerId(customerId);
        clientEvent.setEquipmentId(equipmentId);

        LOG.info("publishing client event {} to cloud", clientEvent);
        cloudEventDispatcherInterface.publishEvent(clientEvent);

    }

    void publishClientAuthSystemEvent(int customerId, long equipmentId, ClientAuthEvent clientAuthEvent) {
        LOG.info("Received ClientEvent {} for customerId {} equipmentId {}", clientAuthEvent, customerId, equipmentId);

        com.telecominfraproject.wlan.client.models.events.realtime.ClientAuthEvent clientEvent;

        if (clientAuthEvent.hasTimestampMs()) {
            clientEvent = new com.telecominfraproject.wlan.client.models.events.realtime.ClientAuthEvent(Long.valueOf(clientAuthEvent.getTimestampMs()));
        } else {
            clientEvent = new com.telecominfraproject.wlan.client.models.events.realtime.ClientAuthEvent(
                    System.currentTimeMillis());
        }

        clientEvent.setSessionId(clientAuthEvent.getSessionId());
        clientEvent.setSsid(clientAuthEvent.getSsid());
        clientEvent.setDeviceMacAddress(MacAddress.valueOf(clientAuthEvent.getStaMac()));
        clientEvent.setRadioType(OvsdbToWlanCloudTypeMappingUtility
                .getRadioTypeFromOpensyncStatsRadioBandType(clientAuthEvent.getBand()));

        if (clientAuthEvent.hasAuthStatus()) {
            clientEvent.setAuthStatus(clientAuthEvent.getAuthStatus());
        }
        clientEvent.setCustomerId(customerId);
        clientEvent.setEquipmentId(equipmentId);

        LOG.info("publishing client event {} to cloud", clientEvent);
        cloudEventDispatcherInterface.publishEvent(clientEvent);

    }

    void publishClientAssocEvent(int customerId, long equipmentId, ClientAssocEvent clientAssocEvent) {
        LOG.info("Received ClientEvent {} for customerId {} equipmentId {}", clientAssocEvent, customerId, equipmentId);

        com.telecominfraproject.wlan.client.models.events.realtime.ClientAssocEvent clientEvent;

        if (clientAssocEvent.hasTimestampMs()) {                   
            clientEvent = new com.telecominfraproject.wlan.client.models.events.realtime.ClientAssocEvent(Long.valueOf(clientAssocEvent.getTimestampMs()));
        } else {
            clientEvent = new com.telecominfraproject.wlan.client.models.events.realtime.ClientAssocEvent(
                    System.currentTimeMillis());
        }

        clientEvent.setSessionId(clientAssocEvent.getSessionId());
        clientEvent.setSsid(clientAssocEvent.getSsid());
        clientEvent.setDeviceMacAddress(MacAddress.valueOf(clientAssocEvent.getStaMac()));
        clientEvent.setRadioType(OvsdbToWlanCloudTypeMappingUtility
                .getRadioTypeFromOpensyncStatsRadioBandType(clientAssocEvent.getBand()));

        if (clientAssocEvent.hasAssocType()) {
            clientEvent.setReassociation(clientAssocEvent.getAssocType().equals(AssocType.REASSOC));
        }

        if (clientAssocEvent.hasInternalSc()) {
            clientEvent.setInternalSC(clientAssocEvent.getInternalSc());
        }

        if (clientAssocEvent.hasRssi()) {
            clientEvent.setRssi(clientAssocEvent.getRssi());
        }

        if (clientAssocEvent.hasStatus()) {
            clientEvent.setStatus(clientAssocEvent.getStatus());
        }

        if (clientAssocEvent.hasUsing11K()) {
            clientEvent.setUsing11k(clientAssocEvent.getUsing11K());
        }

        if (clientAssocEvent.hasUsing11R()) {
            clientEvent.setUsing11r(clientAssocEvent.getUsing11R());
        }

        if (clientAssocEvent.hasUsing11V()) {
            clientEvent.setUsing11v(clientAssocEvent.getUsing11V());
        }

        clientEvent.setCustomerId(customerId);
        clientEvent.setEquipmentId(equipmentId);

        LOG.info("publishing client event {} to cloud", clientEvent);
        cloudEventDispatcherInterface.publishEvent(clientEvent);

    }

    void publishClientFailureEvent(int customerId, long equipmentId, ClientFailureEvent clientFailureEvent) {
        LOG.info("Received ClientEvent {} for customerId {} equipmentId {}", clientFailureEvent, customerId,
                equipmentId);

        com.telecominfraproject.wlan.client.models.events.realtime.ClientFailureEvent clientEvent;

        if (clientFailureEvent.hasTimestampMs()) {
            clientEvent = new com.telecominfraproject.wlan.client.models.events.realtime.ClientFailureEvent(Long.valueOf(clientFailureEvent.getTimestampMs()));
        } else {
            clientEvent = new com.telecominfraproject.wlan.client.models.events.realtime.ClientFailureEvent(
                    System.currentTimeMillis());
        }

        clientEvent.setSessionId(clientFailureEvent.getSessionId());
        clientEvent.setDeviceMacAddress(MacAddress.valueOf(clientFailureEvent.getStaMac()));
        clientEvent.setSsid(clientFailureEvent.getSsid());

        if (clientFailureEvent.hasReasonStr()) {
            clientEvent.setReasonString(clientFailureEvent.getReasonStr());
        }

        if (clientFailureEvent.hasReasonCode()) {
            clientEvent.setReasonCode(clientFailureEvent.getReasonCode());
        }

        clientEvent.setCustomerId(customerId);
        clientEvent.setEquipmentId(equipmentId);

        LOG.info("publishing client event {} to cloud", clientEvent);
        cloudEventDispatcherInterface.publishEvent(clientEvent);
    }

    void publishClientFirstDataEvent(int customerId, long equipmentId, ClientFirstDataEvent clientFirstDataEvent) {
        LOG.info("Received ClientEvent {} for customerId {} equipmentId {}", clientFirstDataEvent, customerId,
                equipmentId);

        com.telecominfraproject.wlan.client.models.events.realtime.ClientFirstDataEvent clientEvent;

        if (clientFirstDataEvent.hasTimestampMs()) {
            clientEvent = new com.telecominfraproject.wlan.client.models.events.realtime.ClientFirstDataEvent(
                    Long.valueOf(clientFirstDataEvent.getTimestampMs()));
        } else {
            clientEvent = new com.telecominfraproject.wlan.client.models.events.realtime.ClientFirstDataEvent(
                    System.currentTimeMillis());
        }

        clientEvent.setSessionId(clientFirstDataEvent.getSessionId());
        clientEvent.setDeviceMacAddress(MacAddress.valueOf(clientFirstDataEvent.getStaMac()));

        if (clientFirstDataEvent.hasFdataTxUpTsInUs()) {
            clientEvent.setFirstDataSentTs(clientFirstDataEvent.getFdataTxUpTsInUs());
        }

        if (clientFirstDataEvent.hasFdataRxUpTsInUs()) {
            clientEvent.setFirstDataRcvdTs(clientFirstDataEvent.getFdataRxUpTsInUs());
        }

        clientEvent.setCustomerId(customerId);
        clientEvent.setEquipmentId(equipmentId);

        LOG.info("publishing client event {} to cloud", clientEvent);
        cloudEventDispatcherInterface.publishEvent(clientEvent);

    }

    void publishClientIdEvent(int customerId, long equipmentId, ClientIdEvent clientIdEvent) {

        LOG.info("Received ClientEvent {} for customerId {} equipmentId {}", clientIdEvent, customerId, equipmentId);

        com.telecominfraproject.wlan.client.models.events.realtime.ClientIdEvent clientEvent;

        if (clientIdEvent.hasTimestampMs()) {
            clientEvent = new com.telecominfraproject.wlan.client.models.events.realtime.ClientIdEvent(Long.valueOf(clientIdEvent.getTimestampMs()));
        } else {
            clientEvent = new com.telecominfraproject.wlan.client.models.events.realtime.ClientIdEvent(
                    System.currentTimeMillis());
        }

        clientEvent.setSessionId(clientIdEvent.getSessionId());
        clientEvent.setDeviceMacAddress(MacAddress.valueOf(clientIdEvent.getCltMac()));
        if (clientIdEvent.hasCltId()) {
            clientEvent.setUserId(clientIdEvent.getCltId());
        }

        clientEvent.setCustomerId(customerId);
        clientEvent.setEquipmentId(equipmentId);

        LOG.info("publishing client event {} to cloud", clientEvent);
        cloudEventDispatcherInterface.publishEvent(clientEvent);
    }

    void publishClientIpEvent(int customerId, long equipmentId, ClientIpEvent clientIpEvent) {

        LOG.info("Received ClientEvent {} for customerId {} equipmentId {}", clientIpEvent, customerId, equipmentId);

        com.telecominfraproject.wlan.client.models.events.realtime.ClientIpAddressEvent clientEvent;

        if (clientIpEvent.hasTimestampMs()) {
            clientEvent = new com.telecominfraproject.wlan.client.models.events.realtime.ClientIpAddressEvent(
                    Long.valueOf(clientIpEvent.getTimestampMs()));
        } else {
            clientEvent = new com.telecominfraproject.wlan.client.models.events.realtime.ClientIpAddressEvent(
                    System.currentTimeMillis());
        }

        clientEvent.setSessionId(clientIpEvent.getSessionId());
        clientEvent.setDeviceMacAddress(MacAddress.valueOf(clientIpEvent.getStaMac()));
        if (clientIpEvent.hasIpAddr()) {
            clientEvent.setIpAddr(clientIpEvent.getIpAddr().toByteArray());
        }

        clientEvent.setCustomerId(customerId);
        clientEvent.setEquipmentId(equipmentId);

        LOG.info("publishing client event {} to cloud", clientEvent);
        cloudEventDispatcherInterface.publishEvent(clientEvent);
    }

    void publishClientTimeoutEvent(int customerId, long equipmentId, ClientTimeoutEvent clientTimeoutEvent) {

        LOG.info("Received ClientEvent {} for customerId {} equipmentId {}", clientTimeoutEvent, customerId,
                equipmentId);

        com.telecominfraproject.wlan.client.models.events.realtime.ClientTimeoutEvent clientEvent;

        if (clientTimeoutEvent.hasTimestampMs()) {
            clientEvent = new com.telecominfraproject.wlan.client.models.events.realtime.ClientTimeoutEvent(Long.valueOf(clientTimeoutEvent.getTimestampMs()));
        } else {
            clientEvent = new com.telecominfraproject.wlan.client.models.events.realtime.ClientTimeoutEvent(
                    System.currentTimeMillis());
        }

        clientEvent.setSessionId(clientTimeoutEvent.getSessionId());
        clientEvent.setDeviceMacAddress(MacAddress.valueOf(clientTimeoutEvent.getStaMac()));
        if (clientTimeoutEvent.hasRCode()) {
            clientEvent.setTimeoutReason(clientTimeoutEvent.getRCode().equals(CTReasonType.CTR_IDLE_TOO_LONG)
                    ? ClientTimeoutReason.IdleTooLong
                    : ClientTimeoutReason.FailedProbe);
        }
        if (clientTimeoutEvent.hasLastRcvUpTsInUs()) {
            clientEvent.setLastRecvTime(clientTimeoutEvent.getLastRcvUpTsInUs());
        }
        if (clientTimeoutEvent.hasLastSentUpTsInUs()) {
            clientEvent.setLastSentTime(clientTimeoutEvent.getLastSentUpTsInUs());
        }

        clientEvent.setCustomerId(customerId);
        clientEvent.setEquipmentId(equipmentId);

        LOG.info("publishing client event {} to cloud", clientEvent);
        cloudEventDispatcherInterface.publishEvent(clientEvent);

    }

    void publishDhcpTransactionEvents(int customerId, long equipmentId,
            List<DhcpTransaction> dhcpTransactionList) {
        for (DhcpTransaction dhcpTransaction : dhcpTransactionList) {

            for (DhcpAckEvent ackEvent : dhcpTransaction.getDhcpAckEventList()) {
                LOG.info("DhcpAckEvent {}", ackEvent);

            }

            for (DhcpNakEvent nakEvent : dhcpTransaction.getDhcpNakEventList()) {
                LOG.info("DhcpNakEvent {}", nakEvent);

            }

            for (DhcpOfferEvent offerEvent : dhcpTransaction.getDhcpOfferEventList()) {
                LOG.info("DhcpOfferEvent {}", offerEvent);

            }

            for (DhcpInformEvent informEvent : dhcpTransaction.getDhcpInformEventList()) {
                LOG.info("DhcpInformEvent {}", informEvent);

            }

            for (DhcpDeclineEvent declineEvent : dhcpTransaction.getDhcpDeclineEventList()) {
                LOG.info("DhcpDeclineEvent {}", declineEvent);

            }

            for (DhcpRequestEvent requestEvent : dhcpTransaction.getDhcpRequestEventList()) {
                LOG.info("DhcpRequestEvent {}", requestEvent);

            }

            for (DhcpDiscoverEvent discoverEvent : dhcpTransaction.getDhcpDiscoverEventList()) {
                LOG.info("DhcpDiscoverEvent {}", discoverEvent);

            }
        }
    }
}
