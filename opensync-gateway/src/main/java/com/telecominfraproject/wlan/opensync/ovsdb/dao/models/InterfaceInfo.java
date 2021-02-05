package com.telecominfraproject.wlan.opensync.ovsdb.dao.models;

import java.util.HashMap;
import java.util.Map;

import com.telecominfraproject.wlan.opensync.ovsdb.dao.OvsdbDaoBase;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;
@Deprecated
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
  
    public InterfaceInfo() {}
    
    public InterfaceInfo(Row row) {
        this.name = row.getStringColumn("name");
        this.type = row.getStringColumn("type");
        this.uuid = row.getUuidColumn("_uuid");
        Long tmp = OvsdbDaoBase.getSingleValueFromSet(row, "ofport");
        this.ofport = tmp != null ? tmp.intValue() : 0;
        tmp = OvsdbDaoBase.getSingleValueFromSet(row, "mtu");
        this.mtu = tmp != null ? tmp.intValue() : 0;
        tmp = OvsdbDaoBase.getSingleValueFromSet(row, "ifindex");
        this.ifIndex = tmp != null ? tmp.intValue() : 0;
        String tmpStr = OvsdbDaoBase.getSingleValueFromSet(row, "link_state");
        this.linkState = tmpStr != null ? tmpStr : "";
        tmpStr = OvsdbDaoBase.getSingleValueFromSet(row, "admin_state");
        this.adminState = tmpStr != null ? tmpStr : "";
        this.options = row.getMapColumn("options");
    }
    
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