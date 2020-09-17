package com.telecominfraproject.wlan.opensync.ovsdb.dao.models.enumerations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.telecominfraproject.wlan.core.model.extensibleenum.EnumWithId;


public class DhcpFpDeviceType implements EnumWithId {


    private static Object lock = new Object();
    private static final Map<Integer, DhcpFpDeviceType> ELEMENTS = new ConcurrentHashMap<>();
    private static final Map<String, DhcpFpDeviceType> ELEMENTS_BY_NAME = new ConcurrentHashMap<>();

    public static final DhcpFpDeviceType DHCP_FP_DEV_TYPE_MISC = new DhcpFpDeviceType(0, "DHCP_FP_DEV_TYPE_MISC"),
            DHCP_FP_DEV_TYPE_MOBILE = new DhcpFpDeviceType(1, "DHCP_FP_DEV_TYPE_MOBILE"),
            DHCP_FP_DEV_TYPE_PC = new DhcpFpDeviceType(2, "DHCP_FP_DEV_TYPE_PC"),
            DHCP_FP_DEV_TYPE_PRINTER = new DhcpFpDeviceType(3, "DHCP_FP_DEV_TYPE_PRINTER"),
            DHCP_FP_DEV_TYPE_VIDEO = new DhcpFpDeviceType(4, "DHCP_FP_DEV_TYPE_VIDEO"),
            DHCP_FP_DEV_TYPE_GAME = new DhcpFpDeviceType(5, "DHCP_FP_DEV_TYPE_GAME"),
            DHCP_FP_DEV_TYPE_VOIP = new DhcpFpDeviceType(6, "DHCP_FP_DEV_TYPE_VOIP"),
            DHCP_FP_DEV_TYPE_MONITORING = new DhcpFpDeviceType(7, "DHCP_FP_DEV_TYPE_MONITORING"),
            DHCP_FP_DEV_TYPE_MAX = new DhcpFpDeviceType(8, "DHCP_FP_DEV_TYPE_MAX"),
            UNSUPPORTED = new DhcpFpDeviceType(-1, "UNSUPPORTED");

    private final int id;
    private final String name;

    protected DhcpFpDeviceType(int id, String name) {
        synchronized (lock) {
            this.id = id;
            this.name = name;

            ELEMENTS_BY_NAME.values().forEach(s -> {
                if (s.getName().equals(name)) {
                    throw new IllegalStateException("DhcpFpDeviceType item for " + name
                            + " is already defined, cannot have more than one of them");
                }
            });

            if (ELEMENTS.containsKey(id)) {
                throw new IllegalStateException("DhcpFpDeviceType item " + name + "(" + id
                        + ") is already defined, cannot have more than one of them");
            }

            ELEMENTS.put(id, this);
            ELEMENTS_BY_NAME.put(name, this);
        }
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public static DhcpFpDeviceType getById(int enumId) {
        return ELEMENTS.get(enumId);
    }

    @JsonCreator
    public static DhcpFpDeviceType getByName(String value) {
        DhcpFpDeviceType ret = ELEMENTS_BY_NAME.get(value);
        if (ret == null) {
            ret = UNSUPPORTED;
        }

        return ret;
    }


    public static List<DhcpFpDeviceType> getValues() {
        return new ArrayList<>(ELEMENTS.values());
    }

    public static boolean isUnsupported(DhcpFpDeviceType value) {
        return (UNSUPPORTED.equals(value));
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DhcpFpDeviceType)) {
            return false;
        }
        DhcpFpDeviceType other = (DhcpFpDeviceType) obj;
        return id == other.id;
    }

    @Override
    public String toString() {
        return name;
    }


}
