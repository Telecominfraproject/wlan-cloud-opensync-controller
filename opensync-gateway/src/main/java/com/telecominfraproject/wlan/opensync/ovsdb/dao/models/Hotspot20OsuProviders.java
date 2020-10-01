package com.telecominfraproject.wlan.opensync.ovsdb.dao.models;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;

public class Hotspot20OsuProviders implements Cloneable {

    public static String[] ovsdbColumns = { "_version", "osu_nai", "osu_icons", "server_uri", "method_list", "_uuid",
            "osu_friendly_name", "service_description" };
    public Uuid version;
    public Set<String> osuNai;
    public Set<String> osuIcons;
    public String serverUri;
    public Set<Integer> methodList;
    public Uuid uuid;
    public Set<String> osuFriendlyName;
    public Set<String> serviceDescription;

    public Hotspot20OsuProviders() {
    }

    public Hotspot20OsuProviders(Row row) {
        this.version = row.getUuidColumn("_version");
        this.uuid = row.getUuidColumn("_uuid");
        this.osuNai = row.getSetColumn("osu_nai");
        this.osuIcons = row.getSetColumn("osu_icons");
        if ((row.getColumns().get("server_uri") != null) && row.getColumns().get("server_uri").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.serverUri = row.getStringColumn("server_uri");
        }
        this.methodList = row.getSetColumn("method_list");
        this.osuFriendlyName = row.getSetColumn("osu_friendly_name");
        this.serviceDescription = row.getSetColumn("service_description");
    }


    @Override
    public Hotspot20OsuProviders clone() {
        try {
            Hotspot20OsuProviders ret = (Hotspot20OsuProviders) super.clone();

            if (osuNai != null) {
                ret.osuNai = new HashSet<>(this.osuNai);
            }
            if (osuIcons != null) {
                ret.osuIcons = new HashSet<>(this.osuIcons);
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
        return "Hotspot20OsuProviders [version=" + version + ", osuNai=" + osuNai + ", osuIcons=" + osuIcons
                + ", serverUri=" + serverUri + ", methodList=" + methodList + ", uuid=" + uuid + ", osuFriendlyName="
                + osuFriendlyName + ", serviceDescription=" + serviceDescription + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodList, osuFriendlyName, osuIcons, osuNai, serverUri, serviceDescription, uuid,
                version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Hotspot20OsuProviders)) {
            return false;
        }
        Hotspot20OsuProviders other = (Hotspot20OsuProviders) obj;
        return Objects.equals(methodList, other.methodList) && Objects.equals(osuFriendlyName, other.osuFriendlyName)
                && Objects.equals(osuIcons, other.osuIcons) && Objects.equals(osuNai, other.osuNai)
                && Objects.equals(serverUri, other.serverUri)
                && Objects.equals(serviceDescription, other.serviceDescription) && Objects.equals(uuid, other.uuid)
                && Objects.equals(version, other.version);
    }

  

}