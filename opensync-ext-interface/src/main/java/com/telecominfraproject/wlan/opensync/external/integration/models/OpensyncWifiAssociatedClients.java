/**
 * 
 */
package com.telecominfraproject.wlan.opensync.external.integration.models;

import java.util.HashSet;
import java.util.Set;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;

/**
 * @author mikehansen
 *
 */
public class OpensyncWifiAssociatedClients extends BaseJsonModel {

    private static final long serialVersionUID = -7088651136971662138L;

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
        super();
        capabilities = new HashSet<>();
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Set<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Set<String> set) {
        this.capabilities = set;
    }

    public int getUapsd() {
        return uapsd;
    }

    public void setUapsd(int uapsd) {
        this.uapsd = uapsd;
    }

    public String getKick() {
        return kick;
    }

    public void setKick(String kick) {
        this.kick = kick;
    }

    public String getOftag() {
        return oftag;
    }

    public void setOftag(String oftag) {
        this.oftag = oftag;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public Uuid get_uuid() {
        return _uuid;
    }

    public void set_uuid(Uuid _uuid) {
        this._uuid = _uuid;
    }

    public Uuid getVersion() {
        return version;
    }

    public void setVersion(Uuid version) {
        this.version = version;
    }

}
