/**
 *
 */
package com.telecominfraproject.wlan.opensync.external.integration.models;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;
import com.vmware.ovsdb.protocol.operation.notation.Value;

/**
 * @author mikehansen
 *
 */
public class OpensyncWifiAssociatedClients extends OpensyncAPBase {

    private static final long serialVersionUID = -7088651136971662138L;

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public String keyId;
    public String mac;
    public String state;
    public Set<String> capabilities;
    public int uapsd;
    public String kick;
    public String oftag;
    public Uuid _uuid;

    public Uuid version;

    public OpensyncWifiAssociatedClients() {
        capabilities = new HashSet<>();
    }

    public OpensyncWifiAssociatedClients(Row row) {
        this();

        Map<String, Value> map = row.getColumns();

        if ((map.get("mac") != null)
                && map.get("mac").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setMac(row.getStringColumn("mac"));
        }
        if (row.getSetColumn("capabilities") != null) {
            this.setCapabilities(row.getSetColumn("capabilities"));
        }
        if ((map.get("state") != null)
                && map.get("state").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setState(row.getStringColumn("state"));
        }
        if ((map.get("_version") != null)
                && map.get("_version").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setVersion(row.getUuidColumn("_version"));
        }
        if ((map.get("_uuid") != null)
                && map.get("_uuid").getClass().equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.setVersion(row.getUuidColumn("_uuid"));
        }
    }

    public Uuid get_uuid() {
        return _uuid;
    }

    public Set<String> getCapabilities() {
        return capabilities;
    }

    public String getKeyId() {
        return keyId;
    }

    public String getKick() {
        return kick;
    }

    public String getMac() {
        return mac;
    }

    public String getOftag() {
        return oftag;
    }

    public String getState() {
        return state;
    }

    public int getUapsd() {
        return uapsd;
    }

    public Uuid getVersion() {
        return version;
    }

    public void set_uuid(Uuid _uuid) {
        this._uuid = _uuid;
    }

    public void setCapabilities(Set<String> set) {
        this.capabilities = set;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public void setKick(String kick) {
        this.kick = kick;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public void setOftag(String oftag) {
        this.oftag = oftag;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setUapsd(int uapsd) {
        this.uapsd = uapsd;
    }

    public void setVersion(Uuid version) {
        this.version = version;
    }

}
