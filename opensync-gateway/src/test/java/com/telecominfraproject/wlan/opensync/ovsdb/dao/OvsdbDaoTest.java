package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
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
import org.springframework.util.ResourceUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.telecominfraproject.wlan.opensync.external.integration.models.ConnectNodeInfo;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPInetState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPRadioState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPVIFState;
import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.jsonrpc.v1.util.JsonUtil;
import com.vmware.ovsdb.protocol.methods.RowUpdate;
import com.vmware.ovsdb.protocol.methods.TableUpdate;
import com.vmware.ovsdb.protocol.methods.TableUpdates;
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

        assertNotNull(connectNodeInfo);
        assert (connectNodeInfo.wifiRadioStates.entrySet().size() == 3);
        assert (connectNodeInfo.firmwareVersion.equals(FW_VERSION));
        assert (connectNodeInfo.redirectorAddr.equals(REDIRECT_ADDR));
        assert (connectNodeInfo.ipV4Address.equals(WAN_IP));

        assert (connectNodeInfo.lanIfName.equals(LAN_IF_NAME));
        assert (connectNodeInfo.ifName.equals(WAN_IF_NAME));

        Mockito.verify(ovsdbClient, Mockito.times(4)).transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList());

    }

    @Test
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
    public void testOvsdbDaoGetOpensyncAPVIFState() throws Exception {

        String path = "src/test/resources/Wifi_VIF_State-home-ap-24.json";

        File file = new File(path);
        String absolutePath = file.getAbsolutePath();
        File jsonFile = ResourceUtils.getFile(absolutePath);

        JsonNode jn = JsonLoader.fromFile(jsonFile);
        Row row = JsonUtil.deserializeNoException(jn.toString(), Row.class);
        RowUpdate rowUpdate = new RowUpdate();
        rowUpdate.setNew(row);

        path = "src/test/resources/Wifi_VIF_State-home-ap-l50.json";
        file = new File(path);
        absolutePath = file.getAbsolutePath();
        jsonFile = ResourceUtils.getFile(absolutePath);
        jn = JsonLoader.fromFile(jsonFile);
        Row row1 = JsonUtil.deserializeNoException(jn.toString(), Row.class);

        RowUpdate rowUpdate1 = new RowUpdate();
        rowUpdate1.setNew(row1);

        path = "src/test/resources/Wifi_VIF_State-home-ap-u50.json";
        file = new File(path);
        absolutePath = file.getAbsolutePath();
        jsonFile = ResourceUtils.getFile(absolutePath);
        jn = JsonLoader.fromFile(jsonFile);
        Row row2 = JsonUtil.deserializeNoException(jn.toString(), Row.class);

        RowUpdate rowUpdate2 = new RowUpdate();
        rowUpdate2.setNew(row2);

        List<OpensyncAPVIFState> vifStateList = ovsdbDao.getOpensyncApVifStateForRowUpdate(rowUpdate, "SomeAPId", ovsdbClient);
        assert (vifStateList.size() == 1);
        vifStateList.addAll(ovsdbDao.getOpensyncApVifStateForRowUpdate(rowUpdate1, "SomeAPId", ovsdbClient));
        assert (vifStateList.size() == 2);
        vifStateList.addAll(ovsdbDao.getOpensyncApVifStateForRowUpdate(rowUpdate2, "SomeAPId", ovsdbClient));
        assert (vifStateList.size() == 3);


    }

    @Test
    public void testOvsdbDaoGetOpensyncAPRadioState() throws Exception {

        String path = "src/test/resources/Wifi_Radio_State-home-ap-24.json";

        File file = new File(path);
        String absolutePath = file.getAbsolutePath();
        File jsonFile = ResourceUtils.getFile(absolutePath);

        JsonNode jn = JsonLoader.fromFile(jsonFile);
        Row row = JsonUtil.deserializeNoException(jn.toString(), Row.class);
        RowUpdate rowUpdate = new RowUpdate();
        rowUpdate.setNew(row);

        path = "src/test/resources/Wifi_Radio_State-home-ap-l50.json";
        file = new File(path);
        absolutePath = file.getAbsolutePath();
        jsonFile = ResourceUtils.getFile(absolutePath);
        jn = JsonLoader.fromFile(jsonFile);
        Row row1 = JsonUtil.deserializeNoException(jn.toString(), Row.class);

        RowUpdate rowUpdate1 = new RowUpdate();
        rowUpdate1.setNew(row1);

        path = "src/test/resources/Wifi_Radio_State-home-ap-u50.json";
        file = new File(path);
        absolutePath = file.getAbsolutePath();
        jsonFile = ResourceUtils.getFile(absolutePath);
        jn = JsonLoader.fromFile(jsonFile);
        Row row2 = JsonUtil.deserializeNoException(jn.toString(), Row.class);

        RowUpdate rowUpdate2 = new RowUpdate();
        rowUpdate2.setNew(row2);

        TableUpdate tableUpdate = new TableUpdate(ImmutableMap.of(row.getUuidColumn("_uuid").getUuid(), rowUpdate,
                row1.getUuidColumn("_uuid").getUuid(), rowUpdate1, row2.getUuidColumn("_uuid").getUuid(), rowUpdate2));
        TableUpdates tableUpdates = new TableUpdates(ImmutableMap.of(OvsdbDao.wifiRadioStateDbTable, tableUpdate));

        List<OpensyncAPRadioState> radioStateList = ovsdbDao.getOpensyncAPRadioState(tableUpdates, "SomeAPId",
                ovsdbClient);
        assert (radioStateList.size() == 3);

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

    @Test
    public void testOvsdbDaoGetOpensyncAPInetState() throws Exception {

        String path = "src/test/resources/Wifi_Inet_State-home-ap-24.json";

        File file = new File(path);
        String absolutePath = file.getAbsolutePath();
        File jsonFile = ResourceUtils.getFile(absolutePath);

        JsonNode jn = JsonLoader.fromFile(jsonFile);
        Row row = JsonUtil.deserializeNoException(jn.toString(), Row.class);
        RowUpdate rowUpdate = new RowUpdate();
        rowUpdate.setNew(row);

        path = "src/test/resources/Wifi_Inet_State-home-ap-l50.json";
        file = new File(path);
        absolutePath = file.getAbsolutePath();
        jsonFile = ResourceUtils.getFile(absolutePath);
        jn = JsonLoader.fromFile(jsonFile);
        Row row1 = JsonUtil.deserializeNoException(jn.toString(), Row.class);

        RowUpdate rowUpdate1 = new RowUpdate();
        rowUpdate1.setNew(row1);

        path = "src/test/resources/Wifi_Inet_State-home-ap-u50.json";
        file = new File(path);
        absolutePath = file.getAbsolutePath();
        jsonFile = ResourceUtils.getFile(absolutePath);
        jn = JsonLoader.fromFile(jsonFile);
        Row row2 = JsonUtil.deserializeNoException(jn.toString(), Row.class);

        RowUpdate rowUpdate2 = new RowUpdate();
        rowUpdate2.setNew(row2);
        
        List<OpensyncAPInetState> inetStateList = ovsdbDao.getOpensyncApInetStateForRowUpdate(rowUpdate, "SomeAPId", ovsdbClient);
        assert (inetStateList.size() == 1);
        inetStateList.addAll(ovsdbDao.getOpensyncApInetStateForRowUpdate(rowUpdate1, "SomeAPId", ovsdbClient));
        assert (inetStateList.size() == 2);
        inetStateList.addAll(ovsdbDao.getOpensyncApInetStateForRowUpdate(rowUpdate2, "SomeAPId", ovsdbClient));
        assert (inetStateList.size() == 3);


    }

}
