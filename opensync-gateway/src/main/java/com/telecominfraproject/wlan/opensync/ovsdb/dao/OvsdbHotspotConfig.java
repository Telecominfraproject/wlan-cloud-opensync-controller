package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPHotspot20Config;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.Hotspot20Config;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.Hotspot20IconConfig;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.Hotspot20OsuProviders;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.WifiVifConfigInfo;
import com.telecominfraproject.wlan.opensync.util.OvsdbToWlanCloudTypeMappingUtility;
import com.telecominfraproject.wlan.profile.models.Profile;
import com.telecominfraproject.wlan.profile.passpoint.models.PasspointDuple;
import com.telecominfraproject.wlan.profile.passpoint.models.PasspointIPv4AddressType;
import com.telecominfraproject.wlan.profile.passpoint.models.PasspointIPv6AddressType;
import com.telecominfraproject.wlan.profile.passpoint.models.PasspointMccMnc;
import com.telecominfraproject.wlan.profile.passpoint.models.PasspointProfile;
import com.telecominfraproject.wlan.profile.passpoint.models.operator.PasspointOperatorProfile;
import com.telecominfraproject.wlan.profile.passpoint.models.provider.PasspointEapMethods;
import com.telecominfraproject.wlan.profile.passpoint.models.provider.PasspointNaiRealmEapAuthInnerNonEap;
import com.telecominfraproject.wlan.profile.passpoint.models.provider.PasspointNaiRealmEapAuthParam;
import com.telecominfraproject.wlan.profile.passpoint.models.provider.PasspointNaiRealmEapCredType;
import com.telecominfraproject.wlan.profile.passpoint.models.provider.PasspointOsuIcon;
import com.telecominfraproject.wlan.profile.passpoint.models.provider.PasspointOsuProviderProfile;
import com.telecominfraproject.wlan.profile.passpoint.models.venue.PasspointVenueName;
import com.telecominfraproject.wlan.profile.passpoint.models.venue.PasspointVenueProfile;
import com.telecominfraproject.wlan.profile.passpoint.models.venue.PasspointVenueTypeAssignment;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration;
import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.protocol.operation.Delete;
import com.vmware.ovsdb.protocol.operation.Insert;
import com.vmware.ovsdb.protocol.operation.Operation;
import com.vmware.ovsdb.protocol.operation.Update;
import com.vmware.ovsdb.protocol.operation.notation.Atom;
import com.vmware.ovsdb.protocol.operation.notation.Condition;
import com.vmware.ovsdb.protocol.operation.notation.Function;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;
import com.vmware.ovsdb.protocol.operation.notation.Value;
import com.vmware.ovsdb.protocol.operation.result.InsertResult;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.protocol.operation.result.UpdateResult;
import com.vmware.ovsdb.protocol.schema.DatabaseSchema;
import com.vmware.ovsdb.service.OvsdbClient;

@Component
public class OvsdbHotspotConfig extends OvsdbDaoBase {

    @Autowired
    OvsdbGet ovsdbGet;

    protected void getNaiRealms(PasspointOsuProviderProfile providerProfile, Set<Atom<String>> naiRealms) {
        providerProfile.getNaiRealmList().stream().forEach(c -> {

            StringBuffer naiBuffer = new StringBuffer();
            naiBuffer.append(Integer.toString(c.getEncoding()));
            naiBuffer.append(",");
            Iterator<String> realmsIterator = c.getNaiRealms().iterator();
            if (realmsIterator != null) {
                while (realmsIterator.hasNext()) {
                    String realm = realmsIterator.next();
                    naiBuffer.append(realm);
                    if (realmsIterator.hasNext()) {
                        naiBuffer.append(";");
                    }
                }

            }

            if (c.getEapMap() == null || c.getEapMap().isEmpty()) {
                naiRealms.add(new Atom<String>(naiBuffer.toString()));
            } else {
                naiBuffer.append(",");

                Map<String, Set<String>> eapMap = c.getEapMap();
                eapMap.entrySet().stream().forEach(e -> {

                    String eapMethodName = e.getKey();
                    String eapMethodId = String.valueOf(PasspointEapMethods.getByName(eapMethodName).getId());
                    naiBuffer.append(eapMethodId);

                    for (String credential : e.getValue()) {

                        String[] keyValue = credential.split(":");
                        String keyId = String.valueOf(PasspointNaiRealmEapAuthParam.getByName(keyValue[0]).getId());
                        if (keyValue[0].equals(
                                PasspointNaiRealmEapAuthParam.NAI_REALM_EAP_AUTH_NON_EAP_INNER_AUTH.getName())) {

                            String valueId = String
                                    .valueOf(PasspointNaiRealmEapAuthInnerNonEap.getByName(keyValue[1]).getId());

                            naiBuffer.append("[");
                            naiBuffer.append(keyId);
                            naiBuffer.append(":");
                            naiBuffer.append(valueId);
                            naiBuffer.append("]");

                        } else if (keyValue[0]
                                .equals(PasspointNaiRealmEapAuthParam.NAI_REALM_EAP_AUTH_CRED_TYPE.getName())
                                || keyValue[0]
                                        .equals(PasspointNaiRealmEapAuthParam.NAI_REALM_EAP_AUTH_TUNNELED_CRED_TYPE
                                                .getName())) {

                            String valueId = String
                                    .valueOf(PasspointNaiRealmEapCredType.getByName(keyValue[1]).getId());

                            naiBuffer.append("[");
                            naiBuffer.append(keyId);
                            naiBuffer.append(":");
                            naiBuffer.append(valueId);
                            naiBuffer.append("]");

                        }
                    }
                    naiBuffer.append(",");

                });
                String naiRealm = naiBuffer.toString();
                if (naiRealm.endsWith(",")) {
                    naiRealm = naiRealm.substring(0, naiRealm.lastIndexOf(","));
                }
                naiRealms.add(new Atom<String>(naiRealm));

            }

        });

    }

    protected void getOsuIconUuidsForOsuProvider(OvsdbClient ovsdbClient, PasspointOsuProviderProfile providerProfile,
            Map<String, Value> rowColumns) {
        Map<String, Hotspot20IconConfig> osuIconsMap = ovsdbGet.getProvisionedHotspot20IconConfig(ovsdbClient);
        Set<Uuid> iconsSet = new HashSet<>();
        if (osuIconsMap.size() > 0) {
            for (PasspointOsuIcon icon : providerProfile.getOsuIconList()) {
                if (osuIconsMap.containsKey(icon.getImageUrl())) {
                    iconsSet.add(osuIconsMap.get(icon.getImageUrl()).uuid);
                }
            }
        }

        if (iconsSet.size() > 0) {
            com.vmware.ovsdb.protocol.operation.notation.Set iconUuidSet = com.vmware.ovsdb.protocol.operation.notation.Set
                    .of(iconsSet);
            rowColumns.put("osu_icons", iconUuidSet);
        }
    }

    protected void getOsuProviderFriendlyNames(PasspointOsuProviderProfile providerProfile,
            Map<String, Value> rowColumns) {
        Set<Atom<String>> providerFriendlyNames = new HashSet<>();
        for (PasspointDuple friendlyName : providerProfile.getOsuFriendlyName()) {
            providerFriendlyNames.add(new Atom<String>(friendlyName.getAsDuple()));
        }

        if (providerFriendlyNames.size() > 0) {
            com.vmware.ovsdb.protocol.operation.notation.Set providerFriendlyNamesSet = com.vmware.ovsdb.protocol.operation.notation.Set
                    .of(providerFriendlyNames);
            rowColumns.put("osu_friendly_name", providerFriendlyNamesSet);
        }
    }

    protected void getOsuProviderMethodList(PasspointOsuProviderProfile providerProfile,
            Map<String, Value> rowColumns) {
        Set<Atom<Integer>> methods = new HashSet<>();
        for (Integer method : providerProfile.getOsuMethodList()) {
            methods.add(new Atom<Integer>(method));
        }
        if (methods.size() > 0) {
            com.vmware.ovsdb.protocol.operation.notation.Set methodsSet = com.vmware.ovsdb.protocol.operation.notation.Set
                    .of(methods);
            rowColumns.put("method_list", methodsSet);
        }
    }

    protected void getOsuProviderServiceDescriptions(PasspointOsuProviderProfile providerProfile,
            Map<String, Value> rowColumns) {
        Set<Atom<String>> serviceDescriptions = new HashSet<>();
        for (PasspointDuple serviceDescription : providerProfile.getOsuServiceDescription()) {
            serviceDescriptions.add(new Atom<String>(serviceDescription.getAsDuple()));
        }

        if (serviceDescriptions.size() > 0) {
            com.vmware.ovsdb.protocol.operation.notation.Set serviceDescriptionSet = com.vmware.ovsdb.protocol.operation.notation.Set
                    .of(serviceDescriptions);
            rowColumns.put("service_description", serviceDescriptionSet);
        }
    }

    void configureHotspots(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig) {

        provisionHotspot2IconConfig(ovsdbClient, opensyncApConfig);
        provisionHotspot20OsuProviders(ovsdbClient, opensyncApConfig);
        provisionHotspot20Config(ovsdbClient, opensyncApConfig);

    }

    /**
     * Add the operator specific information, taken from the operator profile
     * for the given hotspotProfile being configured.
     *
     * @param hs20cfg
     * @param rowColumns
     * @param hs2Profile
     */
    void getOperatorInformationForPasspointConfiguration(OpensyncAPHotspot20Config hs20cfg,
            Map<String, Value> rowColumns, PasspointProfile hs2Profile) {
        PasspointOperatorProfile passpointOperatorProfile = getOperatorProfileForPasspoint(hs20cfg, hs2Profile);

        Set<Atom<String>> domainNames = new HashSet<>();
        for (String domainName : passpointOperatorProfile.getDomainNameList()) {
            domainNames.add(new Atom<>(domainName));
        }
        if (domainNames.size() > 0) {
            com.vmware.ovsdb.protocol.operation.notation.Set domainNameSet = com.vmware.ovsdb.protocol.operation.notation.Set
                    .of(domainNames);
            rowColumns.put("domain_name", domainNameSet);
        }
        rowColumns.put("osen", new Atom<>(passpointOperatorProfile.isServerOnlyAuthenticatedL2EncryptionNetwork()));

        Set<Atom<String>> operatorFriendlyName = new HashSet<>();
        passpointOperatorProfile.getOperatorFriendlyName().stream()
                .forEach(c -> operatorFriendlyName.add(new Atom<>(c.getAsDuple())));
        com.vmware.ovsdb.protocol.operation.notation.Set operatorFriendlyNameSet = com.vmware.ovsdb.protocol.operation.notation.Set
                .of(operatorFriendlyName);
        rowColumns.put("operator_friendly_name", operatorFriendlyNameSet);

    }

    PasspointOperatorProfile getOperatorProfileForPasspoint(OpensyncAPHotspot20Config hs20cfg,
            PasspointProfile hs2Profile) {
        Profile operator = hs20cfg.getHotspot20OperatorSet().stream().filter(new Predicate<Profile>() {

            @Override
            public boolean test(Profile t) {
                return t.getId() == hs2Profile.getPasspointOperatorProfileId();
            }

        }).findFirst().get();

        PasspointOperatorProfile passpointOperatorProfile = (PasspointOperatorProfile) operator.getDetails();
        return passpointOperatorProfile;
    }

    /**
     * Adds map entries the UUIDs for the OSU Providers and Icons based on the
     * entries in the for providers that are associated with this
     * hotspotProfile.
     *
     * @param ovsdbClient
     * @param hs20cfg
     * @param hotspotProfile
     * @param rowColumns
     */
    void getOsuIconUuidsForPasspointConfiguration(OvsdbClient ovsdbClient, OpensyncAPHotspot20Config hs20cfg,
            Profile hotspotProfile, Map<String, Value> rowColumns) {
        Set<Uuid> osuIconUuids = getOsuProvidersInfoForPasspointConfiguration(ovsdbClient, hs20cfg, hotspotProfile,
                rowColumns);

        if (osuIconUuids.size() > 0) {
            com.vmware.ovsdb.protocol.operation.notation.Set iconUuids = com.vmware.ovsdb.protocol.operation.notation.Set
                    .of(osuIconUuids);
            rowColumns.put("operator_icons", iconUuids);
        }
    }

    /**
     * Get providers profiles. Helper method.
     *
     * @param hs20cfg
     * @param hotspotProfile
     * @return
     */
    List<Profile> getOsuProvidersForPasspoint(OpensyncAPHotspot20Config hs20cfg, Profile hotspotProfile) {
        List<Profile> providerList = new ArrayList<>();
        if (hs20cfg.getHotspot20ProviderSet() != null) {
            providerList = hs20cfg.getHotspot20ProviderSet().stream().filter(new Predicate<Profile>() {

                @Override
                public boolean test(Profile t) {
                    return hotspotProfile.getChildProfileIds().contains(t.getId());
                }
            }).collect(Collectors.toList());

        }
        return providerList;
    }

    /**
     * Get's the OSU Provider related information for a given hotspot, the osu
     * providers being configured on the ovsdb in Hotspot20_OSU_Providers and
     * defined as children of the hotspot profile
     *
     * @param ovsdbClient
     * @param hs20cfg
     * @param hotspotProfile
     * @param rowColumns
     * @return
     */
    Set<Uuid> getOsuProvidersInfoForPasspointConfiguration(OvsdbClient ovsdbClient, OpensyncAPHotspot20Config hs20cfg,
            Profile hotspotProfile, Map<String, Value> rowColumns) {
        Map<String, Hotspot20OsuProviders> osuProviders = ovsdbGet.getProvisionedHotspot20OsuProviders(ovsdbClient);
        List<Profile> providerList = getOsuProvidersForPasspoint(hs20cfg, hotspotProfile);

        Set<Uuid> osuProvidersUuids = new HashSet<>();
        Set<Uuid> osuIconUuids = new HashSet<>();

        StringBuffer mccMncBuffer = new StringBuffer();
        Set<Atom<String>> naiRealms = new HashSet<>();
        Set<Atom<String>> roamingOis = new HashSet<>();
        for (Profile provider : providerList) {
            PasspointOsuProviderProfile providerProfile = (PasspointOsuProviderProfile) provider.getDetails();

            osuProviders.keySet().stream().filter(new Predicate<String>() {

                @Override
                public boolean test(String providerNameOnAp) {
                    return providerNameOnAp.startsWith(OvsdbToWlanCloudTypeMappingUtility
                            .getApOsuProviderStringFromOsuProviderName(provider.getName()));
                }

            }).forEach(p -> {
                providerProfile.getRoamingOi().stream().forEach(o -> {
                    roamingOis.add(new Atom<>(o));
                });
                osuProvidersUuids.add(osuProviders.get(p).uuid);
                osuIconUuids.addAll(osuProviders.get(p).osuIcons);
                getNaiRealms(providerProfile, naiRealms);

                for (PasspointMccMnc passpointMccMnc : providerProfile.getMccMncList()) {
                    mccMncBuffer.append(passpointMccMnc.getMccMncPairing());
                    mccMncBuffer.append(";");
                }
            });

        }

        String mccMncString = mccMncBuffer.toString();
        if (mccMncString.endsWith(";")) {
            mccMncString = mccMncString.substring(0, mccMncString.lastIndexOf(";"));
        }

        rowColumns.put("mcc_mnc", new Atom<>(mccMncString));

        com.vmware.ovsdb.protocol.operation.notation.Set roamingOiSet = com.vmware.ovsdb.protocol.operation.notation.Set
                .of(roamingOis);
        rowColumns.put("roaming_oi", roamingOiSet);

        com.vmware.ovsdb.protocol.operation.notation.Set naiRealmsSet = com.vmware.ovsdb.protocol.operation.notation.Set
                .of(naiRealms);
        rowColumns.put("nai_realm", naiRealmsSet);

        if (osuProvidersUuids.size() > 0) {
            com.vmware.ovsdb.protocol.operation.notation.Set providerUuids = com.vmware.ovsdb.protocol.operation.notation.Set
                    .of(osuProvidersUuids);
            rowColumns.put("osu_providers", providerUuids);
        }
        return osuIconUuids;
    }

    /**
     * Passpoint configuration requires profile information from children, as
     * well as information from parent VIF Configurations These values are
     * placed in the rowColumns map to be passed into the transaction creating
     * the Hotspot20_Config in ovsdb.
     *
     * @param ovsdbClient
     * @param opensyncApConfig
     * @param hs20cfg
     * @param hotspotProfile
     * @param rowColumns
     * @param hs2Profile
     */
    void getPasspointConfigurationInformationFromDependencies(OvsdbClient ovsdbClient,
            OpensyncAPConfig opensyncApConfig, OpensyncAPHotspot20Config hs20cfg, Profile hotspotProfile,
            Map<String, Value> rowColumns, PasspointProfile hs2Profile) {

        getOperatorInformationForPasspointConfiguration(hs20cfg, rowColumns, hs2Profile);

        getVenueInformationForPasspointConfiguration(hs20cfg, rowColumns, hs2Profile);

        getOsuIconUuidsForPasspointConfiguration(ovsdbClient, hs20cfg, hotspotProfile, rowColumns);

        getVifInformationForPasspointConfiguration(ovsdbClient, opensyncApConfig, rowColumns, hs2Profile);
    }

    /**
     * Add the venue specific information, taken from the venue profile for the
     * given hotspotProfile being configured.
     *
     * @param hs20cfg
     * @param rowColumns
     * @param hs2Profile
     */
    void getVenueInformationForPasspointConfiguration(OpensyncAPHotspot20Config hs20cfg, Map<String, Value> rowColumns,
            PasspointProfile hs2Profile) {
        PasspointVenueProfile passpointVenueProfile = getVenueProfileForPasspoint(hs20cfg, hs2Profile);
        Set<Atom<String>> venueNames = new HashSet<>();
        Set<Atom<String>> venueUrls = new HashSet<>();
        int index = 1;
        for (PasspointVenueName passpointVenueName : passpointVenueProfile.getVenueNameSet()) {
            venueNames.add(new Atom<String>(passpointVenueName.getAsDuple()));
            String url = String.valueOf(index) + ":" + passpointVenueName.getVenueUrl();
            venueUrls.add(new Atom<String>(url));
            index++;
        }
        com.vmware.ovsdb.protocol.operation.notation.Set venueNameSet = com.vmware.ovsdb.protocol.operation.notation.Set
                .of(venueNames);
        com.vmware.ovsdb.protocol.operation.notation.Set venueUrlSet = com.vmware.ovsdb.protocol.operation.notation.Set
                .of(venueUrls);
        rowColumns.put("venue_name", venueNameSet);
        rowColumns.put("venue_url", venueUrlSet);

        PasspointVenueTypeAssignment passpointVenueTypeAssignment = passpointVenueProfile.getVenueTypeAssignment();
        String groupType = String.valueOf(passpointVenueTypeAssignment.getVenueGroupId()) + ":"
                + passpointVenueTypeAssignment.getVenueTypeId();

        rowColumns.put("venue_group_type", new Atom<>(groupType));
    }

    /**
     * Get's the Venue Profile for the hotspot, helper method
     *
     * @param hs20cfg
     * @param hs2Profile
     * @return
     */
    PasspointVenueProfile getVenueProfileForPasspoint(OpensyncAPHotspot20Config hs20cfg, PasspointProfile hs2Profile) {
        Profile venue = hs20cfg.getHotspot20VenueSet().stream().filter(new Predicate<Profile>() {

            @Override
            public boolean test(Profile t) {
                return t.getId() == hs2Profile.getPasspointVenueProfileId();
            }

        }).findFirst().get();

        PasspointVenueProfile passpointVenueProfile = (PasspointVenueProfile) venue.getDetails();
        return passpointVenueProfile;
    }

    /**
     * Get the UUIDs for the associated access Wifi_VIF_Config parents, as well
     * as the osu_ssid for the "OPEN" Wifi_VIF_Config used to connect to the
     * passpoint
     *
     * @param ovsdbClient
     * @param opensyncApConfig
     * @param rowColumns
     * @param hs2Profile
     */
    void getVifInformationForPasspointConfiguration(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig,
            Map<String, Value> rowColumns, PasspointProfile hs2Profile) {
        Map<String, WifiVifConfigInfo> vifConfigMap = ovsdbGet.getProvisionedWifiVifConfigs(ovsdbClient);

        Set<Uuid> vifConfigs = new HashSet<>();
        List<Atom<String>> hessids = new ArrayList<>();

        for (Profile ssidProfile : opensyncApConfig.getSsidProfile()) {
            if (hs2Profile.getAssociatedAccessSsidProfileIds().contains(ssidProfile.getId())) {

                String accessSsidProfileName = ((SsidConfiguration) ssidProfile.getDetails()).getSsid();

                for (WifiVifConfigInfo vifConfig : vifConfigMap.values()) {
                    if (vifConfig.ssid.equals(accessSsidProfileName)) {
                        vifConfigs.add(vifConfig.uuid);
                    }
                }

                List<String> vifStates = ovsdbGet.getWifiVifStates(ovsdbClient, accessSsidProfileName);
                for (String mac : vifStates) {
                    hessids.add(new Atom<>(mac));
                }

            }
        }

        if (vifConfigs.size() > 0) {
            com.vmware.ovsdb.protocol.operation.notation.Set vifConfigUuids = com.vmware.ovsdb.protocol.operation.notation.Set
                    .of(vifConfigs);
            rowColumns.put("vif_config", vifConfigUuids);
        }

        if (hessids.size() > 0) {
            rowColumns.put("hessid", new Atom<>(hessids.get(0)));
        }

        for (Profile ssidProfile : opensyncApConfig.getSsidProfile()) {
            if (hs2Profile.getOsuSsidProfileId() != null) {
                if (ssidProfile.getId() == hs2Profile.getOsuSsidProfileId()) {
                    rowColumns.put("osu_ssid", new Atom<>(((SsidConfiguration) ssidProfile.getDetails()).getSsid()));
                    break;
                }
            }
        }
    }

    /**
     * Configure a Hotspot20 Passpoint for AP
     *
     * @param ovsdbClient
     * @param opensyncApConfig
     */
    void provisionHotspot20Config(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig) {
        try {
            DatabaseSchema schema = ovsdbClient.getSchema(ovsdbName).get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            if (schema.getTables().containsKey(hotspot20ConfigDbTable)
                    && schema.getTables().get(hotspot20ConfigDbTable) != null) {
                Map<String, Hotspot20Config> hotspot20ConfigMap = ovsdbGet.getProvisionedHotspot20Configs(ovsdbClient);

                OpensyncAPHotspot20Config hs20cfg = opensyncApConfig.getHotspotConfig();

                if (hs20cfg.getHotspot20ProfileSet() != null) {
                    List<Operation> operations = new ArrayList<>();
                    for (Profile hotspotProfile : hs20cfg.getHotspot20ProfileSet()) {

                        Map<String, Value> rowColumns = new HashMap<>();

                        PasspointProfile hs2Profile = (PasspointProfile) hotspotProfile.getDetails();

                        getPasspointConfigurationInformationFromDependencies(ovsdbClient, opensyncApConfig, hs20cfg,
                                hotspotProfile, rowColumns, hs2Profile);

                        rowColumns.put("deauth_request_timeout", new Atom<>(hs2Profile.getDeauthRequestTimeout()));
                        if (hs2Profile.getTermsAndConditionsFile() != null) {
                            rowColumns.put("tos", new Atom<>(hs2Profile.getTermsAndConditionsFile().getApExportUrl()));
                        }
                        rowColumns.put("enable", new Atom<>(hs2Profile.isEnableInterworkingAndHs20()));
                        rowColumns.put("network_auth_type",
                                new Atom<>("0" + hs2Profile.getNetworkAuthenticationType().getId()));
                        if (hs2Profile.getGasAddr3Behaviour() != null) {
                            rowColumns.put("gas_addr3_behavior", new Atom<>(hs2Profile.getGasAddr3Behaviour().getId()));
                        }
                        rowColumns.put("operating_class", new Atom<>(hs2Profile.getOperatingClass()));
                        rowColumns.put("anqp_domain_id", new Atom<>(hs2Profile.getAnqpDomainId()));
                        rowColumns.put("asra", new Atom<>(hs2Profile.getAdditionalStepsRequiredForAccess() == 1 ? true:false));
                        rowColumns.put("disable_dgaf", new Atom<>(hs2Profile.isDisableDownstreamGroupAddressedForwarding()));
                        rowColumns.put("esr", new Atom<>(hs2Profile.isEmergencyServicesReachable()));
                        if (hs2Profile.getHessid() != null) {
                            rowColumns.put("hessid", new Atom<>(hs2Profile.getHessid().getAddressAsString()));
                        }
                        rowColumns.put("internet", new Atom<>(hs2Profile.isInternetConnectivity()));
                        if (hs2Profile.getQosMapSetConfiguration() != null) {
                            rowColumns.put("qos_map_set", new Atom<>(String.join(",", hs2Profile.getQosMapSetConfiguration())));
                        }
                        rowColumns.put("uesa", new Atom<>(hs2Profile.isUnauthenticatedEmergencyServiceAccessible()));
                        Set<Atom<String>> connectionCapabilities = new HashSet<>();
                        hs2Profile.getConnectionCapabilitySet().stream()
                        .forEach(c -> connectionCapabilities
                                .add(new Atom<>(c.getConnectionCapabilitiesIpProtocol().getId() + ":"
                                        + c.getConnectionCapabilitiesPortNumber() + ":"
                                        + c.getConnectionCapabilitiesStatus().getId())));
                        com.vmware.ovsdb.protocol.operation.notation.Set connectionCapabilitySet = com.vmware.ovsdb.protocol.operation.notation.Set
                                .of(connectionCapabilities);
                        rowColumns.put("connection_capability", connectionCapabilitySet);

                        // access_network_type to add when supported by AP
                        if (ovsdbClient.getSchema(ovsdbName).get().getTables().get(hotspot20ConfigDbTable).getColumns().containsKey("access_network_type")) {
                            if (hs2Profile.getAccessNetworkType() != null) {
                                rowColumns.put("access_network_type", new Atom<>(hs2Profile.getAccessNetworkType().getId()));
                            }
                        }
                        
                        
                        // # format: <1-octet encoded value as hex str>
                        // # (ipv4_type & 0x3f) << 2 | (ipv6_type & 0x3) << 2
                        // 0x3f = 63 in decimal
                        // 0x3 = 3 in decimal
                        if (PasspointIPv6AddressType.getByName(
                                hs2Profile.getIpAddressTypeAvailability()) != PasspointIPv6AddressType.UNSUPPORTED) {
                            int availability = PasspointIPv6AddressType
                                    .getByName(hs2Profile.getIpAddressTypeAvailability()).getId();
                            String hexString = Integer.toHexString((availability & 3) << 2);
                            rowColumns.put("ipaddr_type_availability", new Atom<>(hexString));
                        } else if (PasspointIPv4AddressType.getByName(
                                hs2Profile.getIpAddressTypeAvailability()) != PasspointIPv4AddressType.UNSUPPORTED) {
                            int availability = PasspointIPv4AddressType
                                    .getByName(hs2Profile.getIpAddressTypeAvailability()).getId();
                            String hexString = Integer.toHexString((availability & 63) << 2);
                            rowColumns.put("ipaddr_type_availability", new Atom<>(hexString));
                        }

                        Row row = new Row(rowColumns);

                        Insert newHs20Config = new Insert(hotspot20ConfigDbTable, row);

                        operations.add(newHs20Config);

                    }

                    CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
                    OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

                    for (OperationResult res : result) {
                        LOG.debug("provisionHotspot20Config Op Result {}", res);
                        if (res instanceof InsertResult) {
                            LOG.info("provisionHotspot20Config insert new row result {}", (res));
                            // for insert, make sure it is actually in the table
                            confirmRowExistsInTable(ovsdbClient, ((InsertResult) res).getUuid(),
                                    hotspot20ConfigDbTable);
                        }
                    }

                }

                LOG.info("Current Hotspot20_Config {}", hotspot20ConfigMap);
            } else {
                LOG.info("Table {} not present in {}. Cannot provision Hotspot20_Config", hotspot20ConfigDbTable,
                        ovsdbName);
            }
        } catch (InterruptedException | ExecutionException | TimeoutException | OvsdbClientException e) {
            LOG.error("Error in provisionHotspot20Config", e);
            throw new RuntimeException(e);
        }

    }

    /**
     * Provision the OSU Providers in the Hotspot20_OSU_Providers ovsdb table.
     *
     * @param ovsdbClient
     * @param opensyncApConfig
     */
    void provisionHotspot20OsuProviders(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig) {
        try {
            DatabaseSchema schema = ovsdbClient.getSchema(ovsdbName).get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            if (schema.getTables().containsKey(hotspot20OsuProvidersDbTable)
                    && schema.getTables().get(hotspot20OsuProvidersDbTable) != null) {
                Map<String, Hotspot20OsuProviders> osuProviders = ovsdbGet
                        .getProvisionedHotspot20OsuProviders(ovsdbClient);

                OpensyncAPHotspot20Config hs20cfg = opensyncApConfig.getHotspotConfig();
                Set<Operation> operations = new HashSet<>();
                if (hs20cfg.getHotspot20ProviderSet() != null && hs20cfg.getHotspot20ProviderSet().size() > 0) {

                    for (Profile provider : hs20cfg.getHotspot20ProviderSet()) {
                        PasspointOsuProviderProfile providerProfile = (PasspointOsuProviderProfile) provider
                                .getDetails();
                        String apOsuProviderName = OvsdbToWlanCloudTypeMappingUtility
                                .getApOsuProviderStringFromOsuProviderName(provider.getName());
                        Map<String, Value> rowColumns = new HashMap<>();
                        rowColumns.put("osu_nai", new Atom<>(providerProfile.getOsuNaiStandalone()));

                        rowColumns.put("osu_nai2", new Atom<>(providerProfile.getOsuNaiShared()));

                        rowColumns.put("osu_provider_name", new Atom<>(apOsuProviderName));

                        getOsuIconUuidsForOsuProvider(ovsdbClient, providerProfile, rowColumns);
                        getOsuProviderFriendlyNames(providerProfile, rowColumns);
                        getOsuProviderMethodList(providerProfile, rowColumns);
                        if (providerProfile.getOsuServerUri() != null) {
                            rowColumns.put("server_uri", new Atom<>(providerProfile.getOsuServerUri()));
                        }
                        getOsuProviderServiceDescriptions(providerProfile, rowColumns);

                        Row row = new Row(rowColumns);

                        if (!osuProviders.containsKey(apOsuProviderName)) {
                            Insert newOsuProvider = new Insert(hotspot20OsuProvidersDbTable, row);
                            operations.add(newOsuProvider);
                        } else {
                            List<Condition> conditions = new ArrayList<>();
                            conditions.add(new Condition("server_uri", Function.EQUALS,
                                    new Atom<>(providerProfile.getOsuServerUri())));
                            Update updatedOsuProvider = new Update(hotspot20OsuProvidersDbTable, conditions, row);
                            operations.add(updatedOsuProvider);
                        }

                    }

                }

                if (operations.size() > 0) {
                    CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName,
                            List.copyOf(operations));
                    OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

                    for (OperationResult res : result) {
                        LOG.debug("provisionHotspot20OsuProviders Op Result {}", res);
                        if (res instanceof InsertResult) {
                            LOG.info("provisionHotspot20OsuProviders insert new row result {}", (res));
                            // for insert, make sure it is actually in the table
                            confirmRowExistsInTable(ovsdbClient, ((InsertResult) res).getUuid(),
                                    hotspot20OsuProvidersDbTable);
                        } else if (res instanceof UpdateResult) {
                            LOG.info("provisionHotspot20OsuProviders update row result {}", (res));

                        }
                    }
                }

            } else {
                LOG.info("Table {} not present in {}. Cannot provision Hotspot20_OSU_Providers",
                        hotspot20OsuProvidersDbTable, ovsdbName);
            }
        } catch (InterruptedException | ExecutionException | TimeoutException | OvsdbClientException e) {
            LOG.error("Error in provisionHotspot20OsuProviders", e);
            throw new RuntimeException(e);
        }

    }

    void provisionHotspot2IconConfig(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig) {
        try {
            DatabaseSchema schema = ovsdbClient.getSchema(ovsdbName).get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            if (schema.getTables().containsKey(hotspot20IconConfigDbTable)
                    && schema.getTables().get(hotspot20IconConfigDbTable) != null) {
                Map<String, Hotspot20IconConfig> osuIconConfigs = ovsdbGet
                        .getProvisionedHotspot20IconConfig(ovsdbClient);

                OpensyncAPHotspot20Config hs20cfg = opensyncApConfig.getHotspotConfig();
                Set<Operation> operations = new HashSet<>();
                if (hs20cfg.getHotspot20ProviderSet() != null && hs20cfg.getHotspot20ProviderSet().size() > 0) {

                    for (Profile provider : hs20cfg.getHotspot20ProviderSet()) {
                        PasspointOsuProviderProfile providerProfile = (PasspointOsuProviderProfile) provider
                                .getDetails();
                        for (PasspointOsuIcon passpointOsuIcon : providerProfile.getOsuIconList()) {
                            Map<String, Value> rowColumns = new HashMap<>();
                            rowColumns.put("name", new Atom<>(passpointOsuIcon.getIconName()));
                            if (schema.getTables().get(hotspot20IconConfigDbTable).getColumns().containsKey("path")) {
                                rowColumns.put("path", new Atom<>(passpointOsuIcon.getFilePath()));
                            }
                            rowColumns.put("url", new Atom<>(passpointOsuIcon.getImageUrl()));
                            rowColumns.put("lang_code", new Atom<>(passpointOsuIcon.getLanguageCode()));
                            rowColumns.put("height", new Atom<>(passpointOsuIcon.getIconHeight()));
                            rowColumns.put("img_type", new Atom<>(PasspointOsuIcon.ICON_TYPE));
                            rowColumns.put("width", new Atom<>(passpointOsuIcon.getIconWidth()));
                            if (passpointOsuIcon.getImageUrl() != null) {
                                String md5Hex = DigestUtils.md5Hex(passpointOsuIcon.getImageUrl()).toUpperCase();
                                if (schema.getTables().get(hotspot20IconConfigDbTable).getColumns()
                                        .containsKey("icon_config_name")) {
                                    rowColumns.put("icon_config_name", new Atom<>(md5Hex));
                                }
                            }
                            Row row = new Row(rowColumns);

                            if (!osuIconConfigs.containsKey(passpointOsuIcon.getImageUrl())) {
                                Insert newHs20Config = new Insert(hotspot20IconConfigDbTable, row);
                                operations.add(newHs20Config);
                            } else {
                                List<Condition> conditions = new ArrayList<>();
                                conditions.add(new Condition("url", Function.EQUALS,
                                        new Atom<>(passpointOsuIcon.getImageUrl())));
                                Update newHs20Config = new Update(hotspot20IconConfigDbTable, conditions, row);
                                operations.add(newHs20Config);
                            }
                        }
                    }
                }
                if (operations.size() > 0) {
                    CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName,
                            List.copyOf(operations));
                    OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

                    for (OperationResult res : result) {
                        LOG.debug("provisionHotspot20Config Op Result {}", res);
                        if (res instanceof InsertResult) {
                            LOG.info("provisionHotspot20Config insert new row result {}", (res));
                        } else if (res instanceof UpdateResult) {
                            LOG.info("provisionHotspot20Config update row result {}", (res));
                        }
                    }
                }
            } else {
                LOG.info("Table {} not present in {}. Cannot provision Hotspot20_Icon_Config",
                        hotspot20IconConfigDbTable, ovsdbName);
            }
        } catch (InterruptedException | ExecutionException | TimeoutException | OvsdbClientException e) {
            LOG.error("Error in provisionHotspot2IconConfig", e);
            throw new RuntimeException(e);
        }
    }

    void removeAllHotspot20Config(OvsdbClient ovsdbClient) {
        try {
//            DatabaseSchema schema = ovsdbClient.getSchema(ovsdbName).get(ovsdbTimeoutSec, TimeUnit.SECONDS);
//            if (schema.getTables().containsKey(hotspot20ConfigDbTable)
//                    && schema.getTables().get(hotspot20ConfigDbTable) != null) {
                List<Operation> operations = new ArrayList<>();
                operations.add(new Delete(hotspot20ConfigDbTable));
                CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
                OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Removed all existing hotspot configs from {}:", hotspot20ConfigDbTable);
                    for (OperationResult res : result) {
                        LOG.debug("Op Result {}", res);
                    }
                }
//            }
        } catch (InterruptedException | ExecutionException | TimeoutException | OvsdbClientException e) {
            LOG.error("Error in removeAllHotspot20Config", e);
            throw new RuntimeException(e);
        }
    }

    void removeAllHotspot20IconConfig(OvsdbClient ovsdbClient) {
        try {
            DatabaseSchema schema = ovsdbClient.getSchema(ovsdbName).get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            if (schema.getTables().containsKey(hotspot20IconConfigDbTable)
                    && schema.getTables().get(hotspot20IconConfigDbTable) != null) {
                List<Operation> operations = new ArrayList<>();

                operations.add(new Delete(hotspot20IconConfigDbTable));

                CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
                OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Removed all existing hotspot icon configs from {}:", hotspot20IconConfigDbTable);

                    for (OperationResult res : result) {
                        LOG.debug("Op Result {}", res);
                    }
                }
            }
        } catch (InterruptedException | ExecutionException | TimeoutException | OvsdbClientException e) {
            LOG.error("Error in removeAllHotspot20IconConfig", e);
            throw new RuntimeException(e);
        }

    }

    void removeAllHotspot20OsuProviders(OvsdbClient ovsdbClient) {
        try {
            DatabaseSchema schema = ovsdbClient.getSchema(ovsdbName).get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            if (schema.getTables().containsKey(hotspot20OsuProvidersDbTable)
                    && schema.getTables().get(hotspot20OsuProvidersDbTable) != null) {
                List<Operation> operations = new ArrayList<>();

                operations.add(new Delete(hotspot20OsuProvidersDbTable));

                CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
                OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Removed all existing hotspot osu providers from {}:", hotspot20OsuProvidersDbTable);

                    for (OperationResult res : result) {
                        LOG.debug("Op Result {}", res);
                    }
                }
            }
        } catch (InterruptedException | ExecutionException | TimeoutException | OvsdbClientException e) {
            LOG.error("Error in removeAllHotspot20OsuProviders", e);
            throw new RuntimeException(e);
        }

    }

    void removeAllPasspointConfigs(OvsdbClient ovsdbClient) {
        removeAllHotspot20Config(ovsdbClient);
        removeAllHotspot20OsuProviders(ovsdbClient);
        removeAllHotspot20IconConfig(ovsdbClient);
    }

}
