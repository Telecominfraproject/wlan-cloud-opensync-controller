package com.telecominfraproject.wlan.opensync.ovsdb.dao.utilities;

import com.telecominfraproject.wlan.client.models.ClientType;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.core.model.equipment.SecurityType;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.enumerations.DhcpFpDeviceType;

import com.telecominfraproject.wlan.servicemetric.apnode.models.StateUpDownError;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentUpgradeState;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentUpgradeState.FailureReason;
import com.telecominfraproject.wlan.systemevent.equipment.realtime.StreamingVideoType;

import sts.OpensyncStats.RadioBandType;
import sts.OpensyncStats.StateUpDown;

public class OvsdbToWlanCloudTypeMappingUtility {

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
            case OvsdbStringConstants.OVSDB_FREQ_BAND_2pt4G:
                return RadioType.is2dot4GHz;
            case OvsdbStringConstants.OVSDB_FREQ_BAND_5G:
                return RadioType.is5GHz;
            case OvsdbStringConstants.OVSDB_FREQ_BAND_5GL:
                return RadioType.is5GHzL;
            case OvsdbStringConstants.OVSDB_FREQ_BAND_5GU:
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

    public static StateUpDownError getCloudMetricsStateFromOpensyncStatsStateUpDown(StateUpDown apNetworkProbeState) {
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

    public static StreamingVideoType getCloudStreamingVideoTypeFromApReport(
            sts.OpensyncStats.StreamingVideoType apReportStreamingVideoType) {
        switch (apReportStreamingVideoType) {
            case NETFLIX:
                return StreamingVideoType.NETFLIX;
            case YOUTUBE:
                return StreamingVideoType.YOUTUBE;

            case PLEX:
                return StreamingVideoType.PLEX;

            case UNKNOWN:
                return StreamingVideoType.UNKNOWN;

            default:
                return StreamingVideoType.UNSUPPORTED;
        }
    }

    public static EquipmentUpgradeState getCloudEquipmentUpgradeStateFromOpensyncUpgradeStatus(int upgradeStatus) {

        EquipmentUpgradeState ret = EquipmentUpgradeState.undefined;

        switch (upgradeStatus) {
            case 0:
                break; // nothing
            case -1:
                ret = EquipmentUpgradeState.download_failed;
                break;
            case -3:
                ret = EquipmentUpgradeState.download_failed;
                break;
            case -4:
                ret = EquipmentUpgradeState.download_failed;
                break;
            case -5:
                ret = EquipmentUpgradeState.download_failed;
                break;
            case -6:
                ret = EquipmentUpgradeState.download_failed;
                break;
            case -7:
                ret = EquipmentUpgradeState.apply_failed;
                break;
            case -8:
                ret = EquipmentUpgradeState.apply_failed;
                break;
            case -9:
                ret = EquipmentUpgradeState.apply_failed;
                break;
            case -10:
                ret = EquipmentUpgradeState.apply_failed;
                break;
            case -11:
                ret = EquipmentUpgradeState.apply_failed;
                break;
            case -12:
                ret = EquipmentUpgradeState.reboot_failed;
                break;
            case -14:
                ret = EquipmentUpgradeState.apply_failed;
                break;
            case -15:
                ret = EquipmentUpgradeState.apply_failed;
                break;
            case -16:
                ret = EquipmentUpgradeState.download_failed;
                break;
            case 10:
                ret = EquipmentUpgradeState.download_initiated;
                break;
            case 11:
                ret = EquipmentUpgradeState.download_complete;
                break;
            case 20:
                ret = EquipmentUpgradeState.apply_initiated;
                break;
            case 21:
                ret = EquipmentUpgradeState.apply_complete;
                break;
            case 30:
                ret = EquipmentUpgradeState.apply_initiated;
                break;
            case 31:
                ret = EquipmentUpgradeState.apply_complete;
                break;
            default:

        }

        return ret;

    }

    public static EquipmentUpgradeState.FailureReason getCloudEquipmentUpgradeFailureReasonFromOpensyncUpgradeStatus(
            int upgradeStatus) {

        EquipmentUpgradeState.FailureReason ret = null;

        switch (upgradeStatus) {
            case 0:
                break; // nothing
            case -1:
                ret = FailureReason.downloadRequestRejected;
                break;
            case -3:
                ret = FailureReason.unreachableUrl;
                break;
            case -4:
                ret = FailureReason.downloadFailed;
                break;
            case -5:
                ret = FailureReason.downloadFailed;
                break;
            case -6:
                ret = FailureReason.validationFailed;
                break;
            case -7:
                ret = FailureReason.validationFailed;
                break;
            case -8:
                ret = FailureReason.applyFailed;
                break;
            case -9:
                ret = FailureReason.applyFailed;
                break;
            case -10:
                ret = FailureReason.validationFailed;
                break;
            case -11:
                ret = FailureReason.applyFailed;
                break;
            case -12:
                ret = FailureReason.rebootTimedout;
                break;
            case -14:
                ret = FailureReason.applyFailed;
                break;
            case -15:
                ret = FailureReason.applyFailed;
                break;
            case -16:
                ret = FailureReason.downloadRequestFailedFlashFull;
                break;

            default:

        }

        return ret;

    }

    public static SecurityType getCloudSecurityTypeFromOpensyncStats(sts.OpensyncStats.SecurityType statsSecurityType) {
        switch (statsSecurityType) {
            case SEC_OPEN:
                return SecurityType.OPEN;
            case SEC_PSK:
                return SecurityType.PSK;
            case SEC_RADIUS:
                return SecurityType.RADIUS;
            default:
                return SecurityType.UNSUPPORTED;
        }
    }

}
