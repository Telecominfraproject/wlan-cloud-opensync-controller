package com.telecominfraproject.wlan.opensync.ovsdb.dao.models;

import java.util.Map;

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
	public String gateway;
	public Map<String,String> dns;
	public Map<String,String> dhcpd;

    
    @Override
    public WifiInetConfigInfo clone() {
        try {
            WifiInetConfigInfo ret = (WifiInetConfigInfo)super.clone();
            if (dns != null) ret.dns = this.dns;
            if (dhcpd != null) ret.dhcpd = this.dhcpd;
            return ret;
        }catch(CloneNotSupportedException e) {                
            throw new IllegalStateException("Cannot clone ", e);
        }            
    }

    
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((broadcast == null) ? 0 : broadcast.hashCode());
		result = prime * result + ((dhcpd == null) ? 0 : dhcpd.hashCode());
		result = prime * result + ((dns == null) ? 0 : dns.hashCode());
		result = prime * result + (enabled ? 1231 : 1237);
		result = prime * result + ((gateway == null) ? 0 : gateway.hashCode());
		result = prime * result + ((ifName == null) ? 0 : ifName.hashCode());
		result = prime * result + ((ifType == null) ? 0 : ifType.hashCode());
		result = prime * result + ((inetAddr == null) ? 0 : inetAddr.hashCode());
		result = prime * result + ((ipAssignScheme == null) ? 0 : ipAssignScheme.hashCode());
		result = prime * result + mtu;
		result = prime * result + (nat ? 1231 : 1237);
		result = prime * result + ((netmask == null) ? 0 : netmask.hashCode());
		result = prime * result + (network ? 1231 : 1237);
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		result = prime * result + vlanId;
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WifiInetConfigInfo other = (WifiInetConfigInfo) obj;
		if (broadcast == null) {
			if (other.broadcast != null)
				return false;
		} else if (!broadcast.equals(other.broadcast))
			return false;
		if (dhcpd == null) {
			if (other.dhcpd != null)
				return false;
		} else if (!dhcpd.equals(other.dhcpd))
			return false;
		if (dns == null) {
			if (other.dns != null)
				return false;
		} else if (!dns.equals(other.dns))
			return false;
		if (enabled != other.enabled)
			return false;
		if (gateway == null) {
			if (other.gateway != null)
				return false;
		} else if (!gateway.equals(other.gateway))
			return false;
		if (ifName == null) {
			if (other.ifName != null)
				return false;
		} else if (!ifName.equals(other.ifName))
			return false;
		if (ifType == null) {
			if (other.ifType != null)
				return false;
		} else if (!ifType.equals(other.ifType))
			return false;
		if (inetAddr == null) {
			if (other.inetAddr != null)
				return false;
		} else if (!inetAddr.equals(other.inetAddr))
			return false;
		if (ipAssignScheme == null) {
			if (other.ipAssignScheme != null)
				return false;
		} else if (!ipAssignScheme.equals(other.ipAssignScheme))
			return false;
		if (mtu != other.mtu)
			return false;
		if (nat != other.nat)
			return false;
		if (netmask == null) {
			if (other.netmask != null)
				return false;
		} else if (!netmask.equals(other.netmask))
			return false;
		if (network != other.network)
			return false;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		if (vlanId != other.vlanId)
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "WifiInetConfigInfo [nat=" + nat + ", enabled=" + enabled + ", ifName=" + ifName + ", ifType=" + ifType
				+ ", ipAssignScheme=" + ipAssignScheme + ", network=" + network + ", uuid=" + uuid + ", vlanId="
				+ vlanId + ", broadcast=" + broadcast + ", inetAddr=" + inetAddr + ", mtu=" + mtu + ", netmask="
				+ netmask + ", gateway=" + gateway + ", dns=" + dns + ", dhcpd=" + dhcpd + "]";
	}
    
}