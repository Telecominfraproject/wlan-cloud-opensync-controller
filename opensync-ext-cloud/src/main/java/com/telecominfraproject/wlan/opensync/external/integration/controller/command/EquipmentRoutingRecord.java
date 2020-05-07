package com.telecominfraproject.wlan.opensync.external.integration.controller.command;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

public class EquipmentRoutingRecord extends BaseJsonModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2269183737047459318L;

	private long id;

    ////////////////////////////////////////////////////////////////////////////
    // If more parameters are required in this object, try to maintain the order
    // to match the DAO implementation.
    ////////////////////////////////////////////////////////////////////////////
    
    /** Unique identifier for a hardware device. */
    private long equipmentId;
    
    /** Unique identifier of a customer. */
    private int customerId;
    
    /** Unique identifier for a CustomerEquipmentGwRecord id. */
    private long gatewayRecordId;
    
    ////////////////////////////////////////////////////////////////////////////
    private long createdTimestamp;
    private long lastModifiedTimestamp;
    
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public long getLastModifiedTimestamp() {
        return lastModifiedTimestamp;
    }

    public void setLastModifiedTimestamp(long lastModifiedTimestamp) {
        this.lastModifiedTimestamp = lastModifiedTimestamp;
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

    public long getGatewayRecordId() {
        return gatewayRecordId;
    }

    public void setGatewayRecordId(long gatewayId) {
        this.gatewayRecordId = gatewayId;
    }

    @Override
    public EquipmentRoutingRecord clone() {
        return (EquipmentRoutingRecord) super.clone();
    }
    
    //WARNING: do not use any mutable fields in equals/hashCode - it screws up rule engine
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        return result;
    }

    //WARNING: do not use any mutable fields in equals/hashCode - it screws up rule engine
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof EquipmentRoutingRecord)) {
            return false;
        }
        EquipmentRoutingRecord other = (EquipmentRoutingRecord) obj;
        if (id != other.id) {
            return false;
        }
        return true;
    }
}
