package com.telecominfraproject.wlan.opensync.ovsdb.dao.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.telecominfraproject.wlan.opensync.ovsdb.dao.OvsdbDaoBase;
import com.vmware.ovsdb.protocol.operation.notation.Row;
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

    public WifiVifConfigInfo() {}
    
    public WifiVifConfigInfo(Row row) {
        String bridge = OvsdbDaoBase.getSingleValueFromSet(row, "bridge");
        if (bridge != null) {
            this.bridge = bridge;
        }
        Boolean apBridge = OvsdbDaoBase.getSingleValueFromSet(row, "ap_bridge");
        if (apBridge != null) {
            this.apBridge = apBridge;
        }
        this.uuid = row.getUuidColumn("_uuid");
        Long btm = OvsdbDaoBase.getSingleValueFromSet(row, "btm");
        if (btm != null) {
            this.btm = btm.intValue();
        }
        Boolean enabled = OvsdbDaoBase.getSingleValueFromSet(row, "enabled");
        if (enabled != null) {
            this.enabled = enabled;
        }
        Long ftPsk = OvsdbDaoBase.getSingleValueFromSet(row, "ft_psk");
        if (ftPsk != null) {
            this.ftPsk = ftPsk.intValue();
        }
        Long ftMobilityDomain = OvsdbDaoBase.getSingleValueFromSet(row, "ft_mobility_domain");
        if (ftMobilityDomain != null) {
            this.ftMobilityDomain = ftMobilityDomain.intValue();
        }
        Long groupRekey = OvsdbDaoBase.getSingleValueFromSet(row, "group_rekey");
        if (groupRekey != null) {
            this.groupRekey = groupRekey.intValue();
        }
        String minHwMode = OvsdbDaoBase.getSingleValueFromSet(row, "min_hw_mode");
        if (minHwMode != null) {
            this.bridge = minHwMode;
        }
        this.ifName = row.getStringColumn("if_name");
        String mode = OvsdbDaoBase.getSingleValueFromSet(row, "mode");
        if (mode != null) {
            this.mode = mode;
        }
        Long rrm = OvsdbDaoBase.getSingleValueFromSet(row, "rrm");
        if (rrm != null) {
            this.rrm = rrm.intValue();
        }
        String ssid = OvsdbDaoBase.getSingleValueFromSet(row, "ssid");
        if (ssid != null) {
            this.ssid = ssid;
        }
        String ssidBroadcast = OvsdbDaoBase.getSingleValueFromSet(row, "ssid_broadcast");
        if (ssid != null) {
            this.ssidBroadcast = ssidBroadcast;
        }
        Boolean uapsdEnable = OvsdbDaoBase.getSingleValueFromSet(row, "uapsd_enable");
        if (uapsdEnable != null) {
            this.uapsdEnable = uapsdEnable;
        }
        Long vifRadioIdx = OvsdbDaoBase.getSingleValueFromSet(row, "vif_radio_idx");
        if (vifRadioIdx != null) {
            this.vifRadioIdx = vifRadioIdx.intValue();
        }
        this.security = row.getMapColumn("security");
        Long vlanId = OvsdbDaoBase.getSingleValueFromSet(row, "vlan_id");
        if (vlanId != null) {
            this.vlanId = vlanId.intValue();
        }
        this.macList = row.getSetColumn("mac_list");
        if ((row.getColumns().get("mac_list_type") != null) && row.getColumns().get("mac_list_type").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.macListType = row.getStringColumn("mac_list_type");
        }
        this.customOptions = row.getMapColumn("custom_options");
        this.captiveAllowlist = row.getSetColumn("captive_allowlist");
        this.captivePortal = row.getMapColumn("captive_portal");
        Boolean wpsPbc = OvsdbDaoBase.getSingleValueFromSet(row, "wps_pbc");
        if (wpsPbc != null) {
            this.wpsPbc = wpsPbc;
        } else {
            this.wpsPbc = false;
        }
        Boolean wps = OvsdbDaoBase.getSingleValueFromSet(row, "wps");
        if (wps != null) {
            this.wps = wps;
        } else {
            this.wps = false;
        }
        Boolean wds = OvsdbDaoBase.getSingleValueFromSet(row, "wds");
        if (wds != null) {
            this.wds = wds;
        } else {
            this.wds = false;
        }
        this.wpsPbcKeyId = row.getStringColumn("wps_pbc_key_id");
        Boolean mcast2ucast = OvsdbDaoBase.getSingleValueFromSet(row, "mcast2ucast");
        if (mcast2ucast != null) {
            this.mcast2ucast = mcast2ucast;
        } else {
            this.mcast2ucast = false;
        }
        Boolean dynamicBeacon = OvsdbDaoBase.getSingleValueFromSet(row, "dynamic_beacon");
        if (dynamicBeacon != null) {
            this.dynamicBeacon = dynamicBeacon;
        } else {
            this.dynamicBeacon = false;
        }
        Long vifDbgLvl = OvsdbDaoBase.getSingleValueFromSet(row, "vif_dbg_lvl");
        if (vifDbgLvl != null) {
            this.vifDbgLvl = vifDbgLvl.intValue();
        } else {
            this.vifDbgLvl = 0;
        }
        if (row.getColumns().containsKey("mesh_options")) {
            this.meshOptions = row.getMapColumn("mesh_options");
        }
        this.credentialConfigs = row.getSetColumn("credential_configs");
        String parent = OvsdbDaoBase.getSingleValueFromSet(row, "parent");
        if (parent != null) {
            this.parent = parent;
        }
        String multiAp = OvsdbDaoBase.getSingleValueFromSet(row, "multi_ap");
        if (multiAp != null) {
            this.multiAp = multiAp;
        }
    }
    
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