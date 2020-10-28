package com.telecominfraproject.wlan.opensync.external.integration.models;

import java.util.HashMap;
import java.util.Map;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;

public class OpensyncAPInetState extends BaseJsonModel {

	private static final long serialVersionUID = 1707053648715030173L;

	public String ifName;
	public Map<String, String> dhcpd;
	public String unpnpMode;
	public String ifType;
	public String softwdsMacAddr;
	public boolean enabled;
	public boolean sofwdsWrap;
	public int vlanId;
	public String netmask;
	public boolean nat;
	public String greRemoteInetAddr;
	public String ifUuid;
	public String inetAddr;
	public String hwAddr;
	public int mtw;
	public boolean network;
	public Map<String, String> dns;
	public String parentIfName;
	public String greIfName;
	public String broadcast;
	public Map<String, String> dhcpc;
	public String gateway;
	public String ipAssignScheme;
	public String inetConfig;
	public Uuid _uuid;
	public Uuid version;
	public String greLocalInetAddr;
	public String greRemoteMacAddr;

	public OpensyncAPInetState() {
		super();
		dns = new HashMap<>();
		dhcpc = new HashMap<>();
	}

	public String getIfName() {
		return ifName;
	}

	public void setIfName(String ifName) {
		this.ifName = ifName;
	}

	public Map<String, String> getDhcpd() {
		return dhcpd;
	}

	public void setDhcpd(Map<String, String> dhcpd) {
		this.dhcpd = dhcpd;
	}

	public String getUnpnpMode() {
		return unpnpMode;
	}

	public void setUnpnpMode(String unpnpMode) {
		this.unpnpMode = unpnpMode;
	}

	public String getIfType() {
		return ifType;
	}

	public void setIfType(String ifType) {
		this.ifType = ifType;
	}

	public String getSoftwdsMacAddr() {
		return softwdsMacAddr;
	}

	public void setSoftwdsMacAddr(String softwdsMacAddr) {
		this.softwdsMacAddr = softwdsMacAddr;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isSofwdsWrap() {
		return sofwdsWrap;
	}

	public void setSofwdsWrap(boolean sofwdsWrap) {
		this.sofwdsWrap = sofwdsWrap;
	}

	public int getVlanId() {
		return vlanId;
	}

	public void setVlanId(int vlanId) {
		this.vlanId = vlanId;
	}

	public String getNetmask() {
		return netmask;
	}

	public void setNetmask(String netmask) {
		this.netmask = netmask;
	}

	public boolean isNat() {
		return nat;
	}

	public void setNat(boolean nat) {
		this.nat = nat;
	}

	public String getGreRemoteInetAddr() {
		return greRemoteInetAddr;
	}

	public void setGreRemoteInetAddr(String greRemoteInetAddr) {
		this.greRemoteInetAddr = greRemoteInetAddr;
	}

	public String getIfUuid() {
		return ifUuid;
	}

	public void setIfUuid(String ifUuid) {
		this.ifUuid = ifUuid;
	}

	public String getInetAddr() {
		return inetAddr;
	}

	public void setInetAddr(String inetAddr) {
		this.inetAddr = inetAddr;
	}

	public String getHwAddr() {
		return hwAddr;
	}

	public void setHwAddr(String hwAddr) {
		this.hwAddr = hwAddr;
	}

	public int getMtw() {
		return mtw;
	}

	public void setMtw(int mtw) {
		this.mtw = mtw;
	}

	public boolean isNetwork() {
		return network;
	}

	public void setNetwork(boolean network) {
		this.network = network;
	}

	public Map<String, String> getDns() {
		return dns;
	}

	public void setDns(Map<String, String> dns) {
		this.dns = dns;
	}

	public String getParentIfName() {
		return parentIfName;
	}

	public void setParentIfName(String parentIfName) {
		this.parentIfName = parentIfName;
	}

	public String getGreIfName() {
		return greIfName;
	}

	public void setGreIfName(String greIfName) {
		this.greIfName = greIfName;
	}

	public String getBroadcast() {
		return broadcast;
	}

	public void setBroadcast(String broadcast) {
		this.broadcast = broadcast;
	}

	public Map<String, String> getDhcpc() {
		return dhcpc;
	}

	public void setDhcpc(Map<String, String> dhcpc) {
		this.dhcpc = dhcpc;
	}

	public String getGateway() {
		return gateway;
	}

	public void setGateway(String gateway) {
		this.gateway = gateway;
	}

	public String getIpAssignScheme() {
		return ipAssignScheme;
	}

	public void setIpAssignScheme(String ipAssignScheme) {
		this.ipAssignScheme = ipAssignScheme;
	}

	public String getInetConfig() {
		return inetConfig;
	}

	public void setInetConfig(String inetConfig) {
		this.inetConfig = inetConfig;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public Uuid get_uuid() {
		return _uuid;
	}

	public void set_uuid(Uuid _uuid) {
		this._uuid = _uuid;
	}

	public Uuid getVersion() {
		return version;
	}

	public void setVersion(Uuid version) {
		this.version = version;
	}

	public void setGreLocalInetAddr(String greLocalInetAddr) {
		this.greLocalInetAddr = greLocalInetAddr;

	}

	public String getGreLocalInetAddr() {
		return greLocalInetAddr;
	}

	public void setGreRemoteMacAddr(String greRemoteMacAddr) {
		this.greRemoteMacAddr = greRemoteMacAddr;
	}

	public String getGreRemoteMacAddr(){
		return this.greRemoteMacAddr;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_uuid == null) ? 0 : _uuid.hashCode());
		result = prime * result + ((broadcast == null) ? 0 : broadcast.hashCode());
		result = prime * result + ((dhcpc == null) ? 0 : dhcpc.hashCode());
		result = prime * result + ((dhcpd == null) ? 0 : dhcpd.hashCode());
		result = prime * result + ((dns == null) ? 0 : dns.hashCode());
		result = prime * result + (enabled ? 1231 : 1237);
		result = prime * result + ((gateway == null) ? 0 : gateway.hashCode());
		result = prime * result + ((greIfName == null) ? 0 : greIfName.hashCode());
		result = prime * result + ((greLocalInetAddr == null) ? 0 : greLocalInetAddr.hashCode());
		result = prime * result + ((greRemoteInetAddr == null) ? 0 : greRemoteInetAddr.hashCode());
		result = prime * result + ((hwAddr == null) ? 0 : hwAddr.hashCode());
		result = prime * result + ((ifName == null) ? 0 : ifName.hashCode());
		result = prime * result + ((ifType == null) ? 0 : ifType.hashCode());
		result = prime * result + ((ifUuid == null) ? 0 : ifUuid.hashCode());
		result = prime * result + ((inetAddr == null) ? 0 : inetAddr.hashCode());
		result = prime * result + ((inetConfig == null) ? 0 : inetConfig.hashCode());
		result = prime * result + ((ipAssignScheme == null) ? 0 : ipAssignScheme.hashCode());
		result = prime * result + mtw;
		result = prime * result + (nat ? 1231 : 1237);
		result = prime * result + ((netmask == null) ? 0 : netmask.hashCode());
		result = prime * result + (network ? 1231 : 1237);
		result = prime * result + ((parentIfName == null) ? 0 : parentIfName.hashCode());
		result = prime * result + ((greRemoteMacAddr == null) ? 0 : greRemoteMacAddr.hashCode());
		result = prime * result + ((softwdsMacAddr == null) ? 0 : softwdsMacAddr.hashCode());
		result = prime * result + (sofwdsWrap ? 1231 : 1237);
		result = prime * result + ((unpnpMode == null) ? 0 : unpnpMode.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
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
		OpensyncAPInetState other = (OpensyncAPInetState) obj;
		if (_uuid == null) {
			if (other._uuid != null)
				return false;
		} else if (!_uuid.equals(other._uuid))
			return false;
		if (broadcast == null) {
			if (other.broadcast != null)
				return false;
		} else if (!broadcast.equals(other.broadcast))
			return false;
		if (dhcpc == null) {
			if (other.dhcpc != null)
				return false;
		} else if (!dhcpc.equals(other.dhcpc))
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
		if (greIfName == null) {
			if (other.greIfName != null)
				return false;
		} else if (!greIfName.equals(other.greIfName))
			return false;
		if (greLocalInetAddr == null) {
			if (other.greLocalInetAddr != null)
				return false;
		} else if (!greLocalInetAddr.equals(other.greLocalInetAddr))
			return false;
		if (greRemoteInetAddr == null) {
			if (other.greRemoteInetAddr != null)
				return false;
		} else if (!greRemoteInetAddr.equals(other.greRemoteInetAddr))
			return false;
		if (hwAddr == null) {
			if (other.hwAddr != null)
				return false;
		} else if (!hwAddr.equals(other.hwAddr))
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
		if (ifUuid == null) {
			if (other.ifUuid != null)
				return false;
		} else if (!ifUuid.equals(other.ifUuid))
			return false;
		if (inetAddr == null) {
			if (other.inetAddr != null)
				return false;
		} else if (!inetAddr.equals(other.inetAddr))
			return false;
		if (inetConfig == null) {
			if (other.inetConfig != null)
				return false;
		} else if (!inetConfig.equals(other.inetConfig))
			return false;
		if (ipAssignScheme == null) {
			if (other.ipAssignScheme != null)
				return false;
		} else if (!ipAssignScheme.equals(other.ipAssignScheme))
			return false;
		if (mtw != other.mtw)
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
		if (parentIfName == null) {
			if (other.parentIfName != null)
				return false;
		} else if (!parentIfName.equals(other.parentIfName))
			return false;
		if (greRemoteMacAddr == null) {
			if (other.greRemoteMacAddr != null)
				return false;
		} else if (!greRemoteMacAddr.equals(other.greRemoteMacAddr))
			return false;
		if (softwdsMacAddr == null) {
			if (other.softwdsMacAddr != null)
				return false;
		} else if (!softwdsMacAddr.equals(other.softwdsMacAddr))
			return false;
		if (sofwdsWrap != other.sofwdsWrap)
			return false;
		if (unpnpMode == null) {
			if (other.unpnpMode != null)
				return false;
		} else if (!unpnpMode.equals(other.unpnpMode))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		if (vlanId != other.vlanId)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "OpensyncAPInetState [ifName=" + ifName + ", dhcpd=" + dhcpd + ", unpnpMode=" + unpnpMode + ", ifType="
				+ ifType + ", softwdsMacAddr=" + softwdsMacAddr + ", enabled=" + enabled + ", sofwdsWrap=" + sofwdsWrap
				+ ", vlanId=" + vlanId + ", netmask=" + netmask + ", nat=" + nat + ", greRemoteInetAddr="
				+ greRemoteInetAddr + ", ifUuid=" + ifUuid + ", inetAddr=" + inetAddr + ", hwAddr=" + hwAddr + ", mtw="
				+ mtw + ", network=" + network + ", dns=" + dns + ", parentIfName=" + parentIfName + ", greIfName="
				+ greIfName + ", broadcast=" + broadcast + ", dhcpc=" + dhcpc + ", gateway=" + gateway
				+ ", ipAssignScheme=" + ipAssignScheme + ", inetConfig=" + inetConfig + ", _uuid=" + _uuid
				+ ", version=" + version + ", greLocalInetAddr=" + greLocalInetAddr + ", greRemoteMacAddr="
				+ greRemoteMacAddr + "]";
	}

}
