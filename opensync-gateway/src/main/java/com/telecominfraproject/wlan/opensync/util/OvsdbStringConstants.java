package com.telecominfraproject.wlan.opensync.util;


public class OvsdbStringConstants {

    // freq_band
    public static final String OVSDB_FREQ_BAND_5G = "5G";
    public static final String OVSDB_FREQ_BAND_5GL = "5GL";
    public static final String OVSDB_FREQ_BAND_5GU = "5GU";
    public static final String OVSDB_FREQ_BAND_2pt4G = "2.4G";

    // AWLAN_Node version_matrix
    public static final String FW_IMAGE_NAME_KEY = "FW_IMAGE_NAME";
    public static final String FW_IMAGE_ACTIVE_KEY = "FW_IMAGE_ACTIVE";
    public static final String FW_IMAGE_INACTIVE_KEY = "FW_IMAGE_INACTIVE";
    public static final String FW_IMAGE_DATE = "DATE";

    // AWLAN_Node values which can be placed in firmware_url column to trigger
    // behavior other than firmware download/flash
    public static final String OVSDB_AWLAN_AP_REBOOT = "reboot";
    public static final String OVSDB_AWLAN_AP_FACTORY_RESET = "factory";
    public static final String OVSDB_AWLAN_AP_SWITCH_SOFTWARE_BANK = "switch";
    
    

}
