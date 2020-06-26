package com.telecominfraproject.wlan.opensync.external.integration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.alarm.AlarmServiceInterface;
import com.telecominfraproject.wlan.client.ClientServiceInterface;
import com.telecominfraproject.wlan.client.info.models.ClientInfoDetails;
import com.telecominfraproject.wlan.client.session.models.ClientDhcpDetails;
import com.telecominfraproject.wlan.client.session.models.ClientSession;
import com.telecominfraproject.wlan.client.session.models.ClientSessionDetails;
import com.telecominfraproject.wlan.client.session.models.ClientSessionMetricDetails;
import com.telecominfraproject.wlan.cloudeventdispatcher.CloudEventDispatcherInterface;
import com.telecominfraproject.wlan.core.model.entity.CountryCode;
import com.telecominfraproject.wlan.core.model.equipment.AutoOrManualValue;
import com.telecominfraproject.wlan.core.model.equipment.DetectedAuthMode;
import com.telecominfraproject.wlan.core.model.equipment.EquipmentType;
import com.telecominfraproject.wlan.core.model.equipment.MacAddress;
import com.telecominfraproject.wlan.core.model.equipment.NeighborScanPacketType;
import com.telecominfraproject.wlan.core.model.equipment.NetworkType;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.customer.models.Customer;
import com.telecominfraproject.wlan.customer.service.CustomerServiceInterface;
import com.telecominfraproject.wlan.datastore.exceptions.DsConcurrentModificationException;
import com.telecominfraproject.wlan.equipment.EquipmentServiceInterface;
import com.telecominfraproject.wlan.equipment.models.ApElementConfiguration;
import com.telecominfraproject.wlan.equipment.models.Equipment;
import com.telecominfraproject.wlan.equipment.models.StateSetting;
import com.telecominfraproject.wlan.firmware.FirmwareServiceInterface;
import com.telecominfraproject.wlan.firmware.models.CustomerFirmwareTrackRecord;
import com.telecominfraproject.wlan.firmware.models.CustomerFirmwareTrackSettings;
import com.telecominfraproject.wlan.firmware.models.CustomerFirmwareTrackSettings.TrackFlag;
import com.telecominfraproject.wlan.location.models.Location;
import com.telecominfraproject.wlan.location.service.LocationServiceInterface;
import com.telecominfraproject.wlan.opensync.external.integration.controller.OpensyncCloudGatewayController;
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
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration.SecureMode;
import com.telecominfraproject.wlan.routing.RoutingServiceInterface;
import com.telecominfraproject.wlan.routing.models.EquipmentRoutingRecord;
import com.telecominfraproject.wlan.servicemetric.apnode.models.ApNodeMetrics;
import com.telecominfraproject.wlan.servicemetric.apnode.models.ApPerformance;
import com.telecominfraproject.wlan.servicemetric.apnode.models.EthernetLinkState;
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
import com.telecominfraproject.wlan.status.equipment.models.EquipmentUpgradeStatusData;
import com.telecominfraproject.wlan.status.equipment.models.VLANStatusData;
import com.telecominfraproject.wlan.status.equipment.report.models.ActiveBSSID;
import com.telecominfraproject.wlan.status.equipment.report.models.ActiveBSSIDs;
import com.telecominfraproject.wlan.status.equipment.report.models.EquipmentCapacityDetails;
import com.telecominfraproject.wlan.status.equipment.report.models.EquipmentPerRadioUtilizationDetails;
import com.telecominfraproject.wlan.status.equipment.report.models.RadioUtilizationReport;
import com.telecominfraproject.wlan.status.models.Status;
import com.telecominfraproject.wlan.status.models.StatusCode;
import com.telecominfraproject.wlan.status.models.StatusDataType;
import com.telecominfraproject.wlan.status.network.models.CapacityDetails;
import com.telecominfraproject.wlan.status.network.models.ChannelUtilizationDetails;
import com.telecominfraproject.wlan.status.network.models.CommonProbeDetails;
import com.telecominfraproject.wlan.status.network.models.EquipmentPerformanceDetails;
import com.telecominfraproject.wlan.status.network.models.NetworkAdminStatusData;
import com.telecominfraproject.wlan.status.network.models.NetworkAggregateStatusData;
import com.telecominfraproject.wlan.status.network.models.NoiseFloorDetails;
import com.telecominfraproject.wlan.status.network.models.RadioUtilizationDetails;
import com.telecominfraproject.wlan.status.network.models.RadiusDetails;
import com.telecominfraproject.wlan.status.network.models.TrafficDetails;
import com.telecominfraproject.wlan.status.network.models.UserDetails;

import sts.OpensyncStats.Client;
import sts.OpensyncStats.ClientReport;
import sts.OpensyncStats.Device;
import sts.OpensyncStats.Device.RadioTemp;
import sts.OpensyncStats.Neighbor;
import sts.OpensyncStats.Neighbor.NeighborBss;
import sts.OpensyncStats.RadioBandType;
import sts.OpensyncStats.Report;
import sts.OpensyncStats.RssiPeer;
import sts.OpensyncStats.RssiPeer.RssiSample;
import sts.OpensyncStats.RssiPeer.RssiSource;
import sts.OpensyncStats.RssiReport;
import sts.OpensyncStats.Survey;
import sts.OpensyncStats.Survey.SurveySample;
import sts.OpensyncStats.SurveyType;
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

    @Value("${connectus.ovsdb.autoProvisionedCustomerId:1970}")
    private int autoProvisionedCustomerId;
    @Value("${connectus.ovsdb.autoProvisionedLocationId:8}")
    private int autoProvisionedLocationId;
    @Value("${connectus.ovsdb.autoProvisionedProfileId:1}")
    private int autoProvisionedProfileId;
    @Value("${connectus.ovsdb.autoProvisionedSsid:autoProvisionedSsid}")
    private String autoProvisionedSsid;
    @Value("${connectus.ovsdb.isAutoconfigEnabled:true}")
    private boolean isAutoconfigEnabled;
    @Value("${connectus.ovsdb.defaultFwVersion:r10947-65030d81f3}")
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

    @Override
    public void apConnected(String apId, ConnectNodeInfo connectNodeInfo) {

        if (isAutoconfigEnabled) {
            // default track settings for firmware
            CustomerFirmwareTrackSettings trackSettings = firmwareServiceInterface.getDefaultCustomerTrackSetting();

            // check for updated/modified track settings for this customer
            CustomerFirmwareTrackRecord custFwTrackRecord = firmwareServiceInterface
                    .getCustomerFirmwareTrackRecord(autoProvisionedCustomerId);
            if (custFwTrackRecord != null) {
                trackSettings = custFwTrackRecord.getSettings();
            }
            // determine if AP requires FW upgrade before cloud connection/provision
            if (trackSettings.getAutoUpgradeDeprecatedOnBind().equals(TrackFlag.ALWAYS)
                    || trackSettings.getAutoUpgradeUnknownOnBind().equals(TrackFlag.ALWAYS)) {

                // check the reported fw version for the AP, if it is < than
                // the default version for the cloud, then download and
                // flash the firmware before proceeding.
                // then return;
                LOG.debug(
                        "Automatic firmware upgrade is configured for track {}. Firmware will be flashed to newer version if required.",
                        trackSettings);

            } else {
                // auto upgrade not configured for this customer
                LOG.debug("Automatic firmware upgrade is not configured for track {}", trackSettings);
            }

            Equipment ce = getCustomerEquipment(apId);

            try {

                if (ce == null) {
                    ce = new Equipment();
                    ce.setEquipmentType(EquipmentType.AP);
                    ce.setInventoryId(apId);
                    ce.setSerial(connectNodeInfo.serialNumber);
                    ce.setDetails(ApElementConfiguration.createWithDefaults());
                    ce.setCustomerId(autoProvisionedCustomerId);
                    ce.setName(ce.getEquipmentType().name() + "_" + ce.getSerial());
                    ce.setLocationId(autoProvisionedLocationId);
                    
                    ce = equipmentServiceInterface.create(ce);

                    ApElementConfiguration apElementConfig = (ApElementConfiguration) ce.getDetails();
                    apElementConfig.setDeviceName(ce.getName());
                    apElementConfig.setEquipmentModel(connectNodeInfo.model);
                    apElementConfig.getAdvancedRadioMap().get(RadioType.is2dot4GHz)
                            .setAutoChannelSelection(StateSetting.disabled);
                    apElementConfig.getAdvancedRadioMap().get(RadioType.is5GHzL)
                            .setAutoChannelSelection(StateSetting.disabled);
                    apElementConfig.getAdvancedRadioMap().get(RadioType.is5GHzU)
                            .setAutoChannelSelection(StateSetting.disabled);

                    apElementConfig.getRadioMap().get(RadioType.is2dot4GHz).setAutoChannelSelection(false);
                    apElementConfig.getRadioMap().get(RadioType.is5GHzL).setAutoChannelSelection(false);
                    apElementConfig.getRadioMap().get(RadioType.is5GHzU).setAutoChannelSelection(false);

                    ce.setDetails(apElementConfig);
                    ce = equipmentServiceInterface.update(ce);

                    Profile apProfile = new Profile();
                    apProfile.setCustomerId(ce.getCustomerId());
                    apProfile.setName("DefaultApProfile");
                    apProfile.setDetails(ApNetworkConfiguration.createWithDefaults());
                    apProfile = profileServiceInterface.create(apProfile);

                    Profile ssidProfile = new Profile();
                    ssidProfile.setCustomerId(ce.getCustomerId());
                    ssidProfile.setName("DefaultSsid-2g");
                    SsidConfiguration ssidConfig = SsidConfiguration.createWithDefaults();
                    ssidConfig.setSsid("DefaultSsid-2g");
                    ssidConfig.setSecureMode(SecureMode.wpa2PSK);
                    ssidConfig.setKeyStr("12345678");
                    ssidConfig.setVlanId(0);


                    Set<RadioType> appliedRadios = new HashSet<>();
                    appliedRadios.add(RadioType.is2dot4GHz);
                    // ssidConfig.getRadioBasedConfigs().get(RadioType.is2dot4GHz).setEnable80211r(true);

                    ssidConfig.setAppliedRadios(appliedRadios);
                    ssidProfile.setDetails(ssidConfig);
                    ssidProfile = profileServiceInterface.create(ssidProfile);

                    Profile ssidProfile5g = new Profile();
                    ssidProfile5g.setCustomerId(ce.getCustomerId());
                    ssidProfile5g.setName("DefaultSsid-5g");
                    SsidConfiguration ssidConfig5g = SsidConfiguration.createWithDefaults();
                    ssidConfig5g.setSecureMode(SecureMode.wpa2PSK);
                    ssidConfig5g.setSsid("DefaultSsid-5g");
                    ssidConfig5g.setKeyStr("12345678");
                    ssidConfig5g.setVlanId(0);
                    Set<RadioType> appliedRadios5g = new HashSet<>();
                    appliedRadios5g.add(RadioType.is5GHzL);
                    appliedRadios5g.add(RadioType.is5GHzU);
                    ssidConfig5g.setAppliedRadios(appliedRadios5g);
                    // ssidConfig5g.getRadioBasedConfigs().get(RadioType.is5GHzL).setEnable80211r(true);
                    // ssidConfig5g.getRadioBasedConfigs().get(RadioType.is5GHzU).setEnable80211r(true);

                    ssidProfile5g.setDetails(ssidConfig5g);
                    ssidProfile5g = profileServiceInterface.create(ssidProfile5g);

                    Set<Long> childProfileIds = new HashSet<>();
                    childProfileIds.add(ssidProfile.getId());
                    childProfileIds.add(ssidProfile5g.getId());

                    apProfile.setChildProfileIds(childProfileIds);

                    apProfile = profileServiceInterface.update(apProfile);
                    ce.setProfileId(apProfile.getId());

                    ce = equipmentServiceInterface.update(ce);

                    Customer customer = customerServiceInterface.getOrNull(ce.getCustomerId());
                    if (customer == null) {
                        customer = new Customer();
                        customer.setId(autoProvisionedCustomerId);
                        customerServiceInterface.create(customer);
                        ce.setCustomerId(customer.getId());
                        equipmentServiceInterface.update(ce);
                    }
                }

                EquipmentRoutingRecord equipmentRoutingRecord = gatewayController
                        .registerCustomerEquipment(ce.getName(), ce.getCustomerId(), ce.getId());

                updateApStatus(ce, connectNodeInfo);

                OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);
                ovsdbSession.setRoutingId(equipmentRoutingRecord.getId());
                ovsdbSession.setEquipmentId(ce.getId());
                ovsdbSession.setCustomerId(ce.getCustomerId());

                LOG.debug("Equipment {}", ce);
                LOG.info("AP {} got connected to the gateway", apId);
                LOG.info("ConnectNodeInfo {}", connectNodeInfo);

            } catch (Exception e) {
                LOG.error("Could not process connection from AP {}", apId, e);
                throw e;
            }
        } else {
            LOG.info("Autoconfig is not enabled for this AP {}", apId);
        }

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
            protocolStatusData.setReportedSwVersion(connectNodeInfo.firmwareVersion);
            protocolStatusData.setReportedSwAltVersion(connectNodeInfo.firmwareVersion);
            try {
                protocolStatusData.setReportedIpV4Addr(InetAddress.getByName(connectNodeInfo.ipV4Address));
            } catch (UnknownHostException e) {
                // do nothing here
            }
            if (connectNodeInfo.macAddress != null && MacAddress.valueOf(connectNodeInfo.macAddress) != null) {
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
            fwUpgradeStatusData.setActiveSwVersion(connectNodeInfo.firmwareVersion);
            fwUpgradeStatusData.setAlternateSwVersion(connectNodeInfo.firmwareVersion);
            fwUpgradeStatusData.setTargetSwVersion(connectNodeInfo.firmwareVersion);
            fwUpgradeStatusData.setUpgradeState(EquipmentUpgradeState.up_to_date);
            statusRecord.setDetails(fwUpgradeStatusData);
            statusServiceInterface.update(statusRecord);

            // TODO:
            // equipmentStatusInterface.updateNetworkAdminStatus(networkAdminStatusRecord);
            // dtop: this one populates traffic capacity and usage dial on the
            // main dashboard
            // from APDemoMetric properties getPeriodLengthSec, getRxBytes2G,
            // getTxBytes2G, getRxBytes5G, getTxBytes5G
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

            Status networkAggStatusRec = statusServiceInterface.getOrNull(ce.getCustomerId(), ce.getId(),
                    StatusDataType.NETWORK_AGGREGATE);
            if (networkAggStatusRec == null) {
                networkAggStatusRec = new Status();
                networkAggStatusRec.setCustomerId(ce.getCustomerId());
                networkAggStatusRec.setEquipmentId(ce.getId());
                networkAdminStatusRec.setStatusDataType(StatusDataType.NETWORK_AGGREGATE);
                NetworkAggregateStatusData naStatusData = new NetworkAggregateStatusData();
                naStatusData.setApPerformanceDetails(new EquipmentPerformanceDetails());
                naStatusData.setCapacityDetails(new CapacityDetails());
                naStatusData.setChannelUtilizationDetails(new ChannelUtilizationDetails());
                naStatusData.setCloudLinkDetails(new CommonProbeDetails());
                naStatusData.setDhcpDetails(new CommonProbeDetails());
                naStatusData.setDnsDetails(new CommonProbeDetails());
                naStatusData.setEquipmentPerformanceDetails(new EquipmentPerformanceDetails());
                naStatusData.setNoiseFloorDetails(new NoiseFloorDetails());
                naStatusData.setRadioUtilizationDetails(new RadioUtilizationDetails());
                naStatusData.setRadiusDetails(new RadiusDetails());
                naStatusData.setTrafficDetails(new TrafficDetails());
                naStatusData.setUserDetails(new UserDetails());
                networkAggStatusRec.setDetails(naStatusData);

                networkAggStatusRec = statusServiceInterface.update(networkAggStatusRec);

            }

        } catch (Exception e) {
            LOG.debug("Exception in updateApStatus", e);
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
            if (isAutoconfigEnabled) {
                OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);
                if (ovsdbSession == null) {
                    throw new IllegalStateException("AP is not connected " + apId);
                }

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
                for (Profile ssidProfile : ret.getSsidProfile()) {

                    radiusSet.addAll(profileContainer.getChildrenOfType(ssidProfile.getId(), ProfileType.radius));

                }
                ret.setRadiusProfiles(new ArrayList<>(radiusSet));

                LOG.debug("ApConfig {}", ret.toString());
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

        long equipmentId = extractEquipmentIdFromTopic(topic);
        if (equipmentId <= 0 || customerId <= 0) {
            LOG.warn("Cannot determine equipment ids from topic {} - customerId {} equipmentId {}", topic, customerId,
                    equipmentId);
            return;
        }

        gatewayController.updateActiveCustomer(customerId);

        List<ServiceMetric> metricRecordList = new ArrayList<>();

        populateApClientMetrics(metricRecordList, report, customerId, equipmentId);
        populateApNodeMetrics(metricRecordList, report, customerId, equipmentId);
        populateNeighbourScanReports(metricRecordList, report, customerId, equipmentId);
        try {
            // TODO: depends on survey
            // populateChannelInfoReports(metricRecordList, report, customerId,
            // equipmentId);
            populateApSsidMetrics(metricRecordList, report, customerId, equipmentId, extractApIdFromTopic(topic));
            // handleRssiMetrics(metricRecordList, report, customerId,
            // equipmentId);

        } catch (Exception e) {
            LOG.error("Exception when processing populateApSsidMetrics", e);
        }

        if (!metricRecordList.isEmpty()) {
            equipmentMetricsCollectorInterface.publishMetrics(metricRecordList);
        }

    }

    private void handleRssiMetrics(List<ServiceMetric> metricRecordList, Report report, int customerId,
            long equipmentId) {
        LOG.debug("handleRssiMetrics for Customer {} Equipment {}", customerId, equipmentId);

        for (RssiReport rssiReport : report.getRssiReportList()) {

            for (RssiPeer peer : rssiReport.getPeerListList()) {
                if (peer.getRssiSource().equals(RssiSource.CLIENT)) {
                    int rssi = 0;

                    for (RssiSample sample : peer.getRssiListList()) {
                        rssi += getNegativeSignedIntFromUnsigned(sample.getRssi());
                        LOG.debug("RSSI Sample: unsignedValue {} signedValue {}", sample.getRssi(),
                                getNegativeSignedIntFromUnsigned(sample.getRssi()));
                    }

                    rssi = rssi / peer.getRssiListCount();

                    LOG.debug("RssiReport::RssiPeer::Band {} RssiPeer MAC {} RssiSamples Avg {} RxPpdus {} TxPpdus {}",
                            rssiReport.getBand(), peer.getMacAddress(), rssi, peer.getRxPpdus(), peer.getTxPpdus());
                }
            }

        }
    }

    private void populateApNodeMetrics(List<ServiceMetric> metricRecordList, Report report, int customerId,
            long equipmentId) {
        {
            LOG.debug("populateApNodeMetrics for Customer {} Equipment {}", customerId, equipmentId);
            ApNodeMetrics apNodeMetrics = new ApNodeMetrics();
            ServiceMetric smr = new ServiceMetric(customerId, equipmentId);
            metricRecordList.add(smr);
            smr.setDetails(apNodeMetrics);

            for (Device deviceReport : report.getDeviceList()) {

                ApPerformance apPerformance = new ApPerformance();
                apNodeMetrics.setApPerformance(apPerformance);

                smr.setCreatedTimestamp(deviceReport.getTimestampMs());
                // data.setChannelUtilization2G(channelUtilization2G);
                // data.setChannelUtilization5G(channelUtilization5G);

                if (deviceReport.getRadioTempCount() > 0) {
                    int cpuTemperature = 0;
                    int numSamples = 0;
                    for (RadioTemp r : deviceReport.getRadioTempList()) {
                        if (r.hasValue()) {
                            cpuTemperature += r.getValue();
                            numSamples++;
                        }
                    }

                    if (numSamples > 0) {
                        apPerformance.setCpuTemperature(cpuTemperature / numSamples);
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
            }

            // Main Network dashboard shows Traffic and Capacity values that
            // are
            // calculated
            // from
            // ApNodeMetric properties getPeriodLengthSec, getRxBytes2G,
            // getTxBytes2G,
            // getRxBytes5G, getTxBytes5G

            // go over all the clients to aggregate per-client tx/rx stats -
            // we
            // want to do
            // this
            // only once per batch of ApNodeMetrics - so we do not repeat
            // values
            // over and
            // over again

            for (ClientReport clReport : report.getClientsList()) {

                long rxBytes = 0;
                long txBytes = 0;

                for (Client cl : clReport.getClientListList()) {
                    if (cl.getStats().hasTxBytes()) {
                        txBytes += cl.getStats().getTxBytes();
                    }
                    if (cl.getStats().hasRxBytes()) {
                        rxBytes += cl.getStats().getRxBytes();
                    }

                }

                RadioType radioType = getRadioTypeFromOpensyncRadioBand(clReport.getBand());

                apNodeMetrics.setRxBytes(radioType, rxBytes);
                apNodeMetrics.setTxBytes(radioType, txBytes);

                // TODO: temporary solution as this was causing Noise Floor to
                // disappear from Dashboard and Access Point rows
                apNodeMetrics.setNoiseFloor(radioType, -98);
                // TODO: Radio Utilization will be calculated when the survey is
                // enabled
                apNodeMetrics.setRadioUtilization(radioType, new ArrayList<>());

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

            // populate it from report.survey
            // for (Survey survey : report.getSurveyList()) {
            /*
             * LOG.debug("MJH Survey {}", survey); // int oBSS = 0; // int iBSS
             * = 0; // int totalBusy = 0; // int durationMs = 0; for
             * (SurveySample surveySample : survey.getSurveyListList()) { if
             * (surveySample.getDurationMs() == 0) { continue; }
             *
             * // iBSS += surveySample.getBusySelf() + //
             * surveySample.getBusyTx(); // oBSS += surveySample.getBusyRx(); //
             * totalBusy += surveySample.getBusy(); // durationMs +=
             * surveySample.getDurationMs();
             *
             * RadioUtilization radioUtil = new RadioUtilization(); radioUtil
             * .setTimestampSeconds((int) ((survey.getTimestampMs() +
             * surveySample.getOffsetMs()) / 1000));
             * radioUtil.setAssocClientTx(100 * surveySample.getBusyTx() /
             * surveySample.getDurationMs()); radioUtil.setAssocClientRx(100 *
             * surveySample.getBusyRx() / surveySample.getDurationMs());
             * radioUtil.setNonWifi( 100 * (surveySample.getBusy() -
             * surveySample.getBusyTx() - surveySample.getBusyRx()) /
             * surveySample.getDurationMs());
             *
             * RadioType radioType = RadioType.UNSUPPORTED; switch
             * (survey.getBand()) { case BAND2G: radioType =
             * RadioType.is2dot4GHz; break; case BAND5G: radioType =
             * RadioType.is5GHz; break; case BAND5GL: radioType =
             * RadioType.is5GHzL; break; case BAND5GU: radioType =
             * RadioType.is5GHzU; break; }
             *
             * apNodeMetrics.getRadioUtilization(radioType).add(radioUtil);
             *
             * }
             *
             * // Double totalUtilization = 100D * totalBusy / durationMs; //
             * LOG.trace("Total Utilization {}", totalUtilization); // Double
             * totalWifiUtilization = 100D * (iBSS + oBSS) / // durationMs; //
             * LOG.trace("Total Wifi Utilization {}", // totalWifiUtilization);
             * // LOG.trace("Total Non-Wifi Utilization {}", // totalUtilization
             * - // totalWifiUtilization); // if (survey.getBand() ==
             * RadioBandType.BAND2G) { //
             * data.setChannelUtilization2G(totalUtilization.intValue()); // }
             * else { //
             * data.setChannelUtilization5G(totalUtilization.intValue()); // }
             */
            // }

        }

    }

    private void populateApClientMetrics(List<ServiceMetric> metricRecordList, Report report, int customerId,
            long equipmentId) {
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
                metricRecordList.add(smr);

                smr.setCreatedTimestamp(clReport.getTimestampMs());

                // clReport.getChannel();
                ClientMetrics cMetrics = new ClientMetrics();
                smr.setDetails(cMetrics);

                Integer periodLengthSec = 60; // matches what's configured by
                // OvsdbDao.configureStats(OvsdbClient)
                cMetrics.setPeriodLengthSec(periodLengthSec);

                cMetrics.setRadioType(getRadioTypeFromOpensyncRadioBand(clReport.getBand()));

                if (cl.hasStats()) {
                    if (cl.getStats().hasRssi()) {
                        int unsignedRssi = cl.getStats().getRssi();
                        cMetrics.setRssi(getNegativeSignedIntFromUnsigned(unsignedRssi));
                    }

                    // we'll report each device as having a single (very long)
                    // session
                    cMetrics.setSessionId(smr.getClientMac());

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
                        cMetrics.setAverageTxRate(Double.valueOf(cl.getStats().getTxRate() / 1000));
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
            long equipmentId) {
        LOG.debug("populateNeighbourScanReports for Customer {} Equipment {}", customerId, equipmentId);

        for (Neighbor neighbor : report.getNeighborsList()) {

            ServiceMetric smr = new ServiceMetric(customerId, equipmentId);
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
                nr.setPrivacy((nBss.getSsid() == null || nBss.getSsid().isEmpty()) ? true : false);
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
            RadioBandType band, long timestamp, sts.OpensyncStats.Client client, String nodeId, MacAddress macAddress,
            String ssid) {

        LOG.debug("Client numConnected {} connectOffsetMs {} numDisconnected {} disconnectOffsetMs {} durationMs {}",
                client.getConnectCount(), client.getConnectOffsetMs(), client.getDisconnectCount(),
                client.getDisconnectOffsetMs(), client.getDurationMs());

        com.telecominfraproject.wlan.client.models.Client clientInstance = clientServiceInterface.getOrNull(customerId,
                new MacAddress(client.getMacAddress()));
        if (clientInstance == null) {
            clientInstance = new com.telecominfraproject.wlan.client.models.Client();
            clientInstance.setCustomerId(customerId);
            clientInstance.setMacAddress(new MacAddress(client.getMacAddress()));
            clientInstance.setDetails(new ClientInfoDetails());
            clientInstance = clientServiceInterface.create(clientInstance);
        }
        ClientInfoDetails clientDetails = (ClientInfoDetails) clientInstance.getDetails();
        clientDetails.setHostName(nodeId);
        clientInstance.setDetails(clientDetails);
        clientInstance = clientServiceInterface.update(clientInstance);

        try {

            ClientSession clientSession = clientServiceInterface.getSessionOrNull(customerId, equipmentId,
                    clientInstance.getMacAddress());
            if (clientSession == null) {
                LOG.debug("No session found for Client {}, creating new one.", client.getMacAddress());
                clientSession = new ClientSession();
                clientSession.setCustomerId(customerId);
                clientSession.setEquipmentId(equipmentId);
                clientSession.setLocationId(locationId);
                clientSession.setMacAddress(new MacAddress(client.getMacAddress()));
                clientSession.setDetails(new ClientSessionDetails());

                clientSession = clientServiceInterface.updateSession(clientSession);
            }

            ClientSessionDetails clientSessionDetails = clientSession.getDetails();
            clientSessionDetails.setRadioType(getRadioTypeFromOpensyncRadioBand(band));
            clientSessionDetails.setSessionId(clientSession.getMacAddress().getAddressAsLong());
            clientSessionDetails.setSsid(ssid);
            clientSessionDetails.setAssociationStatus(0);
            clientSessionDetails.setAssocTimestamp(timestamp - client.getConnectOffsetMs());
            clientSessionDetails.setAuthTimestamp(timestamp - client.getConnectOffsetMs());
            clientSessionDetails.setFirstDataRcvdTimestamp(timestamp);
            clientSessionDetails.setFirstDataSentTimestamp(timestamp);
            clientSessionDetails.setLastRxTimestamp(timestamp);
            clientSessionDetails.setHostname(nodeId);

            ClientDhcpDetails dhcpDetails = new ClientDhcpDetails(clientSessionDetails.getSessionId());
            clientSessionDetails.setDhcpDetails(dhcpDetails);

            clientSessionDetails.setMetricDetails(calculateClientSessionMetricDetails(client));
            clientSession.setDetails(clientSessionDetails);

            clientSession = clientServiceInterface.updateSession(clientSession);

            LOG.debug("CreatedOrUpdated clientSession {}", clientSession);

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

    private ClientSessionMetricDetails calculateClientSessionMetricDetails(sts.OpensyncStats.Client client) {
        ClientSessionMetricDetails metricDetails = new ClientSessionMetricDetails();
        metricDetails.setRssi(getNegativeSignedIntFromUnsigned(client.getStats().getRssi()));
        metricDetails.setRxBytes(client.getStats().getRxBytes());
        metricDetails.setTxBytes(client.getStats().getTxBytes());
        metricDetails.setTotalTxPackets(client.getStats().getTxFrames());
        metricDetails.setTotalRxPackets(client.getStats().getRxFrames());
        metricDetails.setTxDataFrames((int) ((int) client.getStats().getTxFrames() - client.getStats().getTxRetries()));
        metricDetails.setRxDataFrames((int) ((int) client.getStats().getRxFrames() - client.getStats().getRxRetries()));
        // values reported in Kbps, convert to Mbps
        metricDetails.setRxMbps(Float.valueOf((float) (client.getStats().getRxRate() / 1000)));
        metricDetails.setTxMbps(Float.valueOf((float) (client.getStats().getTxRate() / 1000)));
        metricDetails.setRxRateKbps((long) client.getStats().getRxRate());
        metricDetails.setTxRateKbps((long) client.getStats().getTxRate());
        return metricDetails;
    }

    private void populateApSsidMetrics(List<ServiceMetric> metricRecordList, Report report, int customerId,
            long equipmentId, String apId) {

        LOG.debug("populateApSsidMetrics for Customer {} Equipment {}", customerId, equipmentId);
        ServiceMetric smr = new ServiceMetric(customerId, equipmentId);
        ApSsidMetrics apSsidMetrics = new ApSsidMetrics();

        // we need to populate location Id on the client sessions, that's why
        // we're
        // getting equipment object in here (from the cache)
        Equipment equipment = getCustomerEquipment(apId);
        long locationId = (equipment != null) ? equipment.getLocationId() : 0;

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
            if (activeBssidsStatus != null) {
                ActiveBSSIDs statusDetails = (ActiveBSSIDs) activeBssidsStatus.getDetails();
                for (ActiveBSSID activeBSSID : statusDetails.getActiveBSSIDs()) {
                    if (activeBSSID.getRadioType().equals(radioType)) {
                        ssidStatistics.setBssid(new MacAddress(activeBSSID.getBssid()));
                        // ssid value, in case not in stats, else will take
                        // stats value after
                        ssid = activeBSSID.getSsid();
                    }
                }
            }

            for (Client client : clientReport.getClientListList()) {

                if (client.hasSsid() && client.getSsid() != null && !client.getSsid().equals("")) {
                    ssid = client.getSsid();
                }

                LOG.debug("Client {} connected {} connectedCount {}", client.getMacAddress(), client.getConnected(),
                        client.getConnectCount());

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

                    try {
                        handleClientSessionUpdate(customerId, equipmentId, apId, locationId, clientReport.getChannel(),
                                clientReport.getBand(), clientReport.getTimestampMs(), client, report.getNodeID(),
                                ssidStatistics.getBssid(), ssidStatistics.getSsid());
                    } catch (Exception e) {
                        LOG.debug("Unabled to update client {} session {}", client, e);
                    }
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
            ssidStatistics.setNumClient(clientMacs.size());
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

        // LOG.debug("Created ApSsidMetrics Report {}", apSsidMetrics);

    }

    int getNegativeSignedIntFromUnsigned(int unsignedValue) {
        int negSignedValue = (unsignedValue << 1) >> 1;
        return negSignedValue;
    }

    private void populateChannelInfoReports(List<ServiceMetric> metricRecordList, Report report, int customerId,
            long equipmentId) {

        LOG.debug("populateChannelInfoReports for Customer {} Equipment {}", customerId, equipmentId);
        ServiceMetric smr = new ServiceMetric();
        smr.setCustomerId(customerId);
        smr.setEquipmentId(equipmentId);

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

            if (survey.getSurveyType().equals(SurveyType.OFF_CHANNEL)
                    || survey.getSurveyType().equals(SurveyType.FULL)) {

                // in this case, we have multiple channels (potentially) and
                // will make
                // ChannelInfo entries per surveyed channel
                Map<Integer, List<SurveySample>> sampleByChannelMap = new HashMap<>();

                survey.getSurveyListList().stream().forEach(s -> {
                    List<SurveySample> surveySampleList;
                    if (sampleByChannelMap.get(s.getChannel()) == null) {
                        surveySampleList = new ArrayList<>();
                    } else {
                        surveySampleList = sampleByChannelMap.get(s.getChannel());
                    }
                    surveySampleList.add(s);
                    sampleByChannelMap.put(s.getChannel(), surveySampleList);
                });

                for (List<SurveySample> surveySampleList : sampleByChannelMap.values()) {
                    ChannelInfo channelInfo = createChannelInfo(equipmentId, radioType, surveySampleList);

                    List<ChannelInfo> channelInfoList = channelInfoReports.getRadioInfo(radioType);
                    if (channelInfoList == null) {
                        channelInfoList = new ArrayList<>();
                    }
                    channelInfoList.add(channelInfo);
                    channelInfoReports.getChannelInformationReportsPerRadio().put(radioType, channelInfoList);
                }

            } else {

                List<SurveySample> surveySampleList = survey.getSurveyListList();

                ChannelInfo channelInfo = createChannelInfo(equipmentId, radioType, surveySampleList);

                List<ChannelInfo> channelInfoList = channelInfoReports.getRadioInfo(radioType);
                if (channelInfoList == null) {
                    channelInfoList = new ArrayList<>();
                }
                channelInfoList.add(channelInfo);
                channelInfoReports.getChannelInformationReportsPerRadio().put(radioType, channelInfoList);
            }

        }

        LOG.debug("ChannelInfoReports {}", channelInfoReports);

    }

    private ChannelInfo createChannelInfo(long equipmentId, RadioType radioType, List<SurveySample> surveySampleList) {
        int busyTx = 0; /* Tx */
        int busySelf = 0; /* Rx_self (derived from succesful Rx frames) */
        int busy = 0; /* Busy = Rx + Tx + Interference */
        ChannelInfo channelInfo = new ChannelInfo();

        for (SurveySample sample : surveySampleList) {

            busyTx += sample.getBusyTx();
            busySelf += sample.getBusySelf();
            busy += sample.getBusy();
            channelInfo.setChanNumber(sample.getChannel());
        }

        int iBSS = busyTx + busySelf;

        int totalWifi = busy - iBSS;

        channelInfo.setTotalUtilization(busy);
        channelInfo.setWifiUtilization(totalWifi);
        channelInfo.setBandwidth(((ApElementConfiguration) equipmentServiceInterface.get(equipmentId).getDetails())
                .getRadioMap().get(radioType).getChannelBandwidth());
        channelInfo.setNoiseFloor(Integer.valueOf(-84)); // TODO: when this
                                                         // becomes available
                                                         // add
        return channelInfo;
    }

    @Override
    public void processMqttMessage(String topic, FlowReport flowReport) {

        LOG.info("Received report on topic {}", topic);
        int customerId = extractCustomerIdFromTopic(topic);

        long equipmentId = extractEquipmentIdFromTopic(topic);
        if (equipmentId <= 0 || customerId <= 0) {
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
        if (equipmentId <= 0 || customerId <= 0) {
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
        
        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession == null) {
            LOG.debug("wifiVIFStateDbTableUpdate::Cannot get Session for AP {}", apId);
            return;
        }

        int customerId = ovsdbSession.getCustomerId();
        long equipmentId = ovsdbSession.getEquipmentId();

        if (customerId < 0 || equipmentId < 0) {
            LOG.debug("wifiVIFStateDbTableUpdate::Cannot get valid CustomerId {} or EquipmentId {} for AP {}",
                    customerId, equipmentId, apId);
            return;
        }



        for (OpensyncAPVIFState vifState : vifStateTables) {

            if (vifState.getMac() != null && vifState.getSsid() != null && vifState.getChannel() > 0) {
                String bssid = vifState.getMac();
                String ssid = vifState.getSsid();

                int channel = vifState.getChannel();

                RadioType radioType = null;

                Equipment apNode = equipmentServiceInterface.getOrNull(equipmentId);
                if (apNode == null) {
                    continue; // we don't have the required info to get the
                              // radio type yet
                }

                ApElementConfiguration apElementConfig = (ApElementConfiguration) apNode.getDetails();
                for (RadioType key : apElementConfig.getRadioMap().keySet()) {
                    if (apElementConfig.getRadioMap().get(key).getChannelNumber() == channel) {
                        radioType = key;
                        break;
                    }
                }

                if (radioType == null) {
                    continue; // we cannot determine radioType for this BSSID
                }

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

                }

                ActiveBSSIDs statusDetails = (ActiveBSSIDs) activeBssidsStatus.getDetails();

                List<ActiveBSSID> bssidList = statusDetails.getActiveBSSIDs();
                boolean bssidAlreadyPresent = false;
                for (ActiveBSSID activeBssid : bssidList) {

                    if (activeBssid.getBssid().equals(bssid) && activeBssid.getSsid().equals(ssid)
                            && activeBssid.getRadioType().equals(radioType)) {
                        activeBssid.setNumDevicesConnected(vifState.getAssociatedClients().size());
                        bssidAlreadyPresent = true;
                        break;
                    }

                }

                if (!bssidAlreadyPresent) {
                    ActiveBSSID newActiveBssid = new ActiveBSSID();
                    newActiveBssid.setBssid(bssid);
                    newActiveBssid.setSsid(ssid);
                    newActiveBssid.setRadioType(radioType);
                    newActiveBssid.setNumDevicesConnected(vifState.getAssociatedClients().size());
                    bssidList.add(newActiveBssid);
                }

                statusDetails.setActiveBSSIDs(bssidList);
                activeBssidsStatus.setDetails(statusDetails);
                activeBssidsStatus = statusServiceInterface.update(activeBssidsStatus);

            }

        }

    }

    @Override
    public void wifiRadioStatusDbTableUpdate(List<OpensyncAPRadioState> radioStateTables, String apId) {

        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession == null) {
            LOG.debug("wifiRadioStatusDbTableUpdate::Cannot get Session for AP {}", apId);
            return;
        }

        int customerId = ovsdbSession.getCustomerId();
        long equipmentId = ovsdbSession.getEquipmentId();

        if (customerId < 0 || equipmentId < 0) {
            LOG.debug("wifiRadioStatusDbTableUpdate::Cannot get valid CustomerId {} or EquipmentId {} for AP {}",
                    customerId, equipmentId, apId);
            return;
        }

        for (OpensyncAPRadioState radioState : radioStateTables) {
            Equipment ce = equipmentServiceInterface.getByInventoryIdOrNull(apId);
            if (ce == null) {
                LOG.debug("wifiRadioStatusDbTableUpdate::Cannot get Equipment for AP {}", apId);
                return;
            }

            gatewayController.updateActiveCustomer(ce.getCustomerId());

            ApElementConfiguration apElementConfiguration = ((ApElementConfiguration) ce.getDetails());

            if (radioState.getFreqBand().equals(RadioType.UNSUPPORTED)) {
                LOG.debug("Could not get radio configuration for AP {}", apId);
                continue;
            }

            if (radioState.getAllowedChannels() != null) {
                apElementConfiguration = ((ApElementConfiguration) ce.getDetails());
                apElementConfiguration.getRadioMap().get(radioState.getFreqBand())
                        .setAllowedChannels(new ArrayList<>(radioState.getAllowedChannels()));

                LOG.debug("Updated AllowedChannels from Wifi_Radio_State table change for AP {}", apId);

            }

            if (radioState.getTxPower() > 0) {
                apElementConfiguration = ((ApElementConfiguration) ce.getDetails());
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

            ce.setDetails(apElementConfiguration);
            try {
                equipmentServiceInterface.update(ce);
            } catch (DsConcurrentModificationException e) {
                LOG.debug("Equipment reference changed, update instance and retry.", e.getMessage());
                ce = equipmentServiceInterface.getByInventoryIdOrNull(apId);
                ce.setDetails(apElementConfiguration);
                ce = equipmentServiceInterface.update(ce);
            }

            initializeRadioUtilizationReport(customerId, equipmentId, radioState.getFreqBand());
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

            EquipmentProtocolStatusData protocolStatusData = (EquipmentProtocolStatusData) protocolStatus.getDetails();
            protocolStatusData.setReportedCC(CountryCode.valueOf(radioState.getCountry().toLowerCase()));

            try {
                Location location = locationServiceInterface.get(ce.getLocationId());
                if (location != null) {
                    protocolStatusData.setSystemLocation(location.getName());
                }
            } catch (Exception e) {
                LOG.debug("Could not get location {} for customer {} equipment {}", ce.getLocationId(),
                        ce.getCustomerId(), ce.getId());
            }
            protocolStatus.setDetails(protocolStatusData);

            protocolStatus = statusServiceInterface.update(protocolStatus);
        }

    }

    private void initializeRadioUtilizationReport(int customerId, long equipmentId, RadioType radioFreqBand) {
        Status radioUtilizationStatus = statusServiceInterface.getOrNull(customerId, equipmentId,
                StatusDataType.RADIO_UTILIZATION);
        if (radioUtilizationStatus == null) {
            radioUtilizationStatus = new Status();
            radioUtilizationStatus.setCustomerId(customerId);
            radioUtilizationStatus.setEquipmentId(equipmentId);
            radioUtilizationStatus.setStatusDataType(StatusDataType.RADIO_UTILIZATION);
            RadioUtilizationReport radioUtilizationReport = new RadioUtilizationReport();
            radioUtilizationStatus.setDetails(radioUtilizationReport);

            radioUtilizationStatus = statusServiceInterface.update(radioUtilizationStatus);

        }

        RadioUtilizationReport radioUtilizationReport = (RadioUtilizationReport) radioUtilizationStatus.getDetails();

        Map<RadioType, EquipmentPerRadioUtilizationDetails> radioEquipment = radioUtilizationReport
                .getRadioUtilization();
        if (!radioEquipment.containsKey(radioFreqBand)) {
            radioEquipment.put(radioFreqBand, new EquipmentPerRadioUtilizationDetails());
            radioUtilizationReport.setRadioUtilization(radioEquipment);
        }

        Map<RadioType, EquipmentCapacityDetails> capacityDetails = radioUtilizationReport.getCapacityDetails();
        if (!capacityDetails.containsKey(radioFreqBand)) {
            capacityDetails.put(radioFreqBand, new EquipmentCapacityDetails());
            radioUtilizationReport.setCapacityDetails(capacityDetails);
        }

        Map<RadioType, Integer> avgNoiseFloor = radioUtilizationReport.getAvgNoiseFloor();
        if (!avgNoiseFloor.containsKey(radioFreqBand)) {
            avgNoiseFloor.put(radioFreqBand, null);
            radioUtilizationReport.setAvgNoiseFloor(avgNoiseFloor);
        }

        radioUtilizationStatus.setDetails(radioUtilizationReport);

        radioUtilizationStatus = statusServiceInterface.update(radioUtilizationStatus);

    }

    @Override
    public void wifiInetStateDbTableUpdate(List<OpensyncAPInetState> inetStateTables, String apId) {
        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession == null) {
            LOG.debug("wifiInetStateDbTableUpdate::Cannot get Session for AP {}", apId);
            return;
        }

        int customerId = ovsdbSession.getCustomerId();
        long equipmentId = ovsdbSession.getEquipmentId();

        if (customerId < 0 || equipmentId < 0) {
            LOG.debug("wifiInetStateDbTableUpdate::Cannot get valid CustomerId {} or EquipmentId {} for AP {}",
                    customerId, equipmentId, apId);
            return;
        }


        if (inetStateTables == null || inetStateTables.isEmpty() || apId == null) {
            return;
        }

        for (OpensyncAPInetState inetState : inetStateTables) {
            // TODO: implement me
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

        if (customerId < 0 || equipmentId < 0) {
            LOG.debug("wifiAssociatedClientsDbTableUpdate::Cannot get valid CustomerId {} or EquipmentId {} for AP {}",
                    customerId, equipmentId, apId);
            return;
        }

        if (wifiAssociatedClients == null || wifiAssociatedClients.isEmpty() || apId == null) {
            return;
        }

    }

    @Override
    public void awlanNodeDbTableUpdate(OpensyncAWLANNode opensyncAPState, String apId) {

        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession == null) {
            LOG.debug("awlanNodeDbTableUpdate::Cannot get Session for AP {}", apId);
            return;
        }

        int customerId = ovsdbSession.getCustomerId();
        long equipmentId = ovsdbSession.getEquipmentId();

        if (customerId < 0 || equipmentId < 0) {
            LOG.debug("awlanNodeDbTableUpdate::Cannot get valid CustomerId {} or EquipmentId {} for AP {}", customerId,
                    equipmentId, apId);
            return;
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

        EquipmentProtocolStatusData protocolStatusData = (EquipmentProtocolStatusData) protocolStatus.getDetails();
        protocolStatusData.setReportedSku(opensyncAPState.getSkuNumber());
        protocolStatusData.setReportedSwVersion(opensyncAPState.getFirmwareVersion());
        protocolStatusData.setReportedHwVersion(opensyncAPState.getPlatformVersion());
        protocolStatusData.setSystemName(opensyncAPState.getModel());

        protocolStatus.setDetails(protocolStatusData);

        protocolStatus = statusServiceInterface.update(protocolStatus);

    }

    @Override
    public void wifiVIFStateDbTableDelete(List<OpensyncAPVIFState> vifStateTables, String apId) {

        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession == null) {
            LOG.debug("wifiVIFStateDbTableDelete::Cannot get Session for AP {}", apId);
            return;
        }

        int customerId = ovsdbSession.getCustomerId();
        long equipmentId = ovsdbSession.getEquipmentId();

        if (customerId < 0 || equipmentId < 0) {
            LOG.debug("wifiVIFStateDbTableDelete::Cannot get valid CustomerId {} or EquipmentId {} for AP {}",
                    customerId, equipmentId, apId);
            return;
        }

        if (equipmentId < 0L) {
            LOG.debug("Cannot get equipmentId {} for session {}", equipmentId);
            return;
        }

        // TODO: if applicable, remove SsidProfiles related to deleted VIF rows
        // for this AP
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

        if (customerId < 0 || equipmentId < 0) {
            LOG.debug("wifiAssociatedClientsDbTableDelete::Cannot get valid CustomerId {} or EquipmentId {} for AP {}",
                    customerId, equipmentId, apId);
            return;
        }

        // TODO: update activeBSSDs, etc based on associated clients, client
        // information, etc

    }

}
