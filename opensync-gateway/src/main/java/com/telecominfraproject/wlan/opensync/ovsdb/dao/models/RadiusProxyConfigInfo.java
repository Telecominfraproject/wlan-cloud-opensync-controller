package com.telecominfraproject.wlan.opensync.ovsdb.dao.models;

import java.util.Objects;

import com.telecominfraproject.wlan.opensync.ovsdb.dao.OvsdbDaoBase;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import java.util.Set;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;

public class RadiusProxyConfigInfo implements Cloneable {
    
/**
    Column             Type
    ------------------ ---------------------------------------------------------------
    _version           "uuid"
    server             "string"
    realm              {"key":{"maxLength":256,"type":"string"},"max":16,"min":0}
    port               "integer"
    radsec             "boolean"
    client_cert        {"key":{"maxLength":256,"minLength":1,"type":"string"},"min":0}
    radius_config_name "string"
    passphrase         {"key":{"maxLength":128,"type":"string"},"min":0}
    _uuid              "uuid"
    client_key         {"key":{"maxLength":256,"minLength":1,"type":"string"},"min":0}
    ca_cert            {"key":{"maxLength":256,"minLength":1,"type":"string"},"min":0}
 */

    public Uuid uuid;
    public Uuid version;
    public String server;
    public String clientCert;
    public String radiusConfigName;
    public String passphrase;
    public String clientKey;
    public String caCert;
    public Set<String> realm;
    public Boolean radsec;
    public Integer port;

    public RadiusProxyConfigInfo() {

    }

    public RadiusProxyConfigInfo(Row row) {
        this.uuid = row.getUuidColumn("_uuid");
        this.version = row.getUuidColumn("_version");
        this.server = row.getStringColumn("server");
        this.clientCert = OvsdbDaoBase.getSingleValueFromSet(row, "client_cert");
        this.caCert = OvsdbDaoBase.getSingleValueFromSet(row, "ca_cert");
        this.clientKey = OvsdbDaoBase.getSingleValueFromSet(row, "client_key");
        this.passphrase = OvsdbDaoBase.getSingleValueFromSet(row, "passphrase");
        this.realm = row.getSetColumn("realm");
        this.port = row.getIntegerColumn("port").intValue();
        this.radsec = row.getBooleanColumn("radsec");
        this.radiusConfigName = row.getStringColumn("radius_config_name");
    }
    
    @Override
    public RadiusProxyConfigInfo clone() {
        try {
            RadiusProxyConfigInfo ret = (RadiusProxyConfigInfo)super.clone();
            if (realm != null) {
                ret.realm = this.realm;
            }
            return ret;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Cannot clone ", e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(caCert, clientCert, clientKey, passphrase, port, radiusConfigName, radsec, realm, server,
                uuid, version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RadiusProxyConfigInfo other = (RadiusProxyConfigInfo) obj;
        return Objects.equals(caCert, other.caCert) && Objects.equals(clientCert, other.clientCert)
                && Objects.equals(clientKey, other.clientKey) && Objects.equals(passphrase, other.passphrase)
                && Objects.equals(port, other.port) && Objects.equals(radiusConfigName, other.radiusConfigName)
                && Objects.equals(radsec, other.radsec) && Objects.equals(realm, other.realm)
                && Objects.equals(server, other.server) && Objects.equals(uuid, other.uuid)
                && Objects.equals(version, other.version);
    }

    @Override
    public String toString() {
        return "RadiusProxyConfigInfo [uuid=" + uuid + ", version=" + version + ", server=" + server + ", clientCert="
                + clientCert + ", radiusConfigName=" + radiusConfigName + ", passphrase=" + passphrase + ", clientKey="
                + clientKey + ", caCert=" + caCert + ", realm=" + realm + ", radsec=" + radsec + ", port=" + port + "]";
    }

}
