package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import com.telecominfraproject.wlan.equipment.models.Equipment;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.profile.ethernetport.models.WiredEthernetPortConfiguration;
import com.telecominfraproject.wlan.profile.ethernetport.models.WiredPort;
import com.telecominfraproject.wlan.profile.models.Profile;
import com.telecominfraproject.wlan.profile.models.ProfileType;
import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.protocol.operation.result.UpdateResult;
import com.vmware.ovsdb.service.OvsdbClient;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.times;

/**
 * @author rsharma
 */

@RunWith(SpringRunner.class)
@ActiveProfiles(profiles = { "integration_test", })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = OvsdbNetworkConfigTest.class)
@Import(value = { OvsdbNetworkConfig.class

})
public class OvsdbNetworkConfigTest {

    @Mock(answer = Answers.RETURNS_MOCKS)
    OvsdbClient ovsdbClient;

    @Autowired
    OvsdbNetworkConfig ovsdbNetworkConfig;

    @MockBean(answer = Answers.RETURNS_MOCKS)
    OvsdbGet ovsdbGet;

    @Mock(answer = Answers.RETURNS_MOCKS)
    CompletableFuture<OperationResult[]> futureResult;

    private MockitoSession mockito;

    @Before
    public void setUp() throws Exception {
        mockito = Mockito.mockitoSession().initMocks(this).strictness(Strictness.STRICT_STUBS).startMocking();
        Mockito.when(ovsdbClient.transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList())).thenReturn(futureResult);
    }

    @After
    public void tearDown() throws Exception {
        mockito.finishMocking();
    }

    // Eth0 port is in LAN mode and we need it to convert to eth0_trunk
    @Test
    public void testConfigureEthernetPorts_BridgeTrunkMode() throws ExecutionException, InterruptedException, TimeoutException, OvsdbClientException {
        OperationResult[] updateResult = new OperationResult[] { new UpdateResult(1) };
        Mockito.when(futureResult.get(ovsdbNetworkConfig.ovsdbTimeoutSec, TimeUnit.SECONDS))
                .thenReturn(updateResult);

        ovsdbNetworkConfig.configureEthernetPorts(ovsdbClient, createOpensyncApConfig(true, "bridge"));

        Mockito.verify(ovsdbClient, times(2)).transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList());
    }

    // Other tests that are useful to consider -- NOT anymore with GW deleting existing interfaces
    // Case when ETH port is in eth0_trunk mode and no changes are required
    // Case when ETH port is already in eth0_vlan100 and no changes are required

    // Case when ETH port is in LAN mode and we convert it into BRIDGE + VlanId
    @Test
    public void testConfigureEthernetPorts_BridgeAccessMode() throws ExecutionException, InterruptedException, TimeoutException, OvsdbClientException {
        OperationResult[] updateResult = new OperationResult[] { new UpdateResult(1) };
        Mockito.when(futureResult.get(ovsdbNetworkConfig.ovsdbTimeoutSec, TimeUnit.SECONDS))
                .thenReturn(updateResult);

        ovsdbNetworkConfig.configureEthernetPorts(ovsdbClient, createOpensyncApConfig(false, "bridge"));

        Mockito.verify(ovsdbClient, times(2)).transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList());
    }

    // Case when ETH port is in BRIDGE + VlanId and we convert it into NAT (LAN)
    @Test
    public void testConfigureEthernetPorts_BridgeAccessToNATConversion() throws ExecutionException, InterruptedException, TimeoutException, OvsdbClientException {
        OperationResult[] updateResult = new OperationResult[] { new UpdateResult(1) };
        Mockito.when(futureResult.get(ovsdbNetworkConfig.ovsdbTimeoutSec, TimeUnit.SECONDS))
                .thenReturn(updateResult);

        ovsdbNetworkConfig.configureEthernetPorts(ovsdbClient, createOpensyncApConfig(false, "NAT"));

        Mockito.verify(ovsdbClient, times(1)).transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList());
    }

    private OpensyncAPConfig createOpensyncApConfig(boolean isTrunkEnabled, String ifType) {
        OpensyncAPConfig opensyncApConfig = new OpensyncAPConfig();

        Equipment customerEquip = new Equipment();
        customerEquip.setCustomerId(1);
        customerEquip.setId(1L);
        opensyncApConfig.setCustomerEquipment(customerEquip);
        opensyncApConfig.setWiredEthernetPortProfile(createWiredEthernetPortProfile(isTrunkEnabled, ifType));

        return opensyncApConfig;
    }

    private Profile createWiredEthernetPortProfile(boolean isTrunkEnabled, String ifType) {
        WiredPort portLan = createWiredPort("lan", ifType, isTrunkEnabled, 10, List.of(100, 200), "eth0", "ETHERNET");
        WiredPort portWan = createWiredPort("wan", "bridge", false, 0, null, "eth1", "INTERNET");

        WiredEthernetPortConfiguration details = new WiredEthernetPortConfiguration();
        details.setEquipmentModel("EA8300");
        details.setEthPorts(Set.of(portLan, portWan));

        Profile wiredEthernetPortProfile = new Profile();
        wiredEthernetPortProfile.setCustomerId(1);
        wiredEthernetPortProfile.setProfileType(ProfileType.wired_ethernet_port);
        wiredEthernetPortProfile.setId(10L);
        wiredEthernetPortProfile.setName("WiredProfileForTest");
        wiredEthernetPortProfile.setCreatedTimestamp(1634060798000L);
        wiredEthernetPortProfile.setLastModifiedTimestamp(1634060798000L);
        wiredEthernetPortProfile.setDetails(details);

        return wiredEthernetPortProfile;
    }

    private WiredPort createWiredPort(String ifName, String ifType, boolean isTrunkEnabled, int vlanId, List<Integer> allowedVlans,
                                      String name, String displayName) {
        WiredPort wiredPort = new WiredPort();
        wiredPort.setIfName(ifName);
        wiredPort.setIfType(ifType);
        wiredPort.setTrunkEnabled(isTrunkEnabled);
        wiredPort.setVlanId(vlanId);
        wiredPort.setAllowedVlanIds(allowedVlans);
        wiredPort.setName(name);
        wiredPort.setDisplayName(displayName);

        return wiredPort;
    }
}