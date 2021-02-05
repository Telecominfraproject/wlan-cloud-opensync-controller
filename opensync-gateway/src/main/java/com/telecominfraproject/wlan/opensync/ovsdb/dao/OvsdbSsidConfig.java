package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.core.model.equipment.MacAddress;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.equipment.models.ApElementConfiguration;
import com.telecominfraproject.wlan.equipment.models.NetworkForwardMode;
import com.telecominfraproject.wlan.equipment.models.RadioConfiguration;
import com.telecominfraproject.wlan.equipment.models.RadioMode;
import com.telecominfraproject.wlan.equipment.models.StateSetting;
import com.telecominfraproject.wlan.opensync.external.integration.models.ConnectNodeInfo;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.WifiRadioConfigInfo;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.WifiVifConfigInfo;
import com.telecominfraproject.wlan.profile.bonjour.models.BonjourGatewayProfile;
import com.telecominfraproject.wlan.profile.bonjour.models.BonjourServiceSet;
import com.telecominfraproject.wlan.profile.captiveportal.models.CaptivePortalAuthenticationType;
import com.telecominfraproject.wlan.profile.captiveportal.models.CaptivePortalConfiguration;
import com.telecominfraproject.wlan.profile.models.Profile;
import com.telecominfraproject.wlan.profile.models.common.ManagedFileInfo;
import com.telecominfraproject.wlan.profile.network.models.ApNetworkConfiguration;
import com.telecominfraproject.wlan.profile.network.models.GreTunnelConfiguration;
import com.telecominfraproject.wlan.profile.radius.models.RadiusProfile;
import com.telecominfraproject.wlan.profile.radius.models.RadiusServer;
import com.telecominfraproject.wlan.profile.rf.models.RfConfiguration;
import com.telecominfraproject.wlan.profile.rf.models.RfElementConfiguration;
import com.telecominfraproject.wlan.profile.ssid.models.NasIdType;
import com.telecominfraproject.wlan.profile.ssid.models.NasIpType;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration;
import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.protocol.operation.Delete;
import com.vmware.ovsdb.protocol.operation.Insert;
import com.vmware.ovsdb.protocol.operation.Operation;
import com.vmware.ovsdb.protocol.operation.Select;
import com.vmware.ovsdb.protocol.operation.Update;
import com.vmware.ovsdb.protocol.operation.notation.Atom;
import com.vmware.ovsdb.protocol.operation.notation.Condition;
import com.vmware.ovsdb.protocol.operation.notation.Function;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;
import com.vmware.ovsdb.protocol.operation.notation.Value;
import com.vmware.ovsdb.protocol.operation.result.ErrorResult;
import com.vmware.ovsdb.protocol.operation.result.InsertResult;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.protocol.operation.result.SelectResult;
import com.vmware.ovsdb.protocol.operation.result.UpdateResult;
import com.vmware.ovsdb.service.OvsdbClient;

@Component
public class OvsdbSsidConfig extends OvsdbDaoBase {

    @Autowired
    OvsdbGet getProvisionedData;
    @Autowired
    OvsdbRadioConfig radioConfig;
    @Autowired
    OvsdbNetworkConfig networkConfig;
    @Autowired
    OvsdbNode ovsdbNode;

    protected void getEnabledRadios(OvsdbClient ovsdbClient, List<RadioType> radios) {
        try {
            List<Operation> operations = new ArrayList<>();
            List<Condition> conditions = new ArrayList<>();
            List<String> columns = new ArrayList<>();

            columns.add("freq_band");

            conditions.add(new Condition("enabled", Function.EQUALS, new Atom<>(true)));

            operations.add(new Select(wifiRadioStateDbTable, conditions, columns));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Select from {}:", wifiRadioStateDbTable);

                for (OperationResult res : result) {
                    LOG.debug("Op Result {}", res);
                }
            }

            if ((result != null) && (result.length > 0) && (result[0] instanceof SelectResult)
                    && !((SelectResult) result[0]).getRows().isEmpty()) {

                for (Row row : ((SelectResult) result[0]).getRows()) {

                    String radioFrequencyBand = getSingleValueFromSet(row, "freq_band");
                    switch (radioFrequencyBand) {
                    case "2.4G":
                        radios.add(RadioType.is2dot4GHz);
                        break;
                    case "5G":
                        radios.add(RadioType.is5GHz);
                        break;
                    case "5GL":
                        radios.add(RadioType.is5GHzL);
                        break;
                    case "5GU":
                        radios.add(RadioType.is5GHzU);
                        break;
                    default:
                        LOG.debug("Unsupported or unrecognized radio band type {}", radioFrequencyBand);

                    }

                }

            }

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    void configureBlockList(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig, List<MacAddress> macBlockList) {

        LOG.debug("Starting configureBlockList {}", macBlockList);

        List<Operation> operations = new ArrayList<>();
        Map<String, Value> updateColumns = new HashMap<>();

        try {
            updateBlockList(updateColumns, macBlockList);

            Row row = new Row(updateColumns);
            List<Condition> conditions = new ArrayList<>(); // No condition,
            // apply all ssid
            operations.add(new Update(wifiVifConfigDbTable, conditions, row));

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            for (OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }

            LOG.debug("Provisioned blockList {}", macBlockList);

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in configureSingleSsid", e);
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @param enable80211k
     * @param dtimPeriod
     * @param fragThresholdBytes
     *            TODO
     * @param customOptions
     */
    void configureCustomOptionsForDtimFragAnd80211k(boolean enable80211k, int dtimPeriod, int fragThresholdBytes,
            Map<String, String> customOptions) {

        // TODO: the frag_threshold is not supported on the AP
        // customOptions.put("frag_threshold",
        // String.valueOf(fragThresholdBytes));

        customOptions.put("dtim_period", String.valueOf(dtimPeriod));

        if (enable80211k) {
            customOptions.put("ieee80211k", String.valueOf(1));
        } else {
            customOptions.put("ieee80211k", String.valueOf(0));
        }
    }

    /**
     *
     * @param ovsdbClient
     * @param radiusNasId
     * @param radiusNasIp
     * @param radiusOperatorName
     * @param customOptions
     */
    void configureCustomOptionsForRadiusNas(OvsdbClient ovsdbClient, String radiusNasId, String radiusNasIp,
            String radiusOperatorName, Map<String, String> customOptions) {
        ConnectNodeInfo partialConnectNode = new ConnectNodeInfo();
        ovsdbNode.fillInWanIpAddressAndMac(ovsdbClient, partialConnectNode, defaultWanInterfaceType,
                defaultWanInterfaceName);

        if (radiusNasId != null) {
            if (radiusNasId.equals(NasIdType.DEFAULT.toString())) {
                customOptions.put("radius_nas_id", partialConnectNode.macAddress);
            } else {
                customOptions.put("radius_nas_id", radiusNasId);
            }
        }
        if (radiusNasIp != null) {
            if (radiusNasIp.equals(NasIpType.WAN_IP.toString())) {
                customOptions.put("radius_nas_ip", partialConnectNode.ipV4Address);
            } else {
                customOptions.put("radius_nas_ip", radiusNasIp);
            }
        }
        if (radiusOperatorName != null) {
            customOptions.put("radius_oper_name", radiusOperatorName);
        }
    }

    /**
     *
     * @param rateLimitEnable
     * @param ssidDlLimit
     * @param ssidUlLimit
     * @param clientDlLimit
     * @param clientUlLimit
     * @param rtsCtsThreshold
     * @param customOptions
     */
    void configureCustomOptionsForRatesAndLimits(boolean rateLimitEnable, int ssidDlLimit, int ssidUlLimit,
            int clientDlLimit, int clientUlLimit, int rtsCtsThreshold, Map<String, String> customOptions) {
        customOptions.put("rate_limit_en", rateLimitEnable ? "1" : "0");
        customOptions.put("ssid_ul_limit", String.valueOf(ssidUlLimit * 1000));
        customOptions.put("ssid_dl_limit", String.valueOf(ssidDlLimit * 1000));
        customOptions.put("client_dl_limit", String.valueOf(clientDlLimit * 1000));
        customOptions.put("client_ul_limit", String.valueOf(clientUlLimit * 1000));
        customOptions.put("rts_threshold", String.valueOf(rtsCtsThreshold));
    }

    /**
     * Populate the various <K,V> fields in the custom_options column of the
     * Wifi_VIF_Config ovsdb table.
     *
     * @param ovsdbClient
     * @param enable80211k
     * @param rateLimitEnable
     * @param ssidDlLimit
     * @param ssidUlLimit
     * @param clientDlLimit
     * @param clientUlLimit
     * @param rtsCtsThreshold
     * @param fragThresholdBytes
     *            TODO
     * @param dtimPeriod
     * @param radiusNasId
     * @param radiusNasIp
     * @param radiusOperatorName
     * @param updateColumns
     *            TODO
     */
    void configureCustomOptionsForSsid(OvsdbClient ovsdbClient, boolean enable80211k, boolean rateLimitEnable,
            int ssidDlLimit, int ssidUlLimit, int clientDlLimit, int clientUlLimit, int rtsCtsThreshold,
            int fragThresholdBytes, int dtimPeriod, String radiusNasId, String radiusNasIp, String radiusOperatorName,
            Map<String, Value> updateColumns) {
        Map<String, String> customOptions = new HashMap<>();
        configureCustomOptionsForRatesAndLimits(rateLimitEnable, ssidDlLimit, ssidUlLimit, clientDlLimit, clientUlLimit,
                rtsCtsThreshold, customOptions);

        configureCustomOptionsForRadiusNas(ovsdbClient, radiusNasId, radiusNasIp, radiusOperatorName, customOptions);

        configureCustomOptionsForDtimFragAnd80211k(enable80211k, dtimPeriod, fragThresholdBytes, customOptions);

        @SuppressWarnings("unchecked")
        com.vmware.ovsdb.protocol.operation.notation.Map<String, String> customMap = com.vmware.ovsdb.protocol.operation.notation.Map
                .of(customOptions);
        updateColumns.put("custom_options", customMap);
    }

    Uuid configureSingleSsid(OvsdbClient ovsdbClient, String vifInterfaceName, String ssid, boolean ssidBroadcast,
            Map<String, String> security, String radioFreqBand, int vlanId, boolean rrmEnabled, boolean enable80211r,
            int mobilityDomain, boolean enable80211v, boolean enable80211k, String minHwMode, boolean enabled,
            int keyRefresh, boolean uapsdEnabled, boolean apBridge, NetworkForwardMode networkForwardMode,
            String gateway, String inet, Map<String, String> dns, String ipAssignScheme, List<MacAddress> macBlockList,
            boolean rateLimitEnable, int ssidDlLimit, int ssidUlLimit, int clientDlLimit, int clientUlLimit,
            int rtsCtsThreshold, int fragThresholdBytes, int dtimPeriod, Map<String, String> captiveMap,
            List<String> walledGardenAllowlist, Map<Short, Set<String>> bonjourServiceMap, String radiusNasId,
            String radiusNasIp, String radiusOperatorName, String greTunnelName) {

        List<Operation> operations = new ArrayList<>();
        Map<String, Value> updateColumns = new HashMap<>();

        try {

            // If we are doing a NAT SSID, no bridge, else yes
            // If gre tunnel and vlanId > 1 use vlan if name for bridge
            String bridgeInterfaceName = defaultWanInterfaceName;
            if (greTunnelName != null && vlanId > 1) {
                bridgeInterfaceName = greTunnelName + "_" + String.valueOf(vlanId);
            } else if (networkForwardMode == NetworkForwardMode.NAT) {
                bridgeInterfaceName = defaultLanInterfaceName;
            }

            if (vlanId > 1) {
                updateColumns.put("vlan_id", new Atom<>(vlanId));
            } else {
                updateColumns.put("vlan_id", new Atom<>(1));
            }

            updateColumns.put("mode", new Atom<>("ap"));
            @SuppressWarnings("unchecked")
            com.vmware.ovsdb.protocol.operation.notation.Map<String, String> captivePortalMap = com.vmware.ovsdb.protocol.operation.notation.Map
                    .of(captiveMap);
            updateColumns.put("captive_portal", captivePortalMap);

            if (walledGardenAllowlist != null && !walledGardenAllowlist.isEmpty()) {
                Set<Atom<String>> atomMacList = new HashSet<>();
                walledGardenAllowlist.stream().forEach(allow -> atomMacList.add(new Atom<>(allow)));
                com.vmware.ovsdb.protocol.operation.notation.Set allowListSet = com.vmware.ovsdb.protocol.operation.notation.Set
                        .of(atomMacList);
                updateColumns.put("captive_allowlist", allowListSet);
            } else {
                updateColumns.put("captive_allowlist", new com.vmware.ovsdb.protocol.operation.notation.Set());
            }

            // TODO: when AP support for Bonjour Gateway set values
            if (bonjourServiceMap != null && bonjourServiceMap.size() > 0) {
                LOG.info("SSID {} Bonjour Services per vlan {}", ssid, bonjourServiceMap);
            }

            updateColumns.put("bridge", new Atom<>(bridgeInterfaceName));

            if (enable80211v) {
                updateColumns.put("btm", new Atom<>(1));
            } else {
                updateColumns.put("btm", new Atom<>(0));
            }
            updateColumns.put("enabled", new Atom<>(enabled));
            if (enable80211r) {
                updateColumns.put("ft_psk", new Atom<>(1));
                updateColumns.put("ft_mobility_domain", new Atom<>(mobilityDomain));
            } else {
                updateColumns.put("ft_psk", new Atom<>(0));
                updateColumns.put("ft_mobility_domain", new com.vmware.ovsdb.protocol.operation.notation.Set());
            }
            updateColumns.put("if_name", new Atom<>(vifInterfaceName));
            updateColumns.put("rrm", new Atom<>(rrmEnabled ? 1 : 0));
            updateColumns.put("ssid", new Atom<>(ssid));
            updateColumns.put("ssid_broadcast", new Atom<>(ssidBroadcast ? "enabled" : "disabled"));
            updateColumns.put("uapsd_enable", new Atom<>(uapsdEnabled));

            updateColumns.put("min_hw_mode", new Atom<>(minHwMode));

            updateColumns.put("group_rekey", new Atom<>(keyRefresh));
            updateColumns.put("uapsd_enable", new Atom<>(uapsdEnabled));
            updateColumns.put("ap_bridge", new Atom<>(apBridge));

            @SuppressWarnings("unchecked")
            com.vmware.ovsdb.protocol.operation.notation.Map<String, String> securityMap = com.vmware.ovsdb.protocol.operation.notation.Map
                    .of(security);
            updateColumns.put("security", securityMap);

            configureCustomOptionsForSsid(ovsdbClient, enable80211k, rateLimitEnable, ssidDlLimit, ssidUlLimit,
                    clientDlLimit, clientUlLimit, rtsCtsThreshold, fragThresholdBytes, dtimPeriod, radiusNasId,
                    radiusNasIp, radiusOperatorName, updateColumns);

            updateBlockList(updateColumns, macBlockList);
            Row row = new Row(updateColumns);

            operations.add(new Insert(wifiVifConfigDbTable, row));

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            Uuid vifConfigUuid = null;
            for (OperationResult res : result) {
                InsertResult insertResult = null;
                if (res instanceof InsertResult) {
                    insertResult = (InsertResult) res;
                    LOG.info("configureSingleSsid:InsertResult {}", insertResult);
                    vifConfigUuid = ((InsertResult) res).getUuid();
                } else if (res instanceof ErrorResult) {
                    LOG.error("configureSingleSsid: error {}", (res));
                    throw new RuntimeException("configureSingleSsid " + ((ErrorResult) res).getError() + " "
                            + ((ErrorResult) res).getDetails());
                }
            }
            if (vifConfigUuid == null) {
                throw new IllegalStateException("Wifi_VIF_Config entry was not created successfully");
            }

            confirmRowExistsInTable(ovsdbClient, vifConfigUuid, wifiVifConfigDbTable);

            LOG.info("configureSingleSsid:Provisioned SSID {} on interface {} / {}", ssid, vifInterfaceName,
                    radioFreqBand);

            return vifConfigUuid;

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in configureSingleSsid", e);
            throw new RuntimeException(e);
        }

    }

    void configureSsids(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig) {

        boolean rrmEnabled = false;
        if ((opensyncApConfig.getEquipmentLocation() != null)
                && (opensyncApConfig.getEquipmentLocation().getDetails() != null)) {
            rrmEnabled = opensyncApConfig.getEquipmentLocation().getDetails().isRrmEnabled();
        }
        List<MacAddress> macBlockList = opensyncApConfig.getBlockedClients();
        LOG.debug("configureSsids {} with blockList {}", opensyncApConfig.getSsidProfile(), macBlockList);

        List<RadioType> enabledRadiosFromAp = new ArrayList<>();
        getEnabledRadios(ovsdbClient, enabledRadiosFromAp);

        for (Profile ssidProfile : opensyncApConfig.getSsidProfile()) {

            SsidConfiguration ssidConfig = (SsidConfiguration) ssidProfile.getDetails();
            ApElementConfiguration apElementConfig = (ApElementConfiguration) opensyncApConfig.getCustomerEquipment()
                    .getDetails();
            RfConfiguration rfConfig = (RfConfiguration) opensyncApConfig.getRfProfile().getDetails();

            for (RadioType radioType : ssidConfig.getAppliedRadios()) {
                // Still put profiles on disabled radios for now.
                //
                // if (!enabledRadiosFromAp.contains(radioType)) {
                // // Not on this AP
                // LOG.debug(
                // "AP {} does not have a radio where frequency band is {}.
                // Cannot provision this radio profile on AP.",
                // opensyncApConfig.getCustomerEquipment().getInventoryId(),
                // radioType);
                // continue;
                // }

                // custom_options:='["map",[["rate_limit_en","1"],["ssid_ul_limit","1024"],["ssid_dl_limit","1024"],["client_dl_limit","200"],["client_ul_limit","200"]]]'

                boolean rateLimitEnable = false;
                int ssidUlLimit = 0;
                int ssidDlLimit = 0;
                int clientDlLimit = 0;
                int clientUlLimit = 0;

                if (((ssidConfig.getBandwidthLimitDown() != null) && (ssidConfig.getBandwidthLimitUp() > 0))
                        || ((ssidConfig.getBandwidthLimitUp() != null) && (ssidConfig.getBandwidthLimitUp() > 0))) {
                    rateLimitEnable = true;
                    ssidUlLimit = ssidConfig.getBandwidthLimitUp();
                    ssidDlLimit = ssidConfig.getBandwidthLimitDown();
                    clientDlLimit = ssidConfig.getClientBandwidthLimitDown();
                    clientUlLimit = ssidConfig.getClientBandwidthLimitUp();
                }

                Map<String, WifiRadioConfigInfo> provisionedRadioConfigs = getProvisionedData
                        .getProvisionedWifiRadioConfigs(ovsdbClient);
                String freqBand = null;
                String ifName = null;
                String radioName = null;
                for (Entry<String, WifiRadioConfigInfo> entry : provisionedRadioConfigs.entrySet()) {
                    if ((radioType == RadioType.is2dot4GHz) && entry.getValue().freqBand.equals("2.4G")) {
                        freqBand = "2.4G";
                        radioName = entry.getKey();
                        break;
                    } else if ((radioType == RadioType.is5GHzL) && entry.getValue().freqBand.equals("5GL")) {
                        freqBand = "5GL";
                        radioName = entry.getKey();
                        break;
                    } else if ((radioType == RadioType.is5GHzU) && entry.getValue().freqBand.equals("5GU")) {
                        freqBand = "5GU";
                        radioName = entry.getKey();
                        break;
                    } else if ((radioType == RadioType.is5GHz) && entry.getValue().freqBand.equals("5G")) {
                        freqBand = "5G";
                        radioName = entry.getKey();
                        break;
                    }
                }
                if ((radioName == null) || (freqBand == null)) {
                    LOG.debug("Cannot provision SSID with radio if_name {} and freqBand {}", radioName, freqBand);
                    continue;
                }
                if (radioName.equals(radio0)) {
                    ifName = defaultRadio0;
                } else if (radioName.equals(radio1)) {
                    ifName = defaultRadio1;
                } else if (radioName.equals(radio2)) {
                    ifName = defaultRadio2;
                }
                if (ifName == null) {
                    LOG.debug("Cannot provision SSID for radio {} freqBand {} with VIF if_name {}", radioName, freqBand,
                            ifName);
                    continue;
                }

                int keyRefresh = ssidConfig.getKeyRefresh();

                boolean ssidBroadcast = ssidConfig.getBroadcastSsid() == StateSetting.enabled;

                String ipAssignScheme = apElementConfig.getGettingIP().toString();
                // the following 5 attributes only applicable to static
                // ipAssignScheme,
                // else they are
                // ignored
                String gateway = null;
                String inet = null;
                Map<String, String> dns = null;
                if (ipAssignScheme.equals("manual")) {
                    if (apElementConfig.getStaticIP() != null) {
                        ipAssignScheme = "static";
                        inet = apElementConfig.getStaticIP().getHostAddress();
                        gateway = apElementConfig.getStaticIpGw().getHostAddress();
                        dns = new HashMap<>();
                        dns.put(apElementConfig.getStaticDnsIp1().getHostName(),
                                apElementConfig.getStaticDnsIp1().getHostAddress());
                        dns.put(apElementConfig.getStaticDnsIp2().getHostName(),
                                apElementConfig.getStaticDnsIp2().getHostAddress());
                    } else {
                        ipAssignScheme = "none";
                    }
                }

                RfElementConfiguration rfElementConfig = rfConfig.getRfConfig(radioType);
                int rtsCtsThreshold = rfElementConfig.getRtsCtsThreshold();
                RadioMode radioMode = rfElementConfig.getRadioMode();

                int dtimPeriod = 2;
                int fragThresholdBytes = 2346;
                boolean uapsdEnabled = true;
                boolean apBridge = true;
                RadioConfiguration radioConfiguration = apElementConfig.getAdvancedRadioMap().get(radioType);
                if (radioConfiguration != null) {
                    dtimPeriod = radioConfiguration.getDtimPeriod();
                    uapsdEnabled = radioConfiguration.getUapsdState() == StateSetting.enabled;
                    apBridge = radioConfiguration.getStationIsolation() == StateSetting.disabled; // stationIsolation
                    fragThresholdBytes = radioConfiguration.getFragmentationThresholdBytes();
                }
                String minHwMode = "11n"; // min_hw_mode is 11ac, wifi 5, we can
                // also take ++ (11ax) but 2.4GHz only
                // Wifi4 --
                if (!radioType.equals(RadioType.is2dot4GHz)) {
                    minHwMode = "11ac";
                }
                if (!radioType.equals(RadioType.is2dot4GHz) && radioMode.equals(RadioMode.modeX)) {
                    minHwMode = "11x";
                }

                // off by default
                boolean enable80211r = false;
                int mobilityDomain = 0;
                // on by default
                boolean enable80211v = true;
                // on by default
                boolean enable80211k = true;
                // off by default, only applicable for is2do4GHz
                if ((ssidConfig.getRadioBasedConfigs() != null)
                        && (ssidConfig.getRadioBasedConfigs().containsKey(radioType)
                                && (ssidConfig.getRadioBasedConfigs().get(radioType) != null))) {
                    if (ssidConfig.getRadioBasedConfigs().get(radioType).getEnable80211r() != null) {
                        enable80211r = ssidConfig.getRadioBasedConfigs().get(radioType).getEnable80211r();
                        if (enable80211r) {
                            mobilityDomain = opensyncApConfig.getCustomerEquipment().getCustomerId(); // for
                                                                                                      // uniqueness,
                                                                                                      // mobility
                                                                                                      // domain
                                                                                                      // is
                                                                                                      // per
                                                                                                      // customer
                        }
                    }
                    if (ssidConfig.getRadioBasedConfigs().get(radioType).getEnable80211v() != null) {
                        enable80211v = ssidConfig.getRadioBasedConfigs().get(radioType).getEnable80211v();
                    }
                    if (ssidConfig.getRadioBasedConfigs().get(radioType).getEnable80211k() != null) {
                        enable80211k = ssidConfig.getRadioBasedConfigs().get(radioType).getEnable80211k();
                    }
                }

                Map<String, String> security = new HashMap<>();
                String ssidSecurityMode = ssidConfig.getSecureMode().name();
                String opensyncSecurityMode = "OPEN";

                String radiusNasId = null;
                String radiusNasIp = null;
                String radiusOperName = null;

                opensyncSecurityMode = getOpensyncSecurityMode(ssidSecurityMode, opensyncSecurityMode);
                populateSecurityMap(opensyncApConfig, ssidConfig, security, ssidSecurityMode, opensyncSecurityMode);

                if (opensyncSecurityMode.endsWith("EAP")) {
                    if (ssidConfig.getRadiusClientConfiguration() != null) {
                        radiusNasId = ssidConfig.getRadiusClientConfiguration().getNasClientId()
                                .equals(NasIdType.USER_DEFINED)
                                        ? ssidConfig.getRadiusClientConfiguration().getUserDefinedNasId()
                                        : ssidConfig.getRadiusClientConfiguration().getNasClientId().toString();
                        radiusNasIp = ssidConfig.getRadiusClientConfiguration().getNasClientIp()
                                .equals(NasIpType.USER_DEFINED)
                                        ? ssidConfig.getRadiusClientConfiguration().getUserDefinedNasIp()
                                        : ssidConfig.getRadiusClientConfiguration().getNasClientIp().toString();
                        radiusOperName = ssidConfig.getRadiusClientConfiguration().getOperatorId();
                    } else {
                        radiusNasId = NasIdType.DEFAULT.toString();
                        radiusNasIp = NasIpType.WAN_IP.toString();
                    }
                }

                // TODO put into AP captive parameter
                Map<String, String> captiveMap = new HashMap<>();
                List<String> walledGardenAllowlist = new ArrayList<>();
                getCaptiveConfiguration(opensyncApConfig, ssidConfig, captiveMap, walledGardenAllowlist);

                Map<Short, Set<String>> bonjourServiceMap = new HashMap<>();
                getBonjourGatewayConfiguration(opensyncApConfig, ssidConfig, bonjourServiceMap);

                boolean enabled = ssidConfig.getSsidAdminState().equals(StateSetting.enabled);
                int vlanId = ssidConfig.getVlanId() != null ? ssidConfig.getVlanId() : 1;
                Optional<GreTunnelConfiguration> tunnelConfiguration = ((ApNetworkConfiguration) opensyncApConfig
                        .getApProfile().getDetails()).getGreTunnelConfigurations().stream()
                                .filter(new Predicate<GreTunnelConfiguration>() {

                                    @Override
                                    public boolean test(GreTunnelConfiguration t) {

                                        return t.getVlanIdsInGreTunnel().contains(vlanId);
                                    }

                                }).findFirst();

                try {

                    ifName = getInterfaceNameForVifConfig(ovsdbClient, opensyncApConfig, ssidConfig, freqBand, ifName);
                    String greTunnelName = null;
                    if (tunnelConfiguration.isPresent()) {
                        greTunnelName = tunnelConfiguration.get().getGreTunnelName();
                    }

                    Uuid vifConfigUuid = configureSingleSsid(ovsdbClient, ifName, ssidConfig.getSsid(), ssidBroadcast,
                            security, freqBand, vlanId, rrmEnabled, enable80211r, mobilityDomain, enable80211v,
                            enable80211k, minHwMode, enabled, keyRefresh, uapsdEnabled, apBridge,
                            ssidConfig.getForwardMode(), gateway, inet, dns, ipAssignScheme, macBlockList,
                            rateLimitEnable, ssidDlLimit, ssidUlLimit, clientDlLimit, clientUlLimit, rtsCtsThreshold,
                            fragThresholdBytes, dtimPeriod, captiveMap, walledGardenAllowlist, bonjourServiceMap,
                            radiusNasId, radiusNasIp, radiusOperName, greTunnelName);

                    radioConfig.updateVifConfigsSetForRadio(ovsdbClient, ssidConfig.getSsid(), freqBand, vifConfigUuid);

                    networkConfig.configureInetVifInterface(ovsdbClient, ifName, enabled, ssidConfig.getForwardMode());

                } catch (IllegalStateException | OvsdbClientException | InterruptedException | ExecutionException
                        | TimeoutException e) {
                    // could not provision this SSID, but still can go on
                    LOG.warn("could not provision SSID {} on {}", ssidConfig.getSsid(), freqBand);
                }

            }

        }

    }

    void getBonjourGatewayConfiguration(OpensyncAPConfig opensyncApConfig, SsidConfiguration ssidConfig,
            Map<Short, Set<String>> bonjourServiceMap) {
        if ((ssidConfig.getBonjourGatewayProfileId() != null)
                && (opensyncApConfig.getBonjourGatewayProfiles() != null)) {
            for (Profile profileBonjour : opensyncApConfig.getBonjourGatewayProfiles()) {
                if ((ssidConfig.getBonjourGatewayProfileId() == profileBonjour.getId())
                        && (profileBonjour.getDetails() != null)) {

                    BonjourGatewayProfile bonjourGatewayConfiguration = (BonjourGatewayProfile) profileBonjour
                            .getDetails();

                    Collection<BonjourServiceSet> bonjourServicesCollection = bonjourGatewayConfiguration
                            .getBonjourServices();
                    bonjourServicesCollection.stream().forEach(b -> {
                        Set<String> serviceSet = new HashSet<>();
                        if (bonjourServiceMap.containsKey(b.getVlanId())) {
                            serviceSet.addAll(bonjourServiceMap.get(b.getVlanId()));
                        }
                        serviceSet.addAll(b.getServiceNames());
                        bonjourServiceMap.put(b.getVlanId(), serviceSet);
                    });

                    LOG.debug("bonjourServiceMap {}", bonjourServiceMap);
                }
            }
        }
    }

    /**
     *
     * @param authentication
     * @return ovsdb value for captive portal authentication map entry based on
     *         cloud type.
     */
    String getCaptiveAuthentication(CaptivePortalAuthenticationType authentication) {
        switch (authentication) {
        case guest:
            return "None";
        case username:
            return "Captive Portal User List";
        case radius:
            return "RADIUS";
        default:
            LOG.error("Unsupported captive portal authentication {}", authentication);
            return "None";
        }
    }

    /**
     *
     * @param opensyncApConfig
     * @param ssidConfig
     * @param captiveMap
     * @param walledGardenAllowlist
     */
    void getCaptiveConfiguration(OpensyncAPConfig opensyncApConfig, SsidConfiguration ssidConfig,
            Map<String, String> captiveMap, List<String> walledGardenAllowlist) {
        if ((ssidConfig.getCaptivePortalId() != null) && (opensyncApConfig.getCaptiveProfiles() != null)) {
            for (Profile profileCaptive : opensyncApConfig.getCaptiveProfiles()) {
                if ((ssidConfig.getCaptivePortalId() == profileCaptive.getId())
                        && (profileCaptive.getDetails() != null)) {
                    CaptivePortalConfiguration captiveProfileDetails = ((CaptivePortalConfiguration) profileCaptive
                            .getDetails());

                    // +#define SCHEMA_CONSTS_PAGE_TITLE "page_title"
                    if (captiveProfileDetails.getBrowserTitle() != null) {
                        captiveMap.put("session_timeout",
                                String.valueOf(captiveProfileDetails.getSessionTimeoutInMinutes()));
                        captiveMap.put("page_title", captiveProfileDetails.getBrowserTitle());
                    }
                    if (captiveProfileDetails.getAuthenticationType().equals(CaptivePortalAuthenticationType.radius)) {
                        Optional<Profile> optional = opensyncApConfig.getRadiusProfiles().stream()
                                .filter(p -> p.getId() == captiveProfileDetails.getRadiusServiceId()).findFirst();
                        if (optional != null && optional.isPresent()) {
                            Profile profile = optional.get();
                            RadiusProfile radiusProfile = (RadiusProfile) profile.getDetails();
                            captiveMap.put("radius_server_ip", String.valueOf(
                                    radiusProfile.getPrimaryRadiusAuthServer().getIpAddress().getHostAddress()));

                            captiveMap.put("radius_server_port",
                                    String.valueOf(radiusProfile.getPrimaryRadiusAuthServer().getPort()));

                            captiveMap.put("radius_server_secret",
                                    String.valueOf(radiusProfile.getPrimaryRadiusAuthServer().getSecret()));
                            if (captiveProfileDetails.getRadiusAuthMethod() != null) {

                                captiveMap.put("radius_auth_type",
                                        String.valueOf(captiveProfileDetails.getRadiusAuthMethod()));
                            }
                        }
                    }

                    if (captiveProfileDetails.getRedirectURL() != null) {
                        captiveMap.put("redirect_url", captiveProfileDetails.getRedirectURL());
                    }
                    captiveMap.put("session_timeout",
                            String.valueOf(captiveProfileDetails.getSessionTimeoutInMinutes()));

                    captiveMap.put("browser_title", captiveProfileDetails.getBrowserTitle());
                    captiveMap.put("splash_page_title", captiveProfileDetails.getHeaderContent());
                    captiveMap.put("acceptance_policy", captiveProfileDetails.getUserAcceptancePolicy());
                    captiveMap.put("login_success_text", captiveProfileDetails.getSuccessPageMarkdownText());
                    captiveMap.put("authentication",
                            getCaptiveAuthentication(captiveProfileDetails.getAuthenticationType()));
                    if (captiveProfileDetails.getUsernamePasswordFile() != null) {
                        captiveMap
                                .put("username_password_file",
                                        ManagedFileInfo.resolveWithPopulatedHostname(
                                                captiveProfileDetails.getUsernamePasswordFile(), externalFileStoreURL)
                                                .getApExportUrl());
                    }
                    if (captiveProfileDetails.getLogoFile() != null) {
                        captiveMap.put("splash_page_logo", ManagedFileInfo
                                .resolveWithPopulatedHostname(captiveProfileDetails.getLogoFile(), externalFileStoreURL)
                                .getApExportUrl());
                    }
                    if (captiveProfileDetails.getBackgroundFile() != null) {
                        captiveMap.put("splash_page_background_logo",
                                ManagedFileInfo.resolveWithPopulatedHostname(captiveProfileDetails.getBackgroundFile(),
                                        externalFileStoreURL).getApExportUrl());
                    }
                    LOG.debug("captiveMap {}", captiveMap);
                    walledGardenAllowlist.addAll(captiveProfileDetails.getWalledGardenAllowlist());

                }
            }
        }
    }

    /**
     * Get the interface name for the new Wifi_VIF_Config. Maximum of 8
     * interface names per frequencyBand
     *
     * @param ovsdbClient
     * @param opensyncApConfig
     * @param ssidConfig
     * @param freqBand
     * @param ifName
     * @return the interface name, i.e. wlan0, wlan0_1, etc
     */
    String getInterfaceNameForVifConfig(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig,
            SsidConfiguration ssidConfig, String freqBand, String ifName) {
        Map<String, WifiVifConfigInfo> provisionedVifs = getProvisionedData.getProvisionedWifiVifConfigs(ovsdbClient);

        List<String> interfaces = new ArrayList<>();
        interfaces.add(ifName);
        for (int i = 1; i < MAX_VIF_PER_FREQ; i++) {
            interfaces.add(ifName + "_" + Integer.toString(i));
        }
        for (String key : provisionedVifs.keySet()) {
            if (key.contains(ifName)) {
                String provisionedIfName = provisionedVifs.get(key).ifName;
                if (interfaces.remove(provisionedIfName)) {
                    LOG.info("Interface {} already in use on Radio {}, cannot be used for new Wifi_VIF_Config.",
                            provisionedIfName, freqBand);
                }
            }
        }
        if (interfaces.isEmpty()) {
            throw new RuntimeException("No more available interfaces on AP "
                    + opensyncApConfig.getCustomerEquipment().getName() + " for frequency band " + freqBand);
        } else {
            // take the first available interface for this band
            ifName = interfaces.get(0);
            LOG.info("Configuring new Wifi_VIF_Config for ssid {} with if_name {}", ssidConfig.getSsid(), ifName);
        }
        return ifName;
    }

    /**
     * Maps between the osvdb security definitions and the cloud's security mode
     * for the give SSID being configured.
     *
     * @param ssidSecurityMode
     * @param opensyncSecurityMode
     * @return
     */
    String getOpensyncSecurityMode(String ssidSecurityMode, String opensyncSecurityMode) {
        if (ssidSecurityMode.equals("wpaPSK") || ssidSecurityMode.equals("wpa2PSK")
                || ssidSecurityMode.equals("wpa2OnlyPSK")) {
            opensyncSecurityMode = "WPA-PSK";
        } else if (ssidSecurityMode.equals("wep")) {
            opensyncSecurityMode = "WEP";
        } else if (ssidSecurityMode.equals("wpaEAP") || ssidSecurityMode.equals("wpa2EAP")
                || ssidSecurityMode.equals("wpa2OnlyEAP")) {
            opensyncSecurityMode = "WPA-EAP";
        } else if (ssidSecurityMode.equals("wpaRadius") || ssidSecurityMode.equals("wpa2OnlyRadius")
                || ssidSecurityMode.equals("wpa2Radius")) {
            opensyncSecurityMode = "WPA-EAP";
        } else if (ssidSecurityMode.equals("wpa3OnlySAE") || ssidSecurityMode.equals("wpa3MixedSAE")) {
            opensyncSecurityMode = "WPA-SAE";
        } else if (ssidSecurityMode.equals("wpa3OnlyEAP") || ssidSecurityMode.equals("wpa3MixedEAP")) {
            opensyncSecurityMode = "WPA3-EAP";
        }
        return opensyncSecurityMode;
    }

    void getRadiusAccountingConfiguration(OpensyncAPConfig opensyncApConfig, SsidConfiguration ssidConfig,
            Map<String, String> security) {

        LOG.debug("getRadiusAccountingConfiguration for ssidConfig {} from radiusProfiles {}", ssidConfig,
                opensyncApConfig.getRadiusProfiles());

        LOG.debug("Radius Accounting Profiles {}", opensyncApConfig.getRadiusProfiles());

        List<Profile> radiusProfileList = opensyncApConfig.getRadiusProfiles().stream()
                .filter(new Predicate<Profile>() {

                    @Override
                    public boolean test(Profile t) {
                        return t.getId() == ssidConfig.getRadiusServiceId();
                    }

                }).collect(Collectors.toList());

        if (radiusProfileList != null && radiusProfileList.size() > 0) {
            Profile profileRadius = radiusProfileList.get(0);
            RadiusProfile profileDetails = ((RadiusProfile) profileRadius.getDetails());
            RadiusServer rServer = profileDetails.getPrimaryRadiusAccountingServer();
            if (rServer != null) {
                security.put("radius_acct_ip",
                        rServer.getIpAddress() != null ? rServer.getIpAddress().getHostAddress() : null);
                security.put("radius_acct_port", rServer.getPort() != null ? String.valueOf(rServer.getPort()) : null);
                security.put("radius_acct_secret", rServer.getSecret());
                if (ssidConfig.getRadiusAcountingServiceInterval() != null) {
                    // if the value is present, use the
                    // radius_acct_interval
                    security.put("radius_acct_interval", ssidConfig.getRadiusAcountingServiceInterval().toString());

                } else {
                    LOG.info("No radius_acct_interval defined for ssid {}, Setting radius_acct_interval to 0",
                            ssidConfig.getSsid(), rServer);
                    security.put("radius_acct_interval", "0");
                }
                LOG.info(
                        "set Radius Accounting server attributes radius_acct_ip {} radius_acct_port {} radius_acct_secret {} radius_acct_interval {}",
                        security.get("radius_acct_ip"), security.get("radius_acct_port"),
                        security.get("radius_acct_secret"), security.get("radius_acct_interval"));
            } else {
                LOG.info("No Radius Accounting Server defined in Radius Profile");
            }

        } else {
            LOG.warn("Could not find radius profile {} in {}", ssidConfig.getRadiusServiceId(),
                    opensyncApConfig.getRadiusProfiles());
        }

    }

    void getRadiusConfiguration(OpensyncAPConfig opensyncApConfig, SsidConfiguration ssidConfig,
            Map<String, String> security) {

        LOG.debug("getRadiusConfiguration for ssidConfig {} from radiusProfiles {}", ssidConfig,
                opensyncApConfig.getRadiusProfiles());

        LOG.debug("Radius Profiles {}", opensyncApConfig.getRadiusProfiles());

        List<Profile> radiusProfileList = opensyncApConfig.getRadiusProfiles().stream()
                .filter(new Predicate<Profile>() {

                    @Override
                    public boolean test(Profile t) {
                        return t.getId() == ssidConfig.getRadiusServiceId();
                    }

                }).collect(Collectors.toList());

        if (radiusProfileList != null && radiusProfileList.size() > 0) {
            Profile profileRadius = radiusProfileList.get(0);
            RadiusProfile profileDetails = ((RadiusProfile) profileRadius.getDetails());
            RadiusServer radiusServer = profileDetails.getPrimaryRadiusAuthServer();
            security.put("radius_server_ip",
                    radiusServer.getIpAddress() != null ? radiusServer.getIpAddress().getHostAddress() : null);
            security.put("radius_server_port",
                    radiusServer.getPort() != null ? String.valueOf(radiusServer.getPort()) : null);
            security.put("radius_server_secret", radiusServer.getSecret());
            LOG.info("set Radius server attributes radius_server_ip {} radius_server_port {} radius_server_secret {}",
                    security.get("radius_server_ip"), security.get("radius_server_port"),
                    security.get("radius_server_secret"));

        } else {
            LOG.warn("Could not find radius profile {} in {}", ssidConfig.getRadiusServiceId(),
                    opensyncApConfig.getRadiusProfiles());
        }
    }

    /**
     * Constructs the map to place in the Wifi_VIF_Config ovsdb table's security
     * column.
     *
     * @param opensyncApConfig
     * @param ssidConfig
     * @param security
     * @param ssidSecurityMode
     * @param opensyncSecurityMode
     */
    void populateSecurityMap(OpensyncAPConfig opensyncApConfig, SsidConfiguration ssidConfig,
            Map<String, String> security, String ssidSecurityMode, String opensyncSecurityMode) {
        security.put("encryption", opensyncSecurityMode);
        // key and mode is N/A for OPEN security
        if (!opensyncSecurityMode.equals("OPEN")) {
            if (ssidSecurityMode.equals("wpa2PSK") || ssidSecurityMode.equals("wpa3MixedSAE")) {
                security.put("key", ssidConfig.getKeyStr());
                security.put("mode", "mixed");
            } else if (ssidSecurityMode.equals("wpa2OnlyPSK")) {
                security.put("key", ssidConfig.getKeyStr());
                security.put("mode", "2");
            } else if (ssidSecurityMode.equals("wpa3OnlySAE")) {
                security.put("key", ssidConfig.getKeyStr());
                security.put("mode", "3");
            } else if (ssidSecurityMode.equals("wpaPSK")) {
                security.put("key", ssidConfig.getKeyStr());
                security.put("mode", "1");
            } else if (ssidSecurityMode.equals("wpa2OnlyEAP") || ssidSecurityMode.equals("wpa2OnlyRadius")) {
                security.put("mode", "2");
                getRadiusConfiguration(opensyncApConfig, ssidConfig, security);
                getRadiusAccountingConfiguration(opensyncApConfig, ssidConfig, security);
            } else if (ssidSecurityMode.equals("wpa3OnlyEAP")) {
                security.put("mode", "3");
                getRadiusConfiguration(opensyncApConfig, ssidConfig, security);
                getRadiusAccountingConfiguration(opensyncApConfig, ssidConfig, security);

            } else if (ssidSecurityMode.equals("wpa2EAP") || ssidSecurityMode.equals("wpa2Radius")
                    || ssidSecurityMode.equals("wpa3MixedEAP")) {
                security.put("mode", "mixed");
                getRadiusConfiguration(opensyncApConfig, ssidConfig, security);
                getRadiusAccountingConfiguration(opensyncApConfig, ssidConfig, security);

            } else if (ssidSecurityMode.equals("wpaEAP") || ssidSecurityMode.equals("wpaRadius")) {
                security.put("mode", "1");
                getRadiusConfiguration(opensyncApConfig, ssidConfig, security);
                getRadiusAccountingConfiguration(opensyncApConfig, ssidConfig, security);

            } else if (ssidSecurityMode.equals("wep")) {
                security.put("key", ssidConfig.getKeyStr());
                security.put("mode", "1");
            }
        }
    }

    void removeAllSsids(OvsdbClient ovsdbClient) {
        LOG.info("removeAllSsids from {}:", wifiVifConfigDbTable);

        try {

            List<Operation> operations = new ArrayList<>();
            operations.add(new Delete(wifiVifConfigDbTable));

            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            for (OperationResult res : result) {
                LOG.info("Op Result {}", res);
                if (res instanceof UpdateResult) {
                    LOG.info("removeAllSsids:result {}", ((UpdateResult) res).toString());
                } else if (res instanceof ErrorResult) {
                    LOG.error("removeAllSsids:result error {}", (res));
                    throw new RuntimeException("removeAllSsids " + ((ErrorResult) res).getError() + " "
                            + ((ErrorResult) res).getDetails());
                }
            }

            Map<String, WifiVifConfigInfo> provisionedVifConfigs = getProvisionedData
                    .getProvisionedWifiVifConfigs(ovsdbClient);
            // this should be empty
            if (!provisionedVifConfigs.isEmpty()) {
                throw new RuntimeException(
                        "Failed to remove all vif configurations from Wifi_VIF_Config dbTable, still has "
                                + provisionedVifConfigs.values());
            }

            LOG.info("Removed all ssids");

        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in removeAllSsids", e);
            throw new RuntimeException(e);
        }

    }

    void updateBlockList(Map<String, Value> updateColumns, List<MacAddress> macBlockList) {

        if ((macBlockList != null) && !macBlockList.isEmpty()) {
            updateColumns.put("mac_list_type", new Atom<>("blacklist"));
            Set<Atom<String>> atomMacList = new HashSet<>();
            for (MacAddress mac : macBlockList) {
                atomMacList.add(new Atom<>(mac.getAddressAsString()));
            }
            com.vmware.ovsdb.protocol.operation.notation.Set macListSet = com.vmware.ovsdb.protocol.operation.notation.Set
                    .of(atomMacList);
            updateColumns.put("mac_list", macListSet);
        } else {
            updateColumns.put("mac_list_type", new Atom<>("none"));
            updateColumns.put("mac_list", new com.vmware.ovsdb.protocol.operation.notation.Set());
        }
    }

}
