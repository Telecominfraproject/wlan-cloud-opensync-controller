package com.telecominfraproject.wlan.opensync.ovsdb.dao.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
	public String country;
	public int beaconInterval;

	public String ifName;
	public Uuid uuid;

	public String hwType;

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
			return ret;
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException("Cannot clone ", e);
		}
	}

	@Override
	public String toString() {
		return String.format(
				"WifiRadioConfigInfo [vifConfigUuids=%s, freqBand=%s, channel=%s, txPower=%s, channelMode=%s, enabled=%s, htMode=%s, hwConfig=%s, hwType=%s, country=%s, bcn_int=%s, ifName=%s, uuid=%s]",
				vifConfigUuids, freqBand, channel, txPower, channelMode, enabled, htMode, hwConfig, hwType,country,
				beaconInterval, ifName, uuid);
	}

}