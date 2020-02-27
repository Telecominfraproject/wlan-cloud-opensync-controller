package ai.connectus.opensync.ovsdb.dao.models;

import java.util.HashSet;
import java.util.Set;

import com.vmware.ovsdb.protocol.operation.notation.Uuid;

public class BridgeInfo implements Cloneable{
    
    public Set<Uuid> portUuids;
    
    public String name;
    public Uuid uuid;
    
    @Override
    public BridgeInfo clone() {
        try {
            BridgeInfo ret = (BridgeInfo)super.clone();
            if(portUuids!=null) {
                ret.portUuids = new HashSet<>(this.portUuids);
            }
            return ret;
        }catch(CloneNotSupportedException e) {                
            throw new IllegalStateException("Cannot clone ", e);
        }            
    }

    @Override
    public String toString() {
        return String.format("BridgeInfo [portUuids=%s, name=%s, uuid=%s]", portUuids, name, uuid);
    }
    
}