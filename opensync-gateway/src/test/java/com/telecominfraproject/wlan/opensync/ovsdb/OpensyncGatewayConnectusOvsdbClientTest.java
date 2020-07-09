package com.telecominfraproject.wlan.opensync.ovsdb;

import static org.junit.Assert.assertTrue;

import java.util.HashSet;
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

import com.telecominfraproject.wlan.opensync.external.integration.OpensyncExternalIntegrationInterface;
import com.telecominfraproject.wlan.opensync.external.integration.OvsdbSession;
import com.telecominfraproject.wlan.opensync.external.integration.OvsdbSessionMapInterface;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.OvsdbDao;
import com.vmware.ovsdb.callback.MonitorCallback;
import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.protocol.methods.MonitorRequests;
import com.vmware.ovsdb.protocol.methods.TableUpdates;
import com.vmware.ovsdb.service.OvsdbClient;

/**
 * @author dtoptygin
 * 
 *         Integration test for ProfileController
 *
 */
@RunWith(SpringRunner.class)
@ActiveProfiles(profiles = { "integration_test", }) // NOTE: these profiles will
                                                    // be ADDED to the list of
                                                    // active profiles
@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = OpensyncGatewayConnectusOvsdbClientTest.class)
@Import(value = { OpensyncGatewayConnectusOvsdbClientTest.Config.class, ConnectusOvsdbClient.class,
        ConnectusOvsdbRedirector.class, NettySslContextConfig.class, OvsdbListenerConfig.class,
        OvsdbSessionMapInterface.class, OvsdbDao.class, OpensyncExternalIntegrationInterface.class,
        OvsdbSession.class })
public class OpensyncGatewayConnectusOvsdbClientTest {

    @MockBean
    private ConnectusOvsdbRedirector connectusOvsdbRedirector;

    @MockBean
    private OpensyncExternalIntegrationInterface opensyncExternalIntegrationInterface;

    @MockBean
    private OvsdbSessionMapInterface ovsdbSessionMapInterface;

    @MockBean
    private OvsdbDao ovsdbDao;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private OvsdbClient ovsdbClient;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CompletableFuture<TableUpdates> completableFuture;

    @Autowired
    ConnectusOvsdbClient connectusOvsdbClient;

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
        public ConnectusOvsdbClient connectusOvsdbClient() {
            return new ConnectusOvsdbClient();
        }
    }

    @Test
    public void testGetConnectedClientIds() throws Exception {
        Set<String> connectedClientIds = new HashSet<String>();
        Mockito.when(ovsdbSessionMapInterface.getConnectedClientIds()).thenReturn(connectedClientIds);
        assert (connectedClientIds.equals(connectusOvsdbClient.getConnectedClientIds()));
    }

    @Test
    public void testProcessConfigChanged() throws Exception {
        OvsdbSession ovsdbSession = Mockito.mock(OvsdbSession.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(ovsdbSession.getOvsdbClient()).thenReturn(ovsdbClient);
        Mockito.when(ovsdbSessionMapInterface.getSession("Test_Client_21P10C68818122")).thenReturn(ovsdbSession);

        connectusOvsdbClient.processConfigChanged("Test_Client_21P10C68818122");

        Mockito.verify(ovsdbSessionMapInterface).getSession("Test_Client_21P10C68818122");
        Mockito.verify(ovsdbSession).getOvsdbClient();
        Mockito.verify(ovsdbClient).cancelMonitor(OvsdbDao.awlanNodeDbTable + "_Test_Client_21P10C68818122");
        Mockito.verify(ovsdbClient).monitor(Mockito.eq(OvsdbDao.ovsdbName),
                Mockito.eq(OvsdbDao.awlanNodeDbTable + "_Test_Client_21P10C68818122"),
                Mockito.any(MonitorRequests.class), Mockito.any(MonitorCallback.class));
        Mockito.verify(ovsdbClient).cancelMonitor(OvsdbDao.wifiRadioStateDbTable + "_Test_Client_21P10C68818122");
        Mockito.verify(ovsdbClient).monitor(Mockito.eq(OvsdbDao.ovsdbName),
                Mockito.eq(OvsdbDao.wifiRadioStateDbTable + "_Test_Client_21P10C68818122"),
                Mockito.any(MonitorRequests.class), Mockito.any(MonitorCallback.class));
        Mockito.verify(ovsdbClient).cancelMonitor(OvsdbDao.wifiVifStateDbTable + "_Test_Client_21P10C68818122");
        Mockito.verify(ovsdbClient).monitor(Mockito.eq(OvsdbDao.ovsdbName),
                Mockito.eq(OvsdbDao.wifiVifStateDbTable + "_Test_Client_21P10C68818122"),
                Mockito.any(MonitorRequests.class), Mockito.any(MonitorCallback.class));
        Mockito.verify(ovsdbClient).cancelMonitor(OvsdbDao.wifiVifStateDbTable + "_delete_Test_Client_21P10C68818122");
        Mockito.verify(ovsdbClient).monitor(Mockito.eq(OvsdbDao.ovsdbName),
                Mockito.eq(OvsdbDao.wifiVifStateDbTable + "_delete_Test_Client_21P10C68818122"),
                Mockito.any(MonitorRequests.class), Mockito.any(MonitorCallback.class));
        Mockito.verify(ovsdbClient).cancelMonitor(OvsdbDao.wifiInetStateDbTable + "_Test_Client_21P10C68818122");
        Mockito.verify(ovsdbClient).monitor(Mockito.eq(OvsdbDao.ovsdbName),
                Mockito.eq(OvsdbDao.wifiInetStateDbTable + "_Test_Client_21P10C68818122"),
                Mockito.any(MonitorRequests.class), Mockito.any(MonitorCallback.class));
        Mockito.verify(ovsdbClient)
                .cancelMonitor(OvsdbDao.wifiAssociatedClientsDbTable + "_delete_Test_Client_21P10C68818122");
        Mockito.verify(ovsdbClient).monitor(Mockito.eq(OvsdbDao.ovsdbName),
                Mockito.eq(OvsdbDao.wifiAssociatedClientsDbTable + "_delete_Test_Client_21P10C68818122"),
                Mockito.any(MonitorRequests.class), Mockito.any(MonitorCallback.class));
        Mockito.verify(ovsdbClient)
                .cancelMonitor(OvsdbDao.wifiAssociatedClientsDbTable + "_Test_Client_21P10C68818122");
        Mockito.verify(ovsdbClient).monitor(Mockito.eq(OvsdbDao.ovsdbName),
                Mockito.eq(OvsdbDao.wifiAssociatedClientsDbTable + "_Test_Client_21P10C68818122"),
                Mockito.any(MonitorRequests.class), Mockito.any(MonitorCallback.class));

    }

    @Test
    public void testProcessFirmwareDownload() throws Exception {

        OvsdbSession ovsdbSession = Mockito.mock(OvsdbSession.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(ovsdbSession.getOvsdbClient()).thenReturn(ovsdbClient);

        Mockito.when(ovsdbSessionMapInterface.getSession("Test_Client_21P10C68818122")).thenReturn(ovsdbSession);

        String expectedResult = "Initialized firmware download to Test_Client_21P10C68818122";

        assert (connectusOvsdbClient.processFirmwareDownload("Test_Client_21P10C68818122",
                "http://127.0.0.1/~username/ea8300-2020-07-08-6632239/openwrt-ipq40xx-generic-linksys_ea8300-squashfs-sysupgrade.bin",
                "openwrt-ipq40xx-generic-linksys_ea8300-squashfs-sysupgrade", "username",
                "b0d03d8fba6b2261786ac97d49a629f2").equals(expectedResult));

        Mockito.verify(ovsdbDao).configureFirmwareDownload(ovsdbClient, "Test_Client_21P10C68818122",
                "http://127.0.0.1/~username/ea8300-2020-07-08-6632239/openwrt-ipq40xx-generic-linksys_ea8300-squashfs-sysupgrade.bin",
                "openwrt-ipq40xx-generic-linksys_ea8300-squashfs-sysupgrade", "username",
                "b0d03d8fba6b2261786ac97d49a629f2");

    }

}
