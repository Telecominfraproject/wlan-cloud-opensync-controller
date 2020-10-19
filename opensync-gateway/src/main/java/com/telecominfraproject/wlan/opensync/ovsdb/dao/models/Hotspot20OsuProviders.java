package com.telecominfraproject.wlan.opensync.ovsdb.dao.models;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.telecominfraproject.wlan.opensync.ovsdb.dao.OvsdbDao;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;

public class Hotspot20OsuProviders implements Cloneable {

//    public static String[] ovsdbColumns = { "_version", "osu_nai", "osu_nai2", "osu_icons", "osu_provider_name",
//            "server_uri", "method_list", "_uuid", "osu_friendly_name", "service_description" };
    public static String[] ovsdbColumns = { "_version", "osu_nai", "osu_icons", 
            "server_uri", "method_list", "_uuid", "osu_friendly_name", "service_description" };
    public Uuid version;
    public String osuNai;
    /* public String osuNai2; */
    public Set<Uuid> osuIcons;
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
        if ((row.getColumns().get("osu_nai") != null) && row.getColumns().get("osu_nai").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.osuNai = row.getStringColumn("osu_nai");
        }
        /*
         * if (row.getColumns().containsKey("osu_nai2")) { this.osuNai2 =
         * OvsdbDao.getSingleValueFromSet(row, "osu_nai2"); }
         */

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
    public int hashCode() {
        return Objects.hash(methodList, osuFriendlyName, osuIcons, osuNai,
                /* osuNai2, */ serverUri, serviceDescription, uuid,
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
                /* && Objects.equals(osuNai2, other.osuNai2) */&& Objects.equals(serverUri, other.serverUri)
                && Objects.equals(serviceDescription, other.serviceDescription) && Objects.equals(uuid, other.uuid)
                && Objects.equals(version, other.version);
    }

    @Override
    public String toString() {
        return "Hotspot20OsuProviders [version=" + version + ", osuNai=" + osuNai
                /*+  ", osuNai2=" + osuNai2 */
                + ", osuIcons=" + osuIcons + ", serverUri=" + serverUri + ", methodList=" + methodList + ", uuid="
                + uuid + ", osuFriendlyName=" + osuFriendlyName + ", serviceDescription=" + serviceDescription + "]";
    }


}