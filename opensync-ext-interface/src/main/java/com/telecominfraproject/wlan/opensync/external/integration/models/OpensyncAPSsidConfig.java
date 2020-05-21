package com.telecominfraproject.wlan.opensync.external.integration.models;

import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

public class OpensyncAPSsidConfig extends BaseJsonModel {

    private static final long serialVersionUID = -8540144450360788799L;

    private RadioType radioType;
    private String ssid;
    private String encryption;
    private String key;
    private String mode;
    private boolean broadcast;
    
    public RadioType getRadioType() {
        return radioType;
    }

    public void setRadioType(RadioType radioType) {
        this.radioType = radioType;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getEncryption() {
        return encryption;
    }

    public void setEncryption(String encryption) {
        this.encryption = encryption;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public boolean isBroadcast() {
        return broadcast;
    }

    public void setBroadcast(boolean broadcast) {
        this.broadcast = broadcast;
    }

    @Override
    public OpensyncAPSsidConfig clone() {
        return (OpensyncAPSsidConfig)super.clone();
    }

}