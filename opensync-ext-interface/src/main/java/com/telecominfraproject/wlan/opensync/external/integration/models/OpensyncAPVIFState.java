package com.telecominfraproject.wlan.opensync.external.integration.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;

public class OpensyncAPVIFState extends BaseJsonModel {

    private static final long serialVersionUID = -4916251246542770881L;

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

    public String getIfName() {
        return ifName;
    }

    public OpensyncAPVIFState() {
        super();
        security = new HashMap<>();
        associatedClients = new ArrayList<>();

    }

    public void setIfName(String ifName) {
        this.ifName = ifName;
    }

    public int getVifRadioIdx() {
        return vifRadioIdx;
    }

    public void setVifRadioIdx(int vifRadioIdx) {
        this.vifRadioIdx = vifRadioIdx;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public boolean isApBridge() {
        return apBridge;
    }

    public void setApBridge(boolean apBridge) {
        this.apBridge = apBridge;
    }

    public boolean isUapsdEnable() {
        return uapsdEnable;
    }

    public void setUapsdEnable(boolean uapsdEnable) {
        this.uapsdEnable = uapsdEnable;
    }

    public boolean isWds() {
        return wds;
    }

    public void setWds(boolean wds) {
        this.wds = wds;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public Map<String, String> getSecurity() {
        return security;
    }

    public void setSecurity(Map<String, String> security) {
        this.security = security;
    }

    public String getMacList() {
        return macList;
    }

    public void setMacList(String macList) {
        this.macList = macList;
    }

    public List<Uuid> getAssociatedClients() {
        return associatedClients;
    }

    public void setAssociatedClients(List<Uuid> list) {
        this.associatedClients = list;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getVlanId() {
        return vlanId;
    }

    public void setVlanId(int vlanId) {
        this.vlanId = vlanId;
    }

    public int getBtm() {
        return btm;
    }

    public void setBtm(int btm) {
        this.btm = btm;
    }

    public String getMinHwMode() {
        return minHwMode;
    }

    public void setMinHwMode(String minHwMode) {
        this.minHwMode = minHwMode;
    }

    public String getSsidBroadcast() {
        return ssidBroadcast;
    }

    public void setSsidBroadcast(String ssidBroadcast) {
        this.ssidBroadcast = ssidBroadcast;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getBridge() {
        return bridge;
    }

    public void setBridge(String bridge) {
        this.bridge = bridge;
    }

    public int getGroupRekey() {
        return groupRekey;
    }

    public void setGroupRekey(int groupRekey) {
        this.groupRekey = groupRekey;
    }

    public int getFtMobilityDomain() {
        return ftMobilityDomain;
    }

    public void setFtMobilityDomain(int ftMobilityDomain) {
        this.ftMobilityDomain = ftMobilityDomain;
    }

    public int getFtPsk() {
        return ftPsk;
    }

    public void setFtPsk(int ftPsk) {
        this.ftPsk = ftPsk;
    }

    public int getRrm() {
        return rrm;
    }

    public void setRrm(int rrm) {
        this.rrm = rrm;
    }

    public boolean isDynamicBeacon() {
        return dynamicBeacon;
    }

    public void setDynamicBeacon(boolean dynamicBeacon) {
        this.dynamicBeacon = dynamicBeacon;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
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
