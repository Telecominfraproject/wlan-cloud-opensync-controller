package ai.connectus.opensync.ovsdb.dao.models;

import java.util.HashSet;
import java.util.Set;

import com.vmware.ovsdb.protocol.operation.notation.Uuid;

public class PortInfo implements Cloneable{
    
    public Set<Uuid> interfaceUuids;
    
    public String name;
    public Uuid uuid;
    
    @Override
    public PortInfo clone() {
        try {
            PortInfo ret = (PortInfo)super.clone();
            if(interfaceUuids!=null) {
                ret.interfaceUuids = new HashSet<>(this.interfaceUuids);
            }
            return ret;
        }catch(CloneNotSupportedException e) {                
            throw new IllegalStateException("Cannot clone ", e);
        }            
    }

    @Override
    public String toString() {
        return String.format("PortInfo [interfaceUuids=%s, name=%s, uuid=%s]", interfaceUuids, name, uuid);
    }
    
}