package com.telecominfraproject.wlan.opensync.external.integration.models;

import java.util.Set;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.vmware.ovsdb.protocol.operation.notation.Row;

public abstract class OpensyncAPBase extends BaseJsonModel {

    private static final long serialVersionUID = -68509242520818671L;

    public static <T> T getSingleValueFromSet(Row row, String columnName) {

        Set<T> set = row != null ? row.getSetColumn(columnName) : null;
        T ret = (set != null) && !set.isEmpty() ? set.iterator().next() : null;

        return ret;
    }

    public OpensyncAPBase() {
    }

    public <T> Set<T> getSet(Row row, String columnName) {

        Set<T> set = row != null ? row.getSetColumn(columnName) : null;

        return set;
    }

}