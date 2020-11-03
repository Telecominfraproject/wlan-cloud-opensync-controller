package com.telecominfraproject.wlan.opensync.ovsdb.dao.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.vmware.ovsdb.protocol.operation.notation.Uuid;

public class WifiVifConfigInfo implements Cloneable{
    
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
    public Map<String,String> security;
            
    public Uuid uuid;
    public int vlanId;
	public Boolean apBridge;
	public String minHwMode;
	public Set<String> macList;
	public String macListType;
    public int ftMobilityDomain;
    
    @Override
    public WifiVifConfigInfo clone() {
        try {
            WifiVifConfigInfo ret = (WifiVifConfigInfo)super.clone();
            
            if(security!=null) {
                ret.security = new HashMap<>(this.security);
            }
            if (macList!=null) {
                ret.macList = new HashSet<>(this.macList);
            }
            return ret;
        }catch(CloneNotSupportedException e) {                
            throw new IllegalStateException("Cannot clone ", e);
        }            
    }

    @Override
    public int hashCode() {
        return Objects.hash(apBridge, bridge, btm, enabled, ftMobilityDomain, ftPsk, groupRekey, ifName, macList,
                macListType, minHwMode, mode, rrm, security, ssid, ssidBroadcast, uapsdEnable, uuid, vifRadioIdx,
                vlanId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WifiVifConfigInfo)) {
            return false;
        }
        WifiVifConfigInfo other = (WifiVifConfigInfo) obj;
        return Objects.equals(apBridge, other.apBridge) && Objects.equals(bridge, other.bridge) && btm == other.btm
                && enabled == other.enabled && ftMobilityDomain == other.ftMobilityDomain && ftPsk == other.ftPsk
                && groupRekey == other.groupRekey && Objects.equals(ifName, other.ifName)
                && Objects.equals(macList, other.macList) && Objects.equals(macListType, other.macListType)
                && Objects.equals(minHwMode, other.minHwMode) && Objects.equals(mode, other.mode) && rrm == other.rrm
                && Objects.equals(security, other.security) && Objects.equals(ssid, other.ssid)
                && Objects.equals(ssidBroadcast, other.ssidBroadcast) && uapsdEnable == other.uapsdEnable
                && Objects.equals(uuid, other.uuid) && vifRadioIdx == other.vifRadioIdx && vlanId == other.vlanId;
    }

    @Override
    public String toString() {
        return String.format(
                "WifiVifConfigInfo [bridge=%s, ap_bridge=%s, btm=%s, enabled=%s, ftPsk=%s, ftMobilityDomain=%s, groupRekey=%s, ifName=%s, minHwMode=%s, mode=%s, rrm=%s, ssid=%s, ssidBroadcast=%s, uapsdEnable=%s, vifRadioIdx=%s, security=%s, uuid=%s, vlanId=%s, macList=%s, macListType=%s]",
                bridge, apBridge, btm, enabled, ftPsk, ftMobilityDomain, groupRekey, ifName, minHwMode, mode, rrm, ssid, ssidBroadcast, uapsdEnable,
                vifRadioIdx, security, uuid, vlanId, macList, macListType);
    }
    
}