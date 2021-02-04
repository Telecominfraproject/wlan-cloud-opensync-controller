package com.telecominfraproject.wlan.opensync.external.integration.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;
import com.vmware.ovsdb.protocol.operation.notation.Value;

public class OpensyncAPInetState extends OpensyncAPBase {

    private static final long serialVersionUID = 1707053648715030173L;

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

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
        dns = new HashMap<>();
        dhcpc = new HashMap<>();
    }

    public OpensyncAPInetState(Row row) {
        dns = new HashMap<>();
        dhcpc = new HashMap<>();

        Map<String, Value> map = row.getColumns();

        if ((map.get("NAT") != null)
                && map.get("NAT").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setNat(row.getBooleanColumn("NAT"));
        }
        if ((map.get("enabled") != null)
                && map.get("enabled").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setEnabled(row.getBooleanColumn("enabled"));
        }
        if ((map.get("if_name") != null)
                && map.get("if_name").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setIfName(row.getStringColumn("if_name"));
        }
        if ((map.get("if_type") != null)
                && map.get("if_type").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setIfType(row.getStringColumn("if_type"));
        }

        if (map.containsKey("dhcpc")) {
            this.setDhcpc(row.getMapColumn("dhcpc"));
        }
        if (map.containsKey("dhcpd")) {
            this.setDhcpd(row.getMapColumn("dhcpd"));
        }
        if (map.containsKey("dns")) {
            this.setDns(row.getMapColumn("dns"));
        }
        if (map.get("inet_addr") != null
                && map.get("inet_addr").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setInetAddr(row.getStringColumn("inet_addr"));
        }
        if (map.containsKey("netmask")) {
            this.setNetmask(getSingleValueFromSet(row, "netmask"));
        }
        if (map.get("vlan_id") != null
                && map.get("vlan_id").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setVlanId(row.getIntegerColumn("vlan_id").intValue());
        }
        if (map.get("gre_ifname") != null
                && map.get("gre_ifname").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setGreIfName(row.getStringColumn("gre_ifname"));
        }
        if (map.get("gre_remote_inet_addr") != null && map.get("gre_remote_inet_addr").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setGreRemoteInetAddr(row.getStringColumn("gre_remote_inet_addr"));
        }
        if (map.get("gre_local_inet_addr") != null && map.get("gre_local_inet_addr").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setGreLocalInetAddr(row.getStringColumn("gre_local_inet_addr"));
        }
        if (map.get("gre_remote_mac_addr") != null && map.get("gre_remote_mac_addr").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setGreRemoteMacAddr(row.getStringColumn("gre_remote_mac_addr"));
        }

        if ((map.get("ip_assign_scheme") != null) && map.get("ip_assign_scheme").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setIpAssignScheme(row.getStringColumn("ip_assign_scheme"));
        }
        if ((map.get("network") != null)
                && map.get("network").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setNetwork(row.getBooleanColumn("network"));
        }
        if ((map.get("hwaddr") != null)
                && map.get("hwaddr").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setHwAddr(row.getStringColumn("hwaddr"));
        }
        if ((map.get("_version") != null)
                && map.get("_version").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setVersion(row.getUuidColumn("_version"));
        }
        if ((map.get("_uuid") != null)
                && map.get("_uuid").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setVersion(row.getUuidColumn("_uuid"));
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        OpensyncAPInetState other = (OpensyncAPInetState) obj;
        if (!Objects.equals(_uuid, other._uuid)) {
            return false;
        }
        if (!Objects.equals(broadcast, other.broadcast)) {
            return false;
        }
        if (!Objects.equals(dhcpc, other.dhcpc)) {
            return false;
        }
        if (!Objects.equals(dhcpd, other.dhcpd)) {
            return false;
        }
        if (!Objects.equals(dns, other.dns)) {
            return false;
        }
        if (enabled != other.enabled) {
            return false;
        }
        if (!Objects.equals(gateway, other.gateway)) {
            return false;
        }
        if (!Objects.equals(greIfName, other.greIfName)) {
            return false;
        }
        if (!Objects.equals(greLocalInetAddr, other.greLocalInetAddr)) {
            return false;
        }
        if (!Objects.equals(greRemoteInetAddr, other.greRemoteInetAddr)) {
            return false;
        }
        if (!Objects.equals(hwAddr, other.hwAddr)) {
            return false;
        }
        if (!Objects.equals(ifName, other.ifName)) {
            return false;
        }
        if (!Objects.equals(ifType, other.ifType)) {
            return false;
        }
        if (!Objects.equals(ifUuid, other.ifUuid)) {
            return false;
        }
        if (!Objects.equals(inetAddr, other.inetAddr)) {
            return false;
        }
        if (!Objects.equals(inetConfig, other.inetConfig)) {
            return false;
        }
        if (!Objects.equals(ipAssignScheme, other.ipAssignScheme)) {
            return false;
        }
        if (mtw != other.mtw) {
            return false;
        }
        if (nat != other.nat) {
            return false;
        }
        if (!Objects.equals(netmask, other.netmask)) {
            return false;
        }
        if (network != other.network) {
            return false;
        }
        if (!Objects.equals(parentIfName, other.parentIfName)) {
            return false;
        }
        if (!Objects.equals(greRemoteMacAddr, other.greRemoteMacAddr)) {
            return false;
        }
        if (!Objects.equals(softwdsMacAddr, other.softwdsMacAddr)) {
            return false;
        }
        if (sofwdsWrap != other.sofwdsWrap) {
            return false;
        }
        if (!Objects.equals(unpnpMode, other.unpnpMode)) {
            return false;
        }
        if (!Objects.equals(version, other.version)) {
            return false;
        }
        if (vlanId != other.vlanId) {
            return false;
        }
        return true;
    }

    public Uuid get_uuid() {
        return _uuid;
    }

    public String getBroadcast() {
        return broadcast;
    }

    public Map<String, String> getDhcpc() {
        return dhcpc;
    }

    public Map<String, String> getDhcpd() {
        return dhcpd;
    }

    public Map<String, String> getDns() {
        return dns;
    }

    public String getGateway() {
        return gateway;
    }

    public String getGreIfName() {
        return greIfName;
    }

    public String getGreLocalInetAddr() {
        return greLocalInetAddr;
    }

    public String getGreRemoteInetAddr() {
        return greRemoteInetAddr;
    }

    public String getGreRemoteMacAddr() {
        return this.greRemoteMacAddr;
    }

    public String getHwAddr() {
        return hwAddr;
    }

    public String getIfName() {
        return ifName;
    }

    public String getIfType() {
        return ifType;
    }

    public String getIfUuid() {
        return ifUuid;
    }

    public String getInetAddr() {
        return inetAddr;
    }

    public String getInetConfig() {
        return inetConfig;
    }

    public String getIpAssignScheme() {
        return ipAssignScheme;
    }

    public int getMtw() {
        return mtw;
    }

    public String getNetmask() {
        return netmask;
    }

    public String getParentIfName() {
        return parentIfName;
    }

    public String getSoftwdsMacAddr() {
        return softwdsMacAddr;
    }

    public String getUnpnpMode() {
        return unpnpMode;
    }

    public Uuid getVersion() {
        return version;
    }

    public int getVlanId() {
        return vlanId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_uuid, broadcast, dhcpc, dhcpd, dns, enabled, gateway, greIfName, greLocalInetAddr, greRemoteInetAddr,
                hwAddr, ifName, ifType, ifUuid, inetAddr, inetConfig, ipAssignScheme, mtw, nat, netmask, network,
                parentIfName, greRemoteMacAddr, softwdsMacAddr, sofwdsWrap, unpnpMode, version, vlanId);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isNat() {
        return nat;
    }

    public boolean isNetwork() {
        return network;
    }

    public boolean isSofwdsWrap() {
        return sofwdsWrap;
    }

    public void set_uuid(Uuid _uuid) {
        this._uuid = _uuid;
    }

    public void setBroadcast(String broadcast) {
        this.broadcast = broadcast;
    }

    public void setDhcpc(Map<String, String> dhcpc) {
        this.dhcpc = dhcpc;
    }

    public void setDhcpd(Map<String, String> dhcpd) {
        this.dhcpd = dhcpd;
    }

    public void setDns(Map<String, String> dns) {
        this.dns = dns;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void setGreIfName(String greIfName) {
        this.greIfName = greIfName;
    }

    public void setGreLocalInetAddr(String greLocalInetAddr) {
        this.greLocalInetAddr = greLocalInetAddr;

    }

    public void setGreRemoteInetAddr(String greRemoteInetAddr) {
        this.greRemoteInetAddr = greRemoteInetAddr;
    }

    public void setGreRemoteMacAddr(String greRemoteMacAddr) {
        this.greRemoteMacAddr = greRemoteMacAddr;
    }

    public void setHwAddr(String hwAddr) {
        this.hwAddr = hwAddr;
    }

    public void setIfName(String ifName) {
        this.ifName = ifName;
    }

    public void setIfType(String ifType) {
        this.ifType = ifType;
    }

    public void setIfUuid(String ifUuid) {
        this.ifUuid = ifUuid;
    }

    public void setInetAddr(String inetAddr) {
        this.inetAddr = inetAddr;
    }

    public void setInetConfig(String inetConfig) {
        this.inetConfig = inetConfig;
    }

    public void setIpAssignScheme(String ipAssignScheme) {
        this.ipAssignScheme = ipAssignScheme;
    }

    public void setMtw(int mtw) {
        this.mtw = mtw;
    }

    public void setNat(boolean nat) {
        this.nat = nat;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public void setNetwork(boolean network) {
        this.network = network;
    }

    public void setParentIfName(String parentIfName) {
        this.parentIfName = parentIfName;
    }

    public void setSoftwdsMacAddr(String softwdsMacAddr) {
        this.softwdsMacAddr = softwdsMacAddr;
    }

    public void setSofwdsWrap(boolean sofwdsWrap) {
        this.sofwdsWrap = sofwdsWrap;
    }

    public void setUnpnpMode(String unpnpMode) {
        this.unpnpMode = unpnpMode;
    }

    public void setVersion(Uuid version) {
        this.version = version;
    }

    public void setVlanId(int vlanId) {
        this.vlanId = vlanId;
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
