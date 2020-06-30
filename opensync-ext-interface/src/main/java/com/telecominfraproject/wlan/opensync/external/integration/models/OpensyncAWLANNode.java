/**
 * 
 */
package com.telecominfraproject.wlan.opensync.external.integration.models;

import java.util.HashMap;
import java.util.Map;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;

/**
 * @author mikehansen
 *
 */
public class OpensyncAWLANNode extends BaseJsonModel {

    private static final long serialVersionUID = -6172956297643126710L;

    public Map<Object, Object> mqttSettings;
    public String model;
    public String skuNumber;
    public String id;
    public Map<String, String> versionMatrix;
    public String firmwareVersion;
    public String firmwareUrl;
    public int upgradeDlTimer;
    public String platformVersion;
    public String firmwarePass;
    public int upgradeTimer;
    public int maxBackoff;
    public Map<String, String> ledConfig;
    public String redirectorAddr;
    public Map<String, String> mqttHeaders;
    public String serialNumber;
    public int upgradeStatus;
    public String deviceMode;
    public int minBackoff;
    public Map<String, String> mqttTopics;
    public String revision;
    public String managerAddr;
    public boolean factoryReset;
    public Uuid _uuid;
    public Uuid version;

    public OpensyncAWLANNode() {
        super();
        mqttSettings = new HashMap<>();
        versionMatrix = new HashMap<>();
        ledConfig = new HashMap<>();
        mqttHeaders = new HashMap<>();
        mqttTopics = new HashMap<>();
    }

    public Map<Object, Object> getMqttSettings() {
        return mqttSettings;
    }

    public void setMqttSettings(Map<Object, Object> map) {
        this.mqttSettings = map;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSkuNumber() {
        return skuNumber;
    }

    public void setSkuNumber(String skuNumber) {
        this.skuNumber = skuNumber;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, String> getVersionMatrix() {
        return versionMatrix;
    }

    public void setVersionMatrix(Map<String, String> versionMatrix) {
        this.versionMatrix = versionMatrix;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getFirmwareUrl() {
        return firmwareUrl;
    }

    public void setFirmwareUrl(String firmwareUrl) {
        this.firmwareUrl = firmwareUrl;
    }

    public int getUpgradeDlTimer() {
        return upgradeDlTimer;
    }

    public void setUpgradeDlTimer(int upgradeDlTimer) {
        this.upgradeDlTimer = upgradeDlTimer;
    }

    public String getPlatformVersion() {
        return platformVersion;
    }

    public void setPlatformVersion(String platformVersion) {
        this.platformVersion = platformVersion;
    }

    public String getFirmwarePass() {
        return firmwarePass;
    }

    public void setFirmwarePass(String firmwarePass) {
        this.firmwarePass = firmwarePass;
    }

    public int getUpgradeTimer() {
        return upgradeTimer;
    }

    public void setUpgradeTimer(int upgradeTimer) {
        this.upgradeTimer = upgradeTimer;
    }

    public int getMaxBackoff() {
        return maxBackoff;
    }

    public void setMaxBackoff(int maxBackoff) {
        this.maxBackoff = maxBackoff;
    }

    public Map<String, String> getLedConfig() {
        return ledConfig;
    }

    public void setLedConfig(Map<String, String> ledConfig) {
        this.ledConfig = ledConfig;
    }

    public String getRedirectorAddr() {
        return redirectorAddr;
    }

    public void setRedirectorAddr(String redirectorAddr) {
        this.redirectorAddr = redirectorAddr;
    }

    public Map<String, String> getMqttHeaders() {
        return mqttHeaders;
    }

    public void setMqttHeaders(Map<String, String> mqttHeaders) {
        this.mqttHeaders = mqttHeaders;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public int getUpgradeStatus() {
        return upgradeStatus;
    }

    public void setUpgradeStatus(int upgradeStatus) {
        this.upgradeStatus = upgradeStatus;
    }

    public String getDeviceMode() {
        return deviceMode;
    }

    public void setDeviceMode(String deviceMode) {
        this.deviceMode = deviceMode;
    }

    public int getMinBackoff() {
        return minBackoff;
    }

    public void setMinBackoff(int minBackoff) {
        this.minBackoff = minBackoff;
    }

    public Map<String, String> getMqttTopics() {
        return mqttTopics;
    }

    public void setMqttTopics(Map<String, String> mqttTopics) {
        this.mqttTopics = mqttTopics;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getManagerAddr() {
        return managerAddr;
    }

    public void setManagerAddr(String managerAddr) {
        this.managerAddr = managerAddr;
    }

    public boolean isFactoryReset() {
        return factoryReset;
    }

    public void setFactoryReset(boolean factoryReset) {
        this.factoryReset = factoryReset;
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
