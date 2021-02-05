package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import java.util.Map;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.telecominfraproject.wlan.core.model.entity.CountryCode;
import com.telecominfraproject.wlan.equipment.models.ApElementConfiguration;
import com.telecominfraproject.wlan.equipment.models.Equipment;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.profile.models.Profile;
import com.telecominfraproject.wlan.profile.rf.models.RfConfiguration;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.protocol.schema.DatabaseSchema;
import com.vmware.ovsdb.protocol.schema.TableSchema;
import com.vmware.ovsdb.service.OvsdbClient;


@RunWith(SpringRunner.class)
@ActiveProfiles(profiles = { "integration_test", }) // NOTE: these profiles will
// be ADDED to the list of
// active profiles
@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = OvsdbRadioConfigTest.class)
@Import(value = { OvsdbRadioConfig.class,OvsdbRadioConfigTest.Config.class,

})
public class OvsdbRadioConfigTest {

    @Autowired
    OvsdbRadioConfig ovsdbRadioConfig;
    
    @Mock(answer = Answers.RETURNS_MOCKS)
    CompletableFuture<OperationResult[]> selectionFutureResult;
    
    @Mock(answer = Answers.RETURNS_MOCKS)
    CompletableFuture<DatabaseSchema> getSchemaResult;
    
    @Mock(answer = Answers.RETURNS_MOCKS)
    DatabaseSchema databaseSchema;
    
    @Mock(answer = Answers.RETURNS_MOCKS)
    Map<String,TableSchema> tableSchemaMap;
    
    @Mock(answer = Answers.RETURNS_MOCKS)
    TableSchema tableSchema;
    
    @Mock(answer = Answers.RETURNS_MOCKS)
    OvsdbClient ovsdbClient;
    
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
        Mockito.when(selectionFutureResult.get(Mockito.anyLong(), Mockito.eq(TimeUnit.SECONDS))).thenReturn(new OperationResult[0]);
        Mockito.when(ovsdbClient.transact(Mockito.eq(OvsdbDao.ovsdbName), Mockito.anyList()))
        .thenReturn(selectionFutureResult);
        Mockito.when(tableSchemaMap.get(Mockito.anyString())).thenReturn(tableSchema);
        Mockito.when(databaseSchema.getTables()).thenReturn(tableSchemaMap);
        Mockito.when(getSchemaResult.get()).thenReturn(databaseSchema);
        Mockito.when(ovsdbClient.getSchema(OvsdbDaoBase.ovsdbName)).thenReturn(getSchemaResult);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testConfigureWifiRadiosOvsdbClientOpensyncAPConfig() throws Exception {
        OpensyncAPConfig opensyncAPConfig = constructOpensyncAPConfig();
        ovsdbRadioConfig.configureWifiRadios(ovsdbClient, opensyncAPConfig);
    }

    OpensyncAPConfig constructOpensyncAPConfig() {
        ApElementConfiguration equipmentDetails = Mockito.mock(ApElementConfiguration.class, Mockito.RETURNS_MOCKS);
        RfConfiguration rfConfig = Mockito.mock(RfConfiguration.class, Mockito.RETURNS_MOCKS);
        Profile rfProfile = Mockito.mock(Profile.class, Mockito.RETURNS_MOCKS);
        Mockito.when(rfProfile.getDetails()).thenReturn(rfConfig);
        Equipment equipment = Mockito.mock(Equipment.class);
        Mockito.when(equipment.getDetails()).thenReturn(equipmentDetails);
        OpensyncAPConfig opensyncAPConfig = Mockito.mock(OpensyncAPConfig.class, Mockito.RETURNS_MOCKS);
        Mockito.when(opensyncAPConfig.getCountryCode()).thenReturn(CountryCode.CA.getName());
        Mockito.when(opensyncAPConfig.getCustomerEquipment()).thenReturn(equipment);
        Mockito.when(opensyncAPConfig.getRfProfile()).thenReturn(rfProfile);
        return opensyncAPConfig;
    }

}
