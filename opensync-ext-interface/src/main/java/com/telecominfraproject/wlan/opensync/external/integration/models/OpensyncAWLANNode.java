/**
 *
 */

package com.telecominfraproject.wlan.opensync.external.integration.models;

import java.util.HashMap;
import java.util.Map;

import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;
import com.vmware.ovsdb.protocol.operation.notation.Value;

/**
 * @author mikehansen
 *
 */
public class OpensyncAWLANNode extends OpensyncAPBase {

    private static final long serialVersionUID = -6172956297643126710L;

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

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
    public String referenceDesign;
    public Map<String, String> qrCode;
    public String modelDescription;
    public String manufacturerUrl;
    public String manufacturerName;
    public String manufacturerDate;
    public String certificationRegion;

    public Uuid _uuid;

    public Uuid version;

    public OpensyncAWLANNode() {
        mqttSettings = new HashMap<>();
        versionMatrix = new HashMap<>();
        ledConfig = new HashMap<>();
        mqttHeaders = new HashMap<>();
        mqttTopics = new HashMap<>();
    }

    public OpensyncAWLANNode(Row row) {
        this();
        Map<String, Value> map = row.getColumns();
        if (map.get("mqtt_settings") != null) {
            this.setMqttSettings(row.getMapColumn("mqtt_settings"));
        }
        if (map.get("mqtt_headers") != null) {
            this.setMqttHeaders(row.getMapColumn("mqtt_headers"));
        }
        if (map.get("mqtt_topics") != null) {
            this.setMqttHeaders(row.getMapColumn("mqtt_topics"));
        }
        if ((map.get("model") != null) && map.get("model").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setModel(row.getStringColumn("model"));
        }
        if ((map.get("sku_number") != null) && map.get("sku_number").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setSkuNumber(row.getStringColumn("sku_number"));
        }
        if (map.get("id") != null) {
            this.setId(getSingleValueFromSet(row, "id"));
        }
        if (map.get("reference_design") != null) {
            this.setReferenceDesign(getSingleValueFromSet(row, "reference_design"));
        }
        if (map.get("qr_code") != null) {
            this.setQrCode(row.getMapColumn("qr_code"));
        }
        if ((map.get("model_description") != null)) {
            this.setModelDescription(getSingleValueFromSet(row, "model_description"));
        }
        if ((map.get("manufacturer_url") != null)) {
            this.setManufacturerUrl(getSingleValueFromSet(row, "manufacturer_url"));
        }
        if ((map.get("manufacturer_name") != null)) {
            this.setManufacturerName(getSingleValueFromSet(row, "manufacturer_name"));
        }
        if ((map.get("manufacturer_date") != null)) {
            this.setManufacturerDate(getSingleValueFromSet(row, "manufacturer_date"));
        }
        if (map.get("certification_region") != null) {
            this.setCertificationRegion(getSingleValueFromSet(row, "certification_region"));
        }
        if (map.get("version_matrix") != null) {
            this.setVersionMatrix(row.getMapColumn("version_matrix"));
        }
        if ((map.get("firmware_version") != null) && map.get("firmware_version").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setFirmwareVersion(row.getStringColumn("firmware_version"));
        }
        if ((map.get("firmware_url") != null) && map.get("firmware_url").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setFirmwareUrl(row.getStringColumn("firmware_url"));
        }
        if ((map.get("_uuid") != null) && map.get("_uuid").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setVersion(row.getUuidColumn("_uuid"));
        }
        if ((map.get("upgrade_dl_timer") != null) && map.get("upgrade_dl_timer").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setUpgradeDlTimer(row.getIntegerColumn("upgrade_dl_timer").intValue());
        }
        if ((map.get("platform_version") != null) && map.get("platform_version").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setPlatformVersion(row.getStringColumn("platform_version"));
        }
        if ((map.get("firmware_pass") != null) && map.get("firmware_pass").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setFirmwarePass(row.getStringColumn("firmware_pass"));
        }
        if ((map.get("upgrade_timer") != null) && map.get("upgrade_timer").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setUpgradeTimer(row.getIntegerColumn("upgrade_timer").intValue());
        }
        if ((map.get("max_backoff") != null) && map.get("max_backoff").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setMaxBackoff(row.getIntegerColumn("max_backoff").intValue());
        }
        if (map.get("led_config") != null) {
            this.setLedConfig(row.getMapColumn("led_config"));
        }
        if ((map.get("redirector_addr") != null) && map.get("redirector_addr").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setRedirectorAddr(row.getStringColumn("redirector_addr"));
        }
        if ((map.get("serial_number") != null) && map.get("serial_number").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setSerialNumber(row.getStringColumn("serial_number"));
        }
        if ((map.get("_version") != null) && map.get("_version").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setVersion(row.getUuidColumn("_version"));
        }
        this.setUpgradeStatus(row.getIntegerColumn("upgrade_status").intValue());
        if ((map.get("device_mode") != null) && map.get("device_mode").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setDeviceMode(row.getStringColumn("device_mode"));
        }
        if ((map.get("min_backoff") != null) && map.get("min_backoff").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setMinBackoff(row.getIntegerColumn("min_backoff").intValue());
        }
        if ((map.get("revision") != null) && map.get("revision").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setRevision(row.getStringColumn("revision"));
        }
        if ((map.get("manager_addr") != null) && map.get("manager_addr").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setManagerAddr(row.getStringColumn("manager_addr"));
        }
        if ((map.get("factory_reset") != null) && map.get("factory_reset").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setFactoryReset(row.getBooleanColumn("factory_reset"));
        }
    }

    public Uuid get_uuid() {
        return _uuid;
    }

    public String getDeviceMode() {
        return deviceMode;
    }

    public String getFirmwarePass() {
        return firmwarePass;
    }

    public String getFirmwareUrl() {
        return firmwareUrl;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public String getId() {
        return id;
    }

    public Map<String, String> getLedConfig() {
        return ledConfig;
    }

    public String getManagerAddr() {
        return managerAddr;
    }

    public int getMaxBackoff() {
        return maxBackoff;
    }

    public int getMinBackoff() {
        return minBackoff;
    }

    public String getModel() {
        return model;
    }

    public Map<String, String> getMqttHeaders() {
        return mqttHeaders;
    }

    public Map<Object, Object> getMqttSettings() {
        return mqttSettings;
    }

    public Map<String, String> getMqttTopics() {
        return mqttTopics;
    }

    public String getPlatformVersion() {
        return platformVersion;
    }

    public String getRedirectorAddr() {
        return redirectorAddr;
    }

    public String getRevision() {
        return revision;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getSkuNumber() {
        return skuNumber;
    }

    public int getUpgradeDlTimer() {
        return upgradeDlTimer;
    }

    public int getUpgradeStatus() {
        return upgradeStatus;
    }

    public int getUpgradeTimer() {
        return upgradeTimer;
    }

    public Uuid getVersion() {
        return version;
    }

    public Map<String, String> getVersionMatrix() {
        return versionMatrix;
    }

    public boolean isFactoryReset() {
        return factoryReset;
    }

    public String getReferenceDesign() {
        return referenceDesign;
    }

    public void setReferenceDesign(String referenceDesign) {
        this.referenceDesign = referenceDesign;
    }

    public Map<String, String> getQrCode() {
        return qrCode;
    }

    public void setQrCode(Map<String, String> qrCode) {
        this.qrCode = qrCode;
    }

    public String getModelDescription() {
        return modelDescription;
    }

    public void setModelDescription(String modelDescription) {
        this.modelDescription = modelDescription;
    }

    public String getManufacturerUrl() {
        return manufacturerUrl;
    }

    public void setManufacturerUrl(String manufacturerUrl) {
        this.manufacturerUrl = manufacturerUrl;
    }

    public String getManufacturerName() {
        return manufacturerName;
    }

    public void setManufacturerName(String manufacturerName) {
        this.manufacturerName = manufacturerName;
    }

    public String getManufacturerDate() {
        return manufacturerDate;
    }

    public void setManufacturerDate(String manufacturerDate) {
        this.manufacturerDate = manufacturerDate;
    }

    public String getCertificationRegion() {
        return certificationRegion;
    }

    public void setCertificationRegion(String certificationRegion) {
        this.certificationRegion = certificationRegion;
    }

    public void set_uuid(Uuid _uuid) {
        this._uuid = _uuid;
    }

    public void setDeviceMode(String deviceMode) {
        this.deviceMode = deviceMode;
    }

    public void setFactoryReset(boolean factoryReset) {
        this.factoryReset = factoryReset;
    }

    public void setFirmwarePass(String firmwarePass) {
        this.firmwarePass = firmwarePass;
    }

    public void setFirmwareUrl(String firmwareUrl) {
        this.firmwareUrl = firmwareUrl;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLedConfig(Map<String, String> ledConfig) {
        this.ledConfig = ledConfig;
    }

    public void setManagerAddr(String managerAddr) {
        this.managerAddr = managerAddr;
    }

    public void setMaxBackoff(int maxBackoff) {
        this.maxBackoff = maxBackoff;
    }

    public void setMinBackoff(int minBackoff) {
        this.minBackoff = minBackoff;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setMqttHeaders(Map<String, String> mqttHeaders) {
        this.mqttHeaders = mqttHeaders;
    }

    public void setMqttSettings(Map<Object, Object> map) {
        this.mqttSettings = map;
    }

    public void setMqttTopics(Map<String, String> mqttTopics) {
        this.mqttTopics = mqttTopics;
    }

    public void setPlatformVersion(String platformVersion) {
        this.platformVersion = platformVersion;
    }

    public void setRedirectorAddr(String redirectorAddr) {
        this.redirectorAddr = redirectorAddr;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public void setSkuNumber(String skuNumber) {
        this.skuNumber = skuNumber;
    }

    public void setUpgradeDlTimer(int upgradeDlTimer) {
        this.upgradeDlTimer = upgradeDlTimer;
    }

    public void setUpgradeStatus(int upgradeStatus) {
        this.upgradeStatus = upgradeStatus;
    }

    public void setUpgradeTimer(int upgradeTimer) {
        this.upgradeTimer = upgradeTimer;
    }

    public void setVersion(Uuid version) {
        this.version = version;
    }

    public void setVersionMatrix(Map<String, String> versionMatrix) {
        this.versionMatrix = versionMatrix;
    }

}
