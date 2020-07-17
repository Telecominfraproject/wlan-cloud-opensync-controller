package com.telecominfraproject.wlan.opensync.external.integration;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.ByteString;
import com.telecominfraproject.wlan.alarm.AlarmServiceInterface;
import com.telecominfraproject.wlan.client.ClientServiceInterface;
import com.telecominfraproject.wlan.client.info.models.ClientInfoDetails;
import com.telecominfraproject.wlan.client.session.models.ClientSession;
import com.telecominfraproject.wlan.client.session.models.ClientSessionDetails;
import com.telecominfraproject.wlan.cloudeventdispatcher.CloudEventDispatcherInterface;
import com.telecominfraproject.wlan.core.model.entity.CountryCode;
import com.telecominfraproject.wlan.core.model.equipment.EquipmentType;
import com.telecominfraproject.wlan.core.model.equipment.MacAddress;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.customer.models.Customer;
import com.telecominfraproject.wlan.customer.models.CustomerDetails;
import com.telecominfraproject.wlan.customer.models.EquipmentAutoProvisioningSettings;
import com.telecominfraproject.wlan.customer.service.CustomerServiceInterface;
import com.telecominfraproject.wlan.equipment.EquipmentServiceInterface;
import com.telecominfraproject.wlan.equipment.models.ApElementConfiguration;
import com.telecominfraproject.wlan.equipment.models.Equipment;
import com.telecominfraproject.wlan.firmware.FirmwareServiceInterface;
import com.telecominfraproject.wlan.firmware.models.CustomerFirmwareTrackRecord;
import com.telecominfraproject.wlan.firmware.models.CustomerFirmwareTrackSettings;
import com.telecominfraproject.wlan.location.service.LocationServiceInterface;
import com.telecominfraproject.wlan.opensync.external.integration.controller.OpensyncCloudGatewayController;
import com.telecominfraproject.wlan.opensync.external.integration.models.ConnectNodeInfo;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPRadioState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPVIFState;
import com.telecominfraproject.wlan.profile.ProfileServiceInterface;
import com.telecominfraproject.wlan.profile.models.Profile;
import com.telecominfraproject.wlan.profile.network.models.ApNetworkConfiguration;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration;
import com.telecominfraproject.wlan.routing.RoutingServiceInterface;
import com.telecominfraproject.wlan.servicemetric.apnode.models.ApNodeMetrics;
import com.telecominfraproject.wlan.servicemetric.apnode.models.StateUpDownError;
import com.telecominfraproject.wlan.status.StatusServiceInterface;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentProtocolStatusData;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentUpgradeStatusData;
import com.telecominfraproject.wlan.status.equipment.report.models.ActiveBSSID;
import com.telecominfraproject.wlan.status.equipment.report.models.ActiveBSSIDs;
import com.telecominfraproject.wlan.status.equipment.report.models.ClientConnectionDetails;
import com.telecominfraproject.wlan.status.models.Status;
import com.telecominfraproject.wlan.status.models.StatusDataType;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;

import sts.OpensyncStats.Client;
import sts.OpensyncStats.ClientReport;
import sts.OpensyncStats.DNSProbeMetric;
import sts.OpensyncStats.NetworkProbe;
import sts.OpensyncStats.RADIUSMetrics;
import sts.OpensyncStats.RadioBandType;
import sts.OpensyncStats.Report;
import sts.OpensyncStats.StateUpDown;
import sts.OpensyncStats.VLANMetrics;

@RunWith(SpringRunner.class)
@ActiveProfiles(profiles = { "integration_test", })
@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = OpensyncExternalIntegrationCloudTest.class)
@Import(value = { AlarmServiceInterface.class, OpensyncExternalIntegrationCloud.class,
        OpensyncExternalIntegrationCloudTest.Config.class,

})
public class OpensyncExternalIntegrationCloudTest {

    @MockBean(answer = Answers.RETURNS_MOCKS)
    AlarmServiceInterface alarmServiceInterface;
    @MockBean(answer = Answers.RETURNS_MOCKS)
    CustomerServiceInterface customerServiceInterface;
    @MockBean(answer = Answers.RETURNS_MOCKS)
    LocationServiceInterface locationServiceInterface;
    @MockBean(answer = Answers.RETURNS_MOCKS)
    OvsdbSessionMapInterface ovsdbSessionMapInterface;
    @MockBean(answer = Answers.RETURNS_MOCKS)
    CloudEventDispatcherInterface equipmentMetricsCollectorInterface;
    @MockBean
    EquipmentServiceInterface equipmentServiceInterface;
    @MockBean(answer = Answers.RETURNS_MOCKS)
    RoutingServiceInterface routingServiceInterface;
    @MockBean
    ProfileServiceInterface profileServiceInterface;
    @MockBean(answer = Answers.RETURNS_MOCKS)
    StatusServiceInterface statusServiceInterface;
    @MockBean(answer = Answers.RETURNS_MOCKS)
    ClientServiceInterface clientServiceInterface;
    @MockBean(answer = Answers.RETURNS_MOCKS)
    FirmwareServiceInterface firmwareServiceInterface;
    @MockBean(answer = Answers.RETURNS_MOCKS)
    OpensyncCloudGatewayController gatewayController;

    @Mock(answer = Answers.RETURNS_MOCKS)
    ConnectNodeInfo connectNodeInfo;

    @Autowired
    OpensyncExternalIntegrationCloud opensyncExternalIntegrationCloud;

    MockitoSession mockito;

    @Configuration
    static class Config {
        @Bean
        public OpensyncExternalIntegrationCloud opensyncExternalIntegrationCloud() {
            return new OpensyncExternalIntegrationCloud();
        }

        @Bean
        public CacheManager cacheManager() {
            return new CaffeineCacheManager();
        }
    }

    @Before
    public void setUp() throws Exception {

        mockito = Mockito.mockitoSession().initMocks(this).strictness(Strictness.STRICT_STUBS).startMocking();

    }

    @After
    public void tearDown() throws Exception {
        mockito.finishMocking();

    }

    @Test
    public void testGetCustomerEquipment() {

        Equipment equipment = new Equipment();
        equipment.setDetails(ApElementConfiguration.createWithDefaults());

        Mockito.when(equipmentServiceInterface.getByInventoryIdOrNull(Mockito.eq("Test_Client_21P10C68818122")))
                .thenReturn(equipment);
        assertNotNull(opensyncExternalIntegrationCloud.getCustomerEquipment("Test_Client_21P10C68818122"));
        assertNull(opensyncExternalIntegrationCloud.getCustomerEquipment("Test_Client_21P10C68818133"));

    }

    @Test
    public void testApConnected() {
        connectNodeInfo.wifiRadioStates = ImmutableMap.of("2.4G", "home-ap-24", "5GL", "home-ap-l50", "5GU",
                "home-ap-u50");
        Customer customer = new Customer();
        customer.setId(2);
        CustomerDetails customerDetails = new CustomerDetails();
        customerDetails.setAutoProvisioning(new EquipmentAutoProvisioningSettings());
        customerDetails.getAutoProvisioning().setEnabled(true);
        customer.setDetails(new CustomerDetails());

        Profile apProfile = new Profile();
        apProfile.setDetails(ApNetworkConfiguration.createWithDefaults());

        Profile ssidProfile = new Profile();
        ssidProfile.setDetails(SsidConfiguration.createWithDefaults());

        Mockito.when(profileServiceInterface.create(Mockito.any(Profile.class))).thenReturn(apProfile)
                .thenReturn(ssidProfile);
        Mockito.when(profileServiceInterface.update(Mockito.any(Profile.class))).thenReturn(apProfile);

        Mockito.when(customerServiceInterface.getOrNull(Mockito.anyInt())).thenReturn(customer);

        Status fwStatus = new Status();
        fwStatus.setDetails(new EquipmentUpgradeStatusData());
        Mockito.when(statusServiceInterface.getOrNull(Mockito.anyInt(), Mockito.anyLong(),
                Mockito.eq(StatusDataType.FIRMWARE))).thenReturn(fwStatus);

        Mockito.when(firmwareServiceInterface.getDefaultCustomerTrackSetting())
                .thenReturn(new CustomerFirmwareTrackSettings());
        CustomerFirmwareTrackRecord fwTrackRecord = new CustomerFirmwareTrackRecord();
        fwTrackRecord.setSettings(new CustomerFirmwareTrackSettings());
        fwTrackRecord.setTrackRecordId(3);
        fwTrackRecord.setCustomerId(2);
        Mockito.when(firmwareServiceInterface.getCustomerFirmwareTrackRecord(Mockito.anyInt()))
                .thenReturn(fwTrackRecord);
        opensyncExternalIntegrationCloud.apConnected("Test_Client_21P10C68818122", connectNodeInfo);

    }

    @Test
    public void testApDisconnected() {
        opensyncExternalIntegrationCloud.apDisconnected("Test_Client_21P10C68818122");
    }

    @Test
    public void testGetApConfig() throws Exception {

        Equipment equipment = new Equipment();
        equipment.setDetails(ApElementConfiguration.createWithDefaults());

        Mockito.when(equipmentServiceInterface.getByInventoryIdOrNull(Mockito.any())).thenReturn(equipment);

        Profile apProfile = new Profile();
        apProfile.setDetails(ApNetworkConfiguration.createWithDefaults());

        Profile ssidProfile = new Profile();
        ssidProfile.setDetails(SsidConfiguration.createWithDefaults());

        List<Profile> profileWithChildren = ImmutableList.of(apProfile, ssidProfile);
        Mockito.when(profileServiceInterface.getProfileWithChildren(Mockito.anyLong())).thenReturn(profileWithChildren);

        assertNotNull(opensyncExternalIntegrationCloud.getApConfig("Test_Client_21P10C68818122"));

    }

    @Test
    public void testExtractApIdFromTopic() {

        String topic = "/ap/Test_Client_21P10C68818122/opensync";

        assertEquals("Test_Client_21P10C68818122", OpensyncExternalIntegrationCloud.extractApIdFromTopic(topic));

    }

    @Test
    public void testExtractCustomerIdFromTopic() {
        String topic = "/ap/Test_Client_21P10C68818122/opensync";
        OvsdbSession session = Mockito.mock(OvsdbSession.class);
        Mockito.when(session.getCustomerId()).thenReturn(2);

        Mockito.when(ovsdbSessionMapInterface.getSession("Test_Client_21P10C68818122")).thenReturn(session);

        assertEquals(2, opensyncExternalIntegrationCloud.extractCustomerIdFromTopic(topic));
    }

    @Test
    public void testExtractEquipmentIdFromTopic() {

        String topic = "/ap/Test_Client_21P10C68818122/opensync";
        OvsdbSession session = Mockito.mock(OvsdbSession.class);
        Mockito.when(session.getEquipmentId()).thenReturn(1L);

        Mockito.when(ovsdbSessionMapInterface.getSession("Test_Client_21P10C68818122")).thenReturn(session);

        assertEquals(1L, opensyncExternalIntegrationCloud.extractEquipmentIdFromTopic(topic));

    }

    @Test
    public void testProcessMqttMessageStringReport() {

        Report report = Report.newBuilder().setNodeID("21P10C68818122")
                .addAllClients(getOpensyncStatsClientReportsList()).build();

        String topic = "/ap/Test_Client_21P10C68818122/opensync";

        OvsdbSession session = Mockito.mock(OvsdbSession.class);
        Mockito.when(session.getEquipmentId()).thenReturn(1L);
        Mockito.when(session.getCustomerId()).thenReturn(2);

        Mockito.when(ovsdbSessionMapInterface.getSession("Test_Client_21P10C68818122")).thenReturn(session);

        Status bssidStatus = new Status();
        bssidStatus.setStatusDataType(StatusDataType.ACTIVE_BSSIDS);
        bssidStatus.setCustomerId(2);

        ActiveBSSIDs activeBssidsDetails = new ActiveBSSIDs();
        activeBssidsDetails.setActiveBSSIDs(getActiveBssidList());
        bssidStatus.setDetails(activeBssidsDetails);

        Mockito.when(statusServiceInterface.getOrNull(Mockito.anyInt(), Mockito.anyLong(),
                Mockito.eq(StatusDataType.ACTIVE_BSSIDS))).thenReturn(bssidStatus);

        Mockito.when(statusServiceInterface.update(Mockito.any(Status.class))).thenReturn(bssidStatus);
        com.telecominfraproject.wlan.client.models.Client clientInstance = new com.telecominfraproject.wlan.client.models.Client();
        clientInstance.setMacAddress(new MacAddress("7C:AB:60:E6:EA:4D"));
        clientInstance.setDetails(new ClientInfoDetails());
        com.telecominfraproject.wlan.client.models.Client clientInstance2 = new com.telecominfraproject.wlan.client.models.Client();
        clientInstance2.setMacAddress(new MacAddress("C0:9A:D0:76:A9:69"));
        clientInstance2.setDetails(new ClientInfoDetails());
        Mockito.when(clientServiceInterface.getOrNull(Mockito.anyInt(), Mockito.any(MacAddress.class)))
                .thenReturn(clientInstance).thenReturn(clientInstance2);
        Mockito.when(
                clientServiceInterface.update(Mockito.any(com.telecominfraproject.wlan.client.models.Client.class)))
                .thenReturn(clientInstance).thenReturn(clientInstance2);

        ClientSession clientSession = new ClientSession();
        clientSession.setMacAddress(new MacAddress("7C:AB:60:E6:EA:4D"));
        clientSession.setDetails(new ClientSessionDetails());
        ClientSession clientSession2 = new ClientSession();
        clientSession2.setMacAddress(new MacAddress("C0:9A:D0:76:A9:69"));
        clientSession2.setDetails(new ClientSessionDetails());
        Mockito.when(clientServiceInterface.getSessionOrNull(Mockito.anyInt(), Mockito.anyLong(),
                Mockito.any(MacAddress.class))).thenReturn(clientSession).thenReturn(clientSession2);

        Mockito.when(clientServiceInterface.updateSession(Mockito.any(ClientSession.class))).thenReturn(clientSession)
                .thenReturn(clientSession2);

        opensyncExternalIntegrationCloud.processMqttMessage(topic, report);

        Mockito.verify(clientServiceInterface, Mockito.times(2)).getOrNull(Mockito.anyInt(),
                Mockito.any(MacAddress.class));
        Mockito.verify(clientServiceInterface, Mockito.times(2)).getSessionOrNull(Mockito.anyInt(), Mockito.anyLong(),
                Mockito.any(MacAddress.class));
        Mockito.verify(clientServiceInterface, Mockito.times(2)).updateSession(Mockito.any(ClientSession.class));
        Mockito.verify(clientServiceInterface, Mockito.times(2))
                .update(Mockito.any(com.telecominfraproject.wlan.client.models.Client.class));
        Mockito.verify(statusServiceInterface, Mockito.times(3)).getOrNull(Mockito.anyInt(), Mockito.anyLong(),
                Mockito.eq(StatusDataType.ACTIVE_BSSIDS));

    }

    @Test
    public void testGetNegativeSignedIntFromUnsigned() {
        int unsignedVal = Long.valueOf(4294967239L).intValue();
        int expectedVal = -57; // (unsignedValue << 1) >> 1
        assert (expectedVal == opensyncExternalIntegrationCloud.getNegativeSignedIntFromUnsigned(unsignedVal));

    }

    @Ignore
    public void testProcessMqttMessageStringFlowReport() {
        // TODO: implement me when support flow reports
    }

    @Ignore
    public void testProcessMqttMessageStringWCStatsReport() {
        // TODO: implement me when wcs stats reports supported
    }

    @Test
    public void testWifiVIFStateDbTableUpdate() {

        String apId = "Test_Client_21P10C68818122";
        OpensyncAPVIFState vifState1 = new OpensyncAPVIFState();
        vifState1.setMac("24:f5:a2:ef:2e:54");
        vifState1.setSsid("ssid-1");
        // OpensyncWifiAssociatedClients wifiClient1 = new
        // OpensyncWifiAssociatedClients();
        // wifiClient1.setMac("C0:9A:D0:E6:EA:4D");
        Uuid uuid1 = Uuid.of(UUID.randomUUID());
        vifState1.setAssociatedClients(ImmutableList.of(uuid1));
        OpensyncAPVIFState vifState2 = new OpensyncAPVIFState();
        vifState2.setMac("24:f5:a2:ef:2e:55");
        vifState2.setSsid("ssid-2");
        // OpensyncWifiAssociatedClients wifiClient2 = new
        // OpensyncWifiAssociatedClients();
        // wifiClient2.setMac("7C:AB:60:E6:EA:4D");
        Uuid uuid2 = Uuid.of(UUID.randomUUID());
        vifState2.setAssociatedClients(ImmutableList.of(uuid2));
        OpensyncAPVIFState vifState3 = new OpensyncAPVIFState();
        vifState3.setMac("24:f5:a2:ef:2e:56");
        vifState3.setSsid("ssid-3");
        // OpensyncWifiAssociatedClients wifiClient3 = new
        // OpensyncWifiAssociatedClients();
        // wifiClient3.setMac("C0:9A:D0:76:A9:69");
        Uuid uuid3 = Uuid.of(UUID.randomUUID());

        vifState3.setAssociatedClients(ImmutableList.of(uuid3));

        Status bssidStatus = new Status();
        bssidStatus.setStatusDataType(StatusDataType.ACTIVE_BSSIDS);
        bssidStatus.setCustomerId(2);

        ActiveBSSIDs activeBssidsDetails = new ActiveBSSIDs();
        activeBssidsDetails.setActiveBSSIDs(getActiveBssidList());
        bssidStatus.setDetails(activeBssidsDetails);

        Mockito.when(statusServiceInterface.getOrNull(2, 1L, StatusDataType.ACTIVE_BSSIDS)).thenReturn(bssidStatus);
        Mockito.when(statusServiceInterface.update(bssidStatus)).thenReturn(bssidStatus);

        Status clientStatus = new Status();
        clientStatus.setCustomerId(2);
        clientStatus.setEquipmentId(1L);
        clientStatus.setStatusDataType(StatusDataType.CLIENT_DETAILS);
        ClientConnectionDetails clientConnectionDetails = new ClientConnectionDetails();
        Map<RadioType, Integer> clientsPerRadio = new HashMap<>();
        clientConnectionDetails.setNumClientsPerRadio(clientsPerRadio);
        clientStatus.setDetails(clientConnectionDetails);

        Mockito.when(statusServiceInterface.getOrNull(2, 1L, StatusDataType.CLIENT_DETAILS)).thenReturn(clientStatus);
        Mockito.when(statusServiceInterface.update(clientStatus)).thenReturn(clientStatus);

        OvsdbSession session = Mockito.mock(OvsdbSession.class);
        Mockito.when(session.getEquipmentId()).thenReturn(1L);
        Mockito.when(session.getCustomerId()).thenReturn(2);

        Equipment equipment = new Equipment();
        equipment.setCustomerId(1);
        equipment.setEquipmentType(EquipmentType.AP);
        equipment.setInventoryId(apId);
        equipment.setDetails(ApElementConfiguration.createWithDefaults());

        Mockito.when(equipmentServiceInterface.getByInventoryIdOrNull(apId)).thenReturn(equipment);
        Mockito.when(equipmentServiceInterface.update(equipment)).thenReturn(equipment);

        Mockito.when(ovsdbSessionMapInterface.getSession(apId)).thenReturn(session);

        opensyncExternalIntegrationCloud.wifiVIFStateDbTableUpdate(ImmutableList.of(vifState1, vifState2, vifState3),
                apId);

        Mockito.verify(session).getCustomerId();
        Mockito.verify(session).getEquipmentId();
        Mockito.verify(ovsdbSessionMapInterface).getSession(apId);
        Mockito.verify(equipmentServiceInterface).getByInventoryIdOrNull(apId);
        Mockito.verify(statusServiceInterface).getOrNull(2, 1L, StatusDataType.CLIENT_DETAILS);
        Mockito.verify(statusServiceInterface).update(clientStatus);
        Mockito.verify(statusServiceInterface).update(bssidStatus);

    }

    @Test
    public void testWifiRadioStatusDbTableUpdate() {

        String apId = "Test_Client_21P10C68818122";
        OpensyncAPVIFState vifState1 = new OpensyncAPVIFState();
        vifState1.setMac("24:f5:a2:ef:2e:54");
        vifState1.setSsid("ssid-1");
        Uuid uuid1 = Uuid.of(UUID.randomUUID());

        vifState1.setAssociatedClients(ImmutableList.of(uuid1));

        OpensyncAPRadioState radioState1 = new OpensyncAPRadioState();
        radioState1.setChannel(6);
        radioState1.setVifStates(ImmutableList.of(vifState1));
        radioState1.setFreqBand(RadioType.is5GHzL);
        radioState1.setAllowedChannels(ImmutableSet.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11));
        radioState1.setTxPower(32);
        radioState1.setEnabled(true);
        radioState1.setCountry("CA");

        OpensyncAPVIFState vifState2 = new OpensyncAPVIFState();
        vifState2.setMac("24:f5:a2:ef:2e:55");
        vifState2.setSsid("ssid-2");
        Uuid uuid2 = Uuid.of(UUID.randomUUID());

        vifState2.setAssociatedClients(ImmutableList.of(uuid2));

        OpensyncAPRadioState radioState2 = new OpensyncAPRadioState();
        radioState2.setChannel(36);
        radioState2.setVifStates(ImmutableList.of(vifState2));
        radioState2.setFreqBand(RadioType.is5GHzL);
        radioState2.setAllowedChannels(ImmutableSet.of(36, 40, 44, 48, 52, 56, 60, 64));
        radioState2.setTxPower(32);
        radioState2.setEnabled(true);
        radioState2.setCountry("CA");

        OpensyncAPVIFState vifState3 = new OpensyncAPVIFState();
        vifState3.setMac("24:f5:a2:ef:2e:56");
        vifState3.setSsid("ssid-3");
        Uuid uuid3 = Uuid.of(UUID.randomUUID());

        vifState3.setAssociatedClients(ImmutableList.of(uuid3));

        OpensyncAPRadioState radioState3 = new OpensyncAPRadioState();
        radioState3.setChannel(149);
        radioState3.setVifStates(ImmutableList.of(vifState3));
        radioState3.setFreqBand(RadioType.is5GHzL);
        radioState3.setAllowedChannels(
                ImmutableSet.of(00, 104, 108, 112, 116, 132, 136, 140, 144, 149, 153, 157, 161, 165));
        radioState3.setTxPower(32);
        radioState3.setEnabled(true);
        radioState3.setCountry("CA");

        Equipment equipment = new Equipment();
        equipment.setCustomerId(1);
        equipment.setEquipmentType(EquipmentType.AP);
        equipment.setInventoryId(apId);
        equipment.setDetails(ApElementConfiguration.createWithDefaults());

        Mockito.when(equipmentServiceInterface.getByInventoryIdOrNull(apId)).thenReturn(equipment);
        Mockito.when(equipmentServiceInterface.update(equipment)).thenReturn(equipment);

        Status protocolStatus = new Status();
        protocolStatus.setCustomerId(2);
        protocolStatus.setEquipmentId(1L);
        EquipmentProtocolStatusData protocolStatusData = new EquipmentProtocolStatusData();
        protocolStatusData.setReportedCC(CountryCode.ca);
        protocolStatus.setDetails(protocolStatusData);
        protocolStatus.setStatusDataType(StatusDataType.PROTOCOL);

        Mockito.when(statusServiceInterface.getOrNull(2, 1L, StatusDataType.PROTOCOL)).thenReturn(protocolStatus);

        Status bssidStatus = new Status();
        bssidStatus.setStatusDataType(StatusDataType.ACTIVE_BSSIDS);
        bssidStatus.setCustomerId(2);

        ActiveBSSIDs activeBssidsDetails = new ActiveBSSIDs();
        activeBssidsDetails.setActiveBSSIDs(getActiveBssidList());
        bssidStatus.setDetails(activeBssidsDetails);

        Mockito.when(statusServiceInterface.getOrNull(2, 1L, StatusDataType.ACTIVE_BSSIDS)).thenReturn(bssidStatus);

        Mockito.when(statusServiceInterface.update(bssidStatus)).thenReturn(bssidStatus);

        Status clientStatus = new Status();
        clientStatus.setCustomerId(2);
        clientStatus.setEquipmentId(1L);
        clientStatus.setStatusDataType(StatusDataType.CLIENT_DETAILS);
        ClientConnectionDetails clientConnectionDetails = new ClientConnectionDetails();

        Map<RadioType, Integer> clientsPerRadio = new HashMap<>();
        clientConnectionDetails.setNumClientsPerRadio(clientsPerRadio);
        clientStatus.setDetails(clientConnectionDetails);

        Mockito.when(statusServiceInterface.getOrNull(2, 1L, StatusDataType.CLIENT_DETAILS)).thenReturn(clientStatus);
        Mockito.when(statusServiceInterface.update(clientStatus)).thenReturn(clientStatus);

        OvsdbSession session = Mockito.mock(OvsdbSession.class);
        Mockito.when(session.getEquipmentId()).thenReturn(1L);
        Mockito.when(session.getCustomerId()).thenReturn(2);

        Mockito.when(ovsdbSessionMapInterface.getSession(apId)).thenReturn(session);

        opensyncExternalIntegrationCloud
                .wifiRadioStatusDbTableUpdate(ImmutableList.of(radioState1, radioState2, radioState3), apId);

        Mockito.verify(session).getCustomerId();
        Mockito.verify(session).getEquipmentId();

        Mockito.verify(ovsdbSessionMapInterface).getSession(apId);
        Mockito.verify(equipmentServiceInterface, Mockito.times(2)).getByInventoryIdOrNull(apId);

        Mockito.verify(statusServiceInterface).getOrNull(2, 1L, StatusDataType.CLIENT_DETAILS);
        Mockito.verify(statusServiceInterface).update(clientStatus);
        Mockito.verify(statusServiceInterface, Mockito.times(3)).update(bssidStatus);

    }

    @Ignore
    public void testWifiInetStateDbTableUpdate() {
        // TODO: implement me
    }

    @Ignore
    public void testWifiAssociatedClientsDbTableUpdate() {
        // TODO: implement me
    }

    @Ignore
    public void testAwlanNodeDbTableUpdate() {
        // TODO: implement me
    }

    @Test
    public void testWifiVIFStateDbTableDelete() {

        Status bssidStatus = new Status();
        bssidStatus.setStatusDataType(StatusDataType.ACTIVE_BSSIDS);
        bssidStatus.setCustomerId(2);

        ActiveBSSIDs activeBssidsDetails = new ActiveBSSIDs();
        activeBssidsDetails.setActiveBSSIDs(getActiveBssidList());
        bssidStatus.setDetails(activeBssidsDetails);

        Mockito.when(statusServiceInterface.getOrNull(Mockito.anyInt(), Mockito.anyLong(),
                Mockito.eq(StatusDataType.ACTIVE_BSSIDS))).thenReturn(bssidStatus);

        opensyncExternalIntegrationCloud.wifiVIFStateDbTableDelete(ImmutableList.of(new OpensyncAPVIFState()), "apId");
    }

    @Test
    public void testWifiAssociatedClientsDbTableDelete() {

        opensyncExternalIntegrationCloud.wifiAssociatedClientsDbTableDelete("7C:AB:60:E6:EA:4D", "apId");
    }

    @Test
    public void testpopulateNetworkProbeMetrics() throws Exception {

        InetAddress ip = InetAddress.getByName("192.168.1.1");

        DNSProbeMetric dnsProbeMetric = DNSProbeMetric.getDefaultInstance().toBuilder().setLatency(10)
                .setState(StateUpDown.SUD_up).setServerIP(ByteString.copyFrom(ip.getAddress())).build();
        RADIUSMetrics radiusProbeMetric = RADIUSMetrics.getDefaultInstance().toBuilder().setLatencyAve(10).build();
        VLANMetrics vlanMetrics = VLANMetrics.getDefaultInstance().toBuilder().setDhcpLatency(10)
                .setDhcpState(StateUpDown.SUD_up).setVlanIF("vlan-1").setObsV200RadiusLatency(15)
                .setObsV200RadiusState(StateUpDown.SUD_up).build();
        NetworkProbe networkProbe = NetworkProbe.getDefaultInstance().toBuilder().setVlanProbe(vlanMetrics)
                .setDnsProbe(dnsProbeMetric).setRadiusProbe(radiusProbeMetric).build();

        Report report = Report.getDefaultInstance().toBuilder().setNodeID("21P10C68818122")
                .addNetworkProbe(networkProbe).build();

        ApNodeMetrics apNodeMetrics = new ApNodeMetrics();

        opensyncExternalIntegrationCloud.populateNetworkProbeMetrics(report, apNodeMetrics);

        assertNotNull(apNodeMetrics.getNetworkProbeMetrics());

        assert (apNodeMetrics.getNetworkProbeMetrics().get(0).getDhcpLatencyMs() == 10);
        assert (apNodeMetrics.getNetworkProbeMetrics().get(0).getDnsLatencyMs() == 10);
        assert (apNodeMetrics.getNetworkProbeMetrics().get(0).getRadiusState().equals(StateUpDownError.enabled));
        assert (apNodeMetrics.getNetworkProbeMetrics().get(0).getDhcpState().equals(StateUpDownError.enabled));
        assert (apNodeMetrics.getNetworkProbeMetrics().get(0).getDnsState().equals(StateUpDownError.enabled));
        assert (apNodeMetrics.getNetworkProbeMetrics().get(0).getRadiusLatencyInMs() == 15);
        assert (apNodeMetrics.getNetworkProbeMetrics().get(0).getVlanIF().equals("vlan-1"));

    }

    // Helper methods
    private List<ActiveBSSID> getActiveBssidList() {
        ActiveBSSID activeBssid = new ActiveBSSID();
        activeBssid.setBssid("24:f5:a2:ef:2e:54");
        activeBssid.setSsid("ssid-1");
        activeBssid.setNumDevicesConnected(1);
        activeBssid.setRadioType(RadioType.is2dot4GHz);
        ActiveBSSID activeBssid2 = new ActiveBSSID();
        activeBssid2.setBssid("24:f5:a2:ef:2e:55");
        activeBssid2.setSsid("ssid-2");
        activeBssid2.setNumDevicesConnected(1);
        activeBssid2.setRadioType(RadioType.is5GHzL);
        ActiveBSSID activeBssid3 = new ActiveBSSID();
        activeBssid3.setBssid("24:f5:a2:ef:2e:56");
        activeBssid3.setSsid("ssid-3");
        activeBssid3.setNumDevicesConnected(1);
        activeBssid3.setRadioType(RadioType.is5GHzU);

        List<ActiveBSSID> bssidList = new ArrayList<ActiveBSSID>();
        bssidList.add(activeBssid);
        bssidList.add(activeBssid2);
        bssidList.add(activeBssid3);
        return bssidList;
    }

    private List<ClientReport> getOpensyncStatsClientReportsList() {
        int rssi = Long.valueOf(4294967239L).intValue();

        Client.Stats clientStats = Client.Stats.getDefaultInstance().toBuilder().setRssi(rssi).setRxBytes(225554786)
                .setRxRate(24000.0).setTxBytes(1208133026).setTxRate(433300.0).build();
        Client client2g = Client.getDefaultInstance().toBuilder().setMacAddress("7C:AB:60:E6:EA:4D").setSsid("ssid-1")
                .setConnected(true).setDurationMs(59977).setStats(clientStats).build();
        Client client5gu = Client.getDefaultInstance().toBuilder().setMacAddress("C0:9A:D0:76:A9:69").setSsid("ssid-3")
                .setConnected(true).setDurationMs(298127).setStats(clientStats).build();

        ClientReport clientReport2g = ClientReport.getDefaultInstance().toBuilder().setBand(RadioBandType.BAND2G)
                .setChannel(6).addAllClientList(ImmutableList.of(client2g)).build();
        ClientReport clientReport5gl = ClientReport.getDefaultInstance().toBuilder().setBand(RadioBandType.BAND5GL)
                .setChannel(36).build();
        ClientReport clientReport5gu = ClientReport.getDefaultInstance().toBuilder().setBand(RadioBandType.BAND5GU)
                .setChannel(157).addAllClientList(ImmutableList.of(client5gu)).build();
        List<ClientReport> clients = new ArrayList<ClientReport>();
        clients.add(clientReport2g);
        clients.add(clientReport5gl);
        clients.add(clientReport5gu);
        return clients;
    }

}
