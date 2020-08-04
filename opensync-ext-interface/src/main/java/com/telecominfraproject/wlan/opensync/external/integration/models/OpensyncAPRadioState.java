/**
 * 
 */
package com.telecominfraproject.wlan.opensync.external.integration.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;

/**
 * @author mikehansen
 *
 */
public class OpensyncAPRadioState extends BaseJsonModel {

    private static final long serialVersionUID = 5003143778489404219L;

    public int temperatureControl;
    public boolean thermalDowngraded;
    public boolean dfsDemo;
    public String ifName;
    public String mac;
    public int bcnInt;
    public int thermalTxChainmask;
    public Set<Integer> allowedChannels;
    public int thermalShutdown;
    public int channelSync;
    public int hwType;
    public int txChainmask;
    public String radar;
    public String country;
    public Map<String, String> hwConfig;
    public int channel;
    public int txPower;
    public String htMode;
    public int thermalDowngradeTemp;
    public String hwMode;
    public boolean enabled;
    public Map<String, String> channels;
    public int thermalUpgradeTemp;
    public Map<String, String> hwParams;
    public RadioType freqBand;
    public int thermalIntegration;
    public Set<Uuid> vifStates;

    public OpensyncAPRadioState() {
        super();
        allowedChannels = new HashSet<>();
        hwConfig = new HashMap<>();
        channels = new HashMap<>();
        hwParams = new HashMap<>();
        vifStates = new HashSet<>();
    }

    public String channelMode;
    public Uuid _uuid;
    public Uuid version;

    public int getTemperatureControl() {
        return temperatureControl;
    }

    public void setTemperatureControl(int temperatureControl) {
        this.temperatureControl = temperatureControl;
    }

    public boolean isThermalDowngraded() {
        return thermalDowngraded;
    }

    public void setThermalDowngraded(boolean thermalDowngraded) {
        this.thermalDowngraded = thermalDowngraded;
    }

    public boolean isDfsDemo() {
        return dfsDemo;
    }

    public void setDfsDemo(boolean dfsDemo) {
        this.dfsDemo = dfsDemo;
    }

    public String getIfName() {
        return ifName;
    }

    public void setIfName(String ifName) {
        this.ifName = ifName;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public Set<Uuid> getVifStates() {
        return vifStates;
    }

    public void setVifStates(Set<Uuid> vifStates) {
        this.vifStates = vifStates;
    }

    public int getBcnInt() {
        return bcnInt;
    }

    public void setBcnInt(int bcnInt) {
        this.bcnInt = bcnInt;
    }

    public int getThermalTxChainmask() {
        return thermalTxChainmask;
    }

    public void setThermalTxChainmask(int thermalTxChainmask) {
        this.thermalTxChainmask = thermalTxChainmask;
    }

    public Set<Integer> getAllowedChannels() {
        return allowedChannels;
    }

    public void setAllowedChannels(Set<Integer> allowedChannels) {
        this.allowedChannels = allowedChannels;
    }

    public int getThermalShutdown() {
        return thermalShutdown;
    }

    public void setThermalShutdown(int thermalShutdown) {
        this.thermalShutdown = thermalShutdown;
    }

    public int getChannelSync() {
        return channelSync;
    }

    public void setChannelSync(int channelSync) {
        this.channelSync = channelSync;
    }

    public int getHwType() {
        return hwType;
    }

    public void setHwType(int hwType) {
        this.hwType = hwType;
    }

    public int getTxChainmask() {
        return txChainmask;
    }

    public void setTxChainmask(int txChainmask) {
        this.txChainmask = txChainmask;
    }

    public String getRadar() {
        return radar;
    }

    public void setRadar(String radar) {
        this.radar = radar;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Map<String, String> getHwConfig() {
        return hwConfig;
    }

    public void setHwConfig(Map<String, String> hwConfig) {
        this.hwConfig = hwConfig;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getTxPower() {
        return txPower;
    }

    public void setTxPower(int txPower) {
        this.txPower = txPower;
    }

    public String getHtMode() {
        return htMode;
    }

    public void setHtMode(String htMode) {
        this.htMode = htMode;
    }

    public int getThermalDowngradeTemp() {
        return thermalDowngradeTemp;
    }

    public void setThermalDowngradeTemp(int thermalDowngradeTemp) {
        this.thermalDowngradeTemp = thermalDowngradeTemp;
    }

    public String getHwMode() {
        return hwMode;
    }

    public void setHwMode(String hwMode) {
        this.hwMode = hwMode;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, String> getChannels() {
        return channels;
    }

    public void setChannels(Map<String, String> channels) {
        this.channels = channels;
    }

    public int getThermalUpgradeTemp() {
        return thermalUpgradeTemp;
    }

    public void setThermalUpgradeTemp(int thermalUpgradeTemp) {
        this.thermalUpgradeTemp = thermalUpgradeTemp;
    }

    public Map<String, String> getHwParams() {
        return hwParams;
    }

    public void setHwParams(Map<String, String> hwParams) {
        this.hwParams = hwParams;
    }

    public RadioType getFreqBand() {
        return freqBand;
    }

    public void setFreqBand(RadioType freqBand) {
        this.freqBand = freqBand;
    }

    public int getThermalIntegration() {
        return thermalIntegration;
    }

    public void setThermalIntegration(int thermalIntegration) {
        this.thermalIntegration = thermalIntegration;
    }

    public String getChannelMode() {
        return channelMode;
    }

    public void setChannelMode(String channelMode) {
        this.channelMode = channelMode;
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
