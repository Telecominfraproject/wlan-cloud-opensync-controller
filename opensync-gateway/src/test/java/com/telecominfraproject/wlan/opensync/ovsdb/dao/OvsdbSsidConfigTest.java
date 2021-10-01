package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import java.util.HashMap;
import java.util.List;
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

import com.telecominfraproject.wlan.location.models.Location;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.profile.models.Profile;
import com.telecominfraproject.wlan.profile.models.ProfileType;
import com.telecominfraproject.wlan.profile.radius.models.RadiusProfile;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.service.OvsdbClient;

@RunWith(SpringRunner.class)
@ActiveProfiles(profiles = { "integration_test", }) // NOTE: these profiles will
// be ADDED to the list of
// active profiles
@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = OvsdbSsidConfigTest.class)
@Import(value = { OvsdbDao.class, OvsdbSsidConfigTest.Config.class, OvsdbNode.class, OvsdbRadioConfig.class,
        OvsdbHotspotConfig.class, OvsdbCommandConfig.class, OvsdbMonitor.class, OvsdbFirmwareConfig.class,
        OvsdbStatsConfig.class, OvsdbSsidConfig.class, OvsdbRrmConfig.class, OvsdbNetworkConfig.class,
        OvsdbNodeConfig.class,OvsdbRadiusProxyConfig.class
})
public class OvsdbSsidConfigTest {

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
    public void testGetRadiusConfiguration() throws Exception {
        OpensyncAPConfig apConfig = new OpensyncAPConfig();
        Profile profileRadius = OvsdbDaoTestUtilities.createRadiusProfile(DEFAULT_CUSTOMER_ID);
        apConfig.setRadiusProfiles(List.of(profileRadius));
        Profile ssidProfile = new Profile();
        SsidConfiguration ssidConfig = SsidConfiguration.createWithDefaults();
        ssidConfig.setRadiusServiceId(profileRadius.getId());
        ssidProfile.setDetails(ssidConfig);
        ssidProfile.getChildProfileIds().add(profileRadius.getId());
        apConfig.setSsidProfile(List.of(ssidProfile));
        Map<String, String> security = new HashMap<>();
        Location location = new Location();
        location.setName("Ottawa");
        apConfig.setEquipmentLocation(location);
        ovsdbSsid.getRadiusConfiguration(apConfig, ssidConfig, security);
        assert (security.get("radius_server_ip").equals("192.168.0.1"));
        assert (security.get("radius_server_port").equals(String.valueOf(RadiusProfile.DEFAULT_RADIUS_AUTH_PORT)));
        assert (security.get("radius_server_secret").equals(RadiusProfile.DEFAULT_RADIUS_SECRET));
    }

    @Test
    public void testGetRadiusAccountingConfiguration() throws Exception {
        OpensyncAPConfig apConfig = new OpensyncAPConfig();
        Profile profileRadius = OvsdbDaoTestUtilities.createRadiusProfile(DEFAULT_CUSTOMER_ID);
        apConfig.setRadiusProfiles(List.of(profileRadius));
        Profile ssidProfile = new Profile();
        ssidProfile.setCustomerId(DEFAULT_CUSTOMER_ID);
        ssidProfile.setName("SsidProfile");
        ssidProfile.setProfileType(ProfileType.ssid);
        SsidConfiguration ssidConfig = SsidConfiguration.createWithDefaults();

        ssidConfig.setRadiusServiceId(OvsdbDaoTestUtilities.RADIUS_PROFILE_ID);
        ssidConfig.setRadiusAcountingServiceInterval(60);
        ssidProfile.setDetails(ssidConfig);
        apConfig.setSsidProfile(List.of(ssidProfile));
        Map<String, String> security = new HashMap<>();
        Location location = new Location();
        location.setName("Ottawa");
        apConfig.setEquipmentLocation(location);
        ovsdbSsid.getRadiusAccountingConfiguration(apConfig, ssidConfig, security);
        assert (Integer.valueOf(security.get("radius_acct_interval"))
                .equals(ssidConfig.getRadiusAcountingServiceInterval()));
        assert (security.get("radius_acct_ip").equals("192.168.0.1"));
        assert (security.get("radius_acct_port").equals("1813"));
        assert (security.get("radius_acct_secret").equals("secret"));
    }
}
