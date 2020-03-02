package com.telecominfraproject.wlan.opensync.external.integration.models;

import java.util.ArrayList;
import java.util.List;

import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

public class OpensyncAPConfig extends BaseJsonModel {

    private static final long serialVersionUID = 3917975477206236668L;

    private OpensyncAPRadioConfig radioConfig;    
    private List<OpensyncAPSsidConfig> ssidConfigs;
    
    
    public OpensyncAPRadioConfig getRadioConfig() {
        return radioConfig;
    }


    public void setRadioConfig(OpensyncAPRadioConfig radioConfig) {
        this.radioConfig = radioConfig;
    }


    public List<OpensyncAPSsidConfig> getSsidConfigs() {
        return ssidConfigs;
    }


    public void setSsidConfigs(List<OpensyncAPSsidConfig> ssidConfigs) {
        this.ssidConfigs = ssidConfigs;
    }


    @Override
    public OpensyncAPConfig clone() {
        OpensyncAPConfig ret = (OpensyncAPConfig)super.clone();
        if(radioConfig!=null) {
            ret.radioConfig = radioConfig.clone();
        }
        
        if(ssidConfigs!=null) {
            ret.ssidConfigs = new ArrayList<OpensyncAPSsidConfig>();
            for(OpensyncAPSsidConfig s: ssidConfigs) {
                ret.ssidConfigs.add(s.clone());
            }
        }
        
        return ret;
    }
    
    public static void main(String[] args) {
        OpensyncAPConfig cfg = new OpensyncAPConfig();
        cfg.radioConfig = new OpensyncAPRadioConfig();
        cfg.ssidConfigs = new ArrayList<OpensyncAPSsidConfig>();
        
        cfg.radioConfig.setRadioChannel24G(1);
        cfg.radioConfig.setRadioChannel5LG(44);
        cfg.radioConfig.setRadioChannel5HG(108);

        OpensyncAPSsidConfig ssidCfg = new OpensyncAPSsidConfig();
        ssidCfg.setRadioType(RadioType.is2dot4GHz);
        ssidCfg.setSsid("Connectus-standalone");
        ssidCfg.setEncryption("WPA-PSK");
        ssidCfg.setKey("12345678");
        ssidCfg.setMode("2");
        cfg.ssidConfigs.add(ssidCfg);

        ssidCfg = new OpensyncAPSsidConfig();
        ssidCfg.setRadioType(RadioType.is5GHz);
        ssidCfg.setSsid("Connectus-standalone-5");
        ssidCfg.setEncryption("WPA-PSK");
        ssidCfg.setKey("12345678");
        ssidCfg.setMode("2");
        cfg.ssidConfigs.add(ssidCfg);

        System.out.println(cfg.toPrettyString());
    }
}
