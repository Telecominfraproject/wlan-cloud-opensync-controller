package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.telecominfraproject.wlan.opensync.external.integration.models.ConnectNodeInfo;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.profile.gre.tunnels.GreTunnelProfile;
import com.telecominfraproject.wlan.profile.models.Profile;
import com.telecominfraproject.wlan.profile.models.ProfileType;
import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.protocol.operation.notation.Atom;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;
import com.vmware.ovsdb.protocol.operation.notation.Value;
import com.vmware.ovsdb.protocol.operation.result.ErrorResult;
import com.vmware.ovsdb.protocol.operation.result.InsertResult;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.protocol.operation.result.SelectResult;
import com.vmware.ovsdb.service.OvsdbClient;

@RunWith(SpringRunner.class)
@ActiveProfiles(profiles = { "integration_test", }) // NOTE: these profiles will
                                                    // be ADDED to the list of
                                                    // active profiles
@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = OvsdbDaoTest.class)
@Import(value = { OvsdbDao.class, OvsdbDaoTest.Config.class,

})
public class OvsdbDaoTest {

    private static final String LAN_IF_TYPE = "bridge";

    private static final String LAN_IF_NAME = "br-lan";

    private static final String LAN_MAC = "24:f5:a2:ef:2e:52";

    private static final String LAN_IP = "192.168.1.1";

    private static final String WAN_IF_TYPE = "eth";

    private static final String WAN_IF_NAME = "eth1";

    private static final String WAN_MAC = "24:f5:a2:ef:2e:53";

    private static final String WAN_IP = "10.0.0.129";

    private static final String MODEL = "EA8300-CA";

    private static final String REVISION = "1";

    private static final String SERIAL_NUM = "21P10C68818122";

    private static final String SKU_NUMBER = "connectus.ai_21P10C68818122";

    private static final String REDIRECT_ADDR = "ssl:opensync.zone1.art2wave.com:6643";

    private static final String MGR_ADDR = "ssl:192.168.1.101:6640";

    private static final String PLATFORM_VERSION = "OPENWRT_EA8300";

    private static final String FW_VERSION = "0.1.0";

    @Mock(answer = Answers.RETURNS_MOCKS)
    OvsdbClient ovsdbClient;

    @Mock
    CompletableFuture<OperationResult[]> futureResult;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    CompletableFuture<OperationResult[]> selectionFutureResult;

    @Autowired
    OvsdbDao ovsdbDao;

    MockitoSession mockito;

    @Configuration
    // @PropertySource({ "classpath:persistence-${envTarget:dev}.properties" })
    static class Config {

        @Bean
        public OvsdbDao ovsdbDao() {
            return new OvsdbDao();
        }
    }

    @Before
    public void setup() {
        mockito = Mockito.mockitoSession().initMocks(this).strictness(Strictness.STRICT_STUBS).startMocking();
    }

    @After
    public void teardown() {
        mockito.finishMocking();
    }

    @Test
    public void testRemoveAllGreTunnels() throws Exception {
        List<Row> rows = new ArrayList<>();
        OperationResult[] operationResult = new OperationResult[] { new SelectResult(rows) };
        Mockito.when(ovsdbClient.transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList()))
                .thenReturn(selectionFutureResult);
        Mockito.when(selectionFutureResult.get(30, TimeUnit.SECONDS)).thenReturn(operationResult);
        Profile greProfile = new Profile();
        greProfile.setCustomerId(2);
        greProfile.setId(1L);
        greProfile.setName("gre1");
        greProfile.setProfileType(ProfileType.gre_tunnel);
        GreTunnelProfile tunnelProfileDetails = GreTunnelProfile.createWithDefaults();
        tunnelProfileDetails.setGreLocalInetAddr(InetAddress.getByName("10.0.10.10"));
        tunnelProfileDetails.setGreRemoteInetAddr(InetAddress.getByName("192.168.0.10"));
        tunnelProfileDetails.setGreTunnelName("gre1");
        tunnelProfileDetails.setGreParentIfName("wan");
        greProfile.setDetails(tunnelProfileDetails);
        List<Profile> greTunnelList = ImmutableList.of(greProfile);
        OpensyncAPConfig apConfig = Mockito.mock(OpensyncAPConfig.class);
        Mockito.when(apConfig.getGreTunnelProfiles()).thenReturn(greTunnelList);
        ovsdbDao.removeAllGreTunnels(ovsdbClient, apConfig);
        
        Mockito.verify(apConfig, Mockito.times(2)).getGreTunnelProfiles();
        Mockito.verify(ovsdbClient, Mockito.times(1)).transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList());

    }
    
    @Test
    public void testRemoveAllGreTunnelsNoProfile() throws Exception {
        List<Row> rows = new ArrayList<>();
        OperationResult[] operationResult = new OperationResult[] { new SelectResult(rows) };
        Mockito.when(ovsdbClient.transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList()))
                .thenReturn(selectionFutureResult);
        Mockito.when(selectionFutureResult.get(30, TimeUnit.SECONDS)).thenReturn(operationResult);
      
        ovsdbDao.removeAllGreTunnels(ovsdbClient, null);
        
        Mockito.verify(ovsdbClient, Mockito.times(1)).transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList());

    }

    @Test
    public void testConfigureGreTunnels() throws Exception {
        // test create 2 gre tunnel profiles
        List<Row> rows = new ArrayList<>();
        OperationResult[] operationResult = new OperationResult[] { new SelectResult(rows) };
        Mockito.when(ovsdbClient.transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList()))
                .thenReturn(selectionFutureResult);
        Mockito.when(selectionFutureResult.get(30, TimeUnit.SECONDS)).thenReturn(operationResult);
        Profile greProfile = new Profile();
        greProfile.setCustomerId(2);
        greProfile.setId(1L);
        greProfile.setName("gre1");
        greProfile.setProfileType(ProfileType.gre_tunnel);
        GreTunnelProfile tunnelProfileDetails = GreTunnelProfile.createWithDefaults();
        tunnelProfileDetails.setGreLocalInetAddr(InetAddress.getByName("10.0.10.10"));
        tunnelProfileDetails.setGreRemoteInetAddr(InetAddress.getByName("192.168.0.10"));
        tunnelProfileDetails.setGreTunnelName("gre1");
        tunnelProfileDetails.setGreParentIfName("wan");
        greProfile.setDetails(tunnelProfileDetails);
        Profile greProfile2 = greProfile.clone();
        greProfile2.setName("gre2");
        ((GreTunnelProfile)greProfile2.getDetails()).setGreTunnelName("gre2");
        List<Profile> greTunnelList = ImmutableList.of(greProfile,greProfile2);
        OpensyncAPConfig apConfig = Mockito.mock(OpensyncAPConfig.class);
        Mockito.when(apConfig.getGreTunnelProfiles()).thenReturn(greTunnelList);
        ovsdbDao.configureGreTunnels(ovsdbClient, apConfig);
        // 2 calls to check existence, 2 calls to insert tunnel (1 each per Profile)
        Mockito.verify(ovsdbClient, Mockito.times(4)).transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList());
        Mockito.verify(apConfig, Mockito.times(3)).getGreTunnelProfiles();

    }

    @Test
    public void testGetConnectNodeInfo() throws Exception {

        Mockito.when(ovsdbClient.transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList())).thenReturn(futureResult);

        java.util.Map<String, Value> awlanColumns = new HashMap<>();
        awlanColumns.put("mqtt_settings", com.vmware.ovsdb.protocol.operation.notation.Map
                .of(ImmutableMap.of("broker", "192.168.1.101", "port", "1883")));
        awlanColumns.put("redirector_addr", Atom.string(REDIRECT_ADDR));
        awlanColumns.put("manager_addr", Atom.string(MGR_ADDR));
        awlanColumns.put("platform_version", Atom.string(PLATFORM_VERSION));
        awlanColumns.put("firmware_version", Atom.string(FW_VERSION));
        awlanColumns.put("revision", Atom.string(REVISION));
        awlanColumns.put("sku_number", Atom.string(SKU_NUMBER));
        awlanColumns.put("serial_number", Atom.string(SERIAL_NUM));
        awlanColumns.put("model", Atom.string(MODEL));
        Row awlanRow = new Row(awlanColumns);

        List<Row> awlanRows = ImmutableList.of(awlanRow);

        OperationResult[] awlanResult = new OperationResult[] { new SelectResult(awlanRows) };

        java.util.Map<String, Value> inetWanColumns = new HashMap<>();
        inetWanColumns.put("inet_addr", Atom.string(WAN_IP));
        inetWanColumns.put("hwaddr", Atom.string(WAN_MAC));
        inetWanColumns.put("if_name", Atom.string(WAN_IF_NAME));
        inetWanColumns.put("if_type", Atom.string(WAN_IF_TYPE));

        Row inetWanRow = new Row(inetWanColumns);

        List<Row> inetWanRows = ImmutableList.of(inetWanRow);

        OperationResult[] inetWanResult = new OperationResult[] { new SelectResult(inetWanRows) };

        java.util.Map<String, Value> inetLanColumns = new HashMap<>();
        inetLanColumns.put("inet_addr", Atom.string(LAN_IP));
        inetLanColumns.put("hwaddr", Atom.string(LAN_MAC));
        inetLanColumns.put("if_name", Atom.string(LAN_IF_NAME));
        inetLanColumns.put("if_type", Atom.string(LAN_IF_TYPE));

        Row inetLanRow = new Row(inetLanColumns);

        List<Row> inetLanRows = ImmutableList.of(inetLanRow);

        OperationResult[] inetLanResult = new OperationResult[] { new SelectResult(inetLanRows) };

        java.util.Map<String, Value> wifiRadioStateColumns = new HashMap<>();
        wifiRadioStateColumns.put("freq_band", Atom.string("2.4G"));
        wifiRadioStateColumns.put("if_name", Atom.string("home-ap-24"));
        Row wifiRadioStateRow = new Row(wifiRadioStateColumns);

        java.util.Map<String, Value> wifiRadioStateColumns2 = new HashMap<>();
        wifiRadioStateColumns2.put("freq_band", Atom.string("5GL"));
        wifiRadioStateColumns2.put("if_name", Atom.string("home-ap-l50"));
        Row wifiRadioStateRow2 = new Row(wifiRadioStateColumns2);

        java.util.Map<String, Value> wifiRadioStateColumns3 = new HashMap<>();
        wifiRadioStateColumns3.put("freq_band", Atom.string("5GU"));
        wifiRadioStateColumns3.put("if_name", Atom.string("home-ap-u50"));
        Row wifiRadioStateRow3 = new Row(wifiRadioStateColumns3);

        List<Row> wifiRadioStateRows = ImmutableList.of(wifiRadioStateRow, wifiRadioStateRow2, wifiRadioStateRow3);

        OperationResult[] wifiRadioStateResult = new OperationResult[] { new SelectResult(wifiRadioStateRows) };

        Mockito.when(futureResult.get(Mockito.anyLong(), Mockito.eq(TimeUnit.SECONDS))).thenReturn(awlanResult)
                .thenReturn(inetWanResult).thenReturn(inetLanResult).thenReturn(wifiRadioStateResult);

        ConnectNodeInfo connectNodeInfo = ovsdbDao.getConnectNodeInfo(ovsdbClient);

        assertNotNull(connectNodeInfo);
        assert (connectNodeInfo.wifiRadioStates.entrySet().size() == 3);
        assert (connectNodeInfo.firmwareVersion.equals(FW_VERSION));
        assert (connectNodeInfo.redirectorAddr.equals(REDIRECT_ADDR));
        assert (connectNodeInfo.ipV4Address.equals(WAN_IP));

        assert (connectNodeInfo.lanIfName.equals(LAN_IF_NAME));
        assert (connectNodeInfo.ifName.equals(WAN_IF_NAME));

        Mockito.verify(ovsdbClient, Mockito.times(4)).transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList());

    }

    @Test(expected = RuntimeException.class)
    public void testFailGetConnectNodeInfo() throws Exception {

        Mockito.when(ovsdbClient.transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList())).thenReturn(futureResult);

        java.util.Map<String, Value> awlanColumns = new HashMap<>();
        awlanColumns.put("mqtt_settings", com.vmware.ovsdb.protocol.operation.notation.Map
                .of(ImmutableMap.of("broker", "192.168.1.101", "port", "1883")));
        awlanColumns.put("redirector_addr", Atom.string(REDIRECT_ADDR));
        awlanColumns.put("manager_addr", Atom.string(MGR_ADDR));
        awlanColumns.put("platform_version", Atom.string(PLATFORM_VERSION));
        awlanColumns.put("firmware_version", Atom.string(FW_VERSION));
        awlanColumns.put("revision", Atom.string(REVISION));
        awlanColumns.put("sku_number", Atom.string(SKU_NUMBER));
        awlanColumns.put("serial_number", Atom.string(SERIAL_NUM));
        awlanColumns.put("model", Atom.string(MODEL));
        Row awlanRow = new Row(awlanColumns);

        List<Row> awlanRows = ImmutableList.of(awlanRow);

        OperationResult[] awlanResult = new OperationResult[] { new SelectResult(awlanRows) };
        java.util.Map<String, Value> inetWanColumns = new HashMap<>();
        inetWanColumns.put("inet_addr", Atom.string(WAN_IP));
        inetWanColumns.put("hwaddr", Atom.string(WAN_MAC));
        inetWanColumns.put("if_name", Atom.string(WAN_IF_NAME));
        inetWanColumns.put("if_type", Atom.string(WAN_IF_TYPE));

        Row inetWanRow = new Row(inetWanColumns);

        List<Row> inetWanRows = ImmutableList.of(inetWanRow);

        OperationResult[] inetWanResult = new OperationResult[] { new SelectResult(inetWanRows) };

        OperationResult[] errorResult = new OperationResult[] { new ErrorResult("Error", "Error") };

        java.util.Map<String, Value> inetLanColumns = new HashMap<>();
        inetLanColumns.put("inet_addr", Atom.string(LAN_IP));
        inetLanColumns.put("hwaddr", Atom.string(LAN_MAC));
        inetLanColumns.put("if_name", Atom.string(LAN_IF_NAME));
        inetLanColumns.put("if_type", Atom.string(LAN_IF_TYPE));

        Row inetLanRow = new Row(inetLanColumns);

        List<Row> inetLanRows = ImmutableList.of(inetLanRow);

        OperationResult[] inetLanResult = new OperationResult[] { new SelectResult(inetLanRows) };

        java.util.Map<String, Value> wifiRadioStateColumns = new HashMap<>();
        wifiRadioStateColumns.put("freq_band", Atom.string("2.4G"));
        wifiRadioStateColumns.put("if_name", Atom.string("home-ap-24"));
        Row wifiRadioStateRow = new Row(wifiRadioStateColumns);

        java.util.Map<String, Value> wifiRadioStateColumns2 = new HashMap<>();
        wifiRadioStateColumns2.put("freq_band", Atom.string("5GL"));
        wifiRadioStateColumns2.put("if_name", Atom.string("home-ap-l50"));
        Row wifiRadioStateRow2 = new Row(wifiRadioStateColumns2);

        java.util.Map<String, Value> wifiRadioStateColumns3 = new HashMap<>();
        wifiRadioStateColumns3.put("freq_band", Atom.string("5GU"));
        wifiRadioStateColumns3.put("if_name", Atom.string("home-ap-u50"));
        Row wifiRadioStateRow3 = new Row(wifiRadioStateColumns3);

        List<Row> wifiRadioStateRows = ImmutableList.of(wifiRadioStateRow, wifiRadioStateRow2, wifiRadioStateRow3);

        OperationResult[] wifiRadioStateResult = new OperationResult[] { new SelectResult(wifiRadioStateRows) };

        // No 'WAN' for this test, will have an ERROR
        Mockito.when(futureResult.get(Mockito.anyLong(), Mockito.eq(TimeUnit.SECONDS))).thenReturn(awlanResult)
                .thenReturn(errorResult).thenReturn(errorResult).thenReturn(inetLanResult)
                .thenReturn(wifiRadioStateResult);
        ConnectNodeInfo connectNodeInfo = ovsdbDao.getConnectNodeInfo(ovsdbClient);
        assertNotNull(connectNodeInfo.firmwareVersion);
        assertNotNull(connectNodeInfo.model);
        assertNotNull(connectNodeInfo.skuNumber);
        assertNotNull(connectNodeInfo.platformVersion);
        assertNotNull(connectNodeInfo.serialNumber);
        assertNotNull(connectNodeInfo.mqttSettings);
        assertNotNull(connectNodeInfo.redirectorAddr);
        assertNotNull(connectNodeInfo.managerAddr);

        assertNull(connectNodeInfo.ifName);
        assertNull(connectNodeInfo.ifType);
        assertNull(connectNodeInfo.ipV4Address);
        assertNull(connectNodeInfo.macAddress);

        assertNotNull(connectNodeInfo.lanIfName);
        assertNotNull(connectNodeInfo.lanIfType);
        assertNotNull(connectNodeInfo.lanIpV4Address);
        assertNotNull(connectNodeInfo.lanMacAddress);

        assert (connectNodeInfo.wifiRadioStates.entrySet().size() == 3);
        Mockito.verify(ovsdbClient, Mockito.times(5)).transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList());
        Mockito.clearInvocations(ovsdbClient);

        // No 'LAN' for this test, will have an ERROR
        Mockito.when(futureResult.get(Mockito.anyLong(), Mockito.eq(TimeUnit.SECONDS))).thenReturn(awlanResult)
                .thenReturn(inetWanResult).thenReturn(errorResult).thenReturn(wifiRadioStateResult);
        connectNodeInfo = ovsdbDao.getConnectNodeInfo(ovsdbClient);
        assertNotNull(connectNodeInfo.firmwareVersion);
        assertNotNull(connectNodeInfo.model);
        assertNotNull(connectNodeInfo.skuNumber);
        assertNotNull(connectNodeInfo.platformVersion);
        assertNotNull(connectNodeInfo.serialNumber);
        assertNotNull(connectNodeInfo.mqttSettings);
        assertNotNull(connectNodeInfo.redirectorAddr);
        assertNotNull(connectNodeInfo.managerAddr);

        assertNotNull(connectNodeInfo.ifName);
        assertNotNull(connectNodeInfo.ifType);
        assertNotNull(connectNodeInfo.ipV4Address);
        assertNotNull(connectNodeInfo.macAddress);

        assertNull(connectNodeInfo.lanIfName);
        assertNull(connectNodeInfo.lanIfType);
        assertNull(connectNodeInfo.lanIpV4Address);
        assertNull(connectNodeInfo.lanMacAddress);

        assert (connectNodeInfo.wifiRadioStates.entrySet().size() == 3);
        Mockito.verify(ovsdbClient, Mockito.times(4)).transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList());
        Mockito.clearInvocations(ovsdbClient);

        // No 'Wifi_Radio_State data' for this test, will have an ERROR
        Mockito.when(futureResult.get(Mockito.anyLong(), Mockito.eq(TimeUnit.SECONDS))).thenReturn(awlanResult)
                .thenReturn(inetWanResult).thenReturn(inetLanResult).thenReturn(errorResult);
        connectNodeInfo = ovsdbDao.getConnectNodeInfo(ovsdbClient);

        assertNotNull(connectNodeInfo.firmwareVersion);
        assertNotNull(connectNodeInfo.model);
        assertNotNull(connectNodeInfo.skuNumber);
        assertNotNull(connectNodeInfo.platformVersion);
        assertNotNull(connectNodeInfo.serialNumber);
        assertNotNull(connectNodeInfo.mqttSettings);
        assertNotNull(connectNodeInfo.redirectorAddr);
        assertNotNull(connectNodeInfo.managerAddr);

        assertNotNull(connectNodeInfo.ifName);
        assertNotNull(connectNodeInfo.ifType);
        assertNotNull(connectNodeInfo.ipV4Address);
        assertNotNull(connectNodeInfo.macAddress);

        assertNotNull(connectNodeInfo.lanIfName);
        assertNotNull(connectNodeInfo.lanIfType);
        assertNotNull(connectNodeInfo.lanIpV4Address);
        assertNotNull(connectNodeInfo.lanMacAddress);

        assert (connectNodeInfo.wifiRadioStates.isEmpty());

        Mockito.verify(ovsdbClient, Mockito.times(4)).transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList());
        Mockito.clearInvocations(ovsdbClient);

        // No 'AWLAN_Node data' for this test, will have an ERROR
        Mockito.when(futureResult.get(Mockito.anyLong(), Mockito.eq(TimeUnit.SECONDS))).thenReturn(errorResult)
                .thenReturn(inetWanResult).thenReturn(inetLanResult).thenReturn(wifiRadioStateResult);
        connectNodeInfo = ovsdbDao.getConnectNodeInfo(ovsdbClient);
        assertNull(connectNodeInfo.firmwareVersion);
        assertNull(connectNodeInfo.model);
        assertNull(connectNodeInfo.skuNumber);
        assertNull(connectNodeInfo.platformVersion);
        assertNull(connectNodeInfo.serialNumber);
        assertNull(connectNodeInfo.mqttSettings);
        assertNull(connectNodeInfo.redirectorAddr);
        assertNull(connectNodeInfo.managerAddr);

        assertNotNull(connectNodeInfo.ifName);
        assertNotNull(connectNodeInfo.ifType);
        assertNotNull(connectNodeInfo.ipV4Address);
        assertNotNull(connectNodeInfo.macAddress);

        assertNotNull(connectNodeInfo.lanIfName);
        assertNotNull(connectNodeInfo.lanIfType);
        assertNotNull(connectNodeInfo.lanIpV4Address);
        assertNotNull(connectNodeInfo.lanMacAddress);

        assert (connectNodeInfo.wifiRadioStates.entrySet().size() == 3);

        Mockito.verify(ovsdbClient, Mockito.times(4)).transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList());
        Mockito.clearInvocations(ovsdbClient);

    }

    @Test
    public void testObsdbDaoEnableNetworkProbe() throws Exception {

        Uuid rowUuid = Uuid.of(UUID.randomUUID());

        OperationResult[] wifiStatsConfigEnableNetworkProbe = new OperationResult[] { new InsertResult(rowUuid) };

        Mockito.when(futureResult.get(30L, TimeUnit.SECONDS)).thenReturn(wifiStatsConfigEnableNetworkProbe);

        Mockito.when(ovsdbClient.transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList())).thenReturn(futureResult);

        ovsdbDao.enableNetworkProbeForSyntheticClient(ovsdbClient);

        Mockito.verify(futureResult).get(30L, TimeUnit.SECONDS);

    }

    @Test
    public void testObsdbDaoEnableNetworkProbeError() throws Exception {

        OperationResult[] wifiStatsConfigEnableNetworkProbeFail = new OperationResult[] { new ErrorResult(
                "constraint violation",
                "network_probe is not one of the allowed values ([capacity, client, device, essid, neighbor, quality, radio, rssi, steering, survey])") };

        Mockito.when(futureResult.get(30L, TimeUnit.SECONDS)).thenReturn(wifiStatsConfigEnableNetworkProbeFail);

        Mockito.when(ovsdbClient.transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList())).thenReturn(futureResult);

        ovsdbDao.enableNetworkProbeForSyntheticClient(ovsdbClient);

        Mockito.verify(futureResult).get(30L, TimeUnit.SECONDS);

    }

    @Test(expected = RuntimeException.class)
    public void testObsdbDaoEnableNetworkProbeException() throws Exception {

        Mockito.when(futureResult.get(30L, TimeUnit.SECONDS))
                .thenThrow(new OvsdbClientException("OvsdbClientException"));

        Mockito.when(ovsdbClient.transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList())).thenReturn(futureResult);

        ovsdbDao.enableNetworkProbeForSyntheticClient(ovsdbClient);

    }


}
