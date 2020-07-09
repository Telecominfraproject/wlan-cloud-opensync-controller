package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.List;
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
import com.vmware.ovsdb.protocol.operation.notation.Atom;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Value;
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

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    CompletableFuture<OperationResult[]> futureResult;

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

        System.out.println("ConnectNodeInfo " + connectNodeInfo);
        assertNotNull(connectNodeInfo);
        assert (connectNodeInfo.wifiRadioStates.entrySet().size() == 3);
        assert (connectNodeInfo.firmwareVersion.equals(FW_VERSION));
        assert (connectNodeInfo.redirectorAddr.equals(REDIRECT_ADDR));
        assert (connectNodeInfo.ipV4Address.equals(WAN_IP));

        assert (connectNodeInfo.lanIfName.equals(LAN_IF_NAME));
        assert (connectNodeInfo.ifName.equals(WAN_IF_NAME));

        Mockito.verify(ovsdbClient, Mockito.times(4)).transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList());

    }

}
