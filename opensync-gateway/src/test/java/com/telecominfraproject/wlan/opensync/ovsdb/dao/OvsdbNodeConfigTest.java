package com.telecominfraproject.wlan.opensync.ovsdb.dao;

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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.profile.models.Profile;
import com.telecominfraproject.wlan.profile.models.ProfileType;
import com.telecominfraproject.wlan.profile.network.models.ApNetworkConfiguration;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.protocol.operation.result.UpdateResult;
import com.vmware.ovsdb.service.OvsdbClient;

@RunWith(SpringRunner.class)
@ActiveProfiles(profiles = { "integration_test", })
@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = OvsdbNodeConfigTest.class)
@Import(value = { OvsdbNodeConfig.class, OvsdbNodeConfigTest.Config.class,

})
public class OvsdbNodeConfigTest {

    @Mock(answer = Answers.RETURNS_MOCKS)
    OvsdbClient ovsdbClient;

    @MockBean(answer = Answers.RETURNS_MOCKS)
    OvsdbGet ovsdbGet;

    @Mock(answer = Answers.RETURNS_MOCKS)
    CompletableFuture<OperationResult[]> futureResult;

    @Autowired
    OvsdbNodeConfig ovsdbNodeConfig;

    private MockitoSession mockito;

    @Configuration
    // @PropertySource({ "classpath:persistence-${envTarget:dev}.properties" })
    static class Config {
        @Bean
        public OvsdbNodeConfig ovsdbNodeConfig() {
            return new OvsdbNodeConfig();
        }
    }

    @Before
    public void setUp() throws Exception {
        mockito = Mockito.mockitoSession().initMocks(this).strictness(Strictness.STRICT_STUBS).startMocking();
        Mockito.when(ovsdbClient.transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList())).thenReturn(futureResult);
    }

    @After
    public void tearDown() throws Exception {
        mockito.finishMocking();
    }

    @Test
    public void testConfigureNtpServer() throws Exception {
        OpensyncAPConfig opensyncApConfig = new OpensyncAPConfig();
        Profile apProfile = new Profile();
        apProfile.setId(1L);
        apProfile.setName("ap-profile");
        apProfile.setProfileType(ProfileType.equipment_ap);
        apProfile.setCustomerId(2);
        apProfile.setDetails(ApNetworkConfiguration.createWithDefaults());
        opensyncApConfig.setApProfile(apProfile);
        OperationResult[] updateNtpServerResult = new OperationResult[] { new UpdateResult(1) };
        Mockito.when(futureResult.get(ovsdbNodeConfig.ovsdbTimeoutSec, TimeUnit.SECONDS))
                .thenReturn(updateNtpServerResult);
        ovsdbNodeConfig.configureNtpServer(ovsdbClient, opensyncApConfig);
        Mockito.verify(futureResult).get(ovsdbNodeConfig.ovsdbTimeoutSec, TimeUnit.SECONDS);
        Mockito.verify(ovsdbClient).transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList());
    }

}
