package com.telecominfraproject.wlan.opensync.external.integration;

import java.util.concurrent.atomic.AtomicInteger;

import com.vmware.ovsdb.service.OvsdbClient;

public class OvsdbSession {
    private OvsdbClient ovsdbClient;
    private String apId;
    private long routingId;
    private long equipmentId;
    private AtomicInteger currentConfigNumInFlight = new AtomicInteger();
    
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
    
    public AtomicInteger getCurrentConfigNumInFlight() {
        return currentConfigNumInFlight;
    }
 
}
