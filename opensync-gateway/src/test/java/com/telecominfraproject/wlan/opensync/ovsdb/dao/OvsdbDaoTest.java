
package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.location.models.Location;
import com.telecominfraproject.wlan.opensync.external.integration.models.ConnectNodeInfo;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPHotspot20Config;
import com.telecominfraproject.wlan.profile.models.Profile;
import com.telecominfraproject.wlan.profile.models.ProfileType;
import com.telecominfraproject.wlan.profile.network.models.ApNetworkConfiguration;
import com.telecominfraproject.wlan.profile.network.models.GreTunnelConfiguration;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration;
import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.protocol.operation.notation.Atom;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;
import com.vmware.ovsdb.protocol.operation.notation.Value;
import com.vmware.ovsdb.protocol.operation.result.ErrorResult;
import com.vmware.ovsdb.protocol.operation.result.InsertResult;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.protocol.operation.result.SelectResult;
import com.vmware.ovsdb.protocol.operation.result.UpdateResult;
import com.vmware.ovsdb.protocol.schema.DatabaseSchema;
import com.vmware.ovsdb.protocol.schema.TableSchema;
import com.vmware.ovsdb.service.OvsdbClient;

@RunWith(SpringRunner.class)
@ActiveProfiles(profiles = {"integration_test",}) // NOTE: these profiles will
// be ADDED to the list of
// active profiles
@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = OvsdbDaoTest.class)
@Import(
        value = {OvsdbDao.class, OvsdbDaoTest.Config.class, OvsdbNode.class, OvsdbRadioConfig.class, OvsdbHotspotConfig.class, OvsdbCommandConfig.class,
                OvsdbMonitor.class, OvsdbFirmwareConfig.class, OvsdbStatsConfig.class, OvsdbSsidConfig.class, OvsdbRrmConfig.class, OvsdbNetworkConfig.class,
                OvsdbNodeConfig.class, OvsdbRadiusProxyConfig.class

        })
public class OvsdbDaoTest {

    static final int DEFAULT_CUSTOMER_ID = 1;

    private static final long HOTSPOT_CONFIG_ID = 1;
    private static final long HOTSPOT_PROVIDER_ID_1 = 2;
    private static final long HOTSPOT_PROVIDER_ID_2 = 3;
    private static final long SSID_PSK_ID = 4;
    private static final long SSID_OSU_ID = 5;
    private static final long OPERATOR_ID = 6;
    private static final long VENUE_ID = 7;
    private static final long EQUIPMENT_AP_ID = 8;
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
    OvsdbRadiusProxyConfig ovsdbRadiusProxyConfig;

    @MockBean(answer = Answers.RETURNS_MOCKS)
    OvsdbGet ovsdbGet;

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
    public void testConfigureGreTunnels() throws Exception {
        List<Row> rows = new ArrayList<>();
        OperationResult[] operationResult = new OperationResult[] {new SelectResult(rows)};
        Mockito.when(ovsdbClient.transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList())).thenReturn(selectionFutureResult);
        Mockito.when(selectionFutureResult.get(30, TimeUnit.SECONDS)).thenReturn(operationResult);
        Profile apProfile = new Profile();
        apProfile.setCustomerId(2);
        apProfile.setId(1L);
        apProfile.setName("ApProfile");
        apProfile.setProfileType(ProfileType.equipment_ap);
        ApNetworkConfiguration tunnelProfileDetails = ApNetworkConfiguration.createWithDefaults();
        GreTunnelConfiguration greTunnelConfiguration = GreTunnelConfiguration.createWithDefaults();
        greTunnelConfiguration.setGreRemoteInetAddr(InetAddress.getByName("192.168.0.10"));
        greTunnelConfiguration.setGreTunnelName("gre");
        greTunnelConfiguration.setVlanIdsInGreTunnel(Set.of(Integer.valueOf(100)));
        Set<GreTunnelConfiguration> greTunnels = Set.of(greTunnelConfiguration);
        tunnelProfileDetails.setGreTunnelConfigurations(greTunnels);
        apProfile.setDetails(tunnelProfileDetails);

        OpensyncAPConfig apConfig = Mockito.mock(OpensyncAPConfig.class);
        Mockito.when(apConfig.getApProfile()).thenReturn(apProfile);
        ovsdbDao.configureGreTunnels(ovsdbClient, apConfig);
        Mockito.verify(ovsdbClient, Mockito.times(1)).transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList());
        Mockito.verify(apConfig, Mockito.times(3)).getApProfile();

    }

    @Test
    public void testGetRadiusAccountingConfigurationNoAcctInterval() throws Exception {
        OpensyncAPConfig apConfig = new OpensyncAPConfig();
        Profile profileRadius = OvsdbDaoTestUtilities.createRadiusProfile(DEFAULT_CUSTOMER_ID);
        apConfig.setRadiusProfiles(List.of(profileRadius));
        SsidConfiguration ssidConfig = SsidConfiguration.createWithDefaults();
        ssidConfig.setRadiusServiceId(OvsdbDaoTestUtilities.RADIUS_PROFILE_ID);
        Map<String, String> security = new HashMap<>();
        Location location = new Location();
        location.setName("Ottawa");
        apConfig.setEquipmentLocation(location);
        ovsdbDao.getRadiusAccountingConfiguration(apConfig, ssidConfig, security);

        assertNull (security.get("radius_acct_interval"));
        assert (security.get("radius_acct_ip").equals("192.168.0.1"));
        assert (security.get("radius_acct_port").equals("1813"));
        assert (security.get("radius_acct_secret").equals("secret"));
    }

    @Ignore
    public void testConfigureHotspots() throws Exception {

        // TODO: needs refactoring.

        DatabaseSchema schemaMock = Mockito.mock(DatabaseSchema.class);
        CompletableFuture<DatabaseSchema> schemaFuture = Mockito.mock(CompletableFuture.class);
        Mockito.when(schemaFuture.get(Mockito.anyLong(), Mockito.any())).thenReturn(schemaMock);
        HashMap<String, TableSchema> mapMock = Mockito.mock(HashMap.class);

        Mockito.when(mapMock.containsKey(Mockito.any())).thenReturn(true);
        Mockito.when(mapMock.get(Mockito.any())).thenReturn(Mockito.mock(TableSchema.class));
        Mockito.when(schemaMock.getTables()).thenReturn(mapMock);
        Mockito.when(ovsdbClient.getSchema(Mockito.anyString())).thenReturn(schemaFuture);

        OpensyncAPConfig apConfig = new OpensyncAPConfig();
        OpensyncAPHotspot20Config hsConfig = new OpensyncAPHotspot20Config();

        Profile profileSsidPsk = OvsdbDaoTestUtilities.createPasspointAccessSsid(DEFAULT_CUSTOMER_ID);
        profileSsidPsk.setId(SSID_PSK_ID);
        Profile profileSsidOsu = OvsdbDaoTestUtilities.createPasspointOsuSsid(DEFAULT_CUSTOMER_ID);
        profileSsidOsu.setId(SSID_OSU_ID);
        Profile passpointOperatorProfile = OvsdbDaoTestUtilities.createPasspointOperatorProfile(DEFAULT_CUSTOMER_ID);
        passpointOperatorProfile.setId(OPERATOR_ID);
        Profile passpointVenueProfile = OvsdbDaoTestUtilities.createPasspointVenueProfile(DEFAULT_CUSTOMER_ID);
        passpointVenueProfile.setId(VENUE_ID);
        Profile hotspot20IdProviderProfile = new Profile();
        hotspot20IdProviderProfile.setId(HOTSPOT_PROVIDER_ID_1);
        hotspot20IdProviderProfile = OvsdbDaoTestUtilities.createPasspointIdProviderProfile(DEFAULT_CUSTOMER_ID, hotspot20IdProviderProfile,
                "TipWlan-Hotspot20-OSU-Provider", "Rogers AT&T Wireless", "Canada", "ca", 302, 720, "rogers.com", 1);
        Profile hotspot20IdProviderProfile2 = new Profile();
        hotspot20IdProviderProfile2.setId(HOTSPOT_PROVIDER_ID_2);
        hotspot20IdProviderProfile2 = OvsdbDaoTestUtilities.createPasspointIdProviderProfile(DEFAULT_CUSTOMER_ID, hotspot20IdProviderProfile2,
                "TipWlan-Hotspot20-OSU-Provider-2", "Telus Mobility", "Canada", "ca", 302, 220, "telus.com", 1);

        profileSsidOsu.getChildProfileIds().add(hotspot20IdProviderProfile.getId());
        profileSsidOsu.getChildProfileIds().add(hotspot20IdProviderProfile2.getId());

        Profile passpointHotspotConfig = OvsdbDaoTestUtilities.createPasspointHotspotConfig(DEFAULT_CUSTOMER_ID, hotspot20IdProviderProfile2,
                hotspot20IdProviderProfile, passpointOperatorProfile, passpointVenueProfile, profileSsidPsk, profileSsidOsu);
        passpointHotspotConfig.setId(HOTSPOT_CONFIG_ID);

        Profile hotspotProfileAp = OvsdbDaoTestUtilities.createPasspointApProfile(DEFAULT_CUSTOMER_ID, profileSsidPsk, profileSsidOsu);
        hotspotProfileAp.setId(EQUIPMENT_AP_ID);

        hsConfig.setHotspot20OperatorSet(Set.of(passpointOperatorProfile));
        hsConfig.setHotspot20ProviderSet(Set.of(hotspot20IdProviderProfile, hotspot20IdProviderProfile2));
        hsConfig.setHotspot20VenueSet(Set.of(passpointVenueProfile));
        hsConfig.setHotspot20ProfileSet(Set.of(passpointHotspotConfig));

        apConfig.setHotspotConfig(hsConfig);

        apConfig.setApProfile(hotspotProfileAp);

        apConfig.setSsidProfile(List.of(profileSsidOsu, profileSsidPsk));

        Mockito.when(futureResult.get(Mockito.anyLong(), Mockito.eq(TimeUnit.SECONDS))).thenReturn(OvsdbDaoTestUtilities.hs20IconRows())
                .thenReturn(OvsdbDaoTestUtilities.hs20InsertIconRows()).thenReturn(OvsdbDaoTestUtilities.hs20OsuProviders())
                .thenReturn(OvsdbDaoTestUtilities.hs20IconRows()).thenReturn(OvsdbDaoTestUtilities.hs20IconRows())
                .thenReturn(OvsdbDaoTestUtilities.hs20InsertProviderRows()).thenReturn(OvsdbDaoTestUtilities.hs20Config())
                .thenReturn(OvsdbDaoTestUtilities.hs20OsuProviders()).thenReturn(OvsdbDaoTestUtilities.vifConfigRows())
                .thenReturn(OvsdbDaoTestUtilities.vifConfigRows()).thenReturn(OvsdbDaoTestUtilities.hs20Config());

        Mockito.when(ovsdbClient.transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList())).thenReturn(futureResult);

        ovsdbDao.configureHotspots(ovsdbClient, apConfig);

        Mockito.verify(futureResult, Mockito.times(13)).get(Mockito.anyLong(), Mockito.eq(TimeUnit.SECONDS));

    }

    @Test
    public void testConfigureGreTunnelsWithNoRemoteAddress() throws Exception {
        Profile apProfile = new Profile();
        apProfile.setCustomerId(2);
        apProfile.setId(1L);
        apProfile.setName("ApProfile");
        apProfile.setProfileType(ProfileType.equipment_ap);
        ApNetworkConfiguration tunnelProfileDetails = ApNetworkConfiguration.createWithDefaults();

        GreTunnelConfiguration greTunnelConfiguration = GreTunnelConfiguration.createWithDefaults();
        greTunnelConfiguration.setGreRemoteInetAddr(null);
        greTunnelConfiguration.setGreTunnelName("gre");
        greTunnelConfiguration.setVlanIdsInGreTunnel(Set.of(Integer.valueOf(100)));
        Set<GreTunnelConfiguration> greTunnels = Set.of(greTunnelConfiguration);
        tunnelProfileDetails.setGreTunnelConfigurations(greTunnels);
        apProfile.setDetails(tunnelProfileDetails);

        OpensyncAPConfig apConfig = Mockito.mock(OpensyncAPConfig.class);
        Mockito.when(apConfig.getApProfile()).thenReturn(apProfile);
        ovsdbDao.configureGreTunnels(ovsdbClient, apConfig);
        // Should not create
        Mockito.verify(ovsdbClient, Mockito.times(0)).transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList());
        Mockito.verify(apConfig, Mockito.times(3)).getApProfile();

    }

    @Test
    public void testConfigureGreTunnelsWithNoTunnelName() throws Exception {
        Profile apProfile = new Profile();
        apProfile.setCustomerId(2);
        apProfile.setId(1L);
        apProfile.setName("ApProfile");
        apProfile.setProfileType(ProfileType.equipment_ap);
        ApNetworkConfiguration tunnelProfileDetails = ApNetworkConfiguration.createWithDefaults();

        GreTunnelConfiguration greTunnelConfiguration = GreTunnelConfiguration.createWithDefaults();
        greTunnelConfiguration.setGreRemoteInetAddr(InetAddress.getByName("192.168.0.10"));
        greTunnelConfiguration.setGreTunnelName(null);
        greTunnelConfiguration.setVlanIdsInGreTunnel(Set.of(Integer.valueOf(100)));
        Set<GreTunnelConfiguration> greTunnels = Set.of(greTunnelConfiguration);
        tunnelProfileDetails.setGreTunnelConfigurations(greTunnels);
        apProfile.setDetails(tunnelProfileDetails);

        OpensyncAPConfig apConfig = Mockito.mock(OpensyncAPConfig.class);
        Mockito.when(apConfig.getApProfile()).thenReturn(apProfile);
        ovsdbDao.configureGreTunnels(ovsdbClient, apConfig);
        // Should not create
        Mockito.verify(ovsdbClient, Mockito.times(0)).transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList());
        Mockito.verify(apConfig, Mockito.times(3)).getApProfile();

    }

    @Test
    public void testObsdbDaoEnableNetworkProbe() throws Exception {

        Uuid rowUuid = Uuid.of(UUID.randomUUID());

        OperationResult[] wifiStatsConfigEnableNetworkProbe = new OperationResult[] {new InsertResult(rowUuid)};

        Mockito.when(futureResult.get(30L, TimeUnit.SECONDS)).thenReturn(wifiStatsConfigEnableNetworkProbe);

        Mockito.when(ovsdbClient.transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList())).thenReturn(futureResult);

        ovsdbDao.enableNetworkProbeForSyntheticClient(ovsdbClient);

        Mockito.verify(futureResult).get(30L, TimeUnit.SECONDS);

    }

    @Test
    public void testObsdbDaoEnableNetworkProbeError() throws Exception {

        OperationResult[] wifiStatsConfigEnableNetworkProbeFail = new OperationResult[] {new ErrorResult("constraint violation",
                "network_probe is not one of the allowed values ([capacity, client, device, essid, neighbor, quality, radio, rssi, steering, survey])")};

        Mockito.when(futureResult.get(30L, TimeUnit.SECONDS)).thenReturn(wifiStatsConfigEnableNetworkProbeFail);

        Mockito.when(ovsdbClient.transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList())).thenReturn(futureResult);

        ovsdbDao.enableNetworkProbeForSyntheticClient(ovsdbClient);

        Mockito.verify(futureResult).get(30L, TimeUnit.SECONDS);

    }

    @Test
    public void testProcessNewChannelsRequest() throws Exception {

        OperationResult[] testProcessNewChannelsRequestResult = new OperationResult[] {new UpdateResult(1), new UpdateResult(1), new UpdateResult(1),
                new UpdateResult(1), new UpdateResult(1), new UpdateResult(1)};

        Mockito.when(futureResult.get(30L, TimeUnit.SECONDS)).thenReturn(testProcessNewChannelsRequestResult);

        Mockito.when(ovsdbClient.transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList())).thenReturn(futureResult);

        ovsdbDao.processNewChannelsRequest(ovsdbClient,
                Map.of(RadioType.is2dot4GHz, Integer.valueOf(1), RadioType.is5GHzL, Integer.valueOf(40), RadioType.is5GHzU, Integer.valueOf(153)),
                Map.of(RadioType.is2dot4GHz, Integer.valueOf(6), RadioType.is5GHzL, Integer.valueOf(36), RadioType.is5GHzU, Integer.valueOf(149)));

        Mockito.verify(futureResult).get(30L, TimeUnit.SECONDS);

    }

    @Test(expected = RuntimeException.class)
    public void testObsdbDaoEnableNetworkProbeException() throws Exception {

        Mockito.when(futureResult.get(30L, TimeUnit.SECONDS)).thenThrow(new OvsdbClientException("OvsdbClientException"));

        Mockito.when(ovsdbClient.transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList())).thenReturn(futureResult);

        ovsdbDao.enableNetworkProbeForSyntheticClient(ovsdbClient);

    }

    static Row[] hs20Icons = {

    };
}
