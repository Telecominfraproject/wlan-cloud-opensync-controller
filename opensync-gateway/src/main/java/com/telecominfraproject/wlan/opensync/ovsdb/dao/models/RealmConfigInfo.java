package com.telecominfraproject.wlan.opensync.ovsdb.dao.models;

import java.util.Objects;

import com.telecominfraproject.wlan.opensync.ovsdb.dao.OvsdbDaoBase;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;

public class RealmConfigInfo implements Cloneable {

    public Uuid uuid;
    public Uuid server;
    public Uuid version;
    public String realm;
    public String realmConfigName;
        
    public RealmConfigInfo() {
        
    }
    
    public RealmConfigInfo(Row row) {
        this.uuid = row.getUuidColumn("_uuid");
        this.version = row.getUuidColumn("_version");
        this.server = OvsdbDaoBase.getSingleValueFromSet(row, "server");
        this.realm = OvsdbDaoBase.getSingleValueFromSet(row, "realm");
        this.realmConfigName = row.getStringColumn("realm_config_name");       
    }
    
    @Override
    public RealmConfigInfo clone() {
        try {
            return (RealmConfigInfo)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Cannot clone ", e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(realm, realmConfigName, server, uuid, version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RealmConfigInfo other = (RealmConfigInfo) obj;
        return Objects.equals(realm, other.realm) && Objects.equals(realmConfigName, other.realmConfigName)
                && Objects.equals(server, other.server) && Objects.equals(uuid, other.uuid)
                && Objects.equals(version, other.version);
    }

    @Override
    public String toString() {
        return "RealmConfigInfo [uuid=" + uuid + ", server=" + server + ", version=" + version + ", realm=" + realm
                + ", realmConfigName=" + realmConfigName + "]";
    }
   
    
    
}
