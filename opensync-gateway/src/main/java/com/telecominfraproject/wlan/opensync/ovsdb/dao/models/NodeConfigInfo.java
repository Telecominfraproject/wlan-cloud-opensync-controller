package com.telecominfraproject.wlan.opensync.ovsdb.dao.models;

import java.util.Objects;

import com.telecominfraproject.wlan.opensync.ovsdb.dao.OvsdbDaoBase;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;

public class NodeConfigInfo implements Cloneable {

	public Uuid uuid;
	public String key;
	public String value;
	public Boolean persist;
	public String module;

	public NodeConfigInfo() {
	    
	}
	
	public NodeConfigInfo(Row row) {
        this.uuid = row.getUuidColumn("_uuid");
        this.key = OvsdbDaoBase.getSingleValueFromSet(row, "key");
        this.value = OvsdbDaoBase.getSingleValueFromSet(row, "value");
        this.module = OvsdbDaoBase.getSingleValueFromSet(row, "module");
        Boolean tmpPersist = OvsdbDaoBase.getSingleValueFromSet(row, "persist");
        this.persist = tmpPersist == null ? false : tmpPersist;       
	}
	
	@Override
	public NodeConfigInfo clone() {
		try {
			NodeConfigInfo ret = (NodeConfigInfo) super.clone();
			
			return ret;
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException("Cannot clone ", e);
		}
	}

    @Override
    public int hashCode() {
        return Objects.hash(key, module, persist, uuid, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NodeConfigInfo other = (NodeConfigInfo) obj;
        return Objects.equals(key, other.key) && Objects.equals(module, other.module)
                && Objects.equals(persist, other.persist) && Objects.equals(uuid, other.uuid)
                && Objects.equals(value, other.value);
    }

    @Override
    public String toString() {
        return "NodeConfigInfo [" + (uuid != null ? "uuid=" + uuid + ", " : "")
                + (key != null ? "key=" + key + ", " : "") + (value != null ? "value=" + value + ", " : "")
                + (persist != null ? "persist=" + persist + ", " : "") + (module != null ? "module=" + module : "")
                + "]";
    }

}