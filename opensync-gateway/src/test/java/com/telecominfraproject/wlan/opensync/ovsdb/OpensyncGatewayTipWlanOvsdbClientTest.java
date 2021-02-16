package com.telecominfraproject.wlan.opensync.ovsdb;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
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

import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.opensync.external.integration.OpensyncExternalIntegrationInterface;
import com.telecominfraproject.wlan.opensync.external.integration.OvsdbSession;
import com.telecominfraproject.wlan.opensync.external.integration.OvsdbSessionMapInterface;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.OvsdbDao;
import com.telecominfraproject.wlan.profile.network.models.ApNetworkConfiguration;
import com.vmware.ovsdb.callback.MonitorCallback;
import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.protocol.methods.MonitorRequests;
import com.vmware.ovsdb.protocol.methods.TableUpdates;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.service.OvsdbClient;

import io.netty.handler.ssl.SslContext;

@RunWith(SpringRunner.class)
@ActiveProfiles(profiles = { "integration_test", }) // NOTE: these profiles will
                                                    // be ADDED to the list of
                                                    // active profiles
@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = OpensyncGatewayTipWlanOvsdbClientTest.class)
@Import(value = { OpensyncGatewayTipWlanOvsdbClientTest.Config.class, TipWlanOvsdbClient.class,
        TipWlanOvsdbRedirector.class, OvsdbListenerConfig.class, OvsdbSessionMapInterface.class, OvsdbDao.class,
        OpensyncExternalIntegrationInterface.class, OvsdbSession.class, SslContext.class })
public class OpensyncGatewayTipWlanOvsdbClientTest {

    @MockBean
    private TipWlanOvsdbRedirector tipwlanOvsdbRedirector;

    @MockBean
    private OpensyncExternalIntegrationInterface opensyncExternalIntegrationInterface;

    @MockBean
    private OvsdbSessionMapInterface ovsdbSessionMapInterface;

    @MockBean
    private SslContext sslContext;

    @MockBean
    private OvsdbDao ovsdbDao;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private OvsdbClient ovsdbClient;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CompletableFuture<TableUpdates> completableFuture;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private CompletableFuture<OperationResult[]> operationResult;

    @Autowired
    TipWlanOvsdbClient tipwlanOvsdbClient;

    MockitoSession mockito;

    @Before
    public void setup() throws OvsdbClientException {

        Mockito.when(ovsdbSessionMapInterface.getSession(Mockito.anyString()))
                .thenReturn(Mockito.mock(OvsdbSession.class, Mockito.RETURNS_DEEP_STUBS));
        Mockito.when(opensyncExternalIntegrationInterface.getApConfig(Mockito.anyString()))
                .thenReturn(Mockito.mock(OpensyncAPConfig.class, Mockito.RETURNS_DEEP_STUBS));

        Mockito.when(ovsdbClient.monitor(Mockito.anyString(), Mockito.anyString(), Mockito.any(MonitorRequests.class),
                Mockito.any(MonitorCallback.class))).thenReturn(completableFuture);

        mockito = Mockito.mockitoSession().initMocks(this).strictness(Strictness.STRICT_STUBS).startMocking();

    }

    @After
    public void teardown() {
        mockito.finishMocking();
    }

    @Configuration
    // @PropertySource({ "classpath:persistence-${envTarget:dev}.properties" })
    static class Config {
        @Bean
        public TipWlanOvsdbClient tipwlanOvsdbClient() {
            return new TipWlanOvsdbClient();
        }
    }

    @Test
    public void testGetConnectedClientIds() throws Exception {
        Set<String> connectedClientIds = new HashSet<String>();
        Mockito.when(ovsdbSessionMapInterface.getConnectedClientIds()).thenReturn(connectedClientIds);
        assert (connectedClientIds.equals(tipwlanOvsdbClient.getConnectedClientIds()));
    }

    @Test
    public void testProcessConfigChanged() throws Exception {
        OvsdbSession ovsdbSession = Mockito.mock(OvsdbSession.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(ovsdbSession.getOvsdbClient()).thenReturn(ovsdbClient);
        Mockito.when(ovsdbSessionMapInterface.getSession("Test_Client_21P10C68818122")).thenReturn(ovsdbSession);

        OpensyncAPConfig apConfig = Mockito.mock(OpensyncAPConfig.class, Mockito.RETURNS_DEEP_STUBS);

        Mockito.when(apConfig.getApProfile().getDetails()).thenReturn(Mockito.mock(ApNetworkConfiguration.class));

        Mockito.when(opensyncExternalIntegrationInterface.getApConfig(Mockito.anyString())).thenReturn(apConfig);

        tipwlanOvsdbClient.processConfigChanged("Test_Client_21P10C68818122");

        Mockito.verify(ovsdbSessionMapInterface).getSession("Test_Client_21P10C68818122");
        Mockito.verify(ovsdbSession).getOvsdbClient();
        Mockito.verify(opensyncExternalIntegrationInterface).getApConfig("Test_Client_21P10C68818122");
        Mockito.verify(ovsdbDao).removeAllSsids(ovsdbClient);
        Mockito.verify(ovsdbDao).removeAllStatsConfigs(ovsdbClient);
        Mockito.verify(ovsdbDao).configureWifiRadios(ovsdbClient, apConfig);
        Mockito.verify(ovsdbDao).configureSsids(ovsdbClient, apConfig);
        Mockito.verify(ovsdbDao).configureStatsFromProfile(ovsdbClient, apConfig);

    }

    @Test
    public void testProcessFirmwareDownload() throws Exception {

        OvsdbSession ovsdbSession = Mockito.mock(OvsdbSession.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(ovsdbSession.getOvsdbClient()).thenReturn(ovsdbClient);

        Mockito.when(ovsdbSessionMapInterface.getSession("Test_Client_21P10C68818122")).thenReturn(ovsdbSession);

        String expectedResult = "Initialized firmware download to Test_Client_21P10C68818122";

        assert (tipwlanOvsdbClient.processFirmwareDownload("Test_Client_21P10C68818122",
                "http://127.0.0.1/~username/ea8300-2020-07-08-6632239/openwrt-ipq40xx-generic-linksys_ea8300-squashfs-sysupgrade.bin",
                "openwrt-ipq40xx-generic-linksys_ea8300-squashfs-sysupgrade", "username").equals(expectedResult));

        Mockito.verify(ovsdbDao).configureFirmwareDownload(ovsdbClient, "Test_Client_21P10C68818122",
                "http://127.0.0.1/~username/ea8300-2020-07-08-6632239/openwrt-ipq40xx-generic-linksys_ea8300-squashfs-sysupgrade.bin",
                "openwrt-ipq40xx-generic-linksys_ea8300-squashfs-sysupgrade", "username");

    }

    @Test
    public void testProcessNewChannelsRequest() throws Exception {

        OvsdbSession ovsdbSession = Mockito.mock(OvsdbSession.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(ovsdbSession.getOvsdbClient()).thenReturn(ovsdbClient);

        Mockito.when(ovsdbSessionMapInterface.getSession("Test_Client_21P10C68818122")).thenReturn(ovsdbSession);
        
        
        String expectedResponse = " change backup and/or primary channels for AP Test_Client_21P10C68818122";

        assert (tipwlanOvsdbClient.processNewChannelsRequest("Test_Client_21P10C68818122",
                Map.of(RadioType.is2dot4GHz, Integer.valueOf(1), RadioType.is5GHzL, Integer.valueOf(40),
                        RadioType.is5GHzU, Integer.valueOf(153)),
                Map.of(RadioType.is2dot4GHz, Integer.valueOf(6), RadioType.is5GHzL, Integer.valueOf(36),
                        RadioType.is5GHzU, Integer.valueOf(149)))
                .equals(expectedResponse));

        Mockito.verify(ovsdbDao).processNewChannelsRequest(ovsdbClient,
                Map.of(RadioType.is2dot4GHz, Integer.valueOf(1), RadioType.is5GHzL, Integer.valueOf(40),
                        RadioType.is5GHzU, Integer.valueOf(153)),
                Map.of(RadioType.is2dot4GHz, Integer.valueOf(6), RadioType.is5GHzL, Integer.valueOf(36),
                        RadioType.is5GHzU, Integer.valueOf(149)));

    }

}
