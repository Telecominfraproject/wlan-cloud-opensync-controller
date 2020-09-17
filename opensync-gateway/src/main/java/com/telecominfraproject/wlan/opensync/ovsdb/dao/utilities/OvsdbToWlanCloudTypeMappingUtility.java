package com.telecominfraproject.wlan.opensync.ovsdb.dao.utilities;

import com.telecominfraproject.wlan.client.models.ClientType;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.enumerations.DhcpFpDeviceType;
//import com.telecominfraproject.wlan.servicemetric.apnode.models.StateUpDownError;
import com.telecominfraproject.wlan.servicemetric.apnode.models.StateUpDownError;

import sts.OpensyncStats.RadioBandType;
import sts.OpensyncStats.StateUpDown;

public class OvsdbToWlanCloudTypeMappingUtility {


    public static final String OVSDB_FREQ_BAND_5G = "5G";
    public static final String OVSDB_FREQ_BAND_5GL = "5GL";
    public static final String OVSDB_FREQ_BAND_5GU = "5GU";
    public static final String OVSDB_FREQ_BAND_2pt4G = "2.4G";

    public static ClientType getClientTypeForDhcpFpDeviceType(DhcpFpDeviceType dhcpFpDeviceType) {

        ClientType ret = ClientType.UNSUPPORTED;

        if (dhcpFpDeviceType.equals(DhcpFpDeviceType.DHCP_FP_DEV_TYPE_GAME)) {
            ret = ClientType.Game;
        } else if (dhcpFpDeviceType.equals(DhcpFpDeviceType.DHCP_FP_DEV_TYPE_PC)) {
            ret = ClientType.PersonalComputer;
        } else if (dhcpFpDeviceType.equals(DhcpFpDeviceType.DHCP_FP_DEV_TYPE_MOBILE)) {
            ret = ClientType.Mobile;
        } else if (dhcpFpDeviceType.equals(DhcpFpDeviceType.DHCP_FP_DEV_TYPE_PRINTER)) {
            ret = ClientType.Printer;
        } else if (dhcpFpDeviceType.equals(DhcpFpDeviceType.DHCP_FP_DEV_TYPE_VOIP)) {
            ret = ClientType.VoIP;
        } else if (dhcpFpDeviceType.equals(DhcpFpDeviceType.DHCP_FP_DEV_TYPE_MISC)) {
            ret = ClientType.Misc;
        } else if (dhcpFpDeviceType.equals(DhcpFpDeviceType.DHCP_FP_DEV_TYPE_MONITORING)) {
            ret = ClientType.Monitoring;
        } else if (dhcpFpDeviceType.equals(DhcpFpDeviceType.DHCP_FP_DEV_TYPE_VIDEO)) {
            ret = ClientType.Video;
        } else if (dhcpFpDeviceType.equals(DhcpFpDeviceType.DHCP_FP_DEV_TYPE_MAX)) {
            ret = ClientType.Max;
        }

        return ret;
    }

    public static RadioType getRadioTypeForOvsdbRadioFreqBand(String ovsdbRadioFreqBand) {

        switch (ovsdbRadioFreqBand) {
            case OVSDB_FREQ_BAND_2pt4G:
                return RadioType.is2dot4GHz;
            case OVSDB_FREQ_BAND_5G:
                return RadioType.is5GHz;
            case OVSDB_FREQ_BAND_5GL:
                return RadioType.is5GHzL;
            case OVSDB_FREQ_BAND_5GU:
                return RadioType.is5GHzU;
            default:
                return RadioType.UNSUPPORTED;
        }

    }

    public static RadioType getRadioTypeFromOpensyncStatsRadioBandType(RadioBandType band) {
        switch (band) {
            case BAND2G:
                return RadioType.is2dot4GHz;
            case BAND5G:
                return RadioType.is5GHz;
            case BAND5GU:
                return RadioType.is5GHzU;
            case BAND5GL:
                return RadioType.is5GHzL;
            default:
                return RadioType.UNSUPPORTED;
        }
    }

    public static StateUpDownError getCloudDnsStateFromOpensyncStatsStateUpDown(StateUpDown apNetworkProbeState) {
        switch (apNetworkProbeState) {
            case SUD_down:
                return StateUpDownError.disabled;
            case SUD_up:
                return StateUpDownError.enabled;
            case SUD_error:
                return StateUpDownError.error;

            default:
                return StateUpDownError.UNSUPPORTED;

        }

    }

}
