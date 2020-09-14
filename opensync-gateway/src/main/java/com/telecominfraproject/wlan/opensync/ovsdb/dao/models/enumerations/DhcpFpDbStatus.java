package com.telecominfraproject.wlan.opensync.ovsdb.dao.models.enumerations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.telecominfraproject.wlan.core.model.extensibleenum.EnumWithId;


public class DhcpFpDbStatus implements EnumWithId {

    private static Object lock = new Object();
    private static final Map<Integer, DhcpFpDbStatus> ELEMENTS = new ConcurrentHashMap<>();
    private static final Map<String, DhcpFpDbStatus> ELEMENTS_BY_NAME = new ConcurrentHashMap<>();

    // typedef enum {
    // DHCP_FP_DB_SUCCESS = 0,
    // DHCP_FP_DB_FAILURE
    // } dhcp_fp_dbstatus_t;


    public static final DhcpFpDbStatus DHCP_FP_DB_SUCCESS = new DhcpFpDbStatus(0, "DHCP_FP_DB_SUCCESS"),
            DHCP_FP_DB_FAILURE = new DhcpFpDbStatus(1, "DHCP_FP_DB_FAILURE"),
            UNSUPPORTED = new DhcpFpDbStatus(-1, "UNSUPPORTED");

    private final int id;
    private final String name;

    protected DhcpFpDbStatus(int id, String name) {
        synchronized (lock) {
            this.id = id;
            this.name = name;

            ELEMENTS_BY_NAME.values().forEach(s -> {
                if (s.getName().equals(name)) {
                    throw new IllegalStateException("DhcpFpDbStatus item for " + name
                            + " is already defined, cannot have more than one of them");
                }
            });

            if (ELEMENTS.containsKey(id)) {
                throw new IllegalStateException("DhcpFpDbStatus item " + name + "(" + id
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

    public static DhcpFpDbStatus getById(int enumId) {
        return ELEMENTS.get(enumId);
    }

    @JsonCreator
    public static DhcpFpDbStatus getByName(String value) {
        DhcpFpDbStatus ret = ELEMENTS_BY_NAME.get(value);
        if (ret == null) {
            ret = UNSUPPORTED;
        }

        return ret;
    }


    public static List<DhcpFpDbStatus> getValues() {
        return new ArrayList<>(ELEMENTS.values());
    }

    public static boolean isUnsupported(DhcpFpDbStatus value) {
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
        if (!(obj instanceof DhcpFpDbStatus)) {
            return false;
        }
        DhcpFpDbStatus other = (DhcpFpDbStatus) obj;
        return id == other.id;
    }

    @Override
    public String toString() {
        return name;
    }
}
