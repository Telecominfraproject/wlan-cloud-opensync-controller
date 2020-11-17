package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.customer.models.Customer;
import com.telecominfraproject.wlan.profile.models.Profile;
import com.telecominfraproject.wlan.profile.models.ProfileType;
import com.telecominfraproject.wlan.profile.network.models.ApNetworkConfiguration;
import com.telecominfraproject.wlan.profile.passpoint.models.PasspointDuple;
import com.telecominfraproject.wlan.profile.passpoint.models.PasspointMccMnc;
import com.telecominfraproject.wlan.profile.passpoint.models.PasspointProfile;
import com.telecominfraproject.wlan.profile.passpoint.models.operator.PasspointOperatorProfile;
import com.telecominfraproject.wlan.profile.passpoint.models.provider.PasspointNaiRealmInformation;
import com.telecominfraproject.wlan.profile.passpoint.models.provider.PasspointOsuIcon;
import com.telecominfraproject.wlan.profile.passpoint.models.provider.PasspointOsuProviderProfile;
import com.telecominfraproject.wlan.profile.passpoint.models.venue.PasspointVenueProfile;
import com.telecominfraproject.wlan.profile.rf.models.RfConfiguration;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration.SecureMode;
import com.vmware.ovsdb.protocol.operation.notation.Atom;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;
import com.vmware.ovsdb.protocol.operation.notation.Value;
import com.vmware.ovsdb.protocol.operation.result.InsertResult;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.protocol.operation.result.SelectResult;
import com.vmware.ovsdb.protocol.operation.result.UpdateResult;

public class OvsdbDaoTestUtilities {


    // Static creation of Profiles and Results to use with the OvsdbDao JUnit tests.

    static void createPasspointHotspot(Customer customer, Profile passpointHotspotConfig,
            Profile passpointOperatorProfile, Profile passpointVenueProfile, Profile hotspot20IdProviderProfile,
            Profile hotspot20IdProviderProfile2, Profile profileSsidPsk, Profile profileSsidOsu,
            Profile hotspotProfileAp) {

        profileSsidPsk = createPasspointAccessSsid(customer);
        profileSsidOsu = createPasspointOsuSsid(customer);

        passpointOperatorProfile = createPasspointOperatorProfile(customer);

        passpointVenueProfile = createPasspointVenueProfile(customer);

        hotspot20IdProviderProfile = createPasspointIdProviderProfile(customer, hotspot20IdProviderProfile,
                "TipWlan-Hotspot20-OSU-Provider", "Rogers AT&T Wireless", "Canada", "ca", 302, 720, "rogers.com", 1);

        hotspot20IdProviderProfile2 = createPasspointIdProviderProfile(customer, hotspot20IdProviderProfile2,
                "TipWlan-Hotspot20-OSU-Provider-2", "Telus Mobility", "Canada", "ca", 302, 220, "telus.com", 1);


        profileSsidOsu.getChildProfileIds().add(hotspot20IdProviderProfile.getId());
        profileSsidOsu.getChildProfileIds().add(hotspot20IdProviderProfile2.getId());

        passpointHotspotConfig = createPasspointHotspotConfig(customer, hotspot20IdProviderProfile2,
                hotspot20IdProviderProfile, passpointOperatorProfile, passpointVenueProfile, profileSsidPsk,
                profileSsidOsu);

        hotspotProfileAp = createPasspointApProfile(customer, profileSsidPsk, profileSsidOsu);
    }

    static Profile createPasspointHotspotConfig(Customer customer, Profile hotspot20IdProviderProfile2,
            Profile hotspot20IdProviderProfile, Profile passpointOperatorProfile, Profile passpointVenueProfile,
            Profile profileSsidPsk, Profile profileSsidOpen) {
        Profile passpointHotspotConfig;
        passpointHotspotConfig = new Profile();
        passpointHotspotConfig.setCustomerId(customer.getId());
        passpointHotspotConfig.setName("TipWlan-Hotspot20-Config");
        passpointHotspotConfig.setProfileType(ProfileType.passpoint);
        Set<Long> passpointHotspotConfigChildIds = new HashSet<>();
        passpointHotspotConfigChildIds.add(passpointOperatorProfile.getId());
        passpointHotspotConfigChildIds.add(passpointVenueProfile.getId());
        passpointHotspotConfigChildIds.add(hotspot20IdProviderProfile.getId());
        passpointHotspotConfigChildIds.add(hotspot20IdProviderProfile2.getId());
        passpointHotspotConfig.setChildProfileIds(passpointHotspotConfigChildIds);
        passpointHotspotConfig.setDetails(PasspointProfile.createWithDefaults());
        Set<String> providerNames = new HashSet<>();
        providerNames.add(hotspot20IdProviderProfile.getName());
        providerNames.add(hotspot20IdProviderProfile2.getName());
        ((PasspointProfile) passpointHotspotConfig.getDetails()).setIdProviderProfileNames(providerNames);
        ((PasspointProfile) passpointHotspotConfig.getDetails())
                .setOperatorProfileName(passpointOperatorProfile.getName());
        ((PasspointProfile) passpointHotspotConfig.getDetails()).setVenueProfileName(passpointVenueProfile.getName());
        ((PasspointProfile) passpointHotspotConfig.getDetails()).setOsuSsidName(profileSsidOpen.getName());
        profileSsidPsk.getChildProfileIds().add(passpointHotspotConfig.getId());
        return passpointHotspotConfig;
    }

    static Profile createPasspointIdProviderProfile(Customer customer, Profile providerProfile, String providerName,
            String network, String country, String iso, int mcc, int mnc, String naiRealm, int countryCode) {
        Profile hotspot20IdProviderProfile;
        hotspot20IdProviderProfile = new Profile();
        hotspot20IdProviderProfile.setCustomerId(customer.getId());
        hotspot20IdProviderProfile.setName(providerName);
        hotspot20IdProviderProfile.setProfileType(ProfileType.passpoint_osu_id_provider);
        PasspointMccMnc passpointMccMnc = PasspointMccMnc.createWithDefaults();
        passpointMccMnc.setMcc(mcc);
        passpointMccMnc.setMnc(mnc);
        passpointMccMnc.setIso(iso);
        passpointMccMnc.setCountry(country);
        passpointMccMnc.setCountryCode(1);
        passpointMccMnc.setNetwork(network);
        List<PasspointMccMnc> mccMncList = new ArrayList<>();
        mccMncList.add(passpointMccMnc);
        Set<String> naiRealms = new HashSet<>();
        naiRealms.add(naiRealm);
        naiRealm.split(".");
        List<Byte> roamingOi = new ArrayList<>();
        roamingOi.add(Byte.valueOf("1"));
        roamingOi.add(Byte.valueOf("2"));
        roamingOi.add(Byte.valueOf("3"));
        roamingOi.add(Byte.valueOf("4"));
        hotspot20IdProviderProfile = createOsuProviderProfile(customer, hotspot20IdProviderProfile, mccMncList,
                naiRealms, "https://example.com/osu/" + naiRealm.split(".com")[0], naiRealm.split(".com")[0], naiRealm,
                roamingOi);
        return hotspot20IdProviderProfile;
    }

    static Profile createPasspointVenueProfile(Customer customer) {
        Profile passpointVenueProfile;
        passpointVenueProfile = new Profile();
        passpointVenueProfile.setCustomerId(customer.getId());
        passpointVenueProfile.setName("TipWlan-Hotspot20-Venue");
        passpointVenueProfile.setProfileType(ProfileType.passpoint_venue);
        passpointVenueProfile.setDetails(PasspointVenueProfile.createWithDefaults());
        return passpointVenueProfile;
    }

    static Profile createPasspointOperatorProfile(Customer customer) {
        Profile passpointOperatorProfile;
        passpointOperatorProfile = new Profile();
        passpointOperatorProfile.setCustomerId(customer.getId());
        passpointOperatorProfile.setName("TipWlan-Hotspot20-Operator");
        passpointOperatorProfile.setProfileType(ProfileType.passpoint_operator);
        passpointOperatorProfile.setDetails(PasspointOperatorProfile.createWithDefaults());
        return passpointOperatorProfile;
    }

    static Profile createPasspointAccessSsid(Customer customer) {
        Profile profileSsidPsk;
        profileSsidPsk = new Profile();
        profileSsidPsk.setCustomerId(customer.getId());
        profileSsidPsk.setName("TipWlan-cloud-hotspot-access");
        SsidConfiguration ssidConfigPsk = SsidConfiguration.createWithDefaults();
        Set<RadioType> appliedRadiosPsk = new HashSet<RadioType>();
        appliedRadiosPsk.add(RadioType.is5GHzL);
        appliedRadiosPsk.add(RadioType.is5GHzU);
        ssidConfigPsk.setSsid("TipWlan-cloud-hotspot-access");
        ssidConfigPsk.setAppliedRadios(appliedRadiosPsk);
        ssidConfigPsk.setSecureMode(SecureMode.wpa2PSK);
        ssidConfigPsk.setKeyStr("testing123");
        profileSsidPsk.setDetails(ssidConfigPsk);
        return profileSsidPsk;
    }

    static Profile createPasspointOsuSsid(Customer customer) {
        Profile profileSsidPsk;
        profileSsidPsk = new Profile();
        profileSsidPsk.setCustomerId(customer.getId());
        profileSsidPsk.setName("TipWlan-cloud-hotspot-osu");
        SsidConfiguration ssidConfigPsk = SsidConfiguration.createWithDefaults();
        Set<RadioType> appliedRadiosPsk = new HashSet<RadioType>();
        appliedRadiosPsk.add(RadioType.is2dot4GHz);
        ssidConfigPsk.setSsid("TipWlan-cloud-hotspot-osu");
        ssidConfigPsk.setAppliedRadios(appliedRadiosPsk);
        ssidConfigPsk.setSecureMode(SecureMode.open);
        profileSsidPsk.setDetails(ssidConfigPsk);
        return profileSsidPsk;
    }

    static Profile createPasspointApProfile(Customer customer, Profile profileSsidPsk, Profile profileSsidOpen) {

        Profile hotspotProfileAp = new Profile();
        hotspotProfileAp.setCustomerId(customer.getId());
        hotspotProfileAp.setName("HotspotProfileAp");
        hotspotProfileAp.setDetails(ApNetworkConfiguration.createWithDefaults());
        hotspotProfileAp.getChildProfileIds().add(profileSsidPsk.getId());
        hotspotProfileAp.getChildProfileIds().add(profileSsidOpen.getId());
        hotspotProfileAp.getChildProfileIds().add(createPasspointRfProfile(customer).getId());
        return hotspotProfileAp;

    }

    static Profile createPasspointRfProfile(Customer customer) {

        Profile profileRf = new Profile();
        profileRf.setCustomerId(customer.getId());
        profileRf.setName("TipWlan-rf-passpoint");
        RfConfiguration rfConfig = RfConfiguration.createWithDefaults();
        rfConfig.getRfConfigMap().forEach((x, y) -> y.setRf("TipWlan-rf-passpoint"));
        profileRf.setDetails(rfConfig);

        return profileRf;
    }

    static Profile createOsuProviderProfile(Customer customer, Profile hotspot20IdProviderProfile,
            List<PasspointMccMnc> mccMncList, Set<String> realms, String serverUri, String suffix, String domainName,
            List<Byte> roamingOi) {

        PasspointOsuProviderProfile passpointIdProviderProfile = PasspointOsuProviderProfile.createWithDefaults();

        passpointIdProviderProfile.setMccMncList(mccMncList);
        PasspointOsuIcon icon1 = new PasspointOsuIcon();
        icon1.setIconLocale(Locale.CANADA);
        icon1.setIconWidth(32);
        icon1.setIconHeight(32);
        icon1.setLanguageCode(Locale.CANADA.getISO3Language());
        icon1.setIconName("icon32eng");
        icon1.setImageUrl("https://localhost:9096/icon32eng.png");
        icon1.setFilePath("/tmp/icon32eng.png");
        PasspointOsuIcon icon2 = new PasspointOsuIcon();
        icon2.setIconLocale(Locale.CANADA_FRENCH);
        icon2.setIconWidth(32);
        icon2.setIconHeight(32);
        icon2.setLanguageCode(Locale.CANADA_FRENCH.getISO3Language());
        icon2.setIconName("icon32fra");
        icon2.setImageUrl("https://localhost:9096/icon32fra.png");
        icon2.setFilePath("/tmp/icon32fra.png");
        PasspointOsuIcon icon3 = new PasspointOsuIcon();
        icon3.setIconLocale(Locale.US);
        icon3.setIconWidth(32);
        icon3.setIconHeight(32);
        icon3.setLanguageCode(Locale.US.getISO3Language());
        icon3.setIconName("icon32usa");
        icon3.setImageUrl("https://localhost:9096/icon32usa.png");
        icon3.setFilePath("/tmp/icon32usa.png");
        List<PasspointOsuIcon> osuIconList = new ArrayList<>();
        osuIconList.add(icon1);
        osuIconList.add(icon2);
        osuIconList.add(icon3);
        passpointIdProviderProfile.setOsuIconList(osuIconList);

        passpointIdProviderProfile.setRoamingOi(roamingOi);
        List<PasspointNaiRealmInformation> naiRealmList = new ArrayList<>();

        PasspointNaiRealmInformation naiRealmInfo = PasspointNaiRealmInformation.createWithDefaults();
        naiRealmInfo.setNaiRealms(realms);

        naiRealmList.add(naiRealmInfo);
        passpointIdProviderProfile.setNaiRealmList(naiRealmList);
        passpointIdProviderProfile.setDomainName(domainName);
        passpointIdProviderProfile.setOsuNaiStandalone("anonymous@" + domainName);
        passpointIdProviderProfile.setOsuNaiShared("anonymous@" + domainName);
        List<Integer> methodList = new ArrayList<>();
        methodList.add(1);
        methodList.add(0);
        passpointIdProviderProfile.setOsuMethodList(methodList);
        PasspointDuple enOsuProvider = PasspointDuple.createWithDefaults();
        enOsuProvider.setLocale(Locale.CANADA);
        enOsuProvider.setDupleName("Example provider " + suffix);
        PasspointDuple frOsuProvider = PasspointDuple.createWithDefaults();
        frOsuProvider.setLocale(Locale.CANADA_FRENCH);
        frOsuProvider.setDupleName("Exemple de fournisseur " + suffix);
        List<PasspointDuple> friendlyNameList = new ArrayList<>();
        friendlyNameList.add(enOsuProvider);
        friendlyNameList.add(frOsuProvider);
        passpointIdProviderProfile.setOsuFriendlyName(friendlyNameList);
        List<PasspointDuple> osuServiceDescription = new ArrayList<>();
        PasspointDuple enService = PasspointDuple.createWithDefaults();
        enService.setLocale(Locale.CANADA);
        enService.setDupleName("Example services " + suffix);
        osuServiceDescription.add(enService);
        PasspointDuple frService = PasspointDuple.createWithDefaults();
        frService.setLocale(Locale.CANADA_FRENCH);
        frService.setDupleName("Exemples de services " + suffix);
        osuServiceDescription.add(frService);
        passpointIdProviderProfile.setOsuServiceDescription(osuServiceDescription);
        passpointIdProviderProfile.setOsuServerUri(serverUri);

        hotspot20IdProviderProfile.setDetails(passpointIdProviderProfile);
        return hotspot20IdProviderProfile;
    }

    static OperationResult[] hs20Config() {
        List<Row> ret = new ArrayList<>();

        Map<String, Value> columns = new HashMap<>();
        columns.put("_uuid", new Atom<>(Uuid.of(UUID.fromString("28f2b88e-d10d-4cae-832d-784210940709"))));
        columns.put("_version", new Atom<>(Uuid.of(UUID.randomUUID())));
        columns.put("anqp_domain_id", new Atom<>(1234));
        columns.put("connection_capability", new Atom<>(" 6:8888:1"));
        columns.put("deauth_request_timeout", new Atom<>(0));
        columns.put("domain_name",
                com.vmware.ovsdb.protocol.operation.notation.Set.of(new Atom<>("rogers.com"), new Atom<>("telus.com")));
        columns.put("enable", new Atom<>(true));
        columns.put("gas_addr3_behavior", new Atom<>(0));
        columns.put("hessid", new Atom<>("26:f5:a2:ef:2e:56"));
        columns.put("ipaddr_type_availability", new Atom<>(4));
        columns.put("mcc_mnc", new Atom<>("302,720;302,220"));
        columns.put("nai_realm", com.vmware.ovsdb.protocol.operation.notation.Set
                .of(new Atom<>("0,rogers.com,21[5:7][2:4],13[5:6]"), new Atom<>("0,telus.com,21[5:7][2:4],13[5:6]")));
        columns.put("network_auth_type", new Atom<>("00"));
        columns.put("operating_class", new Atom<>(0));
        columns.put("operator_friendly_name",
                com.vmware.ovsdb.protocol.operation.notation.Set.of(new Atom<>("eng:Default friendly passpoint_operator name"),
                        new Atom<>("fra:Nom de l'opérateur convivial par défaut")));
        columns.put("operator_icons",
                com.vmware.ovsdb.protocol.operation.notation.Set.of(
                        new Atom<>(Uuid.of(UUID.fromString("5f2d0e46-92bd-43a1-aa66-94474deb2212"))),
                        new Atom<>(Uuid.of(UUID.fromString("7cf892c2-3f04-4851-986c-a7b7d8ad1dfa"))),
                        new Atom<>(Uuid.of(UUID.fromString("9449e6cf-de62-4805-855b-3fc9bb5cd3ec")))));
        columns.put("osen", new Atom<>(false));
        columns.put("osu_providers",
                com.vmware.ovsdb.protocol.operation.notation.Set.of(
                        new Atom<>(Uuid.of(UUID.fromString("33b78c84-3242-4477-831f-185c6532cfda"))),
                        new Atom<>(Uuid.of(UUID.fromString("ae51393c-f9e5-4021-af73-c5ad4b751f88")))));
        columns.put("osu_ssid", new Atom<>("ssid-open"));
        columns.put("roaming_oi",
                com.vmware.ovsdb.protocol.operation.notation.Set.of(new Atom<>("11223344"), new Atom<>("234433")));
        columns.put("tos", new Atom<>("https://localhost:9091/filestore/termsAndConditions"));
        columns.put("venue_group_type", new Atom<>("2:8"));
        columns.put("venue_name", com.vmware.ovsdb.protocol.operation.notation.Set.of(new Atom<>("eng:Example passpoint_venue"),
                new Atom<>("fra:Exemple de lieu")));
        columns.put("venue_url", com.vmware.ovsdb.protocol.operation.notation.Set
                .of(new Atom<>("1:http://www.example.com/info-fra"), new Atom<>("2:http://www.example.com/info-eng")));
        columns.put("vif_config",
                com.vmware.ovsdb.protocol.operation.notation.Set.of(
                        new Atom<>(Uuid.of(UUID.fromString("66abe4b3-2a26-4769-b684-da70e2392a07"))),
                        new Atom<>(Uuid.of(UUID.fromString("fad30b59-fada-41fc-ad62-2afca67cf9d5")))));

        OperationResult[] operationResult = new OperationResult[1];
        SelectResult selectResult = new SelectResult(ret);
        operationResult[0] = selectResult;

        return operationResult;
    }

    static OperationResult[] hs20InsertProviderRows() {

        InsertResult insertResult = new InsertResult(Uuid.of(UUID.fromString("33b78c84-3242-4477-831f-185c6532cfda")));
        InsertResult insertResult2 = new InsertResult(Uuid.of(UUID.fromString("ae51393c-f9e5-4021-af73-c5ad4b751f88")));

        OperationResult[] operationResult = new OperationResult[2];
        operationResult[0] = insertResult;
        operationResult[1] = insertResult2;

        return operationResult;
    }

    static OperationResult[] hs20OsuProviders() {
        List<Row> ret = new ArrayList<>();

        Map<String, Value> columns = new HashMap<>();
        columns.put("_uuid", new Atom<>(Uuid.of(UUID.fromString("33b78c84-3242-4477-831f-185c6532cfda"))));
        columns.put("_version", new Atom<>(Uuid.of(UUID.randomUUID())));
        columns.put("method_list", com.vmware.ovsdb.protocol.operation.notation.Set.of(new Atom<>(0), new Atom<>(1)));
        columns.put("osu_friendly_name", com.vmware.ovsdb.protocol.operation.notation.Set
                .of(new Atom<>("eng:Example provider rogers"), new Atom<>("fra:Exemple de fournisseur rogers")));
        columns.put("osu_icons",
                com.vmware.ovsdb.protocol.operation.notation.Set.of(
                        new Atom<>(Uuid.of(UUID.fromString("5f2d0e46-92bd-43a1-aa66-94474deb2212"))),
                        new Atom<>(Uuid.of(UUID.fromString("7cf892c2-3f04-4851-986c-a7b7d8ad1dfa"))),
                        new Atom<>(Uuid.of(UUID.fromString("9449e6cf-de62-4805-855b-3fc9bb5cd3ec")))));
        columns.put("osu_nai", new Atom<>("anonymous@rogers.com"));
        columns.put("server_uri", new Atom<>("https://example.com/osu/rogers"));
        columns.put("service_description", com.vmware.ovsdb.protocol.operation.notation.Set
                .of(new Atom<>("eng:Example provider rogers"), new Atom<>("fra:Exemple de fournisseur rogers")));

        ret.add(new Row(columns));

        columns = new HashMap<>();
        columns.put("_uuid", new Atom<>(Uuid.of(UUID.fromString("ae51393c-f9e5-4021-af73-c5ad4b751f88"))));
        columns.put("_version", new Atom<>(Uuid.of(UUID.randomUUID())));
        columns.put("method_list", com.vmware.ovsdb.protocol.operation.notation.Set.of(new Atom<>(0), new Atom<>(1)));
        columns.put("osu_friendly_name", com.vmware.ovsdb.protocol.operation.notation.Set
                .of(new Atom<>("eng:Example provider telus"), new Atom<>("fra:Exemple de fournisseur telus")));
        columns.put("osu_icons",
                com.vmware.ovsdb.protocol.operation.notation.Set.of(
                        new Atom<>(Uuid.of(UUID.fromString("5f2d0e46-92bd-43a1-aa66-94474deb2212"))),
                        new Atom<>(Uuid.of(UUID.fromString("7cf892c2-3f04-4851-986c-a7b7d8ad1dfa"))),
                        new Atom<>(Uuid.of(UUID.fromString("9449e6cf-de62-4805-855b-3fc9bb5cd3ec")))));
        columns.put("osu_nai", new Atom<>("anonymous@telus.com"));
        columns.put("server_uri", new Atom<>("https://example.com/osu/telus"));
        columns.put("service_description", com.vmware.ovsdb.protocol.operation.notation.Set
                .of(new Atom<>("eng:Example provider telus"), new Atom<>("fra:Exemple de fournisseur telus")));

        OperationResult[] operationResult = new OperationResult[1];
        SelectResult selectResult = new SelectResult(ret);
        operationResult[0] = selectResult;

        return operationResult;
    }

    static OperationResult[] hs20InsertIconRows() {

        UpdateResult insertResult = new UpdateResult(1);
        UpdateResult insertResult2 = new UpdateResult(2);
        UpdateResult insertResult3 = new UpdateResult(3);

        OperationResult[] operationResult = new OperationResult[3];
        operationResult[0] = insertResult;
        operationResult[1] = insertResult2;
        operationResult[2] = insertResult3;

        return operationResult;
    }


    static OperationResult[] hs20IconRows() {
        List<Row> ret = new ArrayList<>();

        Map<String, Value> columns = new HashMap<>();
        columns.put("_uuid", new Atom<>(Uuid.of(UUID.fromString("5f2d0e46-92bd-43a1-aa66-94474deb2212"))));
        columns.put("_version", new Atom<>(Uuid.of(UUID.randomUUID())));
        columns.put("height", new Atom<>(32L));
        columns.put("img_type", new Atom<>("image/png"));
        columns.put("lang_code", new Atom<>("eng"));
        columns.put("name", new Atom<>("icon32usa"));
        columns.put("path", new Atom<String>("/tmp/icon32usa.png"));
        columns.put("url", new Atom<String>("https://localhost:9096/icon32usa.png"));
        columns.put("width", new Atom<>(32L));

        ret.add(new Row(columns));

        columns = new HashMap<>();
        columns.put("_uuid", new Atom<>(Uuid.of(UUID.fromString("7cf892c2-3f04-4851-986c-a7b7d8ad1dfa"))));
        columns.put("_version", new Atom<>(Uuid.of(UUID.randomUUID())));
        columns.put("height", new Atom<>(32L));
        columns.put("img_type", new Atom<>("image/png"));
        columns.put("lang_code", new Atom<>("eng"));
        columns.put("name", new Atom<>("icon32eng"));
        columns.put("path", new Atom<String>("/tmp/icon32eng.png"));
        columns.put("url", new Atom<String>("https://localhost:9096/icon32eng.png"));
        columns.put("width", new Atom<>(32L));

        ret.add(new Row(columns));

        columns = new HashMap<>();
        columns.put("_uuid", new Atom<>(Uuid.of(UUID.fromString("9449e6cf-de62-4805-855b-3fc9bb5cd3ec"))));
        columns.put("_version", new Atom<>(Uuid.of(UUID.randomUUID())));
        columns.put("height", new Atom<>(32L));
        columns.put("img_type", new Atom<>("image/png"));
        columns.put("lang_code", new Atom<>("fra"));
        columns.put("name", new Atom<>("icon32fra"));
        columns.put("path", new Atom<String>("/tmp/icon32fra.png"));
        columns.put("url", new Atom<String>("https://localhost:9096/icon32fra.png"));
        columns.put("width", new Atom<>(32L));

        ret.add(new Row(columns));

        SelectResult selectResult = new SelectResult(ret);

        OperationResult[] operationResult = new OperationResult[1];
        operationResult[0] = selectResult;

        return operationResult;
    }

    static OperationResult[] vifConfigRows() {

        List<Row> ret = new ArrayList<>();
        Row row = new Row(new HashMap<String, Value>());
        row.getColumns().put("_uuid", new Atom<>(Uuid.of(UUID.fromString("66abe4b3-2a26-4769-b684-da70e2392a07"))));
        row.getColumns().put("ssid", new Atom<>("ssid-access"));
        row.getColumns().put("mac", new Atom<>("26:f5:a2:ef:2e:56"));
        row.getColumns().put("if_name", new Atom<>("wlan0"));
        ret.add(row);

        Row row2 = new Row(new HashMap<String, Value>());
        row2.getColumns().put("_uuid", new Atom<>(Uuid.of(UUID.fromString("fad30b59-fada-41fc-ad62-2afca67cf9d5"))));
        row2.getColumns().put("ssid", new Atom<>("ssid-access"));
        row2.getColumns().put("mac", new Atom<>("24:f5:a2:ef:2e:54"));
        row2.getColumns().put("if_name", new Atom<>("wlan2"));
        ret.add(row2);

        Row row3 = new Row(new HashMap<String, Value>());
        row3.getColumns().put("_uuid", new Atom<>(Uuid.of(UUID.randomUUID())));
        row3.getColumns().put("ssid", new Atom<>("ssid-open"));
        row3.getColumns().put("mac", new Atom<>("24:f5:a2:ef:2e:55"));
        row3.getColumns().put("if_name", new Atom<>("wlan1"));
        ret.add(row3);

        SelectResult selectResult = new SelectResult(ret);

        OperationResult[] operationResult = new OperationResult[1];
        operationResult[0] = selectResult;
        return operationResult;
    }

    static OperationResult[] vifStates() {

        List<Row> ret = new ArrayList<>();

        Row row = new Row(new HashMap<String, Value>());
        row.getColumns().put("_uuid", new Atom<>(Uuid.of(UUID.fromString("66abe4b3-2a26-4769-b684-da70e2392a07"))));
        row.getColumns().put("ssid", new Atom<>("ssid-access"));
        row.getColumns().put("if_name", new Atom<>("wlan0"));
        ret.add(row);

        Row row2 = new Row(new HashMap<String, Value>());
        row2.getColumns().put("_uuid", new Atom<>(Uuid.of(UUID.fromString("fad30b59-fada-41fc-ad62-2afca67cf9d5"))));
        row2.getColumns().put("ssid", new Atom<>("ssid-access"));
        row2.getColumns().put("if_name", new Atom<>("wlan2"));
        ret.add(row2);

        Row row3 = new Row(new HashMap<String, Value>());
        row3.getColumns().put("_uuid", new Atom<>(Uuid.of(UUID.randomUUID())));
        row3.getColumns().put("ssid", new Atom<>("ssid-open"));
        row3.getColumns().put("if_name", new Atom<>("wlan1"));
        ret.add(row3);

        SelectResult selectResult = new SelectResult(ret);


        OperationResult[] operationResult = new OperationResult[1];
        operationResult[0].equals(selectResult);
        return operationResult;
    }
    
    
    
}
