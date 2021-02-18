package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.telecominfraproject.wlan.core.model.equipment.EquipmentType;
import com.telecominfraproject.wlan.equipment.models.ApElementConfiguration;
import com.telecominfraproject.wlan.equipment.models.Equipment;
import com.telecominfraproject.wlan.location.models.Location;
import com.telecominfraproject.wlan.location.models.LocationDetails;
import com.telecominfraproject.wlan.location.models.LocationType;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.profile.models.Profile;
import com.telecominfraproject.wlan.profile.models.ProfileType;
import com.telecominfraproject.wlan.profile.network.models.ApNetworkConfiguration;
import com.telecominfraproject.wlan.profile.rf.models.RfConfiguration;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.protocol.schema.DatabaseSchema;
import com.vmware.ovsdb.protocol.schema.TableSchema;
import com.vmware.ovsdb.service.OvsdbClient;

@RunWith(SpringRunner.class)
@ActiveProfiles(profiles = { "integration_test", }) // NOTE: these profiles will
// be ADDED to the list of
// active profiles
@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = OvsdbRadioConfigTest.class)
@Import(value = { OvsdbRadioConfig.class, OvsdbRadioConfigTest.Config.class, OvsdbGet.class,

})
public class OvsdbRadioConfigTest {

    @Autowired
    OvsdbRadioConfig ovsdbRadioConfig;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    OvsdbGet ovsdbGet;

    @Mock(answer = Answers.RETURNS_MOCKS)
    CompletableFuture<OperationResult[]> selectionFutureResult;

    @Mock(answer = Answers.RETURNS_MOCKS)
    CompletableFuture<DatabaseSchema> getSchemaResult;

    @Mock(answer = Answers.RETURNS_MOCKS)
    DatabaseSchema databaseSchema;

    @Mock(answer = Answers.RETURNS_MOCKS)
    Map<String, TableSchema> tableSchemaMap;

    @Mock(answer = Answers.RETURNS_MOCKS)
    TableSchema tableSchema;

    @Mock(answer = Answers.RETURNS_MOCKS)
    OvsdbClient ovsdbClient;

    OpensyncAPConfig opensyncAPConfig;

    @Configuration
    // @PropertySource({ "classpath:persistence-${envTarget:dev}.properties" })
    static class Config {

        @Bean
        public OvsdbRadioConfig ovsdbRadioConfig() {
            return new OvsdbRadioConfig();
        }
    }

    @Before
    public void setUp() throws Exception {

        Mockito.when(selectionFutureResult.get(Mockito.anyLong(), Mockito.eq(TimeUnit.SECONDS)))
                .thenReturn(new OperationResult[0]);
        Mockito.when(ovsdbClient.transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList()))
                .thenReturn(selectionFutureResult);
        Mockito.when(tableSchemaMap.get(Mockito.anyString())).thenReturn(tableSchema);
        Mockito.when(databaseSchema.getTables()).thenReturn(tableSchemaMap);
        Mockito.when(getSchemaResult.get()).thenReturn(databaseSchema);
        Mockito.when(ovsdbClient.getSchema(OvsdbDaoBase.ovsdbName)).thenReturn(getSchemaResult);

        opensyncAPConfig = new OpensyncAPConfig();

        Equipment ce = new Equipment();
        ce.setEquipmentType(EquipmentType.AP);
        ce.setDetails(ApElementConfiguration.createWithDefaults());
        ce.setProfileId(8);
        ce.setLocationId(8L);
        opensyncAPConfig.setCustomerEquipment(ce);

        Location equipmentLocation = new Location();
        equipmentLocation.setCustomerId(2);
        equipmentLocation.setId(8);
        equipmentLocation.setDetails(LocationDetails.createWithDefaults());
        equipmentLocation.setLocationType(LocationType.COUNTRY);
        equipmentLocation.setName("location");
        opensyncAPConfig.setEquipmentLocation(equipmentLocation);

        Profile ssidProfile = new Profile();
        ssidProfile.setId(2);
        ssidProfile.setName("ssid-profile");
        ssidProfile.setProfileType(ProfileType.ssid);
        ssidProfile.setDetails(SsidConfiguration.createWithDefaults());

        opensyncAPConfig.setSsidProfile(List.of(ssidProfile));

        Profile rfProfile = new Profile();
        rfProfile.setId(4);
        rfProfile.setName("rf-profile");
        rfProfile.setDetails(RfConfiguration.createWithDefaults());
        rfProfile.setProfileType(ProfileType.rf);
        opensyncAPConfig.setRfProfile(rfProfile);

        Profile apProfile = new Profile();
        apProfile.setId(8);
        apProfile.setCustomerId(2);
        apProfile.setDetails(ApNetworkConfiguration.createWithDefaults());
        apProfile.setProfileType(ProfileType.equipment_ap);
        apProfile.setName("ap-profile");
        apProfile.setChildProfileIds(Set.of(2L, 4L));
        opensyncAPConfig.setApProfile(apProfile);

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testConfigureWifiRadiosOvsdbClientOpensyncAPConfig() throws Exception {
        ovsdbRadioConfig.configureWifiRadios(ovsdbClient, opensyncAPConfig);
    }

}
