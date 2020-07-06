package com.telecominfraproject.wlan.opensync.ovsdb.dao.models;

import java.util.HashMap;
import java.util.Map;
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
    
    @Override
    public WifiVifConfigInfo clone() {
        try {
            WifiVifConfigInfo ret = (WifiVifConfigInfo)super.clone();
            
            if(security!=null) {
                ret.security = new HashMap<>(this.security);
            }
            return ret;
        }catch(CloneNotSupportedException e) {                
            throw new IllegalStateException("Cannot clone ", e);
        }            
    }

    @Override
    public String toString() {
        return String.format(
                "WifiVifConfigInfo [bridge=%s, ap_bridge=%s, btm=%s, enabled=%s, ftPsk=%s, groupRekey=%s, ifName=%s, minHwMode=%s, mode=%s, rrm=%s, ssid=%s, ssidBroadcast=%s, uapsdEnable=%s, vifRadioIdx=%s, security=%s, uuid=%s, vlanId=%s, macList=%s, macListType=%s]",
                bridge, apBridge, btm, enabled, ftPsk, groupRekey, ifName, minHwMode, mode, rrm, ssid, ssidBroadcast, uapsdEnable,
                vifRadioIdx, security, uuid, vlanId, macList, macListType);
    }
    
}