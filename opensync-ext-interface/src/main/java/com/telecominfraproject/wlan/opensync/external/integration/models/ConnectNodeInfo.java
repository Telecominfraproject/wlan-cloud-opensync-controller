
package com.telecominfraproject.wlan.opensync.external.integration.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
    public String referenceDesign;
    public Map<String, String> qrCode;
    public String modelDescription;
    public String manufacturerUrl;
    public String manufacturerName;
    public String manufacturerDate;
    public String certificationRegion;

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
            if (this.qrCode != null) {
                ret.qrCode = new HashMap<>(this.qrCode);
            }
            return ret;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Cannot clone ", e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(certificationRegion, country, firmwareVersion, ifName, ifType, ipV4Address, lanIfName, lanIfType, lanIpV4Address, lanMacAddress,
                macAddress, managerAddr, manufacturerDate, manufacturerName, manufacturerUrl, model, modelDescription, mqttSettings, platformVersion, qrCode,
                redirectorAddr, referenceDesign, revision, serialNumber, skuNumber, versionMatrix, wifiRadioStates);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConnectNodeInfo other = (ConnectNodeInfo) obj;
        return Objects.equals(certificationRegion, other.certificationRegion) && Objects.equals(country, other.country)
                && Objects.equals(firmwareVersion, other.firmwareVersion) && Objects.equals(ifName, other.ifName) && Objects.equals(ifType, other.ifType)
                && Objects.equals(ipV4Address, other.ipV4Address) && Objects.equals(lanIfName, other.lanIfName) && Objects.equals(lanIfType, other.lanIfType)
                && Objects.equals(lanIpV4Address, other.lanIpV4Address) && Objects.equals(lanMacAddress, other.lanMacAddress)
                && Objects.equals(macAddress, other.macAddress) && Objects.equals(managerAddr, other.managerAddr)
                && Objects.equals(manufacturerDate, other.manufacturerDate) && Objects.equals(manufacturerName, other.manufacturerName)
                && Objects.equals(manufacturerUrl, other.manufacturerUrl) && Objects.equals(model, other.model)
                && Objects.equals(modelDescription, other.modelDescription) && Objects.equals(mqttSettings, other.mqttSettings)
                && Objects.equals(platformVersion, other.platformVersion) && Objects.equals(qrCode, other.qrCode)
                && Objects.equals(redirectorAddr, other.redirectorAddr) && Objects.equals(referenceDesign, other.referenceDesign)
                && Objects.equals(revision, other.revision) && Objects.equals(serialNumber, other.serialNumber) && Objects.equals(skuNumber, other.skuNumber)
                && Objects.equals(versionMatrix, other.versionMatrix) && Objects.equals(wifiRadioStates, other.wifiRadioStates);
    }

    @Override
    public String toString() {
        return "ConnectNodeInfo [mqttSettings=" + mqttSettings + ", versionMatrix=" + versionMatrix + ", wifiRadioStates=" + wifiRadioStates
                + ", redirectorAddr=" + redirectorAddr + ", managerAddr=" + managerAddr + ", skuNumber=" + skuNumber + ", serialNumber=" + serialNumber
                + ", macAddress=" + macAddress + ", ipV4Address=" + ipV4Address + ", platformVersion=" + platformVersion + ", firmwareVersion="
                + firmwareVersion + ", revision=" + revision + ", model=" + model + ", ifName=" + ifName + ", ifType=" + ifType + ", country=" + country
                + ", lanIpV4Address=" + lanIpV4Address + ", lanIfName=" + lanIfName + ", lanIfType=" + lanIfType + ", lanMacAddress=" + lanMacAddress
                + ", referenceDesign=" + referenceDesign + ", qrCode=" + qrCode + ", modelDescription=" + modelDescription + ", manufacturerUrl="
                + manufacturerUrl + ", manufacturerName=" + manufacturerName + ", manufacturerDate=" + manufacturerDate + ", certificationRegion="
                + certificationRegion + "]";
    }

}
