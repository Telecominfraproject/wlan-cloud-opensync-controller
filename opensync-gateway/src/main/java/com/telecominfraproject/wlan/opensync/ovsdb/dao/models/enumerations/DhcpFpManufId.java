package com.telecominfraproject.wlan.opensync.ovsdb.dao.models.enumerations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.telecominfraproject.wlan.core.model.extensibleenum.EnumWithId;


public class DhcpFpManufId implements EnumWithId {


    private static Object lock = new Object();
    private static final Map<Integer, DhcpFpManufId> ELEMENTS = new ConcurrentHashMap<>();
    private static final Map<String, DhcpFpManufId> ELEMENTS_BY_NAME = new ConcurrentHashMap<>();

    // typedef enum {
    // DHCP_FP_DEV_MANUF_MISC = 0,
    // DHCP_FP_DEV_MANUF_SAMSUNG = 1,
    // DHCP_FP_DEV_MANUF_APPLE = 2,
    // DHCP_FP_DEV_MANUF_GOOGLE = 3,
    // DHCP_FP_DEV_MANUF_HP = 4,
    // DHCP_FP_DEV_MANUF_INTEL = 5,
    // DHCP_FP_DEV_MANUF_MICROSOFT = 6,
    // DHCP_FP_DEV_MANUF_LG = 7,
    // DHCP_FP_DEV_MANUF_CANON = 8,
    // DHCP_FP_DEV_MANUF_BROTHER = 9,
    // DHCP_FP_DEV_MANUF_DELL = 10,
    // DHCP_FP_DEV_MANUF_LENOVO = 11,
    // DHCP_FP_DEV_MANUF_VIVO = 12,
    // DHCP_FP_DEV_MANUF_ALCATEL = 13,
    // DHCP_FP_DEV_MANUF_ZTE = 14,
    // DHCP_FP_DEV_MANUF_SONY = 15,
    // DHCP_FP_DEV_MANU_MAX = 16
    // } dhcp_fp_manufid_t;


    public static final DhcpFpManufId DHCP_FP_DEV_MANUF_MISC = new DhcpFpManufId(0, "DHCP_FP_DEV_MANUF_MISC"),
            DHCP_FP_DEV_MANUF_SAMSUNG = new DhcpFpManufId(1, "DHCP_FP_DEV_MANUF_SAMSUNG"),
            DHCP_FP_DEV_MANUF_APPLE = new DhcpFpManufId(2, "DHCP_FP_DEV_MANUF_APPLE"),
            DHCP_FP_DEV_MANUF_GOOGLE = new DhcpFpManufId(3, "DHCP_FP_DEV_MANUF_GOOGLE"),
            DHCP_FP_DEV_MANUF_HP = new DhcpFpManufId(4, "DHCP_FP_DEV_MANUF_HP"),
            DHCP_FP_DEV_MANUF_INTEL = new DhcpFpManufId(5, "DHCP_FP_DEV_MANUF_INTEL"),
            DHCP_FP_DEV_MANUF_MICROSOFT = new DhcpFpManufId(6, "DHCP_FP_DEV_MANUF_MICROSOFT"),
            DHCP_FP_DEV_MANUF_LG = new DhcpFpManufId(7, "DHCP_FP_DEV_MANUF_LG"),
            DHCP_FP_DEV_MANUF_CANON = new DhcpFpManufId(8, "DHCP_FP_DEV_MANUF_CANON"),
            DHCP_FP_DEV_MANUF_BROTHER = new DhcpFpManufId(9, "DHCP_FP_DEV_MANUF_BROTHER"),
            DHCP_FP_DEV_MANUF_DELL = new DhcpFpManufId(10, "DHCP_FP_DEV_MANUF_DELL"),
            DHCP_FP_DEV_MANUF_LENOVO = new DhcpFpManufId(11, "DHCP_FP_DEV_MANUF_LENOVO"),
            DHCP_FP_DEV_MANUF_VIVO = new DhcpFpManufId(12, "DHCP_FP_DEV_MANUF_VIVO"),
            DHCP_FP_DEV_MANUF_ALCATEL = new DhcpFpManufId(13, "DHCP_FP_DEV_MANUF_ALCATEL"),
            DHCP_FP_DEV_MANUF_ZTE = new DhcpFpManufId(14, "DHCP_FP_DEV_MANUF_ZTE"),
            DHCP_FP_DEV_MANUF_SONY = new DhcpFpManufId(15, "DHCP_FP_DEV_MANUF_SONY"),
            DHCP_FP_DEV_MANU_MAX = new DhcpFpManufId(16, "DHCP_FP_DEV_MANU_MAX"),
            UNSUPPORTED = new DhcpFpManufId(-1, "UNSUPPORTED");

    private final int id;
    private final String name;

    protected DhcpFpManufId(int id, String name) {
        synchronized (lock) {
            this.id = id;
            this.name = name;

            ELEMENTS_BY_NAME.values().forEach(s -> {
                if (s.getName().equals(name)) {
                    throw new IllegalStateException("DhcpFpManufId item for " + name
                            + " is already defined, cannot have more than one of them");
                }
            });

            if (ELEMENTS.containsKey(id)) {
                throw new IllegalStateException("DhcpFpManufId item " + name + "(" + id
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

    public static DhcpFpManufId getById(int enumId) {
        return ELEMENTS.get(enumId);
    }

    @JsonCreator
    public static DhcpFpManufId getByName(String value) {
        DhcpFpManufId ret = ELEMENTS_BY_NAME.get(value);
        if (ret == null) {
            ret = UNSUPPORTED;
        }

        return ret;
    }


    public static List<DhcpFpManufId> getValues() {
        return new ArrayList<>(ELEMENTS.values());
    }

    public static boolean isUnsupported(DhcpFpManufId value) {
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
        if (!(obj instanceof DhcpFpManufId)) {
            return false;
        }
        DhcpFpManufId other = (DhcpFpManufId) obj;
        return id == other.id;
    }

    @Override
    public String toString() {
        return name;
    }
}

