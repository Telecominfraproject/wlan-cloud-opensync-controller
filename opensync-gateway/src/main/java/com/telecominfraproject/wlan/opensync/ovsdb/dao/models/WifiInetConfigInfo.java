package com.telecominfraproject.wlan.opensync.ovsdb.dao.models;

import com.vmware.ovsdb.protocol.operation.notation.Uuid;

public class WifiInetConfigInfo implements Cloneable{
    
    public boolean nat;
    public boolean enabled;
    public String ifName;
    public String ifType;
    public String ipAssignScheme;
    public boolean network;
                    
    public Uuid uuid;
	public int vlanId;
	public String broadcast;
	public String inetAddr;
	public int mtu;
	public String netmask;
    
    @Override
    public WifiInetConfigInfo clone() {
        try {
            WifiInetConfigInfo ret = (WifiInetConfigInfo)super.clone();
            return ret;
        }catch(CloneNotSupportedException e) {                
            throw new IllegalStateException("Cannot clone ", e);
        }            
    }

    @Override
    public String toString() {
        return String.format(
                "WifiInetConfigInfo [nat=%s, broadcast=%s, enabled=%s, ifName=%s, ifType=%s, ipAssignScheme=%s, network=%s, uuid=%s, inetAddr=%s, mtu=%s, netmask=%s, vlanId=%s]",
                nat, broadcast, enabled, ifName, ifType, ipAssignScheme, network, uuid,inetAddr, mtu, netmask, vlanId);
    }
    
}