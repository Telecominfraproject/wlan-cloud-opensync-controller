package com.telecominfraproject.wlan.opensync.ovsdb.dao.models;

import java.util.Objects;

import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;

public class Hotspot20IconConfig implements Cloneable {

    public static String[] ovsdbColumns = { "_version", "name", "path", "url", "lang_code", "height", "img_type",
            "_uuid", "width" };

    public String name;
    public String path;
    public String url;
    public String langCode;
    public Integer height;
    public String imgType;
    public Integer width;
    public Uuid uuid;
    public Uuid version;


    public Hotspot20IconConfig() {
    }

    public Hotspot20IconConfig(Row row) {
        this.version = row.getUuidColumn("_version");
        this.uuid = row.getUuidColumn("_uuid");
        if ((row.getColumns().get("name") != null) && row.getColumns().get("name").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.name = row.getStringColumn("name");
        }
        if ((row.getColumns().get("path") != null) && row.getColumns().get("path").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.path = row.getStringColumn("path");
        }
        if ((row.getColumns().get("url") != null) && row.getColumns().get("url").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.url = row.getStringColumn("url");
        }
        if ((row.getColumns().get("lang_code") != null) && row.getColumns().get("lang_code").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.langCode = row.getStringColumn("lang_code");
        }
        if ((row.getColumns().get("height") != null) && row.getColumns().get("height").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.height = row.getIntegerColumn("height").intValue();
        }

        if ((row.getColumns().get("width") != null) && row.getColumns().get("width").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.width = row.getIntegerColumn("width").intValue();
        }

        if ((row.getColumns().get("img_type") != null) && row.getColumns().get("img_type").getClass()
                .equals(com.vmware.ovsdb.protocol.operation.notation.Atom.class)) {
            this.imgType = row.getStringColumn("img_type");
        }

    }


    @Override
    public Hotspot20IconConfig clone() {
        try {
            Hotspot20IconConfig ret = (Hotspot20IconConfig) super.clone();
            return ret;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Cannot clone ", e);
        }
    }


    @Override
    public String toString() {
        return "Hotspot20IconConfig [name=" + name + ", path=" + path + ", url=" + url + ", langCode=" + langCode
                + ", height=" + height + ", imgType=" + imgType + ", width=" + width + ", uuid=" + uuid + ", version="
                + version + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(height, imgType, langCode, name, path, url, uuid, version, width);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Hotspot20IconConfig)) {
            return false;
        }
        Hotspot20IconConfig other = (Hotspot20IconConfig) obj;
        return Objects.equals(height, other.height) && Objects.equals(imgType, other.imgType)
                && Objects.equals(langCode, other.langCode) && Objects.equals(name, other.name)
                && Objects.equals(path, other.path) && Objects.equals(url, other.url)
                && Objects.equals(uuid, other.uuid) && Objects.equals(version, other.version)
                && Objects.equals(width, other.width);
    }


}