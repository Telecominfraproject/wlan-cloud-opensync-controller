package com.telecominfraproject.wlan.opensync.external.integration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Predicate;
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
import com.telecominfraproject.wlan.alarm.AlarmServiceInterface;
import com.telecominfraproject.wlan.client.ClientServiceInterface;
import com.telecominfraproject.wlan.client.info.models.ClientInfoDetails;
import com.telecominfraproject.wlan.client.session.models.AssociationState;
import com.telecominfraproject.wlan.client.session.models.ClientSession;
import com.telecominfraproject.wlan.client.session.models.ClientSessionDetails;
import com.telecominfraproject.wlan.client.session.models.ClientSessionMetricDetails;
import com.telecominfraproject.wlan.cloudeventdispatcher.CloudEventDispatcherInterface;
import com.telecominfraproject.wlan.core.model.entity.CountryCode;
import com.telecominfraproject.wlan.core.model.equipment.AutoOrManualValue;
import com.telecominfraproject.wlan.core.model.equipment.ChannelBandwidth;
import com.telecominfraproject.wlan.core.model.equipment.DetectedAuthMode;
import com.telecominfraproject.wlan.core.model.equipment.EquipmentType;
import com.telecominfraproject.wlan.core.model.equipment.MacAddress;
import com.telecominfraproject.wlan.core.model.equipment.NeighborScanPacketType;
import com.telecominfraproject.wlan.core.model.equipment.NetworkType;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.core.model.equipment.SecurityType;
import com.telecominfraproject.wlan.core.model.equipment.WiFiSessionUtility;
import com.telecominfraproject.wlan.core.model.pagination.PaginationContext;
import com.telecominfraproject.wlan.core.model.pagination.PaginationResponse;
import com.telecominfraproject.wlan.core.model.utils.DecibelUtils;
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
import com.telecominfraproject.wlan.servicemetric.apnode.models.ApNodeMetrics;
import com.telecominfraproject.wlan.servicemetric.apnode.models.ApPerformance;
import com.telecominfraproject.wlan.servicemetric.apnode.models.DnsProbeMetric;
import com.telecominfraproject.wlan.servicemetric.apnode.models.EthernetLinkState;
import com.telecominfraproject.wlan.servicemetric.apnode.models.NetworkProbeMetrics;
import com.telecominfraproject.wlan.servicemetric.apnode.models.RadioStatistics;
import com.telecominfraproject.wlan.servicemetric.apnode.models.RadioUtilization;
import com.telecominfraproject.wlan.servicemetric.apnode.models.StateUpDownError;
import com.telecominfraproject.wlan.servicemetric.apssid.models.ApSsidMetrics;
import com.telecominfraproject.wlan.servicemetric.apssid.models.SsidStatistics;
import com.telecominfraproject.wlan.servicemetric.channelinfo.models.ChannelInfo;
import com.telecominfraproject.wlan.servicemetric.channelinfo.models.ChannelInfoReports;
import com.telecominfraproject.wlan.servicemetric.client.models.ClientMetrics;
import com.telecominfraproject.wlan.servicemetric.models.ServiceMetric;
import com.telecominfraproject.wlan.servicemetric.neighbourscan.models.NeighbourReport;
import com.telecominfraproject.wlan.servicemetric.neighbourscan.models.NeighbourScanReports;
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
import com.telecominfraproject.wlan.status.equipment.report.models.EquipmentCapacityDetails;
import com.telecominfraproject.wlan.status.equipment.report.models.OperatingSystemPerformance;
import com.telecominfraproject.wlan.status.equipment.report.models.RadioUtilizationReport;
import com.telecominfraproject.wlan.status.models.Status;
import com.telecominfraproject.wlan.status.models.StatusCode;
import com.telecominfraproject.wlan.status.models.StatusDataType;
import com.telecominfraproject.wlan.status.network.models.NetworkAdminStatusData;

import sts.OpensyncStats.Client;
import sts.OpensyncStats.ClientReport;
import sts.OpensyncStats.DNSProbeMetric;
import sts.OpensyncStats.Device;
import sts.OpensyncStats.Device.RadioTemp;
import sts.OpensyncStats.Neighbor;
import sts.OpensyncStats.Neighbor.NeighborBss;
import sts.OpensyncStats.NetworkProbe;
import sts.OpensyncStats.RADIUSMetrics;
import sts.OpensyncStats.RadioBandType;
import sts.OpensyncStats.Report;
import sts.OpensyncStats.Survey;
import sts.OpensyncStats.Survey.SurveySample;
import sts.OpensyncStats.SurveyType;
import sts.OpensyncStats.VLANMetrics;
import traffic.NetworkMetadata.FlowReport;
import wc.stats.IpDnsTelemetry.WCStatsReport;

@org.springframework.context.annotation.Profile("opensync_cloud_config")
@Component
public class OpensyncExternalIntegrationCloud implements OpensyncExternalIntegrationInterface {

    private static final Logger LOG = LoggerFactory.getLogger(OpensyncExternalIntegrationCloud.class);

    @Autowired
    private AlarmServiceInterface alarmServiceInterface;
    @Autowired
    private CustomerServiceInterface customerServiceInterface;
    @Autowired
    private LocationServiceInterface locationServiceInterface;
    @Autowired
    private OvsdbSessionMapInterface ovsdbSessionMapInterface;
    @Autowired
    private CloudEventDispatcherInterface equipmentMetricsCollectorInterface;
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
            ce = cloudEquipmentRecordCache.get(apId, new Callable<Equipment>() {
                @Override
                public Equipment call() throws Exception {
                    return equipmentServiceInterface.getByInventoryIdOrNull(apId);
                }
            });
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
                for (Profile ssidProfile : ret.getSsidProfile()) {

                    radiusSet.addAll(profileContainer.getChildrenOfType(ssidProfile.getId(), ProfileType.radius));
                    if (ssidProfile.getDetails() != null) {
                        Long captivePortId = ((SsidConfiguration) ssidProfile.getDetails()).getCaptivePortalId();
                        if (captivePortId != null) {
                            captiveProfileIds.add(captivePortId);
                        }
                    }
                }
                ret.setRadiusProfiles(new ArrayList<>(radiusSet));
                ret.setCaptiveProfiles(profileServiceInterface.get(captiveProfileIds));

                List<com.telecominfraproject.wlan.client.models.Client> blockedClients = clientServiceInterface
                        .getBlockedClients(customerId);
                List<MacAddress> blockList = Lists.newArrayList();
                if ((blockedClients != null) && !blockedClients.isEmpty()) {
                    blockedClients.forEach(new Consumer<com.telecominfraproject.wlan.client.models.Client>() {
                        @Override
                        public void accept(com.telecominfraproject.wlan.client.models.Client client) {
                            blockList.add(client.getMacAddress());
                        }
                    });
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

    /**
     * @param topic
     * @return apId extracted from the topic name, or null if it cannot be
     *         extracted
     */
    public static String extractApIdFromTopic(String topic) {
        // Topic is formatted as
        // "/ap/"+clientCn+"_"+ret.serialNumber+"/opensync"
        if (topic == null) {
            return null;
        }

        String[] parts = topic.split("/");
        if (parts.length < 3) {
            return null;
        }

        // apId is the third element in the topic
        return parts[2];
    }

    /**
     * @param topic
     * @return customerId looked up from the topic name, or -1 if it cannot be
     *         extracted
     */
    public int extractCustomerIdFromTopic(String topic) {

        String apId = extractApIdFromTopic(topic);
        if (apId == null) {
            return -1;
        }

        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession != null) {
            return ovsdbSession.getCustomerId();
        }

        return -1;

    }

    public long extractEquipmentIdFromTopic(String topic) {

        String apId = extractApIdFromTopic(topic);
        if (apId == null) {
            return -1;
        }

        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession != null) {
            return ovsdbSession.getEquipmentId();
        }

        return -1;

    }

    @Override
    public void processMqttMessage(String topic, Report report) {
        LOG.info("Received report on topic {} for ap {}", topic, report.getNodeID());
        int customerId = extractCustomerIdFromTopic(topic);
        String apId = extractApIdFromTopic(topic);
        long equipmentId = extractEquipmentIdFromTopic(topic);
        if ((equipmentId <= 0) || (customerId <= 0)) {
            LOG.warn("Cannot determine equipment ids from topic {} - customerId {} equipmentId {}", topic, customerId,
                    equipmentId);
            return;
        }

        gatewayController.updateActiveCustomer(customerId);

        Equipment ce = getCustomerEquipment(apId);
        if (ce == null) {
            LOG.warn("Cannot read equipment {}", apId);
            return;
        }

        long locationId = ce.getLocationId();

        List<ServiceMetric> metricRecordList = new ArrayList<>();

        try {
            populateApClientMetrics(metricRecordList, report, customerId, equipmentId, locationId);
            populateApNodeMetrics(metricRecordList, report, customerId, equipmentId, locationId);
            populateNeighbourScanReports(metricRecordList, report, customerId, equipmentId, locationId);
            populateChannelInfoReports(metricRecordList, report, customerId, equipmentId, locationId);
            populateApSsidMetrics(metricRecordList, report, customerId, equipmentId, apId, locationId);
            // handleRssiMetrics(metricRecordList, report, customerId,
            // equipmentId, locationId);

        } catch (Exception e) {
            LOG.error("Exception when processing populateApSsidMetrics", e);
        }

        if (!metricRecordList.isEmpty()) {
            LOG.debug("Publishing Metrics {}", metricRecordList);
            equipmentMetricsCollectorInterface.publishMetrics(metricRecordList);
        }

    }

    private void populateApNodeMetrics(List<ServiceMetric> metricRecordList, Report report, int customerId,
            long equipmentId, long locationId) {
        LOG.debug("populateApNodeMetrics for Customer {} Equipment {}", customerId, equipmentId);
        ApNodeMetrics apNodeMetrics = new ApNodeMetrics();
        ServiceMetric smr = new ServiceMetric(customerId, equipmentId);

        smr.setLocationId(locationId);
        metricRecordList.add(smr);
        smr.setDetails(apNodeMetrics);

        for (Device deviceReport : report.getDeviceList()) {

            int avgRadioTemp = 0;

            ApPerformance apPerformance = new ApPerformance();
            apNodeMetrics.setApPerformance(apPerformance);

            smr.setCreatedTimestamp(deviceReport.getTimestampMs());

            if (deviceReport.getRadioTempCount() > 0) {
                float cpuTemperature = 0;
                int numSamples = 0;
                for (RadioTemp r : deviceReport.getRadioTempList()) {
                    if (r.hasValue()) {
                        cpuTemperature += r.getValue();
                        numSamples++;
                    }
                }

                if (numSamples > 0) {
                    avgRadioTemp = Math.round((cpuTemperature / numSamples));
                    apPerformance.setCpuTemperature(avgRadioTemp);
                }
            }

            if (deviceReport.hasCpuUtil() && deviceReport.getCpuUtil().hasCpuUtil()) {
                Integer cpuUtilization = deviceReport.getCpuUtil().getCpuUtil();
                apPerformance.setCpuUtilized(new byte[] { cpuUtilization.byteValue() });
            }

            apPerformance.setEthLinkState(EthernetLinkState.UP1000_FULL_DUPLEX);

            if (deviceReport.hasMemUtil() && deviceReport.getMemUtil().hasMemTotal()
                    && deviceReport.getMemUtil().hasMemUsed()) {
                apPerformance.setFreeMemory(
                        deviceReport.getMemUtil().getMemTotal() - deviceReport.getMemUtil().getMemUsed());
            }
            apPerformance.setUpTime((long) deviceReport.getUptime());

            updateDeviceStatusForReport(customerId, equipmentId, deviceReport, avgRadioTemp);

        }

        // statusList.add(status);

        // Main Network dashboard shows Traffic and Capacity values that
        // are calculated from
        // ApNodeMetric properties getPeriodLengthSec, getRxBytes2G,
        // getTxBytes2G, getRxBytes5G, getTxBytes5G

        // go over all the clients to aggregate per-client tx/rx stats -
        // we want to do this
        // only once per batch of ApNodeMetrics - so we do not repeat
        // values over and over again

        for (ClientReport clReport : report.getClientsList()) {

            long rxBytes = 0;
            long txBytes = 0;

            long txFrames = 0;
            long rxFrames = 0;
            int txRetries = 0;
            int rxRetries = 0;
            int rxErrors = 0;
            int rssi = 0;

            Set<String> clientMacs = new HashSet<>();

            for (Client cl : clReport.getClientListList()) {

                if (!cl.hasConnected() || (cl.getConnected() != true)) {
                    // this client is not currently connected, skip it
                    // TODO: how come AP reports disconencted clients? What
                    // if it is a busy coffe shop with thousands of peopele
                    // per day, when do clients disappear from the reports?
                    continue;
                }

                if (cl.hasMacAddress()) {
                    clientMacs.add(cl.getMacAddress());
                }

                if (cl.getStats().hasTxBytes()) {
                    txBytes += cl.getStats().getTxBytes();
                }
                if (cl.getStats().hasRxBytes()) {
                    rxBytes += cl.getStats().getRxBytes();
                }
                if (cl.getStats().hasRssi()) {
                    rssi += cl.getStats().getRssi();
                }
                if (cl.getStats().hasTxFrames()) {
                    txFrames += cl.getStats().getTxFrames();
                }
                if (cl.getStats().hasRxFrames()) {
                    rxFrames += cl.getStats().getRxFrames();
                }
                if (cl.getStats().hasTxRetries()) {
                    txRetries += cl.getStats().getTxRetries();
                }
                if (cl.getStats().hasRxRetries()) {
                    rxRetries += cl.getStats().getRxRetries();
                }
                if (cl.getStats().hasTxErrors()) {
                    cl.getStats().getTxErrors();
                }
                if (cl.getStats().hasRxErrors()) {
                    rxErrors += cl.getStats().getRxErrors();
                }
                if (cl.getStats().hasTxRate()) {
                    cl.getStats().getTxRate();
                }
                if (cl.getStats().hasRxRate()) {
                    cl.getStats().getRxRate();
                }

            }

            RadioType radioType = getRadioTypeFromOpensyncRadioBand(clReport.getBand());
            RadioStatistics radioStats = apNodeMetrics.getRadioStats(radioType);
            if (radioStats == null) {
                radioStats = new RadioStatistics();
            }

            radioStats.setNumTxRetryAttemps(txRetries);
            radioStats.setNumTxFramesTransmitted(txFrames);
            radioStats.setNumTxRetryAttemps(txRetries);
            radioStats.setCurChannel(clReport.getChannel());
            if (clReport.getClientListCount() > 0) {
                radioStats.setRxLastRssi(getNegativeSignedIntFromUnsigned(rssi) / clReport.getClientListCount());
            }
            radioStats.setRxDataBytes(rxBytes);
            radioStats.setNumRxDataFrames(rxFrames);
            radioStats.setNumRxErr(rxErrors);
            radioStats.setNumRxRetry(rxRetries);

            apNodeMetrics.setRadioStats(radioType, radioStats);

            apNodeMetrics.setRxBytes(radioType, rxBytes);
            apNodeMetrics.setTxBytes(radioType, txBytes);

            List<MacAddress> clientMacList = new ArrayList<>();
            clientMacs.forEach(new Consumer<String>() {
                @Override
                public void accept(String macStr) {
                    try {
                        clientMacList.add(new MacAddress(macStr));
                    } catch (RuntimeException e) {
                        LOG.warn("Cannot parse mac address from MQTT ClientReport {} ", macStr);
                    }
                }
            });

            apNodeMetrics.setClientMacAddresses(radioType, clientMacList);

            // TODO: Radio Utilization will be calculated when the survey is
            // enabled

            apNodeMetrics.setRadioUtilization(radioType, new ArrayList<RadioUtilization>());

        }

        // Status radioUtilizationStatus =
        // statusServiceInterface.getOrNull(customerId, equipmentId,
        // StatusDataType.RADIO_UTILIZATION);
        //
        // if (radioUtilizationStatus != null) {
        // RadioUtilizationReport radioUtilizationReport =
        // (RadioUtilizationReport) radioUtilizationStatus
        // .getDetails();
        // }

        apNodeMetrics.setPeriodLengthSec(60);

        // Now try to populate metrics for calculation of radio capacity
        // see
        // com.telecominfraproject.wlan.metrics.streaming.spark.equipmentreport.CapacityDStreamsConfig.toAggregatedStats(int,
        // long, ApNodeMetric data)
        // result.stats2g =
        // toAggregatedRadioStats(data.getNoiseFloor2G(),data.getRadioUtilization2G());
        // result.stats5g =
        // toAggregatedRadioStats(data.getNoiseFloor5G(),data.getRadioUtilization5G());
        // RadioUtilization
        // private Integer assocClientTx;
        // private Integer unassocClientTx;
        // private Integer assocClientRx;
        // private Integer unassocClientRx;
        // private Integer nonWifi;
        // private Integer timestampSeconds;
        Map<RadioType, Integer> avgNoiseFloor = new HashMap<>();
        new HashMap<>();
        Map<RadioType, EquipmentCapacityDetails> capacityDetails = new HashMap<>();
        RadioUtilizationReport radioUtilizationReport = new RadioUtilizationReport();

        // populate it from report.survey
        for (Survey survey : report.getSurveyList()) {

            int oBSS = 0;
            int iBSS = 0;
            int totalBusy = 0;
            int durationMs = 0;
            RadioType radioType = RadioType.UNSUPPORTED;

            List<Integer> noiseList = new ArrayList<>();
            for (SurveySample surveySample : survey.getSurveyListList()) {
                if (surveySample.getDurationMs() == 0) {
                    continue;
                }

                iBSS += surveySample.getBusySelf() + surveySample.getBusyTx();
                oBSS += surveySample.getBusyRx();
                totalBusy += surveySample.getBusy();
                durationMs += surveySample.getDurationMs();
                noiseList.add(getNegativeSignedIntFrom8BitUnsigned(surveySample.getNoise()));
                RadioUtilization radioUtil = new RadioUtilization();
                radioUtil.setTimestampSeconds((int) ((survey.getTimestampMs() + surveySample.getOffsetMs()) / 1000));
                radioUtil.setAssocClientTx((100 * surveySample.getBusyTx()) / surveySample.getDurationMs());
                radioUtil.setAssocClientRx((100 * surveySample.getBusyRx()) / surveySample.getDurationMs());
                // TODO not totally correct, NonWifi = totalBusy - iBSS - oBSS
                radioUtil.setNonWifi(
                        (100 * (surveySample.getBusy() - surveySample.getBusyTx() - surveySample.getBusyRx()))
                                / surveySample.getDurationMs());

                switch (survey.getBand()) {
                case BAND2G:
                    radioType = RadioType.is2dot4GHz;
                    break;
                case BAND5G:
                    radioType = RadioType.is5GHz;
                    break;
                case BAND5GL:
                    radioType = RadioType.is5GHzL;
                    break;
                case BAND5GU:
                    radioType = RadioType.is5GHzU;
                    break;
                }

                if (radioType != RadioType.UNSUPPORTED) {

                    if (!apNodeMetrics.getRadioUtilizationPerRadio().containsKey(radioType)) {
                        List<RadioUtilization> radioUtilizationList = new ArrayList<>();
                        radioUtilizationList.add(radioUtil);
                        apNodeMetrics.getRadioUtilizationPerRadio().put(radioType, radioUtilizationList);
                    } else {
                        apNodeMetrics.getRadioUtilizationPerRadio().get(radioType).add(radioUtil);
                    }
                }
            }

            if ((survey.getSurveyListCount() > 0) && (radioType != RadioType.UNSUPPORTED)) {
                int noiseAvg = (int) Math.round(DecibelUtils.getAverageDecibel(toIntArray(noiseList)));
                avgNoiseFloor.put(radioType, noiseAvg);
                apNodeMetrics.setNoiseFloor(radioType, noiseAvg);
            }
            Double totalUtilization = (100D * totalBusy) / durationMs;
            Double totalNonWifi = (100D * (totalBusy - iBSS - oBSS)) / durationMs;

            EquipmentCapacityDetails cap = new EquipmentCapacityDetails();
            cap.setUnavailableCapacity(totalNonWifi.intValue());
            int avaiCapacity = 100 - totalNonWifi.intValue();
            cap.setAvailableCapacity(avaiCapacity);
            cap.setUsedCapacity(totalUtilization.intValue());
            cap.setUnusedCapacity(avaiCapacity - totalUtilization.intValue());

            if (radioType != RadioType.UNSUPPORTED) {
                apNodeMetrics.setChannelUtilization(radioType, totalUtilization.intValue());
                capacityDetails.put(radioType, cap);
            }
        }

        new RadioStatistics();

        populateNetworkProbeMetrics(report, apNodeMetrics);

        radioUtilizationReport.setAvgNoiseFloor(avgNoiseFloor);
        radioUtilizationReport.setCapacityDetails(capacityDetails);

        updateDeviceStatusRadioUtilizationReport(customerId, equipmentId, radioUtilizationReport);
    }

    private void updateDeviceStatusRadioUtilizationReport(int customerId, long equipmentId,
            RadioUtilizationReport radioUtilizationReport) {
        LOG.debug(
                "Processing updateDeviceStatusRadioUtilizationReport for equipmentId {} with RadioUtilizationReport {}",
                equipmentId, radioUtilizationReport);

        Status radioUtilizationStatus = statusServiceInterface.getOrNull(customerId, equipmentId,
                StatusDataType.RADIO_UTILIZATION);

        if (radioUtilizationStatus == null) {
            LOG.debug("Create new radioUtilizationStatus");
            radioUtilizationStatus = new Status();
            radioUtilizationStatus.setCustomerId(customerId);
            radioUtilizationStatus.setEquipmentId(equipmentId);
            radioUtilizationStatus.setStatusDataType(StatusDataType.RADIO_UTILIZATION);
        }

        radioUtilizationStatus.setDetails(radioUtilizationReport);

        statusServiceInterface.update(radioUtilizationStatus);
    }

    void populateNetworkProbeMetrics(Report report, ApNodeMetrics apNodeMetrics) {
        List<NetworkProbeMetrics> networkProbeMetricsList = new ArrayList<>();

        for (NetworkProbe networkProbe : report.getNetworkProbeList()) {

            NetworkProbeMetrics networkProbeMetrics = new NetworkProbeMetrics();

            List<DnsProbeMetric> dnsProbeResults = new ArrayList<>();
            if (networkProbe.hasDnsProbe()) {

                DNSProbeMetric dnsProbeMetricFromAp = networkProbe.getDnsProbe();

                LOG.debug("DNSProbeMetric from AP {}", dnsProbeMetricFromAp);

                DnsProbeMetric dnsProbeMetric = new DnsProbeMetric();

                if (dnsProbeMetricFromAp.hasLatency()) {
                    networkProbeMetrics.setDnsLatencyMs(dnsProbeMetricFromAp.getLatency());
                    dnsProbeMetric.setDnsLatencyMs(dnsProbeMetricFromAp.getLatency());
                }

                if (dnsProbeMetricFromAp.hasState()) {
                    switch (dnsProbeMetricFromAp.getState()) {
                    case SUD_down:
                        networkProbeMetrics.setDnsState(StateUpDownError.disabled);
                        dnsProbeMetric.setDnsState(StateUpDownError.disabled);
                        break;
                    case SUD_up:
                        networkProbeMetrics.setDnsState(StateUpDownError.enabled);
                        dnsProbeMetric.setDnsState(StateUpDownError.enabled);
                        break;
                    case SUD_error:
                        networkProbeMetrics.setDnsState(StateUpDownError.error);
                        dnsProbeMetric.setDnsState(StateUpDownError.error);
                        break;
                    default:
                        networkProbeMetrics.setDnsState(StateUpDownError.UNSUPPORTED);
                        dnsProbeMetric.setDnsState(StateUpDownError.UNSUPPORTED);
                    }
                }

                if (dnsProbeMetricFromAp.hasServerIP()) {
                    InetAddress ipAddress;
                    try {
                        ipAddress = InetAddress.getByName(dnsProbeMetricFromAp.getServerIP());
                        dnsProbeMetric.setDnsServerIp(ipAddress);

                    } catch (UnknownHostException e) {
                        LOG.error("Could not get DNS Server IP from network_probe metrics", e);
                    }
                }

                dnsProbeResults.add(dnsProbeMetric);

            }

            networkProbeMetrics.setDnsProbeResults(dnsProbeResults);

            if (networkProbe.hasRadiusProbe()) {

                RADIUSMetrics radiusMetrics = networkProbe.getRadiusProbe();

                LOG.debug("Network Probe Radius Metrics {}", radiusMetrics);
                if (networkProbe.hasVlanProbe()) {
                    if (networkProbe.getVlanProbe().hasObsV200RadiusLatency()) {
                        networkProbeMetrics.setRadiusLatencyInMs(networkProbe.getVlanProbe().getObsV200RadiusLatency());
                    }
                    if (networkProbe.getVlanProbe().hasObsV200RadiusState()) {
                        switch (networkProbe.getVlanProbe().getObsV200RadiusState()) {
                        case SUD_down:
                            networkProbeMetrics.setRadiusState(StateUpDownError.disabled);
                            break;
                        case SUD_up:
                            networkProbeMetrics.setRadiusState(StateUpDownError.enabled);
                            break;
                        case SUD_error:
                            networkProbeMetrics.setRadiusState(StateUpDownError.error);
                            break;
                        default:
                            networkProbeMetrics.setRadiusState(StateUpDownError.UNSUPPORTED);
                        }

                    }
                } else {
                    // take the average if we don't have from the VLAN Probe
                    if (radiusMetrics.hasLatencyAve()) {
                        networkProbeMetrics.setRadiusLatencyInMs(radiusMetrics.getLatencyAve());
                    }
                }

            }
            if (networkProbe.hasVlanProbe()) {
                VLANMetrics vlanMetrics = networkProbe.getVlanProbe();

                LOG.debug("NetworkProbe Vlan Metrics {}", vlanMetrics);
                if (vlanMetrics.hasVlanIF()) {
                    networkProbeMetrics.setVlanIF(vlanMetrics.getVlanIF());
                }
                if (vlanMetrics.hasDhcpLatency()) {
                    networkProbeMetrics.setDhcpLatencyMs(vlanMetrics.getDhcpLatency());
                }
                if (vlanMetrics.hasDhcpState()) {
                    switch (vlanMetrics.getDhcpState()) {
                    case SUD_down:
                        networkProbeMetrics.setDhcpState(StateUpDownError.disabled);
                        break;
                    case SUD_up:
                        networkProbeMetrics.setDhcpState(StateUpDownError.enabled);
                        break;
                    case SUD_error:
                        networkProbeMetrics.setDhcpState(StateUpDownError.error);
                        break;
                    default:
                        networkProbeMetrics.setDhcpState(StateUpDownError.UNSUPPORTED);
                    }
                }

            }

            networkProbeMetricsList.add(networkProbeMetrics);

        }

        apNodeMetrics.setNetworkProbeMetrics(networkProbeMetricsList);
    }

    private void updateDeviceStatusForReport(int customerId, long equipmentId, Device deviceReport, int avgRadioTemp) {
        Status status = new Status();
        status.setCustomerId(customerId);
        status.setEquipmentId(equipmentId);
        OperatingSystemPerformance eqOsPerformance = new OperatingSystemPerformance();
        eqOsPerformance.setUptimeInSeconds(deviceReport.getUptime());
        eqOsPerformance.setAvgCpuTemperature(avgRadioTemp);
        eqOsPerformance.setAvgCpuUtilization(deviceReport.getCpuUtil().getCpuUtil());
        eqOsPerformance
                .setAvgFreeMemoryKb(deviceReport.getMemUtil().getMemTotal() - deviceReport.getMemUtil().getMemUsed());
        eqOsPerformance.setTotalAvailableMemoryKb(deviceReport.getMemUtil().getMemTotal());
        status.setDetails(eqOsPerformance);
        status = statusServiceInterface.update(status);
        LOG.debug("updated status {}", status);

        // Status networkAggStatusRec =
        // statusServiceInterface.getOrNull(customerId, equipmentId,
        // StatusDataType.NETWORK_AGGREGATE);
        //
        // NetworkAggregateStatusData naStatusData =
        // (NetworkAggregateStatusData) networkAggStatusRec.getDetails();
        //
        // EquipmentPerformanceDetails equipmentPerformanceDetails = new
        // EquipmentPerformanceDetails();
        // equipmentPerformanceDetails.setAvgCpuTemperature((int)
        // eqOsPerformance.getAvgCpuTemperature());
        // equipmentPerformanceDetails.setAvgFreeMemory(eqOsPerformance.getAvgFreeMemoryKb());
        //
        // naStatusData.setApPerformanceDetails(equipmentPerformanceDetails);
        // networkAggStatusRec.setDetails(naStatusData);
        // networkAggStatusRec =
        // statusServiceInterface.update(networkAggStatusRec);
        //
        // LOG.debug("updated aggregate status {}", networkAggStatusRec);

    }

    private void populateApClientMetrics(List<ServiceMetric> metricRecordList, Report report, int customerId,
            long equipmentId, long locationId) {
        LOG.debug("populateApClientMetrics for Customer {} Equipment {}", customerId, equipmentId);

        for (ClientReport clReport : report.getClientsList()) {
            LOG.debug("Opensync Stats for ClientReport {}", clReport);

            for (Client cl : clReport.getClientListList()) {

                if (cl.getMacAddress() == null) {
                    LOG.debug(
                            "No mac address for Client {}, cannot set device mac address for client in ClientMetrics.",
                            cl);
                    continue;
                }

                LOG.debug("Processing ClientReport from AP {}", clReport);

                ServiceMetric smr = new ServiceMetric(customerId, equipmentId, new MacAddress(cl.getMacAddress()));
                smr.setLocationId(locationId);
                metricRecordList.add(smr);

                smr.setCreatedTimestamp(clReport.getTimestampMs());
                smr.setClientMac(new MacAddress(cl.getMacAddress()).getAddressAsLong());

                // clReport.getChannel();
                ClientMetrics cMetrics = new ClientMetrics();
                smr.setDetails(cMetrics);

                Integer periodLengthSec = 60; // matches what's configured by
                // OvsdbDao.configureStats(OvsdbClient)
                cMetrics.setPeriodLengthSec(periodLengthSec);

                cMetrics.setRadioType(getRadioTypeFromOpensyncRadioBand(clReport.getBand()));
                // we'll report each device as having a single (very long)
                // session
                long sessionId = WiFiSessionUtility.encodeWiFiAssociationId(clReport.getTimestampMs() / 1000L,
                        MacAddress.convertMacStringToLongValue(cl.getMacAddress()));

                LOG.debug("populateApClientMetrics Session Id {}", sessionId);
                cMetrics.setSessionId(sessionId);

                if (cl.hasStats()) {
                    if (cl.getStats().hasRssi()) {
                        int unsignedRssi = cl.getStats().getRssi();
                        cMetrics.setRssi(getNegativeSignedIntFromUnsigned(unsignedRssi));
                    }

                    // populate Rx stats
                    if (cl.getStats().hasRxBytes()) {
                        cMetrics.setRxBytes(cl.getStats().getRxBytes());
                    }

                    if (cl.getStats().hasRxRate()) {
                        cMetrics.setAverageRxRate(cl.getStats().getTxRate() / 1000);
                    }

                    if (cl.getStats().hasRxErrors()) {
                        cMetrics.setNumRxNoFcsErr((int) cl.getStats().getRxErrors());
                    }

                    if (cl.getStats().hasRxFrames()) {
                        cMetrics.setNumRxFramesReceived(cl.getStats().getRxFrames());
                        // cMetrics.setNumRxPackets(cl.getStats().getRxFrames());
                    }

                    if (cl.getStats().hasRxRetries()) {
                        cMetrics.setNumRxRetry((int) cl.getStats().getRxRetries());
                    }

                    // populate Tx stats
                    if (cl.getStats().hasTxBytes()) {
                        cMetrics.setNumTxBytes(cl.getStats().getTxBytes());
                    }

                    if (cl.getStats().hasTxRate()) {
                        cMetrics.setAverageTxRate(cl.getStats().getTxRate() / 1000);
                    }

                    if (cl.getStats().hasTxRate() && cl.getStats().hasRxRate()) {
                        cMetrics.setRates(new byte[] { Double.valueOf(cl.getStats().getTxRate() / 1000).byteValue(),
                                Double.valueOf(cl.getStats().getRxRate() / 1000).byteValue() });
                    }

                    if (cl.getStats().hasTxErrors()) {
                        cMetrics.setNumTxDropped((int) cl.getStats().getTxErrors());
                    }

                    if (cl.getStats().hasRxFrames()) {
                        cMetrics.setNumTxFramesTransmitted(cl.getStats().getTxFrames());
                    }

                    if (cl.getStats().hasTxRetries()) {
                        cMetrics.setNumTxDataRetries((int) cl.getStats().getTxRetries());
                    }
                }

                LOG.debug("ApClientMetrics Report {}", cMetrics);

            }

        }

    }

    private void populateNeighbourScanReports(List<ServiceMetric> metricRecordList, Report report, int customerId,
            long equipmentId, long locationId) {
        LOG.debug("populateNeighbourScanReports for Customer {} Equipment {}", customerId, equipmentId);

        for (Neighbor neighbor : report.getNeighborsList()) {

            ServiceMetric smr = new ServiceMetric(customerId, equipmentId);
            smr.setLocationId(locationId);
            metricRecordList.add(smr);
            NeighbourScanReports neighbourScanReports = new NeighbourScanReports();
            smr.setDetails(neighbourScanReports);
            smr.setCreatedTimestamp(neighbor.getTimestampMs());

            List<NeighbourReport> neighbourReports = new ArrayList<>();
            neighbourScanReports.setNeighbourReports(neighbourReports);

            for (NeighborBss nBss : neighbor.getBssListList()) {
                NeighbourReport nr = new NeighbourReport();
                neighbourReports.add(nr);

                if (neighbor.getBand() == RadioBandType.BAND2G) {
                    nr.setAcMode(false);
                    nr.setbMode(false);
                    nr.setnMode(true);
                    nr.setRadioType(RadioType.is2dot4GHz);
                } else if (neighbor.getBand() == RadioBandType.BAND5G) {
                    nr.setAcMode(true);
                    nr.setbMode(false);
                    nr.setnMode(false);
                    nr.setRadioType(RadioType.is5GHz);
                } else if (neighbor.getBand() == RadioBandType.BAND5GL) {
                    nr.setAcMode(true);
                    nr.setbMode(false);
                    nr.setnMode(false);
                    nr.setRadioType(RadioType.is5GHzL);
                } else if (neighbor.getBand() == RadioBandType.BAND5GU) {
                    nr.setAcMode(true);
                    nr.setbMode(false);
                    nr.setnMode(false);
                    nr.setRadioType(RadioType.is5GHzU);
                }

                nr.setChannel(nBss.getChannel());
                nr.setMacAddress(new MacAddress(nBss.getBssid()));
                nr.setNetworkType(NetworkType.AP);
                nr.setPacketType(NeighborScanPacketType.BEACON);
                nr.setPrivacy(((nBss.getSsid() == null) || nBss.getSsid().isEmpty()) ? true : false);
                // nr.setRate(rate);
                // we can only get Rssi as an unsigned int from opensync, so
                // some shifting
                nr.setRssi(getNegativeSignedIntFromUnsigned(nBss.getRssi()));
                // nr.setScanTimeInSeconds(scanTimeInSeconds);
                nr.setSecureMode(DetectedAuthMode.WPA);
                // nr.setSignal(signal);
                nr.setSsid(nBss.getSsid());
            }

        }
    }

    private void handleClientSessionUpdate(int customerId, long equipmentId, String apId, long locationId, int channel,
            RadioBandType band, long timestamp, sts.OpensyncStats.Client client, String nodeId, MacAddress bssidAddress,
            String ssid) {
        try

        {
            LOG.info("handleClientSessionUpdate for {} on BSSID {}", client, bssidAddress);

            com.telecominfraproject.wlan.client.models.Client clientInstance = clientServiceInterface
                    .getOrNull(customerId, new MacAddress(client.getMacAddress()));
            if (clientInstance != null) {

                ClientInfoDetails clientDetails = (ClientInfoDetails) clientInstance.getDetails();

                clientDetails.setAlias("alias " + clientInstance.getMacAddress().getAddressAsLong());
                clientDetails.setApFingerprint("fp " + clientInstance.getMacAddress().getAddressAsString());
                clientDetails.setHostName("hostName-" + clientInstance.getMacAddress().getAddressAsLong());
                clientDetails.setUserName("user-" + clientInstance.getMacAddress().getAddressAsLong());
                clientInstance.setDetails(clientDetails);
                clientInstance = clientServiceInterface.update(clientInstance);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("handleClientSessionUpdate update ClientInstance {}", clientInstance.toPrettyString());
                } else {
                    LOG.info("handleClientSessionUpdate update ClientInstance {}", clientInstance);

                }

                clientDetails = (ClientInfoDetails) clientInstance.getDetails();

                ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId,
                        new MacAddress(client.getMacAddress()));

                if (clientSession != null) {
                    LOG.debug("Session found for Client {}.", client.getMacAddress());

                    ClientSessionDetails clientSessionDetails = new ClientSessionDetails();
                    clientSessionDetails.setRadioType(getRadioTypeFromOpensyncRadioBand(band));
                    clientSessionDetails.setSsid(ssid);
                    clientSessionDetails.setAssocRssi(getNegativeSignedIntFromUnsigned(client.getStats().getRssi()));

                    if (client.getConnected()) {
                        if (client.hasConnectCount()) {
                            if (client.hasConnectOffsetMs()) {
                                clientSessionDetails.setAssocTimestamp(timestamp + client.getConnectOffsetMs());
                            } else {
                                clientSessionDetails.setAssocTimestamp(timestamp);
                            }
                        }
                        long sessionId = WiFiSessionUtility.encodeWiFiAssociationId(timestamp / 1000L,
                                MacAddress.convertMacStringToLongValue(client.getMacAddress()));
                        clientSessionDetails.setSessionId(sessionId);
                        LOG.debug("handleClientSessionUpdate Session Id {}", sessionId);

                        clientSessionDetails.setLastEventTimestamp(timestamp);
                        clientSessionDetails.setLastRxTimestamp(timestamp);
                        clientSessionDetails.setLastTxTimestamp(timestamp);
                        if (client.hasDurationMs()) {
                            clientSessionDetails.setFirstDataRcvdTimestamp(timestamp - client.getDurationMs());
                            clientSessionDetails.setFirstDataSentTimestamp(timestamp - client.getDurationMs());

                        }
                        Equipment ce = equipmentServiceInterface.getByInventoryIdOrNull(apId);
                        if (ce != null) {
                            ProfileContainer profileContainer = new ProfileContainer(
                                    profileServiceInterface.getProfileWithChildren(ce.getProfileId()));

                            List<Profile> ssidProfiles = profileContainer.getChildrenOfType(ce.getProfileId(),
                                    ProfileType.ssid);

                            for (Profile ssidProfile : ssidProfiles) {

                                SsidConfiguration ssidConfig = (SsidConfiguration) ssidProfile.getDetails();
                                if (ssidConfig.getAppliedRadios().contains(getRadioTypeFromOpensyncRadioBand(band))
                                        && ssidConfig.getSsid().equals(ssid)) {
                                    clientSessionDetails.setIs11KUsed(ssidConfig.getRadioBasedConfigs()
                                            .get(getRadioTypeFromOpensyncRadioBand(band)).getEnable80211k());
                                    clientSessionDetails.setIs11RUsed(ssidConfig.getRadioBasedConfigs()
                                            .get(getRadioTypeFromOpensyncRadioBand(band)).getEnable80211r());
                                    clientSessionDetails.setIs11VUsed(ssidConfig.getRadioBasedConfigs()
                                            .get(getRadioTypeFromOpensyncRadioBand(band)).getEnable80211v());

                                    SecureMode secureMode = ssidConfig.getSecureMode();
                                    if (secureMode.equals(SecureMode.open)) {
                                        clientSessionDetails.setSecurityType(SecurityType.OPEN);
                                    } else if (secureMode.equals(SecureMode.wpa2OnlyPSK)
                                            || secureMode.equals(SecureMode.wpaPSK)
                                            || secureMode.equals(SecureMode.wpa2PSK)) {
                                        clientSessionDetails.setSecurityType(SecurityType.PSK);
                                    } else if (secureMode.equals(SecureMode.wpa2OnlyEAP)
                                            || secureMode.equals(SecureMode.wpa2EAP)
                                            || secureMode.equals(SecureMode.wpaEAP)) {
                                        clientSessionDetails.setSecurityType(SecurityType.RADIUS);
                                    }

                                }

                            }

                        }

                    } else {
                        clientSessionDetails.setAssociationState(AssociationState.Disconnected);
                        if (client.hasDisconnectCount()) {
                            if (client.hasDisconnectOffsetMs()) {
                                clientSessionDetails
                                        .setDisconnectByClientTimestamp(timestamp - client.getDisconnectOffsetMs());

                                if (client.hasDurationMs()) {
                                    clientSessionDetails
                                            .setLastEventTimestamp(timestamp - client.getDisconnectOffsetMs());
                                    clientSessionDetails.setLastRxTimestamp(timestamp - client.getDisconnectOffsetMs());
                                    clientSessionDetails.setLastTxTimestamp(timestamp - client.getDisconnectOffsetMs());

                                }
                            } else {
                                clientSessionDetails.setDisconnectByClientTimestamp(timestamp);

                            }
                        } else {
                            clientSessionDetails.setDisconnectByClientTimestamp(timestamp);
                        }

                    }

                    // update the client metrics, based on what we see from
                    // the MQTT data

                    if (client.getDurationMs() > 0) {
                        clientSessionDetails.setMetricDetails(calculateClientSessionMetricDetails(client, timestamp));
                    }
                    clientSession.getDetails().mergeSession(clientSessionDetails);
                    clientSession.getDetails()
                            .setAssociationState(clientSession.getDetails().calculateAssociationState());

                    LOG.debug("Assocation State {}", clientSession.getDetails().getAssociationState());
                    clientSession = clientServiceInterface.updateSession(clientSession);

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("handleClientSessionUpdate Updated clientSession {}", clientSession.toPrettyString());
                    } else {
                        LOG.info("handleClientSessionUpdate Updated clientSession {}", clientSession);

                    }
                }

            }

        } catch (Exception e) {
            LOG.error("Error while attempting to create ClientSession and Info", e);
        }
    }

    private RadioType getRadioTypeFromOpensyncRadioBand(RadioBandType band) {
        RadioType radioType = null;
        switch (band) {
        case BAND2G:
            radioType = RadioType.is2dot4GHz;
            break;
        case BAND5G:
            radioType = RadioType.is5GHz;
            break;
        case BAND5GU:
            radioType = RadioType.is5GHzU;
            break;
        case BAND5GL:
            radioType = RadioType.is5GHzL;
            break;
        default:
            radioType = RadioType.UNSUPPORTED;
        }
        return radioType;
    }

    private ClientSessionMetricDetails calculateClientSessionMetricDetails(sts.OpensyncStats.Client client,
            long timestamp) {

        LOG.debug("calculateClientSessionMetricDetails for Client {} at timestamp {}", client, timestamp);

        ClientSessionMetricDetails metricDetails = new ClientSessionMetricDetails();
        metricDetails.setRssi(getNegativeSignedIntFromUnsigned(client.getStats().getRssi()));
        metricDetails.setRxBytes(client.getStats().getRxBytes());
        metricDetails.setTxBytes(client.getStats().getTxBytes());
        metricDetails.setTotalTxPackets(client.getStats().getTxFrames());
        metricDetails.setTotalRxPackets(client.getStats().getRxFrames());
        metricDetails.setTxDataFrames((int) ((int) client.getStats().getTxFrames() - client.getStats().getTxRetries()));
        metricDetails.setRxDataFrames((int) ((int) client.getStats().getRxFrames() - client.getStats().getRxRetries()));
        // values reported in Kbps, convert to Mbps
        metricDetails.setRxMbps((float) (client.getStats().getRxRate() / 1000));
        metricDetails.setTxMbps((float) (client.getStats().getTxRate() / 1000));
        // Throughput, do rate / duration
        if (client.getDurationMs() > 0) {
            metricDetails.setRxRateKbps((long) client.getStats().getRxRate() / client.getDurationMs());
            metricDetails.setTxRateKbps((long) client.getStats().getTxRate() / client.getDurationMs());
        } else {
            LOG.info("Cannot calculate tx/rx throughput for Client {} based on duration of {} Ms",
                    client.getMacAddress(), client.getDurationMs());
        }

        return metricDetails;
    }

    private void populateApSsidMetrics(List<ServiceMetric> metricRecordList, Report report, int customerId,
            long equipmentId, String apId, long locationId) {

        LOG.debug("populateApSsidMetrics for Customer {} Equipment {}", customerId, equipmentId);
        ServiceMetric smr = new ServiceMetric(customerId, equipmentId);
        smr.setLocationId(locationId);
        ApSsidMetrics apSsidMetrics = new ApSsidMetrics();

        smr.setDetails(apSsidMetrics);
        metricRecordList.add(smr);
        for (ClientReport clientReport : report.getClientsList()) {

            LOG.debug("ClientReport for channel {} RadioBand {}", clientReport.getChannel(), clientReport.getBand());

            if (smr.getCreatedTimestamp() < clientReport.getTimestampMs()) {
                smr.setCreatedTimestamp(clientReport.getTimestampMs());
            }

            long txBytes = 0L;
            long rxBytes = 0L;
            long txFrames = 0L;
            long rxFrames = 0L;

            int txErrors = 0;
            int rxErrors = 0;

            int txRetries = 0;
            int rxRetries = 0;

            int lastRssi = 0;
            String ssid = null;

            Set<String> clientMacs = new HashSet<>();

            RadioType radioType = getRadioTypeFromOpensyncRadioBand(clientReport.getBand());

            SsidStatistics ssidStatistics = new SsidStatistics();
            // GET the Radio IF MAC (BSSID) from the activeBSSIDs

            Status activeBssidsStatus = statusServiceInterface.getOrNull(customerId, equipmentId,
                    StatusDataType.ACTIVE_BSSIDS);
            ActiveBSSIDs statusDetails = null;
            if (activeBssidsStatus != null) {
                statusDetails = (ActiveBSSIDs) activeBssidsStatus.getDetails();
                for (ActiveBSSID activeBSSID : ((ActiveBSSIDs) activeBssidsStatus.getDetails()).getActiveBSSIDs()) {
                    if (activeBSSID.getRadioType().equals(radioType)) {
                        ssidStatistics.setBssid(new MacAddress(activeBSSID.getBssid()));
                        // ssid value, in case not in stats, else will take
                        // stats value after
                        ssid = activeBSSID.getSsid();
                        statusDetails.getActiveBSSIDs().indexOf(activeBSSID);
                    }
                }
            }
            LOG.debug("Client Report Date is {}", new Date(clientReport.getTimestampMs()));
            int numConnectedClients = 0;
            for (Client client : clientReport.getClientListList()) {
                numConnectedClients += 1;

                if (client.hasSsid() && (client.getSsid() != null) && !client.getSsid().equals("")) {
                    ssid = client.getSsid();
                }

                if (client.hasStats()) {
                    clientMacs.add(client.getMacAddress());
                    sts.OpensyncStats.Client.Stats clientStats = client.getStats();

                    rxBytes += clientStats.getRxBytes();
                    txBytes += clientStats.getTxBytes();
                    txFrames += clientStats.getRxFrames();
                    rxFrames += clientStats.getTxFrames();
                    rxRetries += clientStats.getRxRetries();
                    txRetries += clientStats.getTxRetries();
                    rxErrors += clientStats.getRxErrors();
                    txErrors += clientStats.getTxErrors();

                    lastRssi = client.getStats().getRssi();

                    for (sts.OpensyncStats.Client.TxStats txStats : client.getTxStatsList()) {

                        LOG.debug("txStats {}", txStats);
                    }

                    for (sts.OpensyncStats.Client.RxStats rxStats : client.getRxStatsList()) {

                        LOG.debug("rxStats {}", rxStats);
                    }

                    for (sts.OpensyncStats.Client.TidStats tidStats : client.getTidStatsList()) {

                        LOG.debug("tidStats {}", tidStats);
                    }

                }

                try {
                    if ((ssidStatistics.getBssid() != null) && (ssid != null) && (client.getMacAddress() != null)) {
                        handleClientSessionUpdate(customerId, equipmentId, apId, locationId, clientReport.getChannel(),
                                clientReport.getBand(), clientReport.getTimestampMs(), client, report.getNodeID(),
                                ssidStatistics.getBssid(), ssid);
                    }
                } catch (Exception e) {
                    LOG.debug("Unabled to update client {} session {}", client, e);
                }

            }

            // we can only get Rssi as an unsigned int from opensync, so some
            // shifting
            ssidStatistics.setRxLastRssi(getNegativeSignedIntFromUnsigned(lastRssi));
            ssidStatistics.setNumRxData(Long.valueOf(rxFrames).intValue());
            ssidStatistics.setRxBytes(rxBytes - rxErrors - rxRetries);
            ssidStatistics.setNumTxDataRetries(txRetries);
            ssidStatistics.setNumRcvFrameForTx(txFrames);
            ssidStatistics.setNumTxBytesSucc(txBytes - txErrors - txRetries);
            ssidStatistics.setNumRxRetry(rxRetries);
            ssidStatistics.setNumClient(numConnectedClients);
            ssidStatistics.setSsid(ssid);

            if (radioType != null) {
                List<SsidStatistics> ssidStatsList = apSsidMetrics.getSsidStats().get(radioType);
                if (ssidStatsList == null) {
                    ssidStatsList = new ArrayList<>();
                }
                ssidStatsList.add(ssidStatistics);
                apSsidMetrics.getSsidStats().put(radioType, ssidStatsList);
            }

        }

        LOG.debug("ApSsidMetrics {}", apSsidMetrics);

    }

    int getNegativeSignedIntFromUnsigned(int unsignedValue) {
        int negSignedValue = (unsignedValue << 1) >> 1;
        return negSignedValue;
    }

    int getNegativeSignedIntFrom8BitUnsigned(int unsignedValue) {
        byte b = (byte) Integer.parseInt(Integer.toHexString(unsignedValue), 16);
        return b;
    }

    private void populateChannelInfoReports(List<ServiceMetric> metricRecordList, Report report, int customerId,
            long equipmentId, long locationId) {

        LOG.debug("populateChannelInfoReports for Customer {} Equipment {}", customerId, equipmentId);
        ServiceMetric smr = new ServiceMetric();
        smr.setCustomerId(customerId);
        smr.setEquipmentId(equipmentId);
        smr.setLocationId(locationId);

        ChannelInfoReports channelInfoReports = new ChannelInfoReports();

        smr.setDetails(channelInfoReports);
        metricRecordList.add(smr);

        for (Survey survey : report.getSurveyList()) {

            smr.setCreatedTimestamp(survey.getTimestampMs());
            // message SurveySample {
            // required uint32 channel = 1;
            // optional uint32 duration_ms = 2;
            // optional uint32 total_count = 3;
            // optional uint32 sample_count = 4;
            // optional uint32 busy = 5; /* Busy = Rx + Tx + Interference */
            // optional uint32 busy_tx = 6; /* Tx */
            // optional uint32 busy_rx = 7; /* Rx = Rx_obss + Rx_errr (self and
            // obss errors) */
            // optional uint32 busy_self = 8; /* Rx_self (derived from succesful
            // Rx frames)*/
            // optional uint32 offset_ms = 9;
            // optional uint32 busy_ext = 10; /* 40MHz extention channel busy */
            // }

            RadioType radioType = null;
            if (survey.getBand() == RadioBandType.BAND2G) {
                radioType = RadioType.is2dot4GHz;
            } else if (survey.getBand() == RadioBandType.BAND5G) {
                radioType = RadioType.is5GHz;
            } else if (survey.getBand() == RadioBandType.BAND5GL) {
                radioType = RadioType.is5GHzL;
            } else if (survey.getBand() == RadioBandType.BAND5GU) {
                radioType = RadioType.is5GHzU;
            }

            ChannelBandwidth channelBandwidth = ((ApElementConfiguration) equipmentServiceInterface.get(equipmentId)
                    .getDetails()).getRadioMap().get(radioType).getChannelBandwidth();

            if (survey.getSurveyType().equals(SurveyType.OFF_CHANNEL)
                    || survey.getSurveyType().equals(SurveyType.FULL)) {

                // in this case, we have multiple channels (potentially) and
                // will make
                // ChannelInfo entries per surveyed channel
                Map<Integer, List<SurveySample>> sampleByChannelMap = new HashMap<>();

                survey.getSurveyListList().stream().forEach(new Consumer<SurveySample>() {
                    @Override
                    public void accept(SurveySample s) {
                        List<SurveySample> surveySampleList;
                        if (sampleByChannelMap.get(s.getChannel()) == null) {
                            surveySampleList = new ArrayList<>();
                        } else {
                            surveySampleList = sampleByChannelMap.get(s.getChannel());
                        }
                        surveySampleList.add(s);
                        sampleByChannelMap.put(s.getChannel(), surveySampleList);
                    }
                });

                for (List<SurveySample> surveySampleList : sampleByChannelMap.values()) {
                    ChannelInfo channelInfo = createChannelInfo(equipmentId, radioType, surveySampleList,
                            channelBandwidth);

                    List<ChannelInfo> channelInfoList = channelInfoReports.getRadioInfo(radioType);
                    if (channelInfoList == null) {
                        channelInfoList = new ArrayList<>();
                    }
                    channelInfoList.add(channelInfo);
                    Map<RadioType, List<ChannelInfo>> channelInfoMap = channelInfoReports
                            .getChannelInformationReportsPerRadio();
                    channelInfoMap.put(radioType, channelInfoList);
                    channelInfoReports.setChannelInformationReportsPerRadio(channelInfoMap);
                }

            } else {

                List<SurveySample> surveySampleList = survey.getSurveyListList();

                ChannelInfo channelInfo = createChannelInfo(equipmentId, radioType, surveySampleList, channelBandwidth);
                LOG.debug("ChannelInfo for Survey {}", channelInfo.toPrettyString());
                List<ChannelInfo> channelInfoList = channelInfoReports.getRadioInfo(radioType);
                if (channelInfoList == null) {
                    channelInfoList = new ArrayList<>();
                }
                channelInfoList.add(channelInfo);
                Map<RadioType, List<ChannelInfo>> channelInfoMap = channelInfoReports
                        .getChannelInformationReportsPerRadio();
                channelInfoMap.put(radioType, channelInfoList);
                channelInfoReports.setChannelInformationReportsPerRadio(channelInfoMap);
            }

        }

        LOG.debug("ChannelInfoReports {}", channelInfoReports);

    }

    private ChannelInfo createChannelInfo(long equipmentId, RadioType radioType, List<SurveySample> surveySampleList,
            ChannelBandwidth channelBandwidth) {
        int busyTx = 0; /* Tx */
        int busySelf = 0; /* Rx_self (derived from succesful Rx frames) */
        int busy = 0; /* Busy = Rx + Tx + Interference */
        ChannelInfo channelInfo = new ChannelInfo();

        int[] noiseArray = new int[surveySampleList.size()];
        int index = 0;
        for (SurveySample sample : surveySampleList) {

            busyTx += sample.getBusyTx();
            busySelf += sample.getBusySelf();
            busy += sample.getBusy();
            channelInfo.setChanNumber(sample.getChannel());
            noiseArray[index++] = getNegativeSignedIntFrom8BitUnsigned(sample.getNoise());
        }

        int iBSS = busyTx + busySelf;

        int totalWifi = busy - iBSS;

        channelInfo.setTotalUtilization(busy);
        channelInfo.setWifiUtilization(totalWifi);
        channelInfo.setBandwidth(channelBandwidth);
        if (surveySampleList.size() > 0) {
            channelInfo.setNoiseFloor((int) Math.round(DecibelUtils.getAverageDecibel(noiseArray)));
        }
        return channelInfo;
    }

    @Override
    public void processMqttMessage(String topic, FlowReport flowReport) {

        LOG.info("Received report on topic {}", topic);
        int customerId = extractCustomerIdFromTopic(topic);

        long equipmentId = extractEquipmentIdFromTopic(topic);
        if ((equipmentId <= 0) || (customerId <= 0)) {
            LOG.warn("Cannot determine equipment ids from topic {} - customerId {} equipmentId {}", topic, customerId,
                    equipmentId);
            return;
        }

        String apId = extractApIdFromTopic(topic);

        if (apId == null) {
            LOG.warn("Cannot determine AP id from topic {} - customerId {} equipmentId {} apId {}", topic, customerId,
                    equipmentId, apId);
            return;
        }

    }

    @Override
    public void processMqttMessage(String topic, WCStatsReport wcStatsReport) {
        LOG.debug("Received WCStatsReport {}", wcStatsReport.toString());

        LOG.info("Received report on topic {}", topic);
        int customerId = extractCustomerIdFromTopic(topic);

        long equipmentId = extractEquipmentIdFromTopic(topic);
        if ((equipmentId <= 0) || (customerId <= 0)) {
            LOG.warn("Cannot determine equipment ids from topic {} - customerId {} equipmentId {}", topic, customerId,
                    equipmentId);
            return;
        }

        String apId = extractApIdFromTopic(topic);

        if (apId == null) {
            LOG.warn("Cannot determine AP id from topic {} - customerId {} equipmentId {} apId {}", topic, customerId,
                    equipmentId, apId);
            return;
        }

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

        Status activeBssidsStatus = statusServiceInterface.getOrNull(customerId, equipmentId,
                StatusDataType.ACTIVE_BSSIDS);

        if (activeBssidsStatus != null) {

            ActiveBSSIDs statusDetails = (ActiveBSSIDs) activeBssidsStatus.getDetails();

            for (OpensyncAPVIFState vifState : vifStateTables) {

                LOG.debug("Processing vifState for interface {} on AP {}", vifState.getIfName(), apId);

                String bssid = vifState.getMac();

                if ((bssid == null) || bssid.equals("")) {
                    LOG.warn("BSSID from AP {} interface {} is null or empty", apId, vifState.getIfName());
                    continue;
                }
                String ssid = vifState.getSsid();

                if ((ssid == null) || ssid.equals("")) {
                    LOG.warn("SSID from AP {} interface {} is null or empty", apId, vifState.getIfName());
                    continue;
                }

                int numClients = vifState.getAssociatedClients().size();

                LOG.debug("Values from Vif State Mac (BSSID) {} SSID {} AssociatedClients {}", bssid, ssid,
                        vifState.getAssociatedClients());

                List<ActiveBSSID> bssidList = statusDetails.getActiveBSSIDs();
                for (ActiveBSSID activeBssid : bssidList) {

                    LOG.debug("Checking BSSID {} and SSID {} from BssidList against BSSID {} SSID {}",
                            activeBssid.getBssid(), activeBssid.getSsid(), bssid, ssid);
                    if (activeBssid.getBssid().equals(bssid) && activeBssid.getSsid().equals(ssid)) {
                        LOG.debug("Match BSSID {} and SSID {} from BssidList against BSSID {} SSID {}",
                                activeBssid.getBssid(), activeBssid.getSsid(), bssid, ssid);

                        int idx = bssidList.indexOf(activeBssid);

                        activeBssid.setNumDevicesConnected(numClients);
                        bssidList.set(idx, activeBssid);
                        statusDetails.setActiveBSSIDs(bssidList);
                        activeBssidsStatus.setDetails(statusDetails);

                        break;
                    }

                }

            }

            activeBssidsStatus = statusServiceInterface.update(activeBssidsStatus);

            if (LOG.isDebugEnabled()) {
                LOG.debug("ActiveBSSIDs {}", activeBssidsStatus.toPrettyString());
            }

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

        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing updateClientDetailsStatus. Updated clientConnectionDetails to {}",
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
        Status activeBssidsStatus = null;
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

            activeBssidsStatus = statusServiceInterface.getOrNull(customerId, equipmentId,
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
                LOG.debug("Processing Wifi_Radio_State table update for AP {}, created new ACTIVE_BSSID Status {}",
                        apId, activeBssidsStatus);

            }

            ActiveBSSIDs statusDetails = (ActiveBSSIDs) activeBssidsStatus.getDetails();

            LOG.debug("Processing Wifi_Radio_State table update for AP {}, activeBSSIDs StatusDetails before update {}",
                    apId, statusDetails);

            List<ActiveBSSID> currentActiveBSSIDs = statusDetails.getActiveBSSIDs();
            if (currentActiveBSSIDs == null) {
                currentActiveBSSIDs = new ArrayList<>();
            } else {
                currentActiveBSSIDs = currentActiveBSSIDs.stream().filter(new Predicate<ActiveBSSID>() {
                    @Override
                    public boolean test(ActiveBSSID p) {
                        return !(p.getRadioType().equals(radioState.getFreqBand()));
                    }
                }).collect(Collectors.toList());
                LOG.debug(
                        "Processing Wifi_Radio_State table update for AP {}, activeBSSIDs bssidList without current radio freq {}",
                        apId, currentActiveBSSIDs);
            }

            ProfileContainer profileContainer = new ProfileContainer(
                    profileServiceInterface.getProfileWithChildren(ce.getProfileId()));

            List<Profile> ssidProfiles = profileContainer.getChildrenOfType(ce.getProfileId(), ProfileType.ssid);

            for (Profile ssidProfile : ssidProfiles) {

                SsidConfiguration ssidConfig = (SsidConfiguration) ssidProfile.getDetails();
                if (ssidConfig.getAppliedRadios().contains(radioState.freqBand)) {
                    ActiveBSSID activeBssid = new ActiveBSSID();
                    activeBssid.setBssid(radioState.getMac());
                    activeBssid.setSsid(ssidConfig.getSsid());
                    activeBssid.setRadioType(radioState.getFreqBand());
                    currentActiveBSSIDs.add(activeBssid);
                }

            }

            statusDetails.setActiveBSSIDs(currentActiveBSSIDs);
            activeBssidsStatus.setDetails(statusDetails);

            activeBssidsStatus = statusServiceInterface.update(activeBssidsStatus);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Processing Wifi_Radio_State table update for AP {}, updated ACTIVE_BSSID Status {}", apId,
                        activeBssidsStatus.toPrettyString());
            }

        }

        updateClientDetailsStatus(customerId, equipmentId, (ActiveBSSIDs) activeBssidsStatus.getDetails());

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

        if ((inetStateTables == null) || inetStateTables.isEmpty() || (apId == null)) {
            return;
        }

        for (OpensyncAPInetState inetState : inetStateTables) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Received InetState table update {}", inetState.toPrettyString());
            }
        }

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
                isReassociation = false; // this is a new client
            }
            ClientInfoDetails clientDetails = (ClientInfoDetails) clientInstance.getDetails();

            clientDetails.setAlias("alias " + clientInstance.getMacAddress().getAddressAsLong());
            clientDetails.setApFingerprint("fp " + clientInstance.getMacAddress().getAddressAsString());
            clientDetails.setHostName("hostName-" + clientInstance.getMacAddress().getAddressAsLong());
            clientDetails.setUserName("user-" + clientInstance.getMacAddress().getAddressAsLong());
            clientInstance.setDetails(clientDetails);
            if (isReassociation) {
                clientInstance = clientServiceInterface.update(clientInstance);
            } else {
                clientInstance = clientServiceInterface.create(clientInstance);
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("wifiAssociatedClientsDbTableUpdate client instance {}", clientInstance.toPrettyString());
            }

            ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId,
                    new MacAddress(opensyncWifiAssociatedClients.getMac()));

            if (clientSession == null) {
                LOG.debug("wifiAssociatedClientsDbTableUpdate No session found for Client {}, creating new one.",
                        opensyncWifiAssociatedClients.getMac());
                clientSession = new ClientSession();
                clientSession.setCustomerId(customerId);
                clientSession.setEquipmentId(equipmentId);
                clientSession.setLocationId(ce.getLocationId());
                clientSession.setMacAddress(new MacAddress(opensyncWifiAssociatedClients.getMac()));

                ClientSessionDetails clientSessionDetails = new ClientSessionDetails();
                long sessionId = WiFiSessionUtility.encodeWiFiAssociationId(System.currentTimeMillis() / 1000L,
                        MacAddress.convertMacStringToLongValue(opensyncWifiAssociatedClients.getMac()));
                clientSessionDetails.setSessionId(sessionId);
                LOG.debug("wifiAssociatedClientDbTableUpdate Session Id {}", sessionId);
                clientSession.setDetails(clientSessionDetails);
                clientSession = clientServiceInterface.updateSession(clientSession);
            }

            clientSession.setLocationId(ce.getLocationId());
            ClientSessionDetails clientSessionDetails = clientSession.getDetails();
            clientSessionDetails.setHostname(clientDetails.getHostName());
            clientSessionDetails.setIsReassociation(isReassociation);
            clientSessionDetails.setAssocTimestamp(System.currentTimeMillis());
            clientSessionDetails.setAssociationState(AssociationState._802_11_Associated);
            long sessionId = WiFiSessionUtility.encodeWiFiAssociationId(System.currentTimeMillis() / 1000L,
                    MacAddress.convertMacStringToLongValue(opensyncWifiAssociatedClients.getMac()));
            clientSessionDetails.setPreviousValidSessionId(clientSessionDetails.getSessionId());
            clientSessionDetails.setSessionId(sessionId);
            clientSessionDetails.setApFingerprint(clientDetails.getApFingerprint());
            clientSession.setDetails(clientSessionDetails);
            clientSession = clientServiceInterface.updateSession(clientSession);

            if (LOG.isDebugEnabled()) {
                LOG.debug("wifiAssociatedClientsDbTableUpdate client session {}", clientSession.toPrettyString());
            }

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
        List<ClientSession> clientSessionsToDisconnect = new ArrayList<>();
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
                        for (ClientSession session : clientSessionsForCustomerAndEquipment) {

                            ClientSessionDetails clientSessionDetails = session.getDetails();

                            if (clientSessionDetails.getSsid().equals(vifState.ssid)
                                    && clientSessionDetails.getRadioType().equals(activeBSSID.getRadioType())) {
                                clientSessionDetails.setDisconnectByApTimestamp(System.currentTimeMillis());
                                session.setDetails(clientSessionDetails);
                                clientSessionsToDisconnect.add(session);
                            }

                        }
                    }
                }
            }

        }

        if (!clientSessionsToDisconnect.isEmpty()) {
            List<ClientSession> disconnectedSessionsUpdate = clientServiceInterface
                    .updateSessions(clientSessionsToDisconnect);
            LOG.debug("wifiVIFStateDbTableDelete Disconnect clients {}", disconnectedSessionsUpdate);
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
            clientSessionDetails.setDisconnectByClientTimestamp(System.currentTimeMillis());
            clientSessionDetails.setAssociationState(AssociationState.Disconnected);

            session.setDetails(clientSessionDetails);
            session = clientServiceInterface.updateSession(session);

            if (LOG.isDebugEnabled()) {
                LOG.debug("wifiAssociatedClientsDbTableDelete Updated client session, set to disconnected {}",
                        session.toPrettyString());
            }

        }

    }

    private static int[] toIntArray(List<Integer> values) {
        if (values != null) {
            int returnValue[] = new int[values.size()];
            int index = 0;

            for (Integer value : values) {
                returnValue[index++] = value;
            }

            return returnValue;
        }
        return null;
    }

}
