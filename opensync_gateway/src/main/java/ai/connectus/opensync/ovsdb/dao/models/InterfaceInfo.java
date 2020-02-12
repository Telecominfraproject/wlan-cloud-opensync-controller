package ai.connectus.opensync.ovsdb.dao.models;

import java.util.HashMap;

import java.util.Map;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;

public class InterfaceInfo implements Cloneable{
    
    public String adminState;
    public String linkState;
    public int ifIndex;
    public int mtu;
    public int ofport;
    
    public String name;
    public String type;
    public Uuid uuid;
    
    public Map<String,String> options;
    
    @Override
    public InterfaceInfo clone() {
        try {
            InterfaceInfo ret = (InterfaceInfo)super.clone();
            if(options!=null) {
                ret.options = new HashMap<>(this.options);
            }
            return ret;
        }catch(CloneNotSupportedException e) {                
            throw new IllegalStateException("Cannot clone ", e);
        }            
    }

    @Override
    public String toString() {
        return String.format(
                "InterfaceInfo [adminState=%s, linkState=%s, ifIndex=%s, mtu=%s, ofport=%s, name=%s, uuid=%s]",
                adminState, linkState, ifIndex, mtu, ofport, name, uuid);
    }
    
}