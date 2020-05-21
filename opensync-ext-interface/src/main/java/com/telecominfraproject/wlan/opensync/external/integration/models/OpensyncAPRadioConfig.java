package com.telecominfraproject.wlan.opensync.external.integration.models;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

public class OpensyncAPRadioConfig extends BaseJsonModel {

    private static final long serialVersionUID = 5683558403622855381L;

    private String country;
    private int radioChannel24G;
    private int radioChannel5LG;
    private int radioChannel5HG;
    
    public int getRadioChannel24G() {
        return radioChannel24G;
    }

    public void setRadioChannel24G(int radioChannel24G) {
        this.radioChannel24G = radioChannel24G;
    }

    public int getRadioChannel5LG() {
        return radioChannel5LG;
    }

    public void setRadioChannel5LG(int radioChannel5LG) {
        this.radioChannel5LG = radioChannel5LG;
    }

    public int getRadioChannel5HG() {
        return radioChannel5HG;
    }

    public void setRadioChannel5HG(int radioChannel5HG) {
        this.radioChannel5HG = radioChannel5HG;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    @Override
    public OpensyncAPRadioConfig clone() {
        return (OpensyncAPRadioConfig)super.clone();
    }
}