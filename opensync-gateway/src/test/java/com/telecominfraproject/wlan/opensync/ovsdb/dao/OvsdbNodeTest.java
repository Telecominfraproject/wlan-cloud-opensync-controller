package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import java.util.HashMap;
import java.util.Map;
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

import com.telecominfraproject.wlan.opensync.external.integration.models.ConnectNodeInfo;
import com.telecominfraproject.wlan.opensync.util.OvsdbToWlanCloudTypeMappingUtility;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.service.OvsdbClient;

@RunWith(SpringRunner.class)
@ActiveProfiles(profiles = { "integration_test", }) // NOTE: these profiles will
// be ADDED to the list of
// active profiles
@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = OvsdbNodeTest.class)
@Import(value = { OvsdbDao.class, OvsdbNodeTest.Config.class, OvsdbNode.class, OvsdbRadioConfig.class,
        OvsdbHotspotConfig.class, OvsdbCommandConfig.class, OvsdbMonitor.class, OvsdbFirmwareConfig.class,
        OvsdbStatsConfig.class, OvsdbSsidConfig.class, OvsdbRrmConfig.class, OvsdbNetworkConfig.class,
        OvsdbNodeConfig.class,OvsdbRadSecConfig.class

})
public class OvsdbNodeTest {

    static final int DEFAULT_CUSTOMER_ID = 1;

    @Mock(answer = Answers.RETURNS_MOCKS)
    OvsdbClient ovsdbClient;

    @Mock(answer = Answers.RETURNS_MOCKS)
    CompletableFuture<OperationResult[]> futureResult;

    @Mock(answer = Answers.RETURNS_MOCKS)
    CompletableFuture<OperationResult[]> selectionFutureResult;

    @Autowired
    OvsdbDao ovsdbDao;

    @Autowired
    OvsdbNode ovsdbNode;
    @Autowired
    OvsdbHotspotConfig ovsdbHotspot;
    @Autowired
    OvsdbSsidConfig ovsdbSsid;
    @Autowired
    OvsdbNetworkConfig ovsdbNetwork;
    @Autowired
    OvsdbRrmConfig ovsdbRrm;
    @Autowired
    OvsdbStatsConfig ovsdbStats;
    @Autowired
    OvsdbRadioConfig ovsdbRadio;
    @Autowired
    OvsdbMonitor ovsdbMonitor;
    @Autowired
    OvsdbFirmwareConfig ovsdbFirmware;
    @Autowired
    OvsdbCommandConfig ovsdbCommand;
    @Autowired
    OvsdbNodeConfig ovsdbNodeConfig;
    @Autowired
    OvsdbRadSecConfig ovsdbRadSecConfig;
    @MockBean(answer = Answers.RETURNS_MOCKS)
    OvsdbGet ovsdbGet;

    MockitoSession mockito;

    @Configuration
    // @PropertySource({ "classpath:persistence-${envTarget:dev}.properties" })
    static class Config {

        @Bean
        public OvsdbNode ovsdbNode() {
            return new OvsdbNode();
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
    public void testUpdateConnectNodeInfoOnConnect() throws Exception {
        ConnectNodeInfo connectNodeInfo = new ConnectNodeInfo();
        connectNodeInfo.ifName = "wan";
        connectNodeInfo.ifType = "bridge";
        connectNodeInfo.lanIfName = "lan";
        connectNodeInfo.lanIfType = "bridge";
        connectNodeInfo.serialNumber = "0123456789";

        Map<String, String> newMqttSettings = new HashMap<>();
        newMqttSettings.put("broker", ovsdbNode.mqttBrokerAddress);
        String mqttClientName = OvsdbToWlanCloudTypeMappingUtility.getAlteredClientCnIfRequired("AP-1", connectNodeInfo,
                false);
        newMqttSettings.put("topics", "/ap/" + mqttClientName + "/opensync");
        newMqttSettings.put("port", "" + ovsdbNode.mqttBrokerExternalPort);
        newMqttSettings.put("compress", "zlib");
        newMqttSettings.put("qos", "0");
        newMqttSettings.put("remote_log", "1");
        assert (!connectNodeInfo.mqttSettings.equals(newMqttSettings));
        ConnectNodeInfo newConnectNodeInfo = ovsdbNode.updateConnectNodeInfoOnConnect(ovsdbClient, "AP-1",
                connectNodeInfo, false);
        assert (connectNodeInfo.ifName.equals(newConnectNodeInfo.ifName));
        assert (newConnectNodeInfo.skuNumber.equals("tip.wlan_" + connectNodeInfo.serialNumber));
        assert (newConnectNodeInfo.mqttSettings.equals(newMqttSettings));
    }

}
