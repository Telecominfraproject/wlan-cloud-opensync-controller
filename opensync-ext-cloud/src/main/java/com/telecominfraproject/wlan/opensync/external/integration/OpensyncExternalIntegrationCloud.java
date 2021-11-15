
package com.telecominfraproject.wlan.opensync.external.integration;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.telecominfraproject.wlan.alarm.AlarmServiceInterface;
import com.telecominfraproject.wlan.alarm.models.Alarm;
import com.telecominfraproject.wlan.alarm.models.AlarmCode;
import com.telecominfraproject.wlan.alarm.models.AlarmDetails;
import com.telecominfraproject.wlan.client.ClientServiceInterface;
import com.telecominfraproject.wlan.client.info.models.ClientInfoDetails;
import com.telecominfraproject.wlan.client.models.Client;
import com.telecominfraproject.wlan.client.models.ClientType;
import com.telecominfraproject.wlan.client.session.models.AssociationState;
import com.telecominfraproject.wlan.client.session.models.ClientDhcpDetails;
import com.telecominfraproject.wlan.client.session.models.ClientSession;
import com.telecominfraproject.wlan.client.session.models.ClientSessionDetails;
import com.telecominfraproject.wlan.core.model.entity.CountryCode;
import com.telecominfraproject.wlan.core.model.equipment.ChannelBandwidth;
import com.telecominfraproject.wlan.core.model.equipment.EquipmentType;
import com.telecominfraproject.wlan.core.model.equipment.LedStatus;
import com.telecominfraproject.wlan.core.model.equipment.MacAddress;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.core.model.equipment.WiFiSessionUtility;
import com.telecominfraproject.wlan.core.model.pagination.PaginationContext;
import com.telecominfraproject.wlan.core.model.pagination.PaginationResponse;
import com.telecominfraproject.wlan.customer.models.Customer;
import com.telecominfraproject.wlan.customer.models.EquipmentAutoProvisioningSettings;
import com.telecominfraproject.wlan.customer.service.CustomerServiceInterface;
import com.telecominfraproject.wlan.datastore.exceptions.DsConcurrentModificationException;
import com.telecominfraproject.wlan.equipment.EquipmentServiceInterface;
import com.telecominfraproject.wlan.equipment.models.ApElementConfiguration;
import com.telecominfraproject.wlan.equipment.models.ChannelPowerLevel;
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
import com.telecominfraproject.wlan.opensync.external.integration.utils.StatsPublisherInterface;
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
import com.telecominfraproject.wlan.status.equipment.models.EquipmentChannelStatusData;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentLANStatusData;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentManufacturerDataStatus;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentManufacturerQrCode;
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
import com.telecominfraproject.wlan.status.equipment.report.models.WiredEthernetPortStatusData;
import com.telecominfraproject.wlan.status.equipment.report.models.WiredPortStatus;
import com.telecominfraproject.wlan.status.models.Status;
import com.telecominfraproject.wlan.status.models.StatusCode;
import com.telecominfraproject.wlan.status.models.StatusDataType;
import com.telecominfraproject.wlan.status.network.models.NetworkAdminStatusData;
import com.telecominfraproject.wlan.systemevent.equipment.realtime.ApcElectionEvent;
import com.telecominfraproject.wlan.systemevent.equipment.realtime.ApcElectionEvent.ApcMode;
import com.telecominfraproject.wlan.systemevent.equipment.realtime.RealTimeEventType;

import sts.OpensyncStats.Report;

@org.springframework.context.annotation.Profile("opensync_cloud_config")
@Component
public class OpensyncExternalIntegrationCloud implements OpensyncExternalIntegrationInterface {

    protected static final String standard_linux_date_format = "EEE MMM dd HH:mm:ss zzz yyyy";
    private static final String VLAN_TRUNK_IF_TYPE = "vlan_trunk";
    private static final String ALLOWED_VLANS = "allowed_vlans";
    private static final String NATIVE_VLAN_ID = "pvid";
    private static final String SPACE_SEPERATOR = " ";

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
    private StatsPublisherInterface statsPublisherInterface;
    @Autowired
    private AlarmServiceInterface alarmServiceInterface;

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
    @Value("${tip.wlan.ovsdb.wifi-iface.default_lan_type:bridge}")
    public String defaultLanInterfaceType;
    @Value("${tip.wlan.ovsdb.wifi-iface.default_lan_name:lan}")
    public String defaultLanInterfaceName;
    @Value("${tip.wlan.ovsdb.wifi-iface.default_wan_type:bridge}")
    public String defaultWanInterfaceType;
    @Value("${tip.wlan.ovsdb.wifi-iface.default_wan_name:wan}")
    public String defaultWanInterfaceName;
    @Value("${tip.wlan.ovsdb.wifi-iface.default_wan6_name:wan6}")
    public String defaultWan6InterfaceName;

    @Value("${tip.wlan.ovsdb.syncUpRadioConfigsForProvisionedEquipment:true}")
    private boolean syncUpRadioConfigsForProvisionedEquipment;

    @Override
    public void apConnected(String apId, ConnectNodeInfo connectNodeInfo) {

        Equipment ce = equipmentServiceInterface.getByInventoryIdOrNull(apId);

        try {
            if (ce == null) {

                Customer customer = customerServiceInterface.getOrNull(autoProvisionedCustomerId);
                if (customer == null) {
                    LOG.error("Cannot auto-provision equipment because customer with id {} is not found", autoProvisionedCustomerId);
                    throw new IllegalStateException("Cannot auto-provision equipment because customer is not found : " + autoProvisionedCustomerId);
                }

                if ((customer.getDetails() != null) && (customer.getDetails().getAutoProvisioning() != null)
                        && !customer.getDetails().getAutoProvisioning().isEnabled()) {
                    LOG.error("Cannot auto-provision equipment because customer with id {} explicitly turned that feature off", autoProvisionedCustomerId);
                    throw new IllegalStateException(
                            "Cannot auto-provision equipment because customer explicitly turned that feature off : " + autoProvisionedCustomerId);
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
                    Location location = locationServiceInterface.getOrNull(locationId);
                    if (location != null)
                        ce.setLocationId(location.getId());
                } catch (Exception e) {
                    LOG.error("Cannot auto-provision equipment because customer location with id {} cannot be found", locationId);
                    throw new IllegalStateException("Cannot auto-provision equipment because customer location cannot be found : " + locationId);
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
                    profileId = customer.getDetails().getAutoProvisioning().getEquipmentProfileIdPerModel().get(ce.getDetails().getEquipmentModel());
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
                boolean needToUpdateEquipment = false;
                MacAddress reportedMacAddress = null;
                alarmServiceInterface.delete(ce.getCustomerId(), ce.getId());
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
                        ce.setBaseMacAddress(reportedMacAddress);
                        needToUpdateEquipment = true;
                    }
                }

                if (syncUpRadioConfigsForProvisionedEquipment) {

                    // sync up available radios reported by AP with the
                    // ApElementConfiguration, update equipment in DB if needed
                    ApElementConfiguration apElementConfig = (ApElementConfiguration) ce.getDetails();
                    if (apElementConfig == null) {
                        apElementConfig = ApElementConfiguration.createWithDefaults();
                        needToUpdateEquipment = true;
                    }

                    if (apElementConfig.getDeviceName() == null || !apElementConfig.getDeviceName().equals(ce.getName())) {
                        apElementConfig.setDeviceName(ce.getName());
                        needToUpdateEquipment = true;
                    }

                    if (apElementConfig.getEquipmentModel() == null || !apElementConfig.getEquipmentModel().equals(connectNodeInfo.model)) {
                        apElementConfig.setEquipmentModel(connectNodeInfo.model);
                        needToUpdateEquipment = true;
                    }

                    Map<RadioType, RadioConfiguration> advancedRadioMap = apElementConfig.getAdvancedRadioMap();
                    Map<RadioType, ElementRadioConfiguration> radioMap = apElementConfig.getRadioMap();

                    if (apElementConfig.getAdvancedRadioMap() == null) {
                        advancedRadioMap = new EnumMap<>(RadioType.class);
                        apElementConfig.setAdvancedRadioMap(advancedRadioMap);
                        needToUpdateEquipment = true;
                    }

                    if (radioMap == null) {
                        radioMap = new EnumMap<>(RadioType.class);
                        apElementConfig.setRadioMap(radioMap);
                        needToUpdateEquipment = true;
                    }

                    Set<RadioType> radiosFromAp = new HashSet<>();
                    connectNodeInfo.wifiRadioStates.keySet().forEach(k -> {
                        radiosFromAp.add(OvsdbToWlanCloudTypeMappingUtility.getRadioTypeForOvsdbRadioFreqBand(k));
                    });

                    // add missing radio configs from the AP into the DB
                    for (RadioType radio : radiosFromAp) {
                        if (!advancedRadioMap.containsKey(radio) || advancedRadioMap.get(radio) == null) {
                            advancedRadioMap.put(radio, RadioConfiguration.createWithDefaults(radio));
                            needToUpdateEquipment = true;
                        }
                        if (!radioMap.containsKey(radio) || radioMap.get(radio) == null) {
                            radioMap.putIfAbsent(radio, ElementRadioConfiguration.createWithDefaults(radio));
                            needToUpdateEquipment = true;
                        }
                    }

                    // remove radio configs from the DB that are no longer
                    // present in the AP but still exist in DB

                    for (RadioType radio : new ArrayList<>(advancedRadioMap.keySet())) {
                        if (!radiosFromAp.contains(radio)) {
                            advancedRadioMap.remove(radio);
                            needToUpdateEquipment = true;
                        }
                    }
                    for (RadioType radio : new ArrayList<>(radioMap.keySet())) {
                        if (!radiosFromAp.contains(radio)) {
                            radioMap.remove(radio);
                            needToUpdateEquipment = true;
                        }
                    }
                    apElementConfig.setAdvancedRadioMap(advancedRadioMap);
                    apElementConfig.setRadioMap(radioMap);
                    ce.setDetails(apElementConfig);
                }
                if (needToUpdateEquipment) {
                    ce = equipmentServiceInterface.update(ce);
                    LOG.info("Updated Equipment {}", ce);
                }

            }

            EquipmentRoutingRecord equipmentRoutingRecord = gatewayController.registerCustomerEquipment(ce.getName(), ce.getCustomerId(), ce.getId());

            // Status and client cleanup, when AP reconnects or has been
            // disconnected, reset statuses, clients set to disconnected as
            // SSIDs etc will be reconfigured on AP
            LOG.info("Clear existing status {} for AP {}", statusServiceInterface.delete(ce.getCustomerId(), ce.getId()), apId);
            LOG.info("Set pre-existing client sessions to disconnected for AP {}", apId);
            disconnectClients(ce);

            updateApStatus(ce, connectNodeInfo);

            removeNonWifiClients(ce, connectNodeInfo);

            OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);
            ovsdbSession.setRoutingId(equipmentRoutingRecord.getId());
            ovsdbSession.setEquipmentId(ce.getId());

            LOG.debug("Equipment {}", ce);
            LOG.info("AP {} got connected to the gateway", apId);
            LOG.info("ConnectNodeInfo {}", connectNodeInfo);

            if (connectNodeInfo.versionMatrix.containsKey(OvsdbStringConstants.FW_IMAGE_ACTIVE_KEY) && connectNodeInfo.versionMatrix.containsKey("DATE")) {
                // The AP uses standard linux date format. So the format would be:
                // root@OpenAp-0498b5:~# date
                // Tue Aug 3 14:55:28 UTC 2021
                DateFormat dateFormat = new SimpleDateFormat(standard_linux_date_format, Locale.ENGLISH);
                String dateString = connectNodeInfo.versionMatrix.get("DATE").strip();
                try {
                    Date date = dateFormat.parse(dateString);
                    reconcileFwVersionToTrack(ce, connectNodeInfo.versionMatrix.get(OvsdbStringConstants.FW_IMAGE_ACTIVE_KEY),
                            connectNodeInfo.versionMatrix.get(OvsdbStringConstants.FW_IMAGE_INACTIVE_KEY), date.getTime(), connectNodeInfo.model,
                            connectNodeInfo.firmwareVersion);
                } catch (java.text.ParseException p) {
                    LOG.info("Could not parse release date {} from AP fw, set date to EPOCH start value.", dateString, Instant.EPOCH);
                    reconcileFwVersionToTrack(ce, connectNodeInfo.versionMatrix.get(OvsdbStringConstants.FW_IMAGE_ACTIVE_KEY),
                            connectNodeInfo.versionMatrix.get(OvsdbStringConstants.FW_IMAGE_INACTIVE_KEY), Instant.EPOCH.getLong(ChronoField.INSTANT_SECONDS), connectNodeInfo.model,
                            connectNodeInfo.firmwareVersion);
                }
            } else {
                LOG.info("Cloud based firmware upgrade is not supported for this AP");
            }

        } catch (Exception e) {
            LOG.error("Could not process connection from AP {}", apId, e);
            throw new RuntimeException(e);
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

            ssidConfig.setSsid(autoProvisionedSsid);
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
            Status statusRecord = statusServiceInterface.getOrNull(ce.getCustomerId(), ce.getId(), StatusDataType.EQUIPMENT_ADMIN);
            if (statusRecord == null) {
                statusRecord = new Status();
                statusRecord.setCustomerId(ce.getCustomerId());
                statusRecord.setEquipmentId(ce.getId());
                EquipmentAdminStatusData statusData = new EquipmentAdminStatusData();
                statusRecord.setDetails(statusData);
            }

            ((EquipmentAdminStatusData) statusRecord.getDetails()).setStatusCode(StatusCode.normal);
            if (((EquipmentAdminStatusData) statusRecord.getDetails()).getLedStatus() == null) {
                ((EquipmentAdminStatusData) statusRecord.getDetails()).setLedStatus(LedStatus.UNKNOWN);
            }
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
            CountryCode countryCode = Location.getCountryCode(locationServiceInterface.getOrNull(ce.getLocationId()));
            if (countryCode != null)
                protocolStatusData.setCountryCode(countryCode.getName());
            if (connectNodeInfo.country != null) {
                protocolStatusData.setReportedCC(CountryCode.getByName(connectNodeInfo.country));
            }
            protocolStatusData.setReportedHwVersion(connectNodeInfo.platformVersion);
            if (connectNodeInfo.versionMatrix.containsKey(OvsdbStringConstants.FW_IMAGE_ACTIVE_KEY)) {
                protocolStatusData.setReportedSwVersion(connectNodeInfo.versionMatrix.get(OvsdbStringConstants.FW_IMAGE_ACTIVE_KEY));
            } else if (connectNodeInfo.versionMatrix.containsKey(OvsdbStringConstants.FW_IMAGE_NAME_KEY)) {
                protocolStatusData.setReportedSwVersion(connectNodeInfo.versionMatrix.get(OvsdbStringConstants.FW_IMAGE_NAME_KEY));
            } else {
                protocolStatusData.setReportedSwVersion("Unknown");
            }
            if (connectNodeInfo.versionMatrix.containsKey(OvsdbStringConstants.FW_IMAGE_INACTIVE_KEY)) {
                protocolStatusData.setReportedSwAltVersion(connectNodeInfo.versionMatrix.get(OvsdbStringConstants.FW_IMAGE_INACTIVE_KEY));
            } else if (connectNodeInfo.versionMatrix.containsKey(OvsdbStringConstants.FW_IMAGE_NAME_KEY)) {
                protocolStatusData.setReportedSwVersion(connectNodeInfo.versionMatrix.get(OvsdbStringConstants.FW_IMAGE_NAME_KEY));
            } else {
                protocolStatusData.setReportedSwVersion("Unknown");
            }
            try {
                if (connectNodeInfo.ipV4Address != null) {
                    protocolStatusData.setReportedIpV4Addr(InetAddress.getByName(connectNodeInfo.ipV4Address));
                }
            } catch (UnknownHostException e) {
                LOG.error("Could not set IpV4Addr {} on AP {} due to UnknownHostException ", connectNodeInfo.ipV4Address, ce.getName(), e);
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

            statusRecord = statusServiceInterface.getOrNull(ce.getCustomerId(), ce.getId(), StatusDataType.EQUIPMENT_MANUFACTURER_DATA);
            if (statusRecord == null) {
                statusRecord = new Status();
                statusRecord.setCustomerId(ce.getCustomerId());
                statusRecord.setEquipmentId(ce.getId());
                statusRecord.setStatusDataType(StatusDataType.EQUIPMENT_MANUFACTURER_DATA);
                EquipmentManufacturerDataStatus statusData = new EquipmentManufacturerDataStatus();
                statusRecord.setDetails(statusData);
            }

            EquipmentManufacturerQrCode qrCode = new EquipmentManufacturerQrCode();

            if (connectNodeInfo.qrCode != null) {
                if (connectNodeInfo.qrCode.get("DT") != null) {
                    qrCode.setDeviceType(EquipmentType.getByName(connectNodeInfo.qrCode.get("DT")));
                }
                if (connectNodeInfo.qrCode.get("VN") != null) {
                    qrCode.setVendorName(connectNodeInfo.qrCode.get("VN").toUpperCase());
                }
                if (isValidMACAddress(connectNodeInfo.qrCode.get("DM"))) {
                    qrCode.setDeviceMac(MacAddress.valueOf(connectNodeInfo.qrCode.get("DM")));
                }
                qrCode.setHwRevision(connectNodeInfo.qrCode.get("HW"));
                qrCode.setModelName(connectNodeInfo.qrCode.get("MN"));
                qrCode.setSerialNumber(connectNodeInfo.qrCode.get("SN"));
            }

            ((EquipmentManufacturerDataStatus) statusRecord.getDetails()).setSkuNumber(connectNodeInfo.skuNumber);
            ((EquipmentManufacturerDataStatus) statusRecord.getDetails()).setModel(connectNodeInfo.model);
            ((EquipmentManufacturerDataStatus) statusRecord.getDetails()).setRevision(connectNodeInfo.revision);
            ((EquipmentManufacturerDataStatus) statusRecord.getDetails()).setSerialNumber(connectNodeInfo.serialNumber);
            ((EquipmentManufacturerDataStatus) statusRecord.getDetails()).setQrCode(qrCode);
            if (connectNodeInfo.manufacturerName != null) {
                ((EquipmentManufacturerDataStatus) statusRecord.getDetails()).setManufacturerName(connectNodeInfo.manufacturerName);
            }
            if (connectNodeInfo.manufacturerDate != null) {
                ((EquipmentManufacturerDataStatus) statusRecord.getDetails()).setManufacturerDate(connectNodeInfo.manufacturerDate);
            }
            if (connectNodeInfo.manufacturerUrl != null) {
                ((EquipmentManufacturerDataStatus) statusRecord.getDetails()).setManufacturerUrl(connectNodeInfo.manufacturerUrl);
            }
            if (connectNodeInfo.modelDescription != null) {
                ((EquipmentManufacturerDataStatus) statusRecord.getDetails()).setModelDescription(connectNodeInfo.modelDescription);
            }
            if (connectNodeInfo.referenceDesign != null) {
                ((EquipmentManufacturerDataStatus) statusRecord.getDetails()).setReferenceDesign(connectNodeInfo.referenceDesign);
            }
            if (connectNodeInfo.certificationRegion != null && !connectNodeInfo.certificationRegion.equalsIgnoreCase("unknown")) {
                ((EquipmentManufacturerDataStatus) statusRecord.getDetails())
                        .setCertificationRegion(CountryCode.getByName(connectNodeInfo.certificationRegion.toUpperCase()));
            }
            if (isValidMACAddress(connectNodeInfo.macAddress)) {
                ((EquipmentManufacturerDataStatus) statusRecord.getDetails()).setMacAddress(MacAddress.valueOf(connectNodeInfo.macAddress));
            }
            Status manufacturerStatus = statusServiceInterface.update(statusRecord);
            LOG.debug("EQUIPMENT_MANUFACTURER_DATA for AP {} updated to {}", ce.getName(), manufacturerStatus);

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
                fwUpgradeStatusData.setActiveSwVersion(connectNodeInfo.versionMatrix.get(OvsdbStringConstants.FW_IMAGE_ACTIVE_KEY));
            } else if (connectNodeInfo.versionMatrix.containsKey(OvsdbStringConstants.FW_IMAGE_NAME_KEY)) {
                fwUpgradeStatusData.setActiveSwVersion(connectNodeInfo.versionMatrix.get(OvsdbStringConstants.FW_IMAGE_NAME_KEY));
            } else {
                fwUpgradeStatusData.setActiveSwVersion("Unknown");
            }
            if (connectNodeInfo.versionMatrix.containsKey(OvsdbStringConstants.FW_IMAGE_INACTIVE_KEY)) {
                fwUpgradeStatusData.setAlternateSwVersion(connectNodeInfo.versionMatrix.get(OvsdbStringConstants.FW_IMAGE_INACTIVE_KEY));
            } else if (connectNodeInfo.versionMatrix.containsKey(OvsdbStringConstants.FW_IMAGE_NAME_KEY)) {
                fwUpgradeStatusData.setAlternateSwVersion(connectNodeInfo.versionMatrix.get(OvsdbStringConstants.FW_IMAGE_NAME_KEY));
            } else {
                fwUpgradeStatusData.setAlternateSwVersion("Unknown");
            }

            if (fwUpgradeStatusData.getUpgradeState() == null) {
                fwUpgradeStatusData.setUpgradeState(EquipmentUpgradeState.undefined);
                fwUpgradeStatusData.setUpgradeStartTime(null);

            }

            statusRecord.setDetails(fwUpgradeStatusData);
            statusServiceInterface.update(statusRecord);

            Status networkAdminStatusRec = statusServiceInterface.getOrNull(ce.getCustomerId(), ce.getId(), StatusDataType.NETWORK_ADMIN);
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
            ClientSession clientSession = clientServiceInterface.getSessionOrNull(ce.getCustomerId(), ce.getId(), ce.getBaseMacAddress());
            if (clientSession != null) {
                clientSession = clientServiceInterface.deleteSession(ce.getCustomerId(), ce.getId(), client.getMacAddress());
                LOG.info("Removed invalid client session {}", clientSession);
            }
            if (clientServiceInterface.getOrNull(ce.getCustomerId(), client.getMacAddress()) != null) {
                LOG.info("Deleting invalid client {}", clientServiceInterface.delete(ce.getCustomerId(), client.getMacAddress()));
            }
        } else {
            LOG.info("No clients with MAC address {} registered for customer {}", ce.getBaseMacAddress(), ce.getCustomerId());
        }

        LOG.info("Finished checking for and removing non-wifi client types for Equipment {}", ce);

    }

    void reconcileFwVersionToTrack(Equipment ce, String activeFirmwareImageAp, String inactiveFirmwareImageAp, Long activeFwReleaseDate, String model,
            String firmwareVersion) {

        LOG.debug("reconcileFwVersionToTrack for AP {} with active firmware version {} model {}", ce.getInventoryId(), activeFirmwareImageAp, model);
        Status statusRecord = statusServiceInterface.getOrNull(autoProvisionedCustomerId, autoProvisionedCustomerId, StatusDataType.FIRMWARE);
        if (statusRecord == null) {
            statusRecord = new Status();
            statusRecord.setCreatedTimestamp(System.currentTimeMillis());
            statusRecord.setCustomerId(ce.getCustomerId());
            statusRecord.setEquipmentId(ce.getId());
            statusRecord.setStatusDataType(StatusDataType.FIRMWARE);
            statusRecord.setDetails(new EquipmentUpgradeStatusData());
            ((EquipmentUpgradeStatusData) statusRecord.getDetails()).setActiveSwVersion(activeFirmwareImageAp);
            if (inactiveFirmwareImageAp != null) {
                ((EquipmentUpgradeStatusData) statusRecord.getDetails()).setActiveSwVersion(inactiveFirmwareImageAp);
            }
        }
        EquipmentUpgradeStatusData fwUpgradeStatusData = (EquipmentUpgradeStatusData) statusRecord.getDetails();
        fwUpgradeStatusData.setActiveSwVersion(firmwareVersion);
        fwUpgradeStatusData.setAlternateSwVersion(inactiveFirmwareImageAp);

        // default track settings for firmware
        CustomerFirmwareTrackSettings trackSettings = firmwareServiceInterface.getDefaultCustomerTrackSetting();

        // check for updated/modified track settings for this customer
        CustomerFirmwareTrackRecord custFwTrackRecord = firmwareServiceInterface.getCustomerFirmwareTrackRecord(ce.getCustomerId());
        if (custFwTrackRecord != null)
            trackSettings = custFwTrackRecord.getSettings();

        // determine if AP requires FW upgrade before cloud
        // connection/provision
        if (trackSettings.getAutoUpgradeDeprecatedOnBind().equals(TrackFlag.ALWAYS) || trackSettings.getAutoUpgradeUnknownOnBind().equals(TrackFlag.ALWAYS)) {
            LOG.debug("reconcileFwVersionToTrack for AP {} track flag for auto-upgrade {}", ce.getInventoryId(),
                    trackSettings.getAutoUpgradeDeprecatedOnBind());
            // check the reported fw version for the AP, if it is < than
            // the default version for the cloud, then download and
            // flash the firmware before proceeding.
            // then return;
            FirmwareTrackRecord fwTrackRecord = null;
            if (custFwTrackRecord == null) {
                // take the default
                fwTrackRecord = firmwareServiceInterface.getFirmwareTrackByName(FirmwareTrackRecord.DEFAULT_TRACK_NAME);
            } else {
                // there must be a customer one
                fwTrackRecord = firmwareServiceInterface.getFirmwareTrackById(custFwTrackRecord.getTrackRecordId());
            }
            if (fwTrackRecord != null) {
                LOG.debug("reconcileFwVersionToTrack for AP {} firmwareTrackRecord {}", ce.getInventoryId(), fwTrackRecord);
                Optional<FirmwareTrackAssignmentDetails> assignmentDetails = firmwareServiceInterface.getFirmwareTrackAssignments(fwTrackRecord.getTrackName())
                        .stream().filter(new Predicate<FirmwareTrackAssignmentDetails>() {
                            @Override
                            public boolean test(FirmwareTrackAssignmentDetails t) {
                                // AP may report type as UPPER case
                                return model.equalsIgnoreCase(t.getModelId());
                            }
                        }).findFirst();
                if (assignmentDetails.isPresent()) {
                    FirmwareTrackAssignmentDetails targetFirmwareForTrack = assignmentDetails.get();
                    if (activeFwReleaseDate == null) {
                        LOG.info("Active FW release date is unknown, firmware upgrade required.");
                        fwUpgradeStatusData.setTargetSwVersion(targetFirmwareForTrack.getVersionName());
                        fwUpgradeStatusData.setUpgradeState(EquipmentUpgradeState.out_of_date);
                        statusRecord.setDetails(fwUpgradeStatusData);
                        statusRecord = statusServiceInterface.update(statusRecord);
                        triggerFwDownload(ce, fwUpgradeStatusData, trackSettings);
                    } else {
                        Date activeReleaseDate = new Date(activeFwReleaseDate);
                        Date targetReleaseDate = new Date(targetFirmwareForTrack.getReleaseDate());
                        if (activeReleaseDate.after(targetReleaseDate) || activeReleaseDate.equals(targetReleaseDate)) {
                            LOG.info("Active FW release date {} is more recent than or equal to the target firmware release date {}, no upgrade required.",
                                    activeReleaseDate, targetReleaseDate);
                            fwUpgradeStatusData.setUpgradeState(EquipmentUpgradeState.up_to_date);
                            fwUpgradeStatusData.setTargetSwVersion(targetFirmwareForTrack.getVersionName());
                            statusRecord.setDetails(fwUpgradeStatusData);
                            statusRecord = statusServiceInterface.update(statusRecord);
                        } else {
                            LOG.info("Active FW release date {} is earlier than target firmware release date {}, firmware upgrade required.", activeReleaseDate,
                                    targetReleaseDate);
                            fwUpgradeStatusData.setTargetSwVersion(targetFirmwareForTrack.getVersionName());
                            fwUpgradeStatusData.setUpgradeState(EquipmentUpgradeState.out_of_date);
                            statusRecord.setDetails(fwUpgradeStatusData);
                            statusRecord = statusServiceInterface.update(statusRecord);
                            triggerFwDownload(ce, fwUpgradeStatusData, trackSettings);
                        }
                    }
                } else {
                    LOG.info("No firmware assignment present in track for AP model {}. Auto-upgrade of firmware is not possible for this node.", model);
                }
            }
        } else {
            LOG.info("Automatic firmware upgrade is not configured for track {}", trackSettings);
        }
    }

    private void triggerFwDownload(Equipment ce, EquipmentUpgradeStatusData fwUpgradeStatusData, CustomerFirmwareTrackSettings trackSettings) {
        LOG.debug("triggerFwDownloadAndFlash Automatic firmware upgrade is configured for track {}.", trackSettings);

        try {
            FirmwareVersion fwVersion = firmwareServiceInterface.getFirmwareVersionByName(fwUpgradeStatusData.getTargetSwVersion());

            if (fwVersion != null) {
                OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(ce.getInventoryId());
                if (ovsdbSession == null) {
                    throw new IllegalStateException("AP is not connected " + ce.getInventoryId());
                }

                CEGWFirmwareDownloadRequest fwDownloadRequest =
                        new CEGWFirmwareDownloadRequest(ce.getInventoryId(), ce.getId(), fwVersion.getVersionName(), fwVersion.getFilename());
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
                        LOG.warn("Unable to delete routing service Id {} for ap {}. {}", ovsdbSession.getRoutingId(), apId, e);
                    }
                }
            } else {
                LOG.warn("Cannot find ap {} in inventory", apId);
            }
            Equipment ce = equipmentServiceInterface.getByInventoryIdOrNull(apId);
            if (ce != null) {
                List<Status> deletedStatuses = statusServiceInterface.deleteOnEquipmentDisconnect(ce.getCustomerId(), ce.getId());
                LOG.info("AP {} disconnected, deleted status records {}", apId, deletedStatuses);
                updateApDisconnectedStatus(apId, ce);
                disconnectClients(ce);
            } else {
                LOG.error("updateDisconnectedApStatus::Cannot get Equipment for AP {} to update the EquipmentAdminStatus", apId);

            }
        } catch (Exception e) {
            LOG.error("Exception when registering ap routing {}", apId, e);
        }

    }

    private void updateApDisconnectedStatus(String apId, Equipment ce) {
        LOG.info("updateApDisconnectedStatus disconnected AP {}", apId);
        try {
            Alarm disconnectedAlarm = new Alarm();
            disconnectedAlarm.setCustomerId(ce.getCustomerId());
            disconnectedAlarm.setEquipmentId(ce.getId());
            disconnectedAlarm.setAlarmCode(AlarmCode.Disconnected);

            AlarmDetails alarmDetails = new AlarmDetails();
            alarmDetails.setMessage(AlarmCode.Disconnected.getDescription());
            alarmDetails.setAffectedEquipmentIds(List.of(ce.getId()));
            alarmDetails.setGeneratedBy(apId);

            disconnectedAlarm.setDetails(alarmDetails);

            alarmServiceInterface.create(disconnectedAlarm);

            Status statusRecord = new Status();
            statusRecord.setCustomerId(ce.getCustomerId());
            statusRecord.setEquipmentId(ce.getId());
            statusRecord.setStatusDataType(StatusDataType.EQUIPMENT_ADMIN);

            EquipmentAdminStatusData statusData = new EquipmentAdminStatusData();
            statusData.setStatusMessage("AP Disconnected");
            statusData.setStatusCode(StatusCode.disabled);

            statusRecord.setDetails(statusData);

            LOG.info("Updated EquipmentAdminStatus {} for disconnected AP {}", statusServiceInterface.update(statusRecord), apId);

        } catch (Exception e) {
            LOG.error("Exception in updateApDisconnectedStatus", e);
            throw e;
        }
    }

    private void disconnectClients(Equipment ce) {

        LOG.info("OpensyncExternalIntegrationCloud::disconnectClients for Equipment {}", ce);
        PaginationResponse<ClientSession> clientSessions = clientServiceInterface.getSessionsForCustomer(ce.getCustomerId(), Set.of(ce.getId()),
                Set.of(ce.getLocationId()), null, null, new PaginationContext<ClientSession>(100));

        if (clientSessions == null) {
            LOG.info("There are no existing client sessions to disconnect.");
            return;
        }

        List<ClientSession> toBeDisconnected = new ArrayList<>();

        clientSessions.getItems().stream().forEach(c -> {
            if (c.getDetails().getAssociationState() != null && !c.getDetails().getAssociationState().equals(AssociationState.Disconnected)) {
                LOG.info("Change association state for client {} from {} to {}", c.getMacAddress(), c.getDetails().getAssociationState(),
                        AssociationState.Disconnected);

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
            Equipment equipmentConfig = equipmentServiceInterface.getByInventoryIdOrNull(apId);
            if (equipmentConfig == null) {
                throw new IllegalStateException("Cannot retrieve configuration for " + apId);
            }
            int customerId = equipmentConfig.getCustomerId();

            ret = new OpensyncAPConfig();

            ret.setCustomerEquipment(equipmentConfig);

            Location eqLocation = locationServiceInterface.get(equipmentConfig.getLocationId());

            ret.setEquipmentLocation(eqLocation);

            ProfileContainer profileContainer = new ProfileContainer(profileServiceInterface.getProfileWithChildren(equipmentConfig.getProfileId()));

            ret.setApProfile(profileContainer.getOrNull(equipmentConfig.getProfileId()));

            ret.setRfProfile(profileContainer.getChildOfTypeOrNull(equipmentConfig.getProfileId(), ProfileType.rf));

            ret.setSsidProfile(profileContainer.getChildrenOfType(equipmentConfig.getProfileId(), ProfileType.ssid));
            
			ret.setWiredEthernetPortProfile(
					profileContainer.getChildOfTypeOrNullByEquipmentModel(equipmentConfig.getProfileId(),
							ProfileType.wired_ethernet_port, equipmentConfig.getDetails().getEquipmentModel()));

            ret.setMetricsProfiles(profileContainer.getChildrenOfType(equipmentConfig.getProfileId(), ProfileType.service_metrics_collection_config));

            Set<Profile> radiusSet = new HashSet<>();
            Set<Long> captiveProfileIds = new HashSet<>();
            Set<Long> bonjourGatewayProfileIds = new HashSet<>();

            OpensyncAPHotspot20Config hotspotConfig = new OpensyncAPHotspot20Config();

            Set<Profile> hotspot20ProfileSet = new HashSet<>();
            Set<Profile> hotspot20OperatorSet = new HashSet<>();
            Set<Profile> hotspot20VenueSet = new HashSet<>();
            Set<Profile> hotspot20ProviderSet = new HashSet<>();

            for (Profile ssidProfile : ret.getSsidProfile()) {

                hotspot20ProfileSet.addAll(profileContainer.getChildrenOfType(ssidProfile.getId(), ProfileType.passpoint));

                radiusSet.addAll(profileContainer.getChildrenOfType(ssidProfile.getId(), ProfileType.radius));
                if (ssidProfile.getDetails() != null) {
                    Long captivePortId = ((SsidConfiguration) ssidProfile.getDetails()).getCaptivePortalId();
                    if (captivePortId != null) {
                        captiveProfileIds.add(captivePortId);

                    }
                    Long bonjourGatewayProfileId = ((SsidConfiguration) ssidProfile.getDetails()).getBonjourGatewayProfileId();
                    if (bonjourGatewayProfileId != null) {
                        bonjourGatewayProfileIds.add(bonjourGatewayProfileId);
                    }
                }
            }

            if (hotspot20ProfileSet.size() > 0) {
                for (Profile hotspot20Profile : hotspot20ProfileSet) {
                    hotspot20OperatorSet.addAll(profileContainer.getChildrenOfType(hotspot20Profile.getId(), ProfileType.passpoint_operator));
                    hotspot20VenueSet.addAll(profileContainer.getChildrenOfType(hotspot20Profile.getId(), ProfileType.passpoint_venue));
                    hotspot20ProviderSet.addAll(profileContainer.getChildrenOfType(hotspot20Profile.getId(), ProfileType.passpoint_osu_id_provider));
                }
                hotspotConfig.setHotspot20OperatorSet(hotspot20OperatorSet);
                hotspotConfig.setHotspot20ProfileSet(hotspot20ProfileSet);
                hotspotConfig.setHotspot20ProviderSet(hotspot20ProviderSet);
                hotspotConfig.setHotspot20VenueSet(hotspot20VenueSet);

                ret.setHotspotConfig(hotspotConfig);
            }

            ret.setCaptiveProfiles(profileServiceInterface.get(captiveProfileIds));
            for (Profile captivePortal : ret.getCaptiveProfiles()) {
                radiusSet.addAll(profileContainer.getChildrenOfType(captivePortal.getId(), ProfileType.radius));
            }
            ret.setRadiusProfiles(new ArrayList<>(radiusSet));

            ret.setBonjourGatewayProfiles(profileServiceInterface.get(bonjourGatewayProfileIds));

            List<Client> blockedClients = clientServiceInterface.getBlockedClients(customerId);
            List<MacAddress> blockList = Lists.newArrayList();
            if ((blockedClients != null) && !blockedClients.isEmpty()) {
                blockedClients.forEach(client -> blockList.add(client.getMacAddress()));
            }

            ret.setBlockedClients(blockList);
            ret.setRadiusProfiles(new ArrayList<>(radiusSet));

            LOG.debug("ApConfig {}", ret);

        } catch (Exception e) {
            LOG.error("Cannot read config for AP {}", apId, e);
        }

        return ret;
    }

    @Override
    public void wifiVIFStateDbTableUpdate(List<OpensyncAPVIFState> vifStateTables, String apId) {
        LOG.debug("Received Wifi_VIF_State table update for AP {}", apId);

        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession == null) {
            LOG.debug("wifiVIFStateDbTableUpdate::Cannot get Session for AP {}", apId);
            return;
        }

        long equipmentId = ovsdbSession.getEquipmentId();

        Equipment apNode = equipmentServiceInterface.getOrNull(equipmentId);
        if (apNode == null) {
            LOG.debug("wifiVIFStateDbTableUpdate::Cannot get EquipmentId for AP {}", apId);
            return; // we don't have the required info to get the
            // radio type yet
        }

        int customerId = apNode.getCustomerId();

        if ((customerId < 0) || (equipmentId < 0)) {
            LOG.debug("wifiVIFStateDbTableUpdate::Cannot get valid CustomerId {} or EquipmentId {} for AP {}", customerId, equipmentId, apId);
            return;
        }

        ApElementConfiguration apElementConfig = (ApElementConfiguration) apNode.getDetails();

        ProfileContainer profileContainer = new ProfileContainer(profileServiceInterface.getProfileWithChildren(apNode.getProfileId()));
        RfConfiguration rfConfig = (RfConfiguration) profileContainer.getChildOfTypeOrNull(apNode.getProfileId(), ProfileType.rf).getDetails();

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
                    vifState.getAssociatedClients(), channel);

            RadioType radioType = null;
            Map<RadioType, RfElementConfiguration> rfElementMap = rfConfig.getRfConfigMap();
            Map<RadioType, ElementRadioConfiguration> elementRadioMap = apElementConfig.getRadioMap();

            if (apElementConfig.getAdvancedRadioMap().isEmpty()) {
                LOG.warn("No AdvancedRadioMap for ap {} {}", apId, apElementConfig);
                continue;
            }
            for (RadioType rType : elementRadioMap.keySet()) {
                boolean autoChannelSelection = rfElementMap.get(rType).getAutoChannelSelection();
                if (elementRadioMap.get(rType).getActiveChannel(autoChannelSelection) == channel) {
                    radioType = rType;
                    break;
                }
            }

            updateActiveBssids(customerId, equipmentId, apId, ssid, radioType, bssid, numClients);

        }

        Status activeBssidsStatus = statusServiceInterface.getOrNull(customerId, equipmentId, StatusDataType.ACTIVE_BSSIDS);
        if (activeBssidsStatus != null) {
            ActiveBSSIDs bssidStatusDetails = (ActiveBSSIDs) activeBssidsStatus.getDetails();

            if (bssidStatusDetails != null && bssidStatusDetails.getActiveBSSIDs() != null) {
                updateClientDetailsStatus(customerId, equipmentId, bssidStatusDetails);
            }
        }

        LOG.info("Finished wifiVIFStateDbTableUpdate updated {}", activeBssidsStatus);

    }

    private void updateClientDetailsStatus(int customerId, long equipmentId, ActiveBSSIDs statusDetails) {
        Status clientDetailsStatus = statusServiceInterface.getOrNull(customerId, equipmentId, StatusDataType.CLIENT_DETAILS);

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
            if (bssid.getRadioType() == null) {
                LOG.info("No radio type for BSSID, ignore");
                continue;
            }
            if (!clientsPerRadioType.containsKey(bssid.getRadioType())) {
                clientsPerRadioType.put(bssid.getRadioType(), 0);
            }
            int numConnectedForBssid = bssid.getNumDevicesConnected();
            int currentNumberOfClients = clientsPerRadioType.get(bssid.getRadioType());
            clientsPerRadioType.put(bssid.getRadioType(), currentNumberOfClients + numConnectedForBssid);
            LOG.debug("Processing updateClientDetailsStatus. Upgrade numClients for RadioType {} from {} to {}", bssid.getRadioType(), currentNumberOfClients,
                    clientsPerRadioType.get(bssid.getRadioType()));
        }

        clientConnectionDetails.setNumClientsPerRadio(clientsPerRadioType);
        clientDetailsStatus.setDetails(clientConnectionDetails);
        clientDetailsStatus = statusServiceInterface.update(clientDetailsStatus);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Processing updateClientDetailsStatus. Updated clientConnectionDetails to {}", clientDetailsStatus.toPrettyString());
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

        long equipmentId = ovsdbSession.getEquipmentId();
        Equipment ce = equipmentServiceInterface.getByInventoryIdOrNull(apId);
        if (ce == null) {
            LOG.debug("wifiRadioStatusDbTableUpdate::Cannot get Equipment for AP {}", apId);
            return;
        }

        int customerId = ce.getCustomerId();
        if ((customerId < 0) || (equipmentId < 0)) {
            LOG.debug("wifiRadioStatusDbTableUpdate::Cannot get valid CustomerId {} or EquipmentId {} for AP {}", customerId, equipmentId, apId);
            return;
        }

        ApElementConfiguration apElementConfiguration = ((ApElementConfiguration) ce.getDetails());

        boolean configStateMismatch = false;

        Status protocolStatus = null;

        Status channelStatus = statusServiceInterface.getOrNull(customerId, equipmentId, StatusDataType.RADIO_CHANNEL);
        Status channelStatusClone = null;
        if (channelStatus != null) {
            channelStatusClone = channelStatus.clone();
        }

        for (OpensyncAPRadioState radioState : radioStateTables) {
            LOG.debug("Processing Wifi_Radio_State table update for AP {} Radio {}", apId, radioState.freqBand);

            if (radioState.getFreqBand().equals(RadioType.UNSUPPORTED)) {
                LOG.debug("Could not get radio configuration for AP {}", apId);
                continue;
            }
            configStateMismatch = updateEquipmentConfigFromState(apId, apElementConfiguration, radioState);

            protocolStatus = updateProtocolStatus(customerId, equipmentId, radioState);

            channelStatus = updateChannelStatus(customerId, equipmentId, channelStatus, radioState);
        }

        if (protocolStatus != null) {
            statusServiceInterface.update(protocolStatus);
        }

        if (channelStatus != null && !Objects.equals(channelStatus, channelStatusClone)) {
            LOG.debug("wifiRadioStatusDbTableUpdate update Channel Status before {} after {}", channelStatusClone, channelStatus);
            statusServiceInterface.update(channelStatus);
        }

        if (configStateMismatch) {
            try {
                ((ApElementConfiguration) ce.getDetails()).getRadioMap().putAll(apElementConfiguration.getRadioMap());
                ce = equipmentServiceInterface.update(ce);
            } catch (DsConcurrentModificationException e) {
                LOG.error("Caught DsConcurrentModificationException.", e);
                throw new RuntimeException(e);
            }
        }
        LOG.info("Finished wifiRadioStateDbTableUpdate");

    }

    private boolean updateEquipmentConfigFromState(String apId, ApElementConfiguration apElementConfiguration, OpensyncAPRadioState radioState) {
        if (apElementConfiguration.getRadioMap().containsKey(radioState.getFreqBand())
                && apElementConfiguration.getRadioMap().get(radioState.getFreqBand()) != null) {
            if (radioState.getChannels() != null) {
                return updateChannelPowerLevels(apId, apElementConfiguration, radioState);
            }

        }

        return false;
    }

    private Status updateProtocolStatus(int customerId, long equipmentId, OpensyncAPRadioState radioState) {
        Status protocolStatus;
        EquipmentProtocolStatusData protocolStatusData;
        protocolStatus = statusServiceInterface.getOrNull(customerId, equipmentId, StatusDataType.PROTOCOL);

        if (protocolStatus != null) {

            protocolStatusData = (EquipmentProtocolStatusData) protocolStatus.getDetails();
            if (!protocolStatusData.getReportedCC().equals(CountryCode.getByName((radioState.getCountry())))) {

                LOG.debug("Protocol Status reportedCC {} radioStatus.getCountry {} radioStatus CountryCode fromName {}", protocolStatusData.getReportedCC(),
                        radioState.getCountry(), CountryCode.getByName((radioState.getCountry())));
                protocolStatusData.setReportedCC(CountryCode.getByName((radioState.getCountry())));
                protocolStatusData.setCountryCode(radioState.getCountry());
                protocolStatus.setDetails(protocolStatusData);

            } else {
                protocolStatus = null;
            }

        }
        return protocolStatus;
    }

    private Status updateChannelStatus(int customerId, long equipmentId, Status channelStatus, OpensyncAPRadioState radioState) {
        if (channelStatus == null) {
            channelStatus = new Status();
            channelStatus.setCustomerId(customerId);
            channelStatus.setEquipmentId(equipmentId);
            channelStatus.setStatusDataType(StatusDataType.RADIO_CHANNEL);
            EquipmentChannelStatusData channelStatusData = new EquipmentChannelStatusData();
            channelStatus.setDetails(channelStatusData);
        }
        ((EquipmentChannelStatusData) channelStatus.getDetails()).getChannelNumberStatusDataMap().put(radioState.getFreqBand(), radioState.getChannel());
        ChannelBandwidth channelBandwidth = convertHtModeToChannelBandwidth(radioState.getHtMode());
        ((EquipmentChannelStatusData) channelStatus.getDetails()).getChannelBandwidthStatusDataMap().put(radioState.getFreqBand(), channelBandwidth);
        ((EquipmentChannelStatusData) channelStatus.getDetails()).getTxPowerDataMap().put(radioState.getFreqBand(), radioState.getTxPower());
        return channelStatus;
    }
    
    private ChannelBandwidth convertHtModeToChannelBandwidth(String htMode) {
        switch (htMode) {
            case "HT20":
                return ChannelBandwidth.is20MHz;
            case "HT40":
            case "HT40-":
            case "HT40+":
                return ChannelBandwidth.is40MHz;
            case "HT80":
                return ChannelBandwidth.is80MHz;
            case "HT160":
                return ChannelBandwidth.is160MHz;
            default:
                LOG.warn("Unrecognized channel HtMode {}", htMode);
                return ChannelBandwidth.UNSUPPORTED;
        }
    }
    
    private boolean updateChannelPowerLevels(String apId, ApElementConfiguration apElementConfiguration, OpensyncAPRadioState radioState) {

        boolean configStateMismatch = false;
        Set<ChannelPowerLevel> channelPowerLevels = new HashSet<>();

        radioState.getChannels().entrySet().stream().forEach(k -> {
            if (k.getKey().equals("allowed") || k.getKey().equals("radar_detection")) {

                String[] channelNumbers = k.getValue().split(",");
                for (String channel : channelNumbers) {
                    if (channel != null) {
                        ChannelPowerLevel cpl = new ChannelPowerLevel();
                        cpl.setChannelNumber(Integer.parseInt(channel));
                        cpl.setDfs(k.getKey().equals("radar_detection"));
                        if (radioState.getChannelMode() != null && radioState.getChannelMode().equals("auto")) {
                            cpl.setChannelWidth(-1);
                        } else {
                            switch (radioState.getHtMode()) {
                                case "HT20":
                                    cpl.setChannelWidth(20);
                                    break;
                                case "HT40":
                                case "HT40-":
                                case "HT40+":
                                    cpl.setChannelWidth(40);
                                    break;
                                case "HT80":
                                    cpl.setChannelWidth(80);
                                    break;
                                case "HT160":
                                    cpl.setChannelWidth(160);
                                    break;
                                default:
                                    LOG.warn("Unrecognized channel HtMode {}", radioState.getHtMode());
                            }
                        }
                        cpl.setPowerLevel(radioState.getTxPower());
                        channelPowerLevels.add(cpl);
                    }
                }

            }
        });

        if (!Objects.deepEquals(apElementConfiguration.getRadioMap().get(radioState.getFreqBand()).getAllowedChannelsPowerLevels(), channelPowerLevels)) {
            configStateMismatch = true;
            apElementConfiguration.getRadioMap().get(radioState.getFreqBand()).setAllowedChannelsPowerLevels(channelPowerLevels);
        }

        LOG.debug("Updated AllowedChannels from Wifi_Radio_State table change for AP {} configStateMismatch {}", apId, configStateMismatch);
        return configStateMismatch;
    }

    private void updateActiveBssids(int customerId, long equipmentId, Object apId, String ssid, RadioType freqBand, String macAddress, int numClients) {
        Status activeBssidsStatus = statusServiceInterface.getOrNull(customerId, equipmentId, StatusDataType.ACTIVE_BSSIDS);

        if (activeBssidsStatus == null) {
            activeBssidsStatus = new Status();
            activeBssidsStatus.setCustomerId(customerId);
            activeBssidsStatus.setEquipmentId(equipmentId);
            activeBssidsStatus.setStatusDataType(StatusDataType.ACTIVE_BSSIDS);

            ActiveBSSIDs statusDetails = new ActiveBSSIDs();
            statusDetails.setActiveBSSIDs(new ArrayList<ActiveBSSID>());

            activeBssidsStatus.setDetails(statusDetails);

            activeBssidsStatus = statusServiceInterface.update(activeBssidsStatus);
            LOG.debug("Processing Wifi_VIF_State table update for AP {}, created new ACTIVE_BSSID Status {}", apId, activeBssidsStatus);

        }

        ActiveBSSIDs statusDetails = (ActiveBSSIDs) activeBssidsStatus.getDetails();

        LOG.debug("Processing Wifi_VIF_State table update for AP {}, activeBSSIDs StatusDetails before update {}", apId, statusDetails);

        List<ActiveBSSID> currentActiveBSSIDs = statusDetails.getActiveBSSIDs();
        if (currentActiveBSSIDs == null) {
            currentActiveBSSIDs = new ArrayList<>();
        } else {
            currentActiveBSSIDs = currentActiveBSSIDs.stream().filter(p -> (p.getRadioType() != null && p.getSsid() != null))
                    .filter(p -> !p.getRadioType().equals(freqBand) || !p.getSsid().equals(ssid)).collect(Collectors.toList());
            LOG.debug("Processing Wifi_VIF_State table update for AP {}, activeBSSIDs bssidList without current radio freq {} and ssid {}", apId,
                    currentActiveBSSIDs, ssid);
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

        LOG.info("Processing Wifi_VIF_State table update for AP {}, updated ACTIVE_BSSID Status {}", apId, activeBssidsStatus);

    }

    @Override
    public void wifiInetStateDbTableUpdate(List<OpensyncAPInetState> inetStateTables, String apId) {

        LOG.debug("Received Wifi_Inet_State table update for AP {}", apId);
        
        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession == null) {
            LOG.debug("wifiInetStateDbTableUpdate::Cannot get Session for AP {}", apId);
            return;
        }

        long equipmentId = ovsdbSession.getEquipmentId();
        Equipment ce = equipmentServiceInterface.getOrNull(equipmentId);
        if (ce == null) {
            LOG.debug("wifiInetStateDbTableUpdate Cannot get customer Equipment for {}", apId);
            return;
        }
        int customerId = ce.getCustomerId();
        if ((customerId < 0) || (equipmentId < 0)) {
            LOG.debug("wifiInetStateDbTableUpdate::Cannot get valid CustomerId {} or EquipmentId {} for AP {}", customerId, equipmentId, apId);
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
        
        Status ethernetPortStatus = statusServiceInterface.getOrNull(customerId, equipmentId, StatusDataType.WIRED_ETHERNET_PORT);
        if (ethernetPortStatus == null) {
        	ethernetPortStatus = new Status();
        	ethernetPortStatus.setCustomerId(customerId);
        	ethernetPortStatus.setEquipmentId(equipmentId);
        	ethernetPortStatus.setStatusDataType(StatusDataType.WIRED_ETHERNET_PORT);
        	ethernetPortStatus.setDetails(new WiredEthernetPortStatusData());
        	ethernetPortStatus = statusServiceInterface.update(ethernetPortStatus);
        }
        
        WiredEthernetPortStatusData ethernetPortStatusData = (WiredEthernetPortStatusData) ethernetPortStatus.getDetails();	
        Map<String, List<WiredPortStatus>> portStatus = ethernetPortStatusData.getInterfacePortStatusMap();

		for (OpensyncAPInetState inetState : inetStateTables) {
			if (inetState.ifName != null) {
				parseRawDataToWiredPortStatus(customerId, equipmentId, portStatus, inetState);
				ethernetPortStatusData.setInterfacePortStatusMap(portStatus);
				ethernetPortStatus.setDetails(ethernetPortStatusData);
				ethernetPortStatus = statusServiceInterface.update(ethernetPortStatus);
				LOG.debug("EthernetPortStatus for AP {} updated to {}", apId, ethernetPortStatus);
			}

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

            if (inetState.getIfType().equals("vlan") && inetState.parentIfName != null && inetState.parentIfName.equals(defaultWanInterfaceName)) {

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

	protected void parseRawDataToWiredPortStatus(int customerId, long equipmentId,
			Map<String, List<WiredPortStatus>> portStatus, OpensyncAPInetState inetState) {
        LOG.debug("Entering parseRawDataToWiredPortStatus for Customer {}, Equipment {} with inetState {}",
                customerId, equipmentId, inetState);
        List<WiredPortStatus> ports = new ArrayList<>();
        inetState.ethPorts.forEach((key, ethPort) -> {
            try {
                // Raw data from AP example: ethPorts={eth1=up wan 1000Mbps full}
                String[] ethPortsValues = ethPort.split(SPACE_SEPERATOR);
                List<Integer> allowedVlans = getAllowedVlans(inetState.getVlanTrunk());

                if (ethPortsValues.length == 4 && inetState.getIfType() != null) {
                    int speed = Integer.parseInt(ethPortsValues[2].replaceAll("[^0-9]", ""));
                    boolean isTrunkEnabled = VLAN_TRUNK_IF_TYPE.equalsIgnoreCase(inetState.getIfType());

                    int vlanId = inetState.getVlanId();
                    if (isTrunkEnabled) {
                        vlanId = getNativeVlanIdForTrunk(inetState.getVlanTrunk());
                    }

                    WiredPortStatus wps = new WiredPortStatus(key, ethPortsValues[1], inetState.getIfName(),
                            inetState.getIfType(), speed, ethPortsValues[3], ethPortsValues[0], vlanId,
                            isTrunkEnabled, allowedVlans);
                    
                    ports.add(wps);
                } else {
                    LOG.error("EthPorts doesn't have enough raw data for CustomerId {} or EquipmentId {}", customerId,
                            equipmentId);
                }
            } catch (Exception e) {
                LOG.error("CustomerId {} or EquipmentId {} has error when parsing raw data to WiredPortStatus: {}",
                        customerId, equipmentId, e);
            }
        });
        addToPortStatus(portStatus, inetState.getIfName(), ports);
    }

    // Raw data from AP example: ["allowed_vlans": " 100  200  300 "]
    // convert to List of Integer
	private List<Integer> getAllowedVlans(Map<String, String> vlanTrunk) {
		List<Integer> allowedVlans = new ArrayList<>();
		try {
			if (!vlanTrunk.isEmpty() && vlanTrunk.get(ALLOWED_VLANS) != null
					&& !Objects.equals(vlanTrunk.get(ALLOWED_VLANS), "")) {
				List<String> allowedVlansStringList = Arrays.asList(vlanTrunk.get(ALLOWED_VLANS).trim().split("\\s+"));
				for (String allowedVlan : allowedVlansStringList) {
					allowedVlans.add(Integer.parseInt(allowedVlan));
				}
			}
		} catch (Exception ex) {
			LOG.error("Unable to parse the allowed vlans from the vlanTrunk. Returning empty AllowedVlanList", ex);
		}
		LOG.debug("Returning allowed Vlans {}", allowedVlans);
		return allowedVlans;
	}

    private int getNativeVlanIdForTrunk(Map<String, String> vlanTrunk) {
        int nativeVlanId = 0;
        if (!vlanTrunk.isEmpty() && vlanTrunk.get(NATIVE_VLAN_ID) != null &&
                !Objects.equals(vlanTrunk.get(NATIVE_VLAN_ID), "")) {
            nativeVlanId = Integer.parseInt(vlanTrunk.get(NATIVE_VLAN_ID));
        }
        return nativeVlanId;
    }

	private void addToPortStatus(Map<String, List<WiredPortStatus>> portStatus, String ifName,
			List<WiredPortStatus> ports) {
		portStatus.put(ifName, ports);
		LOG.debug("Returning addToPortStatus with portStatus {}", portStatus);
	}

	@Override
	public void wifiInetStateDbTableDelete(List<OpensyncAPInetState> inetStateTables, String apId) {

		LOG.debug("Received Wifi_Inet_State table delete for AP {}", apId);

		OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

		if (ovsdbSession == null) {
			LOG.debug("wifiInetStateDbTableDelete::Cannot get Session for AP {}", apId);
			return;
		}

		long equipmentId = ovsdbSession.getEquipmentId();
		Equipment ce = equipmentServiceInterface.getOrNull(equipmentId);
		if (ce == null) {
			LOG.debug("wifiInetStateDbTableDelete Cannot get customer Equipment for {}", apId);
			return;
		}

		int customerId = ce.getCustomerId();
		if ((customerId < 0) || (equipmentId < 0)) {
			LOG.debug("wifiInetStateDbTableUpdate::Cannot get valid CustomerId {} or EquipmentId {} for AP {}",
					customerId, equipmentId, apId);
			return;
		}

		Status ethernetPortStatus = statusServiceInterface.getOrNull(customerId, equipmentId,
				StatusDataType.WIRED_ETHERNET_PORT);
		if (ethernetPortStatus != null) {
			WiredEthernetPortStatusData ethernetPortStatusData = (WiredEthernetPortStatusData) ethernetPortStatus
					.getDetails();
			Map<String, List<WiredPortStatus>> portStatus = ethernetPortStatusData.getInterfacePortStatusMap();

			for (OpensyncAPInetState inetState : inetStateTables) {

				portStatus.remove(inetState.getIfName());
				ethernetPortStatusData.setInterfacePortStatusMap(portStatus);
				ethernetPortStatus.setDetails(ethernetPortStatusData);
				ethernetPortStatus = statusServiceInterface.update(ethernetPortStatus);
				LOG.debug("Deleted ifName {} from the AP {}. EthernetPortStatus after deletion{}",
						inetState.getIfName(), apId, ethernetPortStatus);
			}
		}
	}

    @Override
    public void wifiAssociatedClientsDbTableUpdate(List<OpensyncWifiAssociatedClients> wifiAssociatedClients, String apId) {

        LOG.info("Received wifiAssociatedClientsDbTableUpdate monitor notification for Client(s) {} on AP {}", wifiAssociatedClients, apId);

        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession == null) {
            LOG.debug("wifiAssociatedClientsDbTableUpdate::Cannot get Session for AP {}", apId);
            return;
        }
        long equipmentId = ovsdbSession.getEquipmentId();
        Equipment ce = equipmentServiceInterface.getOrNull(equipmentId);
        if (ce == null) {
            LOG.debug("wifiAssociatedClientsDbTableUpdate Cannot get customer Equipment for {}", apId);
            return;
        }
        int customerId = ce.getCustomerId();
        if ((customerId < 0) || (equipmentId < 0)) {
            LOG.debug("wifiAssociatedClientsDbTableUpdate::Cannot get valid CustomerId {} or EquipmentId {} for AP {}", customerId, equipmentId, apId);
            return;
        }

        if ((wifiAssociatedClients == null) || wifiAssociatedClients.isEmpty()) {
            return;
        }

        List<ClientSession> clientSessions = new ArrayList<>();

        long timestamp = System.currentTimeMillis();

        for (OpensyncWifiAssociatedClients opensyncWifiAssociatedClients : wifiAssociatedClients) {

            LOG.info("opensyncWifiAssociatedClients {}", opensyncWifiAssociatedClients);

            String mMac = opensyncWifiAssociatedClients.mac;
            MacAddress macAddress = MacAddress.valueOf(mMac);

            Client clientInstance = clientServiceInterface.getOrNull(customerId, macAddress);
            if (clientInstance == null) {
                clientInstance = new Client();
                clientInstance.setCustomerId(customerId);
                clientInstance.setMacAddress(MacAddress.valueOf(mMac));
                clientInstance.setCreatedTimestamp(System.currentTimeMillis());
                clientInstance.setDetails(new ClientInfoDetails());
                clientInstance = clientServiceInterface.create(clientInstance);
                LOG.info("Created client from Wifi_Associated_Clients ovsdb table change {}", clientInstance);
            }

            ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId, clientInstance.getMacAddress());
            if (clientSession == null) {
                clientSession = new ClientSession();
                clientSession.setCustomerId(customerId);
                clientSession.setEquipmentId(equipmentId);
                clientSession.setMacAddress(clientInstance.getMacAddress());
                clientSession.setLocationId(ce.getLocationId());
                clientSession.setDetails(new ClientSessionDetails());
                long derivedSessionId = WiFiSessionUtility.encodeWiFiAssociationId(timestamp / 1000, clientInstance.getMacAddress().getAddressAsLong());
                clientSession.getDetails().setSessionId(Long.toUnsignedString(derivedSessionId));
                clientSession.getDetails().setDhcpDetails(new ClientDhcpDetails(Long.toUnsignedString(derivedSessionId)));
                clientSession.getDetails().setAssociationState(AssociationState._802_11_Associated);
                clientSession.getDetails().setIsReassociation(false);
                clientSession.getDetails().setAssocTimestamp(timestamp);
                clientSessions.add(clientSession);
            } else {
                if (!clientSession.getDetails().getAssociationState().equals(AssociationState._802_11_Associated)) {
                    clientSession.getDetails().setAssociationState(AssociationState._802_11_Associated);
                    clientSession.getDetails().setAssocTimestamp(timestamp);
                    clientSessions.add(clientSession);
                }
            }
        }

        if (clientSessions.size() > 0) {
            LOG.info("Updated client sessions from Wifi_Associatied_Clients ovsdb table change {}", clientServiceInterface.updateSessions(clientSessions));
        }

    }

    @Override
    public void awlanNodeDbTableUpdate(OpensyncAWLANNode node, String apId) {

        LOG.debug("AP {} table AWLAN_Node updated {}", apId, node);
        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession == null) {
            LOG.info("awlanNodeDbTableUpdate::Cannot get Session for AP {}", apId);
            return;
        }

        long equipmentId = ovsdbSession.getEquipmentId();
        Equipment ce = equipmentServiceInterface.getByInventoryIdOrNull(apId);
        if (ce == null) {
            LOG.info("awlanNodeDbTableUpdate::Cannot find AP {}", apId);
            return;
        }
        int customerId = ce.getCustomerId();
        if ((customerId < 0) || (equipmentId < 0)) {
            LOG.info("awlanNodeDbTableUpdate::Cannot get valid CustomerId {} or EquipmentId {} for AP {}", customerId, equipmentId, apId);
            return;
        }

        int upgradeStatusFromAp = node.getUpgradeStatus();
        EquipmentUpgradeState fwUpgradeState = null;
        FailureReason fwUpgradeFailureReason = null;

        if (node.getFirmwareUrl().equals(OvsdbStringConstants.OVSDB_AWLAN_AP_FACTORY_RESET)
                || node.getFirmwareUrl().equals(OvsdbStringConstants.OVSDB_AWLAN_AP_REBOOT)
                || node.getFirmwareUrl().equals(OvsdbStringConstants.OVSDB_AWLAN_AP_SWITCH_SOFTWARE_BANK) || node.getFirmwareUrl().equals("")) {

            LOG.debug("Firmware Url {}, no fwUpgradeState", node.getFirmwareUrl());
        } else {
            fwUpgradeState = OvsdbToWlanCloudTypeMappingUtility.getCloudEquipmentUpgradeStateFromOpensyncUpgradeStatus(upgradeStatusFromAp);

            if (upgradeStatusFromAp < 0) {
                fwUpgradeFailureReason = OvsdbToWlanCloudTypeMappingUtility.getCloudEquipmentUpgradeFailureReasonFromOpensyncUpgradeStatus(upgradeStatusFromAp);
            }
        }

        String reportedFwImageName = null;
        String reportedAltFwImageName = null;

        if (node.getVersionMatrix().containsKey(OvsdbStringConstants.FW_IMAGE_ACTIVE_KEY)) {
            reportedFwImageName = node.getVersionMatrix().get(OvsdbStringConstants.FW_IMAGE_ACTIVE_KEY);
        } else {
            reportedFwImageName = node.getVersionMatrix().get(OvsdbStringConstants.FW_IMAGE_NAME_KEY);

        }

        if (node.getVersionMatrix().containsKey(OvsdbStringConstants.FW_IMAGE_INACTIVE_KEY)) {
            reportedAltFwImageName = node.getVersionMatrix().get(OvsdbStringConstants.FW_IMAGE_INACTIVE_KEY);
        } else {
            reportedAltFwImageName = node.getVersionMatrix().get(OvsdbStringConstants.FW_IMAGE_NAME_KEY);

        }
        List<Status> updates = new ArrayList<>();

        Status protocolStatus = configureProtocolStatus(node, customerId, equipmentId, reportedFwImageName, reportedAltFwImageName);
        if (protocolStatus != null) {
            updates.add(protocolStatus);
        }
        // TODO: this should be turned on after the AP has a load with the new mappings
        Status manufacturerData = configureManufacturerDetailsStatus(node, customerId, equipmentId);
        if (manufacturerData != null) {
            updates.add(manufacturerData);
        }

        Status firmwareStatus =
                configureFirmwareStatus(customerId, equipmentId, fwUpgradeState, fwUpgradeFailureReason, reportedFwImageName, reportedAltFwImageName);
        if (firmwareStatus != null) {
            updates.add(firmwareStatus);
        }

        if (!updates.isEmpty()) {// may be some updates from
            // protocol
            // status
            updates = statusServiceInterface.update(updates);
        }
    }

    private Status configureManufacturerDetailsStatus(OpensyncAWLANNode node, int customerId, long equipmentId) {

        Status statusRecord = statusServiceInterface.getOrNull(customerId, equipmentId, StatusDataType.EQUIPMENT_MANUFACTURER_DATA);
        if (statusRecord == null) {
            statusRecord = new Status();
            statusRecord.setCustomerId(customerId);
            statusRecord.setEquipmentId(equipmentId);
            statusRecord.setStatusDataType(StatusDataType.EQUIPMENT_MANUFACTURER_DATA);
            EquipmentManufacturerDataStatus statusData = new EquipmentManufacturerDataStatus();
            statusRecord.setDetails(statusData);
        }

        EquipmentManufacturerQrCode qrCode = new EquipmentManufacturerQrCode();
        if (node.qrCode != null) {
            if (node.qrCode.containsKey("DT")) {
                qrCode.setDeviceType(EquipmentType.getByName(node.qrCode.get("DT")));
            }
            qrCode.setVendorName(node.qrCode.get("VN"));
            if (isValidMACAddress(node.qrCode.get("DM"))) {
                qrCode.setDeviceMac(MacAddress.valueOf(node.qrCode.get("DM")));
            }
            qrCode.setHwRevision(node.qrCode.get("HW"));
            qrCode.setModelName(node.qrCode.get("MN"));
            qrCode.setSerialNumber(node.qrCode.get("SN"));
        }

        ((EquipmentManufacturerDataStatus) statusRecord.getDetails()).setSkuNumber(node.skuNumber);
        ((EquipmentManufacturerDataStatus) statusRecord.getDetails()).setModel(node.model);
        ((EquipmentManufacturerDataStatus) statusRecord.getDetails()).setRevision(node.revision);
        ((EquipmentManufacturerDataStatus) statusRecord.getDetails()).setSerialNumber(node.serialNumber);
        ((EquipmentManufacturerDataStatus) statusRecord.getDetails()).setQrCode(qrCode);
        if (node.manufacturerName != null) {
            ((EquipmentManufacturerDataStatus) statusRecord.getDetails()).setManufacturerName(node.manufacturerName);
        }
        ((EquipmentManufacturerDataStatus) statusRecord.getDetails()).setManufacturerDate(node.manufacturerDate);
        ((EquipmentManufacturerDataStatus) statusRecord.getDetails()).setManufacturerUrl(node.manufacturerUrl);
        ((EquipmentManufacturerDataStatus) statusRecord.getDetails()).setModelDescription(node.modelDescription);
        ((EquipmentManufacturerDataStatus) statusRecord.getDetails()).setReferenceDesign(node.referenceDesign);
        if (node.certificationRegion != null && !node.certificationRegion.equalsIgnoreCase("unknown")) {
            ((EquipmentManufacturerDataStatus) statusRecord.getDetails()).setCertificationRegion(CountryCode.getByName(node.certificationRegion.toUpperCase()));
        }
        if (isValidMACAddress(node.getId())) {
            ((EquipmentManufacturerDataStatus) statusRecord.getDetails()).setMacAddress(MacAddress.valueOf(node.getId()));
        }
        return statusRecord;
    }

    private boolean isValidMACAddress(String str) {
        String regex = "^([0-9A-Fa-f]{2}[:-])" + "{5}([0-9A-Fa-f]{2})|" + "([0-9a-fA-F]{4}\\." + "[0-9a-fA-F]{4}\\." + "[0-9a-fA-F]{4})$";

        Pattern p = Pattern.compile(regex);
        if (str == null) {
            return false;
        }
        // Find match between given string
        // and regular expression
        // uSing Pattern.matcher()
        Matcher m = p.matcher(str);
        return m.matches();
    }

    private Status configureProtocolStatus(OpensyncAWLANNode node, int customerId, long equipmentId, String reportedSwImageName,
            String reportedAltSwImageName) {
        Status protocolStatus = statusServiceInterface.getOrNull(customerId, equipmentId, StatusDataType.PROTOCOL);
        if (protocolStatus != null) {
            EquipmentProtocolStatusData protocolStatusData = ((EquipmentProtocolStatusData) protocolStatus.getDetails());
            if (protocolStatusData.getReportedSku() != null && protocolStatusData.getReportedSku().equals(node.getSkuNumber())
                    && protocolStatusData.getReportedSwVersion() != null && protocolStatusData.getReportedSwVersion().equals(reportedSwImageName)
                    && protocolStatusData.getReportedSwAltVersion() != null && protocolStatusData.getReportedSwAltVersion().equals(reportedAltSwImageName)
                    && protocolStatusData.getReportedHwVersion() != null && protocolStatusData.getReportedHwVersion().equals(node.getPlatformVersion())
                    && protocolStatusData.getSystemName() != null && protocolStatusData.getSystemName().equals(node.getModel())) {
                // no changes
                return null;
            }
            protocolStatusData.setReportedSku(node.getSkuNumber());
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
            protocolStatusData.setReportedHwVersion(node.getPlatformVersion());
            protocolStatusData.setSystemName(node.getModel());
        }
        return protocolStatus;
    }

    private Status configureFirmwareStatus(int customerId, long equipmentId, EquipmentUpgradeState fwUpgradeState, FailureReason fwUpgradeFailureReason,
            String reportedFwImageName, String reportedAltFwImageName) {

        Status firmwareStatus = statusServiceInterface.getOrNull(customerId, equipmentId, StatusDataType.FIRMWARE);
        if (firmwareStatus != null) {
            EquipmentUpgradeStatusData upgradeStatusData = (EquipmentUpgradeStatusData) firmwareStatus.getDetails();
            if (upgradeStatusData.getActiveSwVersion() != null && upgradeStatusData.getActiveSwVersion().equals(reportedFwImageName)
                    && upgradeStatusData.getAlternateSwVersion() != null && upgradeStatusData.getAlternateSwVersion().equals(reportedAltFwImageName)
                    && upgradeStatusData.getUpgradeState() != null && upgradeStatusData.getUpgradeState().equals(fwUpgradeState)) {
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
            } else if (fwUpgradeState.equals(EquipmentUpgradeState.download_complete) || fwUpgradeState.equals(EquipmentUpgradeState.apply_complete)
                    || fwUpgradeState.equals(EquipmentUpgradeState.apply_initiated) || fwUpgradeState.equals(EquipmentUpgradeState.applying)
                    || fwUpgradeState.equals(EquipmentUpgradeState.downloading) || fwUpgradeState.equals(EquipmentUpgradeState.download_initiated)
                    || fwUpgradeState.equals(EquipmentUpgradeState.reboot_initiated) || fwUpgradeState.equals(EquipmentUpgradeState.rebooting)) {

                LOG.info("Firmware upgrade is in state {}", fwUpgradeState);

                upgradeStatusData.setUpgradeState(fwUpgradeState);
                if (fwUpgradeState.equals(EquipmentUpgradeState.apply_initiated)) {
                    upgradeStatusData.setUpgradeStartTime(System.currentTimeMillis());
                } else if (fwUpgradeState.equals(EquipmentUpgradeState.reboot_initiated) || fwUpgradeState.equals(EquipmentUpgradeState.rebooting)) {
                    upgradeStatusData.setRebooted(true);
                }
                firmwareStatus.setDetails(upgradeStatusData);
            } else if (fwUpgradeState.equals(EquipmentUpgradeState.apply_failed) || fwUpgradeState.equals(EquipmentUpgradeState.download_failed)
                    || fwUpgradeState.equals(EquipmentUpgradeState.reboot_failed)) {
                LOG.warn("Firmware upgrade is in a failed state {} due to {}", fwUpgradeState, fwUpgradeFailureReason);

                upgradeStatusData.setUpgradeState(fwUpgradeState, fwUpgradeFailureReason);
                firmwareStatus.setDetails(upgradeStatusData);
            } else {

                ((EquipmentUpgradeStatusData) firmwareStatus.getDetails()).setUpgradeState(EquipmentUpgradeState.undefined);
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
        long equipmentId = ovsdbSession.getEquipmentId();
        Equipment ce = equipmentServiceInterface.getOrNull(equipmentId);
        if (ce == null) {
            LOG.debug("wifiVIFStateDbTableDelete Cannot get customer Equipment for {}", apId);
            return;
        }
        int customerId = ce.getCustomerId();
        if ((customerId < 0) || (equipmentId < 0)) {
            LOG.debug("wifiVIFStateDbTableDelete::Cannot get valid CustomerId {} or EquipmentId {} for AP {}", customerId, equipmentId, apId);
            return;
        }

        Status activeBssidsStatus = statusServiceInterface.getOrNull(customerId, equipmentId, StatusDataType.ACTIVE_BSSIDS);
        if (activeBssidsStatus == null) {
            return; // nothing to delete
        }
        ActiveBSSIDs statusDetails = (ActiveBSSIDs) activeBssidsStatus.getDetails();
        List<ActiveBSSID> bssidList = statusDetails.getActiveBSSIDs();
        List<ActiveBSSID> toBeDeleted = new ArrayList<>();
        List<ClientSession> clientSessionsForCustomerAndEquipment = new ArrayList<>();
        if (ce != null) {
            PaginationResponse<ClientSession> clientSessions = clientServiceInterface.getSessionsForCustomer(customerId, ImmutableSet.of(equipmentId),
                    ImmutableSet.of(ce.getLocationId()), null, null, new PaginationContext<ClientSession>());
            clientSessionsForCustomerAndEquipment.addAll(clientSessions.getItems());
        }
        for (OpensyncAPVIFState vifState : vifStateTables) {

            if (bssidList != null) {
                for (ActiveBSSID activeBSSID : bssidList) {
                    if (activeBSSID.getBssid().equals(vifState.getMac()) && activeBSSID.getSsid().equals(vifState.getSsid())) {
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

        LOG.info("Received wifiAssociatedClientsDbTableDelete monitor notification for MAC {} on AP {}", deletedClientMac, apId);
        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession == null) {
            LOG.debug("wifiAssociatedClientsDbTableDelete::Cannot get Session for AP {}", apId);
            return;
        }
        long equipmentId = ovsdbSession.getEquipmentId();
        Equipment ce = equipmentServiceInterface.getOrNull(equipmentId);
        if (ce == null) {
            LOG.debug("wifiAssociatedClientsDbTableDelete Cannot get customer Equipment for {}", apId);
            return;
        }
        int customerId = ce.getCustomerId();
        if ((customerId < 0) || (equipmentId < 0)) {
            LOG.debug("wifiAssociatedClientsDbTableDelete::Cannot get valid CustomerId {} or EquipmentId {} for AP {}", customerId, equipmentId, apId);
            return;
        }
        Client client = clientServiceInterface.getOrNull(customerId, MacAddress.valueOf(deletedClientMac));
        ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId, MacAddress.valueOf(deletedClientMac));

        if (client != null) {
            if (clientSession != null && clientSession.getDetails() != null && clientSession.getDetails().getAssociationState() != null) {
                if (!clientSession.getDetails().getAssociationState().equals(AssociationState.Disconnected)) {
                    clientSession.getDetails().setAssociationState(AssociationState.Disconnected);
                    clientSession = clientServiceInterface.updateSession(clientSession);
                    LOG.info("Session {} for client {} is now disconnected.", clientSession, client.getMacAddress());
                }
            }

        } else {
            if (clientSession != null) {
                clientSession = clientServiceInterface.deleteSession(customerId, equipmentId, MacAddress.valueOf(deletedClientMac));
                LOG.info("No client {} found, delete session {}", MacAddress.valueOf(deletedClientMac), clientSession);
            }
        }

    }

    @Override
    public void dhcpLeasedIpDbTableUpdate(List<Map<String, String>> dhcpAttributes, String apId, RowUpdateOperation rowUpdateOperation) {

        LOG.info("dhcpLeasedIpDbTableUpdate {} operations on AP {} ", rowUpdateOperation, apId);

        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession == null) {
            LOG.debug("updateDhcpIpClientFingerprints::Cannot get Session for AP {}", apId);
            return;
        }

        long equipmentId = ovsdbSession.getEquipmentId();
        Equipment ce = equipmentServiceInterface.getByInventoryIdOrNull(apId);
        if (ce == null) {
            LOG.debug("updateDhcpIpClientFingerprints::Cannot get Equipment for AP {}", apId);
            return;
        }
        int customerId = ce.getCustomerId();
        if ((customerId < 0) || (equipmentId < 0)) {
            LOG.debug("updateDhcpIpClientFingerprints::Cannot get valid CustomerId {} or EquipmentId {} for AP {}", customerId, equipmentId, apId);
            return;
        }

        long locationId = ce.getLocationId();

        if (rowUpdateOperation.equals(RowUpdateOperation.INSERT) || rowUpdateOperation.equals(RowUpdateOperation.MODIFY)
                || rowUpdateOperation.equals(RowUpdateOperation.INIT)) {
            List<ClientSession> clientSessionList = new ArrayList<>();
            for (Map<String, String> dhcpLeasedIps : dhcpAttributes) {
                if (!dhcpLeasedIps.containsKey("hwaddr")) {
                    LOG.info("Cannot insert a client {} that has no hwaddr.", dhcpLeasedIps);
                    continue;
                }
                MacAddress clientMacAddress = MacAddress.valueOf(dhcpLeasedIps.get("hwaddr"));
                if (clientMacAddress.equals(equipmentServiceInterface.get(equipmentId).getBaseMacAddress())) {
                    LOG.info("Not a client device {} ", dhcpLeasedIps);
                    // In case somehow this equipment has accidentally been
                    // tagged as a client, remove
                    ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId, clientMacAddress);
                    if (clientSession != null) {
                        LOG.info("Deleting invalid client session {}", clientServiceInterface.deleteSession(customerId, equipmentId, clientMacAddress));
                    }
                    if (clientServiceInterface.getOrNull(customerId, clientMacAddress) != null) {
                        LOG.info("Deleting invalid client {}", clientServiceInterface.delete(customerId, clientMacAddress));
                    }
                    continue;
                } else {
                    // Create or update
                    Client client = clientServiceInterface.getOrNull(customerId, clientMacAddress);
                    if (client == null) {
                        LOG.info("No client present for {}", clientMacAddress);
                        continue;
                    } else {
                        LOG.info("Client {} already exists on the cloud, update client values", dhcpLeasedIps);
                        if (dhcpLeasedIps.containsKey("hostname")) {
                            ((ClientInfoDetails) client.getDetails()).setHostName(dhcpLeasedIps.get("hostname"));
                        }
                        if (dhcpLeasedIps.containsKey("fingerprint")) {
                            ((ClientInfoDetails) client.getDetails()).setApFingerprint(dhcpLeasedIps.get("fingerprint"));
                        }
                        if (dhcpLeasedIps.containsKey("device_type")) {
                            DhcpFpDeviceType dhcpFpDeviceType = DhcpFpDeviceType.getByName(dhcpLeasedIps.get("device_type"));
                            ClientType clientType = OvsdbToWlanCloudTypeMappingUtility.getClientTypeForDhcpFpDeviceType(dhcpFpDeviceType);
                            LOG.debug("Translate from ovsdb {} to cloud {}", dhcpFpDeviceType, clientType);
                            ((ClientInfoDetails) client.getDetails()).setClientType(clientType.getId());
                        }
                        client = clientServiceInterface.update(client);
                        LOG.info("Updated Client {}.", client);
                        // In this case, we might have a session, as the client
                        // already exists on the cloud, update if required
                        ClientSession session = updateClientSession(customerId, equipmentId, locationId, dhcpLeasedIps, clientMacAddress);
                        if (session != null) {
                            clientSessionList.add(session);
                        }
                    }
                }
            }

            if (!clientSessionList.isEmpty()) {
                LOG.info("Updating client sessions {}", clientSessionList);
                clientSessionList = clientServiceInterface.updateSessions(clientSessionList);
                LOG.info("Updated client sessions {}", clientSessionList);
            }

        } else if (rowUpdateOperation.equals(RowUpdateOperation.DELETE)) {
            for (Map<String, String> dhcpLeasedIps : dhcpAttributes) {
                if (!dhcpLeasedIps.containsKey("hwaddr")) {
                    LOG.info("Cannot find a client {} that has no hwaddr.", dhcpLeasedIps);
                    continue;
                }
                MacAddress clientMacAddress = MacAddress.valueOf(dhcpLeasedIps.get("hwaddr"));
                Client client = clientServiceInterface.getOrNull(customerId, clientMacAddress);
                if (client == null) {
                    LOG.info("Cannot find client instance for {}", clientMacAddress);
                    ClientSession session = clientServiceInterface.getSessionOrNull(customerId, equipmentId, clientMacAddress);
                    if (session != null) {
                        LOG.info("Delete clientSession for client that was removed from the Dhcp_Leased_IP table {}",
                                clientServiceInterface.deleteSession(customerId, equipmentId, clientMacAddress));
                    }
                } else if (clientMacAddress.equals(equipmentServiceInterface.get(equipmentId).getBaseMacAddress())) {
                    LOG.info("Not a client device {} ", dhcpLeasedIps);
                    // In case somehow this equipment has accidentally been
                    // tagged as a client, remove
                    ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId, clientMacAddress);
                    if (clientSession != null) {
                        LOG.info("Deleting invalid client session {}", clientServiceInterface.deleteSession(customerId, equipmentId, clientMacAddress));
                    }
                    if (clientServiceInterface.getOrNull(customerId, clientMacAddress) != null) {
                        LOG.info("Deleting invalid client {}", clientServiceInterface.delete(customerId, clientMacAddress));
                    }
                }
            }
        }
    }

    protected ClientSession updateClientSession(int customerId, long equipmentId, long locationId, Map<String, String> dhcpLeasedIps,
            MacAddress clientMacAddress) {

        long timestamp = System.currentTimeMillis();

        ClientSession session = clientServiceInterface.getSessionOrNull(customerId, equipmentId, clientMacAddress);
        if (session == null) {
            LOG.info("No session for client {} with for customer {} equipment {}", clientMacAddress, customerId, equipmentId);
            return null;
        }

        if (dhcpLeasedIps.containsKey("fingerprint")) {
            session.getDetails().setApFingerprint(dhcpLeasedIps.get("fingerprint"));
        }

        if (dhcpLeasedIps.containsKey("inet_addr")) {
            String ipAddress = dhcpLeasedIps.get("inet_addr");
            LOG.info("Dhcp_Leased_IP inet_addr {}", ipAddress);
            if (ipAddress != null) {
                try {
                    InetAddress inetAddress = InetAddress.getByName(ipAddress);
                    if ((inetAddress instanceof Inet4Address) && inetAddress.getHostAddress().equals(ipAddress)) {
                        LOG.info("IPv4 address {}", inetAddress);
                        session.getDetails().setIpAddress(inetAddress);
                    } else if (inetAddress instanceof Inet6Address) {
                        LOG.info("IPv6 address {}", inetAddress);
                        session.getDetails().setIpAddress(inetAddress);
                    } else {
                        LOG.error("Invalid IP Address {}", ipAddress);
                    }
                    session.getDetails().setIpTimestamp(timestamp);

                } catch (UnknownHostException ex) {
                }
            }
        }

        if (dhcpLeasedIps.containsKey("hostname")) {
            session.getDetails().setHostname(dhcpLeasedIps.get("hostname"));
        }

        if (dhcpLeasedIps.containsKey("dhcp_server")) {
            String ipAddress = dhcpLeasedIps.get("dhcp_server");
            LOG.info("Dhcp_Leased_IP dhcp_server {}", ipAddress);
            if (ipAddress != null) {
                try {
                    InetAddress inetAddress = InetAddress.getByName(ipAddress);
                    if ((inetAddress instanceof Inet4Address) && inetAddress.getHostAddress().equals(ipAddress)) {
                        LOG.info("IPv4 address {}", inetAddress);
                        session.getDetails().getDhcpDetails().setDhcpServerIp(inetAddress);
                    } else if (inetAddress instanceof Inet6Address) {
                        LOG.info("IPv6 address {}", inetAddress);
                        session.getDetails().getDhcpDetails().setDhcpServerIp(inetAddress);
                    } else {
                        LOG.error("Invalid IP Address {}", ipAddress);
                    }

                } catch (UnknownHostException ex) {
                }
            }
        }

        if (dhcpLeasedIps.containsKey("lease_time")) {
            Integer leaseTime = Integer.valueOf(dhcpLeasedIps.get("lease_time"));
            session.getDetails().getDhcpDetails().setLeaseTimeInSeconds(leaseTime);
            session.getDetails().getDhcpDetails().setLeaseStartTimestamp(session.getDetails().getAssocTimestamp());
        }

        if (dhcpLeasedIps.containsKey("gateway")) {
            String ipAddress = dhcpLeasedIps.get("gateway");
            LOG.info("Dhcp_Leased_IP gateway {}", ipAddress);
            if (ipAddress != null) {
                try {
                    InetAddress inetAddress = InetAddress.getByName(ipAddress);
                    if ((inetAddress instanceof Inet4Address) && inetAddress.getHostAddress().equals(ipAddress)) {
                        LOG.info("IPv4 address {}", inetAddress);
                        session.getDetails().getDhcpDetails().setGatewayIp(inetAddress);
                    } else if (inetAddress instanceof Inet6Address) {
                        LOG.info("IPv6 address {}", inetAddress);
                        session.getDetails().getDhcpDetails().setGatewayIp(inetAddress);
                    } else {
                        LOG.error("Invalid Gateway IP {}", ipAddress);
                    }

                } catch (UnknownHostException ex) {
                }
            }
        }

        if (dhcpLeasedIps.containsKey("subnet_mask")) {
            String ipAddress = dhcpLeasedIps.get("subnet_mask");
            LOG.info("Dhcp_Leased_IP subnet_mask {}", ipAddress);
            if (ipAddress != null) {
                try {
                    InetAddress inetAddress = InetAddress.getByName(ipAddress);
                    if ((inetAddress instanceof Inet4Address) && inetAddress.getHostAddress().equals(ipAddress)) {
                        LOG.info("IPv4 address {}", inetAddress);
                        session.getDetails().getDhcpDetails().setSubnetMask(inetAddress);
                    } else if (inetAddress instanceof Inet6Address) {
                        LOG.info("IPv6 address {}", inetAddress);
                        session.getDetails().getDhcpDetails().setSubnetMask(inetAddress);
                    } else {
                        LOG.error("Invalid subnet mask IP {}", ipAddress);
                    }
                } catch (UnknownHostException ex) {
                }
            }
        }

        if (dhcpLeasedIps.containsKey("primary_dns")) {
            String ipAddress = dhcpLeasedIps.get("primary_dns");
            LOG.info("Dhcp_Leased_IP primary_dns {}", ipAddress);
            if (ipAddress != null) {
                try {
                    InetAddress inetAddress = InetAddress.getByName(ipAddress);
                    if ((inetAddress instanceof Inet4Address) && inetAddress.getHostAddress().equals(ipAddress)) {
                        LOG.info("IPv4 address {}", inetAddress);
                        session.getDetails().getDhcpDetails().setPrimaryDns(inetAddress);
                    } else if (inetAddress instanceof Inet6Address) {
                        LOG.info("IPv6 address {}", inetAddress);
                        session.getDetails().getDhcpDetails().setPrimaryDns(inetAddress);
                    } else {
                        LOG.error("Invalid primary_dns IP {}", ipAddress);
                    }

                } catch (UnknownHostException ex) {
                }
            }
        }

        if (dhcpLeasedIps.containsKey("secondary_dns")) {
            String ipAddress = dhcpLeasedIps.get("secondary_dns");
            LOG.info("Dhcp_Leased_IP secondary_dns {}", ipAddress);
            if (ipAddress != null) {
                try {
                    InetAddress inetAddress = InetAddress.getByName(ipAddress);
                    if ((inetAddress instanceof Inet4Address) && inetAddress.getHostAddress().equals(ipAddress)) {
                        LOG.info("IPv4 address {}", inetAddress);
                        session.getDetails().getDhcpDetails().setSecondaryDns(inetAddress);
                    } else if (inetAddress instanceof Inet6Address) {
                        LOG.info("IPv6 address {}", inetAddress);
                        session.getDetails().getDhcpDetails().setSecondaryDns(inetAddress);
                    } else {
                        LOG.error("Invalid secondary_dns IP {}", ipAddress);
                    }

                } catch (UnknownHostException ex) {
                }
            }
        }

        if (dhcpLeasedIps.containsKey("device_name")) {
            session.getDetails().setClassificationName(dhcpLeasedIps.get("device_name"));
        }
        return session;
    }

    @Override
    public void commandStateDbTableUpdate(List<Map<String, String>> commandStateAttributes, String apId, RowUpdateOperation rowUpdateOperation) {
        LOG.info("Received Command_State row(s) {} rowUpdateOperation {} for ap {}", commandStateAttributes, rowUpdateOperation, apId);

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

        long equipmentId = ovsdbSession.getEquipmentId();
        Equipment ce = equipmentServiceInterface.getOrNull(equipmentId);
        if (ce == null) {
            LOG.debug("clearEquipmentStatus Cannot get customer Equipment for {}", apId);
            return;
        }
        int customerId = ce.getCustomerId();
        if ((customerId < 0) || (equipmentId < 0)) {
            LOG.debug("clearEquipmentStatus::Cannot get valid CustomerId {} or EquipmentId {} for AP {}", customerId, equipmentId, apId);
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

        List<Status> customerDashboardStatus =
                statusServiceInterface.getForEquipment(customerId, Set.of(equipmentId), Set.of(StatusDataType.CUSTOMER_DASHBOARD));
        for (Status customerDashStatus : customerDashboardStatus) {
            LOG.info("Updated customer status {}", statusServiceInterface.update(customerDashStatus));
        }

        // Set any existing client sessions to disconnected
        LOG.info("OpensyncExternalIntegrationCloud::clearEquipmentStatus disconnect any existing client sessions on AP {}", apId);
        disconnectClients(ce);

    }

    @Override
    public void apcStateDbTableUpdate(Map<String, String> apcStateAttributes, String apId, RowUpdateOperation rowUpdateOperation) {
        LOG.info("apcStateDbTableUpdate {} operations on AP {} with values {} ", rowUpdateOperation, apId, apcStateAttributes);

        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);
        if (ovsdbSession == null) {
            LOG.info("apcStateDbTableUpdate::Cannot get Session for AP {}", apId);
            return;
        }

        long equipmentId = ovsdbSession.getEquipmentId();
        Equipment ce = equipmentServiceInterface.getByInventoryIdOrNull(apId);
        if (ce == null) {
            LOG.info("apcStateDbTableUpdate::Cannot get Equipment for AP {}", apId);
            return;
        }

        int customerId = ce.getCustomerId();
        if ((customerId < 0) || (equipmentId < 0)) {
            LOG.info("apcStateDbTableUpdate::Cannot get valid CustomerId {} or EquipmentId {} for AP {}", customerId, equipmentId, apId);
            return;
        }

        InetAddress drIpAddr = null;
        InetAddress bdrIpAddr = null;
        try {
            bdrIpAddr = InetAddress.getByName(apcStateAttributes.get("backupDesignatedRouterIp"));
        } catch (UnknownHostException e) {
            LOG.error("UnknownHost for backupDesignatedRouterIp", e);
        }
        try {
            drIpAddr = InetAddress.getByName(apcStateAttributes.get("designatedRouterIp"));
        } catch (UnknownHostException e) {
            LOG.error("UnknownHost for designatedRouterIp", e);
        }
        Status protocolStatus = statusServiceInterface.getOrNull(customerId, equipmentId, StatusDataType.PROTOCOL);
        String mode = apcStateAttributes.get("mode");

        InetAddress localIpV4Addr = null;
        if (protocolStatus != null) {
            localIpV4Addr = ((EquipmentProtocolStatusData) protocolStatus.getDetails()).getReportedIpV4Addr();
            ((EquipmentProtocolStatusData) protocolStatus.getDetails()).setIsApcConnected((drIpAddr != null && !drIpAddr.getHostAddress().equals("0.0.0.0")));
            ((EquipmentProtocolStatusData) protocolStatus.getDetails()).setLastApcUpdate(System.currentTimeMillis());
            ((EquipmentProtocolStatusData) protocolStatus.getDetails()).setReportedApcAddress(drIpAddr);
            ((EquipmentProtocolStatusData) protocolStatus.getDetails()).setRadiusProxyAddress(drIpAddr);
            ((EquipmentProtocolStatusData) protocolStatus.getDetails()).setApcMode(ApcMode.valueOf(mode));
            ((EquipmentProtocolStatusData) protocolStatus.getDetails()).setApcBackupDesignatedRouterIpAddress(bdrIpAddr);
            ((EquipmentProtocolStatusData) protocolStatus.getDetails()).setApcDesignatedRouterIpAddress(drIpAddr);

            protocolStatus = statusServiceInterface.update(protocolStatus);
            LOG.info("Protocol Status updated for APC_State table monitor change {}", protocolStatus);
        }
        ApcElectionEvent electionEvent =
                new ApcElectionEvent(drIpAddr, bdrIpAddr, localIpV4Addr, drIpAddr, mode, Boolean.valueOf(apcStateAttributes.get("enabled")),
                        RealTimeEventType.APC_Election_event, customerId, ce.getLocationId(), equipmentId, System.currentTimeMillis());
        statsPublisherInterface.publishSystemEventFromTableStateMonitor(electionEvent);
    }

    @Override
    public void nodeStateDbTableUpdate(List<Map<String, String>> nodeStateAttributes, String apId) {
        LOG.debug("start nodeStateDbTableUpdate for {}", apId);
        if (LOG.isTraceEnabled())
            LOG.trace("nodeStateDbTableUpdate tableAttributes {}", nodeStateAttributes);

        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);
        if (ovsdbSession == null) {
            LOG.warn("nodeStateDbTableUpdate::Cannot get Session for AP {}", apId);
            return;
        }

        long equipmentId = ovsdbSession.getEquipmentId();
        Equipment ce = equipmentServiceInterface.getByInventoryIdOrNull(apId);
        if (ce == null) {
            LOG.warn("nodeStateDbTableUpdate::Cannot get Equipment for AP {}", apId);
            return;
        }

        int customerId = ce.getCustomerId();
        if ((customerId < 0) || (equipmentId < 0)) {
            LOG.warn("nodeStateDbTableUpdate::Cannot get valid CustomerId {} or EquipmentId {} for AP {}", customerId, equipmentId, apId);
            return;
        }

        Status eqAdminStatus = statusServiceInterface.getOrNull(customerId, equipmentId, StatusDataType.EQUIPMENT_ADMIN);

		LedStatus ledStatus = null;
		for (Map<String, String> nsa : nodeStateAttributes) {
			if (nsa.get("module").equals("led")) {
				if (nsa.get("key").equals("led_state") && nsa.get("value").equals("on")) {
					ledStatus = LedStatus.led_on;
				} else if (nsa.get("key").equals("led_state") && nsa.get("value").equals("off")) {
					ledStatus = LedStatus.led_off;
				} else if (nsa.get("key").equals("led_blink")) {
					ledStatus = LedStatus.led_blink;
				} else {
					ledStatus = LedStatus.UNKNOWN;
				}
			}
		}

        if (ledStatus != null) {
            if (eqAdminStatus != null) {
                if (((EquipmentAdminStatusData) eqAdminStatus.getDetails()).getLedStatus() == null
                        || !((EquipmentAdminStatusData) eqAdminStatus.getDetails()).getLedStatus().equals(ledStatus)) {
                    ((EquipmentAdminStatusData) eqAdminStatus.getDetails()).setLedStatus(ledStatus);
                    eqAdminStatus = statusServiceInterface.update(eqAdminStatus);
                    LOG.debug("nodeStateDbTableUpdate updated status {}", eqAdminStatus);
                }
            }
        }

        LOG.debug("finished nodeStateDbTableUpdate for {}", apId);

    }

    @Override
    public void processMqttMessage(String topic, Report report) {
        statsPublisherInterface.processMqttMessage(topic, report);
    }
}
