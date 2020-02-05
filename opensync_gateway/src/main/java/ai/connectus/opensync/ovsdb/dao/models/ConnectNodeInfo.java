package ai.connectus.opensync.ovsdb.dao.models;

import java.util.HashMap;
import java.util.Map;

public class ConnectNodeInfo implements Cloneable{
    public Map<String,String> mqttSettings;
    public String redirectorAddr;
    public String managerAddr;
    public String skuNumber;
    public String serialNumber;
    
    @Override
    public ConnectNodeInfo clone() {
        try {
            ConnectNodeInfo ret = (ConnectNodeInfo)super.clone();
            if (this.mqttSettings!=null) {
                ret.mqttSettings = new HashMap<>(this.mqttSettings);
            }
            return ret;
        }catch(CloneNotSupportedException e) {                
            throw new IllegalStateException("Cannot clone ", e);
        }            
    }

    @Override
    public String toString() {
        return String.format(
                "ConnectNodeInfo [mqttSettings=%s, redirectorAddr=%s, managerAddr=%s, skuNumber=%s, serialNumber=%s]",
                mqttSettings, redirectorAddr, managerAddr, skuNumber, serialNumber);
    }
            
}