package com.telecominfraproject.wlan.opensync.external.integration.models;

import java.util.HashMap;
import java.util.Map;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;

public class OpensyncAPInetState extends BaseJsonModel  {


	private static final long serialVersionUID = 1707053648715030173L;
	
	public String ifName;
	public String dhcpd;
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
	public String getDhcpd() {
		return dhcpd;
	}
	public void setDhcpd(String dhcpd) {
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
	
	

}
