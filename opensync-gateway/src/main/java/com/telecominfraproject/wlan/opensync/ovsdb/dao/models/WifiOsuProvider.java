package com.telecominfraproject.wlan.opensync.ovsdb.dao.models;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;

public class WifiOsuProvider implements Cloneable {

    public static String[] ovsdbColumns = { "_version", "osu_nai", "icon", "server_uri", "method_list", "_uuid",
            "osu_friendly_name", "service_description" };
    public Uuid version;
    public Set<String> osuNai;
    public Set<String> icon;
    public String serverUri;
    public Set<Integer> methodList;
    public Uuid uuid;
    public Set<String> osuFriendlyName;
    public Set<String> serviceDescription;


    public WifiOsuProvider() {
    }

    public WifiOsuProvider(Row row) {
        this.version = row.getUuidColumn("_version");
        this.uuid = row.getUuidColumn("_uuid");
        this.osuNai = row.getSetColumn("osu_nai");
        this.icon = row.getSetColumn("icon");
        if ((row.getColumns().get("server_uri") != null) && row.getColumns().get("server_uri").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.serverUri = row.getStringColumn("server_uri");
        }
        this.methodList = row.getSetColumn("method_list");
        this.osuFriendlyName = row.getSetColumn("osu_friendly_name");
        this.serviceDescription = row.getSetColumn("service_description");
    }


    @Override
    public WifiOsuProvider clone() {
        try {
            WifiOsuProvider ret = (WifiOsuProvider) super.clone();

            if (osuNai != null) {
                ret.osuNai = new HashSet<>(this.osuNai);
            }
            if (icon != null) {
                ret.icon = new HashSet<>(this.icon);
            }
            if (methodList != null) {
                ret.methodList = new HashSet<>(this.methodList);
            }
            if (osuFriendlyName != null) {
                ret.osuFriendlyName = new HashSet<>(this.osuFriendlyName);
            }
            if (serviceDescription != null) {
                ret.serviceDescription = new HashSet<>(this.serviceDescription);
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
        return Objects.equals(icon, other.icon) && Objects.equals(methodList, other.methodList)
                && Objects.equals(osuFriendlyName, other.osuFriendlyName) && Objects.equals(osuNai, other.osuNai)
                && Objects.equals(serverUri, other.serverUri)
                && Objects.equals(serviceDescription, other.serviceDescription) && Objects.equals(uuid, other.uuid)
                && Objects.equals(version, other.version);
    }


}