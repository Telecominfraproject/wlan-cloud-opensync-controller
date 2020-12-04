package com.telecominfraproject.wlan.opensync.ovsdb.dao.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.vmware.ovsdb.protocol.operation.notation.Uuid;

public class WifiVifConfigInfo implements Cloneable {

    // multi_ap
    // {"key":{"enum":["set",["backhaul_bss","backhaul_sta","fronthaul_backhaul_bss","fronthaul_bss","none"]],"type":"string"},"min":0}

    public String bridge;
    public int btm;
    public boolean enabled;
    public int ftPsk;
    public int groupRekey;
    public String ifName;
    public String mode;
    public int rrm;
    public String ssid;
    public String ssidBroadcast;
    public boolean uapsdEnable;
    public int vifRadioIdx;
    public Map<String, String> security;
    public Map<String, String> captivePortal;
    public Set<String> captiveAllowlist;
    public Map<String, String> customOptions;
    public Map<String, String> meshOptions;

    public Uuid uuid;
    public int vlanId;
    public Boolean apBridge;
    public String minHwMode;
    public Set<String> macList;
    public String macListType;
    public int ftMobilityDomain;
    public boolean wpsPbc;
    public boolean wps;
    public boolean wds;
    public String wpsPbcKeyId;
    public boolean mcast2ucast;
    public boolean dynamicBeacon;
    public int vifDbgLvl;
    public Set<Uuid> credentialConfigs;
    public String parent;
    public String multiAp;

    @Override
    public WifiVifConfigInfo clone() {
        try {
            WifiVifConfigInfo ret = (WifiVifConfigInfo) super.clone();

            if (security != null) {
                ret.security = new HashMap<>(this.security);
            }
            if (macList != null) {
                ret.macList = new HashSet<>(this.macList);
            }
            if (captivePortal != null) {
                ret.captivePortal = new HashMap<>(this.captivePortal);
            }
            if (captiveAllowlist != null) {
                ret.captiveAllowlist = new HashSet<>(this.captiveAllowlist);
            }
            if (customOptions != null) {
                ret.customOptions = new HashMap<>(this.customOptions);
            }
            if (meshOptions != null) {
                ret.meshOptions = new HashMap<>(this.meshOptions);
            }
            if (credentialConfigs != null) {
                ret.credentialConfigs = new HashSet<>(this.credentialConfigs);
            }
            return ret;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Cannot clone ", e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(apBridge, bridge, btm, captiveAllowlist, captivePortal, credentialConfigs, customOptions,
                dynamicBeacon, enabled, ftMobilityDomain, ftPsk, groupRekey, ifName, macList, macListType, mcast2ucast,
                meshOptions, minHwMode, mode, multiAp, parent, rrm, security, ssid, ssidBroadcast, uapsdEnable, uuid,
                vifDbgLvl, vifRadioIdx, vlanId, wds, wps, wpsPbc, wpsPbcKeyId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WifiVifConfigInfo other = (WifiVifConfigInfo) obj;
        return Objects.equals(apBridge, other.apBridge) && Objects.equals(bridge, other.bridge) && btm == other.btm
                && Objects.equals(captiveAllowlist, other.captiveAllowlist)
                && Objects.equals(captivePortal, other.captivePortal)
                && Objects.equals(credentialConfigs, other.credentialConfigs)
                && Objects.equals(customOptions, other.customOptions) && dynamicBeacon == other.dynamicBeacon
                && enabled == other.enabled && ftMobilityDomain == other.ftMobilityDomain && ftPsk == other.ftPsk
                && groupRekey == other.groupRekey && Objects.equals(ifName, other.ifName)
                && Objects.equals(macList, other.macList) && Objects.equals(macListType, other.macListType)
                && mcast2ucast == other.mcast2ucast && Objects.equals(meshOptions, other.meshOptions)
                && Objects.equals(minHwMode, other.minHwMode) && Objects.equals(mode, other.mode)
                && Objects.equals(multiAp, other.multiAp) && Objects.equals(parent, other.parent) && rrm == other.rrm
                && Objects.equals(security, other.security) && Objects.equals(ssid, other.ssid)
                && Objects.equals(ssidBroadcast, other.ssidBroadcast) && uapsdEnable == other.uapsdEnable
                && Objects.equals(uuid, other.uuid) && vifDbgLvl == other.vifDbgLvl && vifRadioIdx == other.vifRadioIdx
                && vlanId == other.vlanId && wds == other.wds && wps == other.wps && wpsPbc == other.wpsPbc
                && Objects.equals(wpsPbcKeyId, other.wpsPbcKeyId);
    }

    @Override
    public String toString() {
        return "WifiVifConfigInfo [bridge=" + bridge + ", btm=" + btm + ", enabled=" + enabled + ", ftPsk=" + ftPsk
                + ", groupRekey=" + groupRekey + ", ifName=" + ifName + ", mode=" + mode + ", rrm=" + rrm + ", ssid="
                + ssid + ", ssidBroadcast=" + ssidBroadcast + ", uapsdEnable=" + uapsdEnable + ", vifRadioIdx="
                + vifRadioIdx + ", security=" + security + ", captivePortal=" + captivePortal + ", captiveAllowlist="
                + captiveAllowlist + ", customOptions=" + customOptions + ", meshOptions=" + meshOptions + ", uuid="
                + uuid + ", vlanId=" + vlanId + ", apBridge=" + apBridge + ", minHwMode=" + minHwMode + ", macList="
                + macList + ", macListType=" + macListType + ", ftMobilityDomain=" + ftMobilityDomain + ", wpsPbc="
                + wpsPbc + ", wps=" + wps + ", wds=" + wds + ", wpsPbcKeyId=" + wpsPbcKeyId + ", mcast2ucast="
                + mcast2ucast + ", dynamicBeacon=" + dynamicBeacon + ", vifDbgLvl=" + vifDbgLvl + ", credentialConfigs="
                + credentialConfigs + ", parent=" + parent + ", multiAp=" + multiAp + "]";
    }

}