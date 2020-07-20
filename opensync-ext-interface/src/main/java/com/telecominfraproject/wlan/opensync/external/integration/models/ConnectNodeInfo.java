package com.telecominfraproject.wlan.opensync.external.integration.models;

import java.util.HashMap;
import java.util.Map;

public class ConnectNodeInfo implements Cloneable {
    public Map<String, String> mqttSettings = new HashMap<>();
    public Map<String, String> versionMatrix = new HashMap<>();
    public Map<String, String> wifiRadioStates = new HashMap<>();
    public String redirectorAddr;
    public String managerAddr;
    public String skuNumber;
    public String serialNumber;
    public String macAddress;
    public String ipV4Address;
    public String platformVersion;
    public String firmwareVersion;
    public String revision;
    public String model;
    public String ifName;
    public String ifType;
    public String country;
    public String lanIpV4Address;
    public String lanIfName;
    public String lanIfType;
    public String lanMacAddress;

    @Override
    public ConnectNodeInfo clone() {
        try {
            ConnectNodeInfo ret = (ConnectNodeInfo) super.clone();
            if (this.mqttSettings != null) {
                ret.mqttSettings = new HashMap<>(this.mqttSettings);
            }
            if (this.wifiRadioStates != null) {
                ret.wifiRadioStates = new HashMap<>(this.wifiRadioStates);
            }
            if (this.versionMatrix != null) {
                ret.versionMatrix = new HashMap<>(this.versionMatrix);
            }
            return ret;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Cannot clone ", e);
        }
    }

    @Override
    public String toString() {
        return String.format(
                "ConnectNodeInfo [mqttSettings=%s, redirectorAddr=%s, managerAddr=%s, skuNumber=%s, serialNumber=%s, "
                        + "macAddress=%s, ipV4Address=%s, platformVersion=%s, firmwareVersion=%s, revision=%s, model=%s, ifName=%s, lanIpV4Address=%s, lanIfName=%s, lanIfType=%s, lanMacAddress=%s, ifType=%s, wifiRadioStates=%s, versionMatrix=%s]",
                mqttSettings, redirectorAddr, managerAddr, skuNumber, serialNumber, macAddress, ipV4Address,
                platformVersion, firmwareVersion, revision, model, ifName, lanIpV4Address, lanIfName, lanIfType,
                lanMacAddress, ifType, wifiRadioStates, versionMatrix);
    }

}
