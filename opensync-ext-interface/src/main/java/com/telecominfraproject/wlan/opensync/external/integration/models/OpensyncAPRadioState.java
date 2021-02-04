/**
 *
 */
package com.telecominfraproject.wlan.opensync.external.integration.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;
import com.vmware.ovsdb.protocol.operation.notation.Value;

/**
 * @author mikehansen
 *
 */
public class OpensyncAPRadioState extends OpensyncAPBase {

    private static final long serialVersionUID = 5003143778489404219L;

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

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

    public String channelMode;

    public Uuid _uuid;
    public Uuid version;

    public OpensyncAPRadioState() {
        allowedChannels = new HashSet<>();
        hwConfig = new HashMap<>();
        channels = new HashMap<>();
        hwParams = new HashMap<>();
        vifStates = new HashSet<>();
    }

    public OpensyncAPRadioState(Row row) {
        this();

        Map<String, Value> map = row.getColumns();

        if ((map.get("mac") != null)
                && map.get("mac").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setMac(row.getStringColumn("mac"));
        }
        if ((map.get("channel") != null)
                && map.get("channel").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setChannel(row.getIntegerColumn("channel").intValue());
        }
        if ((map.get("freq_band") != null)
                && map.get("freq_band").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            String frequencyBand = row.getStringColumn("freq_band");
            switch (frequencyBand) {
            case "2.4G":
                this.setFreqBand(RadioType.is2dot4GHz);
                break;
            case "5G":
                this.setFreqBand(RadioType.is5GHz);
                break;
            case "5GL":
                this.setFreqBand(RadioType.is5GHzL);
                break;
            case "5GU":
                this.setFreqBand(RadioType.is5GHzU);
                break;
            default:
                this.setFreqBand(RadioType.UNSUPPORTED);
            }
        }
        if ((map.get("if_name") != null)
                && map.get("if_name").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setIfName(row.getStringColumn("if_name"));
        }
        if ((map.get("channel_mode") != null)
                && map.get("channel_mode").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setChannelMode(row.getStringColumn("channel_mode"));
        }
        if ((map.get("country") != null)
                && map.get("country").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setCountry(row.getStringColumn("country").toUpperCase());
        }
        if ((map.get("enabled") != null)
                && map.get("enabled").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setEnabled(row.getBooleanColumn("enabled"));
        }
        if ((map.get("ht_mode") != null)
                && map.get("ht_mode").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setHtMode(row.getStringColumn("ht_mode"));
        }
        if ((map.get("tx_power") != null)
                && map.get("tx_power").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setTxPower(row.getIntegerColumn("tx_power").intValue());
        }
        if ((map.get("hw_config") != null)
                && map.get("hw_config").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Map.class)) {
            this.setHwConfig(row.getMapColumn("hw_config"));
        }
        if ((map.get("_version") != null)
                && map.get("_version").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setVersion(row.getUuidColumn("_version"));
        }
        if ((map.get("_uuid") != null)
                && map.get("_uuid").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setVersion(row.getUuidColumn("_uuid"));
        }
        if (map.get("allowed_channels") != null) {

            Set<Long> allowedChannels = getSet(row, "allowed_channels");

            Set<Integer> allowed = new HashSet<>();
            for (Long channel : allowedChannels) {
                allowed.add(channel.intValue());
            }
            this.setAllowedChannels(allowed);
        }
        if (map.get("channels") != null) {

            Map<String, String> channels = row.getMapColumn("channels");
            this.setChannels(channels);
        }

        Set<Uuid> vifStates = row.getSetColumn("vif_states");
        this.setVifStates(vifStates);

    }

    public Uuid get_uuid() {
        return _uuid;
    }

    public Set<Integer> getAllowedChannels() {
        return allowedChannels;
    }

    public int getBcnInt() {
        return bcnInt;
    }

    public int getChannel() {
        return channel;
    }

    public String getChannelMode() {
        return channelMode;
    }

    public Map<String, String> getChannels() {
        return channels;
    }

    public int getChannelSync() {
        return channelSync;
    }

    public String getCountry() {
        return country;
    }

    public RadioType getFreqBand() {
        return freqBand;
    }

    public String getHtMode() {
        return htMode;
    }

    public Map<String, String> getHwConfig() {
        return hwConfig;
    }

    public String getHwMode() {
        return hwMode;
    }

    public Map<String, String> getHwParams() {
        return hwParams;
    }

    public int getHwType() {
        return hwType;
    }

    public String getIfName() {
        return ifName;
    }

    public String getMac() {
        return mac;
    }

    public String getRadar() {
        return radar;
    }

    public int getTemperatureControl() {
        return temperatureControl;
    }

    public int getThermalDowngradeTemp() {
        return thermalDowngradeTemp;
    }

    public int getThermalIntegration() {
        return thermalIntegration;
    }

    public int getThermalShutdown() {
        return thermalShutdown;
    }

    public int getThermalTxChainmask() {
        return thermalTxChainmask;
    }

    public int getThermalUpgradeTemp() {
        return thermalUpgradeTemp;
    }

    public int getTxChainmask() {
        return txChainmask;
    }

    public int getTxPower() {
        return txPower;
    }

    public Uuid getVersion() {
        return version;
    }

    public Set<Uuid> getVifStates() {
        return vifStates;
    }

    public boolean isDfsDemo() {
        return dfsDemo;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isThermalDowngraded() {
        return thermalDowngraded;
    }

    public void set_uuid(Uuid _uuid) {
        this._uuid = _uuid;
    }

    public void setAllowedChannels(Set<Integer> allowedChannels) {
        this.allowedChannels = allowedChannels;
    }

    public void setBcnInt(int bcnInt) {
        this.bcnInt = bcnInt;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public void setChannelMode(String channelMode) {
        this.channelMode = channelMode;
    }

    public void setChannels(Map<String, String> channels) {
        this.channels = channels;
    }

    public void setChannelSync(int channelSync) {
        this.channelSync = channelSync;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setDfsDemo(boolean dfsDemo) {
        this.dfsDemo = dfsDemo;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setFreqBand(RadioType freqBand) {
        this.freqBand = freqBand;
    }

    public void setHtMode(String htMode) {
        this.htMode = htMode;
    }

    public void setHwConfig(Map<String, String> hwConfig) {
        this.hwConfig = hwConfig;
    }

    public void setHwMode(String hwMode) {
        this.hwMode = hwMode;
    }

    public void setHwParams(Map<String, String> hwParams) {
        this.hwParams = hwParams;
    }

    public void setHwType(int hwType) {
        this.hwType = hwType;
    }

    public void setIfName(String ifName) {
        this.ifName = ifName;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public void setRadar(String radar) {
        this.radar = radar;
    }

    public void setTemperatureControl(int temperatureControl) {
        this.temperatureControl = temperatureControl;
    }

    public void setThermalDowngraded(boolean thermalDowngraded) {
        this.thermalDowngraded = thermalDowngraded;
    }

    public void setThermalDowngradeTemp(int thermalDowngradeTemp) {
        this.thermalDowngradeTemp = thermalDowngradeTemp;
    }

    public void setThermalIntegration(int thermalIntegration) {
        this.thermalIntegration = thermalIntegration;
    }

    public void setThermalShutdown(int thermalShutdown) {
        this.thermalShutdown = thermalShutdown;
    }

    public void setThermalTxChainmask(int thermalTxChainmask) {
        this.thermalTxChainmask = thermalTxChainmask;
    }

    public void setThermalUpgradeTemp(int thermalUpgradeTemp) {
        this.thermalUpgradeTemp = thermalUpgradeTemp;
    }

    public void setTxChainmask(int txChainmask) {
        this.txChainmask = txChainmask;
    }

    public void setTxPower(int txPower) {
        this.txPower = txPower;
    }

    public void setVersion(Uuid version) {
        this.version = version;
    }

    public void setVifStates(Set<Uuid> vifStates) {
        this.vifStates = vifStates;
    }

}
