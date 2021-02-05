package com.telecominfraproject.wlan.opensync.external.integration.models;

import com.telecominfraproject.wlan.core.model.equipment.RadioType;

public class OpensyncAPSsidConfig extends OpensyncAPBase {

    private static final long serialVersionUID = -8540144450360788799L;

    private RadioType radioType;
    private String ssid;
    private String encryption;
    private String key;
    private String mode;
    private boolean broadcast;

    @Override
    public OpensyncAPSsidConfig clone() {
        return (OpensyncAPSsidConfig) super.clone();
    }

    public String getEncryption() {
        return encryption;
    }

    public String getKey() {
        return key;
    }

    public String getMode() {
        return mode;
    }

    public RadioType getRadioType() {
        return radioType;
    }

    public String getSsid() {
        return ssid;
    }

    public boolean isBroadcast() {
        return broadcast;
    }

    public void setBroadcast(boolean broadcast) {
        this.broadcast = broadcast;
    }

    public void setEncryption(String encryption) {
        this.encryption = encryption;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setRadioType(RadioType radioType) {
        this.radioType = radioType;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

}