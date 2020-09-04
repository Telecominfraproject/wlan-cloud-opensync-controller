package com.telecominfraproject.wlan.opensync.external.integration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.telecominfraproject.wlan.client.ClientServiceInterface;
import com.telecominfraproject.wlan.client.info.models.ClientInfoDetails;
import com.telecominfraproject.wlan.client.session.models.AssociationState;
import com.telecominfraproject.wlan.client.session.models.ClientDhcpDetails;
import com.telecominfraproject.wlan.client.session.models.ClientSession;
import com.telecominfraproject.wlan.client.session.models.ClientSessionDetails;
import com.telecominfraproject.wlan.core.model.entity.CountryCode;
import com.telecominfraproject.wlan.core.model.equipment.AutoOrManualValue;
import com.telecominfraproject.wlan.core.model.equipment.EquipmentType;
import com.telecominfraproject.wlan.core.model.equipment.MacAddress;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
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
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPInetState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPRadioState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPVIFState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAWLANNode;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncWifiAssociatedClients;
import com.telecominfraproject.wlan.profile.ProfileServiceInterface;
import com.telecominfraproject.wlan.profile.models.Profile;
import com.telecominfraproject.wlan.profile.models.ProfileContainer;
import com.telecominfraproject.wlan.profile.models.ProfileType;
import com.telecominfraproject.wlan.profile.network.models.ApNetworkConfiguration;
import com.telecominfraproject.wlan.profile.network.models.RadioProfileConfiguration;
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
    private String fwImageNameKey = "FW_IMAGE_NAME";
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

    @Autowired
    private CacheManager cacheManagerShortLived;
    private Cache cloudEquipmentRecordCache;

    @PostConstruct
    private void postCreate() {
        LOG.info("Using Cloud integration");
        cloudEquipmentRecordCache = cacheManagerShortLived.getCache("equipment_record_cache");
    }

    public Equipment getCustomerEquipment(String apId) {
        Equipment ce = null;

        try {
            ce = cloudEquipmentRecordCache.get(apId, () -> equipmentServiceInterface.getByInventoryIdOrNull(apId));
        } catch (Exception e) {
            LOG.error("Could not get customer equipment for {}", apId, e);
        }

        return ce;
    }

    private RadioType getRadioTypeForOvsdbRadioFreqBand(String ovsdbRadioFreqBand) {

        switch (ovsdbRadioFreqBand) {
            case "2.4G":
                return RadioType.is2dot4GHz;
            case "5G":
                return RadioType.is5GHz;
            case "5GL":
                return RadioType.is5GHzL;
            case "5GU":
                return RadioType.is5GHzU;
            default:
                return RadioType.UNSUPPORTED;
        }

    }

    @Override
    public void apConnected(String apId, ConnectNodeInfo connectNodeInfo) {

        Equipment ce = getCustomerEquipment(apId);

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
                    ce.setBaseMacAddress(new MacAddress(connectNodeInfo.macAddress));
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
                ce.setName(ce.getEquipmentType().name() + "_" + ce.getSerial());

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
                        advancedRadioConfiguration.setAutoChannelSelection(StateSetting.disabled);

                        advancedRadioMap.put(radioType, advancedRadioConfiguration);
                        radioConfiguration = ElementRadioConfiguration.createWithDefaults(radioType);
                        radioConfiguration.setAutoChannelSelection(false);
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

                ce = equipmentServiceInterface.create(ce);

                // update the cache right away, no need to wait until the
                // entry expires
                cloudEquipmentRecordCache.put(ce.getInventoryId(), ce);

            } else {
                // equipment already exists

                MacAddress reportedMacAddress = null;
                try {
                    reportedMacAddress = new MacAddress(connectNodeInfo.macAddress);
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

                        // update the cache right away, no need to wait
                        // until the entry expires
                        cloudEquipmentRecordCache.put(ce.getInventoryId(), ce);
                    }
                }

            }

            EquipmentRoutingRecord equipmentRoutingRecord = gatewayController.registerCustomerEquipment(ce.getName(),
                    ce.getCustomerId(), ce.getId());

            updateApStatus(ce, connectNodeInfo);

            OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);
            ovsdbSession.setRoutingId(equipmentRoutingRecord.getId());
            ovsdbSession.setEquipmentId(ce.getId());
            ovsdbSession.setCustomerId(ce.getCustomerId());

            LOG.debug("Equipment {}", ce);
            LOG.info("AP {} got connected to the gateway", apId);
            LOG.info("ConnectNodeInfo {}", connectNodeInfo);

            if (connectNodeInfo.versionMatrix.containsKey(fwImageNameKey)) {
                reconcileFwVersionToTrack(ce, connectNodeInfo.versionMatrix.get(fwImageNameKey), connectNodeInfo.model);
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

            RadioType radioType = getRadioTypeForOvsdbRadioFreqBand(radioBand);
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

            apProfile = profileServiceInterface.update(apProfile);
        }

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
            protocolStatusData.setReportedCC(CountryCode.ca);
            protocolStatusData.setReportedHwVersion(connectNodeInfo.platformVersion);
            if (connectNodeInfo.versionMatrix.containsKey(fwImageNameKey)) {
                protocolStatusData.setReportedSwVersion(connectNodeInfo.versionMatrix.get(fwImageNameKey));
                protocolStatusData.setReportedSwAltVersion(connectNodeInfo.versionMatrix.get(fwImageNameKey));
            }
            try {
                protocolStatusData.setReportedIpV4Addr(InetAddress.getByName(connectNodeInfo.ipV4Address));
            } catch (UnknownHostException e) {
                // do nothing here
            }
            if ((connectNodeInfo.macAddress != null) && (MacAddress.valueOf(connectNodeInfo.macAddress) != null)) {
                protocolStatusData.setReportedMacAddr(MacAddress.valueOf(connectNodeInfo.macAddress));
            }
            protocolStatusData.setReportedSku(connectNodeInfo.skuNumber);
            protocolStatusData.setSerialNumber(connectNodeInfo.serialNumber);
            protocolStatusData.setSystemName(connectNodeInfo.model);
            statusRecord.setDetails(protocolStatusData);
            statusServiceInterface.update(statusRecord);

            statusRecord = statusServiceInterface.getOrNull(ce.getCustomerId(), ce.getId(), StatusDataType.FIRMWARE);
            if (statusRecord == null) {
                statusRecord = new Status();
                statusRecord.setCustomerId(ce.getCustomerId());
                statusRecord.setEquipmentId(ce.getId());
                EquipmentUpgradeStatusData statusData = new EquipmentUpgradeStatusData();
                statusRecord.setDetails(statusData);
            }

            EquipmentUpgradeStatusData fwUpgradeStatusData = (EquipmentUpgradeStatusData) statusRecord.getDetails();
            if (connectNodeInfo.versionMatrix.containsKey(fwImageNameKey)) {
                fwUpgradeStatusData.setActiveSwVersion(connectNodeInfo.versionMatrix.get(fwImageNameKey));
                fwUpgradeStatusData.setAlternateSwVersion(connectNodeInfo.versionMatrix.get(fwImageNameKey));
            }
            fwUpgradeStatusData.setUpgradeState(EquipmentUpgradeState.undefined);
            statusRecord.setDetails(fwUpgradeStatusData);
            statusServiceInterface.update(statusRecord);

            Status networkAdminStatusRec = statusServiceInterface.getOrNull(ce.getCustomerId(), ce.getId(),
                    StatusDataType.NETWORK_ADMIN);
            if (networkAdminStatusRec == null) {
                networkAdminStatusRec = new Status();
                networkAdminStatusRec.setCustomerId(ce.getCustomerId());
                networkAdminStatusRec.setEquipmentId(ce.getId());
                NetworkAdminStatusData statusData = new NetworkAdminStatusData();
                networkAdminStatusRec.setDetails(statusData);
            }

            NetworkAdminStatusData netAdminStatusData = (NetworkAdminStatusData) networkAdminStatusRec.getDetails();
            netAdminStatusData.setDhcpStatus(StatusCode.normal);
            netAdminStatusData.setCloudLinkStatus(StatusCode.normal);
            netAdminStatusData.setDnsStatus(StatusCode.normal);

            networkAdminStatusRec.setDetails(netAdminStatusData);

            networkAdminStatusRec = statusServiceInterface.update(networkAdminStatusRec);

        } catch (Exception e) {
            LOG.debug("Exception in updateApStatus", e);
        }

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
                // if (ovsdbSession.getCustomerId() > 0 &&
                // ovsdbSession.getEquipmentId() > 0L) {
                // List<Status> statusForDisconnectedAp =
                // statusServiceInterface.delete(ovsdbSession.getCustomerId(),
                // ovsdbSession.getEquipmentId());
                // LOG.info("Deleted status records {} for AP {}",
                // statusForDisconnectedAp, apId);
                // }
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
        } catch (Exception e) {
            LOG.error("Exception when registering ap routing {}", apId, e);
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
            Customer customer = customerServiceInterface.getOrNull(customerId);
            if ((customer != null) && (customer.getDetails() != null)
                    && (customer.getDetails().getAutoProvisioning() != null)
                    && customer.getDetails().getAutoProvisioning().isEnabled()) {
                Equipment equipmentConfig = getCustomerEquipment(apId);

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

                ret.setSsidProfile(
                        profileContainer.getChildrenOfType(equipmentConfig.getProfileId(), ProfileType.ssid));

                Set<Profile> radiusSet = new HashSet<>();
                Set<Long> captiveProfileIds = new HashSet<>();
                Set<Long> bonjourGatewayProfileIds = new HashSet<>();

                for (Profile ssidProfile : ret.getSsidProfile()) {

                    radiusSet.addAll(profileContainer.getChildrenOfType(ssidProfile.getId(), ProfileType.radius));
                    if (ssidProfile.getDetails() != null) {
                        Long captivePortId = ((SsidConfiguration) ssidProfile.getDetails()).getCaptivePortalId();
                        if (captivePortId != null) {
                            captiveProfileIds.add(captivePortId);
                        }
                        Long bonjourGatewayProfileId = ((SsidConfiguration)ssidProfile.getDetails()).getBonjourGatewayProfileId();
                        if (bonjourGatewayProfileId != null) {
                            bonjourGatewayProfileIds.add(bonjourGatewayProfileId);
                        }
                    }
                }
                ret.setRadiusProfiles(new ArrayList<>(radiusSet));
                ret.setCaptiveProfiles(profileServiceInterface.get(captiveProfileIds));
                ret.setBonjourGatewayProfiles(profileServiceInterface.get(bonjourGatewayProfileIds));

                List<com.telecominfraproject.wlan.client.models.Client> blockedClients = clientServiceInterface
                        .getBlockedClients(customerId);
                List<MacAddress> blockList = Lists.newArrayList();
                if ((blockedClients != null) && !blockedClients.isEmpty()) {
                    blockedClients.forEach(client -> blockList.add(client.getMacAddress()));
                }
                ret.setBlockedClients(blockList);

                LOG.debug("ApConfig {}", ret);
            } else {
                LOG.info("Autoconfig is not enabled for this AP {}", apId);
            }

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
            Optional<ElementRadioConfiguration> radioConfiguration = ((ApElementConfiguration) apNode.getDetails())
                    .getRadioMap().values().stream().filter(t -> (t.getActiveChannel() == channel)).findFirst();

            if (radioConfiguration.isPresent()) {
                radioType = radioConfiguration.get().getRadioType();
            }

            updateActiveBssids(customerId, equipmentId, apId, ssid, radioType, bssid, numClients);

        }

        Status activeBssidsStatus = statusServiceInterface.getOrNull(customerId, equipmentId,
                StatusDataType.ACTIVE_BSSIDS);
        if (activeBssidsStatus != null) {
            updateClientDetailsStatus(customerId, equipmentId, (ActiveBSSIDs) activeBssidsStatus.getDetails());
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
                apElementConfiguration.getRadioMap().get(radioState.getFreqBand())
                        .setEirpTxPower(AutoOrManualValue.createManualInstance(radioState.getTxPower()));

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
                if (!protocolStatusData.getReportedCC()
                        .equals(CountryCode.valueOf(radioState.getCountry().toLowerCase()))) {
                    protocolStatusData.setReportedCC(CountryCode.valueOf(radioState.getCountry().toLowerCase()));
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
                    .filter(p -> (!p.getRadioType().equals(freqBand) || !p.getSsid().equals(ssid)))
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

        if (LOG.isTraceEnabled()) {
            LOG.trace("Processing Wifi_VIF_State table update for AP {}, updated ACTIVE_BSSID Status {}", apId,
                    activeBssidsStatus.toPrettyString());
        }
    }

    @Override
    public void wifiInetStateDbTableUpdate(List<OpensyncAPInetState> inetStateTables, String apId) {

        LOG.debug("Received Wifi_Inet_State table update for AP {}", apId);


    }

    @Override
    public void wifiInetStateDbTableDelete(List<OpensyncAPInetState> inetStateTables, String apId) {

        LOG.debug("Received Wifi_Inet_State table delete for AP {}", apId);


    }

    @Override
    public void wifiAssociatedClientsDbTableUpdate(List<OpensyncWifiAssociatedClients> wifiAssociatedClients,
            String apId) {

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
            com.telecominfraproject.wlan.client.models.Client clientInstance = clientServiceInterface
                    .getOrNull(customerId, new MacAddress(opensyncWifiAssociatedClients.getMac()));

            boolean isReassociation = true;
            if (clientInstance == null) {
                clientInstance = new com.telecominfraproject.wlan.client.models.Client();

                clientInstance.setCustomerId(customerId);
                clientInstance.setMacAddress(new MacAddress(opensyncWifiAssociatedClients.getMac()));
                clientInstance.setDetails(new ClientInfoDetails());
                clientInstance = clientServiceInterface.create(clientInstance);

                isReassociation = false;

            }
            ClientInfoDetails clientDetails = (ClientInfoDetails) clientInstance.getDetails();

            clientInstance.setDetails(clientDetails);

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
                clientSessionDetails.setSessionId(clientInstance.getMacAddress().getAddressAsLong());
                clientSession.setDetails(clientSessionDetails);

                clientSession = clientServiceInterface.updateSession(clientSession);
            }

            ClientSessionDetails clientSessionDetails = clientSession.getDetails();
            clientSessionDetails.setAssociationState(AssociationState._802_11_Associated);
            clientSessionDetails.setAssocTimestamp(System.currentTimeMillis());
            clientSessionDetails.setSessionId(clientInstance.getMacAddress().getAddressAsLong());
            clientSession.getDetails().mergeSession(clientSessionDetails);

            clientSession = clientServiceInterface.updateSession(clientSession);

        }

    }

    @Override
    public void awlanNodeDbTableUpdate(OpensyncAWLANNode opensyncAPState, String apId) {

        LOG.debug("AP {} table AWLAN_Node updated {}", apId, opensyncAPState);
        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession == null) {
            LOG.debug("awlanNodeDbTableUpdate::Cannot get Session for AP {}", apId);
            return;
        }

        int customerId = ovsdbSession.getCustomerId();
        long equipmentId = ovsdbSession.getEquipmentId();

        if ((customerId < 0) || (equipmentId < 0)) {
            LOG.debug("awlanNodeDbTableUpdate::Cannot get valid CustomerId {} or EquipmentId {} for AP {}", customerId,
                    equipmentId, apId);
            return;
        }

        EquipmentUpgradeState fwUpgradeState = EquipmentUpgradeState.undefined;
        FailureReason fwUpgradeFailureReason = null;
        switch (opensyncAPState.getUpgradeStatus()) {
            case 0:
                break; // nothing
            case -1:
                LOG.error("upgrade_status: Wrong arguments (app error)");
                fwUpgradeState = EquipmentUpgradeState.download_failed;
                fwUpgradeFailureReason = FailureReason.downloadRequestRejected;
                break;
            case -3:
                LOG.error("upgrade_status: Incorrect URL)");
                fwUpgradeState = EquipmentUpgradeState.download_failed;
                fwUpgradeFailureReason = FailureReason.unreachableUrl;
                break;
            case -4:
                LOG.error("upgrade_status: Failed firmware image download");
                fwUpgradeState = EquipmentUpgradeState.download_failed;
                fwUpgradeFailureReason = FailureReason.downloadFailed;
                break;
            case -5:
                LOG.error("upgrade_status: Error while downloading firmware md5 sum file");
                fwUpgradeState = EquipmentUpgradeState.download_failed;
                fwUpgradeFailureReason = FailureReason.downloadFailed;
                break;
            case -6:
                LOG.error("upgrade_status: md5 checksum file error");
                fwUpgradeState = EquipmentUpgradeState.download_failed;
                fwUpgradeFailureReason = FailureReason.validationFailed;
                break;
            case -7:
                LOG.error("upgrade_status: Firmware image error");
                fwUpgradeState = EquipmentUpgradeState.apply_failed;
                fwUpgradeFailureReason = FailureReason.validationFailed;
                break;
            case -8:
                LOG.error("upgrade_status: Flash erase failed");
                fwUpgradeState = EquipmentUpgradeState.apply_failed;
                fwUpgradeFailureReason = FailureReason.applyFailed;
                break;
            case -9:
                LOG.error("upgrade_status: Flash write failed");
                fwUpgradeState = EquipmentUpgradeState.apply_failed;
                fwUpgradeFailureReason = FailureReason.applyFailed;
                break;
            case -10:
                LOG.error("upgrade_status: Flash verification failed");
                fwUpgradeState = EquipmentUpgradeState.apply_failed;
                fwUpgradeFailureReason = FailureReason.validationFailed;
                break;
            case -11:
                LOG.error("upgrade_status: Set new bootconfig failed");
                fwUpgradeState = EquipmentUpgradeState.apply_failed;
                fwUpgradeFailureReason = FailureReason.applyFailed;
                break;
            case -12:
                LOG.error("upgrade_status: Device restart failed");
                fwUpgradeState = EquipmentUpgradeState.reboot_failed;
                fwUpgradeFailureReason = FailureReason.rebootTimedout;
                break;
            case -14:
                LOG.error("upgrade_status: Flash BootConfig erase failed");
                fwUpgradeState = EquipmentUpgradeState.apply_failed;
                fwUpgradeFailureReason = FailureReason.applyFailed;
                break;
            case -15:
                LOG.error("upgrade_status: Safe update is running");
                fwUpgradeState = EquipmentUpgradeState.apply_failed;
                fwUpgradeFailureReason = FailureReason.applyFailed;
                break;
            case -16:
                LOG.error("upgrade_status: Not enough free space on device");
                fwUpgradeState = EquipmentUpgradeState.download_failed;
                fwUpgradeFailureReason = FailureReason.downloadRequestFailedFlashFull;

                break;
            case 10:
                LOG.info("upgrade_status: Firmware download started for AP {}", apId);
                fwUpgradeState = EquipmentUpgradeState.download_initiated;
                break;
            case 11:
                LOG.info("upgrade_status: Firmware download successful, triggering upgrade.");
                fwUpgradeState = EquipmentUpgradeState.download_complete;
                break;
            case 20:
                LOG.info("upgrade_status: FW write on alt partition started");
                fwUpgradeState = EquipmentUpgradeState.apply_initiated;
                break;
            case 21:
                LOG.info("upgrade_status: FW image write successfully completed");
                fwUpgradeState = EquipmentUpgradeState.apply_complete;
                break;
            case 30:
                LOG.info("upgrade_status: Bootconfig partition update started");
                fwUpgradeState = EquipmentUpgradeState.apply_initiated;
                break;
            case 31:
                LOG.info("upgrade_status: Bootconfig partition update completed");
                fwUpgradeState = EquipmentUpgradeState.apply_complete;
                break;
            default:
                LOG.debug("upgrade_status: {}", opensyncAPState.getUpgradeStatus());

        }

        Status protocolStatus = statusServiceInterface.getOrNull(customerId, equipmentId, StatusDataType.PROTOCOL);
        if (protocolStatus == null) {
            protocolStatus = new Status();
            protocolStatus.setCustomerId(customerId);
            protocolStatus.setEquipmentId(equipmentId);
            protocolStatus.setStatusDataType(StatusDataType.PROTOCOL);
            EquipmentProtocolStatusData protocolStatusData = new EquipmentProtocolStatusData();
            protocolStatus.setDetails(protocolStatusData);

            protocolStatus = statusServiceInterface.update(protocolStatus);

        }

        String reportedFwImageName = null;

        if (opensyncAPState.getVersionMatrix().containsKey(fwImageNameKey)) {
            reportedFwImageName = opensyncAPState.getVersionMatrix().get(fwImageNameKey);
        }

        EquipmentProtocolStatusData protocolStatusData = (EquipmentProtocolStatusData) protocolStatus.getDetails();
        protocolStatusData.setReportedSku(opensyncAPState.getSkuNumber());
        if (reportedFwImageName != null) {
            protocolStatusData.setReportedSwVersion(reportedFwImageName);
        }
        protocolStatusData.setReportedSwAltVersion(reportedFwImageName);
        protocolStatusData.setReportedHwVersion(opensyncAPState.getPlatformVersion());
        protocolStatusData.setSystemName(opensyncAPState.getModel());

        List<Status> updates = new ArrayList<>();

        // only post update if there is a change
        if (!((EquipmentProtocolStatusData) statusServiceInterface
                .getOrNull(customerId, equipmentId, StatusDataType.PROTOCOL).getDetails()).equals(protocolStatusData)) {
            protocolStatus.setDetails(protocolStatusData);
            updates.add(protocolStatus);
        }

        Status firmwareStatus = statusServiceInterface.getOrNull(customerId, equipmentId, StatusDataType.FIRMWARE);
        if (firmwareStatus == null) {
            firmwareStatus = new Status();
            firmwareStatus.setCustomerId(customerId);
            firmwareStatus.setEquipmentId(equipmentId);
            firmwareStatus.setStatusDataType(StatusDataType.FIRMWARE);
            firmwareStatus.setDetails(new EquipmentUpgradeStatusData());
            firmwareStatus = statusServiceInterface.update(firmwareStatus);

        }

        Equipment ce = getCustomerEquipment(apId);
        if (ce != null) {
            ce.getDetails();

            if (fwUpgradeState.equals(EquipmentUpgradeState.up_to_date)) {
                LOG.info("Firmware load is up to date.");

                EquipmentUpgradeStatusData firmwareStatusData = (EquipmentUpgradeStatusData) firmwareStatus
                        .getDetails();
                if (reportedFwImageName != null) {
                    if (!firmwareStatusData.getActiveSwVersion().equals(reportedFwImageName)
                            || !firmwareStatusData.getUpgradeState().equals(fwUpgradeState)) {
                        firmwareStatusData.setActiveSwVersion(reportedFwImageName);
                        firmwareStatusData.setUpgradeState(fwUpgradeState, fwUpgradeFailureReason);
                        firmwareStatus.setDetails(firmwareStatusData);
                        updates.add(firmwareStatus);
                    }

                }

                if (!updates.isEmpty()) { // may be some updates from protocol
                                          // status
                    updates = statusServiceInterface.update(updates);
                }

            } else if (fwUpgradeState.equals(EquipmentUpgradeState.download_complete)
                    || fwUpgradeState.equals(EquipmentUpgradeState.apply_complete)
                    || fwUpgradeState.equals(EquipmentUpgradeState.apply_initiated)
                    || fwUpgradeState.equals(EquipmentUpgradeState.applying)
                    || fwUpgradeState.equals(EquipmentUpgradeState.downloading)
                    || fwUpgradeState.equals(EquipmentUpgradeState.download_initiated)
                    || fwUpgradeState.equals(EquipmentUpgradeState.reboot_initiated)
                    || fwUpgradeState.equals(EquipmentUpgradeState.rebooting)) {

                LOG.info("Firmware upgrade is in state {}", fwUpgradeState);

                EquipmentUpgradeStatusData firmwareStatusData = (EquipmentUpgradeStatusData) firmwareStatus
                        .getDetails();
                if (reportedFwImageName != null) {
                    if (!firmwareStatusData.getActiveSwVersion().equals(reportedFwImageName)
                            || !firmwareStatusData.getUpgradeState().equals(fwUpgradeState)) {
                        firmwareStatusData.setActiveSwVersion(reportedFwImageName);
                        firmwareStatusData.setUpgradeState(fwUpgradeState, fwUpgradeFailureReason);
                        if (fwUpgradeState.equals(EquipmentUpgradeState.apply_initiated)) {
                            firmwareStatusData.setUpgradeStartTime(System.currentTimeMillis());
                        } else if (fwUpgradeState.equals(EquipmentUpgradeState.reboot_initiated)
                                || fwUpgradeState.equals(EquipmentUpgradeState.rebooting)) {
                            firmwareStatusData.setRebooted(true);
                        }
                        firmwareStatus.setDetails(firmwareStatusData);
                        updates.add(firmwareStatus);
                    }
                }

                if (!updates.isEmpty()) {// may be some updates from protocol
                                         // status
                    updates = statusServiceInterface.update(updates);
                }

                // no other action here, these are just transient states

            } else if (fwUpgradeState.equals(EquipmentUpgradeState.apply_failed)
                    || fwUpgradeState.equals(EquipmentUpgradeState.download_failed)
                    || fwUpgradeState.equals(EquipmentUpgradeState.reboot_failed)) {
                LOG.warn("Firmware upgrade is in a failed state {} due to {}", fwUpgradeState, fwUpgradeFailureReason);

                EquipmentUpgradeStatusData firmwareStatusData = (EquipmentUpgradeStatusData) firmwareStatus
                        .getDetails();
                if (reportedFwImageName != null) {
                    if (!firmwareStatusData.getActiveSwVersion().equals(reportedFwImageName)
                            || !firmwareStatusData.getUpgradeState().equals(fwUpgradeState)
                            || !firmwareStatusData.getReason().equals(fwUpgradeFailureReason)) {
                        firmwareStatusData.setActiveSwVersion(reportedFwImageName);
                        firmwareStatusData.setUpgradeState(fwUpgradeState, fwUpgradeFailureReason);
                        firmwareStatus.setDetails(firmwareStatusData);
                        updates.add(firmwareStatus);
                        updates = statusServiceInterface.update(updates);

                        reconcileFwVersionToTrack(ce, reportedFwImageName, opensyncAPState.getModel());
                    } else {
                        if (!updates.isEmpty()) {
                            updates = statusServiceInterface.update(updates);
                        }
                    }

                } else {
                    if (!updates.isEmpty()) {// may be some updates from
                                             // protocol
                        // status
                        updates = statusServiceInterface.update(updates);
                    }
                }

            } else {
                LOG.info("Firmware upgrade state is {}", fwUpgradeState);
                EquipmentUpgradeStatusData firmwareStatusData = (EquipmentUpgradeStatusData) firmwareStatus
                        .getDetails();
                if (reportedFwImageName != null) {
                    if (!firmwareStatusData.getActiveSwVersion().equals(reportedFwImageName)
                            || !firmwareStatusData.getUpgradeState().equals(fwUpgradeState)) {
                        firmwareStatusData.setActiveSwVersion(reportedFwImageName);
                        firmwareStatusData.setUpgradeState(fwUpgradeState, fwUpgradeFailureReason);
                        firmwareStatus.setDetails(firmwareStatusData);
                        updates.add(firmwareStatus);
                        updates = statusServiceInterface.update(updates);
                    } else {
                        if (!updates.isEmpty()) {// may be some updates from
                                                 // protocol
                            // status
                            updates = statusServiceInterface.update(updates);
                        }
                    }
                } else {
                    if (!updates.isEmpty()) {// may be some updates from
                                             // protocol
                        // status
                        updates = statusServiceInterface.update(updates);
                    }

                }
            }
        }

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

        if (equipmentId < 0L) {
            LOG.debug("wifiVIFStateDbTableDelete Cannot get equipmentId {} for session {}", equipmentId);
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


        bssidList.removeAll(toBeDeleted);

        statusDetails.setActiveBSSIDs(bssidList);

        activeBssidsStatus.setDetails(statusDetails);

        activeBssidsStatus = statusServiceInterface.update(activeBssidsStatus);

        LOG.debug("wifiVIFStateDbTableDelete Updated activeBSSIDs {}", activeBssidsStatus);

    }

    @Override
    public void wifiAssociatedClientsDbTableDelete(String deletedClientMac, String apId) {

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

        Set<MacAddress> macAddressSet = new HashSet<>();
        macAddressSet.add(new MacAddress(deletedClientMac));
        List<ClientSession> clientSessionList = clientServiceInterface.getSessions(customerId, macAddressSet);

        for (ClientSession session : clientSessionList) {

            ClientSessionDetails clientSessionDetails = session.getDetails();


            if ((clientSessionDetails.getAssociationState() != null)
                    && !clientSessionDetails.getAssociationState().equals(AssociationState.Disconnected)) {
                clientSessionDetails.setDisconnectByClientTimestamp(System.currentTimeMillis());
                clientSessionDetails.setAssociationState(AssociationState.Disconnected);

                session.setDetails(clientSessionDetails);
                session = clientServiceInterface.updateSession(session);

                if (LOG.isTraceEnabled()) {
                    LOG.trace("wifiAssociatedClientsDbTableDelete Updated client session, set to disconnected {}",
                            session.toPrettyString());
                }
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


        Equipment ce = getCustomerEquipment(apId);

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

                MacAddress clientMacAddress = new MacAddress(dhcpLeasedIps.get("hwaddr"));
                com.telecominfraproject.wlan.client.models.Client client = clientServiceInterface.getOrNull(customerId,
                        clientMacAddress);
                if (client != null) {
                    LOG.info("Client {} already exists on the cloud, update client values", dhcpLeasedIps);

                    ClientInfoDetails clientDetails = (ClientInfoDetails) client.getDetails();
                    if (dhcpLeasedIps.containsKey("hostname")) {

                        clientDetails.setHostName(dhcpLeasedIps.get("hostname"));

                    }

                    if (dhcpLeasedIps.containsKey("fingerprint")) {

                        clientDetails.setApFingerprint(dhcpLeasedIps.get("fingerprint"));
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


                } else {

                    client = new com.telecominfraproject.wlan.client.models.Client();

                    client.setCustomerId(customerId);
                    client.setMacAddress(clientMacAddress);

                    ClientInfoDetails clientDetails = new ClientInfoDetails();

                    if (dhcpLeasedIps.containsKey("hostname")) {

                        clientDetails.setHostName(dhcpLeasedIps.get("hostname"));

                    }

                    if (dhcpLeasedIps.containsKey("fingerprint")) {

                        clientDetails.setApFingerprint(dhcpLeasedIps.get("fingerprint"));
                    }

                    client.setDetails(clientDetails);

                    client = clientServiceInterface.create(client);

                    LOG.info("Created Client {}.", client);
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

                MacAddress clientMacAddress = new MacAddress(dhcpLeasedIps.get("hwaddr"));
                com.telecominfraproject.wlan.client.models.Client client = clientServiceInterface.getOrNull(customerId,
                        clientMacAddress);
                if (client == null) {
                    LOG.info("Client {} does not exist on the cloud. Creating...", dhcpLeasedIps);
                    client = new com.telecominfraproject.wlan.client.models.Client();
                    client.setCustomerId(customerId);
                    client.setMacAddress(clientMacAddress);
                    ClientInfoDetails clientDetails = new ClientInfoDetails();

                    client.setDetails(clientDetails);

                    client = clientServiceInterface.create(client);

                }

                ClientInfoDetails clientDetails = (ClientInfoDetails) client.getDetails();

                if (dhcpLeasedIps.containsKey("inet_addr")) {

                }

                if (dhcpLeasedIps.containsKey("hostname")) {

                    clientDetails.setHostName(dhcpLeasedIps.get("hostname"));

                }

                if (dhcpLeasedIps.containsKey("fingerprint")) {

                    clientDetails.setApFingerprint(dhcpLeasedIps.get("fingerprint"));
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
            session = new ClientSession();
        }
        session.setCustomerId(customerId);
        session.setEquipmentId(equipmentId);
        session.setLocationId(locationId);
        session.setMacAddress(clientMacAddress);
        session.setDetails(new ClientSessionDetails());

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

        if (dhcpLeasedIps.containsKey("lease_time")) {
            Integer leaseTime = Integer.valueOf(dhcpLeasedIps.get("lease_time"));
            clientDhcpDetails.setLeaseTimeInSeconds(leaseTime / 1000);
        }

        if (dhcpLeasedIps.containsKey("gateway")) {
            try {
                clientDhcpDetails.setGatewayIp(InetAddress.getByName(dhcpLeasedIps.get("gateway")));
            } catch (UnknownHostException e) {
                LOG.error("Invalid Gateway IP", e);

            }
        }

        if (dhcpLeasedIps.containsKey("subnet_mask")) {
            try {
                clientDhcpDetails.setSubnetMask(InetAddress.getByName(dhcpLeasedIps.get("subnet_mask")));
            } catch (UnknownHostException e) {
                LOG.error("Invalid Subnet Mask", e);

            }
        }

        if (dhcpLeasedIps.containsKey("primary_dns")) {
            try {
                clientDhcpDetails.setPrimaryDns(InetAddress.getByName(dhcpLeasedIps.get("primary_dns")));
            } catch (UnknownHostException e) {
                LOG.error("Invalid Primary DNS", e);

            }
        }

        if (dhcpLeasedIps.containsKey("secondary_dns")) {
            try {
                clientDhcpDetails.setSecondaryDns(InetAddress.getByName(dhcpLeasedIps.get("secondary_dns")));
            } catch (UnknownHostException e) {
                LOG.error("Invalid Secondary DNS", e);

            }
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


}
