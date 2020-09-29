package com.telecominfraproject.wlan.opensync.ovsdb.dao.models;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;

public class WifiPasspointConfig implements Cloneable {

    public static String[] ovsdbColumns = {

            "osu_ssid", "network_auth_type", "osu_providers", "hessid", "deauth_request_timeout", "venue_url",
            "ipaddr_type", "venue_group_type", "_uuid", "venue_name", "domain_name", "anqp_domain_id", "osu_icons",
            "nai_realm", "osen", "mcc_mnc", "enable", "vif_config", "roaming_oi", "_version", "connection_capability",
            "operating_class", "operator_icons", "gas_addr3_behavior", "tos", "operator_friendly_name"


    };

    public String osuSsid;
    public String networkAuthType;
    public Set<Uuid> osuProviders;
    public String hessid;
    public int deauthRequestTimeout;
    public Set<String> venueUrl;
    public String ipAddrType;
    public String venueGroupType;
    public Uuid uuid;
    public Set<String> venueName;
    public Set<String> domainName;
    public int anqpDomainId;
    public Set<String> naiRealm;
    public boolean osen;
    public Set<String> mccMnc;
    public boolean enable;
    public Set<Uuid> vifConfig;
    public Set<String> roamingOi;
    public Uuid version;
    public Set<String> connectionCapability;
    public int operatingClass;
    public Set<String> operatorIcons;
    public int gasAddr3Behaviour;
    public String tos;
    public Set<String> operatorFriendlyName;

    @Override
    public WifiPasspointConfig clone() {
        try {
            WifiPasspointConfig ret = (WifiPasspointConfig) super.clone();
            if (osuProviders != null) {
                ret.osuProviders = new HashSet<>(this.osuProviders);
            }
            if (venueUrl != null) {
                ret.venueUrl = new HashSet<>(this.venueUrl);
            }
            if (venueName != null) {
                ret.venueName = new HashSet<>(this.venueName);
            }
            if (domainName != null) {
                ret.domainName = new HashSet<>(this.domainName);
            }
            if (naiRealm != null) {
                ret.naiRealm = new HashSet<>(this.naiRealm);
            }
            if (mccMnc != null) {
                ret.mccMnc = new HashSet<>(this.mccMnc);
            }
            if (roamingOi != null) {
                ret.roamingOi = new HashSet<>(this.roamingOi);
            }
            if (connectionCapability != null) {
                ret.connectionCapability = new HashSet<>(this.connectionCapability);
            }
            if (operatorIcons != null) {
                ret.operatorIcons = new HashSet<>(this.operatorIcons);
            }
            if (operatorFriendlyName != null) {
                ret.operatorFriendlyName = new HashSet<>(this.operatorFriendlyName);
            }
            if (osuProviders != null) {
                ret.osuProviders = new HashSet<>(this.osuProviders);
            }
            if (vifConfig != null) {
                ret.vifConfig = new HashSet<>(this.vifConfig);
            }

            return ret;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Cannot clone ", e);
        }
    }


    public WifiPasspointConfig(Row row) {
        this.osuSsid = row.getStringColumn("osu_ssid");
        if ((row.getColumns().get("network_auth_type") != null) && row.getColumns().get("network_auth_type").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.networkAuthType = row.getStringColumn("network_auth_type");
        }
        this.osuProviders = row.getSetColumn("osu_providers");
        if ((row.getColumns().get("hessid") != null) && row.getColumns().get("hessid").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.hessid = row.getStringColumn("hessid");
        }
        if ((row.getColumns().get("deauth_request_timeout") != null) && row.getColumns().get("deauth_request_timeout")
                .getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.deauthRequestTimeout = row.getIntegerColumn("deauth_request_timeout").intValue();
        }
        this.venueUrl = row.getSetColumn("venue_url");
        if ((row.getColumns().get("ip_addr_type") != null) && row.getColumns().get("ip_addr_type").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.ipAddrType = row.getStringColumn("ip_addr_type");
        }
        if ((row.getColumns().get("venue_group_type") != null) && row.getColumns().get("venue_group_type").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.venueGroupType = row.getStringColumn("venue_group_type");
        }
        this.uuid = row.getUuidColumn("_uuid");
        this.venueName = row.getSetColumn("venue_name");
        this.domainName = row.getSetColumn("domain_name");
        if ((row.getColumns().get("anqp_domain_id") != null) && row.getColumns().get("anqp_domain_id").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.anqpDomainId = row.getIntegerColumn("anqp_domain_id").intValue();
        }
        this.naiRealm = row.getSetColumn("nai_realm");
        if ((row.getColumns().get("osen") != null) && row.getColumns().get("osen").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.osen = row.getBooleanColumn("osen");
        }
        this.mccMnc = row.getSetColumn("mcc_mnc");
        if ((row.getColumns().get("enable") != null) && row.getColumns().get("enable").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.enable = row.getBooleanColumn("enable");
        }
        this.vifConfig = row.getSetColumn("vif_config");
        this.roamingOi = row.getSetColumn("roaming_oi");
        this.version = row.getUuidColumn("_version");
        this.connectionCapability = row.getSetColumn("connection_capability");
        if ((row.getColumns().get("operating_class") != null) && row.getColumns().get("operating_class").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.operatingClass = row.getIntegerColumn("operating_class").intValue();
        }
        this.operatorIcons = row.getSetColumn("operator_icons");
        if ((row.getColumns().get("gas_addr3_behavior") != null) && row.getColumns().get("gas_addr3_behavior")
                .getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.gasAddr3Behaviour = row.getIntegerColumn("gas_addr3_behavior").intValue();
        }
        this.tos = row.getStringColumn("tos");
        this.operatorFriendlyName = row.getSetColumn("operator_friendly_name");
    }

    public WifiPasspointConfig() {
        // TODO Auto-generated constructor stub
    }


    @Override
    public String toString() {
        return String.format(
                "WifiPasspointConfig [osuSsid=%s, networkAuthType=%s, osuProviders=%s, hessid=%s, deauthRequestTimeout=%s, venueUrl=%s, ipAddrType=%s, venueGroupType=%s, uuid=%s, venueName=%s, domainName=%s, anqpDomainId=%s, naiRealm=%s, osen=%s, mccMnc=%s, enable=%s, vifConfig=%s, roamingOi=%s, version=%s, connectionCapability=%s, operatingClass=%s, operatorIcons=%s, gasAddr3Behaviour=%s, tos=%s, operatorFriendlyName=%s]",
                osuSsid, networkAuthType, osuProviders, hessid, deauthRequestTimeout, venueUrl, ipAddrType,
                venueGroupType, uuid, venueName, domainName, anqpDomainId, naiRealm, osen, mccMnc, enable, vifConfig,
                roamingOi, version, connectionCapability, operatingClass, operatorIcons, gasAddr3Behaviour, tos,
                operatorFriendlyName);
    }


    @Override
    public int hashCode() {
        return Objects.hash(anqpDomainId, connectionCapability, deauthRequestTimeout, domainName, enable,
                gasAddr3Behaviour, hessid, ipAddrType, mccMnc, naiRealm, networkAuthType, operatingClass,
                operatorFriendlyName, operatorIcons, osen, osuProviders, osuSsid, roamingOi, tos, uuid, venueGroupType,
                venueName, venueUrl, version, vifConfig);
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WifiPasspointConfig)) {
            return false;
        }
        WifiPasspointConfig other = (WifiPasspointConfig) obj;
        return anqpDomainId == other.anqpDomainId && Objects.equals(connectionCapability, other.connectionCapability)
                && deauthRequestTimeout == other.deauthRequestTimeout && Objects.equals(domainName, other.domainName)
                && enable == other.enable && gasAddr3Behaviour == other.gasAddr3Behaviour
                && Objects.equals(hessid, other.hessid) && Objects.equals(ipAddrType, other.ipAddrType)
                && Objects.equals(mccMnc, other.mccMnc) && Objects.equals(naiRealm, other.naiRealm)
                && Objects.equals(networkAuthType, other.networkAuthType) && operatingClass == other.operatingClass
                && Objects.equals(operatorFriendlyName, other.operatorFriendlyName)
                && Objects.equals(operatorIcons, other.operatorIcons) && osen == other.osen
                && Objects.equals(osuProviders, other.osuProviders) && Objects.equals(osuSsid, other.osuSsid)
                && Objects.equals(roamingOi, other.roamingOi) && Objects.equals(tos, other.tos)
                && Objects.equals(uuid, other.uuid) && Objects.equals(venueGroupType, other.venueGroupType)
                && Objects.equals(venueName, other.venueName) && Objects.equals(venueUrl, other.venueUrl)
                && Objects.equals(version, other.version) && Objects.equals(vifConfig, other.vifConfig);
    }


}