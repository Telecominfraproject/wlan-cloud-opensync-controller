package com.telecominfraproject.wlan.opensync.external.integration;

import com.vmware.ovsdb.service.OvsdbClient;

public class OvsdbSession {
    private OvsdbClient ovsdbClient;
    private String apId;
    private long routingId;
    private long equipmentId;
    private int customerId;
    
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
    public int getCustomerId() {
        return customerId;
    }
    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }
    
    
}
