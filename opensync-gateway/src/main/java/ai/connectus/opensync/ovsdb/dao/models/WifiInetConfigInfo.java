package ai.connectus.opensync.ovsdb.dao.models;

import com.vmware.ovsdb.protocol.operation.notation.Uuid;

public class WifiInetConfigInfo implements Cloneable{
    
    public boolean nat;
    public boolean enabled;
    public String ifName;
    public String ifType;
    public String ipAssignScheme;
    public boolean network;
                    
    public Uuid uuid;
    
    @Override
    public WifiInetConfigInfo clone() {
        try {
            WifiInetConfigInfo ret = (WifiInetConfigInfo)super.clone();
            return ret;
        }catch(CloneNotSupportedException e) {                
            throw new IllegalStateException("Cannot clone ", e);
        }            
    }

    @Override
    public String toString() {
        return String.format(
                "WifiInetConfigInfo [nat=%s, enabled=%s, ifName=%s, ifType=%s, ipAssignScheme=%s, network=%s, uuid=%s]",
                nat, enabled, ifName, ifType, ipAssignScheme, network, uuid);
    }
    
}