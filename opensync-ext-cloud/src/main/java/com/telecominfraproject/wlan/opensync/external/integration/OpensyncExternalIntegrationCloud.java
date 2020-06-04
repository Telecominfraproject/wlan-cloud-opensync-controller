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

import com.telecominfraproject.wlan.client.ClientServiceInterface;
import com.telecominfraproject.wlan.cloudeventdispatcher.CloudEventDispatcherInterface;
import com.telecominfraproject.wlan.core.model.entity.CountryCode;
import com.telecominfraproject.wlan.core.model.equipment.AutoOrManualValue;
import com.telecominfraproject.wlan.core.model.equipment.EquipmentType;
import com.telecominfraproject.wlan.core.model.equipment.MacAddress;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.customer.models.Customer;
import com.telecominfraproject.wlan.customer.service.CustomerServiceInterface;
import com.telecominfraproject.wlan.equipment.EquipmentServiceInterface;
import com.telecominfraproject.wlan.equipment.models.ApElementConfiguration;
import com.telecominfraproject.wlan.equipment.models.Equipment;
import com.telecominfraproject.wlan.equipment.models.StateSetting;
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
import com.telecominfraproject.wlan.profile.models.ProfileType;
import com.telecominfraproject.wlan.profile.network.models.ApNetworkConfiguration;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration.SecureMode;
import com.telecominfraproject.wlan.routing.RoutingServiceInterface;
import com.telecominfraproject.wlan.routing.models.EquipmentRoutingRecord;
import com.telecominfraproject.wlan.servicemetric.apnode.models.ApNodeMetrics;
import com.telecominfraproject.wlan.servicemetric.apnode.models.ApPerformance;
import com.telecominfraproject.wlan.servicemetric.apnode.models.EthernetLinkState;
import com.telecominfraproject.wlan.servicemetric.apnode.models.RadioUtilization;
import com.telecominfraproject.wlan.servicemetric.client.models.ClientMetrics;
import com.telecominfraproject.wlan.servicemetric.models.ServiceMetric;
import com.telecominfraproject.wlan.status.StatusServiceInterface;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentAdminStatusData;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentLANStatusData;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentProtocolState;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentProtocolStatusData;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentUpgradeState;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentUpgradeStatusData;
import com.telecominfraproject.wlan.status.equipment.models.VLANStatusData;
import com.telecominfraproject.wlan.status.models.Status;
import com.telecominfraproject.wlan.status.models.StatusCode;
import com.telecominfraproject.wlan.status.models.StatusDataType;
import com.telecominfraproject.wlan.status.network.models.NetworkAdminStatusData;
import com.telecominfraproject.wlan.status.network.models.NetworkAggregateStatusData;
import com.telecominfraproject.wlan.status.network.models.UserDetails;

import sts.PlumeStats.Client;
import sts.PlumeStats.ClientReport;
import sts.PlumeStats.Device;
import sts.PlumeStats.Device.RadioTemp;
import sts.PlumeStats.RadioBandType;
import sts.PlumeStats.Report;
import sts.PlumeStats.Survey;
import sts.PlumeStats.Survey.SurveySample;
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
    private OpensyncCloudGatewayController gatewayController;

    @Value("${connectus.ovsdb.autoProvisionedCustomerId:2}")
    private int autoProvisionedCustomerId;
    @Value("${connectus.ovsdb.autoProvisionedLocationId:8}")
    private int autoProvisionedLocationId;
    @Value("${connectus.ovsdb.autoProvisionedProfileId:1}")
    private int autoProvisionedProfileId;
    @Value("${connectus.ovsdb.autoProvisionedSsid:autoProvisionedSsid}")
    private String autoProvisionedSsid;

    @Autowired
    private CacheManager cacheManagerShortLived;
    private Cache cloudEquipmentRecordCache;
    // private Map<String, OpensyncNode> opensyncNodeMap;

    // @Value("${connectus.ovsdb.configFileName:/Users/mikehansen/git/wlan-cloud-workspace/wlan-cloud-opensync-controller/opensync-ext-cloud/src/main/resources/config_2_ssids.json}")
    // private String configFileName;

    @PostConstruct
    private void postCreate() {
        LOG.info("Using Cloud integration");
        cloudEquipmentRecordCache = cacheManagerShortLived.getCache("equipment_record_cache");
        // opensyncNodeMap = Collections.synchronizedMap(new HashMap<String,
        // OpensyncNode>());
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
        }
        catch (Exception e) {
            LOG.error("Could not get customer equipment for {}", apId, e);
        }

        return ce;
    }

    @Override
    public void apConnected(String apId, ConnectNodeInfo connectNodeInfo) {

        Equipment ce = getCustomerEquipment(apId);

        try {

            if (ce == null) {

                ce = new Equipment();
                ce.setCustomerId(autoProvisionedCustomerId);
                ce.setEquipmentType(EquipmentType.AP);
                ce.setInventoryId(apId);
                ce.setLocationId(autoProvisionedLocationId);

                ce.setProfileId(autoProvisionedProfileId);
                ce.setName(apId);
                ce.setSerial(connectNodeInfo.serialNumber);
                ce.setDetails(ApElementConfiguration.createWithDefaults());

                ce = equipmentServiceInterface.create(ce);
                //
                // ce = new Equipment();
                // ce.setCustomerId(autoProvisionedCustomerId);
                // ce.setInventoryId(apId);
                // ce.setEquipmentType(EquipmentType.AP);
                // ce.setName(apId);
                //
                // ce.setSerial(connectNodeInfo.serialNumber);
                // ApElementConfiguration apElementConfig =
                // ApElementConfiguration.createWithDefaults();
                // apElementConfig.setDeviceName(apId);
                // ce.setDetails(apElementConfig);
                // ce.setLocationId(autoProvisionedLocationId);
                // ce.setProfileId(autoProvisionedProfileId);

                // ce = equipmentServiceInterface.create(ce);
            }

            Profile apProfile = profileServiceInterface.getOrNull(ce.getProfileId());

            if (apProfile == null || !apProfile.getProfileType().equals(ProfileType.equipment_ap)) {
                apProfile = new Profile();
                apProfile.setCustomerId(ce.getCustomerId());
                apProfile.setName("autoprovisionedApProfile");
                apProfile.setDetails(ApNetworkConfiguration.createWithDefaults());
                apProfile = profileServiceInterface.create(apProfile);

                Profile ssidProfile = new Profile();
                ssidProfile.setCustomerId(ce.getCustomerId());
                ssidProfile.setName(autoProvisionedSsid);
                SsidConfiguration ssidConfig = SsidConfiguration.createWithDefaults();
                Set<RadioType> appliedRadios = new HashSet<>();
                appliedRadios.addAll(((ApElementConfiguration) ce.getDetails()).getRadioMap().keySet());
                ssidConfig.setAppliedRadios(appliedRadios);
                ssidProfile.setDetails(ssidConfig);
                ssidProfile = profileServiceInterface.create(ssidProfile);

                Set<Long> childProfileIds = new HashSet<>();
                childProfileIds.add(ssidProfile.getId());

                apProfile.setChildProfileIds(childProfileIds);

                apProfile = profileServiceInterface.update(apProfile);

                // update AP only if the apProfile was missing
                ce.setProfileId(apProfile.getId());
                ce = equipmentServiceInterface.update(ce);
            }

            Customer customer = customerServiceInterface.getOrNull(ce.getCustomerId());
            if (customer == null) {
                customer = new Customer();
                customer.setId(autoProvisionedCustomerId);
                customerServiceInterface.create(customer);
                ce.setCustomerId(customer.getId());
                equipmentServiceInterface.update(ce);
            }

            updateApStatus(ce, connectNodeInfo);

            EquipmentRoutingRecord equipmentRoutingRecord = gatewayController.registerCustomerEquipment(ce.getName(),
                    ce.getCustomerId(), ce.getId());

            OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);
            ovsdbSession.setRoutingId(equipmentRoutingRecord.getId());
            ovsdbSession.setEquipmentId(ce.getId());
            ovsdbSession.setCustomerId(ce.getCustomerId());

            LOG.debug("Equipment {}", ce);
            LOG.info("AP {} got connected to the gateway", apId);
            LOG.info("ConnectNodeInfo {}", connectNodeInfo);

        }
        catch (Exception e) {
            LOG.error("Could not process connection from AP {}", apId, e);
            throw e;
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
            }
            catch (UnknownHostException e) {
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
            Status networkAdminStatusRec = statusServiceInterface.getOrNull(ce.getCustomerId(), 0,
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

            statusServiceInterface.update(networkAdminStatusRec);

            Status networkAggStatusRec = statusServiceInterface.getOrNull(ce.getCustomerId(), 0,
                    StatusDataType.NETWORK_AGGREGATE);
            if (networkAggStatusRec == null) {
                networkAggStatusRec = new Status();
                networkAggStatusRec.setCustomerId(ce.getCustomerId());
                NetworkAggregateStatusData naStatusData = new NetworkAggregateStatusData();
                networkAggStatusRec.setDetails(naStatusData);
            }

            networkAggStatusRec.setEquipmentId(ce.getId());

            statusServiceInterface.update(networkAggStatusRec);

        }
        catch (Exception e) {
            // do nothing
            LOG.debug("Exception in updateApStatus", e);
        }

    }

    @Override
    public void apDisconnected(String apId) {
        LOG.info("AP {} got disconnected from the gateway", apId);
        try {
            // removed the 'in-memory' cached node
            // synchronized (opensyncNodeMap) {
            // if (opensyncNodeMap.containsKey(apId)) {
            // opensyncNodeMap.remove(apId);
            // LOG.info("AP {} and table state data removed from memory cache",
            // apId);
            // }
            // }

            OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

            if (ovsdbSession != null) {
                if (ovsdbSession.getRoutingId() > 0L) {
                    try {
                        routingServiceInterface.delete(ovsdbSession.getRoutingId());
                    }
                    catch (Exception e) {
                        LOG.warn("Unable to delete routing service Id {} for ap {}. {}", ovsdbSession.getRoutingId(),
                                apId, e);
                    }
                }
            }
            else {
                LOG.warn("Cannot find ap {} in inventory", apId);
            }
        }
        catch (Exception e) {
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

            Equipment equipmentConfig = getCustomerEquipment(apId);

            if (equipmentConfig == null) {
                throw new IllegalStateException("Cannot retrieve configuration for " + apId);
            }

            ret = new OpensyncAPConfig();

            ret.setCustomerEquipment(equipmentConfig);

            Location eqLocation = locationServiceInterface.get(equipmentConfig.getLocationId());

            ret.setEquipmentLocation(eqLocation);

            // CountryCode countryCode = Location.getCountryCode(eqLocation);
            // if (countryCode != null && countryCode !=
            // CountryCode.UNSUPPORTED) {
            // countryCode.toString().toUpperCase();
            // }
            //
            // ApElementConfiguration apElementConfiguration =
            // (ApElementConfiguration) equipmentConfig.getDetails();

            // Map<RadioType, ElementRadioConfiguration> erc =
            // apElementConfiguration.getRadioMap();
            // if (erc != null) {
            //
            // ElementRadioConfiguration erc24 = erc.get(RadioType.is2dot4GHz);
            // ElementRadioConfiguration erc5gl = erc.get(RadioType.is5GHzL);
            // ElementRadioConfiguration erc5gh = erc.get(RadioType.is5GHzU);
            //
            // if (erc24 != null) {
            // erc24.getChannelNumber();
            // }
            //
            // if (erc5gl != null) {
            // erc5gl.getChannelNumber();
            // }
            //
            // if (erc5gh != null) {
            // erc5gh.getChannelNumber();
            // }
            //
            // }

            com.telecominfraproject.wlan.profile.models.Profile apProfile = profileServiceInterface
                    .getOrNull(equipmentConfig.getProfileId());

            ret.setApProfile(apProfile);

            if (apProfile != null) {
                List<com.telecominfraproject.wlan.profile.models.Profile> ssidProfiles = new ArrayList<>();

                Set<Long> childProfileIds = apProfile.getChildProfileIds();
                for (Long id : childProfileIds) {
                    com.telecominfraproject.wlan.profile.models.Profile profile = profileServiceInterface.get(id);
                    if (profile.getProfileType().equals(ProfileType.ssid)) {

                        SsidConfiguration ssidConfig = ((SsidConfiguration) profile.getDetails());
                        ssidConfig.setSsid(autoProvisionedSsid);
                        if (ssidConfig.getSecureMode().equals(SecureMode.open)) {
                            ssidConfig.setSecureMode(SecureMode.wpa2PSK);
                            ssidConfig.setKeyStr("12345678");
                        }
                        profile.setDetails(ssidConfig);
                        profile = profileServiceInterface.update(profile);

                        ssidProfiles.add(profile);

                    }
                }
                ret.setSsidProfile(ssidProfiles);

            }

            LOG.debug("Config content : Equipment {}", ret.getCustomerEquipment());
            LOG.debug("Config content : APProfile {}", ret.getApProfile());
            ret.getSsidProfile().stream().forEach(ssid -> LOG.debug("Config content : SSIDProfile {}", ssid));
            LOG.debug("Config content : Location {}", ret.getEquipmentLocation());

        }
        catch (Exception e) {
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

        List<ServiceMetric> metricRecordList = new ArrayList<>();

        populateApClientMetrics(metricRecordList, report, customerId, equipmentId);
        populateApNodeMetrics(metricRecordList, report, customerId, equipmentId);

        try {
            populateApSsidMetrics(metricRecordList, report, customerId, equipmentId, extractApIdFromTopic(topic));
        }
        catch (Exception e) {
            LOG.error("Exception when processing populateApSsidMetrics", e);
        }

        if (!metricRecordList.isEmpty()) {
            equipmentMetricsCollectorInterface.publishMetrics(metricRecordList);
        }

    }

    private void populateApNodeMetrics(List<ServiceMetric> metricRecordList, Report report, int customerId,
            long equipmentId) {
        {
            LOG.debug("populateApNodeMetrics for Customer {} Equipment {}", customerId, equipmentId);
            ApNodeMetrics apNodeMetrics = null;

            for (Device deviceReport : report.getDeviceList()) {

                ServiceMetric smr = new ServiceMetric(customerId, equipmentId);
                metricRecordList.add(smr);

                apNodeMetrics = new ApNodeMetrics();
                smr.setDetails(apNodeMetrics);
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
                    apPerformance
                            .setCpuUtilized(new byte[] { (byte) deviceReport.getCpuUtil().getCpuUtil(), (byte) 0 });
                }

                apPerformance.setEthLinkState(EthernetLinkState.UP1000_FULL_DUPLEX);

                if (deviceReport.hasMemUtil() && deviceReport.getMemUtil().hasMemTotal()
                        && deviceReport.getMemUtil().hasMemUsed()) {
                    apPerformance.setFreeMemory(
                            deviceReport.getMemUtil().getMemTotal() - deviceReport.getMemUtil().getMemUsed());
                }
                apPerformance.setUpTime((long) deviceReport.getUptime());
            }
            if (apNodeMetrics != null) {

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
                long rxBytes2g = 0;
                long txBytes2g = 0;

                long rxBytes5g = 0;
                long txBytes5g = 0;

                for (ClientReport clReport : report.getClientsList()) {
                    for (Client cl : clReport.getClientListList()) {
                        if (clReport.getBand() == RadioBandType.BAND2G) {
                            if (cl.getStats().hasTxBytes()) {
                                txBytes2g += cl.getStats().getTxBytes();
                            }
                            if (cl.getStats().hasRxBytes()) {
                                rxBytes2g += cl.getStats().getRxBytes();
                            }
                        }
                        else {
                            if (cl.getStats().hasTxBytes()) {
                                txBytes5g += cl.getStats().getTxBytes();
                            }
                            if (cl.getStats().hasRxBytes()) {
                                rxBytes5g += cl.getStats().getRxBytes();
                            }
                        }
                    }
                }

                apNodeMetrics.setRxBytes(RadioType.is2dot4GHz, rxBytes2g);
                apNodeMetrics.setTxBytes(RadioType.is2dot4GHz, txBytes2g);
                apNodeMetrics.setRxBytes(RadioType.is5GHz, rxBytes5g);
                apNodeMetrics.setTxBytes(RadioType.is5GHz, txBytes5g);
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

                // TODO: temporary solution as this was causing Noise Floor to
                // disappear from Dashboard and Access Point rows
                apNodeMetrics.setNoiseFloor(RadioType.is2dot4GHz, -98);
                apNodeMetrics.setNoiseFloor(RadioType.is5GHz, -98);

                apNodeMetrics.setRadioUtilization(RadioType.is2dot4GHz, new ArrayList<>());
                apNodeMetrics.setRadioUtilization(RadioType.is5GHz, new ArrayList<>());

                // populate it from report.survey
                for (Survey survey : report.getSurveyList()) {
                    // int oBSS = 0;
                    // int iBSS = 0;
                    // int totalBusy = 0;
                    // int durationMs = 0;
                    for (SurveySample surveySample : survey.getSurveyListList()) {
                        if (surveySample.getDurationMs() == 0) {
                            continue;
                        }

                        // iBSS += surveySample.getBusySelf() +
                        // surveySample.getBusyTx();
                        // oBSS += surveySample.getBusyRx();
                        // totalBusy += surveySample.getBusy();
                        // durationMs += surveySample.getDurationMs();

                        RadioUtilization radioUtil = new RadioUtilization();
                        radioUtil.setTimestampSeconds(
                                (int) ((survey.getTimestampMs() + surveySample.getOffsetMs()) / 1000));
                        radioUtil.setAssocClientTx(100 * surveySample.getBusyTx() / surveySample.getDurationMs());
                        radioUtil.setAssocClientRx(100 * surveySample.getBusyRx() / surveySample.getDurationMs());
                        radioUtil.setNonWifi(
                                100 * (surveySample.getBusy() - surveySample.getBusyTx() - surveySample.getBusyRx())
                                        / surveySample.getDurationMs());
                        if (survey.getBand() == RadioBandType.BAND2G) {
                            apNodeMetrics.getRadioUtilization(RadioType.is2dot4GHz).add(radioUtil);
                        }
                        else {
                            apNodeMetrics.getRadioUtilization(RadioType.is5GHz).add(radioUtil);
                        }

                    }

                    // Double totalUtilization = 100D * totalBusy / durationMs;
                    // LOG.trace("Total Utilization {}", totalUtilization);
                    // Double totalWifiUtilization = 100D * (iBSS + oBSS) /
                    // durationMs;
                    // LOG.trace("Total Wifi Utilization {}",
                    // totalWifiUtilization);
                    // LOG.trace("Total Non-Wifi Utilization {}",
                    // totalUtilization -
                    // totalWifiUtilization);
                    // if (survey.getBand() == RadioBandType.BAND2G) {
                    // data.setChannelUtilization2G(totalUtilization.intValue());
                    // } else {
                    // data.setChannelUtilization5G(totalUtilization.intValue());
                    // }
                }

            }
            LOG.debug("ApNodeMetrics Report {}", apNodeMetrics);

        }

    }

    private void populateApClientMetrics(List<ServiceMetric> metricRecordList, Report report, int customerId,
            long equipmentId) {
        LOG.debug("populateApClientMetrics for Customer {} Equipment {}", customerId, equipmentId);

        for (ClientReport clReport : report.getClientsList()) {

            for (Client cl : clReport.getClientListList()) {

                if (cl.getMacAddress() == null) {
                    LOG.debug(
                            "No mac address for Client {}, cannot set device mac address for client in ClientMetrics.",
                            cl);
                    continue;
                }

                ServiceMetric smr = new ServiceMetric(customerId, equipmentId, new MacAddress(cl.getMacAddress()));
                metricRecordList.add(smr);

                smr.setCreatedTimestamp(clReport.getTimestampMs());

                // clReport.getChannel();
                ClientMetrics cMetrics = new ClientMetrics();
                smr.setDetails(cMetrics);

                Integer periodLengthSec = 60; // matches what's configured by
                // OvsdbDao.configureStats(OvsdbClient)
                cMetrics.setPeriodLengthSec(periodLengthSec);

                RadioType radioType = RadioType.UNSUPPORTED;
                switch (clReport.getBand()) {
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
                cMetrics.setRadioType(radioType);

                if (cl.hasStats()) {
                    if (cl.getStats().hasRssi()) {
                        cMetrics.setRssi(cl.getStats().getRssi());
                    }

                    // we'll report each device as having a single (very long)
                    // session
                    cMetrics.setSessionId(smr.getClientMac());

                    // populate Rx stats
                    if (cl.getStats().hasRxBytes()) {
                        cMetrics.setRxBytes(cl.getStats().getRxBytes());
                    }

                    if (cl.getStats().hasRxRate()) {
                        // cMetrics.setAverageRxRate(cl.getStats().getRxRate());
                    }

                    if (cl.getStats().hasRxErrors()) {
                        cMetrics.setNumRxNoFcsErr((int) cl.getStats().getRxErrors());
                    }

                    if (cl.getStats().hasRxFrames()) {
                        // cMetrics.setNumRxFramesReceived(cl.getStats().getRxFrames());
                        cMetrics.setNumRxPackets(cl.getStats().getRxFrames());
                    }

                    if (cl.getStats().hasRxRetries()) {
                        cMetrics.setNumRxRetry((int) cl.getStats().getRxRetries());
                    }

                    // populate Tx stats
                    if (cl.getStats().hasTxBytes()) {
                        cMetrics.setNumTxBytes(cl.getStats().getTxBytes());
                    }

                    if (cl.getStats().hasTxRate()) {
                        // cMetrics.setAverageTxRate(cl.getStats().getTxRate());
                    }

                    if (cl.getStats().hasTxRate() && cl.getStats().hasRxRate()) {
                        cMetrics.setRates(
                                new byte[] { (byte) cl.getStats().getTxRate(), (byte) cl.getStats().getRxRate() });
                    }

                    if (cl.getStats().hasTxErrors()) {
                        cMetrics.setNumTxDropped((int) cl.getStats().getTxErrors());
                    }

                    if (cl.getStats().hasRxFrames()) {
                        // cMetrics.setNumTxFramesTransmitted(cl.getStats().getTxFrames());
                        cMetrics.setNumTxPackets(cl.getStats().getRxFrames());
                    }

                    if (cl.getStats().hasTxRetries()) {
                        cMetrics.setNumTxDataRetries((int) cl.getStats().getTxRetries());
                    }

                }

                LOG.debug("APClientMetrics Report {}", cMetrics);

            }

        }

    }

    private void populateApSsidMetrics(List<ServiceMetric> metricRecordList, Report report, int customerId,
            long equipmentId, String apId) {

        if (report.getClientsCount() == 0) {
            LOG.debug("No clients reported, will not populate ApSsidMetrics report");
            return;
        }
        else {
            LOG.debug("populateApSsidMetrics for Customer {} Equipment {} AP {}", customerId, equipmentId, apId);
        }

        // TODO: implement, using information from status and config to
        // populate/correlate AP data with the Client SSID data

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

        Equipment ce = getCustomerEquipment(apId);
        if (ce == null) {
            LOG.debug("wifiVIFStateDbTableUpdate::Cannot get Equipment for AP {}", apId);
            return;
        }

        long equipmentId = ce.getId();

        if (equipmentId < 0L) {
            LOG.debug("Cannot get equipmentId {} for session {}", equipmentId);
            return;
        }

        if (vifStateTables == null || vifStateTables.isEmpty() || apId == null) {
            return;
        }

        // TODO: update Equipment and Profiles related to VIF_State table update

    }

    @Override
    public void wifiRadioStatusDbTableUpdate(List<OpensyncAPRadioState> radioStateTables, String apId) {

        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession == null) {
            LOG.debug("wifiRadioStatusDbTableUpdate::Cannot get Session for AP {}", apId);
            return;
        }

        Equipment ce = equipmentServiceInterface.getByInventoryIdOrNull(apId);
        if (ce == null) {
            LOG.debug("wifiRadioStatusDbTableUpdate::Cannot get Equipment for AP {}", apId);
            return;
        }

        for (OpensyncAPRadioState radioState : radioStateTables) {

            if (radioState.getFreqBand().equals(RadioType.UNSUPPORTED)) {
                LOG.debug("Could not get radio configuration for AP {}", apId);
                continue;
            }

            if (radioState.getAllowedChannels() != null && !radioState.getAllowedChannels().isEmpty()) {
                ApElementConfiguration apElementConfiguration = ((ApElementConfiguration) ce.getDetails());
                apElementConfiguration.getRadioMap().get(radioState.getFreqBand())
                        .setAllowedChannels(new ArrayList<>(radioState.getAllowedChannels()));
                ce.setDetails(apElementConfiguration);
                ce = equipmentServiceInterface.update(ce);
                LOG.debug("Updated AllowedChannels from Wifi_Radio_State table change {}", ce);

            }

            if (radioState.getTxPower() > 0) {
                ApElementConfiguration apElementConfiguration = ((ApElementConfiguration) ce.getDetails());
                apElementConfiguration.getRadioMap().get(radioState.getFreqBand())
                        .setEirpTxPower(AutoOrManualValue.createManualInstance(radioState.getTxPower()));
                ce.setDetails(apElementConfiguration);
                ce = equipmentServiceInterface.update(ce);
                LOG.debug("Updated TxPower from Wifi_Radio_State table change {}", ce);

            }

            StateSetting state = StateSetting.disabled;
            if (radioState.isEnabled()) {
                state = StateSetting.enabled;
            }
            ApElementConfiguration apElementConfiguration = ((ApElementConfiguration) ce.getDetails());
            if (!apElementConfiguration.getAdvancedRadioMap().get(radioState.getFreqBand()).getRadioAdminState()
                    .equals(state)) {
                // only update if changed
                apElementConfiguration.getAdvancedRadioMap().get(radioState.getFreqBand()).setRadioAdminState(state);
                ce.setDetails(apElementConfiguration);
                ce = equipmentServiceInterface.update(ce);

                LOG.debug("Updated RadioAdminState from Wifi_Radio_State table change {}", ce);

            }

        }

    }

    @Override
    public void wifiInetStateDbTableUpdate(List<OpensyncAPInetState> inetStateTables, String apId) {
        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession == null) {
            LOG.debug("wifiInetStateDbTableUpdate::Cannot get Session for AP {}", apId);
            return;
        }

        Equipment ce = equipmentServiceInterface.getByInventoryIdOrNull(apId);
        if (ce == null) {
            LOG.debug("wifiInetStateDbTableUpdate::Cannot get Equipment for AP {}", apId);
            return;
        }

        if (inetStateTables == null || inetStateTables.isEmpty() || apId == null) {
            return;
        }

        // TODO: update Config/Status for Network from Wifi_Inet_State where
        // applicable

    }

    @Override
    public void wifiAssociatedClientsDbTableUpdate(List<OpensyncWifiAssociatedClients> wifiAssociatedClients,
            String apId) {

        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession == null) {
            LOG.debug("wifiAssociatedClientsDbTableUpdate::Cannot get Session for AP {}", apId);
            return;
        }

        Equipment ce = equipmentServiceInterface.getByInventoryIdOrNull(apId);
        if (ce == null) {
            LOG.debug("wifiAssociatedClientsDbTableUpdate::Cannot get Equipment for AP {}", apId);
            return;
        }

        long equipmentId = ce.getId();

        if (equipmentId < 0L) {
            LOG.debug("Cannot get equipmentId {} for session {}", equipmentId);
            return;
        }

        if (wifiAssociatedClients == null || wifiAssociatedClients.isEmpty() || apId == null) {
            return;
        }

        // TODO: update activeBSSDs, etc based on associated clients

    }

    @Override
    public void awlanNodeDbTableUpdate(OpensyncAWLANNode opensyncAPState, String apId) {

        Equipment ce = getCustomerEquipment(apId);
        if (ce == null) {
            LOG.debug("awlanNodeDbTableUpdate::Cannot get Equipment for AP {}", apId);
            return;
        }

        long equipmentId = ce.getId();

        if (equipmentId < 0L) {
            LOG.debug("awlanNodeDbTableUpdate::Cannot get equipmentId {} for session {}", equipmentId);
            return;
        }

        // TODO: update config where applicable
    }

    @Override
    public void wifiVIFStateDbTableDelete(List<OpensyncAPVIFState> vifStateTables, String apId) {

        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if (ovsdbSession == null) {
            LOG.debug("wifiVIFStateDbTableDelete::Cannot get Session for AP {}", apId);
            return;
        }

        Equipment ce = equipmentServiceInterface.getByInventoryIdOrNull(apId);
        if (ce == null) {
            LOG.debug("wifiVIFStateDbTableDelete::Cannot get Equipment for AP {}", apId);
            return;
        }

        long equipmentId = ce.getId();

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

        Equipment ce = equipmentServiceInterface.getByInventoryIdOrNull(apId);
        if (ce == null) {
            LOG.debug("wifiAssociatedClientsDbTableDelete::Cannot get Equipment for AP {}", apId);
            return;
        }
        long equipmentId = ce.getId();

        if (equipmentId < 0L) {
            LOG.debug("Cannot get equipmentId {} for session {}", equipmentId);
            return;
        }

        // TODO: update activeBSSDs, etc based on associated clients, client
        // information, etc

    }

}
