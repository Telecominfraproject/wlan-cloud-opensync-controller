package com.telecominfraproject.wlan.opensync.external.integration.utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
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

import com.google.protobuf.ByteString;
import com.telecominfraproject.wlan.client.models.events.realtime.ClientAssocEvent;
import com.telecominfraproject.wlan.client.models.events.realtime.ClientAuthEvent;
import com.telecominfraproject.wlan.client.models.events.utils.WlanStatusCode;
import com.telecominfraproject.wlan.cloudeventdispatcher.CloudEventDispatcherInterface;
import com.telecominfraproject.wlan.equipment.EquipmentServiceInterface;
import com.telecominfraproject.wlan.opensync.external.integration.OpensyncExternalIntegrationCloud;

import sts.OpensyncStats;
import sts.OpensyncStats.RadioBandType;
import sts.OpensyncStats.Report;

@RunWith(SpringRunner.class)
@ActiveProfiles(profiles = { "integration_test", })
@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = RealtimeEventPublisherTest.class)
@Import(value = { OpensyncExternalIntegrationCloud.class, RealtimeEventPublisherTest.Config.class,

})
public class RealtimeEventPublisherTest {

    @MockBean
    private CloudEventDispatcherInterface cloudEventDispatcherInterface;

    @MockBean
    private EquipmentServiceInterface equipmentServiceInterface;

    @Autowired
    RealtimeEventPublisher realtimeEventPublisher;
    MockitoSession mockito;

    @Configuration
    static class Config {

        @Bean
        public RealtimeEventPublisher realtimeEventPublisher() {
            return new RealtimeEventPublisher();
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

    @Ignore
    public void testPublishChannelHopEvents() {
        // TODO: implement
    }

    @Ignore
    public void testPublishClientConnectSuccessEvent() {
        // TODO: implement
    }

    @Ignore
    public void testPublishClientDisconnectEvent() {
        // TODO: implement
    }

    @Test
    public void testPublishClientAuthSystemEvent() throws Exception {

        OpensyncStats.EventReport.ClientAuthEvent clientAuthEvent = OpensyncStats.EventReport.ClientAuthEvent
                .newBuilder().setBand(RadioBandType.BAND5GL).setSsid("TipWlan-cloud-3-radios")
                .setStaMac("c0:9a:d0:76:a9:69").setSessionId(Long.parseUnsignedLong("12377998144488079334"))
                .setAuthStatus(WlanStatusCode.WLAN_STATUS_SUCCESS.getId()).setTimestampMs(1610050309).build();

        realtimeEventPublisher.publishClientAuthSystemEvent(2, 1L, clientAuthEvent);

        Mockito.verify(cloudEventDispatcherInterface, Mockito.times(1))
                .publishEvent(Mockito.any(ClientAuthEvent.class));
    }

    @Test
    public void testPublishClientAssocEvent() throws Exception {

        OpensyncStats.EventReport.ClientAssocEvent clientAssocEvent = OpensyncStats.EventReport.ClientAssocEvent
                .newBuilder().setBand(RadioBandType.BAND5GL).setRssi(-37).setSsid("TipWlan-cloud-3-radios")
                .setStaMac("c0:9a:d0:76:a9:69").setSessionId(Long.parseUnsignedLong("12377998144488079334"))
                .setUsing11K(true).setUsing11V(true).setStatus(WlanStatusCode.WLAN_STATUS_SUCCESS.getId())
                .setTimestampMs(1610050309).build();

        realtimeEventPublisher.publishClientAssocEvent(2, 1L, clientAssocEvent);

        Mockito.verify(cloudEventDispatcherInterface, Mockito.times(1))
                .publishEvent(Mockito.any(ClientAssocEvent.class));
    }

    @Ignore
    public void testPublishClientFailureEvent() {
        // TODO: implement
    }

    @Ignore
    public void testPublishClientFirstDataEvent() {
        // TODO: implement
    }

    @Ignore
    public void testPublishClientIdEvent() {
        // TODO: implement
    }

    @Ignore
    public void testPublishClientIpEvent() {
        // TODO: implement
    }

    @Ignore
    public void testPublishClientTimeoutEvent() {
        // TODO: implement
    }

    @Ignore
    public void testPublishDhcpTransactionEvents() {
        // TODO: implement
    }

    @Test
    public void testPublishSipCallEvents() throws Exception {

        OpensyncStats.VideoVoiceReport.Builder callStartVoiceReportBuilder = OpensyncStats.VideoVoiceReport.newBuilder()
                .setCallStart(getDefaultCallStart());
        OpensyncStats.VideoVoiceReport.Builder callReportGotPublishVoiceReportBuilder = OpensyncStats.VideoVoiceReport
                .newBuilder().setCallReport(getDefaultCallReport(OpensyncStats.CallReport.CallReportReason.GOT_PUBLISH,
                        121, 1028, 1316, 1888, 298, 2, 100, 200));
        OpensyncStats.VideoVoiceReport.Builder callReportRoamedToVoiceReportBuilder = OpensyncStats.VideoVoiceReport
                .newBuilder().setCallReport(getDefaultCallReport(OpensyncStats.CallReport.CallReportReason.ROAMED_TO,
                        123, 1020, 1116, 1345, 223, 0, 102, 203));
        OpensyncStats.VideoVoiceReport.Builder callReportRoamedFromVoiceReportBuilder = OpensyncStats.VideoVoiceReport
                .newBuilder().setCallReport(getDefaultCallReport(OpensyncStats.CallReport.CallReportReason.ROAMED_FROM,
                        122, 1029, 1300, 1234, 111, 3, 101, 201));
        OpensyncStats.VideoVoiceReport.Builder callStopVoiceReportBuilder = OpensyncStats.VideoVoiceReport.newBuilder()
                .setCallStop(getDefaultCallStop());
        // Create report with multiple voiceReports in one
        Report multipleVoiceReportsInOneReport = Report.newBuilder().addVideoVoiceReport(callStartVoiceReportBuilder)
                .addVideoVoiceReport(callReportGotPublishVoiceReportBuilder)
                .addVideoVoiceReport(callReportRoamedFromVoiceReportBuilder)
                .addVideoVoiceReport(callReportRoamedToVoiceReportBuilder)
                .addVideoVoiceReport(callStopVoiceReportBuilder).setNodeID("1").build();

        realtimeEventPublisher.publishSipCallEvents(1, 2L, multipleVoiceReportsInOneReport.getVideoVoiceReportList());

        Mockito.verify(cloudEventDispatcherInterface, Mockito.times(1)).publishEventsBulk(Mockito.anyList());
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
        callStopBuilder.addStats(getRtpFlowStats(121, 1380, 1400, 3000, 119, 3, 205, 350));

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
            int jitter, int latency, int totalPackets, int totalPacketsLost, int mos, int firstRtpSeq, int lastRtpSeq) {
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
        callReportBuilder.addStats(
                getRtpFlowStats(codec, jitter, latency, totalPackets, totalPacketsLost, mos, firstRtpSeq, lastRtpSeq));

        return callReportBuilder.build();
    }

}
