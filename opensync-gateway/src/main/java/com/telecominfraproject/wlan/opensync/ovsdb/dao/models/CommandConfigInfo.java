package com.telecominfraproject.wlan.opensync.ovsdb.dao.models;

import java.util.HashMap;
import java.util.Map;

import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Uuid;

public class CommandConfigInfo implements Cloneable {

    public long delay;
    public long duration;
    public String command;
    public Map<String, String> payload;
    public long timestamp;

    public Uuid uuid;

    public CommandConfigInfo(Row row) {
        this.uuid = row.getUuidColumn("_uuid");
        this.delay = row.getIntegerColumn("delay");
        this.duration = row.getIntegerColumn("duration");
        this.command = row.getStringColumn("command");
        this.payload = row.getMapColumn("payload");
        this.timestamp = row.getIntegerColumn("timestamp");
    }
    
    public CommandConfigInfo() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public CommandConfigInfo clone() {
        try {
            CommandConfigInfo ret = (CommandConfigInfo) super.clone();
            if (payload != null) {
                ret.payload = new HashMap<>(this.payload);
            }
            return ret;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Cannot clone ", e);
        }
    }

    @Override
    public String toString() {
        return String.format("CommandConfigInfo [delay=%s, duration=%s, command=%s, payload=%s, timestamp=%s, uuid=%s]",
                delay, duration, command, payload, timestamp, uuid);
    }

}