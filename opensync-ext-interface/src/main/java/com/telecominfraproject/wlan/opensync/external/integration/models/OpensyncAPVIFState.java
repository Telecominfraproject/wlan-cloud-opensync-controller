package com.telecominfraproject.wlan.opensync.external.integration.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;
import com.vmware.ovsdb.protocol.operation.notation.Value;

public class OpensyncAPVIFState extends OpensyncAPBase {

    private static final long serialVersionUID = -4916251246542770881L;

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public String ifName;
    public int vifRadioIdx;
    public String parent;
    public String state;
    public String mac;
    public boolean apBridge;
    public boolean uapsdEnable;
    public boolean wds;
    public String ssid;
    public Map<String, String> security;
    public String macList;

    public List<Uuid> associatedClients;
    public boolean enabled;
    public int vlanId;
    public int btm;
    public String minHwMode;
    public String ssidBroadcast;
    public String mode;
    public String bridge;
    public int groupRekey;
    public int ftMobilityDomain;
    public int ftPsk;
    public int rrm;
    public boolean dynamicBeacon;
    public int channel;
    public Uuid _uuid;

    public Uuid version;

    public OpensyncAPVIFState() {
        security = new HashMap<>();
        associatedClients = new ArrayList<>();

    }

    public OpensyncAPVIFState(Row row) {
        Map<String, Value> map = row.getColumns();

        if ((map.get("mac") != null)
                && map.get("mac").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setMac(row.getStringColumn("mac"));
        }
        if ((map.get("bridge") != null)
                && map.get("bridge").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setBridge(row.getStringColumn("bridge"));
        }
        if ((map.get("btm") != null)
                && map.get("btm").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setBtm(row.getIntegerColumn("btm").intValue());
        }

        if ((map.get("channel") != null)
                && map.get("channel").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setChannel(row.getIntegerColumn("channel").intValue());
        }

        if ((map.get("enabled") != null)
                && map.get("enabled").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setEnabled(row.getBooleanColumn("enabled"));
        }

        Long ftPsk = getSingleValueFromSet(row, "ft_psk");
        if (ftPsk != null) {
            this.setFtPsk(ftPsk.intValue());
        }

        Long ftMobilityDomain = getSingleValueFromSet(row, "ft_mobility_domain");
        if (ftMobilityDomain != null) {
            this.setFtMobilityDomain(ftMobilityDomain.intValue());
        }

        if ((map.get("group_rekey") != null)
                && map.get("group_rekey").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setGroupRekey(row.getIntegerColumn("group_rekey").intValue());
        }
        if ((map.get("if_name") != null)
                && map.get("if_name").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setIfName(row.getStringColumn("if_name"));
        }

        if ((map.get("mode") != null)
                && map.get("mode").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setMode(row.getStringColumn("mode"));
        }

        if ((map.get("rrm") != null)
                && map.get("rrm").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setRrm(row.getIntegerColumn("rrm").intValue());
        }
        if ((map.get("ssid") != null)
                && map.get("ssid").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setSsid(row.getStringColumn("ssid"));
        }

        if ((map.get("ssid_broadcast") != null) && map.get("ssid_broadcast").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setSsidBroadcast(row.getStringColumn("ssid_broadcast"));
        }
        if ((map.get("uapsd_enable") != null)
                && map.get("uapsd_enable").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setUapsdEnable(row.getBooleanColumn("uapsd_enable"));
        }
        if ((map.get("vif_radio_idx") != null) && map.get("vif_radio_idx").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setVifRadioIdx(row.getIntegerColumn("vif_radio_idx").intValue());
        }

        List<Uuid> associatedClientsList = new ArrayList<>();

        Set<Uuid> clients = row.getSetColumn("associated_clients");
        associatedClientsList.addAll(clients);

        this.setAssociatedClients(associatedClientsList);

        if (map.get("security") != null) {
            this.setSecurity(row.getMapColumn("security"));
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

    public Uuid get_uuid() {
        return _uuid;
    }

    public List<Uuid> getAssociatedClients() {
        return associatedClients;
    }

    public String getBridge() {
        return bridge;
    }

    public int getBtm() {
        return btm;
    }

    public int getChannel() {
        return channel;
    }

    public int getFtMobilityDomain() {
        return ftMobilityDomain;
    }

    public int getFtPsk() {
        return ftPsk;
    }

    public int getGroupRekey() {
        return groupRekey;
    }

    public String getIfName() {
        return ifName;
    }

    public String getMac() {
        return mac;
    }

    public String getMacList() {
        return macList;
    }

    public String getMinHwMode() {
        return minHwMode;
    }

    public String getMode() {
        return mode;
    }

    public String getParent() {
        return parent;
    }

    public int getRrm() {
        return rrm;
    }

    public Map<String, String> getSecurity() {
        return security;
    }

    public String getSsid() {
        return ssid;
    }

    public String getSsidBroadcast() {
        return ssidBroadcast;
    }

    public String getState() {
        return state;
    }

    public Uuid getVersion() {
        return version;
    }

    public int getVifRadioIdx() {
        return vifRadioIdx;
    }

    public int getVlanId() {
        return vlanId;
    }

    public boolean isApBridge() {
        return apBridge;
    }

    public boolean isDynamicBeacon() {
        return dynamicBeacon;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isUapsdEnable() {
        return uapsdEnable;
    }

    public boolean isWds() {
        return wds;
    }

    public void set_uuid(Uuid _uuid) {
        this._uuid = _uuid;
    }

    public void setApBridge(boolean apBridge) {
        this.apBridge = apBridge;
    }

    public void setAssociatedClients(List<Uuid> list) {
        this.associatedClients = list;
    }

    public void setBridge(String bridge) {
        this.bridge = bridge;
    }

    public void setBtm(int btm) {
        this.btm = btm;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public void setDynamicBeacon(boolean dynamicBeacon) {
        this.dynamicBeacon = dynamicBeacon;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setFtMobilityDomain(int ftMobilityDomain) {
        this.ftMobilityDomain = ftMobilityDomain;
    }

    public void setFtPsk(int ftPsk) {
        this.ftPsk = ftPsk;
    }

    public void setGroupRekey(int groupRekey) {
        this.groupRekey = groupRekey;
    }

    public void setIfName(String ifName) {
        this.ifName = ifName;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public void setMacList(String macList) {
        this.macList = macList;
    }

    public void setMinHwMode(String minHwMode) {
        this.minHwMode = minHwMode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public void setRrm(int rrm) {
        this.rrm = rrm;
    }

    public void setSecurity(Map<String, String> security) {
        this.security = security;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public void setSsidBroadcast(String ssidBroadcast) {
        this.ssidBroadcast = ssidBroadcast;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setUapsdEnable(boolean uapsdEnable) {
        this.uapsdEnable = uapsdEnable;
    }

    public void setVersion(Uuid version) {
        this.version = version;
    }

    public void setVifRadioIdx(int vifRadioIdx) {
        this.vifRadioIdx = vifRadioIdx;
    }

    public void setVlanId(int vlanId) {
        this.vlanId = vlanId;
    }

    public void setWds(boolean wds) {
        this.wds = wds;
    }
}
