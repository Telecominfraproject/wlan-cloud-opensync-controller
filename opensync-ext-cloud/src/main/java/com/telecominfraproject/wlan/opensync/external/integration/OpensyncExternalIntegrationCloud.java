package com.telecominfraproject.wlan.opensync.external.integration;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.telecominfraproject.wlan.client.ClientServiceInterface;
import com.telecominfraproject.wlan.client.info.models.ClientInfoDetails;
import com.telecominfraproject.wlan.client.models.Client;
import com.telecominfraproject.wlan.client.models.ClientType;
import com.telecominfraproject.wlan.client.session.models.AssociationState;
import com.telecominfraproject.wlan.client.session.models.ClientDhcpDetails;
import com.telecominfraproject.wlan.client.session.models.ClientSession;
import com.telecominfraproject.wlan.client.session.models.ClientSessionDetails;
import com.telecominfraproject.wlan.core.model.entity.CountryCode;
import com.telecominfraproject.wlan.core.model.equipment.EquipmentType;
import com.telecominfraproject.wlan.core.model.equipment.MacAddress;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.core.model.equipment.SourceSelectionValue;
import com.telecominfraproject.wlan.core.model.equipment.SourceType;
import com.telecominfraproject.wlan.core.model.pagination.PaginationContext;
import com.telecominfraproject.wlan.core.model.pagination.PaginationResponse;
import com.telecominfraproject.wlan.customer.models.Customer;
import com.telecominfraproject.wlan.customer.models.EquipmentAutoProvisioningSettings;
import com.telecominfraproject.wlan.customer.service.CustomerServiceInterface;
import com.telecominfraproject.wlan.datastore.exceptions.DsConcurrentModificationException;
import com.telecominfraproject.wlan.equipment.EquipmentServiceInterface;
import com.telecominfraproject.wlan.equipment.models.ApElementConfiguration;
import com.telecominfraproject.wlan.equipment.models.ElementRadioConfiguration;
import com.telecominfraproject.wlan.equipment.models.Equipment;
import com.telecominfraproject.wlan.equipment.models.RadioConfiguration;
import com.telecominfraproject.wlan.equipment.models.StateSetting;
import com.telecominfraproject.wlan.equipmentgateway.models.CEGWBaseCommand;
import com.telecominfraproject.wlan.equipmentgateway.models.CEGWFirmwareDownloadRequest;
import com.telecominfraproject.wlan.firmware.FirmwareServiceInterface;
import com.telecominfraproject.wlan.firmware.models.CustomerFirmwareTrackRecord;
import com.telecominfraproject.wlan.firmware.models.CustomerFirmwareTrackSettings;
import com.telecominfraproject.wlan.firmware.models.CustomerFirmwareTrackSettings.TrackFlag;
import com.telecominfraproject.wlan.firmware.models.FirmwareTrackAssignmentDetails;
import com.telecominfraproject.wlan.firmware.models.FirmwareTrackRecord;
import com.telecominfraproject.wlan.firmware.models.FirmwareVersion;
import com.telecominfraproject.wlan.location.models.Location;
import com.telecominfraproject.wlan.location.service.LocationServiceInterface;
import com.telecominfraproject.wlan.opensync.external.integration.controller.OpensyncCloudGatewayController;
import com.telecominfraproject.wlan.opensync.external.integration.controller.OpensyncCloudGatewayController.ListOfEquipmentCommandResponses;
import com.telecominfraproject.wlan.opensync.external.integration.models.ConnectNodeInfo;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPHotspot20Config;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPInetState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPRadioState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPVIFState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAWLANNode;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncWifiAssociatedClients;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.enumerations.DhcpFpDeviceType;
import com.telecominfraproject.wlan.opensync.util.OvsdbStringConstants;
import com.telecominfraproject.wlan.opensync.util.OvsdbToWlanCloudTypeMappingUtility;
import com.telecominfraproject.wlan.profile.ProfileServiceInterface;
import com.telecominfraproject.wlan.profile.models.Profile;
import com.telecominfraproject.wlan.profile.models.ProfileContainer;
import com.telecominfraproject.wlan.profile.models.ProfileType;
import com.telecominfraproject.wlan.profile.network.models.ApNetworkConfiguration;
import com.telecominfraproject.wlan.profile.network.models.RadioProfileConfiguration;
import com.telecominfraproject.wlan.profile.rf.models.RfConfiguration;
import com.telecominfraproject.wlan.profile.rf.models.RfElementConfiguration;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration.SecureMode;
import com.telecominfraproject.wlan.routing.RoutingServiceInterface;
import com.telecominfraproject.wlan.routing.models.EquipmentRoutingRecord;
import com.telecominfraproject.wlan.status.StatusServiceInterface;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentAdminStatusData;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentLANStatusData;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentProtocolState;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentProtocolStatusData;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentUpgradeState;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentUpgradeState.FailureReason;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentUpgradeStatusData;
import com.telecominfraproject.wlan.status.equipment.models.VLANStatusData;
import com.telecominfraproject.wlan.status.equipment.report.models.ActiveBSSID;
import com.telecominfraproject.wlan.status.equipment.report.models.ActiveBSSIDs;
import com.telecominfraproject.wlan.status.equipment.report.models.ClientConnectionDetails;
import com.telecominfraproject.wlan.status.equipment.report.models.EquipmentScanDetails;
import com.telecominfraproject.wlan.status.equipment.report.models.OperatingSystemPerformance;
import com.telecominfraproject.wlan.status.equipment.report.models.RadioUtilizationReport;
import com.telecominfraproject.wlan.status.models.Status;
import com.telecominfraproject.wlan.status.models.StatusCode;
import com.telecominfraproject.wlan.status.models.StatusDataType;
import com.telecominfraproject.wlan.status.network.models.NetworkAdminStatusData;

import sts.OpensyncStats.Report;
import traffic.NetworkMetadata.FlowReport;
import wc.stats.IpDnsTelemetry.WCStatsReport;

@org.springframework.context.annotation.Profile("opensync_cloud_config")
@Component
public class OpensyncExternalIntegrationCloud implements OpensyncExternalIntegrationInterface {

    private static final Logger LOG = LoggerFactory.getLogger(OpensyncExternalIntegrationCloud.class);

    @Autowired
    private CustomerServiceInterface customerServiceInterface;
    @Autowired
    private LocationServiceInterface locationServiceInterface;
    @Autowired
    private OvsdbSessionMapInterface ovsdbSessionMapInterface;
    @Autowired
    private EquipmentServiceInterface equipmentServiceInterface;
    @Autowired
    private RoutingServiceInterface routingServiceInterface;
    @Autowired
    private ProfileServiceInterface profileServiceInterface;
    @Autowired
    private StatusServiceInterface statusServiceInterface;
    @Autowired
    private ClientServiceInterface clientServiceInterface;
    @Autowired
    private FirmwareServiceInterface firmwareServiceInterface;
    @Autowired
    private OpensyncExternalIntegrationMqttMessageProcessor mqttMessageProcessor;

    @Autowired
    private OpensyncCloudGatewayController gatewayController;

    @Value("${tip.wlan.ovsdb.autoProvisionedCustomerId:1970}")
    private int autoProvisionedCustomerId;
    @Value("${tip.wlan.ovsdb.autoProvisionedLocationId:8}")
    private int autoProvisionedLocationId;
    @Value("${tip.wlan.ovsdb.autoProvisionedProfileId:1}")
    private int autoProvisionedProfileId;
    @Value("${tip.wlan.ovsdb.autoProvisionedSsid:DefaultSsid-}")
    private String autoProvisionedSsid;
    @Value("${tip.wlan.ovsdb.autoprovisionedSsidKey:12345678}")
    private String autoprovisionedSsidKey;
    @Value("${tip.wlan.ovsdb.isAutoconfigEnabled:true}")
    private boolean isAutoconfigEnabled;
    @Value("${tip.wlan.ovsdb.defaultFwVersion:r10947-65030d81f3}")
    private String defaultFwVersion;
    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-iface.default_lan_type:bridge}")
    public String defaultLanInterfaceType;
    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-iface.default_lan_name:lan}")
    public String defaultLanInterfaceName;
    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-iface.default_wan_type:bridge}")
    public String defaultWanInterfaceType;
    @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.wifi-iface.default_wan_name:wan}")
    public String defaultWanInterfaceName;

    @Override
    public void apConnected(String apId, ConnectNodeInfo connectNodeInfo) {

        Equipment ce = equipmentServiceInterface.getByInventoryIdOrNull(apId);

        try {
            if (ce == null) {

                Customer customer = customerServiceInterface.getOrNull(autoProvisionedCustomerId);
                if (customer == null) {
                    LOG.error("Cannot auto-provision equipment because customer with id {} is not found",
                            autoProvisionedCustomerId);
                    throw new IllegalStateException("Cannot auto-provision equipment because customer is not found : "
                            + autoProvisionedCustomerId);
                }

                if ((customer.getDetails() != null) && (customer.getDetails().getAutoProvisioning() != null)
                        && !customer.getDetails().getAutoProvisioning().isEnabled()) {
                    LOG.error(
                            "Cannot auto-provision equipment because customer with id {} explicitly turned that feature off",
                            autoProvisionedCustomerId);
                    throw new IllegalStateException(
                            "Cannot auto-provision equipment because customer explicitly turned that feature off : "
                                    + autoProvisionedCustomerId);
                }

                // String dbUrlString =
                // customer.getDetails().getClientFingerPrintsDbUrl();
                ce = new Equipment();
                ce.setEquipmentType(EquipmentType.AP);
                ce.setInventoryId(apId);
                try {
                    ce.setBaseMacAddress(MacAddress.valueOf(connectNodeInfo.macAddress));
                } catch (RuntimeException e) {
                    LOG.warn("Auto-provisioning: cannot parse equipment mac address {}", connectNodeInfo.macAddress);
                }

                long locationId = autoProvisionedLocationId;
                if ((customer.getDetails() != null) && (customer.getDetails().getAutoProvisioning() != null)
                        && customer.getDetails().getAutoProvisioning().isEnabled()) {
                    locationId = customer.getDetails().getAutoProvisioning().getLocationId();
                }

                try {
                    Location location = locationServiceInterface.get(locationId);
                    ce.setLocationId(location.getId());
                } catch (Exception e) {
                    LOG.error("Cannot auto-provision equipment because customer location with id {} cannot be found",
                            locationId);
                    throw new IllegalStateException(
                            "Cannot auto-provision equipment because customer location cannot be found : "
                                    + locationId);

                }

                ce.setSerial(connectNodeInfo.serialNumber);
                ce.setDetails(ApElementConfiguration.createWithDefaults());
                ce.setCustomerId(autoProvisionedCustomerId);
                ce.setName(apId);
                ce = equipmentServiceInterface.create(ce);

                ApElementConfiguration apElementConfig = (ApElementConfiguration) ce.getDetails();
                apElementConfig.setDeviceName(ce.getName());
                apElementConfig.setEquipmentModel(connectNodeInfo.model);
                Map<RadioType, RadioConfiguration> advancedRadioMap = new HashMap<>();
                Map<RadioType, ElementRadioConfiguration> radioMap = new HashMap<>();
                for (String radio : connectNodeInfo.wifiRadioStates.keySet()) {
                    RadioConfiguration advancedRadioConfiguration = null;
                    ElementRadioConfiguration radioConfiguration = null;
                    RadioType radioType = RadioType.UNSUPPORTED;
                    if (radio.equals("2.4G")) {
                        radioType = RadioType.is2dot4GHz;
                    } else if (radio.equals("5G")) {
                        radioType = RadioType.is5GHz;
                    } else if (radio.equals("5GL")) {
                        radioType = RadioType.is5GHzL;
                    } else if (radio.equals("5GU")) {
                        radioType = RadioType.is5GHzU;
                    }
                    if (!radioType.equals(RadioType.UNSUPPORTED)) {
                        advancedRadioConfiguration = RadioConfiguration.createWithDefaults(radioType);

                        advancedRadioMap.put(radioType, advancedRadioConfiguration);
                        radioConfiguration = ElementRadioConfiguration.createWithDefaults(radioType);
                        radioMap.put(radioType, radioConfiguration);
                    }
                }

                apElementConfig.setRadioMap(radioMap);
                apElementConfig.setAdvancedRadioMap(advancedRadioMap);

                ce.setDetails(apElementConfig);

                Long profileId = null;
                if ((customer.getDetails() != null) && (customer.getDetails().getAutoProvisioning() != null)
                        && customer.getDetails().getAutoProvisioning().isEnabled()
                        && (customer.getDetails().getAutoProvisioning().getEquipmentProfileIdPerModel() != null)) {

                    // try to find auto-provisioning profile for the current
                    // equipment model
                    profileId = customer.getDetails().getAutoProvisioning().getEquipmentProfileIdPerModel()
                            .get(ce.getDetails().getEquipmentModel());
                    if (profileId == null) {
                        // could not find profile for the equipment model,
                        // lets try to find a default profile
                        profileId = customer.getDetails().getAutoProvisioning().getEquipmentProfileIdPerModel()
                                .get(EquipmentAutoProvisioningSettings.DEFAULT_MODEL_NAME);
                    }
                }

                if (profileId == null) {
                    // create default apProfile if cannot find applicable
                    // one:
                    Profile apProfile = createDefaultApProfile(ce, connectNodeInfo);
                    profileId = apProfile.getId();
                }

                ce.setProfileId(profileId);

                ce = equipmentServiceInterface.update(ce);

            } else {
                // equipment already exists

                MacAddress reportedMacAddress = null;
                try {
                    reportedMacAddress = MacAddress.valueOf(connectNodeInfo.macAddress);
                } catch (RuntimeException e) {
                    LOG.warn("AP connect: cannot parse equipment mac address {}", connectNodeInfo.macAddress);
                }

                if (reportedMacAddress != null) {
                    // check if reported mac address matches what is in the
                    // db
                    if (!reportedMacAddress.equals(ce.getBaseMacAddress())) {
                        // need to update base mac address on equipment in
                        // DB
                        ce = equipmentServiceInterface.get(ce.getId());
                        ce.setBaseMacAddress(reportedMacAddress);
                        ce = equipmentServiceInterface.update(ce);

                    }
                }

            }

            EquipmentRoutingRecord equipmentRoutingRecord = gatewayController.registerCustomerEquipment(ce.getName(),
                    ce.getCustomerId(), ce.getId());

            // Status and client cleanup, when AP reconnects or has been
            // disconnected, reset statuses, clients set to disconnected as
            // SSIDs etc will be reconfigured on AP
            LOG.info("Clear existing status {} for AP {}",
                    statusServiceInterface.delete(ce.getCustomerId(), ce.getId()), apId);
            LOG.info("Set pre-existing client sessions to disconnected for AP {}", apId);
            disconnectClients(ce);

            updateApStatus(ce, connectNodeInfo);

            removeNonWifiClients(ce, connectNodeInfo);

            OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);
            ovsdbSession.setRoutingId(equipmentRoutingRecord.getId());
            ovsdbSession.setEquipmentId(ce.getId());
            ovsdbSession.setCustomerId(ce.getCustomerId());

            LOG.debug("Equipment {}", ce);
            LOG.info("AP {} got connected to the gateway", apId);
            LOG.info("ConnectNodeInfo {}", connectNodeInfo);

            if (connectNodeInfo.versionMatrix.containsKey(OvsdbStringConstants.FW_IMAGE_ACTIVE_KEY)) {
                reconcileFwVersionToTrack(ce,
                        connectNodeInfo.versionMatrix.get(OvsdbStringConstants.FW_IMAGE_ACTIVE_KEY),
                        connectNodeInfo.model);
            } else {
                LOG.info("Cloud based firmware upgrade is not supported for this AP");
            }

        } catch (Exception e) {
            LOG.error("Could not process connection from AP {}", apId, e);
            throw e;
        }

    }

    private Profile createDefaultApProfile(Equipment ce, ConnectNodeInfo connectNodeInfo) {
        Profile apProfile = new Profile();
        apProfile.setCustomerId(ce.getCustomerId());
        apProfile.setName("DefaultApProfile for " + ce.getName());
        apProfile.setDetails(ApNetworkConfiguration.createWithDefaults());
        apProfile = profileServiceInterface.create(apProfile);

        ApNetworkConfiguration apNetworkConfig = (ApNetworkConfiguration) apProfile.getDetails();
        Map<RadioType, RadioProfileConfiguration> radioProfileMap = new HashMap<>();

        for (String radioBand : connectNodeInfo.wifiRadioStates.keySet()) {

            RadioType radioType = OvsdbToWlanCloudTypeMappingUtility.getRadioTypeForOvsdbRadioFreqBand(radioBand);
            if (!radioType.equals(RadioType.UNSUPPORTED)) {
                radioProfileMap.put(radioType, RadioProfileConfiguration.createWithDefaults(radioType));
            }

        }

        apNetworkConfig.setRadioMap(radioProfileMap);

        apProfile.setDetails(apNetworkConfig);

        apProfile = profileServiceInterface.update(apProfile);

        apNetworkConfig = (ApNetworkConfiguration) apProfile.getDetails();

        Set<RadioType> radioTypes = radioProfileMap.keySet();

        for (RadioType radioType : radioTypes) {
            // SSID Profile init
            Profile ssidProfile = new Profile();
            ssidProfile.setCustomerId(ce.getCustomerId());
            ssidProfile.setName(autoProvisionedSsid + radioType.name() + " for " + ce.getName());
            SsidConfiguration ssidConfig = SsidConfiguration.createWithDefaults();

            ssidConfig.setSsid(ssidProfile.getName());
            ssidConfig.setSsidAdminState(StateSetting.enabled);
            ssidConfig.setBroadcastSsid(StateSetting.enabled);
            ssidConfig.setSecureMode(SecureMode.wpa2PSK);
            ssidConfig.setKeyStr(autoprovisionedSsidKey);

            Set<RadioType> appliedRadios = new HashSet<>();
            appliedRadios.add(radioType);
            ssidConfig.setAppliedRadios(appliedRadios);
            ssidProfile.setDetails(ssidConfig);
            ssidProfile = profileServiceInterface.create(ssidProfile);

            apProfile.getChildProfileIds().add(ssidProfile.getId());
        }

        // RF Profile Init
        Profile rfProfile = new Profile();
        rfProfile.setCustomerId(ce.getCustomerId());
        rfProfile.setName("DefaultRf for " + ce.getName());
        RfConfiguration rfConfig = RfConfiguration.createWithDefaults();

        // Override default values
        for (RadioType radioType : radioTypes) {
            rfConfig.getRfConfig(radioType).setRf(rfProfile.getName());
        }

        rfProfile.setDetails(rfConfig);
        rfProfile = profileServiceInterface.create(rfProfile);

        apProfile.getChildProfileIds().add(rfProfile.getId());

        // Update AP profile with SSID and RF child profiles
        apProfile = profileServiceInterface.update(apProfile);

        return apProfile;
    }

    private void updateApStatus(Equipment ce, ConnectNodeInfo connectNodeInfo) {

        try {

            Status statusRecord = statusServiceInterface.getOrNull(ce.getCustomerId(), ce.getId(),
                    StatusDataType.EQUIPMENT_ADMIN);
            if (statusRecord == null) {
                statusRecord = new Status();
                statusRecord.setCustomerId(ce.getCustomerId());
                statusRecord.setEquipmentId(ce.getId());
                EquipmentAdminStatusData statusData = new EquipmentAdminStatusData();
                statusRecord.setDetails(statusData);
            }

            ((EquipmentAdminStatusData) statusRecord.getDetails()).setStatusCode(StatusCode.normal);
            // Update the equipment admin status
            statusRecord = statusServiceInterface.update(statusRecord);

            // update LAN status - nothing to do here for now
            statusRecord = statusServiceInterface.getOrNull(ce.getCustomerId(), ce.getId(), StatusDataType.LANINFO);
            if (statusRecord == null) {
                statusRecord = new Status();
                statusRecord.setCustomerId(ce.getCustomerId());
                statusRecord.setEquipmentId(ce.getId());

                EquipmentLANStatusData statusData = new EquipmentLANStatusData();
                statusRecord.setDetails(statusData);
            }

            Map<Integer, VLANStatusData> vlanStatusDataMap = new HashMap<>();
            ((EquipmentLANStatusData) statusRecord.getDetails()).setVlanStatusDataMap(vlanStatusDataMap);

            statusServiceInterface.update(statusRecord);

            // update protocol status
            statusRecord = statusServiceInterface.getOrNull(ce.getCustomerId(), ce.getId(), StatusDataType.PROTOCOL);
            if (statusRecord == null) {
                statusRecord = new Status();
                statusRecord.setCustomerId(ce.getCustomerId());
                statusRecord.setEquipmentId(ce.getId());

                EquipmentProtocolStatusData statusData = new EquipmentProtocolStatusData();
                statusRecord.setDetails(statusData);
            }

            EquipmentProtocolStatusData protocolStatusData = (EquipmentProtocolStatusData) statusRecord.getDetails();
            protocolStatusData.setPoweredOn(true);
            protocolStatusData.setCloudProtocolVersion("1100");
            protocolStatusData.setProtocolState(EquipmentProtocolState.ready);
            protocolStatusData.setBandPlan("FCC");
            protocolStatusData.setBaseMacAddress(MacAddress.valueOf(connectNodeInfo.macAddress));
            protocolStatusData.setCloudCfgDataVersion(42L);
            protocolStatusData.setReportedCfgDataVersion(42L);
            protocolStatusData.setCountryCode("CA");
            protocolStatusData.setReportedCC(CountryCode.CA);
            protocolStatusData.setReportedHwVersion(connectNodeInfo.platformVersion);
            if (connectNodeInfo.versionMatrix.containsKey(OvsdbStringConstants.FW_IMAGE_ACTIVE_KEY)) {
                protocolStatusData.setReportedSwVersion(
                        connectNodeInfo.versionMatrix.get(OvsdbStringConstants.FW_IMAGE_ACTIVE_KEY));
            } else if (connectNodeInfo.versionMatrix.containsKey(OvsdbStringConstants.FW_IMAGE_NAME_KEY)) {
                protocolStatusData.setReportedSwVersion(
                        connectNodeInfo.versionMatrix.get(OvsdbStringConstants.FW_IMAGE_NAME_KEY));
            } else {
                protocolStatusData.setReportedSwVersion("Unknown");
            }
            if (connectNodeInfo.versionMatrix.containsKey(OvsdbStringConstants.FW_IMAGE_INACTIVE_KEY)) {
                protocolStatusData.setReportedSwAltVersion(
                        connectNodeInfo.versionMatrix.get(OvsdbStringConstants.FW_IMAGE_INACTIVE_KEY));
            } else if (connectNodeInfo.versionMatrix.containsKey(OvsdbStringConstants.FW_IMAGE_NAME_KEY)) {
                protocolStatusData.setReportedSwVersion(
                        connectNodeInfo.versionMatrix.get(OvsdbStringConstants.FW_IMAGE_NAME_KEY));
            } else {
                protocolStatusData.setReportedSwVersion("Unknown");
            }
            try {
                if (connectNodeInfo.ipV4Address != null) {
                    protocolStatusData.setReportedIpV4Addr(InetAddress.getByName(connectNodeInfo.ipV4Address));
                }
            } catch (UnknownHostException e) {
                LOG.error("Could not set IpV4Addr {} on AP {} due to UnknownHostException ",
                        connectNodeInfo.ipV4Address, ce.getName(), e);
            }
            if ((connectNodeInfo.macAddress != null) && (MacAddress.valueOf(connectNodeInfo.macAddress) != null)) {
                protocolStatusData.setReportedMacAddr(MacAddress.valueOf(connectNodeInfo.macAddress));
            }
            protocolStatusData.setReportedSku(connectNodeInfo.skuNumber);
            protocolStatusData.setSerialNumber(connectNodeInfo.serialNumber);
            protocolStatusData.setSystemName(connectNodeInfo.model);
            statusRecord.setDetails(protocolStatusData);
            Status protocolStatus = statusServiceInterface.update(statusRecord);
            LOG.debug("ProtocolStatus for AP {} updated to {}", ce.getName(), protocolStatus);

            statusRecord = statusServiceInterface.getOrNull(ce.getCustomerId(), ce.getId(), StatusDataType.FIRMWARE);
            if (statusRecord == null) {
                statusRecord = new Status();
                statusRecord.setCustomerId(ce.getCustomerId());
                statusRecord.setEquipmentId(ce.getId());
                EquipmentUpgradeStatusData statusData = new EquipmentUpgradeStatusData();
                statusRecord.setDetails(statusData);
            }

            EquipmentUpgradeStatusData fwUpgradeStatusData = (EquipmentUpgradeStatusData) statusRecord.getDetails();
            if (connectNodeInfo.versionMatrix.containsKey(OvsdbStringConstants.FW_IMAGE_ACTIVE_KEY)) {
                fwUpgradeStatusData.setActiveSwVersion(
                        connectNodeInfo.versionMatrix.get(OvsdbStringConstants.FW_IMAGE_ACTIVE_KEY));
            } else if (connectNodeInfo.versionMatrix.containsKey(OvsdbStringConstants.FW_IMAGE_NAME_KEY)) {
                fwUpgradeStatusData
                        .setActiveSwVersion(connectNodeInfo.versionMatrix.get(OvsdbStringConstants.FW_IMAGE_NAME_KEY));
            } else {
                fwUpgradeStatusData.setActiveSwVersion("Unknown");
            }
            if (connectNodeInfo.versionMatrix.containsKey(OvsdbStringConstants.FW_IMAGE_INACTIVE_KEY)) {
                fwUpgradeStatusData.setAlternateSwVersion(
                        connectNodeInfo.versionMatrix.get(OvsdbStringConstants.FW_IMAGE_INACTIVE_KEY));
            } else if (connectNodeInfo.versionMatrix.containsKey(OvsdbStringConstants.FW_IMAGE_NAME_KEY)) {
                fwUpgradeStatusData.setAlternateSwVersion(
                        connectNodeInfo.versionMatrix.get(OvsdbStringConstants.FW_IMAGE_NAME_KEY));
            } else {
                fwUpgradeStatusData.setAlternateSwVersion("Unknown");
            }

            if (fwUpgradeStatusData.getUpgradeState() == null) {
                fwUpgradeStatusData.setUpgradeState(EquipmentUpgradeState.undefined);
                fwUpgradeStatusData.setUpgradeStartTime(null);

            }

            statusRecord.setDetails(fwUpgradeStatusData);
            statusServiceInterface.update(statusRecord);

            Status networkAdminStatusRec = statusServiceInterface.getOrNull(ce.getCustomerId(), ce.getId(),
                    StatusDataType.NETWORK_ADMIN);
            if (networkAdminStatusRec == null) {
                networkAdminStatusRec = new Status();
                networkAdminStatusRec.setCustomerId(ce.getCustomerId());
                networkAdminStatusRec.setEquipmentId(ce.getId());
                networkAdminStatusRec.setStatusDataType(StatusDataType.NETWORK_ADMIN);
                NetworkAdminStatusData statusData = new NetworkAdminStatusData();
                networkAdminStatusRec.setDetails(statusData);
            }

            NetworkAdminStatusData netAdminStatusData = (NetworkAdminStatusData) networkAdminStatusRec.getDetails();
            netAdminStatusData.setDhcpStatus(StatusCode.normal);
            netAdminStatusData.setCloudLinkStatus(StatusCode.normal);
            netAdminStatusData.setDnsStatus(StatusCode.normal);
            netAdminStatusData.setRadiusStatus(StatusCode.normal);

            networkAdminStatusRec.setDetails(netAdminStatusData);

            networkAdminStatusRec = statusServiceInterface.update(networkAdminStatusRec);

        } catch (Exception e) {
            LOG.error("Exception in updateApStatus", e);
            throw e;
        }

    }

    private void removeNonWifiClients(Equipment ce, ConnectNodeInfo connectNodeInfo) {
        // need to make sure that this AP didn't accidentally get registered as
        // a client previously via a partial DHCP lease event
        LOG.info("Checking for non-wifi client types for Equipment {}", ce);
        Client client = clientServiceInterface.getOrNull(ce.getCustomerId(), ce.getBaseMacAddress());

        if (client != null) {
            ClientSession clientSession = clientServiceInterface.getSessionOrNull(ce.getCustomerId(), ce.getId(),
                    ce.getBaseMacAddress());
            if (clientSession != null) {
                clientSession = clientServiceInterface.deleteSession(ce.getCustomerId(), ce.getId(),
                        client.getMacAddress());
                LOG.info("Removed invalid client session {}", clientSession);
            }
            client = clientServiceInterface.delete(ce.getCustomerId(), client.getMacAddress());
            LOG.info("Removed invalid client type {}", client);
        } else {
            LOG.info("No clients with MAC address {} registered for customer {}", ce.getBaseMacAddress(),
                    ce.getCustomerId());
        }

        LOG.info("Finished checking for and removing non-wifi client types for Equipment {}", ce);

    }

    private void reconcileFwVersionToTrack(Equipment ce, String reportedFwVersionFromAp, String model) {

        LOG.debug("reconcileFwVersionToTrack for AP {} with active firmware version {} model {}", ce.getInventoryId(),
                reportedFwVersionFromAp, model);
        Status statusRecord = statusServiceInterface.getOrNull(ce.getCustomerId(), ce.getId(), StatusDataType.FIRMWARE);

        EquipmentUpgradeStatusData fwUpgradeStatusData = (EquipmentUpgradeStatusData) statusRecord.getDetails();

        // default track settings for firmware
        CustomerFirmwareTrackSettings trackSettings = firmwareServiceInterface.getDefaultCustomerTrackSetting();

        // check for updated/modified track settings for this customer
        CustomerFirmwareTrackRecord custFwTrackRecord = firmwareServiceInterface
                .getCustomerFirmwareTrackRecord(ce.getCustomerId());

        long trackRecordId = -1;
        if (custFwTrackRecord != null) {
            trackSettings = custFwTrackRecord.getSettings();
            trackRecordId = custFwTrackRecord.getTrackRecordId();
        }
        // determine if AP requires FW upgrade before cloud
        // connection/provision
        if (trackSettings.getAutoUpgradeDeprecatedOnBind().equals(TrackFlag.ALWAYS)
                || trackSettings.getAutoUpgradeUnknownOnBind().equals(TrackFlag.ALWAYS)) {

            LOG.debug("reconcileFwVersionToTrack for AP {} track flag for auto-upgrade {}", ce.getInventoryId(),
                    trackSettings.getAutoUpgradeDeprecatedOnBind());

            // check the reported fw version for the AP, if it is < than
            // the default version for the cloud, then download and
            // flash the firmware before proceeding.
            // then return;
            FirmwareTrackRecord fwTrackRecord = null;
            if (trackRecordId == -1) {
                // take the default
                fwTrackRecord = firmwareServiceInterface.getFirmwareTrackByName(FirmwareTrackRecord.DEFAULT_TRACK_NAME);

            } else {
                // there must be a customer one
                fwTrackRecord = firmwareServiceInterface.getFirmwareTrackById(trackRecordId);
            }

            if (fwTrackRecord != null) {

                LOG.debug("reconcileFwVersionToTrack for AP {} firmwareTrackRecord {}", ce.getInventoryId(),
                        fwTrackRecord);

                List<FirmwareTrackAssignmentDetails> fwTrackAssignmentDetails = firmwareServiceInterface
                        .getFirmwareTrackAssignments(fwTrackRecord.getTrackName());

                String targetFwVersionNameForTrack = null;

                if (fwTrackAssignmentDetails != null) {
                    for (FirmwareTrackAssignmentDetails details : fwTrackAssignmentDetails) {
                        if (model.equalsIgnoreCase(details.getModelId())) {
                            targetFwVersionNameForTrack = details.getVersionName();
                            break;
                        }
                    }
                }

                if (targetFwVersionNameForTrack == null) {
                    LOG.info("No target FW version for this track {}", fwTrackRecord);

                } else {
                    LOG.debug("reconcileFwVersionToTrack for AP {} targetFwVersion for track {}", ce.getInventoryId(),
                            targetFwVersionNameForTrack);

                    if (reportedFwVersionFromAp != null) {
                        if (!targetFwVersionNameForTrack.equals(reportedFwVersionFromAp)) {
                            LOG.debug(
                                    "reconcileFwVersionToTrack for AP {} targetFwVersion {} doesn't match reported fw version {}, triggering download and flash",
                                    ce.getInventoryId(), targetFwVersionNameForTrack, reportedFwVersionFromAp);

                            fwUpgradeStatusData.setTargetSwVersion(targetFwVersionNameForTrack);
                            fwUpgradeStatusData.setUpgradeState(EquipmentUpgradeState.out_of_date);
                            statusRecord.setDetails(fwUpgradeStatusData);
                            statusRecord = statusServiceInterface.update(statusRecord);
                            triggerFwDownload(ce, fwUpgradeStatusData, trackSettings);
                        } else if (targetFwVersionNameForTrack.equals(reportedFwVersionFromAp)) {
                            LOG.debug("reconcileFwVersionToTrack for AP {} targetFwVersion {} is active",
                                    ce.getInventoryId(), targetFwVersionNameForTrack);

                            fwUpgradeStatusData.setUpgradeState(EquipmentUpgradeState.up_to_date);
                            fwUpgradeStatusData.setActiveSwVersion(targetFwVersionNameForTrack);
                            fwUpgradeStatusData.setAlternateSwVersion(targetFwVersionNameForTrack);
                            fwUpgradeStatusData.setTargetSwVersion(targetFwVersionNameForTrack);

                            statusRecord.setDetails(fwUpgradeStatusData);
                            statusRecord = statusServiceInterface.update(statusRecord);

                        }
                    }

                }

            }

        } else

        {
            LOG.debug("Automatic firmware upgrade is not configured for track {}", trackSettings);
        }
    }

    private void triggerFwDownload(Equipment ce, EquipmentUpgradeStatusData fwUpgradeStatusData,
            CustomerFirmwareTrackSettings trackSettings) {
        LOG.debug("triggerFwDownloadAndFlash Automatic firmware upgrade is configured for track {}.", trackSettings);

        try {
            FirmwareVersion fwVersion = firmwareServiceInterface
                    .getFirmwareVersionByName(fwUpgradeStatusData.getTargetSwVersion());

            if (fwVersion != null) {
                OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(ce.getInventoryId());
                if (ovsdbSession == null) {
                    throw new IllegalStateException("AP is not connected " + ce.getInventoryId());
                }

                CEGWFirmwareDownloadRequest fwDownloadRequest = new CEGWFirmwareDownloadRequest(ce.getInventoryId(),
                        ce.getId(), fwVersion.getVersionName(), fwVersion.getFilename(),
                        fwVersion.getValidationMethod(), fwVersion.getValidationCode());
                List<CEGWBaseCommand> commands = new ArrayList<>();
                commands.add(fwDownloadRequest);

                gatewayController.updateActiveCustomer(ce.getCustomerId());
                ListOfEquipmentCommandResponses responses = gatewayController.sendCommands(commands);
                LOG.debug("FW Download Response {}", responses);

            }
        } catch (Exception e) {
            LOG.error("Cannot trigger FW download {}", e);
        }
    }

    @Override
    public void apDisconnected(String apId) {
        LOG.info("AP {} got disconnected from the gateway", apId);
        try {

            OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

            if (ovsdbSession != null) {
                if (ovsdbSession.getRoutingId() > 0L) {
                    try {
                        routingServiceInterface.delete(ovsdbSession.getRoutingId());
                    } catch (Exception e) {
                        LOG.warn("Unable to delete routing service Id {} for ap {}. {}", ovsdbSession.getRoutingId(),
                                apId, e);
                    }
                }
            } else {
                LOG.warn("Cannot find ap {} in inventory", apId);
            }
            Equipment ce = equipmentServiceInterface.getByInventoryIdOrNull(apId);
            if (ce != null) {
                LOG.info("AP {} disconnected, delete status records {}", apId,
                        statusServiceInterface.delete(ce.getCustomerId(), ce.getId()));
                updateApDisconnectedStatus(apId, ce);
                disconnectClients(ce);
            } else {
                LOG.error(
                        "updateDisconnectedApStatus::Cannot get Equipment for AP {} to update the EquipmentAdminStatus",
                        apId);

            }
        } catch (Exception e) {
            LOG.error("Exception when registering ap routing {}", apId, e);
        }

    }

    private void updateApDisconnectedStatus(String apId, Equipment ce) {
        LOG.info("updateApDisconnectedStatus disconnected AP {}", apId);
        try {
            Status statusRecord = new Status();
            statusRecord.setCustomerId(ce.getCustomerId());
            statusRecord.setEquipmentId(ce.getId());
            statusRecord.setStatusDataType(StatusDataType.EQUIPMENT_ADMIN);

            EquipmentAdminStatusData statusData = new EquipmentAdminStatusData();
            statusData.setStatusMessage("AP Disconnected");
            statusData.setStatusCode(StatusCode.disabled);

            statusRecord.setDetails(statusData);

            LOG.info("Updated EquipmentAdminStatus {} for disconnected AP {}",
                    statusServiceInterface.update(statusRecord), apId);

        } catch (Exception e) {
            LOG.error("Exception in updateApDisconnectedStatus", e);
            throw e;
        }
    }

    private void disconnectClients(Equipment ce) {

        LOG.info("OpensyncExternalIntegrationCloud::disconnectClients for Equipment {}", ce);
        PaginationResponse<ClientSession> clientSessions = clientServiceInterface.getSessionsForCustomer(
                ce.getCustomerId(), Set.of(ce.getId()), Set.of(ce.getLocationId()), null,
                new PaginationContext<ClientSession>(100));

        if (clientSessions == null) {
            LOG.info("There are no existing client sessions to disconnect.");
            return;
        }

        List<ClientSession> toBeDisconnected = new ArrayList<>();

        clientSessions.getItems().stream().forEach(c -> {
            if (!c.getDetails().getAssociationState().equals(AssociationState.Disconnected)) {
                LOG.info("Change association state for client {} from {} to {}", c.getMacAddress(),
                        c.getDetails().getAssociationState(), AssociationState.Disconnected);

                c.getDetails().setAssociationState(AssociationState.Disconnected);
                toBeDisconnected.add(c);

            }
        });

        if (!toBeDisconnected.isEmpty()) {
            LOG.info("Sending disconnect for client sessions {}", toBeDisconnected);
            List<ClientSession> disconnectedSessions = clientServiceInterface.updateSessions(toBeDisconnected);
            LOG.info("Result of client disconnect {}", disconnectedSessions);
        } else {
            LOG.info("There are no existing client sessions that are not already in Disconnected state.");
        }

    }

    @Override
    public OpensyncAPConfig getApConfig(String apId) {
        LOG.info("Retrieving config for AP {} ", apId);
        OpensyncAPConfig ret = null;

        try {

            OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);
            if (ovsdbSession == null) {
                throw new IllegalStateException("AP is not connected " + apId);
            }
            int customerId = ovsdbSession.getCustomerId();

            Equipment equipmentConfig = equipmentServiceInterface.getByInventoryIdOrNull(apId);
            if (equipmentConfig == null) {
                throw new IllegalStateException("Cannot retrieve configuration for " + apId);
            }

            ret = new OpensyncAPConfig();

            ret.setCustomerEquipment(equipmentConfig);

            Location eqLocation = locationServiceInterface.get(equipmentConfig.getLocationId());

            ret.setEquipmentLocation(eqLocation);

            ProfileContainer profileContainer = new ProfileContainer(
                    profileServiceInterface.getProfileWithChildren(equipmentConfig.getProfileId()));

            ret.setApProfile(profileContainer.getOrNull(equipmentConfig.getProfileId()));

            ret.setRfProfile(profileContainer.getChildOfTypeOrNull(equipmentConfig.getProfileId(), ProfileType.rf));

            ret.setSsidProfile(profileContainer.getChildrenOfType(equipmentConfig.getProfileId(), ProfileType.ssid));

            ret.setMetricsProfiles(profileContainer.getChildrenOfType(equipmentConfig.getProfileId(),
                    ProfileType.service_metrics_collection_config));

            Set<Profile> radiusSet = new HashSet<>();
            Set<Long> captiveProfileIds = new HashSet<>();
            Set<Long> bonjourGatewayProfileIds = new HashSet<>();

            OpensyncAPHotspot20Config hotspotConfig = new OpensyncAPHotspot20Config();

            Set<Profile> hotspot20ProfileSet = new HashSet<>();
            Set<Profile> hotspot20OperatorSet = new HashSet<>();
            Set<Profile> hotspot20VenueSet = new HashSet<>();
            Set<Profile> hotspot20ProviderSet = new HashSet<>();

            for (Profile ssidProfile : ret.getSsidProfile()) {

                hotspot20ProfileSet
                        .addAll(profileContainer.getChildrenOfType(ssidProfile.getId(), ProfileType.passpoint));

                radiusSet.addAll(profileContainer.getChildrenOfType(ret.getApProfile().getId(), ProfileType.radius));
                if (ssidProfile.getDetails() != null) {
                    Long captivePortId = ((SsidConfiguration) ssidProfile.getDetails()).getCaptivePortalId();
                    if (captivePortId != null) {
                        captiveProfileIds.add(captivePortId);
                    }
                    Long bonjourGatewayProfileId = ((SsidConfiguration) ssidProfile.getDetails())
                            .getBonjourGatewayProfileId();
                    if (bonjourGatewayProfileId != null) {
                        bonjourGatewayProfileIds.add(bonjourGatewayProfileId);
                    }
                }
            }

            if (hotspot20ProfileSet.size() > 0) {
                for (Profile hotspot20Profile : hotspot20ProfileSet) {
                    hotspot20OperatorSet.addAll(profileContainer.getChildrenOfType(hotspot20Profile.getId(),
                            ProfileType.passpoint_operator));
                    hotspot20VenueSet.addAll(
                            profileContainer.getChildrenOfType(hotspot20Profile.getId(), ProfileType.passpoint_venue));
                    hotspot20ProviderSet.addAll(profileContainer.getChildrenOfType(hotspot20Profile.getId(),
                            ProfileType.passpoint_osu_id_provider));
                }
                hotspotConfig.setHotspot20OperatorSet(hotspot20OperatorSet);
                hotspotConfig.setHotspot20ProfileSet(hotspot20ProfileSet);
                hotspotConfig.setHotspot20ProviderSet(hotspot20ProviderSet);
                hotspotConfig.setHotspot20VenueSet(hotspot20VenueSet);

                ret.setHotspotConfig(hotspotConfig);
            }

            ret.setRadiusProfiles(new ArrayList<>(radiusSet));
            ret.setCaptiveProfiles(profileServiceInterface.get(captiveProfileIds));
            ret.setBonjourGatewayProfiles(profileServiceInterface.get(bonjourGatewayProfileIds));

            List<Client> blockedClients = clientServiceInterface.getBlockedClients(customerId);
            List<MacAddress> blockList = Lists.newArrayList();
            if ((blockedClients != null) && !blockedClients.isEmpty()) {
                blockedClients.forEach(client -> blockList.add(client.getMacAddress()));
            }
            ret.setBlockedClients(blockList);

            LOG.debug("ApConfig {}", ret);

        } catch (Exception e) {
            LOG.error("Cannot read config for AP {}", apId, e);
        }

        return ret;
    }

    @Override
    public void processMqttMessage(String topic, Report report) {
        mqttMessageProcessor.processMqttMessage(topic, report);
    }

    @Override
    public void processMqttMessage(String topic, FlowReport flowReport) {
        mqttMessageProcessor.processMqttMessage(topic, flowReport);
    }

    @Override
    public void processMqttMessage(String topic, WCStatsReport wcStatsReport) {
        mqttMessageProcessor.processMqttMessage(topic, wcStatsReport);
    }

    @Override
    public void wifiVIFStateDbTableUpdate(List<OpensyncAPVIFState> vifStateTables, String apId) {
        LOG.debug("Received Wifi_VIF_State table update for AP {}", apId);

        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession == null) {
            LOG.debug("wifiVIFStateDbTableUpdate::Cannot get Session for AP {}", apId);
            return;
        }

        int customerId = ovsdbSession.getCustomerId();
        long equipmentId = ovsdbSession.getEquipmentId();

        if ((customerId < 0) || (equipmentId < 0)) {
            LOG.debug("wifiVIFStateDbTableUpdate::Cannot get valid CustomerId {} or EquipmentId {} for AP {}",
                    customerId, equipmentId, apId);
            return;
        }

        Equipment apNode = equipmentServiceInterface.getByInventoryIdOrNull(apId);
        if (apNode == null) {
            LOG.debug("wifiVIFStateDbTableUpdate::Cannot get EquipmentId for AP {}", apId);
            return; // we don't have the required info to get the
                    // radio type yet
        }
        ApElementConfiguration apElementConfig = (ApElementConfiguration) apNode.getDetails();

        ProfileContainer profileContainer = new ProfileContainer(
                profileServiceInterface.getProfileWithChildren(apNode.getProfileId()));
        RfConfiguration rfConfig = (RfConfiguration) profileContainer
                .getChildOfTypeOrNull(apNode.getProfileId(), ProfileType.rf).getDetails();

        for (OpensyncAPVIFState vifState : vifStateTables) {

            LOG.debug("Processing vifState for interface {} on AP {}", vifState.getIfName(), apId);

            String bssid = vifState.getMac();

            if ((bssid == null) || bssid.equals("")) {
                LOG.warn("BSSID from AP {} for vif {} is null or empty", apId, vifState.getIfName());
                continue;
            }
            String ssid = vifState.getSsid();

            if ((ssid == null) || ssid.equals("")) {
                LOG.warn("SSID from AP {} interface {} is null or empty", apId, vifState.getIfName());
                continue;
            }

            int numClients = vifState.getAssociatedClients().size();

            int channel = vifState.getChannel();

            if ((channel < 1)) {
                LOG.warn("Channel from AP {} interface {} is null or empty", apId, vifState.getIfName());
                continue;
            }

            LOG.debug("Values from Vif State Mac (BSSID) {} SSID {} AssociatedClients {} Channel {}", bssid, ssid,
                    vifState.getAssociatedClients());

            RadioType radioType = null;
            Map<RadioType, RfElementConfiguration> rfElementMap = rfConfig.getRfConfigMap();
            Map<RadioType, ElementRadioConfiguration> elementRadioMap = apElementConfig.getRadioMap();
            for (RadioType rType : elementRadioMap.keySet()) {
                boolean autoChannelSelection = rfElementMap.get(rType).getAutoChannelSelection();
                if (elementRadioMap.get(rType).getActiveChannel(autoChannelSelection) == channel) {
                    radioType = rType;
                    break;
                }
            }

            updateActiveBssids(customerId, equipmentId, apId, ssid, radioType, bssid, numClients);

        }

        Status activeBssidsStatus = statusServiceInterface.getOrNull(customerId, equipmentId,
                StatusDataType.ACTIVE_BSSIDS);

        ActiveBSSIDs bssidStatusDetails = (ActiveBSSIDs) activeBssidsStatus.getDetails();

        if (activeBssidsStatus != null && bssidStatusDetails != null && bssidStatusDetails.getActiveBSSIDs() != null) {
            updateClientDetailsStatus(customerId, equipmentId, bssidStatusDetails);
        }

        LOG.info("Finished wifiVIFStateDbTableUpdate updated {}", activeBssidsStatus);

    }

    private void updateClientDetailsStatus(int customerId, long equipmentId, ActiveBSSIDs statusDetails) {
        Status clientDetailsStatus = statusServiceInterface.getOrNull(customerId, equipmentId,
                StatusDataType.CLIENT_DETAILS);

        LOG.debug("Processing updateClientDetailsStatus Status for ActiveBSSIDs {}", statusDetails);

        if (clientDetailsStatus == null) {
            clientDetailsStatus = new Status();
            clientDetailsStatus.setCustomerId(customerId);
            clientDetailsStatus.setEquipmentId(equipmentId);
            clientDetailsStatus.setStatusDataType(StatusDataType.CLIENT_DETAILS);
            clientDetailsStatus.setDetails(new ClientConnectionDetails());
            clientDetailsStatus = statusServiceInterface.update(clientDetailsStatus);

            LOG.debug("Processing updateClientDetailsStatus, new ClientDetailsStatus {}", clientDetailsStatus);
        }

        ClientConnectionDetails clientConnectionDetails = (ClientConnectionDetails) clientDetailsStatus.getDetails();

        Map<RadioType, Integer> clientsPerRadioType = new HashMap<>();

        for (ActiveBSSID bssid : statusDetails.getActiveBSSIDs()) {

            if (!clientsPerRadioType.containsKey(bssid.getRadioType())) {
                clientsPerRadioType.put(bssid.getRadioType(), 0);
            }
            int numConnectedForBssid = bssid.getNumDevicesConnected();
            int currentNumberOfClients = clientsPerRadioType.get(bssid.getRadioType());
            clientsPerRadioType.put(bssid.getRadioType(), currentNumberOfClients + numConnectedForBssid);
            LOG.debug("Processing updateClientDetailsStatus. Upgrade numClients for RadioType {} from {} to {}",
                    bssid.getRadioType(), currentNumberOfClients, clientsPerRadioType.get(bssid.getRadioType()));
        }

        clientConnectionDetails.setNumClientsPerRadio(clientsPerRadioType);
        clientDetailsStatus.setDetails(clientConnectionDetails);
        clientDetailsStatus = statusServiceInterface.update(clientDetailsStatus);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Processing updateClientDetailsStatus. Updated clientConnectionDetails to {}",
                    clientDetailsStatus.toPrettyString());
        }

        LOG.info("Finished updateClientDetailsStatus updated {}", clientDetailsStatus);

    }

    @Override
    public void wifiRadioStatusDbTableUpdate(List<OpensyncAPRadioState> radioStateTables, String apId) {
        LOG.debug("Received Wifi_Radio_State table update for AP {}", apId);
        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession == null) {
            LOG.debug("wifiRadioStatusDbTableUpdate::Cannot get Session for AP {}", apId);
            return;
        }

        int customerId = ovsdbSession.getCustomerId();
        long equipmentId = ovsdbSession.getEquipmentId();

        if ((customerId < 0) || (equipmentId < 0)) {
            LOG.debug("wifiRadioStatusDbTableUpdate::Cannot get valid CustomerId {} or EquipmentId {} for AP {}",
                    customerId, equipmentId, apId);
            return;
        }

        Equipment ce = equipmentServiceInterface.getByInventoryIdOrNull(apId);
        if (ce == null) {
            LOG.debug("wifiRadioStatusDbTableUpdate::Cannot get Equipment for AP {}", apId);
            return;
        }

        ApElementConfiguration apElementConfiguration = ((ApElementConfiguration) ce.getDetails());

        Status protocolStatus = null;
        EquipmentProtocolStatusData protocolStatusData = null;

        for (OpensyncAPRadioState radioState : radioStateTables) {
            LOG.debug("Processing Wifi_Radio_State table update for AP {} {}", apId, radioState);

            if (radioState.getFreqBand().equals(RadioType.UNSUPPORTED)) {
                LOG.debug("Could not get radio configuration for AP {}", apId);
                continue;
            }

            if (radioState.getAllowedChannels() != null) {
                apElementConfiguration.getRadioMap().get(radioState.getFreqBand())
                        .setAllowedChannels(new ArrayList<>(radioState.getAllowedChannels()));

                LOG.debug("Updated AllowedChannels from Wifi_Radio_State table change for AP {}", apId);

            }

            if (radioState.getTxPower() > 0) {
                SourceType txPowerSource = apElementConfiguration.getRadioMap().get(radioState.getFreqBand())
                        .getEirpTxPower().getSource();
                // Preserve the source while updating the value
                if (txPowerSource == SourceType.auto) {
                    apElementConfiguration.getRadioMap().get(radioState.getFreqBand())
                            .setEirpTxPower(SourceSelectionValue.createAutomaticInstance(radioState.getTxPower()));
                } else if (txPowerSource == SourceType.profile) {
                    apElementConfiguration.getRadioMap().get(radioState.getFreqBand())
                            .setEirpTxPower(SourceSelectionValue.createProfileInstance(radioState.getTxPower()));
                } else {
                    apElementConfiguration.getRadioMap().get(radioState.getFreqBand())
                            .setEirpTxPower(SourceSelectionValue.createManualInstance(radioState.getTxPower()));
                }

                LOG.debug("Updated TxPower from Wifi_Radio_State table change for AP {}", apId);
            }

            StateSetting state = StateSetting.disabled;
            if (radioState.isEnabled()) {
                state = StateSetting.enabled;
            }

            if (!apElementConfiguration.getAdvancedRadioMap().get(radioState.getFreqBand()).getRadioAdminState()
                    .equals(state)) {
                // only update if changed
                apElementConfiguration.getAdvancedRadioMap().get(radioState.getFreqBand()).setRadioAdminState(state);

                LOG.debug("Updated RadioAdminState from Wifi_Radio_State table change for AP {}", apId);

            }

            protocolStatus = statusServiceInterface.getOrNull(customerId, equipmentId, StatusDataType.PROTOCOL);

            if (protocolStatus != null) {

                protocolStatusData = (EquipmentProtocolStatusData) protocolStatus.getDetails();
                if (!protocolStatusData.getReportedCC().equals(CountryCode.getByName((radioState.getCountry())))) {

                    LOG.debug(
                            "Protocol Status reportedCC {} radioStatus.getCountry {} radioStatus CountryCode fromName {}",
                            protocolStatusData.getReportedCC(), radioState.getCountry(),
                            CountryCode.getByName((radioState.getCountry())));
                    protocolStatusData.setReportedCC(CountryCode.getByName((radioState.getCountry())));
                    protocolStatus.setDetails(protocolStatusData);

                } else {
                    protocolStatus = null;
                }

            }

        }

        if (protocolStatus != null) {
            statusServiceInterface.update(protocolStatus);
        }

        ce = equipmentServiceInterface.getByInventoryIdOrNull(apId);
        if (ce == null) {
            LOG.debug("wifiRadioStatusDbTableUpdate::Cannot get Equipment for AP {}", apId);
            return;
        }

        try {

            if (!apElementConfiguration.equals((ce.getDetails()))) {

                ((ApElementConfiguration) ce.getDetails()).setRadioMap(apElementConfiguration.getRadioMap());
                ((ApElementConfiguration) ce.getDetails())
                        .setAdvancedRadioMap(apElementConfiguration.getAdvancedRadioMap());

                apElementConfiguration = (ApElementConfiguration) ce.getDetails();
                ce = equipmentServiceInterface.update(ce);
            }
        } catch (DsConcurrentModificationException e) {
            LOG.debug("Equipment reference changed, update instance and retry.", e.getMessage());
            ce = equipmentServiceInterface.getByInventoryIdOrNull(apId);
            ce.setDetails(apElementConfiguration);
            ce = equipmentServiceInterface.update(ce);
        }
        LOG.info("Finished wifiRadioStateDbTableUpdate");

    }

    private void updateActiveBssids(int customerId, long equipmentId, Object apId, String ssid, RadioType freqBand,
            String macAddress, int numClients) {
        Status activeBssidsStatus = statusServiceInterface.getOrNull(customerId, equipmentId,
                StatusDataType.ACTIVE_BSSIDS);

        if (activeBssidsStatus == null) {
            activeBssidsStatus = new Status();
            activeBssidsStatus.setCustomerId(customerId);
            activeBssidsStatus.setEquipmentId(equipmentId);
            activeBssidsStatus.setStatusDataType(StatusDataType.ACTIVE_BSSIDS);

            ActiveBSSIDs statusDetails = new ActiveBSSIDs();
            statusDetails.setActiveBSSIDs(new ArrayList<ActiveBSSID>());

            activeBssidsStatus.setDetails(statusDetails);

            activeBssidsStatus = statusServiceInterface.update(activeBssidsStatus);
            LOG.debug("Processing Wifi_VIF_State table update for AP {}, created new ACTIVE_BSSID Status {}", apId,
                    activeBssidsStatus);

        }

        ActiveBSSIDs statusDetails = (ActiveBSSIDs) activeBssidsStatus.getDetails();

        LOG.debug("Processing Wifi_VIF_State table update for AP {}, activeBSSIDs StatusDetails before update {}", apId,
                statusDetails);

        List<ActiveBSSID> currentActiveBSSIDs = statusDetails.getActiveBSSIDs();
        if (currentActiveBSSIDs == null) {
            currentActiveBSSIDs = new ArrayList<>();
        } else {
            currentActiveBSSIDs = currentActiveBSSIDs.stream()
                    .filter(p -> (p.getRadioType() != null && p.getSsid() != null)).filter(p -> !p.getRadioType().equals(freqBand) || !p.getSsid().equals(ssid))
                    .collect(Collectors.toList());
            LOG.debug(
                    "Processing Wifi_VIF_State table update for AP {}, activeBSSIDs bssidList without current radio freq {} and ssid {}",
                    apId, currentActiveBSSIDs, ssid);
        }

        ActiveBSSID activeBssid = new ActiveBSSID();
        activeBssid.setBssid(macAddress);
        activeBssid.setSsid(ssid);
        activeBssid.setRadioType(freqBand);
        activeBssid.setNumDevicesConnected(numClients);
        currentActiveBSSIDs.add(activeBssid);

        statusDetails.setActiveBSSIDs(currentActiveBSSIDs);
        activeBssidsStatus.setDetails(statusDetails);

        activeBssidsStatus = statusServiceInterface.update(activeBssidsStatus);

        LOG.info("Processing Wifi_VIF_State table update for AP {}, updated ACTIVE_BSSID Status {}", apId,
                activeBssidsStatus);

    }

    @Override
    public void wifiInetStateDbTableUpdate(List<OpensyncAPInetState> inetStateTables, String apId) {

        LOG.debug("Received Wifi_Inet_State table update for AP {}", apId);

        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession == null) {
            LOG.debug("wifiInetStateDbTableUpdate::Cannot get Session for AP {}", apId);
            return;
        }

        int customerId = ovsdbSession.getCustomerId();
        long equipmentId = ovsdbSession.getEquipmentId();

        if ((customerId < 0) || (equipmentId < 0)) {
            LOG.debug("wifiInetStateDbTableUpdate::Cannot get valid CustomerId {} or EquipmentId {} for AP {}",
                    customerId, equipmentId, apId);
            return;
        }

        Equipment ce = equipmentServiceInterface.getOrNull(equipmentId);

        if (ce == null) {
            LOG.debug("wifiInetStateDbTableUpdate Cannot get customer Equipment for {}", apId);
            return;
        }

        Status lanStatus = statusServiceInterface.getOrNull(customerId, equipmentId, StatusDataType.LANINFO);
        if (lanStatus == null) {
            lanStatus = new Status();
            lanStatus.setCustomerId(customerId);
            lanStatus.setEquipmentId(equipmentId);
            lanStatus.setStatusDataType(StatusDataType.LANINFO);
            lanStatus.setDetails(new EquipmentLANStatusData());
            lanStatus = statusServiceInterface.update(lanStatus);
        }

        EquipmentLANStatusData lanStatusData = (EquipmentLANStatusData) lanStatus.getDetails();

        Status protocolStatus = statusServiceInterface.getOrNull(customerId, equipmentId, StatusDataType.PROTOCOL);
        if (protocolStatus == null) {
            protocolStatus = new Status();
            protocolStatus.setCustomerId(customerId);
            protocolStatus.setEquipmentId(equipmentId);
            protocolStatus.setStatusDataType(StatusDataType.PROTOCOL);
            protocolStatus.setDetails(new EquipmentProtocolStatusData());
            protocolStatus = statusServiceInterface.update(protocolStatus);
        }

        EquipmentProtocolStatusData protocolStatusData = (EquipmentProtocolStatusData) protocolStatus.getDetails();

        for (OpensyncAPInetState inetState : inetStateTables) {

            if (inetState.ifName != null && inetState.ifName.equals(defaultWanInterfaceName)) {

                if (inetState.inetAddr != null) {
                    try {
                        protocolStatusData.setReportedIpV4Addr(Inet4Address.getByName(inetState.inetAddr));
                        protocolStatus.setDetails(protocolStatusData);
                        protocolStatus = statusServiceInterface.update(protocolStatus);
                        LOG.info("Updated IpV4Addr for AP {} to {} from Wifi_Inet_State change for if_name {}", apId,
                                ((EquipmentProtocolStatusData) protocolStatus.getDetails()).getReportedIpV4Addr(),
                                inetState.ifName);
                        LOG.debug("ProtocolStatus for AP {} updated to {}", apId, protocolStatus);

                    } catch (UnknownHostException e) {
                        LOG.error("Could not set IpV4Addr {} on AP {} due to UnknownHostException ", inetState.inetAddr,
                                apId, e);
                    }
                }

            }

            if (inetState.getIfType().equals("vlan") && inetState.parentIfName != null
                    && inetState.parentIfName.equals(defaultWanInterfaceName)) {

                try {

                    VLANStatusData vlanStatusData = new VLANStatusData();

                    if (inetState.gateway != null)
                        vlanStatusData.setGateway(InetAddress.getByName(inetState.getGateway()));

                    if (inetState.dns != null) {
                        String primaryDns = inetState.dns.get("primary");
                        if (primaryDns != null) {
                            vlanStatusData.setDnsServer1(InetAddress.getByName(primaryDns));
                        }
                        String secondaryDns = inetState.dns.get("secondary");
                        if (secondaryDns != null) {
                            vlanStatusData.setDnsServer2(InetAddress.getByName(secondaryDns));
                        }
                    }

                    if (inetState.netmask != null) {
                        vlanStatusData.setSubnetMask(InetAddress.getByName(inetState.netmask));
                    }
                    if (inetState.dhcpd != null) {
                        String dhcpOption = inetState.dhcpd.get("dhcp_option");
                        if (dhcpOption != null) {
                            String dhcpServer = dhcpOption.split(",")[1];
                            if (dhcpServer != null) {
                                vlanStatusData.setDhcpServer(InetAddress.getByName(dhcpServer));
                            }
                        }
                    }

                    String inetAddr = inetState.getInetAddr();
                    if (inetAddr != null) {
                        vlanStatusData.setIpBase(InetAddress.getByName(inetAddr));
                    }
                    lanStatusData.getVlanStatusDataMap().put(inetState.vlanId, vlanStatusData);
                    lanStatus.setDetails(lanStatusData);
                    lanStatus = statusServiceInterface.update(lanStatus);

                    LOG.info("LANINFO updated for VLAN {}", lanStatus);

                } catch (UnknownHostException e) {
                    LOG.error("Unknown Host while configuring LANINFO", e);
                }

            }
        }

    }

    @Override
    public void wifiInetStateDbTableDelete(List<OpensyncAPInetState> inetStateTables, String apId) {

        LOG.debug("Received Wifi_Inet_State table delete for AP {}", apId);

    }

    @Override
    public void wifiAssociatedClientsDbTableUpdate(List<OpensyncWifiAssociatedClients> wifiAssociatedClients,
            String apId) {

        LOG.info("Received wifiAssociatedClientsDbTableUpdate monitor notification for Client(s) {} on AP {}",
                wifiAssociatedClients, apId);

        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession == null) {
            LOG.debug("wifiAssociatedClientsDbTableUpdate::Cannot get Session for AP {}", apId);
            return;
        }

        int customerId = ovsdbSession.getCustomerId();
        long equipmentId = ovsdbSession.getEquipmentId();

        if ((customerId < 0) || (equipmentId < 0)) {
            LOG.debug("wifiAssociatedClientsDbTableUpdate::Cannot get valid CustomerId {} or EquipmentId {} for AP {}",
                    customerId, equipmentId, apId);
            return;
        }

        Equipment ce = equipmentServiceInterface.getOrNull(equipmentId);

        if (ce == null) {
            LOG.debug("wifiAssociatedClientsDbTableUpdate Cannot get customer Equipment for {}", apId);
            return;
        }

        if ((wifiAssociatedClients == null) || wifiAssociatedClients.isEmpty()) {
            return;
        }

        for (OpensyncWifiAssociatedClients opensyncWifiAssociatedClients : wifiAssociatedClients) {

            LOG.info("opensyncWifiAssociatedClients {}", opensyncWifiAssociatedClients.toPrettyString());

            String mMac = opensyncWifiAssociatedClients.mac;
            MacAddress macAddress = MacAddress.valueOf(mMac);

            Client clientInstance = clientServiceInterface.getOrNull(customerId, macAddress);

            boolean isReassociation = true;
            if (clientInstance == null) {
                clientInstance = new Client();

                clientInstance.setCustomerId(customerId);
                clientInstance.setMacAddress(MacAddress.valueOf(mMac));
                clientInstance.setDetails(new ClientInfoDetails());
                clientInstance = clientServiceInterface.create(clientInstance);

                isReassociation = false;

            }

            clientInstance = clientServiceInterface.update(clientInstance);

            ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId,
                    clientInstance.getMacAddress());

            if (clientSession == null) {
                clientSession = new ClientSession();
                clientSession.setCustomerId(customerId);
                clientSession.setEquipmentId(equipmentId);
                clientSession.setMacAddress(clientInstance.getMacAddress());
                clientSession.setLocationId(ce.getLocationId());
                ClientSessionDetails clientSessionDetails = new ClientSessionDetails();
                clientSessionDetails.setIsReassociation(isReassociation);
                clientSession.setDetails(clientSessionDetails);
                clientSession = clientServiceInterface.updateSession(clientSession);
            }

            ClientSessionDetails clientSessionDetails = clientSession.getDetails();
            clientSessionDetails.setAssociationState(AssociationState._802_11_Associated);
            clientSessionDetails.setAssocTimestamp(System.currentTimeMillis());
            clientSession.getDetails().mergeSession(clientSessionDetails);

            clientSession = clientServiceInterface.updateSession(clientSession);

        }

    }

    @Override
    public void awlanNodeDbTableUpdate(OpensyncAWLANNode opensyncAPState, String apId) {

        LOG.debug("AP {} table AWLAN_Node updated {}", apId, opensyncAPState);
        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession == null) {
            LOG.info("awlanNodeDbTableUpdate::Cannot get Session for AP {}", apId);
            return;
        }

        int customerId = ovsdbSession.getCustomerId();
        long equipmentId = ovsdbSession.getEquipmentId();

        if ((customerId < 0) || (equipmentId < 0)) {
            LOG.info("awlanNodeDbTableUpdate::Cannot get valid CustomerId {} or EquipmentId {} for AP {}", customerId,
                    equipmentId, apId);
            return;
        }

        Equipment ce = equipmentServiceInterface.getByInventoryIdOrNull(apId);
        if (ce == null) {
            LOG.info("awlanNodeDbTableUpdate::Cannot find AP {}", apId);
            return;
        }

        int upgradeStatusFromAp = opensyncAPState.getUpgradeStatus();
        EquipmentUpgradeState fwUpgradeState = null;
        FailureReason fwUpgradeFailureReason = null;

        if (opensyncAPState.getFirmwareUrl().equals(OvsdbStringConstants.OVSDB_AWLAN_AP_FACTORY_RESET)
                || opensyncAPState.getFirmwareUrl().equals(OvsdbStringConstants.OVSDB_AWLAN_AP_REBOOT)
                || opensyncAPState.getFirmwareUrl().equals(OvsdbStringConstants.OVSDB_AWLAN_AP_SWITCH_SOFTWARE_BANK)
                || opensyncAPState.getFirmwareUrl().equals("")) {

            LOG.debug("Firmware Url {}, no fwUpgradeState", opensyncAPState.getFirmwareUrl());
        } else {
            fwUpgradeState = OvsdbToWlanCloudTypeMappingUtility
                    .getCloudEquipmentUpgradeStateFromOpensyncUpgradeStatus(upgradeStatusFromAp);

            if (upgradeStatusFromAp < 0) {
                fwUpgradeFailureReason = OvsdbToWlanCloudTypeMappingUtility
                        .getCloudEquipmentUpgradeFailureReasonFromOpensyncUpgradeStatus(upgradeStatusFromAp);
            }
        }

        String reportedFwImageName = null;
        String reportedAltFwImageName = null;

        if (opensyncAPState.getVersionMatrix().containsKey(OvsdbStringConstants.FW_IMAGE_ACTIVE_KEY)) {
            reportedFwImageName = opensyncAPState.getVersionMatrix().get(OvsdbStringConstants.FW_IMAGE_ACTIVE_KEY);
        } else {
            reportedFwImageName = opensyncAPState.getVersionMatrix().get(OvsdbStringConstants.FW_IMAGE_NAME_KEY);

        }

        if (opensyncAPState.getVersionMatrix().containsKey(OvsdbStringConstants.FW_IMAGE_INACTIVE_KEY)) {
            reportedAltFwImageName = opensyncAPState.getVersionMatrix().get(OvsdbStringConstants.FW_IMAGE_INACTIVE_KEY);
        } else {
            reportedAltFwImageName = opensyncAPState.getVersionMatrix().get(OvsdbStringConstants.FW_IMAGE_NAME_KEY);

        }
        List<Status> updates = new ArrayList<>();

        Status protocolStatus = configureProtocolStatus(opensyncAPState, customerId, equipmentId, reportedFwImageName,
                reportedAltFwImageName);
        if (protocolStatus != null) {
            updates.add(protocolStatus);
        }

        Status firmwareStatus = configureFirmwareStatus(customerId, equipmentId, fwUpgradeState, fwUpgradeFailureReason,
                reportedFwImageName, reportedAltFwImageName);
        if (firmwareStatus != null) {
            updates.add(firmwareStatus);
        }

        if (!updates.isEmpty()) {// may be some updates from
            // protocol
            // status
            updates = statusServiceInterface.update(updates);
        }
    }

    private Status configureProtocolStatus(OpensyncAWLANNode opensyncAPState, int customerId, long equipmentId,
            String reportedSwImageName, String reportedAltSwImageName) {
        Status protocolStatus = statusServiceInterface.getOrNull(customerId, equipmentId, StatusDataType.PROTOCOL);
        if (protocolStatus != null) {
            EquipmentProtocolStatusData protocolStatusData = ((EquipmentProtocolStatusData) protocolStatus
                    .getDetails());
            if (protocolStatusData.getReportedSku() != null
                    && protocolStatusData.getReportedSku().equals(opensyncAPState.getSkuNumber())
                    && protocolStatusData.getReportedSwVersion() != null
                    && protocolStatusData.getReportedSwVersion().equals(reportedSwImageName)
                    && protocolStatusData.getReportedSwAltVersion() != null
                    && protocolStatusData.getReportedSwAltVersion().equals(reportedAltSwImageName)
                    && protocolStatusData.getReportedHwVersion() != null
                    && protocolStatusData.getReportedHwVersion().equals(opensyncAPState.getPlatformVersion())
                    && protocolStatusData.getSystemName() != null
                    && protocolStatusData.getSystemName().equals(opensyncAPState.getModel())) {
                // no changes
                return null;
            }
            protocolStatusData.setReportedSku(opensyncAPState.getSkuNumber());
            if (reportedSwImageName != null) {
                protocolStatusData.setReportedSwVersion(reportedSwImageName);
            } else {
                protocolStatusData.setReportedSwVersion("Unknown");
            }
            if (reportedAltSwImageName != null) {
                protocolStatusData.setReportedSwAltVersion(reportedAltSwImageName);
            } else {
                protocolStatusData.setReportedSwAltVersion("Unknown");
            }
            protocolStatusData.setReportedHwVersion(opensyncAPState.getPlatformVersion());
            protocolStatusData.setSystemName(opensyncAPState.getModel());
        }
        return protocolStatus;
    }

    private Status configureFirmwareStatus(int customerId, long equipmentId, EquipmentUpgradeState fwUpgradeState,
            FailureReason fwUpgradeFailureReason, String reportedFwImageName, String reportedAltFwImageName) {

        Status firmwareStatus = statusServiceInterface.getOrNull(customerId, equipmentId, StatusDataType.FIRMWARE);
        if (firmwareStatus != null) {
            EquipmentUpgradeStatusData upgradeStatusData = (EquipmentUpgradeStatusData) firmwareStatus.getDetails();
            if (upgradeStatusData.getActiveSwVersion() != null
                    && upgradeStatusData.getActiveSwVersion().equals(reportedFwImageName)
                    && upgradeStatusData.getAlternateSwVersion() != null
                    && upgradeStatusData.getAlternateSwVersion().equals(reportedAltFwImageName)
                    && upgradeStatusData.getUpgradeState() != null
                    && upgradeStatusData.getUpgradeState().equals(fwUpgradeState)) {
                return null; // no changes
            }
            if (reportedFwImageName != null) {
                upgradeStatusData.setActiveSwVersion(reportedFwImageName);
            } else {
                upgradeStatusData.setActiveSwVersion("Unknown");

            }
            if (reportedAltFwImageName != null) {
                upgradeStatusData.setAlternateSwVersion(reportedAltFwImageName);
            } else {
                ((EquipmentUpgradeStatusData) firmwareStatus.getDetails()).setAlternateSwVersion("Unknown");
            }

            if (fwUpgradeState == null)
                fwUpgradeState = EquipmentUpgradeState.undefined;

            if (fwUpgradeState.equals(EquipmentUpgradeState.up_to_date)) {
                LOG.info("Firmware load is up to date.");
                upgradeStatusData.setUpgradeState(fwUpgradeState);
                firmwareStatus.setDetails(upgradeStatusData);
            } else if (fwUpgradeState.equals(EquipmentUpgradeState.download_complete)
                    || fwUpgradeState.equals(EquipmentUpgradeState.apply_complete)
                    || fwUpgradeState.equals(EquipmentUpgradeState.apply_initiated)
                    || fwUpgradeState.equals(EquipmentUpgradeState.applying)
                    || fwUpgradeState.equals(EquipmentUpgradeState.downloading)
                    || fwUpgradeState.equals(EquipmentUpgradeState.download_initiated)
                    || fwUpgradeState.equals(EquipmentUpgradeState.reboot_initiated)
                    || fwUpgradeState.equals(EquipmentUpgradeState.rebooting)) {

                LOG.info("Firmware upgrade is in state {}", fwUpgradeState);

                upgradeStatusData.setUpgradeState(fwUpgradeState);
                if (fwUpgradeState.equals(EquipmentUpgradeState.apply_initiated)) {
                    upgradeStatusData.setUpgradeStartTime(System.currentTimeMillis());
                } else if (fwUpgradeState.equals(EquipmentUpgradeState.reboot_initiated)
                        || fwUpgradeState.equals(EquipmentUpgradeState.rebooting)) {
                    upgradeStatusData.setRebooted(true);
                }
                firmwareStatus.setDetails(upgradeStatusData);
            } else if (fwUpgradeState.equals(EquipmentUpgradeState.apply_failed)
                    || fwUpgradeState.equals(EquipmentUpgradeState.download_failed)
                    || fwUpgradeState.equals(EquipmentUpgradeState.reboot_failed)) {
                LOG.warn("Firmware upgrade is in a failed state {} due to {}", fwUpgradeState, fwUpgradeFailureReason);

                upgradeStatusData.setUpgradeState(fwUpgradeState, fwUpgradeFailureReason);
                firmwareStatus.setDetails(upgradeStatusData);
            } else {

                ((EquipmentUpgradeStatusData) firmwareStatus.getDetails())
                        .setUpgradeState(EquipmentUpgradeState.undefined);
                ((EquipmentUpgradeStatusData) firmwareStatus.getDetails()).setUpgradeStartTime(null);
            }
        }
        return firmwareStatus;
    }

    @Override
    public void wifiVIFStateDbTableDelete(List<OpensyncAPVIFState> vifStateTables, String apId) {

        LOG.info("wifiVIFStateDbTableDelete for AP {} rows {}", apId, vifStateTables);
        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession == null) {
            LOG.debug("wifiVIFStateDbTableDelete::Cannot get Session for AP {}", apId);
            return;
        }

        int customerId = ovsdbSession.getCustomerId();
        long equipmentId = ovsdbSession.getEquipmentId();

        if ((customerId < 0) || (equipmentId < 0)) {
            LOG.debug("wifiVIFStateDbTableDelete::Cannot get valid CustomerId {} or EquipmentId {} for AP {}",
                    customerId, equipmentId, apId);
            return;
        }

        Status activeBssidsStatus = statusServiceInterface.getOrNull(customerId, equipmentId,
                StatusDataType.ACTIVE_BSSIDS);

        if (activeBssidsStatus == null) {
            return; // nothing to delete

        }

        ActiveBSSIDs statusDetails = (ActiveBSSIDs) activeBssidsStatus.getDetails();

        List<ActiveBSSID> bssidList = statusDetails.getActiveBSSIDs();
        List<ActiveBSSID> toBeDeleted = new ArrayList<>();
        Equipment ce = equipmentServiceInterface.getOrNull(equipmentId);
        List<ClientSession> clientSessionsForCustomerAndEquipment = new ArrayList<>();
        if (ce != null) {
            PaginationResponse<ClientSession> clientSessions = clientServiceInterface.getSessionsForCustomer(customerId,
                    ImmutableSet.of(equipmentId), ImmutableSet.of(ce.getLocationId()), null,
                    new PaginationContext<ClientSession>());
            clientSessionsForCustomerAndEquipment.addAll(clientSessions.getItems());
        }
        for (OpensyncAPVIFState vifState : vifStateTables) {

            if (bssidList != null) {
                for (ActiveBSSID activeBSSID : bssidList) {
                    if (activeBSSID.getBssid().equals(vifState.getMac())
                            && activeBSSID.getSsid().equals(vifState.getSsid())) {
                        toBeDeleted.add(activeBSSID);

                    }
                }
            }

        }

        if (bssidList != null)
            bssidList.removeAll(toBeDeleted);

        statusDetails.setActiveBSSIDs(bssidList);

        activeBssidsStatus.setDetails(statusDetails);

        activeBssidsStatus = statusServiceInterface.update(activeBssidsStatus);

        LOG.debug("wifiVIFStateDbTableDelete Updated activeBSSIDs {}", activeBssidsStatus);

    }

    @Override
    public void wifiAssociatedClientsDbTableDelete(String deletedClientMac, String apId) {

        LOG.info("Received wifiAssociatedClientsDbTableDelete monitor notification for MAC {} on AP {}",
                deletedClientMac, apId);
        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession == null) {
            LOG.debug("wifiAssociatedClientsDbTableDelete::Cannot get Session for AP {}", apId);
            return;
        }

        int customerId = ovsdbSession.getCustomerId();
        long equipmentId = ovsdbSession.getEquipmentId();

        if ((customerId < 0) || (equipmentId < 0)) {
            LOG.debug("wifiAssociatedClientsDbTableDelete::Cannot get valid CustomerId {} or EquipmentId {} for AP {}",
                    customerId, equipmentId, apId);
            return;
        }

        Client client = clientServiceInterface.getOrNull(customerId, MacAddress.valueOf(deletedClientMac));
        ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId,
                MacAddress.valueOf(deletedClientMac));

        if (client != null) {
            if (clientSession != null && clientSession.getDetails() != null
                    && clientSession.getDetails().getAssociationState() != null) {
                if (!clientSession.getDetails().getAssociationState().equals(AssociationState.Disconnected)) {
                    clientSession.getDetails().setAssociationState(AssociationState.Disconnected);
                    clientSession = clientServiceInterface.updateSession(clientSession);
                    LOG.info("Session {} for client {} is now disconnected.", clientSession, client.getMacAddress());
                }
            }

        } else {
            if (clientSession != null) {

                clientSession = clientServiceInterface.deleteSession(customerId, equipmentId,
                        MacAddress.valueOf(deletedClientMac));

                LOG.info("No client {} found, delete session {}", MacAddress.valueOf(deletedClientMac), clientSession);
            }
        }

    }

    @Override
    public void dhcpLeasedIpDbTableUpdate(List<Map<String, String>> dhcpAttributes, String apId,
            RowUpdateOperation rowUpdateOperation) {

        LOG.info("dhcpLeasedIpDbTableUpdate {} operations on AP {} ", rowUpdateOperation, apId);

        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession == null) {
            LOG.debug("updateDhcpIpClientFingerprints::Cannot get Session for AP {}", apId);
            return;
        }

        int customerId = ovsdbSession.getCustomerId();
        long equipmentId = ovsdbSession.getEquipmentId();

        if ((customerId < 0) || (equipmentId < 0)) {
            LOG.debug("updateDhcpIpClientFingerprints::Cannot get valid CustomerId {} or EquipmentId {} for AP {}",
                    customerId, equipmentId, apId);
            return;
        }

        Equipment ce = equipmentServiceInterface.getByInventoryIdOrNull(apId);

        if (ce == null) {
            LOG.debug("updateDhcpIpClientFingerprints::Cannot get Equipment for AP {}", apId);
            return;
        }

        long locationId = ce.getLocationId();

        if (rowUpdateOperation.equals(RowUpdateOperation.INSERT)) {

            List<ClientSession> clientSessionList = new ArrayList<>();
            for (Map<String, String> dhcpLeasedIps : dhcpAttributes) {
                if (!dhcpLeasedIps.containsKey("hwaddr")) {

                    LOG.info("Cannot insert a client {} that has no hwaddr.", dhcpLeasedIps);
                    continue;

                }
                MacAddress clientMacAddress = MacAddress.valueOf(dhcpLeasedIps.get("hwaddr"));

                Client client = clientServiceInterface.getOrNull(customerId, clientMacAddress);

                if (client == null) {
                    LOG.info("Cannot find client instance for {}", clientMacAddress);
                    continue;
                } else if (clientMacAddress.equals(equipmentServiceInterface.get(equipmentId).getBaseMacAddress())) {
                    LOG.info("Not a client device {} ", dhcpLeasedIps);

                    // In case somehow this equipment has accidentally been
                    // tagged as a client, remove

                    ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId,
                            clientMacAddress);

                    if (clientSession != null) {
                        LOG.info("Deleting invalid client session {}",
                                clientServiceInterface.deleteSession(customerId, equipmentId, clientMacAddress));
                    }

                    LOG.info("Deleting invalid client {}", clientServiceInterface.delete(customerId, clientMacAddress));

                    continue;
                } else {
                    LOG.info("Client {} already exists on the cloud, update client values", dhcpLeasedIps);

                    ClientInfoDetails clientDetails = (ClientInfoDetails) client.getDetails();
                    if (dhcpLeasedIps.containsKey("hostname")) {

                        clientDetails.setHostName(dhcpLeasedIps.get("hostname"));

                    }

                    if (dhcpLeasedIps.containsKey("fingerprint")) {

                        clientDetails.setApFingerprint(dhcpLeasedIps.get("fingerprint"));
                    }

                    if (dhcpLeasedIps.containsKey("device_type")) {

                        DhcpFpDeviceType dhcpFpDeviceType = DhcpFpDeviceType
                                .getByName(dhcpLeasedIps.get("device_type"));
                        ClientType clientType = OvsdbToWlanCloudTypeMappingUtility
                                .getClientTypeForDhcpFpDeviceType(dhcpFpDeviceType);

                        LOG.debug("Translate from ovsdb {} to cloud {}", dhcpFpDeviceType, clientType);

                        clientDetails.setClientType(clientType.getId());

                    }

                    client.setDetails(clientDetails);

                    client = clientServiceInterface.update(client);

                    LOG.info("Updated Client {}.", client);

                    // In this case, we might have a session, as the client
                    // already exists on the cloud, update if required

                    ClientSession session = updateClientSession(customerId, equipmentId, locationId, dhcpLeasedIps,
                            clientMacAddress);
                    if (session != null) {
                        clientSessionList.add(session);

                    }

                }
            }

            if (!clientSessionList.isEmpty()) {
                LOG.info("Updating client sessions {}", clientSessionList);
                clientSessionList = clientServiceInterface.updateSessions(clientSessionList);
                LOG.info("Updated client sessions {}", clientSessionList);
            }

        } else if (rowUpdateOperation.equals(RowUpdateOperation.MODIFY)
                || rowUpdateOperation.equals(RowUpdateOperation.INIT)) {

            List<ClientSession> clientSessionList = new ArrayList<>();

            for (Map<String, String> dhcpLeasedIps : dhcpAttributes) {

                if (!dhcpLeasedIps.containsKey("hwaddr")) {

                    LOG.info("Cannot update a client {} that has no hwaddr.", dhcpLeasedIps);
                    continue;

                }

                MacAddress clientMacAddress = MacAddress.valueOf(dhcpLeasedIps.get("hwaddr"));

                Client client = clientServiceInterface.getOrNull(customerId, clientMacAddress);
                if (client == null) {
                    LOG.info("Cannot find client instance for {}", clientMacAddress);
                    continue;
                } else if (clientMacAddress.equals(equipmentServiceInterface.get(equipmentId).getBaseMacAddress())) {

                    LOG.info("Not a client device {} ", dhcpLeasedIps);

                    // In case somehow this equipment has accidentally been
                    // tagged as a client, remove

                    ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId,
                            clientMacAddress);

                    if (clientSession != null) {
                        LOG.info("Deleting invalid client session {}",
                                clientServiceInterface.deleteSession(customerId, equipmentId, clientMacAddress));
                    }

                    LOG.info("Deleting invalid client {}", clientServiceInterface.delete(customerId, clientMacAddress));

                    continue;

                } else {

                    ClientInfoDetails clientDetails = (ClientInfoDetails) client.getDetails();
                    if (dhcpLeasedIps.containsKey("hostname")) {

                        clientDetails.setHostName(dhcpLeasedIps.get("hostname"));

                    }

                    if (dhcpLeasedIps.containsKey("fingerprint")) {

                        clientDetails.setApFingerprint(dhcpLeasedIps.get("fingerprint"));
                    }

                    if (dhcpLeasedIps.containsKey("device_type")) {

                        DhcpFpDeviceType dhcpFpDeviceType = DhcpFpDeviceType
                                .getByName(dhcpLeasedIps.get("device_type"));
                        ClientType clientType = OvsdbToWlanCloudTypeMappingUtility
                                .getClientTypeForDhcpFpDeviceType(dhcpFpDeviceType);

                        LOG.debug("Translate from ovsdb {} to cloud {}", dhcpFpDeviceType, clientType);

                        clientDetails.setClientType(clientType.getId());

                    }

                    client.setDetails(clientDetails);

                    client = clientServiceInterface.update(client);

                    LOG.info("Updated Client {}.", client);

                    // check if there is a session for this client

                    ClientSession session = updateClientSession(customerId, equipmentId, locationId, dhcpLeasedIps,
                            clientMacAddress);
                    if (session != null) {
                        clientSessionList.add(session);

                    }
                }

            }

            if (!clientSessionList.isEmpty()) {
                LOG.info("Updating client sessions {}", clientSessionList);
                clientSessionList = clientServiceInterface.updateSessions(clientSessionList);
                LOG.info("Updated client sessions {}", clientSessionList);
            }

        } else if (rowUpdateOperation.equals(RowUpdateOperation.DELETE)) {
            // Client should not be 'deleted' from Cloud
            LOG.debug("Recieved deletions, not removing client(s) from cloud", dhcpAttributes);
        }

    }

    protected ClientSession updateClientSession(int customerId, long equipmentId, long locationId,
            Map<String, String> dhcpLeasedIps, MacAddress clientMacAddress) {

        ClientSession session = clientServiceInterface.getSessionOrNull(customerId, equipmentId, clientMacAddress);

        if (session == null) {

            LOG.info("Cannot get session for client {} for customerId {} equipmentId {} locationId {}",
                    clientMacAddress, customerId, equipmentId, locationId);
            return null;
        }

        ClientSessionDetails clientSessionDetails = new ClientSessionDetails();

        if (dhcpLeasedIps.containsKey("fingerprint")) {

            clientSessionDetails.setApFingerprint(dhcpLeasedIps.get("fingerprint"));
        }

        if (dhcpLeasedIps.containsKey("inet_addr")) {

            try {
                clientSessionDetails.setIpAddress(InetAddress.getByName(dhcpLeasedIps.get("inet_addr")));
            } catch (UnknownHostException e) {
                LOG.error("Invalid Client IP", e);
            }

        }

        if (dhcpLeasedIps.containsKey("hostname")) {

            clientSessionDetails.setHostname(dhcpLeasedIps.get("hostname"));

        }

        ClientDhcpDetails clientDhcpDetails = new ClientDhcpDetails(clientSessionDetails.getSessionId());

        if (dhcpLeasedIps.containsKey("dhcp_server")) {
            try {
                clientDhcpDetails.setDhcpServerIp(InetAddress.getByName(dhcpLeasedIps.get("dhcp_server")));
            } catch (UnknownHostException e) {
                LOG.error("Invalid DhcpServer", e);
            }
        }

        if (dhcpLeasedIps.containsKey("lease_time")) {
            Integer leaseTime = Integer.valueOf(dhcpLeasedIps.get("lease_time"));
            clientDhcpDetails.setLeaseTimeInSeconds(leaseTime);
        }

        if (dhcpLeasedIps.containsKey("gateway")) {
            try {
                clientDhcpDetails.setGatewayIp(InetAddress.getByName(dhcpLeasedIps.get("gateway")));
            } catch (UnknownHostException e) {
                LOG.error("Invalid Gateway IP", e);

                try {
                    clientDhcpDetails.setGatewayIp(InetAddress.getByAddress(dhcpLeasedIps.get("gateway").getBytes()));
                } catch (UnknownHostException e1) {
                    // TODO Auto-generated catch block
                    LOG.error("Invalid Gateway Address", e);
                }

            }
        }

        if (dhcpLeasedIps.containsKey("subnet_mask")) {
            try {
                clientDhcpDetails.setSubnetMask(InetAddress.getByName(dhcpLeasedIps.get("subnet_mask")));
            } catch (UnknownHostException e) {
                LOG.error("Invalid Subnet Mask", e);
                try {
                    clientDhcpDetails
                            .setGatewayIp(InetAddress.getByAddress(dhcpLeasedIps.get("subnet_mask").getBytes()));
                } catch (UnknownHostException e1) {
                    // TODO Auto-generated catch block
                    LOG.error("Invalid Subnet Mask Address", e);
                }
            }
        }

        if (dhcpLeasedIps.containsKey("primary_dns")) {
            try {
                clientDhcpDetails.setPrimaryDns(InetAddress.getByName(dhcpLeasedIps.get("primary_dns")));
            } catch (UnknownHostException e) {
                LOG.error("Invalid Primary DNS", e);
                try {
                    clientDhcpDetails
                            .setGatewayIp(InetAddress.getByAddress(dhcpLeasedIps.get("primary_dns").getBytes()));
                } catch (UnknownHostException e1) {
                    // TODO Auto-generated catch block
                    LOG.error("Invalid Primary DNS Address", e);
                }

            }
        }

        if (dhcpLeasedIps.containsKey("secondary_dns")) {
            try {
                clientDhcpDetails.setSecondaryDns(InetAddress.getByName(dhcpLeasedIps.get("secondary_dns")));
            } catch (UnknownHostException e) {
                LOG.error("Invalid Secondary DNS", e);
                try {
                    clientDhcpDetails
                            .setGatewayIp(InetAddress.getByAddress(dhcpLeasedIps.get("secondary_dns").getBytes()));
                } catch (UnknownHostException e1) {
                    // TODO Auto-generated catch block
                    LOG.error("Invalid Seconary DNS Address", e);
                }
            }
        }

        if (dhcpLeasedIps.containsKey("device_name")) {
            clientSessionDetails.setClassificationName(dhcpLeasedIps.get("device_name"));
        }

        clientSessionDetails.setDhcpDetails(clientDhcpDetails);

        session.getDetails().mergeSession(clientSessionDetails);

        return session;
    }

    @Override
    public void commandStateDbTableUpdate(List<Map<String, String>> commandStateAttributes, String apId,
            RowUpdateOperation rowUpdateOperation) {
        LOG.info("Received Command_State row(s) {} rowUpdateOperation {} for ap {}", commandStateAttributes,
                rowUpdateOperation, apId);

        // TODO: will handle changes from Command_State table
    }

    /**
     * Clear the EquipmentStatus for this AP, and set all client sessions to
     * disconnected. Done as part of a reconfiguration/configuration change
     */
    @Override
    public void clearEquipmentStatus(String apId) {

        LOG.info("OpensyncExternalIntegrationCloud::clearEquipmentStatus for AP {}", apId);
        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession == null) {
            LOG.debug("clearEquipmentStatus::Cannot get Session for AP {}", apId);
            return;
        }

        int customerId = ovsdbSession.getCustomerId();
        long equipmentId = ovsdbSession.getEquipmentId();

        if ((customerId < 0) || (equipmentId < 0)) {
            LOG.debug("clearEquipmentStatus::Cannot get valid CustomerId {} or EquipmentId {} for AP {}", customerId,
                    equipmentId, apId);
            return;
        }

        Equipment ce = equipmentServiceInterface.getOrNull(equipmentId);

        if (ce == null) {
            LOG.debug("clearEquipmentStatus Cannot get customer Equipment for {}", apId);
            return;
        }

        List<Status> statusList = new ArrayList<>();

        Status status = new Status();
        status.setCustomerId(customerId);
        status.setEquipmentId(equipmentId);
        status.setStatusDataType(StatusDataType.ACTIVE_BSSIDS);
        status.setDetails(new ActiveBSSIDs());

        statusList.add(status);

        status = new Status();
        status.setCustomerId(customerId);
        status.setEquipmentId(equipmentId);
        status.setStatusDataType(StatusDataType.CLIENT_DETAILS);
        status.setDetails(new ClientConnectionDetails());

        statusList.add(status);

        status = new Status();
        status.setCustomerId(customerId);
        status.setEquipmentId(equipmentId);
        status.setStatusDataType(StatusDataType.RADIO_UTILIZATION);
        status.setDetails(new RadioUtilizationReport());

        statusList.add(status);

        status = new Status();
        status.setCustomerId(customerId);
        status.setEquipmentId(equipmentId);
        status.setStatusDataType(StatusDataType.NEIGHBOUR_SCAN);
        status.setDetails(new EquipmentScanDetails());

        statusList.add(status);

        status = new Status();
        status.setCustomerId(customerId);
        status.setEquipmentId(equipmentId);
        status.setStatusDataType(StatusDataType.OS_PERFORMANCE);
        status.setDetails(new OperatingSystemPerformance());

        statusList.add(status);

        LOG.info("statusList to clear {}", statusList);

        List<Status> clearedStatus = statusServiceInterface.update(statusList);

        clearedStatus.stream().forEach(s -> {
            LOG.info("Cleared Status Data {}", s.toPrettyString());
        });

        List<Status> customerDashboardStatus = statusServiceInterface.getForEquipment(customerId, Set.of(equipmentId),
                Set.of(StatusDataType.CUSTOMER_DASHBOARD));
        for (Status customerDashStatus : customerDashboardStatus) {
            LOG.info("Updated customer status {}", statusServiceInterface.update(customerDashStatus));
        }

        // Set any existing client sessions to disconnected
        LOG.info(
                "OpensyncExternalIntegrationCloud::clearEquipmentStatus disconnect any existing client sessions on AP {}",
                apId);
        disconnectClients(ce);

    }
}
