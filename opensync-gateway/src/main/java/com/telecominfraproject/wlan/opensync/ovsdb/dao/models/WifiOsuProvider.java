package com.telecominfraproject.wlan.opensync.ovsdb.dao.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;

public class WifiOsuProvider implements Cloneable {

    public static String[] ovsdbColumns = { "_version", "osu_nai", "icon", "server_uri", "method_list", "_uuid",
            "osu_friendly_name", "service_description" };
    public Uuid version;
    public Map<String, String> osuNai;
    public Map<String, String> icon;
    public String serverUri;
    public int methodList;
    public Uuid uuid;
    public Map<String, String> osuFriendlyName;
    public Map<String, String> serviceDescription;


    public WifiOsuProvider() {
    }

    public WifiOsuProvider(Row row) {
        this.version = row.getUuidColumn("_version");
        this.uuid = row.getUuidColumn("_uuid");
        this.osuNai = row.getMapColumn("osu_nai");
        this.icon = row.getMapColumn("icon");
        if ((row.getColumns().get("server_uri") != null) && row.getColumns().get("server_uri").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.serverUri = row.getStringColumn("server_uri");
        }
        if ((row.getColumns().get("method_list") != null) && row.getColumns().get("method_list").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.methodList = row.getIntegerColumn("method_list").intValue();
        }
        this.osuFriendlyName = row.getMapColumn("osu_friendly_name");
        this.serviceDescription = row.getMapColumn("service_description");
    }


    @Override
    public WifiOsuProvider clone() {
        try {
            WifiOsuProvider ret = (WifiOsuProvider) super.clone();

            if (osuNai != null) {
                ret.osuNai = new HashMap<>(this.osuNai);
            }
            if (icon != null) {
                ret.icon = new HashMap<>(this.icon);
            }
            if (osuFriendlyName != null) {
                ret.osuFriendlyName = new HashMap<>(this.osuFriendlyName);
            }
            if (serviceDescription != null) {
                ret.serviceDescription = new HashMap<>(this.serviceDescription);
            }

            return ret;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Cannot clone ", e);
        }
    }

    @Override
    public String toString() {
        return String.format(
                "WifiOsuProvider [version=%s, osuNai=%s, icon=%s, serverUri=%s, methodList=%s, uuid=%s, osuFriendlyName=%s, serviceDescription=%s]",
                version, osuNai, icon, serverUri, methodList, uuid, osuFriendlyName, serviceDescription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(icon, methodList, osuFriendlyName, osuNai, serverUri, serviceDescription, uuid, version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WifiOsuProvider)) {
            return false;
        }
        WifiOsuProvider other = (WifiOsuProvider) obj;
        return Objects.equals(icon, other.icon) && methodList == other.methodList
                && Objects.equals(osuFriendlyName, other.osuFriendlyName) && Objects.equals(osuNai, other.osuNai)
                && Objects.equals(serverUri, other.serverUri)
                && Objects.equals(serviceDescription, other.serviceDescription) && Objects.equals(uuid, other.uuid)
                && Objects.equals(version, other.version);
    }


}