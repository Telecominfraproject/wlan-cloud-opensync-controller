package com.telecominfraproject.wlan.opensync.ovsdb.dao.models;

import java.util.HashMap;
import java.util.Map;

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
                "WifiVifConfigInfo [bridge=%s, btm=%s, enabled=%s, ftPsk=%s, groupRekey=%s, ifName=%s, mode=%s, rrm=%s, ssid=%s, ssidBroadcast=%s, uapsdEnable=%s, vifRadioIdx=%s, security=%s, uuid=%s]",
                bridge, btm, enabled, ftPsk, groupRekey, ifName, mode, rrm, ssid, ssidBroadcast, uapsdEnable,
                vifRadioIdx, security, uuid);
    }
    
}