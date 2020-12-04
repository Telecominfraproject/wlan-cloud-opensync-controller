package com.telecominfraproject.wlan.opensync.external.integration;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import com.telecominfraproject.wlan.systemevent.models.SystemEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.telecominfraproject.wlan.alarm.AlarmServiceInterface;
import com.telecominfraproject.wlan.client.ClientServiceInterface;
import com.telecominfraproject.wlan.client.info.models.ClientInfoDetails;
import com.telecominfraproject.wlan.client.session.models.ClientSession;
import com.telecominfraproject.wlan.client.session.models.ClientSessionDetails;
import com.telecominfraproject.wlan.cloudeventdispatcher.CloudEventDispatcherInterface;
import com.telecominfraproject.wlan.core.model.equipment.MacAddress;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.customer.service.CustomerServiceInterface;
import com.telecominfraproject.wlan.equipment.EquipmentServiceInterface;
import com.telecominfraproject.wlan.equipment.models.ApElementConfiguration;
import com.telecominfraproject.wlan.equipment.models.Equipment;
import com.telecominfraproject.wlan.firmware.FirmwareServiceInterface;
import com.telecominfraproject.wlan.location.service.LocationServiceInterface;
import com.telecominfraproject.wlan.opensync.external.integration.controller.OpensyncCloudGatewayController;
import com.telecominfraproject.wlan.profile.ProfileServiceInterface;
import com.telecominfraproject.wlan.routing.RoutingServiceInterface;
import com.telecominfraproject.wlan.servicemetric.apnode.models.ApNodeMetrics;
import com.telecominfraproject.wlan.servicemetric.apnode.models.StateUpDownError;
import com.telecominfraproject.wlan.status.StatusServiceInterface;
import com.telecominfraproject.wlan.status.equipment.report.models.ActiveBSSID;
import com.telecominfraproject.wlan.status.equipment.report.models.ActiveBSSIDs;
import com.telecominfraproject.wlan.status.models.Status;
import com.telecominfraproject.wlan.status.models.StatusDataType;

import sts.OpensyncStats;
import sts.OpensyncStats.AssocType;
import sts.OpensyncStats.ChannelSwitchReason;
import sts.OpensyncStats.Client;
import sts.OpensyncStats.ClientReport;
import sts.OpensyncStats.DNSProbeMetric;
import sts.OpensyncStats.EventReport;
import sts.OpensyncStats.EventReport.ChannelSwitchEvent;
import sts.OpensyncStats.EventReport.ClientAssocEvent;
import sts.OpensyncStats.NetworkProbe;
import sts.OpensyncStats.RADIUSMetrics;
import sts.OpensyncStats.RadioBandType;
import sts.OpensyncStats.Report;
import sts.OpensyncStats.StateUpDown;
import sts.OpensyncStats.VLANMetrics;

@RunWith(SpringRunner.class)
@ActiveProfiles(profiles = { "integration_test", })
@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = OpensyncExternalIntegrationMqttMessageProcessorTest.class)
@Import(value = { AlarmServiceInterface.class, OpensyncExternalIntegrationCloud.class,
        OpensyncExternalIntegrationMqttMessageProcessorTest.Config.class,

})
public class OpensyncExternalIntegrationMqttMessageProcessorTest {

    @MockBean
    AlarmServiceInterface alarmServiceInterface;
    @MockBean
    CustomerServiceInterface customerServiceInterface;
    @MockBean
    LocationServiceInterface locationServiceInterface;
    @MockBean(answer = Answers.RETURNS_MOCKS)
    OvsdbSessionMapInterface ovsdbSessionMapInterface;
    @MockBean
    CloudEventDispatcherInterface equipmentMetricsCollectorInterface;
    @MockBean
    EquipmentServiceInterface equipmentServiceInterface;
    @MockBean
    RoutingServiceInterface routingServiceInterface;
    @MockBean
    ProfileServiceInterface profileServiceInterface;
    @MockBean
    StatusServiceInterface statusServiceInterface;
    @MockBean
    ClientServiceInterface clientServiceInterface;
    @MockBean
    FirmwareServiceInterface firmwareServiceInterface;
    @MockBean(answer = Answers.RETURNS_MOCKS)
    OpensyncCloudGatewayController gatewayController;

    @Autowired
    OpensyncExternalIntegrationMqttMessageProcessor opensyncExternalIntegrationMqttProcessor;

    MockitoSession mockito;

    @Configuration
    static class Config {

        @Bean
        public OpensyncExternalIntegrationMqttMessageProcessor opensyncExternalIntegrationMqttMessageProcessor() {
            return new OpensyncExternalIntegrationMqttMessageProcessor();
        }

    }

    @Before
    public void setUp() throws Exception {

        mockito = Mockito.mockitoSession().initMocks(this).strictness(Strictness.STRICT_STUBS).startMocking();

    }

    @After
    public void tearDown() throws Exception {
        mockito.finishMocking();

    }


    @Test
    public void testExtractApIdFromTopic() {

        String topic = "/ap/Test_Client_21P10C68818122/opensync";

        assertEquals("Test_Client_21P10C68818122",
                OpensyncExternalIntegrationMqttMessageProcessor.extractApIdFromTopic(topic));

    }

    @Test
    public void testExtractCustomerIdFromTopic() {
        String topic = "/ap/Test_Client_21P10C68818122/opensync";
        OvsdbSession session = Mockito.mock(OvsdbSession.class);
        Mockito.when(session.getCustomerId()).thenReturn(2);

        Mockito.when(ovsdbSessionMapInterface.getSession("Test_Client_21P10C68818122")).thenReturn(session);

        assertEquals(2, opensyncExternalIntegrationMqttProcessor.extractCustomerIdFromTopic(topic));
    }

    @Test
    public void testExtractEquipmentIdFromTopic() {

        String topic = "/ap/Test_Client_21P10C68818122/opensync";
        OvsdbSession session = Mockito.mock(OvsdbSession.class);
        Mockito.when(session.getEquipmentId()).thenReturn(1L);

        Mockito.when(ovsdbSessionMapInterface.getSession("Test_Client_21P10C68818122")).thenReturn(session);

        assertEquals(1L, opensyncExternalIntegrationMqttProcessor.extractEquipmentIdFromTopic(topic));

    }

    @Test
    public void testProcessMqttMessageStringReport() {
        Equipment equipment = new Equipment();

        equipment.setDetails(ApElementConfiguration.createWithDefaults());
        
        equipment.setId(1L);

        Mockito.when(
                equipmentServiceInterface.getByInventoryIdOrNull(ArgumentMatchers.eq("Test_Client_21P10C68818122")))
                .thenReturn(equipment);
        
        equipment.setProfileId(0L);
        
        Mockito.when(equipmentServiceInterface.getOrNull(1L)).thenReturn(equipment);
        Mockito.when(equipmentServiceInterface.get(1L)).thenReturn(equipment);

        Report report = Report.newBuilder().setNodeID("21P10C68818122")
                .addAllClients(getOpensyncStatsClientReportsList())
                .addAllEventReport(getOpensyncStatsEventReportsList()).build();

        String topic = "/ap/Test_Client_21P10C68818122/opensync";

        OvsdbSession session = Mockito.mock(OvsdbSession.class);
        Mockito.when(session.getEquipmentId()).thenReturn(1L);
        Mockito.when(session.getCustomerId()).thenReturn(2);

        Mockito.when(ovsdbSessionMapInterface.getSession("Test_Client_21P10C68818122")).thenReturn(session);

        Status bssidStatus = new Status();
        bssidStatus.setStatusDataType(StatusDataType.ACTIVE_BSSIDS);
        bssidStatus.setCustomerId(2);

        ActiveBSSIDs activeBssidsDetails = new ActiveBSSIDs();
        activeBssidsDetails.setActiveBSSIDs(getActiveBssidList());
        bssidStatus.setDetails(activeBssidsDetails);

        Mockito.when(statusServiceInterface.getOrNull(ArgumentMatchers.anyInt(), ArgumentMatchers.anyLong(),
                ArgumentMatchers.eq(StatusDataType.ACTIVE_BSSIDS))).thenReturn(bssidStatus);

        Mockito.when(statusServiceInterface.update(ArgumentMatchers.any(Status.class))).thenReturn(bssidStatus);
        com.telecominfraproject.wlan.client.models.Client clientInstance = new com.telecominfraproject.wlan.client.models.Client();
        clientInstance.setMacAddress(new MacAddress("7C:AB:60:E6:EA:4D"));
        clientInstance.setDetails(new ClientInfoDetails());
        com.telecominfraproject.wlan.client.models.Client clientInstance2 = new com.telecominfraproject.wlan.client.models.Client();
        clientInstance2.setMacAddress(new MacAddress("C0:9A:D0:76:A9:69"));
        clientInstance2.setDetails(new ClientInfoDetails());
        Mockito.when(
                clientServiceInterface.getOrNull(ArgumentMatchers.anyInt(), ArgumentMatchers.any(MacAddress.class)))
                .thenReturn(clientInstance).thenReturn(clientInstance2);
        Mockito.when(clientServiceInterface
                .update(ArgumentMatchers.any(com.telecominfraproject.wlan.client.models.Client.class)))
                .thenReturn(clientInstance).thenReturn(clientInstance2);

        ClientSession clientSession = new ClientSession();
        clientSession.setMacAddress(new MacAddress("7C:AB:60:E6:EA:4D"));
        clientSession.setDetails(new ClientSessionDetails());
        ClientSession clientSession2 = new ClientSession();
        clientSession2.setMacAddress(new MacAddress("C0:9A:D0:76:A9:69"));
        clientSession2.setDetails(new ClientSessionDetails());
        Mockito.when(clientServiceInterface.getSessionOrNull(ArgumentMatchers.anyInt(), ArgumentMatchers.anyLong(),
                ArgumentMatchers.any(MacAddress.class))).thenReturn(clientSession).thenReturn(clientSession2);

        Mockito.when(clientServiceInterface.updateSession(ArgumentMatchers.any(ClientSession.class)))
                .thenReturn(clientSession).thenReturn(clientSession2);

        opensyncExternalIntegrationMqttProcessor.processMqttMessage(topic, report);

        // Mockito.verify(clientServiceInterface,
        // Mockito.times(4)).getOrNull(Mockito.anyInt(),
        // Mockito.any(MacAddress.class));
        // Mockito.verify(clientServiceInterface,
        // Mockito.times(4)).getSessionOrNull(Mockito.anyInt(),
        // Mockito.anyLong(),
        // Mockito.any(MacAddress.class));
        // Mockito.verify(statusServiceInterface,
        // Mockito.times(3)).getOrNull(Mockito.anyInt(), Mockito.anyLong(),
        // Mockito.eq(StatusDataType.ACTIVE_BSSIDS));

    }

    @Ignore
    public void testProcessMqttMessageStringFlowReport() {
        // TODO: implement me when support flow reports
    }

    @Ignore
    public void testProcessMqttMessageStringWCStatsReport() {
        // TODO: implement me when wcs stats reports supported
    }

    // Verify CallStart, CallStop and CallReport are properly processed and published as
    // respective events (5 events in total)
    @Test
    public void testProcessMqttMessageStringMultipleVideoVoiceReport() {
        OpensyncStats.VideoVoiceReport.Builder callStartVoiceReportBuilder = OpensyncStats.VideoVoiceReport.newBuilder().
                setCallStart(getDefaultCallStart());
        OpensyncStats.VideoVoiceReport.Builder callReportGotPublishVoiceReportBuilder = OpensyncStats.VideoVoiceReport.newBuilder().
                setCallReport(getDefaultCallReport(OpensyncStats.CallReport.CallReportReason.GOT_PUBLISH, 121, 1028, 1316, 1888, 298, 2, 100, 200));
        OpensyncStats.VideoVoiceReport.Builder callReportRoamedToVoiceReportBuilder = OpensyncStats.VideoVoiceReport.newBuilder().
                setCallReport(getDefaultCallReport(OpensyncStats.CallReport.CallReportReason.ROAMED_TO, 123, 1020, 1116, 1345, 223, 0, 102, 203));
        OpensyncStats.VideoVoiceReport.Builder callReportRoamedFromVoiceReportBuilder = OpensyncStats.VideoVoiceReport.newBuilder().
                setCallReport(getDefaultCallReport(OpensyncStats.CallReport.CallReportReason.ROAMED_FROM, 122, 1029, 1300, 1234, 111, 3, 101, 201));
        OpensyncStats.VideoVoiceReport.Builder callStopVoiceReportBuilder = OpensyncStats.VideoVoiceReport.newBuilder().
                setCallStop(getDefaultCallStop());
        // Create report with multiple voiceReports in one
        Report multipleVoiceReportsInOneReport = Report.newBuilder().addVideoVoiceReport(callStartVoiceReportBuilder).
                addVideoVoiceReport(callReportGotPublishVoiceReportBuilder).
                addVideoVoiceReport(callReportRoamedFromVoiceReportBuilder).
                addVideoVoiceReport(callReportRoamedToVoiceReportBuilder).
                addVideoVoiceReport(callStopVoiceReportBuilder).
                setNodeID("1").
                build();

        opensyncExternalIntegrationMqttProcessor.populateSipCallReport(null, multipleVoiceReportsInOneReport, 1, 2L, "TestAP", 12L);

        Mockito.verify(equipmentMetricsCollectorInterface, Mockito.times(1)).publishEventsBulk(Mockito.anyList());
    }

    // Create report with 1 voiceReports that contains multiple Calls
    // Note that GOT_PUBLISH and ROAMED_FROM are ignored in the CallReport. ROAMED_TO overwrites them
    @Test
    public void testProcessMqttMessageStringOneVideoVoiceReport() {

        OpensyncStats.VideoVoiceReport.Builder videoVoiceReportBuilder = OpensyncStats.VideoVoiceReport.newBuilder().
                setCallReport(getDefaultCallReport(OpensyncStats.CallReport.CallReportReason.GOT_PUBLISH, 121, 1028, 1316,1888, 298, 2, 100,200)).
                setCallReport(getDefaultCallReport(OpensyncStats.CallReport.CallReportReason.ROAMED_FROM, 122, 1029, 1300,1234, 111, 3, 101,201)).
                setCallReport(getDefaultCallReport(OpensyncStats.CallReport.CallReportReason.ROAMED_TO, 123, 1020, 1116,1345, 223, 0, 102,203)).
                setCallStart(getDefaultCallStart()).
                setCallStop(getDefaultCallStop());
        Report oneVoiceReportWithMultipleCallsInOneReport = Report.getDefaultInstance().toBuilder().
                addVideoVoiceReport(videoVoiceReportBuilder).
                setNodeID("1").
                build();

        opensyncExternalIntegrationMqttProcessor.populateSipCallReport(null, oneVoiceReportWithMultipleCallsInOneReport, 1, 2L, "TestAP", 12L);

        Mockito.verify(equipmentMetricsCollectorInterface, Mockito.times(1)).publishEventsBulk(Mockito.anyList());
    }

    @Test
    public void testpopulateNetworkProbeMetrics() throws Exception {

        InetAddress ip = InetAddress.getLocalHost();

        DNSProbeMetric dnsProbeMetric = DNSProbeMetric.getDefaultInstance().toBuilder().setLatency(10)
                .setState(StateUpDown.SUD_up).setServerIP(ip.getHostName()).build();
        RADIUSMetrics radiusProbeMetric = RADIUSMetrics.getDefaultInstance().toBuilder().setLatency(10)
                .setRadiusState(StateUpDown.SUD_up).setServerIP(ip.getHostName()).build();
        VLANMetrics vlanMetrics = VLANMetrics.getDefaultInstance().toBuilder().setLatency(10)
                .setDhcpState(StateUpDown.SUD_up).setVlanIF("vlan-1").build();
        NetworkProbe networkProbe = NetworkProbe.getDefaultInstance().toBuilder().setVlanProbe(vlanMetrics)
                .setDnsProbe(dnsProbeMetric).addRadiusProbe(radiusProbeMetric).build();

        Report report = Report.getDefaultInstance().toBuilder().setNodeID("21P10C68818122")
                .addNetworkProbe(networkProbe).build();

        ApNodeMetrics apNodeMetrics = new ApNodeMetrics();

        opensyncExternalIntegrationMqttProcessor.populateNetworkProbeMetrics(report, apNodeMetrics);

        assertNotNull(apNodeMetrics.getNetworkProbeMetrics());

        assert (apNodeMetrics.getNetworkProbeMetrics().get(0).getDnsLatencyMs() == 10);
        assert (apNodeMetrics.getNetworkProbeMetrics().get(0).getDhcpLatencyMs() == 10);
        assert (apNodeMetrics.getNetworkProbeMetrics().get(0).getRadiusState().equals(StateUpDownError.enabled));
        assert (apNodeMetrics.getNetworkProbeMetrics().get(0).getDhcpState().equals(StateUpDownError.enabled));
        assert (apNodeMetrics.getNetworkProbeMetrics().get(0).getDnsState().equals(StateUpDownError.enabled));
        assert (apNodeMetrics.getNetworkProbeMetrics().get(0).getRadiusLatencyInMs() == 10);
        assert (apNodeMetrics.getNetworkProbeMetrics().get(0).getVlanIF().equals("vlan-1"));

    }

    // Helper methods
    private List<ActiveBSSID> getActiveBssidList() {
        ActiveBSSID activeBssid = new ActiveBSSID();
        activeBssid.setBssid("24:f5:a2:ef:2e:54");
        activeBssid.setSsid("ssid-1");
        activeBssid.setNumDevicesConnected(1);
        activeBssid.setRadioType(RadioType.is2dot4GHz);
        ActiveBSSID activeBssid2 = new ActiveBSSID();
        activeBssid2.setBssid("24:f5:a2:ef:2e:55");
        activeBssid2.setSsid("ssid-2");
        activeBssid2.setNumDevicesConnected(1);
        activeBssid2.setRadioType(RadioType.is5GHzL);
        ActiveBSSID activeBssid3 = new ActiveBSSID();
        activeBssid3.setBssid("24:f5:a2:ef:2e:56");
        activeBssid3.setSsid("ssid-3");
        activeBssid3.setNumDevicesConnected(1);
        activeBssid3.setRadioType(RadioType.is5GHzU);

        List<ActiveBSSID> bssidList = new ArrayList<>();
        bssidList.add(activeBssid);
        bssidList.add(activeBssid2);
        bssidList.add(activeBssid3);
        return bssidList;
    }

    private OpensyncStats.CallStart getDefaultCallStart() {
        OpensyncStats.CallStart.Builder callStartBuilder = OpensyncStats.CallStart.newBuilder();
        callStartBuilder.setBand(RadioBandType.BAND5G);
        callStartBuilder.setChannel(40);
        callStartBuilder.addCodecs("110 opus/48000/2");
        callStartBuilder.addCodecs("102 iLBC/8000");
        callStartBuilder.setClientMac(ByteString.copyFrom("C0:9A:D0:76:A9:69".getBytes()));
        callStartBuilder.setDeviceInfo("Test Device");
        callStartBuilder.setProviderDomain("skype");
        callStartBuilder.setSessionId(123L);
        callStartBuilder.setWifiSessionId(1234L);

        return callStartBuilder.build();
    }

    private OpensyncStats.CallStop getDefaultCallStop() {
        OpensyncStats.CallStop.Builder callStopBuilder = OpensyncStats.CallStop.newBuilder();
        callStopBuilder.setBand(RadioBandType.BAND5G);
        callStopBuilder.setChannel(40);
        callStopBuilder.addCodecs("110 opus/48000/2");
        callStopBuilder.addCodecs("102 iLBC/8000");
        callStopBuilder.setClientMac(ByteString.copyFrom("C0:9A:D0:76:A9:69".getBytes()));
        callStopBuilder.setCallDuration(1230);
        callStopBuilder.setProviderDomain("skype");
        callStopBuilder.setSessionId(123L);
        callStopBuilder.setWifiSessionId(1234L);
        callStopBuilder.setReason(OpensyncStats.CallStop.CallStopReason.BYE_OK);
        callStopBuilder.addStats(getRtpFlowStats(121, 1380, 1400,3000, 119, 3, 205,350));

        return callStopBuilder.build();
    }
    
    private OpensyncStats.RtpFlowStats getRtpFlowStats(int codec, int jitter, int latency, int totalPackets,
                                                       int totalPacketsLost, int mos, int firstRtpSeq, int lastRtpSeq) {
        OpensyncStats.RtpFlowStats.Builder rtpFlowStatsBuilder = OpensyncStats.RtpFlowStats.newBuilder();
        rtpFlowStatsBuilder.setCodec(codec);
        rtpFlowStatsBuilder.setBlockCodecs(ByteString.copyFrom(new byte[] { (byte) 0xe6, 0x1 }));
        rtpFlowStatsBuilder.setDirection(OpensyncStats.RtpFlowStats.RtpFlowDirection.RTP_DOWNSTREAM);
        rtpFlowStatsBuilder.setRtpFlowType(OpensyncStats.RtpFlowStats.RtpFlowType.RTP_VOICE);
        rtpFlowStatsBuilder.setJitter(jitter);
        rtpFlowStatsBuilder.setLatency(latency);
        rtpFlowStatsBuilder.setTotalPacketsSent(totalPackets);
        rtpFlowStatsBuilder.setTotalPacketsLost(totalPacketsLost);
        rtpFlowStatsBuilder.setMosx100(mos);
        rtpFlowStatsBuilder.setRtpSeqFirst(firstRtpSeq);
        rtpFlowStatsBuilder.setRtpSeqLast(lastRtpSeq);

        return rtpFlowStatsBuilder.build();
    }

    private OpensyncStats.CallReport getDefaultCallReport(OpensyncStats.CallReport.CallReportReason reason, int codec,
                                                          int jitter, int latency, int totalPackets, int totalPacketsLost,
                                                          int mos, int firstRtpSeq, int lastRtpSeq) {
        OpensyncStats.CallReport.Builder callReportBuilder = OpensyncStats.CallReport.newBuilder();
        callReportBuilder.setBand(RadioBandType.BAND5G);
        callReportBuilder.setChannel(40);
        callReportBuilder.addCodecs("110 opus/48000/2");
        callReportBuilder.addCodecs("102 iLBC/8000");
        callReportBuilder.setClientMac(ByteString.copyFrom("C0:9A:D0:76:A9:69".getBytes()));
        callReportBuilder.setProviderDomain("skype");
        callReportBuilder.setSessionId(123L);
        callReportBuilder.setWifiSessionId(1234L);
        callReportBuilder.setReason(reason);
        callReportBuilder.addStats(getRtpFlowStats(codec, jitter, latency, totalPackets, totalPacketsLost,
                mos, firstRtpSeq, lastRtpSeq));

        return callReportBuilder.build();
    }

    private List<EventReport> getOpensyncStatsEventReportsList() {


        List<ClientAssocEvent> clientAssocEventList = new ArrayList<>();
        sts.OpensyncStats.EventReport.ClientAssocEvent.Builder clientAssocBuilder = EventReport.ClientAssocEvent
                .getDefaultInstance().toBuilder();
        clientAssocBuilder.setAssocType(AssocType.ASSOC);
        clientAssocBuilder.setBand(RadioBandType.BAND5GU);
        clientAssocBuilder.setRssi(-65);
        clientAssocBuilder.setStaMac("C0:9A:D0:76:A9:69");
        clientAssocBuilder.setStaMacBytes(ByteString.copyFrom("C0:9A:D0:76:A9:69".getBytes()));
        clientAssocBuilder.setSessionId(1000L);
        clientAssocBuilder.setInternalSc(1);
        clientAssocBuilder.setSsid("ssid-3");
        clientAssocBuilder.setStatus(1);

        sts.OpensyncStats.EventReport.ClientAssocEvent.Builder clientAssocBuilder2 = EventReport.ClientAssocEvent
                .getDefaultInstance().toBuilder();
        clientAssocBuilder2.setAssocType(AssocType.ASSOC);
        clientAssocBuilder2.setBand(RadioBandType.BAND2G);
        clientAssocBuilder2.setRssi(-65);
        clientAssocBuilder2.setStaMac("7C:AB:60:E6:EA:4D");
        clientAssocBuilder2.setStaMacBytes(ByteString.copyFrom("7C:AB:60:E6:EA:4D".getBytes()));
        clientAssocBuilder2.setSessionId(1000L);
        clientAssocBuilder2.setInternalSc(1);
        clientAssocBuilder2.setSsid("ssid-1");
        clientAssocBuilder2.setStatus(1);

        clientAssocEventList.add(clientAssocBuilder.build());
        clientAssocEventList.add(clientAssocBuilder2.build());

        List<EventReport> eventReportList = new ArrayList<>();

        EventReport.Builder eventReportBuilder = EventReport.getDefaultInstance().toBuilder();

        sts.OpensyncStats.EventReport.ClientSession.Builder clientSessionBuilder = sts.OpensyncStats.EventReport.ClientSession
                .getDefaultInstance().toBuilder();

        clientSessionBuilder.setSessionId(1000L);

        clientSessionBuilder.addAllClientAssocEvent(clientAssocEventList);
        List<sts.OpensyncStats.EventReport.ClientSession> clientSessionList = new ArrayList<>();
        clientSessionList.add(clientSessionBuilder.build());
        
        sts.OpensyncStats.EventReport.ChannelSwitchEvent.Builder channelSwitchEventBuilder = sts.OpensyncStats.EventReport.ChannelSwitchEvent.getDefaultInstance().toBuilder();
        channelSwitchEventBuilder.setBand(RadioBandType.BAND5GL).setChannel(40).setReason(ChannelSwitchReason.high_interference).setTimestampMs(System.currentTimeMillis());

        List<ChannelSwitchEvent> channelSwitchEventList = new ArrayList<>();
        channelSwitchEventList.add(channelSwitchEventBuilder.build());
        
        eventReportBuilder.addAllClientSession(clientSessionList);
        eventReportBuilder.addAllChannelSwitch(channelSwitchEventList);

        eventReportList.add(eventReportBuilder.build());


        return eventReportList;


    }

    private List<ClientReport> getOpensyncStatsClientReportsList() {
        int rssi = Long.valueOf(4294967239L).intValue();

        Client.Stats clientStats = Client.Stats.getDefaultInstance().toBuilder().setRssi(rssi).setRxBytes(225554786)
                .setRxRate(24000.0).setTxBytes(1208133026).setTxRate(433300.0).setRssi(758722570).setRxFrames(10000)
                .setTxFrames(10000).setTxRate(24000.0).build();
        Client client2g = Client.getDefaultInstance().toBuilder().setMacAddress("7C:AB:60:E6:EA:4D").setSsid("ssid-1")
                .setConnected(true).setDurationMs(59977).setStats(clientStats).build();
        Client client5gu = Client.getDefaultInstance().toBuilder().setMacAddress("C0:9A:D0:76:A9:69").setSsid("ssid-3")
                .setConnected(true).setDurationMs(298127).setStats(clientStats).build();

        ClientReport clientReport2g = ClientReport.getDefaultInstance().toBuilder().setBand(RadioBandType.BAND2G)
                .setChannel(6).addAllClientList(ImmutableList.of(client2g)).build();
        ClientReport clientReport5gl = ClientReport.getDefaultInstance().toBuilder().setBand(RadioBandType.BAND5GL)
                .setChannel(36).addAllClientList(new ArrayList<Client>()).build();
        ClientReport clientReport5gu = ClientReport.getDefaultInstance().toBuilder().setBand(RadioBandType.BAND5GU)
                .setChannel(157).addAllClientList(ImmutableList.of(client5gu)).build();

        List<ClientReport> clients = new ArrayList<>();
        clients.add(clientReport2g);
        clients.add(clientReport5gl);
        clients.add(clientReport5gu);
        return clients;
    }

}
