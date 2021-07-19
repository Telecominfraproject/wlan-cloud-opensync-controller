package com.telecominfraproject.wlan.opensync.external.integration;

import java.util.Objects;

import com.vmware.ovsdb.service.OvsdbClient;

public class OvsdbSession {
    private OvsdbClient ovsdbClient;
    private String apId;
    private long routingId;
    private long equipmentId;
    private long mostRecentStatsTimestamp;
    
    public OvsdbClient getOvsdbClient() {
        return ovsdbClient;
    }
    public void setOvsdbClient(OvsdbClient ovsdbClient) {
        this.ovsdbClient = ovsdbClient;
    }
    public String getApId() {
        return apId;
    }
    public void setApId(String apId) {
        this.apId = apId;
    }
    public long getRoutingId() {
        return routingId;
    }
    public void setRoutingId(long routingId) {
        this.routingId = routingId;
    }
    public long getEquipmentId() {
        return equipmentId;
    }
    public void setEquipmentId(long equipmentId) {
        this.equipmentId = equipmentId;
    }
    public long getMostRecentStatsTimestamp() {
        return mostRecentStatsTimestamp;
    }
    public void setMostRecentStatsTimestamp(long mostRecentStatsTimestamp) {
        this.mostRecentStatsTimestamp = mostRecentStatsTimestamp;
    }
    @Override
    public int hashCode() {
        return Objects.hash(apId, equipmentId, mostRecentStatsTimestamp, ovsdbClient, routingId);
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OvsdbSession other = (OvsdbSession) obj;
        return Objects.equals(apId, other.apId) && equipmentId == other.equipmentId && mostRecentStatsTimestamp == other.mostRecentStatsTimestamp
                && Objects.equals(ovsdbClient, other.ovsdbClient) && routingId == other.routingId;
    }
    
    @Override
    public String toString() {
        return "OvsdbSession [ovsdbClient=" + ovsdbClient + ", apId=" + apId + ", routingId=" + routingId + ", equipmentId=" + equipmentId
                + ", mostRecentStatsTimestamp=" + mostRecentStatsTimestamp + "]";
    }

    
}
