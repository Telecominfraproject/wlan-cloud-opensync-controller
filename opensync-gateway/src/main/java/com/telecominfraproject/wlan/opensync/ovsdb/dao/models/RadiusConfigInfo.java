package com.telecominfraproject.wlan.opensync.ovsdb.dao.models;

import java.util.Objects;

import com.telecominfraproject.wlan.opensync.ovsdb.dao.OvsdbDaoBase;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;

public class RadiusConfigInfo implements Cloneable {

    public Uuid uuid;
    public Uuid version;
    public String server;
    public String clientCert;
    public String radiusConfigName;
    public String passphrase;
    public String clientKey;
    public String caCert;

    public RadiusConfigInfo() {

    }

    public RadiusConfigInfo(Row row) {
        this.uuid = row.getUuidColumn("_uuid");
        this.version = row.getUuidColumn("_version");
        this.server = row.getStringColumn("server");
        this.clientCert = OvsdbDaoBase.getSingleValueFromSet(row, "client_cert");
        this.caCert = OvsdbDaoBase.getSingleValueFromSet(row, "ca_cert");
        this.clientKey = OvsdbDaoBase.getSingleValueFromSet(row, "client_key");
        this.passphrase = OvsdbDaoBase.getSingleValueFromSet(row, "passphrase");
        this.radiusConfigName = row.getStringColumn("radius_config_name");
    }
    
    @Override
    public RadiusConfigInfo clone() {
        try {
            return (RadiusConfigInfo)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Cannot clone ", e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(caCert, clientCert, clientKey, passphrase, radiusConfigName, server, uuid, version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RadiusConfigInfo other = (RadiusConfigInfo) obj;
        return Objects.equals(caCert, other.caCert) && Objects.equals(clientCert, other.clientCert)
                && Objects.equals(clientKey, other.clientKey) && Objects.equals(passphrase, other.passphrase)
                && Objects.equals(radiusConfigName, other.radiusConfigName) && Objects.equals(server, other.server)
                && Objects.equals(uuid, other.uuid) && Objects.equals(version, other.version);
    }

    @Override
    public String toString() {
        return "RadiusConfigInfo [uuid=" + uuid + ", version=" + version + ", server=" + server + ", clientCert="
                + clientCert + ", radiusConfigName=" + radiusConfigName + ", passphrase=" + passphrase + ", clientKey="
                + clientKey + ", caCert=" + caCert + "]";
    }
    
    

}
