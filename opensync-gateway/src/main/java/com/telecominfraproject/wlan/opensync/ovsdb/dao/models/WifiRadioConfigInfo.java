package com.telecominfraproject.wlan.opensync.ovsdb.dao.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.telecominfraproject.wlan.opensync.ovsdb.dao.OvsdbDaoBase;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;

public class WifiRadioConfigInfo implements Cloneable {

    public Set<Uuid> vifConfigUuids;

    public String freqBand;
    public int channel;
    public Integer txPower;
    public String channelMode;
    public boolean enabled;
    public String htMode;
    public Map<String, String> hwConfig;
    public Map<String, String> customOptions;
    public String country;
    public int beaconInterval;
    public int txChainmask;
    public int rxChainmask;

    public String ifName;
    public Uuid uuid;

    public String hwType;

    public WifiRadioConfigInfo() {

    }

    public WifiRadioConfigInfo(Row row) {
        this.uuid = row.getUuidColumn("_uuid");
        this.ifName = row.getStringColumn("if_name");
        Long beaconTmp = OvsdbDaoBase.getSingleValueFromSet(row, "bcn_int");
        if (beaconTmp == null) {
            beaconTmp = 0L;
        }
        this.beaconInterval = beaconTmp.intValue();
        Long channelTmp = OvsdbDaoBase.getSingleValueFromSet(row, "channel");
        if (channelTmp == null) {
            channelTmp = -1L;
        }
        this.channel = channelTmp.intValue();
        this.channelMode = OvsdbDaoBase.getSingleValueFromSet(row, "channel_mode");
        this.country = OvsdbDaoBase.getSingleValueFromSet(row, "country");
        Boolean tmp = OvsdbDaoBase.getSingleValueFromSet(row, "enabled");
        this.enabled = tmp != null ? tmp : false;
        this.htMode = OvsdbDaoBase.getSingleValueFromSet(row, "ht_mode");
        this.txPower = OvsdbDaoBase.getSingleValueFromSet(row, "txPower");
        Long txChainmaskTmp = OvsdbDaoBase.getSingleValueFromSet(row, "tx_chainmask");
        if (txChainmaskTmp == null) txChainmaskTmp = 0L;
        this.txChainmask = txChainmaskTmp.intValue();       
        Long rxChainmaskTmp = OvsdbDaoBase.getSingleValueFromSet(row, "rx_chainmask");
        if (rxChainmaskTmp == null) rxChainmaskTmp = 0L;
        this.rxChainmask = rxChainmaskTmp.intValue();        
        this.vifConfigUuids = row.getSetColumn("vif_configs");
        this.freqBand = row.getStringColumn("freq_band");
        this.customOptions = row.getMapColumn("custom_options");
        this.hwConfig = row.getMapColumn("hw_config");
        this.hwType = row.getStringColumn("hw_type");
    }

    @Override
    public WifiRadioConfigInfo clone() {
        try {
            WifiRadioConfigInfo ret = (WifiRadioConfigInfo) super.clone();
            if (vifConfigUuids != null) {
                ret.vifConfigUuids = new HashSet<>(this.vifConfigUuids);
            }
            if (hwConfig != null) {
                ret.hwConfig = new HashMap<>(this.hwConfig);
            }
            if (customOptions != null) {
                ret.customOptions = new HashMap<>(this.customOptions);
            }
            return ret;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Cannot clone ", e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(beaconInterval, channel, channelMode, country, customOptions, enabled, freqBand, htMode,
                hwConfig, hwType, ifName, rxChainmask, txChainmask, txPower, uuid, vifConfigUuids);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WifiRadioConfigInfo other = (WifiRadioConfigInfo) obj;
        return beaconInterval == other.beaconInterval && channel == other.channel
                && Objects.equals(channelMode, other.channelMode) && Objects.equals(country, other.country)
                && Objects.equals(customOptions, other.customOptions) && enabled == other.enabled
                && Objects.equals(freqBand, other.freqBand) && Objects.equals(htMode, other.htMode)
                && Objects.equals(hwConfig, other.hwConfig) && Objects.equals(hwType, other.hwType)
                && Objects.equals(ifName, other.ifName) && rxChainmask == other.rxChainmask
                && txChainmask == other.txChainmask && Objects.equals(txPower, other.txPower)
                && Objects.equals(uuid, other.uuid) && Objects.equals(vifConfigUuids, other.vifConfigUuids);
    }

    @Override
    public String toString() {
        return "WifiRadioConfigInfo [" + (vifConfigUuids != null ? "vifConfigUuids=" + vifConfigUuids + ", " : "")
                + (freqBand != null ? "freqBand=" + freqBand + ", " : "") + "channel=" + channel + ", "
                + (txPower != null ? "txPower=" + txPower + ", " : "")
                + (channelMode != null ? "channelMode=" + channelMode + ", " : "") + "enabled=" + enabled + ", "
                + (htMode != null ? "htMode=" + htMode + ", " : "")
                + (hwConfig != null ? "hwConfig=" + hwConfig + ", " : "")
                + (customOptions != null ? "customOptions=" + customOptions + ", " : "")
                + (country != null ? "country=" + country + ", " : "") + "beaconInterval=" + beaconInterval
                + ", txChainmask=" + txChainmask + ", rxChainmask=" + rxChainmask + ", "
                + (ifName != null ? "ifName=" + ifName + ", " : "") + (uuid != null ? "uuid=" + uuid + ", " : "")
                + (hwType != null ? "hwType=" + hwType : "") + "]";
    }



}